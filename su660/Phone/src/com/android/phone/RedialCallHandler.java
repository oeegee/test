// LGE_AUTO_REDIAL START
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

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.telephony.ServiceState;
import android.view.WindowManager;
import android.util.Log;
import android.net.Uri;
import android.content.DialogInterface;

 /**
 * This class coordinates with the InCallScreen by throwing the original intent
 * (from the dialer) back and forth. An extra integer referenced by
 * Redial_CALL_RETRY_KEY, is used to convey all the information we need.
 */
public class RedialCallHandler extends Activity {
    /** the key used to get the count from our Intent's extra(s) */
    public static final String REDIAL_CALL_RETRY_KEY = "Redial_call_retry_count";
    public static final String REDIAL_CALL_RETRY_FINISH = "Redial_call_retry_finish";
    public static final String REDIAL_CALL_RETRY_BLACK_LIST = "Redial_call_retry_black_list";

    private static final String LOG_TAG = "RetryCall";

    // constant events
    private static final int EVENT_TIMEOUT_REDIAL_CALL = 200;
    private static final int EVENT_CANCEL_REDIAL = 300;
    /**
     * Package holding information needed for the callback.
     */
    private static class RedialCallInfo {
        public Phone phone;
        public Intent intent;
        public ProgressDialog dialog;
        public Application app;
    }
    private static AlertDialog alert ;
    /**
     * static handler class, used to handle the two relevent events.
     */
    private static RedialCallEventHandler sHandler;
    private class RedialCallEventHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what) {

                case EVENT_TIMEOUT_REDIAL_CALL: {
                        // repeated call after the timeout period.
                        RedialCallInfo rdi = (RedialCallInfo) msg.obj;
                        Log.d(LOG_TAG, "EVENT_TIMEOUT_REDIAL_CALL");
                        rdi.app.startActivity(rdi.intent);
                        rdi.dialog.dismiss();
                    }
                    break;

                case EVENT_CANCEL_REDIAL: {
                    // repeated call after the timeout period.
                    RedialCallInfo rdi = (RedialCallInfo) msg.obj;
                    Log.d(LOG_TAG, "EVENT_CANCEL_REDIAL");
                    sHandler.removeMessages(EVENT_TIMEOUT_REDIAL_CALL);
                    final Intent cancel_intent = new Intent(REDIAL_CALL_RETRY_FINISH, null);
                    cancel_intent.setClassName("com.android.phone", InCallScreen.class.getName());
                    startActivity(cancel_intent);
                    finish();
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // setup the phone and get the retry count embedded in the intent.
        Phone phone = PhoneFactory.getDefaultPhone();
        final String number = getIntent().getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        boolean blacklist = getIntent().getBooleanExtra(RedialCallHandler.REDIAL_CALL_RETRY_BLACK_LIST,false);
        int repeat = getIntent().getIntExtra(REDIAL_CALL_RETRY_KEY, -1);
     // create the handler.
        if (sHandler == null) {
            sHandler = new RedialCallEventHandler();
        }
        // create a new message object.


        RedialCallInfo rdi = new RedialCallInfo();
        rdi.phone = phone;
        rdi.app = getApplication();
        if (blacklist == false){
            rdi.dialog = constructDialog();
            rdi.intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel",number,null));
            rdi.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);


            Log.d(LOG_TAG, "onCreate_progress_dialog");

           // get the message and attach the data, then wait the alloted
           // time and send.
            Message m = sHandler.obtainMessage(EVENT_TIMEOUT_REDIAL_CALL);
            m.obj = rdi;
            sHandler.sendMessageDelayed(m, repeat);
        }
        else{
        	Log.d(LOG_TAG,"onCreate_alert_dialog");
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("Unable to perform redial,please clean up autoredial list")
        	       .setCancelable(false)
        	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   Intent ok_intent = new Intent();
        	        	   ok_intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
        	               ok_intent.setClassName("com.android.phone", BlackListSelectActivity.class.getName());
        	               startActivity(ok_intent);

        	           }
        	       })
        	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   final Intent cancel_intent = new Intent(REDIAL_CALL_RETRY_FINISH, null);
                           cancel_intent.setClassName("com.android.phone", InCallScreen.class.getName());
                           startActivity(cancel_intent);
                           alert.dismiss();
        	           }
        	       });
        	alert = builder.create();
        	alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        	alert.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            alert.show();
        }
    }

    protected void onResume() {
    	Log.d(LOG_TAG,"onResume()...");
        super.onResume();


    }

    protected void onPause() {
    	Log.d(LOG_TAG,"onPause()...");
        super.onPause();


    }


    protected void onStop() {
    	Log.d(LOG_TAG,"onStop()...");
        super.onStop();
        finish();

    }


    protected void onDestroy() {
    	Log.d(LOG_TAG,"onDestroy()...");
        super.onDestroy();
        finish();

    }
    /**
     * create the dialog and hand it back to caller.
     */
    private ProgressDialog constructDialog() {
        // create a system dialog that will persist outside this activity.
        ProgressDialog pd = new ProgressDialog(getApplication());
        pd.setTitle(getText(R.string.redial_radio_dialog_title));
        pd.setMessage(getText(R.string.redial_radio_dialog_wait));
        pd.setIndeterminate(true);
        pd.setCancelable(true);
        pd.setButton(DialogInterface.BUTTON_NEGATIVE,getText(R.string.cancel),sHandler.obtainMessage(EVENT_CANCEL_REDIAL));
        pd.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        pd.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

        // show the dialog
        pd.show();

        return pd;
    }

}
// LGE_AUTO_REDIAL END