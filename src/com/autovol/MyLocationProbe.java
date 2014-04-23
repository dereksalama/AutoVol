package com.autovol;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;

public class MyLocationProbe extends Base implements ConnectionCallbacks,
		OnConnectionFailedListener {
	
	private LocationClient mLocClient;
	private PendingIntent mLocPendingIntent;
	
	public static final String LOCATION_BROADCAST_ACTION = "loc_broadcast";
	
private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			JsonObject data = new JsonObject();
			Log.d("MyLocationProbe", "Data received");
			
			Location location = intent.getParcelableExtra(LocationClient.KEY_LOCATION_CHANGED);
			data.addProperty("lat", location.getLatitude());
			data.addProperty("lon", location.getLongitude());
			data.addProperty("provider", location.getProvider());
			
			sendData(data);
		}
	};
	
	@Override
	protected void onEnable() {
		super.onEnable();
		
		mLocClient = new LocationClient(getContext(), this, this);
		
		Intent intent = new Intent(getContext(),
				LocationService.class);
		
		mLocPendingIntent = PendingIntent.getService(getContext(), 0, intent, 0);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(LOCATION_BROADCAST_ACTION);
		LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);
		
		mLocClient.connect();
	}
	
	@Override
	public void onDisable() {
		LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
		mLocClient.disconnect();
		super.onDisable();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.d("MyLocationProbe", "play services connected");
		
		LocationRequest req = LocationRequest.create();
		req.setInterval(1000 * 60 * 5); // TODO
		req.setFastestInterval(1000 * 15);
		req.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		
		mLocClient.requestLocationUpdates(req, mLocPendingIntent);

	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}

}
