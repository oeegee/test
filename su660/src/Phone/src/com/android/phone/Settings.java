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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ThrottleManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
// LGE_CUSTOMER_SERVICE_PROFILE START
import android.telephony.CspConstants;
// LGE_CUSTOMER_SERVICE_PROFILE END
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
//LGE_TD3658 START
import com.android.phone.Use2GOnlyCheckBoxPreference;
//LGE_TD3658 END
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
import java.util.ArrayList; //park
import java.util.HashMap;
import android.app.Dialog;	//park
import android.preference.PreferenceCategory;
import android.widget.Toast;
import android.provider.Settings.Secure;
import com.lge.IBuildInfo;
import com.lge.ICarrier;
import com.lge.IModelID;
import com.android.internal.telephony.DataConnectionManager;
import android.telephony.TelephonyManager;
import java.util.Observable;
import java.util.Observer;
import android.content.ContentQueryMap;
import android.database.Cursor;
import android.preference.PreferenceCategory;
import com.lge.config.StarConfig;
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]

/**
 * List of Phone-specific settings screens.
 */
public class Settings extends PreferenceActivity implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener{

    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM         = 17;

    //String keys for preference lookup
    private static final String BUTTON_DATA_ENABLED_KEY = "button_data_enabled_key";
    private static final String BUTTON_DATA_USAGE_KEY = "button_data_usage_key";
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_ROAMING_KEY = "cdma_roaming_mode_key";

    private static final String BUTTON_GSM_UMTS_OPTIONS = "gsm_umts_options_key";
    private static final String BUTTON_CDMA_OPTIONS = "cdma_options_key";
//LGE_TD3658 START
    private Use2GOnlyCheckBoxPreference mButtonPrefer2g;
    private static final String BUTTON_PREFER_2G_KEY = "button_prefer_2g_key";
//LGE_TD3658 END

// LGE_ECLAIR_PORTING START
    // Used for CDMA roaming mode
    private static final int CDMA_ROAMING_MODE_HOME = 0;
    //private static final int CDMA_ROAMING_MODE_AFFILIATED = 1;
    private static final int CDMA_ROAMING_MODE_ANY = 2;

    // PREFERRED_CDMA_ROAMING_MODE  0 - Home Networks only, preferred
    //                              1 - Roaming on affiliated networks
    //                              2 - Roaming on any network
    static final int PREFERRED_CDMA_ROAMING_MODE = 0;
// LGE_ECLAIR_PORTING END

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
    private static final String PREFKEY_CARRIER_SELECT = "button_carrier_sel_key";		//Network operators
    private static final String BUTTON_DATA_MODE_ASK_KEY = "button_data_mode_ask_key";

    private static final int MODE_FLAG_OPTION = 0xffff0000;
    private static final int MODE_FLAG_CONNECTION = 0xffff;    
	
    private static final int MODE_DISABLED = 0x0;
    private static final int MODE_ENABLED = 0x1;
    private static final int MODE_OPTION_ASK_AT_BOOT = 0x10000;
    private static final int MODE_OPTION_ASK_ALWAYS = 0x20000;
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]

    //UI objects
    private ListPreference mButtonPreferredNetworkMode;
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
// LGE_PDP_MODE START
    private ListPreference mListPrefPdpMode;
//20101006 jongwany.lee@lge.com blocked this function due to duplication with native code. 
    //private static final String PDP_MODE_PREF = "mpdp_mode_key";
// LGE_PDP_MODE END
// LGE_PDP_MODE START
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
    private CheckBoxPreference mButtonDataRoam;
    private CheckBoxPreference mButtonDataEnabled;
    private CdmaRoamingListPreference mButtonCdmaRoam;
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
    private CheckBoxPreference mButtonDataModeAsk;	
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]

    private Preference mButtonDataUsage;
    private DataUsageListener mDataUsageListener;
    private static final String iface = "rmnet0"; //TODO: this will go away

    private Phone mPhone;
    private MyHandler mHandler;
    private boolean mOkClicked;

// LGE_MOBILE_NETWORK_TYPE START
    private static final String KEY_NET_PREF = "network_preference";
// LGE_MOBILE_NETWORK_TYPE END

    //GsmUmts options and Cdma options
    GsmUmtsOptions gsmumtsOptions;
    CdmaOptions cdmaOptions;

//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
    private AlertDialog mWarnDataRoaming = null;
    private AlertDialog mWarnDataEnable = null;
    private AlertDialog mWarnModeAsk = null;
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]

//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
    private ContentQueryMap mContentQueryMap;
    private boolean mCheckState;
//20101005 eunju.hahm@lge.com for DataNetworkButton 
	private ContentQueryMap mContentQueryMapDataNetwork;

    private final class SettingsObserver implements Observer {
        public void update(Observable o, Object arg) {
            updateToggles();
        }
    }

    private void updateToggles() {
    	if (mButtonDataEnabled != null) {
	       if (!(TelephonyManager.getDefault().isNetworkRoaming() && IBuildInfo.Carrier == ICarrier.SKT))		   	
    			mButtonDataEnabled.setChecked(Secure.getInt(mPhone.getContext().getContentResolver(), Secure.MOBILE_DATA, 1)==1);
    	}       	
    }
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]
    //This is a method implemented for DialogInterface.OnClickListener.
    //  Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
		if ( StarConfig.COUNTRY.equals("KR") )
		{
				if (dialog == mWarnDataRoaming) // roaming dialog
				{
					log("onClick - mWarnDataRoaming :" + which);
					if (which == DialogInterface.BUTTON1) {
						mPhone.setDataRoamingEnabled(true);
						mOkClicked = true;
						mButtonDataRoam.setChecked(true);
					} else {
						// Reset the toggle
						mButtonDataRoam.setChecked(false);
					}
				}
				else if (dialog == mWarnDataEnable)  // data enable dialog
				{
				 	log("onClick - mWarnDataEnable :" + which);

					if( which == DialogInterface.BUTTON1){
						mOkClicked = true;
	                                     DataConnectionManager dataMgr = new DataConnectionManager(mPhone.getContext());
	                                     if( mCheckState ) {
	                                              dataMgr.openNetwork();
	                                     }else{
	                                              dataMgr.closeNetwork();
	                                     }
					}
//					ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
//             			       cm.setMobileDataEnabled(mButtonDataEnabled.isChecked());
				}
				else if (dialog == mWarnModeAsk) // data option dialog
				{
					log("onClick - mWarnModeAsk :" + which);
		        
					if( which == DialogInterface.BUTTON1){
						mOkClicked = true;
						Secure.putInt(mPhone.getContext().getContentResolver(), Secure.PREFERRED_DATA_NETWORK_MODE, (mButtonDataModeAsk.isChecked() ? 1 : 0) );
						mButtonDataModeAsk.setChecked(true);
					}
				}

		}
		else
		{
		        if (which == DialogInterface.BUTTON1) {
		            mPhone.setDataRoamingEnabled(true);
		            mOkClicked = true;
		        } else {
		            // Reset the toggle
		            mButtonDataRoam.setChecked(false);
		        }
		}
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (!mOkClicked) {
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]		
		if ( StarConfig.COUNTRY.equals("KR") )
		{
			if (dialog == mWarnDataRoaming)
				mButtonDataRoam.setChecked(false);
			else if (dialog == mWarnDataEnable) {
			        ConnectivityManager cm =
			                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			        mButtonDataEnabled.setChecked(cm.getMobileDataEnabled());
			}
			else if (dialog == mWarnModeAsk)
				mButtonDataModeAsk.setChecked(false);		
		}
		else
		{
			mButtonDataRoam.setChecked(false);
		}            
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]
        }
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (gsmumtsOptions != null &&
                gsmumtsOptions.onPreferenceTreeClick(preferenceScreen, preference) == true) {
            return true;
        } else if (cdmaOptions != null &&
                   cdmaOptions.onPreferenceTreeClick(preferenceScreen, preference) == true) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                    getContentResolver(), android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        }
        else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
		if ( StarConfig.COUNTRY.equals("KR") )
		{
				mWarnDataRoaming = new AlertDialog.Builder(this).setMessage(
                        getResources().getString(R.string.roaming_warning))
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.roaming_warning_yes, this)
                        .setNegativeButton(R.string.roaming_warning_no, this)
                        .show();
				mWarnDataRoaming.setOnDismissListener(this);
		}
		else
		{
	                new AlertDialog.Builder(this).setMessage(
	                        getResources().getString(R.string.roaming_warning))
	                        .setTitle(android.R.string.dialog_alert_title)
	                        .setIcon(android.R.drawable.ic_dialog_alert)
	                        .setPositiveButton(android.R.string.yes, this)
	                        .setNegativeButton(android.R.string.no, this)
	                        .show()
	                        .setOnDismissListener(this);
		}
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]
            }
            else {
                mPhone.setDataRoamingEnabled(false);
            }
            return true;
        } else if (preference == mButtonDataEnabled) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataEnabled.");
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
		if ( StarConfig.COUNTRY.equals("KR") )
		{
			mOkClicked = false;
	            int strID = (mButtonDataEnabled.isChecked() ? R.string.data_mode_enabled_description : R.string.data_mode_disabled_description );
	            mCheckState = mButtonDataEnabled.isChecked();
	            log("onPreferenceTreeClick:  mCheckState >> "+	mCheckState +"strID >>" +strID)  ;
				mWarnDataEnable = new AlertDialog.Builder(this)
	                    .setMessage(getResources().getString(strID))
	                    .setIcon(android.R.drawable.ic_dialog_alert)
	                    .setPositiveButton(R.string.data_mode_ok, this)
	                    .show();

				mWarnDataEnable.setOnDismissListener(this);
		}
		else
		{
		            ConnectivityManager cm =
		                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

		            cm.setMobileDataEnabled(mButtonDataEnabled.isChecked());
//20101005 eunju.hahm@lge.com for Quicksettings DataNetwork Mode change				  
			Secure.putInt(mPhone.getContext().getContentResolver(), Secure.PREFERRED_DATA_NETWORK_MODE, (mButtonDataEnabled.isChecked() ? 1 : 0) );		
		}
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]			
            return true;
        }        else if(preference == mButtonDataModeAsk ){
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]        
		if ( StarConfig.COUNTRY.equals("KR") )
		{
	            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataModeAsk.");
				
	            //normally called on the toggle click
	            if (mButtonDataModeAsk.isChecked()) {
	                // First confirm with a warning dialog about charges
	                mOkClicked = false;

			mWarnModeAsk = new AlertDialog.Builder(this)
				.setMessage(getResources().getString(R.string.data_mode_ask_description))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.data_mode_ok, this)
				.show();

	                mWarnModeAsk.setOnDismissListener(this);
	            }
			else {
		            Secure.putInt(mPhone.getContext().getContentResolver(), Secure.PREFERRED_DATA_NETWORK_MODE, 0);
	            }
			return true;
		}		
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]			
        }else if (preference == mButtonCdmaRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonCdmaRoam.");
            //displays the value taken from the Settings.System
            int cdmaRoamingMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.CDMA_ROAMING_MODE, PREFERRED_CDMA_ROAMING_MODE);
            mButtonCdmaRoam.setValue(Integer.toString(cdmaRoamingMode));
            return true;
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
// LGE_PDP_MODE START
        } else if(preference != mListPrefPdpMode) {
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
// LGE_PDP_MODE END
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
        }
        // Let the intents be launched by the Preference manager
        return false;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]        
		if ( StarConfig.COUNTRY.equals("KR") )
		{
		        addPreferencesFromResource(R.xml.network_setting_kor);
		}
		else
		{
		        addPreferencesFromResource(R.xml.network_setting);
		}
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]			
        mPhone = PhoneFactory.getDefaultPhone();

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mHandler = new MyHandler();
        mButtonDataEnabled = (CheckBoxPreference) prefSet.findPreference(BUTTON_DATA_ENABLED_KEY);
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]        
	if ( StarConfig.COUNTRY.equals("KR") )
	{
	    mButtonDataModeAsk= (CheckBoxPreference) prefSet.findPreference(BUTTON_DATA_MODE_ASK_KEY);		
	}
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]			
        mButtonDataRoam = (CheckBoxPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonDataUsage = prefSet.findPreference(BUTTON_DATA_USAGE_KEY);

        if (getResources().getBoolean(R.bool.world_phone) == true) {
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            //Get the networkMode from Settings.System and displays it
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                    getContentResolver(),android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            // The intent code that resided here in the past has been moved into the
            // more conventional location in network_setting.xml

        } else {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            prefSet.removePreference(prefSet.findPreference(BUTTON_GSM_UMTS_OPTIONS));
            prefSet.removePreference(prefSet.findPreference(BUTTON_CDMA_OPTIONS));
	// LGE_MOBILE_NETWORK_TYPE START
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
	if (!( StarConfig.COUNTRY.equals("KR") ))
	{
	           mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(KEY_NET_PREF);
	            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
	            int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
	                   getContentResolver(), android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
	                   preferredNetworkMode);
	            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
         }
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]	
	// LGE_MOBILE_NETWORK_TYPE END
            int phoneType = mPhone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                addPreferencesFromResource(R.xml.cdma_options);
                mButtonCdmaRoam =
                    (CdmaRoamingListPreference) prefSet.findPreference(BUTTON_CDMA_ROAMING_KEY);
                cdmaOptions = new CdmaOptions();
            } else if (phoneType == Phone.PHONE_TYPE_GSM) {
            int val;
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]				
		if (!( StarConfig.COUNTRY.equals("KR") ))
		{
              	  addPreferencesFromResource(R.xml.gsm_umts_options);
	                gsmumtsOptions = new GsmUmtsOptions();

	//LGE_TD3658 update Network mode after click on use 2G key START
              	  mButtonPrefer2g = (Use2GOnlyCheckBoxPreference) prefSet.findPreference(BUTTON_PREFER_2G_KEY);
	                mButtonPrefer2g.setButtonPreferredNetworkMode (mButtonPreferredNetworkMode);
	//LGE_TD3658 END
	
	// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
	// LGE_PDP_MODE START
//20101006 jongwany.lee@lge.com blocked this function due to duplication with native code. 
//	                 mListPrefPdpMode = (ListPreference) findPreference(PDP_MODE_PREF);
//	                 mListPrefPdpMode.setOnPreferenceChangeListener(this);
//	                val = android.provider.Settings.Secure.getInt(getContentResolver(), 
//	                        android.provider.Settings.Secure.PDP_MODE, -1);
//	                 if( val < 0 || val > 2 ) {
//	                    val = 0; //"always on" as default
//	                }
//	                 log("PDP: loaded value: " + val);
//	                 mListPrefPdpMode.setValue(Integer.toString(val));
		}

	// LGE_PDP_MODE END
	// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]			
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
        ThrottleManager tm = (ThrottleManager) getSystemService(Context.THROTTLE_SERVICE);
        mDataUsageListener = new DataUsageListener(this, mButtonDataUsage, prefSet);

// LGE_CUSTOMER_SERVICE_PROFILE START
	if (!( StarConfig.COUNTRY.equals("KR") ))
	{
       	 checkCustomerServiceProfile();
	}
// LGE_CUSTOMER_SERVICE_PROFILE END

//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
	Cursor settingsCursor;

	if ( StarConfig.COUNTRY.equals("KR") )
	{
//	        settingsCursor = mPhone.getContext().getContentResolver().query(Secure.CONTENT_URI, null,
//	                "(" + android.provider.Settings.System.NAME + "=?)",
//	                new String[]{Secure.MOBILE_DATA},
//	                null);
//	        mContentQueryMap = new ContentQueryMap(settingsCursor, android.provider.Settings.System.NAME, true, null);
//	        mContentQueryMap.addObserver(new SettingsObserver());

		if(mButtonPreferredNetworkMode != null){
			prefSet.removePreference(mButtonPreferredNetworkMode);
			mButtonPreferredNetworkMode = null;
		}

		prefSet.removePreference(prefSet.findPreference(PREFKEY_CARRIER_SELECT));

	}
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]

//20101005 eunju.hahm@lge.com for DataNetworkButton [Start]
	//DataNetwork
	Cursor DataNetworkSettingsCursor = mPhone.getContext().getContentResolver().query(Secure.CONTENT_URI, null, 
			"(" + android.provider.Settings.System.NAME+ "=?)", 
			new String[]{Secure.PREFERRED_DATA_NETWORK_MODE},
			null);
		  mContentQueryMapDataNetwork = new ContentQueryMap(DataNetworkSettingsCursor, android.provider.Settings.Secure.NAME, true, null);
	
	Observer DataNetworkSettingsObserver = new Observer(){
		public void update(Observable arg0, Object arg1) {
			log("********DataNetworkSettingsObserver************");
			//setNetworkButton();
			ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			mButtonDataEnabled.setChecked(cm.getMobileDataEnabled());
		}
	};
	mContentQueryMapDataNetwork.addObserver(DataNetworkSettingsObserver);
//20101005 eunju.hahm@lge.com for DataNetworkButton [End]

    }
    @Override
    protected void onResume() {
        super.onResume();

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        getPreferenceScreen().setEnabled(true);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mButtonDataEnabled.setChecked(cm.getMobileDataEnabled());

//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]
	int settingValue;
	if ( StarConfig.COUNTRY.equals("KR") )
	{
		settingValue = Secure.getInt(mPhone.getContext().getContentResolver(), Secure.PREFERRED_DATA_NETWORK_MODE, 1);
		mButtonDataModeAsk.setChecked((settingValue ==1) ? true:false);

	       if (TelephonyManager.getDefault().isNetworkRoaming() && IBuildInfo.Carrier == ICarrier.SKT)
	        {
	            log("onResume =  isNetworkRoaming"+ TelephonyManager.getDefault().isNetworkRoaming() );
	            mButtonDataEnabled.setEnabled(false);
	            mButtonDataModeAsk.setEnabled(false);
	        }		
	}
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
        }
//20100918 hangyul.park@lge.com LAB1_UX [START_LGE_LAB1]		
	// LGE_CUSTOMER_SERVICE_PROFILE START
	if (!( StarConfig.COUNTRY.equals("KR") ))
	{
       	 checkCustomerServiceProfile();
	}
	// LGE_CUSTOMER_SERVICE_PROFILE END
//20100918 hangyul.park@lge.com LAB1_UX [END_LGE_LAB1]	
        mDataUsageListener.resume();
	if ( StarConfig.COUNTRY.equals("KR") )
	{

        if(mContentQueryMap == null) {
            log("onResume   mContentQueryMap addObservable");
		
            Cursor settingsCursor = mPhone.getContext().getContentResolver().query(Secure.CONTENT_URI, null,
                    "(" + android.provider.Settings.System.NAME + "=?)", new String[]{Secure.MOBILE_DATA}, null);
            mContentQueryMap = new ContentQueryMap(settingsCursor, android.provider.Settings.System.NAME, true, null);
            mContentQueryMap.addObserver(new Observer(){
                    public void update(Observable o, Object arg) {
                        if (mButtonDataEnabled != null) {
	                     log("SettingsObserver ===>mButtonDataEnabled");
                            if (!(TelephonyManager.getDefault().isNetworkRoaming() && IBuildInfo.Carrier == ICarrier.SKT))		   	
                                mButtonDataEnabled.setChecked(Secure.getInt(mPhone.getContext().getContentResolver(), Secure.MOBILE_DATA, 1)==1);
                            }       	
                    } });
	    }


	}
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataUsageListener.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy");
	if ( StarConfig.COUNTRY.equals("KR") )
	{
		
        if (mContentQueryMap != null) {
            log("onDestroy   mContentQueryMap close");

            mContentQueryMap.close();
            mContentQueryMap = null;
        }
	}
        
    }
    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                switch(buttonNetworkMode) {
                    case Phone.NT_MODE_GLOBAL:
                        modemNetworkMode = Phone.NT_MODE_GLOBAL;
                        break;
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                        modemNetworkMode = Phone.NT_MODE_EVDO_NO_CDMA;
                        break;
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                        modemNetworkMode = Phone.NT_MODE_CDMA_NO_EVDO;
                        break;
                    case Phone.NT_MODE_CDMA:
                        modemNetworkMode = Phone.NT_MODE_CDMA;
                        break;
                    case Phone.NT_MODE_GSM_UMTS:
                        modemNetworkMode = Phone.NT_MODE_GSM_UMTS;
                        break;
                    case Phone.NT_MODE_WCDMA_ONLY:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_ONLY;
                        break;
                    case Phone.NT_MODE_GSM_ONLY:
                        modemNetworkMode = Phone.NT_MODE_GSM_ONLY;
                        break;
                    case Phone.NT_MODE_WCDMA_PREF:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        break;
                    default:
                        modemNetworkMode = Phone.PREFERRED_NT_MODE;
                }
                UpdatePreferredNetworkModeSummary(buttonNetworkMode);

                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        buttonNetworkMode );
                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        } else if (preference == mButtonCdmaRoam) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonCdmaRoam.setValue((String) objValue);
            int buttonCdmaRoamingMode;
            buttonCdmaRoamingMode = Integer.valueOf((String) objValue).intValue();
            int settingsCdmaRoamingMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.CDMA_ROAMING_MODE, PREFERRED_CDMA_ROAMING_MODE);
            if (buttonCdmaRoamingMode != settingsCdmaRoamingMode) {
                int statusCdmaRoamingMode;
                switch(buttonCdmaRoamingMode) {
// LGE_ECLAIR_PORTING START
                    case CDMA_ROAMING_MODE_ANY:
                        statusCdmaRoamingMode = Phone.CDMA_RM_ANY;
                        break;
                    case CDMA_ROAMING_MODE_HOME:
// LGE_ECLAIR_PORTING END
                    default:
                        statusCdmaRoamingMode = Phone.CDMA_RM_HOME;
                }
                //Set the Settings.System network mode
                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.CDMA_ROAMING_MODE,
                        buttonCdmaRoamingMode );
                //Set the roaming preference mode
                mPhone.setCdmaRoamingPreference(statusCdmaRoamingMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_ROAMING_PREFERENCE));
            }
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [START]
// LGE_PDP_MODE START
        } 
        else if(preference == mListPrefPdpMode) {
            String strValue = (String) objValue;
            int intValue;
            try {
                intValue = Integer.parseInt(strValue);
            } catch (NumberFormatException ex) {
                intValue = 0;
            }

            int oldval = android.provider.Settings.Secure.getInt(getContentResolver(), 
                      android.provider.Settings.Secure.PDP_MODE, -1);
            if( oldval < 0 || oldval > 2 ) oldval = -1;
            
            log("PDP mode changed by user:" + strValue);

            mListPrefPdpMode.setValue(strValue);

            if(intValue != oldval) {
                log("sending PDP mode to settings: " + intValue);
                android.provider.Settings.Secure.putInt(getContentResolver(), 
                    android.provider.Settings.Secure.PDP_MODE, intValue);
                
                if(intValue == 1) { //when needed
                    new AlertDialog.Builder(Settings.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.pdp_mode_when_needed_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
                } else if(intValue == 2) { //always off
                    new AlertDialog.Builder(Settings.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.pdp_mode_aways_off_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
                }
            }
        }
// LGE_PDP_MODE END
// 20100819 yongsung.kim@lge.com apply Teleca 4th drop code [END]

        // always let the preference setting proceed.
        return true;
    }

    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;
        private static final int MESSAGE_QUERY_ROAMING_PREFERENCE = 2;
        private static final int MESSAGE_SET_ROAMING_PREFERENCE = 3;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
                case MESSAGE_QUERY_ROAMING_PREFERENCE:
                    handleQueryCdmaRoamingPreference(msg);
                    break;

                case MESSAGE_SET_ROAMING_PREFERENCE:
                    handleSetCdmaRoamingPreference(msg);
                    break;
             }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    log ("handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode);

                if (DBG) {
                    log("handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
                            settingsNetworkMode);
                }

                //check that modemNetworkMode is from an accepted value
                if (modemNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                        modemNetworkMode == Phone.NT_MODE_GSM_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_WCDMA_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_GSM_UMTS ||
                        modemNetworkMode == Phone.NT_MODE_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_CDMA_NO_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_EVDO_NO_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_GLOBAL ) {
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) { log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        android.provider.Settings.Secure.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                                settingsNetworkMode );
                    }

                    UpdatePreferredNetworkModeSummary(modemNetworkMode);
                    // changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                } else {
                    if (DBG) log("handleGetPreferredNetworkTypeResponse: else: reset to default");
                    resetNetworkModeToDefault();
                }
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int networkMode = Integer.valueOf(
                        mButtonPreferredNetworkMode.getValue()).intValue();
                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        networkMode);
//LGE_TD3658 update use 2G checkbox after changing Network mode START
		mButtonPrefer2g.update();
//LGE_TD3658 END
            } else {
                mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            }
        }

        private void resetNetworkModeToDefault() {
            if (mPhone.getPhoneName().equals("CDMA")) {
                //set the mButtonPreferredNetworkMode
                mButtonPreferredNetworkMode.setValue(Integer.toString(preferredNetworkMode));
            } else {
            	// LGE_MOBILE_NETWORK_TYPE START
            	//set the mButtonPreferredNetworkMode
            	mButtonPreferredNetworkMode.setValue(Integer.toString(preferredNetworkMode));
            	// LGE_MOBILE_NETWORK_TYPE END
            }
            //set the Settings.System
            android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode );
            //Set the Modem
            mPhone.setPreferredNetworkType(preferredNetworkMode,
                    this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
        }

        private void handleQueryCdmaRoamingPreference(Message msg) {
            mPhone = PhoneFactory.getDefaultPhone();
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int statusCdmaRoamingMode = ((int[])ar.result)[0];
                int settingsRoamingMode = android.provider.Settings.Secure.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.CDMA_ROAMING_MODE,
                        PREFERRED_CDMA_ROAMING_MODE);
                //check that statusCdmaRoamingMode is from an accepted value
                if (statusCdmaRoamingMode == Phone.CDMA_RM_HOME ||
                        statusCdmaRoamingMode == Phone.CDMA_RM_AFFILIATED ||
                        statusCdmaRoamingMode == Phone.CDMA_RM_ANY ) {
                    //check changes in statusCdmaRoamingMode and updates settingsRoamingMode
                    if (statusCdmaRoamingMode != settingsRoamingMode) {
                        settingsRoamingMode = statusCdmaRoamingMode;
                        //changes the Settings.System accordingly to statusCdmaRoamingMode
                        android.provider.Settings.Secure.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Secure.CDMA_ROAMING_MODE,
                                settingsRoamingMode );
                    }
                    //changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    mButtonCdmaRoam.setValue(
                            Integer.toString(statusCdmaRoamingMode));
                } else {
                    resetCdmaRoamingModeToDefault();
                }
            }
        }

        private void handleSetCdmaRoamingPreference(Message msg) {
            mPhone = PhoneFactory.getDefaultPhone();
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int cdmaRoamingMode = Integer.valueOf(mButtonCdmaRoam.getValue()).intValue();
                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.CDMA_ROAMING_MODE,
                        cdmaRoamingMode );
            } else {
                mPhone.queryCdmaRoamingPreference(obtainMessage(MESSAGE_QUERY_ROAMING_PREFERENCE));
            }
        }

        private void resetCdmaRoamingModeToDefault() {
            mPhone = PhoneFactory.getDefaultPhone();
            //set cdmaRoamingMode to default
            int cdmaRoamingMode = PREFERRED_CDMA_ROAMING_MODE;
            int statusCdmaRoamingMode = Phone.CDMA_RM_HOME;
            //set the mButtonCdmaRoam
            mButtonCdmaRoam.setValue(Integer.toString(cdmaRoamingMode));
            //set the Settings.System
            android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.CDMA_ROAMING_MODE,
                        cdmaRoamingMode );
            //Set the Status
            mPhone.setCdmaRoamingPreference(statusCdmaRoamingMode,
                    this.obtainMessage(MyHandler.MESSAGE_SET_ROAMING_PREFERENCE));
        }
    }


    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch(NetworkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                // TODO T: Make all of these strings come from res/values/strings.xml.
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: WCDMA pref");
                break;
            case Phone.NT_MODE_GSM_ONLY:
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: GSM only");
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: WCDMA only");
                break;
            case Phone.NT_MODE_GSM_UMTS:
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: GSM/WCDMA");
                break;
            case Phone.NT_MODE_CDMA:
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: CDMA / EvDo");
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: CDMA only");
                break;
            case Phone.NT_MODE_EVDO_NO_CDMA:
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: EvDo only");
                break;
            case Phone.NT_MODE_GLOBAL:
            default:
                mButtonPreferredNetworkMode.setSummary("Preferred network mode: Global");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the system selection Options
                mButtonCdmaRoam.showDialog(null);
            } else {
                // do nothing
            }
            break;

        default:
            break;
        }
    }


// LGE_CUSTOMER_SERVICE_PROFILE START
    private void checkCustomerServiceProfile() {
        findPreference("button_carrier_sel_key").setEnabled(mPhone.isServiceCustomerAccessible(CspConstants.GROUPCODE_VALUEADDED_SERVICES,
                                                           CspConstants.SERVICE_PLMN_MODE));
//        findPreference("button_preferred_networks_key").setEnabled(mPhone.isServiceCustomerAccessible(CspConstants.GROUPCODE_VALUEADDED_SERVICES,
//                CspConstants.SERVICE_PLMN_MODE));
        findPreference(KEY_NET_PREF).setEnabled(mPhone.isServiceCustomerAccessible(CspConstants.GROUPCODE_PHASE2_SERVICES,
                                               CspConstants.SERVICE_MULTIPLE_BAND));
    }
// LGE_CUSTOMER_SERVICE_PROFILE END

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
