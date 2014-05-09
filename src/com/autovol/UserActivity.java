package com.autovol;

import java.io.UnsupportedEncodingException;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.google.android.gms.common.AccountPicker;

import edu.mit.media.funf.FunfManager;

public class UserActivity extends Activity {
	
	private static int GET_ACCT_REQUEST_CODE = 1;
	private FunfManager funfManager;


	private CheckBox enableBox, classifyBox, tempDisableBox;
	private Button saveDefaultButton;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user);
	    
	    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
	    
	    if (AppPrefs.isFreshInstall(this)) {
	    	chooseNewAccount();
	    }
	    
	    enableBox = (CheckBox) findViewById(R.id.checkbox_enable);
	    enableBox.setChecked(AppPrefs.isEnableCollection(this)); 
	    enableBox.setEnabled(false);
	    enableBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				AppPrefs.setEnableCollection(isChecked, UserActivity.this);
				classifyBox.setEnabled(isChecked);
				if (isChecked) {
					//ClassifyAlarm.scheduleRepeatedAlarm(UserActivity.this);
					CurrentStateListener.get().enable(funfManager, UserActivity.this);
				} else {
					//ClassifyAlarm.cancelAlarm(UserActivity.this);
					CurrentStateListener.get().disable(funfManager, UserActivity.this);
				}
			}
		});
	    
	    classifyBox = (CheckBox) findViewById(R.id.checkbox_classify);
	    classifyBox.setChecked(AppPrefs.isEnableClassify(this)); 
	    classifyBox.setEnabled(AppPrefs.isEnableCollection(this));
	    classifyBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				AppPrefs.setEnableClassify(isChecked, UserActivity.this);
				if (isChecked) {
					ClassifyAlarm.scheduleRepeatedAlarm(UserActivity.this);
				} else {
					ClassifyAlarm.cancelAlarm(UserActivity.this);
				}
			}
		});
	    
	    tempDisableBox = (CheckBox) findViewById(R.id.checkbox_temp_disable);
	    tempDisableBox.setChecked(AppPrefs.isTempDisable(this)); 
	    tempDisableBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				AppPrefs.setTempDisable(isChecked, UserActivity.this);
			}
		});
	    
	    saveDefaultButton = (Button) findViewById(R.id.button_save_default);
	    saveDefaultButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AudioManager audioMan = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int newDefault = audioMan.getStreamVolume(AudioManager.STREAM_RING);
				AppPrefs.setDefaultRingerVolume(UserActivity.this, newDefault);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		tempDisableBox.setChecked(AppPrefs.isTempDisable(this)); 
	}
	@Override
	protected void onDestroy() {
		CurrentStateListener.get().disable(funfManager, getApplicationContext());
		unbindService(funfManagerConn);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.user, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_open_test) {
			Intent intent = new Intent(this, TestActivity.class);
			startActivity(intent);
			return true;
		} else if (id == R.id.action_open_results) {
			//Intent intent = new Intent(this, ResultsActivity.class);
			Intent intent = new Intent(this, ResultsFragmentActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
	
	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	    	Log.d("TestActivity", "onServiceConnected");
	        funfManager = ((FunfManager.LocalBinder)service).getManager();

	        if (AppPrefs.isEnableCollection(UserActivity.this)) {
		        CurrentStateListener.get().enable(funfManager, UserActivity.this);
		        
		        if (AppPrefs.isEnableClassify(UserActivity.this)) {
		        	ClassifyAlarm.scheduleRepeatedAlarm(UserActivity.this);
		        }
	        }
	        enableBox.setEnabled(true);
	        
	    }
	    
	    @Override
	    public void onServiceDisconnected(ComponentName name) {
	        funfManager = null;
	    }
	};
	
	
	

	private void chooseNewAccount() {
		
		Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
		         false, null, null, null, null);
		 startActivityForResult(intent, GET_ACCT_REQUEST_CODE);
		 
	}
}
