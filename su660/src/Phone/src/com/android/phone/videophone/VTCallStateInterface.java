/**
 * 
 */
package com.android.phone.videophone;

/**
 * @author hogyun.kim
 *
 */

/**
 * Call State & Phone State Interface
 * */
public interface VTCallStateInterface {
	public enum VTCallState{
		IDLE, 
		DIALING, 
		ALERTING, 
		INCOMING, 
		ACTIVE, 
		DISCONNECTED
	}

	public boolean isAlive();
	public boolean isRinging();
	public boolean isDialing();

}


/**
 * VT Engine State(ex-NXP Engine) Interface
 * */

interface VTEngineStateInterface{
	public enum VTEngineState{
		INIT, 
		ST_EARLYPREVIEW, 
		ON_EARLYPREVIEW,
		ST_LIVE, ON_LIVE,
		NORMALHANGUP,
		ABNORMALHANGUP,
		CAPTURING,
		REQUESTCMD
	}

	public boolean isEarlyPreview();
	public boolean isCanCommand();
	public boolean isCanHangup();
	public boolean isPendingHangup();
	public boolean isCapturing();
}


/**
 * App Sharing Type Interface
 * */
interface VTSharingTypeInterface{
	public enum VTSharingType{
		NOSHARING, 
		IMAGESHARING, 
		FILESHARING,
		HOLDING
	}

	public boolean isSharing();
}

