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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
// LGE_PREFERRED_NETWORKS_FEATURE START
import android.net.Uri;
import android.content.ContentValues;
// LGE_PREFERRED_NETWORKS_FEATURE END
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.util.Log;

// LGE_PREFERRED_NETWORKS_FEATURE START
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.database.Cursor;
// LGE_PREFERRED_NETWORKS_FEATURE END

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.NetworkInfo;

import java.util.HashMap;
import java.util.List;

//2010.10.16 seungjun.seo@lge.com SKT Roaming CheckBox Display [START_LGE_LAB1]
import com.lge.config.StarConfig;
import com.lge.IBuildInfo;
import com.lge.ICarrier;
import android.text.TextUtils;
import android.telephony.ServiceState;
import android.os.ServiceManager;
import android.app.AlertDialog;
import com.android.internal.telephony.gsm.NetworkInfo.State;
import com.android.internal.telephony.DataConnectionManager;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.Phone;
//2010.10.16 seungjun.seo@lge.com SKT Roaming CheckBox Display [END_LGE_LAB1]

import com.android.internal.telephony.DataConnectionManager;
import android.net.ConnectivityManager;

/**
 * "Networks" settings UI for the Phone app.
 */
public class NetworkSetting extends PreferenceActivity
// LGE_PREFERRED_NETWORKS_FEATURE START
        implements View.OnCreateContextMenuListener, DialogInterface.OnCancelListener {
// LGE_PREFERRED_NETWORKS_FEATURE END

    private static final String LOG_TAG = "phone";
    private static final boolean DBG = true;

    private static final int EVENT_NETWORK_SCAN_COMPLETED = 100;
    private static final int EVENT_NETWORK_SELECTION_DONE = 200;
    private static final int EVENT_AUTO_SELECT_DONE = 300;

	//2010.10.16 seungjun.seo@lge.com SKT T Roaming CheckBox Add [START_LGE_LAB1]
	private static final int EVENT_CARRIER_SELECT_MODE_DONE = 400;
	private static final int EVENT_CARRIER_SELECT_MODE_REGISTED = 500;
	private static final int DIALG_NETWORK_SELECTION_FAILED_ON_MANUAL = 400;
	//2010.10.16 seungjun.seo@lge.com SKT T Roaming CheckBox Add [END_LGE_LAB1]	

    //dialog ids
    private static final int DIALOG_NETWORK_SELECTION = 100;
    private static final int DIALOG_NETWORK_LIST_LOAD = 200;
    private static final int DIALOG_NETWORK_AUTO_SELECT = 300;
    // LGE_PREFERRED_NETWORKS_FEATURE START
    private static final int DIALOG_NETWORK_ADD_TO_PREFERRED = 400;
    // LGE_PREFERRED_NETWORKS_FEATURE END

    //String keys for preference lookup
    private static final String LIST_NETWORKS_KEY = "list_networks_key";
    private static final String BUTTON_SRCH_NETWRKS_KEY = "button_srch_netwrks_key";
    private static final String BUTTON_AUTO_SELECT_KEY = "button_auto_select_key";

    //map of network controls to the network data.
    private HashMap<Preference, NetworkInfo> mNetworkMap;
    
    // LGE_PREFERRED_NETWORKS_FEATURE START
    private static final int REGISTER_ON_NETWORK = 1;
    private static final int ADD_TO_PREFERRED = 2;
    private static final int SEARCH_NETWORKS = 3;
    private static final int SELECT_AUTOMATICALLY = 4;
    
    private static final String PREFERRED_NETWORKS_URI = "content://preferred-networks/raw";
    private static final Uri CONTENT_URI = Uri.parse(PREFERRED_NETWORKS_URI);
    private static final String LONGALPHA_TAG = "alphalong";
    private static final String SHORTALPHA_TAG = "alphashort";
    private static final String NUMERIC_TAG = "numeric";
    // LGE_PREFERRED_NETWORKS_FEATURE END
    
    Phone mPhone;
    protected boolean mIsForeground = false;

    /** message for network selection */
    String mNetworkSelectMsg;
    // LGE_PREFERRED_NETWORKS_FEATURE START
    String mNetworkAddMsg;
    // LGE_PREFERRED_NETWORKS_FEATURE END

	//2010.10.16 seungjun.seo@lge.com SKT T Roaming CheckBox Add [START_LGE_LAB1]
	ITelephony iTelephony;
	DataConnectionManager dataMgr;
	CommandsInterface mCm;
	private int m3GNetworkMode;

	private Context mContext;    

	private static CheckBoxPreferenceCarrier mSearchButtonKor;
	private CheckBoxPreferenceCarrier mAutoSelectKor;

	private Preference mRegistedCarrier = null;

	private static final int EVENT_GET_USIM_TYPE = 63;
	private static final int WLGE_NO_USIM = 0x00;
	private static final int WLGE_SKT_USIM = 0x01;
	private static final int WLGE_KT_USIM = 0x02;	
	private static int usimTypeValue = -1;
	
	private NetProgress mNetProgress = null;
	static ProgressDialog tempPD;		
	private static boolean bManualSelect = false;

    private static class LGEBuild{
    	public static boolean ENABLE_FORBIDDEN_CARRIER_SELECTABLE 			= IBuildInfo.Carrier == ICarrier.SKT;
    	public static boolean USE_MULTILINE_TITLE_PREFER_FOR_CARRIER_LIST = IBuildInfo.Carrier == ICarrier.SKT;
    	public static boolean USE_FORBIDDEN_CARRIER_CUSTOM_COLOR			= false;
    	public static boolean ENABLE_ASAP_LOAD_NETWORK_LIST 				= false;
    	public static boolean ENABLE_RESTART_NETWORK_OPERATION_ON_RESUME 	= true;
    	public static boolean USE_CHECKBOX_CARRIER_SELECT_MODE = true;
    	public static boolean DISPLAY_REGISTED_CARRIER = true;
    	public static boolean ENABLE_MANUALL_CARRIER_SELECTABLE_ON_NETWORK_REJECT = IBuildInfo.Carrier != ICarrier.KT;
    	public static int THRESHOLD_SAVE_CARRIER_LIST = 10;
    };	
    private static final int DISABLE_MANUAL_CARRIER_SELECT_STATUS_ID[] = {
    	2, // STATUS_IMSI_UNKNOWN
    	3, // STATUS_ILLEGAL_MS
    	6, // STATUS_ILLEGAL_ME
    	8, // STATUS_LU_FAIL
    };		
	//2010.10.16 seungjun.seo@lge.com SKT T Roaming CheckBox Add [END_LGE_LAB1]


    //preference objects
    private PreferenceGroup mNetworkList;
    private Preference mSearchButton;
    private Preference mAutoSelect;

	private SharedPreferences sp = null;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
			int[] res;
			Log.i(LOG_TAG, "msg.what = " + msg.what); 
						
            switch (msg.what) {
					//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
			 	case EVENT_CARRIER_SELECT_MODE_DONE:
					break;
					//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
					
                case EVENT_NETWORK_SCAN_COMPLETED:
                    networksListLoaded ((List<NetworkInfo>) msg.obj, msg.arg1);
					Log.i(LOG_TAG, "[EVENT_NETWORK_SCAN_COMPLETED] msg.obj = " +  msg.obj); 
                    break;

                case EVENT_NETWORK_SELECTION_DONE:
                    if (DBG) log("hideProgressPanel");

					//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
					if(StarConfig.OPERATOR.equals("SKT"))
					{
	                    mNetProgress.onEndOperation();
	                    setPreferenceEnabled(true);					
					}
					else
					{
	                    removeDialog(DIALOG_NETWORK_SELECTION);
	                    getPreferenceScreen().setEnabled(true);
	// LGE_ECLAIR_PORTING START
	                    updateForbiddenNetworks();
	// LGE_ECLAIR_PORTING END
            		}
					//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
					
                    ar = (AsyncResult) msg.obj;
					res = (int[])ar.result;

					Log.i(LOG_TAG, "ar.result  = " + ar.result );
					Log.d(LOG_TAG, "res  = " + res);
					Log.i(LOG_TAG, "ar.exception = " + ar.exception );

					if(StarConfig.OPERATOR.equals("SKT"))
					{
						sp = PreferenceManager.getDefaultSharedPreferences(NetworkSetting.this);
						sp.edit().putString(PhoneBase.NETWORK_SELECTION_NAME_KEY, "true").commit();
					} 	

                    if (ar.exception != null) {
                        if (DBG) log("manual network selection: failed!");
                        displayNetworkSelectionFailed(ar.exception);
                    } else {
                        if (DBG) log("manual network selection: succeeded!");
						//Manual Seleting... Other Key exception Add									
                        displayNetworkSelectionSucceeded(); 											
                    }
                    break;
                case EVENT_AUTO_SELECT_DONE:
                    if (DBG) log("hideProgressPanel");

                    if (mIsForeground) {
                        dismissDialog(DIALOG_NETWORK_AUTO_SELECT);
                    }
                    getPreferenceScreen().setEnabled(true);
// LGE_ECLAIR_PORTING START
                    updateForbiddenNetworks();
// LGE_ECLAIR_PORTING END

                    ar = (AsyncResult) msg.obj;
					res = (int[])ar.result;

					Log.i(LOG_TAG, "[EVENT_AUTO_SELECT_DONE] msg.obj = " +  msg.obj); 
					Log.d(LOG_TAG, "(int[])ar.result  = " + (int[])ar.result);
					Log.d(LOG_TAG, "res  = " + res);
					Log.i(LOG_TAG, "ar.exception = " + ar.exception);

                    if (ar.exception != null) {
                        if (DBG) log("automatic network selection: failed!");
                        displayNetworkSelectionFailed(ar.exception);
                    } else {
                        if (DBG) log("automatic network selection: succeeded!");
                        displayNetworkSelectionSucceeded();						
                    } 					  															
                    break;
            }

            return;
        }
    };

    /**
     * Service connection code for the NetworkQueryService.
     * Handles the work of binding to a local object so that we can make
     * the appropriate service calls.
     */

    /** Local service interface */
    private INetworkQueryService mNetworkQueryService = null;

    /** Service connection */
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {

        /** Handle the task of binding the local object to the service */
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) log("connection created, binding local service.");
            mNetworkQueryService = ((NetworkQueryService.LocalBinder) service).getService();
            // as soon as it is bound, run a query.
			//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
			if(StarConfig.OPERATOR.equals("SKT"))
			{
	            setPreferenceEnabled(true);			
				if(LGEBuild.ENABLE_ASAP_LOAD_NETWORK_LIST == true /*&& mNetworkListLoaded == false*/){
					loadNetworksListKor(true);
	            }else{
	            	mNetProgress.onServiceConnected();
	            }				
			}
			else
			{
				//20101117 jongwany.lee PVG TD issue #51112 fixed - start
	            //loadNetworksList();
				//20101117 jongwany.lee PVG TD issue #51112 fixed - end
			}
			//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
			
        }

        /** Handle the task of cleaning up the local binding */
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) log("connection disconnected, cleaning local binding.");
            mNetworkQueryService = null;
        }
    };

    /**
     * This implementation of INetworkQueryServiceCallback is used to receive
     * callback notifications from the network query service.
     */
    private final INetworkQueryServiceCallback mCallback = new INetworkQueryServiceCallback.Stub() {

        /** place the message on the looper queue upon query completion. */
        public void onQueryComplete(List<NetworkInfo> networkInfoArray, int status) {
            if (DBG) log("notifying message loop of query completion.");
            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED,
                    status, 0, networkInfoArray);
            msg.sendToTarget();
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean handled = false;
		String networkStr;
		Message msg;

		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
			if (DBG) log("onPreferenceTreeClick [handled] =" + handled);
	        if (preference == mSearchButtonKor) {
	        	boolean manualSelectable = true;     	

				if(manualSelectable){
					if(LGEBuild.USE_CHECKBOX_CARRIER_SELECT_MODE){
						if(mSearchButtonKor.isChecked() == false){
							mSearchButtonKor.setChecked(true);
						}
						((CheckBoxPreference) mAutoSelectKor).setChecked(false);
		        		}
		        	}else{
		        		mSearchButtonKor.setChecked(!mSearchButtonKor.isChecked());
		        		mSearchButtonKor.setEnabled(false);
		        		showDialog(DIALG_NETWORK_SELECTION_FAILED_ON_MANUAL);
		        		return true;
		        	}
					mIsForeground = true;
					loadNetworksListKor(true);							

					handled = true;
	        } else if (preference == mAutoSelectKor) {
	        	if(LGEBuild.USE_CHECKBOX_CARRIER_SELECT_MODE){
	        		if(((CheckBoxPreference)mAutoSelectKor).isChecked() == false){
	        			((CheckBoxPreference) mAutoSelectKor).setChecked(true);
	        		}
	        		mSearchButtonKor.setChecked(false);
	        	}	        	
	            selectNetworkAutomatic();
	            handled = true;
	        }
			else if(preference == mRegistedCarrier){
				handled = false;
			}					
	        else {
				if (DBG) log("select network !!");								
			
	            Preference selectedCarrier = preference;

	            networkStr = selectedCarrier.getTitle().toString();
	            if (DBG) log("selected network: " + networkStr);

				if(bManualSelect == false)
				{
					msg = mHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);
					mPhone.selectNetworkManually(mNetworkMap.get(selectedCarrier), msg);
					
					displayNetworkSeletionInProgress(networkStr);
				}
				
	            handled = true;		
	        }
		}
		else
		{
	        if (preference == mSearchButton) {
	            loadNetworksList();
	            handled = true;
	        } else if (preference == mAutoSelect) {
	            selectNetworkAutomatic();
	            handled = true;
	        } else {
	            Preference selectedCarrier = preference;

	            networkStr = selectedCarrier.getTitle().toString();
	            if (DBG) log("selected network: " + networkStr);

	           msg = mHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);
	            mPhone.selectNetworkManually(mNetworkMap.get(selectedCarrier), msg);

	            displayNetworkSeletionInProgress(networkStr);

	            handled = true;
	        }
		}
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
		
        return handled;
    }

    //implemented for DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        // request that the service stop the query with this callback object.
        try {
            mNetworkQueryService.stopNetworkQuery(mCallback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        finish();
    }

    public String getNormalizedCarrierName(NetworkInfo ni) {
        if (ni != null) {
            return ni.getOperatorAlphaLong() + " (" + ni.getOperatorNumeric() + ")";
        }
        return null;
    }

	//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
    private boolean checkKT(NetworkInfo ni)
    {
        boolean result=false;
        int mcc = Integer.parseInt(ni.getOperatorNumeric().substring(0,3));
        int mnc = Integer.parseInt(ni.getOperatorNumeric().substring(3,ni.getOperatorNumeric().length()));

        log("checkKT ni.getOperatorAlphaLong() = " + ni.getOperatorAlphaLong() );
        log("checkKT mmc = " + mcc + " mnc = " + mnc );
	
        if(TextUtils.isEmpty(ni.getOperatorAlphaLong()) && (mcc == 450 && ( mnc== 8 || mnc == 2)) )
            result = true;
        log("checkKT result = " + result );
        return result;
    }	
    private String makeSummary(NetworkInfo ni) {
    	StringBuffer summary = new StringBuffer();
		// phjjiny.park 100803  KT search result format : O [W] KT MCC 450 MNC 08 => W KT 450 08(O)
		/*
		GSM (0),
		GSM_COMPACT (1),
		UMTS (2);
		*/
		
		int mncLen = ni.getOperatorNumeric().length();	//phjjiny.park 100826
		log("ni.getOperatorNumeric()" + ni.getOperatorNumeric()+ "length = "+ mncLen);
		log("ni.getState().toString()= "+ ni.getState().toString());
		log("ni.getOperatorAlphaLong()= "+ ni.getOperatorAlphaLong());
		log("ni.getRadioAccessTechnology()= "+ ni.getRadioAccessTechnology());

		if (IBuildInfo.Carrier == ICarrier.SKT) {
			if(ni.getState().toString()=="CURRENT" || ni.getState().toString() == "AVAILABLE")
				summary.append("O");
			else
				summary.append("X");

			if (ni.getRadioAccessTechnology()== NetworkInfo.RadioAccessTechnology.UMTS)
				summary.append(" [W]");
			else
				summary.append(" [G]");

			summary.append(" " + ni.getOperatorAlphaLong());
			summary.append(" MCC " + ni.getOperatorNumeric().substring(0,3));
			summary.append(" MNC " + ni.getOperatorNumeric().substring(3,mncLen));
		} else {
			if (ni.getRadioAccessTechnology()== NetworkInfo.RadioAccessTechnology.UMTS)
				summary.append(" [W]");
			else
				summary.append(" [G]");

			if(checkKT(ni))
				summary.append(" " + "KT");
			else
				summary.append(" " + ni.getOperatorAlphaLong());

			summary.append(" " + ni.getOperatorNumeric().substring(0,3));
			summary.append(" " + ni.getOperatorNumeric().substring(3,mncLen));
			if(ni.getState().toString()=="CURRENT" || ni.getState().toString() == "AVAILABLE")
				summary.append(" (O)");
			else
				summary.append(" (X)");
		}
		return summary.toString();
	}
	private void addCarrier(NetworkInfo ni){
		final CarrierInfoPreference carrier = new CarrierInfoPreference(this, null);
		carrier.setStyle(CarrierInfoPreference.STYLE_MULTI_LINE_TITLE);
		carrier.setTitle(makeSummary(ni));
		if( checkKT(ni) )
			carrier.setSummary("KT");
		else
			carrier.setSummary(ni.getOperatorAlphaLong());
		
		if (DBG) log("addCarrier ni:	" + ni);		

		if (LGEBuild.ENABLE_FORBIDDEN_CARRIER_SELECTABLE) {
		} else {
			if (usimTypeValue == WLGE_KT_USIM || usimTypeValue == WLGE_NO_USIM) {
				int mcc = Integer.parseInt(ni.getOperatorNumeric().substring(0,3));
				int mnc = Integer.parseInt(ni.getOperatorNumeric().substring(3,ni.getOperatorNumeric().length())); //phjjiny.park
				if (mcc == 450 && mnc == 5)  // SKT
					carrier.setEnabled(false);
				else
					carrier.setEnabled(true);
			}
		}

		carrier.setPersistent(false);
		
		mNetworkList.addPreference(carrier);
		mNetworkMap.put(carrier, ni);
	}
	//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		//2010.10.12 seungjun.seo SKT T Roaming CheckBox Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
			addPreferencesFromResource(R.xml.carrier_select_kor);
		}
		else
		{
	        addPreferencesFromResource(R.xml.carrier_select);
		}
		//2010.10.12 seungjun.seo SKT T Roaming CheckBox Add [END_LGE_LAB1]

        mPhone = PhoneApp.getInstance().phone;

        mNetworkList = (PreferenceGroup) getPreferenceScreen().findPreference(LIST_NETWORKS_KEY);
        mNetworkMap = new HashMap<Preference, NetworkInfo>();

		//2010.10.12 seungjun.seo SKT T Roaming CheckBox Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
			//Close 3G Network
			dataMgr = new DataConnectionManager(this);		
			m3GNetworkMode = dataMgr.getDataNetworkMode(false);		
			dataMgr.closeNetwork();		

			mNetProgress = new NetProgress();		
			setPreferenceEnabled(false);
			mSearchButtonKor = (CheckBoxPreferenceCarrier) getPreferenceScreen().findPreference(BUTTON_SRCH_NETWRKS_KEY);
			mAutoSelectKor = (CheckBoxPreferenceCarrier) getPreferenceScreen().findPreference(BUTTON_AUTO_SELECT_KEY);
					
			mSearchButtonKor.setEnabled(false);
			if(LGEBuild.USE_CHECKBOX_CARRIER_SELECT_MODE){
				mAutoSelectKor.setEnabled(false);				
				mSearchButtonKor.setPersistent(true);
				mAutoSelectKor.setPersistent(true);
			}

			if(LGEBuild.DISPLAY_REGISTED_CARRIER){
				Preference pref = new Preference(this);
				ServiceState ss = mPhone.getServiceState();
				String carrier = ss.getOperatorAlphaLong();
				String mccMnc = ss.getOperatorNumeric();				
				if(DBG)log( "CARRIER=" + carrier);
				if(DBG)log( "MCC/MNC=" + mccMnc);
				if(TextUtils.isEmpty(carrier)){
					carrier = getResources().getString(R.string.unknown_carrier);
					pref.setSummary(getResources().getString(R.string.unknown_mccmnc));
				}else{	//phjjiny.park 100826 romming "310410"
					pref.setSummary("MCC: " + mccMnc.substring(0, 3) + ", MNC: " + mccMnc.substring(3, mccMnc.length()));
				}
				pref.setTitle(carrier);
				pref.setEnabled(false);
				mRegistedCarrier = pref;
				mNetworkList.addPreference(pref);

				sp = PreferenceManager.getDefaultSharedPreferences(NetworkSetting.this);
				String networkSelection = sp.getString(PhoneBase.NETWORK_SELECTION_NAME_KEY, "");
			
				if (DBG) log("TextUtils.isEmpty(networkSelection)..." + TextUtils.isEmpty(networkSelection));
				if (TextUtils.isEmpty(networkSelection))
				{
					if (DBG) log("updateCarrierSelectModePrefs[AUTO]");
					updateCarrierSelectModePrefs(true); 					
				} 									
				else
				{
					if (DBG) log("updateCarrierSelectModePrefs[MANUAL]");
					updateCarrierSelectModePrefs(false);
				} 				
			}				
		}
		else 
		{
	        mSearchButton = getPreferenceScreen().findPreference(BUTTON_SRCH_NETWRKS_KEY);
	        mAutoSelect = getPreferenceScreen().findPreference(BUTTON_AUTO_SELECT_KEY);
		}
		//2010.10.12 seungjun.seo SKT T Roaming CheckBox Add [END_LGE_LAB1]
		
        // Start the Network Query service, and bind it.
        // The OS knows to start he service only once and keep the instance around (so
        // long as startService is called) until a stopservice request is made.  Since
        // we want this service to just stay in the background until it is killed, we
        // don't bother stopping it from our end.
        startService (new Intent(this, NetworkQueryService.class));
        bindService (new Intent(this, NetworkQueryService.class), mNetworkQueryServiceConnection,
                Context.BIND_AUTO_CREATE);
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
	        if(icicle != null){
	        	int carrierListCount = 0;
	        	if(icicle.containsKey("carrierListCount")){
	        		carrierListCount = icicle.getInt("carrierListCount");
	        	}
	        	for(int index=0; index < carrierListCount; index++){
	        		String niStr = icicle.getString("carrierList[" + index + "]");
	        		if(DBG) log("->carrierList[" + index + "]=[" + niStr + "]");
	        		String[] niStrSeg = niStr.split(";");
       		
	        		NetworkInfo ni = new NetworkInfo(niStrSeg[0], niStrSeg[1], niStrSeg[2], niStrSeg[3], niStrSeg[4]);
	        		addCarrier(ni);
	        	}
	        }
	        mNetProgress.onCreateActivity(this, icicle);					
		}        				
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]		

        // LGE_PREFERRED_NETWORKS_FEATURE START
        registerForContextMenu(getListView());
        // LGE_PREFERRED_NETWORKS_FEATURE END
    }

    @Override
    public void onResume() {
        super.onResume();
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
			mIsForeground = false;
		}
		else
		{			
	        mIsForeground = true;
		}
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
    }

	public void onStop() {
		super.onStop();
		if(StarConfig.OPERATOR.equals("SKT"))
		{
 			if(DBG)log("onStop()");
			if(DBG)log("->stop NetworkQuery");
			
			try {
				if(mNetworkQueryService != null)
					mNetworkQueryService.stopNetworkQuery(mCallback);
			} catch (RemoteException e) {
				if(DBG) log("RemoteException on stopNetworkQuery()" + e);
				throw new RuntimeException(e);
			}			
			mNetProgress.onStopActivity(this);
		}
	}


    @Override
    public void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    /**
     * Override onDestroy() to unbind the query service, avoiding service
     * leak exceptions.
     */
    @Override
    protected void onDestroy() {
        // unbind the service.
		if(DBG)log("onDestroy()");
        unbindService(mNetworkQueryServiceConnection);
        super.onDestroy();
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
			if( m3GNetworkMode == DataConnectionManager.DCM_MOBILE_NETWORK_IS_ALLOWED ) {
				ConnectivityManager  mConnMgr = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
				mConnMgr.setMobileDataEnabled(true);
			}		
			//Open 3G Network
			//dataMgr.openNetwork();				
			//Manual List Select Flag Init
			bManualSelect = false;
		}
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
    }

// LGE_PREFERRED_NETWORKS_FEATURE START
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

	if(StarConfig.OPERATOR.equals("SKT") == false){
//20101117 jongwany.lee PVG TD issue #51112 fixed - START
			//menu.add(0, REGISTER_ON_NETWORK, 0, R.string.menu_register_net);
			// 20100514 chopark2@lge.com Delete menu in Network operators	
	    	// menu.add(0, ADD_TO_PREFERRED, 0, R.string.menu_add_to_preferred);
	    	//menu.add(0, SEARCH_NETWORKS, 0, R.string.menu_search_networks);
	    	//menu.add(0, SELECT_AUTOMATICALLY, 0, R.string.menu_select_automatically);
//20101117 jongwany.lee PVG TD issue #51112 fixed - END
	}
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	
    	Preference selectedCarrier = (Preference) getListView().getSelectedItem();

		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		//if(StarConfig.OPERATOR.equals("SKT"))
			if(bManualSelect)
			{
				if (DBG) log("Already Manual Selected !!");
				return false;
			}
    	//}
			if (DBG) log("Already Manual Selected !!");
    	
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
    	
    	switch(item.getItemId()){
    	case REGISTER_ON_NETWORK: {	
    		if (mNetworkMap.get(selectedCarrier) != null) {
    		String networkStr = selectedCarrier.getTitle().toString();
            if (DBG) log("selected network: " + networkStr);

            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);
            mPhone.selectNetworkManually(mNetworkMap.get(selectedCarrier), msg);

            displayNetworkSeletionInProgress(networkStr);
    		}
            return true;
    	}
    	case ADD_TO_PREFERRED:{
    		addToPreferred(selectedCarrier);
    		return true;
    	}
    	case SEARCH_NETWORKS:{
    		if(StarConfig.OPERATOR.equals("SKT"))
    		{
    			loadNetworksList();
    		}else{
    			//20101117 jongwany.lee PVG TD issue #51112 fixed - START	
        		if(mNetworkQueryService != null){
        			loadNetworksList();
        		}
        		//20101117 jongwany.lee PVG TD issue #51112 fixed - START
    		}
    		return true;
    	}
    	case SELECT_AUTOMATICALLY:{
    		 selectNetworkAutomatic();
    		 return true;
    	}
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
    	
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    	Preference selectedCarrier = (Preference) getPreferenceScreen().getRootAdapter().getItem(info.position);
    	if (mNetworkMap.get(selectedCarrier) != null){    	
    	    	
    		super.onCreateContextMenu(menu, v, menuInfo);
    		menu.add(0, REGISTER_ON_NETWORK, 0, R.string.menu_register_net);
		// 20100514 chopark2@lge.com Delete menu in Network operators
    		// menu.add(0, ADD_TO_PREFERRED, 0, R.string.menu_add_to_preferred);
    		//menu.add(0, SEARCH_NETWORKS, 0, R.string.menu_search_networks);
    		//menu.add(0, SELECT_AUTOMATICALLY, 0, R.string.menu_select_automatically);
    	}
    }
   
    @Override
    public boolean onContextItemSelected(MenuItem item){
    	 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		 if (DBG) log("item.getItemId(): " + item.getItemId());
         
    	switch(item.getItemId()) {
    	case REGISTER_ON_NETWORK: {
    		Preference selectedCarrier = (Preference) getPreferenceScreen().getRootAdapter().getItem(info.position);

            String networkStr = selectedCarrier.getTitle().toString();
            if (DBG) log("selected network: " + networkStr);

            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);
            mPhone.selectNetworkManually(mNetworkMap.get(selectedCarrier), msg);

            displayNetworkSeletionInProgress(networkStr);
            return true;
    	}
    	case ADD_TO_PREFERRED: {
    		Preference selectedCarrier = (Preference) getPreferenceScreen().getRootAdapter().getItem(info.position);
    		addToPreferred(selectedCarrier);
    		return true;
    	}
    /*	case SEARCH_NETWORKS:{
    		loadNetworksList();
    		return true;
    	}
    	case SELECT_AUTOMATICALLY:{
    		 selectNetworkAutomatic();
    		 return true;
    	}
    */
    	}
    	return super.onContextItemSelected(item);
    }
    
    private void addToPreferred(Preference preference){
    	Uri updateUri;
    	Cursor cur;
    	boolean success = false, alreadyhave;
    	int longname_ind,freepos,i;
    	String numStr, nameStr;
    	String name;
    	
    	if (preference == null || mNetworkMap == null) {
    		return;
    	}
    	numStr = mNetworkMap.get(preference).getOperatorNumeric();
    	nameStr = mNetworkMap.get(preference).getOperatorAlphaLong();
    	if (nameStr == null) {
    		return;
    	}
		try {
		    /* find empty slot and check if network is already there */
            displayNetworkAddToPreferredInProgress(nameStr);
			cur = managedQuery(Uri.parse(PREFERRED_NETWORKS_URI),null,null,null);
			if (cur != null) {
			    longname_ind = cur.getColumnIndexOrThrow(LONGALPHA_TAG);
				if (cur.moveToFirst()) {
				    freepos = -1; i = 0; alreadyhave = false;
				    do {
				        name = cur.getString(longname_ind); 
					    if (name.equals("")) {
					        if (freepos == -1) {
						        /* empty position */
						        freepos = i;
						    }
					    } else if (name.equals(nameStr) || name.equals(numStr)){
						    /* already have */
						    alreadyhave = true;
					    }
					    i++;
				    } while (!alreadyhave && cur.moveToNext());
				    cur.close();
				    if (alreadyhave) {
					    success = true;
				    } else if (freepos != -1) {
      	                ContentValues newValues = new ContentValues();
    	                newValues.put(LONGALPHA_TAG, "");
		                newValues.put(SHORTALPHA_TAG, "");
		                newValues.put(NUMERIC_TAG, numStr);
			            updateUri = Uri.parse(PREFERRED_NETWORKS_URI+"/"+String.valueOf(freepos));
			            if (getContentResolver().update(updateUri,newValues,null,null) == 1) {
			                success = true;
			            }
				    }
				}
			}
	        removeDialog(DIALOG_NETWORK_ADD_TO_PREFERRED);
            getPreferenceScreen().setEnabled(true);
// LGE_ECLAIR_PORTING START
                        updateForbiddenNetworks();
// LGE_ECLAIR_PORTING END
			if (success) {
			    /* success */
				//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
				if(StarConfig.OPERATOR.equals("SKT"))
				{
					bManualSelect = true;
				}
				//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
					
			    displayNetworkAddToPreferredSucceeded();
			} else {
			    displayNetworkAddToPreferredFailed();				
			}
		} catch (NullPointerException e) {
	        removeDialog(DIALOG_NETWORK_ADD_TO_PREFERRED);
            getPreferenceScreen().setEnabled(true);
// LGE_ECLAIR_PORTING START
           updateForbiddenNetworks();
// LGE_ECLAIR_PORTING END
		    displayNetworkAddToPreferredFailed();		
		}
    }
    // LGE_PREFERRED_NETWORKS_FEATURE END
    @Override
    protected Dialog onCreateDialog(int id) {
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
	    	if(id == DIALG_NETWORK_SELECTION_FAILED_ON_MANUAL){
	    		return new AlertDialog.Builder(this)
	    						.setNeutralButton(R.string.ok, null)
	    						.setMessage(R.string.manuall_select_network_failed)
	    						.create();
	    	}		
		}
		else
		{
	        if ((id == DIALOG_NETWORK_SELECTION) || (id == DIALOG_NETWORK_LIST_LOAD) ||
	// LGE_PREFERRED_NETWORKS_FEATURE START
	                (id == DIALOG_NETWORK_AUTO_SELECT) || (id == DIALOG_NETWORK_ADD_TO_PREFERRED)) {
	// LGE_PREFERRED_NETWORKS_FEATURE END
	            ProgressDialog dialog = new ProgressDialog(this);
	            switch (id) {
	                case DIALOG_NETWORK_SELECTION:
	                    // It would be more efficient to reuse this dialog by moving
	                    // this setMessage() into onPreparedDialog() and NOT use
	                    // removeDialog().  However, this is not possible since the
	                    // message is rendered only 2 times in the ProgressDialog -
	                    // after show() and before onCreate.
	                    dialog.setMessage(mNetworkSelectMsg);
	                    dialog.setCancelable(false);
	                    dialog.setIndeterminate(true);
	                    break;
	                case DIALOG_NETWORK_AUTO_SELECT:
	                    dialog.setMessage(getResources().getString(R.string.register_automatically));
	                    dialog.setCancelable(false);
	                    dialog.setIndeterminate(true);
	                    break;
	// LGE_PREFERRED_NETWORKS_FEATURE START
	                case DIALOG_NETWORK_ADD_TO_PREFERRED:
	                	 dialog.setMessage(mNetworkAddMsg);
	                     dialog.setCancelable(false);
	                     dialog.setIndeterminate(true);
	                     break;  
	// LGE_PREFERRED_NETWORKS_FEATURE END
	                case DIALOG_NETWORK_LIST_LOAD:
	                default:
	                    // reinstate the cancelablity of the dialog.
	                    dialog.setMessage(getResources().getString(R.string.load_networks_progress));
	                    dialog.setCancelable(true);
	                    dialog.setOnCancelListener(this);
	                    break;
	            }
	            return dialog;
	        }
    	}
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]		
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if ((id == DIALOG_NETWORK_SELECTION) || (id == DIALOG_NETWORK_LIST_LOAD) ||
// LGE_PREFERRED_NETWORKS_FEATURE START
                (id == DIALOG_NETWORK_AUTO_SELECT) || (id == DIALOG_NETWORK_ADD_TO_PREFERRED)) {
// LGE_PREFERRED_NETWORKS_FEATURE END
            // when the dialogs come up, we'll need to indicate that
            // we're in a busy state to dissallow further input.
            getPreferenceScreen().setEnabled(false);
        }
    }

    private void displayEmptyNetworkList(boolean flag) {
        mNetworkList.setTitle(flag ? R.string.empty_networks_list : R.string.label_available);
    }

    private void displayNetworkSeletionInProgress(String networkStr) {
        // TODO: use notification manager?
        mNetworkSelectMsg = getResources().getString(R.string.register_on_network, networkStr);
		if (DBG) log("displayNetworkSeletionInProgress: " + networkStr);
		if (DBG) log("mIsForeground = " + mIsForeground);

        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_SELECTION);
        }
				
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
        	mNetProgress.onStartOperation(NetProgress.OPERATION_SELECTION, null, networkStr);		
		}
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]		
    }
// LGE_PREFERRED_NETWORKS_FEATURE START
    private void displayNetworkAddToPreferredInProgress(String networkStr){
        //TODO: use notification manager?
        mNetworkAddMsg = getResources().getString(R.string.add_to_preferred_networks, networkStr);

        showDialog(DIALOG_NETWORK_ADD_TO_PREFERRED);
    }

    private void displayNetworkAddToPreferredSucceeded(){
        Toast.makeText(getApplicationContext(), getString(R.string.network_to_preferred_success), Toast.LENGTH_SHORT).show();
    }

    private void displayNetworkAddToPreferredFailed(){
        Toast.makeText(getApplicationContext(), getString(R.string.network_to_preferred_failure), Toast.LENGTH_LONG).show();
  }
// LGE_PREFERRED_NETWORKS_FEATURE END

    private void displayNetworkQueryFailed(int error) {
        String status = getResources().getString(R.string.network_query_error);

        NotificationMgr.getDefault().postTransientNotification(
                        NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);
    }

    private void displayNetworkSelectionFailed(Throwable ex) {
        String status;

        if ((ex != null && ex instanceof CommandException) &&
                ((CommandException)ex).getCommandError()
                  == CommandException.Error.ILLEGAL_SIM_OR_ME)
        {
            status = getResources().getString(R.string.not_allowed);
        } else {
            status = getResources().getString(R.string.connect_later);
        }

        NotificationMgr.getDefault().postTransientNotification(
                        NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);
    }

    private void displayNetworkSelectionSucceeded() {
        String status = getResources().getString(R.string.registration_done);

        NotificationMgr.getDefault().postTransientNotification(
                        NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 3000);
    }

    private void loadNetworksList() {
        if (DBG) log("load networks list...");

        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_LIST_LOAD);
        }

        // delegate query request to the service.
        try {
            mNetworkQueryService.startNetworkQuery(mCallback);
        } catch (RemoteException e) {
        }

        displayEmptyNetworkList(false);
    }

	 private void loadNetworksListKor(boolean aShowProgress) {
        if (DBG) log("load networks list...");
        if(aShowProgress){
        	if(DBG) log("-> invoke startOperation()");
        	mNetProgress.onStartOperation(NetProgress.OPERATION_LIST_LOAD, null, null);
        }else{
        	if(DBG) log("-> do nothing on NetProgress");
        }
        try {
        		mNetworkQueryService.startNetworkQuery(mCallback);
        		if(DBG)	log("Query start");
        } catch (RemoteException e) {
        	if(DBG) log("RemoteException loadNetworkList" + e);
        }
        displayEmptyNetworkList(false);
    }

    /**
     * networksListLoaded has been rewritten to take an array of
     * NetworkInfo objects and a status field, instead of an
     * AsyncResult.  Otherwise, the functionality which takes the
     * NetworkInfo array and creates a list of preferences from it,
     * remains unchanged.
     */
    private void networksListLoaded(List<NetworkInfo> result, int status) {
        if (DBG) log("networks list loaded");

        // update the state of the preferences.
        if (DBG) log("hideProgressPanel");

		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{ 							
	        mNetProgress.onEndOperation();
	        setPreferenceEnabled(true);
	  
	        if(mRegistedCarrier != null){
	        	mNetworkList.removePreference(mRegistedCarrier);
	        	mRegistedCarrier = null;
        	}
		}
		else
		{
	        if (mIsForeground) {
	            dismissDialog(DIALOG_NETWORK_LIST_LOAD);
	        }

	        getPreferenceScreen().setEnabled(true);
    	}		
		//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
		
        clearList();

        if (status != NetworkQueryService.QUERY_OK) {
            if (DBG) log("error while querying available networks");
		//seungjun.seo Korea No SIM...GCF Test error Fix
		if(StarConfig.OPERATOR.equals("SKT") == false)
		{ 							
		    dismissDialog(DIALOG_NETWORK_LIST_LOAD);
        	}
            displayNetworkQueryFailed(status);
            displayEmptyNetworkList(true);
        } else {
            if (result != null){
                displayEmptyNetworkList(false);

                // create a preference for each item in the list.
                // just use the operator name instead of the mildly
                // confusing mcc/mnc.

				//2010.10.16 seungjun.seo@lge.com SKT RAD Add [START_LGE_LAB1]
                if(StarConfig.OPERATOR.equals("SKT"))
                {               
		            if (DBG) log("- NetworkInfo -");
					for (NetworkInfo ni : result) {
						addCarrier(ni);
					}
                }
				else
				{
	                for (NetworkInfo ni : result) {
	                    Preference carrier = new Preference(this, null);
	                    carrier.setTitle(ni.getOperatorAlphaLong());
	                    carrier.setPersistent(false);
	// LGE_ECLAIR_PORTING START
	                    if (ni.getRadioAccessTechnology() == NetworkInfo.RadioAccessTechnology.UMTS) {
	                        carrier.setSummary("UMTS");
	                    } else {
	                        carrier.setSummary("GSM");
	                    }
	                    carrier.setEnabled(ni.getState() != NetworkInfo.State.FORBIDDEN);
	// LGE_ECLAIR_PORTING END
	                    mNetworkList.addPreference(carrier);
	                    mNetworkMap.put(carrier, ni);

	                    if (DBG) log("  " + ni);
	                }
				}
				//2010.10.16 seungjun.seo@lge.com SKT RAD Add [END_LGE_LAB1]
				
            } else {
                displayEmptyNetworkList(true);
            }
			if (DBG) log ("networksListLoaded EVENT_NETWORK_SCAN_COMPLETED");
 						
        }
    }

    private void clearList() {
        for (Preference p : mNetworkMap.keySet()) {
            mNetworkList.removePreference(p);
        }
        mNetworkMap.clear();
		//2010.10.16 seungjun.seo@lge.com SKT Roaming Add [START_LGE_LAB1]
		if(StarConfig.OPERATOR.equals("SKT"))
		{
	        getPreferenceScreen().removeAll();		
		}
		//2010.10.16 seungjun.seo@lge.com SKT Roaming Add [END_LGE_LAB1]
    }

    private void selectNetworkAutomatic() {
        if (DBG) log("select network automatically...");
        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_AUTO_SELECT);
        }

        Message msg = mHandler.obtainMessage(EVENT_AUTO_SELECT_DONE);
        mPhone.setNetworkSelectionModeAutomatic(msg);
		if(StarConfig.OPERATOR.equals("SKT"))
		{
			sp = PreferenceManager.getDefaultSharedPreferences(NetworkSetting.this);
	        sp.edit().putString(PhoneBase.NETWORK_SELECTION_NAME_KEY, "").commit();
		}
    }

// LGE_ECLAIR_PORTING START
    private void updateForbiddenNetworks() {
        for (Preference p : mNetworkMap.keySet()) {
            p.setEnabled(((NetworkInfo)mNetworkMap.get(p)).getState() != NetworkInfo.State.FORBIDDEN);
        }
    }
// LGE_ECLAIR_PORTING END

//2010.10.16 seungjun.seo@lge.com SKT Roaming Add [START_LGE_LAB1]
	private void updateCarrierSelectModePrefs(boolean aIsAutoMode){
		if(DBG) log("updateCarrierSelectModePrefs() aIsAutoMode :" + aIsAutoMode);

		if(LGEBuild.USE_CHECKBOX_CARRIER_SELECT_MODE){
			mSearchButtonKor.setChecked(!aIsAutoMode);
  	   		mAutoSelectKor.setChecked(aIsAutoMode);
  	   		mSearchButtonKor.setEnabled(true);
  	   		mAutoSelectKor.setEnabled(true);
		}
	}

	private void selectNetwork(NetworkInfo ni, String networkStr){
		if(DBG) log("selectNetwork()");
				Message msg = mHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);
				mPhone.selectNetworkManually(ni, msg);
	
				displayNetworkSeletionInProgress(/*ni, */networkStr);		
	}

    private void setPreferenceEnabled(boolean aEnabled){
    	getPreferenceScreen().setEnabled(aEnabled);
    	
    	if(LGEBuild.ENABLE_FORBIDDEN_CARRIER_SELECTABLE == false && aEnabled){
    		int count = mNetworkList.getPreferenceCount();
    		for(int index=0; index < count; index++){
    			Preference pref = mNetworkList.getPreference(index);
    			NetworkInfo info = mNetworkMap.get(pref);
    			if(info != null){
     				if(info.getState() == State.FORBIDDEN){
    					pref.setEnabled(LGEBuild.ENABLE_FORBIDDEN_CARRIER_SELECTABLE);
    				}else{
    					pref.setEnabled(aEnabled);
    				}
    			}else{
    				pref.setEnabled(aEnabled);
    			}
    		}
    	}
    }

	public void onSaveInstanceState(Bundle out) {
		if(DBG)log("onSaveInstanceState()");
		super.onSaveInstanceState(out);
		
		if(StarConfig.OPERATOR.equals("SKT"))
		{
			int count = mNetworkList.getPreferenceCount();
			if(count <= LGEBuild.THRESHOLD_SAVE_CARRIER_LIST){
				int savedCount = 0;
				
				for(int index=0; index < count; index++){
					Preference pref = mNetworkList.getPreference(index);
					NetworkInfo info = mNetworkMap.get(pref);
					if(info != null){
						String niStr = info.getOperatorAlphaLong() + ";" + info.getOperatorAlphaShort() + ";" + info.getOperatorNumeric() + ";" + info.getState() + ";";// + info.getOperatorRAT();
						if(DBG)log("->carrierList[" + savedCount + "]=[" + niStr + "]");
						out.putString("carrierList[" + savedCount + "]", niStr);
						savedCount++;
					}
				}
				
				out.putInt("carrierListCount", savedCount);
			}
			
			mNetProgress.onSaveActivityInstance(this, out);
		}
	}

    private class NetProgress {
    	public static final int OPERATION_NONE = 0;
    	public static final int OPERATION_LIST_LOAD = 1;
    	public static final int OPERATION_SELECTION = 2;
    	public static final int OPERATION_AUTO_SELECT = 3;
    	
    	private ShowProgressDialog mProgressDialog = new ShowProgressDialog();
    	private int mOperation = OPERATION_NONE;
    	private NetworkInfo mNetworkInfo = null;
    	private String mNetworkStr = "";
    	
	    public synchronized void onStartOperation(int aOperation, NetworkInfo ni, String networkStr){
	    	Log.v(LOG_TAG, "onStartOperation");
	    	String msg = "";

		//phjjiny.park 101007 SKT Issue NullPointerException 
		if(mProgressDialog == null)
			mProgressDialog = new ShowProgressDialog();
	    	
	    	mOperation = aOperation;
	    	mNetworkInfo = ni;
	    	mNetworkStr = networkStr;
	    	
	    	int options = ShowProgressDialog.OPTION_NONE;
	    	ProgressDialog dialog = new ProgressDialog(NetworkSetting.this);
	    	switch(aOperation){
	    	case OPERATION_SELECTION:
	    		if(DBG) log("OPERATION_SELECTION");
	            msg = mNetworkSelectMsg;
	            dialog.setCancelable(false);
	            dialog.setIndeterminate(true);
	    
	            options = ShowProgressDialog.setOption(options, ShowProgressDialog.OPTION_CANCELABLE, false);
	            options = ShowProgressDialog.setOption(options, ShowProgressDialog.OPTION_INDETERMINATE, true);
	            break;
	    		
	    	case OPERATION_LIST_LOAD:
	    		if(DBG) { log("OPERATION_LIST_LOAD");
	    		msg = getResources().getString(R.string.load_networks_progress);
	            dialog.setCancelable(true);
	            dialog.setOnCancelListener(NetworkSetting.this);
	            
	            options = ShowProgressDialog.setOption(options, ShowProgressDialog.OPTION_CANCELABLE, true);
	    		}
	    
	    		break;
	    		
	    	case OPERATION_AUTO_SELECT:
	    		if(DBG) log("OPERATION_AUTO_SELECT");
	            msg = getResources().getString(R.string.register_automatically);
	            dialog.setCancelable(false);
	            dialog.setIndeterminate(true);

	            options = ShowProgressDialog.setOption(options, ShowProgressDialog.OPTION_CANCELABLE, false);
	            options = ShowProgressDialog.setOption(options, ShowProgressDialog.OPTION_INDETERMINATE, true);
	    		break;
	    		
	    	default:
	    		if(DBG) log("** Unknown operation=" + aOperation);
	    		dialog = null;
	    		mOperation = OPERATION_NONE;
	    		return;
	    	}
	    		        
	    	mProgressDialog.showDialog(dialog, msg, options);
	    }
	    
	    public synchronized void onEndOperation(){
	    	if(DBG) log("NetProgress:onEndOperation() -> try dismiss dialog");
	    	mOperation = OPERATION_NONE;
	    	if(mProgressDialog != null)
	    		mProgressDialog.dismiss();
	    }

	    
	    public void onCreateActivity(Context aContext, Bundle savedInstanceState){
		if(DBG) log("NetProgress:onCreateActivity() savedInstanceState =" + savedInstanceState);
	
	    	if(savedInstanceState == null)
	    		return;
	    	
	    	if(savedInstanceState.containsKey("operation"))
	    		mOperation = savedInstanceState.getInt("operation");
    		if(savedInstanceState.containsKey("networkStr"))
    			mNetworkStr = savedInstanceState.getString("networkStr");
    		if(savedInstanceState.containsKey("networkInfo"))
    			mNetworkInfo = savedInstanceState.getParcelable("networkInfo");

			if(DBG) log("mOperation" + mOperation);
    		
	    	switch(mOperation){
	    	case OPERATION_LIST_LOAD:
	    		if(LGEBuild.ENABLE_ASAP_LOAD_NETWORK_LIST == true){
	    			// Do Nothing - start service will call loadNetworksList()
	    			if(DBG) log("OPERATION_LIST_LAOD: do nothing");
	    		}else{
		    		// TODO loadNetworkList requires startService
	    			if(DBG) log("OPERATION_LIST_LAOD: start operation");
	    			onStartOperation(OPERATION_LIST_LOAD, null, null);
	    		}
	    		break;
	    	case OPERATION_SELECTION:
	    		if(DBG) log("OPERATION_SELECTION: start operation");
	    		selectNetwork(mNetworkInfo, mNetworkStr);
	    		break;
	    	case OPERATION_AUTO_SELECT:
	    		if(DBG) log("OPERATION_AUTO_SELECT: start operation");
	    		selectNetworkAutomatic();
	    		break;
	    	case OPERATION_NONE:
	    		if(DBG) log("OPERATION_NONE: do nothing");
	    		break;
	    	}
	    	
	    }
     	
    	public void onSaveActivityInstance(Context aContext, Bundle out){
    		if(LGEBuild.ENABLE_RESTART_NETWORK_OPERATION_ON_RESUME){
	    		if(DBG) log("onSaveActivityInstance() mOperation :" + mOperation);
	    		out.putInt("operation", mOperation);
	    		if(mNetworkStr != null)
	    			out.putString("networkStr", mNetworkStr);
	    		if(mNetworkInfo != null)
	    			out.putParcelable("networkInfo", mNetworkInfo);
    		}
    	}
	    
    	public synchronized void onStopActivity(Context aContext){
    		if(DBG) log("onStopActivity -> ");
    		if(mProgressDialog != null){
    			mProgressDialog.dismiss();
    			mProgressDialog = null;
    		}
    	}   	
    		    
	    public void onServiceConnected(){
	    	if(LGEBuild.ENABLE_ASAP_LOAD_NETWORK_LIST == false){
	    		if(DBG) log("onServiceConnected() mOperation :" + mOperation);
	    		if(mOperation == OPERATION_LIST_LOAD){
	    			if(DBG) log("onServiceConnected()->loadNetworksList()");
						if(StarConfig.OPERATOR.equals("SKT"))
						{
							loadNetworksListKor(false);
						}
	    		}
    		}
	    }
    }

//2010.10.16 seungjun.seo@lge.com SKT Roaming Add [END_LGE_LAB1]

    private void log(String msg) {
        Log.d(LOG_TAG, "[NetworksList] " + msg);
    }
}

