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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;


public class BlockActivityHandler {
	private Context current_context;
	private String lockActivityName;
	private String lastRunningPackage;
	private String lockPackage = "com.example.proauth";
	private ActivityManager activity_manager;
	private Handler handler;
	private Hashtable<String, Runnable> timeoutTable= new Hashtable<String, Runnable>();
	private BroadcastReceiver passed;
	private BroadcastReceiver not_passed;
	private BroadcastReceiver screen_off;
	private BroadcastReceiver screen_on;
	public static String TAG = "BlockActivityHandler";
	public int INTERVAL = 1 * 1000 * 5;		// 5 seconds

	private SharedPreferences sp;
	
	public BlockActivityHandler(Context context) {
		current_context = context;
		lockActivityName = ".LockScreenActivity";
		handler = new Handler();
		activity_manager = (ActivityManager)current_context.getSystemService(Context.ACTIVITY_SERVICE);
		lastRunningPackage = getRunningPackage();
		
		Log.d("JOAO", "About to register the receivers");
		sp = PreferenceManager.getDefaultSharedPreferences(current_context);
		INTERVAL = Integer.parseInt(sp.getString("timeout_duration", "5000"));
			
		
		context.registerReceiver(passed = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String packagename = intent.getStringExtra(LockScreenActivity.PACKAGE_NAME);
				lastRunningPackage = packagename;
				
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(current_context);

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
					
					if(timeoutTable.containsKey(MainActivity.PHONE_SECURITY_STATE)){
						// this is wrong.
						Log.wtf(TAG, "already counting down!!!");
					} else {
						Runnable runnable = new systemTimeoutCallback();
						timeoutTable.put(MainActivity.PHONE_SECURITY_STATE, runnable);
						handler.removeCallbacks(runnable);
						handler.postDelayed(runnable, INTERVAL);
					}
				}
			}, new IntentFilter(Intent.ACTION_SCREEN_OFF));
			

			context.registerReceiver(screen_on = new BroadcastReceiver(){
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.d(TAG, "Screen On");
					
					if(timeoutTable.containsKey(MainActivity.PHONE_SECURITY_STATE)){
						Log.d(TAG, "Stop counting down");
						handler.removeCallbacks(timeoutTable.get(MainActivity.PHONE_SECURITY_STATE));
						timeoutTable.remove(MainActivity.PHONE_SECURITY_STATE);		
					}
				}
			}, new IntentFilter(Intent.ACTION_SCREEN_ON));
		}

		
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
	
	private void dropPhoneSecurityLevel(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(current_context);
	    boolean high = sp.getBoolean("security_level_3", false);
	    boolean medium = sp.getBoolean("security_level_2", false);
	    boolean low = sp.getBoolean("security_level_1", false);
	    
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

		//Log.d(TAG, "Next level " + lower_level);
		Editor e = sp.edit();
		e.putString(MainActivity.PHONE_SECURITY_STATE, lower_level);
		e.commit();
		
		Log.d(TAG, "DROPPED! Current phone state:" + sp.getString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString()));
	}
	
	private class systemTimeoutCallback implements Runnable{
		public systemTimeoutCallback(){
		}
		@Override
		public void run() {
			// drop down to the next level
			dropPhoneSecurityLevel();
			Log.d(TAG, "Dropping!");
			timeoutTable.put(MainActivity.PHONE_SECURITY_STATE, this);
			handler.postDelayed(this, INTERVAL);
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
				Log.d("JOAO","A");
				if (activityName.equals(lockActivityName)){
					return;
				}
				blockActivity(packageName, activityName);
			}
			if (packageName.equals(lastRunningPackage)) return;
			// Here we are going to want to go through the list of blocked apps
			// And check if the packageName is in that list
//			Log.d("JOAO", "packageNames variable" + packageName);
			
//			if (packageName.equals("com.example.proauth")) {
//				lastRunningPackage = packageName;
//				return;
//			} /*else if (packageName.equals(lockPackage)){
				// Do nothing.
//			} */ else {
			Log.d("JOAO", "B");
				if (timeoutTable.containsKey(packageName)){
					Log.d("JOAO", "Allowed package " + packageName + " because the timeout had not expired yet");
					return;
					
				}
				blockActivity(packageName, activityName);
				return;
//				lastRunningPackage = packageName;
//				Log.d("JOAO", "Last running package: " + lastRunningPackage);
//			}
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
		
		if (sp.getBoolean("trigger_1", false)){
			current_context.unregisterReceiver(screen_on);
			current_context.unregisterReceiver(screen_off);
		}
	}
}