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

import android.bluetooth.AtCommandHandler;
import android.bluetooth.AtCommandResult;
import android.bluetooth.AtParser;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.HeadsetBase;
//+++ BRCM
import android.bluetooth.BluetoothIntent;
//--- BRCM
import android.bluetooth.ScoSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//+++ BRCM
import android.database.Cursor;
//--- BRCM
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;

import java.util.LinkedList;
//LGE_MERGE_S : LG_BTUI
import java.util.HashMap;
import android.media.ToneGenerator;
import android.provider.Settings;
//LGE_MERGE_E : LG_BTUI

//LGE_VOIP_S
import android.os.RemoteException;
import android.os.ServiceManager;
import com.lge.ims.IFmcCallInterface;
//LGE_VOIP_E
//LGE_MOD_S, yongsung.kang@lge.com
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;

/**
 * Bluetooth headset manager for the Phone app.
 * @hide
 */
public class BluetoothHandsfree {
    private static final String TAG = "BT HS/HF";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 1)
            && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneApp.DBG_LEVEL >= 2);  // even more logging

    public static final int TYPE_UNKNOWN           = 0;
    public static final int TYPE_HEADSET           = 1;
    public static final int TYPE_HANDSFREE         = 2;

    private final Context mContext;
    private final Phone mPhone;
    private final BluetoothA2dp mA2dp;

    private BluetoothDevice mA2dpDevice;
    private int mA2dpState;

    private ServiceState mServiceState;
    private HeadsetBase mHeadset;  // null when not connected
    private int mHeadsetType;
    private boolean mAudioPossible;
    private ScoSocket mIncomingSco;
    private ScoSocket mOutgoingSco;
    private ScoSocket mConnectedSco;

    private Call mForegroundCall;
    private Call mBackgroundCall;
    private Call mRingingCall;

    private AudioManager mAudioManager;
    private PowerManager mPowerManager;

    private boolean mPendingSco;  // waiting for a2dp sink to suspend before establishing SCO
    private boolean mA2dpSuspended;
    private boolean mUserWantsAudio;
    private WakeLock mStartCallWakeLock;  // held while waiting for the intent to start call
    private WakeLock mStartVoiceRecognitionWakeLock;  // held while waiting for voice recognition

    // AT command state
    private static final int GSM_MAX_CONNECTIONS = 6;  // Max connections allowed by GSM
    private static final int CDMA_MAX_CONNECTIONS = 2;  // Max connections allowed by CDMA

    private long mBgndEarliestConnectionTime = 0;
    private boolean mClip = false;  // Calling Line Information Presentation
    private boolean mIndicatorsEnabled = false;
    private boolean mCmee = false;  // Extended Error reporting
    private long[] mClccTimestamps; // Timestamps associated with each clcc index
    private boolean[] mClccUsed;     // Is this clcc index in use
    private boolean mWaitingForCallStart;
    private boolean mWaitingForVoiceRecognition;
//+++ BRCM : BLTH00227761
    private boolean mSendChldResponse;
//--- BRCM

    // do not connect audio until service connection is established
    // for 3-way supported devices, this is after AT+CHLD
    // for non-3-way supported devices, this is after AT+CMER (see spec)
    private boolean mServiceConnectionEstablished;

    private final BluetoothPhoneState mBluetoothPhoneState;  // for CIND and CIEV updates
    private final BluetoothAtPhonebook mPhonebook;
    private Phone.State mPhoneState = Phone.State.IDLE;
    CdmaPhoneCallState.PhoneCallState mCdmaThreeWayCallState =
                                            CdmaPhoneCallState.PhoneCallState.IDLE;

    private DebugThread mDebugThread;
    private int mScoGain = Integer.MIN_VALUE;

    private static Intent sVoiceCommandIntent;

//LGE_MERGE_S : LG_BTUI
    private static final boolean LG_BTUI = true;
    private static final boolean LG_BTUI_SIG = false; //PTS only
    private static final boolean LG_BTUI_MCALL = false; //handle CDMA multi-call
	private static final boolean LG_BTUI_CDMA = false;
    private static final String ACTION_BTUI_LOG = "com.lge.bluetooth.btui_log";
    private static final String PROPERTY_BTUI_LOG = "persist.service.btui.log";
    private boolean mBtUiLog;
    private static final String AT_CGMM_RESULT = "LG-P990";
    private static final String AT_CGMI_RESULT = "LG Electronics Inc.";
    private static final String AT_COPS_RESULT = "";
    BluetoothAdapter mAdapter;
    private boolean isBtEnabled;
    // +CLIP	
    private boolean nextTry;
    // +CCWA (Call Waiting Notification Activation)
    private boolean mCcwa = false;
    // +VTS
    public static final int DTMF_DURATION_MS = 120;
    private ToneGenerator mToneGenerator;
    /* Hash Map to map a character to a tone */
    private static final HashMap<Character, Integer> mToneMap =
        new HashMap<Character, Integer>();
    /* Set up the static maps */
    static {
        // Map the key characters to tones
        mToneMap.put('1', ToneGenerator.TONE_DTMF_1);
        mToneMap.put('2', ToneGenerator.TONE_DTMF_2);
        mToneMap.put('3', ToneGenerator.TONE_DTMF_3);
        mToneMap.put('4', ToneGenerator.TONE_DTMF_4);
        mToneMap.put('5', ToneGenerator.TONE_DTMF_5);
        mToneMap.put('6', ToneGenerator.TONE_DTMF_6);
        mToneMap.put('7', ToneGenerator.TONE_DTMF_7);
        mToneMap.put('8', ToneGenerator.TONE_DTMF_8);
        mToneMap.put('9', ToneGenerator.TONE_DTMF_9);
        mToneMap.put('0', ToneGenerator.TONE_DTMF_0);
        mToneMap.put('#', ToneGenerator.TONE_DTMF_P);
        mToneMap.put('*', ToneGenerator.TONE_DTMF_S);
    }
    // +BVRA
    //private boolean isVRActivated;
    private static Intent mVRCloseIntent;
    private static final String ACTION_AVR_ACTIVITY_STARTED = "com.lge.phone.AVR_ACTIVITY_STARTED";
    private static final String ACTION_AVR_ACTIVITY_CLOSED = "com.lge.phone.AVR_ACTIVITY_CLOSED";
    private static final String ACTION_CLOSE_AVR_ACTIVITY = "com.lge.phone.CLOSE_AVR_ACTIVITY";
//LG_BTUI_VR
    private static final int MODE_NORMAL = 0;
    private static final int MODE_IN_CALL = 2;
    private boolean mNoBVRA;
//LG_BTUI_VR

//LG_BTUI_SIG
    public static final String ACTION_BT_SIG_BATT_LEVEL ="com.android.phone..bt_sig_batt_level";
    public static final String BT_SIG_BATT_LEVEL ="batt_level";
    private static Intent sBtSigBattIntent;
    public static final String ACTION_BT_SIG_RSSI_LEVEL ="com.android.phone..bt_sig_rssi_level";
    public static final String BT_SIG_RSSI_LEVEL ="rssi_level";
    private static Intent sBtSigRssiIntent;
    public static final String ACTION_BT_SIG_SVC_LEVEL ="com.android.phone.bt_sig_svc_level";
    public static final String BT_SIG_SVC_LEVEL ="svc_level";
    private static Intent sBtSigSvcIntent;
    public static final String ACTION_BT_SIG_ROAM_LEVEL ="com.android.phone.bt_sig_roam_level";
    public static final String BT_SIG_ROAM_LEVEL ="roam_level";
    private static Intent sBtSigRoamIntent;
    public static final String BT_SIG_MEM_DIAL_NUM ="01041282926";
//LG_BTUI_SIG : for CDMA
    public static final String ACTION_BT_MO_PEER_ANSWER ="com.android.phone.bt_sig_peer_answer";
    private boolean mWaitingForSignal=false;
    private boolean mPeerAnswer=false;
//LG_BTUI_SIG : for CDMA
//LG_BTUI_SIG
//LG_BTUI_MCALL : for CDMA
    private int mDelayedChld; //TC_AG_TWC_BV_02/03
    private boolean mSkipCIEV4=false; //TC_AG_TWC_BV_02/03
//LG_BTUI_MCALL : for CDMA

//LG_BTUI_SCO
    private boolean isScoOpen;
//LG_BTUI_SCO
//LGE_MERGE_E : LG_BTUI

//LGE_VOIP_S
    public static final String ACTION_FMC_CALL_STATE_CHANGED = "com.lge.SKTFmcCall.FmcCallStateChanged";
    public static final String ACTION_FMC_BT_AUDIO_ON = "com.lge.SKTFmcCall.BluetoothAudioOn";
    public static final String ACTION_FMC_BT_AUDIO_OFF = "com.lge.SKTFmcCall.BluetoothAudioOff";
	
    public IFmcCallInterface mFmcCallInterface;
    public int mFmcControlState;
//LGE_VOIP_E

    // Audio parameters
    private static final String HEADSET_NREC = "bt_headset_nrec";
    private static final String HEADSET_NAME = "bt_headset_name";

    private int mRemoteBrsf = 0;
    private int mLocalBrsf = 0;

    // CDMA specific flag used in context with BT devices having display capabilities
    // to show which Caller is active. This state might not be always true as in CDMA
    // networks if a caller drops off no update is provided to the Phone.
    // This flag is just used as a toggle to provide a update to the BT device to specify
    // which caller is active.
    private boolean mCdmaIsSecondCallActive = false;

    /* Constants from Bluetooth Specification Hands-Free profile version 1.5 */
    private static final int BRSF_AG_THREE_WAY_CALLING = 1 << 0;
    private static final int BRSF_AG_EC_NR = 1 << 1;
    private static final int BRSF_AG_VOICE_RECOG = 1 << 2;
    private static final int BRSF_AG_IN_BAND_RING = 1 << 3;
    private static final int BRSF_AG_VOICE_TAG_NUMBE = 1 << 4;
    private static final int BRSF_AG_REJECT_CALL = 1 << 5;
    private static final int BRSF_AG_ENHANCED_CALL_STATUS = 1 <<  6;
    private static final int BRSF_AG_ENHANCED_CALL_CONTROL = 1 << 7;
    private static final int BRSF_AG_ENHANCED_ERR_RESULT_CODES = 1 << 8;

    private static final int BRSF_HF_EC_NR = 1 << 0;
    private static final int BRSF_HF_CW_THREE_WAY_CALLING = 1 << 1;
    private static final int BRSF_HF_CLIP = 1 << 2;
    private static final int BRSF_HF_VOICE_REG_ACT = 1 << 3;
    private static final int BRSF_HF_REMOTE_VOL_CONTROL = 1 << 4;
    private static final int BRSF_HF_ENHANCED_CALL_STATUS = 1 <<  5;
    private static final int BRSF_HF_ENHANCED_CALL_CONTROL = 1 << 6;

    public static String typeToString(int type) {
        switch (type) {
        case TYPE_UNKNOWN:
            return "unknown";
        case TYPE_HEADSET:
            return "headset";
        case TYPE_HANDSFREE:
            return "handsfree";
        }
        return null;
    }

    public BluetoothHandsfree(Context context, Phone phone) {
//LGE_MERGE_S : LG_BTUI
        if (mToneGenerator == null) {
        	try {
        		mToneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, 80);
        	} catch (RuntimeException e) {
        		BtUiLog("[BTUI] Exception caught while creating local tone generator: "+e);
        		mToneGenerator = null;
        	}
        }
//LGE_MERGE_E : LG_BTUI
        mPhone = phone;
        mContext = context;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean bluetoothCapable = (adapter != null);
        mHeadset = null;  // nothing connected yet
        mA2dp = new BluetoothA2dp(mContext);
        mA2dpState = BluetoothA2dp.STATE_DISCONNECTED;
        mA2dpDevice = null;
        mA2dpSuspended = false;
//+++ BRCM : BLTH00227761
        mSendChldResponse= false;
//--- BRCM


        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mStartCallWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                       TAG + ":StartCall");
        mStartCallWakeLock.setReferenceCounted(false);
        mStartVoiceRecognitionWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                       TAG + ":VoiceRecognition");
        mStartVoiceRecognitionWakeLock.setReferenceCounted(false);

        mLocalBrsf = BRSF_AG_THREE_WAY_CALLING |
                     BRSF_AG_EC_NR |
                     BRSF_AG_REJECT_CALL |
                     BRSF_AG_ENHANCED_CALL_STATUS;

        if (sVoiceCommandIntent == null) {
            sVoiceCommandIntent = new Intent(Intent.ACTION_VOICE_COMMAND);
            sVoiceCommandIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (mContext.getPackageManager().resolveActivity(sVoiceCommandIntent, 0) != null &&
                BluetoothHeadset.isBluetoothVoiceDialingEnabled(mContext)) {
            BtUiLog("[BTUI] BRSF_AG_VOICE_RECOG bit is set");
            mLocalBrsf |= BRSF_AG_VOICE_RECOG;
        }

//LGE_MERGE_S : LG_BTUI
        mLocalBrsf |= BRSF_AG_ENHANCED_ERR_RESULT_CODES;

        if (mVRCloseIntent == null) {
            mVRCloseIntent = new Intent(ACTION_CLOSE_AVR_ACTIVITY);
        }
//LG_BTUI : log
        mBtUiLog = SystemProperties.get(PROPERTY_BTUI_LOG, "0").equals("1");
//LG_BTUI
//LGE_MERGE_E : LG_BTUI
        if (bluetoothCapable) {
            resetAtState();
        }

        mRingingCall = mPhone.getRingingCall();
        mForegroundCall = mPhone.getForegroundCall();
        mBackgroundCall = mPhone.getBackgroundCall();
        mBluetoothPhoneState = new BluetoothPhoneState();
        mUserWantsAudio = true;
        mPhonebook = new BluetoothAtPhonebook(mContext, this);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        cdmaSetSecondCallState(false);
    }

    /* package */ synchronized void onBluetoothEnabled() {
        /* Bluez has a bug where it will always accept and then orphan
         * incoming SCO connections, regardless of whether we have a listening
         * SCO socket. So the best thing to do is always run a listening socket
         * while bluetooth is on so that at least we can diconnect it
         * immediately when we don't want it.
         */
        if (mIncomingSco == null) {
            mIncomingSco = createScoSocket();
            mIncomingSco.accept();
        }
    }

    /* package */ synchronized void onBluetoothDisabled() {
        audioOff();
        if (mIncomingSco != null) {
            mIncomingSco.close();
            mIncomingSco = null;
        }
    }

    private boolean isHeadsetConnected() {
        if (mHeadset == null) {
            return false;
        }
        return mHeadset.isConnected();
    }

    /* package */ void connectHeadset(HeadsetBase headset, int headsetType) {
        mHeadset = headset;
        mHeadsetType = headsetType;
        if (mHeadsetType == TYPE_HEADSET) {
            initializeHeadsetAtParser();
        } else {
            initializeHandsfreeAtParser();
        }
        headset.startEventThread();
        configAudioParameters();

        if (inDebug()) {
            startDebug();
        }

        if (isIncallAudio()) {
            audioOn();
        }
    }

    /* returns true if there is some kind of in-call audio we may wish to route
     * bluetooth to */
    private boolean isIncallAudio() {
        Call.State state = mForegroundCall.getState();

        return (state == Call.State.ACTIVE || state == Call.State.ALERTING);
    }

    /* package */ synchronized void disconnectHeadset() {
        // Close off the SCO sockets
        audioOff();
        mHeadset = null;
        stopDebug();
        resetAtState();
    }

    private void resetAtState() {
        mClip = false;
//LGE_MERGE_S : LG_BTUI
        mCcwa = false;
//LGE_MERGE_E : LG_BTUI
        mIndicatorsEnabled = false;
        mServiceConnectionEstablished = false;
        mCmee = false;
        mClccTimestamps = new long[GSM_MAX_CONNECTIONS];
        mClccUsed = new boolean[GSM_MAX_CONNECTIONS];
        for (int i = 0; i < GSM_MAX_CONNECTIONS; i++) {
            mClccUsed[i] = false;
        }
        mRemoteBrsf = 0;
    }

    private void configAudioParameters() {
        String name = mHeadset.getRemoteDevice().getName();
        if (name == null) {
            name = "<unknown>";
        }
        mAudioManager.setParameters(HEADSET_NAME+"="+name+";"+HEADSET_NREC+"=on");
    }


    /** Represents the data that we send in a +CIND or +CIEV command to the HF
     */
    private class BluetoothPhoneState {
        // 0: no service
        // 1: service
        private int mService;

        // 0: no active call
        // 1: active call (where active means audio is routed - not held call)
        private int mCall;

        // 0: not in call setup
        // 1: incoming call setup
        // 2: outgoing call setup
        // 3: remote party being alerted in an outgoing call setup
        private int mCallsetup;

        // 0: no calls held
        // 1: held call and active call
        // 2: held call only
        private int mCallheld;

        // cellular signal strength of AG: 0-5
        private int mSignal;

        // cellular signal strength in CSQ rssi scale
        private int mRssi;  // for CSQ

        // 0: roaming not active (home)
        // 1: roaming active
        private int mRoam;

        // battery charge of AG: 0-5
        private int mBattchg;

        // 0: not registered
        // 1: registered, home network
        // 5: registered, roaming
        private int mStat;  // for CREG

        private String mRingingNumber;  // Context for in-progress RING's
        private int    mRingingType;
        private boolean mIgnoreRing = false;
        private boolean mStopRing = false;

        private static final int SERVICE_STATE_CHANGED = 1;
        private static final int PRECISE_CALL_STATE_CHANGED = 2;
        private static final int RING = 3;
        private static final int PHONE_CDMA_CALL_WAITING = 4;

//LGE_VOIP_S
        // 0: no service
        // 1: service
        private int mFmcService;

        // 0: no active call
        // 1: active call (where active means audio is routed - not held call)
        private int mFmcCall;

        // 0: not in call setup
        // 1: incoming call setup
        // 2: outgoing call setup
        // 3: remote party being alerted in an outgoing call setup
        private int mFmcCallsetup;

        // 0: no calls held
        // 1: held call and active call
        // 2: held call only
        private int mFmcCallheld;

        private int mFgCallState;
        private int mBgCallState;
        private int mRingCallState;

        private static final int FMC_CALL_STATE_CHANGED = 5;

        private static final int FmcControlState_IDLE = 0;
        private static final int FmcControlState_RINGING = 1;
        private static final int FmcControlState_OFFHOOK = 2;

        private static final int FmcCallState_IDLE = 0;
        private static final int FmcCallState_ACTIVE = 1;
        private static final int FmcCallState_HOLDING = 2;
        private static final int FmcCallState_DIALING = 3;
        private static final int FmcCallState_ALERTING = 4;
        private static final int FmcCallState_INCOMING = 5;
        private static final int FmcCallState_DISCONNECTED = 6;
        private static final int FmcCallState_DISCONNECTING = 7;
//LGE_VOIP_E

        private Handler mStateChangeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case RING:
                    AtCommandResult result = ring();
                    if (result != null) {
                        sendURC(result.toString());
                    }
                    break;
                case SERVICE_STATE_CHANGED:
                    ServiceState state = (ServiceState) ((AsyncResult) msg.obj).result;
                    updateServiceState(sendUpdate(), state);
                    break;
                case PRECISE_CALL_STATE_CHANGED:
                case PHONE_CDMA_CALL_WAITING:
                    Connection connection = null;
                    if (((AsyncResult) msg.obj).result instanceof Connection) {
                        connection = (Connection) ((AsyncResult) msg.obj).result;
                    }
                    handlePreciseCallStateChange(sendUpdate(), connection);
                    break;
//LGE_VOIP_S
                case FMC_CALL_STATE_CHANGED:
                    if (VDBG) log("FMC_CALL_STATE_CHANGED()");
					
                    int controlState = msg.arg1;					
                    handleFmcCallStateChange(sendUpdate(), controlState);
                    break;
//LGE_VOIP_E
                }
            }
        };

        private BluetoothPhoneState() {
            // init members
            updateServiceState(false, mPhone.getServiceState());
            handlePreciseCallStateChange(false, null);
            mBattchg = 5;  // There is currently no API to get battery level
                           // on demand, so set to 5 and wait for an update
            mSignal = asuToSignal(mPhone.getSignalStrength());

            // register for updates
            mPhone.registerForServiceStateChanged(mStateChangeHandler,
                                                  SERVICE_STATE_CHANGED, null);
            mPhone.registerForPreciseCallStateChanged(mStateChangeHandler,
                    PRECISE_CALL_STATE_CHANGED, null);
            if (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
                mPhone.registerForCallWaiting(mStateChangeHandler,
                                              PHONE_CDMA_CALL_WAITING, null);
            }
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED);
            filter.addAction(BluetoothA2dp.ACTION_SINK_STATE_CHANGED);
//LGE_MERGE_S : LG_BTUI
            filter.addAction(ACTION_AVR_ACTIVITY_STARTED);
            filter.addAction(ACTION_AVR_ACTIVITY_CLOSED);
            filter.addAction(ACTION_BTUI_LOG);
//LG_BTUI_SIG
            filter.addAction(ACTION_BT_SIG_BATT_LEVEL);
            filter.addAction(ACTION_BT_SIG_RSSI_LEVEL);
            filter.addAction(ACTION_BT_SIG_SVC_LEVEL);
            filter.addAction(ACTION_BT_SIG_ROAM_LEVEL);
//LG_BTUI_SIG : for CDMA
            if(LG_BTUI_CDMA)	filter.addAction(ACTION_BT_MO_PEER_ANSWER);
//LG_BTUI_SIG : for CDMA
//LG_BTUI_SIG
//LGE_MERGE_E : LG_BTUI

//LGE_VOIP_S
            filter.addAction(ACTION_FMC_CALL_STATE_CHANGED);
            filter.addAction(ACTION_FMC_BT_AUDIO_ON);
            filter.addAction(ACTION_FMC_BT_AUDIO_OFF);
//LGE_VOIP_E

            mContext.registerReceiver(mStateReceiver, filter);
        }

        private void updateBtPhoneStateAfterRadioTechnologyChange() {
            if(VDBG) Log.d(TAG, "updateBtPhoneStateAfterRadioTechnologyChange...");

            //Unregister all events from the old obsolete phone
            mPhone.unregisterForServiceStateChanged(mStateChangeHandler);
            mPhone.unregisterForPreciseCallStateChanged(mStateChangeHandler);
            mPhone.unregisterForCallWaiting(mStateChangeHandler);

            //Register all events new to the new active phone
            mPhone.registerForServiceStateChanged(mStateChangeHandler,
                                                  SERVICE_STATE_CHANGED, null);
            mPhone.registerForPreciseCallStateChanged(mStateChangeHandler,
                    PRECISE_CALL_STATE_CHANGED, null);
            if (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
                mPhone.registerForCallWaiting(mStateChangeHandler,
                                              PHONE_CDMA_CALL_WAITING, null);
            }
        }

        private boolean sendUpdate() {
            return isHeadsetConnected() && mHeadsetType == TYPE_HANDSFREE && mIndicatorsEnabled
                   && mServiceConnectionEstablished;
        }

        private boolean sendClipUpdate() {
            return isHeadsetConnected() && mHeadsetType == TYPE_HANDSFREE && mClip &&
                   mServiceConnectionEstablished;
        }

        private void stopRing() {
            mStopRing = true;
        }

        /* convert [0,31] ASU signal strength to the [0,5] expected by
         * bluetooth devices. Scale is similar to status bar policy
         */
        private int gsmAsuToSignal(SignalStrength signalStrength) {
            int asu = signalStrength.getGsmSignalStrength();
            if      (asu >= 16) return 5;
            else if (asu >= 8)  return 4;
            else if (asu >= 4)  return 3;
            else if (asu >= 2)  return 2;
            else if (asu >= 1)  return 1;
            else                return 0;
        }

        /**
         * Convert the cdma / evdo db levels to appropriate icon level.
         * The scale is similar to the one used in status bar policy.
         *
         * @param signalStrength
         * @return the icon level
         */
        private int cdmaDbmEcioToSignal(SignalStrength signalStrength) {
            int levelDbm = 0;
            int levelEcio = 0;
            int cdmaIconLevel = 0;
            int evdoIconLevel = 0;
            int cdmaDbm = signalStrength.getCdmaDbm();
            int cdmaEcio = signalStrength.getCdmaEcio();

            if (cdmaDbm >= -75) levelDbm = 4;
            else if (cdmaDbm >= -85) levelDbm = 3;
            else if (cdmaDbm >= -95) levelDbm = 2;
            else if (cdmaDbm >= -100) levelDbm = 1;
            else levelDbm = 0;

            // Ec/Io are in dB*10
            if (cdmaEcio >= -90) levelEcio = 4;
            else if (cdmaEcio >= -110) levelEcio = 3;
            else if (cdmaEcio >= -130) levelEcio = 2;
            else if (cdmaEcio >= -150) levelEcio = 1;
            else levelEcio = 0;

            cdmaIconLevel = (levelDbm < levelEcio) ? levelDbm : levelEcio;

            if (mServiceState != null &&
                  (mServiceState.getRadioTechnology() == ServiceState.RADIO_TECHNOLOGY_EVDO_0 ||
                   mServiceState.getRadioTechnology() == ServiceState.RADIO_TECHNOLOGY_EVDO_A)) {
                  int evdoEcio = signalStrength.getEvdoEcio();
                  int evdoSnr = signalStrength.getEvdoSnr();
                  int levelEvdoEcio = 0;
                  int levelEvdoSnr = 0;

                  // Ec/Io are in dB*10
                  if (evdoEcio >= -650) levelEvdoEcio = 4;
                  else if (evdoEcio >= -750) levelEvdoEcio = 3;
                  else if (evdoEcio >= -900) levelEvdoEcio = 2;
                  else if (evdoEcio >= -1050) levelEvdoEcio = 1;
                  else levelEvdoEcio = 0;

                  if (evdoSnr > 7) levelEvdoSnr = 4;
                  else if (evdoSnr > 5) levelEvdoSnr = 3;
                  else if (evdoSnr > 3) levelEvdoSnr = 2;
                  else if (evdoSnr > 1) levelEvdoSnr = 1;
                  else levelEvdoSnr = 0;

                  evdoIconLevel = (levelEvdoEcio < levelEvdoSnr) ? levelEvdoEcio : levelEvdoSnr;
            }
            // TODO(): There is a bug open regarding what should be sent.
            return (cdmaIconLevel > evdoIconLevel) ?  cdmaIconLevel : evdoIconLevel;

        }


        private int asuToSignal(SignalStrength signalStrength) {
            if (signalStrength.isGsm()) {
                return gsmAsuToSignal(signalStrength);
            } else {
                return cdmaDbmEcioToSignal(signalStrength);
            }
        }


        /* convert [0,5] signal strength to a rssi signal strength for CSQ
         * which is [0,31]. Despite the same scale, this is not the same value
         * as ASU.
         */
        private int signalToRssi(int signal) {
            // using C4A suggested values
            switch (signal) {
            case 0: return 0;
            case 1: return 4;
            case 2: return 8;
            case 3: return 13;
            case 4: return 19;
            case 5: return 31;
            }
            return 0;
        }


        private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                    updateBatteryState(intent);
                } else if (intent.getAction().equals(
                            TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED)) {
                    updateSignalState(intent);
                } else if (intent.getAction().equals(BluetoothA2dp.ACTION_SINK_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothA2dp.EXTRA_SINK_STATE,
                            BluetoothA2dp.STATE_DISCONNECTED);
                    int oldState = intent.getIntExtra(BluetoothA2dp.EXTRA_PREVIOUS_SINK_STATE,
                            BluetoothA2dp.STATE_DISCONNECTED);
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BtUiLog("[BTUI] onReceive()... " + intent.getAction() + " : oState("+oldState+") nState("+state+") [0:discntd/2:cntd/4:playing]");

                    // We are only concerned about Connected sinks to suspend and resume
                    // them. We can safely ignore SINK_STATE_CHANGE for other devices.
                    if (mA2dpDevice != null && !device.equals(mA2dpDevice)) return;

                    synchronized (BluetoothHandsfree.this) {
                        mA2dpState = state;
                        if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                            mA2dpDevice = null;
                        } else {
                            mA2dpDevice = device;
                        }
                        if (oldState == BluetoothA2dp.STATE_PLAYING &&
                            mA2dpState == BluetoothA2dp.STATE_CONNECTED) {
                            if (mA2dpSuspended) {
                                if (mPendingSco) {
                                    mHandler.removeMessages(MESSAGE_CHECK_PENDING_SCO);
                                    if (DBG) log("A2DP suspended, completing SCO");
                                    mOutgoingSco = createScoSocket();
                                        BtUiLog("[BTUI] A2DP suspended: ### SCO Open ===>");
                                    if (!mOutgoingSco.connect(
                                            mHeadset.getRemoteDevice().getAddress(),
                                            mHeadset.getRemoteDevice().getName())) {
                                        mOutgoingSco = null;
                                    }
                                    mPendingSco = false;
                                }
                            }
                        }
                    }
                }
//LGE_MERGE_S : LG_BTUI_VR
                else if (intent.getAction().equals(ACTION_AVR_ACTIVITY_STARTED)) {
                    BtUiLog("[BTUI] onReceive() : ACTION_AVR_ACTIVITY_STARTED");
                    if(isHeadsetConnected())
                        startVoiceRecognition();
                }
                else if (intent.getAction().equals(ACTION_AVR_ACTIVITY_CLOSED)) {
                    BtUiLog("[BTUI] onReceive() : ACTION_AVR_ACTIVITY_CLOSED");
                    if(isHeadsetConnected())
                        stopVoiceRecognition();
                }
                else if (intent.getAction().equals(ACTION_BTUI_LOG)) {
                    mBtUiLog = SystemProperties.get(PROPERTY_BTUI_LOG, "0").equals("1");
                }
//LGE_MERGE_E : LG_BTUI_VR
//LGE_MERGE_S : LG_BTUI_SIG
                else if (LG_BTUI_SIG && intent.getAction().equals(ACTION_BT_SIG_BATT_LEVEL)) {
                	int level = intent.getIntExtra(BT_SIG_BATT_LEVEL, 0);
                	sendURC("+CIEV: 7," + level);
                }
                else if (LG_BTUI_SIG && intent.getAction().equals(ACTION_BT_SIG_RSSI_LEVEL)) {
                	int level = intent.getIntExtra(BT_SIG_RSSI_LEVEL, 0);
                	sendURC("+CIEV: 5," + level);
                }
                else if (LG_BTUI_SIG && intent.getAction().equals(ACTION_BT_SIG_SVC_LEVEL)) {
                	int level = intent.getIntExtra(BT_SIG_SVC_LEVEL, 0);
                	sendURC("+CIEV: 1," + level);
                }
                else if (LG_BTUI_SIG && intent.getAction().equals(ACTION_BT_SIG_ROAM_LEVEL)) {
                	int level = intent.getIntExtra(BT_SIG_ROAM_LEVEL, 0);
                	sendURC("+CIEV: 6," + level);
                }
//LG_BTUI_SIG : for CDMA
                else if (LG_BTUI_SIG && LG_BTUI_CDMA && intent.getAction().equals(ACTION_BT_MO_PEER_ANSWER)) {
                    BtUiLog("[BTUI] ### Answer MO call by peer device");
                    if(mWaitingForSignal) {
                        mWaitingForSignal=false;
                        mPeerAnswer=true;
                        handlePreciseCallStateChange(true, null);
                    }
                }
//LG_BTUI_SIG : for CDMA
//LGE_MERGE_E : LG_BTUI_SIG

//LGE_VOIP_S
                else if(intent.getAction().equals(ACTION_FMC_CALL_STATE_CHANGED)) {
                    BtUiLog("[BTUI] onReceive() : ACTION_FMC_CALL_STATE_CHANGED");
		
                    int controlState = intent.getIntExtra("FmcCallControlState", 0);
                    mFgCallState = intent.getIntExtra("FmcForeCallState", 0);
                    mBgCallState = intent.getIntExtra("FmcBackCallState", 0);
                    mRingCallState = intent.getIntExtra("FmcRingCallState", 0);
					
                    Message msg = mStateChangeHandler.obtainMessage(FMC_CALL_STATE_CHANGED, controlState, 0);
                    msg.sendToTarget();
                }
                else if(intent.getAction().equals(ACTION_FMC_BT_AUDIO_ON)) {
                    BtUiLog("[BTUI] onReceive() : ACTION_FMC_BT_AUDIO_ON");
                    userWantsAudioOn();
                }
                else if(intent.getAction().equals(ACTION_FMC_BT_AUDIO_OFF)) {
                    BtUiLog("[BTUI] onReceive() : ACTION_FMC_BT_AUDIO_OFF");
                    userWantsAudioOff();
                }
//LGE_VOIP_E
            }
        };

        private synchronized void updateBatteryState(Intent intent) {
            int batteryLevel = intent.getIntExtra("level", -1);
            int scale = intent.getIntExtra("scale", -1);
            if (batteryLevel == -1 || scale == -1) {
                return;  // ignore
            }
            batteryLevel = batteryLevel * 5 / scale;
            if (mBattchg != batteryLevel) {
                mBattchg = batteryLevel;
                if (sendUpdate()) {
                    sendURC("+CIEV: 7," + mBattchg);
                }
            }
        }

        private synchronized void updateSignalState(Intent intent) {
            // NOTE this function is called by the BroadcastReceiver mStateReceiver after intent
            // ACTION_SIGNAL_STRENGTH_CHANGED and by the DebugThread mDebugThread
            SignalStrength signalStrength = SignalStrength.newFromBundle(intent.getExtras());
            int signal;

            if (signalStrength != null) {
                signal = asuToSignal(signalStrength);
                mRssi = signalToRssi(signal);  // no unsolicited CSQ
                if (signal != mSignal) {
                    mSignal = signal;
                    if (sendUpdate()) {
                        sendURC("+CIEV: 5," + mSignal);
                    }
                }
            } else {
                Log.e(TAG, "Signal Strength null");
            }
        }

        private synchronized void updateServiceState(boolean sendUpdate, ServiceState state) {
            int service = state.getState() == ServiceState.STATE_IN_SERVICE ? 1 : 0;
            int roam = state.getRoaming() ? 1 : 0;
            int stat;
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            mServiceState = state;
            if (service == 0) {
                stat = 0;
            } else {
                stat = (roam == 1) ? 5 : 1;
            }

            if (service != mService) {
                mService = service;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 1," + mService);
                }
            }
            if (roam != mRoam) {
                mRoam = roam;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 6," + mRoam);
                }
            }
            if (stat != mStat) {
                mStat = stat;
                if (sendUpdate) {
                    result.addResponse(toCregString());
                }
            }

            sendURC(result.toString());
        }

        private synchronized void handlePreciseCallStateChange(boolean sendUpdate,
                Connection connection) {
            int call = 0;
            int callsetup = 0;
            int callheld = 0;
            int prevCallsetup = mCallsetup;
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);

            if (VDBG) log("updatePhoneState()");

            // This function will get called when the Precise Call State
            // {@link Call.State} changes. Hence, we might get this update
            // even if the {@link Phone.state} is same as before.
            // Check for the same.

            Phone.State newState = mPhone.getState();
            if (newState != mPhoneState) {
//+++ BRCM
                Intent intent = new Intent(BluetoothIntent.CALL_STATE_CHANGED_ACTION);
//--- BRCM
                mPhoneState = newState;
                switch (mPhoneState) {
                case IDLE:
                    mUserWantsAudio = true;  // out of call - reset state
                    audioOff();
//+++ BRCM
                    intent.putExtra(BluetoothIntent.CALL_STATE, 0);
//--- BRCM
                    break;
                default:
                    callStarted();
//+++ BRCM
                    intent.putExtra(BluetoothIntent.CALL_STATE, 1);		
                    break;
//--- BRCM
                }
//+++ BRCM
                mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
//--- BRCM
            }

//LGE_MERGE_S : LG_BTUI
            if((mAdapter = BluetoothAdapter.getDefaultAdapter()) != null) {
            	if((isBtEnabled = mAdapter.isEnabled()) == true) {
                    BtUiLog("[BTUI] =======================================");
                    BtUiLog("[BTUI] = RING ("+mRingingCall.getState()+")");
                    BtUiLog("[BTUI] = FORE ("+mForegroundCall.getState()+")");
                    BtUiLog("[BTUI] = BACK ("+mBackgroundCall.getState()+")");
                    BtUiLog("[BTUI] =======================================");
            	}
            }
//LGE_MERGE_E : LG_BTUI
            switch(mForegroundCall.getState()) {
            case ACTIVE:
//LGE_MERGE_S : LG_BTUI : Incoming call : A2DP already suspended, SCO open when call connected
                if(mCallsetup==1) {
                    BtUiLog("[BTUI] ### Call Connected => audioOn()");
                    audioOn();
                }
//LGE_MERGE_E : LG_BTUI
//LGE_MERGE_S : LG_BTUI_SIG : for CDMA
                if(LG_BTUI_SIG & LG_BTUI_CDMA) {
                    BtUiLog("[BTUI] mCallsetup("+mCallsetup+") mPeerAnswer("+mPeerAnswer+")");
                    if(mCallsetup==2 && mPeerAnswer) {
                        mPeerAnswer=false;
                        call = 1;
                        callsetup = 0;
                        mAudioPossible = true;
                        break;
                    } else if(mCallsetup==2 && !mPeerAnswer) {
                        return;
                    }
//LG_BTUI_SIG : for CDMA (TC_AG_TWC_BV_05)
                    if(mCallsetup==0 && mCall==1 &&
                        PhoneApp.getInstance().cdmaPhoneCallState.getCurrentCallState() ==
                        CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                    {
                        callStarted(); //OK response for AT+BLDN
                        call = 1;
                        callsetup = 2;
                        mAudioPossible = true;
                        mWaitingForSignal=true;
                        Message msg = mHandler.obtainMessage(DELAYED_2ND_MO_ANSWER);
                        mHandler.sendMessageDelayed(msg, 7000); //peer device answers IUT's MO call within 5sec
                        break;
                    }
//LG_BTUI_SIG
                }
//LGE_MERGE_E : LG_BTUI_SIG : for CDMA

                call = 1;
//LGE_MERGE_S : LG_BTUI : incoming->active, callsetup done
                if(mCallsetup==1)
                    callsetup = 0;
//LGE_MERGE_E : LG_BTUI
                mAudioPossible = true;
                break;
            case DIALING:
                callsetup = 2;
                mAudioPossible = true;

//LGE_MERGE_S : LG_BTUI_SIG : for CDMA
                if(LG_BTUI_SIG & LG_BTUI_CDMA)
                    mWaitingForSignal=true;
//LGE_MERGE_E : LG_BTUI_SIG : for CDMA

//LGE_MERGE_S : LG_BTUI : it's too late until waiting for call=1, so open SCO in advance [H700/H780]
                mAudioPossible = false;
                Message msg = mHandler.obtainMessage(DELAYED_SCO_FOR_RINGTONE);
                mHandler.sendMessageDelayed(msg, 500);
//LGE_MERGE_E : LG_BTUI
                // We also need to send a Call started indication
                // for cases where the 2nd MO was initiated was
                // from a *BT hands free* and is waiting for a
                // +BLND: OK response
                // There is a special case handling of the same case
                // for CDMA below
                if (mPhone.getPhoneType() == Phone.PHONE_TYPE_GSM) {
                    callStarted();
                }
                break;
            case ALERTING:
                callsetup = 3;
                // Open the SCO channel for the outgoing call.
                audioOn();
                mAudioPossible = true;
                break;
            default:
                mAudioPossible = false;
            }

            switch(mRingingCall.getState()) {
            case INCOMING:
            case WAITING:
                callsetup = 1;
                break;
            }

            switch(mBackgroundCall.getState()) {
            case HOLDING:
                if (call == 1) {
                    callheld = 1;
                } else {
                    call = 1;
                    callheld = 2;
                }
                break;
            }

//+++ BRCM : BLTH00227761
            // On CHLD=0, CHLD=1 send OK first and then send call indicators
            if (mSendChldResponse) {
                sendURC("OK");
                mSendChldResponse= false;
            }
//--- BRCM

            if(isBtEnabled) BtUiLog("[BTUI] oCall("+mCall+") -> nCall("+call+")");
            if (mCall != call) {
                if (call == 1) {
                    // This means that a call has transitioned from NOT ACTIVE to ACTIVE.
                    // Switch on audio.
                    audioOn();
                }
                mCall = call;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 2," + mCall);
                }
            }
            if(isBtEnabled) BtUiLog("[BTUI] oCallsetup("+mCallsetup+") -> nCallsetup("+callsetup+")");
            if (mCallsetup != callsetup) {
                mCallsetup = callsetup;
                if (sendUpdate) {
                    // If mCall = 0, send CIEV
                    // mCall = 1, mCallsetup = 0, send CIEV
                    // mCall = 1, mCallsetup = 1, send CIEV after CCWA,
                    // if 3 way supported.
                    // mCall = 1, mCallsetup = 2 / 3 -> send CIEV,
                    // if 3 way is supported
                    if (mCall != 1 || mCallsetup == 0 ||
                        mCallsetup != 1 && (mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
                        result.addResponse("+CIEV: 3," + mCallsetup);
                    }
                }
            }

            if (mPhone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
                PhoneApp app = PhoneApp.getInstance();
                if (app.cdmaPhoneCallState != null) {
                    CdmaPhoneCallState.PhoneCallState currCdmaThreeWayCallState =
                            app.cdmaPhoneCallState.getCurrentCallState();
                    CdmaPhoneCallState.PhoneCallState prevCdmaThreeWayCallState =
                        app.cdmaPhoneCallState.getPreviousCallState();

                    callheld = getCdmaCallHeldStatus(currCdmaThreeWayCallState,
                                                     prevCdmaThreeWayCallState);

                    if (mCdmaThreeWayCallState != currCdmaThreeWayCallState) {
                        // In CDMA, the network does not provide any feedback
                        // to the phone when the 2nd MO call goes through the
                        // stages of DIALING > ALERTING -> ACTIVE we fake the
                        // sequence
                        if ((currCdmaThreeWayCallState ==
                                CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                                    && app.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                            mAudioPossible = true;
                            if (sendUpdate) {
                                if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
//LGE_MERGE_S : LG_BTUI_SIG : for CDMA (TC_AG_TWC_BV_05)
                                    if (LG_BTUI_SIG && LG_BTUI_CDMA && mWaitingForSignal) {
                                        BtUiLog("[BTUI] ### skip +CIEV : MO call is progressing...");
                                    } else
//LGE_MERGE_E : LG_BTUI_SIG
                                    {
                                    result.addResponse("+CIEV: 3,2");
                                    result.addResponse("+CIEV: 3,3");
                                    result.addResponse("+CIEV: 3,0");
                                }
                            }
                            }
                            // We also need to send a Call started indication
                            // for cases where the 2nd MO was initiated was
                            // from a *BT hands free* and is waiting for a
                            // +BLND: OK response
                            callStarted();
                        }

                        // In CDMA, the network does not provide any feedback to
                        // the phone when a user merges a 3way call or swaps
                        // between two calls we need to send a CIEV response
                        // indicating that a call state got changed which should
                        // trigger a CLCC update request from the BT client.
                        if (currCdmaThreeWayCallState ==
                                CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                            mAudioPossible = true;
                            if (sendUpdate) {
                                if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
//LGE_MERGE_S : LG_BTUI_MCALL : for CDMA (TC_AG_TWC_BV_02/03)
                                    if (LG_BTUI_MCALL && LG_BTUI_CDMA && mDelayedChld==2) {
                                        BtUiLog("[BTUI] ### 2nd incoming answer : already handled...");
                                    } else
//LGE_MERGE_E : LG_BTUI_MCALL
                                    {
                                    result.addResponse("+CIEV: 2,1");
                                    result.addResponse("+CIEV: 3,0");
                                }
                            }
                        }
                    }
                    }
                    mCdmaThreeWayCallState = currCdmaThreeWayCallState;
                }
            }

            boolean callsSwitched =
                (callheld == 1 && ! (mBackgroundCall.getEarliestConnectTime() ==
                    mBgndEarliestConnectionTime));

            mBgndEarliestConnectionTime = mBackgroundCall.getEarliestConnectTime();

            if(isBtEnabled) BtUiLog("[BTUI] oCallheld("+mCallheld+") -> nCallheld("+callheld+")");
            if (mCallheld != callheld || callsSwitched) {
                mCallheld = callheld;
                if (sendUpdate) {
//LGE_MERGE_S : LG_BTUI_SIG : for CDMA (TC_AG_TWC_BV_02/03)
                if(LG_BTUI_SIG && LG_BTUI_CDMA) {
                    if(mSkipCIEV4) {
                        mSkipCIEV4=false;
                    } else {
                        result.addResponse("+CIEV: 4," + mCallheld);
                    }
                } else
//LGE_MERGE_E : LG_BTUI_SIG
                    result.addResponse("+CIEV: 4," + mCallheld);
                }
            }

            if (callsetup == 1 && callsetup != prevCallsetup) {
                nextTry = true; //LG_BTUI
                // new incoming call
                String number = null;
                int type = 128;
                // find incoming phone number and type
                if (connection == null) {
                    connection = mRingingCall.getEarliestConnection();
                    if (connection == null) {
                        Log.e(TAG, "Could not get a handle on Connection object for new " +
                              "incoming call");
                    }
                }
                if (connection != null) {
                    number = connection.getAddress();
                    if (number != null) {
                        BtUiLog("[BTUI] mRingingNumber ("+number+")");
                        type = PhoneNumberUtils.toaFromString(number);
                    }
                }
                if (number == null) {
                    number = "";
                }
                if ((call != 0 || callheld != 0) && sendUpdate) {
                    // call waiting
                    if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
//LGE_MERGE_S : LG_BTUI
                        if(mCcwa)
//LGE_MERGE_E : LG_BTUI
                        result.addResponse("+CCWA: \"" + number + "\"," + type);
                        result.addResponse("+CIEV: 3," + callsetup);
                    }
                } else {
                    // regular new incoming call
                    mRingingNumber = number;
                    mRingingType = type;
//LGE_MERGE_S : LG_BTUI : +CLIP only when incoming phone number (mRingingNumber) is available
                    if(LG_BTUI_CDMA && number.length()==0)
                        mIgnoreRing = true;
                    else
//LGE_MERGE_E : LG_BTUI
                    mIgnoreRing = false;

//LGE_MERGE_S : LG_BTUI : Incoming call : A2DP suspend in advance (SCO open when call connected)
                    BtUiA2DPSuspend(1);
                    mPendingSco = false; //to open sco in audioOn()
//LGE_MERGE_E : LG_BTUI
                    mStopRing = false;

                    if ((mLocalBrsf & BRSF_AG_IN_BAND_RING) != 0x0) {
                        audioOn();
                    }
                    result.addResult(ring());
                }
            }
//LGE_MERGE_S : LG_BTUI : +CLIP only when incoming phone number (mRingingNumber) is available
            else if (callsetup == 1 && call != 1 && nextTry)
            {
                nextTry=false;
                connection = mRingingCall.getLatestConnection();
                if (connection != null) {
                    mRingingNumber = connection.getAddress();
                    if (mRingingNumber != null) {
                        BtUiLog("[BTUI] nextTry... mRingingNumber ("+mRingingNumber+")");
                        mRingingType = PhoneNumberUtils.toaFromString(mRingingNumber);
                        mIgnoreRing = false;
                        result.addResult(ring());
                    }
                }
            }
//LGE_MERGE_E : LG_BTUI
            sendURC(result.toString());
        }

//LGE_VOIP_S
        private synchronized void handleFmcCallStateChange(boolean sendUpdate, int controlState) {
            int call = 0;
            int callsetup = 0;
            int callheld = 0;
            int prevCallsetup = mFmcCallsetup;
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);

            if (VDBG) log("handleFmcCallStateChange()");

            // This function will get called when the Precise Call State
            // {@link Call.State} changes. Hence, we might get this update
            // even if the {@link Phone.state} is same as before.
            // Check for the same.

            if (controlState != mFmcControlState) {
//+++ BRCM
                Intent intent = new Intent(BluetoothIntent.CALL_STATE_CHANGED_ACTION);
//--- BRCM
                mFmcControlState = controlState;
                switch (mFmcControlState) {
                case FmcControlState_IDLE:
                    mUserWantsAudio = true;  // out of call - reset state
                    audioOff();
//+++ BRCM
                    intent.putExtra(BluetoothIntent.CALL_STATE, 0);
//--- BRCM
                    break;
                default:
                    callStarted();
//+++ BRCM
                    intent.putExtra(BluetoothIntent.CALL_STATE, 1);		
                    break;
//--- BRCM
                }
//+++ BRCM
                mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
//--- BRCM
            }

//LGE_MERGE_S : LG_BTUI
            if((mAdapter = BluetoothAdapter.getDefaultAdapter()) != null) {
            	if((isBtEnabled = mAdapter.isEnabled()) == true) {
                    BtUiLog("[BTUI] =======================================");
                    BtUiLog("[BTUI] = FMC RING ("+mRingCallState+")");
                    BtUiLog("[BTUI] = FMC FORE ("+mFgCallState+")");
                    BtUiLog("[BTUI] = FMC BACK ("+mBgCallState+")");
                    BtUiLog("[BTUI] =======================================");
            	}
            }
//LGE_MERGE_E : LG_BTUI
            switch(mFgCallState) {
            case FmcCallState_ACTIVE:
//LGE_MERGE_S : LG_BTUI : Incoming call : A2DP already suspended, SCO open when call connected
                if(mFmcCallsetup==1) {
                    BtUiLog("[BTUI] ### Call Connected => audioOn()");
                    audioOn();
                }
//LGE_MERGE_E : LG_BTUI
//LG_BTUI
                call = 1;
//LG_BTUI : incoming->active, callsetup done
                if(mFmcCallsetup==1)
                    callsetup = 0;
//LG_BTUI
                mAudioPossible = true;
                break;
            case FmcCallState_DIALING:
                callsetup = 2;
                mAudioPossible = true;
                audioOn();
                // We also need to send a Call started indication
                // for cases where the 2nd MO was initiated was
                // from a *BT hands free* and is waiting for a
                // +BLND: OK response
                // There is a special case handling of the same case
                // for CDMA below                
                callStarted();                
                break;
            case FmcCallState_ALERTING:
                callsetup = 3;
                // Open the SCO channel for the outgoing call.
                audioOn();
                mAudioPossible = true;
                break;
            default:
                mAudioPossible = false;
            }

            switch(mRingCallState) {
            case FmcCallState_INCOMING:
                callsetup = 1;
                break;
            }

            switch(mBgCallState) {
            case FmcCallState_HOLDING:
                if (call == 1) {
                    callheld = 1;
                } else {
                    call = 1;
                    callheld = 2;
                }
                break;
            }

//LGE_VOIP_S : 20101019
//+++ BRCM : BLTH00227761
            // On CHLD=0, CHLD=1 send OK first and then send call indicators
            if (mSendChldResponse) {
                sendURC("OK");
                mSendChldResponse= false;
            }
//--- BRCM
//LGE_VOIP_E

            if(isBtEnabled) BtUiLog("[BTUI] oCall("+mFmcCall+") -> nCall("+call+")");
            if (mFmcCall != call) {
                if (call == 1) {
                    // This means that a call has transitioned from NOT ACTIVE to ACTIVE.
                    // Switch on audio.
                    audioOn();
                }
                mFmcCall = call;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 2," + mFmcCall);
                }
            }
            if(isBtEnabled) BtUiLog("[BTUI] oCallsetup("+mFmcCallsetup+") -> nCallsetup("+callsetup+")");
            if (mFmcCallsetup != callsetup) {
                mFmcCallsetup = callsetup;
                if (sendUpdate) {
                    // If mCall = 0, send CIEV
                    // mCall = 1, mCallsetup = 0, send CIEV
                    // mCall = 1, mCallsetup = 1, send CIEV after CCWA,
                    // if 3 way supported.
                    // mCall = 1, mCallsetup = 2 / 3 -> send CIEV,
                    // if 3 way is supported
                    if (mFmcCall != 1 || mFmcCallsetup == 0 ||
                        mFmcCallsetup != 1 && (mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
                        result.addResponse("+CIEV: 3," + mFmcCallsetup);
//LG_BTUI
                        if(mFmcCallsetup==2) result.addResponse("+CIEV: 3,3");
//LG_BTUI
                    }
                }
            }

            if(isBtEnabled) BtUiLog("[BTUI] oCallheld("+mFmcCallheld+") -> nCallheld("+callheld+")");
            if (mFmcCallheld != callheld ) {
                mFmcCallheld = callheld;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 4," + mFmcCallheld);
                }
            }

            if (callsetup == 1 && callsetup != prevCallsetup) {
                nextTry = true; //LG_BTUI
                // new incoming call
                String number = null;
                int type = 128;
                // find incoming phone number and type

                loadFmcCallInterface();
                try {
                    number = mFmcCallInterface.getRingingCallNumber();
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to get Ringing Call Number due to remote exception");
                }

                if (number != null) {
                    BtUiLog("[BTUI] mRingingNumber ("+number+")");
                    type = PhoneNumberUtils.toaFromString(number);
                }
				
                if (number == null) {
                    number = "";
                }
                if ((call != 0 || callheld != 0) && sendUpdate) {
                    // call waiting
                    if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
//LGE_MERGE_S : LG_BTUI
                        if(mCcwa)
//LGE_MERGE_E : LG_BTUI
                        result.addResponse("+CCWA: \"" + number + "\"," + type);
                        result.addResponse("+CIEV: 3," + callsetup);
                    }
                } else {
                    // regular new incoming call
                    mRingingNumber = number;
                    mRingingType = type;
//LGE_MERGE_S : LG_BTUI : +CLIP only when incoming phone number (mRingingNumber) is available
                    if(number.length()==0)
                        mIgnoreRing = true;
                    else
//LGE_MERGE_E : LG_BTUI
                    mIgnoreRing = false;

//LGE_MERGE_S : LG_BTUI : Incoming call : A2DP suspend in advance (SCO open when call connected)
                    BtUiA2DPSuspend(1);
                    mPendingSco = false; //to open sco in audioOn()
//LGE_MERGE_E : LG_BTUI
                    mStopRing = false;

//LGE_VOIP_S : 20101019
/*
//BRCM
                    // Set up SCO channel immediately, regardless of in-band
                    // ringtone support. SCO can take up to 2s to set up so
                    // do it now before the call is answered
                    //audioOn();
//BRCM
*/
                    if ((mLocalBrsf & BRSF_AG_IN_BAND_RING) != 0x0) {
                        audioOn();
                    }
//LGE_VOIP_E
                    result.addResult(ring());
                }
            }
//LGE_MERGE_S : LG_BTUI : +CLIP only when incoming phone number (mRingingNumber) is available
            else if (callsetup == 1 && call != 1 && nextTry)	// LGE_VOIP : 20101019
            {
                nextTry=false;

                String number = null;

                loadFmcCallInterface();
                try {
                    number = mFmcCallInterface.getRingingCallNumber();
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to get Ringing Call Number due to remote exception");
                }
				
                mRingingNumber = number;
                if (mRingingNumber != null) {
                    BtUiLog("[BTUI] nextTry... mRingingNumber ("+mRingingNumber+")");
                    mRingingType = PhoneNumberUtils.toaFromString(mRingingNumber);
                    mIgnoreRing = false;
                    result.addResult(ring());
                }
            }
//LGE_MERGE_E : LG_BTUI

            sendURC(result.toString());
        }
//LGE_VOIP_E

        private int getCdmaCallHeldStatus(CdmaPhoneCallState.PhoneCallState currState,
                                  CdmaPhoneCallState.PhoneCallState prevState) {
            int callheld;
            // Update the Call held information
            if (currState == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                if (prevState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                    callheld = 0; //0: no calls held, as now *both* the caller are active
                } else {
                    callheld = 1; //1: held call and active call, as on answering a
                            // Call Waiting, one of the caller *is* put on hold
                }
            } else if (currState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                callheld = 1; //1: held call and active call, as on make a 3 Way Call
                        // the first caller *is* put on hold
            } else {
                callheld = 0; //0: no calls held as this is a SINGLE_ACTIVE call
            }
            return callheld;
        }


        private AtCommandResult ring() {
            if (!mIgnoreRing && !mStopRing && mRingingCall.isRinging()) {
                AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
                result.addResponse("RING");
                if (sendClipUpdate()) {
                    result.addResponse("+CLIP: \"" + mRingingNumber + "\"," + mRingingType);
                }

                Message msg = mStateChangeHandler.obtainMessage(RING);
                mStateChangeHandler.sendMessageDelayed(msg, 3000);
                return result;
            }
//LGE_VOIP_S
            if((mPhoneState == Phone.State.IDLE) &&
                (!mIgnoreRing && !mStopRing && (mRingCallState == FmcCallState_INCOMING))){

                AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
                result.addResponse("RING");
                if (sendClipUpdate()) {
                    result.addResponse("+CLIP: \"" + mRingingNumber + "\"," + mRingingType);
                }

                Message msg = mStateChangeHandler.obtainMessage(RING);
                mStateChangeHandler.sendMessageDelayed(msg, 3000);
                return result;
            }
//LGE_VOIP_E
            return null;
        }

        private synchronized String toCregString() {
            return new String("+CREG: 1," + mStat);
        }

        private synchronized AtCommandResult toCindResult() {
            AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
            String status = "+CIND: " + mService + "," + mCall + "," + mCallsetup + "," +
                            mCallheld + "," + mSignal + "," + mRoam + "," + mBattchg;
            result.addResponse(status);
            return result;
        }
		
//LGE_VOIP_S
        private synchronized AtCommandResult toFmcCindResult() {
            AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
            String status = "+CIND: " + mFmcService + "," + mFmcCall + "," + mFmcCallsetup + "," +
                            mFmcCallheld + "," + mSignal + "," + mRoam + "," + mBattchg;
            result.addResponse(status);
            return result;
        }
//LGE_VOIP_E

        private synchronized AtCommandResult toCsqResult() {
            AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
            String status = "+CSQ: " + mRssi + ",99";
            result.addResponse(status);
            return result;
        }


        private synchronized AtCommandResult getCindTestResult() {
            return new AtCommandResult("+CIND: (\"service\",(0-1))," + "(\"call\",(0-1))," +
                        "(\"callsetup\",(0-3)),(\"callheld\",(0-2)),(\"signal\",(0-5))," +
                        "(\"roam\",(0-1)),(\"battchg\",(0-5))");
        }

        private synchronized void ignoreRing() {
            mCallsetup = 0;
            mIgnoreRing = true;
            if (sendUpdate()) {
                sendURC("+CIEV: 3," + mCallsetup);
            }
        }

    };

    private static final int SCO_ACCEPTED = 1;
    private static final int SCO_CONNECTED = 2;
    private static final int SCO_CLOSED = 3;
    private static final int CHECK_CALL_STARTED = 4;
    private static final int CHECK_VOICE_RECOGNITION_STARTED = 5;
    private static final int MESSAGE_CHECK_PENDING_SCO = 6;
//+++ BRCM
    private static final int DELAYED_SCO_CLOSE = 7;
//--- BRCM
//LGE_MERGE_S
    private static final int DELAYED_SCO_FOR_RINGTONE = 8;
//LG_BTUI : A2DP resume after sco closed
    private static final int A2DP_RESUME = 9;
//LG_BTUI
//LG_BTUI_MCALL : for CDMA (TC_AG_TWC_BV_02/03)
    private static final int DELAYED_CHLD_PROCESS = 10;
//LG_BTUI_MCALL
//LG_BTUI_SIG : for CDMA (TC_AG_TWC_BV_05)
    private static final int DELAYED_2ND_MO_ANSWER = 11;
//LG_BTUI_SIG
//LGE_MERGE_E

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            synchronized (BluetoothHandsfree.this) {
                switch (msg.what) {

//+++ BRCM
                case DELAYED_SCO_CLOSE:
                    ((ScoSocket) msg.obj).close();
                    Log.i(TAG, "Closing Rejected incoming SCO (50ms delay)" );
                    break;
//--- BRCM
                case SCO_ACCEPTED:
                    BtUiLog("[BTUI] mHandler: [SCO_ACCEPTED]");
//LGE_MERGE_S : LG_BTUI_SIG : TC_AG_ACS
                    if (LG_BTUI_SIG && LG_BTUI_CDMA) mAudioPossible = true;
//LGE_MERGE_E : LG_BTUI_SIG
                    if (msg.arg1 == ScoSocket.STATE_CONNECTED) {
                        if (isHeadsetConnected() && (mAudioPossible || allowAudioAnytime()) &&
                                mConnectedSco == null) {
                            Log.i(TAG, "Routing audio for incoming SCO connection");
                            mConnectedSco = (ScoSocket)msg.obj;
                            mAudioManager.setBluetoothScoOn(true);
                            BtUiLog("[BTUI] mHandler: ### SCO Opened");
                            broadcastAudioStateIntent(BluetoothHeadset.AUDIO_STATE_CONNECTED,
                                    mHeadset.getRemoteDevice());
                        } else {
                            Log.i(TAG, "Rejecting incoming SCO connection");
                            BtUiLog("[BTUI] mHandler: ### SCO Rejected");
//+++ BRCM
                            /* some headsets to not like immediate disconnect. e.g. Jabra 3010 */
                            sendMessageDelayed(obtainMessage(DELAYED_SCO_CLOSE, (ScoSocket)msg.obj), 50);
                            /*
                            ((ScoSocket)msg.obj).close();
                            */
//--- BRCM
                        }
                    } // else error trying to accept, try again
                    mIncomingSco = createScoSocket();
                    mIncomingSco.accept();
                    break;
                case SCO_CONNECTED:
                    BtUiLog("[BTUI] mHandler: [SCO_CONNECTED]");
                    if (msg.arg1 == ScoSocket.STATE_CONNECTED && isHeadsetConnected() &&
                            mConnectedSco == null) {
                        if (VDBG) log("Routing audio for outgoing SCO conection");
                        mConnectedSco = (ScoSocket)msg.obj;
                        mAudioManager.setBluetoothScoOn(true);
                        mAudioManager.changeModemAudioPath(4); //jongik2.kim 20101020 cp call path
//LGE_MERGE_S
//LG_BTUI_VR
                        if(mAudioManager.getMode()!=AudioManager.MODE_IN_CALL) {
                            //BtUiSetPhoneState(MODE_IN_CALL, 1);
                    	}
//LG_BTUI_VR
//LGE_MERGE_E
                        broadcastAudioStateIntent(BluetoothHeadset.AUDIO_STATE_CONNECTED,
                                mHeadset.getRemoteDevice());
                    } else if (msg.arg1 == ScoSocket.STATE_CONNECTED) {
                        if (VDBG) log("Rejecting new connected outgoing SCO socket");
                        BtUiLog("[BTUI] mHandler: ### SCO Rejected");
                        ((ScoSocket)msg.obj).close();
                        mOutgoingSco.close();
                    }
                    mOutgoingSco = null;
                    break;
                case SCO_CLOSED:
                    BtUiLog("[BTUI] mHandler: [SCO_CLOSED]");
//+++ BRCM
                    if(msg.obj == null)   // Ensure null pointer check is done before typecasting
                        break;
//--- BRCM
                    if (mConnectedSco == (ScoSocket)msg.obj) {
                        BtUiLog("[BTUI] mHandler: ### connected SCO Closed");
                        mConnectedSco.close();
                        mConnectedSco = null;
                        mAudioManager.setBluetoothScoOn(false);

//jongik2.kim 20101020 cp call path [start]
                        if(mAudioManager.isWiredHeadsetOn()){
						    mAudioManager.changeModemAudioPath(2);
						}
						else{
                            mAudioManager.changeModemAudioPath(1);
						}
//jongik2.kim 20101020 cp call path [end]

//+++ BRCM
                        if (mHeadset != null)
                            broadcastAudioStateIntent(BluetoothHeadset.AUDIO_STATE_DISCONNECTED,
                                                      mHeadset.getRemoteDevice());
                        else
                            broadcastAudioStateIntent(BluetoothHeadset.AUDIO_STATE_DISCONNECTED,
                                                      null);
//--- BRCM
                    } else if (mOutgoingSco == (ScoSocket)msg.obj) {
                        BtUiLog("[BTUI] mHandler: ### outgoing SCO Closed");
                        mOutgoingSco.close();
                        mOutgoingSco = null;
                    }
//+++ BRCM
                    else if (mIncomingSco == (ScoSocket)msg.obj) {
                        mIncomingSco = null;
                    }
//--- BRCM
//LGE_MERGE_S
//LG_BTUI : A2DP resume after sco closed (without delay)
                    if(mA2dpSuspended) {
                        //if(isVRActivated) {
                            //NOT A2DP resume in case of native VR
                        //    mAudioManager.setParameters("A2dpSuspended=false"); //reset A2DP suspend state
                        //    mA2dpSuspended=false;
                        //} else {
                            Message hMsg = mHandler.obtainMessage(A2DP_RESUME);
                            mHandler.sendMessageDelayed(hMsg, 0);
                        //}
                    }
//LG_BTUI
//LG_BTUI_VR
                    if(mUserWantsAudio!=false &&
                        mAudioManager.getMode()!=AudioManager.MODE_NORMAL) {
                        //BtUiSetPhoneState(MODE_NORMAL, 2);
                    }
//LG_BTUI_VR
//LGE_MERGE_E
                    break;
//LGE_MERGE_S: LG_BTUI : A2DP resume after sco closed
                case A2DP_RESUME:
                    if(isIncallAudio()) {
                        BtUiLog("[BTUI] mHandler: [A2DP_RESUME] skip during active call");
                        break;
                    }
                    //BtUiLog("[BTUI] mHandler: [A2DP_RESUME] : mA2dpSuspended ("+mA2dpSuspended+")");
                    if (mA2dpSuspended) {
                        if (isA2dpMultiProfile()) {
                            BtUiLog("[BTUI] mHandler: ### A2DP Resume ===>");
                            mA2dp.resumeSink(mA2dpDevice);
                        }
                        mA2dpSuspended = false;
                    }
                    mHandler.removeMessages(A2DP_RESUME);
                    break;
//LGE_MERGE_E : LG_BTUI
                case CHECK_CALL_STARTED:
                    if (mWaitingForCallStart) {
                        mWaitingForCallStart = false;
                        Log.e(TAG, "Timeout waiting for call to start");
                        sendURC("ERROR");
                        if (mStartCallWakeLock.isHeld()) {
                            mStartCallWakeLock.release();
                        }
                    }
                    break;
                case CHECK_VOICE_RECOGNITION_STARTED:
                    if (mWaitingForVoiceRecognition) {
                        mWaitingForVoiceRecognition = false;
                        BtUiLog("[BTUI] ### VR ### : TIME OUT (VR open fail)");
                        Log.e(TAG, "Timeout waiting for voice recognition to start");
                        sendURC("ERROR");
                    }
                    break;
                case MESSAGE_CHECK_PENDING_SCO:
                    if (mPendingSco && isA2dpMultiProfile()) {
                        Log.w(TAG, "Timeout suspending A2DP for SCO (mA2dpState = " +
                                mA2dpState + "). Starting SCO anyway");
                        mOutgoingSco = createScoSocket();
                        BtUiLog("[BTUI] suspending A2DP timeout... Anyway...: ### SCO Open ===>");
                        if (!(isHeadsetConnected() &&
                                mOutgoingSco.connect(mHeadset.getRemoteDevice().getAddress(),
                                 mHeadset.getRemoteDevice().getName()))) {
                            mOutgoingSco = null;
                        }
                        mPendingSco = false;
                    }
                    break;
//LGE_MERGE_S
//LG_BTUI
                case DELAYED_SCO_FOR_RINGTONE:
                    if (!mForegroundCall.isIdle() || !mRingingCall.isIdle()) {
                        if(BtUiA2DPSuspend(2)==false)
                            audioOn();
                    }
                    mHandler.removeMessages(DELAYED_SCO_FOR_RINGTONE);
                    break;
//LG_BTUI
//LG_BTUI_MCALL : for CDMA (TC_AG_TWC_BV_02/03)
                case DELAYED_CHLD_PROCESS:
                    switch(mDelayedChld) {
                        case 1:
                            mSkipCIEV4=true;
                        case 2:
                            PhoneUtils.answerCall(mPhone);
                            PhoneUtils.setMute(mPhone, false);
                            // Setting the second callers state flag to TRUE (i.e. active)
                            cdmaSetSecondCallState(true);
                            break;
                        case 3:
                            PhoneUtils.switchHoldingAndActive(mPhone);
                            // Toggle the second callers active state flag
                            cdmaSwapSecondCallState();
                            break;
                    }
                    break;
//LG_BTUI_MCALL
//LG_BTUI_SIG : for CDMA (TC_AG_TWC_BV_05)
                case DELAYED_2ND_MO_ANSWER:
                    Intent intent = new Intent(ACTION_BT_MO_PEER_ANSWER);
                    mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
                    break;
//LG_BTUI_SIG
//LGE_MERGE_E
                }
            }
        }
    };

    private ScoSocket createScoSocket() {
        return new ScoSocket(mPowerManager, mHandler, SCO_ACCEPTED, SCO_CONNECTED, SCO_CLOSED);
    }

    private void broadcastAudioStateIntent(int state, BluetoothDevice device) {
        if (VDBG) log("broadcastAudioStateIntent(" + state + ")");
        Intent intent = new Intent(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intent.putExtra(BluetoothHeadset.EXTRA_AUDIO_STATE, state);
//+++ BRCM
        if( device != null )
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
//--- BRCM
        mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
    }

    void updateBtHandsfreeAfterRadioTechnologyChange() {
        if(VDBG) Log.d(TAG, "updateBtHandsfreeAfterRadioTechnologyChange...");

        //Get the Call references from the new active phone again
        mRingingCall = mPhone.getRingingCall();
        mForegroundCall = mPhone.getForegroundCall();
        mBackgroundCall = mPhone.getBackgroundCall();

        mBluetoothPhoneState.updateBtPhoneStateAfterRadioTechnologyChange();
    }

    /** Request to establish SCO (audio) connection to bluetooth
     * headset/handsfree, if one is connected. Does not block.
     * Returns false if the user has requested audio off, or if there
     * is some other immediate problem that will prevent BT audio.
     */
    /* package */ synchronized boolean audioOn() {
    //===============================================
    // LG_BTUI : A2DP suspend not here, we did it in advance
    //===============================================
        if (VDBG) log("audioOn()");
        if (!isHeadsetConnected()) {
            BtUiLog("[BTUI] audioOn(): headset is not connected!");
            return false;
        }
        if (mHeadsetType == TYPE_HANDSFREE && !mServiceConnectionEstablished) {
            BtUiLog("[BTUI] audioOn(): service connection not yet established!");
            return false;
        }

        if (mConnectedSco != null) {
            BtUiLog("[BTUI] audioOn(): audio is already connected");
            return true;
        }

        if (!mUserWantsAudio) {
            BtUiLog("[BTUI] audioOn(): user requested no audio, ignoring");
            return false;
        }

        if (mOutgoingSco != null) {
            BtUiLog("[BTUI] audioOn(): outgoing SCO already in progress");
            return true;
        }

        if (mPendingSco) {
            BtUiLog("[BTUI] audioOn(): SCO already pending");
            return true;
        }

//LGE_MERGE_S : LG_BTUI
/*
        mA2dpSuspended = false;
        mPendingSco = false;
        if (isA2dpMultiProfile() && mA2dpState == BluetoothA2dp.STATE_PLAYING) {
            BtUiLog("[BTUI] audioOn(): ### A2DP Suspend ###");
            mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);
            if (mA2dpSuspended) {
                mPendingSco = true;
                Message msg = mHandler.obtainMessage(MESSAGE_CHECK_PENDING_SCO);
                mHandler.sendMessageDelayed(msg, 2000);
            } else {
                Log.w(TAG, "Could not suspend A2DP stream for SCO, going ahead with SCO");
            }
*/
//LGE_MERGE_E : LG_BTUI

        if (!mPendingSco) {
            mOutgoingSco = createScoSocket();
            BtUiLog("[BTUI] audioOn(): ### SCO Open ===>");
            if (!mOutgoingSco.connect(mHeadset.getRemoteDevice().getAddress(),
                    mHeadset.getRemoteDevice().getName())) {
                mOutgoingSco = null;
            }
        }

        return true;
    }

    /** Used to indicate the user requested BT audio on.
     *  This will establish SCO (BT audio), even if the user requested it off
     *  previously on this call.
     */
    /* package */ synchronized void userWantsAudioOn() {
        mUserWantsAudio = true;
        audioOn();
    }
    /** Used to indicate the user requested BT audio off.
     *  This will prevent us from establishing BT audio again during this call
     *  if audioOn() is called.
     */
    /* package */ synchronized void userWantsAudioOff() {
        mUserWantsAudio = false;
        audioOff();
    }

    /** Request to disconnect SCO (audio) connection to bluetooth
     * headset/handsfree, if one is connected. Does not block.
     */
    /* package */ synchronized void audioOff() {
        if (VDBG) log("audioOff(): mPendingSco: " + mPendingSco +
                ", mConnectedSco: " + mConnectedSco +
                ", mOutgoingSco: " + mOutgoingSco  +
                ", mA2dpState: " + mA2dpState +
                ", mA2dpSuspended: " + mA2dpSuspended +
                ", mIncomingSco:" + mIncomingSco);

//LGE_MERGE_S
//LG_BTUI : A2DP resume after sco closed
/*
        if (mA2dpSuspended) {
            if (isA2dpMultiProfile()) {
              if (DBG) log("resuming A2DP stream after disconnecting SCO");
              mA2dp.resumeSink(mA2dpDevice);
            }
            mA2dpSuspended = false;
        }
*/
//LG_BTUI
//LGE_MERGE_E

        mPendingSco = false;

        if (mConnectedSco != null) {
            BluetoothDevice device = null;
            if (mHeadset != null) {
                device = mHeadset.getRemoteDevice();
            }
            BtUiLog("[BTUI] audioOff(): ### SCO Close (C) ===>");
            mConnectedSco.close();
            mConnectedSco = null;
            mAudioManager.setBluetoothScoOn(false);

//jongik2.kim 20101020 cp call path [start]
            if(mAudioManager.isWiredHeadsetOn()){
			    mAudioManager.changeModemAudioPath(2);
			}
			else{
                mAudioManager.changeModemAudioPath(1);
			}
//jongik2.kim 20101020 cp call path [end]

            broadcastAudioStateIntent(BluetoothHeadset.AUDIO_STATE_DISCONNECTED, device);
        }
        if (mOutgoingSco != null) {
            BtUiLog("[BTUI] audioOff(): ### SCO Close (O) ===>");
            mOutgoingSco.close();
            mOutgoingSco = null;
        }

//LGE_MERGE_S : LG_BTUI : A2DP resume after sco closed (except VR)
        {
            Message msg = mHandler.obtainMessage(A2DP_RESUME);
            mHandler.sendMessageDelayed(msg, 500); //ringtone play through headset when missed call occurs
    	}
//LGE_MERGE_E : LG_BTUI
    }

    /* package */ boolean isAudioOn() {
        return (mConnectedSco != null);
    }

    private boolean isA2dpMultiProfile() {
        return mA2dp != null && mHeadset != null && mA2dpDevice != null &&
                mA2dpDevice.equals(mHeadset.getRemoteDevice());
    }

    /* package */ void ignoreRing() {
        mBluetoothPhoneState.ignoreRing();
    }

    private void sendURC(String urc) {
        if (isHeadsetConnected()) {
            mHeadset.sendURC(urc);
        }
    }

//+++ BRCM
    /** helper to dial a number */
    private AtCommandResult dial(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                Uri.fromParts("tel", number, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        // We do not immediately respond OK, wait until we get a phone state
        // update. If we return OK now and the handsfree immeidately requests
        // our phone state it will say we are not in call yet which confuses
        // some devices
        expectCallStart();
        return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing
    }
//--- BRCM

    /** helper to redial last dialled number */
    private AtCommandResult redial() {
        String number = mPhonebook.getLastDialledNumber();
        if (number == null) {
            // spec seems to suggest sending ERROR if we dont have a
            // number to redial
            if (VDBG) log("Bluetooth redial requested (+BLDN), but no previous " +
                  "outgoing calls found. Ignoring");
            return new AtCommandResult(AtCommandResult.ERROR);
        }

//+++ BRCM
        return dial(number);
        /*
        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                Uri.fromParts("tel", number, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        // We do not immediately respond OK, wait until we get a phone state
        // update. If we return OK now and the handsfree immeidately requests
        // our phone state it will say we are not in call yet which confuses
        // some devices
        expectCallStart();
        return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing
        */
//--- BRCM
    }
    /** helper to do a memory dial */
    private AtCommandResult memoryDial(int index) {

    	int speeddialnum = 999900 + index ;
    	Cursor cursor = null;
        String number = null;
        try {
        	
        	cursor = mContext.getContentResolver().query(
				Data.CONTENT_URI, new String[] {Data.DATA1}, 
				Data.DATA5 + " is " + speeddialnum, null, null);


        	if(cursor != null && cursor.moveToNext()){
				// Get the Number...
				number = cursor.getString(0);
				if(number.length() <= 0)	{
					Log.e(TAG, "**** MEM DIAL Length is not OK *****");
					return new AtCommandResult(AtCommandResult.ERROR);
				}
			}else{
				Log.e(TAG, "**** MEM DIAL is not exist *****");
				return new AtCommandResult(AtCommandResult.ERROR);
			}		
		}finally{
			if(cursor != null) cursor.close();
		}        	

		Log.d(TAG, "**** MEM DIAL is OK *****" + number);
        return dial(number);       
    }

    /** Build the +CLCC result
     *  The complexity arises from the fact that we need to maintain the same
     *  CLCC index even as a call moves between states. */
    private synchronized AtCommandResult gsmGetClccResult() {
        // Collect all known connections
        Connection[] clccConnections = new Connection[GSM_MAX_CONNECTIONS];  // indexed by CLCC index
        LinkedList<Connection> newConnections = new LinkedList<Connection>();
        LinkedList<Connection> connections = new LinkedList<Connection>();
        if (mRingingCall.getState().isAlive()) {
            connections.addAll(mRingingCall.getConnections());
        }
        if (mForegroundCall.getState().isAlive()) {
            connections.addAll(mForegroundCall.getConnections());
        }
        if (mBackgroundCall.getState().isAlive()) {
            connections.addAll(mBackgroundCall.getConnections());
        }

        // Mark connections that we already known about
        boolean clccUsed[] = new boolean[GSM_MAX_CONNECTIONS];
        for (int i = 0; i < GSM_MAX_CONNECTIONS; i++) {
            clccUsed[i] = mClccUsed[i];
            mClccUsed[i] = false;
        }
        for (Connection c : connections) {
            boolean found = false;
            long timestamp = c.getCreateTime();
            for (int i = 0; i < GSM_MAX_CONNECTIONS; i++) {
                if (clccUsed[i] && timestamp == mClccTimestamps[i]) {
                    mClccUsed[i] = true;
                    found = true;
                    clccConnections[i] = c;
                    break;
                }
            }
            if (!found) {
                newConnections.add(c);
            }
        }

        // Find a CLCC index for new connections
        while (!newConnections.isEmpty()) {
            // Find lowest empty index
            int i = 0;
            while (mClccUsed[i]) i++;
            // Find earliest connection
            long earliestTimestamp = newConnections.get(0).getCreateTime();
            Connection earliestConnection = newConnections.get(0);
            for (int j = 0; j < newConnections.size(); j++) {
                long timestamp = newConnections.get(j).getCreateTime();
                if (timestamp < earliestTimestamp) {
                    earliestTimestamp = timestamp;
                    earliestConnection = newConnections.get(j);
                }
            }

            // update
            mClccUsed[i] = true;
            mClccTimestamps[i] = earliestTimestamp;
            clccConnections[i] = earliestConnection;
            newConnections.remove(earliestConnection);
        }

        // Build CLCC
        AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
        for (int i = 0; i < clccConnections.length; i++) {
            if (mClccUsed[i]) {
                String clccEntry = connectionToClccEntry(i, clccConnections[i]);
                if (clccEntry != null) {
                    result.addResponse(clccEntry);
                }
            }
        }

        return result;
    }

    /** Convert a Connection object into a single +CLCC result */
    private String connectionToClccEntry(int index, Connection c) {
        int state;
        switch (c.getState()) {
        case ACTIVE:
            state = 0;
            break;
        case HOLDING:
            state = 1;
            break;
        case DIALING:
            state = 2;
            break;
        case ALERTING:
            state = 3;
            break;
        case INCOMING:
            state = 4;
            break;
        case WAITING:
            state = 5;
            break;
        default:
            return null;  // bad state
        }

        int mpty = 0;
        Call call = c.getCall();
        if (call != null) {
            mpty = call.isMultiparty() ? 1 : 0;
        }

        int direction = c.isIncoming() ? 1 : 0;

        String number = c.getAddress();
        int type = -1;
        if (number != null) {
            type = PhoneNumberUtils.toaFromString(number);
        }

        String result = "+CLCC: " + (index + 1) + "," + direction + "," + state + ",0," + mpty;
        if (number != null) {
            result += ",\"" + number + "\"," + type;
        }
        return result;
    }

    /** Build the +CLCC result for CDMA
     *  The complexity arises from the fact that we need to maintain the same
     *  CLCC index even as a call moves between states. */
    private synchronized AtCommandResult cdmaGetClccResult() {
        // In CDMA at one time a user can have only two live/active connections
        Connection[] clccConnections = new Connection[CDMA_MAX_CONNECTIONS];// indexed by CLCC index

        Call.State ringingCallState = mRingingCall.getState();
        // If the Ringing Call state is INCOMING, that means this is the very first call
        // hence there should not be any Foreground Call
        if (ringingCallState == Call.State.INCOMING) {
            if (VDBG) log("Filling clccConnections[0] for INCOMING state");
            clccConnections[0] = mRingingCall.getLatestConnection();
        } else if (mForegroundCall.getState().isAlive()) {
            // Getting Foreground Call connection based on Call state
            if (mRingingCall.isRinging()) {
                if (VDBG) log("Filling clccConnections[0] & [1] for CALL WAITING state");
                clccConnections[0] = mForegroundCall.getEarliestConnection();
                clccConnections[1] = mRingingCall.getLatestConnection();
            } else {
                BtUiLog("[BTUI] =======================================");
                BtUiLog("[BTUI] [CLCC] MULTI_CALL : call_count ("+mForegroundCall.getConnections().size()+")");
                if (mForegroundCall.getConnections().size() <= 1) {
                    // Single call scenario
                    if (VDBG) log("Filling clccConnections[0] with ForgroundCall latest connection");
                    clccConnections[0] = mForegroundCall.getLatestConnection();
                } else {
                    // Multiple Call scenario. This would be true for both
                    // CONF_CALL and THRWAY_ACTIVE state
                    if (VDBG) log("Filling clccConnections[0] & [1] with ForgroundCall connections");
                    clccConnections[0] = mForegroundCall.getEarliestConnection();
                    clccConnections[1] = mForegroundCall.getLatestConnection();
                    BtUiLog("[BTUI] [CLCC] MULTI_CALL : 1st_call ( "+clccConnections[0].getAddress()+" )");
                    BtUiLog("[BTUI] [CLCC] MULTI_CALL : 2nd_call ( "+clccConnections[1].getAddress()+" )");
                }
            }
        }

        // Update the mCdmaIsSecondCallActive flag based on the Phone call state
        if (PhoneApp.getInstance().cdmaPhoneCallState.getCurrentCallState()
                == CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE) {
            cdmaSetSecondCallState(false);
        } else if (PhoneApp.getInstance().cdmaPhoneCallState.getCurrentCallState()
                == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
            cdmaSetSecondCallState(true);
        }

        // Build CLCC
        AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
        for (int i = 0; (i < clccConnections.length) && (clccConnections[i] != null); i++) {
            String clccEntry = cdmaConnectionToClccEntry(i, clccConnections[i]);
            if (clccEntry != null) {
                result.addResponse(clccEntry);
            }
        }
        BtUiLog("[BTUI] =======================================");

        return result;
    }

    /** Convert a Connection object into a single +CLCC result for CDMA phones */
    private String cdmaConnectionToClccEntry(int index, Connection c) {
        int state;
        PhoneApp app = PhoneApp.getInstance();
        CdmaPhoneCallState.PhoneCallState currCdmaCallState =
                app.cdmaPhoneCallState.getCurrentCallState();
        CdmaPhoneCallState.PhoneCallState prevCdmaCallState =
                app.cdmaPhoneCallState.getPreviousCallState();

//LGE_MERGE_S : LG_BTUI
        int mpty = 0;
//LGE_MERGE_E : LG_BTUI
		
        if ((prevCdmaCallState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                && (currCdmaCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL)) {
            // If the current state is reached after merging two calls
            // we set the state of all the connections as ACTIVE
            state = 0;
//LGE_MERGE_S : LG_BTUI
            BtUiLog("[BTUI] [CLCC] CONF_CALL : index ( "+index+" ),  state ( "+state+" )");
            mpty = 1;
//LGE_MERGE_E : LG_BTUI
        } else {
            switch (c.getState()) {
            case ACTIVE:
                // For CDMA since both the connections are set as active by FW after accepting
                // a Call waiting or making a 3 way call, we need to set the state specifically
                // to ACTIVE/HOLDING based on the mCdmaIsSecondCallActive flag. This way the
                // CLCC result will allow BT devices to enable the swap or merge options
                if (index == 0) { // For the 1st active connection
                    state = mCdmaIsSecondCallActive ? 1 : 0;
                } else { // for the 2nd active connection
                    state = mCdmaIsSecondCallActive ? 0 : 1;
                }
                BtUiLog("[BTUI] [CLCC] ACTIVE : index ( "+index+" ),  state ( "+state+" )");
                break;
            case HOLDING:
                state = 1;
                BtUiLog("[BTUI] [CLCC] HOLDING : index ( "+index+" ),  state ( "+state+" )");
                break;
            case DIALING:
                state = 2;
                BtUiLog("[BTUI] [CLCC] DIALING : index ( "+index+" ),  state ( "+state+" )");
                break;
            case ALERTING:
                state = 3;
                BtUiLog("[BTUI] [CLCC] ALERTING : index ( "+index+" ),  state ( "+state+" )");
                break;
            case INCOMING:
                state = 4;
                BtUiLog("[BTUI] [CLCC] INCOMING : index ( "+index+" ),  state ( "+state+" )");
                break;
            case WAITING:
                state = 5;
                BtUiLog("[BTUI] [CLCC] WAITING : index ( "+index+" ),  state ( "+state+" )");
                break;
            default:
                return null;  // bad state
            }
        }

//LGE_MERGE_S : LG_BTUI : VZW
/*
        int mpty = 0;
        if (currCdmaCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
            mpty = 1;
        } else {
            mpty = 0;
        }
*/
//LGE_MERGE_E
        int direction = c.isIncoming() ? 1 : 0;

        String number = c.getAddress();
        int type = -1;
        if (number != null) {
            type = PhoneNumberUtils.toaFromString(number);
        }

        String result = "+CLCC: " + (index + 1) + "," + direction + "," + state + ",0," + mpty;
        if (number != null) {
            result += ",\"" + number + "\"," + type;
        }
        Log.e(TAG,"[BTUI] [CLCC] result = "+result);
        return result;
    }

//+++ BRCM
    /**
     * Broadcast headset state changed intent after headset SLC (Service Level Connection)
     * is established.
     */
    private void broadcastSlcEstablished() {
        Intent intent = new Intent(BluetoothHeadset.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_CONNECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mHeadset.getRemoteDevice());
        mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
    }
//--- BRCM

    /**
     * Register AT Command handlers to implement the Headset profile
     */
    private void initializeHeadsetAtParser() {
        if (VDBG) log("Registering Headset AT commands");
        AtParser parser = mHeadset.getAtParser();
        // Headset's usually only have one button, which is meant to cause the
        // HS to send us AT+CKPD=200 or AT+CKPD.
        parser.register("+CKPD", new AtCommandHandler() {
            private AtCommandResult headsetButtonPress() {
                if (mRingingCall.isRinging()) {
                    // Answer the call
                    mBluetoothPhoneState.stopRing();
                    sendURC("OK");
                    PhoneUtils.answerCall(mPhone);
                    // If in-band ring tone is supported, SCO connection will already
                    // be up and the following call will just return.
                    audioOn();
                    return new AtCommandResult(AtCommandResult.UNSOLICITED);
                } else if (mForegroundCall.getState().isAlive()) {
                    if (!isAudioOn()) {
                        // Transfer audio from AG to HS
                        audioOn();
                    } else {
                        if (mHeadset.getDirection() == HeadsetBase.DIRECTION_INCOMING &&
                          (System.currentTimeMillis() - mHeadset.getConnectTimestamp()) < 5000) {
                            // Headset made a recent ACL connection to us - and
                            // made a mandatory AT+CKPD request to connect
                            // audio which races with our automatic audio
                            // setup.  ignore
                        } else {
                            // Hang up the call
                            audioOff();
                            PhoneUtils.hangup(mPhone);
                        }
                    }
                    return new AtCommandResult(AtCommandResult.OK);
                } else {
                    // No current call - redial last number
                    return redial();
                }
            }
            @Override
            public AtCommandResult handleActionCommand() {
                return headsetButtonPress();
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                return headsetButtonPress();
            }
        });

//+++ BRCM
        // Microphone Gain
        parser.register("+VGM", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+VGM=<gain>    in range [0,15]
                // Headset/Handsfree is reporting its current gain setting
                return new AtCommandResult(AtCommandResult.OK);
            }
        });

        // Speaker Gain
        parser.register("+VGS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+VGS=<gain>    in range [0,15]
                if (args.length != 1 || !(args[0] instanceof Integer)) {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }

                mScoGain = (Integer) args[0];
                int flag =  mAudioManager.isBluetoothScoOn() ? AudioManager.FLAG_SHOW_UI:0;
                mAudioManager.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO, mScoGain, flag);
                return new AtCommandResult(AtCommandResult.OK);
            }
        });
//--- BRCM

    }

    /**
     * Register AT Command handlers to implement the Handsfree profile
     */
    private void initializeHandsfreeAtParser() {
        if (VDBG) log("Registering Handsfree AT commands");
        AtParser parser = mHeadset.getAtParser();

        // Answer
        parser.register('A', new AtCommandHandler() {
            @Override
            public AtCommandResult handleBasicCommand(String args) {
                BtUiLog("[BTUI] [r] ATA");
                sendURC("OK");
                mBluetoothPhoneState.stopRing();
//LGE_VOIP_S
                if((mPhoneState == Phone.State.IDLE) ||
                    /*3G Call state is Active and FmcCall state is RINGING*/
                    ((mPhoneState == Phone.State.OFFHOOK) && (mFmcControlState == 1/*FmcControlState_RINGING*/))){
                    loadFmcCallInterface();
                    try {
                        mFmcCallInterface.answerFmcCall();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to answer Ringing Call due to remote exception");
                    }
                    return new AtCommandResult(AtCommandResult.UNSOLICITED);
                }
//LGE_VOIP_E
                PhoneUtils.answerCall(mPhone);
                return new AtCommandResult(AtCommandResult.UNSOLICITED);
            }
        });
        parser.register('D', new AtCommandHandler() {
            @Override
            public AtCommandResult handleBasicCommand(String args) {
                if (args.length() > 0) {
                    if (args.charAt(0) == '>') {
                        // Yuck - memory dialling requested.
                        // Just dial last number for now
                        if (args.startsWith(">9999")) {   // for PTS test
                            return new AtCommandResult(AtCommandResult.ERROR);
					}
					else if (args.length() == 1) {	  // No number after ATD>
						if (VDBG) log("Oops: No number specified in ATD>nnn command");
						return new AtCommandResult(AtCommandResult.ERROR);
					}
					
					// Remove trailing ';'
					if (args.charAt(args.length() - 1) == ';') {
						args = args.substring(1, args.length() - 1);
					}
					else {
						args = args.substring(1);
					}
					
					int number;
					try {
						number = Integer.parseInt(args);
					} catch (NumberFormatException e) {
						if (VDBG) log("Oops: args \"" + args + "\" is not a valid integer");
						return new AtCommandResult(AtCommandResult.ERROR);
					}
					
					if (VDBG) log("Dial favorite " + number);
					return memoryDial(number);
					/*
					// Yuck - memory dialling requested.
					// Just dial last number for now
					if (args.startsWith(">9999")) {   // for PTS test
						return new AtCommandResult(AtCommandResult.ERROR);
					}
					return redial();
					*/
//LGE_MOD_E, yongsung.kang@lge.com
                    } else {
                        // Remove trailing ';'
                        if (args.charAt(args.length() - 1) == ';') {
                            args = args.substring(0, args.length() - 1);
                        }
                        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                Uri.fromParts("tel", args, null));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);

                        expectCallStart();
                        return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing
                    }
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
        });

        // Hang-up command
        parser.register("+CHUP", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                BtUiLog("[BTUI] [r] AT+CHUP");
                sendURC("OK");
//LGE_VOIP_S
                if((mPhoneState == Phone.State.IDLE) ||
                    ((mPhoneState == Phone.State.OFFHOOK) && (mFmcControlState == 1/*FmcControlState_RINGING*/))){
                    loadFmcCallInterface();
                    try {
                        mFmcCallInterface.endFmcCall();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to hangup Fmc call due to remote exception");
                    }
                    return new AtCommandResult(AtCommandResult.UNSOLICITED);
                }
//LGE_VOIP_E

//LGE_MERGE_S : LG_BTUI : for CDMA (hang up any ringing call & active call)
                if((!LG_BTUI_SIG) && LG_BTUI_CDMA) {
                    PhoneUtils.hangupRingingAndActive(mPhone);
                }
                else
//LGE_MERGE_E : LG_BTUI
                if (!mForegroundCall.isIdle()) {
                    PhoneUtils.hangupActiveCall(mPhone);
                } else if (!mRingingCall.isIdle()) {
                    PhoneUtils.hangupRingingCall(mPhone);
                } else if (!mBackgroundCall.isIdle()) {
                    PhoneUtils.hangupHoldingCall(mPhone);
                }
                return new AtCommandResult(AtCommandResult.UNSOLICITED);
            }
        });

        // Bluetooth Retrieve Supported Features command
        parser.register("+BRSF", new AtCommandHandler() {
            private AtCommandResult sendBRSF() {
                return new AtCommandResult("+BRSF: " + mLocalBrsf);
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+BRSF=<handsfree supported features bitmap>
                // Handsfree is telling us which features it supports. We
                // send the features we support
                if (args.length == 1 && (args[0] instanceof Integer)) {
                    Log.e(TAG, "[BTUI] [r] AT+BRSF="+args[0]);
                    mRemoteBrsf = (Integer) args[0];
                } else {
                    Log.w(TAG, "HF didn't sent BRSF assuming 0");
                }
                return sendBRSF();
            }
            @Override
            public AtCommandResult handleActionCommand() {
                // This seems to be out of spec, but lets do the nice thing
                return sendBRSF();
            }
            @Override
            public AtCommandResult handleReadCommand() {
                // This seems to be out of spec, but lets do the nice thing
                return sendBRSF();
            }
        });

        // Call waiting notification on/off
        parser.register("+CCWA", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                BtUiLog("[BTUI] [r] AT+CCWA");
                // Seems to be out of spec, but lets return nicely
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleReadCommand() {
                // Call waiting is always on
                return new AtCommandResult("+CCWA: 1");
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+CCWA=<n>
                // Handsfree is trying to enable/disable call waiting. We
                // cannot disable in the current implementation.
//LGE_MERGE_S : LG_BTUI
                mCcwa = args[0].equals(1) ? true : false;
                BtUiLog("[BTUI] [r] AT+CCWA="+args[0]+" : mCcwa("+mCcwa+")");
//LGE_MERGE_E : LG_BTUI
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleTestCommand() {
                // Request for range of supported CCWA paramters
                return new AtCommandResult("+CCWA: (\"n\",(1))");
            }
        });

        // Mobile Equipment Event Reporting enable/disable command
        // Of the full 3GPP syntax paramters (mode, keyp, disp, ind, bfr) we
        // only support paramter ind (disable/enable evert reporting using
        // +CDEV)
        parser.register("+CMER", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult(
                        "+CMER: 3,0,0," + (mIndicatorsEnabled ? "1" : "0"));
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (args.length < 4) {
                    // This is a syntax error
                    return new AtCommandResult(AtCommandResult.ERROR);
                } else if (args[0].equals(3) && args[1].equals(0) &&
                           args[2].equals(0)) {
                    boolean valid = false;
                    if (args[3].equals(0)) {
                        mIndicatorsEnabled = false;
                        valid = true;
                    } else if (args[3].equals(1)) {
                        mIndicatorsEnabled = true;
                        valid = true;
                    }
                    if (valid) {
//+++ BRCM_LOCAL
                        if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) == 0x0 || (mLocalBrsf & BRSF_AG_THREE_WAY_CALLING) == 0x0) {
                        /* Googl Original
                        if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) == 0x0) {
                        */
//--- BRCM_LOCAL
                            mServiceConnectionEstablished = true;
                            sendURC("OK");  // send immediately, then initiate audio
//+++ BRCM
                            broadcastSlcEstablished();
//--- BRCM
                            if (isIncallAudio()) {
                                audioOn();
                            }
                            // only send OK once
                            return new AtCommandResult(AtCommandResult.UNSOLICITED);
                        } else {
                            return new AtCommandResult(AtCommandResult.OK);
                        }
                    }
                }
                return reportCmeError(BluetoothCmeError.OPERATION_NOT_SUPPORTED);
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return new AtCommandResult("+CMER: (3),(0),(0),(0-1)");
            }
        });

        // Mobile Equipment Error Reporting enable/disable
        parser.register("+CMEE", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                BtUiLog("[BTUI] [r] AT+CMEE");
                // out of spec, assume they want to enable
                mCmee = true;
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult("+CMEE: " + (mCmee ? "1" : "0"));
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+CMEE=<n>
                if (args.length == 0) {
                    // <n> ommitted - default to 0
                    mCmee = false;
                    return new AtCommandResult(AtCommandResult.OK);
                } else if (!(args[0] instanceof Integer)) {
                    // Syntax error
                    return new AtCommandResult(AtCommandResult.ERROR);
                } else {
                    mCmee = ((Integer)args[0] == 1);
                    return new AtCommandResult(AtCommandResult.OK);
                }
            }
            @Override
            public AtCommandResult handleTestCommand() {
                // Probably not required but spec, but no harm done
                return new AtCommandResult("+CMEE: (0-1)");
            }
        });

        // Bluetooth Last Dialled Number
        parser.register("+BLDN", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                BtUiLog("[BTUI] [r] AT+BLDN");
                return redial();
            }
        });

        // Indicator Update command
        parser.register("+CIND", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return mBluetoothPhoneState.toCindResult();
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return mBluetoothPhoneState.getCindTestResult();
            }
        });

        // Query Signal Quality (legacy)
        parser.register("+CSQ", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                return mBluetoothPhoneState.toCsqResult();
            }
        });

        // Query network registration state
        parser.register("+CREG", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult(mBluetoothPhoneState.toCregString());
            }
        });

        // Send DTMF. I don't know if we are also expected to play the DTMF tone
        // locally, right now we don't
        parser.register("+VTS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (args.length >= 1) {
                    char c;
                    if (args[0] instanceof Integer) {
                        c = ((Integer) args[0]).toString().charAt(0);
                    } else {
                        c = ((String) args[0]).charAt(0);
                    }
                    if (isValidDtmf(c)) {
//LGE_MERGE_S : LG_BTUI

                        mPhone.sendDtmf(c);

                        BtUiLog("[BTUI] [r] AT+VTS="+c);
//                        String dtmfStr = Character.toString(c);
//                        mPhone.sendBurstDtmf(dtmfStr, 0, 0, null);
                        if(mToneGenerator!=null)
                        	mToneGenerator.startTone(mToneMap.get(c), DTMF_DURATION_MS);
//LGE_MERGE_E : LG_BTUI
                        return new AtCommandResult(AtCommandResult.OK);
                    }
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
            private boolean isValidDtmf(char c) {
                switch (c) {
                case '#':
                case '*':
                    return true;
                default:
                    if (Character.digit(c, 14) != -1) {
                        return true;  // 0-9 and A-D
                    }
                    return false;
                }
            }
        });

        // List calls
        parser.register("+CLCC", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                int phoneType = mPhone.getPhoneType();
                if (phoneType == Phone.PHONE_TYPE_CDMA) {
                    BtUiLog("[BTUI] [r] AT+CLCC");
                    return cdmaGetClccResult();
                } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                    return gsmGetClccResult();
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        });

        // Call Hold and Multiparty Handling command
        parser.register("+CHLD", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                int phoneType = mPhone.getPhoneType();
                if (args.length >= 1) {
                    if (args[0].equals(0)) {
                        boolean result;
//+++ BRCM : BLTH00227761
                        mSendChldResponse= true;
//--- BRCM
//LGE_VOIP_S
                        if(mPhoneState == Phone.State.IDLE) {
                            loadFmcCallInterface();
                            try {
                                mFmcCallInterface.endFmcCall();
                            } catch (RemoteException e) {
                                Log.e(TAG, "Failed to answer Ringing Call due to remote exception");
                            }
                            // return new AtCommandResult(AtCommandResult.OK);
                            return new AtCommandResult(AtCommandResult.UNSOLICITED);	//LGE_VOIP : 20101019 OK > UNSOLICITED
                        }
//LGE_VOIP_E
                        if (mRingingCall.isRinging()) {
                            Log.e(TAG, "[BTUI] [r] AT+CHLD=0 : hangup RingingCall");
                            result = PhoneUtils.hangupRingingCall(mPhone);
                        } else {
                            Log.e(TAG, "[BTUI] [r] AT+CHLD=0 : hangup HoldingCall");
                            result = PhoneUtils.hangupHoldingCall(mPhone);
                        }
                        if (result) {
//+++ BRCM : BLTH00227761
                            return new AtCommandResult(AtCommandResult.UNSOLICITED);
                            /*
                            return new AtCommandResult(AtCommandResult.OK);
                            */
//--- BRCM
                        } else {
                            return new AtCommandResult(AtCommandResult.ERROR);
                        }
                    } else if (args[0].equals(1)) {
                        if (phoneType == Phone.PHONE_TYPE_CDMA) {
//LGE_MERGE_S : LG_BTUI : for CDMA
                            if(!LG_BTUI_SIG)
                            {
                            	Log.e(TAG, "[BTUI] [r] AT+CHLD=1 : Release All Active Calls");
                            	PhoneUtils.hangupRingingCall(mPhone);
                            	PhoneUtils.hangup(mPhone);
                            	return new AtCommandResult(AtCommandResult.OK);
                            }
//LGE_MERGE_E : LG_BTUI
//+++ BRCM : BLTH00227761 : E720
                            sendURC("OK");
//--- BRCM
                            if (mRingingCall.isRinging()) {
                                // If there is Call waiting then answer the call and
                                // put the first call on hold.
                                if (VDBG) log("CHLD:1 Callwaiting Answer call");
//LGE_MERGE_S : LG_BTUI_MCALL : for CDMA (TC_AG_TWC_BV_02/03)
                                BtUiLog("[BTUI] [r] AT+CHLD=1 : Callwaiting Answer call");
                                if (LG_BTUI_MCALL) {
                                    mDelayedChld=1;
                                    Message msg = mHandler.obtainMessage(DELAYED_CHLD_PROCESS);
                                    mHandler.sendMessageDelayed(msg, 100);
                                    return new AtCommandResult(AtCommandResult.OK);
                                }
//LGE_MERGE_E : LG_BTUI_MCALL
                                PhoneUtils.answerCall(mPhone);
                                PhoneUtils.setMute(mPhone, false);
                                // Setting the second callers state flag to TRUE (i.e. active)
                                cdmaSetSecondCallState(true);
                            } else {
                                // If there is no Call waiting then just hangup
                                // the active call. In CDMA this mean that the complete
                                // call session would be ended
                                if (VDBG) log("CHLD:1 Hangup Call");
                                BtUiLog("[BTUI] [r] AT+CHLD=1 : Hangup Call");
                                PhoneUtils.hangup(mPhone);
                            }
//+++ BRCM : BLTH00227761
                            return new AtCommandResult(AtCommandResult.UNSOLICITED);
                            /*
                            return new AtCommandResult(AtCommandResult.OK);
                            */
//--- BRCM
                        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
//+++ BRCM : BLTH00227761
                            mSendChldResponse= true;
//--- BRCM
                            // Hangup active call, answer held call
//LGE_VOIP_S
                            if(mPhoneState == Phone.State.IDLE) {
                                loadFmcCallInterface();
                                try {
                                    mFmcCallInterface.answerFmcCall();
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Failed to answer Ringing Call due to remote exception");
                                }
                                return new AtCommandResult(AtCommandResult.UNSOLICITED);	//LGE_VOIP : 20101019 OK > UNSOLICITED
                                /*
                                return new AtCommandResult(AtCommandResult.OK);
                                */
                            }
//LGE_VOIP_E
                            if (PhoneUtils.answerAndEndActive(mPhone)) {
//+++ BRCM : BLTH00227761
                                return new AtCommandResult(AtCommandResult.UNSOLICITED);
                                /*
                                return new AtCommandResult(AtCommandResult.OK);
                                */
//--- BRCM
                            } else {
                                return new AtCommandResult(AtCommandResult.ERROR);
                            }
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }
                    } else if (args[0].equals(2)) {
                        if (phoneType == Phone.PHONE_TYPE_CDMA) {
                            // For CDMA, the way we switch to a new incoming call is by
                            // calling PhoneUtils.answerCall(). switchAndHoldActive() won't
                            // properly update the call state within telephony.
                            // If the Phone state is already in CONF_CALL then we simply send
                            // a flash cmd by calling switchHoldingAndActive()
//+++ BRCM : BLTH00227761
                            sendURC("OK");
//--- BRCM
                            if (mRingingCall.isRinging()) {
                                if (VDBG) log("CHLD:2 Callwaiting Answer call");
//LGE_MERGE_S : LG_BTUI_MCALL : for CDMA (TC_AG_TWC_BV_02/03)
                                BtUiLog("[BTUI] [r] AT+CHLD=2 : Callwaiting Answer call");
                                if (LG_BTUI_MCALL) {
                                    mDelayedChld=2;
                                    Message msg = mHandler.obtainMessage(DELAYED_CHLD_PROCESS);
                                    mHandler.sendMessageDelayed(msg, 100);
                                    return new AtCommandResult(AtCommandResult.OK);
                                }
//LGE_MERGE_E : LG_BTUI_MCALL
                                PhoneUtils.answerCall(mPhone);
                                PhoneUtils.setMute(mPhone, false);
                                // Setting the second callers state flag to TRUE (i.e. active)
                                cdmaSetSecondCallState(true);
                            } else if (PhoneApp.getInstance().cdmaPhoneCallState
                                    .getCurrentCallState()
                                    == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                                if (VDBG) log("CHLD:2 Swap Calls");
//LGE_MERGE_S : LG_BTUI_MCALL : for CDMA (TC_AG_TWC_BV_02/03)
                                BtUiLog("[BTUI] [r] AT+CHLD=2 : Swap Calls");
                                if (LG_BTUI_MCALL) {
                                    mDelayedChld=3;
                                    Message msg = mHandler.obtainMessage(DELAYED_CHLD_PROCESS);
                                    mHandler.sendMessageDelayed(msg, 100);
                                    return new AtCommandResult(AtCommandResult.OK);
                                }
//LGE_MERGE_E : LG_BTUI_MCALL
                                PhoneUtils.switchHoldingAndActive(mPhone);
                                // Toggle the second callers active state flag
                                cdmaSwapSecondCallState();
                            }
                        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
//+++ BRCM : BLTH00227761
                            sendURC("OK");
//--- BRCM

//LGE_VOIP_S
                        if(mPhoneState == Phone.State.IDLE) {
                                loadFmcCallInterface();
                                try {
                                    mFmcCallInterface.swapFmcCall();
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Failed to answer Ringing Call due to remote exception");
                                }
                                return new AtCommandResult(AtCommandResult.UNSOLICITED);	//LGE_VOIP : 20101019 OK > UNSOLICITED
                                /*
                                return new AtCommandResult(AtCommandResult.OK);
                                */
                        }
//LGE_VOIP_E
                            PhoneUtils.switchHoldingAndActive(mPhone);
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }
//+++ BRCM : BLTH00227761
                        return new AtCommandResult(AtCommandResult.UNSOLICITED);
                        /*
                        return new AtCommandResult(AtCommandResult.OK);
                        */
//--- BRCM
                    } else if (args[0].equals(3)) {
                        if (phoneType == Phone.PHONE_TYPE_CDMA) {
                            // For CDMA, we need to check if the call is in THRWAY_ACTIVE state
//+++ BRCM
                            String threeWaySupport = SystemProperties.get("service.brcm.bt.3way_support", "");
                            if (threeWaySupport != null && threeWaySupport.compareTo("false") == 0) {
                                log("3 way call not supported");
								return new AtCommandResult(AtCommandResult.ERROR);
                            }
//--- BRCM
//+++ BRCM : BLTH00227761
                            sendURC("OK");
//--- BRCM
                            if (PhoneApp.getInstance().cdmaPhoneCallState.getCurrentCallState()
                                    == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                                if (VDBG) log("CHLD:3 Merge Calls");
                                PhoneUtils.mergeCalls(mPhone);
                            }
                        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
//+++ BRCM : BLTH00227761
                            sendURC("OK");
//--- BRCM
                            if (mForegroundCall.getState().isAlive() &&
                                    mBackgroundCall.getState().isAlive()) {
                                PhoneUtils.mergeCalls(mPhone);
                            }
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }
//+++ BRCM : BLTH00227761
                        return new AtCommandResult(AtCommandResult.UNSOLICITED);
                        /*
                        return new AtCommandResult(AtCommandResult.OK);
                        */
//--- BRCM
                    }
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
            @Override
            public AtCommandResult handleTestCommand() {
//+++ BRCM
                String threeWaySupport = SystemProperties.get("service.brcm.bt.3way_support", "");
                if (threeWaySupport != null && threeWaySupport.compareTo("false") == 0) {
                    log("3 way call not supported");
                    return new AtCommandResult("+CHLD: (0,1,2)");
                }
                else {
                    log("3 way call supported");
                    mServiceConnectionEstablished = true;
//LGE_MERGE_S : LG_BTUI : VZW
                    if(LG_BTUI && LG_BTUI_CDMA) {
                        BtUiLog("[BTUI] <CHLD=?> response: (0,1,2)");
                        sendURC("+CHLD: (0,1,2)");
                    } else
//LGE_MERGE_E : LG_BTUI
                    sendURC("+CHLD: (0,1,2,3)");
                    sendURC("OK");  // send reply first, then connect audio
                    broadcastSlcEstablished();
//LGE_MERGE_S : LG_BTUI
                    sendScoGainUpdate(9); //default value
//LGE_MERGE_E : LG_BTUI
                    if (isIncallAudio()) {
                        audioOn();
                    }
                    // already replied
                    return new AtCommandResult(AtCommandResult.UNSOLICITED);
                }
                /* Google Original
                mServiceConnectionEstablished = true;
                sendURC("+CHLD: (0,1,2,3)");
                sendURC("OK");  // send reply first, then connect audio
                if (isIncallAudio()) {
                    audioOn();
                }
                // already replied
                return new AtCommandResult(AtCommandResult.UNSOLICITED);
                */
//--- BRCM
            }
        });

        // Get Network operator name
        parser.register("+COPS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                String operatorName = mPhone.getServiceState().getOperatorAlphaLong();
//LGE_MERGE_S : LG_BTUI
                if(operatorName!=null) BtUiLog("[BTUI] [r] AT+COPS : operatorName("+operatorName+")");
//                if(LG_BTUI_SIG) operatorName = "SK Telecom";
//                else operatorName = AT_COPS_RESULT;
//LGE_MERGE_E : LG_BTUI
                if (operatorName != null) {
                    if (operatorName.length() > 16) {
                        operatorName = operatorName.substring(0, 16);
                    }
                    return new AtCommandResult(
                            "+COPS: 0,0,\"" + operatorName + "\"");
                } else {
                    return new AtCommandResult(
                            "+COPS: 0,0,\"UNKNOWN\",0");
                }
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // Handsfree only supports AT+COPS=3,0
                if (args.length != 2 || !(args[0] instanceof Integer)
                    || !(args[1] instanceof Integer)) {
                    // syntax error
                    return new AtCommandResult(AtCommandResult.ERROR);
                } else if ((Integer)args[0] != 3 || (Integer)args[1] != 0) {
                    return reportCmeError(BluetoothCmeError.OPERATION_NOT_SUPPORTED);
                } else {
                    return new AtCommandResult(AtCommandResult.OK);
                }
            }
            @Override
            public AtCommandResult handleTestCommand() {
                // Out of spec, but lets be friendly
                return new AtCommandResult("+COPS: (3),(0)");
            }
        });

        // Mobile PIN
        // AT+CPIN is not in the handsfree spec (although it is in 3GPP)
        parser.register("+CPIN", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult("+CPIN: READY");
            }
        });

        // Bluetooth Response and Hold
        // Only supported on PDC (Japan) and CDMA networks.
        parser.register("+BTRH", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                // Replying with just OK indicates no response and hold
                // features in use now
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // Neeed PDC or CDMA
                return new AtCommandResult(AtCommandResult.ERROR);
            }
        });

        // Request International Mobile Subscriber Identity (IMSI)
        // Not in bluetooth handset spec
        parser.register("+CIMI", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // AT+CIMI
                String imsi = mPhone.getSubscriberId();
                if (imsi == null || imsi.length() == 0) {
                    return reportCmeError(BluetoothCmeError.SIM_FAILURE);
                } else {
                    return new AtCommandResult(imsi);
                }
            }
        });

        // Calling Line Identification Presentation
        parser.register("+CLIP", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                // Currently assumes the network is provisioned for CLIP
                return new AtCommandResult("+CLIP: " + (mClip ? "1" : "0") + ",1");
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+CLIP=<n>
                if (args.length >= 1 && (args[0].equals(0) || args[0].equals(1))) {
                    mClip = args[0].equals(1);
                    BtUiLog("[BTUI] [r] AT+CCWA="+(mClip?1:0));
                    return new AtCommandResult(AtCommandResult.OK);
                } else {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return new AtCommandResult("+CLIP: (0-1)");
            }
        });

        // AT+CGSN - Returns the device IMEI number.
        parser.register("+CGSN", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // Get the IMEI of the device.
                // mPhone will not be NULL at this point.
                return new AtCommandResult("+CGSN: " + mPhone.getDeviceId());
            }
        });

        // AT+CGMM - Query Model Information
        parser.register("+CGMM", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // Return the Model Information.
                String model = SystemProperties.get("ro.product.model");
//LGE_MERGE_S : LG_BTUI
                model = AT_CGMM_RESULT;
//LGE_MERGE_E : LG_BTUI
                if (model != null) {
                    return new AtCommandResult("+CGMM: " + model);
                } else {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
            }
        });

        // AT+CGMI - Query Manufacturer Information
        parser.register("+CGMI", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // Return the Model Information.
                String manuf = SystemProperties.get("ro.product.manufacturer");
//LGE_MERGE_S : LG_BTUI
                manuf = AT_CGMI_RESULT;
//LGE_MERGE_E : LG_BTUI
                if (manuf != null) {
                    return new AtCommandResult("+CGMI: " + manuf);
                } else {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
            }
        });

        // Noise Reduction and Echo Cancellation control
        parser.register("+NREC", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (args[0].equals(0)) {
                    BtUiLog("[BTUI] [r] AT+NREC=0");
                    mAudioManager.setParameters(HEADSET_NREC+"=off");
                    return new AtCommandResult(AtCommandResult.OK);
                } else if (args[0].equals(1)) {
                    BtUiLog("[BTUI] [r] AT+NREC=1");
                    mAudioManager.setParameters(HEADSET_NREC+"=on");
                    return new AtCommandResult(AtCommandResult.OK);
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
        });

        // Voice recognition (dialing)
        parser.register("+BVRA", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (!BluetoothHeadset.isBluetoothVoiceDialingEnabled(mContext)) {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
                if (args.length >= 1 && args[0].equals(1)) {
                    BtUiLog("[BTUI] [r] AT+BVRA=1 : mWaitForVR("+mWaitingForVoiceRecognition+")");
                    synchronized (BluetoothHandsfree.this) {
                        if (!mWaitingForVoiceRecognition) {
                            try {
                                mContext.startActivity(sVoiceCommandIntent);
                                    BtUiLog("[BTUI] ### VR ### : start VR Activity (waiting for VR open)");
                            } catch (ActivityNotFoundException e) {
                                return new AtCommandResult(AtCommandResult.ERROR);
                            }
                            expectVoiceRecognition();
                        }
                    }
                    return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing yet
                } else if (args.length >= 1 && args[0].equals(0)) {
                    BtUiLog("[BTUI] [r] AT+BVRA=0");
//LGE_MERGE_S : LG_BTUI_VR
/*
                    audioOff();
*/
                    BtUiLog("[BTUI] send intent : ACTION_CLOSE_AVR_ACTIVITY");
                    mContext.sendBroadcast(mVRCloseIntent);
                    mNoBVRA=true;
//LGE_MERGE_E : LG_BTUI_VR
                    return new AtCommandResult(AtCommandResult.OK);
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return new AtCommandResult("+BVRA: (0-1)");
            }
        });

        // Retrieve Subscriber Number
        parser.register("+CNUM", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                String number = mPhone.getLine1Number();
                if (number == null) {
                    return new AtCommandResult(AtCommandResult.OK);
                }
                return new AtCommandResult("+CNUM: ,\"" + number + "\"," +
                        PhoneNumberUtils.toaFromString(number) + ",,4");
            }
        });

        // Microphone Gain
        parser.register("+VGM", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+VGM=<gain>    in range [0,15]
                // Headset/Handsfree is reporting its current gain setting
                return new AtCommandResult(AtCommandResult.OK);
            }
        });

        // Speaker Gain
        parser.register("+VGS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+VGS=<gain>    in range [0,15]
                if (args.length != 1 || !(args[0] instanceof Integer)) {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
                mScoGain = (Integer) args[0];
                BtUiLog("[BTUI] [r] AT+VGS="+mScoGain);
                int flag =  mAudioManager.isBluetoothScoOn() ? AudioManager.FLAG_SHOW_UI:0;

                mAudioManager.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO, mScoGain, flag);
                return new AtCommandResult(AtCommandResult.OK);
            }
        });

        // Phone activity status
        parser.register("+CPAS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                int status = 0;
                switch (mPhone.getState()) {
                case IDLE:
                    status = 0;
                    break;
                case RINGING:
                    status = 3;
                    break;
                case OFFHOOK:
                    status = 4;
                    break;
                }
                return new AtCommandResult("+CPAS: " + status);
            }
        });
        mPhonebook.register(parser);
    }

    public void sendScoGainUpdate(int gain) {
//+++ BRCM
        if (mScoGain != gain && 
            (mHeadsetType == TYPE_HEADSET ||
             (mRemoteBrsf & BRSF_HF_REMOTE_VOL_CONTROL) != 0x0)) {
            sendURC("+VGS:" + gain);
            mScoGain = gain;
        }
        /* Google Original
        if (mScoGain != gain && (mRemoteBrsf & BRSF_HF_REMOTE_VOL_CONTROL) != 0x0) {
            sendURC("+VGS:" + gain);
            mScoGain = gain;
        }
        */
//--- BRCM
    }

    public AtCommandResult reportCmeError(int error) {
        if (mCmee) {
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            result.addResponse("+CME ERROR: " + error);
            return result;
        } else {
            return new AtCommandResult(AtCommandResult.ERROR);
        }
    }

    private static final int START_CALL_TIMEOUT = 10000;  // ms

    private synchronized void expectCallStart() {
        mWaitingForCallStart = true;
        Message msg = Message.obtain(mHandler, CHECK_CALL_STARTED);
        mHandler.sendMessageDelayed(msg, START_CALL_TIMEOUT);
        if (!mStartCallWakeLock.isHeld()) {
            mStartCallWakeLock.acquire(START_CALL_TIMEOUT);
        }
    }

    private synchronized void callStarted() {
        if (mWaitingForCallStart) {
            mWaitingForCallStart = false;
            sendURC("OK");
            if (mStartCallWakeLock.isHeld()) {
                mStartCallWakeLock.release();
            }
        }
    }

    private static final int START_VOICE_RECOGNITION_TIMEOUT = 5000;  // ms

    private synchronized void expectVoiceRecognition() {
        mWaitingForVoiceRecognition = true;
        Message msg = Message.obtain(mHandler, CHECK_VOICE_RECOGNITION_STARTED);
        mHandler.sendMessageDelayed(msg, START_VOICE_RECOGNITION_TIMEOUT);
        BtUiLog("[BTUI] ### VR ### : WakeLock 5 sec (waiting for VR open)");
        if (!mStartVoiceRecognitionWakeLock.isHeld()) {
            mStartVoiceRecognitionWakeLock.acquire(START_VOICE_RECOGNITION_TIMEOUT);
        }
    }

    /* package */ synchronized boolean startVoiceRecognition() {
        if (mWaitingForVoiceRecognition) {
            // HF initiated
            BtUiLog("[BTUI] startVoiceRecognition() : HF initiated...");
            mWaitingForVoiceRecognition = false;
            sendURC("OK");
        } else {
            // AG initiated
            BtUiLog("[BTUI] startVoiceRecognition() : AG initiated...");
//LG_BTUI_VR : BVRA response
            if ((mRemoteBrsf & BRSF_HF_VOICE_REG_ACT) != 0x0) {
                BtUiLog("[BTUI] [s] +BVRA: 1");
                sendURC("+BVRA: 1");
            }
/*
            sendURC("+BVRA: 1");
*/
//LG_BTUI_VR
        }
//LG_BTUI_VR
        mNoBVRA=false; //LG_BTUI : 2010.08.15.9toy : 
        boolean ret=false;
        if(BtUiA2DPSuspend(3)==false) {
            //LG_BTUI_SIG : open SCO when peer device support VR feature
            //if ((mRemoteBrsf & BRSF_HF_VOICE_REG_ACT) != 0x0)
            {
                ret = audioOn();
        }
    	}
/*
        boolean ret = audioOn();
*/
//LG_BTUI_VR
        if (mStartVoiceRecognitionWakeLock.isHeld()) {
            mStartVoiceRecognitionWakeLock.release();
        }
        return ret;
    }

    /* package */ synchronized boolean stopVoiceRecognition() {
//LGE_MERGE_S
//LG_BTUI_VR : BVRA response
        BtUiLog("[BTUI] stopVoiceRecognition() : mNoBVRA("+mNoBVRA+")");
        if ((mRemoteBrsf & BRSF_HF_VOICE_REG_ACT) != 0x0) {
            if(mNoBVRA) {
                mNoBVRA=false;
            } else {
                BtUiLog("[BTUI] [s] +BVRA: 0");
        sendURC("+BVRA: 0");
            }
    	}
//LG_BTUI_VR
        //LG_BTUI : 2010.07.15.9toy : do not close SCO during voice conversation
        if(isIncallAudio()) {
            //do not close SCO
        } else {
        audioOff();
        }
//LG_BTUI_VR
/*
        sendURC("+BVRA: 0");
        audioOff();
*/
//LG_BTUI
//LGE_MERGE_E
        return true;
    }

    private boolean inDebug() {
        return DBG && SystemProperties.getBoolean(DebugThread.DEBUG_HANDSFREE, false);
    }

    private boolean allowAudioAnytime() {
        return inDebug() && SystemProperties.getBoolean(DebugThread.DEBUG_HANDSFREE_AUDIO_ANYTIME,
                false);
    }

    private void startDebug() {
        if (DBG && mDebugThread == null) {
            mDebugThread = new DebugThread();
            mDebugThread.start();
        }
    }

    private void stopDebug() {
        if (mDebugThread != null) {
            mDebugThread.interrupt();
            mDebugThread = null;
        }
    }

    /** Debug thread to read debug properties - runs when debug.bt.hfp is true
     *  at the time a bluetooth handsfree device is connected. Debug properties
     *  are polled and mock updates sent every 1 second */
    private class DebugThread extends Thread {
        /** Turns on/off handsfree profile debugging mode */
        private static final String DEBUG_HANDSFREE = "debug.bt.hfp";

        /** Mock battery level change - use 0 to 5 */
        private static final String DEBUG_HANDSFREE_BATTERY = "debug.bt.hfp.battery";

        /** Mock no cellular service when false */
        private static final String DEBUG_HANDSFREE_SERVICE = "debug.bt.hfp.service";

        /** Mock cellular roaming when true */
        private static final String DEBUG_HANDSFREE_ROAM = "debug.bt.hfp.roam";

        /** false to true transition will force an audio (SCO) connection to
         *  be established. true to false will force audio to be disconnected
         */
        private static final String DEBUG_HANDSFREE_AUDIO = "debug.bt.hfp.audio";

        /** true allows incoming SCO connection out of call.
         */
        private static final String DEBUG_HANDSFREE_AUDIO_ANYTIME = "debug.bt.hfp.audio_anytime";

        /** Mock signal strength change in ASU - use 0 to 31 */
        private static final String DEBUG_HANDSFREE_SIGNAL = "debug.bt.hfp.signal";

        /** Debug AT+CLCC: print +CLCC result */
        private static final String DEBUG_HANDSFREE_CLCC = "debug.bt.hfp.clcc";

        /** Debug AT+BSIR - Send In Band Ringtones Unsolicited AT command.
         * debug.bt.unsol.inband = 0 => AT+BSIR = 0 sent by the AG
         * debug.bt.unsol.inband = 1 => AT+BSIR = 0 sent by the AG
         * Other values are ignored.
         */

        private static final String DEBUG_UNSOL_INBAND_RINGTONE =
            "debug.bt.unsol.inband";

        @Override
        public void run() {
            boolean oldService = true;
            boolean oldRoam = false;
            boolean oldAudio = false;

            while (!isInterrupted() && inDebug()) {
                int batteryLevel = SystemProperties.getInt(DEBUG_HANDSFREE_BATTERY, -1);
                if (batteryLevel >= 0 && batteryLevel <= 5) {
                    Intent intent = new Intent();
                    intent.putExtra("level", batteryLevel);
                    intent.putExtra("scale", 5);
                    mBluetoothPhoneState.updateBatteryState(intent);
                }

                boolean serviceStateChanged = false;
                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_SERVICE, true) != oldService) {
                    oldService = !oldService;
                    serviceStateChanged = true;
                }
                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_ROAM, false) != oldRoam) {
                    oldRoam = !oldRoam;
                    serviceStateChanged = true;
                }
                if (serviceStateChanged) {
                    Bundle b = new Bundle();
                    b.putInt("state", oldService ? 0 : 1);
                    b.putBoolean("roaming", oldRoam);
                    mBluetoothPhoneState.updateServiceState(true, ServiceState.newFromBundle(b));
                }

                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_AUDIO, false) != oldAudio) {
                    oldAudio = !oldAudio;
                    if (oldAudio) {
                        audioOn();
                    } else {
                        audioOff();
                    }
                }

                int signalLevel = SystemProperties.getInt(DEBUG_HANDSFREE_SIGNAL, -1);
                if (signalLevel >= 0 && signalLevel <= 31) {
                    SignalStrength signalStrength = new SignalStrength(signalLevel, -1, -1, -1,
                            -1, -1, -1, true);
                    Intent intent = new Intent();
                    Bundle data = new Bundle();
                    signalStrength.fillInNotifierBundle(data);
                    intent.putExtras(data);
                    mBluetoothPhoneState.updateSignalState(intent);
                }

                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_CLCC, false)) {
                    log(gsmGetClccResult().toString());
                }
                try {
                    sleep(1000);  // 1 second
                } catch (InterruptedException e) {
                    break;
                }

                int inBandRing =
                    SystemProperties.getInt(DEBUG_UNSOL_INBAND_RINGTONE, -1);
                if (inBandRing == 0 || inBandRing == 1) {
                    AtCommandResult result =
                        new AtCommandResult(AtCommandResult.UNSOLICITED);
                    result.addResponse("+BSIR: " + inBandRing);
                    sendURC(result.toString());
                }
            }
        }
    }

    public void cdmaSwapSecondCallState() {
        if (VDBG) log("cdmaSetSecondCallState: Toggling mCdmaIsSecondCallActive");
        BtUiLog("[BTUI] Call Swap : set 2nd_call state to => "+mCdmaIsSecondCallActive);
        mCdmaIsSecondCallActive = !mCdmaIsSecondCallActive;
    }

    public void cdmaSetSecondCallState(boolean state) {
        if (VDBG) log("cdmaSetSecondCallState: Setting mCdmaIsSecondCallActive to " + state);
        BtUiLog("[BTUI] cdmaSetSecondCallState() : is2ndCallActive ( "+state+" )");
        mCdmaIsSecondCallActive = state;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

//LGE_MERGE_S
//LG_BTUI
    private boolean BtUiIsA2DPStreaming() {
        if (isA2dpMultiProfile() && mA2dpState == BluetoothA2dp.STATE_PLAYING)
            return true;
        else
            return false;
    }

    private boolean BtUiA2DPSuspend(int type) {
    	boolean handled=false;
    	mPendingSco = false;
        if(mA2dpState != BluetoothA2dp.STATE_PLAYING) {
            BtUiLog("[BTUI] BtUiA2DPSuspend() : mA2dpState( "+mA2dpState+" ) : skip...");
            return handled;
    	}
    	if (BtUiIsA2DPStreaming()) {
    		String msg;
    		if(type==1) msg="Incoming Call";
    		else if(type==2) msg="Outgoing Call";
    		else if(type==3) msg="VR";
                else if(type==4) msg="VVM";
    		else msg="check";
    
    		mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);
    		BtUiLog("[BTUI] BtUiA2DPSuspend( "+msg+" ): ### A2DP Suspend ### : mA2dpSuspended("+mA2dpSuspended+")");

    		if (mA2dpSuspended) {
    			mPendingSco = true;
    			handled = true; //suspended
    		}
    	}
    	return handled;
    }

    public void BtUiSetPhoneState(int state, int location) {
        if (state==MODE_NORMAL) {
            BtUiLog("[BTUI] setMode(MODE_NORMAL) pos("+location+")");
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
        } else if (state==MODE_IN_CALL) {
            BtUiLog("[BTUI] setMode(MODE_IN_CALL) pos("+location+")");
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        }
    }
    
    private void BtUiSleep(int ms) {
        try {
            Thread.currentThread().sleep(ms);
        } catch (InterruptedException e) {
        }
    }
    
    private void BtUiLog(String msg) {
        if(true/*mBtUiLog*/) Log.e("BluetoothHandsfree", msg);
    }
//LG_BTUI

//LG_BTUI_SCO
    /* package */ synchronized boolean isSLCSetup() {
        return mServiceConnectionEstablished;
    }
    /* package */ synchronized boolean isSupportVoiceReg() {
        boolean isSupport = ((mRemoteBrsf & BRSF_HF_VOICE_REG_ACT) != 0x0) ? true : false;
        BtUiLog("[BTUI] ### BRSF_HF_VOICE_REG_ACT = " + isSupport);
        return isSupport;
    }
//LG_BTUI_SCO

//LGE_VOIP_S
    private void loadFmcCallInterface() {
        if(mFmcCallInterface == null) {
            mFmcCallInterface =
                IFmcCallInterface.Stub.asInterface(ServiceManager.getService("FmcCall"));
		}
	}
//LGE_VOIP_E
}
