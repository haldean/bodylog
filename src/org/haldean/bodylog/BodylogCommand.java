package org.haldean.bodylog;

import java.util.concurrent.locks.ReentrantLock;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

class BodylogCommand implements Runnable, LocationListener {
	Location lastLocation;
	ConnectivityManager connectivityManager;
	WifiManager wifiManager;
	ReentrantLock sensorLock;
	
	public BodylogCommand(LocationManager lm, ConnectivityManager cm, WifiManager wm) {
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 0, this);
		connectivityManager = cm;
		wifiManager = wm;
		sensorLock = new ReentrantLock();
	}
	
	public void run() {
		sensorLock.lock();
		
		Log.i("BodylogCommand", "Start logging.");
		Log.i("BodylogCommand", "Location: " + (lastLocation == null ? "null" : lastLocation.toString()));
		
		sensorLock.unlock();
	}
	
	String getWifiStatus() {
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo.isConnected()) {
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null) {
				return connectionInfo.getSSID();
			}
		}
		return null;
	}

	public void onLocationChanged(Location location) {
		sensorLock.lock();
		lastLocation = location;
		sensorLock.unlock();
	}

	public void onProviderDisabled(String provider) {}
	public void onProviderEnabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}

}
