/**
 * 
 */
package com.android.phone.videophone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.provider.DrmStore;

import com.android.internal.telephony.Phone;
import com.android.phone.InVideoCallScreen;
import com.android.phone.PhoneApp;
import com.android.phone.VideoCallCard;
import com.android.phone.videophone.VTCallStateInterface.VTCallState;
import com.android.phone.videophone.VTEngineStateInterface.VTEngineState;
import com.android.phone.videophone.VTCallStatistics;



import android.provider.MediaStore;
import android.provider.Settings;
import android.os.AsyncResult;

import com.android.phone.PhoneUtils;
//@ import com.lge.config.StarConfig;


/**
 * VideoTelephonyApp
 * 
 * When a using VTWrapper (VTEngine) and VTAppStateManager, manage a flag which needs some feature. 
 *  
 * @author hogyun.kim
 *
 */
public class VideoTelephonyApp implements VTObserver {

	private static final String LOG_TAG = "VideoTelephonyApp";
	private static final boolean DBG =
		(PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);


	/** Flag to determine live or loopback */
	private static boolean vtLoopbackCall = false;

	private boolean bRecording = false;
	private boolean bStartPreviewPended = false;
	private boolean bMute = false;
	private boolean bHold = false;
	private boolean bIncoming = false;
	private int nSensorID = VTPreferences.SENSOR_ID_DEFAULT;

	private Phone 			  mPhone = null;
	private Context 		  mContext = null;
	private VTAppStateManager mStateManager = null;
	private VTWrapper		  mWrapper = null;


	//for preview
	private SurfaceHolder mNearHolder = null;
	private SurfaceHolder mFarHolder = null;

	public static VideoTelephonyApp mSelf = null;

	private static final int CAPTURE_MSG_SUCCESS = 1;
	private static final int CAPTURE_MSG_FAIL = 2;
	private static final int CAPTURE_MSG_NO_SD_CARD = 3;
	private static final int CAPTURE_MSG_SD_CARD_NA = 4;
	private static final int CAPTURE_MSG_CANNT_JPG = 5;
       private static final int CAPTURE_MSG_ENCRYPT_FAIL = 6;
       private static final int CAPTURE_MSG_MEMORY_FULL = 7;

	public static final int VTSNDPATH_NONE = 0x00000000;
	public static final int VTSNDPATH_SPEAKER = 0x00000001;
	public static final int VTSNDPATH_RECEIVER = 0x00000002;
	public static final int VTSNDPATH_EARMIC = 0x00000004;
	public static final int VTSNDPATH_BLUETOOTH= 0x00000008;

	private int mCurSndPath = VTSNDPATH_NONE;
	private int mPreSndPath = VTSNDPATH_NONE;

	private long mCaptureindex;
       private String mCaptureFileName;

       //SKT_CAPTURE_TEST
        private byte[] cipherText = null;
        private String keyPath =   "/data/data/com.android.phone/files/key.txt";  
        private String decodedFile = "/mnt/sdcard/DCIM/VTCAP/tmp.jpg";

    public VTCallStatistics  mStat;
    
	/**
	 * Perform a something job with Video Telephony Engine 
	 */ 
	private VideoTelephonyApp(Context c) {
		if (DBG) Log.i(LOG_TAG, "Create VideoTelephonyApp()");

		mContext = c;
		mStateManager = VTAppStateManager.getInstance();

        // support statistics for VT field test 
        mStat = new VTCallStatistics();

		if (VTPreferences.NETWORK_CSVT == VTPreferences.NETWORK_MODE)
		{
			mWrapper = new CSVT(mContext);
		}
		else  if (VTPreferences.NETWORK_PSVT == VTPreferences.NETWORK_MODE)
		{
			mWrapper = new PSVT(mContext);
		}
        else
        {
            mWrapper = new DummyVT(mContext);
        }

	} 
	public static VideoTelephonyApp getInstance(Context c)
	{
		if(null == mSelf)
		{
			mSelf = new VideoTelephonyApp(c);
		}
		return mSelf;
	} 

	public static VideoTelephonyApp getInstance()
	{
		return mSelf;
	}

	/**
	 * Set Phone instance 
	 */
	public void setPhone(Phone phone) 
	{
		mPhone = phone;

	}

	/**
	 * Set Live or Loopback call 
	 */
//	public void setLiveCall(boolean livecall)
//	{
//		vtLiveCall = livecall;
//	}

	/**
	 * Get Live or Loopback call 
	 */
	public boolean getLoopbackCall()
	{
        if ( Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_LOOPBACK, 0) == 0 )
    		vtLoopbackCall = false;
        else 
            vtLoopbackCall = true;
        
		return vtLoopbackCall;
	}

	/**
	 * initialize
	 * After receiving the call state by the callback, these members variable is initialized;  
	 * */
	private void initialize()
	{
		bRecording = false;		

		bStartPreviewPended = false;
		bMute = false;
              bHold = false;
		bIncoming = false;

		nSensorID = VTPreferences.SENSOR_ID_DEFAULT;

		if (null != mStateManager)
			mStateManager.initialize();

              mWrapper.SetVTCodec();
              mWrapper.SetCallPreference();
              mWrapper.setVideoTelephonyOptions();
              mWrapper.SetupDebugInfo();
	}

	private void log(String msg) {
	    if(DBG)
		Log.d(LOG_TAG, msg);
	}

	public boolean isRecording() {	
		return bRecording;
	}	
	public void setRecording(boolean bFlag)
	{
		bRecording = bFlag;
	}

	public boolean isMute() {
		return bMute;
	}

	public boolean isHold() {
		return bHold;
	}

	public void InitAudioPath(int nPath) {
		mCurSndPath = mPreSndPath = nPath;		
	}

	public void setAudioPath(int nPath) {
		if(mCurSndPath != nPath)
		{
			if(mCurSndPath != VTSNDPATH_EARMIC)
				mPreSndPath = mCurSndPath;

			mCurSndPath = nPath;
		}
	}

	public int getPreAudioPath() {
		if (DBG) log("mPreSndPath : " + mPreSndPath);	
		return mPreSndPath;
	}

	private void startPreview()
	{
		if (DBG) log("startPreview() called");		


		if(mNearHolder == null || mFarHolder == null)
		{
			bStartPreviewPended = true;
			if (DBG) log("startPreview >> pending");		
			return;
		}		
        
		bStartPreviewPended = false;
		mStateManager.setEngineState(VTEngineState.ST_EARLYPREVIEW);
		mWrapper.startPreview(mNearHolder, mFarHolder, getLoopbackCall());

		if (DBG) log("startPreview() end");
	}

	public void connectCall()
	{
		if (DBG) log("connectCall()() called");

		mStateManager.setEngineState(VTEngineState.ST_LIVE);
		mWrapper.connectCall();

	}

	public void userEndCall()
	{
		if (DBG) log("userEndCall() called");

        mStat.userEnd();
        
		if (mStateManager.isPendingHangup())
		{
			if (DBG) log("userEndCall() pended");
			mStateManager.setHangupPended(true);
		}
		else if (mStateManager.isEarlyPreview())
		{
			mStateManager.setEngineState(VTEngineState.ABNORMALHANGUP);
			mWrapper.endCallAbnormal();				
		}
		else if (mStateManager.isCanHangup())
		{
			mStateManager.setEngineState(VTEngineState.NORMALHANGUP);
			mWrapper.endCallNormal();
		}
	}

	private void callEnded()
	{
		if (DBG) log("callEnded() called");

        mStat.callEnded();

		if (mStateManager.isPendingHangup())
		{
			if (DBG) log("callEnded() pended");
			mStateManager.setHangupPended(true);
		}
		else if (mStateManager.isCanHangup())
		{
			mStateManager.setEngineState(VTEngineState.ABNORMALHANGUP);
			mWrapper.endCallAbnormal();
		}
	}

	/**
	 * callhangup on RIL
	 * */
	public void rilCallHangup()
	{
		if (mStateManager.isAlive()
				/*
			check whether following is needed 
			&& (VTEngineState.ABNORMALHANGUP || NORMALHANGUP)
				 */ 
		)
		{
			if (DBG) log("rilCallHangup() called");
			PhoneUtils.hangup(mPhone);
		}
	}

	public boolean startImageSharinig(String fileName)
	{
		if (DBG) log("startImageSharinig() called");

		if (mStateManager.isCanCommand() && mStateManager.isActive())
		{
		       if(mStateManager.getEngineState() != VTEngineState.ON_LIVE)
                            return false;
               
			mStateManager.setEngineState(VTEngineState.REQUESTCMD);
			mWrapper.startImageSharinig(fileName);
		}
		else if(mStateManager.isDialing())
		{
			VideoCallCard vcc = ((InVideoCallScreen)mContext).getVideoCallCard();

			((InVideoCallScreen)mContext).isSubstituImage= 1; 
			//vcc.showSubstitute(true, false);
			vcc.showSubstituteOnDialing(0,true);
		}
		return true;
	}	

	public boolean stopImageSharinig()
	{
		if (DBG) log("stopImageSharinig() called");
              
		if (mStateManager.isCanCommand() && mStateManager.isActive() && mStateManager.isSharing())
		{
			mStateManager.setEngineState(VTEngineState.REQUESTCMD);			
			mWrapper.stopImageSharinig();
		}
		else
		{
			VideoCallCard vcc = ((InVideoCallScreen)mContext).getVideoCallCard();

			((InVideoCallScreen)mContext).isSubstituImage= 0; 
			//vcc.showSubstitute(false, false);
			vcc.showSubstituteOnDialing(0,false);
		}

		return true;
	}	

	public void startFileSharinig(String path)
	{
		if (DBG) log("startFileSharinig() called");

		if (mStateManager.isCanCommand())
		{
			mStateManager.setEngineState(VTEngineState.REQUESTCMD);			
			mWrapper.startFileSharing(path);
		}

	}	

	public void stopFileSharinig()
	{		
		if (DBG) log("stopFileSharinig() called");

		if (mStateManager.isCanCommand() && mStateManager.isSharing())
		{
			mStateManager.setEngineState(VTEngineState.REQUESTCMD);			
			// TBD
		}
	}			

	public int startCapture(boolean  bFarEndSide, String path)
	{
		int retVal = CAPTURE_MSG_FAIL; 

		if (DBG) Log.d(LOG_TAG,"startCapture() called, enginestate: "+mStateManager.getEngineState());

		if (mStateManager.isCanCommand())
		{
			mStateManager.setEngineState(VTEngineState.CAPTURING);			

			if (bFarEndSide)
			{
				retVal = mWrapper.getLastRemoteFrame(path);
			}
			else 
			{
				retVal = mWrapper.getLastLocalFrame(path);			
			}
		}

		return retVal;
	}	

	public void startRecording()
	{
		if (DBG) log("startRecording() called");

		if (mStateManager.isCanCommand())
		{
			mStateManager.setEngineState(VTEngineState.REQUESTCMD);			
			// TBD
		}
	}	

	public void stopRecording()
	{
		if (DBG) log("stopRecording() called");

		if (mStateManager.isCanCommand())
		{
			mStateManager.setEngineState(VTEngineState.REQUESTCMD);			
			// TBD
		}

	}	

	public void toggleMute()
	{
		if (DBG) log("toggleMute() called " + bMute);

		if (mStateManager.isCanCommand())
		{
			if (bMute)
			{
				mWrapper.setMute(false);
				bMute = false;
			}
			else
			{
				mWrapper.setMute(true);
				bMute = true;
			}
		}

	}

	public boolean toggleHold()
	{
		if (DBG) log("toggleHold() called " + bHold);
        
              boolean bRet = false;
              
		if (mStateManager.isCanCommand())
		{
			if (bHold)
			{
				mWrapper.setHold(false);
				bRet = stopImageSharinig();
				bMute = bHold = false;
			}
			else
			{
				VideoCallCard vcc = ((InVideoCallScreen)mContext).getVideoCallCard();

				mWrapper.setHold(true);
				bRet = startImageSharinig(vcc.holdImagePath);
				bHold = true;
			}
		}
		return bRet;
	}
	/**
	 * change substitude mode
	 * */
	public void showSubstitute(boolean isShow, boolean isHold)
	{
		VideoCallCard vcc = ((InVideoCallScreen)mContext).getVideoCallCard();
		vcc.showSubstitute(isShow, isHold);
	}

	public void showMovSubstitute(boolean isShow)
	{
		VideoCallCard vcc = ((InVideoCallScreen)mContext).getVideoCallCard();
		vcc.showMovSubstitute(isShow);
	}

	/* 
		APIs  update at 2010-8-23
	 */
	public void sendDTMFString(String DTMFString)
	{
        if (mStateManager.isCanCommand())
        {
    		if (DTMFString.length() > 0)
    			mWrapper.sendDTMFString(DTMFString);
    		else
    			log("NULL string");
        }
	}


	public void getCameraParameter()
	{
		if (mStateManager.isCanCommand())
		{
			mWrapper.getCameraParameter();
		}
	}

	public void setCameraParameter(String key,String value)
	{
		if (mStateManager.isCanCommand())
		{
			mWrapper.setCameraParameter(key, value);
		}
	}

	public int getSensorID()
	{
		return nSensorID;
	}

	public void switchCamera()
	{
		if (mStateManager.isCanCommand())
		{
			if (nSensorID == VTPreferences.SENSOR_ID_FRONT_PORTRAIT)
			{
				mWrapper.switchCamera(VTPreferences.SENSOR_ID_BACK_PORTRAIT);
				nSensorID = VTPreferences.SENSOR_ID_BACK_PORTRAIT;
			}
			else
			{
				mWrapper.switchCamera(VTPreferences.SENSOR_ID_FRONT_PORTRAIT);
				nSensorID = VTPreferences.SENSOR_ID_FRONT_PORTRAIT;
			}
		}
	}

	public void setCameraParameters(String key,String value)
	{
		//temp
		if (DBG) Log.i(LOG_TAG, "setCameraParameters() " + key + " : " + value);

		if (mStateManager.isCanCommand())
		{
			mWrapper.setCameraParameter(key, value);
		}
		
		//		String strPramete r  = mWrapper.getCameraParameter();
		//		
		//		if (DBG) Log.i(LOG_TAG, "parameter = " + strPrameter);
	}


	///////////////////////////////////////////////////////////////
	/// VTObserver Implements Methods 
	///////////////////////////////////////////////////////////////

	/**
	 * When Far surfaceview is created, called as a callback.    
	 * */
	//@Override
	public void onFarSurfaceViewCreated(SurfaceHolder farHolder) {		
		if(null == farHolder)
		{
			if(DBG) log("Far surfaceView is destroyed! ");
			mFarHolder = null;
			return;
		}

		if(DBG) log("Far surfaceView is created and changed! ");

		mFarHolder = farHolder; 
		if (bStartPreviewPended)
		{
			startPreview();
		}
	}

	/**
	 * When Near surfaceview is created, called as a callback.    
	 * */
	//@Override
	public void onNearSurfaceViewCreated(SurfaceHolder nearHolder) {
		if(null == nearHolder)
		{
			if(DBG) log("Near surfaceView is destroyed! ");
			mNearHolder = null;
			return;
		}

		if(DBG) log("Near surfaceView is created and changed! ");

		mNearHolder = nearHolder;
		if (bStartPreviewPended)
		{
			startPreview();
		}	
	} 

	/**
	 * When Phone State is changed, called as a callback.    
	 * */
	//@Override
	public boolean onUpdateVTState(Phone phone) 
	{
		VTCallState oldState = mStateManager.getCallState();
		VTCallState newState = mStateManager.makeVTCallState(phone);

		// debug code 
		//PhoneUtils.dumpCallState( phone);

		if(oldState == newState)
		{
			return false;
		}

		if (DBG) 
			log("onUpdateVTState() oldState= "+ oldState + ", newState= "+newState);


		switch (newState)
		{
		case IDLE :
            // logging every 10 vt calls
            if ((mStat.vtCallTotoal % 10)==0) log(mStat.toString());
			break;

		case ACTIVE :
			if (bIncoming)
			{
				startPreview();
			}
			else
			{
				connectCall();
			}
			break;

		case DIALING :
                     initialize();
			startPreview();
			break;

		case INCOMING :
                     initialize();
			bIncoming = true;
			break;

		case DISCONNECTED :
			callEnded();
			break;			
		}

        mStat.UpdatebyCallState(newState);
		mStateManager.setCallState(newState);
        return true;
	}

	public int localImageCapture(byte[] data)
	{
		int retVal = CAPTURE_MSG_FAIL;

		log("localImageCapture()");

		YuvImage image;

		Rect mRects = new Rect(0, 0, 176,144);

		//TODO: Buffer Corruption
		image = new YuvImage(data, ImageFormat.NV21/*YUY2*/, 176, 144, null);

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();

		if(image.compressToJpeg(mRects, 100, outStream) == false)
		{
			log("vtEngine.getLastLocalFrame() -> Cannot convert to JPEG format");
			return CAPTURE_MSG_CANNT_JPG;
		}

		byte[] jpegData = outStream.toByteArray();

              retVal = ImageCaptureSaved(jpegData, false);	
              
              if (DBG) log("localImageCapture() finish~~~~result:" + retVal);	
		return retVal;
	}

	public int remoteImageCapture(byte[] data)
	{
		Bitmap bmp = null;
		int retVal = CAPTURE_MSG_FAIL;

		log("remoteImageCapture()");

		bmp = Bitmap.createBitmap(176, 144, Bitmap.Config.RGB_565);
		bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data));	

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		if(bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream) == false)
		{
			log("vtEngine.getLastRemoteFrame() -> Cannot convert to JPEG format");
                     bmp.recycle();
                     bmp = null;
			return CAPTURE_MSG_CANNT_JPG;
		}
              bmp.recycle();
              bmp = null;
		byte[] jpegData = outStream.toByteArray();

              //SKT_CAPTURE_TEST
		if(VTPreferences.OPERATOR.equals("SKT"))
		{
			boolean isMakeencrpytion = false;
			try {
				CapturedImageEncryption(jpegData);
				isMakeencrpytion = true;
			} catch (StreamCorruptedException e) {
				log("StreamCorruptedException()" +e.getMessage());
			} catch (InvalidKeyException e) {
				log("InvalidKeyException()" +e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				log("NoSuchAlgorithmException()" +e.getMessage());
			} catch (NoSuchPaddingException e) {
				log("NoSuchPaddingException()" +e.getMessage());
			} catch (IllegalBlockSizeException e) {
				log("IllegalBlockSizeException()" +e.getMessage());
			} catch (BadPaddingException e) {
				log("BadPaddingException()" +e.getMessage());
			} catch (IOException e) {
				log("IOException()" +e.getMessage());
			} catch (ClassNotFoundException e) {
				log("ClassNotFoundException()" +e.getMessage());
			}
			if(isMakeencrpytion)
                            retVal = ImageCaptureSaved(cipherText, true);	
                    else
                           retVal =  CAPTURE_MSG_ENCRYPT_FAIL;
              }
              else
		      retVal = ImageCaptureSaved(jpegData, true);	

              if (DBG) log("remoteImageCapture() fininsh~~~result:" + retVal);	
		return retVal;
	}

       //SKT_CAPTURE_TEST
       private byte[] CapturedImageEncryption(byte[] jpegData) 
       throws StreamCorruptedException, IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException 
       {
            log("ImageEncode()");

            //Test
            Cipher cipher = null; 
            Key key = null; 

            File file = new File(keyPath);   
            
            //Triple DES Create
            if(!file.exists())
            {           
            	//file.createNewFile();
            	KeyGenerator keyGenerator = null;
            	//FileOutputStream fos = new FileOutputStream(keyPath);
            	FileOutputStream fos = mContext.openFileOutput("key.txt", Context.MODE_WORLD_READABLE);

            	ObjectOutputStream oos = new ObjectOutputStream(fos);

            	keyGenerator = KeyGenerator.getInstance("DESede");
            	keyGenerator.init(168);
            	key = keyGenerator.generateKey();

            	oos.writeObject(key);
            	oos.close();
            }

            FileInputStream fis = new FileInputStream(keyPath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            key = (Key)ois.readObject();

            //Cipher Create       
            cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,key);

            //Encryption Start
            cipherText = cipher.doFinal(jpegData);  

            return cipherText;
       }

       //SKT_CAPTURE_TEST
       private String CapturedImageDecryption(String path) 
       throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException 
       {          
    	   log("ImageDecode()");

    	   //Test
    	   Key key = null; 
    	   File file, tmp;
    	   Cipher cipher = null;                         
    	   int length;

    	   file = new File(path);
    	   tmp = new File(decodedFile);

    	   if(!tmp.exists())
    		   tmp.createNewFile();

    	   FileInputStream fin = null;
    	   fin = new FileInputStream(file);

    	   length = fin.available();
    	   byte[] data = null;
    	   data = new byte[length];
    	   fin.read(data);      

    	   FileInputStream fis = new FileInputStream(keyPath);
    	   ObjectInputStream ois = new ObjectInputStream(fis);
    	   key = (Key)ois.readObject();

    	   cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
    	   cipher.init(Cipher.DECRYPT_MODE, key);

    	   byte[] decryptedImage = cipher.doFinal(data); 

    	   FileOutputStream fos = new FileOutputStream(tmp);            
    	   fos.write(decryptedImage);     
    	   fos.flush();
    	   fos.close();           
    	   return decodedFile;
       }
       
       public int ImageCaptureSaved(byte[] jpegData, boolean bFarEnd)
       {
    	   final String SAVE_ROOT = Environment.getExternalStorageDirectory().toString();

    	   final String SAVE_DIRECTORY = "/DCIM/VTCAP";
    	   final String SAVE_TITLE_FORMAT = "yyyy-MM-dd kk:mm:ss";
    	   final String SAVE_FILENAME_FORMAT = "yyyy-MM-dd kk.mm.ss";

    	   long mAvailableSDCardMem;
    	   int retVal = CAPTURE_MSG_FAIL;        
    	   String externalStorageState = Environment.getExternalStorageState();

    	   log("ImageCaptureSaved()");

    	   mAvailableSDCardMem = AvailableSDCardSize();
    	   if(jpegData.length > mAvailableSDCardMem)
    		   return CAPTURE_MSG_MEMORY_FULL;

    	   if(Environment.MEDIA_MOUNTED.equals(externalStorageState) == true) { 
    		   String prefix ,ext;                        

    		   if(bFarEnd)
    		   {         
    			   prefix = "Far_";
    			   if(VTPreferences.OPERATOR.equals("SKT"))
    				   ext = ".vci";    //video capture image
    			   else
    				   ext = ".jpg";
    		   }
    		   else
    		   {         
    			   prefix = "Near_";
    			   ext = ".jpg";
    		   }

    		   long currentTime = System.currentTimeMillis();
    		   String title = prefix + DateFormat.format(SAVE_TITLE_FORMAT, currentTime).toString();
    		   String fileName = prefix + DateFormat.format(SAVE_FILENAME_FORMAT, currentTime).toString() + ext; 

    		   mCaptureFileName = fileName;
    		   ContentValues values = new ContentValues();
    		   values.put(Images.Media.TITLE, title);
    		   values.put(Images.Media.DISPLAY_NAME, fileName);
    		   values.put(Images.Media.MIME_TYPE, "image/jpeg");
    		   values.put(Images.Media.ORIENTATION, 0);
    		   values.put(Images.Media.DATE_TAKEN, currentTime);
    		   values.put(Images.Media.DATE_ADDED, currentTime / 1000);
    		   values.put(Images.Media.DATE_MODIFIED, currentTime / 1000);
    		   if(ext.equals("vci"))
    			   values.put(Images.Media.IS_PRIVATE, 1);
    		   else
    			   values.put(Images.Media.IS_PRIVATE, 0);

    		   values.put(Images.Media.DATA, SAVE_ROOT + SAVE_DIRECTORY + File.separator + fileName);
    		   values.put(Images.Media.SIZE, jpegData.length);

    		   ContentResolver resolver = mContext.getContentResolver();

    		   Uri uri = null;
    		   try {
    			   uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
    			   Cursor c = resolver.query(uri, null, null, null, null);
    			   c.moveToLast();
    			   mCaptureindex = c.getLong(c.getColumnIndex(Media._ID));
    			   c.close();
    		   } catch(Exception e) {
    			   log("Cannot insert image using ContentResolver.");
    			   return retVal;
    		   }

    		   OutputStream outStream = null;
    		   try {
    			   outStream = resolver.openOutputStream(uri);
    			   outStream.write(jpegData);
    			   //DRM TEST 
    			   /*
                            FileInputStream fin =null;
                            fin = new FileInputStream(SAVE_ROOT + SAVE_DIRECTORY + File.separator + fileName);
                            // transfer the file to the DRM content provider
                            Intent item = DrmStore.addDrmFile(resolver, fin, title);
                            if (item == null) {
                                if (DBG) Log.i(LOG_TAG, "unable to add file " + uri + " to DrmProvider");
                                return retVal;
                            }     
    			    */ 
    		   } catch(Exception e) {
    			   log("Cannot write image.");
    			   return retVal;
    		   } finally {
    			   if(outStream != null) try { outStream.close(); } catch(Exception e) {}
    		   }

    		   retVal = CAPTURE_MSG_SUCCESS;			
    	   }		
    	   else {
    		   if(Environment.MEDIA_REMOVED.equals(externalStorageState)  || Environment.MEDIA_BAD_REMOVAL.equals(externalStorageState)) 
    		   {
    			   log("no sd card");
    			   retVal = CAPTURE_MSG_NO_SD_CARD;
    		   } else {
    			   log("sd card n/a");
    			   retVal = CAPTURE_MSG_SD_CARD_NA;
    		   }
    		   return retVal;
    	   }
    	   return retVal;
       }	

       private long AvailableSDCardSize(){
    	   long availSize = 0;
    	   File path = Environment.getExternalStorageDirectory();
    	   StatFs stat = new StatFs(path.getPath());
    	   long blockSize = stat.getBlockSize();
    	   long availBlocks = stat.getAvailableBlocks();
    	   availSize = blockSize * availBlocks /*/ 1024*/;
    	   return availSize;  //Byte
       }

       private long TotalSDCardSize(){
    	   long totalSize = 0;
    	   File path = Environment.getExternalStorageDirectory();
    	   StatFs stat = new StatFs(path.getPath());
    	   long blockSize = stat.getBlockSize();
    	   long totalBlocks = stat.getBlockCount();
    	   totalSize = blockSize * totalBlocks /*/ 1024*/;
    	   return totalSize;  //Byte
       }

       public long getCapturedIndex()
       {
    	   return mCaptureindex;
       }

       public String getCapturedFileName()
       {
    	   return mCaptureFileName;
       }

	/**
	 * fastFrameUpdate
	 *
	 *	Send I-frame request to encoder.   For CSVT, this API is not applicable.
	 *
	 */
	public void fastFrameUpdate()
	{
		mWrapper.fastFrameUpdate();
	}


	/**
	 * mediaNegoResult
	 *
	 *	Send media negotiated result to encoder and decoder.   For CSVT, this API is not applicable 
	 *
	 */
	public void mediaNegoResult(AsyncResult r)
	{
		mWrapper.mediaNegoResult(r);
	}


	/**
	 * After has received the event - "EV_FIRST_VIDEO_FRAME_RCVD" from NXP Library,
	 * Do job such as - Stop a progressbar animation and show a remote(Far) surfaceview . 
	 * */
	public void doFirstVideoFrameReceived()
	{
        	mStateManager.setRecvFirstFrame(true);
		VideoCallCard vcc = ((InVideoCallScreen)mContext).getVideoCallCard();
		vcc.hideReadyProgressbar();		
              vcc.enableButton(vcc.BUTTON_PRIVATE_SHOW);	     
        mStat.doFirstVideoFrameReceived();
	}

	public void engineCallbackCallstop()
	{
		log("engineDestroyCallback");	
		((InVideoCallScreen)mContext).getVideoCallCard().engineCallbackCallstop();
	}
}

