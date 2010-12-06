package com.android.phone;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.BroadcastReceiver;
//import android.content.ContentResolver;
//import android.content.ContentUris;
//import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.res.Configuration;
//import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
//import android.net.Uri;
//import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
//import android.os.PowerManager;
import android.os.StatFs;
//import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
//import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
//import android.view.KeyEvent;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.SeekBar;
//import android.widget.TextView;
import android.widget.Toast;

class RemainingTimeCalculator {
    public static final int UNKNOWN_LIMIT = 0;
    public static final int FILE_SIZE_LIMIT = 1;
    public static final int DISK_SPACE_LIMIT = 2;
    
    // which of the two limits we will hit (or have fit) first
    private int mCurrentLowerLimit = UNKNOWN_LIMIT;
    
    private File mSDCardDirectory;
    
    // State for tracking file size of recording.
    private File mRecordingFile;
    private long mMaxBytes;
    
    // Rate at which the file grows
    private int mBytesPerSecond;
    
    // time at which number of free blocks last changed
    private long mBlocksChangedTime;
    // number of available blocks at that time
    private long mLastBlocks;
    
    // time at which the size of the file has last changed
    private long mFileSizeChangedTime;
    // size of the file at that time
    private long mLastFileSize;
    
    
    
    public RemainingTimeCalculator() {
        mSDCardDirectory = Environment.getExternalStorageDirectory();
    }    
    
    /**
     * If called, the calculator will return the minimum of two estimates:
     * how long until we run out of disk space and how long until the file
     * reaches the specified size.
     * 
     * @param file the file to watch
     * @param maxBytes the limit
     */
    
    public void setFileSizeLimit(File file, long maxBytes) {
    	mRecordingFile = file;
    	mMaxBytes = maxBytes;
    }
    
    /**
     * Resets the interpolation.
     */
    public void reset() {
    	mCurrentLowerLimit = UNKNOWN_LIMIT;
    	mBlocksChangedTime = -1;
    	mFileSizeChangedTime = -1;
    }
    
    /**
     * Returns how long (in seconds) we can continue recording. 
     */
    public long timeRemaining() {
    	// Calculate how long we can record based on free disk space
        
    	StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
    	long blocks = fs.getAvailableBlocks();
    	long blockSize = fs.getBlockSize();
    	long now = System.currentTimeMillis();
        
    	if (mBlocksChangedTime == -1 || blocks != mLastBlocks) {
    		mBlocksChangedTime = now;
    		mLastBlocks = blocks;
    	}

        /* The calculation below always leaves one free block, since free space
           in the block we're currently writing to is not added. This
           last block might get nibbled when we close and flush the file, but f
           we won't run out of disk. */
        
    	// at mBlocksChangedTime we had this much time
    	long result = mLastBlocks*blockSize/mBytesPerSecond;
    	// so now we have this much time
    	result -= (now - mBlocksChangedTime)/1000;
/*        
        if (mRecordingFile == null) {
            mCurrentLowerLimit = DISK_SPACE_LIMIT;
            return result;
        }
        
        // If we have a recording file set, we calculate a second estimate
        // based on how long it will take us to reach mMaxBytes.
        
        mRecordingFile = new File(mRecordingFile.getAbsolutePath());
        long fileSize = mRecordingFile.length();
        if (mFileSizeChangedTime == -1 || fileSize != mLastFileSize) {
            mFileSizeChangedTime = now;
            mLastFileSize = fileSize;
        }

        long result2 = (mMaxBytes - fileSize)/mBytesPerSecond;
        result2 -= (now - mFileSizeChangedTime)/1000;
        result2 -= 1; // just for safety
        
        mCurrentLowerLimit = result < result2
            ? DISK_SPACE_LIMIT : FILE_SIZE_LIMIT;
        
        return Math.min(result, result2);
        */
    	return result;
    }
    
    /**
     * Indicates which limit we will hit (or have hit) first, by returning one 
     * of FILE_SIZE_LIMIT or DISK_SPACE_LIMIT or UNKNOWN_LIMIT. We need this to 
     * display the correct message to the user when we hit one of the limits.
     */
    public int currentLowerLimit() {
    	return mCurrentLowerLimit;
    }

    /**
     * Is there any point of trying to start recording?
     */
    public boolean diskSpaceAvailable() {
    	StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
    	// keep one free block
    	return fs.getAvailableBlocks() > 1;
    }

    /**
     * Sets the bit rate used in the interpolation.
     *
     * @param bitRate the bit rate to set in bits/sec.
     */
    public void setBitRate(int bitRate) {
        mBytesPerSecond = bitRate/8;
    }
}


public class soundRecord {//implements Recorder.OnStateChangedListener{
	
	static 	final 	String 	TAG = "SoundRecorder";
	

	public 	Recorder 		mRecorder;	
	public 	AudioManager 	mAudioManager;
	public 	Cursor 			mCursor;
	private String 			recFileName;
	private Context 		mContext;

	static final String 		AUDIO_AMR 	= "audio/amr";
	static final int 		BITRATE_AMR	= 5900;// bits/sec
 	 	
	private static	RemainingTimeCalculator mRemainingTimeCalculator;
	static String mErrorUiMessage = null;
	String mTimerFormat;
 	
	//  final static Handler mHandler = new Handler();
	//  static Runnable mUpdateTimer = new Runnable() {
	final Handler mHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() { updateTimerView(); }

		private void updateTimerView() {
			int state = mRecorder.state();
			long time = mRecorder.progress();
	        
			PhoneUtils.setDuration(time);
			updateTimeRemaining();
			Log.d(TAG, "====== setDuration   ==> " + time );

	        // available time 
			mRemainingTimeCalculator.reset();
			mRemainingTimeCalculator.setBitRate(BITRATE_AMR);
	      
			long t = mRemainingTimeCalculator.timeRemaining();
			Log.d(TAG, "====== timeRemaining   ==> " + t );

	// LGSI_SUDHEENDRA   TD ID  32157 START
	// When memory  is  full ,While  playing any recorded file , Duration bar and duration time are not getting incremented
	// the T <=0 , is  not required for  playing state. as  t gives the time remaining for recording 
			if (t <= 0) {
	// LGSI_SUDHEENDRA TD ID  32157 END 
		    	//Toast display 
	        	return;
			}     
			if(PhoneUtils.isSoundRecording())
				mHandler.postDelayed(mUpdateTimer, 500);
		}

    };
    /*
     * Called when we're in recording state. Find out how much longer we can 
     * go on recording. If it's under 5 minutes, we display a count-down in 
     * the UI. If we've run out of time, stop the recording. 
     */
 //   private static void updateTimeRemaining() {
    private void updateTimeRemaining() {
    	long t = mRemainingTimeCalculator.timeRemaining();            
        if (t <= 0) {
//            mSampleInterrupted = true;

            int limit = mRemainingTimeCalculator.currentLowerLimit();
            switch (limit) {
                case RemainingTimeCalculator.DISK_SPACE_LIMIT:
                    mErrorUiMessage 
                        ="1";// getResources().getString(R.string.storage_is_full);
                    break;
                case RemainingTimeCalculator.FILE_SIZE_LIMIT:
                    mErrorUiMessage 
                        = "2";//getResources().getString(R.string.max_length_reached);
                    break;
                default:
                    mErrorUiMessage = null;
                    break;
            }
            mHandler.removeCallbacks(mUpdateTimer);
            mRecorder.stop();
            PhoneUtils.setSoundRecording(false);
            
            String toastMsg = mContext.getResources().getString(R.string.stopRecordText);
           	PhoneUtils.setToast( mContext, toastMsg );
            return;
        }            
        
    }

 	public soundRecord(){		
	    mRecorder = new Recorder();
	    mRemainingTimeCalculator = new RemainingTimeCalculator(); 
	}
/*
	public void onError(int error) {
     //   Resources res = getResources();
        
        String message = null;
        switch (error) {
            case Recorder.SDCARD_ACCESS_ERROR:
          //      message = res.getString(R.string.error_sdcard_access);
                break;
            case Recorder.INTERNAL_ERROR:
            //    message = res.getString(R.string.error_app_internal);
                break;
        }
        if (message != null) {
          //  new AlertDialog.Builder(this)
         //       .setTitle(R.string.app_name)
           //     .setMessage(message)
           //     .setPositiveButton(R.string.button_ok, null)
             //   .setCancelable(false)
             //   .show();
        }
    }
	@Override
	public void onStateChanged(int state) {
		// TODO Auto-generated method stub
		
	}
*/	
	public String initSoundRecord(InCallScreen mInCallScreen, String displayNum){		
		long 		mRecStartTime = System.currentTimeMillis();
		int 		fileCount;
		Resources 	res = mInCallScreen.getResources();
		
		String[] projection = new String[] {
			MediaStore.Audio.Media.DISPLAY_NAME,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.DATE_ADDED,
			MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.MIME_TYPE,
			MediaStore.Audio.Media._ID };
		
	    mRemainingTimeCalculator.setBitRate(BITRATE_AMR);
	    
		String mErrorUiMessage = null; // Some error messages are displayed in the UI, 
	    // not a dialog. This happens when a recording
	    // is interrupted for some reason.
        File path = new File(mRecorder.SELECTED_SD_CARD);
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        	Log.i(TAG,"!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)");
        	
        	mErrorUiMessage = res.getString(R.string.insert_sd_card);
    			
        	return mErrorUiMessage;
        }
        else if (!mRemainingTimeCalculator.diskSpaceAvailable())
        {
        	mErrorUiMessage = res.getString(R.string.storage_is_full);
        	return mErrorUiMessage;
        }
        else 
        {
        	if (!path.exists()&& Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        	{
        		path.mkdir();
        	}
        }
        
       
        fileCount = getFileCount(mInCallScreen, displayNum);
        recFileName = displayNum + "_" + fileCount;//mRecStartTime;//+ "_" + tempDate + "_" + tempHour ; //"010-2233-4545_X"형식으로 저장
        
        mRemainingTimeCalculator.setFileSizeLimit(
        		mRecorder.sampleFile(), -1);
        return null;
	}
	
	private int getFileCount(InCallScreen minCallScreen, String displayNum) {
	// TODO Auto-generated method stub
		String 	tempFileName;
		String[] projection = new String[] {
			MediaStore.Audio.Media.DISPLAY_NAME,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.DATE_ADDED,
			MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.MIME_TYPE,
			MediaStore.Audio.Media._ID };

		String condition =  MediaStore.Audio.Media.MIME_TYPE + "= '"
							+ AUDIO_AMR + "'" + " and " 
							+ MediaStore.Audio.Media.DISPLAY_NAME + " LIKE 'REC%'" + " and " 
							+ MediaStore.Audio.Media.DISPLAY_NAME + " LIKE '%" + displayNum + "%'";

		if (mCursor!= null) {
			/* Because "managedQuery" make a ManageCursor that has a ContentObserver, if the ManagedCursor doesn't be release,
			 ** it make a GREF leakage. So, we try to release the ManagedCursor by calling "stopManagingCursor" */
			minCallScreen.stopManagingCursor(mCursor);
			mCursor = null;
		}

		mCursor = minCallScreen.managedQuery(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				projection, condition, null, null);

		Log.i(TAG,"condition = [ " + condition +"]");
		Log.i(TAG,"makeFileName count = " + mCursor.getCount());
		
		return mCursor.getCount();
	}

	public boolean startRecording(Context context){
        mRecorder.clear();

		mContext = context;
        mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);	
		int mAudioMode = mAudioManager.getMode();
		int mAudioPath = mAudioManager.getRouting(mAudioMode);
		
		Log.d("kimeh", "Rocorder_AMR: M[" + mAudioMode +"], P[" + mAudioPath + "]");
		
		mRecorder.startRecording(MediaRecorder.OutputFormat.THREE_GPP, ".amr", recFileName);
		mHandler.postDelayed(mUpdateTimer, 500);

		return true;
	}

	public boolean stopRecording(InCallScreen minCallScreen){
		String toastMsg;
		Log.d("kimeh", "Rocorder_button: stopButton");
		
		if( mRecorder.progress() < 2 )
		{
			Log.w(TAG, "sampleLength is too small" +  mRecorder.progress());
			return false;
		}
		/* add the recorder path by kimeh@lge.com */
		
		mHandler.removeCallbacks(mUpdateTimer);
		mRecorder.stop();
		PhoneUtils.setSoundRecording(false);
		
		toastMsg = mContext.getResources().getString(R.string.saveRecordText);
		
		addToMediaDB(minCallScreen , mRecorder.sampleFile());
		PhoneUtils.setToast( mContext, toastMsg );
		return true;
	}


	/*
	 * Create a playlist with the given default playlist name, if no such playlist exists.
	 */
	private Uri createPlaylist(Resources res, ContentResolver resolver) {
		Log.i(TAG,"createPlaylist");
		ContentValues cv = new ContentValues();
		cv.put(MediaStore.Audio.Playlists.NAME, res.getString(R.string.audio_db_playlist_name));
		Log.i(TAG,"createPlaylist Playlists.NAME : " + res.getString(R.string.audio_db_playlist_name));
		Uri uri = resolver.insert(MediaStore.Audio.Playlists.getContentUri("external"), cv);
		if (uri == null) {
			// error msg
			return null;
		}
		Log.i(TAG,"createPlaylist uri : " + uri);
		
        return uri;
    }

  private boolean addToMediaDB( InCallScreen minCallScreen, File file) { 
	  	Log.i(TAG,"SoundRecorder, addToMediaDB <---------------");
		Log.i(TAG,"file = "+file.getAbsolutePath());
		
		Resources res = mContext.getResources();
		ContentValues cv = new ContentValues();
		long current = System.currentTimeMillis();
		long modDate = file.lastModified();
		
			
		// 20100920 kim.dukyeol@lge.com add DB - file size [START_LGE]
		long size = file.length();
		// 20100920 kim.dukyeol@lge.com add DB - file size [END_LGE]
			
		Date date = new Date(current);
		SimpleDateFormat formatter = new SimpleDateFormat(
				res.getString(R.string.audio_db_title_format));
		String title = formatter.format(date);
		int mDuration;
		mDuration = (int) mRecorder.sampleLength()*1000;
		// Lets label the recorded audio file as NON-MUSIC so that the file
		// won't be displayed automatically, except for in the playlist.
		cv.put(MediaStore.Audio.Media.IS_MUSIC, "1");
		// 20100920 kim.dukyeol@lge.com add DB - file size [START_LGE]
		cv.put(MediaStore.Audio.Media.SIZE, size);
		// 20100920 kim.dukyeol@lge.com add DB - file size [END_LGE]
			
		cv.put(MediaStore.Audio.Media.TITLE, title);
		Log.i(TAG, "TITLE :" + title);
		cv.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
		Log.i(TAG, "getAbsolutePath :" + file.getAbsolutePath());
		cv.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
		Log.i(TAG, "DATE_ADDED :" + (int) (current / 1000));
		
		cv.put(MediaStore.Audio.Media.DATE_MODIFIED, (int) (modDate / 1000));
		Log.i(TAG, "DATE_MODIFIED :" + (int) (modDate / 1000));
		
		cv.put(MediaStore.Audio.Media.MIME_TYPE, AUDIO_AMR);
		
		Log.i(TAG, "MIME_TYPE " + AUDIO_AMR);
		cv.put(MediaStore.Audio.Media.DURATION, mDuration);
		Log.i(TAG, "mDuration :" + mDuration);
		cv.put(MediaStore.Audio.Media.ARTIST,
				res.getString(R.string.audio_db_artist_name));
		Log.i(TAG, "ARTIST :" + res.getString(R.string.audio_db_artist_name));
		cv.put(MediaStore.Audio.Media.ALBUM,	
				res.getString(R.string.audio_db_album_name));
		Log.i(TAG, "ALBUM :" + res.getString(R.string.audio_db_album_name));
		Log.d(TAG, "Inserting audio record: " + cv.toString());
		
		ContentResolver resolver = mContext.getContentResolver();
		Uri base;
		base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Log.d(TAG, "ContentURI: " + base);
		Uri result = resolver.insert(base, cv);
		if (result == null) {
			// error msg
			return false;
		}
		if (getPlaylistId(minCallScreen, res) == -1) {
			createPlaylist(res, resolver);
		}
		int audioId = Integer.valueOf(result.getLastPathSegment());
		addToPlaylist(resolver, audioId, getPlaylistId(minCallScreen, res));
		
		return true;
  }
  /*
   * Add the given audioId to the playlist with the given playlistId; and maintain the
   * play_order in the playlist.
   */
  private void addToPlaylist(ContentResolver resolver, int audioId, long playlistId) {
	  String[] cols = new String[] {
			  "count(*)"
      };
	  Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
	  Cursor cur = resolver.query(uri, cols, null, null, null);
	  cur.moveToFirst();
	  final int base = cur.getInt(0);
	  cur.close();
	  cur = null;
	  ContentValues values = new ContentValues();
	  values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + audioId));
	  values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
	  resolver.insert(uri, values);
  }
  

  private int getPlaylistId(InCallScreen minCallScreen, Resources res) {
	  Uri uri = MediaStore.Audio.Playlists.getContentUri("external");
	  final String[] ids = new String[] { MediaStore.Audio.Playlists._ID };
	  final String where = MediaStore.Audio.Playlists.NAME + "=?";
	  final String[] args = new String[] { res.getString(R.string.audio_db_playlist_name) };
	  Cursor cursor = query(uri, ids, where, args, null);
	  if (cursor == null) {
		  Log.v(TAG, "query returns null");
	  }
	  int id = -1;
	  if (cursor != null) {
		  cursor.moveToFirst();
		  if (!cursor.isAfterLast()) {
			  id = cursor.getInt(0);
		  }
		  cursor.close();
		  cursor = null;
	  } 
	  return id;
  }
  private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
	  try {
		  ContentResolver resolver = mContext.getContentResolver();
		  if (resolver == null) {
			  return null;
		  }
		  return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
	  } catch (UnsupportedOperationException ex) {
		  return null;
	  }
  }
  public static Cursor query(Context context, Uri uri, String[] projection,
		  String selection, String[] selectionArgs, String sortOrder) {
	  return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
  }
  
  public static Cursor query(Context context, Uri uri, String[] projection,
		  String selection, String[] selectionArgs, String sortOrder, int limit) {
      try {
    	  ContentResolver resolver = context.getContentResolver();
    	  if (resolver == null) {
    		  return null;
    	  }
    	  if (limit > 0) {
    		  uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
    	  }
    	  return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
      } catch (UnsupportedOperationException ex) {
    	  return null;
      }
      
  }

  
}

