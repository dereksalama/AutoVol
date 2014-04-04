package com.autovol.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import com.example.autovol.ActivityProbe;
import com.example.autovol.MyLightSensorProbe;
import com.example.autovol.RingerVolumeProbe;
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

public class CurrentState implements DataListener {
	
	public static final String NEW_STATE_BROADCAST = "new_state";
	
	Map<String, Double> values = new ConcurrentHashMap<String, Double>(TYPES_NEEDING_INIT.length);
	private static final String[] TYPES_NEEDING_INIT = { "lat" , "long",
		"loc_provider", "activity_confidence", "light", "distance", "wifi_count", "charging"};
	private Set<String> typesWaitingInit;
	
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
	
	private static final Map<String, String> PROBE_VAL_TO_ATTR = 
			new HashMap<String, String>();
	static {
		PROBE_VAL_TO_ATTR.put("lux", "light");
		PROBE_VAL_TO_ATTR.put("distance", "distance");
		PROBE_VAL_TO_ATTR.put("plugged", "charging");
		PROBE_VAL_TO_ATTR.put("mLongitude", "long");
		PROBE_VAL_TO_ATTR.put("mLatitude", "lat");
		PROBE_VAL_TO_ATTR.put("mProvider", "loc_provider");
		PROBE_VAL_TO_ATTR.put("confidence", "activity_confidence");
		PROBE_VAL_TO_ATTR.put("ringer_mode", "ringer");
		PROBE_VAL_TO_ATTR.put("wifi_count", "wifi_count");
	}
	
	private static final String[] ACTIVITY_NAMES = {
		"activity_vehicle",
		"activity_bike",
		"activity_foot",
		"activity_still",
		"activity_unknown",
		"activity_tilting"
	};
	private static final String[] AUDIO_NAMES = {
		"audio_silence",
		"audio_noise", 
		"audio_voice", 
		"audio_error"
	};
	
	private static final List<String> ALL_TYPES;
	static {
		ALL_TYPES = new ArrayList<String>(15);
		//ALL_TYPES.add("day");
		ALL_TYPES.add("time");
		ALL_TYPES.addAll(Arrays.asList(TYPES_NEEDING_INIT));
		ALL_TYPES.addAll(Arrays.asList(ACTIVITY_NAMES));
		ALL_TYPES.addAll(Arrays.asList(AUDIO_NAMES));
	}
	
	private static final CurrentState INSTANCE = new CurrentState();
	public static CurrentState get() {
		return INSTANCE;
	}
	
	private CurrentState() {
		typesWaitingInit = Collections.synchronizedSet(new HashSet<String>(TYPES_NEEDING_INIT.length));
		typesWaitingInit.addAll(Arrays.asList(TYPES_NEEDING_INIT));
	}
	
	public void enable(FunfManager funfManager) {
        Gson gson = funfManager.getGson();
        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
        activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
        //audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);
        lightProbe = gson.fromJson(new JsonObject(), MyLightSensorProbe.class);
        wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
        proximityProbe = gson.fromJson(new JsonObject(),ProximitySensorProbe.class);
        batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
        ringerProbe = gson.fromJson(new JsonObject(), RingerVolumeProbe.class);
        
        funfManager.requestData(this, locationProbe.getConfig());
        activityProbe.registerPassiveListener(this); //scheduling in probe
        
        lightProbe.registerPassiveListener(this);
        //audioProbe.registerPassiveListener(this);
        
        funfManager.requestData(this, wifiProbe.getConfig());
        funfManager.requestData(this, proximityProbe.getConfig());
        funfManager.requestData(this, batteryProbe.getConfig());
        ringerProbe.registerPassiveListener(this);
        enabled = true;
	}
	
	public void disable(FunfManager funfManager) {
		enabled = false;
		funfManager.unrequestAllData(this);
		lightProbe.unregisterPassiveListener(this);
		activityProbe.unregisterPassiveListener(this);
		ringerProbe.unregisterPassiveListener(this);
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean dataIsReady() {
		return typesWaitingInit.size() == 0;
	}
	
	public String requestParamString() {
		// update time & date info
		Calendar cal = Calendar.getInstance();
		
		/* Skipping day of week for now
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int convertedDayOfWeek = 0;
		if (dayOfWeek == Calendar.SUNDAY) {
			convertedDayOfWeek = 6;
		} else {
			convertedDayOfWeek = dayOfWeek - 2;
		}
		values.put("day", (double) convertedDayOfWeek);
		*/

		int minuteIntoDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
		values.put("time", (double) minuteIntoDay);
		
		StringBuilder builder = new StringBuilder();
		for (String type : ALL_TYPES) {
			Double val = values.get(type);
			builder.append(type + "=");
			if (val != null) {
				builder.append(val);
			} else {
				builder.append(0.0);
			}
			builder.append("&");
		}
		builder.deleteCharAt(builder.length() - 1); // remove trailing &
		return builder.toString();
	}

	@Override
	public void onDataCompleted(IJsonObject arg0, JsonElement arg1) {
		// do nothing
	}

	
	@Override
	public void onDataReceived(IJsonObject probe, IJsonObject data) {
		JsonElement type = probe.get("@type");
		String probeType = type.getAsString();
		Log.d("CurrentState", "data from " + probeType);
		if (probeType.endsWith("ActivityProbe")) {
			int confidence = data.get("confidence").getAsInt();
			addValue("confidence", confidence);
			
			int activityType = data.get("activity_type").getAsInt();
			for (int i = 0; i < ACTIVITY_NAMES.length; i++) {
				if (activityType == i) {
					values.put(ACTIVITY_NAMES[i], 1.0);
				} else {
					values.put(ACTIVITY_NAMES[i], 0.0);
				}
			}
		} else if (probeType.endsWith("AudioProbe")) {
			int audioType = data.get("audio_type").getAsInt();
			for (int i = 0; i < AUDIO_NAMES.length; i++) {
				if (audioType == i) {
					values.put(AUDIO_NAMES[i], 1.0);
				} else {
					values.put(AUDIO_NAMES[i], 0.0);
				}
			}
		} else if (probeType.endsWith("MyLightSensorProbe")) {
			float lux = data.get("lux").getAsFloat();
			addValue("lux", lux);
		} else if (probeType.endsWith("RingerVolumeProbe")) {
			int ringerMode = data.get("ringer_mode").getAsInt();
			addValue("ringer_mode", ringerMode);
		} else if (probeType.endsWith("SimpleLocationProbe")) {
			double lat = data.get("mLatitude").getAsDouble();
			addValue("mLatitude", lat);
			
			double lon = data.get("mLongitude").getAsDouble();
			addValue("mLongitude", lon);
			
			String provider = data.get("mProvider").getAsString();
			if (provider.equals("gps")) {
				addValue("mProvider", 1);
			} else {
				addValue("mProvider", 0);
			}
			
		} else if (probeType.endsWith("WifiProbe")) {
			long currentTimeInSeconds = System.currentTimeMillis() / 1000;
			long timeSinceLastUpdate = currentTimeInSeconds - lastWifiTime;
			lastWifiTime = currentTimeInSeconds;
			if (timeSinceLastUpdate > WIFI_WAIT_PERIOD) {
				visibleWifis.clear();
			}
			
			String bssid = data.get("BSSID").getAsString();
			visibleWifis.add(bssid);
			
			addValue("wifi_count", visibleWifis.size());
		} else if (probeType.endsWith("ProximitySensorProbe")) {
			double distance = data.get("distance").getAsDouble();
			addValue("distance", distance);
		} else if (probeType.endsWith("BatteryProbe")) {
			double charging = data.get("plugged").getAsDouble();
			addValue("plugged", charging);
		}  else {
			Log.e("CurrentState", "PROBE NOT RECOGNIZED!! -> " + probeType);
		}
		

		if (typesWaitingInit.isEmpty()){
			//TODO: this is kinda hacky
			Log.d("CurrentState", "State updated");
		} else {
			Log.d("CurrentState", "State still incomplete. Need: " + typesWaitingInit.toString());
		}
	}
	
	private void addValue(String probeValueName, double value) {
		String attrName = PROBE_VAL_TO_ATTR.get(probeValueName);
		typesWaitingInit.remove(attrName);
		values.put(attrName, value);
	}

}
