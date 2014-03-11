package com.autovol.ml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.autovol.ActivityProbe;
import com.example.autovol.AudioProbe;
import com.example.autovol.MyLightSensorProbe;
import com.example.autovol.RingerVolumeProbe;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.BluetoothProbe;
import edu.mit.media.funf.probe.builtin.ProximitySensorProbe;
import edu.mit.media.funf.probe.builtin.RunningApplicationsProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;

public class CurrentState implements DataListener {
	
	public static final String NEW_STATE_BROADCAST = "new_state";
	
	Map<String, Double> values = new HashMap<String, Double>(TYPES_NEEDING_INIT.length);
	private static final String[] TYPES_NEEDING_INIT = { "lat" , "long",
		"loc_provider", "activity_confidence", "light", "distance", "wifi_count", "charging",
		"ringer"};
	private Set<String> typesWaitingInit = new HashSet<String>(Arrays.asList(TYPES_NEEDING_INIT));
	private final int numAttrs;
	
	private static final int WIFI_WAIT_PERIOD = 15;
	private int lastWifiTime = 0;
	private final Set<String> visibleWifis = new HashSet<String>(10);
	
	private final Context context;
	
	private SimpleLocationProbe locationProbe;
	private ActivityProbe activityProbe;
	private AudioProbe audioProbe;
	private BluetoothProbe bluetoothProbe;
	private MyLightSensorProbe lightProbe;
	private WifiProbe wifiProbe;
	private ProximitySensorProbe proximityProbe;
	private BatteryProbe batteryProbe;
	private RunningApplicationsProbe appProbe;
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
		"activity_bicycle",
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
	
	private static final Set<String> NON_APP_FIELDS = new HashSet<String>();
	static {
		NON_APP_FIELDS.addAll(Arrays.asList(TYPES_NEEDING_INIT));
		NON_APP_FIELDS.addAll(Arrays.asList(ACTIVITY_NAMES));
		NON_APP_FIELDS.addAll(Arrays.asList(AUDIO_NAMES));
	}
	
	private static final Set<String> APP_FIELDS = new HashSet<String>();
	
	
	public CurrentState(Instances data, Context context) {
		numAttrs = data.numAttributes();
		for (int i = 0; i < numAttrs; i++) {
			String attr = data.attribute(i).name();
			if (!typesWaitingInit.contains(attr)) {
				values.put(attr, 0.0);
			}
			
			if (!NON_APP_FIELDS.contains(attr)) {
				APP_FIELDS.add(attr);
			}
		}
		this.context = context;
	}
	
	public void enable(FunfManager funfManager) {
        Gson gson = funfManager.getGson();
        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
        activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
        audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);
        bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
        lightProbe = gson.fromJson(new JsonObject(), MyLightSensorProbe.class);
        wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
        proximityProbe = gson.fromJson(new JsonObject(),ProximitySensorProbe.class);
        batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
        appProbe = gson.fromJson(new JsonObject(), RunningApplicationsProbe.class);
        ringerProbe = gson.fromJson(new JsonObject(), RingerVolumeProbe.class);
        
        funfManager.requestData(this, locationProbe.getConfig());
        activityProbe.registerPassiveListener(this); //scheduling in probe
        funfManager.requestData(this, bluetoothProbe.getConfig());
        
        lightProbe.registerPassiveListener(this);
        audioProbe.registerPassiveListener(this);
        
        funfManager.requestData(this, wifiProbe.getConfig());
        funfManager.requestData(this, proximityProbe.getConfig());
        funfManager.requestData(this, batteryProbe.getConfig());
        funfManager.requestData(this, appProbe.getConfig());
        ringerProbe.registerPassiveListener(this);
        enabled = true;
	}
	
	public void disable(FunfManager funfManager) {
		enabled = false;
		funfManager.unrequestAllData(this);
		lightProbe.unregisterPassiveListener(this);
		activityProbe.unregisterPassiveListener(this);
		audioProbe.unregisterPassiveListener(this);
		ringerProbe.unregisterPassiveListener(this);
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public Instance toInstance() {
		Instance inst = new Instance(numAttrs);
		for (Entry<String, Double> e : values.entrySet()) {
			Attribute attr = new Attribute(e.getKey());
			inst.setValue(attr, e.getValue());
		}
		return inst;
	}

	@Override
	public void onDataCompleted(IJsonObject arg0, JsonElement arg1) {
		// do nothing
	}

	
	@Override
	public void onDataReceived(IJsonObject probe, IJsonObject data) {
		String probeType = data.get("@type").getAsString();
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
			int lux = data.get("lux").getAsInt();
			addValue("lux", lux);
		} else if (probeType.endsWith("RingerVolumeProbe")) {
			int ringerMode = data.get("ringer_mode").getAsInt();
			addValue("ringer_mode", ringerMode);
		} else if (probeType.endsWith("SimpleLocationProbe")) {
			double lat = data.get("mLatitude").getAsDouble();
			addValue("mLatitude", lat);
			
			double lon = data.get("mLongitude").getAsDouble();
			addValue("mLongitude", lon);
			
			int provider = data.get("mProvider").getAsInt();
			addValue("mProvider", provider);
		} else if (probeType.endsWith("WifiProbe")) {
			long currentTimeInSeconds = System.currentTimeMillis() / 1000;
			long timeSinceLastUpdate = currentTimeInSeconds - lastWifiTime;
			if (timeSinceLastUpdate > WIFI_WAIT_PERIOD) {
				visibleWifis.clear();
			}
			
			String bssid = data.get("BSSID").getAsString();
			visibleWifis.add(bssid);
			
			values.put("wifi_count", (double) visibleWifis.size());
		} else if (probeType.endsWith("ProximitySensorProbe")) {
			double distance = data.get("distance").getAsDouble();
			addValue("distance", distance);
		} else if (probeType.endsWith("RunningApplicationsProbe")) {
			String currentApp = data.getAsJsonObject("taskInfo")
					.getAsJsonObject("baseIntent")
					.getAsJsonObject("mComponent")
					.get("mPackage").getAsString();
			
			for (String app : APP_FIELDS) {
				if (currentApp.equals(app)) {
					values.put(currentApp, 1.0);
				} else {
					values.put(currentApp, 0.0);
				}
			}
			
		}  else {
			Log.e("CurrentState", "PROBE NOT RECOGNIZED!!");
		}
		
		//TODO: this is kinda hacky
		Intent broadcastIntent = new Intent(NEW_STATE_BROADCAST);
		context.sendBroadcast(broadcastIntent);
	}
	
	private void addValue(String probeValueName, double value) {
		String attrName = PROBE_VAL_TO_ATTR.get(probeValueName);
		typesWaitingInit.remove(attrName);
		values.put(attrName, value);
	}

}
