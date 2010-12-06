package com.android.phone;

import java.util.ArrayList;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
// LGE_CUSTOMER_SERVICE_PROFILE START
import com.android.internal.telephony.PhoneFactory;
import android.telephony.CspConstants;
// LGE_CUSTOMER_SERVICE_PROFILE END
// LGE_TWO_LINE_SERVICE START
import com.android.internal.telephony.Phone;
// LGE_TWO_LINE_SERVICE END

//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [START_LGE_LAB1]
import com.lge.config.StarConfig;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [END_LGE_LAB1]

public class GsmUmtsAdditionalCallOptions extends
        TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "GsmUmtsAdditionalCallOptions";
    private final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    private static final String BUTTON_CLIR_KEY  = "button_clir_key";
    private static final String BUTTON_CW_KEY    = "button_cw_key";
//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [START_LGE_LAB1]
	private static final String BUTTON_SV_KEY    = "button_sv_key";
//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [END_LGE_LAB1]

    private CLIRListPreference mCLIRButton;
    private CallWaitingCheckBoxPreference mCWButton;
//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [START_LGE_LAB1]
	private ListPreference mSVButton;
//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [END_LGE_LAB1]

    private ArrayList<Preference> mPreferences = new ArrayList<Preference> ();
    private int mInitIndex= 0;
    
// LGE_TWO_LINE_SERVICE START
    private TwoLineServicePreference mSelectLinePreference;
    private final String SELECT_LINE_PREFERENCE_KEY    = "select_line_preference_key";
    private boolean isTwoLineServiceSupported;
// LGE_TWO_LINE_SERVICE END

// LGE_TWO_LINE_SERVICE START    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.gsm_umts_additional_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        mCLIRButton = (CLIRListPreference) prefSet.findPreference(BUTTON_CLIR_KEY);
        mCWButton = (CallWaitingCheckBoxPreference) prefSet.findPreference(BUTTON_CW_KEY);
        mSelectLinePreference = (TwoLineServicePreference) prefSet.findPreference(SELECT_LINE_PREFERENCE_KEY);
        isTwoLineServiceSupported = mSelectLinePreference.isTwoLineServiceSupported();
        if (DBG) Log.d(LOG_TAG, "onCreate isTwoLineServiceSupporting = " + isTwoLineServiceSupported);
        mSelectLinePreference.setEnabled(isTwoLineServiceSupported);
//20101005 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
	if (StarConfig.OPERATOR.equals("SKT")) {
       	 prefSet.removePreference(mCLIRButton);
	}
	else{
		mPreferences.add(mCLIRButton);
	}

        mPreferences.add(mCWButton);
	if (StarConfig.OPERATOR.equals("SKT")){
	        
		prefSet.removePreference(mSelectLinePreference);
	}
	else{
		if (isTwoLineServiceSupported) { 
	            mPreferences.add(mSelectLinePreference);
		}
	}
//20101005 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]			
//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [START_LGE_LAB1]
		mSVButton = (ListPreference)prefSet.findPreference(BUTTON_SV_KEY);
		if (!StarConfig.OPERATOR.equals("SKT")) {
			prefSet.removePreference(mSVButton);
		} 
		final int index = Settings.System.getInt(getContentResolver(), Settings.System.CALL_WAITING_TYPE, 0);
		mSVButton.setValue(""+index);

		mSVButton.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				Log.i(LOG_TAG, "[LGE] put CallWaiting value : " + arg0 +"|"+ arg1);
				Settings.System.putInt(getContentResolver(), Settings.System.CALL_WAITING_TYPE, Integer.parseInt((String)arg1)); 
				mSVButton.setValue((String)arg1);
				return false;
			}
		});
//20100926 sh80.choi@lge.com  Add SKT CallWaiting Sound/Vibrate Setting [END_LGE_LAB1]

        if (icicle == null) {
            if (DBG) Log.d(LOG_TAG, "start to init ");
            mCLIRButton.init(this, false);
		if (StarConfig.OPERATOR.equals("SKT")) {
		    mCWButton.init(this, false);	
		}
            if (isTwoLineServiceSupported) { 
                mSelectLinePreference.init(this, false);
            }
        } else {
            if (DBG) Log.d(LOG_TAG, "restore stored states");
            mInitIndex = mPreferences.size();
            mCLIRButton.init(this, true);
            mCWButton.init(this, true);
            if (isTwoLineServiceSupported) { 
                 int lineServiceCount = icicle.getInt(mSelectLinePreference.getKey());
                 if (lineServiceCount != 0) {
                     if (DBG) Log.d(LOG_TAG, "onCreate:  lineServiceCount = "
                            + lineServiceCount);
                     mSelectLinePreference.handleGetTwoLineServiceResult(lineServiceCount);
                 } else {
                 mSelectLinePreference.init(this, false);
                 }
            }
            mSelectLinePreference.init(this, true);
            int[] clirArray = icicle.getIntArray(mCLIRButton.getKey());
           
            if (clirArray != null) {
                if (DBG) Log.d(LOG_TAG, "onCreate:  clirArray[0]="
                        + clirArray[0] + ", clirArray[1]=" + clirArray[1]);
                mCLIRButton.handleGetCLIRResult(clirArray);
            } else {
                mCLIRButton.init(this, false);
            }
        }
        Phone phone = PhoneFactory.getDefaultPhone();
        boolean mEnablePreference = !phone.getIccCard().getIccFdnEnabled();
        mCLIRButton.setEnabled(mEnablePreference);
// LGE_CUSTOMER_SERVICE_PROFILE START
	if (!StarConfig.OPERATOR.equals("SKT")) {
	        mCWButton.setEnabled(PhoneFactory.getDefaultPhone().isServiceCustomerAccessible(CspConstants.GROUPCODE_CALL_COMPLETION, CspConstants.SERVICE_CW) && mEnablePreference);
	}
// LGE_CUSTOMER_SERVICE_PROFILE END
    }
// LGE_TWO_LINE_SERVICE END

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCLIRButton.clirArray != null) {
            outState.putIntArray(mCLIRButton.getKey(), mCLIRButton.clirArray);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            Preference pref = mPreferences.get(mInitIndex);
            if (pref instanceof CallWaitingCheckBoxPreference) {
                ((CallWaitingCheckBoxPreference) pref).init(this, false);
            }
        }
        super.onFinished(preference, reading);
    }

}
