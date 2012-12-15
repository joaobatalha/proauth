package com.example.proauth;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


public class SetPreferencesActivity extends PreferenceActivity {

	CheckBoxPreference monitor;
	CheckBoxPreference app_timeout;
	CheckBoxPreference system_timeout;
	CheckBoxPreference gps_setting;
	CheckBoxPreference accelerometer_setting;
	Preference gps_set_location;
	Editor e;
	SharedPreferences sp;
	public static String TAG = "SetPreferencesActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		doBindService();//Binds to the monitor service
        
        addPreferencesFromResource(R.xml.preferences);

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		e = sp.edit();
        PreferenceManager preferenceManager = getPreferenceManager();
        
        monitor = (CheckBoxPreference) preferenceManager.findPreference("monitor_on"); 
        
        monitor.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if(monitor.isChecked()){
					Log.i(TAG, "Starting monitoring service");
					Intent intent = new Intent();
			    	intent.setClass(SetPreferencesActivity.this, MonitorService.class);
			    	startService(intent);
			    	system_timeout.setEnabled(true);
				}
				else{
					Log.i(TAG, "Stopping monitoring service");
					Intent intent = new Intent();
			    	intent.setClass(SetPreferencesActivity.this, MonitorService.class);
			    	stopService(intent);
				}
				return true;
			}
        	
        });
        

        app_timeout = (CheckBoxPreference) preferenceManager.findPreference("trigger_0"); 
        system_timeout = (CheckBoxPreference) preferenceManager.findPreference("trigger_1");
        gps_setting = (CheckBoxPreference) preferenceManager.findPreference("trigger_3");
        gps_set_location = preferenceManager.findPreference("gps_set_location");
        accelerometer_setting = (CheckBoxPreference) preferenceManager.findPreference("trigger_2");
        
        
        app_timeout.setEnabled(app_timeout.isChecked());
        if(!app_timeout.isEnabled()){
        	system_timeout.setEnabled(true);
        }
        
        if(!monitor.isChecked()){
        	app_timeout.setChecked(false);
        	system_timeout.setChecked(false);
        }
        
        gps_set_location.setEnabled(gps_setting.isChecked());
        
        gps_setting.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference arg0) {
				gps_set_location.setEnabled(gps_setting.isChecked());
				Intent intent = new Intent(TrustedLocationState.TURN_ON_OFF_LOC);
				intent.putExtra(TrustedLocationState.ON_OR_OFF, gps_setting.isChecked());
		    	sendBroadcast(intent);
				return true;
			}
        	
        });
        
        accelerometer_setting.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Log.d(TAG, "clicked accelerometer button!");
				Intent intent = new Intent(AccelerometerState.TURN_ON_OFF_ACCEL);
				intent.putExtra(AccelerometerState.ON_OR_OFF_ACCEL, accelerometer_setting.isChecked());
		    	sendBroadcast(intent);
				return true;
			}
        	
        });
        
        gps_set_location.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (SystemClock.elapsedRealtime() - 
						sp.getLong(TrustedLocationState.RECENT_GPS_UPDATE_TIME,
								-TrustedLocationState.UPDATE_THRESHOLD) >= TrustedLocationState.UPDATE_THRESHOLD) {
					Toast toast = Toast.makeText(getApplicationContext(), "Location hasn't been updated recently, so no change was made",
							Toast.LENGTH_SHORT);
					toast.show();
				} else {
					e.putString(TrustedLocationState.TRUSTED_LATITUDE, sp.getString(TrustedLocationState.CURRENT_LATITUDE, null));
					Toast toast = Toast.makeText(getApplicationContext(), "Safe location is being changed", Toast.LENGTH_SHORT);
					toast.show();
				}
				return true;
			}
        	
        });
        
        app_timeout.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if(app_timeout.isChecked()){
					e.putBoolean("trigger_1", false);
					e.commit();
					system_timeout.setChecked(false);
					system_timeout.setEnabled(false);
					Log.i(TAG, "Activated the app timeout feature.");
				} else {
					system_timeout.setEnabled(true);
					Log.i(TAG, "Deactivated the app timeout feature.");
					
				}
				return true;
			}
        	
        });
        
        system_timeout.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if(system_timeout.isChecked()){
					Log.i(TAG, "Activated the system timeout feature.");
					e.putBoolean("trigger_0", false);
					e.commit();
					app_timeout.setChecked(false);
					app_timeout.setEnabled(false);
					
					if(mIsBound){
						monitorService.registerHandlerScreenListeners();
					}
					else{
						Log.d(TAG, "Did not bind to monitor service!");
					}
					
				} else{
					app_timeout.setEnabled(true);
					//Disabled system timeout, set phone state to public 
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					Editor e = sp.edit();
					e.putString(MainActivity.PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString());
					e.commit();
					Log.i(TAG, "Disabled the system timeout feature.");
				}
				return true;
			}        	
        });        
	}
	
	@Override
	public void onPause() {
		super.onPause();
		//Need to finish both the Main Activity and the set preferences one
		//Otherwise if we pressed home from this activity, and then tried to unlock an app
		//it would take us to one of the proauth activities instead of the app itself
		Intent intent = new Intent("com.example.proauth.FINISH_ACTIVITY");
    	sendBroadcast(intent);
		finish();
	}
	
	//Binding to the monitor service, this is needed in order to register the system timeout receivers
	//if the system timeout box is checked after we have started the service
	
	private MonitorService monitorService;
	private boolean mIsBound;
	
	private ServiceConnection mServiceConnection = new ServiceConnection(){
		@Override
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        monitorService = ((MonitorService.LocalBinder)service).getService();
	    }

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			 monitorService = null;
		}
	    
	};
	
	void doBindService() {
	    getApplicationContext().bindService(new Intent(getApplicationContext(), 
	            MonitorService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}
	
	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	    	getApplicationContext().unbindService(mServiceConnection);
	        mIsBound = false;
	    }
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		doUnbindService();
	}
}
