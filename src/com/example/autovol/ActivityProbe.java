package com.example.autovol;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognitionClient;

import edu.mit.media.funf.probe.Probe.Base;

public class ActivityProbe extends Base implements ConnectionCallbacks, OnConnectionFailedListener {
	
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 20;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;
    
    public static final String BROADCAST_ACTION = "com.example.autovol.ACTIVITY_RECOGNITION_DATA";
    
    private PendingIntent mActivityRecognitionPendingIntent;
    // Store the current activity recognition client
    private ActivityRecognitionClient mActivityRecognitionClient;
    
    private boolean mInProgress;
    
    public enum REQUEST_TYPE {START, STOP}
    private REQUEST_TYPE mRequestType;
    
    public ActivityProbe(Context context) {
    	super(context);
        /*
         * Instantiate a new activity recognition client. Since the
         * parent Activity implements the connection listener and
         * connection failure listener, the constructor uses "this"
         * to specify the values of those parameters.
         */
        mActivityRecognitionClient =
                new ActivityRecognitionClient(context, this, this);
        /*
         * Create the PendingIntent that Location Services uses
         * to send activity recognition updates back to this app.
         */
        Intent intent = new Intent(
               context, ActivityRecognitionIntentService.class);
        /*
         * Return a PendingIntent that starts the IntentService.
         */
        mActivityRecognitionPendingIntent =
                PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        
        mInProgress = false;
    }

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
        // Turn off the request flag
        mInProgress = false;
		
	}

	@Override
	public void onConnected(Bundle dataBundle) {
        switch (mRequestType) {
        	case START :
            /*
             * Request activity recognition updates using the
             * preset detection interval and PendingIntent.
             * This call is synchronous.
             */
            mActivityRecognitionClient.requestActivityUpdates(
                    DETECTION_INTERVAL_MILLISECONDS,
                    mActivityRecognitionPendingIntent);
            break;
        
        	case STOP :
            mActivityRecognitionClient.removeActivityUpdates(
                    mActivityRecognitionPendingIntent);
            break;        	

        	default :
            break;
        }
        /*
         * Since the preceding call is synchronous, turn off the
         * in progress flag and disconnect the client
         */
        mInProgress = false;
        mActivityRecognitionClient.disconnect();
		
	}

	@Override
	public void onDisconnected() {
        // Turn off the request flag
        mInProgress = false;
        // Delete the client
        mActivityRecognitionClient = null;
	}
	
    /**
     * Called when the probe switches from the disabled to the enabled
     * state. This is where any passive or opportunistic listeners should be
     * configured. An enabled probe should not keep a wake lock. If you need
     * the device to stay awake consider implementing a StartableProbe, and
     * using the onStart method.
     */
	@Override
    protected void onEnable() {
		if (!mInProgress) {
			mInProgress = true;
			mActivityRecognitionClient.connect();
			
			mRequestType = REQUEST_TYPE.START;
		}
    }

    /**
     * Called when the probe switches from the enabled state to active
     * running state. This should be used to send any data broadcasts, but
     * must return quickly. If you have any long running processes they
     * should be started on a separate thread created by this method, or
     * should be divided into short runnables that are posted to this
     * threads looper one at a time, to allow for the probe to change state.
     */
	@Override
    protected void onStart() {

    }

    /**
     * Called with the probe switches from the running state to the enabled
     * state. This method should be used to stop any running threads
     * emitting data, or remove a runnable that has been posted to this
     * thread's looper. Any passive listeners should continue running.
     */
	@Override
    protected void onStop() {

    }

    /**
     * Called with the probe switches from the enabled state to the disabled
     * state. This method should be used to stop any passive listeners
     * created in the onEnable method. This is the time to cleanup and
     * release any resources before the probe is destroyed.
     */
	@Override
    protected void onDisable() {
		// Set the request type to STOP
        mRequestType = REQUEST_TYPE.STOP;
        
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
        }
    }

}
