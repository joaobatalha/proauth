package com.example.proauth;

import java.util.Hashtable;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;


public class BlockActivityHandler {
	private Context current_context;
	private String lockActivityName;
	private String lastRunningPackage;
	private String lastRunningActivity;
	private String lockPackage = "com.example.proauth";
	private ActivityManager activity_manager;
	private Handler handler;
	private Hashtable<String, Runnable> timeoutTable= new Hashtable<String, Runnable>();
	private BroadcastReceiver passed;
	private BroadcastReceiver not_passed;
	private BroadcastReceiver screen_off;
	private BroadcastReceiver screen_on;
	private BroadcastReceiver turn_on_off_location, turn_on_off_accel;
	public static String TAG = "BlockActivityHandler";
	public int INTERVAL = 1 * 1000 * 5;		// 5 seconds
	private AccelerometerState accelerometerState;
	private TrustedLocationState trustedLocationState;
	private boolean isScreenOn, isAccelMoving, isActive, isSafeLoc, isAccelerometerOn;

	private SharedPreferences sp;
	
	public BlockActivityHandler(Context context) {
		accelerometerState = new AccelerometerState(this, context);
		trustedLocationState = new TrustedLocationState(this, context);
		isSafeLoc = false;
		isActive = true;
		isScreenOn = true;
		current_context = context;
		lockActivityName = ".LockScreenActivity";
		handler = new Handler();
		activity_manager = (ActivityManager)current_context.getSystemService(Context.ACTIVITY_SERVICE);
		lastRunningPackage = getRunningPackage();
		
		Log.d("JOAO", "About to register the receivers");
		sp = PreferenceManager.getDefaultSharedPreferences(current_context);
		INTERVAL = Integer.parseInt(sp.getString("timeout_duration", "5000"));
		
		if (sp.getBoolean("trigger_3", false)) {
			trustedLocationState.turnOn();
		}
		
		isAccelerometerOn = sp.getBoolean("trigger_2", false);
			
		
		context.registerReceiver(passed = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String packagename = intent.getStringExtra(LockScreenActivity.PACKAGE_NAME);
				lastRunningPackage = packagename;
				lastRunningActivity = intent.getStringExtra(LockScreenActivity.ACTIVITY_NAME);
				
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(current_context);
				
				Log.d("JOAO","A");

				if (sp.getBoolean("trigger_1", false)){
					// set the phone state to private
					Editor e = sp.edit();
					e.putString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PRIVATE.toString());
					e.commit();
					Log.d(TAG, "Current phone state:" + sp.getString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString()));
				}
				if(packagename.equals("com.example.proauth")){
					Log.d("JOAO", "Proauth was the app that passed, do not add to timeout table");
					return;
				}
				lockPackage = packagename;
				
				// Don't do timeouts for each app if disabled in preferences
				if (sp.getBoolean("trigger_0", false)){
					Log.d("JOAO","B");
					Log.d(TAG, "App timeout enabled");
					Log.d("JOAO","About to add " + packagename + "to timeoutTable");
					//we should check if the timeout options is set
					if(timeoutTable.contains(packagename)){
						//extend the time
						Log.d("JOAO", "Extended time for package " + packagename);
						handler.removeCallbacks(timeoutTable.get(packagename));
					}
					Runnable runnable = new timeoutCallback(packagename);
					timeoutTable.put(packagename, runnable);
					//Timeout is set to 1 minute
					handler.postDelayed(runnable, 1 * 1000 * 60);
				}
			}
		}, new IntentFilter(LockScreenActivity.PASSED));
		
		context.registerReceiver(not_passed = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String packagename = intent.getStringExtra(LockScreenActivity.PACKAGE_NAME);
				lockPackage = "com.example.proauth";
			}
		}, new IntentFilter(LockScreenActivity.NOT_PASSED));
		
		context.registerReceiver(turn_on_off_location = new BroadcastReceiver(){

			@Override
			public void onReceive(Context arg0, Intent intent) {
				Log.d(TAG, "Got the message to turn on/off the TrustedLocationState");
				boolean shouldTurnOn = intent.getBooleanExtra(TrustedLocationState.ON_OR_OFF, false);
				if (shouldTurnOn) {
					trustedLocationState.turnOn();
				} else {
					trustedLocationState.turnOff();
				}
			}
			
		}, new IntentFilter(TrustedLocationState.TURN_ON_OFF_LOC));
		
		context.registerReceiver(turn_on_off_accel = new BroadcastReceiver(){

			@Override
			public void onReceive(Context arg0, Intent intent) {
				Log.d(TAG, "Got the message to turn on/off the AccelerometerState");
				boolean shouldTurnOn = intent.getBooleanExtra(AccelerometerState.ON_OR_OFF_ACCEL, false);
				if (shouldTurnOn) {
					isAccelerometerOn = true;
				} else {
					isAccelerometerOn = false;
				}
			}
			
		}, new IntentFilter(AccelerometerState.TURN_ON_OFF_ACCEL));

		 //Don't do timeout dropping if disabled
		if (sp.getBoolean("trigger_1", false)){     
			context.registerReceiver(screen_off = new BroadcastReceiver(){
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.d(TAG, "Screen Off");
					//Log.d(TAG, "Timeout table size: " + timeoutTable.size());
					
					/*
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(current_context);
					Editor e = sp.edit();
					e.putString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PRIVATE.toString());
					e.commit();
					Log.d(TAG, "Current phone state:" + sp.getString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString()));
					*/
					
					isScreenOn = false;
					handleScreenAndAccel(isScreenOn, isAccelMoving);
				}
			}, new IntentFilter(Intent.ACTION_SCREEN_OFF));
			

			context.registerReceiver(screen_on = new BroadcastReceiver(){
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.d(TAG, "Screen On");
					
					isScreenOn = true;
					handleScreenAndAccel(isScreenOn, isAccelMoving);
				}
			}, new IntentFilter(Intent.ACTION_SCREEN_ON));
		}

		
	}
	
	
	private void handleScreenAndAccel(boolean isScreenOn, boolean isAccelMoving) {
		if (isScreenOn && (!isAccelerometerOn || isAccelMoving)) {
			if (!isActive) {
				isActive = true;
				if (timeoutTable.containsKey(MainActivity.PHONE_SECURITY_STATE)) {
					Log.d(TAG, "Stop counting down");
					handler.removeCallbacks(timeoutTable.get(MainActivity.PHONE_SECURITY_STATE));
					timeoutTable.remove(MainActivity.PHONE_SECURITY_STATE);
				}
			}
		} else if (!isScreenOn && (!isAccelerometerOn || !isAccelMoving)) {
			if (isActive) {
				isActive = false;
				if (timeoutTable.containsKey(MainActivity.PHONE_SECURITY_STATE)) {
					// this is wrong.
					Log.wtf(TAG, "already counting down!!!");
				} else {
					Log.d(TAG, "start counting down");
					Runnable runnable = new systemTimeoutCallback();
					timeoutTable.put(MainActivity.PHONE_SECURITY_STATE, runnable);
					handler.removeCallbacks(runnable);
					handler.postDelayed(runnable, INTERVAL);
				}
			}
		}
	}
	
	
	public void onAccelerometerStateChange(boolean isCurrStateMoving) {
		Log.d(TAG, "Accelerometer state is: " + (isCurrStateMoving ? "moving" : "not moving"));
		isAccelMoving = isCurrStateMoving;
		handleScreenAndAccel(isScreenOn, isAccelMoving);
	}
	
	public void onSafenessOfLocationChange(boolean isSafe) {
		Log.d(TAG, "Location safeness state is: " + (isSafe ? "safe" : "not safe"));
		isSafeLoc = isSafe;
	}
	
	private class timeoutCallback implements Runnable{
		private String packageName;
		public timeoutCallback(String name){
			packageName = name;
		}
		@Override
		public void run() {
			timeoutTable.remove(packageName);			
		}	
	}
	
	private boolean dropPhoneSecurityLevel(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(current_context);
	    //boolean high = sp.getBoolean("trigger_2", false);
	    boolean high = false;

	    boolean medium = sp.getBoolean("trigger_1", false);
	    boolean low = sp.getBoolean("trigger_3", false);
	    
		String prev_level = sp.getString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString());
		//Log.d(TAG, "Current level " + prev_level);
		String lower_level = SecurityLevel.PUBLIC.toString();
		if (prev_level.equals(SecurityLevel.PRIVATE.toString())){
			if (high){
				lower_level = SecurityLevel.HIGH.toString();
			} else if (medium){
				lower_level = SecurityLevel.MEDIUM.toString();
			} else if (low) {
				lower_level = SecurityLevel.LOW.toString();
			}
		} else if (prev_level.equals(SecurityLevel.HIGH.toString())){
			if (medium){
				lower_level = SecurityLevel.MEDIUM.toString();
			} else if (low) {
				lower_level = SecurityLevel.LOW.toString();
			}
		} else if (prev_level.equals(SecurityLevel.MEDIUM.toString())){
			if (low) {
				lower_level = SecurityLevel.LOW.toString();
			}
		}
		
		if (isSafeLoc && (prev_level.equals(SecurityLevel.PRIVATE.toString()) || prev_level.equals(SecurityLevel.HIGH.toString()) ||
				prev_level.equals(SecurityLevel.MEDIUM.toString())) && (lower_level.equals(SecurityLevel.LOW.toString()) ||
						lower_level.equals(SecurityLevel.PUBLIC.toString()))) {
			Log.d(TAG, "DIDN'T DROP! We're in a safe location! Current phone state:" + sp.getString(MainActivity.PHONE_SECURITY_STATE,
					SecurityLevel.PUBLIC.toString()));
			return true;
		}
		
		//Log.d(TAG, "Next level " + lower_level);
		Editor e = sp.edit();
		e.putString(MainActivity.PHONE_SECURITY_STATE, lower_level);
		e.commit();
		
		Log.d(TAG, "DROPPED! Current phone state:" + sp.getString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString()));
		if (lower_level.equals(SecurityLevel.PUBLIC)){
			return false;
		}
		return true;
	}
	
	private class systemTimeoutCallback implements Runnable{
		public systemTimeoutCallback(){
		}
		@Override
		public void run() {
			// drop down to the next level
			boolean can_still_drop = dropPhoneSecurityLevel();
			Log.d(TAG, "Dropping!");
			timeoutTable.put(MainActivity.PHONE_SECURITY_STATE, this);
			if (can_still_drop){
				handler.postDelayed(this, INTERVAL);
			}
		}	
	}
	
	private String getRunningPackage(){
		List<RunningTaskInfo> infos = activity_manager.getRunningTasks(1);
		if (infos.size()<1) return null; 
		RunningTaskInfo info = infos.get(0);
		return info.topActivity.getPackageName();
	}

	public void onActivityStarting(String packageName, String activityName) {
		synchronized (this) {
			// Here we do not want to block the password activity
			// But we do want to block the Preferences activity
			if (packageName.equals("com.example.proauth")) {
				if (activityName.equals(lockActivityName)){
					return;
				}
				blockActivity(packageName, activityName);
			}
			
			//This fixes the bug where an App would remain unlock after it was locked
			//Also does not ask you to unlock twice when for instance using the search app
			if (packageName.equals(lastRunningPackage) && !activityName.equals(lastRunningActivity)) {
					return;
				}

				boolean app_timeout = sp.getBoolean("trigger_0", false);
				if (timeoutTable.containsKey(packageName) && app_timeout){
					Log.d("JOAO", "Allowed package " + packageName + " because the timeout had not expired yet");
					return;
					
				}
				blockActivity(packageName, activityName);
				return;
		}
	}

	private void blockActivity(String packageName, String activityName) {
		Log.i("Detector", "Blocking: " + packageName);

		Intent locking_intent = new Intent(current_context, LockScreenActivity.class);
		locking_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		locking_intent.putExtra(LockScreenActivity.BlockedPackageName, packageName);
		locking_intent.putExtra(LockScreenActivity.BlockedActivityName, activityName);

		current_context.startActivity(locking_intent);
	}
	

	public void onDestroy(){
		current_context.unregisterReceiver(passed);
		current_context.unregisterReceiver(not_passed);
		current_context.unregisterReceiver(turn_on_off_location);
		current_context.unregisterReceiver(turn_on_off_accel);
		
		if (screen_on != null){
			current_context.unregisterReceiver(screen_on);
			current_context.unregisterReceiver(screen_off);
		}
	}
}