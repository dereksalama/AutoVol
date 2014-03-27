package com.example.autovol;

import com.autovol.ml.CurrentStateListener;

import android.app.IntentService;
import android.content.Intent;

public class ArchiveService extends IntentService {

	public ArchiveService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		CurrentStateListener.getListener().saveRecentObservations(this);
	}

}
