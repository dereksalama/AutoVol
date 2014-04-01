package com.example.autovol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ClassifyAlarm extends BroadcastReceiver {
	
	private static final long FIVE_MINUTES = 5 * 60 * 1000;
	public static void scheduleRepeatedAlarm(Context c) {
		Intent intent = new Intent(c, ClassifyAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		
		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, FIVE_MINUTES,
				FIVE_MINUTES, pendingIntent);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent(context, ClassifyService.class);
		context.startService(serviceIntent);
	}

}
