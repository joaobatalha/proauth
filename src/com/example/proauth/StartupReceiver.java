package com.example.proauth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent intent) {
		Log.d("JOAO", "boot receiver");
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(arg0);
		boolean monitor_on = sp.getBoolean("monitor_on", false);
		arg0.startService(new Intent(arg0, MonitorService.class));	
//		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
				if (monitor_on){
					arg0.startService(new Intent(arg0, MonitorService.class));	
				}
//		}

	}
}
