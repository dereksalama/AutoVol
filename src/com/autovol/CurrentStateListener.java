package com.autovol;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.util.Log;

import com.autovol.CurrentStateData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.ProximitySensorProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;

public class CurrentStateListener implements DataListener {
	public static final String SAVED_FILE = "saved_observations";
	
	private static final int WIFI_WAIT_PERIOD = 15;
	private volatile long lastWifiTime = 0;
	private final Set<String> visibleWifis = Collections.synchronizedSet(new HashSet<String>(10));
	
	private SimpleLocationProbe locationProbe;
	private ActivityProbe activityProbe;
	private MyLightSensorProbe lightProbe;
	private WifiProbe wifiProbe;
	private ProximitySensorProbe proximityProbe;
	private BatteryProbe batteryProbe;
	private RingerVolumeProbe ringerProbe;
	
	private volatile boolean enabled = false;
	
	private CurrentStateData currentState;
	private Set<CurrentStateData> savedStates = Collections.synchronizedSet(
			new HashSet<CurrentStateData>());
	
	
	private static final String[] TYPES_NEEDING_INIT = { "lat" , "lon",
		"loc_provider", "activity_confidence", "light", "distance", "wifi_count", "charging",
		"activity_type", "ringer"};
	private Set<String> typesWaitingInit;
	
	private static final CurrentStateListener INSTANCE = new CurrentStateListener();
	public static CurrentStateListener get() {
		return INSTANCE;
	}
	
	private CurrentStateListener() {
		currentState = new CurrentStateData();
		currentState.setTime(minutesIntoDay());
		typesWaitingInit = Collections.synchronizedSet(new HashSet<String>(TYPES_NEEDING_INIT.length));
		typesWaitingInit.addAll(Arrays.asList(TYPES_NEEDING_INIT));
	}
	
	public boolean dataIsReady() {
		return typesWaitingInit.isEmpty();
	}
	
	public String currentStateJson() {
		Gson gson = new Gson();
		return gson.toJson(currentState, CurrentStateData.class);
	}
	
	public int minutesIntoDay() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
	}
	
	public void enable(FunfManager funfManager) {
		if (!enabled) {
	        Gson gson = funfManager.getGson();
	        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
	        activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
	        //audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);
	        //bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
	        lightProbe = gson.fromJson(new JsonObject(), MyLightSensorProbe.class);
	        wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
	        proximityProbe = gson.fromJson(new JsonObject(),ProximitySensorProbe.class);
	        batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
	        ringerProbe = gson.fromJson(new JsonObject(), RingerVolumeProbe.class);
	        
	        funfManager.requestData(this, locationProbe.getConfig());
	        activityProbe.registerPassiveListener(this); //scheduling in probe
	        //funfManager.requestData(this, bluetoothProbe.getConfig());
	        
	        lightProbe.registerPassiveListener(this);
	        //audioProbe.registerPassiveListener(this);
	        
	        funfManager.requestData(this, wifiProbe.getConfig());
	        funfManager.requestData(this, proximityProbe.getConfig());
	        funfManager.requestData(this, batteryProbe.getConfig());
	        ringerProbe.registerPassiveListener(this);
	        enabled = true;
		}
	}
	
	public void disable(FunfManager funfManager) {
		if (enabled) {
			enabled = false;
			funfManager.unrequestAllData(this);
			lightProbe.unregisterPassiveListener(this);
			activityProbe.unregisterPassiveListener(this);
			//audioProbe.unregisterPassiveListener(this);
			ringerProbe.unregisterPassiveListener(this);
		}
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void onDataCompleted(IJsonObject arg0, JsonElement arg1) {
		// do nothing
	}

	private static final String[] ACTIVITY_NAMES = {
		"activity_vehicle",
		"activity_bike",
		"activity_foot",
		"activity_still",
		"activity_unknown",
		"activity_tilting"
	};
	
	@Override
	public void onDataReceived(IJsonObject probe, IJsonObject data) {
		JsonElement type = probe.get("@type");
		String probeType = type.getAsString();
		Log.d("CurrentState", "data from " + probeType);
		if (probeType.endsWith("ActivityProbe")) {
			int confidence = data.get("confidence").getAsInt();
			typesWaitingInit.remove("activity_confidence");
			currentState.setActivityConfidence(confidence);
			
			int activityType = data.get("activity_type").getAsInt();
			String activityString = ACTIVITY_NAMES[activityType];
			typesWaitingInit.remove("activity_type");
			currentState.setActivityType(activityString);
			
		} else if (probeType.endsWith("MyLightSensorProbe")) {
			float lux = data.get("lux").getAsFloat();
			typesWaitingInit.remove("light");
			currentState.setLight(lux);
		} else if (probeType.endsWith("RingerVolumeProbe")) {
			int ringerMode = data.get("ringer_mode").getAsInt();
			typesWaitingInit.remove("ringer");
			if (ringerMode == 0) {
				currentState.setRinger("silent");
			} else if (ringerMode == 1) {
				currentState.setRinger("vibrate");
			} else {
				currentState.setRinger("normal");
			}
		} else if (probeType.endsWith("SimpleLocationProbe")) {
			double lat = data.get("mLatitude").getAsDouble();
			typesWaitingInit.remove("lat");
			currentState.setLat(lat);
			
			double lon = data.get("mLongitude").getAsDouble();
			typesWaitingInit.remove("lon");
			currentState.setLon(lon);
			
			String provider = data.get("mProvider").getAsString();
			typesWaitingInit.remove("loc_provider");
			currentState.setLocProvider(provider);
			
		} else if (probeType.endsWith("WifiProbe")) {
			long currentTimeInSeconds = System.currentTimeMillis() / 1000;
			long timeSinceLastUpdate = currentTimeInSeconds - lastWifiTime;
			lastWifiTime = currentTimeInSeconds;
			if (timeSinceLastUpdate > WIFI_WAIT_PERIOD) {
				visibleWifis.clear();
			}
			
			String bssid = data.get("BSSID").getAsString();
			visibleWifis.add(bssid);
			
			typesWaitingInit.remove("wifi_count");
			currentState.setWifiCount(visibleWifis.size());
		} else if (probeType.endsWith("ProximitySensorProbe")) {
			double distance = data.get("distance").getAsDouble();
			typesWaitingInit.remove("distance");
			currentState.setDistance(distance);
		} else if (probeType.endsWith("BatteryProbe")) {
			double charging = data.get("plugged").getAsDouble();
			typesWaitingInit.remove("charging");
			if (charging == 0) {
				currentState.setCharging("false");
			} else {
				currentState.setCharging("true");
			}
		}  else {
			Log.e("CurrentState", "PROBE NOT RECOGNIZED!! -> " + probeType);
		}
		
		if (dataIsReady()){
			Log.d("CurrentState", "State updated");
			int currentTime = minutesIntoDay();
			if (currentState.getTime() < currentTime) {
				Log.d("CurrentState", "Minute elapsed, creating new data obj");
				savedStates.add(currentState);
				CurrentStateData next = new CurrentStateData(currentState);
				next.setTime(currentTime);
				currentState = next;
			}
		} else {
			Log.d("CurrentState", "State still incomplete. Need: " + typesWaitingInit.toString());
		}
	}
	
	// Convert all states in memory to GSON serialization string
	private String serializeRecentObservations() {
		Gson gson = new Gson();
		String result =  gson.toJson(savedStates);
		return result;
	}
	
	public void saveRecentObservations(Context c) {
		OutputStream outputStream;
		if (savedStates.isEmpty()) {
			return;
		}
		try {
			outputStream = new BufferedOutputStream(
					c.openFileOutput(SAVED_FILE, Context.MODE_APPEND));
			String json = serializeRecentObservations();
			outputStream.write(json.getBytes());
			outputStream.close();
			savedStates.clear();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
