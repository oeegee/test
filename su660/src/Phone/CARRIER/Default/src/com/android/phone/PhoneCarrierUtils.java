// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-06 : Phone Carrier Utils

package com.android.phone;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import com.android.internal.telephony.PhoneFactory;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call

import com.android.internal.telephony.RAD.RADCarrierUtil;
import com.android.internal.telephony.RAD.RADCarrierUtilProxy;
import com.android.internal.telephony.RAD.RADCarrierUtilFactory;



public class PhoneCarrierUtils 
{
    private static final String LOG_TAG = "PhoneCarrierUtils";
    private static final boolean DBG = true;//(PhoneApp.DBG_LEVEL >= 2);


    private PhoneCarrierUtils(){
    }

    static boolean updateDisplayForPerson(CallerInfo info, int presentation, boolean isTemporary, Call call
    		, CallCard mCallCard, float mDensity, int mTextColorDefaultSecondary
    		, ImageView mPhoto, Button mManageConferencePhotoButton
    		, TextView mName, TextView mPhoneNumber, TextView mLabel, TextView mSocialStatus
    		, TextView mNameSecond, TextView mPhoneNumberSecond) {
        return false;
    }

    static String getPresentationString(Context context, int presentation) {
        return null;
    }

    static int getChangedCallLogType(int callLogType, final int presentation, final String number, final String cdnipNumber){
        if (DBG) log("getChangedCallLogType() : callLogType = " + callLogType);
        return callLogType;
    }

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-17 : SKT CallMeFree and MessageCall
    static String removeSymbol(int changedCallLogType, final String number){
        return number;
    }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-17 : SKT CallMeFree and MessageCall

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : display number
    static void setCallerInfoDisplayNumber(Connection conn, CallerInfo ci) {
        if (conn == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid Connection");
            return;
        }

        String connPhoneNumber = conn.getAddress();

        if (TextUtils.isEmpty(connPhoneNumber)) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid connection phone number");
            return;
        }

        if (ci == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid CallerInfo");
            return;
        }

        if (DBG) log("setCallerInfoDisplayNumber() phoneNumber : " + ci.phoneNumber + " , displayNumber : " + ci.displayNumber);
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : display number

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number
    static void processSpecialCenterNumber(Connection conn, CallerInfo ci) {
        if (conn == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid Connection");
            return;
        }

        String connPhoneNumber = conn.getAddress();

        if (TextUtils.isEmpty(connPhoneNumber)) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid connection phone number");
            return;
        }

        if (ci == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid CallerInfo");
            return;
        }

        String originalNumber = conn.getOriginalNumber();

        if (DBG) log("processSpecialCenterNumber() connPhoneNumber : " + connPhoneNumber + " , originalNumber : " + originalNumber + " , CallerInfo : " + ci);
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-07-06 : call : roaming : process number : call feature code
    static void processCallFeatureCode(Connection conn, CallerInfo ci) {
        if (conn == null) {
            if (DBG) log("processCallFeatureCode(): invalid Connection");
            return;
        }

        String connPhoneNumber = conn.getAddress();

        if (TextUtils.isEmpty(connPhoneNumber)) {
            if (DBG) log("processCallFeatureCode(): invalid connection phone number");
            return;
        }

        if (ci == null) {
            if (DBG) log("processCallFeatureCode(): invalid CallerInfo");
            return;
        }
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-07-06 : call : roaming : process number : call feature code


// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-17 : Reject Message
    static boolean sendRejectMsg(Activity activity, String phoneNumber, String rejectMsg) {
        return false; // true : need to setInCallScreenMode(NORMAL), false : not need to setInCallScreenMode(NORMAL)
    }

    static void editRejectMsg(Activity activity, String phoneNumber, String rejectMsg) {
    }

    static void newRejectMsg(Activity activity, String phoneNumber) {
    }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-14 : Reject Message

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-23 : not save call db
    static boolean getIsNoSaveCallDB(Phone phone) {
        return false;
    }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-23 : not save call db

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
    static String doServiceStatusCheck(Context context, Intent intent) {
        String msgReasonNotAble = null;

        if (context == null || intent == null) {
            if (DBG) log("doServiceStatusCheck() Invalid Parameter : " + context + ", " + intent);
            return msgReasonNotAble;
        }

        String action = intent.getAction();
        String phoneNumber;

        if (action == null) {
            phoneNumber = null;
        } else if (action.equals(Intent.ACTION_CALL) && intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        } else {
            phoneNumber = PhoneNumberUtils.getNumberFromIntent(intent, context);
        }

        ContentResolver contentResolver = context.getContentResolver();
        ServiceState curServiceState = PhoneFactory.getDefaultPhone().getServiceState();
        boolean isEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(phoneNumber);

        if (Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
            msgReasonNotAble = PhoneApp.getInstance().getResources().getString(R.string.FlightModeSetAsOn);
        } else if (curServiceState.getState() != ServiceState.STATE_IN_SERVICE) {
            if (curServiceState.getState() == ServiceState.STATE_POWER_OFF) {
                msgReasonNotAble = PhoneApp.getInstance().getResources().getString(R.string.ServiceRequired);
            } else if (isEmergencyNumber == false) {
                msgReasonNotAble = PhoneApp.getInstance().getResources().getString(R.string.NrcNoServiceArea);
            }
        }

        return msgReasonNotAble;
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call

// LGE_CHANGE_S [janghun.cheong@lge.com]	
    public static String formatIccPhoneNumberToDisplay(String aPhoneNumber){
        return aPhoneNumber;
    }
// LGE_CHANGE_E [janghun.cheong@lge.com]

///////////////////////////////////////////////////////////////////////////////////////////////////
// log
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}

// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-06 : Phone Carrier Utils
