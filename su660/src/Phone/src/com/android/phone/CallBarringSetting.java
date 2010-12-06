//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//ADD: 0001606: [Call] Implement of Call barring supplementary service 
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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
import com.android.internal.telephony.CallForwardInfo;
//END: 0001606  vegas80@lge.com  2009-09-18
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gsm.FacilityLock;		// 2010-02-23, CALL_BARRING_SWI, From Hub RIL

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Contacts.PhonesColumns;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import android.view.KeyEvent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;


public class CallBarringSetting extends PreferenceActivity
        implements DialogInterface.OnClickListener,
        EditPinPreference.OnPinEnteredListener,
        DialogInterface.OnCancelListener{

    // debug data
    private static final String LOG_TAG = "call barring settings";
    private static final boolean DBG = false;

    // String keys for preference lookup
    private static final String BUTTON_BAOC_KEY = "button_baoc_key";
    private static final String BUTTON_BOIC_KEY = "button_boic_key";
    private static final String BUTTON_BOICEXHC_KEY = "button_boicexhc_key";
    private static final String BUTTON_BAIC_KEY = "button_baic_key";
    private static final String BUTTON_BICROAM_KEY = "button_bicroam_key";
    private static final String BUTTON_CBPWD_KEY = "button_cbpwd_key";

    // events
    private static final int EVENT_SERVICE_STATE_CHANGED = 1100;
    private static final int EVENT_CB_EXECUTED = 1200;
    private static final int EVENT_CBPWD_CHANGE_COMPLETE = 1300;
    private static final int EVENT_INITAL_QUERY_CANCELED = 1400;

    private Phone mPhone;

    private static final int BUSY_DIALOG = 1100;
    private static final int EXCEPTION_ERROR = 1200;
    private static final int RESPONSE_ERROR = 1300;

    /** used to track errors with the radio off. */
    private static final int RADIO_OFF_ERROR = 1400;
    private static final int INITIAL_BUSY_DIALOG = 1500;

//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
    private static final int FDN_BLOCKED_ERROR = 10100;
//END: 0001606  vegas80@lge.com  2009-09-18


    // status message sent back from handlers
    private static final int MSG_OK = 1100;
    private static final int MSG_EXCEPTION = 1200;
    private static final int MSG_UNEXPECTED_RESPONSE = 1300;
    private static final int MSG_RADIO_OFF = 1400;
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
    private static final int MSG_FDN_BLOCKED = 10100;
//END: 0001606  vegas80@lge.com  2009-09-18

    static final String ACTION_ACTIVATE = "*";
    static final String ACTION_DEACTIVATE = "#";
    static final String ACTION_INTERROGATE = "*#";
    static final String ACTION_REGISTER = "**";
    static final String ACTION_ERASURE = "##";
    static final String SC_CLIP    = "30";
    static final String SC_CLIR    = "31";
    static final String SC_CFU     = "21";
    static final String SC_CFB     = "67";
    static final String SC_CFNRy   = "61";
    static final String SC_CFNR    = "62";
    static final String SC_CF_All = "002";
    static final String SC_CF_All_Conditional = "004";
    static final String SC_WAIT     = "43";
    static final String SC_BAOC         = "33";
    static final String SC_BAOIC        = "331";
    static final String SC_BAOICxH      = "332";
    static final String SC_BAIC         = "35";
    static final String SC_BAICr        = "351";
    static final String SC_BA_ALL       = "330";
    static final String SC_BA_MO        = "333";
    static final String SC_BA_MT        = "353";
    static final String SC_CNAP     = "300";
    static final String SC_PWD          = "03";
    static final String SC_PIN          = "04";
    static final String SC_PIN2         = "042";
    static final String SC_PUK          = "05";
    static final String SC_PUK2         = "052";


    // application states including network error state.
    // this includes seperate state for the inital query, which is cancelable.
    private enum AppState {
        INPUT_READY,
        DIALOG_OPEN,
        WAITING_NUMBER_SELECT,
        BUSY_NETWORK_CONNECT,
        NETWORK_ERROR,
        INITIAL_QUERY
    };
    private AppState mAppState;

    /** Additional state tracking to handle expanded views (lazy queries)*/
    private static final int DISP_MODE_CB = -1;
    private int mDisplayMode;
    private static boolean mCBDataStale = true;
    private boolean mIsBusyDialogAvailable = false;
	//20021105 jongwany.lee modified dialog popup [Strt]
    private boolean mIsInitialBusyDialogAvailable = false;
	//20021105 jongwany.lee modified dialog popup [End]

    // toggle buttons
    private EditPinPreference mButtonBAOC;
    private EditPinPreference mButtonBOIC;
    private EditPinPreference mButtonBOICEXHC;
    private EditPinPreference mButtonBAIC;
    private EditPinPreference mButtonBICROAM;
    private EditPinPreference mButtonCBPwd;

    // State variables
    private String mOldPwd;
    private String mNewPwd;
    private static final int PWD_CHANGE_OLD = 0;
    private static final int PWD_CHANGE_NEW = 1;
    private static final int PWD_CHANGE_REENTER = 2;
    private int mPwdChangeState;

    private static final String PWD_CHANGE_STATE_KEY = "pwd_change_state_key";
    private static final String OLD_PWD_KEY = "old_pwd_key";
    private static final String NEW_PWD_KEY = "new_pwd_key";
    private static final String DIALOG_MESSAGE_KEY = "dialog_message_key";
    private static final String DIALOG_PWD_ENTRY_KEY = "dialog_pwd_entry_key";
    private static final String APP_STATE_KEY     = "app_state_key";
    private static final String BUTTON_CB_EXPAND_KEY = "button_cb_expand_key";
    private static final String DISPLAY_MODE_KEY  = "display_mode_key";

    // size limits for the pin.
    private static final int CB_PWD_LENGTH = 4;

    // call barring is enabled/disabled
    private static boolean mIsActiveBAOC;
    private static boolean mIsActiveBOIC;
    private static boolean mIsActiveBOICxH;
    private static boolean mIsActiveBAIC;
    private static boolean mIsActiveBICr;

  
    static private CallBarringSetting mCBS;

    private Dialog mErrorAlertDialog;
    private Dialog mRadioOffErrorDialog;
    private Dialog mResponseErrorDialog;
    private boolean mLaunchAlertDialogAgain = false;
    private boolean mLaunchRadioOffErrorDialogAgain = false;
    private boolean mLaunchResponseErrorDialogAgain = false;
    IntentFilter dialogCloseFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

//BEGIN: 0002739  jihae.yi@lge.com  2009-12-24
//ADD: 0002739: [Call] DQ TD#58900: After dismiss a alarm ringing during going into Call fowrarding menu, ANR occurs rarely
    private boolean mIsCBCreated=false;	
//END: 0002739  jihae.yi@lge.com  2009-12-24 
   
    static public void setHandler(CallBarringSetting target)
    {
        mCBS = target;
    }

    static void resetCBDataStale()
    {
        mCBDataStale = true;
    }

    private final void displayPwdChangeDialog() {
        displayPwdChangeDialog(0, true);
    }

    private final void displayPwdChangeDialog(int strId, boolean shouldDisplay) {

        int msgId = -1;
        switch (mPwdChangeState) {
            case PWD_CHANGE_OLD:
                msgId = R.string.oldCBPwdLabel;
                break;
            case PWD_CHANGE_NEW:
                msgId = R.string.newCBPwdLabel;
                break;
            case PWD_CHANGE_REENTER:
                msgId = R.string.confirmCBPwdLabel;
                break;
        }

	//BEGIN: 0002433  jihae.yi@lge.com  2010-01-22
	//MOD: 0002433: [Call]WhiteBox bug fix for CallBarringSetting.java 
	//WBT fix TD#211649
	if (mButtonCBPwd != null){
        // append the note / additional message, if needed.
        if (strId != 0) {
            mButtonCBPwd.setDialogMessage(getText(msgId) + "\n" + getText(strId));
        } else {
            mButtonCBPwd.setDialogMessage(msgId);
        }
		
	//WBT fix TD#211649
	
	//WBT fix TD#240570
        // only display if requested.
        if (shouldDisplay) {
            mButtonCBPwd.showPinDialog();
        }
	}	
	//WBT fix TD#240570	
	//END: 0002433  jihae.yi@lge.com  2010-01-22	
    }

    private final void resetPwdChangeState() {
        mPwdChangeState = PWD_CHANGE_OLD;
        displayPwdChangeDialog(0, false);
        mOldPwd = mNewPwd = "";
    }

    private final void displayMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT)
            .show();
    }

    private boolean validateCBPwd(String password) {

        // check validity
        if (password == null || password.length() != CB_PWD_LENGTH) {
            return false;
        } else {
            return true;
        }
    }

//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//DEL: 0001606: [Call] Implement of Call barring supplementary service

/*
    boolean checkFdn(String mmi) {
//TODO: JIHAE, After FDN&SIM merged, below 2line must be enabeld
//        if (mPhone.checkFdn(mmi))
//            return true;
        setAppState(AppState.NETWORK_ERROR, MSG_FDN_BLOCKED);

        Log.v("nodebug","CFS>FDNblocked");

        return false;
    }
    */
//END: 0001606  vegas80@lge.com  2009-09-18


/*
    String buildMmiSetCB(int action, int reason, String password) {

        String mmi = null;
        String activate = ACTION_DEACTIVATE;
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
//TODO: JIHAE, After ALS merged, below line must be enabled
//        String bs = (mPhone.getCurrentVoiceClass() == CommandsInterface.SERVICE_CLASS_AUX) ? "*89" : "*11";
        String bs = "*11";
//END: 0001606  vegas80@lge.com  2009-09-18

        if (action == CommandsInterface.CF_ACTION_REGISTRATION) {
            activate = ACTION_REGISTER;
        }

        if (reason == CommandsInterface.CB_REASON_BAOC) {
            mmi = activate + SC_BAOC + "*" + password + bs + "#";
        } else if (reason == CommandsInterface.CB_REASON_BAOIC) {
            mmi = activate + SC_BAOIC + "*" + password + bs + "#";
        } else if (reason == CommandsInterface.CB_REASON_BAOICxH) {
            mmi = activate + SC_BAOICxH + "*" + password + bs + "#";
        } else if (reason == CommandsInterface.CB_REASON_BAIC) {
            mmi = activate + SC_BAIC + "*" + password + bs + "#";
        } else if (reason == CommandsInterface.CB_REASON_BAICr) {
            mmi = activate + SC_BAICr + "*" + password + bs + "#";
        }
		
        Log.v("nodebug","CBS>buildMmiSetCB: " + mmi);

        return mmi;
    }
*/

    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {

        if(preference == mButtonCBPwd)
            updatePWDChangeState(positiveResult);
        else if((preference == mButtonBAOC) || (preference == mButtonBOIC) ||
			(preference == mButtonBOICEXHC) || (preference == mButtonBAIC) ||
			(preference == mButtonBICROAM))
            toggleCBEnable(preference, positiveResult);
    }


    static private Handler mCBPwdHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                // when changing the pin we need to pay attention to whether or not
                // the error requests a PUK (usually after too many incorrect tries)
                // Set the state accordingly.
                case EVENT_CBPWD_CHANGE_COMPLETE: {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
//                            CommandException ce = (CommandException) ar.exception;

                            // set the correct error message depending upon the state.
                            mCBS.setAppState(AppState.NETWORK_ERROR, MSG_EXCEPTION);

                            // Reset the state depending upon or knowledge of the PUK state.
                            mCBS.resetPwdChangeState();

                        } else {
                            // reset to normal behaviour on successful change.
                            mCBS.setAppState(AppState.INPUT_READY);
                            mCBS.displayMessage(R.string.CBpwd_changed);
                            mCBS.resetPwdChangeState();
                        }
                    }
                    break;
            }
        }
    };

    private void updatePWDChangeState(boolean positiveResult) {

        if (!positiveResult) {
            resetPwdChangeState();

            return;
        }

        // Progress through the dialog states, generally in this order:
        //   1. Enter old pin
        //   2. Enter new pin
        //   3. Re-Enter new pin
        // While handling any error conditions that may show up in between.
        // Also handle the PUK2 entry, if it is requested.
        //
        // In general, if any invalid entries are made, the dialog re-
        // appears with text to indicate what the issue is.
        switch (mPwdChangeState) {
            case PWD_CHANGE_OLD:
                mOldPwd = mButtonCBPwd.getText();
                mButtonCBPwd.setText("");
                // if the pin is not valid, display a message and reset the state.
                if (validateCBPwd (mOldPwd)) {
                    mPwdChangeState = PWD_CHANGE_NEW;
                    displayPwdChangeDialog();
                } else {
                    displayPwdChangeDialog(R.string.invalidCBPwd, true);
                }
                break;
            case PWD_CHANGE_NEW:
                mNewPwd = mButtonCBPwd.getText();
                mButtonCBPwd.setText("");                // if the new pin is not valid, display a message and reset the state.
                if (validateCBPwd (mNewPwd)) {
                    mPwdChangeState = PWD_CHANGE_REENTER;
                    displayPwdChangeDialog();
                } else {
                    displayPwdChangeDialog(R.string.invalidCBPwd, true);
                }
                break;
            case PWD_CHANGE_REENTER:
                // if the re-entered pin is not valid, display a message and reset the state.
                if (!mNewPwd.equals(mButtonCBPwd.getText())) {
                    mPwdChangeState = PWD_CHANGE_NEW;
                    mButtonCBPwd.setText("");
                    displayPwdChangeDialog(R.string.mismatchCBPwd, true);
                } else {
                    // If the PIN is valid, then we either submit the change PIN request or
                    // display the PUK2 dialog if we KNOW that we're PUK2 locked.
                    mButtonCBPwd.setText("");
                    Message onComplete = mCBPwdHandler.obtainMessage(EVENT_CBPWD_CHANGE_COMPLETE);
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//DEL: 0001606: [Call] Implement of Call barring supplementary service
//TODO: JIHAE, After FDN&SIM merged, below lines must be enabeld
/*

                    String mmi = "*03*330*" + mOldPwd + "*" + mNewPwd + "*" + mNewPwd +"#";
                    if(!checkFdn(mmi)) {
                        mCBDataStale = true;
                        return;
                    }

*/
//END: 0001606  vegas80@lge.com  2009-09-18

					// Swift
                    // mPhone.changeCallBarringPwd(mOldPwd, mNewPwd, onComplete);
					mPhone.setCallBarringPass(null, mOldPwd, mNewPwd, onComplete);

                    setAppState(AppState.BUSY_NETWORK_CONNECT);
                }
                break;
        }
    }

    private void toggleCBEnable(EditPinPreference epn, boolean positiveResult) {

        if (mAppState != AppState.DIALOG_OPEN) {
            return;
        } else if (!positiveResult) {
            // false is the cancel button.
            setAppState (AppState.INPUT_READY);
            return;
        }

        AppState nextState = AppState.INPUT_READY;

        int reason = 0;
        String facility = "";
        int action = -1;

        String password = epn.getText();

        if (validateCBPwd (password)) {

            if (epn == mButtonBAOC) {
                action = (!mIsActiveBAOC) ? CommandsInterface.CB_ACTION_ENABLE : CommandsInterface.CB_ACTION_DISABLE;
                nextState = AppState.BUSY_NETWORK_CONNECT;
                //reason = CommandsInterface.CB_REASON_BAOC;
                facility = CommandsInterface.CB_FACILITY_BAOC;
            } else if (epn == mButtonBOIC) {
                action = (!mIsActiveBOIC) ? CommandsInterface.CB_ACTION_ENABLE : CommandsInterface.CB_ACTION_DISABLE;
                nextState = AppState.BUSY_NETWORK_CONNECT;
                //reason = CommandsInterface.CB_REASON_BAOIC;
                facility = CommandsInterface.CB_FACILITY_BAOIC;
            } else if (epn == mButtonBOICEXHC) {
                action = (!mIsActiveBOICxH) ? CommandsInterface.CB_ACTION_ENABLE : CommandsInterface.CB_ACTION_DISABLE;
                nextState = AppState.BUSY_NETWORK_CONNECT;
                //reason = CommandsInterface.CB_REASON_BAOICxH;
                facility = CommandsInterface.CB_FACILITY_BAOICxH;
            } else if (epn == mButtonBAIC) {
                action = (!mIsActiveBAIC) ? CommandsInterface.CB_ACTION_ENABLE : CommandsInterface.CB_ACTION_DISABLE;
                nextState = AppState.BUSY_NETWORK_CONNECT;
                //reason = CommandsInterface.CB_REASON_BAIC;
                facility = CommandsInterface.CB_FACILITY_BAIC;
            } else if (epn == mButtonBICROAM) {
                action = (!mIsActiveBICr) ? CommandsInterface.CB_ACTION_ENABLE : CommandsInterface.CB_ACTION_DISABLE;
                nextState = AppState.BUSY_NETWORK_CONNECT;
                //reason = CommandsInterface.CB_REASON_BAICr;
                facility = CommandsInterface.CB_FACILITY_BAICr;
            }

            if (nextState == AppState.BUSY_NETWORK_CONNECT) {
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//DEL: 0001606: [Call] Implement of Call barring supplementary service
//TODO: JIHAE, After FDN&SIM merged, below lines must be enabeld
/*
                String mmi = buildMmiSetCB(action, reason, password);
                if(!checkFdn(mmi)) {
                    mCBDataStale = true;
                    epn.setText("");
                    return;
                }
*/
//END: 0001606  vegas80@lge.com  2009-09-18
                //handleCBBtnClickRequest(action, reason, facility, password);
                //handleCBBtnClickRequest(facility, password);
				handleCBBtnClickRequest(action, facility, password);	// 2010-03-28, cutestar@lge.com, added action

            }

            if (nextState != AppState.DIALOG_OPEN) {
                setAppState(nextState);
            }
        }
        else
        {
            displayMessage(R.string.invalidCBPwd);
        }
        epn.setText("");
    }

    public void onCancel(DialogInterface dialog) {
    }

    private void adjustCBbuttonState(EditPinPreference epn, boolean isActive) {
        
        if (epn == null) {
            return;
        }
        
        if (isActive) {
            epn.setSummary(R.string.sum_cb_enabled);
            epn.setDialogTitle(R.string.dialog_title_cb_enabled);
        } else {
            epn.setSummary(R.string.sum_cb_disabled);
            epn.setDialogTitle(R.string.dialog_title_cb_disabled);
        }
    }

/*	
    // set the state of the UI based on CW State
    private void syncCBUIState(int reason, int cbArray[]) {

//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//DEL: 0001606: [Call] Implement of Call barring supplementary service
        //Log.v("nodebug","CFS>syncCBUIState:" + reason + ":" + cbArray[0] + ":" + cbArray[1]);
//END: 0001606  vegas80@lge.com  2009-09-18

//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
//boolean   active = (cbArray[0] == 1) && ((mPhone.getCurrentVoiceClass() & cbArray[1]) != 0);
	boolean active;

	if(cbArray.length < 2)
	{
	// 2010-02-23, CALL_BARRING_SWI : Call Barring From Swift
// // BEGIN: 0002612 nsalty@lge.com 2009-12-17
// // MOD 0002612: [Call] ALS bug fix for call barring  
// 		//active = (cbArray[0] == 1) && (CommandsInterface.SERVICE_CLASS_VOICE  != 0);
// 		active = ((mPhone.getCurrentVoiceClass() & cbArray[0]) != 0);
// // END: 0002612 nsalty@lge.com 2009-12-17

		// 2010-02-23, CALL_BARRING_SWI : Call Barring From Swift
		active = (cbArray[0] == 1) && (CommandsInterface.SERVICE_CLASS_VOICE  != 0);
	}
	else
	{
	    // 2010-02-23, CALL_BARRING_SWI : Call Barring From Swift
		// active = (cbArray[0] == 1) && ((mPhone.getCurrentVoiceClass() & cbArray[1]) != 0);	
		active = (cbArray[0] == 1) && ((CommandsInterface.SERVICE_CLASS_VOICE & cbArray[1]) != 0);	
	}
//END: 0001606  vegas80@lge.com  2009-09-18


        switch (reason) {
            case CommandsInterface.CB_REASON_BAOC:
                adjustCBbuttonState(mButtonBAOC, active);
                mIsActiveBAOC = active;
                mButtonBOIC.setEnabled((mIsActiveBAOC == false));
                mButtonBOICEXHC.setEnabled((mIsActiveBAOC == false));           
                break;
            case CommandsInterface.CB_REASON_BAOIC:
                adjustCBbuttonState(mButtonBOIC, active);
                mIsActiveBOIC = active;
                break;
            case CommandsInterface.CB_REASON_BAOICxH:
                adjustCBbuttonState(mButtonBOICEXHC, active);
                mIsActiveBOICxH = active;
                break;
            case CommandsInterface.CB_REASON_BAIC:
                adjustCBbuttonState(mButtonBAIC, active);
                mIsActiveBAIC = active;
                mButtonBICROAM.setEnabled((mIsActiveBAIC == false));
                break;
            case CommandsInterface.CB_REASON_BAICr:
                adjustCBbuttonState(mButtonBICROAM, active);
                mIsActiveBICr = active;
                break;
        }
    }

*/
	
	// 2010-02-23, CALL_BARRING_SWI, From Hub RIL
    private void syncCBUIState( String fac , FacilityLock info) {
       boolean active = (info.status == 1);

        if(fac.equals(CommandsInterface.CB_FACILITY_BAIC)){
        	if (DBG) log("syncCBUIState: Setting UI state consistent with IC.");

			adjustCBbuttonState(mButtonBAIC, active);
			mIsActiveBAIC = active;
			mButtonBICROAM.setEnabled((mIsActiveBAIC == false));

        }
		else if(fac.equals(CommandsInterface.CB_FACILITY_BAICr)){
        	if (DBG) log("syncCBUIState: Setting UI state consistent with ICr.");

			adjustCBbuttonState(mButtonBICROAM, active);
			mIsActiveBICr = active;

        }
		else if (fac.equals(CommandsInterface.CB_FACILITY_BAOC)){
        	if (DBG) log("syncCBUIState: Setting UI state consistent with OC.");

			adjustCBbuttonState(mButtonBAOC, active);
			mIsActiveBAOC = active;
			mButtonBOIC.setEnabled((mIsActiveBAOC == false));
			mButtonBOICEXHC.setEnabled((mIsActiveBAOC == false));  

        }
		else if (fac.equals(CommandsInterface.CB_FACILITY_BAOIC)){
        	if (DBG) log("syncCBUIState: Setting UI state consistent with OIC.");

			adjustCBbuttonState(mButtonBOIC, active);
			mIsActiveBOIC = active;

        }
		else if (fac.equals(CommandsInterface.CB_FACILITY_BAOICxH)){
        	if (DBG) log("syncCBUIState: Setting UI state consistent with OICxH.");

			adjustCBbuttonState(mButtonBOICEXHC, active);
			mIsActiveBOICxH = active;
        }

    }

    private void initCBUIState() {

 //BEGIN: 0002433  vegas80@lge.com  2009-12-10
 //MOD: 0002433: [Call]WhiteBox bug fix for CallBarringSetting.java 
	 //WBT fix TD#211651
	if (mButtonBAOC != null){
        mButtonBAOC.setEnabled(true);
        adjustCBbuttonState(mButtonBAOC, false);
        mIsActiveBAOC = false;
		}
	//WBT fix TD#211653
	if (mButtonBOIC != null){
        mButtonBOIC.setEnabled(true);
        adjustCBbuttonState(mButtonBOIC, false);
        mIsActiveBOIC = false;
		}
	//WBT fix TD#211654
	if (mButtonBOICEXHC != null){
        mButtonBOICEXHC.setEnabled(true);
        adjustCBbuttonState(mButtonBOICEXHC, false);
        mIsActiveBOICxH = false;
		}
	//WBT fix TD#211650
	if (mButtonBAIC != null){
        mButtonBAIC.setEnabled(true);
        adjustCBbuttonState(mButtonBAIC, false);
        mIsActiveBAIC = false;
		}
	//WBT fix TD#211652
	if (mButtonBICROAM != null){
        mButtonBICROAM.setEnabled(true);
        adjustCBbuttonState(mButtonBICROAM, false);
        mIsActiveBICr = false;
		}
 //END: 0002433  vegas80@lge.com  2009-12-10
    }

//  private int handleGetCBMessage(AsyncResult ar, int reason) {
	private int handleGetCBMessage(AsyncResult ar) {	// From Hub

        // done with query, display the new settings.
        if (ar.exception != null) {

			/* 검토 되어야 하는 상황. Hub에서는 "FDN_FAILURE" 정의 없음.  2010-02-23, CALL_BARRING_SWI : Call Barring From Swift
			//BEGIN: 0003647  jihae.yi@lge.com  2010-01-28
			//ADD: 0003647: [Call]QM TD # 628 Entering Call barring and Call FW are not barred when FDN enabled.
			if (((CommandException)ar.exception).getCommandError()
	                ==  CommandException.Error.FDN_FAILURE) {
	             if (DBG) log("[jihae]handleGetCBMessage: Error FDN blocked");
	             return MSG_FDN_BLOCKED;
				}
			//END: 0003647  jihae.yi@lge.com  2010-01-28
            */
			
            return MSG_EXCEPTION;
        } else if (ar.userObj instanceof Throwable) {
            // TODO: I don't think it makes sense to throw the error up to
            // the user, but this may be reconsidered.  For now, just log
            // the specific error and throw up a generic error.
            return MSG_UNEXPECTED_RESPONSE;
        } else {

// 2010-02-23, CALL_BARRING_SWI, From Hub RIL
/*        
            int InfoArray[] = (int[]) ar.result;
            if (InfoArray.length == 0) {
                return MSG_UNEXPECTED_RESPONSE;
            } else {
                // TODO: look through the information for the voice data
                // in reality, we should probably take the other service
                // classes into account, but this may be more than we
                // want to expose to the user.

                syncCBUIState(reason, InfoArray);
            }
*/			
			FacilityLock cbInfoArray[] = (FacilityLock[]) ar.result;
			if (cbInfoArray.length == 0) {
				if (DBG) log("handleGetCBMessage: Error getting CB state, unexpected value.");
				//showDialog(CB_RESPONSE_ERROR);
				//return false;
				return MSG_UNEXPECTED_RESPONSE;
			} else {
				for (int i = 0, length = cbInfoArray.length; i < length; i++) {
					if ((CommandsInterface.SERVICE_CLASS_VOICE &
							cbInfoArray[i].class_x) != 0) {
						if (DBG) {
							log("handleGetCBMessage: CB state successfully queried for status " +
								(String)ar.userObj);
							// Log.i("handleGetCBMessage","handleGetCBMessage : reason " + reason);
						}

						/*
						//<-- Just for Test
						if(ar.userObj == null){
						    
						    Log.i("handleGetCBMessage"," >>>> ar.userObj == null ");
						    return MSG_UNEXPECTED_RESPONSE;
						}
						// -->
						*/
						
						syncCBUIState((String)ar.userObj ,cbInfoArray[i]);
						break;
					}
				}
			}
        }

        return MSG_OK;
    }

    // Handler to track service availability.
    static private Handler mNetworkServiceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED: {
                        ServiceState state = (ServiceState) ((AsyncResult) msg.obj).result;
                        if (state.getState() == ServiceState.STATE_IN_SERVICE) {
                            // Query ONLY what we are interested in now.
                            switch (mCBS.mDisplayMode) {
                                case DISP_MODE_CB:
                                    mCBS.queryAllCBOptions();
                                    break;
                            }

                            mCBS.mPhone.unregisterForServiceStateChanged(mNetworkServiceHandler);
                        }
                    }
                    break;
                case EVENT_INITAL_QUERY_CANCELED:
                    mCBS.dismissExpandedDialog();
                    break;
            }
        }
    };

    // Request to begin querying for all options.
    private void queryAllCBOptions() {

        initCBUIState();

//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//DEL: 0001606: [Call] Implement of Call barring supplementary service
////TODO: JIHAE, After FDN&SIM merged, below lines must be enabeld
/*
        String mmi = ACTION_INTERROGATE + SC_BAOC + "#";
        if(!checkFdn(mmi)) return;
*/
//END: 0001606  vegas80@lge.com  2009-09-18

/*      //Swift
        mPhone.getCallBarring(CommandsInterface.CB_FACILITY_BAOC,
                Message.obtain(mGetAllCBOptionsComplete, EVENT_CB_EXECUTED,
                        CommandsInterface.CB_REASON_BAOC, 0));
*/
// Swift Style
//		 mPhone.getCallBarringOption(CommandsInterface.CB_FACILITY_BAOC,
//				  Message.obtain(mGetAllCBOptionsComplete, EVENT_CB_EXECUTED,
//						  CommandsInterface.CB_REASON_BAOC, 0 ));
        // From Hub RIL
		mPhone.getCallBarringOption(CommandsInterface.CB_FACILITY_BAOC,
				Message.obtain(mGetAllCBOptionsComplete, EVENT_CB_EXECUTED,
				CommandsInterface.CB_FACILITY_BAOC));
    }

    // callback after each step of querying for all options.

    private Handler mGetAllCBOptionsComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            AsyncResult ar = (AsyncResult) msg.obj;
			
            int status = MSG_OK;

            String para = null;

            switch (msg.what) {
                case EVENT_CB_EXECUTED:
                    //status = mCBS.handleGetCBMessage(ar, msg.arg1);
					status = mCBS.handleGetCBMessage(ar);	// From Hub

                    // Log.i("CallBarringSetting", "CallBarringSetting : msg.arg1[ " + msg.arg1 +"]"); 

                    // int nextReason = -1;
                    String nextFacility = "";
					String msgObj = (String)ar.userObj;
/*					
                    switch (msg.arg1) {
                        case CommandsInterface.CB_REASON_BAOC:
                            nextReason = CommandsInterface.CB_REASON_BAOIC;
                            nextFacility = CommandsInterface.CB_FACILITY_BAOIC;
                            para = SC_BAOIC;
                            break;
                        case CommandsInterface.CB_REASON_BAOIC:
                            nextReason = CommandsInterface.CB_REASON_BAOICxH;
                            nextFacility = CommandsInterface.CB_FACILITY_BAOICxH;
                            para = SC_BAOICxH;
                            break;
                        case CommandsInterface.CB_REASON_BAOICxH:
                            nextReason = CommandsInterface.CB_REASON_BAIC;
                            nextFacility = CommandsInterface.CB_FACILITY_BAIC;
                            para = SC_BAIC;
                            break;
                        case CommandsInterface.CB_REASON_BAIC:
                            nextReason = CommandsInterface.CB_REASON_BAICr;
                            nextFacility = CommandsInterface.CB_FACILITY_BAICr;
                            para = SC_BAICr;
                            break;
                        case CommandsInterface.CB_REASON_BAICr:
                            break;							
                        default:
                            // TODO: should never reach this, may want to throw exception

							//<--
							Log.i("CallBarringSetting", "CallBarringSetting : should never reach this, may want to throw exception");
							break;
							// -->
                    }
*/                    
					if (msgObj.equals(CommandsInterface.CB_FACILITY_BAOC)) {
							// nextReason = CommandsInterface.CB_REASON_BAOIC;
							nextFacility = CommandsInterface.CB_FACILITY_BAOIC;
							para = SC_BAOIC;
					}
					else if(msgObj.equals(CommandsInterface.CB_FACILITY_BAOIC)){
							// nextReason = CommandsInterface.CB_REASON_BAOICxH;
							nextFacility = CommandsInterface.CB_FACILITY_BAOICxH;
							para = SC_BAOICxH;
					}
					else if (msgObj.equals(CommandsInterface.CB_FACILITY_BAOICxH)){
							// nextReason = CommandsInterface.CB_REASON_BAIC;
							nextFacility = CommandsInterface.CB_FACILITY_BAIC;
							para = SC_BAIC;
					}
					else if (msgObj.equals(CommandsInterface.CB_FACILITY_BAIC)){
							// nextReason = CommandsInterface.CB_REASON_BAICr;
							nextFacility = CommandsInterface.CB_FACILITY_BAICr;
							para = SC_BAICr;
					}
					else if(msgObj.equals(CommandsInterface.CB_FACILITY_BAICr)){

					}

                    if (status != MSG_OK) {
                        mCBS.setAppState(AppState.NETWORK_ERROR, status);
                    } 
					else {
                        // if (nextReason != -1) {
                        if (!nextFacility.equals("")) {
// Swift Style
//							mCBS.mPhone.getCallBarringOption(nextFacility,
//									Message.obtain(mGetAllCBOptionsComplete, EVENT_CB_EXECUTED,
//											nextReason, 0));
                            // From Hub RIL
							mCBS.mPhone.getCallBarringOption(nextFacility,
							        Message.obtain(mGetAllCBOptionsComplete, EVENT_CB_EXECUTED,
							        nextFacility));

                        } else {
                            mCBS.mCBDataStale = false;
                            mCBS.setAppState(AppState.INPUT_READY);
                        }
                    }
                    break;

                default:
                    // TODO: should never reach this, may want to throw exception
                    break;
            }
        }
    };

//  private void handleSetCBMessage(int reason, AsyncResult r) {
	private void handleSetCBMessage(AsyncResult r) { 	// Hub

        // String facility = "";
		String facility= (String)r.userObj;
		
/* James, 0327
        if(reason == CommandsInterface.CB_REASON_BAOC)
            facility = CommandsInterface.CB_FACILITY_BAOC;
        else if(reason == CommandsInterface.CB_REASON_BAOIC)
            facility = CommandsInterface.CB_FACILITY_BAOIC;
        else if(reason == CommandsInterface.CB_REASON_BAOICxH)
            facility = CommandsInterface.CB_FACILITY_BAOICxH;
        else if(reason == CommandsInterface.CB_REASON_BAIC)
            facility = CommandsInterface.CB_FACILITY_BAIC;
        else if(reason == CommandsInterface.CB_REASON_BAICr)
            facility = CommandsInterface.CB_FACILITY_BAICr;
*/

/*
		// Swift
        mPhone.getCallBarring(facility,
                Message.obtain(mGetOptionComplete, EVENT_CB_EXECUTED, reason, 0, r.exception));
*/
	    mPhone.getCallBarringOption(facility,
				Message.obtain(mGetOptionComplete, EVENT_CB_EXECUTED, facility));	// Hub
    }

    //private void handleCBBtnClickRequest(int action, int reason, String facility, String password) {
    //private void handleCBBtnClickRequest(String facility, String password) {
	private void handleCBBtnClickRequest(int action, String facility, String password) {	// 2010-03-28, cutestar@lge.com, added action

        /*
        // Swift
		mPhone.setCallBarring(action,
                facility,
                password,
                Message.obtain(mSetOptionComplete, EVENT_CB_EXECUTED, reason, 0));
        */
         
		//mPhone.setCallBarringOption( facility, password, Message.obtain(mSetOptionComplete, EVENT_CB_EXECUTED, facility)); // Hub
		mPhone.setCallBarringOption( action, facility, password, Message.obtain(mSetOptionComplete, EVENT_CB_EXECUTED, facility)); // 2010-03-28, cutestar@lge.com, added action
    }

    public void setDisplayMode(int displayMode) {

        mDisplayMode = displayMode;

        // look for the data if it is considered stale.
        if (mCBDataStale && (displayMode == DISP_MODE_CB)){
            // If airplane mode is on, do not bother querying.
            if (Settings.System.getInt(getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) <= 0) {
                // query state if radio is available
                //  if its out of service, just wait for the radio to be ready
                //  if its neither of these states, throw up an error.
                setAppState(AppState.INITIAL_QUERY);

                int radioState = mPhone.getServiceState().getState();

                if (radioState == ServiceState.STATE_IN_SERVICE) {
                    // Query ONLY what we are currently expanding.
                    if (displayMode == DISP_MODE_CB) {

                        queryAllCBOptions();

                    }
                } else if (radioState == ServiceState.STATE_POWER_OFF){
                    mPhone.registerForServiceStateChanged(mNetworkServiceHandler,
                            EVENT_SERVICE_STATE_CHANGED, null);
                } else {
                    setAppState(AppState.NETWORK_ERROR, MSG_EXCEPTION);
                }
            } else {
                setAppState(AppState.NETWORK_ERROR, MSG_RADIO_OFF);
            }
        }
    }

    // **Callback on option setting when complete.
    static private Handler mSetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            // query to make sure we're looking at the same data as that in the network.
            switch (msg.what) {
                case EVENT_CB_EXECUTED:
                    //mCBS.handleSetCBMessage(msg.arg1, (AsyncResult) msg.obj);
                	AsyncResult asyncResult = (AsyncResult) msg.obj;
                	if(asyncResult.exception == null){
                		mCBS.handleSetCBMessage((AsyncResult) msg.obj);	// Hub
                	}else{
                		Log.e("Exception", "Exception : "+asyncResult.exception.toString(),asyncResult.exception);
                		  mCBS.setAppState(AppState.NETWORK_ERROR,MSG_UNEXPECTED_RESPONSE );
                	}
                    break;
                default:
                    // TODO: should never reach this, may want to throw exception
            }
        }
    };

    // **Callback on option getting when complete.
    static private Handler mGetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            boolean bHandled = false;
            int status = MSG_OK;
            switch (msg.what) {
                case EVENT_CB_EXECUTED:
                    //status = mCBS.handleGetCBMessage((AsyncResult) msg.obj, msg.arg1);
                    status = mCBS.handleGetCBMessage((AsyncResult) msg.obj);
                    bHandled = true;
                    break;
                default:
                    // TODO: should never reach this, may want to throw exception
            }
            if (status != MSG_OK) {
                mCBS.setAppState(AppState.NETWORK_ERROR, status);
            } else if (bHandled) {
                mCBS.setAppState(AppState.INPUT_READY);
            }
        }
    };

    // dialog creation method, called by showDialog()
    @Override
    protected Dialog onCreateDialog(int id) {

 
        if ((id == BUSY_DIALOG) || (id == INITIAL_BUSY_DIALOG)) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle(getText(R.string.updating_title));
            dialog.setIndeterminate(true);

            switch (id) {
                case BUSY_DIALOG:
                    dialog.setCancelable(false);
                    dialog.setMessage(getText(R.string.updating_settings));
                    break;
                case INITIAL_BUSY_DIALOG:
                    // Allowing the user to cancel on the initial query.
                    dialog.setCancelable(true);
                    dialog.setCancelMessage(
                            mNetworkServiceHandler.obtainMessage(EVENT_INITAL_QUERY_CANCELED));
                    dialog.setMessage(getText(R.string.reading_settings));
                    break;
            }
            // make the dialog more obvious by bluring the background.
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            return dialog;

        // Handle error dialog codes
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
//        } else if ((id == RESPONSE_ERROR) || (id == EXCEPTION_ERROR) || (id == RADIO_OFF_ERROR) || (id == FDN_BLOCK_ERROR)) {
        } else if ((id == RESPONSE_ERROR) || (id == EXCEPTION_ERROR) || (id == RADIO_OFF_ERROR) || (id == FDN_BLOCKED_ERROR)) {
//END: 0001606  vegas80@lge.com  2009-09-18


            AlertDialog.Builder b = new AlertDialog.Builder(this);


            int msgId;
            int titleId = R.string.error_updating_title;

            switch (id) {
                case RESPONSE_ERROR:
                    msgId = R.string.response_error;
                    // Set Button 2, tells the activity that the error is
                    // recoverable on dialog exit.
                    b.setNegativeButton(R.string.close_dialog, this);
                    break;
                case RADIO_OFF_ERROR:
                    msgId = R.string.radio_off_error;
                    // Set Button 3
                    b.setNeutralButton(R.string.close_dialog, this);
                    break;
//BEGIN: 0002522  vegas80@lge.com  2009-12-14
//MOD: 0002522: [Call] DQ TD# 57110 Regarding Call Barring password error popup  
//                case EXCEPTION_ERROR:
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
//                case FDN_BLOCK_ERROR:
                case FDN_BLOCKED_ERROR:
                    msgId = R.string.fdn_blocked_error;
                    // Set Button 3
                    b.setNeutralButton(R.string.close_dialog, this);
                    break;
//END: 0001606  vegas80@lge.com  2009-09-18
                case EXCEPTION_ERROR:
//END: 0002522  vegas80@lge.com  2009-12-14
                default:
                    msgId = R.string.exception_error;
                    // Set Button 3, tells the activity that the error is
                    // not recoverable on dialog exit.
                    b.setNeutralButton(R.string.close_dialog, this);
                    break;
            }
            b.setTitle(getText(titleId));
            b.setMessage(getText(msgId));
            b.setCancelable(false);
            AlertDialog dialog = b.create();

            // make the dialog more obvious by bluring the background.
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            return dialog;
        }

        return null;
    }

    // This is a method implemented for DialogInterface.OnClickListener.
    // Used with the error dialog to close the app, voicemail dialog to just dismiss.
    // Close button is mapped to BUTTON1 for the errors that close the activity,
    // while those that are mapped to 3 only move the preference focus.
    public void onClick(DialogInterface dialog, int which) {

        dialog.dismiss();
        switch (which){
            case DialogInterface.BUTTON3:
                // Neutral Button, used when we want to cancel expansion.
                dismissExpandedDialog();
                break;
            case DialogInterface.BUTTON1:
                // Negative Button
                finish();
                break;
            default:
                // just let the dialog close and go back to the input
                // ready state
                setAppState (AppState.INPUT_READY);
                // Positive Button
        }
    }


    private void dismissExpandedDialog() {
        PreferenceScreen prefSet = getPreferenceScreen();

        switch (mDisplayMode) {
            case DISP_MODE_CB:
                if (prefSet != null && prefSet.getDialog() != null) {
                    prefSet.getDialog().dismiss();
                }
                break;
        }

        finish();
    }

    // set the app state when no error message needs to be set.
    private void setAppState(AppState requestedState) throws IllegalStateException{

        if (requestedState == AppState.NETWORK_ERROR) {
            throw new IllegalStateException ("illegal error state without reason.");
        }
        setAppState (requestedState, MSG_OK);
    }

    // set the app state with optional status.
    private void setAppState(AppState requestedState, int msgStatus)
            throws IllegalStateException{

        if (requestedState == mAppState) {
            return;
        }

        // handle errors
        // make sure we dismiss the correct dialogs.
        if (requestedState == AppState.NETWORK_ERROR) {
            switch (msgStatus) {
                case MSG_EXCEPTION:
                    if (mAppState == AppState.INITIAL_QUERY) {
                	//20021105 jongwany.lee modified dialog popup [Strt]
                    	dismissBusyDialodID(INITIAL_BUSY_DIALOG);
                	//20021105 jongwany.lee modified dialog popup [End]
                    } else {
                    	dismissBusyDialodID(BUSY_DIALOG);
                	//20021105 jongwany.lee modified dialog popup [End]
                    }

                    if(mErrorAlertDialog == null)
                       mErrorAlertDialog = onCreateDialog(EXCEPTION_ERROR);
//BEGIN: 0002739  jihae.yi@lge.com  2009-12-24
//ADD: 0002739: [Call] DQ TD#58900: After dismiss a alarm ringing during going into Call fowrarding menu, ANR occurs rarely
			if(mIsCBCreated && mErrorAlertDialog != null)
//END: 0002739  jihae.yi@lge.com  2009-12-24 
                       mErrorAlertDialog.show();
                    break;
                case MSG_RADIO_OFF:

                    if(mRadioOffErrorDialog == null)
                       mRadioOffErrorDialog = onCreateDialog(RADIO_OFF_ERROR);
//BEGIN: 0002739  jihae.yi@lge.com  2009-12-24
//ADD: 0002739: [Call] DQ TD#58900: After dismiss a alarm ringing during going into Call fowrarding menu, ANR occurs rarely
		      if(mIsCBCreated && mRadioOffErrorDialog != null)		
//END: 0002739  jihae.yi@lge.com  2009-12-24 
                       mRadioOffErrorDialog.show();
                    break;
                case MSG_UNEXPECTED_RESPONSE:
                    if (mAppState == AppState.INITIAL_QUERY) {
                	//20021105 jongwany.lee modified dialog popup [Strt]
                    	dismissBusyDialodID(INITIAL_BUSY_DIALOG);
                    } else {
                    	dismissBusyDialodID(BUSY_DIALOG);
                	//20021105 jongwany.lee modified dialog popup [End]
                    }

                    if(mResponseErrorDialog == null)
                        mResponseErrorDialog = onCreateDialog(RESPONSE_ERROR);
//BEGIN: 0002739  jihae.yi@lge.com  2009-12-24
//ADD: 0002739: [Call] DQ TD#58900: After dismiss a alarm ringing during going into Call fowrarding menu, ANR occurs rarely
			if(mIsCBCreated && mResponseErrorDialog != null)		
//END: 0002739  jihae.yi@lge.com  2009-12-24 
                 	   mResponseErrorDialog.show();
                    break;
//BEGIN: 0001606  vegas80@lge.com  2009-09-18
//MOD: 0001606: [Call] Implement of Call barring supplementary service
//                case MSG_FDN_BLOCK:
                case MSG_FDN_BLOCKED:
//END: 0001606  vegas80@lge.com  2009-09-18
                    if (mAppState == AppState.INITIAL_QUERY) {
                	//20021105 jongwany.lee modified dialog popup [Strt]
                    	dismissBusyDialodID(INITIAL_BUSY_DIALOG);
                    } else {
                    	dismissBusyDialodID(BUSY_DIALOG);
                	//20021105 jongwany.lee modified dialog popup [End]
                    }
                    if(mErrorAlertDialog == null)
                       mErrorAlertDialog = onCreateDialog(FDN_BLOCKED_ERROR);
//BEGIN: 0002739  jihae.yi@lge.com  2009-12-24
//ADD: 0002739: [Call] DQ TD#58900: After dismiss a alarm ringing during going into Call fowrarding menu, ANR occurs rarely
		      if(mIsCBCreated && mErrorAlertDialog != null)		
//END: 0002739  jihae.yi@lge.com  2009-12-24 
                       mErrorAlertDialog.show();
                    break;
                case MSG_OK:
                default:
                    // This should never happen.
            }
            mAppState = requestedState;
            return;
        }

        switch (mAppState) {
            // We can now transition out of the NETWORK_ERROR state, when the
            // user is moving from the expanded views back to the main view.
            case NETWORK_ERROR:
                if (requestedState != AppState.INPUT_READY) {
                    throw new IllegalStateException
                            ("illegal transition from NETWORK_ERROR");
                }
                break;
            case INPUT_READY:
                if (requestedState == AppState.INITIAL_QUERY) {
                	//20021105 jongwany.lee modified dialog popup [Strt]
                	showDialodID(INITIAL_BUSY_DIALOG);
                	//20021105 jongwany.lee modified dialog popup [End]
                } else if (requestedState == AppState.BUSY_NETWORK_CONNECT) {
                	//20021105 jongwany.lee modified dialog popup [Strt]
                	showDialodID(BUSY_DIALOG);
                	//20021105 jongwany.lee modified dialog popup [End]
                } else if (requestedState == AppState.WAITING_NUMBER_SELECT) {
                    throw new IllegalStateException
                            ("illegal transition from INPUT_READY");
                }
                break;
            case DIALOG_OPEN:
                if (requestedState == AppState.INPUT_READY) {
                } else {
                	//20021105 jongwany.lee modified dialog popup [Strt]
                	showDialodID(BUSY_DIALOG);
                	//20021105 jongwany.lee modified dialog popup [End]
                }
                break;
            case INITIAL_QUERY:
                // the initial query state can ONLY go to the input ready state.
                if (requestedState != AppState.INPUT_READY) {
                    throw new IllegalStateException
                            ("illegal transition from INITIAL_QUERY");
                }
                 //20021105 jongwany.lee modified dialog popup [Strt]
                dismissBusyDialodID(INITIAL_BUSY_DIALOG);
            	//20021105 jongwany.lee modified dialog popup [End]
                break;
            case BUSY_NETWORK_CONNECT:
                if (requestedState != AppState.INPUT_READY) {
                    throw new IllegalStateException
                            ("illegal transition from BUSY_NETWORK_CONNECT");
                }
                 //20021105 jongwany.lee modified dialog popup [Strt]
                dismissBusyDialodID(BUSY_DIALOG);
            	//20021105 jongwany.lee modified dialog popup [End]
                break;
            case WAITING_NUMBER_SELECT:
                if (requestedState != AppState.DIALOG_OPEN) {
                    throw new IllegalStateException
                            ("illegal transition from WAITING_NUMBER_SELECT");
                }
                 //20021105 jongwany.lee modified dialog popup [Strt]
                dismissBusyDialodID(BUSY_DIALOG);
            	//20021105 jongwany.lee modified dialog popup [End]
                break;
        }
        mAppState = requestedState;
    }
    
	//20021105 jongwany.lee modified dialog popup [Strt]
	private final void dismissBusyDialodID(int dialogId) {
		switch (dialogId) {
		case BUSY_DIALOG:
			if (mIsBusyDialogAvailable) {
				try {
					mIsBusyDialogAvailable = false;
					dismissDialog(BUSY_DIALOG);
				} catch (Exception e) {
			        Log.e(LOG_TAG,"BUSY_DIALOG FLAG SYNC ERROR "+e.toString());
				}
			}
			break;
		case INITIAL_BUSY_DIALOG:
			if (mIsInitialBusyDialogAvailable) {
				try {
					mIsInitialBusyDialogAvailable = false;
					dismissDialog(INITIAL_BUSY_DIALOG);
				} catch (Exception e) {
					Log.e(LOG_TAG,"INITIAL_BUSY_DIALOG FLAG SYNC ERROR "+e.toString());
				}
			}
			break;
		}
	}

	private final void showDialodID(int dialogId) {
		switch (dialogId) {
		case BUSY_DIALOG:
			showDialog(BUSY_DIALOG);
			mIsBusyDialogAvailable = true;
			break;
		case INITIAL_BUSY_DIALOG:
			showDialog(INITIAL_BUSY_DIALOG);
			mIsInitialBusyDialogAvailable = true;
			break;
		}
	}
    //20021105 jongwany.lee modified dialog popup [End]        	

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (mAppState != AppState.INPUT_READY) {
            return false;
        }

        AppState nextState = AppState.INPUT_READY;

        if (preference == mButtonCBPwd) {
            // no action
            return false;
        }
        else if ((preference instanceof EditPinPreference) &&
			((preference == mButtonBAOC) || (preference == mButtonBOIC) ||
			(preference == mButtonBOICEXHC) || (preference == mButtonBAIC) ||
			(preference == mButtonBICROAM))) {

            nextState = AppState.DIALOG_OPEN;
        }

        if (nextState != AppState.INPUT_READY) {

            setAppState(nextState);
            return true;
        }

        return false;
    }

    /*
     * Activity class methods
     */
//BEGIN: Call barring udpate vegas80@lge.com
    protected void onStart() {
        super.onStart();
        // receive broadcasts
        registerReceiver(mBroadcastReceiver, dialogCloseFilter);
    }
//END: Call barring udpate vegas80@lge.com
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.call_barring_setting);
        mPhone = PhoneFactory.getDefaultPhone();
		//BEGIN: 0002739  jihae.yi@lge.com  2009-12-24
		//ADD: 0002739: [Call] DQ TD#58900: After dismiss a alarm ringing during going into Call fowrarding menu, ANR occurs rarely
		mIsCBCreated = true;//jihae	
		//END: 0002739  jihae.yi@lge.com  2009-12-24 
        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
/*        
        mButtonBAOC = (EditPinPreference) prefSet.findPreference(BUTTON_BAOC_KEY);
        mButtonBOIC = (EditPinPreference) prefSet.findPreference(BUTTON_BOIC_KEY);
        mButtonBOICEXHC = (EditPinPreference) prefSet.findPreference(BUTTON_BOICEXHC_KEY);
        mButtonBAIC = (EditPinPreference) prefSet.findPreference(BUTTON_BAIC_KEY);
        mButtonBICROAM = (EditPinPreference) prefSet.findPreference(BUTTON_BICROAM_KEY);
        mButtonCBPwd = (EditPinPreference) prefSet.findPreference(BUTTON_CBPWD_KEY);
        //assign click listener and update state
*/
        mButtonBAOC = (EditPinPreference) findPreference(BUTTON_BAOC_KEY);
        mButtonBOIC = (EditPinPreference) findPreference(BUTTON_BOIC_KEY);
        mButtonBOICEXHC = (EditPinPreference) findPreference(BUTTON_BOICEXHC_KEY);
        mButtonBAIC = (EditPinPreference) findPreference(BUTTON_BAIC_KEY);
        mButtonBICROAM = (EditPinPreference) findPreference(BUTTON_BICROAM_KEY);
        mButtonCBPwd = (EditPinPreference) findPreference(BUTTON_CBPWD_KEY);


        if (mButtonBAOC != null) { 
		  	mButtonBAOC.setOnPinEnteredListener(this);
			mButtonBAOC.setEnabled(true);
        }

        if (mButtonBOIC != null) {  
            mButtonBOIC.setOnPinEnteredListener(this);
            mButtonBOIC.setEnabled((mIsActiveBAOC == false));
        }

        if (mButtonBOICEXHC != null) {  
            mButtonBOICEXHC.setOnPinEnteredListener(this);
            mButtonBOICEXHC.setEnabled((mIsActiveBAOC == false));
        }

        if (mButtonBAIC != null) {   
            mButtonBAIC.setOnPinEnteredListener(this);
            mButtonBAIC.setEnabled(true);
        }

        if (mButtonBICROAM != null) {   
            mButtonBICROAM.setOnPinEnteredListener(this);
            mButtonBICROAM.setEnabled((mIsActiveBAIC == false));
        }

        if (mButtonCBPwd != null) {   
            mButtonCBPwd.setOnPinEnteredListener(this);
            mButtonCBPwd.setEnabled(true);
        }
        mAppState = AppState.INPUT_READY;
        setHandler(this);
        if (icicle == null)
        {
            resetPwdChangeState();
            setDisplayMode(DISP_MODE_CB);
        }
	 	else
        {
            mPwdChangeState = icicle.getInt(PWD_CHANGE_STATE_KEY);
            mOldPwd = icicle.getString(OLD_PWD_KEY);
            mNewPwd = icicle.getString(NEW_PWD_KEY);

            if (mButtonCBPwd != null) {   
            	mButtonCBPwd.setDialogMessage(icicle.getString(DIALOG_MESSAGE_KEY));
              mButtonCBPwd.setText(icicle.getString(DIALOG_PWD_ENTRY_KEY));
            }
            if (mButtonBAOC != null)
                adjustCBbuttonState(mButtonBAOC, mIsActiveBAOC);
            if (mButtonBOIC != null)
                adjustCBbuttonState(mButtonBOIC, mIsActiveBOIC);
            if (mButtonBOICEXHC != null)
                adjustCBbuttonState(mButtonBOICEXHC, mIsActiveBOICxH);
            if (mButtonBAIC != null)
                adjustCBbuttonState(mButtonBAIC, mIsActiveBAIC);
            if (mButtonBICROAM != null)
                adjustCBbuttonState(mButtonBICROAM, mIsActiveBICr);

            mAppState = (AppState) icicle.getSerializable(APP_STATE_KEY);
            mDisplayMode = icicle.getInt(DISPLAY_MODE_KEY);
        }
    }

    @Override
    protected void onStop() {
        if (DBG) log("onStop()...");
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
//BEGIN: 0002739  jihae.yi@lge.com  2009-12-24
//ADD: 0002739: [Call] DQ TD#58900: After dismiss a alarm ringing during going into Call fowrarding menu, ANR occurs rarely
	 mIsCBCreated = false;
	 mErrorAlertDialog = null;
	 mRadioOffErrorDialog = null;
	 mResponseErrorDialog = null;
//END: 0002739  jihae.yi@lge.com  2009-12-24

    }
    
    @Override
    protected void onResume() {
        if (DBG) log("onResume()...");
        super.onResume();
        
        if (mButtonBAOC != null)
            adjustCBbuttonState(mButtonBAOC, mIsActiveBAOC);
        if (mButtonBOIC != null)
            adjustCBbuttonState(mButtonBOIC, mIsActiveBOIC);
        if (mButtonBOICEXHC != null)
            adjustCBbuttonState(mButtonBOICEXHC, mIsActiveBOICxH);
        if (mButtonBAIC != null)
            adjustCBbuttonState(mButtonBAIC, mIsActiveBAIC);
        if (mButtonBICROAM != null)
            adjustCBbuttonState(mButtonBICROAM, mIsActiveBICr);

        if(mLaunchAlertDialogAgain)
        {
            if(mErrorAlertDialog == null)
            {
            	mErrorAlertDialog = onCreateDialog(EXCEPTION_ERROR);
            }
            mErrorAlertDialog.show();
            mLaunchAlertDialogAgain = false;
        }
        else if(mLaunchRadioOffErrorDialogAgain)
        {
            if(mRadioOffErrorDialog == null)
            {
            	mRadioOffErrorDialog = onCreateDialog(RADIO_OFF_ERROR);
            }
            mRadioOffErrorDialog.show();
            mLaunchRadioOffErrorDialogAgain = false;
        }
        else if(mLaunchResponseErrorDialogAgain)
        {
            if(mResponseErrorDialog == null)
            {
            	mResponseErrorDialog = onCreateDialog(RESPONSE_ERROR);
            }
            mResponseErrorDialog.show();
            mLaunchResponseErrorDialogAgain = false;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                if(mErrorAlertDialog != null && mErrorAlertDialog.isShowing())
                {
                    mErrorAlertDialog.dismiss();
                    mLaunchAlertDialogAgain = true;
                }
                else if(mRadioOffErrorDialog != null && mRadioOffErrorDialog.isShowing())
                {
                    mRadioOffErrorDialog.dismiss();
                    mLaunchRadioOffErrorDialogAgain = true;
                }
                else if(mResponseErrorDialog != null && mResponseErrorDialog.isShowing())
                {
                    mResponseErrorDialog.dismiss();
                    mLaunchResponseErrorDialogAgain = true;
                }
            }
        } 
    };

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        out.putInt(PWD_CHANGE_STATE_KEY, mPwdChangeState);
        out.putString(OLD_PWD_KEY, mOldPwd);
        out.putString(NEW_PWD_KEY, mNewPwd);

        if (mButtonCBPwd != null) {  
            out.putString(DIALOG_MESSAGE_KEY, mButtonCBPwd.getDialogMessage().toString());
            out.putString(DIALOG_PWD_ENTRY_KEY, mButtonCBPwd.getText());
        }

        out.putSerializable(APP_STATE_KEY, mAppState);
        out.putInt(DISPLAY_MODE_KEY, mDisplayMode);

    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
//END: 0001606  vegas80@lge.com  2009-09-18 
