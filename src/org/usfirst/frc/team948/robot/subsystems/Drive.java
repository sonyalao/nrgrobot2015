// RobotBuilder Version: 1.5
//
// This file was generated by RobotBuilder. It contains sections of
// code that are automatically generated and assigned by robotbuilder.
// These sections will be updated in the future when you export to
// Java from RobotBuilder. Do not put any code or make any change in
// the blocks indicating autogenerated code or it will be lost on an
// update. Deleting the comments indicating the section will prevent
// it from being updated in the future.

package org.usfirst.frc.team948.robot.subsystems;

import org.usfirst.frc.team948.robot.*;
import org.usfirst.frc.team948.robot.commands.*;
import org.usfirst.frc.team948.robot.utilities.MathHelper;
import org.usfirst.frc.team948.robot.utilities.PreferenceKeys;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The subsystem, which controls the robot drive train.
 * 
 * <p>
 * Provides the tankDrive method, a method for basic robot driving. It also has
 * driveOnHeading, a method to drive in a straight line on a certain heading.
 */
public class Drive extends Subsystem implements PIDOutput {

	private static final double OSCILLATION_PERIOD = 0.844;
	private final double PID_MIN_OUTPUT = 0.08;
	private final double PID_MAX_OUTPUT = 1;
	private final double DRIVE_STRAIGHT_ON_HEADING_P = 0.01;
	private final double DRIVE_STRAIGHT_ON_HEADING_I = 0.001;
	private final double DRIVE_STRAIGHT_ON_HEADING_D = 0.005;
	private final double PID_INTERVAL = 0.05; // PID runs every 50 ms
	private final double TURN_TO_HEADING_P_OSCILLATE = 0.21;
	private final double TURN_TO_HEADING_P = 0.038;
	private final double TURN_TO_HEADING_I = 0.004;
	private final double TURN_TO_HEADING_D = 0.25;
	private final int REQUIRED_CYCLES_ON_TARGET = 6;
	public final PIDController drivePID = new PIDController(0.01,
			0.01 * 2 * 0.05, 0.005, RobotMap.driveGyro, this);
	private double pidOutput;

	private static final double MAX_VOLTAGE = 10;
	private static final double MIN_VOLTAGE = 7;

	private double desiredHeading;
	private int cyclesOnTarget;

	// Put methods for controlling this subsystem
	// here. Call these from Commands.

	public void initDefaultCommand() {

		// BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DEFAULT_COMMAND

		// END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DEFAULT_COMMAND

		// Set the default command for a subsystem here.
		// setDefaultCommand(new MySpecialCommand());

		setDefaultCommand(new ManualDrive());
	}

	// Sends power to the speed controllers
	/*
	 * IMPORTANT REMINDER
	 * 
	 * Power needs to be negative to one of the drives. A task in the future
	 * will be to find out which side needs to set as negative in order for the
	 * robot TO ACTUALLY FUNCTION, which could be a bit important.
	 */
	public void rawTankDrive(double leftPower, double rightPower) {
		double voltage;
		double coeff;
		
		if (CommandBase.driverStation.getInstance().isAutonomous()) {
			coeff = 1;
		} else {
			voltage = DriverStation.getInstance().getBatteryVoltage();
			coeff = MathHelper.normalize(voltage, MIN_VOLTAGE, MAX_VOLTAGE);
		}
		
		RobotMap.victorLeft1.set(-leftPower * coeff);
		RobotMap.victorLeft2.set(-leftPower * coeff);
		RobotMap.victorRight1.set(rightPower * coeff);
		RobotMap.victorRight2.set(rightPower * coeff); 
	} 

	/**
	 * Immediately stop all drive motors using all motor controllers.
	 */
	public void rawStop() {
		RobotMap.victorLeft1.disable();
		RobotMap.victorLeft2.disable();
		RobotMap.victorRight1.disable();
		RobotMap.victorRight2.disable();
	}

	// Returns the Gyro
	public Gyro getGyro() {
		return RobotMap.driveGyro;
	}

	public double getDesiredHeading() {
		return desiredHeading;
	}

	public void setDesiredHeading(double heading) {
		desiredHeading = heading;
	}
	
    public void setDesiredHeadingFromGyro() {
    	setDesiredHeading(RobotMap.driveGyro.getAngle());  
    }
    
	public void pidWrite(double output) {
		pidOutput = output;
	}
	
	public double getPidOutput() {
		return pidOutput;
	}
	
	public double drivePIDInit(double p, double i, double d, double maxOutput) {
		drivePID.reset();
		drivePID.setPID(p,i,d);
		drivePID.setOutputRange(-Math.abs(maxOutput), Math.abs(maxOutput));
		drivePID.enable();
		System.out.println("Drive P:" + p + " I:" + i + " D:" + d);
		return RobotMap.driveGyro.getAngle();
	}
	
	public double driveOnHeadingInit(double maxOutput){
		return drivePIDInit(
			CommandBase.preferences.getDouble(PreferenceKeys.Drive_Straight_On_Heading_P, DRIVE_STRAIGHT_ON_HEADING_P),
			CommandBase.preferences.getDouble(PreferenceKeys.Drive_Straight_On_Heading_I, DRIVE_STRAIGHT_ON_HEADING_I), 
			CommandBase.preferences.getDouble(PreferenceKeys.Drive_Straight_On_Heading_D, DRIVE_STRAIGHT_ON_HEADING_D),
			maxOutput); 	
	}

	public void driveOnHeading(double heading, double power) {
		drivePID.setSetpoint(heading);

		double error = heading - RobotMap.driveGyro.getAngle();
		double outputRange = MathHelper.clamp(PID_MIN_OUTPUT
				+ (Math.abs(error) / 15.0) * (PID_MAX_OUTPUT - PID_MIN_OUTPUT),
				0, PID_MAX_OUTPUT);
		drivePID.setOutputRange(-outputRange, outputRange);

		double currentPIDOutput = MathHelper.clamp(pidOutput, -outputRange,
				outputRange);
		SmartDashboard.putNumber("Current PID OUTPUT", currentPIDOutput);	
		double leftPower = power;
		double rightPower = power;

		if (currentPIDOutput > 0) {
			rightPower -= currentPIDOutput;
		} else {
			leftPower += currentPIDOutput;
		}

		rawTankDrive(leftPower, rightPower);
	}

	public void driveOnHeadingEnd() { 
		rawStop();
		drivePID.reset();
		pidOutput = 0;
	}
	
	public double turnToHeadingInit(double toleranceInDegrees, double maxOutput){
		cyclesOnTarget = getRequiredCyclesOnTarget();
		drivePID.setAbsoluteTolerance(toleranceInDegrees);
		drivePIDInit(
			CommandBase.preferences.getDouble(PreferenceKeys.TURN_P, TURN_TO_HEADING_P),
			CommandBase.preferences.getDouble(PreferenceKeys.TURN_I, TURN_TO_HEADING_I), 
			CommandBase.preferences.getDouble(PreferenceKeys.TURN_D, TURN_TO_HEADING_D),
			maxOutput);
		return desiredHeading;          
	}
	
	public void turnToHeading(double finalHeading, double power){
		drivePID.setSetpoint(finalHeading);
		double currentPower = MathHelper.clamp(pidOutput, -power, power);
		rawTankDrive (currentPower, -currentPower);
	}
	
	public boolean isTurnToHeadingDone(){
		boolean onTarget = drivePID.onTarget();
		if (onTarget) {
			cyclesOnTarget++;
		}
		else {
			cyclesOnTarget = 0;
		}
		return cyclesOnTarget >= getRequiredCyclesOnTarget();
	}
	
	public void turnToHeadingEnd(double newHeading){
		setDesiredHeading(newHeading); 
		drivePID.reset();
		pidOutput = 0;
	}
	
	public PIDController getPIDController(){
		return drivePID;
	}
	
	public int getRequiredCyclesOnTarget(){
		return REQUIRED_CYCLES_ON_TARGET;
	}
}
