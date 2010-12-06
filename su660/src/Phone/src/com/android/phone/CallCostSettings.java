// LGE_CALL_COSTS START
package com.android.phone;

import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import android.widget.Toast;

import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.EditPinPreference;
import java.math.BigDecimal;
import com.android.internal.telephony.CommandException;
import android.content.Intent;

import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.ITelephony;


public class CallCostSettings extends PreferenceActivity {

    private static final String TAG = "AOC_SETTINGS";

    private Phone mPhone;


    // Events we handle
    private static final int EVENT_AOC_RESET_ICC_ACM = 100;
    private static final int EVENT_CHANGE_ICC_PASSWORD_DONE = 101;
    private static final int AOC_QUERY_CCM_MAX_REPORTS = 102;
        
    private static final int EVENT_AOC_QUERY_ICC_PUC = 103;
    private static final int AOC_QUERY_ICC_ACM = 104;
    private static final int AOC_QUERY_ICC_ACM_MAX = 105;    
    private static final int AOC_QUERY_CCM = 106;
        
    private static final int EVENT_AOC_UPDATE_ICC_ACM = 107;
    private static final int READ_ICC_ACM_PREVIOUS_RECORD = 108;

    private static final int MAX_LENGTH_ACM_VALUE  = 6;   
    private static final int MAX_RECORDS_NUMBER  = 2;
    
    
    byte [] previousACMDataValue;
    int mCountResponse;
        

    // String keys for preference lookup
    private static final String BUTTON_LAST_COST_CLEAR_KEY = "button_clear_last_call_key";
    private static final String BUTTON_CALL_COST_CLEAR_KEY = "button_clear_call_cost_key";
    private static final String BUTTON_SET_CALL_COST_KEY = "button_set_tariff_key";
    private static final String BUTTON_SET_LIMIT_KEY = "button_set_credit_key";
// LGE_UPDATE_S // 20100812 // modify default value
    public static final String DEFAULT_UNIT_CURRENCY = "";
//    public static final String DEFAULT_UNIT_CURRENCY = "   ";
// LGE_UPDATE_E
    public static final String DEFAULT_UNIT_VALUE = "0";
    
    private EditPinPreference mButtonPin2AllCalls, mButtonPin2LastCall;
    private PreferenceScreen mSetCallCost, mSetLimit;

    private boolean isUnits, isAllCalls;
    private String mPin2, mCurrencyVal;
    private int mPin2Error;
    
    int mAcmMax;
    int mCcm;
    int mAcm;
    
    int pin2RetryNum;
    
    boolean isPUK2recived;
    
    // price per unit and currency
    private Double mPpu = 1.00;
    
    // size limits for the pin.
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;

    /**
     * Display a toast for message, like the rest of the settings.
     */
    void displayMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle map) {
        super.onCreate(map);

        addPreferencesFromResource(R.xml.call_cost_setting);
        
        mPhone = PhoneFactory.getDefaultPhone();

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonPin2LastCall = (EditPinPreference) prefSet.findPreference(BUTTON_LAST_COST_CLEAR_KEY);
        mButtonPin2AllCalls = (EditPinPreference) prefSet.findPreference(BUTTON_CALL_COST_CLEAR_KEY);
        mSetCallCost = (PreferenceScreen) prefSet.findPreference(BUTTON_SET_CALL_COST_KEY);
        mSetLimit = (PreferenceScreen) prefSet.findPreference(BUTTON_SET_LIMIT_KEY);

        Message msg = mHandler.obtainMessage(AOC_QUERY_CCM_MAX_REPORTS);
        mPhone.getCallMeter(msg);
        
   	    // update PPU
   	    Message msgPricePerUnit = mHandler.obtainMessage(EVENT_AOC_QUERY_ICC_PUC);
        mPhone.getPricePerUnitAndCurrency(msgPricePerUnit);

        //update all calls
        Message msgGetAcm = mHandler.obtainMessage(AOC_QUERY_ICC_ACM);
        mPhone.getAccumulatedCallMeter(msgGetAcm);

        //update last call
		Message acmMsg = mHandler.obtainMessage(AOC_QUERY_CCM);
		mPhone.readACMFile(acmMsg);

        //update limit
        Message msgGetAcmMax = mHandler.obtainMessage(AOC_QUERY_ICC_ACM_MAX);
        mPhone.getAccumulatedCallMeterMax(msgGetAcmMax);
        
        
        mButtonPin2LastCall.setOnPinEnteredListener(mPinVerificationListenerLastCall);
        mButtonPin2AllCalls.setOnPinEnteredListener(mPinVerificationListenerAllCalls);
        
        
        
        try {
	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
        } catch (RemoteException ex){
        	Log.e(TAG, "Failed to get pin2RetryNum: ");
            pin2RetryNum = -1;
            
    		mButtonPin2LastCall.setEnabled(false);
    	    mButtonPin2AllCalls.setEnabled(false);
    	    mSetCallCost.setEnabled(false);
    	    mSetLimit.setEnabled(false);
    	    
            displayMessage (R.string.call_cost_is_not_ready_msg);
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
        } catch (java.lang.NumberFormatException ex){
        	Log.e(TAG, "Failed to get pin2RetryNum: ");
            pin2RetryNum = -1;
            
    		mButtonPin2LastCall.setEnabled(false);
    	    mButtonPin2AllCalls.setEnabled(false);
    	    mSetCallCost.setEnabled(false);
    	    mSetLimit.setEnabled(false);
    	    
            displayMessage (R.string.call_cost_is_not_ready_msg);

        }
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
        
        mButtonPin2LastCall.setDialogMessage(pin2Message(pin2RetryNum));
        mButtonPin2AllCalls.setDialogMessage(pin2Message(pin2RetryNum));
        
    }
    /**
     * Handler for Clear Call cost EditPinPreference.
     */
    private EditPinPreference.OnPinEnteredListener mPinVerificationListenerLastCall =
        new EditPinPreference.OnPinEnteredListener() {

        public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
            mPin2 = preference.getText();
            
          	// get PIN2 retry number 
        	try {
     	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
     	       Log.d(TAG, "onUserInteraction in try() - pin2RetryNum:" + pin2RetryNum);
            } catch (RemoteException ex){
                Log.e(TAG, "Failed to get pin2RetryNum: ");
            }
            
            mButtonPin2LastCall.setDialogMessage(pin2Message(pin2RetryNum));
            

            if(positiveResult) {
            	if (!validatePin(mPin2, false)) {
            		displayMessage(R.string.invalidPin2);
            	} 
            	else if (pin2RetryNum > 0) {
                    
            		//get previous record in ACM EF file
            		Message acmMsg = mHandler.obtainMessage(READ_ICC_ACM_PREVIOUS_RECORD);
            		mPhone.readACMFile(acmMsg);
            		
                    Log.d(TAG, "Reseting last call meter");
                    Message msg = mHandler.obtainMessage(EVENT_CHANGE_ICC_PASSWORD_DONE);
                    mPhone.getIccCard().changeIccFdnPassword(mPin2, mPin2, msg);
                    mPin2Error = R.string.pin2_blocked;

                 }
            } else {
                Log.d(TAG, "mPinVerificationListenerLastCall: Cancel was pressed");
            }
         }
    };
    /**
     * Handler for Clear Call cost EditPinPreference.
     */
    private EditPinPreference.OnPinEnteredListener mPinVerificationListenerAllCalls =
        new EditPinPreference.OnPinEnteredListener() {

        public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
            mPin2 = preference.getText();
            mPin2Error = R.string.pin2_blocked;
            
          	// get PIN2 retry number 
        	try {
     	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
     	       Log.d(TAG, "onUserInteraction in try() - pin2RetryNum:" + pin2RetryNum);
            } catch (RemoteException ex){
                Log.e(TAG, "Failed to get pin2RetryNum: ");
            }
                        
            mButtonPin2AllCalls.setDialogMessage(pin2Message(pin2RetryNum));

            if(positiveResult) {
            	if (!validatePin(mPin2, false)) {
            		displayMessage(R.string.invalidPin2);

            	} 
            	else if (pin2RetryNum > 0 ) {

                    Log.d(TAG, "Reseting accumulated call meter");
                    Message msg = mHandler.obtainMessage(EVENT_CHANGE_ICC_PASSWORD_DONE);
                    mPhone.getIccCard().changeIccFdnPassword(mPin2, mPin2, msg);
                    mPin2Error = R.string.call_cost_clear_error;

                    
                }
            } else {
                Log.d(TAG, "mPinVerificationListenerAllCalls: Cancel was pressed");
            }
        }
    };



    public void onUserInteraction() {
    	    	
    	if (isAllCalls) {    	
            mButtonPin2AllCalls.setText("");
    	} else {
            mButtonPin2LastCall.setText("");
    	}
        
    }
    
    protected void  onResume (){
    	
        try {
	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
	        Log.d(TAG, "onResume in try() - pin2RetryNum:" + pin2RetryNum);
        } catch (RemoteException ex){
        	Log.e(TAG, "Failed to get pin2RetryNum: ");
        }
        
        mButtonPin2LastCall.setDialogMessage(pin2Message(pin2RetryNum));
        mButtonPin2AllCalls.setDialogMessage(pin2Message(pin2RetryNum));
                
        Log.d(TAG, "onResume() - pin2RetryNum:" + pin2RetryNum);
        
    	if (pin2RetryNum == 0){
    		displayMessage (R.string.pin2_blocked);
    		mButtonPin2LastCall.setEnabled(false);
    	    mButtonPin2AllCalls.setEnabled(false);
    	    mSetCallCost.setEnabled(false);
    	    mSetLimit.setEnabled(false);
    	} else {
  		    mButtonPin2LastCall.setEnabled(true);
	        mButtonPin2AllCalls.setEnabled(true);
	        mSetCallCost.setEnabled(true);
	        mSetLimit.setEnabled(true);
    	}
    	
    	
    	 // update PPU
    	 Message msgPricePerUnit = mHandler.obtainMessage(EVENT_AOC_QUERY_ICC_PUC);
         mPhone.getPricePerUnitAndCurrency(msgPricePerUnit);     

         
         super.onResume();
         
    }

    /**
     * Save the state of the pin change.
     */
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
       
    }


    /**
     * Handler for asynchronous replies from RIL.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {        	
        	AsyncResult ar;
        	
            switch (msg.what) {

                case EVENT_AOC_RESET_ICC_ACM:                         
                        ar = (AsyncResult) msg.obj;
                        
                        if (ar.exception != null) {                            
                            Log.e(TAG, "EVENT_AOC_RESET_ICC_ACM error");
                        } else {
                            Log.d(TAG, "EVENT_AOC_RESET_ICC_ACM ok");
                        mCountResponse++;
                        if (mCountResponse == MAX_RECORDS_NUMBER - 1){
                            displayMessage(R.string.call_cost_reset_msg);

                            //update all calls
                            Message msgGetAcm = mHandler.obtainMessage(AOC_QUERY_ICC_ACM);
                            mPhone.getAccumulatedCallMeter(msgGetAcm);
                            
                            //update last call
                    		Message acmMsg = mHandler.obtainMessage(AOC_QUERY_CCM);
                    		mPhone.readACMFile(acmMsg);
                        }
                    }
                    
                break;
                
                case EVENT_AOC_UPDATE_ICC_ACM:                         
                    ar = (AsyncResult) msg.obj;
            		
                    if (ar.exception != null) {                            
                        Log.e(TAG, "EVENT_AOC_UPDATE_ICC_ACM error");
                    } else {
                        Log.d(TAG, "EVENT_AOC_UPDATE_ICC_ACM ok");
                        mCountResponse++;
                        if (mCountResponse == MAX_RECORDS_NUMBER - 1){
                            displayMessage(R.string.call_cost_reset_msg);
                                                    
                            //update all calls
                            Message msgGetAcm = mHandler.obtainMessage(AOC_QUERY_ICC_ACM);
                            mPhone.getAccumulatedCallMeter(msgGetAcm);
                            
                            //update last call
                    		Message acmMsg = mHandler.obtainMessage(AOC_QUERY_CCM);
                    		mPhone.readACMFile(acmMsg);
                        }
                        }
                    
                break;
                    
                case EVENT_AOC_QUERY_ICC_PUC: 
                	ar = (AsyncResult) msg.obj;
                	String finalValue;
                    if (ar.exception != null) {
                        Log.e(TAG, "Failed to get PUCT: " + ar.exception.getMessage());
                        displayMessage (R.string.call_cost_is_not_ready_msg);
                        setResult(RESULT_CANCELED);
                        finish();

                    } else {
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code
                        boolean callCostAvailable = true;
                        String[] puct = (String[])ar.result;
                        
                            Log.d(TAG, "puct[0]: $" + puct[0] + "$ puct[1]: $" + puct[1] + "$");

                            // LGE_UPDATE_S // 20100812 // prevent Exception in case ppu is null
                            if ( puct[0] != null )
                               mCurrencyVal = puct[0];
                            else
                               mCurrencyVal = "";

                            if ( puct[1] != null ){
                                if (!mCurrencyVal.equals(DEFAULT_UNIT_CURRENCY))
                                    mPpu = Double.valueOf(puct[1]);

// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
                            mCurrencyVal = puct[0];
                            
                            if (!mCurrencyVal.equals(DEFAULT_UNIT_CURRENCY)){
		      		    try{
		                      mPpu = Double.valueOf(puct[1]);
		      		    	}
				    catch(java.lang.NumberFormatException nfe){
					callCostAvailable = false;
					}
                            }
                            }
				if (callCostAvailable = true){
	                            String ppu_str = String.valueOf(mPpu);
	                            Log.d(TAG, "currecny = " + mCurrencyVal + ", ppu = " + ppu_str);
	                            
	                            if (!mCurrencyVal.equals(DEFAULT_UNIT_CURRENCY)){
	                                finalValue = ppu_str.concat(" ").concat(mCurrencyVal);
	                                isUnits = false;
	                            } else {
	                            	finalValue = getString(R.string.call_cost_set_call_cost_empty);
	                            	isUnits = true;
	                            }
				}
				else{
 	                            Log.d(TAG, "currecny = " + puct[0] + ", ppu = " + puct[1]);
                            	finalValue = getString(R.string.call_cost_set_call_cost_empty);
		                     isUnits = false;
					
				}
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
                            mSetCallCost.setSummary(finalValue);
                            
                            //update all calls
                            Message msgGetAcm = mHandler.obtainMessage(AOC_QUERY_ICC_ACM);
                            mPhone.getAccumulatedCallMeter(msgGetAcm);

                            //update last call 
                    		Message acmMsg = mHandler.obtainMessage(AOC_QUERY_CCM);
                    		mPhone.readACMFile(acmMsg);
                               
                            //update limit
                            Message msgGetAcmMax = mHandler.obtainMessage(AOC_QUERY_ICC_ACM_MAX);
                            mPhone.getAccumulatedCallMeterMax(msgGetAcmMax);
                            

                          
                    }

                break;
                
                
                case AOC_QUERY_ICC_ACM: 
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "Failed to get ACM.");
                        Log.e(TAG, ar.exception.getMessage());
                    } else {
                        String acmStr = (String)ar.result;

                        mAcm = 0;

                        try {
                            mAcm = Integer.parseInt(acmStr);
                        } catch (NumberFormatException ex) {
                            Log.e(TAG, "Failed to get ACM: " + ex.getMessage());
                        }

                        Log.d(TAG, "acm = " + mAcm);

                        mButtonPin2AllCalls.setSummary(processPpu(mAcm));
                    }

                break;
                
                
                case AOC_QUERY_ICC_ACM_MAX: 
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "Failed to get ACM Max.");
                        Log.e(TAG, ar.exception.getMessage());
                    } else {
                        String acmMaxStr = (String)ar.result;

                        mAcmMax = 0;

                        try {
                            mAcmMax = Integer.parseInt(acmMaxStr);
                        } catch (NumberFormatException ex) {
                            Log.e(TAG, "Failed to get ACM MAX: " + ex.getMessage());
                        }

                        Log.d(TAG, "acm max = " + mAcmMax);

                        if(mAcmMax == 0) {
                        	mSetLimit.setSummary(R.string.call_cost_set_limit_off);
                        } else {
                        	acmMaxStr = unitName(String.valueOf(mAcmMax));
                            mSetLimit.setSummary(acmMaxStr);
                        }
                    }

                break;
                

                case AOC_QUERY_CCM: 
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "Failed to get CCM." + ar.exception.getMessage());
                    } else {
                    	Log.d(TAG, "AOC_QUERY_CCM ok");
                    	previousACMDataValue = (byte [])ar.result;
                    	               		
                        mCcm = calculateCCM(previousACMDataValue, mAcm);
                        mButtonPin2LastCall.setSummary(processPpu(mCcm));   

                        Log.d(TAG, "ccm = " + mCcm);

                        mButtonPin2LastCall.setSummary(processPpu(mCcm));
                    }
                break;
                   
                
                case EVENT_CHANGE_ICC_PASSWORD_DONE:
                     
                	ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        CommandException ce = (CommandException) ar.exception;
                        isPUK2 (ce);
                        
                    	// get PIN2 retry number 
                    	try {
                 	        pin2RetryNum = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
                        } catch (RemoteException ex){
                            Log.e(TAG, "Failed to get pin2RetryNum: ");
                        }
                                                
                        Log.e(TAG, "pin2RetryNum: " + pin2RetryNum);
                        
                        Log.e(TAG, "EVENT_CHANGE_ICC_PASSWORD_DONE error");
                        if(pin2RetryNum > 0) {
                            displayMessage(R.string.pin2_invalid);
                            mButtonPin2AllCalls.setDialogMessage(pin2Message(pin2RetryNum));                            
                            mButtonPin2LastCall.setDialogMessage(pin2Message(pin2RetryNum));
                        } else {
                            Log.d(TAG, "mPinVerificationListener: pin2RetryNum == 0");
                            displayMessage(mPin2Error);

                            setResult(RESULT_OK);
                            finish();
                        }
                    } else {
                        mCountResponse = 0;
                        Log.d(TAG, "EVENT_CHANGE_ICC_PASSWORD_DONE ok");
                        if (isAllCalls) {
                        	for (int i = 0 ; i< MAX_RECORDS_NUMBER; i++){
                        	Message msgAccuMeter = mHandler.obtainMessage(EVENT_AOC_RESET_ICC_ACM);
                            mPhone.resetAccumulatedCallMeter(mPin2, msgAccuMeter);
                        	}
                        } else {
                            //send updated data to SIM
                        	for (int i = 0 ; i< MAX_RECORDS_NUMBER; i++){
                                Message msgUpdateAcm = mHandler.obtainMessage(EVENT_AOC_UPDATE_ICC_ACM);
                        	    mPhone.updateACMFile(previousACMDataValue,mPin2, msgUpdateAcm);
                        	}
                        	

                        }
                        
                    }
                    
                break;
                case READ_ICC_ACM_PREVIOUS_RECORD:
                	 ar = (AsyncResult) msg.obj;

                     if (ar.exception != null) {                            
                         Log.e(TAG, "READ_ICC_ACM error");
                     } else {
                    	 
                         Log.d(TAG, "READ_ICC_ACM ok");
                         previousACMDataValue = (byte [])ar.result;                         
                         
                     }
                	break;               	
                default:
                    break;
            }
        }
    };

    // Click listener for all toggle events
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) { 
    
       if  (preference == mButtonPin2LastCall) {
            mButtonPin2LastCall.setText("");
            mButtonPin2LastCall.setDialogTitle(R.string.last_call_clear);
            
            isAllCalls = false;

            return true;
        } else if  (preference == mButtonPin2AllCalls) {
            mButtonPin2AllCalls.setText("");
            mButtonPin2AllCalls.setDialogTitle(R.string.call_cost_clear);
            
            isAllCalls = true;

            return true;
        }
        return false;
    }
    
    private String processPpu(int value) {
        Double valuePpu = value * mPpu;

        BigDecimal b = new BigDecimal(valuePpu).setScale(2,BigDecimal.ROUND_HALF_UP);
        valuePpu = b.doubleValue();

        String valueStr = String.valueOf(valuePpu);
        valueStr = unitName (valueStr);
        
        return valueStr;
    }
    
    private String unitName (String value){
    	String result ="";
    	String units = getString(R.string.call_cost_units);

    	if (isUnits){
    		result = value.concat(" ").concat(units);
        } else if (mCurrencyVal != null){
        	result = value.concat(" ").concat(mCurrencyVal);
        } else {
        	result = "";
        }
    	return result;
    }
    
    public static boolean fieldVerificationFailed (String value){
    	    	 
        if (value == null || value.equals("") 
        		|| value.equals("0") || value.equals("0.")
        		|| value.equals(".")){
            return true;
        } 
        
        return false;

    }

    private String pin2Message(int remaining){
    	
    	String strPin2 = getString(R.string.enter_pin2_text);
        String strHead = getString(R.string.call_cost_pin2_remainig);
        String value = strPin2.concat("\n").concat(strHead).concat(String.valueOf(remaining));
        
    	return value;
    }
    
    private void isPUK2 (CommandException ce){
    	Log.d(TAG, "ce.getCommandError() = " + ce.getCommandError());
    	if (ce.getCommandError() == CommandException.Error.SIM_PUK2) {
            // make sure we set the PUK2 state so that we can skip
            // some redundant behaviour.
            displayMessage(R.string.fdn_enable_puk2_requested);
            isPUK2recived = true;

        }
    } 	
    	
    	
        /**
         * Validate the pin entry.
         *
         * @param pin This is the pin to validate
         * @param isPuk Boolean indicating whether we are to treat
         * the pin input as a puk.
         */
        public static boolean validatePin(String pin, boolean isPUK) {

            // for pin, we have 4-8 numbers, or puk, we use only 8.
            int pinMinimum = isPUK ? MAX_PIN_LENGTH : MIN_PIN_LENGTH;
            
            // check validity
            if (pin == null || pin.length() < pinMinimum || pin.length() > MAX_PIN_LENGTH) {
                return false;
            } else {
                return true;
            }
        }

        private int calculateCCM(byte [] previousACMRecord, int lastACMRecord){
        	int result = 0;

        	String sAcmPreviousVal = IccUtils.bytesToHexString(previousACMRecord);
    
            int iAcmPreviousValue = 0;
        	
            try {
// 20100902 yoonjung.shin@lge.com Apply Teleca 1st CS Code Drop [START]
           	 iAcmPreviousValue = Integer.parseInt(sAcmPreviousVal, 16);
            } catch (NumberFormatException ex) {
                Log.e(TAG, "Failed to get CCM: " + ex.getMessage());
}    
            if(lastACMRecord > iAcmPreviousValue){
                result = lastACMRecord - iAcmPreviousValue;
            } else {
                result = iAcmPreviousValue;
            }
// 20100902 yoonjung.shin@lge.com Apply Teleca 1st CS Code Drop [END]
        	return result;
        }
    
}    

// LGE_CALL_COSTS END
