/* Kim Su Mi Insert to receive HD VideoCall event from HD VideoCall App*/

package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class InCallReceiver extends BroadcastReceiver {
	public	static	final 	String TAG = "InCallReceiver";
	public 	static 	boolean bHDAvailable = false;
	
	private static 	InCallTouchUi	mInCallTouchUi;
	
	public void onReceive(Context arg0, Intent arg1) {
		String 		LIVESHARE_EXTRA_OPTION = "LiveshareOption";
		String 		LIVESHARE_EXTRA_STARTCOMMAND = "LiveshareStart";
		boolean 	bLiveShareStartCommand = false;
		Log.e(TAG,"onReceive");
		
		bLiveShareStartCommand = arg1.getBooleanExtra(LIVESHARE_EXTRA_STARTCOMMAND, false);
		if(!bLiveShareStartCommand)
		{
			bHDAvailable = arg1.getBooleanExtra(LIVESHARE_EXTRA_OPTION, false);
			Log.i(TAG,"bHDAvailable = "+ bHDAvailable);
			
			if(mInCallTouchUi != null)
				mInCallTouchUi.setHDVideoCallStatus(bHDAvailable);
		}
		else
		{
			Log.i(TAG,"bLiveShareStartCommand = "+ bLiveShareStartCommand);	
			Log.i(TAG,"bHDAvailable = "+ bHDAvailable);			
		}	
	}
	
	public static void setInCallTouchUi(InCallTouchUi inCallTouchUi){
		mInCallTouchUi = inCallTouchUi;
	}
	
	
}

