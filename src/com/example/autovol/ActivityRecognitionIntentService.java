package com.example.autovol;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends IntentService {

	public ActivityRecognitionIntentService() {
		super("ActivityRecognitionIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		 // If the incoming intent contains an update
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Get the update
            ActivityRecognitionResult result =
                    ActivityRecognitionResult.extractResult(intent);
            // Get the most probable activity
            DetectedActivity mostProbableActivity =
                    result.getMostProbableActivity();
            /*
             * Get the probability that this activity is the
             * the user's actual activity
             */
            int confidence = mostProbableActivity.getConfidence();
            /*
             * Get an integer describing the type of activity
             */
            int activityType = mostProbableActivity.getType();
            String activityName = getNameFromType(activityType);
            /*
             * At this point, you have retrieved all the information
             * for the current update. You can display this
             * information to the user in a notification, or
             * send it to an Activity or Service in a broadcast
             * Intent.
             */
            
            Intent resultIntent = new Intent(ActivityProbe.BROADCAST_ACTION);
            resultIntent.putExtra("activity_type", activityType);
            resultIntent.putExtra("activity_name", activityName);
            resultIntent.putExtra("confidence", confidence);
            sendBroadcast(resultIntent);
        }

	}
        
        /**
         * Map detected activity types to strings
         *@param activityType The detected activity type
         *@return A user-readable name for the type
         */
        private String getNameFromType(int activityType) {
            switch(activityType) {
                case DetectedActivity.IN_VEHICLE:
                    return "in_vehicle";
                case DetectedActivity.ON_BICYCLE:
                    return "on_bicycle";
                case DetectedActivity.ON_FOOT:
                    return "on_foot";
                case DetectedActivity.STILL:
                    return "still";
                case DetectedActivity.UNKNOWN:
                    return "unknown";
                case DetectedActivity.TILTING:
                    return "tilting";
            }
            return "unknown";
        }

}
