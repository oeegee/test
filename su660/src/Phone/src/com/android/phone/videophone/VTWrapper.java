/**
 * 
 */
package com.android.phone.videophone;

import android.content.Context;
import android.view.SurfaceHolder;
import android.os.AsyncResult;

/**
 * @author jhyung.lee
 * 
 */
public abstract class VTWrapper 
{
	protected Context mContext = null;

	/**
	 * VTWrapper constructor. only get static API.
	 * */
	public VTWrapper(Context c) {
		log("VTWrapper() called!");
		mContext = c;		
	}

	public abstract void startPreview(SurfaceHolder nearHolder, SurfaceHolder farHolder, boolean bLoopback);
	public abstract void connectCall();
	public abstract void setMute(boolean isOn);
	public abstract void setHold(boolean isOn);
	public abstract void endCallNormal();
	public abstract void endCallAbnormal();
	public abstract void startImageSharinig(String fileName);
	public abstract void stopImageSharinig();
	public abstract void startMedia(int arg);
	public abstract void cleanup();
	protected abstract void log(String msg);


	/* 
		APIs  update at 2010-8-23
	 */
	public abstract int getLastLocalFrame(String path);
	public abstract int getLastRemoteFrame(String path);
	public abstract void startFileSharing(String path);		//jhlee
	public abstract void sendDTMFString(String DTMFString);
	public abstract String getCameraParameter();
	public abstract void setCameraParameter(String key,String value);
	public abstract void switchCamera(int sensorID);

	public abstract void fastFrameUpdate();
	public abstract void mediaNegoResult(AsyncResult r);

       public abstract void SetVTCodec();
       public abstract void SetCallPreference();
       public abstract void setVideoTelephonyOptions();
       public abstract void SetupDebugInfo();
}
