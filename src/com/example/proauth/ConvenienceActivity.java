package com.example.proauth;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ConvenienceActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_convenience);
		String TAG = "Convenience Activity";

		Log.d(TAG, "OnCreate");

		/*
		try {
			Process process = Runtime.getRuntime().exec(
					"logcat ActivityManager:I *:S");
			InputStreamReader isr = new InputStreamReader(
					process.getInputStream());
			BufferedReader bufferedReader = new BufferedReader(isr);

			StringBuilder log = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (!line.contains("MainActivity")) {
					log.append(line);
				}
			}

			// Log.e("LOGCAT CLONE", log.toString());

			TextView tv = (TextView) findViewById(R.id.main_page_welcome);
			tv.setText(log.toString());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		*/
		// Intent intent = new
		// Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		// intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
		// securemeAdmin);
	}
}