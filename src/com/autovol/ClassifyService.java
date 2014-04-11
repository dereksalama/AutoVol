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
		if (!CurrentStateListener.get().dataIsReady()) {
			return;
		}
		
    	String reqUrl = AppPrefs.getBaseUrl(this) + TestActivity.GM_URL + "?" + 
    			"target=" + CurrentStateListener.get().currentStateJson() +
    			"&user=" + AppPrefs.getAccountHash(this);
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
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}

	}

}
