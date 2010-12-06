package com.android.phone;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
// LGE_COLR START
import android.preference.CheckBoxPreference;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import com.android.internal.telephony.PhoneFactory;
// LGE_COLR END
// LGE_CUSTOMER_SERVICE_PROFILE START
import android.telephony.CspConstants;
// LGE_CUSTOMER_SERVICE_PROFILE END

import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandsInterface;

import java.util.ArrayList;


public class GsmUmtsCallForwardOptions extends TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "GsmUmtsCallForwardOptions";
    private final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    private static final String NUM_PROJECTION[] = {Phone.NUMBER};

    private static final String BUTTON_CFU_KEY   = "button_cfu_key";
    private static final String BUTTON_CFB_KEY   = "button_cfb_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";
// LGE_COLR START
    private static final String BUTTON_COLR_KEY   = "button_colr_key";
// LGE_COLR END

    private static final String KEY_TOGGLE = "toggle";
    private static final String KEY_STATUS = "status";
    private static final String KEY_NUMBER = "number";

// LGE_COLR START
    /** Event for COLR change */
    private static final int EVENT_COLR_EXECUTED = 100;
// LGE_COLR END
    private CallForwardEditPreference mButtonCFU;
    private CallForwardEditPreference mButtonCFB;
    private CallForwardEditPreference mButtonCFNRy;
    private CallForwardEditPreference mButtonCFNRc;
// LGE_COLR START
    private CheckBoxPreference mButtonCOLR;

    private com.android.internal.telephony.Phone mPhone;
// LGE_COLR END

    private final ArrayList<CallForwardEditPreference> mPreferences =
            new ArrayList<CallForwardEditPreference> ();
    private int mInitIndex= 0;

    private boolean mFirstResume;
    private Bundle mIcicle;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.callforward_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCFU   = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFU_KEY);
        mButtonCFB   = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFB_KEY);
        mButtonCFNRy = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRY_KEY);
        mButtonCFNRc = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRC_KEY);

        mButtonCFU.setParentActivity(this, mButtonCFU.reason);
        mButtonCFB.setParentActivity(this, mButtonCFB.reason);
        mButtonCFNRy.setParentActivity(this, mButtonCFNRy.reason);
        mButtonCFNRc.setParentActivity(this, mButtonCFNRc.reason);

        mPreferences.add(mButtonCFU);
        mPreferences.add(mButtonCFB);
        mPreferences.add(mButtonCFNRy);
        mPreferences.add(mButtonCFNRc);

        // we wait to do the initialization until onResume so that the
        // TimeConsumingPreferenceActivity dialog can display as it
        // relies on onResume / onPause to maintain its foreground state.

        mFirstResume = true;
        mIcicle = icicle;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFirstResume) {
            if (mIcicle == null) {
                if (DBG) Log.d(LOG_TAG, "start to init ");
                mPreferences.get(mInitIndex).init(this, false);
            } else {
                mInitIndex = mPreferences.size();

                for (CallForwardEditPreference pref : mPreferences) {
                    Bundle bundle = mIcicle.getParcelable(pref.getKey());
                    pref.setToggled(bundle.getBoolean(KEY_TOGGLE));
                    CallForwardInfo cf = new CallForwardInfo();
                    cf.number = bundle.getString(KEY_NUMBER);
                    cf.status = bundle.getInt(KEY_STATUS);
                    pref.handleCallForwardResult(cf);
                    pref.init(this, true);
                }
            }
            mFirstResume = false;
            mIcicle=null;
        }
        mPhone = PhoneFactory.getDefaultPhone();
/* 20100512 cutestar@lge.com  Applied LGE Menu. (deleted "Hide Forwarding ID")		
// LGE_COLR START
        mButtonCOLR = (CheckBoxPreference) prefSet.findPreference(BUTTON_COLR_KEY);
        // mPhone = PhoneFactory.getDefaultPhone();
        mPhone.getColr(Message.obtain(mGetOptionComplete, EVENT_COLR_EXECUTED));
        onStarted(mButtonCOLR, true);
// LGE_COLR END
*/
       
// LGE_CUSTOMER_SERVICE_PROFILE START
        checkCustomerServiceProfile();
// LGE_CUSTOMER_SERVICE_PROFILE END
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for (CallForwardEditPreference pref : mPreferences) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_TOGGLE, pref.isToggled());
            if (pref.callForwardInfo != null) {
                bundle.putString(KEY_NUMBER, pref.callForwardInfo.number);
                bundle.putInt(KEY_STATUS, pref.callForwardInfo.status);
            }
            outState.putParcelable(pref.getKey(), bundle);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            mPreferences.get(mInitIndex).init(this, false);
        }

        super.onFinished(preference, reading);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DBG) Log.d(LOG_TAG, "onActivityResult: done");
        if (resultCode != RESULT_OK) {
            if (DBG) Log.d(LOG_TAG, "onActivityResult: contact picker result not OK.");
            return;
        }
        Cursor cursor = getContentResolver().query(data.getData(),
                NUM_PROJECTION, null, null, null);
   
        try{
        	if ((cursor == null) || (!cursor.moveToFirst())) {
        		if (DBG) Log.d(LOG_TAG, "onActivityResult: bad contact data, no results found.");
        		return;
        	}
        	switch (requestCode) {
        	case CommandsInterface.CF_REASON_UNCONDITIONAL:
        		mButtonCFU.onPickActivityResult(cursor.getString(0));
        		break;
        	case CommandsInterface.CF_REASON_BUSY:	
        		mButtonCFB.onPickActivityResult(cursor.getString(0));
        		break;
        	case CommandsInterface.CF_REASON_NO_REPLY:
        		mButtonCFNRy.onPickActivityResult(cursor.getString(0));
        		break;
        	case CommandsInterface.CF_REASON_NOT_REACHABLE:
        		mButtonCFNRc.onPickActivityResult(cursor.getString(0));
        		break;
        	default:
        		// TODO: may need exception here.
        	}			
        	cursor.close();
        }finally{
        	cursor.close();
        }
    }

// LGE_CUSTOMER_SERVICE_PROFILE START
    private void checkCustomerServiceProfile() {
        mButtonCFU.setEnabled(mPhone.isServiceCustomerAccessible(CspConstants.GROUPCODE_CALL_OFFERING, CspConstants.SERVICE_CFU));
        mButtonCFB.setEnabled(mPhone.isServiceCustomerAccessible(CspConstants.GROUPCODE_CALL_OFFERING, CspConstants.SERVICE_CFB));
        mButtonCFNRy.setEnabled(mPhone.isServiceCustomerAccessible(CspConstants.GROUPCODE_CALL_OFFERING, CspConstants.SERVICE_CFNRY));
        mButtonCFNRc.setEnabled(mPhone.isServiceCustomerAccessible(CspConstants.GROUPCODE_CALL_OFFERING, CspConstants.SERVICE_CFNRC));
    }
// LGE_CUSTOMER_SERVICE_PROFILE END

/* 20100512 cutestar@lge.com  Applied LGE Menu. (deleted "Hide Forwarding ID")
// LGE_COLR START
    private Handler mGetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_COLR_EXECUTED:
                    handleGetCOLRMessage((AsyncResult) msg.obj);
                    break;
                default:
            }
        }
    };

    private void handleGetCOLRMessage(AsyncResult ar) {
        if (ar.exception != null) {
            if (DBG) log("handleGetCOLRMessage: Error getting COLR enable state.");
            onError(mButtonCOLR, EXCEPTION_ERROR);
        } else {
            if (DBG) log("handleGetCOLRMessage: COLR enable state successfully queried.");
            syncCOLRState((int[]) ar.result);
        }
    }

    private void syncCOLRState(int colrArray[]) {
        if (DBG) log("syncCOLRState: Setting UI state consistent with COLR enable state of " +
                ((colrArray[0] == 1) ? "ENABLED" : "DISABLED"));
        mButtonCOLR.setChecked(colrArray[0] == 1);
        onFinished(mButtonCOLR, true);
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
// LGE_COLR END
*/

}
