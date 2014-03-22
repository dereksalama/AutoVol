package com.example.autovol;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.PassiveProbe;

public class MyLightSensorProbe extends Base implements PassiveProbe {
	
	private static final int INTERVAL_SECONDS = 60 * 2;
	private static final int INTERVAL_MILLIS = INTERVAL_SECONDS * 1000;
	private static final int DURATION = 2;
	private static final String ALARM_FILTER = "com.example.autovol.LIGHT_SENSOR_START";
	
	private ScheduledExecutorService executor;
	
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	private AlarmReceiver alarmReceiver;
	
	private AlarmManager alarmMan;
	private PendingIntent alarmIntent;
	
	private ArrayList<Float> measurements;
	
	private final AtomicBoolean running = new AtomicBoolean(false);
	
	private class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("MyLightSensorProbe", "Alarm received");
			executor.submit(startRecording);
			executor.schedule(stopRecording, DURATION,
					TimeUnit.SECONDS);
		}
	}
	
	private final Runnable startRecording = new Runnable() {
		@Override
		public void run() {
			Log.d("MyLightSensorProbe", "start");
			if (!running.getAndSet(true)) {
				Log.d("MyLightSensorProbe", "registering light sensor");
				sensorManager.registerListener(sensorListener, sensor, 
						SensorManager.SENSOR_DELAY_FASTEST);
			}
		}
	};
	
	private final Runnable stopRecording = new Runnable() {
		@Override
		public void run() {
			Log.d("MyLightSensorProbe", "collected " + measurements.size() + " samples");
			sensorManager.unregisterListener(sensorListener);
			if (running.getAndSet(false) && measurements.size() > 0) {
					
				JsonObject data = new JsonObject();
				Float total = Float.valueOf(0);
				for (Float val : measurements) {
					total += val;
				}
				Float avg = total / measurements.size();
				data.add("lux", new JsonPrimitive(avg));
				sendData(data);
				measurements.clear();
			}
		}
	};

	
	@Override
	protected void onEnable() {
		super.onEnable();
		Log.d("MyLightSensorProbe", "enable");
		sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		measurements = new ArrayList<Float>(1000);
		sensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				float value = event.values[0];
				measurements.add(value);
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		};
		
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
	
	@Override
	protected void onStart() {
		super.onStart();

	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
    protected void onDisable() {
		if (running.get()) {
			executor.submit(stopRecording);
			executor.shutdown();
		} else {
			executor.shutdownNow();
		}
		alarmMan.cancel(alarmIntent);
		getContext().unregisterReceiver(alarmReceiver);
		super.onDisable();
	}

}
