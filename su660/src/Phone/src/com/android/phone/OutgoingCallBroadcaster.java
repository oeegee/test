/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.phone.InCallScreen.InCallInitStatus;
import com.android.phone.videophone.VideoTelephonyApp;
import com.lge.config.StarConfig;
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->
/**
 * OutgoingCallBroadcaster receives CALL and CALL_PRIVILEGED Intents, and
 * broadcasts the ACTION_NEW_OUTGOING_CALL intent which allows other
 * applications to monitor, redirect, or prevent the outgoing call.

 * After the other applications have had a chance to see the
 * ACTION_NEW_OUTGOING_CALL intent, it finally reaches the
 * {@link OutgoingCallReceiver}, which passes the (possibly modified)
 * intent on to the {@link InCallScreen}.
 *
 * Emergency calls and calls where no number is present (like for a CDMA
 * "empty flash" or a nonexistent voicemail number) are exempt from being
 * broadcast.
 */
public class OutgoingCallBroadcaster extends Activity {
    public static final String LOG_TAG = "OutgoingCallBroadcaster";
   
    private AlertDialog mGenericErrorDialog;
    private InCallInitStatus mInCallInitialStatus; 
    
    private Handler dialogHandler = new Handler(){
        public void handleMessage(Message msg)
        {
            if(mGenericErrorDialog != null)
            {
                mGenericErrorDialog.dismiss();
                mGenericErrorDialog = null;
            }            
        }       
    };
    
	//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] -->
    @Override
	public void finish() {
		// TODO Auto-generated method stub
        if(DBG)
    	Log.w(TAG, "======OutgoingCallBroadcaster finish =============");
		super.finish();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
	    if(DBG)
	 	Log.w(TAG, "======OutgoingCallBroadcaster onDestroy =============");
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
	    if(DBG)
	 	Log.w(TAG, "======OutgoingCallBroadcaster onPause =============");
		super.onPause();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
	    if(DBG)
		Log.w(TAG, "======OutgoingCallBroadcaster onRestart =============");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
	    if(DBG)
		Log.w(TAG, "======OutgoingCallBroadcaster onResume =============");
		super.onResume();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
	    if(DBG)
		Log.w(TAG, "======OutgoingCallBroadcaster onStart =============");
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
	    if(DBG)
		Log.w(TAG, "======OutgoingCallBroadcaster onStop =============");
		super.onStop();
	}
	//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->
    private static final String PERMISSION = android.Manifest.permission.PROCESS_OUTGOING_CALLS;
    private static final String TAG = "OutgoingCallBroadcaster";
    private static final boolean DBG =
            (PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    public static final String EXTRA_ALREADY_CALLED = "android.phone.extra.ALREADY_CALLED";
    public static final String EXTRA_ORIGINAL_URI = "android.phone.extra.ORIGINAL_URI";

    /**
     * Identifier for intent extra for sending an empty Flash message for
     * CDMA networks. This message is used by the network to simulate a
     * press/depress of the "hookswitch" of a landline phone. Aka "empty flash".
     *
     * TODO: Receiving an intent extra to tell the phone to send this flash is a
     * temporary measure. To be replaced with an external ITelephony call in the future.
     * TODO: Keep in sync with the string defined in TwelveKeyDialer.java in Contacts app
     * until this is replaced with the ITelephony API.
     */
    public static final String EXTRA_SEND_EMPTY_FLASH = "com.android.phone.extra.SEND_EMPTY_FLASH";

    /**
     * OutgoingCallReceiver finishes NEW_OUTGOING_CALL broadcasts, starting
     * the InCallScreen if the broadcast has not been canceled, possibly with
     * a modified phone number and optional provider info (uri + package name + remote views.)
     */
    public class OutgoingCallReceiver extends BroadcastReceiver {
        private static final String TAG = "OutgoingCallReceiver";

        public void onReceive(Context context, Intent intent) {
            doReceive(context, intent);
            finish();
        }

        public void doReceive(Context context, Intent intent) {
            if (DBG) Log.v(TAG, "doReceive: " + intent);

            boolean alreadyCalled;
            String number;
            String originalUri;

	// LGE_VT START
            Intent newIntent;
            String action = intent.getAction();
	// LGE_VT END

            alreadyCalled = intent.getBooleanExtra(
                    OutgoingCallBroadcaster.EXTRA_ALREADY_CALLED, false);
            if (alreadyCalled) {
                if (DBG) Log.v(TAG, "CALL already placed -- returning.");
                return;
            }
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] -->
//<!--[minsoo.jin@lge.com] 2010.11.04	  remove flag for auto call test -->
//	if(StarConfig.COUNTRY.equals("KR"))
//	{
			int autoCallTest = getIntent().getIntExtra("Times", 0);
			int connectionTime = getIntent().getIntExtra("CallTime", 0);
           
                       
			if(autoCallTest > 0)
			{ 
				SharedPreferences prefs = getSharedPreferences("prefs", MODE_WORLD_READABLE|MODE_WORLD_WRITEABLE );
				SharedPreferences.Editor edit =   prefs.edit();
				edit.putInt("Times", autoCallTest);
				edit.commit();
				PhoneUtils.setAutoCallTest(true, connectionTime);  // AutoCall Test & 연결 시간 설정.
			}
			else
			{
	
				PhoneUtils.setAutoCallTest(false, 0);
			}
//	}
//	else
//		PhoneUtils.setAutoCallTest(false, 0);
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->

            number = getResultData();
            final PhoneApp app = PhoneApp.getInstance();
            int phoneType = app.phone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                boolean activateState = (app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION);
                boolean dialogState = (app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState
                        .OTA_STATUS_SUCCESS_FAILURE_DLG);
                boolean isOtaCallActive = false;

                if ((app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_PROGRESS)
                        || (app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_LISTENING)) {
                    isOtaCallActive = true;
                }

                if (activateState || dialogState) {
                    if (dialogState) app.dismissOtaDialogs();
                    app.clearOtaState();
                    app.clearInCallScreenMode();
                } else if (isOtaCallActive) {
                    if (DBG) Log.v(TAG, "OTA call is active, a 2nd CALL cancelled -- returning.");
                    return;
                }
            }

            if (number == null) {
                if (DBG) Log.v(TAG, "CALL cancelled (null number), returning...");
                return;
            } else if ((phoneType == Phone.PHONE_TYPE_CDMA)
                    && ((app.phone.getState() != Phone.State.IDLE)
                    && (app.phone.isOtaSpNumber(number)))) {
                if (DBG) Log.v(TAG, "Call is active, a 2nd OTA call cancelled -- returning.");
                return;
            } else if (PhoneNumberUtils.isEmergencyNumber(number)) {
                if(DBG)
                Log.w(TAG, "Cannot modify outgoing call to emergency number " + number + ".");
                return;
            }

            originalUri = intent.getStringExtra(
                    OutgoingCallBroadcaster.EXTRA_ORIGINAL_URI);
            if (originalUri == null) {
                if(DBG)
                Log.e(TAG, "Intent is missing EXTRA_ORIGINAL_URI -- returning.");
                return;
            }

            Uri uri = Uri.parse(originalUri);

            if (DBG) Log.v(TAG, "CALL to " + number + " proceeding.");

            if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            	newIntent = new Intent(Intent.ACTION_CALL, uri);
            }
            else {//if (intent.ACTION_VIDEO_CALL.equals(action)) {
         	PhoneApp.mIsVideoCall = true;
            	newIntent = new Intent(Intent.ACTION_VIDEO_CALL, uri);
            }

            newIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);

            PhoneUtils.checkAndCopyPhoneProviderExtras(intent, newIntent);
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> outgoing mode [START_LGE_LAB]
			if(StarConfig.COUNTRY.equals("KR")){
            	newIntent.putExtra(Intent.EXTRA_OUTGOING_MODE, intent.getIntExtra(Intent.EXTRA_OUTGOING_MODE, Intent.OUTGOING_MODE_DEFAULT));
        	}
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> outgoing mode [END_LGE_LAB]

//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> make PreInCallScreen activity [START_LGE_LAB]
			if(StarConfig.COUNTRY.equals("KR")){
	            newIntent.setClass(context, PreInCallScreen.class);
        	}else{
        		if(PhoneApp.mIsVideoCall)
        			newIntent.setClass(context, InVideoCallScreen.class);
        		else                 
        			newIntent.setClass(context, InCallScreen.class);
        	}
            /**
             * origin
            newIntent.setClass(context, InCallScreen.class);
             */
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> make PreInCallScreen activity [END_LGE_LAB]
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (DBG) Log.v(TAG, "doReceive(): calling startActivity: " + newIntent);
            context.startActivity(newIntent);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent broadcastIntent = null;
        Intent intent = getIntent();
        final Configuration configuration = getResources().getConfiguration();

        if (DBG) Log.v(TAG, "onCreate: this = " + this + ", icicle = " + icicle);
        if (DBG) Log.v(TAG, " - getIntent() = " + intent);
        if (DBG) Log.v(TAG, " - configuration = " + configuration);

        if (icicle != null) {
            // A non-null icicle means that this activity is being
            // re-initialized after previously being shut down.
            //
            // In practice this happens very rarely (because the lifetime
            // of this activity is so short!), but it *can* happen if the
            // framework detects a configuration change at exactly the
            // right moment; see bug 2202413.
            //
            // In this case, do nothing.  Our onCreate() method has already
            // run once (with icicle==null the first time), which means
            // that the NEW_OUTGOING_CALL broadcast for this new call has
            // already been sent.
            if(DBG)
            Log.i(TAG, "onCreate: non-null icicle!  "
                  + "Bailing out, not sending NEW_OUTGOING_CALL broadcast...");

            // No need to finish() here, since the OutgoingCallReceiver from
            // our original instance will do that.  (It'll actually call
            // finish() on our original instance, which apparently works fine
            // even though the ActivityManager has already shut that instance
            // down.  And note that if we *do* call finish() here, that just
            // results in an "ActivityManager: Duplicate finish request"
            // warning when the OutgoingCallReceiver runs.)

            return;
        }

        String action = intent.getAction();
        String number = PhoneNumberUtils.getNumberFromIntent(intent, this);
        if (number != null) {
            number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
            number = PhoneNumberUtils.stripSeparators(number);
        }
        final boolean emergencyNumber =
                (number != null) && PhoneNumberUtils.isEmergencyNumber(number);

        boolean callNow;

        if (getClass().getName().equals(intent.getComponent().getClassName())) {
            // If we were launched directly from the OutgoingCallBroadcaster,
            // not one of its more privileged aliases, then make sure that
            // only the non-privileged actions are allowed.
            if (!Intent.ACTION_CALL.equals(intent.getAction())) {
                if(DBG)
                Log.w(TAG, "Attempt to deliver non-CALL action; forcing to CALL");
                intent.setAction(Intent.ACTION_CALL);
            }
        }

//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> call : roaming : if emergency call, OUTGOING_MODE_KOREA [START_LGE_LAB]
		if(StarConfig.COUNTRY.equals("KR")){
	        if (Intent.ACTION_CALL_EMERGENCY.equals(action)) {
	            // When user call with ACTION_CALL_EMERGENCY on EmergencyEditor/EmergencyList menu,
	            // User can't originate the call. Because popup dialog selecting RAD mode is below to lock menu.
	            // In case, It set Intent EXTRA_OUTGOING_MODE with OUTGOING_MODE_ETC to ignore popup dialog selecting RAD mode. 
	            intent.putExtra(Intent.EXTRA_OUTGOING_MODE, Intent.OUTGOING_MODE_ETC);
	        }
	        //20101105 sumi920.kim@lge.com SKT SKAF SpeakerOn/Off setting [START_LGE_LAB1]
	        if(StarConfig.OPERATOR.equals("SKT"))
	        {
	        	String 		SKAF_SPEAKER_STATE = "state";
	        	String isOn = intent.getStringExtra(SKAF_SPEAKER_STATE);
	        	if(isOn != null)
	        	{
	        		boolean isSpkPhone = false;
	        		String 		SKAF_SPEAKER_ON = "on";
	        		String 		SKAF_SPEAKER_OFF = "off";
	        		if( isOn.equals(SKAF_SPEAKER_ON) ) 
	        			isSpkPhone = true;
	        		else if ( isOn.equals(SKAF_SPEAKER_OFF) )
	        			isSpkPhone = false;
	        		if(DBG){
	        		Log.e(TAG, "isOn :  " + isOn);
	        		Log.e(TAG, "isSpkPhone :  " + isSpkPhone);
	        		}
	        		PhoneUtils.haveToSetSKAFSpeakerMode(true, isSpkPhone);     		
	        		
	        	}

	        }
	        //20101105 sumi920.kim@lge.com SKT SKAF SpeakerOn/Off setting [END_LGE_LAB1]

		}
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> call : roaming : if emergency call, OUTGOING_MODE_KOREA [END_LGE_LAB]

        /* Change CALL_PRIVILEGED into CALL or CALL_EMERGENCY as needed. */
        // TODO: This code is redundant with some code in InCallScreen: refactor.
        if (Intent.ACTION_CALL_PRIVILEGED.equals(action)) {
            action = emergencyNumber
                    ? Intent.ACTION_CALL_EMERGENCY
                    : Intent.ACTION_CALL;
            intent.setAction(action);
        }
        else if (Intent.ACTION_VIDEO_CALL_PRIVILEGED.equals(action)) 
        {
            if(emergencyNumber)
            {
                action = Intent.ACTION_CALL_EMERGENCY;          
            }
            else
            {
             // LGE_VT_FIXED START- 2010/11/09
             // check a network service state and duplicated call such a in-call and etc.
                         mInCallInitialStatus = checkIfOkToInitiateOutgoingCall(true);
                         mInCallInitialStatus = checkIfOkToDialNumber(number,mInCallInitialStatus);
                         
                         if (mInCallInitialStatus != InCallInitStatus.SUCCESS) 
                         {
                             if(DBG)
                             Log.d(LOG_TAG,"mInCallInitialStatus is not success. so finish() ^^");                
                             handleStartupError(mInCallInitialStatus);
                             return;   
                         }
                        action =  Intent.ACTION_VIDEO_CALL;                                            
             // LGE_VT_FIXED END- 2010/11/09                
            }
            intent.setAction(action);      
        }

        if (Intent.ACTION_CALL.equals(action)) {
            if (emergencyNumber) {
                if(DBG)
                Log.w(TAG, "Cannot call emergency number " + number
                        + " with CALL Intent " + intent + ".");

                Intent invokeFrameworkDialer = new Intent();

                // TwelveKeyDialer is in a tab so we really want
                // DialtactsActivity.  Build the intent 'manually' to
                // use the java resolver to find the dialer class (as
                // opposed to a Context which look up known android
                // packages only)
                invokeFrameworkDialer.setClassName("com.android.contacts",
                                                   "com.android.contacts.DialtactsActivity");
                invokeFrameworkDialer.setAction(Intent.ACTION_DIAL);
                invokeFrameworkDialer.setData(intent.getData());

                if (DBG) Log.v(TAG, "onCreate(): calling startActivity for Dialer: "
                               + invokeFrameworkDialer);
                startActivity(invokeFrameworkDialer);
                finish();
                return;
            }
            callNow = false;
		} else if (Intent.ACTION_VIDEO_CALL.equals(action)) {//VT_AHJ
            if (emergencyNumber) {
                if(DBG)
                Log.w(TAG, "Cannot call emergency number " + number
                        + " with CALL Intent " + intent + ".");
                finish();
                return;
            }
            callNow = false;
        } else if (Intent.ACTION_CALL_EMERGENCY.equals(action)) {
            // ACTION_CALL_EMERGENCY case: this is either a CALL_PRIVILEGED
            // intent that we just turned into a CALL_EMERGENCY intent (see
            // above), or else it really is an CALL_EMERGENCY intent that
            // came directly from some other app (e.g. the EmergencyDialer
            // activity built in to the Phone app.)
            if (!emergencyNumber) {
                if(DBG)
                Log.w(TAG, "Cannot call non-emergency number " + number
                        + " with EMERGENCY_CALL Intent " + intent + ".");
                finish();
                return;
            }
            callNow = true;
        } else {
            if(DBG)
            Log.e(TAG, "Unhandled Intent " + intent + ".");
            finish();
            return;
        }

        // Make sure the screen is turned on.  This is probably the right
        // thing to do, and more importantly it works around an issue in the
        // activity manager where we will not launch activities consistently
        // when the screen is off (since it is trying to keep them paused
        // and has...  issues).
        //
        // Also, this ensures the device stays awake while doing the following
        // broadcast; technically we should be holding a wake lock here
        // as well.
        if(StarConfig.COUNTRY.equals("KR")) //20101120 sumi920.kim@lge.com change function call after preincall screen[LGE_LAB1]
        {        	
        }
        else
        {
	        final PhoneApp app = PhoneApp.getInstance();//hyojin.an 101026
	        app.setBeginningCall(true); //hyojin.an 101026
        }
        PhoneApp.getInstance().updateWakeState(true); //hyojin.an 101026
        PhoneApp.getInstance().wakeUpScreen();

        /* If number is null, we're probably trying to call a non-existent voicemail number,
         * send an empty flash or something else is fishy.  Whatever the problem, there's no
         * number, so there's no point in allowing apps to modify the number. */
        if (number == null || TextUtils.isEmpty(number)) {
            if (intent.getBooleanExtra(EXTRA_SEND_EMPTY_FLASH, false)) {
                if(DBG)
                Log.i(TAG, "onCreate: SEND_EMPTY_FLASH...");
                PhoneUtils.sendEmptyFlash(PhoneApp.getInstance().phone);
                finish();
                return;
            } else {
                if(DBG)
                Log.i(TAG, "onCreate: null or empty number, setting callNow=true...");
                callNow = true;
            }
        }

        if (callNow) {
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> make PreInCallScreen activity [START_LGE_LAB]
			if(StarConfig.COUNTRY.equals("KR")){
	            intent.setClass(this, PreInCallScreen.class);
			}else{
				intent.setClass(this, InCallScreen.class);
			}
            /**
             * origin
            intent.setClass(this, InCallScreen.class);
             */
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> make PreInCallScreen activity [END_LGE_LAB]
            if (DBG) Log.v(TAG, "onCreate(): callNow case, calling startActivity: " + intent);
            startActivity(intent);
        }

        if (Intent.ACTION_CALL.equals(action)) {
        	broadcastIntent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);
        }
        else {
        	 broadcastIntent = new Intent(Intent.ACTION_NEW_OUTGOING_VIDEO_CALL);
        }

        if (number != null) {
            broadcastIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
        }
        PhoneUtils.checkAndCopyPhoneProviderExtras(intent, broadcastIntent);
        broadcastIntent.putExtra(EXTRA_ALREADY_CALLED, callNow);
        broadcastIntent.putExtra(EXTRA_ORIGINAL_URI, intent.getData().toString());

//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> outgoing mode [START_LGE_LAB]
		if(StarConfig.COUNTRY.equals("KR")){
       		broadcastIntent.putExtra(Intent.EXTRA_OUTGOING_MODE, intent.getIntExtra(Intent.EXTRA_OUTGOING_MODE, Intent.OUTGOING_MODE_DEFAULT));
		}
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> outgoing mode [END_LGE_LAB]

        if (DBG) Log.v(TAG, "Broadcasting intent " + broadcastIntent + ".");
        sendOrderedBroadcast(broadcastIntent, PERMISSION,
                new OutgoingCallReceiver(), null, Activity.RESULT_OK, number, null);
        // The receiver will finish our activity when it finally runs.
    }

    // Implement onConfigurationChanged() purely for debugging purposes,
    // to make sure that the android:configChanges element in our manifest
    // is working properly.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DBG) Log.v(TAG, "onConfigurationChanged: newConfig = " + newConfig);
    }
    
    /**
     * Checks the current ServiceState to make sure it's OK to try making an
     * outgoing call to the specified number.
     * 
     * @return InCallInitStatus.SUCCESS if it's OK to try calling the specified
     *         number. If not, like if the radio is powered off or we have no
     *         signal, return one of the other InCallInitStatus codes indicating
     *         what the problem is.
     * @since 2010/11/9 - Move code to here from InVideoCallScreen.java  
     */
    private InCallInitStatus checkIfOkToInitiateOutgoingCall(boolean isVideoCall) {
        // Watch out: do NOT use PhoneStateIntentReceiver.getServiceState()
        // here;
        // that's not guaranteed to be fresh. To synchronously get the
        // CURRENT service state, ask the Phone object directly:
        PhoneApp app = PhoneApp.getInstance();
        
        int state = app.phone.getServiceState().getState();
        if (DBG)
            Log.d(LOG_TAG,"checkIfOkToInitiateOutgoingCall: ServiceState = " + state);

        //only check in VT call
        ///////////////////////////////////////////////////////////////////
        if(isVideoCall)
        {
            // VideoTelephonyApp.getInstance() can be NULL. 
            // so , use Settings.System.VT_LOOPBACK

            // @note - But, in the test mode.
            // we setting VT-loopback true, It'is not working well in the same scenario.
            // just we try to call in VT-loopback mode.
            // Actually, we don't set a VT-loopback. so it has nothing to do with.
            if(VideoTelephonyApp.getInstance() == null)
            {
                if ( Settings.System.getInt(getContentResolver(),
                        Settings.System.VT_LOOPBACK, 0) != 0 )
                {
                    return InCallInitStatus.SUCCESS;
                }
            }
            else
            {
                if (true == VideoTelephonyApp.getInstance().getLoopbackCall()) {
                    return InCallInitStatus.SUCCESS;
                }                
            }
            
            
            if (PhoneApp.getInstance().isLowBattery()) {
                return InCallInitStatus.LOW_BATTERY;
            }
            
            if( true == PhoneApp.getInstance().isPhoneInUse())
            {
                return InCallInitStatus.INCALL_USED;
            }            
            int radiotech = app.phone.getServiceState().getRadioTechnology(); //hyojin.an 101118
            int phoneType = app.phone.getPhoneType();
            Log.d(LOG_TAG,"[isVideoCall]checkIfOkToInitiateOutgoingCall: radiotech = " + radiotech);
            if((radiotech == ServiceState.RADIO_TECHNOLOGY_UNKNOWN 
            		|| radiotech == ServiceState.RADIO_TECHNOLOGY_GPRS
            		|| radiotech == ServiceState.RADIO_TECHNOLOGY_EDGE)
            		&& (phoneType == Phone.PHONE_TYPE_GSM)) //hyojin.an 101118
            	return InCallInitStatus.OUT_OF_SERVICE;
        }

        switch (state) {
        case ServiceState.STATE_IN_SERVICE:
            // Normal operation. It's OK to make outgoing calls.
            return InCallInitStatus.SUCCESS;

        case ServiceState.STATE_POWER_OFF:
            // Radio is explictly powered off.
            return InCallInitStatus.POWER_OFF;

        case ServiceState.STATE_EMERGENCY_ONLY:
            // LGE_NETWORK_STATUS START
        case ServiceState.STATE_SERVICE_DETACHED:
            // LGE_NETWORK_STATUS END
            // The phone is registered, but locked. Only emergency
            // numbers are allowed.
            // Note that as of Android 2.0 at least, the telephony layer
            // does not actually use ServiceState.STATE_EMERGENCY_ONLY,
            // mainly since there's no guarantee that the radio/RIL can
            // make this distinction. So in practice the
            // InCallInitStatus.EMERGENCY_ONLY state and the string
            // "incall_error_emergency_only" are totally unused.
            return InCallInitStatus.EMERGENCY_ONLY;
        case ServiceState.STATE_OUT_OF_SERVICE:
            // No network connection.
            return InCallInitStatus.OUT_OF_SERVICE;

        default:
            throw new IllegalStateException("Unexpected ServiceState: " + state);
        }
    }
    

    /**
     * Brings up UI to handle the various error conditions that can occur when
     * first initializing the in-call UI. This is called from onResume() if we
     * encountered an error while processing our initial Intent.
     * 
     * @param status
     *            one of the InCallInitStatus error codes.
     */
    private void handleStartupError(InCallInitStatus status) {
        if (DBG)
            Log.d(LOG_TAG, "handleStartupError(): status = " + status);

        // NOTE that the regular Phone UI is in an uninitialized state at
        // this point, so we don't ever want the user to see it.
        // That means:
        // - Any cases here that need to go to some other activity should
        // call startActivity() AND immediately call endInCallScreenSession
        // on this one.
        // - Any cases here that bring up a Dialog must ensure that the
        // Dialog handles both OK *and* cancel by calling
        // endInCallScreenSession.
        // Activity. (See showGenericErrorDialog() for an example.)

        switch (status) {
            case POWER_OFF:
                // Radio is explictly powered off.

                // TODO: This UI is ultra-simple for 1.0. It would be nicer
                // to bring up a Dialog instead with the option "turn on radio
                // now". If selected, we'd turn the radio on, wait for
                // network registration to complete, and then make the call.

                showGenericErrorDialog(R.string.incall_error_power_off);
                break;

            case EMERGENCY_ONLY:
                // Only emergency numbers are allowed, but we tried to dial
                // a non-emergency number.
                showGenericErrorDialog(R.string.incall_error_emergency_only);
                break;

            case OUT_OF_SERVICE:
                // No network connection.
                showGenericErrorDialog(R.string.incall_error_out_of_service);
                break;

            case PHONE_NOT_IN_USE:
                // This error is handled directly in onResume() (by bailing
                // out of the activity.) We should never see it here.
                if(DBG)
                Log.w(LOG_TAG, "handleStartupError: unexpected PHONE_NOT_IN_USE status");
                break;

            case NO_PHONE_NUMBER_SUPPLIED:
                // The supplied Intent didn't contain a valid phone number.
                // TODO: Need UI spec for this failure case; for now just
                // show a generic error.
                showGenericErrorDialog(R.string.incall_error_no_phone_number_supplied);
                break;

            case DIALED_MMI:
                // Our initial phone number was actually an MMI sequence.
                // There's no real "error" here, but we do bring up the
                // a Toast (as requested of the New UI paradigm).
                //
                // In-call MMIs do not trigger the normal MMI Initiate
                // Notifications, so we should notify the user here.
                // Otherwise, the code in PhoneUtils.java should handle
                // user notifications in the form of Toasts or Dialogs.
                if (PhoneApp.getInstance().phone.getState() == Phone.State.OFFHOOK) {
                    Toast.makeText(this, R.string.incall_status_dialed_mmi, Toast.LENGTH_SHORT)
                            .show();
                }
                break;

            case CALL_FAILED:
                // We couldn't successfully place the call; there was some
                // failure in the telephony layer.
                // TODO: Need UI spec for this failure case; for now just
                // show a generic error.
                showGenericErrorDialog(R.string.incall_error_call_failed);
                break;

            // LGE_VT START
            case INVALID_NUMBER:
                showGenericErrorDialog(R.string.vt_invalid_number);
                break;
            case LOW_BATTERY:
                showGenericErrorDialog(R.string.vt_lowbattery);
                break;
            case INCALL_USED:
                {
                    if(PhoneApp.getInstance().mIsVideoCall == true)
                    {
                        showGenericErrorDialog(R.string.vt_not_connect_invideocall);
                    }
                    else
                    {
                        showGenericErrorDialog(R.string.vt_not_connect_incall);
                    }
                }
                break;                
            // -- LGE_VT END

            default:
                if(DBG)
                Log.w(LOG_TAG, "handleStartupError: unexpected status code " + status);
                showGenericErrorDialog(R.string.incall_error_call_failed);
                break;
        }
    }

    /**
     * Utility function to bring up a generic "error" dialog, and then bail out
     * of the in-call UI when the user hits OK (or the BACK button.)
     */
    private void showGenericErrorDialog(int resid) {
        mGenericErrorDialog = new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_info)
        //.setTitle(" ")
        .setMessage(resid)
        .create();
        
        mGenericErrorDialog.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                if(DBG)
                Log.d(LOG_TAG,"showGenericErrorDialog is hide!");
                finish();
            }
        });
        
        // When the dialog is up, completely hide the in-call UI
        // underneath (which is in a partially-constructed state).
        mGenericErrorDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mGenericErrorDialog.show();
        
        //dialog is down after 2500 mil-sec.
        dialogHandler.sendEmptyMessageDelayed(0, 2500);        
    }
    
    /**
     * Checks the current dialing number is correct 
     * 
     * @return InCallInitStatus.SUCCESS if it's OK to try calling the specified
     *         number. If not, like if dialing string is wrong number.
     */
    private InCallInitStatus checkIfOkToDialNumber(String number, InCallInitStatus status) 
    {
        if (number.compareToIgnoreCase("*") == 0
            || number.compareToIgnoreCase("#") == 0
            )
        {
            return InCallInitStatus.INVALID_NUMBER;
        }
        
        
        return status;
        
//        else
//        {
            // TODO: Add following conditions if needs --> Be careful if you add conditions
            //  Check the dial number contains only numberic and #,  * , -, +
            //   
            //-- end        
           
//            return InCallInitStatus.SUCCESS;
//        }
    }

}
