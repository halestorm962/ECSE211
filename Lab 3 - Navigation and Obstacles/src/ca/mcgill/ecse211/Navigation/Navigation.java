package ca.mcgill.ecse211.Navigation;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigation extends Thread {
	
	private Odometer odometer;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	boolean navigating;
	private static final long ODOMETER_PERIOD = 25; /*odometer update period, in ms*/
	
	// constants
	private int FORWARD_SPEED = 200;
	private int ROTATE_SPEED = 100;
	private double axleWidth = 12.7, wheelRadius = 2.125;
	private double thetaNow, xNow, yNow;		// current heading
	private double thetaNext, xNext, yNext;	// next heading
	
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
	}
	
	public void run() {
		long updateStart, updateEnd;
		while (true) {
			updateStart = System.currentTimeMillis();
			
			// synchronize robot's current position
		 	synchronized (odometer.lock) {
				thetaNow = odometer.getTheta();
				xNow = odometer.getX();
				yNow = odometer.getY();
			}
			
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
		// calculate distance between current position and next coordinate
		// TODO: decide if we actually need to do this
		double distanceToNext = Math.sqrt(Math.pow(x - xNow, 2) + Math.pow(y-yNow, 2));
		
		// calculate difference in theta required
		double deltaX = x - xNow;
		double deltaY = y - yNow;
		
		// TODO: check for special cases (moving along x or y axis)
		
		double coordinateTheta = Math.atan(x/y);
		
		double differenceInTheta = thetaNow - coordinateTheta;
		
		if (differenceInTheta > Math.PI) {
			differenceInTheta -= 2*Math.PI;
		} else if (differenceInTheta < -(Math.PI)){
			differenceInTheta += 2*Math.PI;
		}
		
		navigating = true;
		turnTo(differenceInTheta);
		
		leftMotor.setAcceleration(500);
		rightMotor.setAcceleration(500);
		leftMotor.setSpeed(FORWARD_SPEED);
		rightMotor.setSpeed(FORWARD_SPEED);
		
		leftMotor.rotate(convertDistance(wheelRadius,distanceToNext), true);
		rightMotor.rotate(convertDistance(wheelRadius,distanceToNext), false);
		
		leftMotor.forward();
		rightMotor.forward();

		leftMotor.stop();
		rightMotor.stop();
		
		navigating = false;
	}
	
	public void turnTo(double theta) {
		leftMotor.setSpeed(ROTATE_SPEED);
		rightMotor.setSpeed(ROTATE_SPEED);
		
		//convert to degrees
		theta = theta * 180 / Math.PI;
		
		//turns to calculated angle
		navigating = true;
		int rotation = convertAngle(wheelRadius, axleWidth, theta);
		leftMotor.rotate(rotation, true);
		rightMotor.rotate(-rotation, false);
		navigating = false;
		
		leftMotor.stop();
		rightMotor.stop();
	}
	
	public boolean isNavigating() {
		return navigating;
	}
	
	// calculation methods
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
}
