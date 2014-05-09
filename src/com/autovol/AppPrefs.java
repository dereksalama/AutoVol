package com.autovol;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {
	public static final String PREFS_NAME = "MyPrefsFile";
	
	private static final String PREF_USE_LOCAL_HOST = "use_local_host";
	private static final String USER_ACCT_HASH = "user_acct_hash";
	private static final String USER_ACCT_DNE = "no_acct";
	private static final String CONTROL_RINGER = "set_ringer";
	private static final String TEMP_CONTROL_DISABLE = "temp_disable";
	
	// breifly set this so we don't think it is user overriding
	private static final String AUTOVOL_SETTING_VOLUME = "autovol_setting_volume";
	
	private static final String DEFAULT_RING_VOLUME = "default_ring_volume";

	private static final String ENABLE_COLLECTION = "enable_collection";
	private static final String ENABLE_CLASSIFY = "enable_classify";
	private static final String LAST_CLASSIFICATION = "last_classification";
	
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
	
	public static String getAccountHash(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getString(USER_ACCT_HASH, USER_ACCT_DNE);
	}
	
	public static boolean isFreshInstall(Context c) {
		String acct = getAccountHash(c);
		return acct.equals(USER_ACCT_DNE);
	}
	
	public static void setAccountHash(Context c, String user) throws UnsupportedEncodingException { 
		String hash = new String(Hex.encodeHex(DigestUtils.sha1(user)));
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		
		SharedPreferences.Editor edit = sp.edit();
		edit.putString(USER_ACCT_HASH, hash);
		edit.commit();
	}
	
	public static boolean isControlRinger(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getBoolean(CONTROL_RINGER, false);
	}
	
	public static void setControlRinger(boolean controlRinger, Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putBoolean(CONTROL_RINGER, controlRinger);
		edit.commit();
	}
	
	public static boolean isTempDisable(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getBoolean(TEMP_CONTROL_DISABLE, false);
	}
	
	public static void setTempDisable(boolean set, Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putBoolean(TEMP_CONTROL_DISABLE, set);
		edit.commit();
	}
	
	public static boolean isEnableCollection(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getBoolean(ENABLE_COLLECTION, false);
	}
	
	public static void setEnableCollection(boolean enable, Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putBoolean(ENABLE_COLLECTION, enable);
		edit.commit();
	}
	
	public static boolean isEnableClassify(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getBoolean(ENABLE_CLASSIFY, false);
	}
	
	public static void setEnableClassify(boolean enable, Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putBoolean(ENABLE_CLASSIFY, enable);
		edit.commit();
	}
	
	public static boolean isAutovolSettingVolume(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getBoolean(AUTOVOL_SETTING_VOLUME, false);
	}
	
	public static void setAutovolSettingVolume(boolean enable, Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putBoolean(AUTOVOL_SETTING_VOLUME, enable);
		edit.commit();
	}
	
	public static String getLastClassification(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getString(LAST_CLASSIFICATION, "silent");
	}
	
	public static void setLastClassification(Context c, String classification) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putString(LAST_CLASSIFICATION, classification);
		edit.commit();
	}
	
	public static int getDefaultRingerVolume(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getInt(DEFAULT_RING_VOLUME, 1);
	}
	
	public static void setDefaultRingerVolume(Context c, int volume) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putInt(DEFAULT_RING_VOLUME, volume);
		edit.commit();
	}
	

}
