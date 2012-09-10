package org.haldean.bodylog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.TextView;

public class Logging extends Activity {
	private boolean isLogging;
	private final ScheduledExecutorService scheduler =
		     Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> handle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);
        isLogging = false;
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
    
    protected BodylogCommand getRunnable() {
    	return new BodylogCommand(
    			(LocationManager) getSystemService(Context.LOCATION_SERVICE)
    			);
    }
    
    protected void toggleLogging() {
    	isLogging = !isLogging;
    	((TextView) findViewById(R.id.statusText)).setText(
    			isLogging ? R.string.logging : R.string.logging_off);
    	if (isLogging) {
    		handle = scheduler.scheduleAtFixedRate(getRunnable(), 0, 2, TimeUnit.SECONDS);
    	} else {
    		handle.cancel(false);
    	}
    }
}
