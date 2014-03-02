package com.example.autovol;

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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.BluetoothProbe;
import edu.mit.media.funf.probe.builtin.LightSensorProbe;
import edu.mit.media.funf.probe.builtin.ProximitySensorProbe;
import edu.mit.media.funf.probe.builtin.RunningApplicationsProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;

public class MainActivity extends Activity implements DataListener {
	
	public static final String PIPELINE_NAME = "default";
	private FunfManager funfManager;
	private BasicPipeline pipeline;
	
	private SimpleLocationProbe locationProbe;
	private ActivityProbe activityProbe;
	private AudioProbe audioProbe;
	private BluetoothProbe bluetoothProbe;
	private LightSensorProbe lightProbe;
	private WifiProbe wifiProbe;
	private ProximitySensorProbe proximityProbe;
	private BatteryProbe batteryProbe;
	private RunningApplicationsProbe appProbe;
	
	private Button scanNowButton;
	private TextView locationText, activityText, audioText;
	private CheckBox enabledBox;
	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	    	Log.d("MainActivity", "onServiceConnected");
	        funfManager = ((FunfManager.LocalBinder)service).getManager();
	        pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
	        funfManager.enablePipeline(PIPELINE_NAME);
	        
	        /*
	        Gson gson = funfManager.getGson();
	        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
	        activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
	        audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);
	        bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
	        lightProbe = gson.fromJson(new JsonObject(), LightSensorProbe.class);
	        wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
	        proximityProbe = gson.fromJson(new JsonObject(),ProximitySensorProbe.class);
	        batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
	        appProbe = gson.fromJson(new JsonObject(), RunningApplicationsProbe.class);

	        locationProbe.registerListener(pipeline);
	        activityProbe.registerListener(pipeline);
	        audioProbe.registerListener(pipeline);
	        bluetoothProbe.registerListener(pipeline);
	        lightProbe.registerListener(pipeline);
	        wifiProbe.registerListener(pipeline);
	        proximityProbe.registerListener(pipeline);
	        batteryProbe.registerListener(pipeline);
	        appProbe.registerListener(pipeline);
	        */
	        
	        scanNowButton.setEnabled(true);
	        enabledBox.setChecked(pipeline.isEnabled());
	        enabledBox.setEnabled(true);
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
		enabledBox = (CheckBox) findViewById(R.id.enabled_checkbox);
		enabledBox.setEnabled(false);
		enabledBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (funfManager != null) {
                    if (isChecked) {
                        funfManager.enablePipeline(PIPELINE_NAME);
                        pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
                    } else {
                        funfManager.disablePipeline(PIPELINE_NAME);
                    }
                }
			}
		});
	    
	 // Forces the pipeline to scan now
	    scanNowButton = (Button) findViewById(R.id.scan_button);
	    scanNowButton.setEnabled(false);

	    scanNowButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            if (pipeline.isEnabled()) {
	                audioProbe.registerListener(MainActivity.this);
	            } else {
	                Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
	            }
	        }
	    });
	    
	    Button stopButton = (Button) findViewById(R.id.stop_button);

	    stopButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            if (pipeline.isEnabled()) {
	                audioProbe.unregisterListener(MainActivity.this);
	            } else {
	                Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
	            }
	        }
	    });
	    
	    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
	    
	    Log.d("MainActivity", "Play Services Connected: " + servicesConnected());
	}
	
	@Override
	protected void onDestroy() {
		funfManager.disablePipeline(PIPELINE_NAME);
		unbindService(funfManagerConn);
		super.onDestroy();
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
