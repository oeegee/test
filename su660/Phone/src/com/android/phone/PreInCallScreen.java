/*
 * Copyright (C)
 */

package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.RAD.RoamingPrefixAppender;
import com.android.internal.telephony.RAD.RoamingPrefixAppenderFactory;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.view.View;
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number
import android.telephony.PhoneNumberUtils;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
import android.view.KeyEvent;
import android.view.WindowManager;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call

import com.android.internal.telephony.RAD.RADCarrierUtil;
import com.android.internal.telephony.RAD.RADCarrierUtilProxy;
import com.android.internal.telephony.RAD.RADCarrierUtilFactory;
import com.android.internal.telephony.RAD.SKT.SKTPhoneNumberUtil;
import com.lge.config.StarConfig;






// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-20 : make PreInCallScreen activity
public class PreInCallScreen extends Activity {
    private static final String LOG_TAG = "PreInCallScreen";
    private static final boolean DEBUGGABLE = true;

    private Intent m_intent;
    private String m_phoneNumber;
    private String m_serviceProvider;
    private Context m_context;
    private ContentResolver m_contentResolver;

    private EventHandler mEventHandler = new EventHandler();

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-06-03 : call : lock outgoing voice call
    private static final int REQUEST_CODE_CONFIRM_PASSWROD = 0;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-06-03 : call : lock outgoing voice call

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode
    private static final int DIALOG_LGT_RAD = 0;
    private static final int DIALOG_SKT_RAD = 1;
    private static final int DIALOG_KT_RAD = 2;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode
// LGE_CHANGE_S [wonho.moon@lge.com] 2010-04-26 : RAD - user roaming number setting
    private static final int DIALOG_KT_OVERSEA_RAD = 3;
// LGE_CHANGE_E [wonho.moon@lge.com] 2010-04-26 : RAD - user roaming number setting

    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_intent = getIntent(); 
        m_phoneNumber = getPhoneNumber(m_intent);
        m_serviceProvider = SystemProperties.get("ro.telephony.service_provider", "null");
        m_context = getBaseContext();
        m_contentResolver = m_context.getContentResolver();
        if (DEBUGGABLE) Log.v(LOG_TAG, "PreInCallScreen.onCreate() m_phoneNumber:" + m_phoneNumber + ", m_serviceProvider:" + m_serviceProvider);

        Message msg = mEventHandler.obtainMessage(EventHandler.EVENT_ON_CREATE, null);
        msg.sendToTarget();
    }

    private class EventHandler extends Handler {
        static final int EVENT_ON_CREATE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_ON_CREATE : {
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
/*
                String msgReasonNotAble = PhoneCarrierUtils.doServiceStatusCheck(m_context, m_intent);
                if (msgReasonNotAble != null) {
                    showDialogSKT(msgReasonNotAble);
                    return;
                }
                */
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-06-03 : call : lock outgoing voice call
                LockScreenDecider lsd = new LockScreenDecider(PhoneApp.getInstance().getApplicationContext());
                if(lsd.isSendingLock(m_phoneNumber)) {
                    Intent intent = new Intent();
                    intent.setClassName("com.lge.lgcommon", "com.lge.lgcommon.password.PasswordInput");
                    startActivityForResult(intent, REQUEST_CODE_CONFIRM_PASSWROD);
					return;
                } 
                
//@                else {
//@// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode
//@                    doRoamingAutoDialCheck();
//@// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode
//@                }
                
                doRoamingAutoDialCheck();
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-06-03 : call : lock outgoing voice call
            }
                break;

            default :
                if (DEBUGGABLE) Log.e(LOG_TAG, "handleMessage() Invalid Message:" + msg.what);
                returnPreActivity();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-06-03 : call : lock outgoing voice call
        case REQUEST_CODE_CONFIRM_PASSWROD :
            if (resultCode == RESULT_OK) {
				//20101102 wonho.moon@lge.com Password check add
				PhoneApp.getInstance().setIsCheckPassword(true);
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode
                doRoamingAutoDialCheck();
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode
            } else {
                returnPreActivity();
            }
            break;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-06-03 : call : lock outgoing voice call

        default :
            if (DEBUGGABLE) Log.e(LOG_TAG, "onActivityResult() Invalid Request Code:" + requestCode);
            returnPreActivity();
            break;
        }
    }

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode
    private void doRoamingAutoDialCheck() {
        boolean bIsRoaming = TelephonyManager.getDefault().isNetworkRoaming();
        if (DEBUGGABLE) Log.v(LOG_TAG, "doRoamingAutoDialCheck() bIsRoaming:" + bIsRoaming);
/**
 * for RAD test
bIsRoaming = true;
m_intent.putExtra(RoamingPrefixAppender.INTENT_EXTRA_AUTO_UPDATE, RoamingPrefixAppender.AUTO_UPDATE_SET);
*/




// LGE_CHANGE_S [wonho.moon@lge.com] 2010-04-26 : KT RAD - user roaming setting check
        boolean isUroaming = true;
        //if (m_serviceProvider.equals("KT")) {
        if (StarConfig.OPERATOR.equals("KT")) {
        
            /*
             * test user roaming setting
             *
            //Settings.Secure.putString(m_contentResolver, Settings.Secure.KT_SHOW_ROAMING_PREFIX, "");
             */
// LGE_CHANGE_S [wonho.moon@lge.com] 2010-05-03 : KT RAD
            bIsRoaming = false;
// LGE_CHANGE_E [wonho.moon@lge.com] 2010-05-03 : KT RAD
            String uSsetting = Settings.Secure.getString(m_contentResolver, Settings.Secure.KT_SHOW_ROAMING_PREFIX);
            if (DEBUGGABLE) Log.v(LOG_TAG, "uSsetting: " + uSsetting);
            if (uSsetting == null || uSsetting.equals("") || uSsetting.equals("null")) {
                isUroaming = true;
            } else {
                isUroaming = false;
            }
        } else {
            isUroaming = true;
        }
// LGE_CHANGE_E [wonho.moon@lge.com] 2010-04-26 : KT RAD - user roaming setting check
		if (Settings.System.getInt(m_contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
			bIsRoaming = false;
		}
		
// LGE_CHANGE_S [wonho.moon@lge.com] 2010-04-26 : KT RAD - isUroaming  == true
        if (DEBUGGABLE) Log.v(LOG_TAG, "isUroaming:" + isUroaming);
        if (DEBUGGABLE) Log.v(LOG_TAG, "bIsRoaming:" + bIsRoaming);
        //if (bIsRoaming == true) {
        if (bIsRoaming == true && isUroaming == true) {
// LGE_CHANGE_E [wonho.moon@lge.com] 2010-04-26 : KT RAD - isUroaming  == true

            /**
             * << NOTICE >> : The following condition is based on placeCall() function of InCallScreen.java files.
             */
            boolean bIsAddPrefix = false;
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender
            boolean ignoreRad = m_intent.getBooleanExtra(RoamingPrefixAppender.INTENT_EXTRA_IGNORE_RAD, RoamingPrefixAppender.IGNORE_RAD_DEFAULT);

            if (DEBUGGABLE) Log.v(LOG_TAG, "ignoreRad:" + ignoreRad);
            /**
             * origin
            int radAutoUpdate = m_intent.getIntExtra(RoamingPrefixAppender.INTENT_EXTRA_AUTO_UPDATE, RoamingPrefixAppender.AUTO_UPDATE_DEFAULT);
            boolean ignoreRad = m_intent.getBooleanExtra(RoamingPrefixAppender.INTENT_EXTRA_IGNORE_RAD, RoamingPrefixAppender.IGNORE_RAD_DEFAULT);
             */
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender

            if (ignoreRad == false) {
                /**
                 * The following code is added to consider that isAddPrefix() use a intent extra data INTENT_EXTRA_RAD_MODE.
                 * INTENT_EXTRA_RAD_MODE is modified with valid data in startInCallScreen() function later.
                 */
                m_intent.putExtra(RoamingPrefixAppender.INTENT_EXTRA_RAD_MODE, RoamingPrefixAppender.RAD_MODE_KOR);

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender
                RoamingPrefixAppender roamingPrefixAppender
                        = RoamingPrefixAppenderFactory.getDefaultRoamingPrefixAppender(m_context, m_contentResolver, m_intent);
                /**
                 * origin
                RoamingPrefixAppender roamingPrefixAppender
                        = RoamingPrefixAppenderFactory.getDefaultRoamingPrefixAppender(m_context, m_contentResolver, radAutoUpdate);
                 */
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-22 : RAD - transfer intent to RoamingPrefixAppender

                if (roamingPrefixAppender != null) {
                    if (roamingPrefixAppender.isAddPrefix(m_phoneNumber)) {
                        bIsAddPrefix = true;
                    }
                }
            }

            if (DEBUGGABLE) Log.v(LOG_TAG, "doRoamingAutoDialCheck() ignoreRad:" + ignoreRad + ", bIsAddPrefix:" + bIsAddPrefix);

            if (bIsAddPrefix) {
                int nOutgoingMode = m_intent.getIntExtra(Intent.EXTRA_OUTGOING_MODE, Intent.OUTGOING_MODE_DEFAULT);
                if (DEBUGGABLE) Log.v(LOG_TAG, "doRoamingAutoDialCheck() EXTRA_OUTGOING_MODE: " + nOutgoingMode);

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-18 : call : roaming : process RAD by setting
                int settingRad = Settings.Secure.getInt(m_contentResolver, Settings.Secure.SKT_ROAMING_AUTODIAL, 1);
                // if settingRad == 2, call to korea always 
                if (settingRad == 2) {
                    nOutgoingMode = Intent.OUTGOING_MODE_KOREA;
                }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-18 : call : roaming : process RAD by setting

                if (DEBUGGABLE) Log.v(LOG_TAG, "doRoamingAutoDialCheck() nOutgoingMode: " + nOutgoingMode);

                switch (nOutgoingMode) {
                case Intent.OUTGOING_MODE_NORMAL :
                    if (StarConfig.OPERATOR.equals("LGT")) {
                        showDialog(DIALOG_LGT_RAD);
                    } else if (StarConfig.OPERATOR.equals("SKT")) {
                        showDialog(DIALOG_SKT_RAD);
                    } else if (StarConfig.OPERATOR.equals("KT")) {
                        showDialog(DIALOG_KT_RAD);
                    } else {
                        if (DEBUGGABLE) Log.e(LOG_TAG, "doRoamingAutoDialCheck() Invalid m_serviceProvider:" + m_serviceProvider);
                        returnPreActivity();
                    }
                    break;
                case Intent.OUTGOING_MODE_KOREA :
                    startInCallScreen(RoamingPrefixAppender.RAD_MODE_KOR);
                    break;
                case Intent.OUTGOING_MODE_ETC :
                    startInCallScreen(RoamingPrefixAppender.RAD_MODE_ETC);
                    break;
                default :
                    if (DEBUGGABLE) Log.e(LOG_TAG, "doRoamingAutoDialCheck() Invalid nOutgoingMode:" + nOutgoingMode);
                    returnPreActivity();
                    break;
                }
            } else {
                startInCallScreen(RoamingPrefixAppender.RAD_MODE_NONE);
            }
        } else {
            startInCallScreen(RoamingPrefixAppender.RAD_MODE_NONE);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;

        switch (id) {
        case DIALOG_LGT_RAD : {
            dialog = new AlertDialog.Builder(this)
                                    .setTitle(R.string.RadModeDlgTitleSkt)
                                    .setItems(R.array.RadModeDlgItemSkt, new DialogInterface.OnClickListener() {
                                        public void onClick (DialogInterface dialog, int which) {
                                            switch (which) {
                                            case 0:
                                                startInCallScreen(RoamingPrefixAppender.RAD_MODE_KOR);
                                                break;
                                            case 1:
                                                startInCallScreen(RoamingPrefixAppender.RAD_MODE_ETC);
                                                break;
                                            default:
                                                if (DEBUGGABLE) Log.e(LOG_TAG, "Invalid RadModeDlgItemSkt:" + which);
                                                returnPreActivity();
                                                break;
                                            }
                                        }
                                    } )
                                    .create();

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel (DialogInterface dialog) {
                    if (DEBUGGABLE) Log.v(LOG_TAG, "PreInCallScreen.Dialog.OnCancelListener()");
                    returnPreActivity();
                }
            });
        }
            break;

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-04-20 : SKT RAD - roaming dial mode
        case DIALOG_SKT_RAD : {
            dialog = new AlertDialog.Builder(this)
                                    .setTitle(R.string.RadModeDlgTitleSkt)
                                    .setItems(R.array.RadModeDlgItemSkt, new DialogInterface.OnClickListener() {
                                        public void onClick (DialogInterface dialog, int which) {
                                            switch (which) {
                                            case 0:
                                                startInCallScreen(RoamingPrefixAppender.RAD_MODE_KOR);
                                                break;
                                            case 1:
                                                startInCallScreen(RoamingPrefixAppender.RAD_MODE_ETC);
                                                break;
                                            default:
                                                if (DEBUGGABLE) Log.e(LOG_TAG, "Invalid RadModeDlgItemSkt:" + which);
                                                returnPreActivity();
                                                break;
                                            }
                                        }
                                    } )
                                    .create();

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel (DialogInterface dialog) {
                    if (DEBUGGABLE) Log.v(LOG_TAG, "PreInCallScreen.Dialog.OnCancelListener()");
                    returnPreActivity();
                }
            });
        }
            break;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-20 : SKT RAD - roaming dial mode

// LGE_CHANGE_S [wonho.moon@lge.com] 2010-04-26 : KT RAD - roaming dial mode
        case DIALOG_KT_RAD : {
            dialog = new AlertDialog.Builder(this)
                                    .setTitle(R.string.RadModeDlgTitleKt)
                                    .setItems(R.array.RadModeDlgItemKt, new DialogInterface.OnClickListener() {
                                        public void onClick (DialogInterface dialog, int which) {
                                            switch (which) {
                                            case 0:
                                                startInCallScreen(RoamingPrefixAppender.RAD_MODE_KOR);
                                                break;
                                            case 1:
                                                showDialog(DIALOG_KT_OVERSEA_RAD);
                                                break;
											case 2:
                                                startInCallScreen(RoamingPrefixAppender.RAD_MODE_ETC);
                                                break;
                                            default:
                                                if (DEBUGGABLE) Log.e(LOG_TAG, "Invalid RadModeDlgItemSkt:" + which);
                                                returnPreActivity();
                                                break;
                                            }
                                        }
                                    } )
                                    .create();

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel (DialogInterface dialog) {
                    if (DEBUGGABLE) Log.v(LOG_TAG, "PreInCallScreen.Dialog.OnCancelListener()");
                    returnPreActivity();
                }
            });
        }
            break;

        case DIALOG_KT_OVERSEA_RAD : {
            LayoutInflater factory = LayoutInflater.from(this);
// LGE_CHANGE_S [wonho.moon@lge.com] 2010-04-29 : KT RAD - roaming dial mode
            final View textEntryView = factory.inflate(R.layout.kt_rad_dialog, null);
            dialog = new AlertDialog.Builder(this)
                                    .setTitle(R.string.customTitle)
                                    .setView(textEntryView)
                                    .setPositiveButton(R.string.custom_roaming_button, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            String str = ((EditText) textEntryView.findViewById(R.id.kt_rad_num)).getText().toString();
                                            if (DEBUGGABLE) Log.v(LOG_TAG, "str     " + str);
                                            if (str.length() > 0) {
                                                m_intent.putExtra(Intent.EXTRA_PHONE_NUMBER, "+" + str + m_intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
                                            }
                                            startInCallScreen(RoamingPrefixAppender.RAD_MODE_ETC);
                                        }
                                    })
                                    .create();
// LGE_CHANGE_E [wonho.moon@lge.com] 2010-04-29 : KT RAD - roaming dial mode
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel (DialogInterface dialog) {
                    if (DEBUGGABLE) Log.v(LOG_TAG, "PreInCallScreen.Dialog.OnCancelListener()");
                    returnPreActivity();
                }
            });
        }
            break;
// LGE_CHANGE_E [wonho.moon@lge.com] 2010-04-26 : KT RAD - roaming dial mode

        default :
            if (DEBUGGABLE) Log.e(LOG_TAG, "Invalid Dialog ID:" + id);
            dialog = new AlertDialog.Builder(this)
                                    .create();
            returnPreActivity();
            break;
        }

        return dialog;
    }

    private void startInCallScreen(int nRadMode) {
        if (DEBUGGABLE) {
            if ( nRadMode != RoamingPrefixAppender.RAD_MODE_NONE
                 && nRadMode != RoamingPrefixAppender.RAD_MODE_KOR
                 && nRadMode != RoamingPrefixAppender.RAD_MODE_ETC ) {
                Log.e(LOG_TAG, "Invalid nRadMode:" + nRadMode);
            }
            Log.v(LOG_TAG, "startInCallScreen():" + nRadMode);
        }

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number
        if (StarConfig.OPERATOR.equals("SKT")) {
            if (nRadMode == RoamingPrefixAppender.RAD_MODE_KOR) {
                if (SKTPhoneNumberUtil.isCustomerCenterNumberSkt(m_phoneNumber)) {
                    m_phoneNumber = SKTPhoneNumberUtil.customerCenterNumberTransSkt;
                    m_intent.putExtra(Intent.EXTRA_PHONE_NUMBER, m_phoneNumber);
                } else if (SKTPhoneNumberUtil.isForeignAffairsTradeNumberSkt(m_phoneNumber)) {
                    m_phoneNumber = SKTPhoneNumberUtil.foreignAffairsTradeNumberTransSkt;
                    m_intent.putExtra(Intent.EXTRA_PHONE_NUMBER, m_phoneNumber);
                    if(PhoneApp.getInstance().mIsVideoCall)
                    	m_intent.setAction(Intent.ACTION_VIDEO_CALL);
                    else
                    	m_intent.setAction(Intent.ACTION_CALL);
                }
            }
        }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number

        if(PhoneApp.getInstance().mIsVideoCall)
        	m_intent.setClass(this, InVideoCallScreen.class);
        else{
           	//20101120 sumi920.kim@lge.com change function call after preincall screen[LGE_LAB1]
        	final PhoneApp app = PhoneApp.getInstance();
        	app.setBeginningCall(true); 

        	m_intent.setClass(this, InCallScreen.class);
        }
        m_intent.putExtra(RoamingPrefixAppender.INTENT_EXTRA_RAD_MODE, nRadMode);
        startActivity(m_intent);
        finish();
    }

    private void returnPreActivity() {
// LGE_CHANGE_S [jhyung.lee@lge.com] 2010-11-13 : initialize videocall feature         
        PhoneApp.mIsVideoCall = false;
// LGE_CHANGE_E [jhyung.lee@lge.com] 2010-11-13 : initialize videocall feature        
        finish();
    }

    private String getPhoneNumber(Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            return null;
        }

        if (action.equals(Intent.ACTION_CALL) && intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
            return intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }

        return PhoneNumberUtils.getNumberFromIntent(intent, this);
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-20 : RAD - roaming dial mode

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
    private static final int MSG_ID_TIMEOUT = 1;
    private final static int DIALOG_TIMEOUT = 2 * 60 * 1000;

    private AlertDialog networkDialog;

    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ID_TIMEOUT:
                    if (networkDialog != null) {
                        networkDialog.dismiss();
                        networkDialog = null;
//                        postDismissDialog();
                        returnPreActivity();
                    }
                    break;
            }
        }
    };

    DialogInterface.OnKeyListener mDialogOnKeyListener =
        new DialogInterface.OnKeyListener() {
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode != KeyEvent.KEYCODE_MENU) {
                if (dialog != null) {
                    dialog.dismiss();
                    networkDialog = null;
//                    postDismissDialog();
                    returnPreActivity();
                    return true;
                }
                return false;
            }
            return false;
        }
    };

    private void showDialogSKT(String msg)
    {
        if (networkDialog != null) {
            networkDialog.dismiss();
//            postDismissDialog();
        }

        networkDialog = new AlertDialog.Builder(this).setMessage(msg).setCancelable(true).create();
        // LGE_CHANGE_S [jaedong.shin@lge.com] : Home key problem when aleart pop up
        //networkDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        // LGE_CHANGE_E [jaedong.shin@lge.com] : Home key problem when aleart pop up
        networkDialog.setOnKeyListener(mDialogOnKeyListener);
        networkDialog.show();
        mTimeoutHandler.sendMessageDelayed(mTimeoutHandler.obtainMessage(MSG_ID_TIMEOUT), DIALOG_TIMEOUT);

        //SKT requested no sound
        /*
        try {
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, FOCUS_BEEP_VOLUME);
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "Exception caught while creating local tone generator: " + e);
            mToneGenerator = null;
        }
        if (mToneGenerator != null) {
            mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2);
        }
        */
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
}
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-04-20 : make PreInCallScreen activity

