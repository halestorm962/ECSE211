package localization;

import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class USLocalizer implements UltrasonicController {
	public enum LocalizationType { FALLING_EDGE, RISING_EDGE };
	public static int rotateSpeed = 50;
	private Odometer odometer;
	private LocalizationType locType;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	
	// constants
	private double axleWidth, wheelRadius;	// passed in on system startup
	private int filterControl = 0;
	private static final int FILTER_OUT = 35;
	private int d = 40;	// drop-off point
	private int k = 1;	// error margin
	
	// variables
	private int distance;

	
	public USLocalizer(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, 
			Odometer odometer, LocalizationType locType) {
		this.odometer = odometer;
		this.locType = locType;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
	}
	
	
	public void doLoc(){
		double angleA, angleB, adjAngle;	
		
		//set the speed and acceleration
		this.leftMotor.setSpeed(rotateSpeed);
		this.rightMotor.setSpeed(rotateSpeed);
		this.leftMotor.setAcceleration(500);
		this.rightMotor.setAcceleration(500);
		
		// rotate clockwise
		this.leftMotor.forward();
		this.rightMotor.backward();
		
		// if falling edge
		if (this.locType == LocalizationType.FALLING_EDGE) {
			while (this.distance < d - k) {
				 //keep turning
			}
			
			// wall detected yet? If not, rotate more
			while (this.distance >= d - k) {
				// keep turning
			}
			
			// wait for first falling edge
			//while (this.distance < d - k) {
				 //keep turning
			//}
			this.leftMotor.stop();
			this.rightMotor.stop();
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// record angle
			angleA = this.odometer.getTheta();
			
			leftMotor.rotate(-convertAngle(lab4.wheelRadius, lab4.axleWidth, 90), true);
			rightMotor.rotate(convertAngle(lab4.wheelRadius, lab4.axleWidth, 90), false);
			
			// turn counterclockwise until next falling edge
			this.leftMotor.backward();
			this.rightMotor.forward();
			
			
			//while (this.distance < d - k) {
				// keep turning
			//}
			
			//I change the logic here, please check
			
			while (this.distance >= d - k) {
				// keep turning
			}
			this.leftMotor.stop();
			this.rightMotor.stop();
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// record angle
			angleB = this.odometer.getTheta();
			
			// calculate middle of those two angles
			adjAngle = angleA + (angleB - angleA)/2;		// TODO: really needed?
			
			// rotate to (hopefully) 0deg
			//TODO: now the turnTo is not working
			turnTo(angleB - ((angleB - angleA)/2) + 3*Math.PI/4);
			this.odometer.setTheta(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
		}
		
		// if rising edge
		else {
			//TODO: the logic here will cause infinite turning, check.
			// already facing wall? Turn more
			while (this.distance <= d + k) {
				 //keep turning
			}
			
			// wait for first rising edge
			//while (this.distance > d + k) {
				// keep turning
			//}
			this.leftMotor.stop();
			this.rightMotor.stop();
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// record angle
			angleA = this.odometer.getTheta();
			
			leftMotor.rotate(-convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), true);
			rightMotor.rotate(convertAngle(lab4.wheelRadius, lab4.axleWidth, 45), false);
			
			// turn counterclockwise until next rising edge
			this.leftMotor.backward();
			this.rightMotor.forward();
			
			//I change the sign here, please check
			while (this.distance <= d + k) {
				// keep turning
			}
			this.leftMotor.stop();
			this.rightMotor.stop();
			Sound.playNote(Sound.FLUTE, 880, 250);
			
			// record angle
			angleB = this.odometer.getTheta();
			
			// calculate middle of those two angles
			adjAngle = angleA + (angleB - angleA)/2;	// TODO: really needed?
			
			
			// rotate to (hopefully) 0deg
			//TODO: now the turnTo is not working
			turnTo(angleB - ((angleB - angleA)/2) + 3*Math.PI/4);
			this.odometer.setTheta(0);
			Sound.playNote(Sound.FLUTE, 880, 250);
		}
	}
	
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
	}
	
	public void turnTo(double theta) {			
		//convert to degrees
		theta = Math.toDegrees(theta);
		
		//turn to calculated angle
		int rotation = convertAngle(wheelRadius, axleWidth, theta);
		
		// rotate the appropriate direction (sign on theta accounts for direction
		this.leftMotor.rotate(rotation, true);
		this.rightMotor.rotate(-rotation, false);
		
		this.leftMotor.stop();
		this.rightMotor.stop();
	}
	
	@Override
	public int readUSDistance() {
	  return this.distance;
	}
	

	
	// calculation methods
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
}