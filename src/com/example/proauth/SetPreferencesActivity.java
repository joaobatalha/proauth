package com.example.proauth;


import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;


public class SetPreferencesActivity extends PreferenceActivity {

	CheckBoxPreference monitor;
	public static String TAG = "SetPreferencesActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        PreferenceManager preferenceManager = getPreferenceManager();  
        monitor = 
                 (CheckBoxPreference) preferenceManager.findPreference("monitor_on"); 

        Log.d(TAG, "The monitor button: " + monitor.toString());
        monitor.setOnPreferenceClickListener(new OnPreferenceClickListener(){
        	
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				// TODO Auto-generated method stub
				monitor.isChecked();
				if(monitor.isChecked()){
					Log.i("PREFERENCES", "Starting monitoring service");
					Intent intent = new Intent();
			    	intent.setClass(SetPreferencesActivity.this, MonitorService.class);
			    	startService(intent);
				}
				else{
					Log.i("PREFERENCES", "Stopping monitoring service");
					Intent intent = new Intent();
			    	intent.setClass(SetPreferencesActivity.this, MonitorService.class);
			    	stopService(intent);
					
				}
				return true;
			}
        	
        });
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

}
