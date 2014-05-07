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
	private static final String LAST_CLUSTER = "last_cluster";
	private static final String ENABLE_COLLECTION = "enable_collection";
	private static final String ENABLE_CLASSIFY = "enable_classify";
	
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
	
	public static Integer getLastCluster(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		return sp.getInt(LAST_CLUSTER, -1);
	}
	
	public static void setLastCluster(Context c, Integer cluster) {
		SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor edit = sp.edit();
		edit.putInt(LAST_CLUSTER, cluster);
		edit.commit();
	}
}
