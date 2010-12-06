/**
 * 
 */
package com.android.phone.videophone;

import java.net.Socket;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.DrmStore.Images;
import android.provider.MediaStore.Images.Media;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.android.phone.InVideoCallScreen;
import com.android.phone.PhoneApp;
import com.android.phone.VideoCallCard;
import com.android.phone.videophone.VTEngineStateInterface.VTEngineState;
import com.android.phone.videophone.VTSharingTypeInterface.VTSharingType;

import com.lifevibes.videotelephony.VideoTelephonyEngine;
import com.lifevibes.videotelephony.VideoTelephonyEngine.EnhancedfeatureType;
import com.lifevibes.videotelephony.VideoTelephonyEngine.ImageType;
import com.lifevibes.videotelephony.VideoTelephonyEngine.MediaMode;

import android.os.AsyncResult;


/** 
 * @author jhyung.lee
 *
 */
public class CSVT extends VTWrapper implements VideoTelephonyEngine.OnStatusChangeListener  {
	public static final String LOG_TAG = "CSVT";
	private static final boolean DBG =
		(PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

	// Reference to VideoTelephonyApp
	private VideoTelephonyApp mVTApp = null;
	private VTAppStateManager mStateManager = null;

	// Interface to VTEngine
	private VideoTelephonyEngine vtEngine = null;
	private VideoTelephonyEngine.CallPreferences mCallPrefs = null;
	private VideoTelephonyEngine.VendorID mLocalVendorID=null;
	private VideoTelephonyEngine.VendorID mRemoteVendorID=null;
	private VideoTelephonyEngine.UserInputIndication remoteUII=null;
	private int mRemoteTerminalType=128;

	private VideoTelephonyEngine.ModuleTraceLevel traceLevel=null;
	private VideoTelephonyEngine.DataDumps dataDumps=null;


	private Socket socketHandle;	

	//for capture function start
	private byte[] localYUV420ImageBuffer;
	private byte [] remoteRGB565ImageBuffer;

	private static final int YUV420_QCIF_BUFFER_SIZE = 38016;
	private static final int RGB565_QCIF_BUFFER_SIZE = 50688;
	//for capture function end 	

	private boolean sendHold = false;
	/**
	 * Get VideoTelephonyApp from context
	 * */
	private VideoTelephonyApp getVideoTelephonyApp()
	{
		if(null == mVTApp)
		{
			mVTApp = VideoTelephonyApp.getInstance(mContext);
		}
		return mVTApp;
	}

	/**
	 * Get VTAppStateManager from context
	 * */
	private VTAppStateManager getStateManager()
	{
		if(null == mStateManager)
		{
			mStateManager = VTAppStateManager.getInstance();
		}

		return mStateManager;
	}



	/**
	 * VTWrapper constructor. only get static API.
	 * */
	public CSVT(Context c) 
	{
		super(c);
        
		log("Constructor called!");

		//Get a engine instance
		vtEngine = new VideoTelephonyEngine(SetVendorID());
		vtEngine.setOnStatusChangeListener(this);
              
		vtEngine.setCameraIdNum(VTPreferences.SENSOR_ID_FRONT_PORTRAIT, VTPreferences.SENSOR_ID_BACK_PORTRAIT);

		// malloc for remote vendorID and UII
		mRemoteVendorID = new VideoTelephonyEngine.VendorID();
		mRemoteVendorID.nonStandardIdentifier.objectIdentifier = new char[VTPreferences.UII_BUFFER_SIZE];
		remoteUII = new VideoTelephonyEngine.UserInputIndication();
		remoteUII.userString = new char[VTPreferences.UII_BUFFER_SIZE];
		remoteUII.nonStandardIdentifier.objectIdentifier = new char[VTPreferences.UII_BUFFER_SIZE];

		localYUV420ImageBuffer = new byte[YUV420_QCIF_BUFFER_SIZE];
		remoteRGB565ImageBuffer = new byte[RGB565_QCIF_BUFFER_SIZE];
	}

       public void SetVTCodec()
       {        
              int value,value1,value2;

              VTPreferences.CODEC_DEFAULT = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_H324_VIDEO_CODEC, 0);  
              //if(Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_H324_SETIMAGEQUALITY, 0) == 1)
             {
                    value = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_H324_FRAMERATE, 0);
                    VTPreferences.video_frame_rate= VTPreferences.VIDEO_FRAME_RATE[value];
                    value1 = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_H324_BITRATE, 0);
                    VTPreferences.video_encoding_bitrate=  VTPreferences.VIDEO_ENCODING_BITRATE[value1];
                    value2 = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_H324_REFRESHPERIOD, 0); 
                     VTPreferences.intra_frame_interval= VTPreferences.INTRA_FRAME_INTERVAL[value2];
                     //Settings.System.putInt(mContext.getContentResolver(), Settings.System.VT_H324_SETIMAGEQUALITY, 0); 
               } 
              VTPreferences.audio_encoding_bitrate = 12200;

             log("SetVTCodec Codec:"+VTPreferences.CODEC_DEFAULT);
             log("SetVTCodec video_frame_rate:"+VTPreferences.VIDEO_FRAME_RATE[value]);
             log("SetVTCodec video_encoding_bitrate:"+VTPreferences.VIDEO_ENCODING_BITRATE[value1]);
             log("SetVTCodec intra_frame_interval:"+VTPreferences.INTRA_FRAME_INTERVAL[value2]);
             log("SetVTCodec audio_encoding_bitrate:"+VTPreferences.audio_encoding_bitrate);
        }

	/**
	 * Setup CallPreference for each carrier 
	 * */
	public void SetCallPreference()
	{
	       log("SetCallPreference");
		if (null == mCallPrefs)
			mCallPrefs = new VideoTelephonyEngine.CallPreferences();

        if (VTPreferences.CODEC_DEFAULT == VTPreferences.CODEC_MPEG4_PREFERED
            || VTPreferences.CODEC_DEFAULT == VTPreferences.CODEC_MPEG4_ONLY)

        {
            mCallPrefs.videoCodingFormat = VideoTelephonyEngine.VideoCodingType.NXPSWVTIL_MPEG4;
        }
        else
        {
            mCallPrefs.videoCodingFormat = VideoTelephonyEngine.VideoCodingType.NXPSWVTIL_H263;
        }

		mCallPrefs.videoCodingBitrate = VTPreferences.video_encoding_bitrate/*VideoTelephonyEngine.VideoBitRate.NXPSWVTIL_40KBPS*/;
		mCallPrefs.videoFrameRate = VTPreferences.video_frame_rate/*VideoTelephonyEngine.VideoFrameRate.NXPSWVTIL_10FPS*/;
		mCallPrefs.audioCodingFormat = VideoTelephonyEngine.AudioFormat.NXPSWVTIL_AMRNB;
		mCallPrefs.audioBitrate = VideoTelephonyEngine.AudioBitRate.NXPSWVTIL_k122KBPS;

		/* TODO: change this when 9/2 lib is applied to git*/
		/* It is the preferred intra Frame interval in number of seconds (default value 2). */
		mCallPrefs.intraFrameInterval = VTPreferences.intra_frame_interval/*VTPreferences.INTRA_FRAME_INTERVAL*/;  
		mCallPrefs.bEnableDeblockingFilter = VTPreferences.ENABLE_DEBLOCKING_FILTER;
		mCallPrefs.bEnableDTX = VTPreferences.ENABLE_DTX;

	}

	/**
	 * Setup VendorID according to vendor ID
	 * */
	private VideoTelephonyEngine.VendorID SetVendorID()
	{
	       log("SetVendorID");     
		if (null == mLocalVendorID)
			mLocalVendorID = new VideoTelephonyEngine.VendorID();

		mLocalVendorID.nonStandardIdentifier = new VideoTelephonyEngine.NonStandardIdentifier();
		mLocalVendorID.vendorIdType = VTPreferences.VENDOR_ID_TYPE;
        
		if (mLocalVendorID.vendorIdType == VideoTelephonyEngine.VendorIdType.NXPSWVTIL_H221_NONSTANDARD)
		{
			mLocalVendorID.nonStandardIdentifier.T35CountryCode = VTPreferences.VENDOR_ID_T35CountryCode;
			mLocalVendorID.nonStandardIdentifier.T35Extension =  VTPreferences.VENDOR_ID_T35Extension;
			mLocalVendorID.nonStandardIdentifier.ManufacturerCode =  VTPreferences.VENDOR_ID_ManufacturerCode;
		}
		else /* if (mLocalVendorID.vendorIdType == NXPSWVTIL_OBJECT_ID) */
		{
			mLocalVendorID.nonStandardIdentifier.objectIdentifierLength = VTPreferences.VENDOR_ID_OBJECT_ID.length();
			mLocalVendorID.nonStandardIdentifier.objectIdentifier =  VTPreferences.VENDOR_ID_OBJECT_ID.toCharArray();
		}
		
		mLocalVendorID.productNumber = VTPreferences.VENDOR_ID_PRODUCT_NUM;
		mLocalVendorID.productVersion = VTPreferences.VENDOR_ID_PRODUCT_VER;

		return mLocalVendorID;
	}


	/**
	 * Set Video Telephony Options. These include, options for disabling standard compliance for certain features 
	 * and setting local terminal type.
	 *
	 * @param vtOptions :  an object of type {@link VideoTelephonyOptions VideoTelephonyOptions}.
	 */
	public void setVideoTelephonyOptions()
	{
              log("setVideoTelephonyOptions");  
		VideoTelephonyEngine.VideoTelephonyOptions vtOptions = new VideoTelephonyEngine.VideoTelephonyOptions();

        //** Terminal Type can be configured to testing videcall message */
		vtOptions.terminalType = 128;
        
        //** Support MPEG only and H.263 only */
        if (VTPreferences.CODEC_DEFAULT == VTPreferences.CODEC_MPEG4_ONLY)
        {
            vtOptions.bDisableMPEG4Codec = false;
            vtOptions.bDisableH263Codec= true;
        }
        else if (VTPreferences.CODEC_DEFAULT == VTPreferences.CODEC_H263_ONLY)
        {
            vtOptions.bDisableMPEG4Codec = true;
            vtOptions.bDisableH263Codec= false;
        }
        
        //** Remove UII in TCS table  */        
        vtOptions.bDisableUIISupport = true;
        
        //** Do not send MaxPDU size,  h221SkewIndication, and TSTO*/
        vtOptions.bDisableMaxPDUSize = true;
        vtOptions.bDisableSkewIndication = true;
        vtOptions.bDisableTSTOSupport = true;

        //** Support MuxLevelChange  */
        vtOptions.bDisableMUXLevelChange = false;

        //** VendorID is sent before TCS */        
        vtOptions.vendorIdConfiguration = 1 /*NXPSWVTIL_SEND_VENDORID_BEFORE_TCS*/;
    
		try 
		{
			log("vtEngine.setVideoTelephonyOptions()");
			vtEngine.setVideoTelephonyOptions(vtOptions);
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
	}


	/**
	 * Setup Debugging info: 
	 *   trace level and dump
	 * */
	public void SetupDebugInfo()
	{     
	        log("SetupDebugInfo");  
		if (null == traceLevel)
			traceLevel = new VideoTelephonyEngine.ModuleTraceLevel();

		traceLevel.VTelSTraceLevel = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_DBG_CORE, 0);
		traceLevel.callControlTraceLevel = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_DBG_CALLCONTROL, 0);
		traceLevel.stack3g324mTraceLevel = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_DBG_3G324M, 0);
		traceLevel.eVTelSTraceLevel = Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_DBG_ENHANCEMENT, 0);
		vtEngine.setTraceLevel(traceLevel);

		if (null == dataDumps)
			dataDumps = new VideoTelephonyEngine.DataDumps();

		/** Enable or Disable Tx and Rx Network Data dump. */
		if(Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_DUMP_NWDATA,0) == 1)                
			dataDumps.bEnableNWDataDumps = true;
		else
			dataDumps.bEnableNWDataDumps = false;
		/** Enable or Disable Tx Audio and Video Data dump. */
		if(Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_DUMP_TXAVDATA,0) == 1)
			dataDumps.bEnableTxAVDataDumps = true;
		else
			dataDumps.bEnableTxAVDataDumps = false;
		/** Enable or Disable Rx Audio and Video Data dump. */
		if(Settings.System.getInt(mContext.getContentResolver(),Settings.System.VT_DUMP_RXAVDATA,0) == 1)
			dataDumps.bEnableRxAVDataDumps = true;
		else
			dataDumps.bEnableRxAVDataDumps = false;

		vtEngine.enableDataDumps(dataDumps);
	}



	/**
	 * startPreview
	 * */
	public void startPreview(SurfaceHolder nearHolder, SurfaceHolder farHolder, boolean bLoopback)
	{
	    if(DBG)
		{
	        Log.w(LOG_TAG, "startPreview(), nearHolder = " + nearHolder);
	        Log.w(LOG_TAG, "startPreview(), farHolder = " + farHolder);
		}
		
		try 
		{
			log("vtEngine.prestart() bLoopback = " + bLoopback);
			if (VTPreferences.SENSOR_ID_DEFAULT == VTPreferences.SENSOR_ID_FRONT_PORTRAIT)
			{
				if (VTPreferences.ENCODER_FLIP_FRONT_PORTRAIT)
				{					
//					vtEngine.prestart(mCallPrefs, nearHolder, farHolder, VideoTelephonyEngine.CamIdSet.frontCameraMirror);
//					//@Fixed ME - by hgkim 10/07
					//in VT scenario, top surfaceview is nearView.
					vtEngine.prestart(mCallPrefs, farHolder, nearHolder, VideoTelephonyEngine.CamIdSet.frontCameraMirror, bLoopback);
				}
				else
				{
//					vtEngine.prestart(mCallPrefs, nearHolder, farHolder, VideoTelephonyEngine.CamIdSet.frontCamera);		
//					//@Fixed ME - by hgkim 10/07
					//in VT scenario, top surfaceview is nearView.
					vtEngine.prestart(mCallPrefs, farHolder, nearHolder, VideoTelephonyEngine.CamIdSet.frontCamera, bLoopback);
				}				
			}
			else
			{
//				vtEngine.prestart(mCallPrefs, nearHolder, farHolder, VideoTelephonyEngine.CamIdSet.backCamera);
				//@Fixed ME - by hgkim 10/07
				//in VT scenario, top surfaceview is nearView.
				vtEngine.prestart(mCallPrefs, farHolder, nearHolder, VideoTelephonyEngine.CamIdSet.backCamera, bLoopback);
			}	   					
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}


	/**
	 * callPrivate
	 * */
	public void callPrivate()
	{
		try 
		{
			log("vtEngine.switchFeature() - FEATURE_AVATARcalled!");
			vtEngine.switchFeature(EnhancedfeatureType.FEATURE_AVATAR,ImageType.NXPSWVTIL_AVATARYUV420,"/sdcard/eagle.yuv");
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}	

	/**
	 * connectCall
	 * */
	public void connectCall()
	{
		try 
		{
			log("vtEngine.callstart() called!");
			vtEngine.callstart(socketHandle);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Set a Mute mode 
	 * @param isOn if true, the Mute is on, else is off.
	 * */
	public void setMute(boolean isOn)
	{
		try 
		{
			if(isOn)
			{
				log("vtEngine.setTxMediaType() - NXPSWVTIL_VIDEO "+ isOn);
				vtEngine.setTxMediaMode(MediaMode.NXPSWVTIL_VIDEO);
			}
			else
			{
				log("vtEngine.setTxMediaType() - NXPSWVTIL_AUDIOVIDEO "+ isOn);
				vtEngine.setTxMediaMode(MediaMode.NXPSWVTIL_AUDIOVIDEO);			
			}
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	public void setHold(boolean isOn)
	{
		try 
		{
			if(isOn)
			{
				log("vtEngine.setTxMediaType() - NXPSWVTIL_VIDEO "+ isOn);
				vtEngine.setTxMediaMode(MediaMode.NXPSWVTIL_VIDEO);
				sendHold = true;
			}
			else
			{
				log("vtEngine.setTxMediaType() - NXPSWVTIL_AUDIOVIDEO "+ isOn);
				vtEngine.setTxMediaMode(MediaMode.NXPSWVTIL_AUDIOVIDEO);		
				sendHold = false;
			}
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * endCallNormal
	 * */
	public void endCallNormal()
	{
		try 
		{
			log("vtEngine.callhangup()-NXPSWVTIL_CALLHANGUP_NORMAL is called");  
			vtEngine.callhangup(VideoTelephonyEngine.CallHangupType.NXPSWVTIL_CALLHANGUP_NORMAL);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


	}

	/**
	 * endCallAbnormal
	 * */
	public void endCallAbnormal()
	{
		try 
		{
			log("vtEngine.callhangup()-NXPSWVTIL_CALLHANGUP_ABNORMAL is called");
			vtEngine.callhangup(VideoTelephonyEngine.CallHangupType.NXPSWVTIL_CALLHANGUP_ABNORMAL);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * startImageSharinig
	 * */
	public void startImageSharinig(String fileName)
	{	   
		try 
		{
			log("vtEngine.switchFeature()" + fileName);
			//YUV => ImageType.NXPSWVTIL_AVATARYUV420
			//RGB =>  ImageType.NXPSWVTIL_AVATARRGB565
			//JPEG =>
			vtEngine.switchFeature(EnhancedfeatureType.FEATURE_AVATAR, ImageType.NXPSWVTIL_AVATARJPEG, fileName);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * stopImageSharinig
	 * */
	public void stopImageSharinig()
	{
		try 
		{
			log("vtEngine.switchFeature()");
			vtEngine.switchFeature(EnhancedfeatureType.FEATURE_CAMERA, 0, "null");
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}   

	/**
	 * startMedia
	 * */
	public void startMedia(int arg)
	{
		try 
		{
			log("vtEngine.startMedia()" + arg);
			vtEngine.startMedia(arg);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * cleanup
	 * */
	public void cleanup()
	{
		try 
		{
			log("vtEngine.cleanup()");
			vtEngine.cleanup();
			sendHold = false;
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @param DTMFString  a String that holds the DTMF message.
	 * @return void 
	 */
	public int getLastLocalFrame(String path)
	{	
		int retVal;

		log("vtEngine.getLastLocalFrame()");

		vtEngine.getLastLocalFrame(localYUV420ImageBuffer);

		retVal = getVideoTelephonyApp().localImageCapture(localYUV420ImageBuffer);

		getStateManager().setEngineState(VTEngineState.ON_LIVE);

		return retVal;
	}

	/**
	 * getLastRemoteFrame
	 */
	public int getLastRemoteFrame(String path)
	{		
		int retVal;

		log("vtEngine.getLastRemoteFrame()");

		vtEngine.getLastRemoteFrame(remoteRGB565ImageBuffer);

		retVal = getVideoTelephonyApp().remoteImageCapture(remoteRGB565ImageBuffer);

		getStateManager().setEngineState(VTEngineState.ON_LIVE);	

		return retVal;
	}

	public void startFileSharing(String path)	
	{
		try 
		{
			//TBD
			log("vtEngine.startImageSharinig()");
			//vtEngine.switchFeature(EnhancedfeatureType.FEATURE_STREAMING,    //later...
			//				0, "/sdcard/Recording_fs.3gp");
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * sendDTMFString
	 */
	public void sendDTMFString(String DTMFString)
	{

		try 
		{
			log("vtEngine.sendDTMFString()" + DTMFString);

			VideoTelephonyEngine.UserInputIndication uii = new VideoTelephonyEngine.UserInputIndication();
			uii.userInputType = VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_ALPHANUMERIC;
			uii.userString = DTMFString.toCharArray();
			uii.userStringLength = uii.userString.length;
			vtEngine.sendUserInputIndication(uii);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}


	/**
	 *   sendUserData:
	 * @param userData: The string sent to the remote
	 */
	public void sendUserData(String userData)
	{

		try 
		{
			log("vtEngine.sendUserData()" + userData);

			VideoTelephonyEngine.UserInputIndication uii = new VideoTelephonyEngine.UserInputIndication();
    		uii.userInputType = VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_NONSTANDARD_H221_PARAMS;

            if (VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_NONSTANDARD_OBJECT_ID == uii.userInputType)
            {
                String oid = "1.1.1";
                uii.nonStandardIdentifier.objectIdentifier = oid.toCharArray();
                uii.nonStandardIdentifier.objectIdentifierLength = oid.length();
            }
            else if (VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_NONSTANDARD_H221_PARAMS == uii.userInputType)
            {
                uii.nonStandardIdentifier.ManufacturerCode = 0x8000;
                uii.nonStandardIdentifier.T35CountryCode = 0x61;
                uii.nonStandardIdentifier.T35Extension = 0x16;
            }
            else
            {
                log("vtEngine.sendUserData() : Invalid Type");
                return ;
            }

			uii.userString = userData.toCharArray();
			uii.userStringLength = uii.userString.length;
            
			vtEngine.sendUserInputIndication(uii);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
        }

/*
 case 1. NXPSWVTIL_UII_NONSTANDARD_OBJECT_ID type
        VideoTelephonyEngine.UserInputIndication uii = new VideoTelephonyEngine.UserInputIndication();
        uii.userInputType = VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_NONSTANDARD_OBJECT_ID;

        String oid = "1.1.1";
        uii.nonStandardIdentifier.objectIdentifier = oid.toCharArray();
        uii.nonStandardIdentifier.objectIdentifierLength = oid.length();

        uii.userString = userData.toCharArray();
        uii.userStringLength = uii.userString.length;

        vtEngine.sendUserInputIndication(uii);
			
<Result> 
value MultimediaSystemControlMessage ::= indication : userInput : nonStandard :
      {
        nonStandardIdentifier h221NonStandard :
          {
            t35CountryCode 3,
            t35Extension 0,
            manufacturerCode 0
          },
        data '31000000000000000000000000'H
      }


case 2. NXPSWVTIL_UII_NONSTANDARD_H221_PARAMS type
        VideoTelephonyEngine.UserInputIndication uii = new VideoTelephonyEngine.UserInputIndication();

        uii.userInputType = VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_NONSTANDARD_H221_PARAMS;
        uii.nonStandardIdentifier.ManufacturerCode = (short) 0x8000;
        uii.nonStandardIdentifier.T35CountryCode = 0x61;
        uii.nonStandardIdentifier.T35Extension = 0x16;

        uii.userString = userData.toCharArray();
        uii.userStringLength = uii.userString.length;

        vtEngine.sendUserInputIndication(uii);			

<Result>
value MultimediaSystemControlMessage ::= indication : userInput : nonStandard :
      {
        nonStandardIdentifier h221NonStandard :
          {
            t35CountryCode 97,
            t35Extension 22,
            manufacturerCode 32768
          },
        data '31000000000000000000000000'H
      }

        
*/
        

	}


	/**
	 * setCameraParameter
	 */
	public void setCameraParameter(String key,String value)
	{

		try 
		{
			log("vtEngine.setCameraParameter()");
			vtEngine.setCameraParameter(key, value);
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


	}

	/**
	 * getCameraParameter
	 */
	public String getCameraParameter()
	{
		String ret; 

		try 
		{
			log("vtEngine.getCameraParameter()");
			ret = vtEngine.getCameraParameter();
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
			ret = "null";
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
			ret = "null";	
		}
		catch (Exception e)
		{
			ret = "null";
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * switchCamera
	 */
	public void switchCamera(int sensorID)
	{
		try 
		{
			if (sensorID == VTPreferences.SENSOR_ID_FRONT_PORTRAIT)
			{
				if (VTPreferences.ENCODER_FLIP_FRONT_PORTRAIT)
				{
					vtEngine.switchCamera(VideoTelephonyEngine.CamIdSet.frontCameraMirror);
				}
				else
				{
					vtEngine.switchCamera(VideoTelephonyEngine.CamIdSet.frontCamera);
				}				
			}
			else
			{
				vtEngine.switchCamera(VideoTelephonyEngine.CamIdSet.backCamera);
			}

			log("vtEngine.switchCamera() ID= " + sensorID);			
		}
		catch (IllegalArgumentException e)
		{
			log("IllegalArgumentException "+ e.getMessage());
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


	}


	/**
	 * Set Camera id's number
	 *
	 * @param frontCameraId front camera id
	 * @param backCameraId back camera id
	 */
	public void setCameraIdNum(int frontCameraId, int backCameraId)
	{
		try 
		{
			log("vtEngine.setCameraIdNum() front=" + frontCameraId + "  back=" + backCameraId);
			vtEngine.setCameraIdNum(frontCameraId, backCameraId);
		}
		catch (IllegalStateException e)
		{
			log("IllegalStateException "+ e.getMessage());
		}
	}	


	/**
	 * fastFrameUpdate
	 *
	 *  Send I-frame request to encoder.   For CSVT, this API is not applicable 
	 *
	 */
	public void fastFrameUpdate(){;}


	/**
	 * mediaNegoResult
	 *
	 *  Send media negotiated result to encoder and decoder.   For CSVT, this API is not applicable 
	 *
	 */
	public void mediaNegoResult(AsyncResult r){;}


	/**
	 * OnStatusCallback
	 */
	//@Override
	public void OnStatusCallback(int vtStatusType, int statusArg1) {

		log("OnStatusCallback(vtStatusType("+vtStatusType+ ") - " + GetEventString(vtStatusType) + ")");

		switch (vtStatusType) {

		case VideoTelephonyEngine.EV_CALLPRESTART:
			if (getStateManager().isHangupPended())
			{
				log("Pended call end!");
				getStateManager().setHangupPended(false);
				getStateManager().setEngineState(VTEngineState.ABNORMALHANGUP);
				endCallAbnormal();				
			}
			else
			{
				getStateManager().setEngineState(VTEngineState.ON_EARLYPREVIEW);
	
				// incoming state
				if (true == getStateManager().isActive()) {
					getVideoTelephonyApp().connectCall();
				}
			}
			break;

		case VideoTelephonyEngine.EV_LCOPEN:
			startMedia(statusArg1);
			break;

		case VideoTelephonyEngine.EV_CALLSTART:
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			if(((InVideoCallScreen)mContext).isSubstituImage == 1)
			{
				VideoCallCard vcc = ((InVideoCallScreen)mContext).getVideoCallCard();
				getVideoTelephonyApp().startImageSharinig(vcc.sendSubsImagePath);
			}
			break;

		case VideoTelephonyEngine.EV_CALLSTOP:
			// check PhoneUtil.callHangup() state check done in the function
			getVideoTelephonyApp().rilCallHangup();

			// call cleanup to NXP 
			cleanup();
			getStateManager().setHangupPended(false);
			getStateManager().setEngineState(VTEngineState.INIT);
			getVideoTelephonyApp().engineCallbackCallstop();
			break;

              case VideoTelephonyEngine.EV_CAMERA_STOP:
                    break;

		case VideoTelephonyEngine.EV_AVATAR_START:
			getStateManager().setSharingType(VTSharingType.IMAGESHARING);
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			getVideoTelephonyApp().showSubstitute(true, sendHold);
			break;

		case VideoTelephonyEngine.EV_AVATAR_STOP:			
			getStateManager().setSharingType(VTSharingType.NOSHARING);
			//getStateManager().setEngineState(VTEngineState.ON_LIVE);
			getVideoTelephonyApp().showSubstitute(false, sendHold);
			break;

		case VideoTelephonyEngine.EV_FILESHARING_START:
			getStateManager().setSharingType(VTSharingType.FILESHARING);
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			getVideoTelephonyApp().showMovSubstitute(true);  
			break;

		case VideoTelephonyEngine.EV_FILESHARING_STOP:
			getStateManager().setSharingType(VTSharingType.NOSHARING);				
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			getVideoTelephonyApp().showMovSubstitute(false);	
			break;

		case VideoTelephonyEngine.EV_CALLRECORD_START:
			getVideoTelephonyApp().setRecording(true);
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			break;

		case VideoTelephonyEngine.EV_CALLRECORD_STOP:
			getVideoTelephonyApp().setRecording(false);
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			break;

		case VideoTelephonyEngine.EV_CAMERA_START:
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			break;

		case VideoTelephonyEngine.EV_AVMUTE_DONE:
			break;

		case VideoTelephonyEngine.EV_AVSECRET_DONE:
                     if(sendHold)
                        getStateManager().setSharingType(VTSharingType.HOLDING);
                     else
                        getStateManager().setSharingType(VTSharingType.NOSHARING);
                     //getStateManager().setEngineState(VTEngineState.ON_LIVE);
			break;

		case VideoTelephonyEngine.EV_TERMINAL_NUM:
			mRemoteTerminalType = statusArg1;
			log("Remote Terminal Type " + statusArg1);
			break;

		case VideoTelephonyEngine.EV_VENDORID:
			vtEngine.getRemoteVendorId(mRemoteVendorID);
		    log("Remote VendorID Type " + mRemoteVendorID.vendorIdType + " Product Number [" + mRemoteVendorID.productNumber+ "] Product Version [" + mRemoteVendorID.productVersion + "]");
			break;

		case VideoTelephonyEngine.EV_UII:
			vtEngine.receiveUserInputIndication(remoteUII);

    		if (remoteUII.userInputType == VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_ALPHANUMERIC)
			{
				log("UserInputIndication alpha-numeric= " + remoteUII.userString[0] + "  len=" + remoteUII.userStringLength);
			}
			else if (remoteUII.userInputType == VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_NONSTANDARD_H221_PARAMS)
			{
				log("UserInputIndication h221 --> " + 
                    " t35CountryCode = "+ remoteUII.nonStandardIdentifier.T35CountryCode +
                    " t35Extension = "+ remoteUII.nonStandardIdentifier.T35Extension +
                    " manufacturerCode = "+ remoteUII.nonStandardIdentifier.ManufacturerCode +
                    "  data= "+ remoteUII.userString+
                    "  len=" + remoteUII.userStringLength);                
			}
            else if (remoteUII.userInputType == VideoTelephonyEngine.UserInputType.NXPSWVTIL_UII_NONSTANDARD_OBJECT_ID)
            {
				log("UserInputIndication OID --> oid=" + remoteUII.nonStandardIdentifier.objectIdentifier + 
                    " data= "  + remoteUII.userString+
                    " len=" + remoteUII.userStringLength);
            }
            else
            {
                log ("unexpected");
            }
			break;

		case VideoTelephonyEngine.EV_FIRST_VIDEO_FRAME_RCVD:
			getVideoTelephonyApp().doFirstVideoFrameReceived();
			break;

		}
	}



	/**
	 * GetEventString
	 */
	String GetEventString(int vtStatusType)
	{
		switch (vtStatusType)
		{
		case VideoTelephonyEngine.EV_LCOPEN : return "EV_LCOPEN";
		case VideoTelephonyEngine.EV_LCCLOSE : return "EV_LCCLOSE";
		case VideoTelephonyEngine.EV_CALLPRESTART : return "EV_CALLPRESTART";
		case VideoTelephonyEngine.EV_CALLSTART : return "EV_CALLSTART";
		case VideoTelephonyEngine.EV_FIRST_VIDEO_FRAME_RCVD : return "EV_FIRST_VIDEO_FRAME_RCVD";
		case VideoTelephonyEngine.EV_UII: return "EV_UII";
		case VideoTelephonyEngine.EV_REMOTE_FRAME_CAPTURED : return "EV_REMOTE_FRAME_CAPTURED";
		case VideoTelephonyEngine.EV_LOCAL_FRAME_CAPTURED : return "EV_LOCAL_FRAME_CAPTURED";
		case VideoTelephonyEngine.EV_AVATAR_START : return "EV_AVATAR_START";
		case VideoTelephonyEngine.EV_AVATAR_STOP : return "EV_AVATAR_STOP";
		case VideoTelephonyEngine.EV_CALLSTOP : return "EV_CALLSTOP";
		case VideoTelephonyEngine.EV_CALLRECORD_START : return "EV_CALLRECORD_START";
		case VideoTelephonyEngine.EV_CALLRECORD_STOP : return "EV_CALLRECORD_STOP";
		case VideoTelephonyEngine.EV_CALLRECORD_STORAGE_FULL : return "EV_CALLRECORD_STORAGE_FULL";
		case VideoTelephonyEngine.EV_FILESHARING_START : return "EV_FILESHARING_START";
		case VideoTelephonyEngine.EV_FILESHARING_STOP : return "EV_FILESHARING_STOP";
		case VideoTelephonyEngine.EV_AVMUTE_DONE : return "EV_AVMUTE_DONE";
		case VideoTelephonyEngine.EV_AVSECRET_DONE : return "EV_AVSECRET_DONE";
		case VideoTelephonyEngine.EV_CAMERA_START : return "EV_CAMERA_START";
		case VideoTelephonyEngine.EV_CAMERA_STOP : return "EV_CAMERA_STOP";
		case VideoTelephonyEngine.EV_TERMINAL_NUM : return "EV_TERMINAL_NUM";
		case VideoTelephonyEngine.EV_VENDORID : return "EV_VENDORID";
		case VideoTelephonyEngine.EV_MAX : return "EV_MAX";

		default: return "Unknown";
		}
	}

	protected void log(String msg) {
		if (DBG) Log.i(LOG_TAG, msg);
	}
}
