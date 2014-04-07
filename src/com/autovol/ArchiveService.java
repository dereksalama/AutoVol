package com.autovol;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ArchiveService extends IntentService {
	
	public ArchiveService() {
		super("ArchiveService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("ArchiveService", "intent received");
		CurrentStateListener.get().saveRecentObservations(this);
	}

}
