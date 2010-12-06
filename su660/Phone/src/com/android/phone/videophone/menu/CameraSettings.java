package com.android.phone.videophone.menu;

import java.util.ArrayList;
import java.util.List;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

/**
 * Preference manged Class
 * It manages preference bases on XML(SharedPreference)  
 *   
 * This class has functions such as add or remove preference from screen.
 * 
 * @author hogyun.kim
 */


public class CameraSettings {
    private static final int FIRST_REQUEST_CODE = 100;
    private static final int NOT_FOUND = -1;

    public static final String KEY_VERSION = "pref_version_key";
    public static final int CURRENT_VERSION = 3;

    private static final boolean DBG = 
        (SystemProperties.getInt("ro.debuggable", 0) == 1);
    
    @SuppressWarnings("unused")
    private static final String TAG = "CameraSettings";

    private final Context mContext;
    private final Parameters mParameters;
    private final PreferenceManager mManager;

    public CameraSettings(Activity activity, Parameters parameters) {
        mContext = activity;
        mParameters = parameters;
        mManager = new PreferenceManager(activity, FIRST_REQUEST_CODE);
       // mManager = null;
    }

    public PreferenceScreen getPreferenceScreen(int preferenceRes) {
        PreferenceScreen screen = mManager.createPreferenceScreen(mContext);
        mManager.inflateFromResource(mContext, preferenceRes, screen);
        initPreference(screen);
        return screen;
    }


    public static void removePreferenceFromScreen(
            PreferenceScreen screen, String key) {
        Preference pref = screen.findPreference(key);
        if (pref == null) {
            if(DBG)
            Log.i(TAG, "No preference found based the key : " + key);
            throw new IllegalArgumentException();
        } else {
            removePreference(screen, pref);
        }
    }

    /**
     * Dynamic menu - Remove menu entry if it is not supported on camera.
     * if you need this API, then you must implement this API. 
     * */
    private void initPreference(PreferenceScreen screen) {
//        ListPreference whiteBalance =
//                (ListPreference) screen.findPreference(KEY_WHITE_BALANCE);
//        
//        //hgkim add it.
//        ListPreference brightnessSelect =
//            (ListPreference) screen.findPreference(KEY_BRIGHTNESS_SELECT);
//        ListPreference zoomMode =
//            (ListPreference) screen.findPreference(KEY_ZOOM_MODE);
//        ListPreference nightMode =
//            (ListPreference) screen.findPreference(KEY_NIGHT_MODE);
        
        
        
//        if (whiteBalance != null) {
//            filterUnsupportedOptions(screen,
//                    whiteBalance, mParameters.getSupportedWhiteBalance());
//        }
//        
//        
//        //HGKIM ADD IT
//        if (brightnessSelect != null) {
//            filterUnsupportedOptions(screen,
//            		brightnessSelect, mParameters.getSupportedBrightness());
//        }
////        if (zoomMode != null) {
////            filterUnsupportedOptions(screen,
////            		zoomMode, mParameters.getSupportedWhiteBalance());
////        }
//        if (nightMode != null) {
//            filterUnsupportedOptions(screen,
//            		nightMode, mParameters.getSupportedNight());
//        }        
    }

    private static boolean removePreference(PreferenceGroup group,
            Preference remove) {
        if (group.removePreference(remove)) return true;

        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference child = group.getPreference(i);
            if (child instanceof PreferenceGroup) {
                if (removePreference((PreferenceGroup) child, remove)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void filterUnsupportedOptions(PreferenceScreen screen,
            ListPreference pref, List<String> supported) {

//        CharSequence[] allEntries = pref.getEntries();
//
//        // Remove the preference if the parameter is not supported or there is
//        // only one options for the settings.
//        if (supported == null || supported.size() <= 1) {
//            removePreference(screen, pref);
//            return;
//        }
//
//        CharSequence[] allEntryValues = pref.getEntryValues();
//        Drawable[] allIcons = (pref instanceof IconListPreference)
//                ? ((IconListPreference) pref).getIcons()
//                : null;
//        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
//        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
//        ArrayList<Drawable> icons =
//                allIcons == null ? null : new ArrayList<Drawable>();
//        for (int i = 0, len = allEntryValues.length; i < len; i++) {
//            if (supported.indexOf(allEntryValues[i].toString()) != NOT_FOUND) {
//                entries.add(allEntries[i]);
//                entryValues.add(allEntryValues[i]);
//                if (allIcons != null) icons.add(allIcons[i]);
//            }
//        }
//
//        // Set entries and entry values to list preference.
//        int size = entries.size();
//        pref.setEntries(entries.toArray(new CharSequence[size]));
//        pref.setEntryValues(entryValues.toArray(new CharSequence[size]));
//        if (allIcons != null) {
//            ((IconListPreference) pref)
//                    .setIcons(icons.toArray(new Drawable[size]));
//        }
//
//        // Set the value to the first entry if it is invalid.
//        String value = pref.getValue();
//        if (pref.findIndexOfValue(value) == NOT_FOUND) {
//            pref.setValueIndex(0);
//        }
    }

    private static List<String> sizeListToStringList(List<Size> sizes) {
        ArrayList<String> list = new ArrayList<String>();
        for (Size size : sizes) {
            list.add(String.format("%dx%d", size.width, size.height));
        }
        return list;
    }

    public static void upgradePreferences(SharedPreferences pref) {
        int version;
        try {
            version = pref.getInt(KEY_VERSION, 0);
        } catch (Exception ex) {
            version = 0;
        }
        if (version == CURRENT_VERSION) return;

        //HGKIM COMMENT IT .
        
//        SharedPreferences.Editor editor = pref.edit();
//        if (version == 0) {
//            // For old version, change 1 to 10 for video duration preference.
//            if (pref.getString(KEY_VIDEO_DURATION, "1").equals("1")) {
//                editor.putString(KEY_VIDEO_DURATION, "10");
//            }
//            version = 1;
//        }
//        if (version == 1) {
//            // Change jpeg quality {65,75,85} to {normal,fine,superfine}
//            String quality = pref.getString(KEY_JPEG_QUALITY, "85");
//            if (quality.equals("65")) {
//                quality = "normal";
//            } else if (quality.equals("75")) {
//                quality = "fine";
//            } else {
//                quality = "superfine";
//            }
//            editor.putString(KEY_JPEG_QUALITY, quality);
//            version = 2;
//        }
//        if (version == 2) {
//            editor.putString(KEY_RECORD_LOCATION,
//                    pref.getBoolean(KEY_RECORD_LOCATION, false)
//                    ? RecordLocationPreference.VALUE_ON
//                    : RecordLocationPreference.VALUE_NONE);
//            version = 3;
//        }
//        editor.putInt(KEY_VERSION, CURRENT_VERSION);
//        editor.commit();
    }
}
