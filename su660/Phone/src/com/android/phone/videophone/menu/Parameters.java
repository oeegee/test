package com.android.phone.videophone.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import android.os.SystemProperties;
import android.util.Log;



class Size {
	/**
	 * Sets the dimensions for pictures.
	 *
	 * @param w the photo width (pixels)
	 * @param h the photo height (pixels)
	 */
	public Size(int w, int h) {
		width = w;
		height = h;
	}
	/** width of the picture */
	public int width;
	/** height of the picture */
	public int height;
};


/**
 * The Parameter which get from Camera Instance. 
 * 
 * This class has Map instance and internally managed key and value of preference.
 * 
 * @note Currently, it does'n use.
 *  because we don't have camera instance and parameter.  
 * 
 * 
 * @author hogyun.kim *
 */
public class Parameters {
	private static final String TAG = "Camera";
    private static final boolean DBG = 
        (SystemProperties.getInt("ro.debuggable", 0) == 1);


	// Parameter keys to communicate with the camera driver.

	//HGKIM 추가사항
	private static final String KEY_BRIGHTNESS_SELECT = "pref_camera_brightness_key";
	private static final String KEY_ZOOM_MODE= "zoom";
	private static final String KEY_NIGHT_MODE = "scene-mode";
	private static final String KEY_WHITE_BALANCE = "whitebalance";
	private static final String KEY_COLOUR_EFFECT = "effect";

	// Parameter key suffix for supported values.
	private static final String SUPPORTED_VALUES_SUFFIX = "-values";


	//HGKIM 추가 사항
	// Values for brightness effect entries.
	public static final String BRIGHTNESS_SELECT_HIGH = "High";
	public static final String BRIGHTNESS_SELECT_MIDDLE = "Middle";
	public static final String BRIGHTNESS_SELECT_LOW = "Low";

	// Values for white balance effect entries.
	public static final String WHITE_BALANCE_AUTO = "auto";
	public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
	public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
	public static final String WHITE_BALANCE_CLOUDY = "cloudy";
	public static final String WHITE_BALANCE_DAYLIGHT = "daylight";    
	//    public static final String WHITE_BALANCE_TWILIGHT = "twilight";
	//    public static final String WHITE_BALANCE_SHADE = "shade";
	//    public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";

	// Values for colour effect entries.
	public static final String COLOUR_EFFECT_OFF = "none";
	public static final String COLOUR_EFFECT_SEPIA = "sepia";
	public static final String COLOUR_EFFECT_MONO = "mono";
	public static final String COLOUR_EFFECT_NEGATIVE = "negative";

	// Values for zoom entries.
	public static final String ZOOM_MODE_1X = "0";
	public static final String ZOOM_MODE_2X = "8";

	// Values for nightmode entries.
	public static final String NIGHT_MODE_ON = "night";
	public static final String NIGHT_MODE_OFF = "auto";

	private HashMap<String, String> mMap;

	private Parameters() {
		mMap = new HashMap<String, String>();
	}

	/**
	 * Writes the current Parameters to the log.
	 * @hide
	 * @deprecated
	 */
	public void dump() {
	    if(DBG)
	    {
	        Log.e(TAG, "dump: size=" + mMap.size());
	        for (String k : mMap.keySet()) {
	            Log.e(TAG, "dump: " + k + "=" + mMap.get(k));
	        }	        
	    }
	}

	/**
	 * Creates a single string with all the parameters set in
	 * this Parameters object.
	 * <p>The {@link #unflatten(String)} method does the reverse.</p>
	 *
	 * @return a String with all values from this Parameters object, in
	 *         semi-colon delimited key-value pairs
	 */
	public String flatten() {
		StringBuilder flattened = new StringBuilder();
		for (String k : mMap.keySet()) {
			flattened.append(k);
			flattened.append("=");
			flattened.append(mMap.get(k));
			flattened.append(";");
		}
		// chop off the extra semicolon at the end
		flattened.deleteCharAt(flattened.length()-1);
		return flattened.toString();
	}

	/**
	 * Takes a flattened string of parameters and adds each one to
	 * this Parameters object.
	 * <p>The {@link #flatten()} method does the reverse.</p>
	 *
	 * @param flattened a String of parameters (key-value paired) that
	 *                  are semi-colon delimited
	 */
	public void unflatten(String flattened) {
		mMap.clear();

		StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
		while (tokenizer.hasMoreElements()) {
			String kv = tokenizer.nextToken();
			int pos = kv.indexOf('=');
			if (pos == -1) {
				continue;
			}
			String k = kv.substring(0, pos);
			String v = kv.substring(pos + 1);
			mMap.put(k, v);
		}
	}

	public void remove(String key) {
		mMap.remove(key);
	}

	/**
	 * Sets a String parameter.
	 *
	 * @param key   the key name for the parameter
	 * @param value the String value of the parameter
	 */
	public void set(String key, String value) {
		if (key.indexOf('=') != -1 || key.indexOf(';') != -1) {
		    if(DBG)
			Log.e(TAG, "Key \"" + key + "\" contains invalid character (= or ;)");
			return;
		}
		if (value.indexOf('=') != -1 || value.indexOf(';') != -1) {
		    if(DBG)
			Log.e(TAG, "Value \"" + value + "\" contains invalid character (= or ;)");
			return;
		}

		mMap.put(key, value);
	}

	/**
	 * Sets an integer parameter.
	 *
	 * @param key   the key name for the parameter
	 * @param value the int value of the parameter
	 */
	public void set(String key, int value) {
		mMap.put(key, Integer.toString(value));
	}

	/**
	 * Returns the value of a String parameter.
	 *
	 * @param key the key name for the parameter
	 * @return the String value of the parameter
	 */
	public String get(String key) {
		return mMap.get(key);
	}

	/**
	 * Returns the value of an integer parameter.
	 *
	 * @param key the key name for the parameter
	 * @return the int value of the parameter
	 */
	public int getInt(String key) {
		return Integer.parseInt(mMap.get(key));
	}

	/**
	 * Gets the current white balance setting.
	 *
	 * @return one of WHITE_BALANCE_XXX string constant. null if white
	 *         balance setting is not supported.
	 */
	public String getWhiteBalance() {
		return get(KEY_WHITE_BALANCE);
	}

	/**
	 * Sets the white balance.
	 *
	 * @param value WHITE_BALANCE_XXX string constant.
	 */
	public void setWhiteBalance(String value) {
		set(KEY_WHITE_BALANCE, value);
	}

	/**
	 * Gets the supported white balance.
	 *
	 * @return a List of WHITE_BALANCE_XXX string constants. null if white
	 *         balance setting is not supported.
	 */
	public List<String> getSupportedWhiteBalance() {
		String str = get(KEY_WHITE_BALANCE + SUPPORTED_VALUES_SUFFIX);
		return split(str);
	}


	/**
	 * Gets the current white balance setting.
	 *
	 * @return one of COLOUR_EFFECT_XXX string constant. null if white
	 *         balance setting is not supported.
	 */
	public String getColourEffect() {
		return get(KEY_COLOUR_EFFECT);
	}

	/**
	 * Sets the white balance.
	 *
	 * @param value COLOUR_EFFECT_XXX string constant.
	 */
	public void setColourEffect(String value) {
		set(KEY_COLOUR_EFFECT, value);
	}

	/**
	 * Gets the supported white balance.
	 *
	 * @return a List of COLOUR_EFFECT_XXX string constants. null if white
	 *         balance setting is not supported.
	 */
	public List<String> getSupportedColourEffect() {
		String str = get(KEY_COLOUR_EFFECT + SUPPORTED_VALUES_SUFFIX);
		return split(str);
	}

	// hgkim add it 
	//////////////////////////////////////////

	/**
	 * Birghtness 모드
	 * @author hogyun.kim
	 */
	public String getBrightness() {
		return get(KEY_BRIGHTNESS_SELECT);
	}

	/**
	 * Sets the brightness.
	 */
	public void setBrightness(String value) {
		set(KEY_BRIGHTNESS_SELECT, value);
	}
	public List<String> getSupportedBrightness() {
		String str = get(KEY_BRIGHTNESS_SELECT + SUPPORTED_VALUES_SUFFIX);
		return split(str);
	}

	/**
	 * Gets the current brightness selectting.
	 * @author hogyun.kim
	 */
	public String getNight() {
		return get(KEY_NIGHT_MODE);
	}

	/**
	 * Night 모드 
	 * Sets the brightness.
	 */
	public void setgetNight(String value) {
		set(KEY_NIGHT_MODE, value);
	}
	public List<String> getSupportedNight() {
		String str = get(KEY_NIGHT_MODE + SUPPORTED_VALUES_SUFFIX);
		return split(str);
	}

	/**
	 * Gets current zoom value. This also works when smooth zoom is in
	 * progress.
	 * 
	 * Birghtness 모드
	 *
	 * @return the current zoom value. The range is 0 to {@link
	 *          #getMaxZoom}.
	 * @hide
	 */
	public int getZoom() {
		return getInt("zoom");
	}

	/**
	 * Sets current zoom value. If {@link #startSmoothZoom(int)} has been
	 * called and zoom is not stopped yet, applications should not call this
	 * method.
	 *
	 * @param value zoom value. The valid range is 0 to {@link #getMaxZoom}.
	 * @hide
	 */
	public void setZoom(int value) {
		set("zoom", value);
	}

	/**
	 * Returns true if zoom is supported. Applications should call this
	 * before using other zoom methods.
	 *
	 * @return true if zoom is supported.
	 * @hide
	 */
	public boolean isZoomSupported() {
		String str = get("zoom-supported");
		return "true".equals(str);
	}


	// Splits a comma delimited string to an ArrayList of String.
	// Return null if the passing string is null or the size is 0.
	private ArrayList<String> split(String str) {
		if (str == null) return null;

		// Use StringTokenizer because it is faster than split.
		StringTokenizer tokenizer = new StringTokenizer(str, ",");
		ArrayList<String> substrings = new ArrayList<String>();
		while (tokenizer.hasMoreElements()) {
			substrings.add(tokenizer.nextToken());
		}
		return substrings;
	}

	// Splits a comma delimited string to an ArrayList of Integer.
	// Return null if the passing string is null or the size is 0.
	private ArrayList<Integer> splitInt(String str) {
		if (str == null) return null;

		StringTokenizer tokenizer = new StringTokenizer(str, ",");
		ArrayList<Integer> substrings = new ArrayList<Integer>();
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			substrings.add(Integer.parseInt(token));
		}
		if (substrings.size() == 0) return null;
		return substrings;
	}

	// Splits a comma delimited string to an ArrayList of Size.
	// Return null if the passing string is null or the size is 0.
	private ArrayList<Size> splitSize(String str) {
		if (str == null) return null;

		StringTokenizer tokenizer = new StringTokenizer(str, ",");
		ArrayList<Size> sizeList = new ArrayList<Size>();
		while (tokenizer.hasMoreElements()) {
			Size size = strToSize(tokenizer.nextToken());
			if (size != null) sizeList.add(size);
		}
		if (sizeList.size() == 0) return null;
		return sizeList;
	}

	// Parses a string (ex: "480x320") to Size object.
	// Return null if the passing string is null.
	private Size strToSize(String str) {
		if (str == null) return null;

		int pos = str.indexOf('x');
		if (pos != -1) {
			String width = str.substring(0, pos);
			String height = str.substring(pos + 1);
			return new Size(Integer.parseInt(width),
					Integer.parseInt(height));
		}
		if(DBG)
		Log.e(TAG, "Invalid size parameter string=" + str);
		return null;
	}
}