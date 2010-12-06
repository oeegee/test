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
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.content.DialogInterface;

import android.view.LayoutInflater;
import android.view.ViewGroup;

public class CallDurationSetting extends PreferenceActivity 
implements DialogInterface.OnClickListener
{
	public static final String PREF_CALL_DURATION = "PrefCallDuration";
	public static final String PREF_LAST_CALL = "lastCall";
	public static final String PREF_ALL_CALL = "allCall";
	public static final String PREF_MO_CALL = "moCall";
	public static final String PREF_MT_CALL = "mtCall";

	private static final String BUTTON_LAST_CALL_KEY  = "button_last_key";
	private static final String BUTTON_ALL_CALL_KEY  = "button_all_key";
	private static final String BUTTON_MO_CALL_KEY  = "button_mo_key";
	private static final String BUTTON_MT_CALL_KEY  = "button_mt_key";

	private static final long DIVIDER_HOUR = 3600000;
	private static final long DIVIDER_MIN = 60000;
	private static final long DIVIDER_SEC = 1000;

	private static int hALL, mALL, sALL;
	private static int hMO, mMO, sMO;
	private static int hMT, mMT, sMT;
	
	private Preference mButtonLast;
	private Preference mButtonAll;
	private Preference mButtonMO;
	private Preference mButtonMT;
	
	AlertDialog alertLast;
	AlertDialog alertAll;
	AlertDialog alertMO;
	AlertDialog alertMT;
	AlertDialog.Builder builder;

	AlertDialog.Builder builderLast;
	AlertDialog.Builder builderAll;
	AlertDialog.Builder builderMO;
	AlertDialog.Builder builderMT;

	LayoutInflater inflaterLast;
	LayoutInflater inflaterALL;
	LayoutInflater inflaterMO;
	LayoutInflater inflaterMT;

	View viewLast;
	View viewALL;
	View viewMO;
	View viewMT;
	
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    	
    	if (preference == mButtonLast) {
    		alertLast.show();
    		return true;
    	} else if (preference == mButtonAll) {
    		alertAll.show();
    		return true;
    	} else if (preference == mButtonMO) {
    		alertMO.show();
    		return true;
    	} else if (preference == mButtonMT) {
    		alertMT.show();
    		return true;
    	}
    	
    	return false;
    }

    private final void displayCallDuration(long duration, Preference which) {
		/** [2009. 08. 28] joypark@lge.com, All time = MO + MT time */
		int hour, minute, second;
		String time_hour, time_min, time_sec;
	
		if(which == mButtonAll){
			hour = hMO+hMT;
			minute = mMO+mMT;
			second = sMO+sMT;
	
			if (minute > 59){
				minute = minute-60;
				hour++;
			}
			if (second > 59){
				second = second-60;
				minute++;
			}
		}
		else{
			hour = (int) (duration / DIVIDER_HOUR);
			minute = (int) ((duration % DIVIDER_HOUR) / DIVIDER_MIN);
			second = (int) ((duration % DIVIDER_MIN) / DIVIDER_SEC);
			
			if(which == mButtonMO){
				hMO = hour;
				mMO = minute;
				sMO = second;
			}else if(which == mButtonMT){
				hMT = hour;
				mMT = minute;
				sMT = second;
			}
		}
	
		time_hour = Integer.valueOf(hour).toString();
		time_min = Integer.valueOf(minute).toString();
		time_sec = Integer.valueOf(second).toString();
		/* [2009. 08. 28] joypark@lge.com **/
	
		if (hour < 10)
			time_hour = "0" + time_hour;			
		if (minute < 10)
			time_min = "0" + time_min;		  
		if (second < 10)
			time_sec = "0" + time_sec;
	
		CharSequence summary = time_hour + ":" + time_min + ":" + time_sec;
		which.setSummary(summary);
	}
      
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.call_duration_setting);

        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonLast  = (Preference) prefSet.findPreference(BUTTON_LAST_CALL_KEY);
        mButtonAll  = (Preference) prefSet.findPreference(BUTTON_ALL_CALL_KEY);
        mButtonMO  = (Preference) prefSet.findPreference(BUTTON_MO_CALL_KEY);
        mButtonMT  = (Preference) prefSet.findPreference(BUTTON_MT_CALL_KEY);

        SharedPreferences settings = getSharedPreferences(PREF_CALL_DURATION, 0);

        long duration;
        
        duration = settings.getLong(PREF_LAST_CALL, 0);
        displayCallDuration(duration, mButtonLast);

        duration = settings.getLong(PREF_MO_CALL, 0);
        displayCallDuration(duration, mButtonMO);

        duration = settings.getLong(PREF_MT_CALL, 0);
        displayCallDuration(duration, mButtonMT);
		
        duration = settings.getLong(PREF_ALL_CALL, 0);
        displayCallDuration(duration, mButtonAll);

		/** [2009. 08. 28] joypark@lge.com , alertDialog for each menu*/
		inflaterLast = LayoutInflater.from(this);
		inflaterALL = LayoutInflater.from(this);
		inflaterMO = LayoutInflater.from(this);
		inflaterMT = LayoutInflater.from(this);
	
	
		viewLast= inflaterLast.inflate(R.layout.delete_call_duration, null);
		viewALL= inflaterALL.inflate(R.layout.delete_call_duration, null);
		viewMO= inflaterMO.inflate(R.layout.delete_call_duration, null);
		viewMT= inflaterMT.inflate(R.layout.delete_call_duration, null);
		
		builderLast = new AlertDialog.Builder(this);
		builderLast.setTitle(R.string.messageCallDuration)
			//.setCancelable(false)
		    .setIcon(com.android.internal.R.drawable.ic_dialog_alert)	// 2010-05-12, cutestar@lge.com Change Alert Image 
		    .setMessage(R.string.messageLastCall)
			.setPositiveButton(R.string.messageOK,this)
			.setNegativeButton(R.string.messageCancel,this)
			.setView(viewLast);
	
		builderAll = new AlertDialog.Builder(this);
		builderAll.setTitle(R.string.messageCallDuration)
			//.setCancelable(false)
			.setIcon(com.android.internal.R.drawable.ic_dialog_alert)	// 2010-05-12, cutestar@lge.com Change Alert Image 
		  	.setMessage(R.string.messageAllCall)
			.setPositiveButton(R.string.messageOK,this)
			.setNegativeButton(R.string.messageCancel,this)
			.setView(viewALL);
	
		builderMO = new AlertDialog.Builder(this);
		builderMO.setTitle(R.string.messageCallDuration)
			//.setCancelable(false)
			.setIcon(com.android.internal.R.drawable.ic_dialog_alert)	// 2010-05-12, cutestar@lge.com Change Alert Image 
			.setMessage(R.string.messageMOCall)
			.setPositiveButton(R.string.messageOK,this)
			.setNegativeButton(R.string.messageCancel,this)
			.setView(viewMO);
	
		builderMT = new AlertDialog.Builder(this);
		builderMT.setTitle(R.string.messageCallDuration)
			//.setCancelable(false)
			.setIcon(com.android.internal.R.drawable.ic_dialog_alert)	// 2010-05-12, cutestar@lge.com Change Alert Image 
			.setMessage(R.string.messageMTCall)
			.setPositiveButton(R.string.messageOK,this)
			.setNegativeButton(R.string.messageCancel,this)
			.setView(viewMT);
		/* [2009. 08. 28] joypark@lge.com **/
	
		alertLast = builderLast.create();
		alertAll = builderAll.create();
		alertMO = builderMO.create();
		alertMT = builderMT.create();
    }

	/* [2009. 09. 13] joypark@lge.com **/
	@Override
	 public void onResume() {
	 	SharedPreferences settings = getSharedPreferences(PREF_CALL_DURATION, 0);
		long duration;

		super.onResume();

        duration = settings.getLong(PREF_LAST_CALL, 0);
        displayCallDuration(duration, mButtonLast);

        duration = settings.getLong(PREF_MO_CALL, 0);
        displayCallDuration(duration, mButtonMO);

        duration = settings.getLong(PREF_MT_CALL, 0);
        displayCallDuration(duration, mButtonMT);
		
        duration = settings.getLong(PREF_ALL_CALL, 0);
        displayCallDuration(duration, mButtonAll);	
	}
	
    private final void clearCallDuration(String key, Preference which) {
    	SharedPreferences settings = getSharedPreferences(PREF_CALL_DURATION, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putLong(key, 0);
    	editor.commit();
    }

    public void onClick(DialogInterface dialog, int which) {

        dialog.dismiss();

        switch (which){
            case DialogInterface.BUTTON1:

            	if (dialog == alertLast) {
            		clearCallDuration(PREF_LAST_CALL, mButtonLast);
                    displayCallDuration(0, mButtonLast);
            	} else if (dialog == alertAll) {
            	/** [2009. 08. 28] joypark@lge.com, clear all duration time */
					clearCallDuration(PREF_LAST_CALL, mButtonLast);
                    displayCallDuration(0, mButtonLast);
					clearCallDuration(PREF_MO_CALL, mButtonMO);
                    displayCallDuration(0, mButtonMO);
					clearCallDuration(PREF_MT_CALL, mButtonMT);
                    displayCallDuration(0, mButtonMT);
            		clearCallDuration(PREF_ALL_CALL, mButtonAll);
                    displayCallDuration(0, mButtonAll);
				/* [2009. 08. 28] joypark@lge.com **/
            	} else if (dialog == alertMO) {
            		clearCallDuration(PREF_MO_CALL, mButtonMO);
                    displayCallDuration(0, mButtonMO);
            	} else if (dialog == alertMT) {
            		clearCallDuration(PREF_MT_CALL, mButtonMT);
                    displayCallDuration(0, mButtonMT);
            	}
            	
            	displayMessage();
                break;
            default:
            	break;
        }
    }
    
    private final void displayMessage() {
        Toast.makeText(this, getString(R.string.popupDone), Toast.LENGTH_SHORT)
            .show();
    }

}
