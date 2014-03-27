package com.autovol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.PassiveProbe;

public class RingerVolumeProbe extends Base implements PassiveProbe {
	
	public static final String RINGER_MODE = "ringer_mode";
	
	private BroadcastReceiver volumeChangedReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			int ringerMode = intent.getIntExtra(android.media.AudioManager.EXTRA_RINGER_MODE, 0);
			Log.d("RingerVolumeProbe", "mode change received: " + ringerMode);
			JsonObject data = new JsonObject();
			data.addProperty(RINGER_MODE, ringerMode);
			sendData(data);
			
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
		IntentFilter filter = new IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION);
		getContext().registerReceiver(volumeChangedReceiver, filter);
		
		// get init volume
		android.media.AudioManager audioMan = (android.media.AudioManager) getContext().
				getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = audioMan.getRingerMode();
		
		Log.d("RingerVolumeProbe", "init ringer mode: " + ringerMode);
		JsonObject data = new JsonObject();
		data.addProperty(RINGER_MODE, ringerMode);
		sendData(data);

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
		getContext().unregisterReceiver(volumeChangedReceiver);
    }

}
