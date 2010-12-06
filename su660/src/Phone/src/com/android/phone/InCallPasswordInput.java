package com.android.phone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import android.net.Uri;
import junit.framework.Assert;


//import com.android.internal.telephony.Phone;
import com.lge.config.StarConfig;
import com.lge.provider.LGProvider;

/**
 * Phone app "InCallPasswordInput" screen.
 * Contents : Inserts Password when MO Lock
 * code by jaeyoung.ha@lge.com
 * 2010-10-12
*/
public class InCallPasswordInput extends Activity{
	private static final String LOG_TAG = "InCallPasswordInput";
    private static final boolean DBG =
            (PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneApp.DBG_LEVEL >= 2);

    private static final int CALL_LOCK_SCREEN = 0;	

    private String   mPhoneNumber   = null;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		if(DBG) log("InCallPasswordInput onCreate()...");

		final PhoneApp app = PhoneApp.getInstance();

		// Get : getIntent() data
		mPhoneNumber 	= getIntent().getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        //Assert
        Assert.assertNotNull("Error : No Phone Number" ,mPhoneNumber);
		
		// Launch com.lge.lgcommon.password.PasswordInput
		Intent intent = new Intent();
		intent.setClassName("com.lge.lgcommon", "com.lge.lgcommon.password.PasswordInput");
		startActivityForResult(intent, CALL_LOCK_SCREEN);

		return;
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		if(DBG) log("InCallPasswordInput onDestroy()...");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(DBG) log("requestCode ="+requestCode+", resultCode ="+resultCode);

		if(requestCode == CALL_LOCK_SCREEN){
			if(resultCode == RESULT_OK){
				if(DBG) log("onActivityResult : RESULT_OK");
				//20101102 wonho.moon@lge.com Password check add
				PhoneApp.getInstance().setIsCheckPassword(true);
				redialInCallScreen();
				finish();
			} else{
				//20101111 sumi920.kim@lge.com sendbroadcast call_state_idle when cancel Outgoing Call [START_LGE_LAB1] 
				if(StarConfig.OPERATOR.equals("SKT"))
				{
					if(PhoneUtils.isHDVideoCallAvailable())
					{
						final String CALL_STATE_IDLE = "com.skt.call.intent.action.CALL_STATE_IDLE";
						
						Intent broadcastIntent = new Intent(CALL_STATE_IDLE);
						broadcastIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, mPhoneNumber);
						//broadcastIntent.putExtra(EXTRA_ALREADY_CALLED, false);
						//broadcastIntent.putExtra(EXTRA_ORIGINAL_URI, "tel:" + number);
						sendBroadcast(broadcastIntent);
					}
				}
				//20101111 sumi920.kim@lge.com sendbroadcast call_state_idle when cancel Outgoing Call [END_LGE_LAB1]				
				finish();
			}
		}
	}

	private void redialInCallScreen() {
		if(DBG) log("InCallPasswordInput redialInCallScreen()...");

		Intent newIntent = new Intent(Intent.ACTION_CALL);

		newIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, mPhoneNumber);
		newIntent.setClass(getBaseContext(), InCallScreen.class);

		// LGT Roaming Redial Check routine 추후확인

		startActivity(newIntent);
	}
	
	private void log(String msg){
		Log.d(LOG_TAG, msg);
	}
}
	
	
	

	



 
