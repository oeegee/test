package com.android.phone;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.os.AsyncResult;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class CallDurations extends PreferenceActivity {

    private Phone mPhone;

	  // String keys for preference lookup
    private static final String BUTTON_CALL_DURATION_LAST_CALL_KEY = "button_call_duration_last_call_key";
    private static final String BUTTON_CALL_DURATION_ALL_CALL_KEY = "button_call_duration_all_call_key";
    private static final String BUTTON_CALL_DURATION_DIALED_CALL_KEY = "call_duration_dialed_call";
    private static final String BUTTON_CALL_DURATION_RECEIVED_CALL_KEY = "call_duration_rcv_call";

    Preference mPrefLast, mPrefAll, mPrefDialed, mPrefReceived;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPhone = PhoneFactory.getDefaultPhone();

        addPreferencesFromResource(R.xml.hub_call_duration_list);

        // get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mPrefLast = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_LAST_CALL_KEY);
        mPrefAll = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_ALL_CALL_KEY);
        mPrefDialed = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_DIALED_CALL_KEY);
        mPrefReceived = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_RECEIVED_CALL_KEY);
    }

	@Override
    protected void onResume() {
        super.onResume();

        mPhone = PhoneFactory.getDefaultPhone();

        // get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mPrefLast = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_LAST_CALL_KEY);
        mPrefAll = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_ALL_CALL_KEY);
        mPrefDialed = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_DIALED_CALL_KEY);
        mPrefReceived = (Preference) prefSet.findPreference(BUTTON_CALL_DURATION_RECEIVED_CALL_KEY);
    }
}
