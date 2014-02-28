package com.example.autovol;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.media.AudioFormat;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;

public class AudioProbe extends Base {
	
	private AudioManager audioManager;
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public static final String AUDIO_TYPE = "audio_type";

	
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
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				Log.d("AudioProbe", "Audio runnable run");
				audioManager.prepare();
				audioManager.start(getContext());
			}
			
		};
		executor.submit(r);
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
		executor.execute(new Runnable() {
			@Override
			public void run() {
				audioManager.stopRecording();
			}
		});
		int inference = audioManager.getLastInference();
		audioManager.reset();
		JsonObject data = new JsonObject();
		data.addProperty(AUDIO_TYPE, inference);
		sendData(data);
		Log.d("AudioProbe", "Stopped, inference: " + inference);
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
