package com.autovol;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {
	public static final String PREFS_NAME = "MyPrefsFile";
	
	private static final String PREF_USE_LOCAL_HOST = "use_local_host";
	
	// Local host
	private static final String LOCAL_BASE_URL = "http://10.0.1.17:8080";
	
	// AWS host
	private static final String AWS_BASE_URL = "http://ec2-54-186-90-159.us-west-2.compute.amazonaws.com:8080";
	
	public static void setUseLocalHost(boolean useLocalHost, Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putBoolean(PREF_USE_LOCAL_HOST, useLocalHost);
		edit.commit();
	}
	
	public static boolean useLocalHost(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getBoolean(PREF_USE_LOCAL_HOST, false);
	}
	
	public static String getBaseUrl(Context c) {
		boolean useLocal = useLocalHost(c);
		if (useLocal) {
			return LOCAL_BASE_URL;
		} else {
			return AWS_BASE_URL;
		}
	}
}
