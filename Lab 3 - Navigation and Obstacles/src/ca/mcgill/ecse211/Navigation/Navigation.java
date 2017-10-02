package ca.mcgill.ecse211.Navigation;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigation extends Thread {
	
	private Odometer odometer;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	boolean navigating;
	private static final long ODOMETER_PERIOD = 25; /*odometer update period, in ms*/
	
	// constants
	private int FORWARD_SPEED = 200;
	private int ROTATE_SPEED = 100;
	private double axleWidth, wheelRadius;	// passed in on system startup
	private double thetaNow, xNow, yNow;	// current heading
	private double gridLength = 30.48;
	TextLCD t;
	
	public Navigation (Odometer odometer, EV3LargeRegulatedMotor leftMotor,EV3LargeRegulatedMotor rightMotor,
			double axleWidth, double wheelRadius){
		this.odometer = odometer;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.axleWidth = axleWidth;
		this.wheelRadius = wheelRadius;
		leftMotor.setAcceleration(500);
		rightMotor.setAcceleration(500);
		navigating = false;
		
		t = LocalEV3.get().getTextLCD();
	}
	
	public void run() {
		long updateStart, updateEnd;
		while (true) {
			updateStart = System.currentTimeMillis();
			
			getCoordinates();
			
			// this ensures that the odometer only runs once every period
		      updateEnd = System.currentTimeMillis();
		      if (updateEnd - updateStart < ODOMETER_PERIOD) {
		        try {
		          Thread.sleep(ODOMETER_PERIOD - (updateEnd - updateStart));
		        } catch (InterruptedException e) {
		          // there is nothing to be done here because it is not
		          // expected that the odometer will be interrupted by
		          // another thread
		        }
		      }
		}
		
	}
	
	public void travelTo(double x, double y) {	
		getCoordinates();
		x *= gridLength;
		y *= gridLength;
		
		double deltaX = x - xNow;
		double deltaY = y - yNow;
		
		// calculate distance between current position and next coordinate (uses Euclidian error)
		double distanceToNext = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
		
		// check which direction X and Y need to change (i.e. whether we need to add or subtract pi)
		double thetaToNextPoint;
		if (deltaX == 0 && deltaY > 0) {
			thetaToNextPoint = 0;
		} else if (deltaX == 0 && deltaY < 0) {
			thetaToNextPoint = Math.PI;
		} else if (deltaX > 0 && deltaY == 0) {
			thetaToNextPoint = Math.PI/2;
		} else if (deltaX < 0 && deltaY == 0) {
			thetaToNextPoint = 3*Math.PI/2;
		} else {
			// the angle of the vector between the current position and the next position
			thetaToNextPoint = Math.atan(deltaY/deltaX);	
		}
		
		if (deltaX < 0 && deltaY > 0) {
			System.out.println("here");
			thetaToNextPoint += Math.PI;
		} else if (deltaX < 0 && deltaY < 0) {
			thetaToNextPoint -= Math.PI;
		}
		
		t.clear();
		t.drawString("deltaX: " + deltaX, 0, 5);
		t.drawString("deltaY: " + deltaY, 0, 6);
		
		// the difference between the robot's current heading and the heading that points
		// to the next position
		double differenceInTheta = thetaToNextPoint - thetaNow;
		
		// ensure the robot rotates the least amount necessary
		if (differenceInTheta > Math.PI) {
			differenceInTheta -= 2*Math.PI;
		} else if (differenceInTheta < -(Math.PI)){
			differenceInTheta += 2*Math.PI;
		}
		
		/*t.clear();
		t.drawString("distance: " + distanceToNext, 0, 5);
		t.drawString("x: " + x, 0, 6);
		t.drawString("y: " + y, 0, 7);
		t.drawString("deltaTheta: " + differenceInTheta*180/Math.PI, 0, 7);*/
		
		turnTo(differenceInTheta);
		
		leftMotor.setAcceleration(500);
		rightMotor.setAcceleration(500);
		leftMotor.setSpeed(FORWARD_SPEED);
		rightMotor.setSpeed(FORWARD_SPEED);
		
		navigating = true;
		leftMotor.forward();
		rightMotor.forward();
		
		leftMotor.rotate(convertDistance(wheelRadius,distanceToNext), true);
		rightMotor.rotate(convertDistance(wheelRadius,distanceToNext), false);
		
		leftMotor.stop();
		rightMotor.stop();
		
		navigating = false;
	}
	
	public void turnTo(double theta) {	
		// slow down
		leftMotor.setSpeed(ROTATE_SPEED);
		rightMotor.setSpeed(ROTATE_SPEED);
		
		//convert to degrees
		theta = theta * 180 / Math.PI;
		
		//turn to calculated angle
		int rotation = convertAngle(wheelRadius, axleWidth, theta);
		
		navigating = true;
		// rotate the appropriate direction
		if (rotation > 0) {
			leftMotor.rotate(rotation, true);
			rightMotor.rotate(-rotation, false);
		} else {
			leftMotor.rotate(-rotation, true);
			rightMotor.rotate(rotation, false);
		}
		navigating = false;
		
		leftMotor.stop();
		rightMotor.stop();
	}
	
	public boolean isNavigating() {
		return navigating;
	}
	
	public void getCoordinates() {
		// synchronize robot's current position
	 	synchronized (odometer.lock) {
			thetaNow = odometer.getTheta();
			xNow = odometer.getX();
			yNow = odometer.getY();
		}
	}
	
	// calculation methods
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
}