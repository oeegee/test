/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
//LGE_TD3658 START
import android.preference.ListPreference;
//LGE_TD3658 END
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class Use2GOnlyCheckBoxPreference extends CheckBoxPreference {
    private static final String LOG_TAG = "Use2GOnlyCheckBoxPreference";
    private static final boolean DBG = true;
//LGE_TD3658 START
    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
//LGE_TD3658 END
    private Phone mPhone;
    private MyHandler mHandler;
//LGE_TD3658 START
    private ListPreference mButtonPreferredNetworkMode;
//LGE_TD3658 END

    public Use2GOnlyCheckBoxPreference(Context context) {
        this(context, null);
    }

    public Use2GOnlyCheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs,com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public Use2GOnlyCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPhone = PhoneFactory.getDefaultPhone();
        mHandler = new MyHandler();
        mPhone.getPreferredNetworkType(
                mHandler.obtainMessage(MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
    }

    @Override
    protected void  onClick() {
        super.onClick();

        int networkType = isChecked() ? Phone.NT_MODE_GSM_ONLY : Phone.NT_MODE_WCDMA_PREF;
        Log.i(LOG_TAG, "set preferred network type="+networkType);
        mPhone.setPreferredNetworkType(networkType, mHandler
                .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
   }

    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int type = ((int[])ar.result)[0];
                Log.i(LOG_TAG, "get preferred network type="+type);
                setChecked(type == Phone.NT_MODE_GSM_ONLY);
            } else {
                // Weird state, disable the setting
                Log.i(LOG_TAG, "get preferred network type, exception="+ar.exception);
//                setEnabled(false);
//chul.park@lge.com // "Use only 2G network" Setting
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                // Yikes, error, disable the setting
                setEnabled(false);
                // Set UI to current state
                Log.i(LOG_TAG, "set preferred network type, exception=" + ar.exception);
                mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            } else {
                Log.i(LOG_TAG, "set preferred network type done");
//LGE_TD3658 update Network mode after click on use 2G key START
		int networkMode;
		if (isChecked()) {
		networkMode = Phone.NT_MODE_GSM_ONLY;		
		} else {
		networkMode = Phone.NT_MODE_WCDMA_PREF; 		
		}
                Log.i(LOG_TAG, "set networkMode to" + networkMode);
                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        networkMode);
       		switch(networkMode) {
          	  case Phone.NT_MODE_WCDMA_PREF:
                // TODO T: Make all of these strings come from res/values/strings.xml.
             	   mButtonPreferredNetworkMode.setSummary("Preferred network mode: WCDMA pref");
                  break;
         	  case Phone.NT_MODE_GSM_ONLY:
            	    mButtonPreferredNetworkMode.setSummary("Preferred network mode: GSM only");
                  break;
	}
                mButtonPreferredNetworkMode.setValue(Integer.toString(networkMode));
//LGE_TD3658 END
            }
        }
    }
//LGE_TD3658 update use 2G checkbox after changing Network mode START
	public void update () {

                int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode);
			Log.i(LOG_TAG, "update 2Gkey" + settingsNetworkMode);
				if (settingsNetworkMode == Phone.NT_MODE_GSM_ONLY) {
                                	setChecked(true);
				} else {
					setChecked(false);
				}
	}
//LGE_TD3658 update Network mode after click on use 2G key
	public void setButtonPreferredNetworkMode (ListPreference pref){
		mButtonPreferredNetworkMode = pref;
	}
//LGE_TD3658 END
}
