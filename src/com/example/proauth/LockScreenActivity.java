package com.example.proauth;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.Bundle;

public class LockScreenActivity extends Activity {

	public static class Controller extends Activity {

		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
			//keyguardManager.
            //KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
           // lock.reenableKeyguard();
		}

	}
}