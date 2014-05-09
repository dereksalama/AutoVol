package com.autovol;

import java.io.File;
import java.io.IOException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

@DisplayName("Audio Capture Probe")
@RequiredPermissions({android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO})
@RequiredFeatures("android.hardware.microphone")
public class AudioMagnitudeProbe extends Base implements PassiveProbe {
	public static final String FILENAME = "audio_file";
	
	private static final String ALARM_FILTER = "com.autovol.AUDIO_START";
	private AlarmReceiver alarmReceiver;
	private AlarmManager alarmMan;
	private PendingIntent alarmIntent;
	private static final int INTERVAL_SECONDS = 60 * 2;
	private static final int INTERVAL_MILLIS = INTERVAL_SECONDS * 1000;
	

	@Configurable
    private String fileNameBase = "audiorectest";

    @Configurable
    private String folderName = "myaudios";
    
    @Configurable
    private int recordingLength = 5; // Duration of recording in seconds

    private String mFileName;
    private String mFolderPath;
    
    private MediaRecorder mRecorder;    
    
	private class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("AudioMagnitudeProbe", "Alarm received");
			onStart();

		}
	}


    private class RecordingCountDown extends CountDownTimer {

        public RecordingCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            stopRecording();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            //Log.d(LogUtil.TAG, "Audio capture: seconds remaining = " + millisUntilFinished / 1000);
        }
    }

    private RecordingCountDown mCountDown;

    @Override
    protected void onEnable() {
        super.onEnable();
        mFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() 
                + "/" + folderName;
        File folder = new File(mFolderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        } else if (!folder.isDirectory()) {
            folder.delete();
            folder.mkdirs();
        }
        
        // set up alarm
		alarmReceiver = new AlarmReceiver();
		getContext().registerReceiver(alarmReceiver, new IntentFilter(ALARM_FILTER));
		
		Intent intent = new Intent(ALARM_FILTER);
		alarmIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);
		alarmMan = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
		alarmMan.setInexactRepeating(AlarmManager.RTC_WAKEUP, 
				System.currentTimeMillis(),
				INTERVAL_MILLIS, alarmIntent);
    }
    
    @Override
    protected void onDisable() {
    	Log.d("AudioMagnitudeProbe", "disabled");
		alarmMan.cancel(alarmIntent);
		getContext().unregisterReceiver(alarmReceiver);
		super.onDisable();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("AudioCaptureProbe", "Probe initialization");

        mFileName = mFolderPath + "/" + FILENAME + ".mp4";
        
        mCountDown = new RecordingCountDown(TimeUtil.secondsToMillis(recordingLength), 1000);
        if (startRecording())
            mCountDown.start();
        else {
            abortRecording();
        }
    }
    
    private boolean startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            Log.d(LogUtil.TAG, "AudioMagnitudeProbe: Recording audio start");
            mRecorder.start();
            mRecorder.getMaxAmplitude(); // just get rid of first call
        } catch (IOException e) {
            Log.e(LogUtil.TAG, "AudioMagnitudeProbe: Error in preparing MediaRecorder");

            Log.e(LogUtil.TAG, e.getLocalizedMessage());
            return false;
        }
        
        return true;
    }

    private void stopRecording() {
    	int audioMagnitude = mRecorder.getMaxAmplitude();
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
        
        File f = new File(mFileName);
        if (f.exists()) {
        	f.delete(); // if we aren't using this, delete
        }

        Log.d("AudioMagnitudeProbe", "Recording audio stop: " + audioMagnitude);

        JsonObject data = new JsonObject();
        //data.addProperty(FILENAME, mFileName);
        data.addProperty("audio_mag", audioMagnitude);
        sendData(data);
    }
    
    private void abortRecording() {
        Log.e(LogUtil.TAG, "AudioCaptureProbe: Recording audio abort");
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
    }

}
