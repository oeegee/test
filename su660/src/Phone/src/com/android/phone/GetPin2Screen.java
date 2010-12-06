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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
// 20100507 euikuk.jeong@lge.com Porting LGE patch <OK button add for PIN2 screen> from Aloha [START_LGE]
import android.widget.Button;
// 20100507 euikuk.jeong@lge.com Porting LGE patch <OK button add for PIN2 screen> from Aloha [END_LGE]
// 20100507 euikuk.jeong@lge.com Porting LGE patch <Add retry count text> from Aloha [START_LGE]
import android.widget.TextView;
import com.android.internal.telephony.ITelephony;
import android.os.ServiceManager;
import android.os.RemoteException;
// 20100507 euikuk.jeong@lge.com Porting LGE patch <Add retry count text> from Aloha [END_LGE]
import android.util.Log;

/**
 * Pin2 entry screen.
 */
public class GetPin2Screen extends Activity {
    private static final String LOG_TAG = PhoneApp.LOG_TAG;

    private EditText mPin2Field;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.get_pin2_screen);
        setupView();
        Log.d("GetPin2Screen","GET PIN 2 SCREEN");
// 20100507 euikuk.jeong@lge.com Porting LGE patch <OK button add for PIN2 screen> from Aloha [START_LGE]
        Button okButton = (Button) findViewById(R.id.button_ok);

        okButton.setOnClickListener(mClicked);
// 20100507 euikuk.jeong@lge.com Porting LGE patch <OK button add for PIN2 screen> from Aloha [END_LGE]
// 20100507 euikuk.jeong@lge.com Porting LGE patch <Add retry count text> from Aloha [START_LGE]
        TextView mRetryText = (TextView) findViewById(R.id.retryText);
        int attemptsRemaining = -1;
        try {
            attemptsRemaining = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getIccPin2RetryCount();
        } catch (RemoteException ex) {
            attemptsRemaining = -1;
        }
        mRetryText.setText(getText(R.string.STR_TITLETEXT_UICC_REMAINING_COMMON_SHORT).toString()+ attemptsRemaining);
	 Log.d("GetPin2Screen","show text :" + getText(R.string.STR_TITLETEXT_UICC_REMAINING_COMMON_SHORT).toString());
//        mRetryText.setText(getText(com.lge.internal.R.string.STR_TITLETEXT_UICC_REMAINING_COMMON_SHORT).toString()+ attemptsRemaining);
// 20100507 euikuk.jeong@lge.com Porting LGE patch <Add retry count text> from Aloha [END_LGE]
    }

    /**
     * Reflect the changes in the layout that force the user to open
     * the keyboard. 
     */
    private void setupView() {
        mPin2Field = (EditText) findViewById(R.id.pin);
        if (mPin2Field != null) {
            mPin2Field.setKeyListener(DigitsKeyListener.getInstance());
            mPin2Field.setMovementMethod(null);
            mPin2Field.setOnClickListener(mClicked);
        }
    }

    private String getPin2() {
        return mPin2Field.getText().toString();
    }

    private void returnResult() {
        Bundle map = new Bundle();
        map.putString("pin2", getPin2());

        Intent intent = getIntent();
        Uri uri = intent.getData();

        Intent action = new Intent();
        if (uri != null) action.setAction(uri.toString());
        setResult(RESULT_OK, action.putExtras(map));
        finish();
    }

    private View.OnClickListener mClicked = new View.OnClickListener() {
        public void onClick(View v) {
// 20100507 euikuk.jeong@lge.com Porting LGE patch <OK button add for PIN2 screen> from Aloha [START_LGE]
            CharSequence pin2 = mPin2Field.getText();
            if ((v.getId() != R.id.button_ok) || ((TextUtils.isEmpty(mPin2Field.getText())) || (pin2.length() < 4))) {   
//            if (TextUtils.isEmpty(mPin2Field.getText())) {  // Abroid original
// 20100507 euikuk.jeong@lge.com Porting LGE patch <OK button add for PIN2 screen> from Aloha [END_LGE]
                return;
            }

            returnResult();
        }
    };

    private void log(String msg) {
        Log.d(LOG_TAG, "[GetPin2] " + msg);
    }
}
