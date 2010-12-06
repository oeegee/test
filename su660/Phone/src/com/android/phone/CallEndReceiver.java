/* 20100723 yongwoon.choi@lge.com Lock/UnLock Incoming Scenario */
package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.Call;
import com.lge.config.StarConfig;

/*
  If you want to end current call(incoming, in call or outgoing) from other applications,
  use this broadcast receiver.
  usage in other applicatoins : 
    Intent callEndIntent = new Intent("com.lge.phone.action.CALL_END");
    sendBroadcast(callEndIntent);
*/

public class CallEndReceiver extends BroadcastReceiver {

  private static final String LOG_TAG = "CallEndReceiver";
  private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

  private Phone mPhone;
  private Call mForegroundCall;
  private Call mRingingCall;
  //20101023 sumi920.kim@lge.com End Call Recording [START_LGE_LAB1]
  private static InCallScreen mInCallScreen;
  
  @Override
  public void onReceive(Context context, Intent intent) {
    final PhoneApp app = PhoneApp.getInstance();
    setPhone(app.phone);

    terminateCurrentCall();

  }

  void setPhone(Phone phone) 
  {
    mPhone = phone;
    mForegroundCall = mPhone.getForegroundCall();
    mRingingCall = mPhone.getRingingCall();
  }

  void terminateCurrentCall()
  {
    Log.d(LOG_TAG,"terminateCurrentCall");
    
    final boolean hasRingingCall = !mRingingCall.isIdle();
    final boolean hasActiveCall = !mForegroundCall.isIdle();
  	Call.State state = mRingingCall.getState();

  	// 20101023 sumi920.kim@lge.com End Call Recording [START_LGE_LAB1]
  	if(StarConfig.COUNTRY.equals("KR"))
  	{
  		if(PhoneUtils.isSoundRecording()) 			
  			mInCallScreen.stopRecording();
  	}
  	// 20101023 sumi920.kim@lge.com End Call Recording [START_LGE_LAB1]
  	
    if ((state == Call.State.WAITING) || (state == Call.State.INCOMING))
    {
      PhoneUtils.hangupRingingCall(mPhone);
    }
    else if (hasActiveCall)
    {
      PhoneUtils.hangup(mPhone);
    }
    else
    {
      Log.d(LOG_TAG, "can't terminate current call : unknown call state : "+state);
    }
  }
  //20101023 sumi920.kim@lge.com End Call Recording [START_LGE_LAB1]
  public static void setInCallScreen(InCallScreen inCallscreen)
  {
	  mInCallScreen = inCallscreen;
  }
  //20101023 sumi920.kim@lge.com End Call Recording [END_LGE_LAB1]
}
