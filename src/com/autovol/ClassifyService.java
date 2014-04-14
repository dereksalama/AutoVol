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
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ClassifyService extends IntentService {
	
	public static final String EVENT_CLASSIFY_RESULT = "classify_result";
	
	private static final String GM_URL = "/AutoVolWeb/GMClassifyServlet";
	private static final String AVG_GM_URL = "/AutoVolWeb/AvGMClassifyServlet";
	private static final String KNN_URL = "/AutoVolWeb/KnnClassifyServlet";
	
	public ClassifyService() {
		super("ClassifyService");
	}

	public static final String FILE_NAME = "classify_result_file";
	
	private void saveResult(String result) {
		try {
			OutputStream output = new BufferedOutputStream(openFileOutput(FILE_NAME, 
					Context.MODE_APPEND));
			output.write(result.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String constructUrl(String servlet, String target) {
    	String reqUrl = AppPrefs.getBaseUrl(this) + servlet + "?" + 
    			"target=" + target +
    			"&user=" + AppPrefs.getAccountHash(this);
    	return reqUrl;
	}

	private void doRequest(String reqUrl, String type) {
		String time = new SimpleDateFormat("dd HH:mm", Locale.US).format(new Date());

		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(reqUrl);
			urlConnection = (HttpURLConnection) url.openConnection();
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
			StringBuilder responseStrBuilder = new StringBuilder();

			String inputStr;
			while ((inputStr = streamReader.readLine()) != null)
				responseStrBuilder.append(inputStr);

			saveResult(time + ": " + responseStrBuilder.toString());

			/*
			Gson gson = new Gson();
			JsonElement jelem = gson.fromJson(responseStrBuilder.toString(), JsonElement.class);
			JsonObject json = jelem.getAsJsonObject();
			
			Integer cluster = json.get("cluster").getAsInt();
			if (cluster != AppPrefs.getLastCluster(this)) {
				AppPrefs.setLastCluster(this, cluster);

				if (AppPrefs.isControlRinger(this)) {
					AudioManager audioMan = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
					String label = json.get("label").getAsString();
					if (label.equals("silent")) {
						audioMan.setRingerMode(AudioManager.RINGER_MODE_SILENT);
					} else if (label.equals("vibrate")) {
						audioMan.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
					} else {
						audioMan.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
					}

					
					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);

					// Let the user know
					
					Notification.Builder builder = new Notification.Builder(this)
					.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle("AutoVol: " + label)
					.setContentText("Set at " + sdf.format(new Date()));

					NotificationManager notificationManager = 
							(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					notificationManager.notify(0, builder.build());
					
				}
			}
			*/

			Intent broadcastIntent = new Intent(EVENT_CLASSIFY_RESULT);
			broadcastIntent.putExtra("json", responseStrBuilder.toString());
			broadcastIntent.putExtra("type", type);
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

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
		String singleTarget = CurrentStateListener.get().currentStateJson();
		if (singleTarget == null) {
			Log.d("ClassifyService", "no data");
			return;
		}
		String knnReqUrl = constructUrl(KNN_URL, singleTarget);
		doRequest(knnReqUrl, "knn");
		
		/*
		String reqReqUrl = constructUrl(GM_URL, singleTarget);
		doRequest(reqReqUrl, "reg");
		
		String targetList = CurrentStateListener.get().recentStatesJson(4);
		if (targetList == null) {
			Log.d("ClassifyService", "not enough states");
			return;
		}
		String avgReqUrl = constructUrl(AVG_GM_URL, targetList);
		doRequest(avgReqUrl, "avg");
		*/
	}
}
