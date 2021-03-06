package localization;

import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * Performs heading localization using the ultrasonic sensor and the walls of the field.
 * 
 * @author Alex Hale
 * @author Xianyi Zhan
 */
public class USLocalizer implements UltrasonicController {
	public enum LocalizationType { FALLING_EDGE, RISING_EDGE };
	public static int rotateSpeed = 50;
	private Odometer odometer;
	private LocalizationType locType;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	final TextLCD display = LocalEV3.get().getTextLCD();
	
	// constants
	private int filterControl = 0;
	private static final int FILTER_OUT = 35;
	private int d = 40;	// drop-off point
	private int k = 1;	// error margin
	
	// variables
	private int distance;
	
	/**
	 * Constructor
	 * 
	 * @param leftMotor - robot's left motor
	 * @param rightMotor - robot's right motor
	 * @param odometer - robot's odometer
	 * @param locType - rising edge or falling edge
	 */
	public USLocalizer(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, 
			Odometer odometer, LocalizationType locType) {
		this.odometer = odometer;
		this.locType = locType;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
	}
	
	/**
	 * Performs heading localization.
	 */
	public void doLoc(){
		//set the speed and acceleration
		this.leftMotor.setSpeed(rotateSpeed);
		this.rightMotor.setSpeed(rotateSpeed);
		this.leftMotor.setAcceleration(500);
		this.rightMotor.setAcceleration(500);
		
		// if falling edge
		if (this.locType == LocalizationType.FALLING_EDGE) {
			// avoid finding falling edge too early
			leftMotor.rotate(convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), true);
			rightMotor.rotate(-convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), false);
			
			// rotate clockwise
			this.leftMotor.forward();
			this.rightMotor.backward();
			
			// keep turning until wall drops away
			while (this.distance >= d - k) {
				 //keep turning
			}

			this.leftMotor.setSpeed(0);
			this.rightMotor.setSpeed(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// start counting change in angle
			odometer.setTheta(0);
			
			this.leftMotor.setSpeed(rotateSpeed);
			this.rightMotor.setSpeed(rotateSpeed);
			
			// avoid finding falling edge too early
			leftMotor.rotate(-convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), true);
			rightMotor.rotate(convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), false);
			
			// turn counterclockwise until next rising edge
			this.leftMotor.backward();
			this.rightMotor.forward();
			
			while (this.distance >= d - k) {
				// keep turning
			}

			this.leftMotor.setSpeed(0);
			this.rightMotor.setSpeed(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// record change in angle
			double deltaTheta = Math.abs(this.odometer.getTheta());
			
			this.leftMotor.setSpeed(rotateSpeed);
			this.rightMotor.setSpeed(rotateSpeed);
			
			// rotate to (hopefully) 0deg via short path
			this.turnTo(deltaTheta/2 - Math.PI/7);		// mathematically should be Math.PI/4;
														// in reality falls short
			
			this.odometer.setTheta(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
		}
		
		// if rising edge
		else {			
			// avoid finding rising edge too early
			leftMotor.rotate(convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), true);
			rightMotor.rotate(-convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), false);
			
			// rotate clockwise
			this.leftMotor.forward();
			this.rightMotor.backward();
			
			// keep turning until wall drops away
			while (this.distance <= d + k) {
				 //keep turning
			}

			this.leftMotor.setSpeed(0);
			this.rightMotor.setSpeed(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// start counting change in angle
			odometer.setTheta(0);
			
			this.leftMotor.setSpeed(rotateSpeed);
			this.rightMotor.setSpeed(rotateSpeed);
			
			// avoid finding rising edge too early
			leftMotor.rotate(-convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), true);
			rightMotor.rotate(convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), false);
			
			// turn counterclockwise until next rising edge
			this.leftMotor.backward();
			this.rightMotor.forward();
			
			while (this.distance <= d + k) {
				// keep turning
			}

			this.leftMotor.setSpeed(0);
			this.rightMotor.setSpeed(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// record change in angle
			double deltaTheta = Math.abs(this.odometer.getTheta());
			
			this.leftMotor.setSpeed(rotateSpeed);
			this.rightMotor.setSpeed(rotateSpeed);
			
			// rotate to (hopefully) 0deg via short path
			this.turnTo(-(5*Math.PI/4 - deltaTheta/1.8));	// mathematically should be deltaTheta/2;
															// in reality undershoots
			
			this.odometer.setTheta(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
		}
	}
	
	/**
	 * Filters out invalid ultrasonic (US) samples, prints US reading to screen.
	 * 
	 * @param distance - the reading of the ultrasonic sensor.
	 */
	@Override
    public void processUSData (int distance) {
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
	    
	    display.drawString("                    ", 0, 6);
	    display.drawString("US: " + this.distance, 0, 6);
	}
	
	/**
	 * Turns robot the specified amount.
	 * 
	 * @param theta - number of radians to turn. theta > 0 => clockwise, theta < 0 => counterclockwise
	 */
	public void turnTo(double theta) {			
		//convert to degrees
		theta = Math.toDegrees(theta);
		
		//turn to calculated angle
		int rotation = convertAngle(lab4.wheelRadius, lab4.axleWidth, theta);
		
		// rotate the appropriate direction (sign on theta accounts for direction)
		this.leftMotor.rotate(rotation, true);
		this.rightMotor.rotate(-rotation, false);
		
		this.leftMotor.stop();
		this.rightMotor.stop();
	}
	
	/**
	 * @return ultrasonic sensor reading
	 */
	@Override
	public int readUSDistance() {
	  return this.distance;
	}
	
	/**
	 * Converts travel distance to number of degrees of rotation of robot's wheel.
	 * 
	 * @param radius - radius of robot's wheel (cm)
	 * @param distance - distance needed to be traveled (cm)
	 * @return number of degrees of wheel rotation (degrees)
	 */
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}
	
	/**
	 * Converts robot rotation to number of degrees each wheel must turn (in opposite directions).
	 * 
	 * @param radius - robot wheel radius (cm)
	 * @param width - robot axle width (cm)
	 * @param angle - amount of robot rotation (radians)
	 * @return number of degrees of wheel rotation (degrees)
	 */
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
}
