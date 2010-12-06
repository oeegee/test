// LGE_CALL_COSTS START
package com.android.phone;


import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Button;

import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.Dialog;


import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.ITelephony;



/**
 * Class similar to the com.android.settings.EditPinPreference
 * class, with a couple of modifications, including a different layout
 * for the dialog.
 */
public class CallCostSetTariffPreference extends PreferenceActivity {

    private static final String TAG = "AOC_SET_TARIFF_DLG";

    // Events we handle
    private static final int EVENT_AOC_QUERY_ICC_PUC = 100;
    private static final int EVENT_CHANGE_ICC_PASSWORD_DONE = 101;
    private static final int EVENT_AOC_SET_ICC_PUC = 102;
    
    Button mDoneButton, mRevertButton;
    
    private Preference mUnit;
    private EditCallCostPreference mUnitValue;
    private EditCallCostPreferenceCurrency mCurrency;
    
    private String mUnitValueStr, mCurrencyStr;

    private static final String BUTTON_PIN2_CHECK_KEY = "button_pin2_check_key";
    private static final String BUTTON_UNIT_KEY = "button_call_cost_unit_key";
    private static final String BUTTON_UNIT_VALUE_KEY = "button_call_cost_unit_value_key";    
    private static final String BUTTON_CURRENCY_KEY = "button_call_cost_currency_key";
    
    private Phone mPhone;

    
    private int mPin2Error;
    
    private String mPin2;
    
    private EditPinPreference mButtonPin2Check;
    
    private boolean isUnitTypeSelected;
    
    int pin2RetryNum;
    
// LGE_UPDATE_S // 20100812
    String mCurrencyName="";
//    String mCurrencyName;
// LGE_UPDATE_E
    
    AlertDialog mAlertDialogCurrency;
    
    
    interface OnSetTariffEnteredListener {
        void onSetTariffEntered(CallCostSetTariffPreference preference, boolean positiveResult);
    }

    private OnSetTariffEnteredListener mSetTariffListener;

    public void setOnSetTariffEnteredListener(OnSetTariffEnteredListener listener) {
        mSetTariffListener = listener;
    }

    @Override
    protected void onCreate(Bundle map) {
        super.onCreate(map);
        
        setContentView(R.layout.call_cost_buttons_screen);
        
        addPreferencesFromResource(R.xml.call_cost_set_call_cost);

        mDoneButton = (Button) findViewById(R.id.doneButton);
        mRevertButton = (Button) findViewById(R.id.revertButton);

        mPhone = PhoneFactory.getDefaultPhone();
        PreferenceScreen prefSet = getPreferenceScreen();
               
        
        mButtonPin2Check = (EditPinPreference) prefSet.findPreference(BUTTON_PIN2_CHECK_KEY);
        mUnit = (Preference) prefSet.findPreference(BUTTON_UNIT_KEY);
        mUnitValue = (EditCallCostPreference) prefSet.findPreference(BUTTON_UNIT_VALUE_KEY);
        mCurrency = (EditCallCostPreferenceCurrency) prefSet.findPreference(BUTTON_CURRENCY_KEY);
        
        Message msg = mHandler.obtainMessage(EVENT_AOC_QUERY_ICC_PUC);
        mPhone.getPricePerUnitAndCurrency(msg);
                
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

        mUnitValue.setOnCallCostEnteredListener(mSetUnitValue);
        mCurrency.setOnCallCostEnteredListenerCurrency(mSetCurrency);
        
        mDoneButton.setOnClickListener(mDoneButtonListener);
        mRevertButton.setOnClickListener(mRevertButtonListener);
                
        
    }

   /**
     * Handler for asynchronous replies from RIL.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch (msg.what) {
                case EVENT_AOC_QUERY_ICC_PUC:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "Failed to get PUCT.");
                        Log.e(TAG, ar.exception.getMessage());

                    } else {
                        String[] puct = (String[])ar.result;

                        Log.d(TAG, "puct[0]: $" + puct[0] + "$ puct[1]: $" + puct[1] + "$");
						
                        // LGE_UPDATE_S // 20100812 // prevent Exception in case ppu is null
                        if ( puct[0] != null )
                            mCurrencyName = puct[0];
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
							Double ppu = 0.0;
							try {
                            	ppu = Double.valueOf(puct[1]);
							} catch (NumberFormatException ex) {
							}
							String ppu_str = String.valueOf(ppu);
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
                        Log.d(TAG, "currecny = " + mCurrencyName + ", ppu = " + ppu_str);

                        mUnitValue.setSummary(ppu_str);
                        mCurrency.setSummary(mCurrencyName);

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
                            Log.d(TAG, "mPinVerificationListener: pin2RetryNum == 0");
                            displayMessage(mPin2Error);
                            setResult(RESULT_OK);
                            finish();
                        }
                    } else {
                        Log.d(TAG, "EVENT_CHANGE_ICC_PASSWORD_DONE ok");
                        mUnit.setEnabled(true);
                        mDoneButton.setEnabled(true);

                        if (mCurrencyName.equals(CallCostSettings.DEFAULT_UNIT_CURRENCY)){
                        	mUnitValue.setEnabled(false);
                            mCurrency.setEnabled(false);
                        } else {
                        	mUnitValue.setEnabled(true);
                            mCurrency.setEnabled(true);
                        }
                    }                
                break;
                
                case EVENT_AOC_SET_ICC_PUC: 
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "EVENT_AOC_SET_ICC_PUC error");
                        displayMessage(R.string.call_cost_set_tariff_error);
                    } else {
                        Log.d(TAG, "EVENT_AOC_SET_ICC_PUC ok");
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
                setResult(RESULT_CANCELED);
                finish();
            }
      }
    };
    
    /**
     * Display a toast for message, like the rest of the settings.
     */
    void displayMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Handler for set text for Call Cost EditCallCostPreference.
     */
    private EditCallCostPreference.OnCallCostEnteredListener mSetUnitValue =
        new EditCallCostPreference.OnCallCostEnteredListener() {

        public void onCallCostEntered(EditCallCostPreference preference, boolean positiveResult) {


            if(positiveResult) {
               
                String value = preference.getText();                
                Log.d(TAG, "value for Set Unit Value" + value);
                
                if (!CallCostSettings.fieldVerificationFailed(value)){
                	mUnitValue.setSummary(value);
               } else {
                    displayMessage(R.string.call_cost_invalid_data);
               }

            }
      }
    };   
    
    
    /**
     * Handler for set text for Call Cost EditCallCostPreference.
     */
    private EditCallCostPreferenceCurrency.OnCallCostEnteredListenerCurrency mSetCurrency =
        new EditCallCostPreferenceCurrency.OnCallCostEnteredListenerCurrency() {

        public void onCallCostEntered(EditCallCostPreferenceCurrency preference, boolean positiveResult) {


            if(positiveResult) {
 
               
                String value = preference.getText();
                Log.d(TAG, "value for Set Currency" + value);

                if (!fieldVerificationFailedCurr(value)){
                    mCurrency.setSummary(value);
                } else {
                    displayMessage(R.string.call_cost_invalid_data);
                }
           }
      }
    };   


    

 // Click listener for all toggle events
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    		if (preference == mUnit){
            	
                String unit = getString(R.string.call_cost_unit);
                String curr = getString(R.string.call_cost_currency);

                final CharSequence[] items = {unit, curr};
                String dialogVal;

                if (mCurrencyName.equals(CallCostSettings.DEFAULT_UNIT_CURRENCY)){
                	dialogVal = unit;
                } else {
                	dialogVal = curr;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.call_cost_unit);
            
                builder.setSingleChoiceItems(items, dialogValue(dialogVal), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        mUnit.setSummary(items[item]);                       
                        String unitVal = items[item].toString();
                        disableEnableItems(unitVal);
                        mAlertDialogCurrency.dismiss();
                        }
                    });
                builder.setNegativeButton(R.string.cancel,  new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int item) {
                		mAlertDialogCurrency.dismiss();
                        }
                });
                mAlertDialogCurrency = builder.create();
                mAlertDialogCurrency.show();
                return true;
            } else if (preference == mUnitValue){
            	mUnitValue.getEditText().setText(mUnitValueStr);
            	return true;
            } else if (preference == mCurrency){
            	if (mCurrencyStr.equals(CallCostSettings.DEFAULT_UNIT_CURRENCY)){
            		mCurrencyStr = "";	
                }
            	mCurrency.getEditText().setText(mCurrencyStr);            	
            }

            return false;
        
    }
    
    public void onUserInteraction() {
    	mUnitValueStr = mUnitValue.getSummary().toString();
    	mCurrencyStr = mCurrency.getSummary().toString();
    }
    
    
    private int dialogValue(String value){
    	int result;
    	String unit = getString(R.string.call_cost_unit);
    	if (value.equals(unit)){
    		result = 0;
    	} else {
    		result = 1;
    	}
    	
    	return result;
    }
    
    private void disableEnableItems(String value){
    	
    	String unit = getString(R.string.call_cost_unit);
    	String curUnitVal =  mUnitValue.getSummary().toString();
    	String curCurrencyVal =  mCurrency.getSummary().toString();
    	
    	if (value.equals(unit)){
            mUnitValue.setEnabled(false);
            mCurrency.setEnabled(false);
            isUnitTypeSelected = true;
            mUnitValue.setSummary(CallCostSettings.DEFAULT_UNIT_VALUE);
            mCurrency.setSummary(CallCostSettings.DEFAULT_UNIT_CURRENCY);
        } else {
        	mUnitValue.setEnabled(true);
            mCurrency.setEnabled(true);
            mUnitValue.setSummary(curUnitVal);
            mCurrency.setSummary(curCurrencyVal);
        }
    }
    
    private String pin2Message(int remaining){
       	
        String strPin2 = getString(R.string.enter_pin2_text);
        String strHead = getString(R.string.call_cost_pin2_remainig);
        String value = strPin2.concat("\n").concat(strHead).concat(String.valueOf(remaining));
        
    	   return value;
    }
    
    private static boolean fieldVerificationFailedCurr (String value){
   	 
        if (value == null || value.equals("") || value.length() != 3){
            return true;
        } 
        
        if (value.equals("0") || value.equals("0.") || value.equals(".")) {
        	return true;
        }
        
        return false;

    }
    
    private void isPUK2 (CommandException ce){
    	if (ce.getCommandError() == CommandException.Error.SIM_PUK2) {
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
 			String ppu = mUnitValue.getSummary().toString();
            String currency = mCurrency.getSummary().toString();
            	
            Message msg = mHandler.obtainMessage(EVENT_AOC_SET_ICC_PUC);
            mPhone.setPricePerUnitAndCurrency(currency, ppu, mPin2, msg);
            mPin2Error = R.string.call_cost_set_limit_error;
                
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
