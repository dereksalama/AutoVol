package com.example.autovol;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import weka.core.Instance;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.autovol.ml.CurrentState;
import com.autovol.ml.SvmClassifier;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.BluetoothProbe;
import edu.mit.media.funf.probe.builtin.ProximitySensorProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;

public class MainActivity extends Activity {
	
	private static final String CSV_FILE_NAME = "labeled_output.csv";
	public static final String PIPELINE_NAME = "default";
	public static final int ARCHIVE_DELAY = 15 * 60;
	private FunfManager funfManager;
	private BasicPipeline pipeline;
	
	private SimpleLocationProbe locationProbe;
	private ActivityProbe activityProbe;
	private AudioProbe audioProbe;
	private BluetoothProbe bluetoothProbe;
	private MyLightSensorProbe lightProbe;
	private WifiProbe wifiProbe;
	private ProximitySensorProbe proximityProbe;
	private BatteryProbe batteryProbe;
	private RingerVolumeProbe ringerProbe;
	
	private CurrentState currentState;
	private SvmClassifier svm;
	
	private CheckBox enabledBox, classifyBox;
	private TextView suggestionText;
	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	    	Log.d("MainActivity", "onServiceConnected");
	        funfManager = ((FunfManager.LocalBinder)service).getManager();
	        pipeline = new BasicPipeline();
	        funfManager.registerPipeline(PIPELINE_NAME, pipeline);
	        funfManager.enablePipeline(PIPELINE_NAME);
	        
	        
	        Gson gson = funfManager.getGson();
	        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
	        activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
	        audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);
	        bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
	        lightProbe = gson.fromJson(new JsonObject(), MyLightSensorProbe.class);
	        wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
	        proximityProbe = gson.fromJson(new JsonObject(),ProximitySensorProbe.class);
	        batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
	        ringerProbe = gson.fromJson(new JsonObject(), RingerVolumeProbe.class);
	        
	        funfManager.requestData(pipeline, locationProbe.getConfig());
	        activityProbe.registerPassiveListener(pipeline); //scheduling in probe
	        funfManager.requestData(pipeline, bluetoothProbe.getConfig());
	        
	        //TODO: see if this fixes
	        lightProbe.registerPassiveListener(pipeline);
	        audioProbe.registerPassiveListener(pipeline);
	        
	        funfManager.requestData(pipeline, wifiProbe.getConfig());
	        funfManager.requestData(pipeline, proximityProbe.getConfig());
	        funfManager.requestData(pipeline, batteryProbe.getConfig());
	        ringerProbe.registerPassiveListener(pipeline);
	        
	        funfManager.registerPipelineAction(pipeline, BasicPipeline.ACTION_ARCHIVE, 
	        		new Schedule.BasicSchedule(new BigDecimal(ARCHIVE_DELAY), null, false, false));
	        
	        enabledBox.setChecked(pipeline.isEnabled());
	        enabledBox.setEnabled(true);
	        classifyBox.setChecked(false);
	        classifyBox.setEnabled(true);
	    }
	    
	    @Override
	    public void onServiceDisconnected(ComponentName name) {
	        funfManager = null;
	    }
	};
	
	private void loadClassificationData() throws IOException {
		/*
	    CSVLoader loader = new CSVLoader();
	    InputStream is = getResources().getAssets().open("data/" + CSV_FILE_NAME);

	    loader.setSource(is);
	    Instances data = loader.getDataSet();
		svm = SvmClassifier.createSvmClassifier(data);
		*/
		InputStream is = getResources().getAssets().open("models/smo_model.model");
		svm = SvmClassifier.loadSvmClassifier(is);
		is.close();
	}
	
	private BroadcastReceiver classifyReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Instance obs = currentState.toInstance();
			try {
				int classification = (int) Math.round(svm.classify(obs));
				classification -= 1; // Translate back to android type
				switch (classification) {
				case android.media.AudioManager.RINGER_MODE_NORMAL:
					suggestionText.setText("on");
					break;
				case android.media.AudioManager.RINGER_MODE_VIBRATE:
					suggestionText.setText("vibrate");
					break;
				case android.media.AudioManager.RINGER_MODE_SILENT:
					suggestionText.setText("silent");
					break;
				default:
					Log.e("MainActivity", "Unknown ringer type");
				}
			} catch (Exception e) {
				Log.e("MainActivity", "Error classifying");
				e.printStackTrace();
			}
			
		}
		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		currentState = new CurrentState(this);
		
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
		
		classifyBox = (CheckBox) findViewById(R.id.classify_checkbox);
		classifyBox.setChecked(false);
		classifyBox.setEnabled(false);
		classifyBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					try {
						loadClassificationData();
						currentState.enable(funfManager);
					} catch (IOException e) {
						Log.e("MainActivity", "failed to load csv data");
						e.printStackTrace();
						classifyBox.setChecked(false);
					}
				} else {
					currentState.disable(funfManager);
				}
			}
		});
		
		suggestionText = (TextView) findViewById(R.id.ringer_suggestion_text);
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(CurrentState.NEW_STATE_BROADCAST);
        registerReceiver(classifyReceiver, filter);
	    
	    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
	    
	    Log.d("MainActivity", "Play Services Connected: " + servicesConnected());
	}
	
	@Override
	protected void onDestroy() {
		currentState.disable(funfManager);
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
