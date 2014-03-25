package com.autovol.ml;

import java.util.Map;

import com.google.gson.Gson;


public class CurrentStateData {
	
	private double time;
	private double lat;
	private double lon; // "long"
	private String locProvider;
	private double light;
	private double distance;
	private double wifiCount;
	private String charging;
	private String activityType;
	private double activityConfidence;
	private String ringer;
	
	//TODO: audio?
	
	public CurrentStateData() {} // no args constructor for gson
	
	public static CurrentStateData fromValuesDict(Map<String, String> nominalValues,
			Map<String, Double> numericalValues) {
		// TODO: size check
		CurrentStateData data = new CurrentStateData();
		data.time = numericalValues.get("time");
		data.lat = numericalValues.get("lat");
		data.lon = numericalValues.get("lon");
		data.light = numericalValues.get("light");
		data.distance = numericalValues.get("distance");
		data.wifiCount = numericalValues.get("wifiCount");
		data.activityConfidence = numericalValues.get("activity_confidence");
		
		data.locProvider = nominalValues.get("loc_provider");
		data.charging = nominalValues.get("charging");
		data.activityType = nominalValues.get("activity_type");
		data.ringer = nominalValues.get("ringer"); //TODO: what about target samples?
		
		return data;
	}
	
	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
	public static CurrentStateData fromJson(String obj) {
		Gson gson = new Gson();
		return gson.fromJson(obj, CurrentStateData.class);
	}
}
