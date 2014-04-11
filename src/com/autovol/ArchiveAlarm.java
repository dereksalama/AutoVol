package com.autovol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ArchiveAlarm extends BroadcastReceiver {

	public static void scheduleRepeatedArchive(Context c) {
		Intent intent = new Intent(c, ArchiveAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		
		alarmManager.setInexactRepeating(AlarmManager.RTC, AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent(context, ArchiveService.class);
		context.startService(serviceIntent);
	}
}
