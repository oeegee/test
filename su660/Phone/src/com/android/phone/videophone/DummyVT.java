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
import com.lifevibes.videotelephony.VideoTelephonyEngine.*;

import android.os.AsyncResult;


/** 
 *  DummyVT is only for test in order to remove NXP libs.
 *
 */
public class DummyVT extends VTWrapper /*implements VideoTelephonyEngine.OnStatusChangeListener */ {
	public static final String LOG_TAG = "DummyVT";
	private static final boolean DBG =
		(PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

	// Reference to VideoTelephonyApp
	private VideoTelephonyApp mVTApp = null;
	private VTAppStateManager mStateManager = null;

	// Interface to VTEngine
	private int mRemoteTerminalType=128;
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
	public DummyVT(Context c) 
	{
		super(c);
		log("Constructor called!");
	}



	/**
	 * startPreview
	 * */
	public void startPreview(SurfaceHolder nearHolder, SurfaceHolder farHolder, boolean bLoopback)
	{
		log("vtEngine.prestart() bLoopback = " + bLoopback);

        OnStatusCallback(VideoTelephonyEngine.EV_CALLPRESTART, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_CAMERA_START, 0);
	}


	/**
	 * callPrivate
	 * */
	public void callPrivate()
	{
		log("vtEngine.switchFeature() - FEATURE_AVATARcalled!");
	}	

	/**
	 * connectCall
	 * */
	public void connectCall()
	{
		log("vtEngine.callstart() called!");

        OnStatusCallback(VideoTelephonyEngine.EV_LCOPEN, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCOPEN, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCOPEN, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCOPEN, 0);
        
        OnStatusCallback(VideoTelephonyEngine.EV_CALLSTART, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_FIRST_VIDEO_FRAME_RCVD, 0);
	}

	/**
	 * Set a Mute mode 
	 * @param isOn if true, the Mute is on, else is off.
	 * */
	public void setMute(boolean isOn)
	{
		if(isOn)
		{
			log("vtEngine.setTxMediaType() - NXPSWVTIL_VIDEO "+ isOn);
		}
		else
		{
			log("vtEngine.setTxMediaType() - NXPSWVTIL_AUDIOVIDEO "+ isOn);
		}
	}

	public void setHold(boolean isOn)
	{
			if(isOn)
			{
				log("vtEngine.setTxMediaType() - NXPSWVTIL_VIDEO "+ isOn);
				sendHold = true;
			}
			else
			{
				log("vtEngine.setTxMediaType() - NXPSWVTIL_AUDIOVIDEO "+ isOn);
				sendHold = false;
			}
	}

	/**
	 * endCallNormal
	 * */
	public void endCallNormal()
	{
		log("vtEngine.callhangup()-NXPSWVTIL_CALLHANGUP_NORMAL is called");  

        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);

        OnStatusCallback(VideoTelephonyEngine.EV_CAMERA_STOP, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_CALLSTOP, 0);
	}

	/**
	 * endCallAbnormal
	 * */
	public void endCallAbnormal()
	{
		log("vtEngine.callhangup()-NXPSWVTIL_CALLHANGUP_ABNORMAL is called");
        
        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_LCCLOSE, 0);

        OnStatusCallback(VideoTelephonyEngine.EV_CAMERA_STOP, 0);
        OnStatusCallback(VideoTelephonyEngine.EV_CALLSTOP, 0);
	}

	/**
	 * startImageSharinig
	 * */
	public void startImageSharinig(String fileName)
	{	   
		log("vtEngine.switchFeature()" + fileName);
	}
	/**
	 * stopImageSharinig
	 * */
	public void stopImageSharinig()
	{
		log("vtEngine.switchFeature()");
	}   

	/**
	 * startMedia
	 * */
	public void startMedia(int arg)
	{
		log("vtEngine.startMedia()" + arg);
	}

	/**
	 * cleanup
	 * */
	public void cleanup()
	{
		log("vtEngine.cleanup()");
		sendHold = false;
	}

	/**
	 * @param DTMFString  a String that holds the DTMF message.
	 * @return void 
	 */
	public int getLastLocalFrame(String path)
	{	
		int retVal=0;

		log("vtEngine.getLastLocalFrame()");

		return retVal;
	}

	/**
	 * getLastRemoteFrame
	 */
	public int getLastRemoteFrame(String path)
	{		
		int retVal=0;

		log("vtEngine.getLastRemoteFrame()");

		return retVal;
	}

	public void startFileSharing(String path)	
	{
		log("vtEngine.startImageSharinig()");
	}

	/**
	 * sendDTMFString
	 */
	public void sendDTMFString(String DTMFString)
	{
	}

	/**
	 * setCameraParameter
	 */
	public void setCameraParameter(String key,String value)
	{
		log("vtEngine.setCameraParameter()");
	}

	/**
	 * getCameraParameter
	 */
	public String getCameraParameter()
	{
		String ret="null"; 

		log("vtEngine.getCameraParameter()");

		return ret;
	}

	/**
	 * switchCamera
	 */
	public void switchCamera(int sensorID)
	{
		log("vtEngine.switchCamera() ID= " + sensorID);			
	}



	/**
	 * Set Camera id's number
	 *
	 * @param frontCameraId front camera id
	 * @param backCameraId back camera id
	 */
	public void setCameraIdNum(int frontCameraId, int backCameraId)
	{
		log("vtEngine.setCameraIdNum() front=" + frontCameraId + "  back=" + backCameraId);
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
	 * SetVTCodec
	 */
       public void SetVTCodec(){;}

       /**
	 * SetCallPreference
	 */
       public void SetCallPreference(){;}

       /**
	 * setVideoTelephonyOptions
	 */
       public void setVideoTelephonyOptions(){;}

       /**
	 * SetupDebugInfo
	 */
       public void SetupDebugInfo(){;}       

	/**
	 * OnStatusCallback
	 */
	//@Override
	public void OnStatusCallback(int vtStatusType, int statusArg1) {

		log("OnStatusCallback(vtStatusType("+vtStatusType+ ") - " + GetEventString(vtStatusType) + ")");

		switch (vtStatusType) {

		case VideoTelephonyEngine.EV_CALLPRESTART:
			getStateManager().setEngineState(VTEngineState.ON_EARLYPREVIEW);

			// incoming state
			if (true == getStateManager().isActive()) {
				getVideoTelephonyApp().connectCall();
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
			getStateManager().setEngineState(VTEngineState.INIT);
			break;

		case VideoTelephonyEngine.EV_AVATAR_START:
			getStateManager().setSharingType(VTSharingType.IMAGESHARING);
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
			getVideoTelephonyApp().showSubstitute(true, sendHold);
			break;

		case VideoTelephonyEngine.EV_AVATAR_STOP:			
			getStateManager().setSharingType(VTSharingType.NOSHARING);
			getStateManager().setEngineState(VTEngineState.ON_LIVE);
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
			break;

		case VideoTelephonyEngine.EV_TERMINAL_NUM:
			break;

		case VideoTelephonyEngine.EV_VENDORID:
			break;

		case VideoTelephonyEngine.EV_UII:
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
