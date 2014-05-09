package com.autovol;

import java.io.UnsupportedEncodingException;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;

public class TestActivity extends Activity {


	private static int GET_ACCT_REQUEST_CODE = 1;
	
	private TextView knnReg, knnLoc, knnAvg, knnAvgLoc, knnClusterLoc, knnProbLoc,
		rf, probLocRf;
	private Button classifyButton, archiveButton, uploadButton;
	
	private BroadcastReceiver classifyReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String jsonStr = intent.getStringExtra("json");
			ClassifyType type = (ClassifyType) intent.getSerializableExtra("type");
			switch (type) {
			case KNN:
				knnReg.setText(type + ": " + jsonStr);
				break;
//			case LOC_KNN:
//				knnLoc.setText(type + ": " +jsonStr);
//				break;
			case AVG_KNN:
				knnAvg.setText(type + ": " +jsonStr);
				break;
			case AVG_LOC_KNN:
				knnAvgLoc.setText(type + ": " +jsonStr);
				break;
			case CLUSTER_KNN:
				knnClusterLoc.setText(type + ": " +jsonStr);
				break;
			case PROB_LOC_KNN:
				knnProbLoc.setText(type + ": " +jsonStr);
				break;
			case RF:
				rf.setText(type + ": " +jsonStr);
				break;
			case PROB_LOC_RF:
				probLocRf.setText(type + ": " +jsonStr);
				break;
			case AVG_RF:
				knnLoc.setText(type + ": " + jsonStr);
			default:
				break;
			}

			Toast.makeText(TestActivity.this, type + " complete",
					Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		classifyButton = (Button) findViewById(R.id.classify_button);
		classifyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				remoteClassify();
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
		
		LocalBroadcastManager.getInstance(this).registerReceiver(classifyReceiver, 
				new IntentFilter(ClassifyService.EVENT_CLASSIFY_RESULT));
		
		knnReg = (TextView) findViewById(R.id.knn_regular);
		knnAvg = (TextView) findViewById(R.id.knn_avg);
		knnLoc = (TextView) findViewById(R.id.knn_loc);
		knnAvgLoc = (TextView) findViewById(R.id.knn_avg_loc);
		knnClusterLoc = (TextView) findViewById(R.id.knn_cluster_loc);
		knnProbLoc = (TextView) findViewById(R.id.knn_prob_loc);
		rf = (TextView) findViewById(R.id.rf);
		probLocRf = (TextView) findViewById(R.id.rf_prob_loc);
		
	}
	
	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(classifyReceiver);
		super.onDestroy();
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
    
    private void remoteClassify() {
    	if (!CurrentStateListener.get().dataIsReady()) {
    		Toast.makeText(this, "Data not ready yet", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	Intent intent = new Intent(this, ClassifyService.class);
    	startService(intent);
    }
}
