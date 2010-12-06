package com.android.phone;

import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

//import com.lge.message.R;

// pull from Message
public class ByteLengthFilter implements InputFilter {
	private final static String LOG_TAG = "ByteLengthFilter";
	private static boolean check = true;  //phjjiny.park 100904 maxfilter check
	public interface OnMaxLengthListener {
		public void onMaxLength();
	}
	
	private static final String DEFAULT_ENCODING = "KSC5601";
	protected int 		mMax;	// byte length
    protected String	mEncode;
    protected Context 	mContext;
    
    private OnMaxLengthListener mOnMaxLength;
    
	public ByteLengthFilter(Context context, int max) {
		this(context, max, DEFAULT_ENCODING);
	}
	
	public ByteLengthFilter(Context context, int max, String encode) {
		mContext = context;
		mMax = max;
		mEncode = encode;
	}

	public void setOnMaxLengthListener(OnMaxLengthListener aListener){
		mOnMaxLength = aListener;
	}
	
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
		if(source.length()==0)return null;

		int keep = mMax - getByteLength(dest.toString());

		Log.d(LOG_TAG,"dest : "+dest+", dstart = "+dstart+", dend = " +dend +", Length = " + +getByteLength(dest.toString()) );
		Log.d(LOG_TAG,"source : " +source + ", start = " + start+", end = " + end + ", Length = " + +getByteLength(source.toString()) );

		Log.d(LOG_TAG, "dest.subSequence(0, dstart).toString() : " + dest.subSequence(0, dstart).toString() +",  getByteLength(dest.subSequence(0, dstart).toString()) " + getByteLength(dest.subSequence(0, dstart).toString()) );		

/*		if(mMax> getByteLength(dest.subSequence(0, dstart).toString())+getByteLength(source.toString()) )
			check = true;
			
		if(mMax<(getByteLength(dest.subSequence(0, dstart).toString())+getByteLength(source.toString()))){
			if(mOnMaxLength != null)
				mOnMaxLength.onMaxLength();
		}
*/
		if (dend - dstart > 0) {
			keep += getByteLength(dest.subSequence(dstart, dend).toString());
		}

		if (keep <= 0) {	
			if(mOnMaxLength != null && check) {
				mOnMaxLength.onMaxLength();
				check = false;
			}
			return "";
		}
		
		if (getByteLength(source.toString()) <= keep) {
			check = true;
			return null;
		}

		int idx = start + keep;
		int srcLen = source.length();
		
		if(idx > srcLen) {
			idx = srcLen;
		}
		
		for (; idx > start; idx--) {
			CharSequence ret = source.subSequence(start, idx);
			if (getByteLength(ret.toString()) <= keep){
				
				if(mOnMaxLength != null && check) {
					mOnMaxLength.onMaxLength();
					check = false;
				}
				
				return ret;
			}
		}

		return "";
	}
	
	protected int getByteLength(String str) {
		//return StringManager.getByteLength(str, mEncode);
		try{
			return str.getBytes(mEncode).length;
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		return -1;
	}
	
	public int getMaxLength(){
		return mMax;
	}
	
	public void setMaxLength(int max){
		mMax = max;
	}
}
