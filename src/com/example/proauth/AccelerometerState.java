package com.example.proauth;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;



public class AccelerometerState implements SensorEventListener {
	private static final double THRESH = 1;
	private double recentx, recenty, recentz, oldx, oldy, oldz;
	private int numEvents;
	private boolean isMoving = false;
	private BlockActivityHandler bahandler;
	public static final String TURN_ON_OFF_ACCEL = "com.example.proauth.TURN_ON_OFF_ACCEL";
	public static final String ON_OR_OFF_ACCEL = "com.example.proauth.ON_OFF_ACCEL";
	public static final String TAG = "AccelerometerState";
	
	public AccelerometerState(BlockActivityHandler bah, Context context) {
		SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		bahandler = bah;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			if (numEvents < 5) {
				recentx += event.values[0];
				recenty += event.values[1];
				recentz += event.values[2];
				numEvents++;
			} else {
				numEvents = 1;
				
				if (Math.abs(recentx - oldx) > THRESH || Math.abs(recenty - oldy) > THRESH || Math.abs(recentz - oldz) > THRESH) {
					// phone is moving
					if (!isMoving) {
						bahandler.onAccelerometerStateChange(true);
					}
					isMoving = true;
				} else {
					// phone is still
					if (isMoving) {
						bahandler.onAccelerometerStateChange(false);
					}
					isMoving = false;
				}
				
				oldx = recentx;
				oldy = recenty;
				oldz = recentz;
				
				recentx = event.values[0];
				recenty = event.values[1];
				recentz = event.values[2];
			}
		}
	}
	
	public boolean getState() {
		return isMoving;
	}
}
