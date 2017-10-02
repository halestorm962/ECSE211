package ca.mcgill.ecse211.Navigation;

public class PController implements UltrasonicController {

  /* Constants */
  private static final int MOTOR_SPEED = 200;
  private static final int FILTER_OUT = 35;

  private final int bandCenter;
  private final int bandWidth;
  private int distance;
  private int filterControl;
  private int tooCloseControl;
  private int error_constant = 6;  //10
  
  public PController(int bandCenter, int bandwidth) {
    this.bandCenter = bandCenter;
    this.bandWidth = bandwidth;
    this.filterControl = 0;
    this.tooCloseControl = 0;

    Lab3.leftMotor.setSpeed(MOTOR_SPEED); // Initialize motor rolling forward
    Lab3.rightMotor.setSpeed(MOTOR_SPEED);
    Lab3.leftMotor.forward();
    Lab3.rightMotor.forward();
  }

  @Override
  public void processUSData(int distance) {
    // rudimentary filter - toss out invalid samples corresponding to null signal
	  
    if ((distance >= 255 && filterControl < FILTER_OUT) || (distance > 20000)) {
      // bad value, do not set the distance var, do increment the filter value
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

    // TODO: process a movement based on the us distance passed in (P style)
    // ASSUME COUNTERCLOCKWISE MOVEMENT
    // RIGHT (inner) MOTOR IS CONNECTED TO PORT D AND HAS A BLUE PIN ON TOP
    int error = Math.abs(this.distance - bandCenter);
	int speed_adjustment = error_constant*error; 
	if (speed_adjustment > 175){
		// set the limit for max. of adjustSpeed
		speed_adjustment = 175;
	}
	
	if (this.distance > bandCenter + (bandWidth/2)) {
		Lab3.rightMotor.forward();
		Lab3.leftMotor.forward();
		// proportionally increase speed of outer wheel
    	Lab3.rightMotor.setSpeed(MOTOR_SPEED + speed_adjustment);
    	Lab3.leftMotor.setSpeed(MOTOR_SPEED - (speed_adjustment/6));
    } else if (this.distance < bandCenter - (bandWidth/2) && this.distance >= 10) {
		Lab3.rightMotor.forward();
		Lab3.leftMotor.forward();
    	// proportionally increase speed of inner wheel
    	Lab3.rightMotor.setSpeed(MOTOR_SPEED - (speed_adjustment/6));
    	Lab3.leftMotor.setSpeed(MOTOR_SPEED + speed_adjustment);
    } else if (this.distance < 20) {
    	// much too close, pivot. Filter to make sure it isn't an erroneous reading. 10
    	if (this.tooCloseControl < 2) {
    		this.tooCloseControl++;
    	} else {
    		this.tooCloseControl = 0;
    		Lab3.rightMotor.backward();
    		Lab3.leftMotor.forward();
    		Lab3.leftMotor.setSpeed(MOTOR_SPEED / 2);
        	Lab3.rightMotor.setSpeed(MOTOR_SPEED / 2);
    	}
    } else {
		Lab3.rightMotor.forward();
		Lab3.leftMotor.forward();
    	// distance is on track
    	Lab3.leftMotor.setSpeed(MOTOR_SPEED);
    	Lab3.rightMotor.setSpeed(MOTOR_SPEED);
    }
  }

  @Override
  public int readUSDistance() {
    return this.distance;
  }
}