package org.haldean.bodylog;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Base64;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

public class Logging extends Activity {
	private static final String PREFS_FILE = "BodylogPreferences";
	private static final String ENDPOINT_KEY = "endpoint";
	private static final String DEVICE_KEY = "deviceid";
	
	private boolean isLogging;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> handle;
	private String deviceId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);
        
        isLogging = false;
        
        deviceId = getPrefs().getString(DEVICE_KEY, null);
        if (deviceId == null) {
        	byte randomBytes[] = new byte[5];
        	new Random().nextBytes(randomBytes);
        	deviceId = Base64.encodeToString(randomBytes, Base64.URL_SAFE).trim();
        	SharedPreferences.Editor editor = getPrefsEditor();
        	editor.putString(DEVICE_KEY, deviceId);
        	editor.commit();
        }
        ((TextView) findViewById(R.id.deviceIdText)).setText(deviceId);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (event.getAction() == MotionEvent.ACTION_DOWN) {
    		toggleLogging();
    	}
    	return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_logging, menu);
        return true;
    }
    
    protected SharedPreferences getPrefs() {
    	return getSharedPreferences(PREFS_FILE, MODE_PRIVATE);
    }
    
    protected SharedPreferences.Editor getPrefsEditor() {
    	return getPrefs().edit();
    }
    
    public void clearUserEndpoint() {
    	SharedPreferences.Editor prefsEditor = getPrefsEditor();
    	prefsEditor.remove(ENDPOINT_KEY);
    	prefsEditor.commit();
    }
    
    protected void setUserEndpoint(String value) {
    	SharedPreferences.Editor prefsEditor = getPrefsEditor();
    	prefsEditor.putString(ENDPOINT_KEY, value);
    	prefsEditor.commit();
    }
    
    protected String getUserEndpoint() {
    	String endpoint = getPrefs().getString(ENDPOINT_KEY, null);
    	if (!endpoint.startsWith("http")) {
    		endpoint = "http://" + endpoint;
    	}
    	return endpoint;
    }
    
    protected boolean confirmEndpointSet() {
    	SharedPreferences prefs = getPrefs();
    	String endpoint = prefs.getString(ENDPOINT_KEY, null);
    	
    	if (endpoint == null) {
    		AlertDialog.Builder inputBox = new AlertDialog.Builder(this);
    		inputBox.setTitle("Set Bodylog Endpoint");
    		inputBox.setMessage("This is the URL that Bodylog will post your location to.");
    		
    		final EditText textBox = new EditText(this);
    		textBox.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
    		inputBox.setView(textBox);
    		inputBox.setPositiveButton("Set", new DialogInterface.OnClickListener() {			
				public void onClick(DialogInterface dialog, int which) {
					setUserEndpoint(textBox.getText().toString());
				}
			});
    		inputBox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Ignore.
				}
			});
    		inputBox.show();
    		return false;
    	}
    	
    	return true;
    }
    
    protected BodylogCommand getRunnable() {
    	return new BodylogCommand(
    			(LocationManager) getSystemService(Context.LOCATION_SERVICE),
    			getUserEndpoint(), deviceId);
    }
    
    protected void toggleLogging() {
    	if (!confirmEndpointSet()) {
    		return;
    	}
    	isLogging = !isLogging;
    	((TextView) findViewById(R.id.statusText)).setText(
    			isLogging ? R.string.logging : R.string.logging_off);
    	if (isLogging) {
    		handle = scheduler.scheduleAtFixedRate(getRunnable(), 0, 5, TimeUnit.SECONDS);
    	} else {
    		handle.cancel(false);
    	}
    }
}
