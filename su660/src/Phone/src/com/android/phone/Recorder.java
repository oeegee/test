package com.android.phone;


import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.util.Log;

public class Recorder implements OnCompletionListener, OnErrorListener {
//    static final String SAMPLE_PREFIX = "recording"; // 20100507 comespain@lge.com file naming
	static final String TAG = "Recorder";
    static String SAMPLE_PREFIX = "REC_"; // // 20100507 comespain@lge.com file naming
    static final String SAMPLE_PATH_KEY = "sample_path";
    static final String SAMPLE_LENGTH_KEY = "sample_length";

    public static final int IDLE_STATE = 0;
    public static final int RECORDING_STATE = 1;
    public static final int PLAYING_STATE = 2;
    
    int mState = IDLE_STATE;
    public static final int NO_ERROR = 0;
    public static final int SDCARD_ACCESS_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final String INTERNAL_SD = "/mnt/sdcard/SoundRecorder";
    public static final String EXTERNAL_SD = "/mnt/sdcard/_ExternalSD/SoundRecorder";
    public static String  SELECTED_SD_CARD = INTERNAL_SD;
    
    public interface OnStateChangedListener {
        public void onStateChanged(int state);
        public void onError(int error);
    }
    OnStateChangedListener mOnStateChangedListener = null;
    
    long mSampleStart = 0;       // time at which latest record or play operation started
    int mSampleLength = 0;      // length of current sample
    File mSampleFile = null;
    String mFileName = null;    // 20100512 comespain@lge.com File Naming
    String mFilePath = null;
    String mName = null;
    
    int mMaxDuration = 0;
    boolean bFromPlaying = false;
    boolean bPauseState = false;
    
    MediaRecorder mRecorder = null;
    MediaPlayer mPlayer = null;
    
    public Recorder() {
    }
    
    public void saveState(Bundle recorderState) {
        recorderState.putString(SAMPLE_PATH_KEY, mSampleFile.getAbsolutePath());
        recorderState.putInt(SAMPLE_LENGTH_KEY, mSampleLength);
    }
    
    public int getMaxAmplitude() {
        if (mState != RECORDING_STATE)
            return 0;
        return mRecorder.getMaxAmplitude();
    }
    
    public void restoreState(Bundle recorderState) {
        String samplePath = recorderState.getString(SAMPLE_PATH_KEY);
        if (samplePath == null)
            return;
        int sampleLength = recorderState.getInt(SAMPLE_LENGTH_KEY, -1);
        if (sampleLength == -1)
            return;

        File file = new File(samplePath);
        if (!file.exists())
            return;
        if (mSampleFile != null
                && mSampleFile.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0)
            return;
        
        delete();
        mSampleFile = file;
        mSampleLength = sampleLength;
        Log.i(TAG,"restoreState, mSampleLength = "+mSampleLength);
        signalStateChanged(IDLE_STATE);
    }
    
    public void setOnStateChangedListener(OnStateChangedListener listener) {
    	Log.i(TAG,"setOnStateChangedListener");
        mOnStateChangedListener = listener;
    }
    
    public int state() {
        return mState;
    }

      public int sampleLength() {
        return mSampleLength;
    }

    public File sampleFile() {
    	// 20100512 comespain@lge.com File Naming [START_LGE]
		if (mFileName == null) {
			return mSampleFile;
		} else {
			File recordedFile = new File(mFileName);
			return recordedFile;
		}
//        return mSampleFile; 
		// 20100512 comespain@lge.com File Naming [END_LGE]
    }
    
    public void setMaxDuration(int max){
    	mMaxDuration = max;
    }
    public int maxDuration(){
    	return mMaxDuration;
    }
    public boolean getPauseState(){
    	return bPauseState;
    }
    
    /**
     * Resets the recorder state. If a sample was recorded, the file is deleted.
     */
    public void delete() {
        stop();
        
        if (mSampleFile != null)
            mSampleFile.delete();

        mSampleFile = null;
        mSampleLength = 0;
        Log.i(TAG,"delete() mSampleLength");
        signalStateChanged(IDLE_STATE);
    }
    
    /**
     * Resets the recorder state. If a sample was recorded, the file is left on disk and will 
     * be reused for a new recording.
     */
    public void clear() {
        stop();
        
        mSampleLength = 0;
        Log.i(TAG,"clear() mSampleLength");
        mSampleFile = null;  // omimio@lge.com 20090729
        
        signalStateChanged(IDLE_STATE);
    }
    
    public void startRecording(int outputfileformat, String extension, String fileName) {
        stop();
        Log.i("SUDHIR_RECORDER"," 1 Recorder. startRecording");
        if (mSampleFile == null) {
            //File sampleDir = Environment.getExternalStorageDirectory();
		File sampleDir = null;
		sampleDir = new File(SELECTED_SD_CARD);
            if (!sampleDir.canWrite()) // Workaround for broken sdcard support on the device.
					// support on the device.
					sampleDir = new File(SELECTED_SD_CARD);
            
            try {
		 Log.i("SUDHIR_RECORDER","2 Recorder. startRecording");
                mSampleFile = File.createTempFile(SAMPLE_PREFIX, extension, sampleDir);
            } catch (IOException e) {
		 Log.i("SUDHIR_RECORDER"," 3 Recorder. startRecording");
            //    setError(SDCARD_ACCESS_ERROR);
                return;
            }
        }
        // 20100512 comespain@lge.com File Naming [START_LGE]
      //  DecimalFormat strIndex = new DecimalFormat("#0000");
      //  String number = strIndex.format(fileIndex);// 20100512 comespain@lge.com File Naming
        mFileName = mSampleFile.getParent()+"/REC_"+fileName+extension;
        
        // 20100512 comespain@lge.com File Naming [END_LGE]
        mRecorder = new MediaRecorder();
     	mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	//	mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);

	
        mRecorder.setOutputFormat(outputfileformat);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());// 20100512 comespain@lge.com File Naming
        mRecorder.setOutputFile(mFileName);// 20100512 comespain@lge.com File Naming
        mRecorder.setMaxDuration(mMaxDuration*1000);
        mRecorder.setOnInfoListener(new OnInfoListener() {
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    Log.d("Recorder","onInfo"+ what);

		    //if( what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED )
		    {
			 Log.i("SUDHIR_RECORDER"," 3.5 Recorder. startRecording");
                    	mRecorder.release();
                   	mRecorder = null;

                    	mSampleLength = (int)( (System.currentTimeMillis() - mSampleStart)/1000 );
                    	setState(IDLE_STATE);
		    }
                }
            });
	

        // Handle IOException
        try 

		{
		 Log.i("SUDHIR_RECORDER"," 4Recorder. startRecording");
            mRecorder.prepare();
	
        } catch(IOException exception) {
		 Log.i("SUDHIR_RECORDER"," 5 Recorder. startRecording");
            setError(INTERNAL_ERROR);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }
        try 
        {
        	mRecorder.start();
        	mSampleStart = System.currentTimeMillis();
        }catch (RuntimeException  ex){
        	Log.i("Recorder", "mRecorder.start() is failed");
            setError(INTERNAL_ERROR);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;        	
        	return; 
        }
		 Log.i("SUDHIR_RECORDER"," 6  Recorder. startRecording");
        mSampleStart = System.currentTimeMillis();
        setState(RECORDING_STATE);
    }
    
    public void stopRecording() {
    	Log.i(TAG,"stopRecording!!!!!!!!!");
        if (mRecorder == null)
            return;

        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        mSampleLength = (int)( (System.currentTimeMillis() - mSampleStart)/1000 );
        Log.i(TAG,"stopRecording(), mSampleLength = "+mSampleLength);
        setState(IDLE_STATE);
        mSampleFile.delete();
    }
     
    public void getDuration(File mFile){
    
        MediaPlayer mp = new MediaPlayer();

        if(mFile == null){
        	return;
        }
        try {
            mp.setDataSource(mFile.getAbsolutePath());
            mp.prepare();
            int duration = ((mp.getDuration()+500)/1000); 	// omimio, 20090902 
            Log.d("Recorder","Duration of file "  + duration );    
            mSampleLength = duration;
            Log.i(TAG,"getDuration(), mSampleLength = "+mSampleLength);
        }catch (IOException ex) {
        	
        }
        mp.release();
    }
    public int progress() {
        return (int) ((System.currentTimeMillis() - mSampleStart)/1000);
    }
     
    public void stop() {
        stopRecording();
  
    }
    public boolean onError(MediaPlayer mp, int what, int extra) {
        stop();
        setError(SDCARD_ACCESS_ERROR);
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
        stop();
    }

    private void setState(int state) {
        if (state == mState)
            return;
        
        mState = state;
        signalStateChanged(mState);
    }
    private void signalStateChanged(int state) {
        if (mOnStateChangedListener != null)
            mOnStateChangedListener.onStateChanged(state);
    }
    private void setError(int error) {
        if (mOnStateChangedListener != null)
            mOnStateChangedListener.onError(error);
    }
}
