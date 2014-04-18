package com.autovol;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.LocationClient;

public class LocationService extends IntentService {

	public LocationService() {
		super("LocationService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Location location = intent.getParcelableExtra(LocationClient.KEY_LOCATION_CHANGED);
		Intent broadcast = new Intent(intent);
		broadcast.setAction(MyLocationProbe.LOCATION_BROADCAST_ACTION);
		if (location != null) {
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
		} else {
			Log.d("LocationService", "null location");
		}
	}

}
