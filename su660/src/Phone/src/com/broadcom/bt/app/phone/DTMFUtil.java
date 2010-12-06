package com.broadcom.bt.app.phone;

import com.android.phone.CallFeaturesSetting;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.provider.Settings;
import android.util.Log;

public class DTMFUtil {
	private static final boolean DBG = true;
	private static final String LOG_TAG = "DTMFUtil";

	public static final int DTMF_DUR_MS = 100;

	private Context mContext;
	private boolean mDTMFToneEnabled = true;
	private ToneGenerator mToneGenerator;

	public DTMFUtil(Context context) {
		mContext = context;

		// see if we need to play local tones.
		// mDTMFToneEnabled =
		// Settings.System.getInt(mContext.getContentResolver(),
		// Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

		// create the tone generator
		// if the mToneGenerator creation fails, just continue without it. It is
		// a local audio signal, and is not as important as the dtmf tone
		// itself.			
		

		if (mDTMFToneEnabled) {
			if (mToneGenerator == null) {
				try {
					mToneGenerator = new ToneGenerator(
							AudioManager.STREAM_DTMF, DTMF_DUR_MS);
				} catch (RuntimeException e) {
					if (DBG)
						Log.e(LOG_TAG,
								"Exception caught while creating local tone generator: "
										+ e);
					mToneGenerator = null;
				}
			}
		}
	}

	public void playDTMF(char c) {
		if (mToneGenerator == null) {
			if (DBG) {
				Log.e(LOG_TAG, "Tone Generator not enabled: ignoring request to play tone " + c);
			}
			return;
		}
		
		int tone = -1;
		switch (c) {
		case '#':
			tone = ToneGenerator.TONE_DTMF_P;
		case '*':
			tone = ToneGenerator.TONE_DTMF_P;
		default:
			if (c >= '0' && c <= '9') {
				tone = ToneGenerator.TONE_DTMF_0 + c - '0';
			}
		}
		if (tone >=0) {
             mToneGenerator.startTone(tone, DTMF_DUR_MS);
		} else if (DBG){
			Log.e(LOG_TAG, "Invalid TONE: " + c);
		}

	}
}
