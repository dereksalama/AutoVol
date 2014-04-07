package com.example.autovol;

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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.autovol.ml.CurrentState;

public class ClassifyService extends IntentService {
	
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

	@Override
	protected void onHandleIntent(Intent intent) {
		String reqUrl = MainActivity.SMO_URL + "?" + CurrentState.get().requestParamString();
		String time = new SimpleDateFormat("dd HH:mm", Locale.US).format(new Date());
		Log.d("ClassifyService", "intent received");

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

			JSONObject result = new JSONObject(responseStrBuilder.toString());
			String k3 = result.getString("k3");
			String k7 = result.getString("k7");
			String rawK3 = result.getString("raw_k3");
			String rawK7 = result.getString("raw_k7");
			String loc = result.getString("loc");
			saveResult(time + ": " + k3 + ", " + k7 + ",\n" + rawK3 + ", " + rawK7 + ", " + loc + "\n");
			Log.d("ClassifyService", "result saved");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
	}

}
