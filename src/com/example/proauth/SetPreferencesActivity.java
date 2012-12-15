package com.example.proauth;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.SystemClock;
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
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		e = sp.edit();
        
        PreferenceManager preferenceManager = getPreferenceManager();  
        monitor = 
                 (CheckBoxPreference) preferenceManager.findPreference("monitor_on"); 

        Log.d(TAG, "The monitor button: " + monitor.toString());
        monitor.setOnPreferenceClickListener(new OnPreferenceClickListener(){
        	
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				// TODO Auto-generated method stub
				if(monitor.isChecked()){
					Log.i("PREFERENCES", "Starting monitoring service");
					Intent intent = new Intent();
			    	intent.setClass(SetPreferencesActivity.this, MonitorService.class);
			    	startService(intent);
			    	system_timeout.setEnabled(true);
				}
				else{
					Log.i("PREFERENCES", "Stopping monitoring service");
					Intent intent = new Intent();
			    	intent.setClass(SetPreferencesActivity.this, MonitorService.class);
			    	stopService(intent);
			    	//system_timeout.setEnabled(false);
			    	//app_timeout.setEnabled(false);
			    	//system_timeout.setChecked(false);
			    	//app_timeout.setChecked(false);
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
				} else {
					system_timeout.setEnabled(true);
					
				}
				return true;
			}
        	
        });
        
        system_timeout.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if(system_timeout.isChecked()){
					e.putBoolean("trigger_0", false);
					e.commit();
					app_timeout.setChecked(false);
					app_timeout.setEnabled(false);
				} else{
					app_timeout.setEnabled(true);
				}
				return true;
			}
        	
        });
        

        
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Intent intent = new Intent("com.example.proauth.FINISH_ACTIVITY");
    	sendBroadcast(intent);
		finish();
	}
	
	/*
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	*/

}
