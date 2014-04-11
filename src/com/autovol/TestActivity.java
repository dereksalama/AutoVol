package com.autovol;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TestActivity extends Activity {

	public static final String GM_URL = "/AutoVolWeb/GMClassifyServlet";
	private static int GET_ACCT_REQUEST_CODE = 1;
	
	private TextView gmLabel, gmClusterProb, gmLabelProb;
	private Button classifyButton, archiveButton, uploadButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		classifyButton = (Button) findViewById(R.id.classify_button);
		classifyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				remoteClassifyGM();
			}
		});
		archiveButton = (Button) findViewById(R.id.archive_button);
		archiveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(TestActivity.this, ArchiveService.class);
				startService(i);
			}
		});
		uploadButton = (Button) findViewById(R.id.upload_button);
		uploadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(TestActivity.this, UploadService.class);
				startService(i);
			}
		});
		
		gmLabel = (TextView) findViewById(R.id.gm_label_text);
		gmClusterProb = (TextView) findViewById(R.id.gm_cluster_prob_text);
		gmLabelProb = (TextView) findViewById(R.id.gm_label_prob_text);

	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode,
			final Intent data) {
		if (requestCode == GET_ACCT_REQUEST_CODE && resultCode == RESULT_OK) {
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			try {
				AppPrefs.setAccountHash(this, accountName);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} 
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test, menu);
		
		boolean checkLocalUrl = AppPrefs.useLocalHost(this);
		menu.findItem(R.id.action_base_url).setChecked(checkLocalUrl);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_base_url) {
			if (item.isChecked()) {
				item.setChecked(false);
				AppPrefs.setUseLocalHost(false, this);
			} else {
				item.setChecked(true);
				AppPrefs.setUseLocalHost(true, this);
			}
		} else if (id == R.id.action_new_account) {
			chooseNewAccount();
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void chooseNewAccount() {
		
		Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
		         false, null, null, null, null);
		 startActivityForResult(intent, GET_ACCT_REQUEST_CODE);
		 
	}
    
    private void remoteClassifyGM() {
    	if (!CurrentStateListener.get().dataIsReady()) {
    		Toast.makeText(this, "Data not ready yet", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	String reqUrl = AppPrefs.getBaseUrl(this) + GM_URL + "?" + "target=" + CurrentStateListener.get().currentStateJson() + 
    			"&user=" + AppPrefs.getAccountHash(this);
    	
    	new AsyncTask<String, Void, String>() {

    		@Override
    		protected String doInBackground(String... urls) {
    			HttpURLConnection urlConnection = null;
    			try {
    				URL url = new URL(urls[0]);
    				urlConnection = (HttpURLConnection) url.openConnection();
    				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
    				BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
    				StringBuilder responseStrBuilder = new StringBuilder();

    				String inputStr;
    				while ((inputStr = streamReader.readLine()) != null)
    					responseStrBuilder.append(inputStr);

    				return responseStrBuilder.toString();
    			} catch (MalformedURLException e) {
    				e.printStackTrace();
    			} catch (IOException e) {
    				e.printStackTrace();
    			} finally {
    				if (urlConnection != null)
    					urlConnection.disconnect();
    			}

    			return null;
    		}

    		@Override
    		protected void onPostExecute(String result) {
    			if (result != null) {
    				Gson gson = new Gson();
    				JsonElement jelem = gson.fromJson(result, JsonElement.class);
    				JsonObject json = jelem.getAsJsonObject();
    				gmLabel.setText(json.get("label").getAsString());
    				gmLabelProb.setText("" + json.get("prob_label").getAsDouble());
    				gmClusterProb.setText("" + json.get("prob_cluster").getAsDouble());
    				Toast.makeText(TestActivity.this, "Classification Complete: " + json, Toast.LENGTH_SHORT).show();
    			}
    		}

    	}.execute(reqUrl);
    }
}
