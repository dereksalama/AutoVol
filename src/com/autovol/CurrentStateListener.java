package com.autovol;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule.BasicSchedule;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.ProximitySensorProbe;
import edu.mit.media.funf.probe.builtin.ScreenProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;

public class CurrentStateListener implements DataListener {
	public static final String SAVED_FILE = "saved_observations";
	
	private static final int WIFI_WAIT_PERIOD = 15;
	private volatile long lastWifiTime = 0;
	private final Set<String> visibleWifis = Collections.synchronizedSet(new HashSet<String>(10));
	
	private volatile long lastScreenOnTime = System.currentTimeMillis();
	private volatile long lastObjectSaveMillis = System.currentTimeMillis();
	private static final long OBJ_SAVE_INTERVAL = 15 * 1000;
	
	//private SimpleLocationProbe locationProbe;
	private MyLocationProbe myLocProbe;
	private ActivityProbe activityProbe;
	private MyLightSensorProbe lightProbe;
	private WifiProbe wifiProbe;
	private ProximitySensorProbe proximityProbe;
	private BatteryProbe batteryProbe;
	private RingerVolumeProbe ringerProbe;
	private AudioMagnitudeProbe audioMagProbe;
	private ScreenProbe screenProbe;
	
	private volatile boolean enabled = false;
	
	private CurrentStateData currentState;
	private List<CurrentStateData> savedStates = Collections.synchronizedList(
			new LinkedList<CurrentStateData>());
	
	
	private static final String[] TYPES_NEEDING_INIT = { "lat" , "lon",
		"loc_provider", "activity_confidence", "light", "distance", "wifi_count", "charging",
		"activity_type", "audio_mag", "screen_on", "screen_last_on", "ringer"};

	private Set<String> typesWaitingInit;
	
	private PendingIntent archiveIntent;
	private PendingIntent uploadIntent;
	
	private static final CurrentStateListener INSTANCE = new CurrentStateListener();
	public static CurrentStateListener get() {
		return INSTANCE;
	}
	
	private CurrentStateListener() {
		currentState = new CurrentStateData();
		typesWaitingInit = Collections.synchronizedSet(new HashSet<String>(TYPES_NEEDING_INIT.length));
		typesWaitingInit.addAll(Arrays.asList(TYPES_NEEDING_INIT));
	}
	
	public boolean dataIsReady() {
		return typesWaitingInit.isEmpty();
	}
	
	public String currentStateJson() {
		if (savedStates.isEmpty()) {
			return null;
		}
		CurrentStateData copy = new CurrentStateData(savedStates.get(savedStates.size() - 1));
		Gson gson = new Gson();
		return gson.toJson(copy);
	}
	
	public String recentStatesJson(int numStates) {
		if (savedStates.size() < numStates) {
			return null;
		}
		
		synchronized(savedStates) {
			List<CurrentStateData> sublist = savedStates.subList(
					savedStates.size() - numStates, savedStates.size());
			List<CurrentStateData> arraySub = new ArrayList<CurrentStateData>(sublist);
			Gson gson = new Gson();
			Type type = new TypeToken<List<CurrentStateData>>(){}.getType();
			return gson.toJson(arraySub, type);
		}
	}
	
	public int minutesIntoDay() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
	}
	
	public int dayOfWeek() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.DAY_OF_WEEK);
	}
	
	public void enable(FunfManager funfManager, Context c) {
		if (!enabled) {
	        Gson gson = funfManager.getGson();
	        //locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
	        myLocProbe = gson.fromJson(new JsonObject(), MyLocationProbe.class);
	        activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
	        //audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);
	        //bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
	        lightProbe = gson.fromJson(new JsonObject(), MyLightSensorProbe.class);
	        wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
	        proximityProbe = gson.fromJson(new JsonObject(),ProximitySensorProbe.class);
	        batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
	        ringerProbe = gson.fromJson(new JsonObject(), RingerVolumeProbe.class);
	        audioMagProbe = gson.fromJson(new JsonObject(), AudioMagnitudeProbe.class);
	        screenProbe = gson.fromJson(new JsonObject(), ScreenProbe.class);
	        
	        BasicSchedule locSched = new BasicSchedule();
	        locSched.setInterval(BigDecimal.valueOf(300));
	        locSched.setOpportunistic(true);
	        locSched.setStrict(false);
	       // funfManager.requestData(this, locationProbe.getConfig(), locSched);
	        
	        activityProbe.registerPassiveListener(this); //scheduling in probe
	        //funfManager.requestData(this, bluetoothProbe.getConfig());
	        
	        lightProbe.registerPassiveListener(this);
	        audioMagProbe.registerPassiveListener(this);
	        myLocProbe.registerPassiveListener(this);
	        //audioProbe.registerPassiveListener(this);
	        
	        funfManager.requestData(this, wifiProbe.getConfig());
	        funfManager.requestData(this, proximityProbe.getConfig());
	        funfManager.requestData(this, batteryProbe.getConfig());

	        funfManager.requestData(this, screenProbe.getConfig());
	        ringerProbe.registerPassiveListener(this);
	        
	        archiveIntent = ArchiveAlarm.scheduleRepeatedArchive(c);
	        uploadIntent = UploadAlarm.scheduleDailyUpload(c);
	        enabled = true;
		}
	}
	
	public void disable(FunfManager funfManager, Context c) {
		if (enabled) {
			enabled = false;
			funfManager.unrequestAllData(this);
			lightProbe.unregisterPassiveListener(this);
			activityProbe.unregisterPassiveListener(this);
			//audioProbe.unregisterPassiveListener(this);
			ringerProbe.unregisterPassiveListener(this);
			
			((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(archiveIntent);
			((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(uploadIntent);
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
		} /*else if (probeType.endsWith("SimpleLocationProbe")) {
			double lat = data.get("mLatitude").getAsDouble();
			typesWaitingInit.remove("lat");
			currentState.setLat(lat);
			
			double lon = data.get("mLongitude").getAsDouble();
			typesWaitingInit.remove("lon");
			currentState.setLon(lon);
			
			String provider = data.get("mProvider").getAsString();
			typesWaitingInit.remove("loc_provider");
			currentState.setLocProvider(provider);
			
		} */ else if (probeType.endsWith("MyLocationProbe")) {
			double lat = data.get("lat").getAsDouble();
			typesWaitingInit.remove("lat");
			currentState.setLat(lat);
			
			double lon = data.get("lon").getAsDouble();
			typesWaitingInit.remove("lon");
			currentState.setLon(lon);
			
			String provider = data.get("provider").getAsString();
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
		} else if (probeType.endsWith("ScreenProbe")) {
			typesWaitingInit.remove("screen_on");
			typesWaitingInit.remove("screen_last_on");
			String screenState = data.get("screenOn").getAsString();
			if (screenState.equalsIgnoreCase("TRUE")) {
				lastScreenOnTime = System.currentTimeMillis();
			}
			currentState.setScreenLastOn(System.currentTimeMillis() - lastScreenOnTime);
			currentState.setScreenOn(screenState);
		} else if (probeType.endsWith("AudioMagnitudeProbe")) {
			typesWaitingInit.remove("audio_mag");
			int mag = data.get("audio_mag").getAsInt();
			currentState.setAudioMag(mag);
		}  else {
			Log.e("CurrentState", "PROBE NOT RECOGNIZED!! -> " + probeType);
		}
		
		if (dataIsReady()){
			Log.d("CurrentState", "State updated");
			synchronized(this) {
				if (System.currentTimeMillis() >= lastObjectSaveMillis + OBJ_SAVE_INTERVAL) {
					Log.d("CurrentState", "Time elapsed, creating new data obj");
					lastObjectSaveMillis = System.currentTimeMillis();
					currentState.setTime(minutesIntoDay());
					currentState.setDay(dayOfWeek());
					currentState.setScreenLastOn(System.currentTimeMillis() - lastScreenOnTime); 
					savedStates.add(currentState);
					CurrentStateData next = new CurrentStateData(currentState);
					currentState = next;
				} else {
					Log.d("CurrentState", "Not enough time passed");
				}
			}
		} else {
			Log.d("CurrentState", "State still incomplete. Need: " + typesWaitingInit.toString());
		}
	}
	
	public synchronized void saveRecentObservations(Context c) {
		if (savedStates.size() <= ClassifyService.NUM_VECTORS_TO_AVG) {
			Log.d("CurrentStateListener", "not enough states");
			return;
		}
		int numStatesToSave = savedStates.size() - ClassifyService.NUM_VECTORS_TO_AVG;
		List<CurrentStateData> mostRecent = savedStates.subList(numStatesToSave, 
				savedStates.size());
		List<CurrentStateData> toSave = savedStates.subList(0, numStatesToSave);
		
		savedStates = mostRecent;
		
		OutputStream outputStream;
		if (toSave.isEmpty()) {
			Log.d("CurrentStateListener", "no states to save");
			return;
		} else {
			Log.d("CurrentStateListener", "saving " + toSave.size());
		}
		
		Gson gson = new Gson();
		String json =  gson.toJson(toSave);
		int closingBracket = json.lastIndexOf(']');
		String editedJson = json.substring(0, closingBracket);
		try {
			if (c.getFileStreamPath(SAVED_FILE).exists()) {
				editedJson = new StringBuilder(editedJson)
					.deleteCharAt(0)
					.insert(0, ',')
					.toString();
			}
			outputStream = new BufferedOutputStream(
					c.openFileOutput(SAVED_FILE, Context.MODE_APPEND));
			
			outputStream.write(editedJson.getBytes());
			outputStream.close();
		} catch (FileNotFoundException e) {
			Log.d("CurrentStateListener", "archive file not found" + e.getMessage());
		} catch (IOException e) {
			Log.d("CurrentStateListener", "archive io exception" + e.getMessage());
		}
	}
}
