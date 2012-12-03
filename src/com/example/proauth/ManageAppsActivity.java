package com.example.proauth;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ManageAppsActivity extends Activity {

	// CheckBox prefCheckBox;
	ListView appList;
	String TAG = "ManageAppsActivity";
	Context mContext;

	String[] values;
	SharedPreferences sp;
	Editor e;

	int selectedItem;
	String app_to_edit;

	final String[] securityLevels = { SecurityLevel.PRIVATE.toString(),
			SecurityLevel.HIGH.toString(), SecurityLevel.MEDIUM.toString(),
			SecurityLevel.LOW.toString(), SecurityLevel.PUBLIC.toString(), };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "OnCreate");
		super.onCreate(savedInstanceState);
		mContext = this.getApplicationContext();

		// Disappear the icon/title
		ActionBar ab = getActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowHomeEnabled(false);
		setContentView(R.layout.manage_apps);

		// For the ListView
		final PackageManager pm = getPackageManager();

		ArrayList<String> apps = new ArrayList<String>();
		// get a list of installed apps.
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);

		List<ResolveInfo> list = pm.queryIntentActivities(intent,
				PackageManager.PERMISSION_GRANTED);
		for (ResolveInfo rInfo : list) {
			apps.add(rInfo.activityInfo.applicationInfo.loadLabel(pm)
					.toString());
			Log.w(TAG, rInfo.activityInfo.applicationInfo.loadLabel(pm)
					.toString());
		}
		appList = (ListView) findViewById(R.id.appList);
		values = apps.toArray(new String[apps.size()]);

		/*
		 * ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
		 * android.R.layout.simple_list_item_1, values);
		 */
		ArrayList<AppSecurity> ass = new ArrayList<AppSecurity>();

		// Grab and Set Preferences
		sp = this.getSharedPreferences(TAG, MODE_PRIVATE);
		e = sp.edit();
		
		for (int i = 0; i < values.length; i++) {
			String security_level = sp.getString(values[i],
					SecurityLevel.PRIVATE.toString());
			e.putString(values[i], security_level);
			Log.d(TAG, values[i]);
			ass.add(new AppSecurity(values[i], security_level));
		}
		e.apply();

		AppSecurityArrayAdapter adapter = new AppSecurityArrayAdapter(this,
				R.layout.app_security_listitem, ass);
		appList.setAdapter(adapter);

		// set listener for changing security level
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> myAdapter, View myView,
					int myItemInt, long mylng) {
				AppSecurity selectedFromList = (AppSecurity) (appList
						.getItemAtPosition(myItemInt));
				Log.d(TAG, "Selected the app: " + selectedFromList.toString());
				app_to_edit = selectedFromList.app_name;
				popDialog();
			}
		};
		appList.setOnItemClickListener(listener);
	}
	
	private void saveSecurityLevel(){
		Log.d(TAG, "Saving..." + app_to_edit + selectedItem);
		String securityLevel = securityLevels[selectedItem];
		e.putString(app_to_edit, securityLevel);
		e.commit();
		refreshApps();
	}

	private void popDialog() {
		// make dialog
		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
		alt_bld.setIcon(R.drawable.ic_launcher);
		alt_bld.setTitle("Choose a Security Level");
		selectedItem = -1;
		alt_bld.setSingleChoiceItems(securityLevels, -1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						selectedItem = item;
					}
				});
		alt_bld.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(getApplicationContext(),
						"Selected digit: " + securityLevels[selectedItem],
						Toast.LENGTH_SHORT).show();
				saveSecurityLevel();
			}
		});
		
		alt_bld.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Do Nothing.
					}
				});

		AlertDialog alert = alt_bld.create();
		alert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * Because it's only ONE option in the menu. In order to make it simple,
		 * We always start SetPreferenceActivity without checking.
		 */
		/*
		 * Intent intent = new Intent();
		 * intent.setClass(ManageAppsActivity.this,
		 * SetPreferencesActivity.class); startActivityForResult(intent, 0);
		 */
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		// super.onActivityResult(requestCode, resultCode, data);

		/*
		 * To make it simple, always re-load Preference setting.
		 */

		refreshApps();
	}

	private void refreshApps() {
		Log.d(TAG, "Refresh");
		ArrayList<AppSecurity> ass = new ArrayList<AppSecurity>();
		for (int i = 0; i < values.length; i++) {
			String security_level = sp.getString(values[i],
					SecurityLevel.PRIVATE.toString());
			e.putString(values[i], security_level);
			Log.d(TAG, values[i]);
			ass.add(new AppSecurity(values[i], security_level));
		}

		AppSecurityArrayAdapter adapter = new AppSecurityArrayAdapter(this,
				R.layout.app_security_listitem, ass);
		appList.setAdapter(adapter);
	}

	public class AppSecurity {
		public String app_name;
		public String app_security_level;

		public AppSecurity(String app_name, String sl) {
			this.app_name = app_name;
			this.app_security_level = sl;
		}

		@Override
		public String toString() {
			return (this.app_name + " " + this.app_security_level);
		}
	}

	public class AppSecurityArrayAdapter extends ArrayAdapter<AppSecurity> {
		private ArrayList<AppSecurity> apps;

		public AppSecurityArrayAdapter(Context context, int textViewResourceId,
				ArrayList<AppSecurity> app_security_settings) {
			super(context, textViewResourceId, app_security_settings);
			this.apps = app_security_settings;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.app_security_listitem, null);
			}

			AppSecurity app_security = apps.get(position);
			if (app_security != null) {
				TextView username = (TextView) v.findViewById(R.id.app_name);
				TextView security_level = (TextView) v
						.findViewById(R.id.app_security_level);

				if (username != null) {
					username.setText(app_security.app_name);
				}

				if (security_level != null) {
					security_level.setText("Security Level: "
							+ app_security.app_security_level);
				}
			}
			return v;
		}
	}
}
