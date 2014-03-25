package com.example.autovol;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
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

import com.autovol.ml.CurrentStateListener;
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
	// Local host
	//private static final String BASE_URL = "http://10.0.1.17:8080";
	
	// AWS host
	private static final String BASE_URL = "http://ec2-54-186-90-159.us-west-2.compute.amazonaws.com:8080";
	
	private static final String SMO_URL = BASE_URL + "/AutoVolWeb/SMOClassifyServlet";

	private FunfManager funfManager;

	private CurrentStateListener currentState;
	
	private CheckBox enabledBox, classifyBox;
	private TextView suggestionText;
	private Button classifyButton;
	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	    	Log.d("MainActivity", "onServiceConnected");
	        funfManager = ((FunfManager.LocalBinder)service).getManager();

	        //TODO: upload shit
	        //enabledBox.setChecked(pipeline.isEnabled());
	        //enabledBox.setEnabled(true);
	        classifyBox.setChecked(false);
	        classifyBox.setEnabled(true);
	        currentState.enable(funfManager);
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
		
		currentState = new CurrentStateListener();
		
		enabledBox = (CheckBox) findViewById(R.id.enabled_checkbox);
		enabledBox.setEnabled(false);
		enabledBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
             //TODO
			}
		});
		
		classifyBox = (CheckBox) findViewById(R.id.classify_checkbox);
		classifyBox.setChecked(false);
		classifyBox.setEnabled(false);
		classifyBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					currentState.enable(funfManager);

				} else {
					currentState.disable(funfManager);
				}
			}
		});
		
		classifyButton = (Button) findViewById(R.id.classify_button);
		classifyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				remoteClassify();
			}
		});
		
		suggestionText = (TextView) findViewById(R.id.ringer_suggestion_text);
	    
	    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
	    
	    Log.d("MainActivity", "Play Services Connected: " + servicesConnected());
	}
	
	@Override
	protected void onDestroy() {
		currentState.disable(funfManager);
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
    
    private void remoteClassify() {
    	if (!currentState.dataIsReady()) {
    		Toast.makeText(this, "Data not ready yet", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	String reqUrl = SMO_URL + "?" + "state=" + currentState.currentStateJson();
    	
    	new AsyncTask<String, Void, Double>() {

			@Override
			protected Double doInBackground(String... urls) {
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
			      
			      JSONObject result = new JSONObject(responseStrBuilder.toString());
			      Double ringer = result.getDouble("ringer_type");
			      return ringer;
			    } catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
			    	if (urlConnection != null)
			    		urlConnection.disconnect();
			    }
				
			    return null;
			}
			
			@Override
			protected void onPostExecute(Double result) {
				if (result != null) {
					suggestionText.setText(result.toString());
					Toast.makeText(MainActivity.this, "New Suggestion: " + result.toString(), Toast.LENGTH_SHORT).show();
				}
			}
    		
		}.execute(reqUrl);

    }

}
