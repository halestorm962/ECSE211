package localization;

import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;

public class USLocalizer {
	public enum LocalizationType { FALLING_EDGE, RISING_EDGE };
	public static int rotateSpeed = 50;
	private Odometer odometer;
	private SampleProvider usValue;
	private Navigation navigation;
	private float[] usData;
	private LocalizationType locType;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	private int wallDis = 30;

	
	public USLocalizer(SampleProvider usValue, float[] usData, EV3LargeRegulatedMotor leftMotor, 
			EV3LargeRegulatedMotor rightMotor, Navigation navigation, Odometer odometer, LocalizationType locType) {
		this.odometer = odometer;
		this.usValue = usValue;
		this.usData = usData;
		this.locType = locType;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.navigation = navigation;
	}
	
	
	public void doLoc(){
		double angleA, angleB, adjAngle;	
		
		//set the speed
		leftMotor.setSpeed(rotateSpeed);
		rightMotor.setSpeed(rotateSpeed);
		
		

	
		
		
	}
}