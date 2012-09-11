package org.haldean.bodylog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

class BodylogCommand implements Runnable, LocationListener {
	Location lastLocation;
	final ReentrantLock sensorLock;
	final String endpoint;
	final String deviceId;
	
	public BodylogCommand(LocationManager lm, String ep, String did) {
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, this);
		endpoint = ep;
		deviceId = did;
		sensorLock = new ReentrantLock();
	}
	
	public void run() {
		sensorLock.lock();

		if (lastLocation != null) {
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("lat", String.valueOf(lastLocation.getLatitude())));
			params.add(new BasicNameValuePair("lon", String.valueOf(lastLocation.getLongitude())));
			params.add(new BasicNameValuePair("alt", String.valueOf(lastLocation.getAltitude())));
			params.add(new BasicNameValuePair("id", deviceId));
			params.add(new BasicNameValuePair("time", String.valueOf(System.nanoTime())));
			
			final HttpPost request = new HttpPost(endpoint);
			try {
				request.setEntity(new UrlEncodedFormEntity(params));
				new Thread(new Runnable() {
					public void run() {
						HttpClient httpClient = new DefaultHttpClient();
						try {
							httpClient.execute(request);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		sensorLock.unlock();
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
