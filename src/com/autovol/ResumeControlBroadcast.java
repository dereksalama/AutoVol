package com.autovol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ResumeControlBroadcast extends BroadcastReceiver {

	@Override
	public void onReceive(Context c, Intent arg1) {
		AppPrefs.setTempDisable(false, c);
	}

}
