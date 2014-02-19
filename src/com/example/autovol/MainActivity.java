package com.example.autovol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;

public class MainActivity extends Activity implements DataListener {
	
	public static final String PIPELINE_NAME = "default";
	private FunfManager funfManager;
	private BasicPipeline pipeline;
	private SimpleLocationProbe locationProbe;
	private TextView dataCountView;
	private Button archiveButton, scanNowButton;
	private Handler handler;
	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	        funfManager = ((FunfManager.LocalBinder)service).getManager();
	        funfManager.enablePipeline(PIPELINE_NAME);
	        
	        Gson gson = funfManager.getGson();
	        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
	        pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
	        locationProbe.registerPassiveListener(MainActivity.this);
	        
	        archiveButton.setEnabled(true);
	        scanNowButton.setEnabled(true);
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
		
		dataCountView = (TextView) findViewById(R.id.data_count);
		
		handler = new Handler();
		// Runs an archive if pipeline is enabled
	    archiveButton = (Button) findViewById(R.id.archive_button);
	    archiveButton.setEnabled(false);
	    archiveButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            if (pipeline.isEnabled()) {
	                pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);
	              
	                // Wait 1 second for archive to finish, then refresh the UI
	                // (Note: this is kind of a hack since archiving is seamless and there are no messages when it occurs)
	                handler.postDelayed(new Runnable() {
	                    @Override
	                    public void run() {
	                        Toast.makeText(getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
	                        updateScanCount();
	                    }
	                }, 1000L);
	            } else {
	                Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
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
	                // Manually register the pipeline
	                locationProbe.registerListener(pipeline);
	            } else {
	                Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
	            }
	        }
	    });
	    
	    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
	    
	}
	
	private static final String TOTAL_COUNT_SQL = "SELECT count(*) FROM " + NameValueDatabaseHelper.DATA_TABLE.name;
	/**
	* Queries the database of the pipeline to determine how many rows of data we have recorded so far.
	*/
	private void updateScanCount() {
	    // Query the pipeline db for the count of rows in the data table
	    SQLiteDatabase db = pipeline.getDb();
	    Cursor mcursor = db.rawQuery(TOTAL_COUNT_SQL, null);
	    mcursor.moveToFirst();
	    final int count = mcursor.getInt(0);
	    // Update interface on main thread
	    runOnUiThread(new Runnable() {
	        @Override
	        public void run() {
	            dataCountView.setText("Data Count: " + count);
	        }
	    });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onDataCompleted(IJsonObject arg0, JsonElement arg1) {
		locationProbe.registerPassiveListener(this);
		
	}

	@Override
	public void onDataReceived(IJsonObject arg0, IJsonObject arg1) {
		// TODO Auto-generated method stub
		
	}

}
