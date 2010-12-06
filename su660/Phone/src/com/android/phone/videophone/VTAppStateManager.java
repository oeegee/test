/**
 * 
 */
package com.android.phone.videophone;

import android.content.Context;
import android.util.Log;

import com.android.internal.telephony.Phone;

/**
 * Manage a app state - 
 * such as phone state or call state and Video Telephony Library engine state. 
 * @author hogyun.kim
 *
 */
public class VTAppStateManager implements 
VTCallStateInterface,VTEngineStateInterface,VTSharingTypeInterface
{
	private static final String LOG_TAG = "VTAppStateManager";

	private static final boolean DBG = true;

	public static VTAppStateManager manager = null;
	private Context mContext = null;

	//for VTCallStateInterface and VTEngineStateInterface , VTSharingTypeInterface
	private VTCallState mCallState = VTCallState.IDLE;
	private VTEngineState mEngineState = VTEngineState.INIT;
	private VTSharingType mSharingType = VTSharingType.NOSHARING;
    private boolean mRecvFirstFrame = false;
    private boolean mHangupPended = false;


	private VTAppStateManager(){
		if(DBG) log("VTAppStateManager() constructor!");
	}
	public static VTAppStateManager getInstance()
	{
		if(null == manager)
		{
			manager = new VTAppStateManager();
		}
		return manager;
	}

	/**
	 * Initialize members for every call 
	 * */
	public void initialize()
	{
		mEngineState = VTEngineState.INIT;
		mSharingType = VTSharingType.NOSHARING;
              mRecvFirstFrame = false;
	}

	private void log(String msg) {
		if(DBG)
		    Log.d(LOG_TAG, msg);
	}

	/**
	 * Set a context 
	 * */
	public void setContext(Context c)
	{
		mContext = c;
	}

	/**
	 * make a VTCallState with a Phone instance
	 * */
	public VTCallState makeVTCallState(Phone p)
	{
		Phone.State state = p.getState();  // IDLE, RINGING, or OFFHOOK
		VTCallState newState ;
		if (state ==  Phone.State.RINGING)
		{
			newState = VTCallState.INCOMING;
		}
		else 
		{
			switch (p.getForegroundCall().getState())
			{
			case IDLE :					
				newState = VTCallState.IDLE;					
				break;
			case ACTIVE :					
				newState = VTCallState.ACTIVE;
				break;
			case DIALING :					
				newState = VTCallState.DIALING;
				break;
			case ALERTING :					
				newState = VTCallState.ALERTING;
				break;
			case DISCONNECTED :
			case DISCONNECTING :					
				newState = VTCallState.DISCONNECTED;
				break;
			default:
				newState = VTCallState.IDLE;				
			}
		}

		return newState;
	}


	/**
	 * Set a VTCallState
	 * @param VTCallState
	 * */
	public void setCallState(VTCallState s) {
		mCallState = s;
	}


	/**
	 * Get a VTCallState
	 * @return VTCallState
	 * */
	public VTCallState getCallState() {
		return mCallState;
	}

	/**
	 * Set a EngineState
	 * @param state VTEngineState
	 * */
	public void setEngineState(VTEngineState state) {
		if (DBG) log("setEngineState >> prev [" + mEngineState + "] >> curr "+ state);

		mEngineState = state;
	}

	/**
	 * Get a VTEngineState
	 * @return VTEngineState
	 * */
	public VTEngineState getEngineState() {
		return mEngineState;
	}

	/**
	 * Get a VTSharingType
	 * @return VTSharingType
	 * */
	public VTSharingType getSharingType() {
		return mSharingType;
	}	

	public void setSharingType(VTSharingType type)
	{
		mSharingType = type;
	}

	//VTCallStateInterface implements Methods
	/////////////////////////////////////////////
	public boolean isAlive() {
		return !(mCallState == VTCallState.IDLE || mCallState == VTCallState.DISCONNECTED);
	}

	public boolean isRinging() {
		return mCallState == VTCallState.INCOMING;
	}

	public boolean isDialing() {
		return mCallState == VTCallState.DIALING || mCallState == VTCallState.ALERTING;
	}

	public boolean isActive() {
		return mCallState == VTCallState.ACTIVE;
	}

	//VTEngineStateInterface implements Methods
	/////////////////////////////////////////////
	public boolean isEarlyPreview() {
		return mEngineState == VTEngineState.ON_EARLYPREVIEW;
	}

	public boolean isCanCommand() {
		return mEngineState == VTEngineState.ON_LIVE;
	}


        // added VTEngineState.ST_EARLYPREVIEW, 20101023
	public boolean isCanHangup() {    	
		return (mEngineState == VTEngineState.ST_LIVE || 
				mEngineState == VTEngineState.ON_LIVE || 
				mEngineState == VTEngineState.CAPTURING || 
				mEngineState == VTEngineState.REQUESTCMD ||
				mEngineState == VTEngineState.ON_EARLYPREVIEW
		); 
	}
	
	public boolean isPendingHangup() {
		return mEngineState == VTEngineState.ST_EARLYPREVIEW;
	}

	public boolean isCapturing() {		
		return mEngineState == VTEngineState.CAPTURING;
	}

	public boolean isCanRetry() {
		return mEngineState == VTEngineState.INIT;
	}
    
	//VTSharingTypeInterface implements Methods
	/////////////////////////////////////////////
	//@Override
	public boolean isSharing() {
		return (mSharingType == VTSharingType.IMAGESHARING || 
				mSharingType == VTSharingType.FILESHARING || 
				mSharingType == VTSharingType.HOLDING
		);			
	}

	public void setRecvFirstFrame(boolean bRecv) {
		mRecvFirstFrame = bRecv;
	}
	
	public boolean isRecvFirstFrame() {
		return mRecvFirstFrame;
	}

	public void setHangupPended(boolean bHangupPended) {
		mHangupPended = bHangupPended;
	}
	
	public boolean isHangupPended() {
		return mHangupPended;
	}
	
	public boolean isCanRemoveSurface() {
		if ((!(mCallState == VTCallState.DIALING || mCallState == VTCallState.ALERTING))
				&& mEngineState == VTEngineState.INIT)
		{
			return true;
		}
		
		return false;
	}
}
