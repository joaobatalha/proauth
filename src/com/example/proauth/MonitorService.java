package com.example.proauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class MonitorService extends Service {
    private static final Class[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    private static final Class[] mStopForegroundSignature = new Class[] {
        boolean.class};
    
    private static final String TAG = "MonitorService";
    
    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private Context mContext;
    private final IBinder mBinder = new LocalBinder();
    
    @Override
    public void onCreate() {
    	
    	/*Need to make it a foreground service so that it does not get killed
    	 by the activity manager */
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

		//When the service first starts the phone state should be public
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		Editor e = sp.edit();
		e.putBoolean("monitor_on", true);
		e.putString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString());
		e.commit();
        
        if(init == 0){
        	init = 1;
        	get_log_command = getResources().getString(R.string.get_log_command);
    		clear_log_command = getResources().getString(R.string.clear_log_command);
    		//Regex used to get app name, and activity name from the logs
    		String regex_pattern = getResources().getString(R.string.activity_name_pattern);
    		ActivityPattern = Pattern.compile(regex_pattern,Pattern.CASE_INSENSITIVE);
    		Log.i(TAG, "Initialized monitor service with regex pattern: " + regex_pattern);        
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
                Log.w("ProAuth", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ProAuth", "Unable to invoke startForeground", e);
            }
            return;
        }
        
        // Fall back on the old API.
        setForeground(true);
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
                Log.w("ProAuth", "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ProAuth", "Unable to invoke stopForeground", e);
            }
            return;
        }
        
        mNM.cancel(id);
        //setForeground(false);
    }

    
   
   public class LocalBinder extends Binder {
        MonitorService getService() {
            return MonitorService.this;
        }
    }

	
    @Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
    
	//This method is called when the user presses the System Timeout box
	public void registerHandlerScreenListeners(){
		if(blocking_handler != null){
			blocking_handler.registerScreenListeners();
		}
	}
	
	
	private static Thread log_monitor_thread;
	private static String get_log_command;
	private static String clear_log_command;
	private static Pattern ActivityPattern;
	private BlockActivityHandler blocking_handler;
	private static int init;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		if(log_monitor_thread != null){
			log_monitor_thread.interrupt();
		}
		
		CharSequence text = getText(R.string.service_running);
		Notification note = new Notification(0,text,System.currentTimeMillis());
		startForegroundCompat(R.string.service_running, note);
		
		//Initialize the thread that actually monitors the log
		log_monitor_thread = new LogMonitoringThread(new BlockActivityHandler(this));
		log_monitor_thread.start();
		return Service.START_STICKY;
	}
	
	
	/* Thread executes the logcat ActivityManager:I *:S which reads through the logs
	 * of the system and uses a regex to find intents starting applications, and extract
	 * the name of the app that the activity manager is trying to start. */
	private class LogMonitoringThread extends Thread{

    	BufferedReader br;
    	SharedPreferences mPrefs;
    	SharedPreferences mAppPrefs;
    	public LogMonitoringThread(BlockActivityHandler handler){
    		blocking_handler = handler;
    		mPrefs = PreferenceManager
    				.getDefaultSharedPreferences(mContext);
    		mAppPrefs = getSharedPreferences(ManageAppsActivity.TAG, MODE_PRIVATE);

    	}
    	
    	private boolean requiresBlocking(String packageName){
    		SecurityLevel phoneSecurityLevel = SecurityLevel.valueOf(mPrefs.getString(
    				MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString()));
    		SecurityLevel appSecurityLevel = SecurityLevel.valueOf(mAppPrefs.getString(
    				packageName, SecurityLevel.PUBLIC.toString()));
    		
			boolean system_timeout = mPrefs.getBoolean("trigger_1", false);
			
			if(system_timeout){
				Log.i(TAG, "App security level: " + appSecurityLevel + ", Phone Security Level: " + phoneSecurityLevel);
				if (appSecurityLevel.value > phoneSecurityLevel.value){
					return true;
				}
				return false;
			}else{//system timeout is not turned on
				if((appSecurityLevel.toString()).equals(SecurityLevel.PRIVATE.toString())){
					return true;
				}
				else{
					return false;
				}
			}		
			
    	}
    	
    	@Override
    	public void interrupt(){
    		if(blocking_handler != null){
    			blocking_handler.onDestroy();
    		}
    		super.interrupt();  		
    	}
    	
		@Override
		public void run() {
			Log.i(TAG, "Starting log monitoring thread...");	
			try {
	    		Process process;
	    		process = Runtime.getRuntime().exec(clear_log_command);
				process = Runtime.getRuntime().exec(get_log_command);
				br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;

				while(( (line=br.readLine()) != null) && !this.isInterrupted()){					
					if (line.contains("cat=[" + Intent.CATEGORY_HOME + "]")){	
						continue;
					} 
					
					Matcher m = ActivityPattern.matcher(line);
					
					if (!m.find()){
						continue;
					}
					
					if (m.groupCount()<2){
						Log.i(TAG, "Error while matching a line of the log.");
						continue;
					}
					
						
						Log.i(TAG, "Detected app launch => app_name: " + m.group(1) + "  / activity_name: " + m.group(2));
						if (!requiresBlocking(m.group(1))){
							continue;
						}
						if(blocking_handler != null){
							blocking_handler.onActivityStarting(m.group(1), m.group(2));
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
		log_monitor_thread = null;
		blocking_handler = null;
		
		//Service was killed, uncheck the box in preferences
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		Editor e = sp.edit();
		e.putBoolean("monitor_on", false);
		e.commit();
		
		stopForegroundCompat(R.string.service_running);
	}
	
}
