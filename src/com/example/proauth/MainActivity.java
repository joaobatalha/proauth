package com.example.proauth;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	//CheckBox prefCheckBox;
	TextView prefManageApps;
	ListView prefList;
	String TAG = "MainActivity";
	String[] values = new String[] {"Manage Your Apps", "History and Logs", "FAQ & Tutorial", 
			"About ProAuth"
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar ab = getActionBar(); 
        ab.setDisplayShowTitleEnabled(false); 
        ab.setDisplayShowHomeEnabled(false);
        setContentView(R.layout.activity_main);

        
        // For the TextView
        //prefCheckBox = (CheckBox)findViewById(R.id.prefCheckBox);
    	prefManageApps = (TextView)findViewById(R.id.prefEditText);
    	loadPref();
    	
    	// For the ListView
    	prefList = (ListView) findViewById(R.id.prefList);
    	
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
    			  android.R.layout.simple_list_item_1, android.R.id.text1, values);
    	prefList.setAdapter(adapter);
    	OnItemClickListener listener = new OnItemClickListener(){
    	      public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng) {
    	          String selectedFromList =(String) (prefList.getItemAtPosition(myItemInt));
    	          Log.d(TAG, selectedFromList);
    	          if (selectedFromList.equals(values[0])){
        	          Intent intent = new Intent("com.example.proauth.ManageAppsActivity"); 
        	          startActivity(intent); 
    	          };
    	        }
    	};
    	prefList.setOnItemClickListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		/*
		 * Because it's only ONE option in the menu.
		 * In order to make it simple, We always start SetPreferenceActivity
		 * without checking.
		 */
		
		Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetPreferencesActivity.class);
        startActivityForResult(intent, 0); 
		
        return true;
	}
	
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		//super.onActivityResult(requestCode, resultCode, data);
		
		/*
		 * To make it simple, always re-load Preference setting.
		 */
		
		loadPref();
	}

	private void loadPref(){
		SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		boolean my_checkbox_preference = mySharedPreferences.getBoolean("security_level_0", false);
		Log.d(TAG, "Checkbox pref:" + my_checkbox_preference);
		//prefCheckBox.setChecked(my_checkbox_preference);

		String my_edittext_preference = mySharedPreferences.getString("edittext_preference", "");
    	prefManageApps.setText(my_edittext_preference);

	}
}
