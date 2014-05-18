package com.autovol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ClassifyService extends IntentService {
	
	public static final String EVENT_CLASSIFY_RESULT = "classify_result";
	
	public static final int NUM_VECTORS_TO_AVG = 8;
	
	public ClassifyService() {
		super("ClassifyService");
	}

	public static final String FILE_NAME = "classify_result_file";
	
	public static String getFileName(String type) {
		return FILE_NAME + "_" + type;
	}
	
	private void saveResult(String type, String result) {
		try {
			String filename = getFileName(type);
			OutputStream output = new BufferedOutputStream(openFileOutput(filename, 
					Context.MODE_APPEND));
			output.write(result.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static final String[] RINGER_MODES = {"silent", "vibrate", "normal"};


	private void applyResult(String response, String type) {
		if (AppPrefs.isControlRinger(this) && !AppPrefs.isTempDisable(this)) {
			Gson gson = new Gson();
			JsonElement jelem = gson.fromJson(response, JsonElement.class);
			JsonObject json = jelem.getAsJsonObject();
			
			String result = json.get("result").getAsString();
			if (result.equals("err")) {
				Log.d("ClassifyService", "error result for main");
				return;
			}
			
			
			AudioManager audioMan = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			int ringerMode = audioMan.getRingerMode();
			String currentRinger = RINGER_MODES[ringerMode];
			
			if (!currentRinger.equalsIgnoreCase(result)) {

				AppPrefs.setAutovolSettingVolume(true, this);
				if (result.equals("silent")) {
					audioMan.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				} else if (result.equals("vibrate")) {
					audioMan.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				} else {
					audioMan.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
					int defaultVol = AppPrefs.getDefaultRingerVolume(this);
					audioMan.setStreamVolume(AudioManager.STREAM_RING, defaultVol, 0);
				}
				AppPrefs.setAutovolSettingVolume(false, this);

				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
				
				String time = new SimpleDateFormat("dd HH:mm", Locale.US).format(new Date());
				saveResult(type, time + ": " + response + "\n");

				// Let the user know

				Notification.Builder builder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("AutoVol: " + result)
				.setVibrate(new long[] {0, 10})
				.setContentText("Set at " + sdf.format(new Date()));

				NotificationManager notificationManager = 
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.notify(0, builder.build());

			}
			
		}
	}

	private void doRequest(ClassifyType type) {
		String time = new SimpleDateFormat("dd HH:mm", Locale.US).format(new Date());

		HttpURLConnection urlConnection = null;
		try {
			String urlStr = type.constructUrl(this);
			if (urlStr == null) {
				return;
			}
			URL url = new URL(urlStr);
			urlConnection = (HttpURLConnection) url.openConnection();
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
			StringBuilder responseStrBuilder = new StringBuilder();

			String inputStr;
			while ((inputStr = streamReader.readLine()) != null)
				responseStrBuilder.append(inputStr);

			saveResult(type.toString(), time + ": " + responseStrBuilder.toString() + "\n");

			if (type.equals(ClassifyType.MAIN)) {
				applyResult(responseStrBuilder.toString(), type.toString());
			} else {
				Intent broadcastIntent = new Intent(EVENT_CLASSIFY_RESULT);
				broadcastIntent.putExtra("json", responseStrBuilder.toString());
				broadcastIntent.putExtra("type", type);
				LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
			}

			Log.d("ClassifyService", "Succes!");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}

	}


	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("ClassifyService", "intent recieved");
		boolean isTest = intent.getBooleanExtra("test", false);
		if (isTest) {
			for (ClassifyType type : ClassifyType.values()) {
				doRequest(type);
			}
		} else {
			if (AppPrefs.isControlRinger(this)) {
				doRequest(ClassifyType.MAIN);
			}
		}
	}
}
