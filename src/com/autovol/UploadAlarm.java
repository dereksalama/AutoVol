package com.autovol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UploadAlarm extends BroadcastReceiver {
	
	private static final int HOUR_IN_MILLIS = 60 * 60 * 1000;
	private static final int DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;
	private static final int QUARTER_DAY_MILLIS = DAY_IN_MILLIS / 4;
		
	public static PendingIntent schedule(Context c) {
		Intent intent = new Intent(c, UploadAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		
		alarmManager.setInexactRepeating(AlarmManager.RTC, QUARTER_DAY_MILLIS, QUARTER_DAY_MILLIS, pendingIntent);

		return pendingIntent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		upload(context);
	}
	
	private void upload(Context c) {
		Intent intent = new Intent(c, UploadService.class);
		c.startService(intent);
	}

}
