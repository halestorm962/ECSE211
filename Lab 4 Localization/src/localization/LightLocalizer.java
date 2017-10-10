package localization;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.hardware.Sound;

public class LightLocalizer {
	// constants
	private final double f = 3.3;	// distance between light sensor and wheels
	public static int forwardSpeed = 100;
	private static final int blackValue = 200;	// TODO: needs to be changed because location of sensor changed
	double wheelRadius, axleWidth;
	
	Odometer odometer;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	final TextLCD display = LocalEV3.get().getTextLCD();
	
	// color sensor setup
	private static final Port lightPort = LocalEV3.get().getPort("S1");
	SensorModes lightSensor = new EV3ColorSensor(lightPort);
	SampleProvider lightValue = lightSensor.getMode("Red");			// provides samples from this instance
	float[] lightData = new float[lightValue.sampleSize()];			// buffer in which data are returned
	
	public LightLocalizer(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, 
			Odometer odometer, double wheelRadius, double axleWidth) {
		this.odometer = odometer;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.wheelRadius = wheelRadius;
		this.axleWidth = axleWidth;
	}
	
	public void doLoc(){
		// assumptions:
			// light sensor is in front of wheels by distance f
			// starting oriented at 0deg somewhere in bottom left square
				// i.e. after US localization has happened
		
		// set speed and acceleration
		this.leftMotor.setSpeed(forwardSpeed);
		this.rightMotor.setSpeed(forwardSpeed);
		this.leftMotor.setAcceleration(500);
		this.rightMotor.setAcceleration(500);
		
		
		// move forward until a line is detected
		this.leftMotor.forward();
		this.rightMotor.forward();
		
		// get light value
	    lightValue.fetchSample(lightData, 0);
	    float reading = lightData[0] * 1000;		// scale up for more accuracy
	    display.drawString("Color: " + reading, 0, 4);
	    
		while (reading <= blackValue) {
			// update light value
		    lightValue.fetchSample(lightData, 0);
		    reading = lightData[0] * 1000;		// scale up for more accuracy
		    display.drawString("                  ", 0, 4);
		    display.drawString("Color: " + reading, 0, 4);
		}
		this.leftMotor.stop();
		this.rightMotor.stop();
		Sound.playNote(Sound.FLUTE, 880, 250);
		
		// turn 90deg clockwise
		this.leftMotor.rotate(convertAngle(this.wheelRadius, this.axleWidth, 90), true);
		this.rightMotor.rotate(-convertAngle(this.wheelRadius, this.axleWidth, 90), false);
		
		// move forward until a line is detected, stop
		// get light value
	    lightValue.fetchSample(lightData, 0);
	    reading = lightData[0] * 1000;		// scale up for more accuracy
	    display.drawString("Color: " + reading, 0, 4);		
		while (reading <= blackValue) {
			// update light value
		    lightValue.fetchSample(lightData, 0);
		    reading = lightData[0] * 1000;		// scale up for more accuracy
		    display.drawString("Color: " + reading, 0, 4);
		}
		this.leftMotor.stop();
		this.rightMotor.stop();
		Sound.playNote(Sound.FLUTE, 880, 250);
		
		// move f cm forward
		this.leftMotor.rotate(convertDistance(this.wheelRadius, f), true);
		this.rightMotor.rotate(convertDistance(this.wheelRadius, f), false);
		
		// turn 90deg counterclockwise
		this.leftMotor.rotate(-convertAngle(this.wheelRadius, this.axleWidth, 90), true);
		this.rightMotor.rotate(convertAngle(this.wheelRadius, this.axleWidth, 90), false);
		
		// move f cm forward
		this.leftMotor.rotate(convertDistance(this.wheelRadius, f), true);
		this.rightMotor.rotate(convertDistance(this.wheelRadius, f), false);
		
		// done => zero odometer values
		this.odometer.setX(0);
		this.odometer.setY(0);
		this.odometer.setTheta(0);
	}
	
	// calculation methods
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
}
