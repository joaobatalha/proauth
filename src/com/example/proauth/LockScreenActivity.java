package com.example.proauth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LockScreenActivity extends Activity {

	public ImageView BackgroundGlowImageView;
	public TextView WarningTextView;
	public EditText PasswordEnter;
	public TextView WhichAppTextView;
	public ImageView AppIconImageView;
	private String TAG = "LockScreenActivity";
	private Context mContext;
	private String app_name;
	
	public static final String BlockedActivityName = "BlockedActivity";
	public static final String BlockedPackageName = "BlockedPackage";
	public static final String PACKAGE_NAME = "com.example.proauth.PackageName";
	public static final String ACTIVITY_NAME = "com.example.proauth.ActivityName";
	public static final String PASSED = "com.example.proauth.PASSED";
	public static final String NOT_PASSED = "com.example.proauth.NOT_PASSED";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lock);
		mContext = this.getApplicationContext();
		setWindowMode();
		getViews();
	}

	private void setWindowMode() {
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	private void getViews() {
		BackgroundGlowImageView = (ImageView) findViewById(R.id.BackgroundGlowImageView);
		AppIconImageView = (ImageView) findViewById(R.id.AppIcon);

		PasswordEnter = (EditText) findViewById(R.id.PasswordEnter);
		PasswordEnter.setOnEditorActionListener(
		        new EditText.OnEditorActionListener() {
		            @Override
		            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
		                        actionId == EditorInfo.IME_ACTION_DONE ||
		                        event.getAction() == KeyEvent.ACTION_DOWN &&
		                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
		                	boolean passed = checkPassword(PasswordEnter.getText().toString());
		                	if (passed){
		                		youShallPass();
		                	} else {
		                		youShallNotPass();
		                	}
		                    return true;
		                }
		                return false;
		            }

		        });

		WarningTextView = (TextView) findViewById(R.id.WarningTextView);
		WhichAppTextView = (TextView) findViewById(R.id.WhichAppTextView);
	}

	@Override
	public void onResume() {
		super.onResume();

		app_name = this.getIntent().getStringExtra(BlockedPackageName);
		
		if(app_name.equals("proauth_settings") || app_name.equals("proauth_app_settings")){	//special case the settings
			WhichAppTextView.setText("to change proAuth settings.");
		} else {//normal app case
			final PackageManager pm = getApplicationContext().getPackageManager();
			ApplicationInfo ai;
			try {
				ai = pm.getApplicationInfo(app_name, 0);
			} catch (final NameNotFoundException e) {
				ai = null;
			}
			final String applicationName = (ai != null ? ai.loadLabel(pm)
					.toString() : "(unknown)");
			
			WhichAppTextView.setText("to unlock " + applicationName + ".");
	
			// grab the icon
			Drawable app_icon = pm.getApplicationIcon(ai);
			if (app_icon != null) {
				AppIconImageView.setImageDrawable(app_icon);
			}
		}
	}

	//
	@Override
	public void onPause() {
		super.onPause();
		finish();
	}

	public boolean checkPassword(String password) {
    	SharedPreferences mySharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String real_password = mySharedPreferences.getString("proauth_password", "1234");
    	if (real_password.equals(password)){
    		return true;
    	} else {
    		return false;
    	}
	}

	//We need to change the back button behaviour
	//otherwise it would take us the the app that we are trying
	//to block
	@Override
	public void onBackPressed() {
		// Now directs the user to the home screen
    	Intent intent = new Intent();
    	intent
    		.setAction(Intent.ACTION_MAIN)
    		.addCategory(Intent.CATEGORY_HOME)
    		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	startActivity(intent);
    	finish();
	}

	
	//Right password
	private void youShallPass(){
		if(app_name.equals("proauth_settings")){	//special case the ProAuth settings 
			Intent intent = new Intent();
			intent.setClass(this, SetPreferencesActivity.class);
			startActivityForResult(intent, 0);
		} else if(app_name.equals("proauth_app_settings")){	//special case for the activity that sets the security level of each app
			Intent intent = new Intent();
			intent.setClass(this, ManageAppsActivity.class);
			startActivityForResult(intent, 0);
		} else {//Every other app
			this.sendBroadcast(
					new Intent()
						.setAction(PASSED)
						.putExtra(PACKAGE_NAME, getIntent().getStringExtra(BlockedPackageName))
						.putExtra(ACTIVITY_NAME, getIntent().getStringExtra(BlockedActivityName)));
			finish();
		}
	}

	//Wrong password
	private void youShallNotPass(){
		String wrong_text = "That wasn't the right password. Use your ProAuth password.";
		Toast wrongPassword = Toast.makeText(mContext, wrong_text, Toast.LENGTH_LONG);
		wrongPassword.show();
		this.sendBroadcast(
				new Intent()
					.setAction(NOT_PASSED)
					.putExtra(PACKAGE_NAME, getIntent().getStringExtra(BlockedPackageName)));

    	Intent intent = new Intent();
    	intent
    		.setAction(Intent.ACTION_MAIN)
    		.addCategory(Intent.CATEGORY_HOME)
    		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	startActivity(intent);
    	finish();	
	}
	
	  @Override 
	    public void onConfigurationChanged(Configuration newConfig) 
	    { 
	        super.onConfigurationChanged(newConfig);
	    }

	
}