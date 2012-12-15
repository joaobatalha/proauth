package com.example.proauth;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class TrustedLocationState implements LocationListener {
	
	private LocationManager locationManager;
	private boolean isOn = false;
	private boolean inSafeLocation = false;
	private final static double DISTANCE_THRESHOLD = 50;
	public final static long UPDATE_THRESHOLD = 120000;
	private SharedPreferences sharedPref;
	private double trustedlatitude;
	private double trustedlongitude;
	private double currLatitude;
	private double currLongitude;
	private BlockActivityHandler bah;
	public static final String TRUSTED_LONGITUDE = "gps_trusted_longitude";
	public static final String TRUSTED_LATITUDE = "gps_trusted_latitude";
	public static final String CURRENT_LONGITUDE = "gps_current_longitude";
	public static final String CURRENT_LATITUDE = "gps_current_latitude";
	public static final String RECENT_GPS_UPDATE_TIME = "gps_recent_update_time";
	public static final String TURN_ON_OFF_LOC = "com.example.proauth.TURN_ON_OFF_LOC";
	public static final String ON_OR_OFF = "com.example.proauth.ON_OFF_LOC";
	public static final String TAG = "TrustedLocationState";

	public TrustedLocationState(BlockActivityHandler bah, Context context) {
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		this.bah = bah;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		Log.d(TAG, "TrustedLocationState: Location has changed!!!");
		currLatitude = location.getLatitude();
		currLongitude = location.getLongitude();
		Editor e = sharedPref.edit();
		e.putString(CURRENT_LATITUDE, currLatitude+"");
		e.putString(CURRENT_LONGITUDE, currLongitude+"");
		e.putLong(RECENT_GPS_UPDATE_TIME, SystemClock.elapsedRealtime());
		e.commit();
		String trustedLatitudeStr = sharedPref.getString(TRUSTED_LATITUDE, null);
		String trustedLongitudeStr = sharedPref.getString(TRUSTED_LONGITUDE, null);
		if (trustedLatitudeStr == null || trustedLongitudeStr == null) {
			if (inSafeLocation) {
				inSafeLocation = false;
				bah.onSafenessOfLocationChange(inSafeLocation);
			}
			return;
		}
		trustedlatitude = Double.parseDouble(trustedLatitudeStr);
		trustedlongitude = Double.parseDouble(trustedLongitudeStr);
		float[] distance = new float[1];
		Location.distanceBetween(currLatitude, currLongitude, trustedlatitude, trustedlongitude, distance);
		if (distance[0] < DISTANCE_THRESHOLD) {
			if (!inSafeLocation) {
				inSafeLocation = true;
				bah.onSafenessOfLocationChange(inSafeLocation);
			}
		} else if (inSafeLocation) {
			// was in a safe location, but not anymore
			inSafeLocation = false;
			bah.onSafenessOfLocationChange(inSafeLocation);
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
	public void turnOn() {
		Log.d(TAG, "Trying to turn on TrustedLocationState");
		if (!isOn) {
			Log.d(TAG, "Turning on TrustedLocationState");
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
			isOn = true;
		} else {
			Log.d(TAG, "WHAT?? TrustedLocationState is already ON!");
		}
	}
	
	public void turnOff() {
		Log.d(TAG, "Trying to turn off TrustedLocationState");
		if (isOn) {
			Log.d(TAG, "Turning off TrustedLocationState");
			locationManager.removeUpdates(this);
			isOn = false;
		} else {
			Log.d(TAG, "What??? TrustedLocationState is already OFF!");
		}
	}
	
}
