package com.android.phone;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CarrierInfoPreference extends Preference {
	public static final int STYLE_NONE = 0x0;
	public static final int STYLE_MULTI_LINE_TITLE = 0x1;
	
	private int mStyle = STYLE_NONE; //STYLE_MULTI_LINE_TITLE;

	public CarrierInfoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarrierInfoPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	    
	public CarrierInfoPreference(Context context){
		super(context);
	}
	
    private boolean hasStyle(int aStyle){
    	return (mStyle & aStyle) != 0x0;
    }
    
    public void setStyle(int aStyle, boolean aEnable){
    	mStyle &= ~aStyle;
    	
    	if(aEnable){	
    		mStyle |= aStyle;
    	}
    }
    
    public void setStyle(int aStyle){
    	mStyle = aStyle;
    }
    
    public int getStyle(){
    	return mStyle;
    }
    
	protected void onBindView(View aView) {
		TextView textView = (TextView) aView.findViewById(com.android.internal.R.id.title);
		textView.setSingleLine(!hasStyle(STYLE_MULTI_LINE_TITLE));
		
		super.onBindView(aView);
	}
}
