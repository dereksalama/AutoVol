package com.autovol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ClassifyAlarm extends BroadcastReceiver {
	
	private static PendingIntent pendingIntent;
	private static volatile boolean enabled = false;
	
	private static final long FIVE_MINUTES = 5 * 60 * 1000;
	
	public synchronized static void scheduleRepeatedAlarm(Context c) {
		if (!enabled) {
			Intent intent = new Intent(c, ClassifyAlarm.class);
			pendingIntent = PendingIntent.getBroadcast(c, 0, intent, 0);
			AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
			
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, FIVE_MINUTES,
					FIVE_MINUTES, pendingIntent);
		}
	}
	
	public synchronized static void cancelAlarm(Context c) {
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("ClassifyAlarm", "intent received");
		Intent serviceIntent = new Intent(context, ClassifyService.class);
		context.startService(serviceIntent);
	}

}
