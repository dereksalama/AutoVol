package com.example.autovol;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.PassiveProbe;

//TODO: fix these
@Schedule.DefaultSchedule(interval=60, duration=2)
public class MyLightSensorProbe extends Base implements PassiveProbe {
	
	private static final int INTERVAL = 50;
	private static final int DURATION = 2;
	
	private ScheduledExecutorService executor;
	
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;
	
	private ArrayList<Float> measurements;
	
	private final AtomicBoolean running = new AtomicBoolean(false);
	
	private final Runnable startRecording = new Runnable() {
		@Override
		public void run() {
			Log.d("MyLightSensorProbe", "start");
			if (!running.getAndSet(true)) {
				sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
				executor.schedule(stopRecording, DURATION, TimeUnit.SECONDS);
			}
		}
	};
	
	private final Runnable stopRecording = new Runnable() {
		@Override
		public void run() {
			Log.d("MyLightSensorProbe", "stop");
			sensorManager.unregisterListener(sensorListener);
			if (running.getAndSet(false)) {
				JsonObject data = new JsonObject();
				Float total = Float.valueOf(0);
				for (Float val : measurements) {
					total += val;
				}
				Float avg = total / measurements.size();
				data.addProperty("lux", avg);
				sendData(data);
				measurements.clear();
				executor.schedule(startRecording, INTERVAL, TimeUnit.SECONDS);
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
		executor.submit(startRecording);
	}
	
	@Override
	protected void onStart() {
		super.onStart();

	}
	
	@Override
	protected void onStop() {
		if (running.get()) {
			executor.submit(stopRecording);
			executor.shutdown();
		} else {
			executor.shutdownNow();
		}
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
		super.onDisable();
	}
	
	
	
	

}
