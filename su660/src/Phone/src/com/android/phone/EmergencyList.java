// 2010.10.20 minji.bae@lge.com Domestic - Add Emergency List [START_LGE_LAB1]
// LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-06 : Remake based on Eclair's EmergencyDialer
package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver; // 2010.11.16 minji.bae@lge.com close activity when screen turns off
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter; // 2010.11.16 minji.bae@lge.com close activity when screen turns off
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.WindowManager;

// LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-12 : SKT's emergency numbers (ROAMING)
import android.telephony.TelephonyManager;
// LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-12 : SKT's emergency numbers (ROAMING)

// LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-24 : No USIM roaming case
import com.android.internal.telephony.TelephonyProperties;
// LGE_CHANGE_E [sk82.lee@lge.com] 2010-05-24 : No USIM roaming case

// 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [START_LGE_LAB1]
import com.android.internal.telephony.gsm.ModemInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
// 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [END_LGE_LAB1]
import com.lge.config.StarConfig;

public class EmergencyList extends Activity implements DialogInterface.OnClickListener {
    private static final boolean DBG = false;
    private static final String TAG = "EmergencyList";
    static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";

    //private static final String PROPERTY_ESCV = "ril.escv";  // not used in STAR
    
    // for SKT
    private static final String[] EMERGENCY_NUMBERS_SKT = {
        "112", "119/***Ambulance", "119/***FireStation", "122",
        "119/***MountainGuard", "113", "125", "127", "111" // 15883249 -> 119
    }; /* "direct", // 2010.11.16 minji.bae@lge.com Domestic - Emergency List Scenario Changed */
    private static final int[] EMERGENCY_NUMBER_DESC_IDS_SKT = {
        R.string.desc_112, R.string.desc_119_Ambulance, R.string.desc_119_FireStation, R.string.desc_122,
        R.string.desc_15883249_MountainGuard, R.string.desc_113, R.string.desc_125, R.string.desc_127, R.string.desc_111
    }; /* R.string.desc_direct, // 2010.11.16 minji.bae@lge.com Domestic - Emergency List Scenario Changed */
    // LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-12 : SKT's emergency numbers (ROAMING)
    private static final String[] EMERGENCY_NUMBERS_SKT_ROAMING = {
        "112/***Police", "119/***Ambulance", "119/***FireStation", "122/***CoastGuard", "119/***MountainGuard" // 15883249 -> 119
    };
    private static final int[] EMERGENCY_NUMBER_DESC_IDS_SKT_ROAMING = {
        R.string.desc_112, R.string.desc_119_Ambulance, R.string.desc_119_FireStation, R.string.desc_122, R.string.desc_15883249_MountainGuard
    };
    // LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-12 : SKT's emergency numbers (ROAMING)
    
    // for KT
    private static final String[] EMERGENCY_NUMBERS_KT = {
        "112", "119","911", "122", "113", "125", "127", "111"
    };
    private static final int[] EMERGENCY_NUMBER_DESC_IDS_KT = {
        R.string.desc_112, R.string.desc_119, R.string.desc_911, R.string.desc_122, R.string.desc_113,
        R.string.desc_125, R.string.desc_127, R.string.desc_111
    };
    
    // for LGT
    private static final String[] EMERGENCY_NUMBERS_LGT = {
        "119", "119", "111", "112", "113", "114"
    };
    private static final int[] EMERGENCY_NUMBER_DESC_IDS_LGT = {
        R.string.desc_119_Ambulance, R.string.desc_119_FireStation, R.string.desc_111, R.string.desc_112,
        R.string.desc_113, R.string.desc_114,
    };

    private String[] emergencyNumbers;
    private int[] emergencyNumberDescIDs;
    
    private boolean mIsDirectCall;
    // 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [START_LGE_LAB1]
    private Phone mPhone;
    // 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [END_LGE_LAB1]

    // 2010.11.16 minji.bae@lge.com close activity when screen turns off
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                finish();
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(DBG) Log.d(TAG, "onCreate()");
        // Eclair's method to show emergency list when screen locked
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        
        boolean isRoaming = TelephonyManager.getDefault().isNetworkRoaming();
        // 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [START_LGE_LAB1]
        mPhone = PhoneFactory.getDefaultPhone();
        boolean isNoUSIMRoaming = false;    // !! 2010.10.20 not implemented yet !!
        /*
        // LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-24 : No USIM roaming case
        String noUSIMRoaming = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NOUSIMISROAMING, "false");
        boolean isNoUSIMRoaming = Boolean.parseBoolean(noUSIMRoaming);
        // LGE_CHANGE_E [sk82.lee@lge.com] 2010-05-24 : No USIM roaming case
        */
        // 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [END_LGE_LAB1]
        
        if(StarConfig.OPERATOR.equals("SKT")) {
            // LGE_CHANGE_S [sk82.lee@lge.com] 2010-05-24 : No USIM roaming case
            if(isRoaming || isNoUSIMRoaming) {
            // LGE_CHANGE_E [sk82.lee@lge.com] 2010-05-24 : No USIM roaming case
                emergencyNumbers = EMERGENCY_NUMBERS_SKT_ROAMING;
                emergencyNumberDescIDs = EMERGENCY_NUMBER_DESC_IDS_SKT_ROAMING;
            } else {
                emergencyNumbers = EMERGENCY_NUMBERS_SKT;
                emergencyNumberDescIDs = EMERGENCY_NUMBER_DESC_IDS_SKT;
            }
        } else if(StarConfig.OPERATOR.equals("KT")) {
            emergencyNumbers = EMERGENCY_NUMBERS_KT;
            emergencyNumberDescIDs = EMERGENCY_NUMBER_DESC_IDS_KT;
        } else {
            emergencyNumbers = EMERGENCY_NUMBERS_LGT;
            emergencyNumberDescIDs = EMERGENCY_NUMBER_DESC_IDS_LGT;
        }
        
        String[] items = new String[emergencyNumbers.length];
        for(int i = 0 ; i < emergencyNumbers.length ; i++) {
            String number = emergencyNumbers[i].split("/")[0];
            String desc = (String)getResources().getText(emergencyNumberDescIDs[i]);
            // 2010.11.16 minji.bae@lge.com Domestic - Emergency List Scenario Changed
            /*
            if(number.equals("direct")) {
                items[i] = desc;
            } else */
            {
                if (number.equals("15883249"))
                    items[i] = desc + "  " + "1588-3249";
                else
                    items[i] = desc + "  " + number;
            }
        }

        // 2010.11.16 minji.bae@lge.com close activity when screen turns off
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mBroadcastReceiver, intentFilter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.emergencyListLabel);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                finish();
            }
        });
        
        builder.setItems(items, this);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    
    public void onClick(DialogInterface dialog, int which) {
        call(emergencyNumbers[which]);
        finish();
    }

    private void call(String number) {
        // 2010.11.16 minji.bae@lge.com Domestic - Emergency List Scenario Changed
        /*
        if(number.equals("direct")) {
            //SystemProperties.set(PROPERTY_ESCV, null);  // not used in STAR
            Intent intent = new Intent(this, EmergencyDialer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mIsDirectCall = true;
            startActivity(intent);
        }
        else */
        {
            String tel = null;
            //String escv = null;  // not used in STAR
            
            String[] splitted = number.split("/");
            tel = splitted[0];
            /* not used in STAR
            if(splitted.length == 2) {
                escv = splitted[1];
            } else {
                escv = null;
            }
            */
            if(DBG) Log.d(TAG, "call() : tel=" + tel + " number = " + number);// + ", escv=" + escv);

            // 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [START_LGE_LAB1]
            if(number.equals("119/***Ambulance")) {
                mPhone.setModemInfo(ModemInfo.LGE_MODEM_INFO_119ECC_CATEGORY, 0x02, null);
            }
            else if("119/***FireStation".equals(number)) {
                mPhone.setModemInfo(ModemInfo.LGE_MODEM_INFO_119ECC_CATEGORY, 0x04, null);
            }
            else if("119/***MountainGuard".equals(number)) {
                mPhone.setModemInfo(ModemInfo.LGE_MODEM_INFO_119ECC_CATEGORY, 0x10, null);
            }
            // 2010.10.20 minji.bae@lge.com Get ModemInfo to set 119 category and get roaming information [END_LGE_LAB1]

             //SystemProperties.set(PROPERTY_ESCV, escv);  // not used in STAR
             Intent intent = new Intent(Intent.ACTION_CALL_EMERGENCY);
             intent.setData(Uri.fromParts("tel", tel, null));
             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             startActivity(intent);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        PhoneApp app = (PhoneApp) getApplication();
        //app.disableKeyguard(); // Donut's method to show emergency list when screen locked but it's better than Eclair's
        app.disableStatusBar();
        app.setScreenTimeout(PhoneApp.ScreenTimeoutDuration.MEDIUM);
    }

    @Override
    public void onPause() {
        PhoneApp app = (PhoneApp) getApplication();
        //if(mIsDirectCall)
        //app.reenableKeyguard(); // Donut's method to show emergency list when screen locked but it's better than Eclair's
        app.reenableStatusBar();
        app.setScreenTimeout(PhoneApp.ScreenTimeoutDuration.DEFAULT);

        super.onPause();
    }
}
// LGE_CHANGE_E [sk82.lee@lge.com] 2010-05-06 : Remake based on Eclair's EmergencyDialer
// 2010.10.20 minji.bae@lge.com Domestic - Add Emergency List [START_LGE_LAB1]
