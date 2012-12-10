package com.example.proauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class MonitorService extends Service {
    private static final Class[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    private static final Class[] mStopForegroundSignature = new Class[] {
        boolean.class};
    
    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private Context mContext;
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        
        mContext = this;
        
        if(init == 0){
        	init = 1;
        	get_log_command = getResources().getString(R.string.get_log_command);
    		clear_log_command = getResources().getString(R.string.clear_log_command);
    		//String regex_pattern = getResources().getString(R.string.activity_name_pattern);
    		String regex_pattern = getResources().getString(R.string.intent_name_pattern);
    		ActivityPattern = Pattern.compile(regex_pattern,Pattern.CASE_INSENSITIVE);
    		Log.d("Detector Service: ", "Initialized detector with pattern: " + regex_pattern);        
        }	
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke startForeground", e);
            }
            return;
        }
        
        // Fall back on the old API.
        //setForeground(true);
        mNM.notify(id, notification);
    }
    
    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke stopForeground", e);
            }
            return;
        }
        
        mNM.cancel(id);
        //setForeground(false);
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Thread log_monitor_thread;
	private static String get_log_command;
	private static String clear_log_command;
	private static Pattern ActivityPattern;
	private static int init;
	
	//Code to execute when service is started 
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		if(log_monitor_thread != null){
			log_monitor_thread.interrupt();
		}
		
		CharSequence text = getText(R.string.service_running);
		Notification note = new Notification(0,text,System.currentTimeMillis());
		startForegroundCompat(R.string.service_running, note);
		log_monitor_thread = new LogMonitoringThread(new BlockActivityHandler(this));
		log_monitor_thread.start();
		return Service.START_STICKY;
	}
	
	private class LogMonitoringThread extends Thread{

    	BufferedReader br;
    	BlockActivityHandler blocking_handler;
    	SharedPreferences mPrefs;
    	public LogMonitoringThread(BlockActivityHandler handler){
    		blocking_handler = handler;

    		mPrefs = PreferenceManager
    				.getDefaultSharedPreferences(mContext);

    	}
    	private boolean requiresBlocking(String packageName){

    		mPrefs = getSharedPreferences(ManageAppsActivity.TAG, MODE_PRIVATE);
    		String appSecurityLevel = mPrefs.getString(
    				packageName, "");
    		if (appSecurityLevel.equals(SecurityLevel.PUBLIC.toString())){
    			return false;
    		}
    		return true;
    	}
    	
		@Override
		public void run() {
			Log.d("JOAO", "Starting Monitoring Thread");	
			try {
	    		Process process;
	    		process = Runtime.getRuntime().exec(clear_log_command);
				process = Runtime.getRuntime().exec(get_log_command);
				br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;

				while(( (line=br.readLine()) != null) && !this.isInterrupted()){
					if (!line.contains("START")){
						continue;
					}
					
					Log.d("JOYC******", "Line read from the log: " + line);	
					
					try {
						Log.d("JOYC", "Sleeping for 500 ms....");	
						this.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
					List<RecentTaskInfo> l = am.getRecentTasks(2, ActivityManager.RECENT_WITH_EXCLUDED);
					String intent_string = l.get(0).baseIntent.toString();
					
					Log.d("JOYC", intent_string);
					//if (line.contains("cat=[" + Intent.CATEGORY_HOME + "]")){
					if (intent_string.contains("cat=[" + Intent.CATEGORY_HOME + "]")){
						Log.d("JOAO", "Cat match");	
						continue;
					} 
					
					//Matcher m = ActivityPattern.matcher(line);
					Matcher m = ActivityPattern.matcher(intent_string);
					
					if (!m.find()){
						Log.d("JOAO", "No match");	
						continue;
					}
					
					if (m.groupCount()<2){
						Log.d("Detector Service: ", "Error while matching a line of the log.");
						continue;
					}
					//Log.d("JOAO", "Line matched in the log: " + line);	
					
					if (!m.group(1).equals("com.example.proauth")){
						Log.i("JOAO", "Found activity launching: " + m.group(1) + "  /   " + m.group(2));
						if (!requiresBlocking(m.group(1))){
							continue;
						}
						if(blocking_handler != null){
							blocking_handler.onActivityStarting(m.group(1), m.group(2));
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }
	
	@Override
	public void onDestroy(){
		log_monitor_thread.interrupt();
		stopForegroundCompat(R.string.service_running);
	}
	
}
