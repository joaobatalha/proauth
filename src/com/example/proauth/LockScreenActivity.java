package com.example.proauth;

import java.util.prefs.Preferences;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LockScreenActivity extends Activity {

	public ImageView BackgroundGlowImageView;
	public TextView WarningTextView;
	public EditText PasswordEnter;
	public TextView WhichAppTextView;
	public ImageView AppIconImageView;
	public Button ToProAuthButton;
	private String TAG = "LockScreenActivity";
	private Context mContext;
	private String app_name;
	
	public static final String BlockedActivityName = "BlockedActivity";
	public static final String BlockedPackageName = "BlockedPackage";
	public static final String PACKAGE_NAME = "com.example.proauth.PackageName";
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

		ToProAuthButton = (Button) findViewById(R.id.EnterButton);
		ToProAuthButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(mContext, MainActivity.class);
				startActivity(intent);
			}
		});

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
		                	Log.d(TAG, "Password was correct:" + passed);
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
		// toggleShowWarning();
		
		if(app_name.equals("proauth_settings")){	//special case the settings
			WhichAppTextView.setText("to change proAuth settings.");
		} else {									//normal app case
			final PackageManager pm = getApplicationContext().getPackageManager();
			ApplicationInfo ai;
			try {
				ai = pm.getApplicationInfo(app_name, 0);
			} catch (final NameNotFoundException e) {
				ai = null;
			}
			final String applicationName = (ai != null ? ai.loadLabel(pm)
					.toString() : "(unknown)");
			Log.d(TAG, "App that summoned me: " + applicationName);
			
			WhichAppTextView.setText("to unlock " + applicationName + ".");
	
			// grab the icon
			Drawable app_icon = pm.getApplicationIcon(ai);
			if (app_icon != null) {
				AppIconImageView.setImageDrawable(app_icon);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// unregisterReceiver(LockState);
	}

	public void toggleLockScreenState() {
	}

	public boolean checkPassword(String password) {
		Log.d(TAG, "Checking password...");
    	SharedPreferences mySharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		String real_password = mySharedPreferences.getString("proauth_password", "1234");
		Log.d(TAG, "real password: " + real_password);
    	if (real_password.equals(password)){
    		return true;
    	} else {
    		return false;
    	}
	}

	public void showSecuritySettingsActivity() {
	}

	@Override
	public void onBackPressed() {
		// Make it dead.
	}

	private void toggleShowWarning() {
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean hideNotification = false;
		if (hideNotification) {
			WarningTextView.setVisibility(View.VISIBLE);
		} else {
			WarningTextView.setVisibility(View.GONE);
		}
	}
	
	private void youShallPass(){
		if(app_name.equals("proauth_settings")){	//special case the settings
			Intent intent = new Intent();
			intent.setClass(this, SetPreferencesActivity.class);
			startActivityForResult(intent, 0);
		} else {									//normal app case
			Log.d(TAG, "You can go to your app. yay! Except I don't know how, until we merge with Joao.");
			this.sendBroadcast(
					new Intent()
						.setAction(PASSED)
						.putExtra(PACKAGE_NAME, getIntent().getStringExtra(BlockedPackageName)));
			
			finish();
			/*
			Intent intent = new Intent(app_name);
			startActivity(intent);
			*/
		}
	}

	private void youShallNotPass(){
		String wrong_text = "That wasn't the right password. Use your proauth password.";
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
	public void onStop(){
		//TODO Second chance?
		super.onStop();
		/*
		Intent intent = new Intent();
		intent
			.setAction(Intent.ACTION_MAIN)
			.addCategory(Intent.CATEGORY_HOME)
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		*/
	}
	
	
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.actionbar_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.MenuItemPreferences: {
			checkPassword();
		}
			break;
		case R.id.MenuItemSecuritySettings: {
			showSecuritySettingsActivity();
		}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}
	*/
}