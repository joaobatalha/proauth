package com.example.proauth;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

public class adminActivity extends DeviceAdminReceiver {

	public static class Controller extends Activity {

		DevicePolicyManager mDPM;
		ComponentName mDeviceAdminSample;

		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
			mDeviceAdminSample = new ComponentName(Controller.this,
					adminActivity.class);
		}

		public void lock() {
			mDPM.lockNow();
		}
	}
}