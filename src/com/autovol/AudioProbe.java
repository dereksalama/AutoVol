package com.autovol;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.PassiveProbe;

public class AudioProbe extends Base implements PassiveProbe {
	
	private AudioManager audioManager;
	
	private ScheduledExecutorService executor;
	
	public static final String AUDIO_TYPE = "audio_type";
	private static final int DURATION = 20;
	private static final int INTERVAL_SECONDS = 5 * 60;
	private static final int INTERVAL_MILLIS = INTERVAL_SECONDS * 1000;
	private volatile boolean recording = false;
	
	private static final String ALARM_FILTER = "com.autovol.AUDIO_START";
	
	
	private AlarmManager alarmMan;
	private PendingIntent alarmIntent;
	private AlarmReceiver alarmReceiver;
	
	private class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("AudioProbe", "alarm received");
			executor.submit(startRecording);
			executor.schedule(stopRecording, DURATION,
					TimeUnit.SECONDS);
		}
	}
	
	private final Runnable startRecording = new Runnable() {
		@Override
		public void run() {
			Log.d("AudioManager", "start");
			audioManager.prepare();
			//audioManager.startRecording();
			audioManager.start();
			recording = true;
			//executor.schedule(stopRecording, RUN_TIME, TimeUnit.SECONDS);
		}
	};
	
	private final Runnable stopRecording = new Runnable() {
		@Override
		public void run() {
			audioManager.stopRecording();
			recording = false;
			int inference = audioManager.getLastInference();
			audioManager.reset();
			
			JsonObject data = new JsonObject();
			data.addProperty(AUDIO_TYPE, inference);
			sendData(data);
			Log.d("AudioProbe", "Stopped, inference: " + inference);
			//executor.schedule(startRecording, PAUSE_TIME, TimeUnit.SECONDS);
		}
	};

	
    /**
     * Called when the probe switches from the disabled to the enabled
     * state. This is where any passive or opportunistic listeners should be
     * configured. An enabled probe should not keep a wake lock. If you need
     * the device to stay awake consider implementing a StartableProbe, and
     * using the onStart method.
     */
	@Override
    protected void onEnable() {
		super.onEnable();
		Log.d("AudioProbe", "enabled");
		audioManager = new AudioManager(android.media.MediaRecorder.AudioSource.MIC, 
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		
		executor = Executors.newScheduledThreadPool(1);
		//executor.submit(startRecording);
		
		alarmReceiver = new AlarmReceiver();
		getContext().registerReceiver(alarmReceiver, new IntentFilter(ALARM_FILTER));
		
		Intent intent = new Intent(ALARM_FILTER);
		alarmIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);
		alarmMan = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
		alarmMan.setInexactRepeating(AlarmManager.RTC_WAKEUP, 
				System.currentTimeMillis(),
				INTERVAL_MILLIS, alarmIntent);
    }

    /**
     * Called when the probe switches from the enabled state to active
     * running state. This should be used to send any data broadcasts, but
     * must return quickly. If you have any long running processes they
     * should be started on a separate thread created by this method, or
     * should be divided into short runnables that are posted to this
     * threads looper one at a time, to allow for the probe to change state.
     */
	@Override
    protected void onStart() {
		super.onStart();
    }

    /**
     * Called with the probe switches from the running state to the enabled
     * state. This method should be used to stop any running threads
     * emitting data, or remove a runnable that has been posted to this
     * thread's looper. Any passive listeners should continue running.
     */
	@Override
    protected void onStop() {
		super.onStop();
    }

    /**
     * Called with the probe switches from the enabled state to the disabled
     * state. This method should be used to stop any passive listeners
     * created in the onEnable method. This is the time to cleanup and
     * release any resources before the probe is destroyed.
     */
	@Override
    protected void onDisable() {
		if (recording) {
			Log.d("AudioProbe", "submitting stop runnable");
			executor.submit(stopRecording);
			executor.shutdown();
		} else {
			Log.d("AudioProbe", "onStop, already stopped");
			executor.shutdownNow();
		}
		audioManager = null;
		alarmMan.cancel(alarmIntent);
		getContext().unregisterReceiver(alarmReceiver);
		super.onDisable();
    }
}
