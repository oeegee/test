package com.mtelo.visualexpression;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.phone.R;

public class AMF_Viewer extends LinearLayout
{
	private static boolean isFinishInflate = false;

	private final static String LOG_TAG = "AMF_Viewer";

	/*
	private String mName = "",
					mNickName = "",
					mPhoneNum = "";
	
	private TextView mNameTextView,
						mNickNameTextView,
						mPhoneNumTextView;
	*/					
	
	private ViewGroup mAmf_Viewer;
	
	private Context _context;
	
	private static AMF_Viewer _amf_viewer;
	
	protected void onDraw(Canvas canvas)
	{
		if ( VE_ContentManager.mBitmapData != null )
				canvas.drawBitmap(VE_ContentManager.mBitmapData, 0, 0, null);
		
		super.onDraw(canvas);
	}

	public AMF_Viewer(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		MyLog.set_LogEnable(false, "wang");
		MyLog.write(Log.INFO, LOG_TAG, "AMF_Viewer("+ context+", "+ attrs+")");
		
		_context = context;
		_amf_viewer = this;
	}
	
	public static AMF_Viewer getInstance()
	{
		MyLog.write(Log.INFO, LOG_TAG, "getInstance()");
		return _amf_viewer;
	}

	protected void onFinishInflate()
	{
		super.onFinishInflate();
		
		MyLog.write(Log.INFO, LOG_TAG, "onFinishInflate()");
		
		isFinishInflate = true;
		
		/*
		mNameTextView = (TextView)findViewById(R.id.name_ve);
		mNickNameTextView = (TextView)findViewById(R.id.label_ve);
		mPhoneNumTextView = (TextView)/findViewById(R.id.phoneNumber_ve);
		*/
		
		mAmf_Viewer = (ViewGroup)findViewById(R.id.Amf_Viewer);
		
		MyLog.write(Log.INFO, LOG_TAG, "findViewById(R.id.Video_Viewer) = " + findViewById(R.id.Video_Viewer));
	}
	
	public boolean isFinishInflate()
	{
		MyLog.write(Log.INFO, LOG_TAG, "isFinishInflate()");
		return isFinishInflate;
	}
	
	/*
	public void setName(String s)
	{
		mName = s;
		mNameTextView.setText(s);
	}
	public void setNickName(String s)
	{
		mNickName = s;
		mNickNameTextView.setText(s);
	}
	public void setPhoneNum(String s)
	{
		mPhoneNum = s;
		mPhoneNumTextView.setText(s);
	}
	*/
	
	public void setAmfViewerToVisable()
	{
		MyLog.write(Log.INFO, LOG_TAG, "setAmfViewerToVisable()");
		mAmf_Viewer.setVisibility(VISIBLE);
	}
	
	public void setAmfViewerToGone()
	{
		MyLog.write(Log.INFO, LOG_TAG, "setAmfViewerToGone()");
		mAmf_Viewer.setVisibility(GONE);
	}

}
