//package edu.dartmouthcs.mltoolkit.ServiceControllers.AudioLib;
package com.example.autovol;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.util.Log;

public class AudioManager {
    /**
     * INITIALIZING : recorder is initializing; READY : recorder has been
     * initialized, recorder not yet started RECORDING : recording ERROR :
     * reconstruction needed STOPPED: reset needed
     */
    public enum State {
    	INITIALIZING, READY, RECORDING, ERROR, STOPPED
    };
    
    public static final int AUDIO_SILENCE = 0;
    public static final int AUDIO_NOISE = 1;
    public static final int AUDIO_VOICE = 2;
    public static final int AUDIO_ERROR = 3;
    

    // Recorder used for uncompressed recording
    private AudioRecord aRecorder = null;
    
    private int[] votes;
 
    // Stores current amplitude (only in uncompressed mode)
    private int cAmplitude = 0;

    // Recorder state; see State
    private State state;

    static {
    	System.loadLibrary("computeFeatures");
    }

    // Number of channels, sample rate, sample size(size in bits), buffer size,
    // audio source, sample size(see AudioFormat)
    private short nChannels;
    private int sRate;
    private short bSamples;
    private int bufferSize;
    private int aSource;
    private int aFormat;

    // Number of frames written to file on each output(only in uncompressed
    // mode)
    private int framePeriod;

    // Buffer for output(only in uncompressed mode)
    private short[] buffer;
    private short[] tempBuffer = { -68, 8, 22, 40, 94, 77, 119, 126, 80, 82,
	    61, 60, 80, 64, 79, 51, 4, 9, -7, 14, 20, -9, -16, 19, -28, -50,
	    -38, -82, -135, -120, -112, -95, -105, -74, 10, 53, 15, 52, 88, 21,
	    32, 15, -31, 13, 22, 32, 8, 12, 89, 88, 42, 22, 7, -49, -115, -148,
	    -117, 22, 33, 65, 138, 133, 78, 60, 89, 92, 83, 67, 53, 8, -17,
	    -35, -31, -35, -21, 4, -2, 27, -18, -97, -79, -63, -54, -26, -3,
	    -38, -58, -34, -48, -19, 29, 17, -15, -3, -46, -91, -65, 10, 106,
	    112, 110, 72, 83, 46, -14, 13, 54, 117, 116, 77, 23, -4, 48, 76,
	    31, -5, 8, 1, -21, -47, -104, -129, -141, -110, -47, -13, 4, 57,
	    -7, -40, -87, -62, -12, 20, 48, 40, 41, 34, 34, -7, -29, -57, -115,
	    -100, -75, -69, -38, 36, 43, 2, 3, 0, -19, -60, -92, -32, -37, -25,
	    -7, -14, -22, -12, 9, 11, 2, -19, 25, 24, -1, 31, 69, 47, -34, -67,
	    -101, -129, -130, -115, -51, 1, 29, 53, 42, 26, 9, 22, 33, 65, 138,
	    133, 78, 60, 89, 92, 83, 67, 53, 8, -17, -35, -31, -35, -21, 4, -2,
	    27, -18, -97, -79, -63, -54, -26, -3, -38, -58, -34, -48, -19, 29,
	    17, -15, -3, -46, -91, -65, 10, 106, 112, 110, 72, 83, 46, -14, 13,
	    54, 117, 116, 77, 23, -4, 48, 76, 31, -5, 8, 1, -21, -47, -104,
	    -129, -141, -110, -47, -13, 4, 57 };

    // Number of bytes written to file after header(only in uncompressed mode)
    // after stop() is called, this size is written to the header/data chunk in
    // the wave file
    private int payloadSize;
    private int updateFlag;

    // audio feature options
    private final int FRAME_SIZE_MULTIPLIER = 8;
    private final int FRAME_SIZE = 256;
    private int FRAME_STEP = FRAME_SIZE / 2;
    private int INFERENCE_FRAME_SIZE = 128;
    private short[] audioFrame;

    // audio buffer
    private short audioBuffer[][];
    private int audioBufferSize = 500;
    private int audioBufferNextPos = 0;

    // feature and audio syncing
    private int sync_id_counter = 0;
    private FileOutputStream fOut;
    private OutputStreamWriter osw;
    private int writeCounter = 0;
    public boolean recordingStopped;

    public class AudioData {
	public short data[];
	public long timestamp;
	public int sync_id;

	public AudioData() {
	}

	public AudioData(short[] data, long timestamp, int sync_id) {
	    // this.data = data;
	    System.arraycopy(data, 0, audioBuffer[audioBufferNextPos], 0,
		    FRAME_STEP * FRAME_SIZE_MULTIPLIER);
	    this.data = audioBuffer[audioBufferNextPos];
	    audioBufferNextPos = (audioBufferNextPos + 1) % audioBufferSize;

	    this.timestamp = timestamp;
	    this.sync_id = sync_id;
	}
    }

    private CircularBufferFeatExtractionInference<AudioData> cirBuffer;
    private AudioData audioFromQueueData = new AudioData();
    private long tempTimestamp = 0;

    // /////////////////////////////////////////////////////////////
    // ////////// Conversation detection codes:start /////////////
    // ////////////////////////////////////////////////////////////

    // all conversation decision making variables are here
    private double[] extractedFeatures;
    private double currentInference;
    private double leavingInference;
    private double minuteToLookBackForPopup = 1; // means only history of last
						 // minute will be kept
    private double[] circularQueueOfInference;
    private final int LengthCircularQueueOfInference = 
	    (int) (minuteToLookBackForPopup * 3750); // number
												// of
												// inferences
												// possible
												// in
												// minuteToLookBackForPopup
												// minutes.
												// 60*8000/128
												// =
												// 3750
    private double sumOfPreviousInferences = 0;
    private int indexToCircularQueueOfInference = 0;
    private boolean inCoversation;
    private boolean conversationIntentSent;
    // currently at 3 percent is the threshold
    private double thresholdForConversation = ((double) LengthCircularQueueOfInference) * 3.0 / 100.0;
    private long conversationStartTime;
    private long conversationEndTime;

    // conversation pop-up timer code
    private Handler mHandler = new Handler();
    private final int rateNotification = 1000 * 10; // every 10 seconds

    // ////////////////////////////////////////////////////////////
    // ////////// Conversation detection codes:end /////////////
    // ////////////////////////////////////////////////////////////

    private native int energy(short[] array);

    private native double[] features(short[] array);

    private native void audioFeatureExtractionInit();

    private native void audioFeatureExtractionDestroy();

    private String dataString;

    private int audioEnergy;


    /**
     * 
     * Returns the state of the recorder in a RehearsalAudioRecord.State typed
     * object. Useful, as no exceptions are thrown.
     * 
     * @return recorder state
     */
    public State getState() {
	return state;
    }

    /**
     * 
     * This thread fetches the audio data from the queue And process the data by
     * calling native C functions
     * 
     */
    public class MyQueuePopper extends Thread {

	CircularBufferFeatExtractionInference<AudioData> obj;
	double[] audioFrameFeature;
	double[] audioWindowFeature;

	private volatile Thread blinker;

	public MyQueuePopper(
		CircularBufferFeatExtractionInference<AudioData> obj) {
	    // initialization
	    this.obj = obj;
	    // audioFeatureExtractionInit();
	    audioFrame = new short[FRAME_SIZE];

	    // initialize the first half with zeros
	    for (int i = 0; i < FRAME_STEP; i++)
		audioFrame[i] = 0;

	    try {
		// <---------------- Rifat commented the following two lines
		// fOut = new FileOutputStream("/sdcard/priv_audio.txt");
		// osw = new OutputStreamWriter(fOut);

		// debug code: for getting training examples
		// open a file
		// fIn = new FileInputStream("/sdcard/fanandvoicedata.csv");
		// fIn = new FileInputStream("/sdcard/voicedata.csv");
		// br = new BufferedReader(new InputStreamReader(fIn));
		// inputStr = null;

	    } catch (Exception e) {
	    }

	}

	public void stopper() {
	    blinker = null;
	}

	@Override
	public void start() {
	    blinker = new Thread(this);
	    blinker.start();
	}

	private void getFrameInference(int idx)
	{
	    // /////////////////////////////////////////////////////////////
	    // ////////// audio features is computed here /////////////
	    // ////////////////////////////////////////////////////////////

	    // decide for conversation
	    // a timer has been added which will check every 30 seconds to
	    // see whether
	    // there is x% percent voice being sensed in the last minute

	    // extractedFeatures = features(audioFrame);
	    extractedFeatures = features(audioFrame); // problem is here we
	    // want to assign the
	    // array to a fixed
	    // place, but variable
	    // length is causing
	    // problems?

	    // /////////////////////////////////////////////////////////////
	    // ////////// Conversation detection codes:start /////////////
	    // ////////////////////////////////////////////////////////////

	    // add the new inference results. 0 = non-human-voice,
	    // 1=human-voice.
	    leavingInference = circularQueueOfInference[indexToCircularQueueOfInference];
	    sumOfPreviousInferences = sumOfPreviousInferences
		    - leavingInference;
	    currentInference = extractedFeatures[8]; // 0 = non-human-voice,
	    // 1 = human-voice
	    sumOfPreviousInferences = sumOfPreviousInferences
		    + currentInference;
	    circularQueueOfInference[indexToCircularQueueOfInference] = currentInference;
	    indexToCircularQueueOfInference = (indexToCircularQueueOfInference + 1)
		    % LengthCircularQueueOfInference;

	    recordAudioInference(extractedFeatures,
		    audioFromQueueData.timestamp,
		    audioFromQueueData.sync_id);
	    // /////////////////////////////////////////////////////////////
	    // ////////// Conversation detection codes:end /////////////
	    // ////////////////////////////////////////////////////////////

	    /*
	 		AudioObject = appState.mMlToolkitObjectPool.borrowObject()
	 			.setValues(tempTimestamp, 3,
	 				MyDataTypeConverter.toByta(extractedFeatures),
	 				audioFromQueueData.sync_id);
	 		appState.ML_toolkit_buffer.insert(AudioObject);// inserting into
	     */					       // the buffer

	    // done for overlapping window
	    System.arraycopy(audioFromQueueData.data, 0, audioFrame, 0,
		    FRAME_STEP);
	}
	
	@Override
	public void run() {
	    Thread thisThread = Thread.currentThread();
	    while (blinker == thisThread) {
		audioFromQueueData = obj.deleteAndHandleData();
		
		//Log.e("AUDIO", "new audio frame!!");
		for(int i = 0;i < FRAME_SIZE_MULTIPLIER; i++) {
		    System.arraycopy(audioFromQueueData.data, FRAME_STEP * i, audioFrame,
			FRAME_STEP, FRAME_STEP);
		    
		    getFrameInference(i);
		}
	    }

	}

    }


    private final double silenceThreshold = 1e8;
    private final int smooth_window = 60;	//80 for 1s
    private int[] cir_inference = new int[smooth_window];
    private int smooth_idx=0;
    
    private void recordAudioInference(double[] extractedFeatures, long ts, int sync_id) {
		int inferent_int = (int)extractedFeatures[8];
		
		if (inferent_int == 1) {
		    cir_inference[smooth_idx] = AUDIO_VOICE;
		} else if(extractedFeatures[5] < silenceThreshold) {
		    cir_inference[smooth_idx] = AUDIO_SILENCE;
		} else {
		    cir_inference[smooth_idx] = AUDIO_NOISE;
		}
	
		smooth_idx++;
		if(smooth_idx == smooth_window)	{
		   inferent_int = getSmoothedInference();
		   //Log.e("audio_vote","audio inference="+inferent_int);
		   saveAudioInference(inferent_int, ts,sync_id);
		   smooth_idx=0;
		}
	}
	    
    private int getSmoothedInference()
	    {
		int[] vote = new int[3];
		for(int i=0;i<smooth_window;i++) {
		    vote[cir_inference[i]]++;
		}
		
		//Log.e("audio_vote",vote[AUDIO_SILENCE]+" "+vote[AUDIO_NOISE]+" "+vote[AUDIO_VOICE]);
		if(vote[AUDIO_SILENCE] * 0.5 > vote[AUDIO_NOISE] + vote[AUDIO_VOICE])
		{
		    return AUDIO_SILENCE;
		} else if(vote[AUDIO_NOISE] * 0.8 > vote[AUDIO_VOICE])
		{
		    return AUDIO_NOISE;
		} else {
		    return AUDIO_VOICE;
		}
    }
    
    // TODO
    private void saveAudioInference(int inference_int, long ts, int sync_id) {
    	/*
    	Intent resultIntent = new Intent(AudioProbe.BROADCAST_ACTION);
    	resultIntent.putExtra(AudioProbe.AUDIO_TYPE, inference_int);
    	context.sendBroadcast(resultIntent);
    	*/
    	votes[inference_int]++;
    	/*
		appState.amount_of_different_all_voice_activities++;
		switch(inference_int)
		{
		case 0:
		    appState.audio_inference = "silence";
		    appState.amount_of_different_voice_activity[0]++;
		    break;
		case 1:
		    appState.audio_inference = "noise";
		    appState.amount_of_different_voice_activity[1]++;
		    break;
		case 2:
		    appState.audio_inference = "voice";
		    appState.amount_of_different_voice_activity[2]++;
		    break;
		case 3:
		    appState.audio_inference = "error";
		    appState.amount_of_different_voice_activity[3]++;
		    break;
		}
		ML_toolkit_object audio_inference = appState.mMlToolkitObjectPool
			.borrowObject().setValues(ts, 5, true,
				appState.audio_inference, sync_id);
		appState.ML_toolkit_buffer.insert(audio_inference);
		*/
    }
    
    public int getLastInference() {
    	if (votes == null) {
    		return AUDIO_ERROR;
    	}
    	int maxVal = votes[0];
    	int maxIndex = 0;
    	for (int i = 0; i < votes.length; i++) {
    		if (votes[i] >= maxVal) {
    			maxVal = votes[i];
    			maxIndex = i;
    		}
    	}
    	
    	return maxIndex;
    }

    private MyQueuePopper myQueuePopper;
    public boolean freeCMemoryActivated;

    /**
     * 
     * Method used for recording and storing data to the buffer for queue
     * insertion
     * 
     */
    private int rec_counter=0;
    private int rec_sampling_rate=2;
    private long lastFrameTimeStamp=0;
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
	public void onPeriodicNotification(AudioRecord recorder) {
	    rec_counter = (rec_counter + 1) % rec_sampling_rate;
	    
	    // ////////////////////////////////////////////////////
	    // /////// buffer contains the audio data /////////////
	    // ////////////////////////////////////////////////////
	    aRecorder.read(buffer, 0, buffer.length); // Fill buffer with
						      // available audio
	    lastFrameTimeStamp = System.currentTimeMillis();
	    //Log.e("AudioCapture","Got a Frame");
	    if(rec_counter!=0)
	    {
		//return;
	    }
	    if (!recordingStopped) {
		// put data in circular buffer for processing
		// you can do other stuffs with the data
		tempTimestamp = System.currentTimeMillis();
		++sync_id_counter;

		// input to cicular buffer
		cirBuffer.insert(new AudioData(buffer, tempTimestamp,
			sync_id_counter % 16384));

		payloadSize += buffer.length;
		updateFlag++;

	    } else {
		// no new data will be inserted at this stage
		// so we will activate the freeCMemory thread
		// this will wait if there is element in the queue
		// when the queue is free it will free the memory.
		// Since no data will
		// be inserted at this stage, thus free queue means
		// (earlier insert calls are completed at this stage)
		// no more data
		// audioFeatureExtractionDestroy();
//		if (freeCMemoryActivated == false) {
//		    freeCMemoryActivated = true;
//		    new Thread(new Runnable() {
//			public void run() {
//			    cirBuffer.freeCMemory();
//			}
//		    }).start();
//		}
	    }

	}

	public void onMarkerReached(AudioRecord recorder) {
	    // NOT USED
	}
    };

    // /////////////////////////////////////////////////////////////
    // ////////// Conversation detection codes:start /////////////
    // ////////////////////////////////////////////////////////////

    public boolean isInConversation()
    {
	return inCoversation;
    }
    

    // /////////////////////////////////////////////////////////////
    // ////////// Conversation detection codes:end /////////////
    // ////////////////////////////////////////////////////////////


    /**
     * 
     * 
     * Default constructor
     * 
     * Instantiates a new recorder, in case of compressed recording the
     * parameters can be left as 0. In case of errors, no exception is thrown,
     * but the state is set to ERROR
     * 
     */
    
    private static final int[] POSSIBLE_SAMPLE_RATES = {8000, 11025, 16000, 22050, 44100};
    public AudioManager(int audioSource, /*int sampleRate, */
	    int channelConfig, int audioFormat) {
	try {

		if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
		    bSamples = 16;
		} else {
		    bSamples = 8;
		}

		if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
		    nChannels = 1;
		} else {
		    nChannels = 2;
		}

		aSource = audioSource;
		
		int sampleRate = 44100;
		for (int rate : POSSIBLE_SAMPLE_RATES) {
			int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
	        if (bufferSize > 0) {
	            sampleRate = rate;
	            break;
	        }
		}
		sRate = sampleRate;
		aFormat = audioFormat;

		framePeriod = this.FRAME_STEP * FRAME_SIZE_MULTIPLIER;
		bufferSize = framePeriod * 100 * bSamples * nChannels / 8;
		if (bufferSize < AudioRecord.getMinBufferSize(sampleRate,
			channelConfig, audioFormat)) { // Check to make sure
						       // buffer size is not
						       // smaller than the
						       // smallest allowed one
		    bufferSize = AudioRecord.getMinBufferSize(sampleRate,
			    channelConfig, audioFormat);
		    // Set frame period and timer interval accordingly
		    framePeriod = bufferSize / (2 * bSamples * nChannels / 8);
		    Log.w(AudioManager.class.getName(),
			    "Increasing buffer size to "
				    + Integer.toString(bufferSize));
		}

		aRecorder = new AudioRecord(audioSource, sampleRate,
			channelConfig, audioFormat, bufferSize);
		if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
		    throw new Exception("AudioRecord initialization failed");
		aRecorder.setRecordPositionUpdateListener(updateListener);
		aRecorder.setPositionNotificationPeriod(framePeriod);
		this.updateFlag = 0;

		// add a new buffer for putting audio-stuff
		cirBuffer = new CircularBufferFeatExtractionInference<AudioManager.AudioData>(
			null, 400);

		// array puller
		audioBuffer = new short[audioBufferSize][FRAME_STEP * FRAME_SIZE_MULTIPLIER];

		// start a new thread for reading audio stuff
		myQueuePopper = new MyQueuePopper(cirBuffer);
		myQueuePopper.start();

		// write file init <--------------- Rifat commented these two
		// lines
		// fOut = new FileOutputStream("/sdcard/priv_audio_" +
		// System.currentTimeMillis() + ".txt");
		// osw = new OutputStreamWriter(fOut);

		// initialize percentage computation queue
		// total number of voiced frames in the last
		// minuteToLookBackForPopup minutes
		// 60*8000/128=3750
		circularQueueOfInference = new double[(int) (minuteToLookBackForPopup * 3750)];

		inCoversation = false;

		// if conversationIntentSent==false and if non-conversation is
		// found then we will send intent.
		// If conversationIntentSent==true then if non-conversation is
		// found then we will not send intent.
		conversationIntentSent = true;

	    cAmplitude = 0;
	    state = State.INITIALIZING;
	} catch (Exception e) {
	    if (e.getMessage() != null) {
		Log.e(AudioManager.class.getName(), e.getMessage());
	    } else {
		Log.e(AudioManager.class.getName(),
			"Unknown error occured while initializing recording");
	    }
	    state = State.ERROR;
	}
    }


    /**
     * 
     * Prepares the recorder for recording, in case the recorder is not in the
     * INITIALIZING state and the file path was not set the recorder is set to
     * the ERROR state, which makes a reconstruction necessary. In case
     * uncompressed recording is toggled, the header of the wave file is
     * written. In case of an exception, the state is changed to ERROR
     * 
     */
    public void prepare() {
	try {
	    if (state == State.INITIALIZING) {
	    	votes = new int[4];
		    if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED)) {
			buffer = new short[framePeriod * bSamples / 16
				* nChannels];
			state = State.READY;
		    } else {
			Log.e(AudioManager.class.getName(),
				"prepare() method called on uninitialized recorder");
			state = State.ERROR;
		    }

	    } else {
		Log.e(AudioManager.class.getName(),
			"prepare() method called on illegal state");
		release();
		state = State.ERROR;
	    }
	} catch (Exception e) {
	    if (e.getMessage() != null) {
		Log.e(AudioManager.class.getName(), e.getMessage());
	    } else {
		Log.e(AudioManager.class.getName(),
			"Unknown error occured in prepare()");
	    }
	    state = State.ERROR;
	}
    }

    public State getManagerState() {
	return state;
    }

    /**
     * 
     * 
     * Releases the resources associated with this class, and removes the
     * unnecessary files, when necessary
     * 
     */
    public void release() {
	if (state == State.RECORDING) {

	    stop();

	} 
	    if (aRecorder != null) {
		aRecorder.release();

		recordingStopped = true;
		// aRecorder = null;

		// audioFeatureExtractionDestroy();


	    }

    }

    /**
     * 
     * 
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped. In
     * case of exceptions the class is set to the ERROR state.
     * 
     */
    public void reset() {
	try {
	    if (state != State.ERROR) {
		release();
		cAmplitude = 0; // Reset amplitude

		aRecorder = new AudioRecord(aSource, sRate, nChannels + 1,
			    aFormat, bufferSize);
		state = State.INITIALIZING;
	    }
	} catch (Exception e) {
	    Log.e(AudioManager.class.getName(), e.getMessage());
	    state = State.ERROR;
	}
    }

    /**
     * 
     * 
     * Starts the recording, and sets the state to RECORDING. Call after
     * prepare().
     * 
     */
    public void start() {
		if (state == State.READY) {
	
			payloadSize = 0;
			audioFeatureExtractionInit();
			aRecorder.startRecording();
			aRecorder.read(buffer, 0, buffer.length);
			recordingStopped = false;
			freeCMemoryActivated = false;
	
		    state = State.RECORDING;
		} else {
		    Log.e(AudioManager.class.getName(),
			    "start() called on illegal state");
		    state = State.ERROR;
		}
		
		Log.d("AudioManager", "start");
    }
    
    public void startRecording() {
		synchronized(this) {
			try {
			    if(state == State.STOPPED) {
					aRecorder.startRecording();
					state = State.RECORDING;
					recordingStopped = false;
					aRecorder.read(buffer, 0, buffer.length);
			    }
			} catch (IllegalStateException ex) {
			    Log.e("AudioManager", "startRecording throws" + ex.toString());
			}
		}
	    Log.d("AudioManager", "startRecording");
    }

    /**
     * 
     * 
     * Stops the recording, and sets the state to STOPPED. In case of further
     * usage, a reset is needed. Also finalizes the wave file in case of
     * uncompressed recording.
     * 
     */
    public void stop() {
	if (state == State.RECORDING) {

		aRecorder.stop();

	    audioFeatureExtractionDestroy();
	    
	    state = State.STOPPED;
	} else {
	    Log.e(AudioManager.class.getName(),
		    "stop() called on illegal state");
	    state = State.ERROR;
	}
	
	Log.d("AudioManager", "stop");
    }

    public void restartRecording() {
	synchronized(this) {
            try {
                if(state==State.RECORDING){
            	aRecorder.stop();
                }
            	aRecorder.startRecording();
		state = State.RECORDING;
		recordingStopped = false;
		aRecorder.read(buffer, 0, buffer.length);
            } catch (IllegalStateException ex) {
                Log.e("AudioManager", "stopRecording throws" + ex.toString());
            }
	}
	
	Log.d("AudioManager", "restartRecording");
    }
    public void stopRecording() {
	synchronized(this) {
            try {
                if(state==State.RECORDING){
                    aRecorder.stop();
                    state = State.STOPPED;
                    recordingStopped = true;
                    sumOfPreviousInferences = 0;
                    for(int i=0; i<circularQueueOfInference.length;i++) {
                        circularQueueOfInference[i] = 0;
                    }
                    indexToCircularQueueOfInference = 0;
                }
            } catch (IllegalStateException ex) {
                Log.e("AudioManager", "stopRecording throws" + ex.toString());
            }
	}
	Log.d("AudioManager", "stopRecording");
    }



    /*
     * 
     * Converts a byte[2] to a short, in LITTLE_ENDIAN format
     */
    private short getShort(byte argB1, byte argB2) {
	return (short) (argB1 | (argB2 << 8));
    }
}
