package com.example.proauth;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {

	ListView prefList;
	String TAG = "MainActivity";
	public static String PHONE_SECURITY_STATE = "com.example.proauth.phone_security_state";
	String[] values = new String[] {"ProAuth Settings", "Manage Your Apps", 
			"FAQ & Tutorial"};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		registerFinishReceiver();
		loadPref();

		// For the ListView
		prefList = (ListView) findViewById(R.id.prefList);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, values);
		prefList.setAdapter(adapter);
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> myAdapter, View myView,
					int myItemInt, long mylng) {
				String selectedFromList = (String) (prefList
						.getItemAtPosition(myItemInt));
				if (selectedFromList.equals(values[0])) {
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, LockScreenActivity.class);
					intent.putExtra(LockScreenActivity.BlockedPackageName, "proauth_settings");
					startActivityForResult(intent, 0);
				} else if (selectedFromList.equals(values[1])) {
					Intent intent = new Intent("com.example.proauth.LockScreenActivity");
					intent.putExtra(LockScreenActivity.BlockedPackageName, "proauth_app_settings");
					startActivity(intent);
				} else if (selectedFromList.equals(values[2])) {
					Intent intent = new Intent("com.example.proauth.FAQActivity");
					startActivity(intent);
				} 
			}
		};
		prefList.setOnItemClickListener(listener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		/*
		 * Because it's only ONE option in the menu. In order to make it simple,
		 * We always start SetPreferenceActivity without checking.
		 */

		Intent intent = new Intent();
		intent.setClass(MainActivity.this, LockScreenActivity.class);
		intent.putExtra(LockScreenActivity.BlockedPackageName, "proauth_settings");
		startActivityForResult(intent, 0);
		
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * To make it simple, always re-load Preference setting.
		 */
		loadPref();
	}

	private void loadPref() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Initiate 
		Editor e = sp.edit();
		String state = sp.getString(PHONE_SECURITY_STATE, SecurityLevel.PUBLIC.toString());
		e.putString(PHONE_SECURITY_STATE, state);
		e.commit();

		Log.i(TAG, "Current phone state:" + state);

		boolean monitor_on = sp.getBoolean(
				"monitor_on", false);
	}
	
	private void registerFinishReceiver(){
		IntentFilter filter = new IntentFilter("com.example.proauth.FINISH_ACTIVITY");
		registerReceiver(finishedReceiver, filter);
	}
	
    BroadcastReceiver finishedReceiver = new BroadcastReceiver(){ 
        public void onReceive(Context context, Intent intent){ 
        	MainActivity.this.finish();   
        } 
    }; 
    
    @Override
    public void onDestroy(){
    	unregisterReceiver(finishedReceiver);
    	super.onDestroy();
    }
}
