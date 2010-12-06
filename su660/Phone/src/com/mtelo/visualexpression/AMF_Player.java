package com.mtelo.visualexpression;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class AMF_Player
{
	private final String TAG = "AMF_Player";
	
	// Define	
	private static final String VE_BGM_MP3 = "bgm.mp3";
	
	public static final int MAX_FRAME_CNT = 10;
	private static final int FRAME_GAP = 100;
	
	// Content Format 
    public static enum ContentFormat
    {
    	DMF, // 지원 하지 않는다.,
    	AM3,
    	SKM
    };
    // Content Format Data
    private ContentFormat mContentFmt = ContentFormat.AM3;

	// Play Time	
	private long mStartime;	
	private long mTotalplaytime;

	// Player State
	private boolean []mHaveFrame = new boolean[MAX_FRAME_CNT];	
	private boolean mHaveBGM = false;
	
	// Frame Info
	private int mFrameWidth;
	private int mFrameHeight;
	
	// Frame Data
	private Bitmap []mBitmap;
	private byte []mFramebuff;	
	
	
	public AMF_Player()
	{
		MyLog.set_LogEnable(false, "wang");
		MyLog.write(Log.INFO, TAG, "AMF_Player()");
		
		AmfCodec.JNICAmfLib();
		AmfCodec.JNIInitialize();
		AmfCodec.JNISetWorkDirectory( VE_ContentManager.DOWNLOAD_PATH );

		mBitmap = new Bitmap[MAX_FRAME_CNT];
	}
	
	public void stop_Player()
	{
		MyLog.write(Log.INFO, TAG, "stop_Player()");
		
		/**
		 * 2010.10.27 주석 처리
		AmfCodec.JNIReleaseLib();
		 */
	}
	
	/**
	 * 2010.10.27 함수 추가 
	 */
	public void releaseLib()
	{
		AmfCodec.JNIReleaseLib();
	}
	
	public String get_BgmName()
	{
		return VE_BGM_MP3;
	}
	
	public int init_player(String path, int w, int h)
	{
		MyLog.write(Log.INFO, TAG, "init_player("+ path+", "+ w+", "+ h+")");
		
		int result = 0;
		
		result = AmfCodec.JNIDecode(path);
		result = AmfCodec.JNIInitPlay(w, h);
		result = AmfCodec.JNIInitBuffer(w, h);
		
		/**
		 * BGM이 있는지 체크를 하고, 있으면 파일을 임시로 저장 한다. 
		 */
		if (AmfCodec.JNIGetClipCnt() > 0)
		{
			if (AmfCodec.JNIIsBGM())
			{
				if (AmfCodec.JNIGetBGM( VE_ContentManager.DOWNLOAD_PATH + VE_BGM_MP3 ) == 0)
				{
					MyLog.write(Log.DEBUG, TAG, "AMF_Player.init_player() have sound content");	
					mHaveBGM = true;
				}
				else
				{
					MyLog.write(Log.DEBUG, TAG, "AMF_Player.init_player() no sound content 1");	
				}
			}
			else
			{
				MyLog.write(Log.DEBUG, TAG, "AMF_Player.init_player() no sound content 2");	
			}
		}
		
		mFrameWidth = w;
		mFrameHeight = h;
		
		mStartime = 0;
		
		for (int i = 0; i < mHaveFrame.length; i++)
		{
			mHaveFrame[i] = false;
		}
		
		mTotalplaytime = get_totaltime();
		
		return result;
	}
	
	public boolean isHaveBGM() 
	{
		return mHaveBGM;
	}

	public long get_totaltime()
	{
		return AmfCodec.JNIGetRunTime();
	}
	
	public Bitmap get_frame(int idx)
	{
		return mBitmap[idx];
	}
	

	public boolean get_framestate(int idx)
	{
		return mHaveFrame[idx];
	}
	
	public void set_framestate(int idx, boolean state)
	{
		try
		{
			mHaveFrame[idx] = state;
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			e.printStackTrace();
		}
	}
	

	public synchronized int Put_FrameToBuffer(int sOffs, int eOffs)
	{
		int i = sOffs;
		
		while (i < eOffs)
		{
			if (mHaveFrame[i] == true)
			{
				break;
			}
			
			mFramebuff = AmfCodec.JNIGetFrame(mFrameWidth, mFrameHeight, mStartime);
			
			if (mFramebuff == null)
			{
				break;
			}
			
			mBitmap[i] = BitmapFactory.decodeByteArray(mFramebuff, 0, mFramebuff.length);
			mHaveFrame[i] = true;
			mStartime += FRAME_GAP;
			
			i ++;
			
			if (mStartime >= mTotalplaytime)
				mStartime = 0;
			
		}
		return i;
	}

    
	public ContentFormat getmContentFmt() 
	{
		return mContentFmt;
	}

	public void setmContentFmt(ContentFormat mContentFmt) 
	{
		this.mContentFmt = mContentFmt;
	}	
}



































