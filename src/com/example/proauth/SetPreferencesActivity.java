package com.example.proauth;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;


public class SetPreferencesActivity extends PreferenceActivity {

	CheckBoxPreference monitor;
	CheckBoxPreference app_timeout;
	CheckBoxPreference system_timeout;
	Editor e;
	public static String TAG = "SetPreferencesActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
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
			    	system_timeout.setEnabled(false);
			    	app_timeout.setEnabled(false);
			    	system_timeout.setChecked(false);
			    	app_timeout.setChecked(false);
				}
				return true;
			}
        	
        });
        

        app_timeout = (CheckBoxPreference) preferenceManager.findPreference("trigger_0"); 
        system_timeout = (CheckBoxPreference) preferenceManager.findPreference("trigger_1"); 
        
        
        app_timeout.setEnabled(app_timeout.isChecked());
        if(!app_timeout.isEnabled()){
        	system_timeout.setEnabled(true);
        }
        
        if(!monitor.isChecked()){
        	app_timeout.setChecked(false);
        	system_timeout.setChecked(false);
        }
        
        
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
