package com.example.autovol;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;

public class MainActivity extends Activity implements DataListener {
	
	public static final String PIPELINE_NAME = "default";
	private FunfManager funfManager;
	private BasicPipeline pipeline;
	private SimpleLocationProbe locationProbe;
	private ActivityProbe activityProbe;
	private AudioProbe audioProbe;
	private Button scanNowButton;
	private TextView locationText, activityText, audioText;
	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	        funfManager = ((FunfManager.LocalBinder)service).getManager();
	        funfManager.enablePipeline(PIPELINE_NAME);
	        
	        Gson gson = funfManager.getGson();
	        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
	        activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
	        audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);
	        pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
	        locationProbe.registerPassiveListener(MainActivity.this);
	        activityProbe.registerPassiveListener(MainActivity.this);
	        
	        scanNowButton.setEnabled(true);
	        
		    Map<String, Schedule> sched = pipeline.getSchedules();
		    Log.d("MainActivity", "sched: " + sched);
	    }
	    
	    @Override
	    public void onServiceDisconnected(ComponentName name) {
	        funfManager = null;
	    }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		locationText = (TextView) findViewById(R.id.loc_text);
		activityText = (TextView) findViewById(R.id.activity_text);
		audioText = (TextView) findViewById(R.id.audio_text);
	    
	 // Forces the pipeline to scan now
	    scanNowButton = (Button) findViewById(R.id.scan_button);
	    scanNowButton.setEnabled(false);

	    scanNowButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            if (pipeline.isEnabled()) {
	                // Manually register the pipeline
	                audioProbe.registerListener(pipeline);
	                
	                //TODO: remove this
	                Executors.newScheduledThreadPool(1).schedule(new Runnable() {
	                	@Override
	                	public void run() {
	                		audioProbe.onStop();
	                	}
	                	
	                }, 20, TimeUnit.SECONDS);
	            } else {
	                Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
	            }
	        }
	    });
	    
	    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
	    
	    Log.d("MainActivity", "Connected: " + servicesConnected());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onDataCompleted(final IJsonObject arg0, final JsonElement arg1) {
		String type = arg0.get("@type").getAsString();
		Log.d("MainActivity", "data complete from " + type);
		if (type.equals("edu.mit.media.funf.probe.builtin.SimpleLocationProbe")) {
			Log.d("MainActivity", "LocationProbe completed");
			locationProbe.registerPassiveListener(this);
		} else if (type.equals("com.example.autovol.ActivityProbe")) {
			Log.d("MainActivity", "ActivityProbe completed");
			activityProbe.registerPassiveListener(this);
		} else if (type.equals("com.example.autovol.AudioProbe")) {
			Log.d("MainActivity", "AudioProbe completed");
		}
	}

	@Override
	public void onDataReceived(final IJsonObject arg0, final IJsonObject arg1) {
		String type = arg0.get("@type").getAsString();
		Log.d("MainActivity", "data received from " + type);
		if (type.equals("edu.mit.media.funf.probe.builtin.SimpleLocationProbe")) {
			final String networkType = arg1.get("mProvider").getAsString();
			final String lat = arg1.get("mLatitude").getAsString();
			final String lon = arg1.get("mLongitude").getAsString();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					locationText.setText(networkType + ": " + lat + ", " + lon);
				}
			});
		} else if (type.equals("com.example.autovol.ActivityProbe")) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					activityText.setText(arg1.get(ActivityProbe.ACTIVITY_NAME).getAsString());
				}
			});
		} else if (type.equals("com.example.autovol.AudioProbe")) {
			final int audioType = arg1.get(AudioProbe.AUDIO_TYPE).getAsInt();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					switch (audioType) {
					case AudioManager.AUDIO_SILENCE:
						audioText.setText("silence");
						break;
					case AudioManager.AUDIO_NOISE:
						audioText.setText("noise");
						break;
					case AudioManager.AUDIO_VOICE:
						audioText.setText("voice");
						break;
					case AudioManager.AUDIO_ERROR:
						audioText.setText("err");
						break;
					}
				}
			});
		}
	}
	
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Activity Recognition",
                    "Google Play services is available.");
            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            return false;
        }
    }

}
