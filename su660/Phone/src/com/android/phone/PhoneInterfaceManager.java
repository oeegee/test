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

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.telephony.NeighboringCellInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
// LGE_MPDP START
import android.telephony.gsm.DataConnectionProfile;
// LGE_MPDP END
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.phone.videophone.VideoTelephonyApp;

import java.util.List;
import java.util.ArrayList;
// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [START_LGE_LAB1]
import java.util.LinkedList;
import java.util.Iterator;
// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [END_LGE_LAB1]


/**
 * Implementation of the ITelephony interface.
 */
public class PhoneInterfaceManager extends ITelephony.Stub {
    private static final String LOG_TAG = "PhoneInterfaceManager";
    private static final boolean DBG = true; //(PhoneApp.DBG_LEVEL >= 2);

    // Message codes used with mMainThreadHandler
    private static final int CMD_HANDLE_PIN_MMI = 1;
    private static final int CMD_HANDLE_NEIGHBORING_CELL = 2;
    private static final int EVENT_NEIGHBORING_CELL_DONE = 3;
    private static final int CMD_ANSWER_RINGING_CALL = 4;
    private static final int CMD_END_CALL = 5;  // not used yet
    private static final int CMD_SILENCE_RINGER = 6;

// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [START_LGE_LAB1]
    private static final int EVENT_SKT_Q_SPIDER_START       = 7;
    private static final int EVENT_SKT_Q_SPIDER_STOP        = 8;
    private static final int CMD_SKT_Q_SPIDER_MOBILE_QUALITY_INFORMATION = 9;
    private static final int EVENT_SKT_Q_SPIDER_MOBILE_QUALITY_INFORMATION = 10;
// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [END_LGE_LAB1]

    PhoneApp mApp;
    Phone mPhone;
    MainThreadHandler mMainThreadHandler;

// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [START_LGE_LAB1]
    private boolean isSktQSpiderMobileQualityInfoStarted = false;   //timer started or not
    private String  sktQSpiderMobileQualityInfo = null;             //real data
    private LinkedList <MainThreadRequest>sktQSpiderMobileQualityInfoRequstList = new LinkedList<MainThreadRequest>();
// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [END_LGE_LAB1]

    /**
     * A request object for use with {@link MainThreadHandler}. Requesters should wait() on the
     * request after sending. The main thread will notify the request when it is complete.
     */
    private static final class MainThreadRequest {
        /** The argument to use for the request */
        public Object argument;
        /** The result of the request that is run on the main thread */
        public Object result;

        public MainThreadRequest(Object argument) {
            this.argument = argument;
        }
    }

    /**
     * A handler that processes messages on the main thread in the phone process. Since many
     * of the Phone calls are not thread safe this is needed to shuttle the requests from the
     * inbound binder threads to the main thread in the phone process.  The Binder thread
     * may provide a {@link MainThreadRequest} object in the msg.obj field that they are waiting
     * on, which will be notified when the operation completes and will contain the result of the
     * request.
     *
     * <p>If a MainThreadRequest object is provided in the msg.obj field,
     * note that request.result must be set to something non-null for the calling thread to
     * unblock.
     */
    private final class MainThreadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MainThreadRequest request;
            Message onCompleted;
            AsyncResult ar;

            switch (msg.what) {
                case CMD_HANDLE_PIN_MMI:
                    request = (MainThreadRequest) msg.obj;
                    request.result = Boolean.valueOf(
                            mPhone.handlePinMmi((String) request.argument));
                    // Wake up the requesting thread
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_HANDLE_NEIGHBORING_CELL:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_NEIGHBORING_CELL_DONE,
                            request);
                    mPhone.getNeighboringCids(onCompleted);
                    break;

                case EVENT_NEIGHBORING_CELL_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        // create an empty list to notify the waiting thread
                        request.result = new ArrayList<NeighboringCellInfo>();
                    }
                    // Wake up the requesting thread
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_ANSWER_RINGING_CALL:
                    answerRingingCallInternal();
                    break;

                case CMD_SILENCE_RINGER:
                    silenceRingerInternal();
                    break;

                case CMD_END_CALL:
                    request = (MainThreadRequest) msg.obj;
                    boolean hungUp = false;
                    int phoneType = mPhone.getPhoneType();

                // LGE_VT START    
                    if (isVideoCall())
                    {
                        VideoTelephonyApp mVideoApp = null;

                        mVideoApp = VideoTelephonyApp.getInstance();
                        if(mVideoApp != null)
                        {
                           mVideoApp.userEndCall(); 
                           hungUp = true;
                         }
                        
                         if (DBG) log("CMD_END_CALL for VT : " + (hungUp ? "hung up!" : "no call to hang up"));
                         request.result = hungUp;
                        
                         // Wake up the requesting thread
                        synchronized (request) {
                            request.notifyAll();
                        }                    
                         break;
                     }
                 // LGE_VT END 
                        
                    
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        // CDMA: If the user presses the Power button we treat it as
                        // ending the complete call session
                        hungUp = PhoneUtils.hangupRingingAndActive(mPhone);
                    } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                        // GSM: End the call as per the Phone state
                        hungUp = PhoneUtils.hangup(mPhone);
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    if (DBG) log("CMD_END_CALL: " + (hungUp ? "hung up!" : "no call to hang up"));
                    request.result = hungUp;
                    // Wake up the requesting thread
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [START_LGE_LAB1]

                case EVENT_SKT_Q_SPIDER_START:
                    sktQSpiderMobileQualityInfo = null;
                    isSktQSpiderMobileQualityInfoStarted = true;

                    sktQSpiderMobileQualityInfoRequstList.clear();
                    break;

                case EVENT_SKT_Q_SPIDER_STOP:
                    sktQSpiderMobileQualityInfo = null;
                    isSktQSpiderMobileQualityInfoStarted = false;

                    /*
                        need to return waiting requests
                     */
                    {
                        Iterator<MainThreadRequest> iter =  sktQSpiderMobileQualityInfoRequstList.iterator();

                        if(iter!=null)
                        {
                            while(iter.hasNext())
                            {
                                MainThreadRequest rq = (MainThreadRequest)iter.next();

                                // Wake up the requesting threadS
                                synchronized (rq) {
                                    rq.result = sktQSpiderMobileQualityInfo;
                                    rq.notifyAll();
                                }
                            }
                        }

                        sktQSpiderMobileQualityInfoRequstList.clear();
                    }
                    break;

                case CMD_SKT_Q_SPIDER_MOBILE_QUALITY_INFORMATION:
                    log("CMD_SKT_Q_SPIDER_MOBILE_QUALITY_INFORMATION msg.obj="+msg.obj);

                    request = (MainThreadRequest) msg.obj;
                    
                    if(isSktQSpiderMobileQualityInfoStarted)
                    {//started
                        sktQSpiderMobileQualityInfoRequstList.add(request);
                    }
                    else
                    {//not started - so need to return null immediately
                        // Wake up the requesting threadS
                        synchronized (request) {
                            request.result = null;
                            request.notifyAll();
                        }
                    }
                    break;

                case EVENT_SKT_Q_SPIDER_MOBILE_QUALITY_INFORMATION:
                    log("EVENT_MOBILE_QUALITY_INFORMATION_DONE msg.obj="+msg.obj);
                    
                    ar = (AsyncResult) msg.obj;
                    //request = (MainThreadRequest) ar.userObj;
                    
                    if (ar.exception == null && ar.result != null) {
                        log("EVENT_MOBILE_QUALITY_INFORMATION_DONE ar.result="+ar.result);
                        
                        sktQSpiderMobileQualityInfo = (String)ar.result;
                    } else {
                        log("EVENT_MOBILE_QUALITY_INFORMATION_DONE fail");
                        // create an empty string
                        sktQSpiderMobileQualityInfo = null;
                    }

                    /*
                        need to return waiting requests
                     */
                    {
                        Iterator<MainThreadRequest> iter =  sktQSpiderMobileQualityInfoRequstList.iterator();

                        if(iter!=null)
                        {
                            while(iter.hasNext())
                            {
                                MainThreadRequest rq = (MainThreadRequest)iter.next();

                                // Wake up the requesting threadS
                                synchronized (rq) {
                                    rq.result = sktQSpiderMobileQualityInfo;
                                    rq.notifyAll();
                                }
                            }
                        }

                        sktQSpiderMobileQualityInfoRequstList.clear();
                    }
                    break;
// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [END_LGE_LAB1]

                default:
                    Log.w(LOG_TAG, "MainThreadHandler: unexpected message code: " + msg.what);
                    break;
            }
        }
    }

    /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see sendRequestAsync
     */
    private Object sendRequest(int command, Object argument) {
        if (Looper.myLooper() == mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }

        MainThreadRequest request = new MainThreadRequest(argument);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();

        // Wait for the request to complete
        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    // Do nothing, go back and wait until the request is complete
                }
            }
        }
        return request.result;
    }

    /**
     * Asynchronous ("fire and forget") version of sendRequest():
     * Posts the specified command to be executed on the main thread, and
     * returns immediately.
     * @see sendRequest
     */
    private void sendRequestAsync(int command) {
        mMainThreadHandler.sendEmptyMessage(command);
    }

    public PhoneInterfaceManager(PhoneApp app, Phone phone) {
        mApp = app;
        mPhone = phone;
        mMainThreadHandler = new MainThreadHandler();
        publish();
    }

    private void publish() {
        if (DBG) log("publish: " + this);

        ServiceManager.addService("phone", this);
    }

    //
    // Implementation of the ITelephony interface.
    //

    public void dial(String number) {
        if (DBG) log("dial: " + number);
        // No permission check needed here: This is just a wrapper around the
        // ACTION_DIAL intent, which is available to any app since it puts up
        // the UI before it does anything.

        String url = createTelUrl(number);
        if (url == null) {
            return;
        }

        // PENDING: should we just silently fail if phone is offhook or ringing?
        Phone.State state = mPhone.getState();
        if (state != Phone.State.OFFHOOK && state != Phone.State.RINGING) {
            Intent  intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mApp.startActivity(intent);
        }
    }

    public void call(String number) {
        if (DBG) log("call: " + number);

        // This is just a wrapper around the ACTION_CALL intent, but we still
        // need to do a permission check since we're calling startActivity()
        // from the context of the phone app.
        enforceCallPermission();

        String url = createTelUrl(number);
        if (url == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(mApp, PhoneApp.getCallScreenClassName());
        mApp.startActivity(intent);
    }

    private boolean showCallScreenInternal(boolean specifyInitialDialpadState,
                                           boolean initialDialpadState) {
        if (isIdle()) {
            return false;
        }
        // If the phone isn't idle then go to the in-call screen
        long callingId = Binder.clearCallingIdentity();
        try {
            Intent intent;
            if (specifyInitialDialpadState) {
                intent = PhoneApp.createInCallIntent(initialDialpadState);
            } else {
                intent = PhoneApp.createInCallIntent();
            }
            mApp.startActivity(intent);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
        return true;
    }

    // Show the in-call screen without specifying the initial dialpad state.
    public boolean showCallScreen() {
        return showCallScreenInternal(false, false);
    }

    // The variation of showCallScreen() that specifies the initial dialpad state.
    // (Ideally this would be called showCallScreen() too, just with a different
    // signature, but AIDL doesn't allow that.)
    public boolean showCallScreenWithDialpad(boolean showDialpad) {
        return showCallScreenInternal(true, showDialpad);
    }

    /**
     * End a call based on call state
     * @return true is a call was ended
     */
    public boolean endCall() {
        enforceCallPermission();
        return (Boolean) sendRequest(CMD_END_CALL, null);
    }

    public void answerRingingCall() {
        if (DBG) log("answerRingingCall...");
        // TODO: there should eventually be a separate "ANSWER_PHONE" permission,
        // but that can probably wait till the big TelephonyManager API overhaul.
        // For now, protect this call with the MODIFY_PHONE_STATE permission.
        enforceModifyPermission();
        sendRequestAsync(CMD_ANSWER_RINGING_CALL);
    }

    /**
     * Make the actual telephony calls to implement answerRingingCall().
     * This should only be called from the main thread of the Phone app.
     * @see answerRingingCall
     *
     * TODO: it would be nice to return true if we answered the call, or
     * false if there wasn't actually a ringing incoming call, or some
     * other error occurred.  (In other words, pass back the return value
     * from PhoneUtils.answerCall() or PhoneUtils.answerAndEndActive().)
     * But that would require calling this method via sendRequest() rather
     * than sendRequestAsync(), and right now we don't actually *need* that
     * return value, so let's just return void for now.
     */
    private void answerRingingCallInternal() {
        
        final boolean hasRingingCall = !mPhone.getRingingCall().isIdle();
        if (hasRingingCall) {
            final boolean hasActiveCall = !mPhone.getForegroundCall().isIdle();
            final boolean hasHoldingCall = !mPhone.getBackgroundCall().isIdle();
            if (hasActiveCall && hasHoldingCall) {
                // Both lines are in use!
                // TODO: provide a flag to let the caller specify what
                // policy to use if both lines are in use.  (The current
                // behavior is hardwired to "answer incoming, end ongoing",
                // which is how the CALL button is specced to behave.)
                PhoneUtils.answerAndEndActive(mPhone);
                return;
            } else {

                // LGE_VT START
                if (PhoneApp.mIsVideoCall && PhoneApp.getInstance().isLowBattery()) {
                     PhoneApp.mLowbatteryHangup = true;
                     PhoneUtils.hangupRingingCall(mPhone);
        	      return;
                } 
                // LGE_VT END            
                
                // answerCall() will automatically hold the current active
                // call, if there is one.
                PhoneUtils.answerCall(mPhone);
                return;
            }
        } else {
            // No call was ringing.
            return;
        }
    }

    public void silenceRinger() {
        if (DBG) log("silenceRinger...");
        // TODO: find a more appropriate permission to check here.
        // (That can probably wait till the big TelephonyManager API overhaul.
        // For now, protect this call with the MODIFY_PHONE_STATE permission.)
        enforceModifyPermission();
        sendRequestAsync(CMD_SILENCE_RINGER);
    }

    /**
     * Internal implemenation of silenceRinger().
     * This should only be called from the main thread of the Phone app.
     * @see silenceRinger
     */
    private void silenceRingerInternal() {
        if ((mPhone.getState() == Phone.State.RINGING)
            && mApp.notifier.isRinging()) {
            // Ringer is actually playing, so silence it.
            if (DBG) log("silenceRingerInternal: silencing...");
            PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_IDLE);
            mApp.notifier.silenceRinger();
        }
    }

    public boolean isOffhook() {
        return (mPhone.getState() == Phone.State.OFFHOOK);
    }

    public boolean isRinging() {
        return (mPhone.getState() == Phone.State.RINGING);
    }


	
    public boolean isIdle() {
        return (mPhone.getState() == Phone.State.IDLE);
    }

    public boolean isSimPinEnabled() {
        enforceReadPermission();
        return (PhoneApp.getInstance().isSimPinEnabled());
    }

// 20100427 euikuk.jeong@lge.com Porting LGE patch <For PUK1 handling> from Aloha [START_LGE]
    public boolean supplyPuk(String puk, String newPin) {
        enforceModifyPermission();
        final CheckSimPuk checkSimPuk = new CheckSimPuk(mPhone.getIccCard());
        checkSimPuk.start();
        return checkSimPuk.checkPuk(puk, newPin);
    }

    /**
     * Helper thread to turn async call to {@link SimCard#supplyPuk} into
     * a synchronous one.
     */
    private static class CheckSimPuk extends Thread {

        private final IccCard mSimCard;

        private boolean mDone = false;
        private boolean mResult = false;

        // For replies from SimCard interface
        private Handler mHandler;

        // For async handler to identify request type
        private static final int SUPPLY_PUK_COMPLETE = 200;

        public CheckSimPuk(IccCard simCard) {
            mSimCard = simCard;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (CheckSimPuk.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case SUPPLY_PUK_COMPLETE:
                                Log.d(LOG_TAG, "SUPPLY_PUK_COMPLETE");
                                synchronized (CheckSimPuk.this) {
                                    mResult = (ar.exception == null);
                                    mDone = true;
                                    CheckSimPuk.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                CheckSimPuk.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized boolean checkPuk(String puk, String newPin) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Message callback = Message.obtain(mHandler, SUPPLY_PUK_COMPLETE);

            mSimCard.supplyPuk(puk, newPin, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            return mResult;
        }
    }
// 20100427 euikuk.jeong@lge.com Porting LGE patch <For PUK1 handling> from Aloha [END_LGE]

    public boolean supplyPin(String pin) {
        enforceModifyPermission();
        final CheckSimPin checkSimPin = new CheckSimPin(mPhone.getIccCard());
        checkSimPin.start();
        return checkSimPin.checkPin(pin);
    }

    /**
     * Helper thread to turn async call to {@link SimCard#supplyPin} into
     * a synchronous one.
     */
    private static class CheckSimPin extends Thread {

        private final IccCard mSimCard;

        private boolean mDone = false;
        private boolean mResult = false;

        // For replies from SimCard interface
        private Handler mHandler;

        // For async handler to identify request type
        private static final int SUPPLY_PIN_COMPLETE = 100;

        public CheckSimPin(IccCard simCard) {
            mSimCard = simCard;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (CheckSimPin.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case SUPPLY_PIN_COMPLETE:
                                Log.d(LOG_TAG, "SUPPLY_PIN_COMPLETE");
                                synchronized (CheckSimPin.this) {
                                    mResult = (ar.exception == null);
                                    mDone = true;
                                    CheckSimPin.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                CheckSimPin.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized boolean checkPin(String pin) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Message callback = Message.obtain(mHandler, SUPPLY_PIN_COMPLETE);

            mSimCard.supplyPin(pin, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            return mResult;
        }
    }

    public void updateServiceLocation() {
        // No permission check needed here: this call is harmless, and it's
        // needed for the ServiceState.requestStateUpdate() call (which is
        // already intentionally exposed to 3rd parties.)
        mPhone.updateServiceLocation();
    }

    public boolean isRadioOn() {
        return mPhone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
    }

    public void toggleRadioOnOff() {
        enforceModifyPermission();
        mPhone.setRadioPower(!isRadioOn());
    }
    public boolean setRadio(boolean turnOn) {
        enforceModifyPermission();
        if ((mPhone.getServiceState().getState() != ServiceState.STATE_POWER_OFF) != turnOn) {
            toggleRadioOnOff();
        }
        return true;
    }

    public boolean enableDataConnectivity() {
        enforceModifyPermission();
        return mPhone.enableDataConnectivity();
    }

    public int enableApnType(String type) {
        enforceModifyPermission();
        return mPhone.enableApnType(type);
    }

    public int disableApnType(String type) {
        enforceModifyPermission();
        return mPhone.disableApnType(type);
    }

// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
//LGE_MPDP START
    public int enableDataConnection(DataConnectionProfile profile, int featureId) {
        enforceModifyPermission();
        return mPhone.enableDataConnection(profile, featureId);
    }

    public int disableDataConnection(int featureId) {
        enforceModifyPermission();
        return mPhone.disableDataConnection(featureId);
    }

    public int modifyDataConnection(int featureId, DataConnectionProfile profile) {
        enforceModifyPermission();
        return mPhone.modifyDataConnection(featureId, profile);
    }

    public boolean isApnTypeAvailable(String type) {
        return mPhone.isApnTypeAvailable(type);
    }
//LGE_MPDP END
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]

// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
    public boolean isDataConnectionAvailable(DataConnectionProfile profile) {
        return mPhone.isDataConnectionAvailable(profile);
    }
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]

    public boolean disableDataConnectivity() {
        enforceModifyPermission();
        return mPhone.disableDataConnectivity();
    }

// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
//LGE_MPDP START
    public boolean isDataConnectivityPossible() {
        return mPhone.isDataConnectivityPossible(null);
    }
//LGE_MPDP END
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]

    public boolean handlePinMmi(String dialString) {
        enforceModifyPermission();
        return (Boolean) sendRequest(CMD_HANDLE_PIN_MMI, dialString);
    }

    public void cancelMissedCallsNotification() {
        enforceModifyPermission();
        NotificationMgr.getDefault().cancelMissedCallNotification();
    }

    public int getCallState() {
        return DefaultPhoneNotifier.convertCallState(mPhone.getState());
    }

    public int getDataState() {
        return DefaultPhoneNotifier.convertDataState(mPhone.getDataConnectionState());
    }

    public int getDataActivity() {
        return DefaultPhoneNotifier.convertDataActivityState(mPhone.getDataActivityState());
    }

    public Bundle getCellLocation() {
        try {
            mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_FINE_LOCATION, null);
        } catch (SecurityException e) {
            // If we have ACCESS_FINE_LOCATION permission, skip the check for ACCESS_COARSE_LOCATION
            // A failure should throw the SecurityException from ACCESS_COARSE_LOCATION since this
            // is the weaker precondition
            mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_COARSE_LOCATION, null);
        }
        Bundle data = new Bundle();
        mPhone.getCellLocation().fillInNotifierBundle(data);
        return data;
    }

    public void enableLocationUpdates() {
        mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.CONTROL_LOCATION_UPDATES, null);
        mPhone.enableLocationUpdates();
    }

    public void disableLocationUpdates() {
        mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.CONTROL_LOCATION_UPDATES, null);
        mPhone.disableLocationUpdates();
    }

    @SuppressWarnings("unchecked")
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        try {
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION, null);
        } catch (SecurityException e) {
            // If we have ACCESS_FINE_LOCATION permission, skip the check
            // for ACCESS_COARSE_LOCATION
            // A failure should throw the SecurityException from
            // ACCESS_COARSE_LOCATION since this is the weaker precondition
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION, null);
        }

        ArrayList<NeighboringCellInfo> cells = null;

        try {
            cells = (ArrayList<NeighboringCellInfo>) sendRequest(
                    CMD_HANDLE_NEIGHBORING_CELL, null);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "getNeighboringCellInfo " + e);
        }

        return (List <NeighboringCellInfo>) cells;
    }


    //
    // Internal helper methods.
    //

    /**
     * Make sure the caller has the READ_PHONE_STATE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceReadPermission() {
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.READ_PHONE_STATE, null);
    }

    /**
     * Make sure the caller has the MODIFY_PHONE_STATE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceModifyPermission() {
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE, null);
    }

    /**
     * Make sure the caller has the CALL_PHONE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceCallPermission() {
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.CALL_PHONE, null);
    }


    private String createTelUrl(String number) {
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        StringBuilder buf = new StringBuilder("tel:");
        buf.append(number);
        return buf.toString();
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[PhoneIntfMgr] " + msg);
    }

    public int getActivePhoneType() {
        return mPhone.getPhoneType();
    }

    /**
     * Returns the CDMA ERI icon index to display
     */
    public int getCdmaEriIconIndex() {
        return mPhone.getCdmaEriIconIndex();
    }

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    public int getCdmaEriIconMode() {
        return mPhone.getCdmaEriIconMode();
    }

    /**
     * Returns the CDMA ERI text,
     */
    public String getCdmaEriText() {
        return mPhone.getCdmaEriText();
    }

    /**
     * Returns true if CDMA provisioning needs to run.
     */
    public boolean getCdmaNeedsProvisioning() {
        if (getActivePhoneType() == Phone.PHONE_TYPE_GSM) {
            return false;
        }

        boolean needsProvisioning = false;
        String cdmaMin = mPhone.getCdmaMin();
        try {
            needsProvisioning = OtaUtils.needsActivation(cdmaMin);
        } catch (IllegalArgumentException e) {
            // shouldn't get here unless hardware is misconfigured
            Log.e(LOG_TAG, "CDMA MIN string " + ((cdmaMin == null) ? "was null" : "was too short"));
        }
        return needsProvisioning;
    }

    /**
     * Returns the unread count of voicemails
     */
    public int getVoiceMessageCount() {
        return mPhone.getVoiceMessageCount();
    }

    /**
     * Returns the network type
     */
    public int getNetworkType() {
        int radiotech = mPhone.getServiceState().getRadioTechnology();
        switch(radiotech) {
            case ServiceState.RADIO_TECHNOLOGY_GPRS:
                return TelephonyManager.NETWORK_TYPE_GPRS;
            case ServiceState.RADIO_TECHNOLOGY_EDGE:
                return TelephonyManager.NETWORK_TYPE_EDGE;
            case ServiceState.RADIO_TECHNOLOGY_UMTS:
                return TelephonyManager.NETWORK_TYPE_UMTS;
            case ServiceState.RADIO_TECHNOLOGY_HSDPA:
                return TelephonyManager.NETWORK_TYPE_HSDPA;
            case ServiceState.RADIO_TECHNOLOGY_HSUPA:
                return TelephonyManager.NETWORK_TYPE_HSUPA;
            case ServiceState.RADIO_TECHNOLOGY_HSPA:
                return TelephonyManager.NETWORK_TYPE_HSPA;
            case ServiceState.RADIO_TECHNOLOGY_IS95A:
            case ServiceState.RADIO_TECHNOLOGY_IS95B:
                return TelephonyManager.NETWORK_TYPE_CDMA;
            case ServiceState.RADIO_TECHNOLOGY_1xRTT:
                return TelephonyManager.NETWORK_TYPE_1xRTT;
            case ServiceState.RADIO_TECHNOLOGY_EVDO_0:
                return TelephonyManager.NETWORK_TYPE_EVDO_0;
            case ServiceState.RADIO_TECHNOLOGY_EVDO_A:
                return TelephonyManager.NETWORK_TYPE_EVDO_A;
            default:
                return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    /**
     * @return true if a ICC card is present
     */
    public boolean hasIccCard() {
        return mPhone.getIccCard().hasIccCard();
    }

// 20100507 euikuk.jeong@lge.com Porting LGE patch <Get all PIN status> from Aloha [START_LGE]
    public int getIccPin1RetryCount() {
        return mPhone.getIccCard().getIccPin1RetryCount();
    }
    public int getIccPin2RetryCount() {
        return mPhone.getIccCard().getIccPin2RetryCount();
    }
    public int getIccPuk1RetryCount() {
        return mPhone.getIccCard().getIccPuk1RetryCount();
    }
    public int getIccPuk2RetryCount() {
        return mPhone.getIccCard().getIccPuk2RetryCount();
    }
    public void updateIccPinStatus() {
        mPhone.getIccCard().updateIccPinStatus();
    }
// 20100507 euikuk.jeong@lge.com Porting LGE patch <Get all PIN status> from Aloha [END_LGE]

// 20100927 <hyunbin.shin@lge.com> LGE_IMS_FMC [START_LGE]
/**
     * Returns whether mobile call state is dialing or ringing.
     */
	public boolean isDialingOrRinging() {
		final boolean hasDialingCall = mPhone.getForegroundCall().isDialingOrAlerting();
        final boolean hasRingingCall = mPhone.getRingingCall().isRinging();
		
        return (hasDialingCall || hasRingingCall);
    }

	/**
	 * Returns whether mobile call state is active.
	 */
	public boolean isActive() {
		final boolean hasActiveCall = !mPhone.getForegroundCall().isIdle();
        final boolean hasBackgroundCall = !mPhone.getBackgroundCall().isIdle();
		
        return (hasActiveCall || hasBackgroundCall);
    }

	/**
	 * Returns whether bluetooth audio is connected or disconnected.
	 */
	public boolean isBluetoothAudioOn() {
		final boolean isAudioOn = mApp.getBluetoothHandsfree().isAudioOn();
		return isAudioOn;	
	}
// 20100927 <hyunbin.shin@lge.com> LGE_IMS_FMC [END_LGE]
        // LGE_RIL_SPEAKERPHONE_SUPPORT START
//LGE_TELECA_CR:746_TD:9427_RIL_SPEAKERPHONE_SUPPORT START
    public void setSpeakerphoneOn(int speakerphoneOn) {
        mPhone.setSpeakerphoneOn(speakerphoneOn);
//LGE_TELECA_CR:746_TD:9427_RIL_SPEAKERPHONE_SUPPORT END
    }
// LGE_RIL_SPEAKERPHONE_SUPPORT END
//20101014, hyeseung.ryu@lge.com, Mofidy to send ATCMDs for battery info via RIL [START]
    public void getBatteryInfo(int param) {
        mPhone.getBatteryInfo(param);
    }
//20101014, hyeseung.ryu@lge.com, Mofidy to send ATCMDs for battery info via RIL [END]



//LGE_VT  Start 
    public boolean isVideoCall() {
       return PhoneApp.mIsVideoCall;
    }
  
    public void videoDial(String number)
    {
        if (DBG) log("videoDial: " + number);

        // This is just a wrapper around the VIDEO_CALL_PRIVILEGED intent, but we still
        // need to do a permission check since we're calling startActivity()
        // from the context of the phone app.
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.VIDEO_CALL_PRIVILEGED, null);
        
        Intent intent = new Intent(Intent.ACTION_VIDEO_CALL_PRIVILEGED, Uri.fromParts("tel", number, null));
        if(number == null)
        {
            return;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mApp.startActivity(intent);
    }
//LGE_VT  end 


// 20101119 yongjin.her@lge.com SKT Q-Spider App support API for Infineon [START_LGE_LAB1]
	/**
     * start/stop the timer to unsol MobileQualityInformation to Telephony
     * This function is not declared in the SKT Q-Spider spec. 
     * CP(dehyun.kim@lge.com) requests this function, because the Infineon Modem has no AT cmd & need to implement.
     * But I don't think so.
     */
	public void startMobileQualityInformation()
    {
        //start
        mPhone.queryGprsCellEnvironmentDescription(1, mMainThreadHandler.obtainMessage(EVENT_SKT_Q_SPIDER_START)); 
        mPhone.registerGprsCellInfo(mMainThreadHandler,EVENT_SKT_Q_SPIDER_MOBILE_QUALITY_INFORMATION, null);

    }

	public void stopMobileQualityInformation()
    {
        //stop
        mPhone.queryGprsCellEnvironmentDescription(2, mMainThreadHandler.obtainMessage(EVENT_SKT_Q_SPIDER_STOP)); 
        mPhone.unregisterGprsCellInfo(mMainThreadHandler);
    }   
    
	/**
     * return SKT Q-Spider information
     */
	public String getMobileQualityInformation()
    {
        String mobileQualityInfo = null;

        log("getMobileQualityInformation() started");
        
        try {
            mobileQualityInfo = (String) sendRequest(
                    CMD_SKT_Q_SPIDER_MOBILE_QUALITY_INFORMATION, null);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "getMobileQualityInformation ex : " + e);
        }

        log("getMobileQualityInformation() mobileQualityInfo = "+mobileQualityInfo);
        
        return mobileQualityInfo;

    }   
// 20101119 yongjin.her@lge.com 2010-08-26, SKT Q-Spider App support API for Infineon [END_LGE_LAB1]

}
