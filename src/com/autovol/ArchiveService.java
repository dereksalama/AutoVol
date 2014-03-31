package com.autovol;

import android.app.IntentService;
import android.content.Intent;

public class ArchiveService extends IntentService {
	
	public ArchiveService() {
		super("ArchiveService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		CurrentStateListener.get().saveRecentObservations(this);
	}

}
