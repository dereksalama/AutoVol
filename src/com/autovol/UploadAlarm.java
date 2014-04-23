package com.autovol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class UploadAlarm extends BroadcastReceiver {
	
	private static final int HOUR_IN_MILLIS = 60 * 60 * 1000;
	private static final int DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;
	private static final int HALF_DAY_MILLIS = DAY_IN_MILLIS / 2;
	
	public static PendingIntent scheduleDailyUpload(Context c) {
		return schedule(c, System.currentTimeMillis() + HALF_DAY_MILLIS);
	}
	
	private void postpone(Context c) {
		// make sure we don't go to next day
		long timeToTrigger = System.currentTimeMillis() + HOUR_IN_MILLIS;
		schedule(c, timeToTrigger);

	}
	
	private static PendingIntent schedule(Context c, long triggerAtMillis) {
		Intent intent = new Intent(c, UploadAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		
		alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
		
		return pendingIntent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) {
		    upload(context);
		} else {
			postpone(context);
		}

	}
	
	private void upload(Context c) {
		Intent intent = new Intent(c, UploadService.class);
		c.startService(intent);
	}

}
