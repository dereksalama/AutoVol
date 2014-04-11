package com.autovol;

import java.io.UnsupportedEncodingException;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.AccountPicker;

import edu.mit.media.funf.FunfManager;

public class UserActivity extends Activity {
	
	private static int GET_ACCT_REQUEST_CODE = 1;
	private FunfManager funfManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
	    
	    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
	    
	    if (AppPrefs.isFreshInstall(this)) {
	    	chooseNewAccount();
	    }
	}
	
	@Override
	protected void onDestroy() {
		CurrentStateListener.get().disable(funfManager);
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_user, container,
					false);
			return rootView;
		}
	}
	
	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	    	Log.d("TestActivity", "onServiceConnected");
	        funfManager = ((FunfManager.LocalBinder)service).getManager();

	        CurrentStateListener.get().enable(funfManager);
	        
	        ArchiveAlarm.scheduleRepeatedArchive(UserActivity.this);
	        UploadAlarm.scheduleDailyUpload(UserActivity.this);
	        //TODO:
	        //ClassifyAlarm.scheduleRepeatedAlarm(TestActivity.this);
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
