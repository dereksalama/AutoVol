package com.autovol;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import edu.mit.media.funf.FunfManager;

public class DataCollectionService extends IntentService {

	public DataCollectionService() {
		super("DataCollectionService");
	}

	public static final String INTENT_EXTRA_COLLECTION = "intent_extra_collection";
	
	private FunfManager funfManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (AppPrefs.isEnableCollection(this)) {
			 bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
		}
		if (AppPrefs.isControlRinger(this)) {
			ClassifyAlarm.scheduleRepeatedAlarm(this);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}

	private ServiceConnection funfManagerConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	    	Log.d("TestActivity", "onServiceConnected");
	        funfManager = ((FunfManager.LocalBinder)service).getManager();

	        if (AppPrefs.isEnableCollection(DataCollectionService.this)) {
		        CurrentStateListener.get().enable(funfManager, DataCollectionService.this);
		        
		        if (AppPrefs.isControlRinger(DataCollectionService.this)) {
		        	ClassifyAlarm.scheduleRepeatedAlarm(DataCollectionService.this);
		        }
	        }
	        
	    }
	    
	    @Override
	    public void onServiceDisconnected(ComponentName name) {
	        funfManager = null;
	    }
	};
	
	@Override
	public void onDestroy() {
		CurrentStateListener.get().disable(funfManager, this);
		unbindService(funfManagerConn);
		ClassifyAlarm.cancelAlarm(this);
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent.getBooleanExtra(INTENT_EXTRA_COLLECTION, false)) {
			bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
		} else if (intent.hasExtra(INTENT_EXTRA_COLLECTION)){
			CurrentStateListener.get().disable(funfManager, this);
			unbindService(funfManagerConn);
		}
	}
}
