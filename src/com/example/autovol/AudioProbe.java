package com.example.autovol;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.media.AudioFormat;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Base;

@Schedule.DefaultSchedule(interval=300, duration=30)
public class AudioProbe extends Base {
	
	private AudioManager audioManager;
	
	private ScheduledExecutorService executor;
	
	public static final String AUDIO_TYPE = "audio_type";
	private static final int RUN_TIME = 20;
	private static final int PAUSE_TIME = 5 * 60 - 20;
	private volatile boolean recording = false;
	
	private final Runnable startRecording = new Runnable() {
		@Override
		public void run() {
			audioManager.prepare();
			//audioManager.startRecording();
			audioManager.start();
			recording = true;
			executor.schedule(stopRecording, RUN_TIME, TimeUnit.SECONDS);
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
				/*8000*/ 44100, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
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
		executor = Executors.newScheduledThreadPool(1);
		executor.submit(startRecording);
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
		if (recording) {
			executor.shutdownNow();
		} else {
			executor.shutdown();
		}

    }

    /**
     * Called with the probe switches from the enabled state to the disabled
     * state. This method should be used to stop any passive listeners
     * created in the onEnable method. This is the time to cleanup and
     * release any resources before the probe is destroyed.
     */
	@Override
    protected void onDisable() {
		super.onDisable();
		audioManager = null;
    }
}
