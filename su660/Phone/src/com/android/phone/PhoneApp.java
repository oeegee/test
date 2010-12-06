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

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.LocalPowerManager;
import android.os.Message;
import android.os.Power;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings.System;
import android.telephony.ServiceState;
import android.util.Config;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.phone.OtaUtils.CdmaOtaScreenState;
import com.android.phone.videophone.VTPreferences;
import com.android.internal.telephony.cdma.TtyIntent;
import com.lge.ims.IFmcCallInterface;


//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
//SIM removed popup
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.view.View;
//OTA reset popup
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.RecentTaskInfo;
import java.util.List;
//
import com.lge.config.StarConfig;
import android.os.IPowerManager;
import android.os.RemoteException;
import com.android.internal.telephony.ATService;
import com.android.internal.telephony.IATService;
import com.android.internal.telephony.ATResponse;
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]

//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
//START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
import android.provider.Settings;
// LGE_CHANGE_S, [DATA_LAB1_COMMON_FROYO_010][LGE_DATA][jaeok.bae@lge.com], 2010-10-01, WCDMA Roaming.. Add check condition of roaming check db
import com.lge.config.StarConfig;
// LGE_CHANGE_E, [DATA_LAB1_COMMON_FROYO_010][LGE_DATA][jaeok.bae@lge.com], 2010-10-01, WCDMA Roaming.. Add check condition of roaming check db
//jundj@mo2.co.kr start
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.gsm.ModemInfo;
import com.android.phone.OtaUtils.CdmaOtaScreenState;
import com.mtelo.visualexpression.VEUtils;
import com.mtelo.visualexpression.VE_ContentManager;
import com.mtelo.visualexpression.VEUtils.IsVEPlayerRunning;
//jundj@mo2.co.kr end

//20101111 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
// LGE_UPDATE_S sanghoon.roh@lge.com 2010/05/29 end a call by long pressing a headsethook key
import android.content.ActivityNotFoundException;
import android.app.ActivityManagerNative;
// LGE_UPDATE_E sanghoon.roh@lge.com 2010/05/29 end a call by long pressing a headsethook key
// LGE_UPDATE_S sanghoon.roh@lge.com 2010/06/04 handle LG headsethook key scenario
import android.view.ViewConfiguration;
// LGE_UPDATE_E sanghoon.roh@lge.com 2010/06/04 handle LG headsethook key scenario
//20101111 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]

//20101113 sumi920.kim@lge.com Thunder porting
//START jiyoung.yoon@lge.com SKT DM TEST
import android.app.Activity;
//END jiyoung.yoon@lge.com SKT DM TEST

//20101111 yongjin.her@lge.com poring VoIPPhone [START_LGE_LAB1]
import com.android.internal.telephony.voip.VoIPPhone;
import com.android.internal.telephony.voip.VoIPMissedCallInfo;
//20101111 yongjin.her@lge.com poring VoIPPhone [END_LGE_LAB1]
/**
 * Top-level Application class for the Phone app.
 */
public class PhoneApp extends Application implements AccelerometerListener.OrientationListener {
    /* package */ static final String LOG_TAG = "PhoneApp";

    /**
     * Phone app-wide debug level:
     *   0 - no debug logging
     *   1 - normal debug logging if ro.debuggable is set (which is true in
     *       "eng" and "userdebug" builds but not "user" builds)
     *   2 - ultra-verbose debug logging
     *
     * Most individual classes in the phone app have a local DBG constant,
     * typically set to
     *   (PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1)
     * or else
     *   (PhoneApp.DBG_LEVEL >= 2)
     * depending on the desired verbosity.
     */
    /* package */ public static final int DBG_LEVEL = 1;

    private static final boolean DBG = true;
            //(PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = true; //(PhoneApp.DBG_LEVEL >= 2);

    // Message codes; see mHandler below.
    private static final int EVENT_SIM_NETWORK_LOCKED = 3;
    private static final int EVENT_WIRED_HEADSET_PLUG = 7;
    private static final int EVENT_SIM_STATE_CHANGED = 8;
    private static final int EVENT_UPDATE_INCALL_NOTIFICATION = 9;
    private static final int EVENT_DATA_ROAMING_DISCONNECTED = 10;
    private static final int EVENT_DATA_ROAMING_OK = 11;
    private static final int EVENT_UNSOL_CDMA_INFO_RECORD = 12;
    private static final int EVENT_DOCK_STATE_CHANGED = 13;
    private static final int EVENT_TTY_PREFERRED_MODE_CHANGED = 14;
    private static final int EVENT_TTY_MODE_GET = 15;
    private static final int EVENT_TTY_MODE_SET = 16;
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
    private static final int EVENT_SIM_ABSENT           = 17;
    private static final int EVENT_SIM_LOCKED           = 18;
    private static final int EVENT_PHONE_RESET          = 19;    // event for excuting the reset
    private static final int EVENT_PHONE_RESET_COMPLETE = 20;    // event after reset complete ril command to AMSS
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]

    // The MMI codes are also used by the InCallScreen.
    public static final int MMI_INITIATE = 51;
    public static final int MMI_COMPLETE = 52;
    public static final int MMI_CANCEL = 53;
    // Don't use message codes larger than 99 here; those are reserved for
    // the individual Activities of the Phone UI.
//20101111 yongjin.her@lge.com poring VoIPPhone [START_LGE_LAB1]
    public static final int EVENT_VOIP_MISSED_CALL = 60;
//20101111 yongjin.her@lge.com poring VoIPPhone [END_LGE_LAB1]

//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
        private static final int SKT_RESET_DELAY_TIME = 5 * 1000;   //SIM_ABSENT,IccCard.State.SIM_REMOVED event 
                                                                    //  => phone reset after some delay, SKT only
        // KT auto reset with the Toast Message for USIM REFRESH event to meet KT UI Scenario Spec
        private static final int RESET_DELAY_TIME = 5 * 1000;   //KT Reset Delay time for OTA REFRESH event
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]

    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
    //START jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
    private boolean isProximitySensorSetting = true;
    //END jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]

//20101102 wonho.moon@lge.com Password check variable 
	private boolean isCheckPassword = false;
	//20101113 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
	//START jiyoung.yoon@lge.com SKT DM TEST
	// INCOM -> answer
	// end -> end
	private static final String SKT_DM_INCOMCALL	= "android.intent.action.INCOMING_CALL";
	private static final String SKT_DM_ENDCALL		= "android.intent.action.CALL_END";
	private static final String SKT_DM_VOICEREC 	= "android.intent.extra.file";
	//END jiyoung.yoon@lge.com SKT DM TEST
	//20101113 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]

    /**
     * Allowable values for the poke lock code (timeout between a user activity and the
     * going to sleep), please refer to {@link com.android.server.PowerManagerService}
     * for additional reference.
     *   SHORT uses the short delay for the timeout (SHORT_KEYLIGHT_DELAY, 6 sec)
     *   MEDIUM uses the medium delay for the timeout (MEDIUM_KEYLIGHT_DELAY, 15 sec)
     *   DEFAULT is the system-wide default delay for the timeout (1 min)
     */
    public enum ScreenTimeoutDuration {
        SHORT,
        MEDIUM,
        DEFAULT
    }

    /**
     * Allowable values for the wake lock code.
     *   SLEEP means the device can be put to sleep.
     *   PARTIAL means wake the processor, but we display can be kept off.
     *   FULL means wake both the processor and the display.
     */
    public enum WakeState {
        SLEEP,
        PARTIAL,
        FULL
    }

    private static PhoneApp sMe;

    // A few important fields we expose to the rest of the package
    // directly (rather than thru set/get methods) for efficiency.
    Phone phone;
    CallNotifier notifier;
    Ringer ringer;
    BluetoothHandsfree mBtHandsfree;
    PhoneInterfaceManager phoneMgr;
    int mBluetoothHeadsetState = BluetoothHeadset.STATE_ERROR;
    int mBluetoothHeadsetAudioState = BluetoothHeadset.STATE_ERROR;
    boolean mShowBluetoothIndication = false;
    static int mDockState = Intent.EXTRA_DOCK_STATE_UNDOCKED;

    // Internal PhoneApp Call state tracker
    CdmaPhoneCallState cdmaPhoneCallState;

    // The InCallScreen instance (or null if the InCallScreen hasn't been
    // created yet.)
    //jundj@mo2.co.kr start change private to public
    public InCallScreen mInCallScreen;
    //jundj@mo2.co.kr end


    //20101111 sumi920.kim@lge.com Thunder porting
    //LGE_CHANGE_S heechul.hyun 2010-09-14 For after reject call(because mediabuttonintentreceiver use ACTION_DOWN)
    private static boolean mIsLongHookPress = false;
    //LGE_CHANGE_E heechul.hyun 2010-09-14 For after reject call(because mediabuttonintentreceiver use ACTION_DOWN)
    
    

    // The currently-active PUK entry activity and progress dialog.
    // Normally, these are the Emergency Dialer and the subsequent
    // progress dialog.  null if there is are no such objects in
    // the foreground.
    private Activity mPUKEntryActivity;
    private ProgressDialog mPUKEntryProgressDialog;

    private boolean mIsSimPinEnabled;
    private String mCachedSimPin;

    // True if a wired headset is currently plugged in, based on the state
    // from the latest Intent.ACTION_HEADSET_PLUG broadcast we received in
    // mReceiver.onReceive().
    private boolean mIsHeadsetPlugged;

    // True if the keyboard is currently *not* hidden
    // Gets updated whenever there is a Configuration change
    private boolean mIsHardKeyboardOpen;

    // True if we are beginning a call, but the phone state has not changed yet
    private boolean mBeginningCall;

    // Last phone state seen by updatePhoneState()
    Phone.State mLastPhoneState = Phone.State.IDLE;

    private WakeState mWakeState = WakeState.SLEEP;
    private ScreenTimeoutDuration mScreenTimeoutDuration = ScreenTimeoutDuration.DEFAULT;
    private boolean mIgnoreTouchUserActivity = false;
    private IBinder mPokeLockToken = new Binder();
    private IPowerManager mPowerManagerService;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager.WakeLock mProximityWakeLock;
    private KeyguardManager mKeyguardManager;
    private StatusBarManager mStatusBarManager;
// 2010.10.20 minji.bae@lge.com Keyguard(lockscreen) setting during Call, Emergency List, Emergency Dial [START_LGE_LAB1]
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-04-22 : for reenable keyguard, mKeyguardLock must be disabled, if mKeyguardLock is not disabled & try to reenable keyguard, Phone app will be dead
    private boolean mIsDisabledKeyguardInCall = false;
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-04-22 : for reenable keyguard, mKeyguardLock must be disabled, if mKeyguardLock is not disabled & try to reenable keyguard, Phone app will be dead
    private KeyguardManager.KeyguardLock mKeyguardLock;
// 2010.10.20 minji.bae@lge.com Keyguard(lockscreen) setting during Call, Emergency List, Emergency Dial [END_LGE_LAB1]
    private int mStatusBarDisableCount;
    private AccelerometerListener mAccelerometerListener;
    private int mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;

    // Broadcast receiver for various intent broadcasts (see onCreate())
    private final BroadcastReceiver mReceiver = new PhoneAppBroadcastReceiver();

    // Broadcast receiver purely for ACTION_MEDIA_BUTTON broadcasts
    private final BroadcastReceiver mMediaButtonReceiver = new MediaButtonBroadcastReceiver();

    /** boolean indicating restoring mute state on InCallScreen.onResume() */
    private boolean mShouldRestoreMuteOnInCallResume;

    // Following are the CDMA OTA information Objects used during OTA Call.
    // cdmaOtaProvisionData object store static OTA information that needs
    // to be maintained even during Slider open/close scenarios.
    // cdmaOtaConfigData object stores configuration info to control visiblity
    // of each OTA Screens.
    // cdmaOtaScreenState object store OTA Screen State information.
    public OtaUtils.CdmaOtaProvisionData cdmaOtaProvisionData;
    public OtaUtils.CdmaOtaConfigData cdmaOtaConfigData;
    public OtaUtils.CdmaOtaScreenState cdmaOtaScreenState;
    public OtaUtils.CdmaOtaInCallScreenUiState cdmaOtaInCallScreenUiState;

    // TTY feature enabled on this platform
    private boolean mTtyEnabled;
    // Current TTY operating mode selected by user
    private int mPreferredTtyMode = Phone.TTY_MODE_OFF;

//20101111 yongjin.her@lge.com poring VoIPPhone [START_LGE_LAB1]
    private VoIPPhone mVoIPPhone;
//20101111 yongjin.her@lge.com poring VoIPPhone [END_LGE_LAB1]

   public static boolean mIsVideoCall;
   public static boolean mLowbatteryHangup;

    //LGE_FMC
    public static int  isFMCState = 0 ;  
    private static IFmcCallInterface iFmcCallInterface = null;
 
   	//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
   	//START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
   	private boolean mIsBeforeKeyguardStateOfIdle = false;
   	public boolean mWillBeResumedAfterScreenOff = false;
   	//END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
   	//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]
    //jundj@mo2.co.kr Start 이벤트 추가
    private final int EVENT_UNSOL_MODEM_INFO = 31; 
    //jundj@mo2.co.kr End이벤트 추가

//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
    //
    //SIM REFRESH
    //
    private String ACTION_OTA_USIM_REFRESH_TO_RESET = "android.intent.action.OTA_USIM_REFRESH_TO_RESET";

    
    //OTA reset popup keyListener
    public static DialogInterface.OnKeyListener OtaPopupKeyListener = 
                new DialogInterface.OnKeyListener () {
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_BACK: 
                                case KeyEvent.KEYCODE_MENU:
                                case KeyEvent.KEYCODE_HOME:
                                case KeyEvent.KEYCODE_SEARCH:
                                case KeyEvent.KEYCODE_VOLUME_UP:
                                case KeyEvent.KEYCODE_VOLUME_DOWN:
                                case KeyEvent.KEYCODE_CAMERA:
                                case KeyEvent.KEYCODE_ENDCALL:  // maybe not available
                                    return true;
                            }
                            return false;
                        }
                    };

//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]

    /**
     * Set the restore mute state flag. Used when we are setting the mute state
     * OUTSIDE of user interaction {@link PhoneUtils#startNewCall(Phone)}
     */
    /*package*/void setRestoreMuteOnInCallResume (boolean mode) {
        mShouldRestoreMuteOnInCallResume = mode;
    }

    /**
     * Get the restore mute state flag.
     * This is used by the InCallScreen {@link InCallScreen#onResume()} to figure
     * out if we need to restore the mute state for the current active call.
     */
    /*package*/boolean getRestoreMuteOnInCallResume () {
        return mShouldRestoreMuteOnInCallResume;
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Phone.State phoneState;
            //jundj@mo2.co.kr Start
            AsyncResult ar;
            //jundj@mo2.co.kr End
            switch (msg.what) {

                // TODO: This event should be handled by the lock screen, just
                // like the "SIM missing" and "Sim locked" cases (bug 1804111).
                case EVENT_SIM_NETWORK_LOCKED:
                    if (getResources().getBoolean(R.bool.ignore_sim_network_locked_events)) {
                        // Some products don't have the concept of a "SIM network lock"
                        Log.i(LOG_TAG, "Ignoring EVENT_SIM_NETWORK_LOCKED event; "
                              + "not showing 'SIM network unlock' PIN entry screen");
                    } else {
                        // Normal case: show the "SIM network unlock" PIN entry screen.
                        // The user won't be able to do anything else until
                        // they enter a valid SIM network PIN.
                        Log.i(LOG_TAG, "show sim depersonal panel");
                        IccNetworkDepersonalizationPanel ndpPanel =
                                new IccNetworkDepersonalizationPanel(PhoneApp.getInstance());
                        ndpPanel.show();
                    }
                    break;

                case EVENT_UPDATE_INCALL_NOTIFICATION:
                    // Tell the NotificationMgr to update the "ongoing
                    // call" icon in the status bar, if necessary.
                    // Currently, this is triggered by a bluetooth headset
                    // state change (since the status bar icon needs to
                    // turn blue when bluetooth is active.)
                    NotificationMgr.getDefault().updateInCallNotification();
                    break;

                case EVENT_DATA_ROAMING_DISCONNECTED:
                    NotificationMgr.getDefault().showDataDisconnectedRoaming();
                    break;

                case EVENT_DATA_ROAMING_OK:
                    NotificationMgr.getDefault().hideDataDisconnectedRoaming();
                    break;

                case MMI_COMPLETE:
                    onMMIComplete((AsyncResult) msg.obj);
                    break;

                case MMI_CANCEL:
                    PhoneUtils.cancelMmiCode(phone);
                    break;

                case EVENT_WIRED_HEADSET_PLUG:
                    // Since the presence of a wired headset or bluetooth affects the
                    // speakerphone, update the "speaker" state.  We ONLY want to do
                    // this on the wired headset connect / disconnect events for now
                    // though, so we're only triggering on EVENT_WIRED_HEADSET_PLUG.

                    phoneState = phone.getState();
                    // Do not change speaker state if phone is not off hook
                    if (phoneState == Phone.State.OFFHOOK) {
                        if (mBtHandsfree == null || !mBtHandsfree.isAudioOn()) {
                            if (!isHeadsetPlugged()) {
								PhoneUtils.restoreSpeakerMode(getApplicationContext());	//20101108 sh80.choi@lge.com restore the SPK Phone Mode [LGE_LAB1]
								if(PhoneUtils.isSpeakerOn(getApplicationContext())){
                                    PhoneUtils.changeModemPath(getApplicationContext(), 3); 
								}
								else{
                                    PhoneUtils.changeModemPath(getApplicationContext(), 1);
								}
				                
                            } else {
                                // if the state is "connected", force the speaker off without
                                // storing the state.
                                PhoneUtils.changeModemPath(getApplicationContext(), 2);
                                if(PhoneUtils.isSpeakerOn(getApplicationContext())){
				                    PhoneUtils.turnOnSpeaker(getApplicationContext(), false, false); //20101108 sh80.choi@lge.com No Store when Headset On(true->false) [LGE_LAB1]
                                }
                            }
                        }
                    }
                    // Update the Proximity sensor based on headset state
                    updateProximitySensorMode(phoneState);

                    // Force TTY state update according to new headset state
                    if (mTtyEnabled) {
                        sendMessage(obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
                    }
                    break;

                case EVENT_SIM_STATE_CHANGED:
                    // Marks the event where the SIM goes into ready state.
                    // Right now, this is only used for the PUK-unlocking
                    // process.
                    if (msg.obj.equals(IccCard.INTENT_VALUE_ICC_READY)) {
                        // when the right event is triggered and there
                        // are UI objects in the foreground, we close
                        // them to display the lock panel.
                        if (mPUKEntryActivity != null) {
                            mPUKEntryActivity.finish();
                            mPUKEntryActivity = null;
                        }
                        if (mPUKEntryProgressDialog != null) {
                            mPUKEntryProgressDialog.dismiss();
                            mPUKEntryProgressDialog = null;
                        }
                    }
                    break;

                case EVENT_UNSOL_CDMA_INFO_RECORD:
                    //TODO: handle message here;
                    break;

                case EVENT_DOCK_STATE_CHANGED:
                    // If the phone is docked/undocked during a call, and no wired or BT headset
                    // is connected: turn on/off the speaker accordingly.
                    boolean inDockMode = false;
                    if (mDockState == Intent.EXTRA_DOCK_STATE_DESK ||
                            mDockState == Intent.EXTRA_DOCK_STATE_CAR) {
                        inDockMode = true;
                    }
                    if (VDBG) Log.d(LOG_TAG, "received EVENT_DOCK_STATE_CHANGED. Phone inDock = "
                            + inDockMode);

                    phoneState = phone.getState();
                    if (phoneState == Phone.State.OFFHOOK &&
                            !isHeadsetPlugged() &&
                            !(mBtHandsfree != null && mBtHandsfree.isAudioOn())) {
                        PhoneUtils.turnOnSpeaker(getApplicationContext(), inDockMode, true);

                        if (mInCallScreen != null) {
                            mInCallScreen.requestUpdateTouchUi();
                        }
                    }

                case EVENT_TTY_PREFERRED_MODE_CHANGED:
                    // TTY mode is only applied if a headset is connected
                    int ttyMode;
                    if (isHeadsetPlugged()) {
                        ttyMode = mPreferredTtyMode;
                    } else {
                        ttyMode = Phone.TTY_MODE_OFF;
                    }
                    phone.setTTYMode(ttyMode, mHandler.obtainMessage(EVENT_TTY_MODE_SET));
                    break;

                case EVENT_TTY_MODE_GET:
                    handleQueryTTYModeResponse(msg);
                    break;

                case EVENT_TTY_MODE_SET:
                    handleSetTTYModeResponse(msg);
                    break;
                //jundj@mo2.co.kr START
                case EVENT_UNSOL_MODEM_INFO: // Unsol에서 noti가 되면 이부분으로 값이 넘어온다.
                    if(VEUtils.isSKTFeature()){
                        Log.d(LOG_TAG, "wang: InCallScreen EVENT_UNSOL_MODEM_INFO--");
                        
                        String[] mVEData = null;  // 초기화
                        String url = "";
                        String codingScheme = "";
                        ar = (AsyncResult) msg.obj;
                        Log.d(LOG_TAG, "wang: ar.exception :"+ar.exception);
                        
                        if (ar.exception == null) {
                            mVEData = (String[]) ar.result; // 값은 String[] Type
                            if(null != mVEData && mVEData.length > 0){
                                
                                for(int i=0;i<mVEData.length;i++){
                                    Log.d(LOG_TAG, "wang:VEDATA["+i+"]:"+mVEData[i]);
                                }
                                // coloring [outgoing call]
                                if (Integer.parseInt(mVEData[0]) == ModemInfo.LGE_UNSOL_VISUAL_COLORRING) {
                                    url = mVEData[1]; // URL DATA
                                    codingScheme = mVEData[2];
                                
                                // lettering [incomming call]
                                } else if (Integer.parseInt(mVEData[0]) == ModemInfo.LGE_UNSOL_VISUAL_LETTERING) {
                                    url = mVEData[1]; // URL DATA
                                    codingScheme = mVEData[2];
                                    
                                    Log.d(LOG_TAG, "wang: ve_contentmgr initialize...");
                                    VEUtils.initVEContent(mHandler, url);
                                }
                                
                            }
                            break;
                        }
                    }
                    break;
                case VE_ContentManager.HANDLE_MSG_READY_PLAY:
                    if(VEUtils.isSKTFeature()){
                        Log.d(LOG_TAG, "wang: HANDLE_MSG_READY_PLAY : VE play begin --");
                        //change from default-callcard to VE-callcard
                        VEUtils.setVisibleCallCardPersonInfo(VEUtils.CALLCARD_PERSONINFO_VE);
                        // play
                        VE_ContentManager.getHandler().sendEmptyMessage(VE_ContentManager.HANDLE_MSG_START_PLAY);
                    }
                    break;
                    
                case VE_ContentManager.HANDLE_MSG_STOP_PLAY:
                    if(VEUtils.isSKTFeature()){
                        Log.d(LOG_TAG, "wang: HANDLE_MSG_STOP_PLAY--");
                        VE_ContentManager.getHandler().sendEmptyMessage(VE_ContentManager.HANDLE_MSG_STOP_PLAY);
                    }
                    break;
                //jundj@mo2.co.kr END

//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
                case EVENT_PHONE_RESET:
                    Log.d(LOG_TAG,"rcv EVENT_PHONE_RESET");
                    // Execute the phone reset
                    // And make event EVENT_PHONE_RESET_COMPLETE after phone reset ril command
                    /*//for Qualcomm
                    phone.resetPhone(mHandler.obtainMessage(EVENT_PHONE_RESET_COMPLETE));
                    */
                    //for Infenion
                    mResetThread = new ResetThread();
                    mResetThread.start();       

                    break;
                case EVENT_PHONE_RESET_COMPLETE:
                    Log.d(LOG_TAG,"rcv EVENT_PHONE_RESET_COMPLETE");
                    // Nothing to do here. just receive a result from ril in QualComm case

                    //
                    {//Infenion case
                        response = new ATResponse((String) msg.obj);

                        int result = response.getResult();
                        
            			if(result >= 1) {
        					mCurrentStatus = ResetThread.STATE_READY;
        					try {
        					    Log.d(LOG_TAG,"reboot request complete result="+result);
                                IPowerManager mPowerManager = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE)); 
                                mPowerManager.reboot(null);
        					} catch(RemoteException e) {
        						e.printStackTrace();
        					}
            			}
                    }
                    break;

                
                case EVENT_SIM_LOCKED:
                    Log.d(LOG_TAG,"rcv EVENT_SIM_LOCKED");
//                    mIsSimPinEnabled = true;
//
//                    if (Config.LOGV) Log.v(LOG_TAG, "show sim unlock panel");
//                    SimPinUnlockPanel pinUnlockPanel = new SimPinUnlockPanel(
//                            PhoneApp.getInstance());
//                    pinUnlockPanel.show();
                    break;

                case EVENT_SIM_ABSENT:
                    Log.d(LOG_TAG,"rcv EVENT_SIM_ABSENT");

                    if(StarConfig.COUNTRY.equals("KR"))
                    {
                        IccCard.State iccState;

                        IccCard sim = phone.getIccCard();

                        iccState = sim.getIccCardState();
//LGE_CHANGE_S [wonho.moon@lge.com] 2010/04/26 : SIM removed popup
                        //String serviceProvider = android.os.SystemProperties.get("ro.telephony.service_provider", "null");
//LGE_CHANGE_E [wonho.moon@lge.com] 2010/04/26 : SIM removed popup

                        Log.d(LOG_TAG, "[LGE_USIM] iccState = " + iccState );

//LGE_CHANGE_S [yongjin.her@lge.com] 2010/04/26 : SIM removed popup
                        if (iccState == IccCard.State.SIM_REMOVED) {
//LGE_CHANGE_S [wonho.moon@lge.com] 2010/04/26 : SIM removed popup
                            if(StarConfig.OPERATOR.equals("SKT")) {

                                //delayed phone reset, only SKT
                                sendEmptyMessageDelayed(EVENT_PHONE_RESET, SKT_RESET_DELAY_TIME);
                                
                                showDialog(getApplicationContext(), 
                                            com.lge.internal.R.string.SKT_STR_USIM_ERR_RECHECK, 
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface arg0, int arg1) {
                                                    //replaced
                                                    /*
                                                    phone.resetPhone(mHandler.obtainMessage(EVENT_PHONE_RESET_COMPLETE));
                                                    */
                                                    sendEmptyMessage(EVENT_PHONE_RESET);
                                                }
                                            },
                                            null //keyListener
                                    );
                            } else if (StarConfig.OPERATOR.equals("KT")) {
                                //NOT Support KT in this model
                                /*
                                showDialog(getApplicationContext(), 
                                            com.lge.internal.R.string.STR_USIM_ERR_RECHECK, 
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface arg0, int arg1) {
                                                    //pass
                                                }
                                            },
                                            null //keyListener
                                    );
                                */
                            } else {
                                showDialog(getApplicationContext(), 
                                            com.lge.internal.R.string.STR_USIM_ERR_RECHECK, 
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface arg0, int arg1) {
                                                    //pass
                                                }
                                            },
                                            null //keyListener
                                    );
                            }
//LGE_CHANGE_E [wonho.moon@lge.com] 2010/04/26 : SIM removed popup
                            return;
                        }
//LGE_CHANGE_E [yongjin.her@lge.com] 2010/04/26 : SIM removed popup

                    }

                    break;
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]

//20101111 yongjin.her@lge.com poring VoIPPhone [START_LGE_LAB1]
                case EVENT_VOIP_MISSED_CALL:
                    Log.d(LOG_TAG, "rcv EVENT_VOIP_MISSED_CALL");

                    AsyncResult result = (AsyncResult) msg.obj;

                    Log.d(LOG_TAG, "EVENT_VOIP_MISSED_CALL AsyncResult result = "+result);

                    VoIPMissedCallInfo missedCall = (VoIPMissedCallInfo)result.result;

                    Log.d(LOG_TAG, "EVENT_VOIP_MISSED_CALL missedCall = "+missedCall);

                    if(missedCall!=null)
                    {
                        displayVoIPMissedCallNotification(
                            missedCall.name,
                            missedCall.number,
                            missedCall.label,
                            missedCall.date
                            );
                    }
                    break;
//20101111 yongjin.her@lge.com poring VoIPPhone [END_LGE_LAB1]

            }
        }
    };

    public PhoneApp() {
        sMe = this;
    }

    @Override
    public void onCreate() {
        if (VDBG) Log.v(LOG_TAG, "onCreate()...");

        ContentResolver resolver = getContentResolver();

        if (phone == null) {
            // Initialize the telephony framework
            PhoneFactory.makeDefaultPhones(this);

            // Get the default phone
            phone = PhoneFactory.getDefaultPhone();

            NotificationMgr.init(this);

            phoneMgr = new PhoneInterfaceManager(this, phone);

            int phoneType = phone.getPhoneType();

            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                // Create an instance of CdmaPhoneCallState and initialize it to IDLE
                cdmaPhoneCallState = new CdmaPhoneCallState();
                cdmaPhoneCallState.CdmaPhoneCallStateInit();
            }

            if (BluetoothAdapter.getDefaultAdapter() != null) {
                mBtHandsfree = new BluetoothHandsfree(this, phone);
                startService(new Intent(this, BluetoothHeadsetService.class));
            } else {
                // Device is not bluetooth capable
                mBtHandsfree = null;
            }

            ringer = new Ringer(phone);

            // before registering for phone state changes
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
            // lock used to keep the processor awake, when we don't care for the display.
            mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
            // Wake lock used to control proximity sensor behavior.
            if ((pm.getSupportedWakeLockFlags()
                 & PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) != 0x0) {
                mProximityWakeLock =
                        pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, LOG_TAG);
            }
            if (DBG) Log.d(LOG_TAG, "onCreate: mProximityWakeLock: " + mProximityWakeLock);

            // create mAccelerometerListener only if we are using the proximity sensor
            if (proximitySensorModeEnabled()) {
                mAccelerometerListener = new AccelerometerListener(this, this);
            }

            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            mKeyguardLock = mKeyguardManager.newKeyguardLock(LOG_TAG);    // 2010.10.20 minji.bae@lge.com Keyguard(lockscreen) setting during Call, Emergency List, Emergency Dial [LGE_LAB1]
            mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);

            // get a handle to the service so that we can use it later when we
            // want to set the poke lock.
            mPowerManagerService = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));

            notifier = new CallNotifier(this, phone, ringer, mBtHandsfree, new CallLogAsync());

            // register for ICC status
            IccCard sim = phone.getIccCard();
            if (sim != null) {
                if (VDBG) Log.v(LOG_TAG, "register for ICC status");
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
                sim.registerForAbsent(mHandler, EVENT_SIM_ABSENT, null);
                sim.registerForLocked(mHandler, EVENT_SIM_LOCKED, null);
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]
                sim.registerForNetworkLocked(mHandler, EVENT_SIM_NETWORK_LOCKED, null);
            }

            // register for MMI/USSD
            if (phoneType == Phone.PHONE_TYPE_GSM) {
                phone.registerForMmiComplete(mHandler, MMI_COMPLETE, null);
            }

            // register connection tracking to PhoneUtils
            PhoneUtils.initializeConnectionHandler(phone);

            // Read platform settings for TTY feature
            mTtyEnabled = getResources().getBoolean(R.bool.tty_enabled);

            // Register for misc other intent broadcasts.
            IntentFilter intentFilter =
                    new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intentFilter.addAction(BluetoothHeadset.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            intentFilter.addAction(Intent.ACTION_DOCK_EVENT);
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
            intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
            intentFilter.addAction(ACTION_OTA_USIM_REFRESH_TO_RESET);
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]

            //20101024 sumi920.kim@lge.com porting
            //<!--[yujung.lee@lge.com] 2010.10.20	  LAB1_CallUI shutdown happen incomming bell around [START] -->
            intentFilter.addAction(Intent.ACTION_REQUEST_SHUTDOWN);
            //<!--[yujung.lee@lge.com] 2010.10.20	  LAB1_CallUI shutdown happen incomming bell around [END] -->

            //20101113 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
            //START jiyoung.yoon@lge.com SKT DM TEST
            intentFilter.addAction(SKT_DM_INCOMCALL);
            intentFilter.addAction(SKT_DM_ENDCALL);
            //END jiyoung.yoon@lge.com SKT DM TEST
            //20101113 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]
            
            if (mTtyEnabled) {
                intentFilter.addAction(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION);
            }
            intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            registerReceiver(mReceiver, intentFilter);

            // Use a separate receiver for ACTION_MEDIA_BUTTON broadcasts,
            // since we need to manually adjust its priority (to make sure
            // we get these intents *before* the media player.)
            IntentFilter mediaButtonIntentFilter =
                    new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
            //
            // Make sure we're higher priority than the media player's
            // MediaButtonIntentReceiver (which currently has the default
            // priority of zero; see apps/Music/AndroidManifest.xml.)
            mediaButtonIntentFilter.setPriority(1);
            //
            registerReceiver(mMediaButtonReceiver, mediaButtonIntentFilter);

            //set the default values for the preferences in the phone.
            PreferenceManager.setDefaultValues(this, R.xml.network_setting, false);

            PreferenceManager.setDefaultValues(this, R.xml.call_feature_setting, false);

            //jundj@mo2.co.kr Start
            if(VEUtils.isSKTFeature()){
                phone.registerLGEUnsol(mHandler, EVENT_UNSOL_MODEM_INFO, null);  //Unsol에서 noti해주기 위해 등록
            }
            //jundj@mo2.co.kr End
            // Make sure the audio mode (along with some
            // audio-mode-related state of our own) is initialized
            // correctly, given the current state of the phone.
            switch (phone.getState()) {
                case IDLE:
                    if (DBG) Log.d(LOG_TAG, "Resetting audio state/mode: IDLE");
                    PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_IDLE);
                    PhoneUtils.setAudioMode(this, AudioManager.MODE_NORMAL);
                    break;
                case RINGING:
                    if (DBG) Log.d(LOG_TAG, "Resetting audio state/mode: RINGING");
                    PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_RINGING);
                    PhoneUtils.setAudioMode(this, AudioManager.MODE_RINGTONE);
                    break;
                case OFFHOOK:
                    if (DBG) Log.d(LOG_TAG, "Resetting audio state/mode: OFFHOOK");
                    PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_OFFHOOK);
                    PhoneUtils.setAudioMode(this, AudioManager.MODE_IN_CALL);
                    break;
            }
        }

        boolean phoneIsCdma = (phone.getPhoneType() == Phone.PHONE_TYPE_CDMA);

        if (phoneIsCdma) {
            cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
            cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
            cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
            cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();
        }

        // XXX pre-load the SimProvider so that it's ready
        resolver.getType(Uri.parse("content://icc/adn"));
        resolver.delete(Uri.parse("content://icc/phonebook"), null, null);
        
        // start with the default value to set the mute state.
        mShouldRestoreMuteOnInCallResume = false;

        // TODO: Register for Cdma Information Records
        // phone.registerCdmaInformationRecord(mHandler, EVENT_UNSOL_CDMA_INFO_RECORD, null);

        // Read TTY settings and store it into BP NV.
        // AP owns (i.e. stores) the TTY setting in AP settings database and pushes the setting
        // to BP at power up (BP does not need to make the TTY setting persistent storage).
        // This way, there is a single owner (i.e AP) for the TTY setting in the phone.
        if (mTtyEnabled) {
            mPreferredTtyMode = android.provider.Settings.Secure.getInt(
                    phone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_TTY_MODE,
                    Phone.TTY_MODE_OFF);
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
        }
        // Read HAC settings and configure audio hardware
        if (getResources().getBoolean(R.bool.hac_enabled)) {
            int hac = android.provider.Settings.System.getInt(phone.getContext().getContentResolver(),
                                                              android.provider.Settings.System.HEARING_AID,
                                                              0);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameter(CallFeaturesSetting.HAC_KEY, hac != 0 ?
                                      CallFeaturesSetting.HAC_VAL_ON :
                                      CallFeaturesSetting.HAC_VAL_OFF);
        }

//20101111 yongjin.her@lge.com poring VoIPPhone [START_LGE_LAB1]
        if(StarConfig.COUNTRY.equals("KR")) 
        {
            if(mVoIPPhone == null)
            {
                Log.d(LOG_TAG,"mVoIPPhone is null, now create");
                mVoIPPhone = VoIPPhone.getInstance();

                Log.d(LOG_TAG,"mVoIPPhone setVoIPPhone");
                mVoIPPhone.setVoIPPhone(this);

                Log.d(LOG_TAG,"mVoIPPhone is registerForVoipMissedCall");
                mVoIPPhone.registerForVoipMissedCall(mHandler, EVENT_VOIP_MISSED_CALL, null);
            }
        }
//20101111 yongjin.her@lge.com poring VoIPPhone [START_LGE_LAB1]

   }

    //jundj@mo2.co.kr START
    @Override
    public void onTerminate(){
        super.onTerminate();
        if(VEUtils.isSKTFeature()){
            phone.unregisterLGEUnsol(mHandler);   //Unsol에서 noti해주는 것을 해제
        }
    }
    //jundj@mo2.co.kr End
    
    /**
     * get state of VE player visibility
     * 0 : default , 1 : ve
     */
    public int getVEVisibility(){
        if( 99 == VE_ContentManager.getInstance().get_Class_State()){
            return 0;
        }else{
            return 1;
        }
    }
    
    /**
     * get CallerInfo
     * @return CallerInfo
     */
    public CallerInfo getCallerInfo(){
        return PhoneUtils.getCallerInfo(getApplicationContext(), phone.getRingingCall().getEarliestConnection());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            mIsHardKeyboardOpen = true;
        } else {
            mIsHardKeyboardOpen = false;
        }

        // Update the Proximity sensor based on keyboard state
        updateProximitySensorMode(phone.getState());
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Returns the singleton instance of the PhoneApp.
     */
    //jundj@mo2.co.kr start add public
    public static PhoneApp getInstance() {
        return sMe;
    }
    //jundj@mo2.co.kr end

    //jundj@mo2.co.kr start add public
    public Ringer getRinger() {
        return ringer;
    }
    //jundj@mo2.co.kr end

    BluetoothHandsfree getBluetoothHandsfree() {
        return mBtHandsfree;
    }

    static Intent createCallLogIntent() {
        Intent  intent = new Intent(Intent.ACTION_VIEW, null);
        intent.setType("vnd.android.cursor.dir/calls");
        return intent;
    }

    /**
     * Return an Intent that can be used to bring up the in-call screen.
     *
     * This intent can only be used from within the Phone app, since the
     * InCallScreen is not exported from our AndroidManifest.
     */
    /* package */ static Intent createInCallIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.setClassName("com.android.phone", getCallScreenClassName());
        return intent;
    }

    /**
     * Variation of createInCallIntent() that also specifies whether the
     * DTMF dialpad should be initially visible when the InCallScreen
     * comes up.
     */
    /* package */ static Intent createInCallIntent(boolean showDialpad) {
        Intent intent = createInCallIntent();
        intent.putExtra(InCallScreen.SHOW_DIALPAD_EXTRA, showDialpad);
        return intent;
    }

    static String getCallScreenClassName() {
	if(mIsVideoCall)		
	        return InVideoCallScreen.class.getName();
	else
	        return InCallScreen.class.getName();
    }

    /**
     * Starts the InCallScreen Activity.
     */
    void displayCallScreen() {
        if (VDBG) Log.d(LOG_TAG, "displayCallScreen()...");
        startActivity(createInCallIntent());
        Profiler.callScreenRequested();
    }


//20101111 yongjin.her@lge.com poring VoIPPhone [START_LGE_LAB1]
	void displayVoIPMissedCallNotification(String name, String phoneNumber,String label, long time)
	{
		//NotificationMgr.getDefault().notifyMissedCall(ci.name, ci.phoneNumber, ci.phoneLabel, date);
		//NotificationMgr.getDefault().notifyMissedCall("test", "2223333", null, 0);
		//tificationMgr.getDefault().notifyMissedCall(null, "2223333", null, 0);
		NotificationMgr.getDefault().notifyMissedCall(name, phoneNumber, label, time);

	}
//20101111 yongjin.her@lge.com poring VoIPPhone [END_LGE_LAB1]
    /**
     * Helper function to check for one special feature of the CALL key:
     * Normally, when the phone is idle, CALL takes you to the call log
     * (see the handler for KEYCODE_CALL in PhoneWindow.onKeyUp().)
     * But if the phone is in use (either off-hook or ringing) we instead
     * handle the CALL button by taking you to the in-call UI.
     *
     * @return true if we intercepted the CALL keypress (i.e. the phone
     *              was in use)
     *
     * @see DialerActivity#onCreate
     */
    boolean handleInCallOrRinging() {
        if (phone.getState() != Phone.State.IDLE) {
            // Phone is OFFHOOK or RINGING.
            if (DBG) Log.v(LOG_TAG,
                           "handleInCallOrRinging: show call screen");
            displayCallScreen();
            return true;
        }
        return false;
    }

    boolean isSimPinEnabled() {
        return mIsSimPinEnabled;
    }

    boolean authenticateAgainstCachedSimPin(String pin) {
        return (mCachedSimPin != null && mCachedSimPin.equals(pin));
    }

    void setCachedSimPin(String pin) {
        mCachedSimPin = pin;
    }

    void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }
    
    InCallScreen getInCallScreen()
    {
    	return mInCallScreen;
    }

    /**
     * @return true if the in-call UI is running as the foreground
     * activity.  (In other words, from the perspective of the
     * InCallScreen activity, return true between onResume() and
     * onPause().)
     *
     * Note this method will return false if the screen is currently off,
     * even if the InCallScreen *was* in the foreground just before the
     * screen turned off.  (This is because the foreground activity is
     * always "paused" while the screen is off.)
     */
    boolean isShowingCallScreen() {
        if (mInCallScreen == null) return false;
        return mInCallScreen.isForegroundActivity();
    }

    /**
     * Dismisses the in-call UI.
     *
     * This also ensures that you won't be able to get back to the in-call
     * UI via the BACK button (since this call removes the InCallScreen
     * from the activity history.)
     * For OTA Call, it call InCallScreen api to handle OTA Call End scenario
     * to display OTA Call End screen.
     */
    void dismissCallScreen() {
        if (mInCallScreen != null) {
            if (mInCallScreen.isOtaCallInActiveState()
                    || mInCallScreen.isOtaCallInEndState()
                    || ((cdmaOtaScreenState != null)
                    && (cdmaOtaScreenState.otaScreenState
                            != CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED))) {
                // TODO: During OTA Call, display should not become dark to
                // allow user to see OTA UI update. Phone app needs to hold
                // a SCREEN_DIM_WAKE_LOCK wake lock during the entire OTA call.
                wakeUpScreen();
                // If InCallScreen is not in foreground we resume it to show the OTA call end screen
                // Fire off the InCallScreen intent
                displayCallScreen();

                mInCallScreen.handleOtaCallEnd();
                return;
            } else {
                mInCallScreen.finish();
            }
        }
    }
    
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
    //START jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
    void setWillBeResumedAfterScreenOff(boolean value) {
    	if (VDBG) Log.w(LOG_TAG, "setWillBeResumedAfterScreenOff() : value = " + value);
    	mWillBeResumedAfterScreenOff = value;
    }

    boolean getWillBeResumedAfterScreenOff() {
    	if (VDBG) Log.w(LOG_TAG, "getWillBeResumedAfterScreenOff()");
    	return mWillBeResumedAfterScreenOff;
    }
    // LGE_CHANGE_S [shinhae.lee@lge.com] 2010-06-15 : Display Call End when is not showing call screen
    void displayCallScreenCallEnd() {
    	if (VDBG) Log.d(LOG_TAG, "displayCallScreenCallEnd()...");
    	
    	Intent intent = createInCallIntent();
    	intent.putExtra(InCallScreen.ACTION_DISPLAY_CALLEND, true);
    	startActivity(intent);
    	Profiler.callScreenRequested();
    }
    // LGE_CHANGE_E [shinhae.lee@lge.com] 2010-06-15 : Display Call End when is not showing call screen
    //END jahyun.park@lge.com 2010.08.16 TD : 27778 TDMB play -> In call -> sleep -> call end -> lcd on
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]
    /**
     * Handle OTA events
     *
     * When OTA call is active and display becomes dark, then CallNotifier will
     * handle OTA Events by calling this api which then calls OtaUtil function.
     */
    void handleOtaEvents(Message msg) {

        if (DBG) Log.d(LOG_TAG, "Enter handleOtaEvents");
        if ((mInCallScreen != null) && (!isShowingCallScreen())) {
            if (mInCallScreen.otaUtils != null) {
                mInCallScreen.otaUtils.onOtaProvisionStatusChanged((AsyncResult) msg.obj);
            }
        }
    }


// LGE_AUTO_REDIAL START
    /**
     * Get the call log status.
     *
     */
    boolean getLogState() {
    	if (mInCallScreen != null) {
            return mInCallScreen.disableLog();
        }
    	else
            return false;
    }

// LGE_AUTO_REDIAL END

    /**
     * Sets the activity responsible for un-PUK-blocking the device
     * so that we may close it when we receive a positive result.
     * mPUKEntryActivity is also used to indicate to the device that
     * we are trying to un-PUK-lock the phone. In other words, iff
     * it is NOT null, then we are trying to unlock and waiting for
     * the SIM to move to READY state.
     *
     * @param activity is the activity to close when PUK has
     * finished unlocking. Can be set to null to indicate the unlock
     * or SIM READYing process is over.
     */
    void setPukEntryActivity(Activity activity) {
        mPUKEntryActivity = activity;
    }

    Activity getPUKEntryActivity() {
        return mPUKEntryActivity;
    }

    /**
     * Sets the dialog responsible for notifying the user of un-PUK-
     * blocking - SIM READYing progress, so that we may dismiss it
     * when we receive a positive result.
     *
     * @param dialog indicates the progress dialog informing the user
     * of the state of the device.  Dismissed upon completion of
     * READYing process
     */
    void setPukEntryProgressDialog(ProgressDialog dialog) {
        mPUKEntryProgressDialog = dialog;
    }

    ProgressDialog getPUKEntryProgressDialog() {
        return mPUKEntryProgressDialog;
    }

    /**
     * Disables the status bar.  This is used by the phone app when in-call UI is active.
     *
     * Any call to this method MUST be followed (eventually)
     * by a corresponding reenableStatusBar() call.
     */
    /* package */ void disableStatusBar() {
        if (DBG) Log.d(LOG_TAG, "disable status bar");
        synchronized (this) {
            if (mStatusBarDisableCount++ == 0) {
               if (DBG)  Log.d(LOG_TAG, "StatusBarManager.DISABLE_EXPAND");
                mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
            }
        }
    }

// 2010.10.20 minji.bae@lge.com Keyguard(lockscreen) setting during Call, Emergency List, Emergency Dial [START_LGE_LAB1]
    /**
     * Disables the keyguard.  This is used by the phone app to allow
     * interaction with the Phone UI when the keyguard would otherwise be
     * active (like receiving an incoming call while the device is
     * locked.)
     *
     * Any call to this method MUST be followed (eventually)
     * by a corresponding reenableKeyguard() call.
     */
    /* package */ void disableKeyguard() {
        if (DBG) Log.d(LOG_TAG, "disable keyguard");
        // if (DBG) Log.d(LOG_TAG, "disableKeyguard()...", new Throwable("stack dump"));

// LGE_CHANGE_S [fred.lee@lge.com] 2010-04-22 : for reenable keyguard, mKeyguardLock must be disabled, if mKeyguardLock is not disabled & try to reenable keyguard, Phone app will be dead
        if (mIsDisabledKeyguardInCall == false) {
            if (DBG) Log.e(LOG_TAG, "disableKeyguard() : keyguard is disabled");
            mIsDisabledKeyguardInCall = true;
// LGE_CHANGE_E [fred.lee@lge.com] 2010-04-22 : for reenable keyguard, mKeyguardLock must be disabled, if mKeyguardLock is not disabled & try to reenable keyguard, Phone app will be dead

            mKeyguardLock.disableKeyguard();
        }
    }
    /**
     * Re-enables the keyguard after a previous disableKeyguard() call.
     *
     * Any call to this method MUST correspond to (i.e. be balanced with)
     * a previous disableKeyguard() call.
     */
    /* package */ void reenableKeyguard() {
        if (DBG) Log.d(LOG_TAG, "re-enable keyguard");
        // if (DBG) Log.d(LOG_TAG, "reenableKeyguard()...", new Throwable("stack dump"));

// LGE_CHANGE_S [fred.lee@lge.com] 2010-04-22 : for reenable keyguard, mKeyguardLock must be disabled, if mKeyguardLock is not disabled & try to reenable keyguard, Phone app will be dead
        if (mIsDisabledKeyguardInCall == true) {
            if (DBG) Log.e(LOG_TAG, "reenableKeyguard() : keyguard is reenabled");
            mIsDisabledKeyguardInCall = false;
// LGE_CHANGE_E [fred.lee@lge.com] 2010-04-22 : for reenable keyguard, mKeyguardLock must be disabled, if mKeyguardLock is not disabled & try to reenable keyguard, Phone app will be dead

            mKeyguardLock.reenableKeyguard();
        }
    }
// 2010.10.20 minji.bae@lge.com Keyguard(lockscreen) setting during Call, Emergency List, Emergency Dial [END_LGE_LAB1]

    /**
     * Re-enables the status bar after a previous disableStatusBar() call.
     *
     * Any call to this method MUST correspond to (i.e. be balanced with)
     * a previous disableStatusBar() call.
     */
    /* package */ void reenableStatusBar() {
        if (DBG) Log.d(LOG_TAG, "re-enable status bar");
        synchronized (this) {
            if (mStatusBarDisableCount > 0) {
                if (--mStatusBarDisableCount == 0) {
                    if (DBG) Log.d(LOG_TAG, "StatusBarManager.DISABLE_NONE");
                    mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
                }
            } else {
                Log.e(LOG_TAG, "mStatusBarDisableCount is already zero");
            }
        }
    }

    /**
     * Controls how quickly the screen times out.
     *
     * The poke lock controls how long it takes before the screen powers
     * down, and therefore has no immediate effect when the current
     * WakeState (see {@link PhoneApp#requestWakeState}) is FULL.
     * If we're in a state where the screen *is* allowed to turn off,
     * though, the poke lock will determine the timeout interval (long or
     * short).
     *
     * @param shortPokeLock tells the device the timeout duration to use
     * before going to sleep
     * {@link com.android.server.PowerManagerService#SHORT_KEYLIGHT_DELAY}.
     */
    /* package */ void setScreenTimeout(ScreenTimeoutDuration duration) {
        if (VDBG) Log.d(LOG_TAG, "setScreenTimeout(" + duration + ")...");

        // make sure we don't set the poke lock repeatedly so that we
        // avoid triggering the userActivity calls in
        // PowerManagerService.setPokeLock().
        if (duration == mScreenTimeoutDuration) {
            return;
        }
        // stick with default timeout if we are using the proximity sensor
        if (proximitySensorModeEnabled()) {
            return;
        }
        mScreenTimeoutDuration = duration;
        updatePokeLock();
    }

    /**
     * Update the state of the poke lock held by the phone app,
     * based on the current desired screen timeout and the
     * current "ignore user activity on touch" flag.
     */
    private void updatePokeLock() {
        // This is kind of convoluted, but the basic thing to remember is
        // that the poke lock just sends a message to the screen to tell
        // it to stay on for a while.
        // The default is 0, for a long timeout and should be set that way
        // when we are heading back into a the keyguard / screen off
        // state, and also when we're trying to keep the screen alive
        // while ringing.  We'll also want to ignore the cheek events
        // regardless of the timeout duration.
        // The short timeout is really used whenever we want to give up
        // the screen lock, such as when we're in call.
        int pokeLockSetting = LocalPowerManager.POKE_LOCK_IGNORE_CHEEK_EVENTS;
        switch (mScreenTimeoutDuration) {
            case SHORT:
                // Set the poke lock to timeout the display after a short
                // timeout (5s). This ensures that the screen goes to sleep
                // as soon as acceptably possible after we the wake lock
                // has been released.
                pokeLockSetting |= LocalPowerManager.POKE_LOCK_SHORT_TIMEOUT;
                break;

            case MEDIUM:
                // Set the poke lock to timeout the display after a medium
                // timeout (15s). This ensures that the screen goes to sleep
                // as soon as acceptably possible after we the wake lock
                // has been released.
                pokeLockSetting |= LocalPowerManager.POKE_LOCK_MEDIUM_TIMEOUT;
                break;

            case DEFAULT:
            default:
                // set the poke lock to timeout the display after a long
                // delay by default.
                // TODO: it may be nice to be able to disable cheek presses
                // for long poke locks (emergency dialer, for instance).
                break;
        }

        if (mIgnoreTouchUserActivity) {
            pokeLockSetting |= LocalPowerManager.POKE_LOCK_IGNORE_TOUCH_AND_CHEEK_EVENTS;
        }

        // Send the request
        try {
            mPowerManagerService.setPokeLock(pokeLockSetting, mPokeLockToken, LOG_TAG);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "mPowerManagerService.setPokeLock() failed: " + e);
        }
    }

    /**
     * Controls whether or not the screen is allowed to sleep.
     *
     * Once sleep is allowed (WakeState is SLEEP), it will rely on the
     * settings for the poke lock to determine when to timeout and let
     * the device sleep {@link PhoneApp#setScreenTimeout}.
     *
     * @param ws tells the device to how to wake.
     */
    /* package */ void requestWakeState(WakeState ws) {
        if (VDBG) Log.d(LOG_TAG, "requestWakeState(" + ws + ")...");
        synchronized (this) {
            if (mWakeState != ws) {
                switch (ws) {
                    case PARTIAL:
                        // acquire the processor wake lock, and release the FULL
                        // lock if it is being held.
                        mPartialWakeLock.acquire();
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        break;
                    case FULL:
                        // acquire the full wake lock, and release the PARTIAL
                        // lock if it is being held.
                        mWakeLock.acquire();
                        if (mPartialWakeLock.isHeld()) {
                            mPartialWakeLock.release();
                        }
                        break;
                    case SLEEP:
                    default:
                        // release both the PARTIAL and FULL locks.
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        if (mPartialWakeLock.isHeld()) {
                            mPartialWakeLock.release();
                        }
                        break;
                }
                mWakeState = ws;
            }
        }
    }

    /**
     * If we are not currently keeping the screen on, then poke the power
     * manager to wake up the screen for the user activity timeout duration.
     */
    /* package */ void wakeUpScreen() {
        synchronized (this) {
            if (mWakeState == WakeState.SLEEP) {
                if (DBG) Log.d(LOG_TAG, "pulse screen lock");
                try {
                    mPowerManagerService.userActivityWithForce(SystemClock.uptimeMillis(), false, true);
                } catch (RemoteException ex) {
                    // Ignore -- the system process is dead.
                }
            }
        }
    }

    /**
     * Sets the wake state and screen timeout based on the current state
     * of the phone, and the current state of the in-call UI.
     *
     * This method is a "UI Policy" wrapper around
     * {@link PhoneApp#requestWakeState} and {@link PhoneApp#setScreenTimeout}.
     *
     * It's safe to call this method regardless of the state of the Phone
     * (e.g. whether or not it's idle), and regardless of the state of the
     * Phone UI (e.g. whether or not the InCallScreen is active.)
     */
    
    void updateWakeState() {
    	updateWakeState(false);
    }
    
    /* package */ void updateWakeState(boolean isImmediatelyON) {
        Phone.State state = phone.getState();
        //Call fgCall = phone.getForegroundCall();

        // True if the in-call UI is the foreground activity.
        // (Note this will be false if the screen is currently off,
        // since in that case *no* activity is in the foreground.)
        boolean isShowingCallScreen = isShowingCallScreen();
        //boolean isShowingCallScreen = false;
        //if(fgCall.getState() != Call.State.IDLE)
        //	isShowingCallScreen = true;
        

        // True if the InCallScreen's DTMF dialer is currently opened.
        // (Note this does NOT imply whether or not the InCallScreen
        // itself is visible.)
        boolean isDialerOpened = (mInCallScreen != null) && mInCallScreen.isDialerOpened();

        // True if the speakerphone is in use.  (If so, we *always* use
        // the default timeout.  Since the user is obviously not holding
        // the phone up to his/her face, we don't need to worry about
        // false touches, and thus don't need to turn the screen off so
        // aggressively.)
        // Note that we need to make a fresh call to this method any
        // time the speaker state changes.  (That happens in
        // PhoneUtils.turnOnSpeaker().)
        boolean isSpeakerInUse = (state == Phone.State.OFFHOOK) && PhoneUtils.isSpeakerOn(this);

        // TODO (bug 1440854): The screen timeout *might* also need to
        // depend on the bluetooth state, but this isn't as clear-cut as
        // the speaker state (since while using BT it's common for the
        // user to put the phone straight into a pocket, in which case the
        // timeout should probably still be short.)

        if (DBG) Log.d(LOG_TAG, "updateWakeState: callscreen " + isShowingCallScreen
                       + ", dialer " + isDialerOpened
                       + ", speaker " + isSpeakerInUse + "...");

        //
        // (1) Set the screen timeout.
        //
        // Note that the "screen timeout" value we determine here is
        // meaningless if the screen is forced on (see (2) below.)
        //
        if (!isShowingCallScreen || isSpeakerInUse) {
            // Use the system-wide default timeout.
            setScreenTimeout(ScreenTimeoutDuration.DEFAULT);
        } else {
            // We're on the in-call screen, and *not* using the speakerphone.
            if (isDialerOpened) {
                // The DTMF dialpad is up.  This case is special because
                // the in-call UI has its own "touch lock" mechanism to
                // disable the dialpad after a very short amount of idle
                // time (to avoid false touches from the user's face while
                // in-call.)
                //
                // In this case the *physical* screen just uses the
                // system-wide default timeout.
                setScreenTimeout(ScreenTimeoutDuration.DEFAULT);
            } else {
                // We're on the in-call screen, and not using the DTMF dialpad.
                // There's actually no touchable UI onscreen at all in
                // this state.  Also, the user is (most likely) not
                // looking at the screen at all, since they're probably
                // holding the phone up to their face.  Here we use a
                // special screen timeout value specific to the in-call
                // screen, purely to save battery life.
                setScreenTimeout(ScreenTimeoutDuration.MEDIUM);
            }
        }

        //
        // (2) Decide whether to force the screen on or not.
        //
        // Force the screen to be on if the phone is ringing or dialing,
        // or if we're displaying the "Call ended" UI for a connection in
        // the "disconnected" state.
        //
        boolean isRinging = (state == Phone.State.RINGING);
        boolean isDialing = (phone.getForegroundCall().getState() == Call.State.DIALING);
        boolean showingDisconnectedConnection =
                PhoneUtils.hasDisconnectedConnections(phone) && isShowingCallScreen;
        boolean keepScreenOn = isRinging || isDialing || showingDisconnectedConnection || isImmediatelyON; //hyojin.an 101026
        if (DBG) Log.d(LOG_TAG, "updateWakeState: keepScreenOn = " + keepScreenOn
                       + " (isRinging " + isRinging
                       + ", isDialing " + isDialing
                       + ", showingDisc " + showingDisconnectedConnection + ")");

        if((mIsVideoCall) && ((phone.getForegroundCall().getState() == Call.State.ACTIVE) || (phone.getForegroundCall().getState() == Call.State.ALERTING)))
        	keepScreenOn = true;
	   
        // keepScreenOn == true means we'll hold a full wake lock:
        requestWakeState(keepScreenOn ? WakeState.FULL : WakeState.SLEEP);
    }

    /**
     * Wrapper around the PowerManagerService.preventScreenOn() API.
     * This allows the in-call UI to prevent the screen from turning on
     * even if a subsequent call to updateWakeState() causes us to acquire
     * a full wake lock.
     */
    /* package */ void preventScreenOn(boolean prevent) {
        if (VDBG) Log.d(LOG_TAG, "- preventScreenOn(" + prevent + ")...");
        try {
            mPowerManagerService.preventScreenOn(prevent);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "mPowerManagerService.preventScreenOn() failed: " + e);
        }
    }

    /**
     * Sets or clears the flag that tells the PowerManager that touch
     * (and cheek) events should NOT be considered "user activity".
     *
     * Since the in-call UI is totally insensitive to touch in most
     * states, we set this flag whenever the InCallScreen is in the
     * foreground.  (Otherwise, repeated unintentional touches could
     * prevent the device from going to sleep.)
     *
     * There *are* some some touch events that really do count as user
     * activity, though.  For those, we need to manually poke the
     * PowerManager's userActivity method; see pokeUserActivity().
     */
    /* package */ void setIgnoreTouchUserActivity(boolean ignore) {
        if (VDBG) Log.d(LOG_TAG, "setIgnoreTouchUserActivity(" + ignore + ")...");
        mIgnoreTouchUserActivity = ignore;
        updatePokeLock();
    }

    /**
     * Manually pokes the PowerManager's userActivity method.  Since we
     * hold the POKE_LOCK_IGNORE_TOUCH_AND_CHEEK_EVENTS poke lock while
     * the InCallScreen is active, we need to do this for touch events
     * that really do count as user activity (like DTMF key presses, or
     * unlocking the "touch lock" overlay.)
     */
    /* package */ void pokeUserActivity() {
        if (VDBG) Log.d(LOG_TAG, "pokeUserActivity()...");
        try {
            mPowerManagerService.userActivity(SystemClock.uptimeMillis(), false);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "mPowerManagerService.userActivity() failed: " + e);
        }
    }

    /**
     * Set when a new outgoing call is beginning, so we can update
     * the proximity sensor state.
     * Cleared when the InCallScreen is no longer in the foreground,
     * in case the call fails without changing the telephony state.
     */
    /* package */ void setBeginningCall(boolean beginning) {
        // Note that we are beginning a new call, for proximity sensor support
        mBeginningCall = beginning;
        // Update the Proximity sensor based on mBeginningCall state
        updateProximitySensorMode(phone.getState());
    }

    /**
     * Updates the wake lock used to control proximity sensor behavior,
     * based on the current state of the phone.  This method is called
     * from the CallNotifier on any phone state change.
     *
     * On devices that have a proximity sensor, to avoid false touches
     * during a call, we hold a PROXIMITY_SCREEN_OFF_WAKE_LOCK wake lock
     * whenever the phone is off hook.  (When held, that wake lock causes
     * the screen to turn off automatically when the sensor detects an
     * object close to the screen.)
     *
     * This method is a no-op for devices that don't have a proximity
     * sensor.
     *
     * Note this method doesn't care if the InCallScreen is the foreground
     * activity or not.  That's because we want the proximity sensor to be
     * enabled any time the phone is in use, to avoid false cheek events
     * for whatever app you happen to be running.
     *
     * Proximity wake lock will *not* be held if any one of the
     * conditions is true while on a call:
     * 1) If the audio is routed via Bluetooth
     * 2) If a wired headset is connected
     * 3) if the speaker is ON
     * 4) If the slider is open(i.e. the hardkeyboard is *not* hidden)
     *
     * @param state current state of the phone (see {@link Phone#State})
     */
    /* package */ void updateProximitySensorMode(Phone.State state) {
        if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: state = " + state);

        if (proximitySensorModeEnabled()) {
            synchronized (mProximityWakeLock) {

            	//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
            	//START jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
            	if (!isProximitySensorSetting()) {
            		if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: proximity setting is false...");
            		if (mProximityWakeLock.isHeld()) {
            			if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: releasing...");
            			mProximityWakeLock.release(0);
            		}
            		return;
            	}
            	//END jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
            	//20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]
            	
                // turn proximity sensor off and turn screen on immediately if
                // we are using a headset, the keyboard is open, or the device
                // is being held in a horizontal position.
                boolean screenOnImmediately = (isHeadsetPlugged()
                            || PhoneUtils.isSpeakerOn(this)
                            || ((mBtHandsfree != null) && mBtHandsfree.isAudioOn())
                            || mIsHardKeyboardOpen
                            || mIsVideoCall
                            //|| mOrientation == AccelerometerListener.ORIENTATION_HORIZONTAL
                            );

                if (((state == Phone.State.OFFHOOK) || mBeginningCall) && !screenOnImmediately) {
                    // Phone is in use!  Arrange for the screen to turn off
                    // automatically when the sensor detects a close object.
                    if (!mProximityWakeLock.isHeld()) {
                        if (DBG) Log.d(LOG_TAG, "updateProximitySensorMode: acquiring...");
                        mProximityWakeLock.acquire();
                    } else {
                        if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: lock already held.");
                    }
                } else {
                    // Phone is either idle, or ringing.  We don't want any
                    // special proximity sensor behavior in either case.
                    if (mProximityWakeLock.isHeld()) {
                        if (DBG) Log.d(LOG_TAG, "updateProximitySensorMode: releasing...");
                        // Wait until user has moved the phone away from his head if we are
                        // releasing due to the phone call ending.
                        // Qtherwise, turn screen on immediately
                        int flags =
                            (screenOnImmediately ? 0 : PowerManager.WAIT_FOR_PROXIMITY_NEGATIVE);
                        mProximityWakeLock.release(flags);
                    } else {
                        if (VDBG) {
                            Log.d(LOG_TAG, "updateProximitySensorMode: lock already released.");
                        }
                    }
                }
            }
        }
    }

    public void orientationChanged(int orientation) {
        mOrientation = orientation;
        updateProximitySensorMode(phone.getState());
    }
  
    //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [START_LGE_LAB1]
    //START jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
    //ProximitySensor On : TDMB play -> In call -> End call -> Lock on
    //ProximitySensor Off : TDMB play -> In call -> End call -> TDMB replay
      /* package */ void checkProximitySensorSetting() {
    	//int mProximitySetting = Settings.System.getInt(getContentResolver(), Settings.System.SENSOR_PROXMITY, 1);
    	  int mProximitySetting = 1;
    	  if(mProximitySetting == 1) {
    		  isProximitySensorSetting = true;
    	  } else {
    		  isProximitySensorSetting = false;
    	  }
    	  if (VDBG) Log.d(LOG_TAG, "checkProximitySensorSetting() : isProximitySensorSetting = " + isProximitySensorSetting);
      }
      /* package */ boolean isProximitySensorSetting() {
    	  if (VDBG) Log.d(LOG_TAG, "isProximitySensorSetting() : isProximitySensorSetting = " + isProximitySensorSetting);
    	  return isProximitySensorSetting;
      }
      //END jahyun.park@lge.com 2010.08.18 TD : 27782  TDMB play -> In call -> sleep -> call end -> TDMB resume
      //20101017 sumi920.kim@lge.com In Call -> LCD off -> call end -> LCD On  [END_LGE_LAB1]

    /**
     * Notifies the phone app when the phone state changes.
     * Currently used only for proximity sensor support.
     */
    /* package */ void updatePhoneState(Phone.State state) {
        if (state != mLastPhoneState) {
            mLastPhoneState = state;
            updateProximitySensorMode(state);

                if ((mIsVideoCall == false) && (mAccelerometerListener != null)) {
                    // use accelerometer to augment proximity sensor when in call
                    mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;
                //20101104 sumi920.kim@lge.com until Accelerometer fixed
                //mAccelerometerListener.enable(state == Phone.State.OFFHOOK);
            }

            // clear our beginning call flag
            mBeginningCall = false;
            // While we are in call, the in-call screen should dismiss the keyguard.
            // This allows the user to press Home to go directly home without going through
            // an insecure lock screen.
            // But we do not want to do this if there is no active call so we do not
            // bypass the keyguard if the call is not answered or declined.
            if (mInCallScreen != null) {
                mInCallScreen.updateKeyguardPolicy(state == Phone.State.OFFHOOK);
            }
        }
    }

    /* package */ Phone.State getPhoneState() {
        return mLastPhoneState;
    }

    /**
     * @return true if this device supports the "proximity sensor
     * auto-lock" feature while in-call (see updateProximitySensorMode()).
     */
    /* package */ boolean proximitySensorModeEnabled() {
        return (mProximityWakeLock != null);
    }

    KeyguardManager getKeyguardManager() {
        return mKeyguardManager;
    }

    private void onMMIComplete(AsyncResult r) {
        if (VDBG) Log.d(LOG_TAG, "onMMIComplete()...");
        MmiCode mmiCode = (MmiCode) r.result;
        PhoneUtils.displayMMIComplete(phone, getInstance(), mmiCode, null, null);
    }

    private void initForNewRadioTechnology() {
        if (DBG) Log.d(LOG_TAG, "initForNewRadioTechnology...");

        if (phone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
            // Create an instance of CdmaPhoneCallState and initialize it to IDLE
            cdmaPhoneCallState = new CdmaPhoneCallState();
            cdmaPhoneCallState.CdmaPhoneCallStateInit();

            //create instances of CDMA OTA data classes
            if (cdmaOtaProvisionData == null) {
                cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
            }
            if (cdmaOtaConfigData == null) {
                cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
            }
            if (cdmaOtaScreenState == null) {
                cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
            }
            if (cdmaOtaInCallScreenUiState == null) {
                cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();
            }
        }

        ringer.updateRingerContextAfterRadioTechnologyChange(this.phone);
        notifier.updateCallNotifierRegistrationsAfterRadioTechnologyChange();
        if (mBtHandsfree != null) {
            mBtHandsfree.updateBtHandsfreeAfterRadioTechnologyChange();
        }
        if (mInCallScreen != null) {
            mInCallScreen.updateAfterRadioTechnologyChange();
        }

        // Update registration for ICC status after radio technology change
        IccCard sim = phone.getIccCard();
        if (sim != null) {
            if (DBG) Log.d(LOG_TAG, "Update registration for ICC status...");

            //Register all events new to the new active phone
            sim.registerForNetworkLocked(mHandler, EVENT_SIM_NETWORK_LOCKED, null);
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
            sim.registerForAbsent(mHandler, EVENT_SIM_ABSENT, null);
            sim.registerForLocked(mHandler, EVENT_SIM_LOCKED, null);
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]
        }
    }


    /**
     * @return true if a wired headset is currently plugged in.
     *
     * @see Intent.ACTION_HEADSET_PLUG (which we listen for in mReceiver.onReceive())
     */
    boolean isHeadsetPlugged() {
        return mIsHeadsetPlugged;
    }

    /**
     * @return true if the onscreen UI should currently be showing the
     * special "bluetooth is active" indication in a couple of places (in
     * which UI elements turn blue and/or show the bluetooth logo.)
     *
     * This depends on the BluetoothHeadset state *and* the current
     * telephony state; see shouldShowBluetoothIndication().
     *
     * @see CallCard
     * @see NotificationMgr.updateInCallNotification
     */
    /* package */ boolean showBluetoothIndication() {
        return mShowBluetoothIndication;
    }

    /**
     * Recomputes the mShowBluetoothIndication flag based on the current
     * bluetooth state and current telephony state.
     *
     * This needs to be called any time the bluetooth headset state or the
     * telephony state changes.
     *
     * @param forceUiUpdate if true, force the UI elements that care
     *                      about this flag to update themselves.
     */
    /* package */ void updateBluetoothIndication(boolean forceUiUpdate) {
        mShowBluetoothIndication = shouldShowBluetoothIndication(mBluetoothHeadsetState,
                                                                 mBluetoothHeadsetAudioState,
                                                                 phone);
        if (forceUiUpdate) {
            // Post Handler messages to the various components that might
            // need to be refreshed based on the new state.
            if (isShowingCallScreen()) mInCallScreen.requestUpdateBluetoothIndication();
            mHandler.sendEmptyMessage(EVENT_UPDATE_INCALL_NOTIFICATION);
        }

        // Update the Proximity sensor based on Bluetooth audio state
        updateProximitySensorMode(phone.getState());
    }

    /**
     * UI policy helper function for the couple of places in the UI that
     * have some way of indicating that "bluetooth is in use."
     *
     * @return true if the onscreen UI should indicate that "bluetooth is in use",
     *         based on the specified bluetooth headset state, and the
     *         current state of the phone.
     * @see showBluetoothIndication()
     */
    private static boolean shouldShowBluetoothIndication(int bluetoothState,
                                                         int bluetoothAudioState,
                                                         Phone phone) {
        // We want the UI to indicate that "bluetooth is in use" in two
        // slightly different cases:
        //
        // (a) The obvious case: if a bluetooth headset is currently in
        //     use for an ongoing call.
        //
        // (b) The not-so-obvious case: if an incoming call is ringing,
        //     and we expect that audio *will* be routed to a bluetooth
        //     headset once the call is answered.

        switch (phone.getState()) {
            case OFFHOOK:
                // This covers normal active calls, and also the case if
                // the foreground call is DIALING or ALERTING.  In this
                // case, bluetooth is considered "active" if a headset
                // is connected *and* audio is being routed to it.
                return ((bluetoothState == BluetoothHeadset.STATE_CONNECTED)
                        && (bluetoothAudioState == BluetoothHeadset.AUDIO_STATE_CONNECTED));

            case RINGING:
                // If an incoming call is ringing, we're *not* yet routing
                // audio to the headset (since there's no in-call audio
                // yet!)  In this case, if a bluetooth headset is
                // connected at all, we assume that it'll become active
                // once the user answers the phone.
                return (bluetoothState == BluetoothHeadset.STATE_CONNECTED);

            default:  // Presumably IDLE
                return false;
        }
    }

//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
    //LGE_CHANGE_S [yongjin.her@lge.com] 2010/04/26 : OTA reset popup after the call end, if phone receives OTA reset event during the call. only KT
        /* package */ void sendPhoneResetEvent() {
            mHandler.sendEmptyMessage(EVENT_PHONE_RESET);
        }
    //LGE_CHANGE_E [yongjin.her@lge.com] 2010/04/26 : OTA reset popup after the call end, if phone receives OTA reset event during the call. only KT
    //LGE_CHANGE_S [yongjin.her@lge.com] 2010/08/11 : KT auto reset with the Toast Message for USIM REFRESH event to meet KT UI Scenario Spec
        /* package */ void sendDelayedPhoneResetEvent() {
            mHandler.sendEmptyMessageDelayed(EVENT_PHONE_RESET, RESET_DELAY_TIME);
        }
    //LGE_CHANGE_E [yongjin.her@lge.com] 2010/08/11 : KT auto reset with the Toast Message for USIM REFRESH event to meet KT UI Scenario Spec
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]


    /**
     * Receiver for misc intent broadcasts the Phone app cares about.
     */
    private class PhoneAppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean enabled = System.getInt(getContentResolver(),
                        System.AIRPLANE_MODE_ON, 0) == 0;
                phone.setRadioPower(enabled);
            } else if (action.equals(BluetoothHeadset.ACTION_STATE_CHANGED)) {
                mBluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                                            BluetoothHeadset.STATE_ERROR);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetState);
                updateBluetoothIndication(true);  // Also update any visible UI if necessary
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                mBluetoothHeadsetAudioState =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_AUDIO_STATE,
                                           BluetoothHeadset.STATE_ERROR);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_AUDIO_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetAudioState);
                updateBluetoothIndication(true);  // Also update any visible UI if necessary
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_ANY_DATA_CONNECTION_STATE_CHANGED");
                if (VDBG) Log.d(LOG_TAG, "- state: " + intent.getStringExtra(Phone.STATE_KEY));
                if (VDBG) Log.d(LOG_TAG, "- reason: "
                                + intent.getStringExtra(Phone.STATE_CHANGE_REASON_KEY));

                // The "data disconnected due to roaming" notification is
                // visible if you've lost data connectivity because you're
                // roaming and you have the "data roaming" feature turned off.
                boolean disconnectedDueToRoaming = false;
                if ("DISCONNECTED".equals(intent.getStringExtra(Phone.STATE_KEY))) {
                    String reason = intent.getStringExtra(Phone.STATE_CHANGE_REASON_KEY);
                    if (Phone.REASON_ROAMING_ON.equals(reason)) {
                        // We just lost our data connection, and the reason
                        // is that we started roaming.  This implies that
                        // the user has data roaming turned off.
                        disconnectedDueToRoaming = true;
                    }

					// LGE_CHANGE_S, [DATA_LAB1_COMMON_FROYO_010][LGE_DATA][jaeok.bae@lge.com], 2010-10-01, WCDMA Roaming.. Add check condition of roaming check db
					if(StarConfig.COUNTRY.equals("KR")) 
					{
						int dslg_data_roaming = Settings.Secure.getInt(getContentResolver(), Settings.Secure.DATA_ROAMING, 0);
						if( reason == null  && dslg_data_roaming == 0 && phone.getServiceState().getRoaming() == true) {
							  disconnectedDueToRoaming = true;
							  if (VDBG) Log.d(LOG_TAG, "[LGE_DATA] DATA_ROAMING = " + dslg_data_roaming);
							  if (VDBG) Log.d(LOG_TAG, "[LGE_DATA] disconnectedDueToRoaming = " + disconnectedDueToRoaming);
						}
					}	
					// LGE_CHANGE_E, [DATA_LAB1_COMMON_FROYO_010][LGE_DATA][jaeok.bae@lge.com], 2010-10-01, WCDMA Roaming.. Add check condition of roaming check db
                }
                mHandler.sendEmptyMessage(disconnectedDueToRoaming
                                          ? EVENT_DATA_ROAMING_DISCONNECTED
                                          : EVENT_DATA_ROAMING_OK);
            } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_HEADSET_PLUG");
                if (VDBG) Log.d(LOG_TAG, "    state: " + intent.getIntExtra("state", 0));
                if (VDBG) Log.d(LOG_TAG, "    name: " + intent.getStringExtra("name"));
                mIsHeadsetPlugged = (intent.getIntExtra("state", 0) == 1);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_WIRED_HEADSET_PLUG, 0));
            } else if (action.equals(Intent.ACTION_BATTERY_LOW)) {
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_BATTERY_LOW");
                notifier.sendBatteryLow();  // Play a warning tone if in-call
            } else if ((action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) &&
                    (mPUKEntryActivity != null)) {
                // if an attempt to un-PUK-lock the device was made, while we're
                // receiving this state change notification, notify the handler.
                // NOTE: This is ONLY triggered if an attempt to un-PUK-lock has
                // been attempted.
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SIM_STATE_CHANGED,
                        intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE)));
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                String newPhone = intent.getStringExtra(Phone.PHONE_NAME_KEY);
                Log.d(LOG_TAG, "Radio technology switched. Now " + newPhone + " is active.");
                initForNewRadioTechnology();
            } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                handleServiceStateChanged(intent);
            } else if (action.equals(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {
                if (phone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
                    Log.d(LOG_TAG, "Emergency Callback Mode arrived in PhoneApp.");
                    // Start Emergency Callback Mode service
                    if (intent.getBooleanExtra("phoneinECMState", false)) {
                        context.startService(new Intent(context,
                                EmergencyCallbackModeService.class));
                    }
                } else {
                    Log.e(LOG_TAG, "Error! Emergency Callback Mode not supported for " +
                            phone.getPhoneName() + " phones");
                }
            } else if (action.equals(Intent.ACTION_DOCK_EVENT)) {
                mDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                        Intent.EXTRA_DOCK_STATE_UNDOCKED);
                if (VDBG) Log.d(LOG_TAG, "ACTION_DOCK_EVENT -> mDockState = " + mDockState);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_DOCK_STATE_CHANGED, 0));
            } else if (action.equals(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION)) {
                mPreferredTtyMode = intent.getIntExtra(TtyIntent.TTY_PREFFERED_MODE,
                                                       Phone.TTY_MODE_OFF);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: TTY_PREFERRED_MODE_CHANGE_ACTION");
                if (VDBG) Log.d(LOG_TAG, "    mode: " + mPreferredTtyMode);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
            } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,
                        AudioManager.RINGER_MODE_NORMAL);
                if(ringerMode == AudioManager.RINGER_MODE_SILENT) {
                    notifier.silenceRinger();
                }
            }
            //20101024 sumi920.kim@lge.com porting [START_LGE_LAB1]
            //<!--[yujung.lee@lge.com] 2010.10.20	  LAB1_CallUI shutdown happen incomming bell around [START] -->
            else if (action.equals(Intent.ACTION_REQUEST_SHUTDOWN)) {
            	Phone.State phoneState = phone.getState();
            	Log.d(LOG_TAG, "[PhoneApp.java] PhoneAppBroadcastReceiver::onReceive()... ACTION_REQUEST_SHUTDOWN, phoneState = " + phoneState);
            	if (phoneState == Phone.State.RINGING) {
            		notifier.silenceRinger();
            	}
            }
            //<!--[yujung.lee@lge.com] 2010.10.20	  LAB1_CallUI shutdown happen incomming bell around [END] -->
            //20101024 sumi920.kim@lge.com porting [END_LGE_LAB1]
            //20101113 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
            //START jiyoung.yoon@lge.com SKT DM TEST
            else if (action.equals(SKT_DM_INCOMCALL)) {
            	if (VDBG) Log.d(LOG_TAG, "Phone receive action type :SKT_DM_INCOMCALL");
            	if (VDBG) Log.i(LOG_TAG, "[SKT_DM_INCOMCALL] 1");
            	
                //Intent intent = getIntent();
            	String filename = intent.getStringExtra(SKT_DM_VOICEREC);
            	
                PhoneUtils.answerCall(phone);
                if (VDBG) Log.i(LOG_TAG, "[SKT_DM_INCOMCALL] 2");
                if (filename == null) {
                	filename = "";
                }
				/*
                else {
                	Intent intentrecord = new Intent();
                	intentrecord.setClassName("com.android.soundrecorder", "com.android.soundrecorder.SoundRecorder");
                	intentrecord.putExtra( "SKTDM_record", filename);
                	if (VDBG) Log.d(LOG_TAG, "[SKTDM_record] Rec filename : "+filename);
                	intentrecord.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                	startActivity(intentrecord);
                	if (VDBG) Log.i(LOG_TAG, "[SKT_DM_INCOMCALL] 3");
                }
                */
            }
            else if (action.equals(SKT_DM_ENDCALL)) {
            	if (VDBG) Log.d(LOG_TAG, "Phone receive action type :SKT_DM_ENDCALL");
            	PhoneUtils.hangup(phone);
            }
           	//END jiyoung.yoon@lge.com SKT DM TEST
            //20101113 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
//LGE_CHANGE_S [yongjin.her@lge.com] 2010/04/26 : OTA reset popup
            else if (action.equals(ACTION_OTA_USIM_REFRESH_TO_RESET)) {
                Log.d(LOG_TAG, "ACTION_OTA_USIM_REFRESH_TO_RESET");

                if(StarConfig.COUNTRY.equals("KR")) {

                    ActivityManager a = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    
                    List<RunningTaskInfo> info = a.getRunningTasks(1);


                    if((StarConfig.OPERATOR.equals("SKT"))
                        && ("com.lge.ota.SKTUsimDownloadActivity").equals(info.get(0).topActivity.getClassName())  ) {
                        //pass
                        //=> in this case, OTA app will do operations.
                        Log.d(LOG_TAG, "pass SKT OTA in PhoneApp......");
                    }
                    else
                    /*
                    if (("com.lge.ota.KTRegiActivity").equals(info.get(0).topActivity.getClassName())) {
                    */
                    if ((StarConfig.OPERATOR.equals("KT") )
                        && ("com.lge.ota.KTRegiActivity").equals(info.get(0).topActivity.getClassName())  ) {
                        //pass
                        //=> in this case, OTA app will do operations.
                        Log.d(LOG_TAG, "pass KT OTA in PhoneApp......");
                    } else {
                        //
                        //Popup & Reset
                        //
                        if (StarConfig.OPERATOR.equals("KT") && phone.getState()!=Phone.State.IDLE) {
                            //NOT Support KT in this model
                            /*
                            //Reset after the Call End
                            notifier.sendPhoneResetAfterCallEnd();
                            */
                        }
    //LGE_CHANGE_S [yongjin.her@lge.com] 2010/08/11 : KT auto reset with the Toast Message for USIM REFRESH event to meet KT UI Scenario Spec
                        else if(StarConfig.OPERATOR.equals("KT") ) 
                        {
                            //NOT Support KT in this model
                            /*
                            Toast.makeText(getApplicationContext(), 
                                com.lge.internal.R.string.kt_regi_wtoa_cardregi, 
                                Toast.LENGTH_LONG).show();
                            
                            sendDelayedPhoneResetEvent();
                            */
                        }
    //LGE_CHANGE_E [yongjin.her@lge.com] 2010/08/11 : KT auto reset with the Toast Message for USIM REFRESH event to meet KT UI Scenario Spec
                        else 
                        {
                            showDialog(context, 
                                        com.lge.internal.R.string.kt_regi_wtoa_cardregi, 
                                        new DialogInterface.OnClickListener() {
                        					public void onClick(DialogInterface arg0, int arg1) {
    //LGE_CHANGE_S [yongjin.her@lge.com] 2010/04/26 : OTA reset popup after the call end, if phone receives OTA reset event during the call. only KT
                                                if (StarConfig.OPERATOR.equals("SKT")) {
                                                    //reset
                                                    /*  //replaced
                                                    phone.resetPhone(mHandler.obtainMessage(EVENT_PHONE_RESET_COMPLETE));
                                                    */
                                                    mHandler.sendEmptyMessage(EVENT_PHONE_RESET);
                                                } else if (StarConfig.OPERATOR.equals("KT")) {
                                                    //NOT Support KT in this model
                                                     /*
                                                    //Reset
                                                    mHandler.sendEmptyMessage(EVENT_PHONE_RESET);
                                                    */
                                                } else {
                                                    Log.d(LOG_TAG, "error : unexpected ACTION_OTA_USIM_REFRESH_TO_RESET, no carrier info");
                                                }
    //LGE_CHANGE_E [yongjin.her@lge.com] 2010/04/26 : OTA reset popup after the call end, if phone receives OTA reset event during the call. only KT
                        					}
                        				},
                        				//keyListener
                        				OtaPopupKeyListener                        		    
                                );
                        }
                    }
                } //KR
            }
//LGE_CHANGE_E [yongjin.her@lge.com] 2010/04/26 : OTA reset popup
//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]
        }
    }

    /**
     * Broadcast receiver for the ACTION_MEDIA_BUTTON broadcast intent.
     *
     * This functionality isn't lumped in with the other intents in
     * PhoneAppBroadcastReceiver because we instantiate this as a totally
     * separate BroadcastReceiver instance, since we need to manually
     * adjust its IntentFilter's priority (to make sure we get these
     * intents *before* the media player.)
     */
    private class MediaButtonBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (VDBG) Log.d(LOG_TAG,
                           "MediaButtonBroadcastReceiver.onReceive()...  event = " + event);
            if ((event != null)
            		&& (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK)) {
            	//20101111 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
            	if(StarConfig.COUNTRY.equals("KR")){
            		//LGE_CHANGE_S heechul.hyun 2010-09-14 For after reject call(because mediabuttonintentreceiver use ACTION_DOWN)
            		if (VDBG) Log.d(LOG_TAG,
            				"KEYCODE_HEADSETHOOK.onReceive()...	event = " + event);
            		
            		if (mIsLongHookPress) {
            			abortBroadcast();
            			if (VDBG) Log.d(LOG_TAG, "abortBroadcast Hookkey");
            		}
            		if (event.getAction() == KeyEvent.ACTION_DOWN) {
            			if (VDBG) Log.d(LOG_TAG,"ACTION_DOWN");
            			
            			if (event.getRepeatCount() == 0) {
            				mHandler.removeCallbacks(mHookLongPress);
            				mHookPressed = true;
            				// [ LGE_CHANGE_S, [kelvin.lee], 2010-07-13, Change timeout value of HOOK Long Key
            				//mHandler.postDelayed(mHookLongPress,
            				//		  ViewConfiguration.getGlobalActionKeyTimeout());
            				mHandler.postDelayed(mHookLongPress, 1000);
            				// LGE_CHANGE_E, [kelvin.lee], 2010-07-13, Change timeout value of HOOK Long Key ]
            			}
            			if (phone.getState() != Phone.State.IDLE)
            				abortBroadcast();
            		} else if (event.getAction() == KeyEvent.ACTION_UP) {
            			//LGE_CHANGE_S heechul.hyun 2010-09-14 For after reject call(because mediabuttonintentreceiver use ACTION_DOWN)
            			if (mIsLongHookPress) {
            				mIsLongHookPress = false;
            				if (VDBG) Log.d(LOG_TAG, "Release mIsLongHookPress");
            			}
            			//LGE_CHANGE_E heechul.hyun 2010-09-14 For after reject call(because mediabuttonintentreceiver use ACTION_DOWN)
            			if (mHookPressed == true) {
            				mHandler.removeCallbacks(mHookLongPress);
            				mHookPressed = false;
            				if (phone.getState() != Phone.State.IDLE) {
            					boolean consumed = PhoneUtils.handleHeadsetHook(phone);
            					if (consumed) {
            						if (isShowingCallScreen()) {
            							updateInCallScreenTouchUi();
            						}
            					}
            					abortBroadcast();
            				}
            			}
            		}
            	}
            	else
           		// LGE_UPDATE_E sanghoon.roh@lge.com 2010/06/04 handle LG headsethook key scenario
            	//20101111 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]
            	{
            		if (VDBG) Log.d(LOG_TAG, "MediaButtonBroadcastReceiver: HEADSETHOOK");
            		boolean consumed = PhoneUtils.handleHeadsetHook(phone, event);
            		if (VDBG) Log.d(LOG_TAG, "==> handleHeadsetHook(): consumed = " + consumed);
            		if (consumed) {
            			// If a headset is attached and the press is consumed, also update
            			// any UI items (such as an InCallScreen mute button) that may need to
            			// be updated if their state changed.
            			if (isShowingCallScreen()) {
            				updateInCallScreenTouchUi();
            			}
            			abortBroadcast();
            		}
            	}
            }
            else {
            	if (phone.getState() != Phone.State.IDLE) {
            		// If the phone is anything other than completely idle,
            		// then we consume and ignore any media key events,
            		// Otherwise it is too easy to accidentally start
            		// playing music while a phone call is in progress.
            		if (VDBG) Log.d(LOG_TAG, "MediaButtonBroadcastReceiver: consumed");
            		abortBroadcast();
            	}
            }
        }
    }

    //20101111 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
    // LGE_UPDATE_S sanghoon.roh@lge.com 2010/06/04 handle LG headsethook key scenario
    boolean mHookPressed;
    Runnable mHookLongPress = new Runnable() {
    	public void run() {
    		if (mHookPressed) {
    			mHookPressed = false;
    			
    			if (VDBG) Log.d(LOG_TAG, "MediaButtonBroadcastReceiver: HEADSETHOOK long press!");
    			boolean consumed = false;
    			if (phone.getState() != Phone.State.IDLE) {
    				// LGE_UPDATE_S sanghoon.roh@lge.com 2010/06/21 handle LG headsethook key scenario
    				
    				consumed = PhoneUtils.handleHeadsetHookLong(phone);
    				//LGE_CHANGE_S heechul.hyun 2010-09-14 For after reject call(because mediabuttonintentreceiver use ACTION_DOWN)
    				mIsLongHookPress = true;
    				Log.d(LOG_TAG, "mIsLongHookPress set true");
    				//LGE_CHANGE_E heechul.hyun 2010-09-14 For after reject call(because mediabuttonintentreceiver use ACTION_DOWN)
    				// LGE_UPDATE_E sanghoon.roh@lge.com 2010/06/21 handle LG headsethook key scenario
    			} else {
    				// launch the VoiceDialer
    				Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
    				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				try {
    					try {
    						ActivityManagerNative.getDefault().closeSystemDialogs("call");
    					} catch (RemoteException e) {
    					}
    					startActivity(intent);
    					consumed = true;
    				} catch (ActivityNotFoundException e) {
    				}
    			}
    			  				
    			if (VDBG) Log.d(LOG_TAG, "==> handleHeadsetHook(): consumed = " + consumed);
    			if (consumed) {
    				// If a headset is attached and the press is consumed, also update
    				// any UI items (such as an InCallScreen mute button) that may need to
    				// be updated if their state changed.
    				if (isShowingCallScreen()) {
    					updateInCallScreenTouchUi();
    				}
    			}
    		}
    	}
    };
    // LGE_UPDATE_E sanghoon.roh@lge.com 2010/06/04 handle LG headsethook key scenario
    //20101111 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]

 

    private void handleServiceStateChanged(Intent intent) {
        /**
         * This used to handle updating EriTextWidgetProvider this routine
         * and and listening for ACTION_SERVICE_STATE_CHANGED intents could
         * be removed. But leaving just in case it might be needed in the near
         * future.
         */

        // If service just returned, start sending out the queued messages
        ServiceState ss = ServiceState.newFromBundle(intent.getExtras());

        boolean hasService = true;
        boolean isCdma = false;
        String eriText = "";

        if (ss != null) {
            int state = ss.getState();
            NotificationMgr.getDefault().updateNetworkSelection(state);
            switch (state) {
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    hasService = false;
                    break;
            }
        } else {
            hasService = false;
        }
    }

    public boolean isOtaCallInActiveState() {
        boolean otaCallActive = false;
        if (mInCallScreen != null) {
            otaCallActive = mInCallScreen.isOtaCallInActiveState();
        }
        if (VDBG) Log.d(LOG_TAG, "- isOtaCallInActiveState " + otaCallActive);
        return otaCallActive;
    }

    public boolean isOtaCallInEndState() {
        boolean otaCallEnded = false;
        if (mInCallScreen != null) {
            otaCallEnded = mInCallScreen.isOtaCallInEndState();
        }
        if (VDBG) Log.d(LOG_TAG, "- isOtaCallInEndState " + otaCallEnded);
        return otaCallEnded;
    }

    // it is safe to call clearOtaState() even if the InCallScreen isn't active
    public void clearOtaState() {
        if (DBG) Log.d(LOG_TAG, "- clearOtaState ...");
        if ((mInCallScreen != null)
                && (mInCallScreen.otaUtils != null)) {
            mInCallScreen.otaUtils.cleanOtaScreen(true);
            if (DBG) Log.d(LOG_TAG, "  - clearOtaState clears OTA screen");
        }
    }

    // it is safe to call dismissOtaDialogs() even if the InCallScreen isn't active
    public void dismissOtaDialogs() {
        if (DBG) Log.d(LOG_TAG, "- dismissOtaDialogs ...");
        if ((mInCallScreen != null)
                && (mInCallScreen.otaUtils != null)) {
            mInCallScreen.otaUtils.dismissAllOtaDialogs();
            if (DBG) Log.d(LOG_TAG, "  - dismissOtaDialogs clears OTA dialogs");
        }
    }

    // it is safe to call clearInCallScreenMode() even if the InCallScreen isn't active
    public void clearInCallScreenMode() {
        if (DBG) Log.d(LOG_TAG, "- clearInCallScreenMode ...");
        if (mInCallScreen != null) {
            mInCallScreen.resetInCallScreenMode();
        }
    }

    // Update InCallScreen's touch UI. It is safe to call even if InCallScreen isn't active
    public void updateInCallScreenTouchUi() {
        if (DBG) Log.d(LOG_TAG, "- updateInCallScreenTouchUi ...");
        if (mInCallScreen != null) {
            mInCallScreen.requestUpdateTouchUi();
        }
    }

//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [START_LGE_LAB]
	public void updateInCallScreenCallCardUi() {
		if (DBG) Log.d(LOG_TAG, "- updateInCallScreenCallCardUi ...");
		if (mInCallScreen != null) {
			mInCallScreen.requestUpdateCallCardUi();
		}
	}
//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Call Silent String [END_LGE_LAB]


    private void handleQueryTTYModeResponse(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        if (ar.exception != null) {
            if (DBG) Log.d(LOG_TAG, "handleQueryTTYModeResponse: Error getting TTY state.");
        } else {
            if (DBG) Log.d(LOG_TAG,
                           "handleQueryTTYModeResponse: TTY enable state successfully queried.");

            int ttymode = ((int[]) ar.result)[0];
            if (DBG) Log.d(LOG_TAG, "handleQueryTTYModeResponse:ttymode=" + ttymode);

            Intent ttyModeChanged = new Intent(TtyIntent.TTY_ENABLED_CHANGE_ACTION);
            ttyModeChanged.putExtra("ttyEnabled", ttymode != Phone.TTY_MODE_OFF);
            sendBroadcast(ttyModeChanged);

            String audioTtyMode;
            switch (ttymode) {
            case Phone.TTY_MODE_FULL:
                audioTtyMode = "tty_full";
                break;
            case Phone.TTY_MODE_VCO:
                audioTtyMode = "tty_vco";
                break;
            case Phone.TTY_MODE_HCO:
                audioTtyMode = "tty_hco";
                break;
            case Phone.TTY_MODE_OFF:
            default:
                audioTtyMode = "tty_off";
                break;
            }
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameters("tty_mode="+audioTtyMode);
        }
    }

    private void handleSetTTYModeResponse(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;

        if (ar.exception != null) {
            if (DBG) Log.d (LOG_TAG,
                    "handleSetTTYModeResponse: Error setting TTY mode, ar.exception"
                    + ar.exception);
        }
        phone.queryTTYMode(mHandler.obtainMessage(EVENT_TTY_MODE_GET));
    }

    //20101107 sumi920.kim@lge.com Thunder porting [START_LGE_LAB1]
    // [2010. 03. 30] reduck@lge.com, Implement the lock button at InCallScreen
    /* package */ void goToSleep() {
    	if (VDBG) Log.d(LOG_TAG, "pokeUserActivity()...");
    	try {
    		mPowerManagerService.goToSleep(SystemClock.uptimeMillis() + 1);
    	} catch (RemoteException e) {
    		Log.w(LOG_TAG, "mPowerManagerService.goToSleep() failed: " + e);
    	}
    }
    // [2010. 03. 30] reduck@lge.com
    //20101107 sumi920.kim@lge.com Thunder porting [END_LGE_LAB1]


//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [START_LGE_LAB1]
//LGE_CHANGE_S [yongjin.her@lge.com] 2010/04/26 : OTA reset popup   
    /*
    public void showDialog(Context ctx, int messageId, DialogInterface.OnClickListener listener)
    */
    /*
    public static void showDialog(Context ctx, int messageId, DialogInterface.OnClickListener listener) {
    */
    //Add Keylistener
    public static void showDialog(Context ctx, int messageId, DialogInterface.OnClickListener listener,
                DialogInterface.OnKeyListener onKeyListener) {
        View v = View.inflate(ctx, com.lge.internal.R.layout.alert_popup, null);
            
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        //b.setCancelable(true);
        
        b.setTitle(com.lge.internal.R.string.always_req_notice);
        
        b.setView(v);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        //b.setMessage(com.lge.internal.R.string.STR_USIM_ERR_RECHECK);
        b.setMessage(messageId);

        /*
        b.setNeutralButton(R.string.ok, 
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    //pass
                }
            }
        );
        */
        b.setNeutralButton(R.string.ok, listener );

        //LGE_CHANGE_S [yongjin.her@lge.com] 2010/08/03 : Add Key Handling   
        if(onKeyListener!=null)
        {
            b.setOnKeyListener(onKeyListener);
        }
        //LGE_CHANGE_E [yongjin.her@lge.com] 2010/08/03 : Add Key Handling

        AlertDialog d = b.create();
        //d.setOnDismissListener(mAlwaysReqWhenPSListener);
        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        d.show();
    }
//LGE_CHANGE_E [yongjin.her@lge.com] 2010/04/26 : OTA reset popup

    //IFX CP,AP Reset Add ** this part is referred to WCDMAChannel.java

    private int mCurrentStatus = 0;
    ATService mAtService;
    ATResponse response;
    ResetThread mResetThread; 		

	private class ResetThread extends Thread {			
		//20101008 seungjun.seo@lge.com IFX CP,AP Reset Modify [START_LGE_LAB1]
			private final static int STATE_READY				 = 0;
			private final static int STATE_WAIT_RESPONSE = 1;
			private int failTimer;

			@Override
			public void run() {
			                if(mAtService==null) mAtService = ATService.getDefault();
							failTimer = 0;
							Log.d(LOG_TAG, "init failTimer : " + failTimer);
							mCurrentStatus = STATE_WAIT_RESPONSE;
							Message m = mHandler.obtainMessage(EVENT_PHONE_RESET_COMPLETE);
							mAtService.sendCommand("AT+CFUN=1,1", m);
							//mAtService.sendCommand("AT%restart", m);
	
							while(mCurrentStatus == STATE_WAIT_RESPONSE) {
									if(failTimer >= 10) {
											mHandler.post(new Runnable() {
													public void run() {
															mCurrentStatus = STATE_READY;
													}
											});
											break;
									}
									SystemClock.sleep(1000);
									failTimer++;
							}
					super.run();
			}					
		}

//20101008 yongjin.her@lge.com SIM removed popup & OTA reset popup [END_LGE_LAB1]

    //LGE_FMC_START
    public  boolean isCallFMC()
    {
              if(VTPreferences.OPERATOR.equals("SKT"))
              {
                  	if (VDBG) Log.d(LOG_TAG, "- isCallFMC = " + isFMCState);
                  	
                    iFmcCallInterface = IFmcCallInterface.Stub.asInterface(ServiceManager.getService("FmcCall"));		
                    try {
						if(iFmcCallInterface != null && iFmcCallInterface.getFmcCallState() != 0) // dialing or active or ringing
                                          {              
                                              setCallFMCState(2);
						    return true;
                                          }
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
              }
              return false;

              // For Test
              //setCallFMCState(2);  
             //return true;                         
    }

    public boolean EndCallFMC()
    {        
        if(VTPreferences.OPERATOR.equals("SKT"))
        {
        	if (VDBG) Log.d(LOG_TAG,"EndCallFMC");
        	
            try {
				if(iFmcCallInterface == null || iFmcCallInterface.getFmcCallState() == 0)
				    return false;
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            
             try {
    			if(iFmcCallInterface.endFmcCall())
                     {         
                          setCallFMCState(3);
    			     return true;
                     }
    		} catch (RemoteException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        }
         //PhoneApp.getInstance().setCallFMCState(3);     //Tor Test
        return false;
    }

    // isFMCState 1 : IDEL
    // isFMCState 2 : ACTIVE
    // isFMCState 3 : Ending // For Update UI
    public void setCallFMCState(int State) 
    {
        Log.d(LOG_TAG, "setCallFMCState : " + State);
        isFMCState = State;
    }

    public  int getCallFMCState()
    {
        return isFMCState;
    }

    //LGE_FMC_END
    
	// LGE_VT start
	/*
		Add interface for getPhone()
	*/
	public Phone getPhone()
    {
    	return phone;
    }

    public boolean isLowBattery()
    {
            //config.xml   <integer name="config_lowBatteryWarningLevel">15</integer>
           // Power.LOW_BATTERY_THRESHOLD 10
             int mBatteryLevel = ((PowerManager)getSystemService(Context.POWER_SERVICE)).getBatteryLevel();
             if (VDBG) Log.d(LOG_TAG,"isLowBattery" + " " + mBatteryLevel);

            if(mBatteryLevel<= Power.LOW_BATTERY_THRESHOLD)
    	        return true;
            else
               return false;
    } 
	//-- LGE_VT end 

//20101102 wonho.moon@lge.com Password check add [START_LGE_LAB1]
	public boolean getIsCheckPassword(){
		return isCheckPassword;
	}

	public void setIsCheckPassword(boolean bValue){
		isCheckPassword = bValue;
	}
//20101102 wonho.moon@lge.com Password check add [END_LGE_LAB1]

	
    /**
     * Returns true if the phone is "in use", meaning that at least one line
     * is active (ie. off hook or ringing or dialing).  Conversely, a return
     * value of false means there's currently no phone activity at all.
     * @since 2010/11/09 - Move to here from InCallScreen.java
     */
    public boolean isPhoneInUse() {
        return phone.getState() != Phone.State.IDLE;
    }
	
}
