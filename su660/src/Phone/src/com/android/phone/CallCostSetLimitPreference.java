// LGE_CALL_COSTS START
package com.android.phone;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.ITelephony;

/**
 * CallCostSetLimitPreference class
 */
public class CallCostSetLimitPreference extends PreferenceActivity{
    private static final String TAG = "AOC_SET_LIMIT_DLG";

    // Events we handle
    private static final int EVENT_AOC_QUERY_ICC_ACM_MAX = 100;
    private static final int EVENT_AOC_QUERY_ICC_PUC = 101;
    private static final int EVENT_CHANGE_ICC_PASSWORD_DONE = 102;
    private static final int EVENT_AOC_SET_ICC_ACM_MAX = 103;
    
    // String keys for preference lookup
    private static final String BUTTON_PIN2_CHECK_KEY = "button_pin2_check_key";
    private static final String BUTTON_SET_LIMIT_KEY = "button_call_cost_set_limit_key";
    private static final String BUTTON_LIMIT_VALUE_KEY = "button_call_cost_limit_value_key";

    private CheckBoxPreference checkBoxSetLimit;
    private EditCallCostPreference mEditLimitValue;
    
    private EditPinPreference mButtonPin2Check;
    
    private int mPin2Error;

    private Phone mPhone;
    
    private String mLimitValue, mPin2, mEditLimitValueStr;
    
    EditTextPreference mLimitValuePreference;
    
    private boolean isValueSet;
    
    
    Button mDoneButton, mRevertButton;
    
    int pin2RetryNum;
    
    
    // price per unit ratio
    Double ppu = 1.00;

    interface OnSetLimitEnteredListener {
        void onSetLimitEntered(CallCostSetLimitPreference preference, boolean positiveResult);
    }

    private OnSetLimitEnteredListener mSetLimitListener;

    public void setOnSetLimitEnteredListener(OnSetLimitEnteredListener listener) {
        mSetLimitListener = listener;
    }

    /**
     * Overridden to setup the correct dialog layout, as well as setting up
     * other properties for the pin / puk entry field.
     */

    @Override
    protected void onCreate(Bundle map) {
        super.onCreate(map);

        setContentView(R.layout.call_cost_buttons_screen);
        
        addPreferencesFromResource(R.xml.call_cost_set_limit);
        
        
        mDoneButton = (Button) findViewById(R.id.doneButton);
        mRevertButton = (Button) findViewById(R.id.revertButton);

        //mAttemptCount = 0;

        mPhone = PhoneFactory.getDefaultPhone();
        PreferenceScreen prefSet = getPreferenceScreen();
        
        
        mButtonPin2Check = (EditPinPreference) prefSet.findPreference(BUTTON_PIN2_CHECK_KEY);
        checkBoxSetLimit = (CheckBoxPreference) prefSet.findPreference(BUTTON_SET_LIMIT_KEY);
        mEditLimitValue = (EditCallCostPreference) prefSet.findPreference(BUTTON_LIMIT_VALUE_KEY);

        Message msgGetAcmMax = mHandler.obtainMessage(EVENT_AOC_QUERY_ICC_ACM_MAX);
        mPhone.getAccumulatedCallMeterMax(msgGetAcmMax);
        
        Message msgGetPpu = mHandler.obtainMessage(EVENT_AOC_QUERY_ICC_PUC);
        mPhone.getPricePerUnitAndCurrency(msgGetPpu);
        
        // get PIN2 retry number 
    	try {
 	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
         } catch (RemoteException ex){
         	Log.e(TAG, "Failed to get pin2RetryNum: ");
            pin2RetryNum = -1;
         }  
        
        mButtonPin2Check.setOnPinEnteredListener(mPinVerificationListener);
        mButtonPin2Check.setDialogMessage(pin2Message(pin2RetryNum));
        mButtonPin2Check.showPinDialog();
        prefSet.removePreference(mButtonPin2Check);
        
        checkBoxSetLimit.setOnPreferenceClickListener(mSetLimitClickListener);

        mEditLimitValue.setOnCallCostEnteredListener(mSetLimitValueListner); 
        
        mDoneButton.setOnClickListener(mDoneButtonListener);
        mRevertButton.setOnClickListener(mRevertButtonListener);
                
    }

 
    /**
     * Handler for messages from RIL
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_AOC_QUERY_ICC_ACM_MAX:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "Failed to get ACM Max.");
                        Log.e(TAG, ar.exception.getMessage());
                    } else {
                        String acmMaxStr = (String)ar.result;

                        int acmMax = 0;

                        try {
                            acmMax = Integer.parseInt(acmMaxStr);
                        } catch (NumberFormatException ex) {
                            Log.e(TAG, "Failed to get ACM Max: " + ex.getMessage());
                        }
                        Log.d(TAG, "acmMax = " + acmMax);                        
                        
                        mLimitValue = String.valueOf(acmMax);
                        
                        if(acmMax == 0) { 
                            checkBoxSetLimit.setChecked(false);
                        } else {
                        	checkBoxSetLimit.setChecked(true);
                        }
                        
                        mEditLimitValue.setSummary(mLimitValue);
                        
                    }
                    break;

                case EVENT_AOC_QUERY_ICC_PUC:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "Failed to get PUCT.");
                        Log.e(TAG, ar.exception.getMessage());
                    } else {
                        String[] puct = (String[])ar.result;                        

                        if(! puct[0].equals(CallCostSettings.DEFAULT_UNIT_CURRENCY)) {
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
	      		       try{
	                         ppu = Double.valueOf(puct[1]);
	      		    	    }
			       catch(java.lang.NumberFormatException nfe){
	                         Log.d(TAG, "ppu = " + ppu);
				}
                           Log.d(TAG, "ppu = " + ppu);
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
                        } else {
                            Log.d(TAG, "PUCT is empty.");
                        }
                    }
                    break;
                case EVENT_CHANGE_ICC_PASSWORD_DONE:                     
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        CommandException ce = (CommandException) ar.exception;
                        isPUK2 (ce);
                        
                        Log.e(TAG, "EVENT_CHANGE_ICC_PASSWORD_DONE error");
                        
                    	// get PIN2 retry number 
                    	try {
                 	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
                        } catch (RemoteException ex){
                            Log.e(TAG, "Failed to get pin2RetryNum: ");
                            //pin2RetryNum = -1;
                        }    


                        if(pin2RetryNum > 0) {
                            displayMessage(R.string.pin2_invalid);
                          	mButtonPin2Check.setDialogMessage(pin2Message(pin2RetryNum));
                            //mButtonPin2Check.setText("");
                            //mButtonPin2Check.showPinDialog();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Log.d(TAG, "mPinVerificationListener: attempt > 3");
                            displayMessage(mPin2Error);
                            setResult(RESULT_OK);
                            finish();
                        }
                    } else {
                        Log.d(TAG, "EVENT_CHANGE_ICC_PASSWORD_DONE ok");
                        checkBoxSetLimit.setEnabled(true);
                        mDoneButton.setEnabled(true);
                        if (mLimitValue.equals(CallCostSettings.DEFAULT_UNIT_VALUE)){
                        	mEditLimitValue.setEnabled(false);                        	                        	
                       } else {
                        	mEditLimitValue.setEnabled(true);
                        }
                        
                        
                    }                
                break;
                
                case EVENT_AOC_SET_ICC_ACM_MAX:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "EVENT_AOC_SET_ICC_ACM_MAX error");
                        displayMessage(R.string.call_cost_set_limit_error);
                    } else {
                        Log.d(TAG, "EVENT_AOC_SET_ICC_ACM_MAX ok");
                    }                
                break;
                
                default:
                    break;
            }
        }
    };
    
    /**
     * Handler for Clear Call cost EditPinPreference.
     */
    private EditPinPreference.OnPinEnteredListener mPinVerificationListener =
        new EditPinPreference.OnPinEnteredListener() {

        public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
            mPin2 = preference.getText();
            
            // get PIN2 retry number
            try {
    	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
            } catch (RemoteException ex){
            	Log.e(TAG, "Failed to get pin2RetryNum: ");
                //pin2RetryNum = -1;
            }   
            
        	mButtonPin2Check.setDialogMessage(pin2Message(pin2RetryNum));
        	
            if(positiveResult) {
            	if (!CallCostSettings.validatePin(mPin2, false)) {
            		
            		displayMessage(R.string.invalidPin2);
            		//mButtonPin2Check.setText("");
            		//mButtonPin2Check.showPinDialog();
                    setResult(RESULT_OK);
                    finish();
            	
            	}
            	else if (pin2RetryNum > 0) {            		

            		Log.d(TAG, "Verification Pin2 for Set Limit");
                    Message msg = mHandler.obtainMessage(EVENT_CHANGE_ICC_PASSWORD_DONE);
                    mPhone.getIccCard().changeIccFdnPassword(mPin2, mPin2, msg);
                    mPin2Error = R.string.pin2_blocked;

               } 
                
            } else {
                Log.d(TAG, "mPinVerificationListener: Cancel was pressed");
                isValueSet = true;
                setResult(RESULT_CANCELED);
                finish();
        }
      }
    };


    public boolean isUnlimited() {
        return checkBoxSetLimit.isChecked();
    }
    
    /**
     * Display a toast for message, like the rest of the settings.
     */
    void displayMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }
    
    
    /**
     * Handler for Set Limit preference.
     */
    private Preference.OnPreferenceClickListener mSetLimitClickListener = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick (Preference preference) {
            if (preference == checkBoxSetLimit){
                if(checkBoxSetLimit.isChecked() && mLimitValue != null) {
                    mEditLimitValue.setSummary(mLimitValue);
                } else {
                    mEditLimitValue.setSummary(CallCostSettings.DEFAULT_UNIT_VALUE);
                }
                disableEnableLimitValue();
                
                return true;
                
            } 

            return false;
        }
    };
        
  
    /**
     * Handler for set text for Call Cost EditCallCostPreference.
     */
    private EditCallCostPreference.OnCallCostEnteredListener mSetLimitValueListner =
        new EditCallCostPreference.OnCallCostEnteredListener() {

        public void onCallCostEntered(EditCallCostPreference preference, boolean positiveResult) {


            if(positiveResult) {
                
            	String value = preference.getText();
                Log.d(TAG, "Call Cost Set Limit = " + value);
                                
                if (!CallCostSettings.fieldVerificationFailed(value)){
                	mEditLimitValue.setSummary(value);
                    isValueSet = true;
                } else {
                    displayMessage(R.string.call_cost_invalid_data);
                }

          
        }
      }
    };     
    
    public void onUserInteraction() {
    	mEditLimitValueStr = mEditLimitValue.getSummary().toString();
    }
    
    
    private void setACMMeter(String text){
        Log.d(TAG, "value for Set Limit");
        String limit = CallCostSettings.DEFAULT_UNIT_VALUE;
        Message msg = mHandler.obtainMessage(EVENT_AOC_SET_ICC_ACM_MAX);
        if (text != null){
            limit = !isUnlimited() ? CallCostSettings.DEFAULT_UNIT_VALUE : text;
        }    
        mPhone.setAccumulatedCallMeterMax(limit, mPin2, msg);
        setResult(RESULT_OK);
        finish();
    }
       
   private void disableEnableLimitValue(){
   	
    	if (!isUnlimited()){
    		mEditLimitValue.setEnabled(false);
    		isValueSet = false;
        } else {
        	mEditLimitValue.setEnabled(true);
        	isValueSet = true;
        }
    }
   
   
   private String pin2Message(int remaining){
   	
       String strPin2 = getString(R.string.enter_pin2_text);
       String strHead = getString(R.string.call_cost_pin2_remainig);
       String value = strPin2.concat("\n").concat(strHead).concat(String.valueOf(remaining));
       
   	   return value;
   }
   
   // Click listener for all toggle events
   @Override
   public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
   	
       if (preference == mEditLimitValue){
    	  mEditLimitValue.getEditText().setText(mEditLimitValueStr);
          return true;
       }
       return false;
   }
      
   
   private void isPUK2 (CommandException ce){
   	if (ce.getCommandError() == CommandException.Error.SIM_PUK2) {
   		Log.d(TAG, "isPUK2 = " + ce.getCommandError());
           // make sure we set the PUK2 state so that we can skip
           // some redundant behaviour.
           displayMessage(R.string.fdn_enable_puk2_requested);
           finish();
       }
	
   }
   
   
   /**
    * Handler doneButton .
    */
   private Button.OnClickListener mDoneButtonListener = 
       new Button.OnClickListener() {

		public void onClick(View v) {
			 if (!isValueSet) {
	    		   setACMMeter(null);    
	    	 } else {
	    		   setACMMeter(mEditLimitValueStr);
	    		   
	    	 }
			 
			 setResult(RESULT_OK);
	         finish();
			
		}
      
   };
   
   /**
    * Handler revertButton .
    */
   private Button.OnClickListener mRevertButtonListener = 
       new Button.OnClickListener() {

		public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();	
		}
      
   };

}
// LGE_CALL_COSTS END
