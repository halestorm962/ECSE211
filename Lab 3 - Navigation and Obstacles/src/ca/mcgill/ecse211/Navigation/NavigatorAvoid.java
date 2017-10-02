package ca.mcgill.ecse211.Navigation;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class NavigatorAvoid extends Thread implements UltrasonicController {
	
	private Odometer odometer;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	final TextLCD t = LocalEV3.get().getTextLCD();
	boolean navigating;
	private static final long ODOMETER_PERIOD = 25; /*odometer update period, in ms*/
	
	// constants
	private int FORWARD_SPEED = 200;
	private int ROTATE_SPEED = 100;
	private double axleWidth, wheelRadius;	// passed in on system startup
	private double thetaNow, xNow, yNow;	// current heading
	private double gridLength = 30.48;
	private boolean avoidingWall = false;
	private int filterCount = 0;
	private int[] filterStorage = new int[5];
	private int filterControl = 0;
	private static final int FILTER_OUT = 35;
	private int tooCloseControl = 0;
	private final int bandCenter = 20;
	private final int bandWidth = 6;
	
	// variables
	private int distance;
	private int avgDistance = 20;
	
	public NavigatorAvoid (Odometer odometer, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
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
		// getCoordinates();
		x *= gridLength;
		y *= gridLength;
		
		double deltaX = x - xNow;
		double deltaY = y - yNow;
		
		// calculate distance between current position and next coordinate
		double distanceToNext = Math.hypot(deltaX, deltaY);
		
		// calculate angle between current position and next coordinate
		double thetaToNextPoint = Math.atan2(deltaX, deltaY) - thetaNow;
		
		// ensure the robot rotates the least amount necessary
		if (thetaToNextPoint > Math.PI) {
			thetaToNextPoint -= 2*Math.PI;
		} else if (thetaToNextPoint < -(Math.PI)){
			thetaToNextPoint += 2*Math.PI;
		}
		
		turnTo(thetaToNextPoint);
		
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
		theta = Math.toDegrees(theta);
		
		//turn to calculated angle
		int rotation = convertAngle(wheelRadius, axleWidth, theta);
		
		navigating = true;
		// rotate the appropriate direction (sign on theta accounts for direction
		leftMotor.rotate(rotation, true);
		rightMotor.rotate(-rotation, false);
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
	
	@Override
	  public void processUSData(int distance) {
		// rudimentary filter - toss out invalid samples corresponding to null signal 
	    if (distance >= 255 && filterControl < FILTER_OUT) {
	      // bad value: do not set the distance var, do increment the filter value
	      this.filterControl++;
	    } else if (distance >= 255) {
	      // We have repeated large values, so there must actually be nothing
	      // there: leave the distance alone
	      this.distance = distance;
	    } else {
	      // distance went below 255: reset filter and leave
	      // distance alone.
	      this.filterControl = 0;
	      this.distance = distance;
	    }
		
	    /*
	    // process a movement based on the us distance passed in (BANG-BANG style)
	    // ASSUME COUNTERCLOCKWISE MOVEMENT
		if (this.distance > bandCenter + (bandWidth/2)) {
			rightMotor.forward();
			leftMotor.forward();
			// too far: reduce speed of inner wheel
			rightMotor.setSpeed(FORWARD_SPEED);
	    	leftMotor.setSpeed(ROTATE_SPEED);
	    } else if (this.distance < bandCenter - (bandWidth/2) && this.distance > 10) {
	    	rightMotor.forward();
			leftMotor.forward();
	    	// too close: reduce speed of outer wheel
	    	leftMotor.setSpeed(FORWARD_SPEED);
	    	rightMotor.setSpeed(ROTATE_SPEED);
	    } else if (this.distance <= 12) {
	    	// much too close, pivot. Filter to make sure it isn't an erroneous reading. 10
	    	if (this.tooCloseControl < 2) {
	    		this.tooCloseControl++;
	    	} else {
	    		this.tooCloseControl = 0;
	    		rightMotor.backward();
	    		leftMotor.forward();
	    		leftMotor.setSpeed(ROTATE_SPEED);
	        	rightMotor.setSpeed(ROTATE_SPEED);
	    	}
	    } else {
	    	rightMotor.forward();
			leftMotor.forward();
	    	// distance correct, set both wheels to high
			leftMotor.setSpeed(FORWARD_SPEED);
	    	rightMotor.setSpeed(FORWARD_SPEED);
	    }
	    */
	    
	    t.drawString("US reading:            ", 0, 5);
	    t.drawString("US reading: " + distance, 0, 5);
		if (distance < 10) {
			// turn 90 degrees right
			turnTo(Math.PI/2);
			
			// move half a block forward
			leftMotor.rotate(convertDistance(wheelRadius, gridLength/2), true);
			rightMotor.rotate(convertDistance(wheelRadius, gridLength/2), false);
			
			// repeat the following until the robot is back on the intended path
				// turn 90 degrees left
				// if distance < 10, turn 90 degrees right and move 1/2 block forward
				// if distance > 10, move 1/2 block forward
		}
	  }

	  @Override
	  public int readUSDistance() {
	    return this.distance;
	  }
}