package com.example.proauth;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


public class BlockActivityHandler {
	private Context current_context;
	private String lockActivityName;
	private String lastRunningPackage;
	private String lockPackage = "com.example.proauth";
	private ActivityManager activity_manager;
	

	public BlockActivityHandler(Context context) {
		current_context = context;
		lockActivityName = ".LockScreenActivity";
		activity_manager = (ActivityManager)current_context.getSystemService(Context.ACTIVITY_SERVICE);
		lastRunningPackage = getRunningPackage();
		
		context.registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String packagename = intent.getStringExtra(LockScreenActivity.PACKAGE_NAME);
				lastRunningPackage = packagename;
				lockPackage = packagename;
			}
		}, new IntentFilter(LockScreenActivity.PASSED));
		

		context.registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String packagename = intent.getStringExtra(LockScreenActivity.PACKAGE_NAME);
				lockPackage = "com.example.proauth";
			}
		}, new IntentFilter(LockScreenActivity.NOT_PASSED));
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
			if (packageName.equals(current_context.getPackageName())) {
				if (activityName.equals(lockActivityName))
					return;
				blockActivity(packageName, activityName);
			}
//			if (packageName.equals(lastRunningPackage)) return;
			// Here we are going to want to go through the list of blocked apps
			// And check if the packageName is in that list
//			Log.d("JOAO", "packageNames variable" + packageName);
			if (packageName.equals("com.example.proauth")) {
				lastRunningPackage = packageName;
				return;
			} else if (packageName.equals(lockPackage)){
				// Do nothing.
			} else {
//				if(packageName.equals("com.android.browser")){
//					Log.d("JOAO","B");
//					blockActivity(packageName, activityName);
//					return;
//				}
				blockActivity(packageName, activityName);
				return;
//				lastRunningPackage = packageName;
//				Log.d("JOAO", "Last running package: " + lastRunningPackage);
			}
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
}