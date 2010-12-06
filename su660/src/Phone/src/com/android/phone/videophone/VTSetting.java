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
package com.android.phone.videophone;

import java.io.File;

import com.android.phone.CallDurationSetting;
import com.android.phone.R;
//import com.lge.config.StarConfig;

import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class VTSetting extends PreferenceActivity 
{
    private static final boolean DBG = 
        (SystemProperties.getInt("ro.debuggable", 0) == 1);
    
	private static final String BUTTON_USE_PRIVATE_KEY = "button_use_private_image_key";
	private static final String BUTTON_SET_PRIVATE_KEY  = "button_set_private_image_key";
	private static final String BUTTON_SET_SPEAKER_KEY   = "button_set_speaker_key";
	private static final String BUTTON_SWITCH_VOICE_KEY  = "button_swtich_voice_key";	

	private CheckBoxPreference mButtonUsePrivate;
	private PreferenceScreen mButtonSetPrivateEx;
	private CheckBoxPreference mButtonSetSpeaker;
	private CheckBoxPreference mButtonFallback;

	private Intent mSetPrivateIntent;

	private static final int GET_VTCAP_CONTENT = 1;
	private static final String DEFAULTIMAGE = "/system/media/image/vt/default.jpg";

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mButtonUsePrivate) {
			Settings.System.putInt(getContentResolver(),
					Settings.System.VT_USE_PRIVATE, mButtonUsePrivate.isChecked()?1:0);
		}else if (preference == mButtonSetSpeaker) {
			Settings.System.putInt(getContentResolver(),
					Settings.System.VT_SET_SPEAKER, mButtonSetSpeaker.isChecked()?1:0);
		}else if (preference == mButtonFallback) {
			if (System.getProperty("user.operator", "unknown").equals("LGT"))
				return false;
			Settings.System.putInt(getContentResolver(),
					Settings.System.VT_AUTO_FALLBACK, mButtonFallback.isChecked()?1:0);
		}else if (preference == mButtonSetPrivateEx) {
			//final String OPERATOR = System.getProperty("user.operator");
			//final String COUNTRY = System.getProperty("user.country");
			final String VTCAP_BUCKET_NAME;

			//if(StarConfig.COUNTRY.equals("KR") && StarConfig.OPERATOR.equals("SKT"))
			//	VTCAP_BUCKET_NAME = "/lgdrm";
			//else
				VTCAP_BUCKET_NAME = Environment.getExternalStorageDirectory().toString() + "/DCIM/" + "VTCAP";
			final int VTCAP_BUCKET_ID = (VTCAP_BUCKET_NAME.toLowerCase().hashCode());
			Uri target = Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("bucketId", Integer.toString(VTCAP_BUCKET_ID)).build();
			Intent pickIntent = new Intent(Intent.ACTION_PICK);

			pickIntent.setData(target);
			startActivityForResult(pickIntent, GET_VTCAP_CONTENT);
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.video_call_setting);

		mButtonUsePrivate    = (CheckBoxPreference) findPreference(BUTTON_USE_PRIVATE_KEY);
		mButtonSetPrivateEx = (PreferenceScreen)findPreference(BUTTON_SET_PRIVATE_KEY);	  
		mButtonSetSpeaker    = (CheckBoxPreference) findPreference(BUTTON_SET_SPEAKER_KEY);
		mButtonFallback    = (CheckBoxPreference) findPreference(BUTTON_SWITCH_VOICE_KEY);

		mButtonUsePrivate.setChecked(Settings.System.getInt(getContentResolver(),Settings.System.VT_USE_PRIVATE,0) > 0);
		mButtonSetSpeaker.setChecked(Settings.System.getInt(getContentResolver(),Settings.System.VT_SET_SPEAKER,0) > 0);
		if (System.getProperty("user.operator", "unknown").equals("LGT"))
		{	
			getPreferenceScreen().removePreference(mButtonFallback);
		}
		else
			mButtonFallback.setChecked(Settings.System.getInt(getContentResolver(),Settings.System.VT_AUTO_FALLBACK,0) > 0);

		// Get name and display
		displayPrivateName();
		/*	 
	 if (mButtonSetPrivateEx != null) {
        mSetPrivateIntent = new Intent(Intent.ACTION_MAIN);
        mSetPrivateIntent.setClassName(this, CallDurationSetting.class.getName());
        mButtonSetPrivateEx.setIntent (mSetPrivateIntent);
        }
		 */
	}

	@Override
	public void onResume() {
		super.onResume();

		// Get name and display
		displayPrivateName();
	}

	private final void displayPrivateName() {
		CharSequence summary = Settings.System.getString(getContentResolver(),Settings.System.VT_PRIVATE_NAME);
              CharSequence defaultimage =  getResources().getString(R.string.vt_summary_private_path);

		int length = summary.length();

		if(summary.toString().equals(DEFAULTIMAGE))
                     mButtonSetPrivateEx.setSummary(defaultimage);
              else 
              {
                  File file;
                  file = new File(summary.toString());
                  if(!file.exists())
                  {
                        Settings.System.putString(getContentResolver(),Settings.System.VT_PRIVATE_NAME, DEFAULTIMAGE);
                        mButtonSetPrivateEx.setSummary(defaultimage);
                        return;
                  }
                  else
                        mButtonSetPrivateEx.setSummary(file.getName());
              }		
	}	

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == GET_VTCAP_CONTENT) {
			if (resultCode == RESULT_OK && intent != null) {
				Uri uri = intent.getData();
				String absoluteFilePath = null;
				Cursor cursor = getContentResolver().query(uri, null, null, null, null);
				if(DBG)
                Log.d("camera gallery"," URI LOGO = "+ uri.toString());

                if(cursor.getCount() == 0)
                {
                    absoluteFilePath = DEFAULTIMAGE;
                }
				else if(cursor.moveToFirst()) {
					int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
					absoluteFilePath = cursor.getString(idx);
				}

                // SQL을 이용하여 query한 후 생성된 cursor를 close하지 않으면 DatabaseObjectNotClosedException() 발생
                if (cursor != null)  cursor.close();
                //-- end 
                
				Settings.System.putString(getContentResolver(),Settings.System.VT_PRIVATE_NAME, absoluteFilePath);
			}
		}
	}
}
