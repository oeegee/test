package com.mtelo.visualexpression;

import com.android.internal.telephony.CallerInfo;
import com.android.phone.PhoneApp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;
import java.util.TimerTask;
import java.util.Timer;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.phone.R;

public class VE_ContentManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
{
	public final static int HANDLE_MSG_READY_PLAY = 4586;
	public final static int HANDLE_MSG_START_PLAY = 4587;
	public final static int HANDLE_MSG_STOP_PLAY = 4588;
	public final static int HANDLE_MSG_PAUSE_PLAY = 4589;
	public final static int HANDLE_MSG_RESUME_PLAY = 4590;
	public final static int HANDLE_MSG_INCALLSCREEN_IS_READY = 4591;
	public final static int HANDLE_MSG_DESTORY_VE = 4592;
	
	private final static int HANDLE_MSG_TOGGLE_PERSON_INFO = 4593;
	
	public final static int CLASS_STATE_NOTHING = 99;
	
	public final static String DOWNLOAD_PATH = "/data/ve/";
	
	public static Bitmap mBitmapData;


	
	private final int CONTENT_WIDTH = 320; //480;
	private final int CONTENT_HEIGHT = 240; //320;
	
	/**
	 * 2010.10.21 �ּ� ó��
	private final static int DOWNLOAD_THREAD_MS = 50;
	 */
	private final static int DECODE_MS = 100;

	private final static int CONNECTION_TIMEOUT = 10000;
	private final static int CONNECTION_READ_TIMEOUT = 10000;
	private final static int TOGGLE_TIME_MS = 2000;

	private final static long PROVIDE_MEMORY = (1024 * 500) * 10 ;
	private final static int MAX_CONTENT_SIZE = 1024 * 500;
	
	private final static int HANDLE_MSG = 4582;

	private final static int CLASS_STATE_GET_DOWNLOAD_FILE_SIZE = 100;
	private final static int CLASS_STATE_GET_DOWNLOAD_FILE_SIZE_DONE = 101;
	private final static int CLASS_STATE_GET_DOWNLOAD_FILE_SIZE_FAIL = 102;
	private final static int CLASS_STATE_DOWNLOAD_CONTENT_START = 104;
	private final static int CLASS_STATE_NO_ENOUGH_MEMORY = 105;
	private final static int CLASS_STATE_DOWNLOAD_CONTENT_DONE = 106;
	private final static int CLASS_STATE_DOWNLOAD_CONTENT_FAIL = 107;
	private final static int CLASS_STATE_CONTENT_EXIST = 108;
	private final static int CLASS_STATE_INIT_CODEC_COMPLETE = 109;
	private final static int CLASS_STATE_INIT_CODEC_FAILD = 110;
	private final static int CLASS_STATE_DO_CHECK = 111;
	private final static int CLASS_STATE_WAIT_INCALLSCREEN = 112;
	private final static int CLASS_STATE_IS_SKM = 113;
	private final static int CLASS_STATE_DOWNLOAD_CONTENT_FAIL_DISCONNECT = 114;
	private final static int CLASS_STATE_DOWNLOAD_CONTENT_DONE_DISCONNECT = 115;
	
	private final static String LOG_TAG = "VE_ContentManager";

	private final static String VE_PREFERENCES = "ve_preferences";

	private final static String KEY_USED_MEMORY = "key_used_memory";

	private Context mPhoneAppContext;
	
	private String mSaveFileName,
					mDownloadUrl = null,
					mContentFormat,
					mCallerNickName,
					mCallerName;

	private Handler mPhoneAppHandler;

	private HttpURLConnection mHttpcon;
	
	private int mClass_State,
				 mDownloadFile_Size,
				 mIncallscreen_State,
				 mBuffering_lop_i,
				 mPlay_lop_i;
	
	private long mUsed_Memory;
	
	private boolean isDownloadThread_Run,
						isBufferingThread_Run,
						isPlayThread_Run,
						isOnPause,
						isIncallscreenReady,
						isFileExists,
						isCallEnd = false,
						isTurnIsToggleName/*,
						isAudioStreamMuteOn,
						stopOEMRingtone*/;

	private SharedPreferences mSharedPreferences;
	
	private Editor mEditor;

	private AMF_Player mAMF_Player;
	
	private AMF_Viewer mAmf_Viewer;
	
	private static VE_ContentManager _VE_ContentManager = null;

	private ViewGroup mCallCardPersonInfoVE_ViewGroup;
	
	private TextView mNameTextView,
					/*mNickNameTextView,*/
					mLabel,
					mPhoneNumTextView;

	private VideoView mVideoView = null;
	
	private Timer mToggleTimer = null;
	
	/**
	 * @author enclosing_method / 2010. 11. 23. / ���� 11:30:33 / mail to -> feb7711@mtelo.com
	 * ���� ���� ��带 Ȯ�� �ϱ����ؼ� ����Ѵ�. 
	 */
	private AudioManager mAudioMgr = null; 	

	/**
	 * @author enclosing_method / 2010. 11. 25. / ���� 10:13:25 / mail to -> feb7711@mtelo.com
	 * ���� ������� ��Ʈ�� Ÿ���� ��� ����. 
	private int mAudioStreamType;
	 */

	private MediaPlayer mMediaPlayer;

	/*
	public VE_ContentManager(Context context)
	{
		MyLog.set_LogEnable(true, "wang");
		MyLog.write(Log.DEBUG, LOG_TAG, "VE_ContentManager("+context+")");
		mIncallScreenContext = context;
	}
	
	public void testFunc_createFile()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "testFunc_createFile()");
		boolean iscontentexist;
		iscontentexist = isContentExist("http://211.115.5.71:7072/MRBT/123/A/123/A12300000517_41_42.am3");
		MyLog.write(Log.DEBUG, LOG_TAG, "isContentExist() return = " + iscontentexist);
		
		if ( iscontentexist == false )
		{
			try
			{
				FileOutputStream os = new FileOutputStream( new File(DOWNLOAD_PATH  + mSaveFileName) );
			} 
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				MyLog.write(Log.ERROR, LOG_TAG, "open file error = " + e.getMessage() );
			}
		}
	}
	*/
	
	public void init(Context context, Handler msghandle, String downloadurl)
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "init("+ context +","+ msghandle+", "+ downloadurl+")");
		
		if ( isCallEnd == false ) // ������ ������ �ٿ�ε� �޴� ���� �ƴϴ�.
		{
			/*
			"http://211.115.5.71:7072/MRBT/123/Z/163/Z16300000163_35_96.skm";
			 */
			
			/**
			 * 2010.10.23 �߼� ó��
			 * 
			if ( (System.currentTimeMillis() % 2) == 1 )
			{
				MyLog.write( Log.INFO, LOG_TAG, "this turn is play skm" );
				downloadurl = "http://211.115.5.71:7072/MRBT/123/Z/163/Z16300000163_35_96.skm";
			}
			else
			{
				MyLog.write( Log.INFO, LOG_TAG, "this turn is play am3" );
			}
			 */
			
			/**
			 * 2010.10.23 ���� ǥ���� ó�� ���� 
			 * 
			 * �ȳ� �ϼ���.. StringŬ������ matches()�޼ҵ�� ����ǥ������
			 * ����մϴ�. �Ʒ��� ���� ����ǥ�������� �־��ָ� ���ǹ��� �����Ǵ°� Ȯ���� �� ������ ����ǥ������ �ڹ� API��
			 * java.util.regex.PatternŬ���� ������ ���ø� �ݹ� ���� �� �����̴ϴ�. 
			 * String a = ".*��ġ.*"; 
			 * String b = "��ġ�"; 
			 * if(b.matches(a)) 
			 * {
			 * System.out.println("�߰�"); 
			 * }
			 */
			
			mPhoneAppContext = context;
			mPhoneAppHandler = msghandle;
			mDownloadUrl = downloadurl;
			
			isDownloadThread_Run = false;
			isBufferingThread_Run = false;
			isPlayThread_Run = false;
			isOnPause = false;
			isIncallscreenReady = false;
			isFileExists = false;
//			isAudioStreamMuteOn = false;
			
			if ( mAMF_Player != null )
			{
				mAMF_Player.releaseLib();
				mAMF_Player = null;
			}
			
			/**
			 * @author init / 2010. 11. 19. / ���� 3:06:58 / mail to -> feb7711@mtelo.com 
			 * �ּ� ó��. videoview�� �ѹ� �ε��ϸ� ��� �ڿ��� Ȱ���Ѵ�
			if ( mVideoView != null )
				mVideoView = null;
			 */

			if ( mToggleTimer != null )
			{
				mToggleTimer.cancel();
				mToggleTimer = null;
				mCallerNickName = mCallerName = "";
			}
			
			set_Class_State(CLASS_STATE_NOTHING);
			
			mSharedPreferences = mPhoneAppContext.getSharedPreferences(VE_PREFERENCES, Activity.MODE_PRIVATE);
			mEditor = mSharedPreferences.edit(); 
			
			/**
			 * @author init / 2010. 11. 23. / ���� 11:31:14 / mail to -> feb7711@mtelo.com 
			 * ���� ���� ��带 Ȯ�� �ϱ����ؼ� ����Ѵ�. 
			 */
			if ( mAudioMgr == null )
				mAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

			/**
			 * @author init / 2010. 11. 25. / ���� 10:18:09 / mail to -> feb7711@mtelo.com
			 * ���� ������� ��Ʈ�� Ÿ���� ��� ����.  
			mAudioStreamType = RingtoneManager.getRingtone(context, RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)).getStreamType();
			 */
			
			if ( isIncallscreenReady )
				doCheck();
		}
		else // ������ ������ �ٿ�ε� �޴� ���̴�.
		{
			MyLog.write(Log.WARN, LOG_TAG, "befor content download was not done !!! return ");
		}
	}
	
	public VE_ContentManager()
	{
		MyLog.set_LogEnable(false, "wang");
		MyLog.write(Log.DEBUG, LOG_TAG, "VE_ContentManager()");
	}
	
	/*
	public VE_ContentManager(Context context, Handler msghandle, String downloadurl) 
	{
		MyLog.set_LogEnable(true, "wang");
		MyLog.write(Log.DEBUG, LOG_TAG, "VE_ContentManager("+ context+", "+ msghandle+", "+ downloadurl+")");
		
		mIncallScreenContext = context;
		mIncallScreenHandler = msghandle;
		mDownloadUrl = downloadurl;
		
		isDownloadThread_Run = false;
		isBufferingThread_Run = false;
		isPlayThread_Run = false;
		isOnPause = false;
		isIncallscreenReady = false;
		
		mSharedPreferences = mIncallScreenContext.getSharedPreferences(VE_PREFERENCES, Activity.MODE_PRIVATE);
		mEditor = mSharedPreferences.edit(); 
		
		_VE_ContentManager = this;
		
		set_Class_State(CLASS_STATE_NOTHING);
	}
	*/
	
	private void doCheck()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "doCheck()");
		
		set_Class_State(CLASS_STATE_DO_CHECK);
		
		/**
		 * 2010.10.27 �ڵ� ����
		mDownloadUrl = "http://211.115.5.71:7072/MRBT/123/A/123/A12300000517_41_42.am3";
		 */
		if ( mDownloadUrl == null )
		{
			MyLog.write(Log.DEBUG, LOG_TAG, "download url is null");
			return;
		}
		else
		{
			/**
			 * 2010 10 25 �߰� �ڵ�
			 * DMF�� ���Դ� �����Ѵ�.
			 */
			String format = mDownloadUrl.substring(mDownloadUrl.lastIndexOf(".") + 1);
			if ( format.equalsIgnoreCase("dmf") )
			{
				MyLog.write(Log.WARN, LOG_TAG, "unsupported dmf format");
				return;
			}
		}
		
		if ( isContentExist(mDownloadUrl) == true ) // ������ DB�� ���� �� ��� .... 
		{
			ve_Handler.sendEmptyMessage(CLASS_STATE_CONTENT_EXIST);
		}
		else // ������ DB�� ���� ���� �ʴ´�. �ٿ�ε� ���� ....
		{
			ve_Handler.sendEmptyMessage(CLASS_STATE_GET_DOWNLOAD_FILE_SIZE);
		}
	}
	
	private Handler ve_Handler = new Handler()
	{

		public void handleMessage(Message msg)
		{
			if ( msg.what < HANDLE_MSG )
				set_Class_State(msg.what);
			
			switch ( msg.what )
			{
				case CLASS_STATE_GET_DOWNLOAD_FILE_SIZE: 
					MyLog.write(Log.INFO, LOG_TAG, "start get download content file size info");
					new Thread(getDownloadContentSize_Runnable).start();
				break;
				
				
				case CLASS_STATE_GET_DOWNLOAD_FILE_SIZE_FAIL:
					MyLog.write(Log.ERROR, LOG_TAG, "get download content file size info fail");
					break;
					
					
				case CLASS_STATE_GET_DOWNLOAD_FILE_SIZE_DONE:
					MyLog.write(Log.INFO, LOG_TAG, "get download content file size info done, mDownloadFile_Size = " + mDownloadFile_Size);
					check_FreeMemory();
					break;
					
					
				case CLASS_STATE_DOWNLOAD_CONTENT_START:
					MyLog.write(Log.ERROR, LOG_TAG, "start download content file");
					new Thread(download_Content_Runnable).start();
					break;
					
					
				case CLASS_STATE_NO_ENOUGH_MEMORY:
					MyLog.write(Log.ERROR, LOG_TAG, "no enough memory for save content file");
					removeContent(mDownloadFile_Size);
					break;
					
					
				case CLASS_STATE_DOWNLOAD_CONTENT_FAIL: // �ٿ�ε� ���д�. incallscreen���� ������ ������
					MyLog.write(Log.ERROR, LOG_TAG, "download content fail");
					deleteContentFile(DOWNLOAD_PATH + mSaveFileName);
					break;
					
					
				case CLASS_STATE_DOWNLOAD_CONTENT_DONE: // �ٿ�ε� �Ϸ��. ��� �� �غ� ����
					MyLog.write(Log.INFO, LOG_TAG, "download content done");
					insertContentDB(mSaveFileName, mDownloadFile_Size);
					initAmfPlayer();
					break;
					
					
				case CLASS_STATE_CONTENT_EXIST:
					MyLog.write(Log.INFO, LOG_TAG, "content exist");
					initAmfPlayer();
					break;
					
					
				case CLASS_STATE_INIT_CODEC_COMPLETE:
					MyLog.write(Log.INFO, LOG_TAG, "codec load complete");
					initBuffer();
					sendMsgToIncallscreenReadyToPlay();
					break;
					
					
				case CLASS_STATE_INIT_CODEC_FAILD: // �ڵ� �ε� ���д�. incallscreen���� ������ ������
					MyLog.write(Log.INFO, LOG_TAG, "codec load faild");
					removeUnusualContent();
					break;
					
					
				case CLASS_STATE_WAIT_INCALLSCREEN:
					MyLog.write(Log.INFO, LOG_TAG, "wait incallscreen created");
					break;
					
					
				case CLASS_STATE_IS_SKM:
					MyLog.write(Log.INFO, LOG_TAG, "is skm file format");
					sendMsgToIncallscreenReadyToPlay();
					break;
					
					
				case CLASS_STATE_DOWNLOAD_CONTENT_FAIL_DISCONNECT:
					MyLog.write(Log.ERROR, LOG_TAG, "download content fail and disconnect");
					deleteContentFile(DOWNLOAD_PATH + mSaveFileName);
					break;
					
					
				case CLASS_STATE_DOWNLOAD_CONTENT_DONE_DISCONNECT:
					MyLog.write(Log.ERROR, LOG_TAG, "download content done and disconnect");
					insertContentDB(mSaveFileName, mDownloadFile_Size);
					break;
					
					
				case HANDLE_MSG_START_PLAY: // ������ ����� ���� �ض� 
					if ( mContentFormat.equalsIgnoreCase("skm") ) // SKM �����̴�.
					{
						start_Play_SKM();
					}
					else if ( mContentFormat.equalsIgnoreCase("am3") )// AM3 �����̴�.
					{
						start_Play_AM3();
					}
					break;
					
					
				case HANDLE_MSG_STOP_PLAY:
					MyLog.write(Log.INFO, LOG_TAG, "stop play content");
					isPlayThread_Run = false;
					isBufferingThread_Run = false;
					
					/**
					 * 2010.10.28 �ڵ� �߰�
					 * VE ����� ���ߴ� �������� �ٿ�ε� URL ������ ���� ������ null ó�� �Ѵ�.
					 */
					mDownloadUrl = null;
					
					/**
					 * 2010.10.22 �ڵ� �߰�
					 * ������ �ٿ�ε� ���̸� isCallEnd�� true�� ���� ���´�
					 */
					isCallEnd = isDownloadThread_Run;
					
					if (mAMF_Player != null)
					{
						mAmf_Viewer.setAmfViewerToGone();
						
						/**
						 * 2010.10.27 �ּ� ó��
						mAMF_Player.stop_Player();
						mAMF_Player = null;
						 */
					}
					
					if (mVideoView != null)
					{
						if ( mVideoView.isPlaying() )
						{
							mVideoView.stopPlayback(); 
						}
						
						mVideoView.setVisibility(View.GONE);

						/**
						 * @author init / 2010. 11. 19. / ���� 3:06:58 / mail to -> feb7711@mtelo.com 
						 * �ּ� ó��. videoview�� �ѹ� �ε��ϸ� ��� �ڿ��� Ȱ���Ѵ�
						mVideoView = null; 
						 */
					}

					/**
					 * 2010.11.10 add code
					 */
					if (mToggleTask != null)
					{
						mToggleTask.cancel();
						mToggleTask = null;
					}
					
					if ( mToggleTimer != null )
					{
						mToggleTimer.cancel();
						mToggleTimer = null;
						mCallerNickName = mCallerName = "";
					}
					
					/**
					 * @author handleMessage / 2010. 11. 25. / ���� 8:04:34 / mail to -> feb7711@mtelo.com 
					 */
					if ( mMediaPlayer != null )
					{
						mMediaPlayer.release();
					}
					
					/**
					 * @author handleMessage / 2010. 11. 25. / ���� 10:33:59 / mail to -> feb7711@mtelo.com 
					if ( isAudioStreamMuteOn )
					{
						isAudioStreamMuteOn = false;
						mAudioMgr.setStreamMute(AudioManager.STREAM_MUSIC, isAudioStreamMuteOn);
					}
					 */
					break;
					
					
				case HANDLE_MSG_PAUSE_PLAY:
					MyLog.write(Log.INFO, LOG_TAG, "pause play content");
					isOnPause = true;
					break;
					
					
				case HANDLE_MSG_RESUME_PLAY:
					MyLog.write(Log.INFO, LOG_TAG, "resume play content");
					isOnPause = false;
					break;
					
					
				case HANDLE_MSG_INCALLSCREEN_IS_READY:
					MyLog.write(Log.INFO, LOG_TAG, "incallscreen is ready");
					isIncallscreenReady = true;
					
					try
					{
						mCallCardPersonInfoVE_ViewGroup = PhoneApp.getInstance().mInCallScreen.mCallCard.callCardPersonInfoVE;
						
						if (mCallCardPersonInfoVE_ViewGroup != null)
						{
							set_CallerInfo_To_Control();
						}
						else
						{
							MyLog.write(Log.WARN, LOG_TAG, "mCallCardPersonInfoVE_ViewGroup is null");
						}
					}
					catch ( NullPointerException e )
					{
						e.printStackTrace();
					}
					
					if ( mDownloadUrl != null )
						doCheck();
					break;
					
					
					/**
					 * @author handleMessage / 2010. 11. 16. / ���� 11:12:58 / mail to -> feb7711@mtelo.com 
					 */
				case HANDLE_MSG_TOGGLE_PERSON_INFO:
					if ( mNameTextView != null )
						mNameTextView.setText( isTurnIsToggleName == true ? mCallerName : mCallerNickName );
					
					if ( isTurnIsToggleName )		isTurnIsToggleName = false;
					else							isTurnIsToggleName = true;
					break;
			}
			
			super.handleMessage(msg);
		}
		
	};
	
	
	
	/**
	 * # 1. �ٿ�ε� ���� ������ data/ve ������ ���� �ϴ��� Ȯ�� �Ѵ�.
	 * @param downloadurl
	 * @return
	 */
	private boolean isContentExist(String downloadurl)
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "isContentExist("+ downloadurl +")");
		
		int filesize = 0;
		boolean isExists = false;

		mSaveFileName = downloadurl.substring(downloadurl.lastIndexOf("/") + 1);
		mContentFormat = mSaveFileName.substring(mSaveFileName.lastIndexOf(".") + 1);
		
		filesize = Integer.parseInt( mSharedPreferences.getString(mSaveFileName, "0") );
		mUsed_Memory = mSharedPreferences.getLong(KEY_USED_MEMORY, 0);
		
		MyLog.write(Log.DEBUG, LOG_TAG, " mUsed_Memory = " + mUsed_Memory);
		
		if ( filesize > 0 ) // DB�� �����Ͱ� ���� ��
		{
			isExists = new File(DOWNLOAD_PATH + mSaveFileName).exists();
			
			if ( isExists == false ) // DB���� �����Ͱ� ���縦 ������, data/ve���� ������ ���� ���� �ʴ´�
			{
				mUsed_Memory -= filesize;
				
				mEditor.remove(mSaveFileName);
				mEditor.putLong(KEY_USED_MEMORY, mUsed_Memory);
				mEditor.commit();

				MyLog.write(Log.DEBUG, LOG_TAG, "DB have data, but file not in the DIR. update mUsed_Memory = " + mUsed_Memory);
			}
		}
		else  // DB�� �����Ͱ� ���� ����
		{
			isExists = new File(DOWNLOAD_PATH + mSaveFileName).exists();
			
			if ( isExists ) // DB���� �����Ͱ� ���縦 ���� ������, data/ve���� ������ ���縦 �Ѵ�
			{
				isExists = false;
				isFileExists = true;
			}
		}
		
		return isExists;
	}
	

	/**
	 * 
	 * @return �ٿ�ε� ���� ������ ����� ��´�
	 */
	private int getDownloadContentSize()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "getDownloadContentSize()");
		
		try
		{
			mHttpcon = (HttpURLConnection) new URL(mDownloadUrl).openConnection();
			mHttpcon.setConnectTimeout(CONNECTION_TIMEOUT);
			mHttpcon.setReadTimeout(CONNECTION_READ_TIMEOUT);

			if (mHttpcon.getResponseCode() == HttpURLConnection.HTTP_OK)
				return mHttpcon.getContentLength();
		} 
		/**
		 * @author getDownloadContentSize / 2010. 11. 17. / ���� 8:02:11 / mail to -> feb7711@mtelo.com 
		 */
		catch (SocketTimeoutException e)
		{
			e.printStackTrace();
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return 0;
	}
	
	
	private Runnable getDownloadContentSize_Runnable = new Runnable()
	{
		public void run()
		{
			boolean bCheck = true;
			mDownloadFile_Size = getDownloadContentSize();
			
			/**
			 * 2010.10.22 �߰� �ڵ�
			 * DB�� �����Ͱ� ������, ������ ���� �� ��쿡 ���� ó��
			 */
			if ( isFileExists )
			{
				long length = new File( DOWNLOAD_PATH + mSaveFileName ).length();
				
				if ( length > 0 )
				{
					if ( length == mDownloadFile_Size ) // ������ ���Ϸ� ���ָ� �ϰ� DB Update �Ѵ�
					{
						MyLog.write(Log.DEBUG, LOG_TAG, "DB no have data, but file in the DIR");
						insertContentDB(mSaveFileName, (int)length);
						ve_Handler.sendEmptyMessage(CLASS_STATE_CONTENT_EXIST);
						
						bCheck = false;
					}
				}
			}

			if ( bCheck )
			{
				if ( mDownloadFile_Size == 0 )
					ve_Handler.sendEmptyMessage(CLASS_STATE_GET_DOWNLOAD_FILE_SIZE_FAIL);
				else
					ve_Handler.sendEmptyMessage(CLASS_STATE_GET_DOWNLOAD_FILE_SIZE_DONE);
			}
		}
	};
	
	private void download_Content()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "download_Content()");
		
		/**
		 * 2010.10.21 �߰� �ڵ�
		 * ��������� ������ �� �Ǵµ�.. �׷��� �ʴ� ��� �ٿ�ε带 �� �޴´�... ������ ����?
		 */
		sleep(500);
		
		InputStream is = null;
		String savename = DOWNLOAD_PATH + mSaveFileName;
		int TotalReadLen = 0;
		
		try
		{	
			is = mHttpcon.getInputStream();			
			FileOutputStream os = new FileOutputStream( new File(savename) );
			
			int nReadLen = 0;
			int navailable = is.available();
			
			if (navailable > 0)
			{
				byte buff[] = new byte[navailable];
				
				while ( isDownloadThread_Run == true )
				{
					nReadLen = is.read(buff);
					
					if (nReadLen > 0)
					{
						os.write(buff, 0, nReadLen);
						TotalReadLen += nReadLen;
					}
					else
					{
						MyLog.write(Log.DEBUG, LOG_TAG, "End Download");
						break;
					}
					
					/**
					 * 2010.10.21 �ּ� ó�� ��� ������ �ٿ�ε� �ӵ��� �������� �����̴�..
					sleep(DOWNLOAD_THREAD_MS);
					 */
				}
			}

			os.close();
			is.close();

			mHttpcon.disconnect();
			mHttpcon = null;
		}
		/**
		 * @author download_Content / 2010. 11. 17. / ���� 8:03:34 / mail to -> feb7711@mtelo.com 
		 */
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (SocketTimeoutException e)
		{
			e.printStackTrace();
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();	
		}
		
		/**
		 * 2010.10.22 �ڵ� �߰�
		 * �ٿ�ε尡 �Ϸ�Ǹ� isDownloadThread_Rund�� ���� false�� ���� ���ش�.
		 */
		isDownloadThread_Run = false;
		
		if ( isCallEnd == true )
		{
			if ( TotalReadLen < mDownloadFile_Size )
			{
				MyLog.write(Log.WARN, LOG_TAG, "TotalReadLen("+TotalReadLen+") < mDownloadFile_Size("+mDownloadFile_Size+")");
				ve_Handler.sendEmptyMessage(CLASS_STATE_DOWNLOAD_CONTENT_FAIL_DISCONNECT);
			}
			else
			{
				ve_Handler.sendEmptyMessage(CLASS_STATE_DOWNLOAD_CONTENT_DONE_DISCONNECT);
			}
			isCallEnd = false;
		}
		else
		{
			if ( TotalReadLen < mDownloadFile_Size )
			{
				MyLog.write(Log.WARN, LOG_TAG, "TotalReadLen("+TotalReadLen+") < mDownloadFile_Size("+mDownloadFile_Size+")");
				ve_Handler.sendEmptyMessage(CLASS_STATE_DOWNLOAD_CONTENT_FAIL);
			}
			else
			{
				ve_Handler.sendEmptyMessage(CLASS_STATE_DOWNLOAD_CONTENT_DONE);
			}
		}
	}

	private Runnable download_Content_Runnable = new Runnable()
	{
		public void run()
		{
			isDownloadThread_Run = true;
			download_Content();
		}
	};

	
	/**
	 * incallscreen�� ���¸� incallscreen���� ���� �Ѵ�.
	 * @param s
	 */
	public void set_Incallscreen_State(int s)
	{
		mIncallscreen_State = s;
	}
	
	private void check_FreeMemory()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "check_FreeMemory()");
		
		if ( mUsed_Memory + mDownloadFile_Size + MAX_CONTENT_SIZE < PROVIDE_MEMORY )
			ve_Handler.sendEmptyMessage(CLASS_STATE_DOWNLOAD_CONTENT_START);
		else
			ve_Handler.sendEmptyMessage(CLASS_STATE_NO_ENOUGH_MEMORY);
	}
	
	private void insertContentDB(String contentname, int contentsize)
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "updateContentDB("+ contentname+", "+ contentsize+")");
		
		mUsed_Memory += contentsize;
		mEditor.putString(contentname, Integer.toString(contentsize));
		mEditor.putLong(KEY_USED_MEMORY, mUsed_Memory);
		mEditor.commit();
		
		MyLog.write(Log.DEBUG, LOG_TAG, "update used memory = " + mUsed_Memory );
	}
	
	private void removeContent(int needsize)
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "removeContent("+ needsize+")");
		
		int nremoved_size = 0,
			nfilesize = 0;

		Map<String, ?> ve_map = mSharedPreferences.getAll();
		
		if ( ve_map.isEmpty() == false )
		{
			for ( String filename : ve_map.keySet() )
			{
				if ( filename.equalsIgnoreCase(KEY_USED_MEMORY) == false )
				{
					nfilesize = Integer.parseInt( mSharedPreferences.getString(filename, "0") );
					
					if (nfilesize > 0)
					{
						if ( deleteContentFile(DOWNLOAD_PATH + filename) == false )
						{
							MyLog.write(Log.DEBUG, LOG_TAG, filename + " file delete faild ");
						}
						else
						{
							mEditor.remove(filename);
							
							/**
							 * 2010.10.21 �ּ�ó�� 
							mEditor.putLong(KEY_USED_MEMORY, mUsed_Memory - nfilesize);
							 */
							
							nremoved_size += nfilesize;
							if ( nremoved_size > needsize )
								break;
						}
					}
				}
			}
		}
		else
		{
			MyLog.write(Log.DEBUG, LOG_TAG, "ve_map.isEmpty()");
		}

		/**
		 * 2010.10.21 �߰� �ڵ�
		 */
		if ( nremoved_size > 0 )
		{
			mUsed_Memory -= nremoved_size;
			mEditor.putLong(KEY_USED_MEMORY, mUsed_Memory);
		}

		mEditor.commit();
		ve_Handler.sendEmptyMessage(CLASS_STATE_DOWNLOAD_CONTENT_START);
	}
	
	private void initAmfPlayer()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "initAmfPlayer()");
		

		if (mContentFormat.equalsIgnoreCase("skm")) // skm�� ��� amfcodec�� ��� �� �ʿ䰡 ����
		{
			MyFile.set_FileAuthority( DOWNLOAD_PATH + mSaveFileName, MyFile.AUTHORITY_FILE );
			
			/**
			 * @author initAmfPlayer / 2010. 11. 29. / ���� 3:09:26 / mail to -> feb7711@mtelo.com 
			 * ���� ��Ȳ���δ� skm ������ ��쿡�� skm ������ ��� ���ϱ�� ��.
			 * �׷��� �ϱ� �Լ��� �ʿ� ����. �ּ� ó��
			check_SoundTrack_from_skm();
			 */
			
			ve_Handler.sendEmptyMessage(CLASS_STATE_IS_SKM);
		}
		else
		{
			mAMF_Player = new AMF_Player();
			
			/**
			 * 2010.10.27 �ּ� ó��
			if (mContentFormat.equalsIgnoreCase("dmf"))
				mAMF_Player.setmContentFmt(AMF_Player.ContentFormat.DMF);
			else 
				mAMF_Player.setmContentFmt(AMF_Player.ContentFormat.AM3);
			 */

			int error = mAMF_Player.init_player(DOWNLOAD_PATH + mSaveFileName, CONTENT_WIDTH, CONTENT_HEIGHT);
			
			if ( error < 0 ) // �ڵ��ε� ������ ...
			{
				ve_Handler.sendEmptyMessage(CLASS_STATE_INIT_CODEC_FAILD);
			}
			else
			{				
				ve_Handler.sendEmptyMessage(CLASS_STATE_INIT_CODEC_COMPLETE);
			}
		}
		
		/*
		if ( mContentFormat.equals("skm") == false )
		{
			AmfCodec.JNICAmfLib();
			AmfCodec.JNIInitialize();
			AmfCodec.JNISetWorkDirectory( DOWNLOAD_PATH );
			
			AmfCodec.JNIDecode( DOWNLOAD_PATH + mSaveFileName );
			AmfCodec.JNIInitPlay( CONTENT_WIDTH , CONTENT_HEIGHT );
			AmfCodec.JNIInitBuffer( CONTENT_WIDTH , CONTENT_HEIGHT );
			
			if (AmfCodec.JNIGetClipCnt() > 0)
			{
				if (AmfCodec.JNIIsBGM())
				{
					if (AmfCodec.JNIGetBGM( DOWNLOAD_PATH + VE_BGM_MP3 ) == 0)
					{
						MyLog.write(Log.DEBUG, LOG_TAG, "have sound content");	
						
						if ( PhoneApp.getInstance().getRinger().isRinging() )
						{
							PhoneApp.getInstance().getRinger().stopRing();
							PhoneApp.getInstance().getRinger().setCustomRingtoneUri( Uri.fromFile(new File( DOWNLOAD_PATH + VE_BGM_MP3 )) );
							PhoneApp.getInstance().getRinger().ring();
						}
					}
					else
					{
						MyLog.write(Log.DEBUG, LOG_TAG, "no sound content 1");	
					}
				}
				else
				{
					MyLog.write(Log.DEBUG, LOG_TAG, "no sound content 2");	
				}
			}
		}
		*/
	}

	
	private void sendMsgToIncallscreenReadyToPlay()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "sendMsgToIncallscreenReadyToPlay()");	
		
		/*
		Message msg = new Message();
		msg.what = HANDLE_MSG_READY_PLAY;
		msg.setTarget(ve_Handler);
		
		mIncallScreenHandler.sendMessage(msg);
		*/
		
		mPhoneAppHandler.sendEmptyMessage(HANDLE_MSG_READY_PLAY);
	}
	
	
	private boolean deleteContentFile(String filepath)
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "deleteContentFile("+ filepath+")");	
		
		File fp = new File(filepath);
		return fp.delete();
	}
	
	private void playRingtone()
	{
		/**
		 * @author playRingtone / 2010. 11. 23. / ���� 11:32:49 / mail to -> feb7711@mtelo.com 
		 * ���� RINGER_MODE_NORMAL�� ��쿡�� ���� �������� ��� �Ѵ�.
		 */
		if ( mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_NORMAL )
		{
			if ( mAMF_Player.isHaveBGM() )
			{
				MyLog.write(Log.DEBUG, LOG_TAG, "have sound content");	
				
				if ( PhoneApp.getInstance().getRinger().isRinging() )
				{
					PhoneApp.getInstance().getRinger().stopRing();
					PhoneApp.getInstance().getRinger().setCustomRingtoneUri( Uri.fromFile(new File( DOWNLOAD_PATH + mAMF_Player.get_BgmName() )) );
					PhoneApp.getInstance().getRinger().ring();
				}
				else 
				{
					MyLog.write(Log.WARN, LOG_TAG, "ringer is not ringing");	
				}
			}
		}
	}
	
	
	private void initBuffer()
	{
		if ( mAMF_Player != null )
		{
			mAMF_Player.Put_FrameToBuffer(0, AMF_Player.MAX_FRAME_CNT);

			mBuffering_lop_i = 0;
			mPlay_lop_i = 0;
			
			/*
			mPlaying_Thread = new Thread(null, playing_Thread, "playing_Thread");
			mPlaying_Thread.start();
			*/
		}
	}
	
	private Runnable buffering_Thread = new Runnable()
	{
		public void run()
		{
			while ( isBufferingThread_Run )
			{
				if (isOnPause == false)
					mBuffering_lop_i = mAMF_Player.Put_FrameToBuffer(mBuffering_lop_i % AMF_Player.MAX_FRAME_CNT, AMF_Player.MAX_FRAME_CNT);

				sleep(DECODE_MS);
			}
		}
	};
	

	private Runnable playing_Thread = new Runnable()
	{
		public void run()
		{
			while (isPlayThread_Run)
			{
				if (isOnPause == false)
				{
					if (mAMF_Player.get_framestate(mPlay_lop_i) == true)
					{
						Draw_Frame();
					}
				}

				sleep(DECODE_MS);
			}
		}

		private void Draw_Frame()
		{	
			mBitmapData = mAMF_Player.get_frame(mPlay_lop_i);
			
			if (mBitmapData != null)
			{
				mAmf_Viewer.postInvalidate();
					
				mAMF_Player.set_framestate(mPlay_lop_i, false);
				mPlay_lop_i ++;					
				
				if (mPlay_lop_i >= AMF_Player.MAX_FRAME_CNT)
					mPlay_lop_i = 0;
			}

		}
	};
	
	private void sleep(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public static Handler getHandler()
	{
		if ( _VE_ContentManager == null )
		{
			_VE_ContentManager = new VE_ContentManager();
		}
		
		return _VE_ContentManager.ve_Handler;
		
		/*
		if (_VE_ContentManager != null)
			return _VE_ContentManager.ve_Handler;
		else
			return null;
		*/
	}
	
	public static VE_ContentManager getInstance()
	{
		if ( _VE_ContentManager == null )
		{
			_VE_ContentManager = new VE_ContentManager();
		}
		
		return _VE_ContentManager;
	}

	public int get_Class_State()
	{
		return mClass_State;
	}
	
	private void set_Class_State(int s)
	{
		mClass_State = s;
	}
	
	private void set_CallerInfo_To_Control()
	{
		MyLog.write(Log.DEBUG, LOG_TAG, "set_CallerInfo_To_Control()");
		
		try 
		{
			boolean ihaveNameVal = false,
					ihavePhoneNumVal = false,
					ihaveCnapNameVal = false,
					ihaveLabel = false;
			/*
			CallerInfo info = PhoneUtils.getCallerInfo( PhoneApp.getInstance().mInCallScreen, PhoneApp.getInstance().getPhone().getRingingCall().getEarliestConnection()); 
			*/
			CallerInfo info = PhoneApp.getInstance().getCallerInfo();
			
			mNameTextView = (TextView)mCallCardPersonInfoVE_ViewGroup.findViewById(R.id.name_ve);
			mPhoneNumTextView = (TextView)mCallCardPersonInfoVE_ViewGroup.findViewById(R.id.phoneNumber_ve);
			mLabel = (TextView)mCallCardPersonInfoVE_ViewGroup.findViewById(R.id.label_ve);
			
			MyLog.write(Log.INFO, LOG_TAG, "mNameTextView = " + mNameTextView);
			MyLog.write(Log.INFO, LOG_TAG, "mPhoneNumTextView = " + mPhoneNumTextView);
			MyLog.write(Log.INFO, LOG_TAG, "mLabel = " + mLabel);
			
			if ( info != null )
			{
				MyLog.write(Log.INFO, LOG_TAG, "info.name = " + info.name);
				MyLog.write(Log.INFO, LOG_TAG, "info.cnapName = " + info.cnapName);
				MyLog.write(Log.INFO, LOG_TAG, "info.phoneNumber = " + info.phoneNumber);
				
				/**
				 * @author set_CallerInfo_To_Control / 2010. 11. 17. / ���� 2:56:05 / mail to -> feb7711@mtelo.com 
				 */
				if ( info.name != null && !info.name.equalsIgnoreCase("null") && !info.name.trim().equals("") )
					ihaveNameVal = true;
				if ( info.cnapName != null && !info.cnapName.equalsIgnoreCase("null") && !info.cnapName.trim().equals("") )
					ihaveCnapNameVal = true;
				if ( info.phoneNumber != null && !info.phoneNumber.equalsIgnoreCase("null") && !info.phoneNumber.trim().equals("") )
					ihavePhoneNumVal = true;
				if ( info.phoneLabel != null && !info.phoneLabel.equalsIgnoreCase("null") && !info.phoneLabel.trim().equals("") )
					ihaveLabel = true;
				
				mNameTextView.setText( "" );
				mPhoneNumTextView.setText( "" );
				mLabel.setText( "" );
				
				if ( ihaveLabel )
					mLabel.setText( info.phoneLabel );
				
				if ( ihaveNameVal && ihaveCnapNameVal && ihavePhoneNumVal ) // �̸�,��Ī,��ȣ ��� ���縦 �Ѵ�.
				{
					MyLog.write(Log.DEBUG, LOG_TAG, "start timer for display toggle name and nickname info");
					/**
					 * 2010.10.26 �ּ� ó��
					mNameTextView.setText( info.name +"/"+ info.cnapName );
					 */
					
					/**
					 * 2010.10.26 �߰� �ڵ�
					 */
					isTurnIsToggleName = true; 
					
					/**
					 * 2010.11.10 add code
					 */
					if (mToggleTask != null)
					{
						mToggleTask.cancel();
						mToggleTask = null;
					}
					
					if ( mToggleTimer != null )
					{
						mToggleTimer.cancel();
						mToggleTimer = null;
						mCallerNickName = mCallerName = "";
					}
					
					mToggleTimer = new Timer();
					mToggleTask = new ToggleTask();
					mToggleTimer.schedule(mToggleTask, 0, TOGGLE_TIME_MS);
					
					mPhoneNumTextView.setText( info.phoneNumber );
					
					mCallerNickName = info.cnapName;
					mCallerName = info.name;
				}
				else if ( !ihaveNameVal && ihaveCnapNameVal && ihavePhoneNumVal ) // ��Ī,��ȣ�� ���縦 �Ѵ�.
				{
					mNameTextView.setText( info.cnapName );
					mPhoneNumTextView.setText( info.phoneNumber );
				}
				else if ( ihaveNameVal && !ihaveCnapNameVal && ihavePhoneNumVal ) // �̸�, ��ȣ�� ���縦 �Ѵ�.
				{
					mNameTextView.setText( info.name );
					mPhoneNumTextView.setText( info.phoneNumber );
				}
				else if ( !ihaveNameVal && !ihaveCnapNameVal && ihavePhoneNumVal ) // ��ȣ�� ���縦 �Ѵ�.
				{
					mNameTextView.setText( info.phoneNumber );
				}
				
				/*
				if ( ihaveNameVal && ihaveCnapNameVal ) // �̸��� ��Ī�� ���ÿ� ���� �� ���
				{
					// �׳� ���ٷ� ���� �������
				}
				else if ( ihaveNameVal && !ihaveCnapNameVal ) // �̸��� ���� �� ���
				{
					mNameTextView.setText( info.name );
					
					if ( ihavePhoneNumVal )
						mPhoneNumTextView.setText( info.phoneNumber );
				}
				else if ( !ihaveNameVal && ihaveCnapNameVal ) // ��Ī�� ���� �� ���
				{
					mNameTextView.setText( info.cnapName );
					
					if ( ihavePhoneNumVal )
						mPhoneNumTextView.setText( info.phoneNumber );
				}
				else if ( !ihaveNameVal && !ihaveCnapNameVal ) // �̸� ��Ī ��� ���� ���
				{
					if ( ihavePhoneNumVal )
						mNameTextView.setText( info.phoneNumber );
				}
				else
				{
					MyLog.write(Log.WARN, LOG_TAG, "... what ??? ");
				}
				
				if ( mNameTextView != null )
				{
					if ( info.name != null && !info.name.equalsIgnoreCase("null") )
						mNameTextView.setText(info.name);
					else
						mNameTextView.setText("Mtelo");
				}
				
				if ( mPhoneNumTextView != null )
				{
					if (info.phoneNumber != null)
						mPhoneNumTextView.setText(info.phoneNumber);
					else
						mPhoneNumTextView.setText("010 5148 4582");
				}
				*/
			}
			else
			{
				MyLog.write(Log.DEBUG, LOG_TAG, "callerinfo is null");
			}
		}
		catch ( NullPointerException e )
		{
			e.printStackTrace();
		}
		
	}
	
	private void start_Play_AM3()
	{
		MyLog.write(Log.INFO, LOG_TAG, "start_Play_AM3()");
		
		mAmf_Viewer = AMF_Viewer.getInstance();
		
		if ( mAmf_Viewer.isFinishInflate() )
		{
			MyLog.write(Log.INFO, LOG_TAG, "AMF_Viewer is Finish Inflate");
			
			mAmf_Viewer.setAmfViewerToVisable();
			
			isPlayThread_Run = true;
			isBufferingThread_Run = true;
			new Thread(playing_Thread).start(); 
			new Thread(buffering_Thread).start();
			playRingtone();
		}
		else
		{
			MyLog.write(Log.WARN, LOG_TAG, "AMF_Viewer is Not Finish Inflate");
		}
	}

	private void start_Play_SKM()
	{
		MyLog.write(Log.INFO, LOG_TAG, "start_Play_SKM()");
		
		if ( mCallCardPersonInfoVE_ViewGroup != null )
		{
			/**
			* @author initAmfPlayer / 2010. 11. 29. / ���� 3:09:26 / mail to -> feb7711@mtelo.com 
			* ���� ��Ȳ���δ� skm ������ ��쿡�� skm ������ ��� ���ϱ�� ��.
			* �׷��� �Ʒ� ������ �ʿ� ���� �ּ� ó����.
			*/
//			/**
//			 * @author start_Play_SKM / 2010. 11. 26. / ���� 5:46:02 / mail to -> feb7711@mtelo.com 
//			 * onPrepared �Լ� ������ ������ ���߰� �ϸ� skm ���� ��� �����̰� �߻��Ѵ�. �׷��� onPrepared �Լ� ������ ������.
//			 * �׷��� ���⿡ ���´� ;;;
//			 */
//			if ( mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_NORMAL )
//			{
//				if (stopOEMRingtone)
//					PhoneApp.getInstance().getRinger().stopRing();
//			}

			/**
			 * @author init / 2010. 11. 19. / ���� 3:06:58 / mail to -> feb7711@mtelo.com 
			 * videoview�� �ѹ� �ε��ϸ� ��� �ڿ��� Ȱ���Ѵ�
			 */
			if ( mVideoView == null )
			{
				mVideoView = (VideoView) mCallCardPersonInfoVE_ViewGroup.findViewById(R.id.Video_Viewer);
				mVideoView.setVisibility(View.VISIBLE);
				mVideoView.setVideoPath(DOWNLOAD_PATH + mSaveFileName);
				mVideoView.setOnPreparedListener(this);
				mVideoView.setOnCompletionListener(this);
				mVideoView.setOnErrorListener(this);
				
				/**
				 * @author start_Play_SKM / 2010. 11. 25. / ���� 8:00:11 / mail to -> feb7711@mtelo.com 
				 * �ּ�
				 */
//				mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
//				{
//					public void onPrepared(MediaPlayer mp)
//					{
//						/**
//						 * @author onPrepared / 2010. 11. 25. / ���� 10:21:41 / mail to -> feb7711@mtelo.com 
//						 * ���� ����� ���¸� �ľ��ؼ� ������� �� ��� ������ ���ҰŸ� ��Ų��.
//						 */
//						if ( (mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) || 
//								(mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_SILENT) )
//						{
//							mp.setVolume(0, 0);
//						}
//						else if ( mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_NORMAL )
//						{
//							if (stopOEMRingtone)
//								PhoneApp.getInstance().getRinger().stopRing();
//						}
//						
//						MyLog.write(Log.INFO, LOG_TAG, " onPrepared("+mp+")");
//						mVideoView.start();
//						
//						/**
//						 * @author onPrepared / 2010. 11. 25. / ���� 5:40:54 / mail to -> feb7711@mtelo.com 
//						 * ��θ����� ��Ī�� ����Ǿ ������ ��Ʈ ���Ѿ� �Ѵ� ;;;
//						if ( (mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) || 
//								(mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_SILENT) )
//						{
//							isAudioStreamMuteOn = true;
//							mAudioMgr.setStreamMute(AudioManager.STREAM_MUSIC, isAudioStreamMuteOn);
//						}
//						 */
//						
//						/**
//						 * @author onPrepared / 2010. 11. 25. / ���� 5:38:10 / mail to -> feb7711@mtelo.com 
//						 * ��θ����� ����Ǹ鼭 �������� ������ �������.
//						if ( mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_NORMAL )
//						{
//							if (stopOEMRingtone)
//								PhoneApp.getInstance().getRinger().stopRing();
//						}
//						*/
//					}
//				});
//				mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
//				{
//					public void onCompletion(MediaPlayer mp)
//					{
//						MyLog.write(Log.INFO, LOG_TAG, " onCompletion("+mp+")");
//						mVideoView.start();
//					}
//				});
//				mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener()
//				{
//					public boolean onError(MediaPlayer mp, int what, int extra)
//					{
//						MyLog.write(Log.ERROR, LOG_TAG, "onError("+ mp+", "+ what+", "+ extra+")");
//						return true;
//					}
//				});
			}
			else
			{
				mVideoView.setVisibility(View.VISIBLE);
				mVideoView.setVideoPath(DOWNLOAD_PATH + mSaveFileName);
			}
		}
		else
		{
			MyLog.write(Log.WARN, LOG_TAG, "mCallCardPersonInfoVE_ViewGroup is null");
		}
	}
	
	private void removeUnusualContent()
	{
		MyLog.write(Log.WARN, LOG_TAG, "removeUnusualContent()");
		
		if ( new File(DOWNLOAD_PATH + mSaveFileName).exists() ) // ���� ������ ���縦 �Ѵ�
		{
			int filesize = Integer.parseInt(mSharedPreferences.getString(mSaveFileName, "0"));

			if ( deleteContentFile(DOWNLOAD_PATH + mSaveFileName) ) // ������ ���������� DB Update ����
			{
				mUsed_Memory -= filesize;
				mEditor.remove(mSaveFileName);
				mEditor.putLong(KEY_USED_MEMORY, mUsed_Memory); 
				mEditor.commit();
			}
			else
			{
				MyLog.write(Log.WARN, LOG_TAG, "delete " + mSaveFileName + " failed");
			}
		}
		else
		{
			MyLog.write(Log.WARN, LOG_TAG, "there is no file to delete");
		}
	}
	
	/**
	 * 2010.10.26 �ڵ� �߰�
	private TimerTask mToggleTask = new TimerTask()
	{
		public void run()
		{
			if ( mNameTextView != null )
				mNameTextView.setText( isTurnIsToggleName == true ? mCallerName : mCallerNickName );
			
			if ( isTurnIsToggleName )
				isTurnIsToggleName = false;
			else
				isTurnIsToggleName = true;
		}
	};
	 */

	/**
	 * 2010.11.10 change code
	 */
	private ToggleTask mToggleTask;
	private class ToggleTask extends TimerTask
	{
		public void run() 
		{
			/**
			 * 2010.11.16 �߰� �ڵ�, �����忡�� �ڲ� UI�� �ǵ帮�� �ڵ带 �� �� �ۼ��� �ұ�;;;
			 */
			try 
			{
				getHandler().sendEmptyMessage( HANDLE_MSG_TOGGLE_PERSON_INFO );
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			/**
			 * 2010.11.16 �ּ� ó��
			if ( mNameTextView != null )
				mNameTextView.setText( isTurnIsToggleName == true ? mCallerName : mCallerNickName );
			
			if ( isTurnIsToggleName )
				isTurnIsToggleName = false;
			else
				isTurnIsToggleName = true;
			 */
		}
	}

	/**
	 * @author onPrepared / 2010. 11. 25. / ���� 7:59:56 / mail to -> feb7711@mtelo.com 
	 */
	public void onPrepared(MediaPlayer mp)
	{
		mMediaPlayer = mp;
		
		/**
		* @author initAmfPlayer / 2010. 11. 29. / ���� 3:09:26 / mail to -> feb7711@mtelo.com 
		* ���� ��Ȳ���δ� skm ������ ��쿡�� skm ������ ��� ���ϱ�� ��.
		* �׷��� skm�� ��쿡�� �̵�� ������ 0���� ������ ���� ��.
		*/
		mMediaPlayer.setVolume(0, 0);
		
//		/**
//		 * @author onPrepared / 2010. 11. 25. / ���� 10:21:41 / mail to -> feb7711@mtelo.com 
//		 * ���� ����� ���¸� �ľ��ؼ� ������� �� ��� ������ ���ҰŸ� ��Ų��.
//		 */
//		if ( (mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) || 
//				(mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_SILENT) )
//		{
//			mMediaPlayer.setVolume(0, 0);
//		}
		
		/**
		 * @author onPrepared / 2010. 11. 26. / ���� 5:43:17 / mail to -> feb7711@mtelo.com 
		 * onPrepared �Լ� ������ ������ ���߰� �ϸ� skm ���� ��� �����̰� �߻��Ѵ�. �׷��� onPrepared �Լ� ������ ������.
		else if ( mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_NORMAL )
		{
			if (stopOEMRingtone)
				PhoneApp.getInstance().getRinger().stopRing();
		}
		 */
		
		MyLog.write(Log.INFO, LOG_TAG, " onPrepared("+mMediaPlayer+")");
		mVideoView.start();
	}

	public void onCompletion(MediaPlayer mp)
	{
		MyLog.write(Log.INFO, LOG_TAG, " onCompletion("+mp+")");
		mVideoView.start();
	}

	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		MyLog.write(Log.ERROR, LOG_TAG, "onError("+ mp+", "+ what+", "+ extra+")");
		return true;
	}

	
	/**
	* @author initAmfPlayer / 2010. 11. 29. / ���� 3:09:26 / mail to -> feb7711@mtelo.com 
	* ���� ��Ȳ���δ� skm ������ ��쿡�� skm ������ ��� ���ϱ�� ��.
	* �׷��� �Ʒ� �Լ��� �ּ� ó�� ��.
	*/
//	/**
//	 * @author check_SoundTrack_from_skm / 2010. 11. 26. / ���� 10:12:52 / mail to -> feb7711@mtelo.com 
//	 * skm ���Ͽ� ������ ���ԵǾ� �ִ��� üũ �Ѵ�.
//	 */
//	private void check_SoundTrack_from_skm()
//	{
//		/**
//		 * @author playRingtone / 2010. 11. 23. / ���� 11:32:49 / mail to -> feb7711@mtelo.com 
//		 * RINGER_MODE_NORMAL�� ���
//		 * �׸��� skm ���Ͽ� ������ ���� �� ��쿡�� OEM���� ������� ������ ���߰� �Ѵ�.
//		 */
//		final int audio_fromat_offset = 44; 
//		stopOEMRingtone = false;
//		
//		try
//		{
//			FileInputStream is = new FileInputStream( DOWNLOAD_PATH + mSaveFileName );
//			
//			if ( is != null )
//			{
//				byte[] buffer = new byte[3];
//				
//				is.skip(audio_fromat_offset);
//				is.read(buffer);
//				is.close();
//				
//				if ( (buffer[0] == 'a' && buffer[1] == 'a' && buffer[2] == 'c') || (buffer[0] == 'a' && buffer[1] == 'm' && buffer[2] == 'r') ||
//						(buffer[0] == 'A' && buffer[1] == 'A' && buffer[2] == 'C') || (buffer[0] == 'A' && buffer[1] == 'M' && buffer[2] == 'R') )
//				{
//					if ( mAudioMgr != null && mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_NORMAL )
//					{
//						if ( PhoneApp.getInstance().getRinger().isRinging() )
//						{
//							stopOEMRingtone = true;
//						}
//					}
//				}
//				
//				buffer = null;
//			}
//		} 
//		catch (FileNotFoundException e)
//		{
//			MyLog.write(Log.ERROR, LOG_TAG, "FileNotFoundException = " + e.getMessage());
//			e.printStackTrace();
//		} 
//		catch (IOException e)
//		{
//			MyLog.write(Log.ERROR, LOG_TAG, "IOException = " + e.getMessage());
//			e.printStackTrace();
//		}
//	}
}
