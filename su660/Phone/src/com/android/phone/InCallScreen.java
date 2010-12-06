/*
 * Copyright (C) 2006 The Android Open Source Project

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

// LGE_CALL_DEFLECTION START
import com.android.internal.telephony.CallStateException;
// LGE_CALL_DEFLECTION END
// LGE_SS_NOTIFY START
import com.android.internal.telephony.Connection.DisconnectCause;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import android.view.Gravity;
// LGE_SS_NOTIFY END
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.text.method.DialerKeyListener;
import android.util.EventLog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.phone.OtaUtils.CdmaOtaInCallScreenUiState;
import com.android.phone.OtaUtils.CdmaOtaScreenState;
// LGE_CALL_COSTS START
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneFactory;
import android.media.ToneGenerator;
// LGE_CALL_COSTS END
import java.util.List;
//20100804 jongwany.lee@lge.com attached it for INCALLCONTROLS IN EMERGENCY
//20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE

// LGE_MERGE_S
// [2010. 03. 18] reduck@lge.com
import android.os.PowerManager;
// [2010. 03. 18] reduck@lge.com
// LGE_MERGE_E

//20100802 jongwany.lee@lge.com	send SMS in Silent incoming [START_LGE]
import com.android.internal.telephony.CallerInfo;
//20100802 jongwany.lee@lge.com	send SMS in Silent incoming [END_LGE]
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] -->
import android.content.SharedPreferences;
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->

//<!--[sumi920.kim@lge.com] 2010.09.06	LAB1_CallUI ==> InCall Option Menu( contact /memo/sms ) [START] -->
import android.view.MenuItem;
import android.content.ComponentName;
import android.view.MenuInflater;
import com.lge.config.StarConfig;
//<!--[sumi920.kim@lge.com] 2010.09.06	LAB1_CallUI ==> InCall Option Menu( contact /memo/sms ) [END] -->

import com.android.internal.telephony.RAD.RoamingPrefixAppender;
import com.android.internal.telephony.RAD.RoamingPrefixAppenderFactory;

//jundj@mo2.co.kr start
import com.mtelo.visualexpression.VEUtils;
import com.mtelo.visualexpression.VE_ContentManager;
import com.mtelo.visualexpression.VEUtils.IsVEPlayerRunning;
//jundj@mo2.co.kr end

/**
 * Phone app "in call" screen.
 */
public class InCallScreen extends AbstractInCallScreen
        implements View.OnClickListener, View.OnTouchListener {
    private static final String LOG_TAG = "InCallScreen";

    private static final boolean DBG = true;
            //(PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = true; //(PhoneApp.DBG_LEVEL >= 2);
//20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
    InCallMessageView mInCallMsg;
    
    /**
     * Intent extra used to specify whether the DTMF dialpad should be
     * initially visible when bringing up the InCallScreen.  (If this
     * extra is present, the dialpad will be initially shown if the extra
     * has the boolean value true, and initially hidden otherwise.)
     */
    // TODO: Should be EXTRA_SHOW_DIALPAD for consistency.
    static final String SHOW_DIALPAD_EXTRA = "com.android.phone.ShowDialpad";
    
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
    //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
    public static final String ACTION_DISPLAY_CALLEND = "com.android.phone.InCallScreen.DISPLAY_CALLEND";
    //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on

    /**
     * Intent extra to specify the package name of the gateway
     * provider.  Used to get the name displayed in the in-call screen
     * during the call setup. The value is a string.
     */
    // TODO: This extra is currently set by the gateway application as
    // a temporary measure. Ultimately, the framework will securely
    // set it.
    /* package */ static final String EXTRA_GATEWAY_PROVIDER_PACKAGE =
            "com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE";

    /**
     * Intent extra to specify the URI of the provider to place the
     * call. The value is a string. It holds the gateway address
     * (phone gateway URL should start with the 'tel:' scheme) that
     * will actually be contacted to call the number passed in the
     * intent URL or in the EXTRA_PHONE_NUMBER extra.
     */
    // TODO: Should the value be a Uri (Parcelable)? Need to make sure
    // MMI code '#' don't get confused as URI fragments.
    /* package */ static final String EXTRA_GATEWAY_URI =
            "com.android.phone.extra.GATEWAY_URI";

    // Event values used with Checkin.Events.Tag.PHONE_UI events:
    /** The in-call UI became active */
    static final String PHONE_UI_EVENT_ENTER = "enter";
    /** User exited the in-call UI */
    static final String PHONE_UI_EVENT_EXIT = "exit";
    /** User clicked one of the touchable in-call buttons */
    static final String PHONE_UI_EVENT_BUTTON_CLICK = "button_click";
    
    // END_CALL Menu sendSMS
    private static final String SCHEME_SMSTO = "smsto";

    // Amount of time (in msec) that we display the "Call ended" state.
    // The "short" value is for calls ended by the local user, and the
    // "long" value is for calls ended by the remote caller.
    private static final int CALL_ENDED_SHORT_DELAY =  200;  // msec
    // sumi920.kim@lge.com 2010.10.07  LAB1_CallUI --> Change  CALL_ENDED_LONG_DELAY time 2000msec to 3000sec
    private static final int CALL_ENDED_LONG_DELAY = 3000;  // msec 

    // Amount of time (in msec) that we keep the in-call menu onscreen
    // *after* the user changes the state of one of the toggle buttons.
    private static final int MENU_DISMISS_DELAY =  1000;  // msec

    // Amount of time that we display the PAUSE alert Dialog showing the
    // post dial string yet to be send out to the n/w
    private static final int PAUSE_PROMPT_DIALOG_TIMEOUT = 2000;  //msec

    // The "touch lock" overlay timeout comes from Gservices; this is the default.
    private static final int TOUCH_LOCK_DELAY_DEFAULT =  6000;  // msec

    // Amount of time for Displaying "Dialing" for 3way Calling origination
    private static final int THREEWAY_CALLERINFO_DISPLAY_TIME = 3000; // msec

    // Amount of time that we display the provider's overlay if applicable.
    private static final int PROVIDER_OVERLAY_TIMEOUT = 5000;  // msec

    // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
    private static final int CALL_RELATED_BUTTON_LOCK_TIMEOUT = 1000;
    // LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
    // These are values for the settings of the auto retry mode:
    // 0 = disabled
    // 1 = enabled
    // TODO (Moto):These constants don't really belong here,
    // they should be moved to Settings where the value is being looked up in the first place
    static final int AUTO_RETRY_OFF = 0;
    static final int AUTO_RETRY_ON = 1;

    // Message codes; see mHandler below.
    // Note message codes < 100 are reserved for the PhoneApp.
    private static final int PHONE_STATE_CHANGED = 101;
    private static final int PHONE_DISCONNECT = 102;
    private static final int EVENT_HEADSET_PLUG_STATE_CHANGED = 103;
    private static final int POST_ON_DIAL_CHARS = 104;
    private static final int WILD_PROMPT_CHAR_ENTERED = 105;
    private static final int ADD_VOICEMAIL_NUMBER = 106;
    private static final int DONT_ADD_VOICEMAIL_NUMBER = 107;
    private static final int DELAYED_CLEANUP_AFTER_DISCONNECT = 108;
    private static final int SUPP_SERVICE_FAILED = 110;
    private static final int DISMISS_MENU = 111;
    private static final int ALLOW_SCREEN_ON = 112;
    private static final int TOUCH_LOCK_TIMER = 113;
    private static final int REQUEST_UPDATE_BLUETOOTH_INDICATION = 114;
    private static final int PHONE_CDMA_CALL_WAITING = 115;
    private static final int THREEWAY_CALLERINFO_DISPLAY_DONE = 116;
    private static final int EVENT_OTA_PROVISION_CHANGE = 117;
    private static final int REQUEST_CLOSE_SPC_ERROR_NOTICE = 118;
    private static final int REQUEST_CLOSE_OTA_FAILURE_NOTICE = 119;
    private static final int EVENT_PAUSE_DIALOG_COMPLETE = 120;
    private static final int EVENT_HIDE_PROVIDER_OVERLAY = 121;  // Time to remove the overlay.
    private static final int REQUEST_UPDATE_TOUCH_UI = 122;
// LGE_SS_NOTIFY START
    private static final int SUPP_SERVICE_NOTIFY = 123;
// LGE_SS_NOTIFY END
// LGE_CALL_COSTS START
    private static final int AOC_QUERY_ICC_ACM_MAX = 124;
    private static final int AOC_QUERY_ICC_ACM = 125;
// LGE_CALL_COSTS END
  // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
    private static final int CALL_RELATED_BUTTON_LOCK = 126;
  // LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] -->
   private static Connection.DisconnectCause autoCallCause;
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->
    //START sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv
    // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
    private static final int ANSWER_RINGINGCALL_IN_THREEWAYCALL = 127;
    // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
    //END sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv
//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [START_LGE_LAB]
    private static final int REQUEST_UPDATE_CALLCARD_UI = 128;
//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [END_LGE_LAB]  


// 20101019 sumi920.kim@lge.com for Home Key In Alerting [START_LGE_LAB1]
    private static final int HOME_KEYUP_IGNORE_RINGING_STATE = 130;



    //following constants are used for OTA Call
    public static final String ACTION_SHOW_ACTIVATION =
           "com.android.phone.InCallScreen.SHOW_ACTIVATION";
    public static final String OTA_NUMBER = "*228";
    public static final String EXTRA_OTA_CALL = "android.phone.extra.OTA_CALL";

    // When InCallScreenMode is UNDEFINED set the default action
    // to ACTION_UNDEFINED so if we are resumed the activity will
    // know its undefined. In particular checkIsOtaCall will return
    // false.
    public static final String ACTION_UNDEFINED = "com.android.phone.InCallScreen.UNDEFINED";

    //START sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv
    // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
    private static final int ANSWER_RINGINGCALL_IN_THREEWAYCALL_TIMEDELAY =  1000; // msec
    // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
    //END sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv
    
    // 20101019 sumi920.kim@lge.com for Home Key In Alerting [START_LGE_LAB1]
    private static final int HOME_KEYUP_RINGINGSTATE_TIMEDELAY =  500; //500msec
	
    //20101107 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
    // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/12 turn off the screen when a call is made with earjack/BT
    private Call.State mPreviousCallState;
    private Phone.State mPreviousPhoneState;
    // LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/12 turn off the screen when a call is made with earjack/BT
    //20101107 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]
    
    // High-level "modes" of the in-call UI.
    protected enum InCallScreenMode {
        /**
         * Normal in-call UI elements visible.
         */
        NORMAL,
        /**
         * "Manage conference" UI is visible, totally replacing the
         * normal in-call UI.
         */
        MANAGE_CONFERENCE,
        /**
         * Non-interactive UI state.  Call card is visible,
         * displaying information about the call that just ended.
         */
        CALL_ENDED,
         /**
         * Normal OTA in-call UI elements visible.
         */
        OTA_NORMAL,
        /**
         * OTA call ended UI visible, replacing normal OTA in-call UI.
         */
        OTA_ENDED,
        //hyojin.an@lge.com 101001 
        DIALING, 
        /**
         * Default state when not on call
         */
        UNDEFINED
    }
    private InCallScreenMode mInCallScreenMode = InCallScreenMode.UNDEFINED;

    // Possible error conditions that can happen on startup.
    // These are returned as status codes from the various helper
    // functions we call from onCreate() and/or onResume().
    // See syncWithPhoneState() and checkIfOkToInitiateOutgoingCall() for details.
    public enum InCallInitStatus {
        SUCCESS,
        VOICEMAIL_NUMBER_MISSING,
        POWER_OFF,
        EMERGENCY_ONLY,
        OUT_OF_SERVICE,
        PHONE_NOT_IN_USE,
        NO_PHONE_NUMBER_SUPPLIED,
        DIALED_MMI,
        CALL_FAILED,
    // LGE_VT START
        INVALID_NUMBER,        
        LOW_BATTERY,    
        INCALL_USED,
        INVIDEOCALL_USED        
    //--- LGE_VT END
    }

//20101012 wonho.moon@lge.com <mailto:wonho.moon@lge.com> handling Hold/UnHold state, if hold/unhold pressed continuous [START_LGE_LAB]
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-18 : handling Hold/UnHold state, if hold/unhold pressed continuous
    private enum InCallHoldState {
        CALL_STATE_NA,
        CALL_STATE_HOLD,
        CALL_STATE_UNHOLD
    }
    private InCallHoldState mHoldState = InCallHoldState.CALL_STATE_NA;
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-18 : handling Hold/UnHold state, if hold/unhold pressed continuous
//20101012 wonho.moon@lge.com <mailto:wonho.moon@lge.com> handling Hold/UnHold state, if hold/unhold pressed continuous [END_LGE_LAB]


    private InCallInitStatus mInCallInitialStatus;  // see onResume()

    private boolean mRegisteredForPhoneStates;
    private boolean mNeedShowCallLostDialog;

    private Phone mPhone;
    private Call mForegroundCall;
    private Call mBackgroundCall;
    private Call mRingingCall;

// LGE_CALL_COSTS START
    private CommandsInterface mCmdIf = PhoneFactory.getCommandsInterface();
    private static int acm = 0;
    private static int acmMax = 0;
    private boolean mCallCostIsEmergency;
// LGE_CALL_COSTS END

    private BluetoothHandsfree mBluetoothHandsfree;
    private BluetoothHeadset mBluetoothHeadset;
    private boolean mBluetoothConnectionPending;
    private long mBluetoothConnectionRequestTime;
//<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]	 -->
    private CallEndReceiver CallEndReceiver;
    private soundRecord		mSoundRecord;
//<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]	 -->	    
    // Main in-call UI ViewGroups
    private ViewGroup mMainFrame;
    private ViewGroup mInCallPanel;

    // Main in-call UI elements:
    //jundj@mo2.co.kr start
    public CallCard mCallCard;
    //jundj@mo2.co.kr end
// LGE_SS_NOTIFY START
    private SuppServiceNotification ss_notify = null;
// LGE_SS_NOTIFY END
// LGE_AUTO_REDIAL START
    private String dialNumber = null;
    private int repeat = 1;
    private static final int[] TIME_BETWEEN = {5000,60000,60000,60000,180000,180000,180000,180000,180000,180000};
    private static BlackList blacklist = new BlackList();
  
    private boolean disable_log = false;
// LGE CR_2024 START
    private static final int MAX_ATTEMPTS = 11;
// LGE CR_2024 END
// LGE_AUTO_REDIAL END
  // UI controls:
    private InCallControlState mInCallControlState;
    private InCallMenu mInCallMenu;  // used on some devices
    private InCallTouchUi mInCallTouchUi;  // used on some devices
    private ManageConferenceUtils mManageConferenceUtils;

    // DTMF Dialer controller and its view:

    private DTMFTwelveKeyDialer mDialer;
    private DTMFTwelveKeyDialerView mDialerView;

    // TODO: Move these providers related fields in their own class.
    // Optional overlay when a 3rd party provider is used.
    private boolean mProviderOverlayVisible = false;
    private CharSequence mProviderLabel;
    private Drawable mProviderIcon;
    private Uri mProviderGatewayUri;
    // The formated address extracted from mProviderGatewayUri. User visible.
    private String mProviderAddress;

    // For OTA Call
    public OtaUtils otaUtils;

    private EditText mWildPromptText;

    // "Touch lock overlay" feature
    private boolean mUseTouchLockOverlay;  // True if we use this feature on the current device
    private View mTouchLockOverlay;  // The overlay over the whole screen
    private View mTouchLockIcon;  // The "lock" icon in the middle of the screen
    private Animation mTouchLockFadeIn;
    private long mTouchLockLastTouchTime;  // in SystemClock.uptimeMillis() time base

    // Various dialogs we bring up (see dismissAllDialogs()).
    // TODO: convert these all to use the "managed dialogs" framework.
    //
    // The MMI started dialog can actually be one of 2 items:
    //   1. An alert dialog if the MMI code is a normal MMI
    //   2. A progress dialog if the user requested a USSD
    private Dialog mMmiStartedDialog;
    private AlertDialog mMissingVoicemailDialog;
    private AlertDialog mGenericErrorDialog;
    private AlertDialog mSuppServiceFailureDialog;
    private AlertDialog mWaitPromptDialog;
    private AlertDialog mWildPromptDialog;
    private AlertDialog mCallLostDialog;
    private AlertDialog mPausePromptDialog;

  //START sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv
  // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
    private AlertDialog mSelectCallEndDialog;
  // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
  //END sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv

    // NOTE: if you add a new dialog here, be sure to add it to dismissAllDialogs() also.

    // TODO: If the Activity class ever provides an easy way to get the
    // current "activity lifecycle" state, we can remove these flags.
    private boolean mIsDestroyed = false;
    private boolean mIsForegroundActivity = false;

    // For use with CDMA Pause/Wait dialogs
    private String mPostDialStrAfterPause;
    private boolean mPauseInProgress = false;

    // Flag indicating whether or not we should bring up the Call Log when
    // exiting the in-call UI due to the Phone becoming idle.  (This is
    // true if the most recently disconnected Call was initiated by the
    // user, or false if it was an incoming call.)
    // This flag is used by delayedCleanupAfterDisconnect(), and is set by
    // onDisconnect() (which is the only place that either posts a
    // DELAYED_CLEANUP_AFTER_DISCONNECT event *or* calls
    // delayedCleanupAfterDisconnect() directly.)
    private boolean mShowCallLogAfterDisconnect;

// LGE_MERGE_S


    private static Phone.State oldstate = Phone.State.IDLE; //hyojin.an 101020
    private static Call.State oldfgCallState = Call.State.IDLE; //hyojin.an 101020
    private static Call.State oldbgCallState = Call.State.IDLE; //hyojin.an 101020
    
    // [2010. 03. 30] reduck@lge.com
    private boolean mCallPressed = false;
    // [2010. 03. 30] reduck@lge.com
    private boolean mIsOptionCreated = false; //<!--[sumi920.kim@lge.com] 2010.09.06	LAB1_CallUI ==> InCall Option Menu

    // 20101019 sumi920.kim@lge.com for Home Key In Alerting [START_LGE_LAB1]
    private boolean mIgnoreHomeKey = false;
	
// LGE_MERGE_E
    // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
    private boolean mCallRelatedButtonLocked = false;
    // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
//    private Intent mOldIntent = null;
    
    //20101025 sumi920.kim [START_LGE_LAB1] 
    // LCD off -> call end -> delayedCleanupAfterDisconnect()-> onresume ()-> ui error 
    private boolean mdelayedCleanupAfterResume = false;
    private DisconnectCause delayedData;
    //20101025 sumi920.kim [END_LGE_LAB1]

    //20101107 sumi920.kim@lge.com  Control state swiching(Hold/Active)[LGE_LAB1]
    private boolean bSendingSuppService = false;
    
	private static final int REQUEST_CODE_CONFIRM_PASSWROD = 0;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mIsDestroyed) {
                if (DBG) log("Handler: ignoring message " + msg + "; we're destroyed!");
                return;
            }
            if (!mIsForegroundActivity) {
                if (DBG) log("Handler: handling message " + msg + " while not in foreground");
                // Continue anyway; some of the messages below *want* to
                // be handled even if we're not the foreground activity
                // (like DELAYED_CLEANUP_AFTER_DISCONNECT), and they all
                // should at least be safe to handle if we're not in the
                // foreground...
            }

            log("[InCallScreen] Handler: msg.what : " + msg.what);
			
            PhoneApp app = PhoneApp.getInstance();
            switch (msg.what) {
                case SUPP_SERVICE_FAILED:
                    onSuppServiceFailed((AsyncResult) msg.obj);
                    break;

                case PHONE_STATE_CHANGED:
                    onPhoneStateChanged((AsyncResult) msg.obj);
                    break;

                case PHONE_DISCONNECT:
                    onDisconnect((AsyncResult) msg.obj);
                    break;

                case EVENT_HEADSET_PLUG_STATE_CHANGED:
                    // Update the in-call UI, since some UI elements (in
                    // particular the "Speaker" menu button) change state
                    // depending on whether a headset is plugged in.
                    // TODO: A full updateScreen() is overkill here, since
                    // the value of PhoneApp.isHeadsetPlugged() only affects a
                    // single menu item.  (But even a full updateScreen()
                    // is still pretty cheap, so let's keep this simple
                    // for now.)
                    if (!isBluetoothAudioConnected()) {
                        if (msg.arg1 == 1) {
                            //jongik2.kim 20100519 CP_CALL_PATH [start]
                            PhoneUtils.changeModemPath(InCallScreen.this, 2); 
							if(PhoneUtils.isSpeakerOn(InCallScreen.this)){
			                    PhoneUtils.turnOnSpeaker(InCallScreen.this, false, false);	//20101108 sh80.choi@lge.com No Store when Headset On(true->false) [LGE_LAB1]
							}
                            //jongik2.kim 20100519 CP_CALL_PATH [end]
                            // If the dialpad is open, we need to start the timer that will
                            // eventually bring up the "touch lock" overlay.
                            if (mDialer.isOpened() && !isTouchLocked()) {
                                resetTouchLockTimer();
                            }
                        	//2001120 sumi920.kim@lge.com Disable Speaker in headset plug [LGE_LAB1]
                        	mInCallTouchUi.disableSpeakerphoneButton();
                        }
                        //jongik2.kim 20100519 CP_CALL_PATH [start]
                        else{
							PhoneUtils.restoreSpeakerMode(InCallScreen.this);	//20101108 sh80.choi@lge.com restore the SPK Phone Mode [LGE_LAB1]
							if(PhoneUtils.isSpeakerOn(InCallScreen.this) ){
                                PhoneUtils.changeModemPath(InCallScreen.this, 3);
							}
							else{
                                PhoneUtils.changeModemPath(InCallScreen.this, 1);
							}
                        	//2001120 sumi920.kim@lge.com Disable Speaker in headset plug [LGE_LAB1]
                        	mInCallTouchUi.enableSpeakerphoneButton();
                        }
                        //jongik2.kim 20100519 CP_CALL_PATH [end]
                    }
				// 20101101 jongwany.lee TD issued fixed #10070
					if(mInCallTouchUi.mInCallMsg != null)
                    if(mInCallTouchUi.mInCallMsg != null && mInCallTouchUi.mInCallMsg.isShown() ){
                        	
                    }else {
                       	updateScreen();
                    }
                    break;

                case PhoneApp.MMI_INITIATE:
                    onMMIInitiate((AsyncResult) msg.obj);
                    break;

                case PhoneApp.MMI_CANCEL:
                    onMMICancel();
                    break;

                // handle the mmi complete message.
                // since the message display class has been replaced with
                // a system dialog in PhoneUtils.displayMMIComplete(), we
                // should finish the activity here to close the window.
                case PhoneApp.MMI_COMPLETE:
                    // Check the code to see if the request is ready to
                    // finish, this includes any MMI state that is not
                    // PENDING.
                    MmiCode mmiCode = (MmiCode) ((AsyncResult) msg.obj).result;
                    // if phone is a CDMA phone display feature code completed message
                    int phoneType = mPhone.getPhoneType();
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        PhoneUtils.displayMMIComplete(mPhone, app, mmiCode, null, null);
                    } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                        if (mmiCode.getState() != MmiCode.State.PENDING) {
                            if (DBG) log("Got MMI_COMPLETE, finishing InCallScreen...");
                            endInCallScreenSession();
                        }
                    }
                    break;

                case POST_ON_DIAL_CHARS:
                    handlePostOnDialChars((AsyncResult) msg.obj, (char) msg.arg1);
                    break;

                case ADD_VOICEMAIL_NUMBER:
                    addVoiceMailNumberPanel();
                    break;

                case DONT_ADD_VOICEMAIL_NUMBER:
                    dontAddVoiceMailNumber();
                    break;

                case DELAYED_CLEANUP_AFTER_DISCONNECT:
// LGE_AUTO_REDIAL START
                	// add parameter
                    delayedCleanupAfterDisconnect((Connection.DisconnectCause) msg.obj);
// LGE_AUTO_REDIAL END
                    break;

                case DISMISS_MENU:
                    // dismissMenu() has no effect if the menu is already closed.
                    dismissMenu(true);  // dismissImmediate = true
                    break;

                case ALLOW_SCREEN_ON:
                    if (VDBG) log("ALLOW_SCREEN_ON message...");
                    // Undo our previous call to preventScreenOn(true).
                    // (Note this will cause the screen to turn on
                    // immediately, if it's currently off because of a
                    // prior preventScreenOn(true) call.)
                    app.preventScreenOn(false);
                    break;

                case TOUCH_LOCK_TIMER:
                    if (VDBG) log("TOUCH_LOCK_TIMER...");
                    touchLockTimerExpired();
                    break;

                case REQUEST_UPDATE_BLUETOOTH_INDICATION:
                    if (VDBG) log("REQUEST_UPDATE_BLUETOOTH_INDICATION...");
                    // The bluetooth headset state changed, so some UI
                    // elements may need to update.  (There's no need to
                    // look up the current state here, since any UI
                    // elements that care about the bluetooth state get it
                    // directly from PhoneApp.showBluetoothIndication().)
                    updateScreen();
                    break;

                case PHONE_CDMA_CALL_WAITING:
                    if (DBG) log("Received PHONE_CDMA_CALL_WAITING event ...");
                    Connection cn = mRingingCall.getLatestConnection();

                    // Only proceed if we get a valid connection object
                    if (cn != null) {
                        // Finally update screen with Call waiting info and request
                        // screen to wake up
                        updateScreen();
                        app.updateWakeState();
                    }
                    break;

                case THREEWAY_CALLERINFO_DISPLAY_DONE:
                    if (DBG) log("Received THREEWAY_CALLERINFO_DISPLAY_DONE event ...");
                    if (app.cdmaPhoneCallState.getCurrentCallState()
                            == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                        // Set the mThreeWayCallOrigStateDialing state to true
                        app.cdmaPhoneCallState.setThreeWayCallOrigState(false);

                        //Finally update screen with with the current on going call
                        updateScreen();
                    }
                    break;

                case EVENT_OTA_PROVISION_CHANGE:
                    if (otaUtils != null) {
                        otaUtils.onOtaProvisionStatusChanged((AsyncResult) msg.obj);
                    }
                    break;

                case REQUEST_CLOSE_SPC_ERROR_NOTICE:
                    if (otaUtils != null) {
                        otaUtils.onOtaCloseSpcNotice();
                    }
                    break;

                case REQUEST_CLOSE_OTA_FAILURE_NOTICE:
                    if (otaUtils != null) {
                        otaUtils.onOtaCloseFailureNotice();
                    }
                    break;

                case EVENT_PAUSE_DIALOG_COMPLETE:
                    if (mPausePromptDialog != null) {
                        if (DBG) log("- DISMISSING mPausePromptDialog.");
                        mPausePromptDialog.dismiss();  // safe even if already dismissed
                        mPausePromptDialog = null;
                    }
                    break;

                case EVENT_HIDE_PROVIDER_OVERLAY:
                    mProviderOverlayVisible = false;
                    updateProviderOverlay();  // Clear the overlay.
                    break;

                case REQUEST_UPDATE_TOUCH_UI:
                    updateInCallTouchUi();
                    break;
//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [START_LGE_LAB]
                case REQUEST_UPDATE_CALLCARD_UI:
                    updateInCallCallCardUi();
                    break;
//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [END_LGE_LAB]

// LGE_SS_NOTIFY START
                case SUPP_SERVICE_NOTIFY:
                	AsyncResult ar = (AsyncResult) msg.obj;
                	ss_notify = (SuppServiceNotification) ar.result;
                	Call fgcall = mPhone.getForegroundCall();
                	Call.State state = fgcall.getState();
                	if(state == Call.State.DIALING || state == Call.State.INCOMING){
                	   updateScreen();
                	}
                	else{
                		//<!--[sumi920.kim@lge.com] 2010.10.01	 Remove SUPP_SERVICE_NOTIFY toast [START_LGE_LAB1] -->
                		if(StarConfig.OPERATOR.equals("SKT"))
                		{
                			updateScreen();
                		}
                		else
                		//<!--[sumi920.kim@lge.com] 2010.10.01	 Remove SUPP_SERVICE_NOTIFY toast [END_LGE_LAB1] -->
                		{
                			Context context = getApplicationContext();
                			int res_item = R.string.card_title_mo_0 + ss_notify.getResource();
                			CharSequence text = getString(res_item);
                			int duration = Toast.LENGTH_SHORT;
                			Toast toast = Toast.makeText(context, text, duration);
                			toast.setGravity(Gravity.CENTER, 0, 0);
                			toast.show();
                			ss_notify = null;
                		}
                	}
                    break;
// LGE_SS_NOTIFY END
// LGE_CALL_COSTS START
                case AOC_QUERY_ICC_ACM_MAX: {
                    if (DBG) log("Received AOC_QUERY_ICC_ACM_MAX event");

                    AsyncResult arAcmMax = (AsyncResult) msg.obj;
                    if (arAcmMax.exception != null) {
                        if (DBG) {
                           log("Failed to get ACM Max");
                           log(arAcmMax.exception.getMessage());
                        }
                    } else {
                        String acmMaxStr = (String)(arAcmMax.result);

                        try {
                            acmMax = Integer.parseInt(acmMaxStr);
                        } catch (NumberFormatException e) {
                            if (DBG) log("Failed to parse acmMax : " + e.getMessage());
                        }
                    }
                    break;
                }

                case AOC_QUERY_ICC_ACM: {
                    if (DBG) log("Received AOC_QUERY_ICC_ACM event");

                    AsyncResult arAcm = (AsyncResult) msg.obj;
                    if (arAcm.exception != null) {
                        if (DBG) {
                           log("Failed to get ACM");
                           log(arAcm.exception.getMessage());
                        }
                    } else {
                        String acmStr = (String)(arAcm.result);

                        try {
                            acm = Integer.parseInt(acmStr);
                        } catch (NumberFormatException e) {
                            if (DBG) log("Failed to parse acm : " + e.getMessage());

                            break;
                        }
                    }


                    if((acmMax > 0) && (acm > acmMax) && !mCallCostIsEmergency) {
                        if (DBG) log("Out of Credit Limit. Hangup calls!");

                        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, ToneGenerator.MAX_VOLUME>>1);
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                        toneGenerator.stopTone();
                        Toast.makeText(InCallScreen.this, R.string.call_cost_limit_exceed_error,
                                    Toast.LENGTH_LONG).show();
                        PhoneUtils.hangup(mPhone);
                    }
                    break;
                }
// LGE_CALL_COSTS END
          // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
                case CALL_RELATED_BUTTON_LOCK:
                    mCallRelatedButtonLocked = false;
                    break;
          // LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
                //START sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv
                // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
                case ANSWER_RINGINGCALL_IN_THREEWAYCALL:
                	if (DBG) log("Received ANSWER_RINGINGCALL_IN_THREEWAYCALL");
                	PhoneUtils.answerCall(mPhone);
                	break;
                // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
                //END sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv


                    // 20101019 sumi920.kim@lge.com for Home Key In Alerting [START_LGE_LAB1]
				case HOME_KEYUP_IGNORE_RINGING_STATE :
					if(StarConfig.COUNTRY.equals("KR"))
					{
						if(mIgnoreHomeKey == false)
							mIgnoreHomeKey = true;
						log(" receive HOME_KEYUP_IGNORE_RINGING_STATE mIgnoreHomeKey" + mIgnoreHomeKey );
					}
					break;						
			    // 20101019 sumi920.kim@lge.com for Home Key In Alerting [END_LGE_LAB1]


            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                    // Listen for ACTION_HEADSET_PLUG broadcasts so that we
                    // can update the onscreen UI when the headset state changes.
                    // if (DBG) log("mReceiver: ACTION_HEADSET_PLUG");
                    // if (DBG) log("==> intent: " + intent);
                    // if (DBG) log("    state: " + intent.getIntExtra("state", 0));
                    // if (DBG) log("    name: " + intent.getStringExtra("name"));
                    // send the event and add the state as an argument.
                    Message message = Message.obtain(mHandler, EVENT_HEADSET_PLUG_STATE_CHANGED,
                            intent.getIntExtra("state", 0), 0);
                    mHandler.sendMessage(message);
// LGE_RIL_SPEAKERPHONE_SUPPORT START
                } else if (action.equals(AudioManager.SET_SPEAKERPHONE_INTENT)) {
                    Log.d(LOG_TAG, "SET_SPEAKERPHONE_INTENT received");
                    //2001120 sumi920.kim@lge.com Disable Speaker in headset plug [LGE_LAB1]
                    if(!PhoneApp.getInstance().isHeadsetPlugged() &&  !isBluetoothAvailable())
                    	mInCallTouchUi.enableSpeakerphoneButton();
                }
                else if (action.equals(Intent.ACTION_SHUTDOWN)) { //hyojin.an 101111 : it access the shutdown intent on power off
                    Log.d(LOG_TAG, "SET_SPEAKERPHONE_INTENT received");
                    //stopSelf();
                }
// LGE_RIL_SPEAKERPHONE_SUPPORT END
            }
        };

        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]        
        //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        private final BroadcastReceiver mScreenStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            	String action = intent.getAction();
            	                
            	if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            		if (VDBG) Log.w(LOG_TAG, "InCallScreen : ACTION_SCREEN_OFF - setWillBeResumedAfterScreenOff(true)");
            		final PhoneApp app = PhoneApp.getInstance();
            		app.setWillBeResumedAfterScreenOff(true);

            		// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-14 : Exception Handling Keyguard for Reject Message
            		if (okToDelayOfUpdateKeyguardPolicy()) {
//                       mIsProcessingRejectMessage = InCallRejectMessageState.REJECTMESSAGE_NONE;
            			updateKeyguardPolicy(false); // true : dismissKeyguard, false : showKeyguard
            		}
            		// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-14 : Exception Handling Keyguard for Reject Message
            	} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
            		if (VDBG) Log.w(LOG_TAG, "InCallScreen : ACTION_SCREEN_ON - setWillBeResumedAfterScreenOff(false)");
                    final PhoneApp app = PhoneApp.getInstance();
                    app.setWillBeResumedAfterScreenOff(false);
            	}
            }
        };
        //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    protected void onSubCreate(Bundle icicle)
   {
        if (DBG) log("onCreate()...  this = " + this);

        Profiler.callScreenOnCreate();

        final PhoneApp app = PhoneApp.getInstance();
        app.setInCallScreenInstance(this);

        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
        //START jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
        app.checkProximitySensorSetting();
        //END jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]
        
        // set this flag so this activity will stay in front of the keyguard
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        if (app.getPhoneState() == Phone.State.OFFHOOK) {
            // While we are in call, the in-call screen should dismiss the keyguard.
            // This allows the user to press Home to go directly home without going through
            // an insecure lock screen.
            // But we do not want to do this if there is no active call so we do not
            // bypass the keyguard if the call is not answered or declined.
            flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
        }
        getWindow().addFlags(flags);

        setPhone(app.phone);  // Sets mPhone and mForegroundCall/mBackgroundCall/mRingingCall

        mBluetoothHandsfree = app.getBluetoothHandsfree();
        if (VDBG) log("- mBluetoothHandsfree: " + mBluetoothHandsfree);

        if (mBluetoothHandsfree != null) {
            // The PhoneApp only creates a BluetoothHandsfree instance in the
            // first place if BluetoothAdapter.getDefaultAdapter()
            // succeeds.  So at this point we know the device is BT-capable.
            mBluetoothHeadset = new BluetoothHeadset(this, null);
            if (VDBG) log("- Got BluetoothHeadset: " + mBluetoothHeadset);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Inflate everything in incall_screen.xml and add it to the screen.
        setContentView(R.layout.incall_screen);

        initInCallScreen();

        // Create the dtmf dialer.  The dialer view we use depends on the
        // current platform:
        //
        // - On non-prox-sensor devices, it's the dialpad contained inside
        //   a SlidingDrawer widget (see dtmf_twelve_key_dialer.xml).
        //
        // - On "full touch UI" devices, it's the compact non-sliding
        //   dialpad that appears on the upper half of the screen,
        //   above the main cluster of InCallTouchUi buttons
        //   (see non_drawer_dialpad.xml).
        //
        // TODO: These should both be ViewStubs, and right here we should
        // inflate one or the other.  (Also, while doing that, let's also
        // move this block of code over to initInCallScreen().)
        //
        SlidingDrawer dialerDrawer;
        if (isTouchUiEnabled()) {
            // This is a "full touch" device.
            mDialerView = (DTMFTwelveKeyDialerView) findViewById(R.id.non_drawer_dtmf_dialer);
            if (DBG) log("- Full touch device!  Found dialerView: " + mDialerView);
            dialerDrawer = null;  // No SlidingDrawer used on this device.
        } else {
            // Use the old-style dialpad contained within the SlidingDrawer.
            mDialerView = (DTMFTwelveKeyDialerView) findViewById(R.id.dtmf_dialer);
            if (DBG) log("- Using SlidingDrawer-based dialpad.  Found dialerView: " + mDialerView);
            dialerDrawer = (SlidingDrawer) findViewById(R.id.dialer_container);
            if (DBG) log("  ...and the SlidingDrawer: " + dialerDrawer);
        }
        // Sanity-check that (regardless of the device) at least the
        // dialer view is present:
        if (mDialerView == null) {
            Log.e(LOG_TAG, "onCreate: couldn't find dialerView", new IllegalStateException());
        }
        // Finally, create the DTMFTwelveKeyDialer instance.
        mDialer = new DTMFTwelveKeyDialer(this, mDialerView, dialerDrawer);

        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
        //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        registerForScreenStates();
        //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]
        
        // No need to change wake state here; that happens in onResume() when we
        // are actually displayed.

        // Handle the Intent we were launched with, but only if this is the
        // the very first time we're being launched (ie. NOT if we're being
        // re-initialized after previously being shut down.)
        // Once we're up and running, any future Intents we need
        // to handle will come in via the onNewIntent() method.
        if (icicle == null) {
            if (DBG) log("onCreate(): this is our very first launch, checking intent...");

            // Stash the result code from internalResolveIntent() in the
            // mInCallInitialStatus field.  If it's an error code, we'll
            // handle it in onResume().
            mInCallInitialStatus = internalResolveIntent(getIntent());
            if (DBG) log("onCreate(): mInCallInitialStatus = " + mInCallInitialStatus);
            if (mInCallInitialStatus != InCallInitStatus.SUCCESS) {
                Log.w(LOG_TAG, "onCreate: status " + mInCallInitialStatus
                      + " from internalResolveIntent()");
                // See onResume() for the actual error handling.
            }
        } else {
            mInCallInitialStatus = InCallInitStatus.SUCCESS;
        }

        // The "touch lock overlay" feature is used only on devices that
        // *don't* use a proximity sensor to turn the screen off while in-call.
        mUseTouchLockOverlay = !app.proximitySensorModeEnabled();
// LGE_SS_NOTIFY START
        final CallNotifier notifier = PhoneApp.getInstance().notifier;
 	    ss_notify = notifier.getSuppServiceState();
// LGE_SS_NOTIFY END
        Profiler.callScreenCreated();
//<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]	 -->
        if(StarConfig.COUNTRY.equals("KR"))
        	mSoundRecord = new soundRecord(); 
//<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]	 -->	
        if (DBG) log("onCreate(): exit");
    }

    /**
     * Sets the Phone object used internally by the InCallScreen.
     *
     * In normal operation this is called from onCreate(), and the
     * passed-in Phone object comes from the PhoneApp.
     * For testing, test classes can use this method to
     * inject a test Phone instance.
     */
    /* package */ void setPhone(Phone phone) {
        if (mPhone != phone) {
            log("[InCallScreen-SetPhone]Phone was updated. Current: " + phone.getPhoneName());
        }
        mPhone = phone;
        // Hang onto the three Call objects too; they're singletons that
        // are constant (and never null) for the life of the Phone.
        mForegroundCall = mPhone.getForegroundCall();
        mBackgroundCall = mPhone.getBackgroundCall();
        mRingingCall = mPhone.getRingingCall();
    }

    @Override
	protected void onStart() {
	super.onStart();
	
	//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
	//START jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
	// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-03-25 : TouchLock Scenario depend on Proximity Setting (HI)
	final PhoneApp app = PhoneApp.getInstance();
	app.checkProximitySensorSetting();
	//LGE_CHANGE_E [shinhae.lee@lge.com] 2010-03-25 : TouchLock Scenario depend on Proximity Setting (HI)
	//END jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
	//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]	
	}

	protected void 	onSubStart()
	{
		registerForPhoneStates();
	}
	
    @Override
    protected void onResume() {
	super.onResume();
    }

    protected void onSubResume()
    {
        if (DBG) log("onResume()...");

        // jongwany.lee@lge.com START FUNCTION FOR CALL_UI
        inCallControl_GONE_Flag = false ;
        // jongwany.lee@lge.com END FUNCTION FOR CALL_UI
        mIsForegroundActivity = true;

        // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
        mCallRelatedButtonLocked = false;
        // LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
        final PhoneApp app = PhoneApp.getInstance();

// LGE_CALL_COSTS START
        mCmdIf = PhoneFactory.getCommandsInterface();
// LGE_CALL_COSTS END

        app.disableStatusBar();

        // Touch events are never considered "user activity" while the
        // InCallScreen is active, so that unintentional touches won't
        // prevent the device from going to sleep.
        app.setIgnoreTouchUserActivity(true);

        // Disable the status bar "window shade" the entire time we're on
        // the in-call screen.
        NotificationMgr.getDefault().getStatusBarMgr().enableExpandedView(false);

        // Listen for broadcast intents that might affect the onscreen UI.
// LGE_RIL_SPEAKERPHONE_SUPPORT START        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.SET_SPEAKERPHONE_INTENT);
        filter.addAction(Intent.ACTION_SHUTDOWN); //hyojin.an 101111 : it access the shutdown intent on power off
        registerReceiver(mReceiver, filter);

        mInCallTouchUi.setSpeakerphoneButtonForcedDisabled(false);
// LGE_RIL_SPEAKERPHONE_SUPPORT END

        // Keep a "dialer session" active when we're in the foreground.
        // (This is needed to play DTMF tones.)
        mDialer.startDialerSession();
//20100723 yongwoon.choi@lge.com Lock/UnLock Incoming Scenario [START_LGE]
        mInCallTouchUi.setInCallMessageViewInstance();
//20100723 yongwoon.choi@lge.com Lock/UnLock Incoming Scenario [END_LGE]

        //20101025 sumi920.kim@lge.com Initialize inCallScreenInstance ( Video Call ->Conv-> End -> Voice Call origination )  
        app.setInCallScreenInstance(this);
        
//20101014 wonho.moon@lge.com <mailto:wonho.moon@lge.com> TD 139399 : AIRPLANE_MODE [START_LGE_LAB]
        if (Settings.System.getInt(mPhone.getContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
            mInCallInitialStatus = InCallInitStatus.POWER_OFF;
        }
//20101014 wonho.moon@lge.com <mailto:wonho.moon@lge.com> TD 139399 : AIRPLANE_MODE [END_LGE_LAB]

        // Check for any failures that happened during onCreate() or onNewIntent().
        if (DBG) log("- onResume: initial status = " + mInCallInitialStatus);
        if (mInCallInitialStatus != InCallInitStatus.SUCCESS) {
            if (DBG) log("- onResume: failure during startup: " + mInCallInitialStatus);

            // Don't bring up the regular Phone UI!  Instead bring up
            // something more specific to let the user deal with the
            // problem.
            handleStartupError(mInCallInitialStatus);

            // But it *is* OK to continue with the rest of onResume(),
            // since any further setup steps (like updateScreen() and the
            // CallCard setup) will fall back to a "blank" state if the
            // phone isn't in use.
            
            // 20101014 jongwany.lee@lge.com TD issue fixed 9044, 9299, 9711 regarding dismissed popup dialog
            //mInCallInitialStatus = InCallInitStatus.SUCCESS;
        }

        // Set the volume control handler while we are in the foreground.
        final boolean bluetoothConnected = isBluetoothAudioConnected();

        if (bluetoothConnected) {
            setVolumeControlStream(AudioManager.STREAM_BLUETOOTH_SCO);
        } else {
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        takeKeyEvents(true);
//20100425 yongwoon.choi@lge.com Second Incoming UI [START_LGE]
//20100804 jongwany.lee@lge.com attached it for CALL UI
      if (mPhone.getState() == Phone.State.RINGING) {
    	  
          mInCallTouchUi.SetIncomingWidget();
        }
//20100425 yongwoon.choi@lge.com Second Incoming UI [END_LGE]

        boolean phoneIsCdma = (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA);

        boolean inOtaCall = false;
        if (phoneIsCdma) {
            inOtaCall = initOtaState();
        }
        //20101025 sumi920.kim [START_LGE_LAB1] 
        // LCD off -> call end -> delayedCleanupAfterDisconnect()-> onresume ()-> ui error
        if(StarConfig.COUNTRY.equals("KR"))
        {
        	if(mdelayedCleanupAfterResume)
        	{
        		if (DBG) log("- mdelayedCleanupAfterResume = " + mdelayedCleanupAfterResume);
        		delayedCleanupAfterDisconnect(delayedData);
        		mdelayedCleanupAfterResume = false;
        	}
        }
        //20101025 sumi920.kim [END_LGE_LAB1]
        
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1] 
        //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        if (getIntent().hasExtra(ACTION_DISPLAY_CALLEND)
        	|| okToShowCallEnd() // Conv => Home Key => Approach Proximity  => LCD Off => remote end : don't has extra ACTION_DISPLAY_CALLEND
        ) {
        	if (DBG) log("onResume() : ACTION_DISPLAY_CALLEND");
             
        	// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-07-01 : Exception Handle for Call End display
        	if (okToExitInCallScreen()) {
        		mPhone.clearDisconnected();
        		mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
        		getIntent().removeExtra(ACTION_DISPLAY_CALLEND);

        		endInCallScreenSession();
        	} else {
        		// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-07-01 : Exception Handle for Call End display
        		updateScreen();
        		if (!phoneIsInUse()) {
        			setInCallScreenMode(InCallScreenMode.CALL_ENDED);
        		} else {
        			setInCallScreenMode(InCallScreenMode.NORMAL);
        		}
        		mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
        		mHandler.sendEmptyMessageDelayed(DELAYED_CLEANUP_AFTER_DISCONNECT, CALL_ENDED_LONG_DELAY);
        		
        		getIntent().removeExtra(ACTION_DISPLAY_CALLEND);
        		//PhoneApp.getInstance().pokeUserActivity();
        	}
        }
        else
        //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on  
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
        {
	        if (!inOtaCall) {
	            // Always start off in NORMAL mode
	            if (Settings.System.getInt(mPhone.getContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 1) {
	            	setInCallScreenMode(InCallScreenMode.NORMAL);
	            }
	        }
        }
        // Before checking the state of the phone, clean up any
        // connections in the DISCONNECTED state.
        // (The DISCONNECTED state is used only to drive the "call ended"
        // UI; it's totally useless when *entering* the InCallScreen.)
        mPhone.clearDisconnected();

        InCallInitStatus status = syncWithPhoneState();
        if (status != InCallInitStatus.SUCCESS) {
            if (DBG) log("- syncWithPhoneState failed! status = " + status);
            // Couldn't update the UI, presumably because the phone is totally
            // idle.  But don't endInCallScreenSession immediately, since we might still
            // have an error dialog up that the user needs to see.
            // (And in that case, the error dialog is responsible for calling
            // endInCallScreenSession when the user dismisses it.)
        } else if (phoneIsCdma) {
            if (mInCallScreenMode == InCallScreenMode.OTA_NORMAL ||
                    mInCallScreenMode == InCallScreenMode.OTA_ENDED) {
                mDialer.setHandleVisible(false);
                if (mInCallPanel != null) mInCallPanel.setVisibility(View.GONE);
                updateScreen();
                return;
            }
        }

        // InCallScreen is now active.
        EventLog.writeEvent(EventLogTags.PHONE_UI_ENTER);

        // Update the poke lock and wake lock when we move to
        // the foreground.
        //
        // But we need to do something special if we're coming
        // to the foreground while an incoming call is ringing:
        if (mPhone.getState() == Phone.State.RINGING) {
            // If the phone is ringing, we *should* already be holding a
            // full wake lock (which we would have acquired before
            // firing off the intent that brought us here; see
            // PhoneUtils.showIncomingCallUi().)
            //
            // We also called preventScreenOn(true) at that point, to
            // avoid cosmetic glitches while we were being launched.
            // So now we need to post an ALLOW_SCREEN_ON message to
            // (eventually) undo the prior preventScreenOn(true) call.
            //
            // (In principle we shouldn't do this until after our first
            // layout/draw pass.  But in practice, the delay caused by
            // simply waiting for the end of the message queue is long
            // enough to avoid any flickering of the lock screen before
            // the InCallScreen comes up.)
            if (VDBG) log("- posting ALLOW_SCREEN_ON message...");
            mHandler.removeMessages(ALLOW_SCREEN_ON);
            mHandler.sendEmptyMessage(ALLOW_SCREEN_ON);

            // TODO: There ought to be a more elegant way of doing this,
            // probably by having the PowerManager and ActivityManager
            // work together to let apps request that the screen on/off
            // state be synchronized with the Activity lifecycle.
            // (See bug 1648751.)
        } else {
            // The phone isn't ringing; this is either an outgoing call, or
            // we're returning to a call in progress.  There *shouldn't* be
            // any prior preventScreenOn(true) call that we need to undo,
            // but let's do this just to be safe:
            app.preventScreenOn(false);
        }
        app.updateWakeState();

        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
        //START jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
        if (app.isProximitySensorSetting()) {
        	mHandler.removeMessages(TOUCH_LOCK_TIMER);
            enableTouchLock(false);
        }
        else
        //END jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]        	
        {
	        // The "touch lock" overlay is NEVER visible when we resume.
	        // (In particular, this check ensures that we won't still be
	        // locked after the user wakes up the screen by pressing MENU.)
	        enableTouchLock(false);
	        // ...but if the dialpad is open we DO need to start the timer
	        // that will eventually bring up the "touch lock" overlay.
	        if (mDialer.isOpened()) resetTouchLockTimer();
        }
        // Restore the mute state if the last mute state change was NOT
        // done by the user.
        if (app.getRestoreMuteOnInCallResume()) {
            PhoneUtils.restoreMuteState(mPhone);
            app.setRestoreMuteOnInCallResume(false);
        }

        if(mInCallScreenMode == InCallScreenMode.CALL_ENDED)
        {
        	updateEndCallTouchUi(true);
        }
        else
        {
        	updateEndCallTouchUi(false);
        }
        
        //jundj@mo2.co.kr start
        if(VEUtils.isSKTFeature()){
            if(mPhone.getState() == Phone.State.RINGING){
                Call ringingCall = mPhone.getRingingCall();
                if(ringingCall.state == Call.State.INCOMING){
                    VE_ContentManager.getInstance().getHandler().sendEmptyMessage(VE_ContentManager.HANDLE_MSG_INCALLSCREEN_IS_READY);
                }
            }
        }
      //jundj@mo2.co.kr end
        //20101101 sumi920.kim@lge.com FMC Call [START_LGE_LAB1]
        if(PhoneApp.getInstance().isCallFMC())    //LGE_FMC
            mCallCard.mCommonFMC.setVisibility(View.VISIBLE);
        else
           app.setCallFMCState(1);
        //20101101 sumi920.kim@lge.com FMC Call [START_LGE_LAB1]
        
        Profiler.profileViewCreate(getWindow(), InCallScreen.class.getName());
        //20101105 sumi920.kim@lge.com SKT SKAF SpeakerOn/Off setting [START_LGE_LAB1]
        if(StarConfig.OPERATOR.equals("SKT"))
        {
        	if(PhoneUtils.haveToSetSKAFSpeakerMode())
        	{
        		// ����Ŀ ����.
        		
        		boolean curSpeakerMode = PhoneUtils.isSpeakerOn(this);
        		if(curSpeakerMode != PhoneUtils.getSKAFSpeakerMode())
        		{
        			if (DBG) log("curSpeakerMode : " + curSpeakerMode );
        			if (DBG) log("getSKAFSpeakerMode : " + PhoneUtils.getSKAFSpeakerMode());
        			onSpeakerClick();
        		}
        		PhoneUtils.initSKAFSpeakerMode(false,false);
        	}
        	
        }
       
        //20101105 sumi920.kim@lge.com SKT SKAF SpeakerOn/Off setting [END_LGE_LAB1]
        if (VDBG) log("onResume() done.");
    }

    // onPause is guaranteed to be called when the InCallScreen goes
    // in the background.
    @Override
    protected void onPause() {
           super.onPause();
    }

    protected void onSubPause()
   {
        if (DBG) log("onPause()...");

        mIsForegroundActivity = false;

        // Force a clear of the provider overlay' frame. Since the
        // overlay is removed using a timed message, it is
        // possible we missed it if the prev call was interrupted.
        mProviderOverlayVisible = false;
        updateProviderOverlay();

        final PhoneApp app = PhoneApp.getInstance();

        // A safety measure to disable proximity sensor in case call failed
        // and the telephony state did not change.
        app.setBeginningCall(false);

        // Make sure the "Manage conference" chronometer is stopped when
        // we move away from the foreground.
        mManageConferenceUtils.stopConferenceTime();

        // as a catch-all, make sure that any dtmf tones are stopped
        // when the UI is no longer in the foreground.
        mDialer.onDialerKeyUp(null);

        // Release any "dialer session" resources, now that we're no
        // longer in the foreground.
        mDialer.stopDialerSession();

        // If the device is put to sleep as the phone call is ending,
        // we may see cases where the DELAYED_CLEANUP_AFTER_DISCONNECT
        // event gets handled AFTER the device goes to sleep and wakes
        // up again.

        // This is because it is possible for a sleep command
        // (executed with the End Call key) to come during the 2
        // seconds that the "Call Ended" screen is up.  Sleep then
        // pauses the device (including the cleanup event) and
        // resumes the event when it wakes up.

        // To fix this, we introduce a bit of code that pushes the UI
        // to the background if we pause and see a request to
        // DELAYED_CLEANUP_AFTER_DISCONNECT.

        // Note: We can try to finish directly, by:
        //  1. Removing the DELAYED_CLEANUP_AFTER_DISCONNECT messages
        //  2. Calling delayedCleanupAfterDisconnect directly

        // However, doing so can cause problems between the phone
        // app and the keyguard - the keyguard is trying to sleep at
        // the same time that the phone state is changing.  This can
        // end up causing the sleep request to be ignored.
        if (mHandler.hasMessages(DELAYED_CLEANUP_AFTER_DISCONNECT)
                && mPhone.getState() != Phone.State.RINGING) {
            if (DBG) log("DELAYED_CLEANUP_AFTER_DISCONNECT detected, moving UI to background.");
            endInCallScreenSession();
        }

        EventLog.writeEvent(EventLogTags.PHONE_UI_EXIT);

        // Clean up the menu, in case we get paused while the menu is up
        // for some reason.
        dismissMenu(true);  // dismiss immediately

        // Dismiss any dialogs we may have brought up, just to be 100%
        // sure they won't still be around when we get back here.
        dismissAllDialogs();

        // Re-enable the status bar (which we disabled in onResume().)
        NotificationMgr.getDefault().getStatusBarMgr().enableExpandedView(true);

        // Unregister for broadcast intents.  (These affect the visible UI
        // of the InCallScreen, so we only care about them while we're in the
        // foreground.)
        unregisterReceiver(mReceiver);

        // Re-enable "user activity" for touch events.
        // We actually do this slightly *after* onPause(), to work around a
        // race condition where a touch can come in after we've paused
        // but before the device actually goes to sleep.
        // TODO: The PowerManager itself should prevent this from happening.
        mHandler.postDelayed(new Runnable() {
                public void run() {
                    app.setIgnoreTouchUserActivity(false);
                }
            }, 500);

        app.reenableStatusBar();

        // Make sure we revert the poke lock and wake lock when we move to
        // the background.
        app.updateWakeState();

        // clear the dismiss keyguard flag so we are back to the default state
        // when we next resume

        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
        //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        if (!okToDelayOfUpdateKeyguardPolicy())
        //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
       	//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]        	
        	updateKeyguardPolicy(false);
        
        if(mInCallScreenMode == InCallScreenMode.CALL_ENDED)
        {
        	updateEndCallTouchUi(false);
        }
        
//        if(mPhone.getState() != Phone.State.OFFHOOK)
//        {
//        	mOldIntent = null;
//        }
        
        //20101025 sumi920.kim [START_LGE_LAB1] 
        // LCD off -> call end -> delayedCleanupAfterDisconnect()-> onresume ()-> ui error 
        mdelayedCleanupAfterResume = false;
        
    	//20101111 sumi920.kim Stop Recording when be background app.[START_LGE_LAB1]
        if(StarConfig.COUNTRY.equals("KR"))
        {
	    	if(PhoneUtils.isSoundRecording() == true)
	    	{
        		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        		boolean isScreenOn = pm.isScreenOn();
        		
        		if(isScreenOn)
        		{
        			if (DBG) log(" stopButton ... isScreenOn :" + isScreenOn);
        			stopRecording();
        		}
	    	}
        }
    	//20101111 sumi920.kim Stop Recording when be background app.[END_LGE_LAB1]
        if (DBG) log("onPause()..mdelayedCleanupAfterResume(" + mdelayedCleanupAfterResume+")");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void onSubStop()
    {
           if (DBG) log("onStop()...");

	unregisterForPhoneStates();
        stopTimer();

        Phone.State state = mPhone.getState();
        if (DBG) log("onStop: state = " + state);

        if (state == Phone.State.IDLE) {
            final PhoneApp app = PhoneApp.getInstance();
            // when OTA Activation, OTA Success/Failure dialog or OTA SPC
            // failure dialog is running, do not destroy inCallScreen. Because call
            // is already ended and dialog will not get redrawn on slider event.
            if ((app.cdmaOtaProvisionData != null) && (app.cdmaOtaScreenState != null)
                    && ((app.cdmaOtaScreenState.otaScreenState !=
                            CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION)
                        && (app.cdmaOtaScreenState.otaScreenState !=
                            CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG)
                        && (!app.cdmaOtaProvisionData.inOtaSpcState))) {
                // we don't want the call screen to remain in the activity history
                // if there are not active or ringing calls.
                if (DBG) log("- onStop: calling finish() to clear activity history...");
                moveTaskToBack(true);
                if (otaUtils != null) {
                    otaUtils.cleanOtaScreen(true);
                }
			}
			//20101102 wonho.moon@lge.com Password check add
			PhoneApp.getInstance().setIsCheckPassword(false);
			log("onStop() isCheckPassword ="+PhoneApp.getInstance().getIsCheckPassword());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

     protected void onSubDestroy()
     {
        if (DBG) log("onDestroy()...");

        // Set the magic flag that tells us NOT to handle any handler
        // messages that come in asynchronously after we get destroyed.
        mIsDestroyed = true;

        final PhoneApp app = PhoneApp.getInstance();
        app.setInCallScreenInstance(null);

        // Clear out the InCallScreen references in various helper objects
        // (to let them know we've been destroyed).
        if (mInCallMenu != null) {
            mInCallMenu.clearInCallScreenReference();
        }
        if (mCallCard != null) {
            mCallCard.setInCallScreenInstance(null);
        }
        if (mInCallTouchUi != null) {
            mInCallTouchUi.setInCallScreenInstance(null);
        }

        mDialer.clearInCallScreenReference();
        mDialer = null;

        unregisterForPhoneStates();
        
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]        
        //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        unregisterForScreenStates();
        //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
        //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
        
        // No need to change wake state here; that happens in onPause() when we
        // are moving out of the foreground.

        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.close();
            mBluetoothHeadset = null;
        }

        // Dismiss all dialogs, to be absolutely sure we won't leak any of
        // them while changing orientation.
        dismissAllDialogs();
    }

    /**
     * Dismisses the in-call screen.
     *
     * We never *really* finish() the InCallScreen, since we don't want to
     * get destroyed and then have to be re-created from scratch for the
     * next call.  Instead, we just move ourselves to the back of the
     * activity stack.
     *
     * This also means that we'll no longer be reachable via the BACK
     * button (since moveTaskToBack() puts us behind the Home app, but the
     * home app doesn't allow the BACK key to move you any farther down in
     * the history stack.)
     *
     * (Since the Phone app itself is never killed, this basically means
     * that we'll keep a single InCallScreen instance around for the
     * entire uptime of the device.  This noticeably improves the UI
     * responsiveness for incoming calls.)
     */
    @Override
    public void finish() {
        if (DBG) log("finish()...");
        moveTaskToBack(true);
// LGE_SS_NOTIFY START
        ss_notify = null;
// LGE_SS_NOTIFY END
    }

    /**
     * End the current in call screen session.
     *
     * This must be called when an InCallScreen session has
     * complete so that the next invocation via an onResume will
     * not be in an old state.
     */
    public void endInCallScreenSession() {
        if (DBG) log("endInCallScreenSession()...");
        moveTaskToBack(true);
        setInCallScreenMode(InCallScreenMode.UNDEFINED);
    }

    /* package */ boolean isForegroundActivity() {
    		return mIsForegroundActivity;
    }

    /* package */ void updateKeyguardPolicy(boolean dismissKeyguard) {
        if (dismissKeyguard) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    private void registerForPhoneStates() {
        if (!mRegisteredForPhoneStates) {
            mPhone.registerForPreciseCallStateChanged(mHandler, PHONE_STATE_CHANGED, null);
            mPhone.registerForDisconnect(mHandler, PHONE_DISCONNECT, null);
            int phoneType = mPhone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_GSM) {
                mPhone.registerForMmiInitiate(mHandler, PhoneApp.MMI_INITIATE, null);

                // register for the MMI complete message.  Upon completion,
                // PhoneUtils will bring up a system dialog instead of the
                // message display class in PhoneUtils.displayMMIComplete().
                // We'll listen for that message too, so that we can finish
                // the activity at the same time.
                mPhone.registerForMmiComplete(mHandler, PhoneApp.MMI_COMPLETE, null);
            } else if (phoneType == Phone.PHONE_TYPE_CDMA) {
                if (DBG) log("Registering for Call Waiting.");
                mPhone.registerForCallWaiting(mHandler, PHONE_CDMA_CALL_WAITING, null);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }

            mPhone.setOnPostDialCharacter(mHandler, POST_ON_DIAL_CHARS, null);
            mPhone.registerForSuppServiceFailed(mHandler, SUPP_SERVICE_FAILED, null);
// LGE_SS_NOTIFY START
            mPhone.registerForSuppServiceNotification(mHandler, SUPP_SERVICE_NOTIFY, null);
// LGE_SS_NOTIFY END
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                mPhone.registerForCdmaOtaStatusChange(mHandler, EVENT_OTA_PROVISION_CHANGE, null);
            }
            mRegisteredForPhoneStates = true;
        }
    }

    private void unregisterForPhoneStates() {
        mPhone.unregisterForPreciseCallStateChanged(mHandler);
        mPhone.unregisterForDisconnect(mHandler);
        mPhone.unregisterForMmiInitiate(mHandler);
        mPhone.unregisterForCallWaiting(mHandler);
        mPhone.setOnPostDialCharacter(null, POST_ON_DIAL_CHARS, null);
        mPhone.unregisterForCdmaOtaStatusChange(mHandler);
// LGE_SS_NOTIFY START
        mPhone.unregisterForSuppServiceNotification(mHandler);
// LGE_SS_NOTIFY END
        mRegisteredForPhoneStates = false;
    }
    
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
    //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
    // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-04-02 : Change Touch Lock Scenario (LGT)
    private void registerForScreenStates() {
    	IntentFilter filter = new IntentFilter(); 
    	filter.addAction(Intent.ACTION_SCREEN_OFF); 
    	filter.addAction(Intent.ACTION_SCREEN_ON); 

    	registerReceiver(mScreenStatusReceiver, filter);
    }

    private void unregisterForScreenStates() {
    	unregisterReceiver(mScreenStatusReceiver);
    }
    // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-04-02 : Change Touch Lock Scenario (LGT)
    //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]

    /* package */ void updateAfterRadioTechnologyChange() {
        if (DBG) Log.d(LOG_TAG, "updateAfterRadioTechnologyChange()...");
        // Unregister for all events from the old obsolete phone
        unregisterForPhoneStates();

        // (Re)register for all events relevant to the new active phone
        registerForPhoneStates();

        // Update mPhone and m{Foreground,Background,Ringing}Call
        PhoneApp app = PhoneApp.getInstance();
        setPhone(app.phone);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DBG) log("onNewIntent: intent=" + intent);

        // We're being re-launched with a new Intent.  Since we keep
        // around a single InCallScreen instance for the life of the phone
        // process (see finish()), this sequence will happen EVERY time
        // there's a new incoming or outgoing call except for the very
        // first time the InCallScreen gets created.  This sequence will
        // also happen if the InCallScreen is already in the foreground
        // (e.g. getting a new ACTION_CALL intent while we were already
        // using the other line.)

        // Stash away the new intent so that we can get it in the future
        // by calling getIntent().  (Otherwise getIntent() will return the
        // original Intent from when we first got created!)
        setIntent(intent);
        
//        if(mPhone.getState() != Phone.State.OFFHOOK)
//        {
//        	mOldIntent = intent;
//        }

        // Activities are always paused before receiving a new intent, so
        // we can count on our onResume() method being called next.

        // Just like in onCreate(), handle this intent, and stash the
        // result code from internalResolveIntent() in the
        // mInCallInitialStatus field.  If it's an error code, we'll
        // handle it in onResume().
        mInCallInitialStatus = internalResolveIntent(intent);
        if (mInCallInitialStatus != InCallInitStatus.SUCCESS) {
            Log.w(LOG_TAG, "onNewIntent: status " + mInCallInitialStatus
                  + " from internalResolveIntent()");
            // See onResume() for the actual error handling.
        }
    }

    /* package */ InCallInitStatus internalResolveIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return InCallInitStatus.SUCCESS;
        }

        checkIsOtaCall(intent);

        String action = intent.getAction();
        if (DBG) log("internalResolveIntent: action=" + action);

        // The calls to setRestoreMuteOnInCallResume() inform the phone
        // that we're dealing with new connections (either a placing an
        // outgoing call or answering an incoming one, and NOT handling
        // an aborted "Add Call" request), so we should let the mute state
        // be handled by the PhoneUtils phone state change handler.
        final PhoneApp app = PhoneApp.getInstance();
        // If OTA Activation is configured for Power up scenario, then
        // InCallScreen UI started with Intent of ACTION_SHOW_ACTIVATION
        // to show OTA Activation screen at power up.
        if ((action.equals(ACTION_SHOW_ACTIVATION))
                && ((mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA))) {
            setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
            if ((app.cdmaOtaProvisionData != null)
                    && (!app.cdmaOtaProvisionData.isOtaCallIntentProcessed)) {
                app.cdmaOtaProvisionData.isOtaCallIntentProcessed = true;
                app.cdmaOtaScreenState.otaScreenState =
                        CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION;
            }
            return InCallInitStatus.SUCCESS;
        } else if (action.equals(Intent.ACTION_ANSWER)) {
            internalAnswerCall();
            app.setRestoreMuteOnInCallResume(false);
            return InCallInitStatus.SUCCESS;
        } else if (action.equals(Intent.ACTION_CALL)
                || action.equals(Intent.ACTION_CALL_EMERGENCY)) {
            app.setRestoreMuteOnInCallResume(false);

            // If a provider is used, extract the info to build the
            // overlay and route the call.  The overlay will be
            // displayed the first time updateScreen is called.
            if (PhoneUtils.hasPhoneProviderExtras(intent)) {
                mProviderLabel = PhoneUtils.getProviderLabel(this, intent);
                mProviderIcon = PhoneUtils.getProviderIcon(this, intent);

                mProviderGatewayUri = PhoneUtils.getProviderGatewayUri(intent);
                mProviderAddress = PhoneUtils.formatProviderUri(mProviderGatewayUri);
                mProviderOverlayVisible = true;

                if (TextUtils.isEmpty(mProviderLabel) || null == mProviderIcon ||
                    null == mProviderGatewayUri || TextUtils.isEmpty(mProviderAddress)) {
                    clearProvider();
                }
            } else {
                clearProvider();
            }
            InCallInitStatus status = placeCall(intent);
            if (status == InCallInitStatus.SUCCESS) {
                // Notify the phone app that a call is beginning so it can
                // enable the proximity sensor
                app.setBeginningCall(true);
            }
            return status;

        } else if (action.equals(intent.ACTION_MAIN)) {
            // The MAIN action is used to bring up the in-call screen without
            // doing any other explicit action, like when you return to the
            // current call after previously bailing out of the in-call UI.
            // SHOW_DIALPAD_EXTRA can be used here to specify whether the DTMF
            // dialpad should be initially visible.  If the extra isn't
            // present at all, we just leave the dialpad in its previous state.

            if ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL)
                    || (mInCallScreenMode == InCallScreenMode.OTA_ENDED)) {
                // If in OTA Call, update the OTA UI
                updateScreen();
                return InCallInitStatus.SUCCESS;
            }
            if (intent.hasExtra(SHOW_DIALPAD_EXTRA)) {
                boolean showDialpad = intent.getBooleanExtra(SHOW_DIALPAD_EXTRA, false);
                if (VDBG) log("- internalResolveIntent: SHOW_DIALPAD_EXTRA: " + showDialpad);
                if (showDialpad) {
                    mDialer.openDialer(false);  // no "opening" animation
                } else {
                    mDialer.closeDialer(false);  // no "closing" animation
                }
            }
            return InCallInitStatus.SUCCESS;
        } else if (action.equals(ACTION_UNDEFINED)) {
            return InCallInitStatus.SUCCESS;
// LGE_AUTO_REDIAL START
        } else if (action.equals(RedialCallHandler.REDIAL_CALL_RETRY_FINISH)) {
            repeat = 1;
            disable_log = false;
            final Intent call_intent = PhoneApp.createCallLogIntent();
            startActivity(call_intent);
            finish();
            return InCallInitStatus.SUCCESS;
// LGE_AUTO_REDIAL END
        } else {
            Log.w(LOG_TAG, "internalResolveIntent: unexpected intent action: " + action);
            // But continue the best we can (basically treating this case
            // like ACTION_MAIN...)
            return InCallInitStatus.SUCCESS;
        }
    }

    private void stopTimer() {
        if (mCallCard != null) mCallCard.stopTimer();
    }

    private void initInCallScreen() {
        if (VDBG) log("initInCallScreen()...");

        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        // Run in a 32-bit window, which improves the appearance of some
        // semitransparent artwork in the in-call UI (like the CallCard
        // photo borders).
        getWindow().setFormat(PixelFormat.RGBX_8888);

        mMainFrame = (ViewGroup) findViewById(R.id.mainFrame);
        mInCallPanel = (ViewGroup) findViewById(R.id.inCallPanel);

        // Initialize the CallCard.
        mCallCard = (CallCard) findViewById(R.id.callCard);
        if (VDBG) log("  - mCallCard = " + mCallCard);
        mCallCard.setInCallScreenInstance(this);

        // Onscreen touch UI elements (used on some platforms)
        initInCallTouchUi();

        // Helper class to keep track of enabledness/state of UI controls
        mInCallControlState = new InCallControlState(this, mPhone);

        // Helper class to run the "Manage conference" UI
        mManageConferenceUtils = new ManageConferenceUtils(this, mPhone);
// LGE_SS_NOTIFY START
        ss_notify = null;
// LGE_SS_NOTIFY END
    }

    /**
     * Returns true if the phone is "in use", meaning that at least one line
     * is active (ie. off hook or ringing or dialing).  Conversely, a return
     * value of false means there's currently no phone activity at all.
     */
    private boolean phoneIsInUse() {
        return mPhone.getState() != Phone.State.IDLE;
    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {
        if (VDBG) log("handleDialerKeyDown: keyCode " + keyCode + ", event " + event + "...");

        // As soon as the user starts typing valid dialable keys on the
        // keyboard (presumably to type DTMF tones) we start passing the
        // key events to the DTMFDialer's onDialerKeyDown.  We do so
        // only if the okToDialDTMFTones() conditions pass.
        if (okToDialDTMFTones()) {
            return mDialer.onDialerKeyDown(event);

            // TODO: If the dialpad isn't currently visible, maybe
            // consider automatically bringing it up right now?
            // (Just to make sure the user sees the digits widget...)
            // But this probably isn't too critical since it's awkward to
            // use the hard keyboard while in-call in the first place,
            // especially now that the in-call UI is portrait-only...
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

     public boolean onSubBackPressed()
     {
             if (DBG) log("onBackPressed()...");

        // To consume this BACK press, the code here should just do
        // something and return.  Otherwise, call super.onBackPressed() to
        // get the default implementation (which simply finishes the
        // current activity.)
        
        // LGE_MERGE_S
        // 20100710 withwind.park sliding sms control
//20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
        if (this.mInCallTouchUi.mInCallMsg.getMode() == InCallMessageView.MODE_UNLOCK
        	&& mInCallTouchUi.mInCallMsg.isShown())
        {
        	mInCallTouchUi.mInCallMsg.updateState(InCallMessageView.ST_HIDE);
        	updateInCallTouchUi();
        	return false;
        }
   
        //20101024 sumi920.kim@lge.com porting [START_LGE_LAB1]
        // jaeyoung.ha@lge.com 2010.10.12 : Case2) BackPress key in MODE_LOCK => ST_LOCK_VIEW : QM1 45046
        else if (this.mInCallTouchUi.mInCallMsg.getMode() == InCallMessageView.MODE_LOCK
        		&& mInCallTouchUi.mInCallMsg.isShown())
        {
        	log("HJY MODE_LOCK");        
        	mInCallTouchUi.mInCallMsg.updateState(InCallMessageView.ST_LOCK_VIEW);
        	updateInCallTouchUi();
        	return false;
        }
        //20100723 yongwoon.choi@lge.com Lock/UnLock Incoming Scenario [END_LGE]
        //20101024 sumi920.kim@lge.com porting [START_LGE_LAB1]
        if (!mRingingCall.isIdle()) {
            // While an incoming call is ringing, BACK behaves just like
            // ENDCALL: it stops the ringing and rejects the current call.
            // (This is only enabled on some platforms, though.)
            if (getResources().getBoolean(R.bool.allow_back_key_to_reject_incoming_call)) {
                if (DBG) log("BACK key while ringing: reject the call");
                internalHangupRingingCall();

                // Don't consume the key; instead let the BACK event *also*
                // get handled normally by the framework (which presumably
                // will cause us to exit out of this activity.)
                return true;
            } else {
                // The BACK key is disabled; don't reject the call, but
                // *do* consume the keypress (otherwise we'll exit out of
                // this activity.)
                if (DBG) log("BACK key while ringing: ignored");
                return false;
            }
        }

        // BACK is also used to exit out of any "special modes" of the
        // in-call UI:

        if (mDialer.isOpened()) {
            // Take down the "touch lock" overlay *immediately* to let the
            // user clearly see the DTMF dialpad's closing animation.
            enableTouchLock(false);

            mDialer.closeDialer(true);  // do the "closing" animation
            return false;
        }

        if (mInCallScreenMode == InCallScreenMode.MANAGE_CONFERENCE) {
            // Hide the Manage Conference panel, return to NORMAL mode.
            setInCallScreenMode(InCallScreenMode.NORMAL);
            return false;
        }

        // Nothing special to do.  Fall back to the default behavior.
        return true;
    }

    /**
     * Handles the green CALL key while in-call.
     * @return true if we consumed the event.
     */
    private boolean handleCallKey() {
        // The green CALL button means either "Answer", "Unhold", or
        // "Swap calls", or can be a no-op, depending on the current state
        // of the Phone.

        final boolean hasRingingCall = !mRingingCall.isIdle();
        final boolean hasActiveCall = !mForegroundCall.isIdle();
        final boolean hasHoldingCall = !mBackgroundCall.isIdle();

        int phoneType = mPhone.getPhoneType();
        if (phoneType == Phone.PHONE_TYPE_CDMA) {
            // The green CALL button means either "Answer", "Swap calls/On Hold", or
            // "Add to 3WC", depending on the current state of the Phone.

            PhoneApp app = PhoneApp.getInstance();
            CdmaPhoneCallState.PhoneCallState currCallState =
                app.cdmaPhoneCallState.getCurrentCallState();
            if (hasRingingCall) {
                //Scenario 1: Accepting the First Incoming and Call Waiting call
                if (DBG) log("answerCall: First Incoming and Call Waiting scenario");
                internalAnswerCall();  // Automatically holds the current active call,
                                       // if there is one
            } else if ((currCallState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                    && (hasActiveCall)) {
                //Scenario 2: Merging 3Way calls
                if (DBG) log("answerCall: Merge 3-way call scenario");
                // Merge calls
                PhoneUtils.mergeCalls(mPhone);
            } else if (currCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                //Scenario 3: Switching between two Call waiting calls or drop the latest
                // connection if in a 3Way merge scenario
                if (DBG) log("answerCall: Switch btwn 2 calls scenario");
                internalSwapCalls();
            }
        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
            if (hasRingingCall) {
                // If an incoming call is ringing, the CALL button is actually
                // handled by the PhoneWindowManager.  (We do this to make
                // sure that we'll respond to the key even if the InCallScreen
                // hasn't come to the foreground yet.)
                //
                // We'd only ever get here in the extremely rare case that the
                // incoming call started ringing *after*
                // PhoneWindowManager.interceptKeyTq() but before the event
                // got here, or else if the PhoneWindowManager had some
                // problem connecting to the ITelephony service.
                Log.w(LOG_TAG, "handleCallKey: incoming call is ringing!"
                      + " (PhoneWindowManager should have handled this key.)");
                // But go ahead and handle the key as normal, since the
                // PhoneWindowManager presumably did NOT handle it:

                // There's an incoming ringing call: CALL means "Answer".
                internalAnswerCall();
            } else if (hasActiveCall && hasHoldingCall) {
                // Two lines are in use: CALL means "Swap calls".
                if (DBG) log("handleCallKey: both lines in use ==> swap calls.");
                internalSwapCalls();
            } else if (hasHoldingCall) {
                // There's only one line in use, AND it's on hold.
                // In this case CALL is a shortcut for "unhold".
                if (DBG) log("handleCallKey: call on hold ==> unhold.");
                PhoneUtils.switchHoldingAndActive(mPhone);  // Really means "unhold" in this state
            } else {
                // The most common case: there's only one line in use, and
                // it's an active call (i.e. it's not on hold.)
                // In this case CALL is a no-op.
                // (This used to be a shortcut for "add call", but that was a
                // bad idea because "Add call" is so infrequently-used, and
                // because the user experience is pretty confusing if you
                // inadvertently trigger it.)
                if (VDBG) log("handleCallKey: call in foregound ==> ignoring.");
                // But note we still consume this key event; see below.
            }
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }

        // We *always* consume the CALL key, since the system-wide default
        // action ("go to the in-call screen") is useless here.
        return true;
    }

//20100916 younkyung.jang@lge.com Earjack hook key scenario [START_LGE]
    private boolean handleShortHookKey() {

        final boolean hasRingingCall = !mRingingCall.isIdle();
        final boolean hasActiveCall = !mForegroundCall.isIdle();
        final boolean hasHoldingCall = !mBackgroundCall.isIdle();

        int phoneType = mPhone.getPhoneType();
        if (phoneType == Phone.PHONE_TYPE_CDMA) {                
            if (hasRingingCall) {
                internalAnswerCall();
				if (DBG) log("ykjang earjack scene : handleShortHookKey: CDMAPhone answer call");
            }         
            else if (hasActiveCall) {
                internalHangup();
				if (DBG) log("ykjang earjack scene : handleShortHookKey: CDMAPhone end call");
            }  
        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
        	//20101107 sumi920.kim@lge.com Add Hook Key handle [START_LGE_LAB1]
        	if(StarConfig.COUNTRY.equals("KR"))
        	{
        		if (hasRingingCall) {
        			internalAnswerCall();
        			if (DBG) log("lab1 earjack scene : handleShortHookKey: GSMPhone answer call");
        		}
        		else if(hasActiveCall && hasHoldingCall){ //3way call
        			if (DBG) log("lab1 earjack scene : handleShortHookKey: swap call");
        			internalSwapCalls();
        		}
        		else {
        			if (DBG) log("lab1 earjack scene : handleShortHookKey: end call");
        			handleOnscreenButtonClick(R.id.endButton);
        		}
            }
        	else
        	//20101107 sumi920.kim@lge.com Add Hook Key handle [END_LGE_LAB1]
        	{
        		if (hasRingingCall) {
        			internalAnswerCall();
        			if (DBG) log("ykjang earjack scene : handleShortHookKey: GSMPhone answer call");
        		}
        		else if (hasActiveCall) {
        			internalHangup();
        			if (DBG) log("ykjang earjack scene : handleShortHookKey: GSMPhone end call");
        		}
        	}
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }
        return true;
    }

    private boolean handleLongHookKey() {
        final boolean hasRingingCall = !mRingingCall.isIdle();
        final boolean hasActiveCall = !mForegroundCall.isIdle();
        final boolean hasHoldingCall = !mBackgroundCall.isIdle();
		final Call.State state = mRingingCall.getState();
			
        int phoneType = mPhone.getPhoneType();
        if (phoneType == Phone.PHONE_TYPE_CDMA) {           
            if (state == Call.State.INCOMING) {
                internalHangupRingingCall();
				if (DBG) log("ykjang earjack scene : internalHangupRingingCall: CDMAPhone end call");
            } else if (state == Call.State.WAITING) {
                internalAnswerAndEnd();
				if (DBG) log("ykjang earjack scene : internalAnswerAndEnd: CDMAPhone end call");
            } else if ((hasActiveCall && !hasHoldingCall) ||(!hasActiveCall && hasHoldingCall)) {
                internalHangup();

				if (DBG) log("ykjang earjack scene : internalHangup: CDMAPhone end call");
            } else if (hasActiveCall && hasHoldingCall) {
                internalHangup();
				if (DBG) log("ykjang earjack scene : internalHangup: CDMAPhone end call");
            } else {
                if (VDBG) log("handleCallKey: call in foregound ==> ignoring.");
            }
        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
        	//20101107 sumi920.kim@lge.com Add Hook Key handle [START_LGE_LAB1]
        	if(StarConfig.COUNTRY.equals("KR")){
        		if (DBG) log("lab1 earjack scene : handleLongHookKey1: ");
        		if (hasRingingCall) {
        			if (DBG) log("lab1 earjack scene : handleLongHookKey1: reject call");
        			internalHangupRingingCall();
        		}
        		else if(hasActiveCall && hasHoldingCall){
        			if (DBG) log("lab1 earjack scene : handleLongHookKey1: End active call");
        			internalHangup();
        		}
        		else{
        			if (DBG) log("lab1 earjack scene : handleLongHookKey1: End call");
        			internalHangup();
        		}
        	}
        	else        	
        	//20101107 sumi920.kim@lge.com Add Hook Key handle [END_LGE_LAB1]
        	{
        		if (state == Call.State.INCOMING) {
        			internalHangupRingingCall();
        			if (DBG) log("ykjang earjack scene : internalHangupRingingCall: GSMPhone end call");
        		} else if (state == Call.State.WAITING) {
        			internalAnswerAndEnd();
        			if (DBG) log("ykjang earjack scene : internalAnswerAndEnd: GSMPhone end call");
        		} else if ((hasActiveCall && !hasHoldingCall) ||(!hasActiveCall && hasHoldingCall)) {
        			internalHangup();
        			if (DBG) log("ykjang earjack scene : onHoldClick: GSMPhone end call");
        		} else if (hasActiveCall && hasHoldingCall) {
        			internalHangup();
        			if (DBG) log("ykjang earjack scene : internalHangup: GSMPhone end call");
        		} else {
        			if (VDBG) log("handleCallKey: call in foregound ==> ignoring.");
        		}
        	}
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }

        return true;
    }
//20100916 younkyung.jang@lge.com Earjack hook key scenario [END_LGE]

    boolean isKeyEventAcceptableDTMF (KeyEvent event) {
        return (mDialer != null && mDialer.isKeyEventAcceptable(event));
    }

    /**
     * Overriden to track relevant focus changes.
     *
     * If a key is down and some time later the focus changes, we may
     * NOT recieve the keyup event; logically the keyup event has not
     * occured in this window.  This issue is fixed by treating a focus
     * changed event as an interruption to the keydown, making sure
     * that any code that needs to be run in onKeyUp is ALSO run here.
     *
     * Note, this focus change event happens AFTER the in-call menu is
     * displayed, so mIsMenuDisplayed should always be correct by the
     * time this method is called in the framework, please see:
     * {@link onCreatePanelView}, {@link onOptionsMenuClosed}
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // the dtmf tones should no longer be played
        if (VDBG) log("onWindowFocusChanged(" + hasFocus + ")...");
        if (!hasFocus && mDialer != null) {
            if (VDBG) log("- onWindowFocusChanged: faking onDialerKeyUp()...");
            mDialer.onDialerKeyUp(null);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    public boolean dispatchSubKeyEvent(KeyEvent event)
    {
           // if (DBG) log("dispatchKeyEvent(event " + event + ")...");

        // Intercept some events before they get dispatched to our views.
        int keyCode = event.getKeyCode();
		log("dispatchKeyEvent : keyCode="+keyCode);
		if (mDialer != null) {
        	switch (keyCode) {
	            case KeyEvent.KEYCODE_DPAD_CENTER:
	            case KeyEvent.KEYCODE_DPAD_UP:
	            case KeyEvent.KEYCODE_DPAD_DOWN:
	            case KeyEvent.KEYCODE_DPAD_LEFT:
	            case KeyEvent.KEYCODE_DPAD_RIGHT:
	                // Disable DPAD keys and trackball clicks if the touch lock
	                // overlay is up, since "touch lock" really means "disable
	                // the DTMF dialpad" (rather than only disabling touch events.)
	                if (mDialer.isOpened() && isTouchLocked()) {
	                    if (DBG) log("- ignoring DPAD event while touch-locked...");
	                    return true;
	                }
	                break;

	            default:
	                break;
	        }
		}
		else 
			log("[ERROR] dispatchKeyEvent : mDialer is NULL");

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

   public boolean onSubKeyUp(int keyCode, KeyEvent event)
   {
           // if (DBG) log("onKeyUp(keycode " + keyCode + ")...");

        // push input to the dialer.
        if ((mDialer != null) && (mDialer.onDialerKeyUp(event))){
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
            // Always consume CALL to be sure the PhoneWindow won't do anything with it
            return true;
        }
//20100916 younkyung.jang@lge.com Earjack hook key scenario [START_LGE]
		else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
			if (!isLongHookKeyDown) {
				boolean hookhandled = handleShortHookKey();
                if (!hookhandled) {
                    Log.w(LOG_TAG, "InCallScreen should always handle KEYCODE_HEADSETHOOK in onKeyUp");
                }
				return true;
			}
			isLongHookKeyDown = false;
		}
//20100916 younkyung.jang@lge.com Earjack hook key scenario [END_LGE]

        // 20101019 sumi920.kim@lge.com for Home Key In Alerting [START_LGE_LAB1]
		else if (keyCode == KeyEvent.KEYCODE_HOME )
		{	
				// Placeholder for other misc temp testing
				log("------------ KEYCODE_HOME KEY UP -----------------");
				if(StarConfig.COUNTRY.equals("KR"))
				{
					if(mPhone.getState() == Phone.State.RINGING)
					{
						int answerMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ANSWER_SETTING, 0);
						if(answerMode == 1)
						{
							log("mIgnoreHomeKey.= " + mIgnoreHomeKey);
							if(mIgnoreHomeKey == true)
								mIgnoreHomeKey = false;
							else
							{
								mHandler.removeMessages(HOME_KEYUP_IGNORE_RINGING_STATE);
								internalAnswerCall();
							}
						}
					}
				}
		
		}
        // 20101019 sumi920.kim@lge.com for Home Key In Alerting [END_LGE_LAB1]

        return false;	//super.onKeyUp(keyCode, event);
    }
//20100916 younkyung.jang@lge.com Earjack hook key scenario [START_LGE]
	private boolean isLongHookKeyDown = false;
	
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    	log("onKeyLongPress(keycode " + keyCode + ")...");
        switch (keyCode) {
			case KeyEvent.KEYCODE_HEADSETHOOK:
				boolean hookhandled = handleLongHookKey();
				isLongHookKeyDown = true;
                if (!hookhandled) {
                    Log.w(LOG_TAG, "InCallScreen should always handle KEYCODE_HEADSETHOOK in onKeyLongPress");
                }
				return true;
		}				

		return false; //super.onKeyLongPress(keyCode, event);
    }
//20100916 younkyung.jang@lge.com Earjack hook key scenario [END_LGE]

//20100804 jongwany.lee@lge.com attached it for CALL UI
    private boolean inCallControl_GONE_Flag = false;
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    public boolean onSubKeyDown(int keyCode, KeyEvent event)
   {
        // if (DBG) log("onKeyDown(keycode " + keyCode + ")...");

        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                boolean handled = handleCallKey();
                if (!handled) {
                    Log.w(LOG_TAG, "InCallScreen should always handle KEYCODE_CALL in onKeyDown");
                }
                // Always consume CALL to be sure the PhoneWindow won't do anything with it
                return true;

//20100916 younkyung.jang@lge.com Earjack hook key scenario [START_LGE]
			case KeyEvent.KEYCODE_HEADSETHOOK:
				event.startTracking();
				return true;
//20100916 younkyung.jang@lge.com Earjack hook key scenario [END_LGE]
            // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
            // The standard system-wide handling of the ENDCALL key
            // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
            // already implements exactly what the UI spec wants,
            // namely (1) "hang up" if there's a current active call,
            // or (2) "don't answer" if there's a current ringing call.

            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mPhone.getState() == Phone.State.RINGING) {
                    // If an incoming call is ringing, the VOLUME buttons are
                    // actually handled by the PhoneWindowManager.  (We do
                    // this to make sure that we'll respond to them even if
                    // the InCallScreen hasn't come to the foreground yet.)
                    //
                    // We'd only ever get here in the extremely rare case that the
                    // incoming call started ringing *after*
                    // PhoneWindowManager.interceptKeyTq() but before the event
                    // got here, or else if the PhoneWindowManager had some
                    // problem connecting to the ITelephony service.
                    Log.w(LOG_TAG, "VOLUME key: incoming call is ringing!"
                          + " (PhoneWindowManager should have handled this key.)");
                    // But go ahead and handle the key as normal, since the
                    // PhoneWindowManager presumably did NOT handle it:

                    final CallNotifier notifier = PhoneApp.getInstance().notifier;
                    if (notifier.isRinging()) {
                        // ringer is actually playing, so silence it.
                        PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_IDLE);
                        if (DBG) log("VOLUME key: silence ringer");
                        notifier.silenceRinger();
                    }

                    // As long as an incoming call is ringing, we always
                    // consume the VOLUME keys.
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_MENU:
                // Special case for the MENU key: if the "touch lock"
                // overlay is up (over the DTMF dialpad), allow MENU to
                // dismiss the overlay just as if you had double-tapped
                // the onscreen icon.
                // (We do this because MENU is normally used to bring the
                // UI back after the screen turns off, and the touch lock
                // overlay "feels" very similar to the screen going off.
                // This is also here to be "backward-compatibile" with the
                // 1.0 behavior, where you *needed* to hit MENU to bring
                // back the dialpad after 6 seconds of idle time.)
                if (mDialer.isOpened() && isTouchLocked()) {
                    if (VDBG) log("- allowing MENU to dismiss touch lock overlay...");
                    // Take down the touch lock overlay, but post a
                    // message in the future to bring it back later.
                    enableTouchLock(false);
                    resetTouchLockTimer();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_MUTE:
                PhoneUtils.setMute(mPhone, !PhoneUtils.getMute(mPhone));
                return true;

            // Various testing/debugging features, enabled ONLY when VDBG == true.
            case KeyEvent.KEYCODE_SLASH:
                if (VDBG) {
                    log("----------- InCallScreen View dump --------------");
                    // Dump starting from the top-level view of the entire activity:
                    Window w = this.getWindow();
                    View decorView = w.getDecorView();
                    decorView.debug();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_EQUALS:
                if (VDBG) {
                    log("----------- InCallScreen call state dump --------------");
                    PhoneUtils.dumpCallState(mPhone);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_GRAVE:
                if (VDBG) {
                    // Placeholder for other misc temp testing
                    log("------------ Temp testing -----------------");
                    return true;
                }
                break;
            // 20101019 sumi920.kim@lge.com for Home Key In Alerting [START_LGE_LAB1]
            case KeyEvent.KEYCODE_HOME:
            	log("------------ KEYCODE_HOME -----------------");
            	if(StarConfig.COUNTRY.equals("KR"))
            	{
            		if((mPhone.getState() == Phone.State.RINGING) &&
            			(event.getRepeatCount() == 0))
            				
            		{            			
            			int answerMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ANSWER_SETTING, 0);
            			if(answerMode == 1)
            				sendHomeKeyUpDelayTime();
            		}
            		return true;
            	}
            	break;
           // 20101019 sumi920.kim@lge.com for Home Key In Alerting [END_LGE_LAB1]
                
        }

        if (event.getRepeatCount() == 0 && handleDialerKeyDown(keyCode, event)) {
            return true;
        }

        return false;
    }

    /**
     * Handle a failure notification for a supplementary service
     * (i.e. conference, switch, separate, transfer, etc.).
     */
    void onSuppServiceFailed(AsyncResult r) {
        Phone.SuppService service = (Phone.SuppService) r.result;
        if (DBG) log("onSuppServiceFailed: " + service);

        //20101107 sumi920.kim@lge.com  Control state swiching(Hold/Active)[START_LGE_LAB1]
    	if(StarConfig.COUNTRY.equals("KR"))
    	{
    		if(bSendingSuppService)
    		{
    			if (DBG) log("onSuppServiceFailed: setSendingSuppService... false");
    			bSendingSuppService = false;
    		}
    	}
        //20101107 sumi920.kim@lge.com  Control state swiching(Hold/Active)[END_LGE_LAB1]
        int errorMessageResId;
        switch (service) {
            case SWITCH:
                // Attempt to switch foreground and background/incoming calls failed
                // ("Failed to switch calls")
                errorMessageResId = R.string.incall_error_supp_service_switch;
                break;

            case SEPARATE:
                // Attempt to separate a call from a conference call
                // failed ("Failed to separate out call")
                errorMessageResId = R.string.incall_error_supp_service_separate;
                break;

            case TRANSFER:
                // Attempt to connect foreground and background calls to
                // each other (and hanging up user's line) failed ("Call
                // transfer failed")
                errorMessageResId = R.string.incall_error_supp_service_transfer;
                break;

            case CONFERENCE:
                // Attempt to add a call to conference call failed
                // ("Conference call failed")
                errorMessageResId = R.string.incall_error_supp_service_conference;
                break;

            case REJECT:
                // Attempt to reject an incoming call failed
                // ("Call rejection failed")
                errorMessageResId = R.string.incall_error_supp_service_reject;
                break;

            case HANGUP:
                // Attempt to release a call failed ("Failed to release call(s)")
                errorMessageResId = R.string.incall_error_supp_service_hangup;
                break;
// LGE_CALL_DEFLECTION START
            case DEFLECTION:
                 // ("Call Deflection failed")
                  log("onSuppServiceFailed: Call Deflection failed");
                  errorMessageResId = R.string.incall_error_supp_service_deflection;
                  break;
// LGE_CALL_DEFLECTION END
            case UNKNOWN:
            default:
                // Attempt to use a service we don't recognize or support
                // ("Unsupported service" or "Selected service failed")
                errorMessageResId = R.string.incall_error_supp_service_unknown;
                break;
        }

        // mSuppServiceFailureDialog is a generic dialog used for any
        // supp service failure, and there's only ever have one
        // instance at a time.  So just in case a previous dialog is
        // still around, dismiss it.
        if (mSuppServiceFailureDialog != null) {
            if (DBG) log("- DISMISSING mSuppServiceFailureDialog.");
            mSuppServiceFailureDialog.dismiss();  // It's safe to dismiss() a dialog
                                                  // that's already dismissed.
            mSuppServiceFailureDialog = null;
        }

        mSuppServiceFailureDialog = new AlertDialog.Builder(this)
                .setMessage(errorMessageResId)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true)
                .create();
        mSuppServiceFailureDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mSuppServiceFailureDialog.show();
    }

    /**
     * Something has changed in the phone's state.  Update the UI.
     */
    private void onPhoneStateChanged(AsyncResult r) {
        if (DBG) log("onPhoneStateChanged()...");
    	//hyojin.an 101020 start
//    	Phone.State state = mPhone.getState();
//    	//Call fgCall = mPhone.getForegroundCall();
//    	final Call.State fgCallState = mForegroundCall.getState();
//    	final Call.State bgCallState = mBackgroundCall.getState();
//    	
//    	
//        if (DBG) log("onPhoneStateChanged: state = " + state + " oldstate = " + oldstate);
        
//        if (state == oldstate 
//        		//&& bgCallState == Call.State.IDLE
//        		//&& (!PhoneUtils.isConferenceCall(fgCall))
//        		){    	
//        	if(fgCallState == oldfgCallState && bgCallState == oldbgCallState){
//        		if (DBG) log("onPhoneStateChanged->Return: fgCallState = " + fgCallState + "oldfgCallState = " + oldfgCallState + " bgCallState = " + bgCallState + " oldbgCallState = " + oldfgCallState);
//        		return;
//        	}
//        }
//        
//        oldstate = state;
//        oldfgCallState = fgCallState;
//        oldbgCallState = bgCallState;
      //hyojin.an 101020 end
        
        //20101107 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
        // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/12 turn off the screen when a call is made with earjack/BT
        if (((mPhone.getForegroundCall().getState() == Call.State.ACTIVE)
        		&& ((mPreviousCallState == Call.State.DIALING)
        		||  (mPreviousCallState == Call.State.ALERTING)))
        	|| ((mPhone.getState() == Phone.State.OFFHOOK)
        			&& (mPreviousPhoneState == Phone.State.RINGING))) {
        	// LGE_S joy.park 2010/07/15, patch for BT availability check in call
        	if (PhoneApp.getInstance().isHeadsetPlugged() ||  isBluetoothAvailable()){
                PhoneApp.getInstance().goToSleep();
        	}
        }
        mPreviousCallState = mPhone.getForegroundCall().getState();
        mPreviousPhoneState = mPhone.getState();
        // LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/12 turn off the screen when a call is made with earjack/BT
        //20101107 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]
        
        // There's nothing to do here if we're not the foreground activity.
        // (When we *do* eventually come to the foreground, we'll do a
        // full update then.)
        if (!mIsForegroundActivity) {
        	
        	//20101107 sumi920.kim@lge.com Thunder porting [LGE_LAB1]
        	// LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/12 turn off the screen when a call is made with earjack/BT
        	mPreviousCallState = mPhone.getForegroundCall().getState();
        	// LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/12 turn off the screen when a call is made with earjack/BT

            if (DBG) log("onPhoneStateChanged: Activity not in foreground! Bailing out...");
            //STAR CALL AHJ 100828 return;
        }
        
        //20101107 sumi920.kim@lge.com  Control state swiching(Hold/Active)[START_LGE_LAB1]
    	if(StarConfig.COUNTRY.equals("KR"))
    	{
    		if(bSendingSuppService)
    		{
    			if (DBG) log("onPhoneStateChanged: setSendingSuppService... false");
    			bSendingSuppService = false;
    		}
    	}
        //20101107 sumi920.kim@lge.com  Control state swiching(Hold/Active)[END_LGE_LAB1]

// LGE_CALL_TRANSFER START
        if (SimpleCallDialog.mIsColdTransfer) {
            if (mPhone.canTransfer()) {
                SimpleCallDialog.mIsColdTransfer = false;
                Toast.makeText(InCallScreen.this,R.string.call_transfer_in_progress,Toast.LENGTH_LONG).show();        
            }
        }
// LGE_CALL_TRANSFER END
        updateScreen();

        // Make sure we update the poke lock and wake lock when certain
        // phone state changes occur.
        PhoneApp.getInstance().updateWakeState();
        
        //updateScreen(); //hyojin.an 101021
        
    }

    /**
     * Updates the UI after a phone connection is disconnected, as follows:
     *
     * - If this was a missed or rejected incoming call, and no other
     *   calls are active, dismiss the in-call UI immediately.  (The
     *   CallNotifier will still create a "missed call" notification if
     *   necessary.)
     *
     * - With any other disconnect cause, if the phone is now totally
     *   idle, display the "Call ended" state for a couple of seconds.
     *
     * - Or, if the phone is still in use, stay on the in-call screen
     *   (and update the UI to reflect the current state of the Phone.)
     *
     * @param r r.result contains the connection that just ended
     */
    private void onDisconnect(AsyncResult r) {
// LGE_SS_NOTIFY START
        ss_notify = null;
// LGE_SS_NOTIFY END
        Connection c = (Connection) r.result;
        Connection.DisconnectCause cause = c.getDisconnectCause();
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] -->
//<!--[younkyung.jang@lge.com] 2010.11.05 apply AUTO_CALL_ANSWER to global [START] -->
//	if(StarConfig.COUNTRY.equals("KR"))	
//	{
		boolean bAutocall = PhoneUtils.isAutoCallTest(); 
		if(bAutocall){
			autoCallCause = cause;
		}
//	}
//<!--[younkyung.jang@lge.com] 2010.11.05 apply AUTO_CALL_ANSWER to global [END] -->
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->			
        //jundj@mo2.co.kr start
        if(VEUtils.isSKTFeature()){
            VEUtils.setVEPlayerStatus(IsVEPlayerRunning.NO);
            VEUtils.setVisibleCallCardPersonInfo(VEUtils.CALLCARD_PERSONINFO_DEFAULT);
            VE_ContentManager.getHandler().sendEmptyMessage(VE_ContentManager.HANDLE_MSG_STOP_PLAY);
        }
        //jundj@mo2.co.kr end
        if (DBG) log("onDisconnect: " + c + ", cause=" + cause);
// LGE_AUTO_REDIAL START
        dialNumber = c.getAddress();
// LGE_AUTO_REDIAL END
        boolean currentlyIdle = !phoneIsInUse();
        int autoretrySetting = AUTO_RETRY_OFF;
        boolean phoneIsCdma = (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA);
//<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]	 -->   
        if(StarConfig.COUNTRY.equals("KR"))
        {
	        if(PhoneUtils.isSoundRecording())
	        {
	        	mSoundRecord.stopRecording(InCallScreen.this);
	        	PhoneUtils.setSoundRecording(false);
	        }
        }
//<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]	 -->        
        if (phoneIsCdma) {
            // Get the Auto-retry setting only if Phone State is IDLE,
            // else let it stay as AUTO_RETRY_OFF
            if (currentlyIdle) {
                autoretrySetting = android.provider.Settings.System.getInt(mPhone.getContext().
                        getContentResolver(), android.provider.Settings.System.CALL_AUTO_RETRY, 0);
            }
        }

        // for OTA Call, only if in OTA NORMAL mode, handle OTA END scenario
        final PhoneApp app = PhoneApp.getInstance();
        if ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL)
                && ((app.cdmaOtaProvisionData != null)
                && (!app.cdmaOtaProvisionData.inOtaSpcState))) {
            setInCallScreenMode(InCallScreenMode.OTA_ENDED);
            updateScreen();
            return;
        } else if ((mInCallScreenMode == InCallScreenMode.OTA_ENDED)
                || ((app.cdmaOtaProvisionData != null) && app.cdmaOtaProvisionData.inOtaSpcState)) {
           if (DBG) log("onDisconnect: OTA Call end already handled");
           return;
        }

        // Any time a call disconnects, clear out the "history" of DTMF
        // digits you typed (to make sure it doesn't persist from one call
        // to the next.)
        mDialer.clearDigits();

        // Under certain call disconnected states, we want to alert the user
        // with a dialog instead of going through the normal disconnect
        // routine.
        if (cause == Connection.DisconnectCause.CALL_BARRED) {
            showGenericErrorDialog(R.string.callFailed_cb_enabled, false);
            return;
        } else if (cause == Connection.DisconnectCause.FDN_BLOCKED) {
            showGenericErrorDialog(R.string.callFailed_fdn_only, false);
            return;
        } else if (cause == Connection.DisconnectCause.CS_RESTRICTED) {
            showGenericErrorDialog(R.string.callFailed_dsac_restricted, false);
            return;
        } else if (cause == Connection.DisconnectCause.CS_RESTRICTED_EMERGENCY) {
            showGenericErrorDialog(R.string.callFailed_dsac_restricted_emergency, false);
            return;
        } else if (cause == Connection.DisconnectCause.CS_RESTRICTED_NORMAL) {
            showGenericErrorDialog(R.string.callFailed_dsac_restricted_normal, false);
            return;
        }

        if (phoneIsCdma) {
            Call.State callState = PhoneApp.getInstance().notifier.getPreviousCdmaCallState();
            if ((callState == Call.State.ACTIVE)
                    && (cause != Connection.DisconnectCause.INCOMING_MISSED)
                    && (cause != Connection.DisconnectCause.NORMAL)
                    && (cause != Connection.DisconnectCause.LOCAL)
                    && (cause != Connection.DisconnectCause.INCOMING_REJECTED)) {
                showCallLostDialog();
            } else if ((callState == Call.State.DIALING || callState == Call.State.ALERTING)
                        && (cause != Connection.DisconnectCause.INCOMING_MISSED)
                        && (cause != Connection.DisconnectCause.NORMAL)
                        && (cause != Connection.DisconnectCause.LOCAL)
                        && (cause != Connection.DisconnectCause.INCOMING_REJECTED)) {

                    if (mNeedShowCallLostDialog) {
                        // Show the dialog now since the call that just failed was a retry.
                        showCallLostDialog();
                        mNeedShowCallLostDialog = false;
                    } else {
                        if (autoretrySetting == AUTO_RETRY_OFF) {
                            // Show the dialog for failed call if Auto Retry is OFF in Settings.
                            showCallLostDialog();
                            mNeedShowCallLostDialog = false;
                        } else {
                            // Set the mNeedShowCallLostDialog flag now, so we'll know to show
                            // the dialog if *this* call fails.
                            mNeedShowCallLostDialog = true;
                        }
                    }
            }
        }

        // Explicitly clean up up any DISCONNECTED connections
        // in a conference call.
        // [Background: Even after a connection gets disconnected, its
        // Connection object still stays around for a few seconds, in the
        // DISCONNECTED state.  With regular calls, this state drives the
        // "call ended" UI.  But when a single person disconnects from a
        // conference call there's no "call ended" state at all; in that
        // case we blow away any DISCONNECTED connections right now to make sure
        // the UI updates instantly to reflect the current state.]
        Call call = c.getCall();
        if (call != null) {
            // We only care about situation of a single caller
            // disconnecting from a conference call.  In that case, the
            // call will have more than one Connection (including the one
            // that just disconnected, which will be in the DISCONNECTED
            // state) *and* at least one ACTIVE connection.  (If the Call
            // has *no* ACTIVE connections, that means that the entire
            // conference call just ended, so we *do* want to show the
            // "Call ended" state.)
            List<Connection> connections = call.getConnections();
            if (connections != null && connections.size() > 1) {
                for (Connection conn : connections) {
                    if (conn.getState() == Call.State.ACTIVE) {
                        // This call still has at least one ACTIVE connection!
                        // So blow away any DISCONNECTED connections
                        // (including, presumably, the one that just
                        // disconnected from this conference call.)

                        // We also force the wake state to refresh, just in
                        // case the disconnected connections are removed
                        // before the phone state change.
                        if (VDBG) log("- Still-active conf call; clearing DISCONNECTED...");
                        app.updateWakeState();
                        mPhone.clearDisconnected();  // This happens synchronously.
                        break;
                    }
                }
            }
        }

        // Retrieve the emergency call retry count from this intent, in
        // case we need to retry the call again.
        int emergencyCallRetryCount = getIntent().getIntExtra(
                EmergencyCallHandler.EMERGENCY_CALL_RETRY_KEY,
                EmergencyCallHandler.INITIAL_ATTEMPT);

        // Note: see CallNotifier.onDisconnect() for some other behavior
        // that might be triggered by a disconnect event, like playing the
        // busy/congestion tone.

        // Keep track of whether this call was user-initiated or not.
        // (This affects where we take the user next; see delayedCleanupAfterDisconnect().)
        //20101009 sumi920.kim@lge.com thunder portion [LGE_LAB1]
        // LGE_CHANGE_S [yujung.lee@lge.com] 2010-02-17 : do not Show Call History
        //      mShowCallLogAfterDisconnect = !c.isIncoming();
        mShowCallLogAfterDisconnect = false;
        //LGE_CHANGE_E [yujung.lee@lge.com] 2010-02-17 : do not Show Call History


        // We bail out immediately (and *don't* display the "call ended"
        // state at all) in a couple of cases, including those where we
        // are waiting for the radio to finish powering up for an
        // emergency call:
        boolean bailOutImmediately =
                ((cause == Connection.DisconnectCause.INCOMING_MISSED)
                 || (cause == Connection.DisconnectCause.INCOMING_REJECTED)
                 || ((cause == Connection.DisconnectCause.OUT_OF_SERVICE)
                         && (emergencyCallRetryCount > 0)))
                && currentlyIdle;

        if (bailOutImmediately) {
            if (VDBG) log("- onDisconnect: bailOutImmediately...");
            // Exit the in-call UI!
            // (This is basically the same "delayed cleanup" we do below,
            // just with zero delay.  Since the Phone is currently idle,
            // this call is guaranteed to immediately finish this activity.)
// LGE_AUTO_REDIAL START
            delayedCleanupAfterDisconnect(null);
// LGE_AUTO_REDIAL END
            // Retry the call, by resending the intent to the emergency
            // call handler activity.
            if ((cause == Connection.DisconnectCause.OUT_OF_SERVICE)
                    && (emergencyCallRetryCount > 0)) {
                startActivity(getIntent()
                        .setClassName(this, EmergencyCallHandler.class.getName()));
            }
        } else {
            if (VDBG) log("- onDisconnect: delayed bailout...");
            // Stay on the in-call screen for now.  (Either the phone is
            // still in use, or the phone is idle but we want to display
            // the "call ended" state for a couple of seconds.)

            // Force a UI update in case we need to display anything
            // special given this connection's DisconnectCause (see
            // CallCard.getCallFailedString()).
            updateScreen();

            // Display the special "Call ended" state when the phone is idle
            // but there's still a call in the DISCONNECTED state:
            if (currentlyIdle
                && ((mForegroundCall.getState() == Call.State.DISCONNECTED)
                    || (mBackgroundCall.getState() == Call.State.DISCONNECTED))) {
                if (VDBG) log("- onDisconnect: switching to 'Call ended' state...");
                setInCallScreenMode(InCallScreenMode.CALL_ENDED);
            }

            // Some other misc cleanup that we do if the call that just
            // disconnected was the foreground call.
            final boolean hasActiveCall = !mForegroundCall.isIdle();
            if (!hasActiveCall) {
                if (VDBG) log("- onDisconnect: cleaning up after FG call disconnect...");

                // Dismiss any dialogs which are only meaningful for an
                // active call *and* which become moot if the call ends.
                if (mWaitPromptDialog != null) {
                    if (VDBG) log("- DISMISSING mWaitPromptDialog.");
                    mWaitPromptDialog.dismiss();  // safe even if already dismissed
                    mWaitPromptDialog = null;
                }
                if (mWildPromptDialog != null) {
                    if (VDBG) log("- DISMISSING mWildPromptDialog.");
                    mWildPromptDialog.dismiss();  // safe even if already dismissed
                    mWildPromptDialog = null;
                }
                if (mPausePromptDialog != null) {
                    if (DBG) log("- DISMISSING mPausePromptDialog.");
                    mPausePromptDialog.dismiss();  // safe even if already dismissed
                    mPausePromptDialog = null;
                }
            }

            // Updating the screen wake state is done in onPhoneStateChanged().
            
            //20101023 sumi920.kim@lge.com hold call after 3way end [START_LGE_LAB1]
            //START youngmi.uhm@lge.com 2010. 8. 26. LAB1_CallUI KU3700 Roaming remain hold after 3way end
            if(StarConfig.COUNTRY.equals("KR"))
            {
	            final boolean hasHoldingCall = !mBackgroundCall.isIdle();
	            final boolean hasRingingCall = !mRingingCall.isIdle();

	 			if (VDBG) log("- onDisconnect: hasHoldingCall : " + hasHoldingCall);
	 			if (VDBG) log("- onDisconnect: hasRingingCall : " + hasRingingCall);
				if (VDBG) log("- onDisconnect: getIsTransferCall : " + PhoneUtils.getIsTransferCall());

	            if(!hasActiveCall && hasHoldingCall && !hasRingingCall && (!PhoneUtils.getIsTransferCall())){
	            	//END youngmi.uhm@lge.com 2010. 9. 02. LAB1_CallUI KT IOT err msg after transfer call
	            	if (VDBG) log("- onDisconnect: switching 'Holding' call to 'Active' call ");
	            	//START youngmi.uhm@lge.com 2010. 8. 26. LAB1_CallUI KU3700 Roaming remain hold after 3way end
	            		PhoneUtils.switchHoldingAndActive(mPhone);
	            }
            }
            //END youngmi.uhm@lge.com 2010. 8. 26. LAB1_CallUI KU3700 Roaming remain hold after 3way end
            //20101023 sumi920.kim@lge.com hold call after 3way end [END_LGE_LAB1]
            // CDMA: We only clean up if the Phone state is IDLE as we might receive an
            // onDisconnect for a Call Collision case (rare but possible).
            // For Call collision cases i.e. when the user makes an out going call
            // and at the same time receives an Incoming Call, the Incoming Call is given
            // higher preference. At this time framework sends a disconnect for the Out going
            // call connection hence we should *not* bring down the InCallScreen as the Phone
            // State would be RINGING
            if (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
                if (!currentlyIdle) {
                    // Clean up any connections in the DISCONNECTED state.
                    // This is necessary cause in CallCollision the foreground call might have
                    // connections in DISCONNECTED state which needs to be cleared.
                    mPhone.clearDisconnected();

                    // The phone is still in use.  Stay here in this activity.
                    // But we don't need to keep the screen on.
                    if (DBG) log("onDisconnect: Call Collision case - staying on InCallScreen.");
                    if (DBG) PhoneUtils.dumpCallState(mPhone);
                    return;
                }
            }

            // Finally, arrange for delayedCleanupAfterDisconnect() to get
            // called after a short interval (during which we display the
            // "call ended" state.)  At that point, if the
            // Phone is idle, we'll finish out of this activity.
            int callEndedDisplayDelay = CALL_ENDED_LONG_DELAY;
//                    (cause == Connection.DisconnectCause.LOCAL)
//                    ? CALL_ENDED_SHORT_DELAY : CALL_ENDED_LONG_DELAY;
            mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
// LGE_AUTO_REDIAL START
            Message msg = mHandler.obtainMessage(DELAYED_CLEANUP_AFTER_DISCONNECT,cause);
            mHandler.sendMessageDelayed(msg,callEndedDisplayDelay);
// LGE_AUTO_REDIAL END
        }

        // Remove 3way timer (only meaningful for CDMA)
        mHandler.removeMessages(THREEWAY_CALLERINFO_DISPLAY_DONE);
    }


    /**
     * Brings up the "MMI Started" dialog.
     */
    private void onMMIInitiate(AsyncResult r) {
        if (VDBG) log("onMMIInitiate()...  AsyncResult r = " + r);

        // Watch out: don't do this if we're not the foreground activity,
        // mainly since in the Dialog.show() might fail if we don't have a
        // valid window token any more...
        // (Note that this exact sequence can happen if you try to start
        // an MMI code while the radio is off or out of service.)
        if (!mIsForegroundActivity) {
            if (VDBG) log("Activity not in foreground! Bailing out...");
            return;
        }

        // Also, if any other dialog is up right now (presumably the
        // generic error dialog displaying the "Starting MMI..."  message)
        // take it down before bringing up the real "MMI Started" dialog
        // in its place.
        dismissAllDialogs();

        MmiCode mmiCode = (MmiCode) r.result;
        if (VDBG) log("  - MmiCode: " + mmiCode);

        Message message = Message.obtain(mHandler, PhoneApp.MMI_CANCEL);
        mMmiStartedDialog = PhoneUtils.displayMMIInitiate(this, mmiCode,
                                                          message, mMmiStartedDialog);
    }

    /**
     * Handles an MMI_CANCEL event, which is triggered by the button
     * (labeled either "OK" or "Cancel") on the "MMI Started" dialog.
     * @see onMMIInitiate
     * @see PhoneUtils.cancelMmiCode
     */
    private void onMMICancel() {
        if (VDBG) log("onMMICancel()...");

        // First of all, cancel the outstanding MMI code (if possible.)
        PhoneUtils.cancelMmiCode(mPhone);

        // Regardless of whether the current MMI code was cancelable, the
        // PhoneApp will get an MMI_COMPLETE event very soon, which will
        // take us to the MMI Complete dialog (see
        // PhoneUtils.displayMMIComplete().)
        //
        // But until that event comes in, we *don't* want to stay here on
        // the in-call screen, since we'll be visible in a
        // partially-constructed state as soon as the "MMI Started" dialog
        // gets dismissed.  So let's forcibly bail out right now.
        if (DBG) log("onMMICancel: finishing InCallScreen...");
        endInCallScreenSession();
    }

    /**
     * Handles the POST_ON_DIAL_CHARS message from the Phone
     * (see our call to mPhone.setOnPostDialCharacter() above.)
     *
     * TODO: NEED TO TEST THIS SEQUENCE now that we no longer handle
     * "dialable" key events here in the InCallScreen: we do directly to the
     * Dialer UI instead.  Similarly, we may now need to go directly to the
     * Dialer to handle POST_ON_DIAL_CHARS too.
     */
    private void handlePostOnDialChars(AsyncResult r, char ch) {
        Connection c = (Connection) r.result;

        if (c != null) {
            Connection.PostDialState state =
                    (Connection.PostDialState) r.userObj;

            if (VDBG) log("handlePostOnDialChar: state = " +
                    state + ", ch = " + ch);

            int phoneType = mPhone.getPhoneType();
            switch (state) {
                case STARTED:
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        mDialer.stopLocalToneCdma();
                        if (mPauseInProgress) {
                            showPausePromptDialogCDMA(c, mPostDialStrAfterPause);
                        }
                        mPauseInProgress = false;
                        mDialer.startLocalToneCdma(ch);
                    }
                    else
                    {
                    	mDialer.stopLocalToneCdma();
                    	if(mPauseInProgress)
                    	{
                    		showPausePromptDialogCDMA(c, mPostDialStrAfterPause);
                    	}
                    	mPauseInProgress = false;
                    	 mDialer.startLocalToneCdma(ch);
                    }
                    // TODO: is this needed, now that you can't actually
                    // type DTMF chars or dial directly from here?
                    // If so, we'd need to yank you out of the in-call screen
                    // here too (and take you to the 12-key dialer in "in-call" mode.)
                    // displayPostDialedChar(ch);
                    break;

                case WAIT:
                    if (DBG) log("handlePostOnDialChars: show WAIT prompt...");
                    String postDialStr = c.getRemainingPostDialString();
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        mDialer.stopLocalToneCdma();
                        showWaitPromptDialogCDMA(c, postDialStr);
                    } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                        showWaitPromptDialogGSM(c, postDialStr);
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    break;

                case WILD:
                    if (DBG) log("handlePostOnDialChars: show WILD prompt");
                    showWildPromptDialog(c);
                    break;

                case COMPLETE:
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        mDialer.stopLocalToneCdma();
                    }
                    break;

                case PAUSE:
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        mPostDialStrAfterPause = c.getRemainingPostDialString();
                        mDialer.stopLocalToneCdma();
                        mPauseInProgress = true;
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private void showWaitPromptDialogGSM(final Connection c, String postDialStr) {
        if (DBG) log("showWaitPromptDialogGSM: '" + postDialStr + "'...");

        Resources r = getResources();
        StringBuilder buf = new StringBuilder();
        buf.append(r.getText(R.string.wait_prompt_str));
        buf.append(postDialStr);

        // if (DBG) log("- mWaitPromptDialog = " + mWaitPromptDialog);
        if (mWaitPromptDialog != null) {
            if (DBG) log("- DISMISSING mWaitPromptDialog.");
            mWaitPromptDialog.dismiss();  // safe even if already dismissed
            mWaitPromptDialog = null;
        }

        mWaitPromptDialog = new AlertDialog.Builder(this)
                .setMessage(buf.toString())
                .setPositiveButton(R.string.send_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (DBG) log("handle WAIT_PROMPT_CONFIRMED, proceed...");
                            c.proceedAfterWaitChar();
                            PhoneApp.getInstance().pokeUserActivity();
                        }
                    })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            if (DBG) log("handle POST_DIAL_CANCELED!");
                            c.cancelPostDial();
                            PhoneApp.getInstance().pokeUserActivity();
                        }
                    })
                .create();
        mWaitPromptDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mWaitPromptDialog.show();
    }

    /**
     * Processes the CDMA specific requirements of a WAIT character in a
     * dial string.
     *
     * Pop up an alert dialog with OK and Cancel buttons to allow user to
     * Accept or Reject the WAIT inserted as part of the Dial string.
     */
    private void showWaitPromptDialogCDMA(final Connection c, String postDialStr) {
        if (DBG) log("showWaitPromptDialogCDMA: '" + postDialStr + "'...");

        Resources r = getResources();
        StringBuilder buf = new StringBuilder();
        buf.append(r.getText(R.string.wait_prompt_str));
        buf.append(postDialStr);

        // if (DBG) log("- mWaitPromptDialog = " + mWaitPromptDialog);
        if (mWaitPromptDialog != null) {
            if (DBG) log("- DISMISSING mWaitPromptDialog.");
            mWaitPromptDialog.dismiss();  // safe even if already dismissed
            mWaitPromptDialog = null;
        }

        mWaitPromptDialog = new AlertDialog.Builder(this)
                .setMessage(buf.toString())
                .setPositiveButton(R.string.pause_prompt_yes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (DBG) log("handle WAIT_PROMPT_CONFIRMED, proceed...");
                            //20101024	sumi920.kim@lge.com LAB1_CallUI dimming lcd off fix [START] -->
                            PhoneApp.getInstance().wakeUpScreen();
                            //20101024	sumi920.kim@lge.com LAB1_CallUI dimming lcd off fix [END] -->
                            c.proceedAfterWaitChar();
                        }
                    })
                .setNegativeButton(R.string.pause_prompt_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (DBG) log("handle POST_DIAL_CANCELED!");
                            //20101024	sumi920.kim@lge.com LAB1_CallUI dimming lcd off fix [START] -->
                            PhoneApp.getInstance().wakeUpScreen();
                            //20101024	sumi920.kim@lge.com LAB1_CallUI dimming lcd off fix [END] -->
                            c.cancelPostDial();
                        }
                    })
                .create();
        mWaitPromptDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mWaitPromptDialog.show();
    }

    /**
     * Pop up an alert dialog which waits for 2 seconds for each P (Pause) Character entered
     * as part of the Dial String.
     */
    private void showPausePromptDialogCDMA(final Connection c, String postDialStrAfterPause) {
        Resources r = getResources();
        StringBuilder buf = new StringBuilder();
        buf.append(r.getText(R.string.pause_prompt_str));
        buf.append(postDialStrAfterPause);

        if (mPausePromptDialog != null) {
            if (DBG) log("- DISMISSING mPausePromptDialog.");
            mPausePromptDialog.dismiss();  // safe even if already dismissed
            mPausePromptDialog = null;
        }

        mPausePromptDialog = new AlertDialog.Builder(this)
                .setMessage(buf.toString())
                .create();
        mPausePromptDialog.show();
        // 2 second timer
        Message msg = Message.obtain(mHandler, EVENT_PAUSE_DIALOG_COMPLETE);
        mHandler.sendMessageDelayed(msg, PAUSE_PROMPT_DIALOG_TIMEOUT);
    }

    private View createWildPromptView() {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(5, 5, 5, 5);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView promptMsg = new TextView(this);
        promptMsg.setTextSize(14);
        promptMsg.setTypeface(Typeface.DEFAULT_BOLD);
        promptMsg.setText(getResources().getText(R.string.wild_prompt_str));

        result.addView(promptMsg, lp);

        mWildPromptText = new EditText(this);
        mWildPromptText.setKeyListener(DialerKeyListener.getInstance());
        mWildPromptText.setMovementMethod(null);
        mWildPromptText.setTextSize(14);
        mWildPromptText.setMaxLines(1);
        mWildPromptText.setHorizontallyScrolling(true);
        mWildPromptText.setBackgroundResource(android.R.drawable.editbox_background);

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(0, 3, 0, 0);

        result.addView(mWildPromptText, lp2);

        return result;
    }

    private void showWildPromptDialog(final Connection c) {
        View v = createWildPromptView();

        if (mWildPromptDialog != null) {
            if (VDBG) log("- DISMISSING mWildPromptDialog.");
            mWildPromptDialog.dismiss();  // safe even if already dismissed
            mWildPromptDialog = null;
        }

        mWildPromptDialog = new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton(
                        R.string.send_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (VDBG) log("handle WILD_PROMPT_CHAR_ENTERED, proceed...");
                                String replacement = null;
                                if (mWildPromptText != null) {
                                    replacement = mWildPromptText.getText().toString();
                                    mWildPromptText = null;
                                }
                                c.proceedAfterWildChar(replacement);
                                PhoneApp.getInstance().pokeUserActivity();
                            }
                        })
                .setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                if (VDBG) log("handle POST_DIAL_CANCELED!");
                                c.cancelPostDial();
                                PhoneApp.getInstance().pokeUserActivity();
                            }
                        })
                .create();
        mWildPromptDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mWildPromptDialog.show();

        mWildPromptText.requestFocus();
    }

    /**
     * Updates the state of the in-call UI based on the current state of
     * the Phone.
     */
    private void updateScreen() {
        if (DBG) log("updateScreen()...");

        // Don't update anything if we're not in the foreground (there's
        // no point updating our UI widgets since we're not visible!)
        // Also note this check also ensures we won't update while we're
        // in the middle of pausing, which could cause a visible glitch in
        // the "activity ending" transition.
        if (!mIsForegroundActivity) {
            if (DBG) log("- updateScreen: not the foreground Activity! Bailing out...");
            //STAR CALL AHJ 100828 return;
        }
        
        if(mInCallScreenMode == InCallScreenMode.UNDEFINED){
        	if (DBG) log("- updateScreen: not the foreground Activity! InCallScreenMode is UNDEFINED");
        	return;
        }

        // Update the state of the in-call menu items.
        if (mInCallMenu != null) {
            // TODO: do this only if the menu is visible!
            if (DBG) log("- updateScreen: updating menu items...");
            boolean okToShowMenu = mInCallMenu.updateItems(mPhone);
            if (!okToShowMenu) {
                // Uh oh: we were only trying to update the state of the
                // menu items, but the logic in InCallMenu.updateItems()
                // just decided the menu shouldn't be visible at all!
                // (That's probably means that the call ended
                // asynchronously while the menu was up.)
                //
                // So take the menu down ASAP.
                if (DBG) log("- updateScreen: Tried to update menu; now need to dismiss!");
                // dismissMenu() has no effect if the menu is already closed.
                dismissMenu(true);  // dismissImmediate = true
            }
        }

        final PhoneApp app = PhoneApp.getInstance();

        if (mInCallScreenMode == InCallScreenMode.OTA_NORMAL) {
            if (DBG) log("- updateScreen: OTA call state NORMAL...");
            if (otaUtils != null) {
                if (DBG) log("- updateScreen: otaUtils is not null, call otaShowProperScreen");
                otaUtils.otaShowProperScreen();
            }
            return;
        } else if (mInCallScreenMode == InCallScreenMode.OTA_ENDED) {
            if (DBG) log("- updateScreen: OTA call ended state ...");
            // Wake up the screen when we get notification, good or bad.
            PhoneApp.getInstance().wakeUpScreen();
            if (app.cdmaOtaScreenState.otaScreenState
                == CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION) {
                if (DBG) log("- updateScreen: OTA_STATUS_ACTIVATION");
                if (otaUtils != null) {
                    if (DBG) log("- updateScreen: otaUtils is not null, "
                                  + "call otaShowActivationScreen");
                    otaUtils.otaShowActivateScreen();
                }
            } else {
                if (DBG) log("- updateScreen: OTA Call end state for Dialogs");
                if (otaUtils != null) {
                    if (DBG) log("- updateScreen: Show OTA Success Failure dialog");
                    otaUtils.otaShowSuccessFailure();
                }
            }
            return;
        } else if (mInCallScreenMode == InCallScreenMode.MANAGE_CONFERENCE) {
            if (DBG) log("- updateScreen: manage conference mode (NOT updating in-call UI)...");
            updateManageConferencePanelIfNecessary();
            return;
        } else if (mInCallScreenMode == InCallScreenMode.CALL_ENDED) {
            if (DBG) log("- updateScreen: call ended state (NOT updating in-call UI)...");
            // Actually we do need to update one thing: the background.
//            updateInCallBackground();
            updateEndCallTouchUi(true);
            return;
        } else {
            updateEndCallTouchUi(false);
        }

        if (DBG) log("- updateScreen: updating the in-call UI...");
        mCallCard.updateState(mPhone);
        updateDialpadVisibility();
        updateInCallTouchUi();
        updateProviderOverlay();
        updateMenuButtonHint();
//        updateInCallBackground();
        if(mForegroundCall.getState() == Call.State.DISCONNECTED)
    	{
    		updateEndCallTouchUi(true);
    	}

        // Forcibly take down all dialog if an incoming call is ringing.
        if (!mRingingCall.isIdle()) {
            dismissAllDialogs();
        } else {
            // Wait prompt dialog is not currently up.  But it *should* be
            // up if the FG call has a connection in the WAIT state and
            // the phone isn't ringing.
            String postDialStr = null;
            List<Connection> fgConnections = mForegroundCall.getConnections();
            int phoneType = mPhone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                Connection fgLatestConnection = mForegroundCall.getLatestConnection();
                if (PhoneApp.getInstance().cdmaPhoneCallState.getCurrentCallState() ==
                        CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                    for (Connection cn : fgConnections) {
                        if ((cn != null) && (cn.getPostDialState() ==
                                Connection.PostDialState.WAIT)) {
                            cn.cancelPostDial();
                        }
                    }
                } else if ((fgLatestConnection != null)
                     && (fgLatestConnection.getPostDialState() == Connection.PostDialState.WAIT)) {
                    if(DBG) log("show the Wait dialog for CDMA");
                    postDialStr = fgLatestConnection.getRemainingPostDialString();
                    showWaitPromptDialogCDMA(fgLatestConnection, postDialStr);
                }
            } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                for (Connection cn : fgConnections) {
                    if ((cn != null) && (cn.getPostDialState() == Connection.PostDialState.WAIT)) {
                        postDialStr = cn.getRemainingPostDialString();
                        showWaitPromptDialogGSM(cn, postDialStr);
                    }
                }
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
    }

    /**
     * (Re)synchronizes the onscreen UI with the current state of the
     * Phone.
     *
     * @return InCallInitStatus.SUCCESS if we successfully updated the UI, or
     *    InCallInitStatus.PHONE_NOT_IN_USE if there was no phone state to sync
     *    with (ie. the phone was completely idle).  In the latter case, we
     *    shouldn't even be in the in-call UI in the first place, and it's
     *    the caller's responsibility to bail out of this activity by
     *    calling endInCallScreenSession if appropriate.
     */
    private InCallInitStatus syncWithPhoneState() {
        boolean updateSuccessful = false;
        if (DBG) log("syncWithPhoneState()...");
        if (DBG) PhoneUtils.dumpCallState(mPhone);
        if (VDBG) dumpBluetoothState();

        // Make sure the Phone is "in use".  (If not, we shouldn't be on
        // this screen in the first place.)

        // Need to treat running MMI codes as a connection as well.
        // Do not check for getPendingMmiCodes when phone is a CDMA phone
        int phoneType = mPhone.getPhoneType();

        if ((phoneType == Phone.PHONE_TYPE_CDMA)
                && ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL)
                || (mInCallScreenMode == InCallScreenMode.OTA_ENDED))) {
            // Even when OTA Call ends, need to show OTA End UI,
            // so return Success to allow UI update.
            return InCallInitStatus.SUCCESS;
        }

        if ((phoneType == Phone.PHONE_TYPE_CDMA)
                || !mForegroundCall.isIdle() || !mBackgroundCall.isIdle() || !mRingingCall.isIdle()
                || !mPhone.getPendingMmiCodes().isEmpty()) {
            if (VDBG) log("syncWithPhoneState: it's ok to be here; update the screen...");
            updateScreen();
            return InCallInitStatus.SUCCESS;
        }

        if (DBG) log("syncWithPhoneState: phone is idle; we shouldn't be here!");
        return InCallInitStatus.PHONE_NOT_IN_USE;
    }

    /**
     * Given the Intent we were initially launched with,
     * figure out the actual phone number we should dial.
     *
     * @return the phone number corresponding to the
     *   specified Intent, or null if the Intent is not
     *   an ACTION_CALL intent or if the intent's data is
     *   malformed or missing.
     *
     * @throws VoiceMailNumberMissingException if the intent
     *   contains a "voicemail" URI, but there's no voicemail
     *   number configured on the device.
     */
    private String getInitialNumber(Intent intent)
            throws PhoneUtils.VoiceMailNumberMissingException {
        String action = intent.getAction();

        if (action == null) {
            return null;
        }

        if (action != null && action.equals(Intent.ACTION_CALL) &&
                intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
            return intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }

        return PhoneUtils.getNumberFromIntent(this, mPhone, intent);
    }

    /**
     * Make a call to whomever the intent tells us to.
     *
     * @param intent the Intent we were launched with
     * @return InCallInitStatus.SUCCESS if we successfully initiated an
     *    outgoing call.  If there was some kind of failure, return one of
     *    the other InCallInitStatus codes indicating what went wrong.
     */
    private InCallInitStatus placeCall(Intent intent) {
        if (VDBG) log("placeCall()...  intent = " + intent);

        String number;

        // Check the current ServiceState to make sure it's OK
        // to even try making a call.
        InCallInitStatus okToCallStatus = checkIfOkToInitiateOutgoingCall();

        try {
            number = getInitialNumber(intent);
        } catch (PhoneUtils.VoiceMailNumberMissingException ex) {
            // If the call status is NOT in an acceptable state, it
            // may effect the way the voicemail number is being
            // retrieved.  Mask the VoiceMailNumberMissingException
            // with the underlying issue of the phone state.
            if (okToCallStatus != InCallInitStatus.SUCCESS) {
                if (DBG) log("Voicemail number not reachable in current SIM card state.");
                return okToCallStatus;
            }
            if (DBG) log("VoiceMailNumberMissingException from getInitialNumber()");
            return InCallInitStatus.VOICEMAIL_NUMBER_MISSING;
        }

        if (number == null) {
            Log.w(LOG_TAG, "placeCall: couldn't get a phone number from Intent " + intent);
            return InCallInitStatus.NO_PHONE_NUMBER_SUPPLIED;
        }

//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> call : roaming : process number : original number [START_LGE_LAB]

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : original number
        String origin_number = null;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : original number

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender
        boolean ignoreRad = intent.getBooleanExtra(RoamingPrefixAppender.INTENT_EXTRA_IGNORE_RAD,
                                            RoamingPrefixAppender.IGNORE_RAD_DEFAULT);

        Log.v(LOG_TAG, "ignoreRad = " + ignoreRad);
        /**
         * origin
        int radAutoUpdate = intent.getIntExtra(RoamingPrefixAppender.INTENT_EXTRA_AUTO_UPDATE,
                                            RoamingPrefixAppender.AUTO_UPDATE_DEFAULT);

        boolean ignoreRad = intent.getBooleanExtra(RoamingPrefixAppender.INTENT_EXTRA_IGNORE_RAD,
                                            RoamingPrefixAppender.IGNORE_RAD_DEFAULT);

        Log.v(LOG_TAG, "radAutoUpdate = " + radAutoUpdate +", ignoreRad = "+ ignoreRad);
    	 */
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender

        if (ignoreRad==false) {
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender
            RoamingPrefixAppender roamingPrefixAppender 
                        = RoamingPrefixAppenderFactory.getDefaultRoamingPrefixAppender(getBaseContext(), getContentResolver(), intent);
            /**
             * origin
            RoamingPrefixAppender roamingPrefixAppender 
                        = RoamingPrefixAppenderFactory.getDefaultRoamingPrefixAppender(getBaseContext(), getContentResolver(), radAutoUpdate);
        	 */
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender
                
            if (roamingPrefixAppender!=null) {
                if (roamingPrefixAppender.isAddPrefix( number)) {
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : original number
                    origin_number = number;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : original number
                    number = roamingPrefixAppender.appendPrefix(number);

                    Log.v(LOG_TAG, 
                            "Roaming Auto Dialing origin_num = " + origin_number+ " new roaming number = " + number );
                }
            }
        }
//LGE_CHANGED_E	 - yongjin.her 2010.04.15 for SKT/KTF/LGT roamiing prefix appending

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : original number
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-02-04 : LGT Roaming Auto Dial
		if(!TextUtils.isEmpty(origin_number))
			intent.putExtra("roamingoriginalnumber", origin_number);
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-02-04 : LGT Roaming Auto Dial
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : original number
//20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> call : roaming : process number : original number  [END_LGE_LAB]

        boolean isEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(number);
        boolean isEmergencyIntent = Intent.ACTION_CALL_EMERGENCY.equals(intent.getAction());

        if (isEmergencyNumber && !isEmergencyIntent) {
            Log.e(LOG_TAG, "Non-CALL_EMERGENCY Intent " + intent
                    + " attempted to call emergency number " + number
                    + ".");
            return InCallInitStatus.CALL_FAILED;
        } else if (!isEmergencyNumber && isEmergencyIntent) {
            Log.e(LOG_TAG, "Received CALL_EMERGENCY Intent " + intent
                    + " with non-emergency number " + number
                    + " -- failing call.");
            return InCallInitStatus.CALL_FAILED;
        }

        // If we're trying to call an emergency number, then it's OK to
        // proceed in certain states where we'd usually just bring up
        // an error dialog:
        // - If we're in EMERGENCY_ONLY mode, then (obviously) you're allowed
        //   to dial emergency numbers.
        // - If we're OUT_OF_SERVICE, we still attempt to make a call,
        //   since the radio will register to any available network.

        if (isEmergencyNumber
            && ((okToCallStatus == InCallInitStatus.EMERGENCY_ONLY)
                || (okToCallStatus == InCallInitStatus.OUT_OF_SERVICE))) {
            if (DBG) log("placeCall: Emergency number detected with status = " + okToCallStatus);
            okToCallStatus = InCallInitStatus.SUCCESS;
            if (DBG) log("==> UPDATING status to: " + okToCallStatus);
        }

        if (okToCallStatus != InCallInitStatus.SUCCESS) {
            // If this is an emergency call, we call the emergency call
            // handler activity to turn on the radio and do whatever else
            // is needed. For now, we finish the InCallScreen (since were
            // expecting a callback when the emergency call handler dictates
            // it) and just return the success state.
            if (isEmergencyNumber && (okToCallStatus == InCallInitStatus.POWER_OFF)) {
//20101014 wonho.moon@lge.com <mailto:wonho.moon@lge.com> TD 139399 : AIRPLANE_MODE [START_LGE_LAB]
                if (Settings.System.getInt(mPhone.getContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 1) {
                    startActivity(intent.setClassName(this, EmergencyCallHandler.class.getName()));
                    if (DBG) log("placeCall: starting EmergencyCallHandler, finishing InCallScreen...");
                    endInCallScreenSession();
                }
                /*
                startActivity(intent.setClassName(this, EmergencyCallHandler.class.getName()));
                if (DBG) log("placeCall: starting EmergencyCallHandler, finishing InCallScreen...");
                endInCallScreenSession();
                */
//20101014 wonho.moon@lge.com <mailto:wonho.moon@lge.com> TD 139399 : AIRPLANE_MODE [END_LGE_LAB]
                return InCallInitStatus.SUCCESS;
            } else {
                return okToCallStatus;
            }
        }

        final PhoneApp app = PhoneApp.getInstance();

        if ((mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) && (mPhone.isOtaSpNumber(number))) {
            if (DBG) log("placeCall: isOtaSpNumber() returns true");
            setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
            if (app.cdmaOtaProvisionData != null) {
                app.cdmaOtaProvisionData.isOtaCallCommitted = false;
            }
        }

        mNeedShowCallLostDialog = false;

        // We have a valid number, so try to actually place a call:
        // make sure we pass along the intent's URI which is a
        // reference to the contact. We may have a provider gateway
        // phone number to use for the outgoing call.
        int callStatus;
        Uri contactUri = intent.getData();

        if (null != mProviderGatewayUri &&
            !(isEmergencyNumber || isEmergencyIntent) &&
            PhoneUtils.isRoutableViaGateway(number)) {  // Filter out MMI, OTA and other codes.

            callStatus = PhoneUtils.placeCallVia(
                this, mPhone, number, contactUri, mProviderGatewayUri);
        } else {
            setInCallScreenMode(InCallScreenMode.DIALING); //hyojin.an@lge.com 101001
        	PhoneApp.getInstance().updateWakeState(true); //hyojin.an 101026
            callStatus = PhoneUtils.placeCall(mPhone, number, contactUri);
			updateScreen(); //STAR CALL AHJ 100828
        }

// LGE_CALL_COSTS START
        mCallCostIsEmergency = isEmergencyNumber;

// LGE_UPDATE_S // in case of MMI key code, Query releated to AOC is not neccessary.
        if (callStatus != PhoneUtils.CALL_STATUS_DIALED_MMI)
        {
            Message msgGetAcmMax = mHandler.obtainMessage(AOC_QUERY_ICC_ACM_MAX);
            mCmdIf.getAccumulatedCallMeterMax(msgGetAcmMax);

            Message msgGetAcm = mHandler.obtainMessage(AOC_QUERY_ICC_ACM);
            mCmdIf.getAccumulatedCallMeter(msgGetAcm);
        }
// LGE_UPDATE_E		
// LGE_CALL_COSTS END


        switch (callStatus) {
            case PhoneUtils.CALL_STATUS_DIALED:
//20101028 wonho.moon@lge.com <mailto:wonho.moon@lge.com> call : roaming : process number : original number  [START_LGE_LAB]
                if (!TextUtils.isEmpty(origin_number)) {
                    mForegroundCall.getLatestConnection().setOriginalNumber(origin_number);
                }
//20101028 wonho.moon@lge.com <mailto:wonho.moon@lge.com> call : roaming : process number : original number  [END_LGE_LAB]

                if (VDBG) log("placeCall: PhoneUtils.placeCall() succeeded for regular call '"
                             + number + "'.");

                if (mInCallScreenMode == InCallScreenMode.OTA_NORMAL) {
                    app.cdmaOtaScreenState.otaScreenState =
                            CdmaOtaScreenState.OtaScreenState.OTA_STATUS_LISTENING;
                    updateScreen();
                }

                // Any time we initiate a call, force the DTMF dialpad to
                // close.  (We want to make sure the user can see the regular
                // in-call UI while the new call is dialing, and when it
                // first gets connected.)
                mDialer.closeDialer(false);  // no "closing" animation

                // Also, in case a previous call was already active (i.e. if
                // we just did "Add call"), clear out the "history" of DTMF
                // digits you typed, to make sure it doesn't persist from the
                // previous call to the new call.
                // TODO: it would be more precise to do this when the actual
                // phone state change happens (i.e. when a new foreground
                // call appears and the previous call moves to the
                // background), but the InCallScreen doesn't keep enough
                // state right now to notice that specific transition in
                // onPhoneStateChanged().
                mDialer.clearDigits();

                if (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
                    // Start the 2 second timer for 3 Way CallerInfo
                    if (app.cdmaPhoneCallState.getCurrentCallState()
                            == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                        //Unmute for the second MO call
                        PhoneUtils.setMuteInternal(mPhone, false);

                        //Start the timer for displaying "Dialing" for second call
                        Message msg = Message.obtain(mHandler, THREEWAY_CALLERINFO_DISPLAY_DONE);
                        mHandler.sendMessageDelayed(msg, THREEWAY_CALLERINFO_DISPLAY_TIME);

                        // Set the mThreeWayCallOrigStateDialing state to true
                        app.cdmaPhoneCallState.setThreeWayCallOrigState(true);

                        //Update screen to show 3way dialing
                        updateScreen();
                    }
                }

                return InCallInitStatus.SUCCESS;
            case PhoneUtils.CALL_STATUS_DIALED_MMI:
                if (DBG) log("placeCall: specified number was an MMI code: '" + number + "'.");
                // The passed-in number was an MMI code, not a regular phone number!
                // This isn't really a failure; the Dialer may have deliberately
                // fired an ACTION_CALL intent to dial an MMI code, like for a
                // USSD call.
                //
                // Presumably an MMI_INITIATE message will come in shortly
                // (and we'll bring up the "MMI Started" dialog), or else
                // an MMI_COMPLETE will come in (which will take us to a
                // different Activity; see PhoneUtils.displayMMIComplete()).
                return InCallInitStatus.DIALED_MMI;
            case PhoneUtils.CALL_STATUS_FAILED:
                Log.w(LOG_TAG, "placeCall: PhoneUtils.placeCall() FAILED for number '"
                      + number + "'.");
                // We couldn't successfully place the call; there was some
                // failure in the telephony layer.
                return InCallInitStatus.CALL_FAILED;
            default:
                Log.w(LOG_TAG, "placeCall: unknown callStatus " + callStatus
                      + " from PhoneUtils.placeCall() for number '" + number + "'.");
                return InCallInitStatus.SUCCESS;  // Try to continue anyway...
        }
    }


    /**
     * Checks the current ServiceState to make sure it's OK
     * to try making an outgoing call to the specified number.
     *
     * @return InCallInitStatus.SUCCESS if it's OK to try calling the specified
     *    number.  If not, like if the radio is powered off or we have no
     *    signal, return one of the other InCallInitStatus codes indicating what
     *    the problem is.
     */
    private InCallInitStatus checkIfOkToInitiateOutgoingCall() {
        // Watch out: do NOT use PhoneStateIntentReceiver.getServiceState() here;
        // that's not guaranteed to be fresh.  To synchronously get the
        // CURRENT service state, ask the Phone object directly:
        int state = mPhone.getServiceState().getState();
        if (VDBG) log("checkIfOkToInitiateOutgoingCall: ServiceState = " + state);

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                // Normal operation.  It's OK to make outgoing calls.
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
                // make this distinction.  So in practice the
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

    private void handleMissingVoiceMailNumber() {
        if (DBG) log("handleMissingVoiceMailNumber");

        final Message msg = Message.obtain(mHandler);
        msg.what = DONT_ADD_VOICEMAIL_NUMBER;

        final Message msg2 = Message.obtain(mHandler);
        msg2.what = ADD_VOICEMAIL_NUMBER;

        mMissingVoicemailDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.no_vm_number)
                .setMessage(R.string.no_vm_number_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (VDBG) log("Missing voicemail AlertDialog: POSITIVE click...");
                            msg.sendToTarget();  // see dontAddVoiceMailNumber()
                            PhoneApp.getInstance().pokeUserActivity();
                        }})
                .setNegativeButton(R.string.add_vm_number_str,
                                   new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (VDBG) log("Missing voicemail AlertDialog: NEGATIVE click...");
                            msg2.sendToTarget();  // see addVoiceMailNumber()
                            PhoneApp.getInstance().pokeUserActivity();
                        }})
                .setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            if (VDBG) log("Missing voicemail AlertDialog: CANCEL handler...");
                            msg.sendToTarget();  // see dontAddVoiceMailNumber()
                            PhoneApp.getInstance().pokeUserActivity();
                        }})
                .create();

        // When the dialog is up, completely hide the in-call UI
        // underneath (which is in a partially-constructed state).
        mMissingVoicemailDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mMissingVoicemailDialog.show();
     // 20101014 jongwany.lee@lge.com TD issue fixed 9044, 9299, 9711 regarding dismissed popup dialog
        mMissingVoicemailDialog.setOnKeyListener(new OnKeyListener(){

			
			public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
				if(arg2.getKeyCode() == 84){
					return true;
				}
				return false;
			}});
    }

    private void addVoiceMailNumberPanel() {
        if (mMissingVoicemailDialog != null) {
            mMissingVoicemailDialog.dismiss();
            mMissingVoicemailDialog = null;
        }
        if (DBG) log("addVoiceMailNumberPanel: finishing InCallScreen...");
        endInCallScreenSession();

        if (DBG) log("show vm setting");

        // navigate to the Voicemail setting in the Call Settings activity.
        Intent intent = new Intent(CallFeaturesSetting.ACTION_ADD_VOICEMAIL);
        intent.setClass(this, CallFeaturesSetting.class);
        startActivity(intent);
    }

    private void dontAddVoiceMailNumber() {
        if (mMissingVoicemailDialog != null) {
            mMissingVoicemailDialog.dismiss();
            mMissingVoicemailDialog = null;
        }
        if (DBG) log("dontAddVoiceMailNumber: finishing InCallScreen...");
        endInCallScreenSession();
    }

    /**
     * Do some delayed cleanup after a Phone call gets disconnected.
     *
     * This method gets called a couple of seconds after any DISCONNECT
     * event from the Phone; it's triggered by the
     * DELAYED_CLEANUP_AFTER_DISCONNECT message we send in onDisconnect().
     *
     * If the Phone is totally idle right now, that means we've already
     * shown the "call ended" state for a couple of seconds, and it's now
     * time to endInCallScreenSession this activity.
     *
     * If the Phone is *not* idle right now, that probably means that one
     * call ended but the other line is still in use.  In that case, we
     * *don't* exit the in-call screen, but we at least turn off the
     * backlight (which we turned on in onDisconnect().)
     */
// LGE_AUTO_REDIAL START
    // add parameter
    private void delayedCleanupAfterDisconnect(Connection.DisconnectCause data) {
    //<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] -->
    boolean bAutocall = PhoneUtils.isAutoCallTest();
    //<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->    	
// LGE_AUTO_REDIAL END
    	if (VDBG) log("delayedCleanupAfterDisconnect()...  Phone state = " + mPhone.getState());

        // Clean up any connections in the DISCONNECTED state.
        //
        // [Background: Even after a connection gets disconnected, its
        // Connection object still stays around, in the special
        // DISCONNECTED state.  This is necessary because we we need the
        // caller-id information from that Connection to properly draw the
        // "Call ended" state of the CallCard.
        //   But at this point we truly don't need that connection any
        // more, so tell the Phone that it's now OK to to clean up any
        // connections still in that state.]
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] -->     
       
		if(bAutocall){
			mShowCallLogAfterDisconnect = false;
		}
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->
        mPhone.clearDisconnected();

        if (!phoneIsInUse()) {
            // Phone is idle!  We should exit this screen now.
            if (DBG) log("- delayedCleanupAfterDisconnect: phone is idle...");

            // And (finally!) exit from the in-call screen
            // (but not if we're already in the process of pausing...)
            if (mIsForegroundActivity) {
                if (DBG) log("- delayedCleanupAfterDisconnect: finishing InCallScreen...");
// LGE_AUTO_REDIAL START
                int redial = Settings.System.getInt(getContentResolver(),Settings.System.AUTOMATIC_REDIAL,0);
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
// LGE_CR2082 START
                if(redial == 1 && repeat < MAX_ATTEMPTS && blacklist.notContain(dialNumber) &&
// LGE_CR2082 END
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
                		( data == Connection.DisconnectCause.BUSY
                		||  data == Connection.DisconnectCause.CONGESTION
                		||  data == Connection.DisconnectCause.OUT_OF_SERVICE)){

                     final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                     intent.putExtra(Intent.EXTRA_PHONE_NUMBER, dialNumber);
                     disable_log = true;
                     if(blacklist.freePlace() == true){
                         intent.putExtra(RedialCallHandler.REDIAL_CALL_RETRY_KEY,TIME_BETWEEN[repeat-1]);
                         intent.putExtra(RedialCallHandler.REDIAL_CALL_RETRY_BLACK_LIST,false);
                         repeat ++ ;
                         if(data == Connection.DisconnectCause.OUT_OF_SERVICE){
                             repeat = MAX_ATTEMPTS;
                         }
                         if(repeat == MAX_ATTEMPTS)
                             blacklist.add(dialNumber);
                     }
                     else{
                         intent.putExtra(RedialCallHandler.REDIAL_CALL_RETRY_BLACK_LIST,true);
                     }
                     intent.setClassName(this, RedialCallHandler.class.getName());
                     startActivity(intent);
                }
                else{
                	repeat = 1;
                	disable_log = false;
                    // If this is a call that was initiated by the user, and
                    // we're *not* in emergency mode, finish the call by
                    // taking the user to the Call Log.
                // Otherwise we simply call endInCallScreenSession, which will take us
                    // back to wherever we came from.
                	if (mShowCallLogAfterDisconnect && !isPhoneStateRestricted()) {
                	                    if (VDBG) log("- Show Call Log after disconnect...");
                	                    final Intent intent = PhoneApp.createCallLogIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                	                    startActivity(intent);
                    // Even in this case we still call endInCallScreenSession (below),
                	                    // to make sure we don't stay in the activity history.
                    }
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [START] --> 	
					else {	   
						if(bAutocall){
							mShowCallLogAfterDisconnect = false;
							int callendcause = 0;
							switch( autoCallCause ){
							case NOT_DISCONNECTED :
								callendcause = 0;
							break;					 
							case INCOMING_MISSED :      
							callendcause = 1;
							break;	
							case NORMAL :       //                   normal; remote 
							callendcause = 2;
								break;
							case LOCAL :           
							callendcause = 3;
								break;
							case BUSY : 
							callendcause = 4;
								break;
							case CONGESTION :  
							callendcause = 5;
								break;
							case MMI :        
							callendcause = 6;
								break;
							case INVALID_NUMBER :   
							callendcause = 7;
								break;
							case LOST_SIGNAL :
								callendcause = 8;
								break;
							case LIMIT_EXCEEDED : 
							callendcause = 9;
								break;
							case INCOMING_REJECTED :  
							callendcause = 10;
								break;
							case POWER_OFF : 
							callendcause = 11;
								break;
							case OUT_OF_SERVICE : 
							callendcause = 12;
								break;
							case ICC_ERROR :   
							callendcause = 13;
								break;
							case CALL_BARRED : 
							callendcause = 14;
								break;
							case FDN_BLOCKED :  
							callendcause = 15;
								break;
							case CS_RESTRICTED :  
							callendcause = 16;
								break;
							case CS_RESTRICTED_NORMAL :     
							callendcause = 17;
								break;
							case CS_RESTRICTED_EMERGENCY :         
							callendcause = 18;
								break;
							case ERROR_UNSPECIFIED :   
							callendcause = 19;
								break;

							}
							SharedPreferences prefs = getSharedPreferences("prefs", MODE_WORLD_READABLE|MODE_WORLD_WRITEABLE );
							prefs.edit().putInt("CallEndCause", callendcause ).commit();
							
							Intent intent = new Intent();
							intent.putExtra("endCause", autoCallCause);
							setResult(RESULT_OK, intent);
							finish();
						}			
					}
//<!--[sumi920.kim@lge.com] 2010.08.27	  LAB1_CallUI AutoCall Test [END] -->
                }
                endInCallScreenSession();
            }
            else
            {
	            //20101025 sumi920.kim [START_LGE_LAB1] 
	            // LCD off -> call end -> delayedCleanupAfterDisconnect()-> onresume ()-> ui error
            	if(StarConfig.COUNTRY.equals("KR")) 
            	{
	            final boolean hasRingingCall = !mRingingCall.isIdle();
	            final boolean hasActiveCall = !mForegroundCall.isIdle();
	            final boolean hasHoldingCall = !mBackgroundCall.isIdle();
	            if(!hasRingingCall && !hasActiveCall && !hasHoldingCall) // no call
	            {
	            	if (DBG) log("- has no call :  mIsForegroundActivity(" + mIsForegroundActivity + ")" );
	            	
	            	mdelayedCleanupAfterResume = true; 
	            	delayedData = data;
		            }
	            }
	            //20101025 sumi920.kim [END_LGE_LAB1] 
            }
        } else {
            // The phone is still in use.  Stay here in this activity, but
            // we don't need to keep the screen on.
            if (DBG) log("- delayedCleanupAfterDisconnect: staying on the InCallScreen...");
            if (DBG) PhoneUtils.dumpCallState(mPhone);


        }
    }


    //
    // Callbacks for buttons / menu items.
    //

    public void onClick(View view) {
        int id = view.getId();
        if (VDBG) log("onClick(View " + view + ", id " + id + ")...");
        if (VDBG && view instanceof InCallMenuItemView) {
            InCallMenuItemView item = (InCallMenuItemView) view;
            log("  ==> menu item! " + item);
        }

        // Most menu items dismiss the menu immediately once you click
        // them.  But some items (the "toggle" buttons) are different:
        // they want the menu to stay visible for a second afterwards to
        // give you feedback about the state change.
        boolean dismissMenuImmediate = true;

        switch (id) {
            case R.id.menuAnswerAndHold:
                if (VDBG) log("onClick: AnswerAndHold...");
                internalAnswerCall();  // Automatically holds the current active call
                break;

            case R.id.menuAnswerAndEnd:
                if (VDBG) log("onClick: AnswerAndEnd...");
                internalAnswerAndEnd();
                break;

            case R.id.menuAnswer:
                if (DBG) log("onClick: Answer...");
                internalAnswerCall();
                break;

            case R.id.menuIgnore:
                if (DBG) log("onClick: Ignore...");
                internalHangupRingingCall();
                break;

            case R.id.menuSwapCalls:
                if (DBG) log("onClick: SwapCalls...");
                internalSwapCalls();
                break;

            case R.id.menuMergeCalls:
                if (VDBG) log("onClick: MergeCalls...");
                PhoneUtils.mergeCalls(mPhone);
                break;

            case R.id.menuManageConference:
                if (VDBG) log("onClick: ManageConference...");
                // Show the Manage Conference panel.
                setInCallScreenMode(InCallScreenMode.MANAGE_CONFERENCE);
                break;

            case R.id.menuShowDialpad:
                if (VDBG) log("onClick: Show/hide dialpad...");
                onShowHideDialpad();
                break;

            case R.id.manage_done:  // mButtonManageConferenceDone
                if (VDBG) log("onClick: mButtonManageConferenceDone...");
                // Hide the Manage Conference panel, return to NORMAL mode.
                setInCallScreenMode(InCallScreenMode.NORMAL);
                break;

            case R.id.menuSpeaker:
                if (VDBG) log("onClick: Speaker...");
                onSpeakerClick();
                // This is a "toggle" button; let the user see the new state for a moment.
                dismissMenuImmediate = false;
                break;

            case R.id.menuBluetooth:
                if (VDBG) log("onClick: Bluetooth...");
                onBluetoothClick();
                // This is a "toggle" button; let the user see the new state for a moment.
                dismissMenuImmediate = false;
                break;

            case R.id.menuMute:
                if (VDBG) log("onClick: Mute...");
                onMuteClick();
                // This is a "toggle" button; let the user see the new state for a moment.
                dismissMenuImmediate = false;
                break;

            case R.id.menuHold:
                if (VDBG) log("onClick: Hold...");
                onHoldClick();
                // This is a "toggle" button; let the user see the new state for a moment.
                dismissMenuImmediate = false;
                break;

            case R.id.menuAddCall:
                if (VDBG) log("onClick: AddCall...");
                PhoneUtils.startNewCall(mPhone);  // Fires off an ACTION_DIAL intent
                break;

            case R.id.menuEndCall:
                if (VDBG) log("onClick: EndCall...");
                internalHangup();
                break;
// LGE_CALL_DEFLECTION START
            case R.id.menuDeflect: {
                if (VDBG) log("onClick: menuDeflect...");
                final String callDeflectCode = "CALL_DEFLECTION";
                Intent intent = new Intent();
                intent.putExtra(callDeflectCode, 1);
                intent.setClass(this, SimpleCallDialog.class);
                startActivity(intent);
                break;
            }
// LGE_CALL_DEFLECTION END
// LGE_CALL_TRANSFER START
            case R.id.menuTransfer: {
                if (VDBG) log("onClick: menuTransfer...");
                try {
                    List<Connection> fgdConnections = mForegroundCall.getConnections();
                    List<Connection> bgdConnections = mBackgroundCall.getConnections();
                    int fgdConnectionsSize = fgdConnections.size();
                    int bgdConnectionsSize = bgdConnections.size();
                    if ((fgdConnections != null) && (bgdConnections != null)) {
                        if ((fgdConnectionsSize == 1) && (bgdConnectionsSize == 1)) {
                            if (mPhone.canTransfer()) {
                                mPhone.explicitCallTransfer(false);
                            }
                        }
                        else if (((fgdConnectionsSize == 0) && (bgdConnectionsSize == 1))
                                || ((fgdConnectionsSize == 1) && (bgdConnectionsSize == 0))) {
                            if (mInCallMenu != null) {
                                if (mInCallMenu.canColdTransfer(mPhone)) {
                                    final String callTransferCode = "CALL_TRANSFER";
                                    Intent intent = new Intent();
                                    intent.putExtra(callTransferCode, 2);
                                    intent.setClass(this, SimpleCallDialog.class);
                                    startActivity(intent);
                                }
                            }
                        }
                    }
                }  
                catch(CallStateException exc) {
                    if (VDBG) log("onClick: menuTransfer...CallStateException");
                    Toast.makeText(InCallScreen.this,R.string.incall_error_supp_service_transfer,Toast.LENGTH_LONG).show();
                    break;
                }
                break;
            }
// LGE_CALL_TRANSFER END
            default:
                if  ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL
                        || mInCallScreenMode == InCallScreenMode.OTA_ENDED)
                        && otaUtils != null) {
                    otaUtils.onClickHandler(id);
                } else {
                    Log.w(LOG_TAG,
                            "Got click from unexpected View ID " + id + " (View = " + view + ")");
                }
                break;
        }

        EventLog.writeEvent(EventLogTags.PHONE_UI_BUTTON_CLICK,
                (view instanceof TextView) ? ((TextView) view).getText() : "");

        // If the user just clicked a "stateful" menu item (i.e. one of
        // the toggle buttons), we keep the menu onscreen briefly to
        // provide visual feedback.  Since we want the user to see the
        // *new* current state, force the menu items to update right now.
        //
        // Note that some toggle buttons ("Hold" in particular) do NOT
        // immediately change the state of the Phone.  In that case, the
        // updateItems() call below won't have any visible effect.
        // Instead, the menu will get updated by the updateScreen() call
        // that happens from onPhoneStateChanged().

        if (!dismissMenuImmediate) {
            // TODO: mInCallMenu.updateItems() is a very big hammer; it
            // would be more efficient to update *only* the menu item(s)
            // we just changed.  (Doing it this way doesn't seem to cause
            // a noticeable performance problem, though.)
            if (VDBG) log("- onClick: updating menu to show 'new' current state...");
            boolean okToShowMenu = mInCallMenu.updateItems(mPhone);
            if (!okToShowMenu) {
                // Uh oh.  All we tried to do was update the state of the
                // menu items, but the logic in InCallMenu.updateItems()
                // just decided the menu shouldn't be visible at all!
                // (That probably means that the call ended asynchronously
                // while the menu was up.)
                //
                // That's OK; just make sure to take the menu down ASAP.
                if (VDBG) log("onClick: Tried to update menu, but now need to take it down!");
                dismissMenuImmediate = true;
            }
        }

        // Any menu item counts as explicit "user activity".
        PhoneApp.getInstance().pokeUserActivity();

        // Finally, *any* action handled here closes the menu (either
        // immediately, or after a short delay).
        //
        // Note that some of the clicks we handle here aren't even menu
        // items in the first place, like the mButtonManageConferenceDone
        // button.  That's OK; if the menu is already closed, the
        // dismissMenu() call does nothing.
        dismissMenu(dismissMenuImmediate);
    }

    private void onHoldClick() {
        if (VDBG) log("onHoldClick()...");

        final boolean hasActiveCall = !mForegroundCall.isIdle();
        final boolean hasHoldingCall = !mBackgroundCall.isIdle();
        if (VDBG) log("- hasActiveCall = " + hasActiveCall
                      + ", hasHoldingCall = " + hasHoldingCall);
        boolean newHoldState;
        boolean holdButtonEnabled;
        if (hasActiveCall && !hasHoldingCall) {
            // There's only one line in use, and that line is active.
            PhoneUtils.switchHoldingAndActive(mPhone);  // Really means "hold" in this state
            newHoldState = true;
            holdButtonEnabled = true;
        } else if (!hasActiveCall && hasHoldingCall) {
            // There's only one line in use, and that line is on hold.
            PhoneUtils.switchHoldingAndActive(mPhone);  // Really means "unhold" in this state
            newHoldState = false;
            holdButtonEnabled = true;
        } else {
            // Either zero or 2 lines are in use; "hold/unhold" is meaningless.
            newHoldState = false;
            holdButtonEnabled = false;
        }
        // TODO: we *could* now forcibly update the "Hold" button based on
        // "newHoldState" and "holdButtonEnabled".  But for now, do
        // nothing here, and instead let the menu get updated when the
        // onPhoneStateChanged() callback comes in.  (This seems to be
        // responsive enough.)

        // Also, any time we hold or unhold, force the DTMF dialpad to close.
        mDialer.closeDialer(true);  // do the "closing" animation
    }

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
    private void onDialpadSendClick() {
    
		Log.w(LOG_TAG, "onDialpadSendClick");
	
        final String number = mDialer.getDigits();
        if (mDialer.isOpened()) {
			
			Log.w(LOG_TAG, "isOpened");
			
            if (!TextUtils.isEmpty(number)) {
               int callStatus = PhoneUtils.placeCall(mPhone, number, null);
               if (callStatus == PhoneUtils.CALL_STATUS_FAILED) {
                  Toast.makeText(this, R.string.incall_error_call_failed, Toast.LENGTH_SHORT)
                      .show();
               }
               enableTouchLock(false);
               mDialer.closeDialer(true);
            } else {
			   Log.w(LOG_TAG, "isClosed");
				
               final boolean hasActiveCall = !mForegroundCall.isIdle();
               final boolean hasHoldingCall = !mBackgroundCall.isIdle();
               if (hasActiveCall && hasHoldingCall) {
                  internalSwapCalls(); // PhoneUtils.switchHoldingAndActive(mPhone);
               } else {
                  onHoldClick();
               }
            }
        } 
//START jahyun.park@lge.com 2010.08.21  QM TD : 30623
        else {
            final String carrier = SystemProperties.get("ro.telephony.service_provider", "null");
            if(carrier.equals("LGT"))
           handleCallKey();
        }
//END jahyun.park@lge.com 2010.08.21  QM TD : 30623        
    }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

    private void onSpeakerClick() {
        if (VDBG) log("onSpeakerClick()...");

        // TODO: Turning on the speaker seems to enable the mic
        //   whether or not the "mute" feature is active!
        // Not sure if this is an feature of the telephony API
        //   that I need to handle specially, or just a bug.
        boolean newSpeakerState = !PhoneUtils.isSpeakerOn(this);
        if (newSpeakerState && isBluetoothAvailable() && isBluetoothAudioConnected()) {
            disconnectBluetoothAudio();
        }
        PhoneUtils.turnOnSpeaker(this, newSpeakerState, true);

//jongik2.kim 20101020 cp call path [start]
        if(newSpeakerState){
	     if(VEUtils.isSKTFeature()== false){ //20101103 seki.par@lge.com Temp SPK<=>RCV nois[LGE_LAB1]
              PhoneUtils.changeModemPath(InCallScreen.this, 3);
	     	}
		}else if(PhoneUtils.isHeadsetOn(InCallScreen.this)){
            PhoneUtils.changeModemPath(InCallScreen.this, 2); 
		}else{
            PhoneUtils.changeModemPath(InCallScreen.this, 1); 
		}
//jongik2.kim 20101020 cp call path [end]
        if (newSpeakerState) {
            // The "touch lock" overlay is NEVER used when the speaker is on.
            enableTouchLock(false);
        } else {
            // User just turned the speaker *off*.  If the dialpad
            // is open, we need to start the timer that will
            // eventually bring up the "touch lock" overlay.
            if (mDialer.isOpened() && !isTouchLocked()) {
                resetTouchLockTimer();
            }
        }
    }

    private void onMuteClick() {
        if (VDBG) log("onMuteClick()...");
        boolean newMuteState = !PhoneUtils.getMute(mPhone);
        PhoneUtils.setMute(mPhone, newMuteState);
    }

    private void onBluetoothClick() {
        if (VDBG) log("onBluetoothClick()...");

        if (isBluetoothAvailable()) {
            // Toggle the bluetooth audio connection state:
            if (isBluetoothAudioConnected()) {
                disconnectBluetoothAudio();
            } else {
                // Manually turn the speaker phone off, instead of allowing the
                // Bluetooth audio routing handle it.  This ensures that the rest
                // of the speakerphone code is executed, and reciprocates the
                // menuSpeaker code above in onClick().  The onClick() code
                // disconnects the active bluetooth headsets when the
                // speakerphone is turned on.
                if (PhoneUtils.isSpeakerOn(this)) {
                    PhoneUtils.turnOnSpeaker(this, false, true);
                }

                connectBluetoothAudio();
            }
        } else {
            // Bluetooth isn't available; the "Audio" button shouldn't have
            // been enabled in the first place!
            Log.w(LOG_TAG, "Got onBluetoothClick, but bluetooth is unavailable");
        }
    }

    private void onShowHideDialpad() {
        if (VDBG) log("onShowHideDialpad()...");
        if (mDialer.isOpened()) {
            mDialer.closeDialer(true);  // do the "closing" animation
        } else {
            mDialer.openDialer(true);  // do the "opening" animation
        }
        mDialer.setHandleVisible(true);
    }

    /**
     * Handles button clicks from the InCallTouchUi widget.
     */
    /* package */ void handleOnscreenButtonClick(int id) {
        if (DBG) log("handleOnscreenButtonClick(id " + id + ")...");

        // LGE_UPDATE_S sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
        switch (id) {
            case R.id.answerButton:
            case R.id.rejectButton:
            case R.id.endButton:
            case R.id.mergeButton:
            case R.id.cdmaMergeButton:
            case R.id.btn_accept:
            case R.id.btn_reject:
            case R.id.btn_send_sms:
            case R.id.holdButton:				
                if (mCallRelatedButtonLocked) {
                    if (VDBG) log("ignore the continuous button click");
                    return;
                }
                mCallRelatedButtonLocked = true;
                mHandler.sendMessageDelayed(mHandler.obtainMessage(CALL_RELATED_BUTTON_LOCK), CALL_RELATED_BUTTON_LOCK_TIMEOUT);
                break;
            default:
                break;
        }
        // LGE_UPDATE_E sanghoon.roh@lge.com 2010/07/01 avoid the call related button to be pressed continuously
        //20101107 sumi920.kim@lge.com  Control state swiching(Hold/Active)[START_LGE_LAB1]
    	if(StarConfig.COUNTRY.equals("KR"))
    	{
    		if(( id == R.id.holdButton) ||( id == R.id.swapButton))
    		{
	    		if(bSendingSuppService)
	    		{
	    			if (VDBG) log("isSendingSuppService : true");
	    			return;
	    		}
	    		else {
	    			if (VDBG) log("setSendingSuppService : true");
	    			bSendingSuppService = true;
	    		}
    		}
    	}
    	//20101107 sumi920.kim@lge.com  Control state swiching(Hold/Active)[END_LGE_LAB1]            	

        switch (id) {
            // TODO: since every button here corresponds to a menu item that we
            // already handle in onClick(), maybe merge the guts of these two
            // methods into a separate helper that takes an ID (of either a menu
            // item *or* touch button) and does the appropriate user action.

            // Actions while an incoming call is ringing:
            case R.id.answerButton:
                // jundj@mo2.co.kr Start answer the incall, remove the ve player and show default call card
                if(VEUtils.isSKTFeature()){
                    VEUtils.setVisibleCallCardPersonInfo(VEUtils.CALLCARD_PERSONINFO_DEFAULT);
                }
                // jundj@mo2.co.kr End
                //20101101 sumi920.kim@lge.com FMC Call [START_LGE_LAB1]
                if(StarConfig.OPERATOR.equals("SKT"))
                {
	                if(PhoneApp.getInstance().getCallFMCState() == 2) //LGE_FMC
	                {
	                     mCallCard.mCommonFMC.setVisibility(View.INVISIBLE);
	                     PhoneApp.getInstance().EndCallFMC();  //�̻���
	                }
	                else  
	                {
	                	mCallCard.mCommonFMC.setVisibility(View.GONE);
	                    internalAnswerCall();
	                }
                }
                else
                //20101101 sumi920.kim@lge.com FMC Call [END_LGE_LAB1]
                	internalAnswerCall();
                break;
            case R.id.rejectButton:
                internalHangupRingingCall();
                break;

            // The other regular (single-tap) buttons used while in-call:
            case R.id.holdButton:
                onHoldClick();
                break;
            case R.id.swapButton:
                internalSwapCalls();
                break;
            case R.id.endButton:
            	if(mDialer != null && mDialer.isOpened())
            	{
            		onShowHideDialpad();
            	}
                internalHangup();
                
                if(mForegroundCall != null && !mForegroundCall.hasConnections())
                	setInCallScreenMode(InCallScreenMode.CALL_ENDED);
                
                break;
            case R.id.dialpadButton:
                onShowHideDialpad();
                break;
            case R.id.bluetoothButton:
            	//20101009 sumi920.kim@lge.com OptionMenu InCall [START_LGE_LAB1]
            	if(StarConfig.COUNTRY.equals("KR"))
            	{	
            		if(PhoneUtils.isSoundRecording())
            		{
            			stopRecording();
            			Toast.makeText(this, getResources().getText(R.string.saveRecordText), Toast.LENGTH_SHORT)
	                      .show();
            		}
            		if(mInCallTouchUi.isHDVideoCallAvailable())
            		{
            			InCallControlState inCallControlState = getUpdatedInCallControlState();
            			if(inCallControlState.canAddCall)
            				gotoHDVideoCall();
            		}
            	}
            	else
               	//20101009 sumi920.kim@lge.com OptionMenu InCall [END_LGE_LAB1]            		
            		onBluetoothClick();
                break;
            case R.id.muteButton:
                onMuteClick();
                break;
            case R.id.speakerButton:
                onSpeakerClick();
                break;
            case R.id.addButton:
                PhoneUtils.startNewCall(mPhone);  // Fires off an ACTION_DIAL intent
                break;
            case R.id.mergeButton:
            case R.id.cdmaMergeButton:
                PhoneUtils.mergeCalls(mPhone);
                break;

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
            case R.id.sendButton:
                onDialpadSendClick();
                break;
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

            //<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]	 -->
            case R.id.recordButton:
            	final Call.State fgCallState = mForegroundCall.getState();
            	if(PhoneUtils.isSoundRecording() == false && fgCallState == Call.State.ACTIVE )
            	{
            		String result;
            		
            		CallEndReceiver.setInCallScreen(InCallScreen.this);
            		
	            	PhoneUtils.setSoundRecording(true);
	            	result = mSoundRecord.initSoundRecord(InCallScreen.this, mCallCard.getDisplayPhoneNumer());
	            	if( result == null)
	            	{
	            		Intent intent = new Intent();
	            		intent.setAction("voice_video_record_playing");
	            		sendBroadcast(intent);

	            		mSoundRecord.startRecording(InCallScreen.this);
	            		mInCallTouchUi.disableSpeakerphoneButton();
	            		updateScreen();
	            	}
	            	else{
	            		 Toast.makeText(this, result, Toast.LENGTH_SHORT)
	                      .show();
	            	}
            	}
            	break;
			           
            case R.id.stopButton :
            	if (DBG) log("recordButton/stopButton " + id + ")...");
            	if(PhoneUtils.isSoundRecording() == true)
            	{
            		
            			stopRecording();
	            		updateScreen();
	            		mInCallTouchUi.enableSpeakerphoneButton();
            	}
            	break;
            //<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]   -->				
				
            case R.id.manageConferencePhotoButton:
                // Show the Manage Conference panel.
                setInCallScreenMode(InCallScreenMode.MANAGE_CONFERENCE);
                break;
//20100322 SilentIncoming Start
//20100804 jongwany.lee@lge.com attached it for CALL UI
            case R.id.btn_accept:
                //20101101 sumi920.kim@lge.com FMC Call [START_LGE_LAB1]
                if(StarConfig.OPERATOR.equals("SKT"))
                {
	                if(PhoneApp.getInstance().getCallFMCState() == 2) //LGE_FMC
	                {
	                     mCallCard.mCommonFMC.setVisibility(View.INVISIBLE);
	                     PhoneApp.getInstance().EndCallFMC();  //�̻���
	                }
	                else  
	                {
	                	mCallCard.mCommonFMC.setVisibility(View.GONE);
	                    internalAnswerCall();
	                }
                }
                else
                //20101101 sumi920.kim@lge.com FMC Call [END_LGE_LAB1]
                	internalAnswerCall();
            break;
            case R.id.btn_send_sms:
            	internalSendSMS();
            break;
            case R.id.btn_reject:
            //internalHangupRingingCall();
            internalHangup();
            break;
//20100322 SilentIncoming End
// LGE_CALL_TRANSFER START
//            case R.id.transferButton: {
//                if (VDBG) log("onClick: menuTransfer...");
//                try {
//                    List<Connection> fgdConnections = mForegroundCall.getConnections();
//                    List<Connection> bgdConnections = mBackgroundCall.getConnections();
//                    int fgdConnectionsSize = fgdConnections.size();
//                    int bgdConnectionsSize = bgdConnections.size();
//                    if ((fgdConnections != null) && (bgdConnections != null)) {
//                        if ((fgdConnectionsSize == 1) && (bgdConnectionsSize == 1)) {
//                            if (mPhone.canTransfer()) {
//                                mPhone.explicitCallTransfer(false);
//                            }
//                        }
//                        else if (((fgdConnectionsSize == 0) && (bgdConnectionsSize == 1))
//                                || ((fgdConnectionsSize == 1) && (bgdConnectionsSize == 0))) {
//                            if (mInCallTouchUi != null && mInCallControlState != null) {
//                                if (mInCallControlState.canColdTransfer(mPhone)) {
//                                    final String callTransferCode = "CALL_TRANSFER";
//                                    Intent intent = new Intent();
//                                    intent.putExtra(callTransferCode, 2);
//                                    intent.setClass(this, SimpleCallDialog.class);
//                                    startActivity(intent);
//                                }
//                            }
//                        }
//                    }
//                }  
//                catch(CallStateException exc) {
//                    if (VDBG) log("onClick: menuTransfer...CallStateException");
//                    Toast.makeText(InCallScreen.this,R.string.incall_error_supp_service_transfer,Toast.LENGTH_LONG).show();
//                    break;
//                }
//                break;
//            }
            case R.id.newAccount:
            	mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	insertContact();
            	mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	break;
            case R.id.addAccount:
            	mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	addContact();
            	mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	break;
            case R.id.voiceCall:
            	mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	
            	if(StarConfig.COUNTRY.equals("KR"))
            	{
            		Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED);
            		final String number = getAddressNumber();
            		intent.setData(Uri.fromParts("tel", number, null));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);        
                    finish();
            	}
            	else
            	{
//            	Toast.makeText(this, "button click voiceCall!!!", 1000).show();
            	setInCallScreenMode(InCallScreenMode.NORMAL);
            	//hyojin.an 101020 start
            	Intent intent = getIntent(); 
            	String action = intent.getAction();
//20101027 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Lock/UnLock Incoming Scenario [START_LGE_LAB] 
				if(StarConfig.COUNTRY.equals("KR")){
					final String number = getAddressNumber();
					
					intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);

					LockScreenDecider lsd = new LockScreenDecider(PhoneApp.getInstance().getApplicationContext());
	                if(lsd.isSendingLock(number) && !PhoneApp.getInstance().getIsCheckPassword()) {
//20101102 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Lock/UnLock Incoming Scenario [START_LGE_LAB] 
						intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
//20101102 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Lock/UnLock Incoming Scenario [END_LGE_LAB] 
						intent.setClass(this, InCallPasswordInput.class); 	
						startActivity(intent);
	                }else{
	                	//20101110 sumi920.kim BroadCastMsg to HDVideoCall App [START_LGE_LAB1]
	                	InCallInitStatus status;
	                	if (action.equals(Intent.ACTION_CALL) || action.equals(Intent.ACTION_CALL_EMERGENCY))
	                		status = placeCall(intent); //20101111 sumi920.kim@lge.com Add status
	                	else{
	                		intent.setAction(Intent.ACTION_CALL);
	                		intent.setData(Uri.fromParts("tel", mCallCard.getDisplayPhoneNumer(), null));
	                		status = placeCall(intent); //20101111 sumi920.kim@lge.com Add status
	                	}
	                	
		            	mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
		            	//hyojin.an 101020 end	
		            	updateScreen();
		            	
		            	//20101110 sumi920.kim BroadCastMsg to HDVideoCall App [START_LGE_LAB1]
		            	if (DBG) log("redial InCallInitStatus " + status + ")...");
		            	if(StarConfig.OPERATOR.equals("SKT"))
		            	{
		            		if (DBG) log("redial mInCallTouchUi.isHDVideoCallAvailable() " + mInCallTouchUi.isHDVideoCallAvailable() + ")...");
					if(status == InCallInitStatus.SUCCESS)
			            	{
			            		final String NEW_OUTGOING_CALL_TO_LIVESHARE = "com.skt.call.intent.action.NEW_OUTGOING_CALL";
			            		//final String EXTRA_ALREADY_CALLED = "android.phone.extra.ALREADY_CALLED";
			            		//final String EXTRA_ORIGINAL_URI = "android.phone.extra.ORIGINAL_URI";
			            		
			            		Intent broadcastIntent = new Intent();

						if (DBG) log("before broadcastIntent " );

			            		if (broadcastIntent != null)
			            		{
			            			broadcastIntent.setAction(NEW_OUTGOING_CALL_TO_LIVESHARE);
			            			broadcastIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
			            			
			            			sendBroadcast(broadcastIntent);
									
							if (DBG) log("after sendBroadcast " );
			            		}

			            	}
		            	}
		            	//20101110 sumi920.kim BroadCastMsg to HDVideoCall App [END_LGE_LAB1]
		            	
	                }
				}else{
					if (action.equals(Intent.ACTION_CALL) || action.equals(Intent.ACTION_CALL_EMERGENCY)) 
	            		placeCall(intent);
	            	else{
	            		intent.setAction(Intent.ACTION_CALL);
	            		intent.setData(Uri.fromParts("tel", mCallCard.getDisplayPhoneNumer(), null));
	            		placeCall(intent);
	            	}

					mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
	            	//hyojin.an 101020 end	
	            	updateScreen();
				}
            	}
//20101027 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Lock/UnLock Incoming Scenario [END_LGE_LAB] 
            	break;
            case R.id.videoCall:
            	mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	
//            	Toast.makeText(this, "button click videoCall!!!", 1000).show();
            	//20101103sumi920.kim@lge.com
            	if (DBG) log("videoCall Button " + id + ")...");
            	if(StarConfig.COUNTRY.equals("KR"))
            	{
            		Intent i = new Intent();
            		final String reDialnumber = getAddressNumber();
					i.setAction(Intent.ACTION_CALL);
            		i.putExtra(Intent.EXTRA_PHONE_NUMBER, reDialnumber);
					if (DBG) log("videoCall reDialnumber " + reDialnumber + ")...");
            		placeVideoCall(i);
            	}
            	else
            		
            	placeVideoCall(getIntent());//<!--//20100929 sumi920.kim@lge.com VideoCall Origination In CallEnd View
//            	retryVTCall();
            	break;
            case R.id.message:
            	mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	sendSMS();
            	mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
            	break;
// LGE_CALL_TRANSFER END
            default:
                Log.w(LOG_TAG, "handleOnscreenButtonClick: unexpected ID " + id);
                break;
        }
        //20101024 sumi920.kim@lge.com porting [START_LGE_LAB1]
        //START youngmi.uhm@lge.com 2010. 8. 21. LAB1_CallUI P500 porting
        // LGE_UPDATE_S daewoomc.kim@lge.com 2010/08/15 turn on screen if touch mute & speakerphone buttons during a call.
        UpdateWakeUpScreenById(id);
        // LGE_UPDATE_E daewoomc.kim@lge.com 2010/08/15 turn on screen if touch mute & speakerphone buttons during a call.
        //END youngmi.uhm@lge.com2010. 8. 21. LAB1_CallUI P500 porting		
        //20101024 sumi920.kim@lge.com porting [END_LGE_LAB1]
        
        // Just in case the user clicked a "stateful" menu item (i.e. one
        // of the toggle buttons), we force the in-call buttons to update,
        // to make sure the user sees the *new* current state.
        //
        // (But note that some toggle buttons may *not* immediately change
        // the state of the Phone, in which case the updateInCallTouchUi()
        // call here won't have any visible effect.  Instead, those
        // buttons will get updated by the updateScreen() call that gets
        // triggered when the onPhoneStateChanged() event comes in.)
        //
        // TODO: updateInCallTouchUi() is overkill here; it would be
        // more efficient to update *only* the affected button(s).
        // Consider adding API for that.  (This is lo-pri since
        // updateInCallTouchUi() is pretty cheap already...)
        updateInCallTouchUi();
    }

	public String getAddressNumber() {
		if(DBG) log("getAddressNumber() Start..");
        Connection conn = null;

        int phoneType = mPhone.getPhoneType();
		if(DBG) log("getAddressNumber() phoneType="+ phoneType);		
        
        if (phoneType == Phone.PHONE_TYPE_CDMA) {
        conn = mForegroundCall.getLatestConnection();
        //In MT call 
            if(conn == null)
            {
                conn = mRingingCall.getLatestConnection();
            }
        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
            conn = mForegroundCall.getEarliestConnection();
            //In MT call
            if(conn == null)
            {
                conn = mRingingCall.getLatestConnection();
            }
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }
		if(DBG) log("getAddressNumber() conn="+ conn+", number="+conn.getAddress());				
        
        return conn.getAddress();
    }

		
    //20101024 sumi920.kim@lge.com porting [START_LGE_LAB1]
    //START youngmi.uhm@lge.com 2010. 8. 21. LAB1_CallUI P500 porting
    // LGE_UPDATE_S daewoomc.kim@lge.com 2010/08/15 turn on screen if touch mute & speakerphone buttons during a call.	
    private void UpdateWakeUpScreenById(int id) {
    	final PhoneApp app = PhoneApp.getInstance();
    	switch(id) {
	  		case R.id.swapButton :
	  		case R.id.holdButton :
	  		case R.id.muteButton :
	  		case R.id.speakerButton :
	  		case R.id.recordButton :
	  		case R.id.stopButton :
	  			if (DBG) log("updateWakeUpById().....");
	  			app.wakeUpScreen();
	  			break;
    	}
    }
    // LGE_UPDATE_E daewoomc.kim@lge.com 2010/08/15 turn on screen if touch mute & speakerphone buttons during a call.
    //END youngmi.uhm@lge.com2010. 8. 21. LAB1_CallUI P500 porting
    //20101023 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]
    
  public void stopRecording() {
		// TODO Auto-generated method stub
 		PhoneUtils.setSoundRecording(false);
		mSoundRecord.stopRecording(InCallScreen.this);

		Intent play_finish = new Intent();
		play_finish.setAction("voice_video_record_finish");
		sendBroadcast(play_finish);
	}
  //20101023 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]
//20101009 sumi920.kim@lge.com OptionMenu InCall [START_LGE_LAB1]
    private void gotoHDVideoCall() {
		// TODO Auto-generated method stub
    	String COMMAND_BROADCAST_EVENT= "com.skt.skaf.ims.aoa.intent.action.EVENT";
    	String LIVESHARE_EXTRA_STARTCOMMAND = "LiveshareStart";
    	
    	//Toast.makeText(this, "gogo HDVideoCall", 1000).show();
    	if (DBG) log("gotoHDVideoCall...");
    	
    	Intent intent = new Intent();
    	if (intent != null)
    	{
    	  intent.setAction(COMMAND_BROADCAST_EVENT);
    	  intent.putExtra (LIVESHARE_EXTRA_STARTCOMMAND, true);	
    	  sendBroadcast(intent);
    	}
    	else
    		Toast.makeText(this, getString(R.string.incall_error_supp_service_unknown), 1000).show();
   	
    }
  //20101009 sumi920.kim@lge.com OptionMenu InCall [END_LGE_LAB1]
    // END_CALL send SMS, addContact
    void sendSMS() {
		if(dialNumber == null || dialNumber == "")
		{
			Toast.makeText(this, "Error!! phone number is null", 1000).show();
			return;
		}
    	
 		//<!--[sumi920.kim@lge.com] 2010.10.01	LAB1_CallUI --> Message Intent [START] -->  
		if(StarConfig.COUNTRY.equals("KR"))
		{
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("smsto:" + dialNumber));  // ������ ��ȭ��ȣ
			startActivity(intent);
		}
		else//<!--[sumi920.kim@lge.com] 2010.10.01	LAB1_CallUI --> Message Intent [END]  
		{
			Intent intent = new Intent (Intent.ACTION_SENDTO,
    			Uri.fromParts(SCHEME_SMSTO, dialNumber, null));  
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
    }
    
    private void addContact(){
//    	Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT); // "com.android.contacts.action.PICK_AND_EDIT"
    	Intent intent = new Intent("com.android.contacts.action.INSERT_OR_EDIT");
        intent.putExtra(Insert.PHONE, dialNumber);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        startActivity(intent);
    }
    
    private void insertContact()
    {
    	Intent intent = new Intent(Intent.ACTION_INSERT);
    	intent.putExtra(Insert.PHONE, dialNumber);
    	intent.setType(Contacts.CONTENT_TYPE);
    	startActivity(intent);
    }
    
    private void retryVTCall()
    {
    	String number = mCallCard.getDisplayPhoneNumer();
    	
    	Intent intent = new Intent(Intent.ACTION_VIDEO_CALL_PRIVILEGED, 
    			Uri.fromParts("tel", number, null));
    	if(number == null || !TextUtils.isGraphic(number))
    	{
    		return;
    	}
    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(intent);
    }
    //
    
    /**
     * Update the network provider's overlay based on the value of
     * mProviderOverlayVisible.
     * If false the overlay is hidden otherwise it is shown.  A
     * delayed message is posted to take the overalay down after
     * PROVIDER_OVERLAY_TIMEOUT. This ensures the user will see the
     * overlay even if the call setup phase is very short.
     */
    private void updateProviderOverlay() {
        if (VDBG) log("updateProviderOverlay: " + mProviderOverlayVisible);

        ViewGroup overlay = (ViewGroup) findViewById(R.id.inCallProviderOverlay);

        if (mProviderOverlayVisible) {
            CharSequence template = getText(R.string.calling_via_template);
            CharSequence text = TextUtils.expandTemplate(template, mProviderLabel,
                                                         mProviderAddress);

            TextView message = (TextView) findViewById(R.id.callingVia);
            message.setCompoundDrawablesWithIntrinsicBounds(mProviderIcon, null, null, null);
            message.setText(text);

            overlay.setVisibility(View.VISIBLE);

            // Remove any zombie messages and then send a message to
            // self to remove the overlay after some time.
            mHandler.removeMessages(EVENT_HIDE_PROVIDER_OVERLAY);
            Message msg = Message.obtain(mHandler, EVENT_HIDE_PROVIDER_OVERLAY);
            mHandler.sendMessageDelayed(msg, PROVIDER_OVERLAY_TIMEOUT);
        } else {
            overlay.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the "Press Menu for more options" hint based on the current
     * state of the Phone.
     */
    private void updateMenuButtonHint() {
        if (VDBG) log("updateMenuButtonHint()...");
        boolean hintVisible = true;

        final boolean hasRingingCall = !mRingingCall.isIdle();
        final boolean hasActiveCall = !mForegroundCall.isIdle();
        final boolean hasHoldingCall = !mBackgroundCall.isIdle();

        // The hint is hidden only when there's no menu at all,
        // which only happens in a few specific cases:
        if (mInCallScreenMode == InCallScreenMode.CALL_ENDED) {
            // The "Call ended" state.
            hintVisible = false;
        } else if (hasRingingCall && !(hasActiveCall && !hasHoldingCall)) {
            // An incoming call where you *don't* have the option to
            // "answer & end" or "answer & hold".
            hintVisible = false;
        } else if (!phoneIsInUse()) {
            // Or if the phone is totally idle (like if an error dialog
            // is up, or an MMI is running.)
            hintVisible = false;
        }

        // The hint is also hidden on devices where we use onscreen
        // touchable buttons instead.
        if (isTouchUiEnabled()) {
            hintVisible = false;
        }

        // Also, if an incoming call is ringing, hide the hint if the
        // "incoming call" touch UI is present (since the SlidingTab
        // widget takes up a lot of space and the hint would collide with
        // it.)
        if (hasRingingCall && isIncomingCallTouchUiEnabled()) {
            hintVisible = false;
        }

        int hintVisibility = (hintVisible) ? View.VISIBLE : View.GONE;
        mCallCard.getMenuButtonHint().setVisibility(hintVisibility);

        // TODO: Consider hiding the hint(s) whenever the menu is onscreen!
        // (Currently, the menu is rendered on top of the hint, but the
        // menu is semitransparent so you can still see the hint
        // underneath, and the hint is *just* visible enough to be
        // distracting.)
    }

    /**
     * Brings up UI to handle the various error conditions that
     * can occur when first initializing the in-call UI.
     * This is called from onResume() if we encountered
     * an error while processing our initial Intent.
     *
     * @param status one of the InCallInitStatus error codes.
     */
    private void handleStartupError(InCallInitStatus status) {
        if (DBG) log("handleStartupError(): status = " + status);

        // NOTE that the regular Phone UI is in an uninitialized state at
        // this point, so we don't ever want the user to see it.
        // That means:
        // - Any cases here that need to go to some other activity should
        //   call startActivity() AND immediately call endInCallScreenSession
        //   on this one.
        // - Any cases here that bring up a Dialog must ensure that the
        //   Dialog handles both OK *and* cancel by calling endInCallScreenSession.
        //   Activity.  (See showGenericErrorDialog() for an example.)

        switch (status) {

            case VOICEMAIL_NUMBER_MISSING:
                // Bring up the "Missing Voicemail Number" dialog, which
                // will ultimately take us to some other Activity (or else
                // just bail out of this activity.)
                handleMissingVoiceMailNumber();
                break;

            case POWER_OFF:
                // Radio is explictly powered off.

                // TODO: This UI is ultra-simple for 1.0.  It would be nicer
                // to bring up a Dialog instead with the option "turn on radio
                // now".  If selected, we'd turn the radio on, wait for
                // network registration to complete, and then make the call.

                showGenericErrorDialog(R.string.incall_error_power_off, true);
                break;

            case EMERGENCY_ONLY:
                // Only emergency numbers are allowed, but we tried to dial
                // a non-emergency number.
               showGenericErrorDialog(R.string.incall_error_emergency_only, true);
//20100804 jongwany.lee@lge.com attached it for CALL UI
               //inCallControl_GONE_Flag = true;
               break;

            case OUT_OF_SERVICE:
                // No network connection.
                showGenericErrorDialog(R.string.incall_error_out_of_service, true);
                //inCallControl_GONE_Flag = true;
                break;

            case PHONE_NOT_IN_USE:
                // This error is handled directly in onResume() (by bailing
                // out of the activity.)  We should never see it here.
                Log.w(LOG_TAG,
                      "handleStartupError: unexpected PHONE_NOT_IN_USE status");
                break;

            case NO_PHONE_NUMBER_SUPPLIED:
                // The supplied Intent didn't contain a valid phone number.
                // TODO: Need UI spec for this failure case; for now just
                // show a generic error.
                showGenericErrorDialog(R.string.incall_error_no_phone_number_supplied, true);
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
                if (mPhone.getState() == Phone.State.OFFHOOK) {
                    Toast.makeText(this, R.string.incall_status_dialed_mmi, Toast.LENGTH_SHORT)
                        .show();
                }
                break;

            case CALL_FAILED:
                // We couldn't successfully place the call; there was some
                // failure in the telephony layer.
                // TODO: Need UI spec for this failure case; for now just
                // show a generic error.
                showGenericErrorDialog(R.string.incall_error_call_failed, true);
                break;

            default:
                Log.w(LOG_TAG, "handleStartupError: unexpected status code " + status);
                showGenericErrorDialog(R.string.incall_error_call_failed, true);
                break;
        }
    }

    /**
     * Utility function to bring up a generic "error" dialog, and then bail
     * out of the in-call UI when the user hits OK (or the BACK button.)
     */
    private void showGenericErrorDialog(int resid, boolean isStartupError) {
        CharSequence msg = getResources().getText(resid);
        if (DBG) log("showGenericErrorDialog('" + msg + "')...");

        // create the clicklistener and cancel listener as needed.
        DialogInterface.OnClickListener clickListener;
        OnCancelListener cancelListener;
        if (isStartupError) {
            clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bailOutAfterErrorDialog();
                }};
            cancelListener = new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    bailOutAfterErrorDialog();
                }};
        } else {
            clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
// LGE_AUTO_REDIAL START
                	delayedCleanupAfterDisconnect(null);
// LGE_AUTO_REDIAL END
                }};
            cancelListener = new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
// LGE_AUTO_REDIAL START
                	delayedCleanupAfterDisconnect(null);
// LGE_AUTO_REDIAL END
                }};
        }

        // TODO: Consider adding a setTitle() call here (with some generic
        // "failure" title?)
        mGenericErrorDialog = new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, clickListener)
                .setOnCancelListener(cancelListener)
                .create();

        // When the dialog is up, completely hide the in-call UI
        // underneath (which is in a partially-constructed state).
        mGenericErrorDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mGenericErrorDialog.show();
     // 20101014 jongwany.lee@lge.com TD issue fixed 9044, 9299, 9711 regarding dismissed popup dialog
        mGenericErrorDialog.setOnKeyListener(new OnKeyListener(){

			
			public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
				if(arg2.getKeyCode() == 84){
					return true;
				}
				return false;
			}});
        
    }
    
    private void showCallLostDialog() {
        if (DBG) log("showCallLostDialog()...");

        // Don't need to show the dialog if InCallScreen isn't in the forgeround
        if (!mIsForegroundActivity) {
            if (DBG) log("showCallLostDialog: not the foreground Activity! Bailing out...");
            return;
        }

        // Don't need to show the dialog again, if there is one already.
        if (mCallLostDialog != null) {
            if (DBG) log("showCallLostDialog: There is a mCallLostDialog already.");
            return;
        }

        mCallLostDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.call_lost)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create();
        mCallLostDialog.show();
    }

    private void bailOutAfterErrorDialog() {
        if (mGenericErrorDialog != null) {
            if (DBG) log("bailOutAfterErrorDialog: DISMISSING mGenericErrorDialog.");
            mGenericErrorDialog.dismiss();
            mGenericErrorDialog = null;
        }
        if (DBG) log("bailOutAfterErrorDialog(): end InCallScreen session...");
        endInCallScreenSession();
    }

    /**
     * Dismisses (and nulls out) all persistent Dialogs managed
     * by the InCallScreen.  Useful if (a) we're about to bring up
     * a dialog and want to pre-empt any currently visible dialogs,
     * or (b) as a cleanup step when the Activity is going away.
     */
    private void dismissAllDialogs() {
        if (DBG) log("dismissAllDialogs()...");

        // Note it's safe to dismiss() a dialog that's already dismissed.
        // (Even if the AlertDialog object(s) below are still around, it's
        // possible that the actual dialog(s) may have already been
        // dismissed by the user.)

        if (mMissingVoicemailDialog != null) {
            if (VDBG) log("- DISMISSING mMissingVoicemailDialog.");
            mMissingVoicemailDialog.dismiss();
            mMissingVoicemailDialog = null;
        }
        if (mMmiStartedDialog != null) {
            if (VDBG) log("- DISMISSING mMmiStartedDialog.");
            mMmiStartedDialog.dismiss();
            mMmiStartedDialog = null;
        }
        if (mGenericErrorDialog != null) {
            if (VDBG) log("- DISMISSING mGenericErrorDialog.");
            mGenericErrorDialog.dismiss();
            mGenericErrorDialog = null;
        }
        if (mSuppServiceFailureDialog != null) {
            if (VDBG) log("- DISMISSING mSuppServiceFailureDialog.");
            mSuppServiceFailureDialog.dismiss();
            mSuppServiceFailureDialog = null;
        }
        if (mWaitPromptDialog != null) {
            if (VDBG) log("- DISMISSING mWaitPromptDialog.");
            mWaitPromptDialog.dismiss();
            mWaitPromptDialog = null;
        }
        if (mWildPromptDialog != null) {
            if (VDBG) log("- DISMISSING mWildPromptDialog.");
            mWildPromptDialog.dismiss();
            mWildPromptDialog = null;
        }
        if (mCallLostDialog != null) {
            if (VDBG) log("- DISMISSING mCallLostDialog.");
            mCallLostDialog.dismiss();
            mCallLostDialog = null;
        }
        if ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL
                || mInCallScreenMode == InCallScreenMode.OTA_ENDED)
                && otaUtils != null) {
            otaUtils.dismissAllOtaDialogs();
        }
        if (mPausePromptDialog != null) {
            if (DBG) log("- DISMISSING mPausePromptDialog.");
            mPausePromptDialog.dismiss();
            mPausePromptDialog = null;
        }
        //START sumi920.kim@lge.com 2010. 10. 06. LAB1_CallUI handle answer of incoming call if 3Way Conv
        // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
        if (mSelectCallEndDialog != null){
        	if (DBG) log("- DISMISSING mSelectCallEndDialog.");
        	mSelectCallEndDialog.dismiss();
        	mSelectCallEndDialog = null;
        }
        // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-22 : handle answer of incoming call if 3Way Conv
        //END sumi920.kim@lge.com 2010. 10. 06.LAB1_CallUI handle answer of incoming call if 3Way Conv

    }


    //
    // Helper functions for answering incoming calls.
    //

    /**
     * Answer a ringing call.  This method does nothing if there's no
     * ringing or waiting call.
     */
    /* package */ void internalAnswerCall() {
        // if (DBG) log("internalAnswerCall()...");
        // if (DBG) PhoneUtils.dumpCallState(mPhone);
		Log.d("CALL Time Check", "MT: Click Accept Button");
        
        final boolean hasRingingCall = !mRingingCall.isIdle();

        if (hasRingingCall) {

            //jundj@mo2.co.kr START
        	if(VEUtils.isSKTFeature()){
	            VEUtils.setVEPlayerStatus(VEUtils.IsVEPlayerRunning.NO);
	            VEUtils.setVisibleCallCardPersonInfo(VEUtils.CALLCARD_PERSONINFO_DEFAULT);
	            PhoneApp.getInstance().mHandler.sendEmptyMessage(VE_ContentManager.HANDLE_MSG_STOP_PLAY);
        	}
            //jundj@mo2.co.kr END

            int phoneType = mPhone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                if (DBG) log("internalAnswerCall: answering (CDMA)...");
                // In CDMA this is simply a wrapper around PhoneUtils.answerCall().
                PhoneUtils.answerCall(mPhone);  // Automatically holds the current active call,
                                                // if there is one
            } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                // GSM: this is usually just a wrapper around
                // PhoneUtils.answerCall(), *but* we also need to do
                // something special for the "both lines in use" case.

                final boolean hasActiveCall = !mForegroundCall.isIdle();
                final boolean hasHoldingCall = !mBackgroundCall.isIdle();

                if (hasActiveCall && hasHoldingCall) {
                    if (DBG) log("internalAnswerCall: answering (both lines in use!)...");
                    // The relatively rare case where both lines are
                    // already in use.  We "answer incoming, end ongoing"
                    // in this case, according to the current UI spec.
                    
                    //20101006 sumi920.kim@lge.com 3rd call inComming call in 3way calling [START_LGE_LAB1]
                    if(StarConfig.OPERATOR.equals("SKT"))
                    {
                    	internalAnswerCallBothLinesInUse();
                    }
                    else
                    //20101006 sumi920.kim@lge.com 3rd call inComming call in 3way calling [END_LGE_LAB1]
                    	PhoneUtils.answerAndEndActive(mPhone);

                    // Alternatively, we could use
                    //    PhoneUtils.answerAndEndHolding(mPhone);
                    // here to end the on-hold call instead.
                } else {
                    if (DBG) log("internalAnswerCall: answering...");
                    PhoneUtils.answerCall(mPhone);  // Automatically holds the current active call,
                                                    // if there is one
                }
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
    }
    //20101006 sumi920.kim@lge.com 3rd call inComming call in 3way calling [START_LGE_LAB1]
    void internalAnswerAndEndActive() {
    	if (DBG) log("internalAnswerAndEndActive()...");
    	if (DBG) log("internalAnswerAndEndActive()... : answerCall()");
    	//PhoneUtils.answerCall(mPhone);
    	PhoneUtils.hangupActiveCall(mPhone);
    }
    void internalAnswerAndEndHold() {
    	if (DBG) log("internalAnswerAndEndHold()...");
    	PhoneUtils.hangupHoldingCall(mPhone);
    	
    	mHandler.removeMessages(ANSWER_RINGINGCALL_IN_THREEWAYCALL);
        Message msg = Message.obtain(mHandler, ANSWER_RINGINGCALL_IN_THREEWAYCALL);
        mHandler.sendMessageDelayed(msg, ANSWER_RINGINGCALL_IN_THREEWAYCALL_TIMEDELAY);
    }
    
    private void internalAnswerCallBothLinesInUse() {
        if (DBG) log("internalAnswerCallBothLinesInUse()...");

        Call ringcall = mPhone.getRingingCall();
        Call forecall = mPhone.getForegroundCall();
        Call backcall = mPhone.getBackgroundCall();
        if (DBG) log("internalAnswerCallBothLinesInUse() : ringcall = " + ringcall);
        if (DBG) log("internalAnswerCallBothLinesInUse() : forecall = " + forecall);
        if (DBG) log("internalAnswerCallBothLinesInUse() : backcall = " + backcall);
        Connection ringconn = ringcall.getLatestConnection();
        Connection foreconn = forecall.getLatestConnection();
        Connection backconn = backcall.getLatestConnection();
        if (DBG) log("internalAnswerCallBothLinesInUse() : ringconn = " + ringconn.getAddress());
        if (DBG) log("internalAnswerCallBothLinesInUse() : foreconn = " + foreconn.getAddress());
        if (DBG) log("internalAnswerCallBothLinesInUse() : backconn = " + backconn.getAddress());

        CharSequence[] numbers = new CharSequence[2];

        CallerInfo foreinfo = PhoneUtils.getCallerInfo(getBaseContext(), foreconn);
        if (foreinfo != null) {
//         numbers[0] = foreconn.getAddress();
           StringBuilder buf = new StringBuilder();
           buf.append(getResources().getText(R.string.incall_select_call_end_value_0));
           if (!TextUtils.isEmpty(foreinfo.name)) {
              buf.append(foreinfo.name);
           } else {
              buf.append(foreinfo.phoneNumber);
           }
           numbers[0] = buf.toString();
        }

        CallerInfo backinfo = PhoneUtils.getCallerInfo(getBaseContext(), backconn);
        if (backinfo != null) {
//         numbers[1] = backconn.getAddress();
           StringBuilder buf = new StringBuilder();
           buf.append(getResources().getText(R.string.incall_select_call_end_value_1));
           if (!TextUtils.isEmpty(backinfo.name)) {
              buf.append(backinfo.name);
           } else {
              buf.append(backinfo.phoneNumber);
           }
           numbers[1] = buf.toString();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.incall_select_call_end_title);
        builder.setItems(numbers, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                mSelectCallEndDialog.dismiss();
                if (item == 0) {
                    internalAnswerAndEndActive();
                } else {
                    internalAnswerAndEndHold();
                }
            }
        });
        mSelectCallEndDialog = builder.create();
        mSelectCallEndDialog.show();    }
    //20101006 sumi920.kim@lge.com 3rd call inComming call in 3way calling [END_LGE_LAB1]

    /**
     * Answer the ringing call *and* hang up the ongoing call.
     */
    /* package */ void internalAnswerAndEnd() {
        if (DBG) log("internalAnswerAndEnd()...");
        // if (DBG) PhoneUtils.dumpCallState(mPhone);
        PhoneUtils.answerAndEndActive(mPhone);
    }

    /**
     * Hang up the ringing call (aka "Don't answer").
     */
    /* package */ void internalHangupRingingCall() {
        if (DBG) log("internalHangupRingingCall()...");
        PhoneUtils.hangupRingingCall(mPhone);
        
        //jundj@mo2.co.kr START
        if(VEUtils.isSKTFeature()){
	        VEUtils.setVEPlayerStatus(VEUtils.IsVEPlayerRunning.NO);
	        VEUtils.setVisibleCallCardPersonInfo(VEUtils.CALLCARD_PERSONINFO_DEFAULT);
	        PhoneApp.getInstance().mHandler.sendEmptyMessage(VE_ContentManager.HANDLE_MSG_STOP_PLAY);
        }
        //jundj@mo2.co.kr END
    }

// LGE_UI_INCOMMING_CALL START
// NOTE: this code block should be removed for Eclair

 /**
     * End the active call.
     */
    /* package */ void internalHangupCall() {
        if (DBG) log("internalHangupCall()...");
        PhoneUtils.hangup(mPhone);
    }
// LGE_UI_INCOMMING_CALL END

    /**
     * Hang up the current active call.
     */
    /* package */ void internalHangup() {
        if (DBG) log("internalHangup()...");
        PhoneUtils.hangup(mPhone);
        
        //jundj@mo2.co.kr START
        if(VEUtils.isSKTFeature()){
	        VEUtils.setVEPlayerStatus(VEUtils.IsVEPlayerRunning.NO);
	        VEUtils.setVisibleCallCardPersonInfo(VEUtils.CALLCARD_PERSONINFO_DEFAULT);
	        PhoneApp.getInstance().mHandler.sendEmptyMessage(VE_ContentManager.HANDLE_MSG_STOP_PLAY);
        }
        //jundj@mo2.co.kr END
    }

    /**
     * InCallScreen-specific wrapper around PhoneUtils.switchHoldingAndActive().
     */
    private void internalSwapCalls() {
        if (DBG) log("internalSwapCalls()...");

        // Any time we swap calls, force the DTMF dialpad to close.
        // (We want the regular in-call UI to be visible right now, so the
        // user can clearly see which call is now in the foreground.)
        mDialer.closeDialer(true);  // do the "closing" animation

        // Also, clear out the "history" of DTMF digits you typed, to make
        // sure you don't see digits from call #1 while call #2 is active.
        // (Yes, this does mean that swapping calls twice will cause you
        // to lose any previous digits from the current call; see the TODO
        // comment on DTMFTwelvKeyDialer.clearDigits() for more info.)
        mDialer.clearDigits();

        // Swap the fg and bg calls.
        PhoneUtils.switchHoldingAndActive(mPhone);

        // If we have a valid BluetoothHandsfree then since CDMA network or
        // Telephony FW does not send us information on which caller got swapped
        // we need to update the second call active state in BluetoothHandsfree internally
        if (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
            BluetoothHandsfree bthf = PhoneApp.getInstance().getBluetoothHandsfree();
            if (bthf != null) {
                bthf.cdmaSwapSecondCallState();
            }
        }

    }

    /**
     * Sets the current high-level "mode" of the in-call UI.
     *
     * NOTE: if newMode is CALL_ENDED, the caller is responsible for
     * posting a delayed DELAYED_CLEANUP_AFTER_DISCONNECT message, to make
     * sure the "call ended" state goes away after a couple of seconds.
     */
    private void setInCallScreenMode(InCallScreenMode newMode) {
        if (DBG) log("setInCallScreenMode: " + newMode);
        mInCallScreenMode = newMode;
        switch (mInCallScreenMode) {
            case MANAGE_CONFERENCE:
                if (!PhoneUtils.isConferenceCall(mForegroundCall)) {
                    Log.w(LOG_TAG, "MANAGE_CONFERENCE: no active conference call!");
                    // Hide the Manage Conference panel, return to NORMAL mode.
                    setInCallScreenMode(InCallScreenMode.NORMAL);
                    return;
                }
                List<Connection> connections = mForegroundCall.getConnections();
                // There almost certainly will be > 1 connection,
                // since isConferenceCall() just returned true.
                if ((connections == null) || (connections.size() <= 1)) {
                    Log.w(LOG_TAG,
                          "MANAGE_CONFERENCE: Bogus TRUE from isConferenceCall(); connections = "
                          + connections);
                    // Hide the Manage Conference panel, return to NORMAL mode.
                    setInCallScreenMode(InCallScreenMode.NORMAL);
                    return;
                }

                // TODO: Don't do this here. The call to
                // initManageConferencePanel() should instead happen
                // automagically in ManageConferenceUtils the very first
                // time you call updateManageConferencePanel() or
                // setPanelVisible(true).
                mManageConferenceUtils.initManageConferencePanel();  // if necessary

                mManageConferenceUtils.updateManageConferencePanel(connections);

                // The "Manage conference" UI takes up the full main frame,
                // replacing the inCallPanel and CallCard PopupWindow.
                mManageConferenceUtils.setPanelVisible(true);

                // Start the chronometer.
                // TODO: Similarly, we shouldn't expose startConferenceTime()
                // and stopConferenceTime(); the ManageConferenceUtils
                // class ought to manage the conferenceTime widget itself
                // based on setPanelVisible() calls.
                long callDuration = mForegroundCall.getEarliestConnection().getDurationMillis();
                mManageConferenceUtils.startConferenceTime(
                        SystemClock.elapsedRealtime() - callDuration);

                mInCallPanel.setVisibility(View.GONE);

                // No need to close the dialer here, since the Manage
                // Conference UI will just cover it up anyway.

                break;

            case CALL_ENDED:
                // Display the CallCard (in the "Call ended" state)
                // and hide all other UI.

                mManageConferenceUtils.setPanelVisible(false);
                mManageConferenceUtils.stopConferenceTime();

                updateMenuButtonHint();  // Hide the Menu button hint

                // Make sure the CallCard (which is a child of mInCallPanel) is visible.
                mInCallPanel.setVisibility(View.VISIBLE);

                break;

            case NORMAL:
                mInCallPanel.setVisibility(View.VISIBLE);
                mManageConferenceUtils.setPanelVisible(false);
                mManageConferenceUtils.stopConferenceTime();
                break;

            case OTA_NORMAL:
                otaUtils.setCdmaOtaInCallScreenUiState(
                        OtaUtils.CdmaOtaInCallScreenUiState.State.NORMAL);
                mInCallPanel.setVisibility(View.GONE);
                break;

            case OTA_ENDED:
                otaUtils.setCdmaOtaInCallScreenUiState(
                        OtaUtils.CdmaOtaInCallScreenUiState.State.ENDED);
                mInCallPanel.setVisibility(View.GONE);
                break;

			case DIALING: //hyojin.an@lge.com 101001
                //jundj@mo2.co.kr start
            	if(VEUtils.isSKTFeature()){
            		VEUtils.setVisibleCallCardPersonInfo(VEUtils.CALLCARD_PERSONINFO_DEFAULT);
            	}
                //jundj@mo2.co.kr end
				mInCallPanel.setVisibility(View.VISIBLE);
				break;

            case UNDEFINED:
                // Set our Activities intent to ACTION_UNDEFINED so
                // that if we get resumed after we've completed a call
                // the next call will not cause checkIsOtaCall to
                // return true.
                //
                // With the framework as of October 2009 the sequence below
                // causes the framework to call onResume, onPause, onNewIntent,
                // onResume. If we don't call setIntent below then when the
                // first onResume calls checkIsOtaCall via initOtaState it will
                // return true and the Activity will be confused.
                //
                //  1) Power up Phone A
                //  2) Place *22899 call and activate Phone A
                //  3) Press the power key on Phone A to turn off the display
                //  4) Call Phone A from Phone B answering Phone A
                //  5) The screen will be blank (Should be normal InCallScreen)
                //  6) Hang up the Phone B
                //  7) Phone A displays the activation screen.
                //
                // Step 3 is the critical step to cause the onResume, onPause
                // onNewIntent, onResume sequence. If step 3 is skipped the
                // sequence will be onNewIntent, onResume and all will be well.
                setIntent(new Intent(ACTION_UNDEFINED));

                // Cleanup Ota Screen if necessary and set the panel
                // to VISIBLE.
                if (mPhone.getState() != Phone.State.OFFHOOK) {
                    if (otaUtils != null) {
                        otaUtils.cleanOtaScreen(true);
                    }
                } else {
                    log("WARNING: Setting mode to UNDEFINED but phone is OFFHOOK,"
                            + " skip cleanOtaScreen.");
                }
                mInCallPanel.setVisibility(View.VISIBLE);
                break;
        }

        // Update the visibility of the DTMF dialer tab on any state
        // change.
        updateDialpadVisibility();

        // Update the in-call touch UI on any state change (since it may
        // need to hide or re-show itself.)
        updateInCallTouchUi();
    }

    /**
     * @return true if the "Manage conference" UI is currently visible.
     */
    /* package */ boolean isManageConferenceMode() {
        return (mInCallScreenMode == InCallScreenMode.MANAGE_CONFERENCE);
    }

    /**
     * Checks if the "Manage conference" UI needs to be updated.
     * If the state of the current conference call has changed
     * since our previous call to updateManageConferencePanel()),
     * do a fresh update.  Also, if the current call is no longer a
     * conference call at all, bail out of the "Manage conference" UI and
     * return to InCallScreenMode.NORMAL mode.
     */
    private void updateManageConferencePanelIfNecessary() {
        if (VDBG) log("updateManageConferencePanelIfNecessary: " + mForegroundCall + "...");

        List<Connection> connections = mForegroundCall.getConnections();
        if (connections == null) {
            if (VDBG) log("==> no connections on foreground call!");
            // Hide the Manage Conference panel, return to NORMAL mode.
            setInCallScreenMode(InCallScreenMode.NORMAL);
            InCallInitStatus status = syncWithPhoneState();
            if (status != InCallInitStatus.SUCCESS) {
                Log.w(LOG_TAG, "- syncWithPhoneState failed! status = " + status);
                // We shouldn't even be in the in-call UI in the first
                // place, so bail out:
                if (DBG) log("updateManageConferencePanelIfNecessary: endInCallScreenSession... 1");
                endInCallScreenSession();
                return;
            }
            return;
        }

        int numConnections = connections.size();
        if (numConnections <= 1) {
            if (VDBG) log("==> foreground call no longer a conference!");
            // Hide the Manage Conference panel, return to NORMAL mode.
            setInCallScreenMode(InCallScreenMode.NORMAL);
            InCallInitStatus status = syncWithPhoneState();
            if (status != InCallInitStatus.SUCCESS) {
                Log.w(LOG_TAG, "- syncWithPhoneState failed! status = " + status);
                // We shouldn't even be in the in-call UI in the first
                // place, so bail out:
                if (DBG) log("updateManageConferencePanelIfNecessary: endInCallScreenSession... 2");
                endInCallScreenSession();
                return;
            }
            return;
        }

        // TODO: the test to see if numConnections has changed can go in
        // updateManageConferencePanel(), rather than here.
        if (numConnections != mManageConferenceUtils.getNumCallersInConference()) {
            if (VDBG) log("==> Conference size has changed; need to rebuild UI!");
            mManageConferenceUtils.updateManageConferencePanel(connections);
        }
    }

    /**
     * Updates the visibility of the DTMF dialpad (and its onscreen
     * "handle", if applicable), based on the current state of the phone
     * and/or the current InCallScreenMode.
     */
    private void updateDialpadVisibility() {
        //
        // (1) The dialpad itself:
        //
        // If an incoming call is ringing, make sure the dialpad is
        // closed.  (We do this to make sure we're not covering up the
        // "incoming call" UI, and especially to make sure that the "touch
        // lock" overlay won't appear.)
        if (mPhone.getState() == Phone.State.RINGING) {
            mDialer.closeDialer(false);  // don't do the "closing" animation

            // Also, clear out the "history" of DTMF digits you may have typed
            // into the previous call (so you don't see the previous call's
            // digits if you answer this call and then bring up the dialpad.)
            //
            // TODO: it would be more precise to do this when you *answer* the
            // incoming call, rather than as soon as it starts ringing, but
            // the InCallScreen doesn't keep enough state right now to notice
            // that specific transition in onPhoneStateChanged().
            mDialer.clearDigits();
        }

        //
        // (2) The onscreen "handle":
        //
        // The handle is visible only if it's OK to actually open the
        // dialpad.  (Note this is meaningful only on platforms that use a
        // SlidingDrawer as a container for the dialpad.)
        mDialer.setHandleVisible(okToShowDialpad());

        //
        // (3) The main in-call panel (containing the CallCard):
        //
        // On some platforms(*) we need to hide the CallCard (which is a
        // child of mInCallPanel) while the dialpad is visible.
        //
        // (*) We need to do this when using the dialpad from the
        //     InCallTouchUi widget, but not when using the
        //     SlidingDrawer-based dialpad, because the SlidingDrawer itself
        //     is opaque.)
        if (!mDialer.usingSlidingDrawer()) {
            if (mDialerView != null) {
                mDialerView.setKeysBackgroundResource(
                        isBluetoothAudioConnected() ? R.drawable.btn_dial_blue
                        : R.drawable.btn_dial_green);
            }

            if (isDialerOpened()) {
                mInCallPanel.setVisibility(View.GONE);
            } else {
                // Dialpad is dismissed; bring back the CallCard if
                // it's supposed to be visible.
//20101108 wonho.moon@lge.com TD : 50088 [START_LGE_LAB1] 
				if ((mInCallScreenMode == InCallScreenMode.DIALING) ||
//20101108 wonho.moon@lge.com TD : 50088 [START_LGE_LAB1] 
					(mInCallScreenMode == InCallScreenMode.NORMAL)
					|| (mInCallScreenMode == InCallScreenMode.CALL_ENDED)) {
					mInCallPanel.setVisibility(View.VISIBLE);
				}

            }
        }
    }

    /**
     * @return true if the DTMF dialpad is currently visible.
     */
    /* package */ boolean isDialerOpened() {
        return (mDialer != null && mDialer.isOpened());
    }

    /**
     * Called any time the DTMF dialpad is opened.
     * @see DTMFTwelveKeyDialer.onDialerOpen()
     */
    /* package */ void onDialerOpen() {
        if (DBG) log("onDialerOpen()...");

        // ANY time the dialpad becomes visible, start the timer that will
        // eventually bring up the "touch lock" overlay.
        resetTouchLockTimer();

        // Update the in-call touch UI (which may need to hide itself, if
        // it's enabled.)
        updateInCallTouchUi();

        // Update any other onscreen UI elements that depend on the dialpad.
        updateDialpadVisibility();

        // This counts as explicit "user activity".
        PhoneApp.getInstance().pokeUserActivity();

        //If on OTA Call, hide OTA Screen
        // TODO: This may not be necessary, now that the dialpad is
        // always visible in OTA mode.
        if  ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL
                || mInCallScreenMode == InCallScreenMode.OTA_ENDED)
                && otaUtils != null) {
            otaUtils.hideOtaScreen();
        }
    }

    /**
     * Called any time the DTMF dialpad is closed.
     * @see DTMFTwelveKeyDialer.onDialerClose()
     */
    /* package */ void onDialerClose() {
        if (DBG) log("onDialerClose()...");

        final PhoneApp app = PhoneApp.getInstance();

        // OTA-specific cleanup upon closing the dialpad.
        if ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL)
            || (mInCallScreenMode == InCallScreenMode.OTA_ENDED)
            || ((app.cdmaOtaScreenState != null)
                && (app.cdmaOtaScreenState.otaScreenState ==
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION))) {
            mDialer.setHandleVisible(false);
            if (otaUtils != null) {
                otaUtils.otaShowProperScreen();
            }
        }

        // Dismiss the "touch lock" overlay if it was visible.
        // (The overlay is only ever used on top of the dialpad).
        enableTouchLock(false);

        // Update the in-call touch UI (which may need to re-show itself.)
        updateInCallTouchUi();

        // Update the visibility of the dialpad itself (and any other
        // onscreen UI elements that depend on it.)
        updateDialpadVisibility();

        // This counts as explicit "user activity".
        app.getInstance().pokeUserActivity();
    }

    /**
     * Determines when we can dial DTMF tones.
     */
    private boolean okToDialDTMFTones() {
        final boolean hasRingingCall = !mRingingCall.isIdle();
        final Call.State fgCallState = mForegroundCall.getState();

      //hyojin.an 101021 if(fgCallState == Call.State.DIALING || fgCallState == Call.State.INCOMING){ //hyojin.an@lge.com
      //hyojin.an 101021 	log("okToDialDTMFTones -> Directly return, fgCallState is Dialing or Incoming");
      //hyojin.an 101021 		return false;
      //hyojin.an 101021  }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
        final Call.State bgCallState = mBackgroundCall.getState();
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]
        	
        // We're allowed to send DTMF tones when there's an ACTIVE
        // foreground call, and not when an incoming call is ringing
        // (since DTMF tones are useless in that state), or if the
        // Manage Conference UI is visible (since the tab interferes
        // with the "Back to call" button.)

        // We can also dial while in ALERTING state because there are
        // some connections that never update to an ACTIVE state (no
        // indication from the network).
        boolean canDial;
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
        if (StarConfig.OPERATOR.equals("SKT") && isDialpadSendKeyEnabled()) {
           canDial = (fgCallState == Call.State.ACTIVE || fgCallState == Call.State.ALERTING || bgCallState == Call.State.HOLDING)
               && (!hasRingingCall)
            && (mInCallScreenMode != InCallScreenMode.MANAGE_CONFERENCE);
        } else {
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

           canDial = (fgCallState == Call.State.ACTIVE || fgCallState == Call.State.ALERTING)
               && (!hasRingingCall)
               && (mInCallScreenMode != InCallScreenMode.MANAGE_CONFERENCE);
        }

        if (VDBG) log ("[okToDialDTMFTones] foreground state: " + fgCallState +
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
				", background state: " + bgCallState +
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]
                ", ringing state: " + hasRingingCall +
                ", call screen mode: " + mInCallScreenMode +
                ", result: " + canDial);

        return canDial;
    }

    /**
     * @return true if the in-call DTMF dialpad should be available to the
     *      user, given the current state of the phone and the in-call UI.
     *      (This is used to control the visibility of the dialer's
     *      onscreen handle, if applicable, and the enabledness of the "Show
     *      dialpad" onscreen button or menu item.)
     */
    /* package */ boolean okToShowDialpad() {
        // The dialpad is available only when it's OK to dial DTMF
        // tones given the current state of the current call.
        return okToDialDTMFTones();
    }
    
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
    //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
    // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-15 : Display Call End when is not showing call screen
    // Conv => Home Key => Approach Proximity  => LCD Off => remote end : don't has extra ACTION_DISPLAY_CALLEND
    private boolean okToShowCallEnd() {
    	boolean result = !phoneIsInUse()
    	&& ((mForegroundCall.getState() == Call.State.DISCONNECTED)
    	|| (mBackgroundCall.getState() == Call.State.DISCONNECTED));
    	
    	if (VDBG) log("okToShowCallEnd()..." + result);
    	
    	return result;
    }

    private boolean okToExitInCallScreen() {
    	boolean result = !phoneIsInUse()
    	&& (mForegroundCall.getState() == Call.State.IDLE) && (mBackgroundCall.getState() == Call.State.IDLE)
    	&& (mRingingCall.getState() == Call.State.IDLE);
    	
    	if (VDBG) log("okToExitInCallScreen()..." + result);

    	return result;
    }
    boolean okToDelayOfUpdateKeyguardPolicy() {
    		//         if (mIsProcessingRejectMessage != InCallRejectMessageState.REJECTMESSAGE_NONE) {
    	if (Settings.System.getInt(getContentResolver(), Settings.System.LOCK_PATTERN_ENABLED, 0) == 1) {
    		if (VDBG) log("okToDelayOfUpdateKeyguardPolicy() : case 1. return false");
    		return false;
    	} else {
    		if (VDBG) log("okToDelayOfUpdateKeyguardPolicy() : case 2. return true");
    		return true;
    	}
    }
    // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-15 : Display Call End when is not showing call screen
    //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
    /**
     * Initializes the in-call touch UI on devices that need it.
     */
    private void initInCallTouchUi() {
        if (DBG) log("initInCallTouchUi()...");
        // TODO: we currently use the InCallTouchUi widget in at least
        // some states on ALL platforms.  But if some devices ultimately
        // end up not using *any* onscreen touch UI, we should make sure
        // to not even inflate the InCallTouchUi widget on those devices.
        mInCallTouchUi = (InCallTouchUi) findViewById(R.id.inCallTouchUi);
        mInCallTouchUi.setInCallScreenInstance(this);
        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]
        if(StarConfig.COUNTRY.equals("KR"))
        	PhoneUtils.setSoundRecording(false);
        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]
    }

    /**
     * Updates the state of the in-call touch UI.
     * @param emergencyFlag2 
     */
    private void updateInCallTouchUi() {
        if (mInCallTouchUi != null) {
//20100804 jongwany.lee@lge.com attached it for CALL UI
            mInCallTouchUi.updateState(mPhone,inCallControl_GONE_Flag);
        }
    }
    
    private void updateEndCallTouchUi(boolean isVisible)
    {
    	if(mInCallTouchUi != null)
    	{
    		if(mCallCard != null)
    		{
    			mInCallTouchUi.setNameInContact(mCallCard.hasNameInContact());
    		}
    		mInCallTouchUi.UpdateEndCallControls(mPhone, isVisible);
    	}
    }

    /**
     * @return true if the onscreen touch UI is enabled (for regular
     * "ongoing call" states) on the current device.
     */
    public boolean isTouchUiEnabled() {
        return (mInCallTouchUi != null) && mInCallTouchUi.isTouchUiEnabled();
    }

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
    public boolean isDialpadSendKeyEnabled() {
        return (mInCallTouchUi != null) && mInCallTouchUi.isDialpadSendKeyEnabled();
    }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

    /**
     * @return true if the onscreen touch UI is enabled for
     * the "incoming call" state on the current device.
     */
    public boolean isIncomingCallTouchUiEnabled() {
        return (mInCallTouchUi != null) && mInCallTouchUi.isIncomingCallTouchUiEnabled();
    }

    /**
     * Posts a handler message telling the InCallScreen to update the
     * onscreen in-call touch UI.
     *
     * This is just a wrapper around updateInCallTouchUi(), for use by the
     * rest of the phone app or from a thread other than the UI thread.
     */
    /* package */ void requestUpdateTouchUi() {
        if (DBG) log("requestUpdateTouchUi()...");

        mHandler.removeMessages(REQUEST_UPDATE_TOUCH_UI);
        mHandler.sendEmptyMessage(REQUEST_UPDATE_TOUCH_UI);
    }

//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [START_LGE_LAB]
    private void updateInCallCallCardUi() {
        if (mCallCard != null) {
            if (VDBG) log("updateInCallCallCardUi()...");
            mCallCard.updateState(mPhone);
        }
    }

    void requestUpdateCallCardUi() {
        if (DBG) log("requestUpdateCallCardUi()...");

        mHandler.removeMessages(REQUEST_UPDATE_CALLCARD_UI);
        mHandler.sendEmptyMessage(REQUEST_UPDATE_CALLCARD_UI);
    }
//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [END_LGE_LAB]


    /**
     * @return true if it's OK to display the in-call touch UI, given the
     * current state of the InCallScreen.
     */
    /* package */ boolean okToShowInCallTouchUi() {
        // Note that this method is concerned only with the internal state
        // of the InCallScreen.  (The InCallTouchUi widget has separate
        // logic to make sure it's OK to display the touch UI given the
        // current telephony state, and that it's allowed on the current
        // device in the first place.)

        // The touch UI is NOT available if:
        // - we're in some InCallScreenMode other than NORMAL
        //   (like CALL_ENDED or one of the OTA modes)
        return (mInCallScreenMode == InCallScreenMode.NORMAL || mInCallScreenMode == InCallScreenMode.DIALING);
    }

    /**
     * @return true if we're in restricted / emergency dialing only mode.
     */
    public boolean isPhoneStateRestricted() {
        // TODO:  This needs to work IN TANDEM with the KeyGuardViewMediator Code.
        // Right now, it looks like the mInputRestricted flag is INTERNAL to the
        // KeyGuardViewMediator and SPECIFICALLY set to be FALSE while the emergency
        // phone call is being made, to allow for input into the InCallScreen.
        // Having the InCallScreen judge the state of the device from this flag
        // becomes meaningless since it is always false for us.  The mediator should
        // have an additional API to let this app know that it should be restricted.
        return ((mPhone.getServiceState().getState() == ServiceState.STATE_EMERGENCY_ONLY) ||
                (mPhone.getServiceState().getState() == ServiceState.STATE_OUT_OF_SERVICE) ||
                (PhoneApp.getInstance().getKeyguardManager().inKeyguardRestrictedInputMode()));
    }

    //
    // In-call menu UI
    //

    /**
     * Override onCreatePanelView(), in order to get complete control
     * over the UI that comes up when the user presses MENU.
     *
     * This callback allows me to return a totally custom View hierarchy
     * (with custom layout and custom "item" views) to be shown instead
     * of a standard android.view.Menu hierarchy.
     *
     * This gets called (with featureId == FEATURE_OPTIONS_PANEL) every
     * time we need to bring up the menu.  (And in cases where we return
     * non-null, that means that the "standard" menu callbacks
     * onCreateOptionsMenu() and onPrepareOptionsMenu() won't get called
     * at all.)
     */
    @Override
    public View onCreatePanelView(int featureId) {
        if (VDBG) log("onCreatePanelView(featureId = " + featureId + ")...");

        // We only want this special behavior for the "options panel"
        // feature (i.e. the standard menu triggered by the MENU button.)
        if (featureId != Window.FEATURE_OPTIONS_PANEL) {
            return null;
        }

        // For now, totally disable the in-call menu on devices where we
        // use onscreen touchable buttons instead.
        // TODO: even on "full touch" devices we may still ultimately need
        // a regular menu in some states.  Need UI spec.
        if (isTouchUiEnabled()) {
            return null;
        }

        // TODO: May need to revisit the wake state here if this needs to be
        // tweaked.

        // Make sure there are no pending messages to *dismiss* the menu.
        mHandler.removeMessages(DISMISS_MENU);

        if (mInCallMenu == null) {
            if (VDBG) log("onCreatePanelView: creating mInCallMenu (first time)...");
            mInCallMenu = new InCallMenu(this);
            mInCallMenu.initMenu();
        }

        boolean okToShowMenu = mInCallMenu.updateItems(mPhone);
        return okToShowMenu ? mInCallMenu.getView() : null;
    }

    /**
     * Dismisses the menu panel (see onCreatePanelView().)
     *
     * @param dismissImmediate If true, hide the panel immediately.
     *            If false, leave the menu visible onscreen for
     *            a brief interval before dismissing it (so the
     *            user can see the state change resulting from
     *            his original click.)
     */
    /* package */ void dismissMenu(boolean dismissImmediate) {
        if (VDBG) log("dismissMenu(immediate = " + dismissImmediate + ")...");

        if (dismissImmediate) {
            closeOptionsMenu();
        } else {
            mHandler.removeMessages(DISMISS_MENU);
            mHandler.sendEmptyMessageDelayed(DISMISS_MENU, MENU_DISMISS_DELAY);
            // This will result in a dismissMenu(true) call shortly.
        }
    }

    /**
     * Override onPanelClosed() to capture the panel closing event,
     * allowing us to set the poke lock correctly whenever the option
     * menu panel goes away.
     */
    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
    }

	 public void onSubPanelClosed(int featureId, Menu menu)
	 {
        if (VDBG) log("onPanelClosed(featureId = " + featureId + ")...");

        // We only want this special behavior for the "options panel"
        // feature (i.e. the standard menu triggered by the MENU button.)
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            // TODO: May need to return to the original wake state here
            // if onCreatePanelView ends up changing the wake state.
        }
    }

    //
    // Bluetooth helper methods.
    //
    // - BluetoothAdapter is the Bluetooth system service.  If
    //   getDefaultAdapter() returns null
    //   then the device is not BT capable.  Use BluetoothDevice.isEnabled()
    //   to see if BT is enabled on the device.
    //
    // - BluetoothHeadset is the API for the control connection to a
    //   Bluetooth Headset.  This lets you completely connect/disconnect a
    //   headset (which we don't do from the Phone UI!) but also lets you
    //   get the address of the currently active headset and see whether
    //   it's currently connected.
    //
    // - BluetoothHandsfree is the API to control the audio connection to
    //   a bluetooth headset. We use this API to switch the headset on and
    //   off when the user presses the "Bluetooth" button.
    //   Our BluetoothHandsfree instance (mBluetoothHandsfree) is created
    //   by the PhoneApp and will be null if the device is not BT capable.
    //

    /**
     * @return true if the Bluetooth on/off switch in the UI should be
     *         available to the user (i.e. if the device is BT-capable
     *         and a headset is connected.)
     */
    /* package */ boolean isBluetoothAvailable() {
        if (VDBG) log("isBluetoothAvailable()...");
        if (mBluetoothHandsfree == null) {
            // Device is not BT capable.
            if (VDBG) log("  ==> FALSE (not BT capable)");
            return false;
        }

        // There's no need to ask the Bluetooth system service if BT is enabled:
        //
        //    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        //    if ((adapter == null) || !adapter.isEnabled()) {
        //        if (DBG) log("  ==> FALSE (BT not enabled)");
        //        return false;
        //    }
        //    if (DBG) log("  - BT enabled!  device name " + adapter.getName()
        //                 + ", address " + adapter.getAddress());
        //
        // ...since we already have a BluetoothHeadset instance.  We can just
        // call isConnected() on that, and assume it'll be false if BT isn't
        // enabled at all.

        // Check if there's a connected headset, using the BluetoothHeadset API.
        boolean isConnected = false;
        if (mBluetoothHeadset != null) {
            if (VDBG) log("  - headset state = " + mBluetoothHeadset.getState());
            BluetoothDevice headset = mBluetoothHeadset.getCurrentHeadset();
            if (VDBG) log("  - headset address: " + headset);
            if (headset != null) {
                isConnected = mBluetoothHeadset.isConnected(headset);
                if (VDBG) log("  - isConnected: " + isConnected);
            }
        }

        if (VDBG) log("  ==> " + isConnected);
        return isConnected;
    }

    /**
     * @return true if a BT device is available, and its audio is currently connected.
     */
    /* package */ boolean isBluetoothAudioConnected() {
        if (mBluetoothHandsfree == null) {
            if (VDBG) log("isBluetoothAudioConnected: ==> FALSE (null mBluetoothHandsfree)");
            return false;
        }
        boolean isAudioOn = mBluetoothHandsfree.isAudioOn();
        if (VDBG) log("isBluetoothAudioConnected: ==> isAudioOn = " + isAudioOn);
        return isAudioOn;
    }

    /**
     * Helper method used to control the state of the green LED in the
     * "Bluetooth" menu item.
     *
     * @return true if a BT device is available and its audio is currently connected,
     *              <b>or</b> if we issued a BluetoothHandsfree.userWantsAudioOn()
     *              call within the last 5 seconds (which presumably means
     *              that the BT audio connection is currently being set
     *              up, and will be connected soon.)
     */
    /* package */ boolean isBluetoothAudioConnectedOrPending() {
        if (isBluetoothAudioConnected()) {
            if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> TRUE (really connected)");
            return true;
        }

        // If we issued a userWantsAudioOn() call "recently enough", even
        // if BT isn't actually connected yet, let's still pretend BT is
        // on.  This is how we make the green LED in the menu item turn on
        // right away.
        if (mBluetoothConnectionPending) {
            long timeSinceRequest =
                    SystemClock.elapsedRealtime() - mBluetoothConnectionRequestTime;
            if (timeSinceRequest < 5000 /* 5 seconds */) {
                if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> TRUE (requested "
                             + timeSinceRequest + " msec ago)");
                return true;
            } else {
                if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> FALSE (request too old: "
                             + timeSinceRequest + " msec ago)");
                mBluetoothConnectionPending = false;
                return false;
            }
        }

        if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> FALSE");
        return false;
    }

    /**
     * Posts a message to our handler saying to update the onscreen UI
     * based on a bluetooth headset state change.
     */
    /* package */ void requestUpdateBluetoothIndication() {
        if (VDBG) log("requestUpdateBluetoothIndication()...");
        // No need to look at the current state here; any UI elements that
        // care about the bluetooth state (i.e. the CallCard) get
        // the necessary state directly from PhoneApp.showBluetoothIndication().
        mHandler.removeMessages(REQUEST_UPDATE_BLUETOOTH_INDICATION);
        mHandler.sendEmptyMessage(REQUEST_UPDATE_BLUETOOTH_INDICATION);
    }

    private void dumpBluetoothState() {
        log("============== dumpBluetoothState() =============");
        log("= isBluetoothAvailable: " + isBluetoothAvailable());
        log("= isBluetoothAudioConnected: " + isBluetoothAudioConnected());
        log("= isBluetoothAudioConnectedOrPending: " + isBluetoothAudioConnectedOrPending());
        log("= PhoneApp.showBluetoothIndication: "
            + PhoneApp.getInstance().showBluetoothIndication());
        log("=");
        if (mBluetoothHandsfree != null) {
            log("= BluetoothHandsfree.isAudioOn: " + mBluetoothHandsfree.isAudioOn());
            if (mBluetoothHeadset != null) {
                BluetoothDevice headset = mBluetoothHeadset.getCurrentHeadset();
                log("= BluetoothHeadset.getCurrentHeadset: " + headset);
                if (headset != null) {
                    log("= BluetoothHeadset.isConnected: "
                        + mBluetoothHeadset.isConnected(headset));
                }
            } else {
                log("= mBluetoothHeadset is null");
            }
        } else {
            log("= mBluetoothHandsfree is null; device is not BT capable");
        }
    }

    /* package */ void connectBluetoothAudio() {
        if (VDBG) log("connectBluetoothAudio()...");
        if (mBluetoothHandsfree != null) {
            mBluetoothHandsfree.userWantsAudioOn();
        }

        // Watch out: The bluetooth connection doesn't happen instantly;
        // the userWantsAudioOn() call returns instantly but does its real
        // work in another thread.  Also, in practice the BT connection
        // takes longer than MENU_DISMISS_DELAY to complete(!) so we need
        // a little trickery here to make the menu item's green LED update
        // instantly.
        // (See isBluetoothAudioConnectedOrPending() above.)
        mBluetoothConnectionPending = true;
        mBluetoothConnectionRequestTime = SystemClock.elapsedRealtime();
    }

    /* package */ void disconnectBluetoothAudio() {
        if (VDBG) log("disconnectBluetoothAudio()...");
        if (mBluetoothHandsfree != null) {
            mBluetoothHandsfree.userWantsAudioOff();
        }
        mBluetoothConnectionPending = false;
    }

    //
    // "Touch lock" UI.
    //
    // When the DTMF dialpad is up, after a certain amount of idle time we
    // display an overlay graphic on top of the dialpad and "lock" the
    // touch UI.  (UI Rationale: We need *some* sort of screen lock, with
    // a fairly short timeout, to avoid false touches from the user's face
    // while in-call.  But we *don't* want to do this by turning off the
    // screen completely, since that's confusing (the user can't tell
    // what's going on) *and* it's fairly cumbersome to have to hit MENU
    // to bring the screen back, rather than using some gesture on the
    // touch screen.)
    //
    // The user can dismiss the touch lock overlay by double-tapping on
    // the central "lock" icon.  Also, the touch lock overlay will go away
    // by itself if the DTMF dialpad is dismissed for any reason, such as
    // the current call getting disconnected (see onDialerClose()).
    //
    // This entire feature is disabled on devices which use a proximity
    // sensor to turn the screen off while in-call.
    //

    /**
     * Initializes the "touch lock" UI widgets.  We do this lazily
     * to avoid slowing down the initial launch of the InCallScreen.
     */
    private void initTouchLock() {
        if (VDBG) log("initTouchLock()...");
        if (mTouchLockOverlay != null) {
            Log.w(LOG_TAG, "initTouchLock: already initialized!");
            return;
        }

        if (!mUseTouchLockOverlay) {
            Log.w(LOG_TAG, "initTouchLock: touch lock isn't used on this device!");
            return;
        }

        mTouchLockOverlay = (View) findViewById(R.id.touchLockOverlay);
        // Note mTouchLockOverlay's visibility is initially GONE.
        mTouchLockIcon = (View) findViewById(R.id.touchLockIcon);

        // Handle touch events.  (Basically mTouchLockOverlay consumes and
        // discards any touch events it sees, and mTouchLockIcon listens
        // for the "double-tap to unlock" gesture.)
        mTouchLockOverlay.setOnTouchListener(this);
        mTouchLockIcon.setOnTouchListener(this);

        mTouchLockFadeIn = AnimationUtils.loadAnimation(this, R.anim.touch_lock_fade_in);
    }

    private boolean isTouchLocked() {
        return mUseTouchLockOverlay
                && (mTouchLockOverlay != null)
                && (mTouchLockOverlay.getVisibility() == View.VISIBLE);
    }

    /**
     * Enables or disables the "touch lock" overlay on top of the DTMF dialpad.
     *
     * If enable=true, bring up the overlay immediately using an animated
     * fade-in effect.  (Or do nothing if the overlay isn't appropriate
     * right now, like if the dialpad isn't up, or the speaker is on.)
     *
     * If enable=false, immediately take down the overlay.  (Or do nothing
     * if the overlay isn't actually up right now.)
     *
     * Note that with enable=false this method will *not* automatically
     * start the touch lock timer.  (So when taking down the overlay while
     * the dialer is still up, the caller is also responsible for calling
     * resetTouchLockTimer(), to make sure the overlay will get
     * (re-)enabled later.)
     *
     */
    private void enableTouchLock(boolean enable) {
        if (VDBG) log("enableTouchLock(" + enable + ")...");
        if (enable) {
            // We shouldn't have even gotten here if we don't use the
            // touch lock overlay feature at all on this device.
            if (!mUseTouchLockOverlay) {
                Log.w(LOG_TAG, "enableTouchLock: touch lock isn't used on this device!");
                return;
            }

            // The "touch lock" overlay is only ever used on top of the
            // DTMF dialpad.
            if (!mDialer.isOpened()) {
                if (VDBG) log("enableTouchLock: dialpad isn't up, no need to lock screen.");
                return;
            }

            // Also, the "touch lock" overlay NEVER appears if the speaker is in use.
            if (PhoneUtils.isSpeakerOn(this)) {
                if (VDBG) log("enableTouchLock: speaker is on, no need to lock screen.");
                return;
            }

            // Initialize the UI elements if necessary.
            if (mTouchLockOverlay == null) {
                initTouchLock();
            }

            // First take down the menu if it's up (since it's confusing
            // to see a touchable menu *above* the touch lock overlay.)
            // Note dismissMenu() has no effect if the menu is already closed.
            dismissMenu(true);  // dismissImmediate = true

            // Bring up the touch lock overlay (with an animated fade)
            mTouchLockOverlay.setVisibility(View.VISIBLE);
            mTouchLockOverlay.startAnimation(mTouchLockFadeIn);
        } else {
            // TODO: it might be nice to immediately kill the animation if
            // we're in the middle of fading-in:
            //   if (mTouchLockFadeIn.hasStarted() && !mTouchLockFadeIn.hasEnded()) {
            //      mTouchLockOverlay.clearAnimation();
            //   }
            // but the fade-in is so quick that this probably isn't necessary.

            // Take down the touch lock overlay (immediately)
            if (mTouchLockOverlay != null) mTouchLockOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Schedule the "touch lock" overlay to begin fading in after a short
     * delay, but only if the DTMF dialpad is currently visible.
     *
     * (This is designed to be triggered on any user activity
     * while the dialpad is up but not locked, and also
     * whenever the user "unlocks" the touch lock overlay.)
     *
     * Calling this method supersedes any previous resetTouchLockTimer()
     * calls (i.e. we first clear any pending TOUCH_LOCK_TIMER messages.)
     */
    private void resetTouchLockTimer() {
        if (VDBG) log("resetTouchLockTimer()...");

        // This is a no-op if this device doesn't use the touch lock
        // overlay feature at all.
        if (!mUseTouchLockOverlay) return;

        mHandler.removeMessages(TOUCH_LOCK_TIMER);
        if (mDialer.isOpened() && !isTouchLocked()) {
            // The touch lock delay value comes from Gservices; we use
            // the same value that's used for the PowerManager's
            // POKE_LOCK_SHORT_TIMEOUT flag (i.e. the fastest possible
            // screen timeout behavior.)

            // Do a fresh lookup each time, since settings values can
            // change on the fly.  (The Settings.Secure helper class
            // caches these values so this call is usually cheap.)
            int touchLockDelay = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.SHORT_KEYLIGHT_DELAY_MS,
                    TOUCH_LOCK_DELAY_DEFAULT);
            mHandler.sendEmptyMessageDelayed(TOUCH_LOCK_TIMER, touchLockDelay);
        }
    }

    /**
     * Handles the TOUCH_LOCK_TIMER event.
     * @see resetTouchLockTimer
     */
    private void touchLockTimerExpired() {
        // Ok, it's been long enough since we had any user activity with
        // the DTMF dialpad up.  If the dialpad is still up, start fading
        // in the "touch lock" overlay.
        enableTouchLock(true);
    }

    // View.OnTouchListener implementation
    public boolean onTouch(View v, MotionEvent event) {
        if (VDBG) log ("onTouch(View " + v + ")...");

        // Handle touch events on the "touch lock" overlay.
        if ((v == mTouchLockIcon) || (v == mTouchLockOverlay)) {

            // TODO: move this big hunk of code to a helper function, or
            // even better out to a separate helper class containing all
            // the touch lock overlay code.

            // We only care about these touches while the touch lock UI is
            // visible (including the time during the fade-in animation.)
            if (!isTouchLocked()) {
                // Got an event from the touch lock UI, but we're not locked!
                // (This was probably a touch-UP right after we unlocked.
                // Ignore it.)
                return false;
            }

            // (v == mTouchLockIcon) means the user hit the lock icon in the
            // middle of the screen, and (v == mTouchLockOverlay) is a touch
            // anywhere else on the overlay.

            if (v == mTouchLockIcon) {
                // Direct hit on the "lock" icon.  Handle the double-tap gesture.
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    long now = SystemClock.uptimeMillis();
                    if (VDBG) log("- touch lock icon: handling a DOWN event, t = " + now);

                    // Look for the double-tap gesture:
                    if (now < mTouchLockLastTouchTime + ViewConfiguration.getDoubleTapTimeout()) {
                        if (VDBG) log("==> touch lock icon: DOUBLE-TAP!");
                        // This was the 2nd tap of a double-tap gesture.
                        // Take down the touch lock overlay, but post a
                        // message in the future to bring it back later.
                        enableTouchLock(false);
                        resetTouchLockTimer();
                        // This counts as explicit "user activity".
                        PhoneApp.getInstance().pokeUserActivity();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Stash away the current time in case this is the first
                    // tap of a double-tap gesture.  (We measure the time from
                    // the first tap's UP to the second tap's DOWN.)
                    mTouchLockLastTouchTime = SystemClock.uptimeMillis();
                }

                // And regardless of what just happened, we *always* consume
                // touch events while the touch lock UI is (or was) visible.
                return true;

            } else {  // (v == mTouchLockOverlay)
                // User touched the "background" area of the touch lock overlay.

                // TODO: If we're in the middle of the fade-in animation,
                // consider making a touch *anywhere* immediately unlock the
                // UI.  This could be risky, though, if the user tries to
                // *double-tap* during the fade-in (in which case the 2nd tap
                // might 't become a false touch on the dialpad!)
                //
                //if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //    if (DBG) log("- touch lock overlay background: handling a DOWN event.");
                //
                //    if (mTouchLockFadeIn.hasStarted() && !mTouchLockFadeIn.hasEnded()) {
                //        // If we're still fading-in, a touch *anywhere* onscreen
                //        // immediately unlocks.
                //        if (DBG) log("==> touch lock: tap during fade-in!");
                //
                //        mTouchLockOverlay.clearAnimation();
                //        enableTouchLock(false);
                //        // ...but post a message in the future to bring it
                //        // back later.
                //        resetTouchLockTimer();
                //    }
                //}

                // And regardless of what just happened, we *always* consume
                // touch events while the touch lock UI is (or was) visible.
                return true;
            }
        } else {
            Log.w(LOG_TAG, "onTouch: event from unexpected View: " + v);
            return false;
        }
    }

    // Any user activity while the dialpad is up, but not locked, should
    // reset the touch lock timer back to the full delay amount.
    @Override
    public void onUserInteraction() {
        if (mDialer.isOpened() && !isTouchLocked()) {
            resetTouchLockTimer();
        }
    }

    /**
     * Posts a handler message telling the InCallScreen to close
     * the OTA failure notice after the specified delay.
     * @see OtaUtils.otaShowProgramFailureNotice
     */
    /* package */ void requestCloseOtaFailureNotice(long timeout) {
        if (DBG) log("requestCloseOtaFailureNotice() with timeout: " + timeout);
        mHandler.sendEmptyMessageDelayed(REQUEST_CLOSE_OTA_FAILURE_NOTICE, timeout);

        // TODO: we probably ought to call removeMessages() for this
        // message code in either onPause or onResume, just to be 100%
        // sure that the message we just posted has no way to affect a
        // *different* call if the user quickly backs out and restarts.
        // (This is also true for requestCloseSpcErrorNotice() below, and
        // probably anywhere else we use mHandler.sendEmptyMessageDelayed().)
    }

    /**
     * Posts a handler message telling the InCallScreen to close
     * the SPC error notice after the specified delay.
     * @see OtaUtils.otaShowSpcErrorNotice
     */
    /* package */ void requestCloseSpcErrorNotice(long timeout) {
        if (DBG) log("requestCloseSpcErrorNotice() with timeout: " + timeout);
        mHandler.sendEmptyMessageDelayed(REQUEST_CLOSE_SPC_ERROR_NOTICE, timeout);
    }

    public boolean isOtaCallInActiveState() {
        final PhoneApp app = PhoneApp.getInstance();
        if ((mInCallScreenMode == InCallScreenMode.OTA_NORMAL)
                || ((app.cdmaOtaScreenState != null)
                    && (app.cdmaOtaScreenState.otaScreenState ==
                        CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Handle OTA Call End scenario when display becomes dark during OTA Call
     * and InCallScreen is in pause mode.  CallNotifier will listen for call
     * end indication and call this api to handle OTA Call end scenario
     */
    public void handleOtaCallEnd() {
        final PhoneApp app = PhoneApp.getInstance();

        if (DBG) log("handleOtaCallEnd entering");
        if (((mInCallScreenMode == InCallScreenMode.OTA_NORMAL)
                || ((app.cdmaOtaScreenState != null)
                && (app.cdmaOtaScreenState.otaScreenState !=
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED)))
                && ((app.cdmaOtaProvisionData != null)
                && (!app.cdmaOtaProvisionData.inOtaSpcState))) {
            if (DBG) log("handleOtaCallEnd - Set OTA Call End stater");
            setInCallScreenMode(InCallScreenMode.OTA_ENDED);
            updateScreen();
        }
    }

    public boolean isOtaCallInEndState() {
        return (mInCallScreenMode == InCallScreenMode.OTA_ENDED);
    }

   /**
    * Checks to see if the current call is a CDMA OTA Call, based on the
    * action of the specified intent and OTA Screen state information.
    *
    * The OTA call is a CDMA-specific concept, so this method will
    * always return false on a GSM phone.
    */
    private boolean checkIsOtaCall(Intent intent) {
        if (VDBG) log("checkIsOtaCall...");

        if (intent == null || intent.getAction() == null) {
            return false;
        }

        if (mPhone.getPhoneType() != Phone.PHONE_TYPE_CDMA) {
            return false;
        }

        final PhoneApp app = PhoneApp.getInstance();

        if ((app.cdmaOtaScreenState == null)
                || (app.cdmaOtaProvisionData == null)) {
            if (DBG) log("checkIsOtaCall: OtaUtils.CdmaOtaScreenState not initialized");
            return false;
        }

        String action = intent.getAction();
        boolean isOtaCall = false;
        if (action.equals(ACTION_SHOW_ACTIVATION)) {
            if (DBG) log("checkIsOtaCall action = ACTION_SHOW_ACTIVATION");
            if (!app.cdmaOtaProvisionData.isOtaCallIntentProcessed) {
                if (DBG) log("checkIsOtaCall: ACTION_SHOW_ACTIVATION is not handled before");
                app.cdmaOtaProvisionData.isOtaCallIntentProcessed = true;
                app.cdmaOtaScreenState.otaScreenState =
                        CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION;
            }
            isOtaCall = true;
        } else if (action.equals(Intent.ACTION_CALL)
                || action.equals(Intent.ACTION_CALL_EMERGENCY)) {
            String number;
            try {
                number = getInitialNumber(intent);
            } catch (PhoneUtils.VoiceMailNumberMissingException ex) {
                if (DBG) log("Error retrieving number using the api getInitialNumber()");
                return false;
            }
            if (mPhone.isOtaSpNumber(number)) {
                if (DBG) log("checkIsOtaCall action ACTION_CALL, it is valid OTA number");
                isOtaCall = true;
            }
        } else if (action.equals(intent.ACTION_MAIN)) {
            if (DBG) log("checkIsOtaCall action ACTION_MAIN");
            boolean isRingingCall = !mRingingCall.isIdle();
            if (isRingingCall) {
                if (DBG) log("checkIsOtaCall isRingingCall: " + isRingingCall);
                return false;
            } else if ((app.cdmaOtaInCallScreenUiState.state
                            == CdmaOtaInCallScreenUiState.State.NORMAL)
                    || (app.cdmaOtaInCallScreenUiState.state
                            == CdmaOtaInCallScreenUiState.State.ENDED)) {
                if (DBG) log("checkIsOtaCall action ACTION_MAIN, OTA call already in progress");
                isOtaCall = true;
            } else {
                if (app.cdmaOtaScreenState.otaScreenState !=
                        CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED) {
                    if (DBG) log("checkIsOtaCall action ACTION_MAIN, "
                                 + "OTA call in progress with UNDEFINED");
                    isOtaCall = true;
                }
            }
        }

        if (DBG) log("checkIsOtaCall: isOtaCall =" + isOtaCall);
        if (isOtaCall && (otaUtils == null)) {
            if (DBG) log("checkIsOtaCall: creating OtaUtils...");
            otaUtils = new OtaUtils(getApplicationContext(),
                                    this, mInCallPanel, mCallCard, mDialer);
        }
        return isOtaCall;
    }

    /**
     * Initialize the OTA State and UI.
     *
     * On Resume, this function is called to check if current call is
     * OTA Call and if it is OTA Call, create OtaUtil object and set
     * InCallScreenMode to OTA Call mode (OTA_NORMAL or OTA_ENDED).
     * As part of initialization, OTA Call Card is inflated.
     * OtaUtil object provides utility apis that InCallScreen calls for OTA Call UI
     * rendering, handling of touck/key events on OTA Screens and handling of
     * Framework events that result in OTA State change
     *
     * @return: true if we are in an OtaCall
     */
    private boolean initOtaState() {
        boolean inOtaCall = false;

        if (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
            final PhoneApp app = PhoneApp.getInstance();

            if ((app.cdmaOtaScreenState == null) || (app.cdmaOtaProvisionData == null)) {
                if (DBG) log("initOtaState func - All CdmaOTA utility classes not initialized");
                return false;
            }

            inOtaCall = checkIsOtaCall(getIntent());
            if (inOtaCall) {
                OtaUtils.CdmaOtaInCallScreenUiState.State cdmaOtaInCallScreenState =
                        otaUtils.getCdmaOtaInCallScreenUiState();
                if (cdmaOtaInCallScreenState == OtaUtils.CdmaOtaInCallScreenUiState.State.NORMAL) {
                    if (DBG) log("initOtaState - in OTA Normal mode");
                    setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
                } else if (cdmaOtaInCallScreenState ==
                                OtaUtils.CdmaOtaInCallScreenUiState.State.ENDED) {
                    if (DBG) log("initOtaState - in OTA END mode");
                    setInCallScreenMode(InCallScreenMode.OTA_ENDED);
                } else if (app.cdmaOtaScreenState.otaScreenState ==
                                CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG) {
                    if (DBG) log("initOtaState - set OTA END Mode");
                    setInCallScreenMode(InCallScreenMode.OTA_ENDED);
                } else {
                    if (DBG) log("initOtaState - Set OTA NORMAL Mode");
                    setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
                }
            } else {
                if (otaUtils != null) {
                    otaUtils.cleanOtaScreen(false);
                }
            }
        }
        return inOtaCall;
    }

    public void updateMenuItems() {
        if (mInCallMenu != null) {
            boolean okToShowMenu =  mInCallMenu.updateItems(PhoneApp.getInstance().phone);
            if (!okToShowMenu) {
                dismissMenu(true);
            }
        }
    }

    /**
     * Updates and returns the InCallControlState instance.
     */
    public InCallControlState getUpdatedInCallControlState() {
        mInCallControlState.update();
        return mInCallControlState;
    }

    /**
     * Updates the background of the InCallScreen to indicate the state of
     * the current call(s).
     */
    private void updateInCallBackground() {
        final boolean hasRingingCall = !mRingingCall.isIdle();
        final boolean hasActiveCall = !mForegroundCall.isIdle();
        final boolean hasHoldingCall = !mPhone.getBackgroundCall().isIdle();
        final PhoneApp app = PhoneApp.getInstance();
        final boolean bluetoothActive = app.showBluetoothIndication();

        int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;

        // Possible states of the background are:
        // - bg_in_call_gradient_bluetooth.9.png     // blue
        // - bg_in_call_gradient_connected.9.png     // green
        // - bg_in_call_gradient_ended.9.png         // red
        // - bg_in_call_gradient_on_hold.9.png       // orange
        // - bg_in_call_gradient_unidentified.9.png  // gray

        if (hasRingingCall) {
            // There's an INCOMING (or WAITING) call.
            if (bluetoothActive) {
                backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
            } else {
                backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
            }
        } else if (hasHoldingCall && !hasActiveCall) {
            // No foreground call, but there is a call on hold.
            backgroundResId = R.drawable.bg_in_call_gradient_on_hold;
        } else {
            // In all cases other than "ringing" and "on hold", the state
            // of the foreground call determines the background.
            final Call.State fgState = mForegroundCall.getState();
            switch (fgState) {
                case ACTIVE:
                case DISCONNECTING:  // Call will disconnect soon, but keep showing
                                     // the normal "connected" background for now.
                    if (bluetoothActive) {
                        backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
                    } else {
                        backgroundResId = R.drawable.bg_in_call_gradient_connected;
                    }
                    break;

                case DISCONNECTED:
                    backgroundResId = R.drawable.bg_in_call_gradient_ended;
                    break;

                case DIALING:
                case ALERTING:
                    if (bluetoothActive) {
                        backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
                    } else {
                        backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
                    }
                    break;

                default:
                    // Foreground call is (presumably) IDLE.
                    // We're not usually here at all in this state, but
                    // this *does* happen in some unusual cases (like
                    // while displaying an MMI result).
                    // Use the most generic background.
                    backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
                    break;
            }
        }
        mMainFrame.setBackgroundResource(backgroundResId);
        	if(mForegroundCall.getState() == Call.State.DISCONNECTED)
        	{
        		updateEndCallTouchUi(true);
        	}
    }

    public void resetInCallScreenMode() {
        if (DBG) log("resetInCallScreenMode - InCallScreenMode set to UNDEFINED");
        setInCallScreenMode(InCallScreenMode.UNDEFINED);
    }

    /**
     * Clear all the fields related to the provider support.
     */
    private void clearProvider() {
        mProviderOverlayVisible = false;
        mProviderLabel = null;
        mProviderIcon = null;
        mProviderGatewayUri = null;
        mProviderAddress = null;
    }

    /**
     * Updates the onscreen hint displayed while the user is dragging one
     * of the handles of the RotarySelector widget used for incoming
     * calls.
     *
     * @param hintTextResId resource ID of the hint text to display,
     *        or 0 if no hint should be visible.
     * @param hintColorResId resource ID for the color of the hint text
     */
    /* package */ void updateSlidingTabHint(int hintTextResId, int hintColorResId) {
        if (VDBG) log("updateRotarySelectorHint(" + hintTextResId + ")...");
        if (mCallCard != null) {
            mCallCard.setRotarySelectorHint(hintTextResId, hintColorResId);
            mCallCard.updateState(mPhone);
            // TODO: if hintTextResId == 0, consider NOT clearing the onscreen
            // hint right away, but instead post a delayed handler message to
            // keep it onscreen for an extra second or two.  (This might make
            // the hint more helpful if the user quickly taps one of the
            // handles without dragging at all...)
            // (Or, maybe this should happen completely within the RotarySelector
            // widget, since the widget itself probably wants to keep the colored
            // arrow visible for some extra time also...)
        }
    }

// LGE_SS_NOTIFY START

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return super.dispatchPopulateAccessibilityEvent(event);
    }

   public void dispatchSubPopulateAccessibilityEvent(AccessibilityEvent event)
  {
        mCallCard.dispatchPopulateAccessibilityEvent(event);
   }
   

      public String getSuppServNotify() {
    	int res_item;
    	String result;
    	int cug_index;
    	if (ss_notify != null){
    	   res_item = R.string.card_title_mo_0 + ss_notify.getResource();
    	   cug_index = ss_notify.getIndex();
    	   if (cug_index != SuppServiceNotification.INVALID_CUG)
              result = cug_index + " " + getString(res_item);
    	   else
              result = getString(res_item);
    	   return result;
    	}
    	else
    	   return null;
    }
// LGE_SS_NOTIFY END
// LGE_AUTO_REDIAL START
    public boolean getRedialStatus() {
    	if(repeat != 1 ){
        return true;
    }
    	else
    	   return false;
    }

    public boolean disableLog() {
    	return disable_log;
    }
// LGE_AUTO_REDIAL END
    
    // LGE_MERGE_S
    //LGE_S kim.jungtae 20100421
    void gotoExcuseMessages(){
    	//goto ExcuseMessagesList
    	String rName ="";
    	String rNumber ="";
    	rNumber	=	mRingingCall.getEarliestConnection().getAddress();

	    	//Log.d(LOG_TAG, "InCallScreen.java:rName:"+rName);
	    	Log.d(LOG_TAG, "InCallScreen.java:rNumber:"+rNumber);

	    	Intent intentSms = new Intent(this, InCallMessagesList.class);
	    	intentSms.putExtra("rName", rName);
	    	intentSms.putExtra("rNumber", rNumber);
			startActivity(intentSms);
    }
    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
//20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
//LGE_E kim.jungtae 20100421
// LGE_MERGE_E
    String getUserNumber()
    {
    	if (mRingingCall == null 
    			|| mRingingCall.getEarliestConnection() == null 
    			|| mRingingCall.getEarliestConnection().getAddress() == null 
    			|| mRingingCall.getEarliestConnection().getAddress().trim().equals("") 
    			|| mRingingCall.getEarliestConnection().getAddress().trim().length() == 0
    		)
    		return null;
    	else
    		return mRingingCall.getEarliestConnection().getAddress();
    }
    // LGE_MERGE_E
    
    
  //20100507 yongwoon.choi@lge.com	send SMS in Silent incoming [START_LGE]
	private static final int SUBACTIVITY_EXCUSE_MESSAGE = 1;
	private static boolean DontKeyguardLock = false;

	private void internalSendSMS () {
		CallerInfo info = PhoneUtils.getCallerInfo(this, mPhone.getRingingCall().getEarliestConnection());
		//Intent intent = new Intent("com.lge.execusemsg.action.GET_EXCUSE_MSG");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		Log.d(LOG_TAG, "internalSendSMS info.phoneNumber : "+info.phoneNumber);
		if (info.phoneNumber != null) {
			DontKeyguardLock = true;
			mInCallTouchUi.handleSilentCall();
			intent.setClassName(this, ExcuseMessages.class.getName());
			intent.putExtra("excusemsg.phoneNumber", info.phoneNumber);
			intent.putExtra("excusemsg.fromInCall", true);
			startActivityForResult(intent, SUBACTIVITY_EXCUSE_MESSAGE);
		}
		else
		{
			Toast toast = Toast.makeText(this, R.string.private_num, Toast.LENGTH_SHORT);
	        toast.show();		
		}
	}

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case SUBACTIVITY_EXCUSE_MESSAGE:
				if (resultCode == RESULT_OK) {
					internalHangupRingingCall();
				}
				break;
		}
    }	
//20100507 yongwoon.choi@lge.com	send SMS in Silent incoming [END_LGE]
	public boolean isEndCallState()
	{
		return (mInCallScreenMode == InCallScreenMode.CALL_ENDED);
	}
//<!--[sumi920.kim@lge.com] 2010.09.06	LAB1_CallUI ==> InCall Option Menu( contact /memo/sms )  [START] --> 

    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		if ( setOptionMenu(menu))
			return super.onPrepareOptionsMenu(menu);
		else 
			return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		if(checkCanMakeOption())
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.incallmenu, menu);
			mIsOptionCreated = true; 
			setOptionMenu(menu);				
		}
  
		return true;
	}

	private boolean checkCanMakeOption() {
		// TODO Auto-generated method stub
		final Call.State fgState = mForegroundCall.getState();
		Log.d(LOG_TAG, "checkCanMakeOption mPhone.getState() : " +  mPhone.getState() );
		Log.d(LOG_TAG, "fgState : " + fgState );
		if((mPhone.getState() != Phone.State.RINGING)&& 
			(!mForegroundCall.isIdle() || !mBackgroundCall.isIdle()))
		{
			
			if((fgState != Call.State.DIALING)&& (fgState != Call.State.ALERTING))
			{
				return true;
			}
		}
		return false;
	}
	private static final int MENU_ADD_CALL		= Menu.FIRST;
	private static final int MENU_MERGE_CALL	= Menu.FIRST + 1;
	private static final int MENU_GOTO_CONTACT	= Menu.FIRST + 2;
	private static final int MENU_GOTO_MEMO		= Menu.FIRST + 3;
	private static final int MENU_GOTO_SMS		= Menu.FIRST + 4;
	private static final int MENU_BLUETOOTH_ON	= Menu.FIRST + 5;
	private static final int MENU_BLUETOOTH_OFF	= Menu.FIRST + 6;
	private static final int MENU_EXPLICIT_CALL_TRANSFER = Menu.FIRST + 7;
	private static final int MENUITEM_ALL_CALLS = Menu.FIRST + 8;
	private static final int MENU_CALL_TRANSFER = Menu.FIRST + 9;

	private boolean setOptionMenu(Menu menu) {
		final boolean hasActiveCall 	= !mForegroundCall.isIdle();
		final boolean hasHoldingCall 	= !mBackgroundCall.isIdle();
	   
		// TODO Auto-generated method stub
		if(checkCanMakeOption())
		{	
			menu.clear();
			
//			if(StarConfig.OPERATOR.equals("SKT"))
//			{
				InCallControlState inCallControlState = getUpdatedInCallControlState();
				
				if( hasActiveCall && hasHoldingCall ) //3way call
				{
					if(StarConfig.OPERATOR.equals("SKT"))
					{
						menu.add(0, MENU_GOTO_CONTACT, 	0,R.string.incallmenu_Contact)
						.setIcon(R.drawable.ic_menu_contact);
					}
					menu.add(0, MENU_GOTO_MEMO, 	0, R.string.incallmenu_memo)
					.setIcon(R.drawable.ic_menu_memo);
					menu.add(0, MENU_GOTO_SMS, 		0, R.string.incallmenu_Sms)
					.setIcon(R.drawable.ic_menu_send_message);
				}
				else // 1call
				{
					
					menu.add(0, MENU_ADD_CALL, 		0, R.string.onscreenAddCallText)	
					.setIcon(R.drawable.ic_menu_add_call);	
					menu.add(0, MENU_GOTO_CONTACT, 	0, R.string.incallmenu_Contact)
					.setIcon(R.drawable.ic_menu_contact);	
					menu.add(0, MENU_GOTO_MEMO, 	0, R.string.incallmenu_memo)
					.setIcon(R.drawable.ic_menu_memo);	
					menu.add(0, MENU_GOTO_SMS, 		0, R.string.incallmenu_Sms)
					.setIcon(R.drawable.ic_menu_send_message);	
				}
				
				if(StarConfig.OPERATOR.equals("SKT"))
				{
					if(inCallControlState.bluetoothEnabled && !PhoneUtils.isSoundRecording())
					{
						if(inCallControlState.bluetoothIndicatorOn)
						{
							menu.add(0, MENU_BLUETOOTH_OFF, 0, R.string.incallmenu_uncheck_bluetooth)
							.setIcon(R.drawable.ic_menu_bluetooth_off);			
						}
						else
						{
							menu.add(0, MENU_BLUETOOTH_ON,	0, R.string.incallmenu_check_bluetooth)
							.setIcon(R.drawable.ic_menu_bluetooth_on);	
						}
					}
					
					if( hasActiveCall && hasHoldingCall ) //3way call
					{
						menu.add(0, MENU_EXPLICIT_CALL_TRANSFER, 0, R.string.menu_transferCall)
						.setIcon(R.drawable.ic_menu_call_forward);
					}
				}
//			}
//			else
//			{
//				menu.add(0, MENU_GOTO_CONTACT, 	0, R.string.incallmenu_Contact)
//				.setIcon(R.drawable.ic_menu_contact);					
//				menu.add(0, MENU_GOTO_MEMO, 	0, R.string.incallmenu_memo)
//				.setIcon(R.drawable.ic_menu_memo);
//				menu.add(0, MENU_GOTO_SMS, 		0, R.string.incallmenu_Sms)
//				.setIcon(R.drawable.ic_menu_send_message);
//				menu.add(0, MENU_CALL_TRANSFER, 0, R.string.call_transfer_title)
//				.setIcon(R.drawable.ic_menu_call_forward);
//			}
			return true;
		}
		return false;
	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		Log.d(LOG_TAG, "onOptionsItemSelected  : " + item.getItemId());
		// TODO Auto-generated method stub		
        switch (item.getItemId()) {              
	        //case R.id.add_call_id :
        	case MENU_ADD_CALL:
				  PhoneUtils.startNewCall(mPhone);  // Fires off an ACTION_DIAL intent
				 break;
			//case R.id.merge_call_id :
        	case MENU_MERGE_CALL :
				  PhoneUtils.mergeCalls(mPhone);
				break;
			//case R.id.goto_contact_id :
        	case MENU_GOTO_CONTACT :
				Intent i = new Intent(Intent.ACTION_MAIN);
				i.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.DialtactsContactsEntryActivity"));			
				startActivity(i);
				
				break;
			
			//case R.id.goto_sms_id :
        	case MENU_GOTO_SMS :

				Intent intent = new Intent(Intent.ACTION_MAIN);
		     	//<!--[sumi920.kim@lge.com] 2010.10.01	LAB1_CallUI --> Message Intent [START] -->  
				if(StarConfig.OPERATOR.equals("SKT"))
					intent.setComponent(new ComponentName("com.btb.ums", "com.btb.ums.lg.ui.messageList.MessageBoxActivity"));
				else
				//<!--[sumi920.kim@lge.com] 2010.10.01	LAB1_CallUI --> Message Intent [END] -->  				
					intent.setComponent(new ComponentName("com.android.mms", "com.android.mms.ui.ConversationList"));
				
				startActivity(intent);
				
				break;
			//case R.id.goto_memo_id :
        	case MENU_GOTO_MEMO :
				Intent memoIntent = new Intent();   
				memoIntent.setComponent(new ComponentName("com.lge.memo", "com.lge.memo.AddMemo"));
				startActivity(memoIntent);

				break;
				
//			case R.id.enable_bluetooth_id :
//			case R.id.disable_bluetooth_id :
        	case MENU_BLUETOOTH_ON :
        	case MENU_BLUETOOTH_OFF :
				onBluetoothClick();	
				if(item.getItemId() == MENU_BLUETOOTH_ON)
					mInCallTouchUi.disableSpeakerphoneButton();
				else
					mInCallTouchUi.enableSpeakerphoneButton();
				break;
// LGE_CR846_Transfer button is absent START
        	case MENU_CALL_TRANSFER:
        	case MENU_EXPLICIT_CALL_TRANSFER :
        		try {
        			List<Connection> fgdConnections = mForegroundCall.getConnections();
        			List<Connection> bgdConnections = mBackgroundCall.getConnections();
        			int fgdConnectionsSize = fgdConnections.size();
        			int bgdConnectionsSize = bgdConnections.size();
        			if ((fgdConnections != null) && (bgdConnections != null)) {
        				if ((fgdConnectionsSize == 1) && (bgdConnectionsSize == 1)) {
        					if (mPhone.canTransfer()) {
        						mPhone.explicitCallTransfer(false);
        					}
        				}
        				else if (((fgdConnectionsSize == 0) && (bgdConnectionsSize == 1))
        						|| ((fgdConnectionsSize == 1) && (bgdConnectionsSize == 0))) {
        					if (mInCallTouchUi != null && mInCallControlState != null) {
        						if (mInCallControlState.canColdTransfer(mPhone)) {
        							final String callTransferCode = "CALL_TRANSFER";
        							Intent intentTransfer = new Intent();
        							intentTransfer.putExtra(callTransferCode, 2);
        							intentTransfer.setClass(this, SimpleCallDialog.class);
        							startActivity(intentTransfer);
        						}
        					}
        				}
        			}
        		}		
        		catch(CallStateException exc) {
        			if (VDBG) log("onClick: menuTransfer...CallStateException");
        			Toast.makeText(InCallScreen.this,R.string.incall_error_supp_service_transfer,Toast.LENGTH_LONG).show();
        			break;
        		}
                break;
// LGE_CR846_Transfer button is absent END
        }
				
		return super.onOptionsItemSelected(item);
	}
//<!--[sumi920.kim@lge.com] 2010.09.06	LAB1_CallUI ==> InCall Option Menu( contact /memo/sms )  [END] --> 
    
//<!--//20100929 sumi920.kim@lge.com VideoCall Origination In CallEnd View [START_LGE_LAB1]	 -->
	  private InCallInitStatus placeVideoCall(Intent intent) {
	        if (VDBG) log("placeCall()...  intent = " + intent);

	        String number;

	        // Check the current ServiceState to make sure it's OK
	        // to even try making a call.
	        InCallInitStatus okToCallStatus = checkIfOkToInitiateOutgoingCall();

	        try {
	            number = getInitialNumber(intent);
	        } catch (PhoneUtils.VoiceMailNumberMissingException ex) {
	            // If the call status is NOT in an acceptable state, it
	            // may effect the way the voicemail number is being
	            // retrieved.  Mask the VoiceMailNumberMissingException
	            // with the underlying issue of the phone state.
	            if (okToCallStatus != InCallInitStatus.SUCCESS) {
	                if (DBG) log("Voicemail number not reachable in current SIM card state.");
	                return okToCallStatus;
	            }
	            if (DBG) log("VoiceMailNumberMissingException from getInitialNumber()");
	            return InCallInitStatus.VOICEMAIL_NUMBER_MISSING;
	        }

	        if (number == null) {
	            Log.w(LOG_TAG, "placeCall: couldn't get a phone number from Intent " + intent);
	            return InCallInitStatus.NO_PHONE_NUMBER_SUPPLIED;
	        }

	        boolean isEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(number);
	        boolean isEmergencyIntent = Intent.ACTION_CALL_EMERGENCY.equals(intent.getAction());

	        if (isEmergencyNumber && !isEmergencyIntent) {
	            Log.e(LOG_TAG, "Non-CALL_EMERGENCY Intent " + intent
	                    + " attempted to call emergency number " + number
	                    + ".");
	            return InCallInitStatus.CALL_FAILED;
	        } else if (!isEmergencyNumber && isEmergencyIntent) {
	            Log.e(LOG_TAG, "Received CALL_EMERGENCY Intent " + intent
	                    + " with non-emergency number " + number
	                    + " -- failing call.");
	            return InCallInitStatus.CALL_FAILED;
	        }

	        // If we're trying to call an emergency number, then it's OK to
	        // proceed in certain states where we'd usually just bring up
	        // an error dialog:
	        // - If we're in EMERGENCY_ONLY mode, then (obviously) you're allowed
	        //   to dial emergency numbers.
	        // - If we're OUT_OF_SERVICE, we still attempt to make a call,
	        //   since the radio will register to any available network.

	        if (isEmergencyNumber
	            && ((okToCallStatus == InCallInitStatus.EMERGENCY_ONLY)
	                || (okToCallStatus == InCallInitStatus.OUT_OF_SERVICE))) {
	            if (DBG) log("placeCall: Emergency number detected with status = " + okToCallStatus);
	            okToCallStatus = InCallInitStatus.SUCCESS;
	            if (DBG) log("==> UPDATING status to: " + okToCallStatus);
	        }

	        if (okToCallStatus != InCallInitStatus.SUCCESS) {
	            // If this is an emergency call, we call the emergency call
	            // handler activity to turn on the radio and do whatever else
	            // is needed. For now, we finish the InCallScreen (since were
	            // expecting a callback when the emergency call handler dictates
	            // it) and just return the success state.
	            if (isEmergencyNumber && (okToCallStatus == InCallInitStatus.POWER_OFF)) {
	                startActivity(intent.setClassName(this, EmergencyCallHandler.class.getName()));
	                if (DBG) log("placeCall: starting EmergencyCallHandler, finishing InCallScreen...");
	                endInCallScreenSession();
	                return InCallInitStatus.SUCCESS;
	            } else {
	                return okToCallStatus;
	            }
	        }

	        final PhoneApp app = PhoneApp.getInstance();

	        if ((mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) && (mPhone.isOtaSpNumber(number))) {
	            if (DBG) log("placeCall: isOtaSpNumber() returns true");
	            setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
	            if (app.cdmaOtaProvisionData != null) {
	                app.cdmaOtaProvisionData.isOtaCallCommitted = false;
	            }
	        }

	        mNeedShowCallLostDialog = false;

	        // We have a valid number, so try to actually place a call:
	        // make sure we pass along the intent's URI which is a
	        // reference to the contact. We may have a provider gateway
	        // phone number to use for the outgoing call.
	        int callStatus;
	        Uri contactUri = intent.getData();

	        if (null != mProviderGatewayUri &&
	            !(isEmergencyNumber || isEmergencyIntent) &&
	            PhoneUtils.isRoutableViaGateway(number)) {  // Filter out MMI, OTA and other codes.

	            callStatus = PhoneUtils.placeCallVia(
	                this, mPhone, number, contactUri, mProviderGatewayUri);
	        } else {
	        	updateScreen(); //STAR CALL AHJ 100828
	        	Intent i = new Intent(Intent.ACTION_VIDEO_CALL_PRIVILEGED,
	                      Uri.fromParts("tel", number, null));
	        	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        	startActivity(i);
	        	finish();	
	        }
	        return InCallInitStatus.SUCCESS;
	    }

//<!--//20100929 sumi920.kim@lge.com VideoCall Origination In CallEnd View [END_LGE_LAB1]	 -->	 

// 20101019 sumi920.kim@lge.com for Home Key In Alerting [START_LGE_LAB1]
	  void sendHomeKeyUpDelayTime() {
		  if (DBG) log("sendHomeKeyUpDelayTime()...");
		  mIgnoreHomeKey = false;
		  mHandler.removeMessages(HOME_KEYUP_IGNORE_RINGING_STATE);
		  Message msg = Message.obtain(mHandler, HOME_KEYUP_IGNORE_RINGING_STATE);
		  mHandler.sendMessageDelayed(msg, HOME_KEYUP_RINGINGSTATE_TIMEDELAY);
	  }
// 20101019 sumi920.kim@lge.com for Home Key In Alerting [END_LGE_LAB1]
	  
	  public CallCard getCallCard()
	  {
		  return mCallCard;
	  }

}
