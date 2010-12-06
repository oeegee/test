package com.android.phone;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class ShowProgressDialog {
	private static final String TAG = ShowProgressDialog.class.getSimpleName();
	
	private static final String BUNDLE_MESSAGE = "message:" + ShowProgressDialog.class.getSimpleName();
	private static final String BUNDLE_OPTIONS = "options:" + ShowProgressDialog.class.getSimpleName();

	public static final int OPTION_NONE = 0x0;
	public static final int OPTION_CANCELABLE = 0x1;
	public static final int OPTION_INDETERMINATE = 0x2;
	
	private static final int OPTION_IS_SHOWING = 0x10000;
	
	private ProgressDialog mDialog = null;
	private String mMessage = "";
	private int mOptions = OPTION_NONE;
	
	public static int setOption(int aOptions, int aOption, boolean aSet){
		if(aSet)
			aOptions |= aOption;
		else
			aOptions &= ~(aOption);
		
		return aOptions;
	}
	
	public static boolean hasOption(int aOptions, int aOption){
		return (aOptions & aOption) == aOption;
	}
	    	
	public ShowProgressDialog(){
	}
	
	public synchronized void showDialog(ProgressDialog aDialog, String aMessage, int aOptions){
		//Log.v(TAG, "showDialog()");
		if(mDialog != null){
			if(mDialog.isShowing()){
				//Log.v(TAG, "->dismiss old dialog");
				mDialog.dismiss();
			}
			mDialog = null;
		}
		
		mDialog = aDialog;
		mMessage = aMessage;
		mOptions = aOptions;
		if(mDialog != null){
			mDialog.setMessage(aMessage);    
			mOptions = setOption(mOptions, OPTION_IS_SHOWING, true);
			//Log.v(TAG, "->show new dialog");
			mDialog.show();
		}    			
	}
	
	public synchronized void hide(){
		//Log.v(TAG, "hide()");
		if(mDialog != null){
			mMessage = "";
			if(mDialog.isShowing()){
				mOptions = setOption(mOptions, OPTION_IS_SHOWING, false);
				//Log.v(TAG, "->hide(");
				mDialog.hide();
			}
		}
	}
	
	public synchronized void dismiss(){
		//Log.v(TAG, "dismiss()");
		if(mDialog != null){
			mMessage = "";
			try{
				if(mDialog.isShowing()){
					//Log.v(TAG, "->dismiss");
					mDialog.dismiss();
				}
			}catch(Exception e){
				Log.e(TAG, "Can't dismiss progress dialog", e);
			}

			mOptions = setOption(mOptions, OPTION_IS_SHOWING, false);
			mDialog = null;
		}else{
			//Log.v(TAG, "-> no dialog is showing to dismiss");
		}
	}
	
	public void onCreateActivity(Context aContext, Bundle savedInstanceState){
		mOptions = OPTION_NONE;
		if(savedInstanceState == null)
			return;
		
		mMessage = savedInstanceState.getString(BUNDLE_MESSAGE);
		mOptions = savedInstanceState.getInt(BUNDLE_OPTIONS);
	}
	
	public void onResumeActivity(Context aContext){
		if(hasOption(mOptions, OPTION_IS_SHOWING)){
			ProgressDialog dialog = new ProgressDialog(aContext);
			dialog.setIndeterminate(hasOption(mOptions, OPTION_INDETERMINATE));
			dialog.setCancelable(hasOption(mOptions, OPTION_CANCELABLE));
			showDialog(dialog, mMessage, mOptions);
		}
	}
	
	public void onStopActivity(Context aContext){
		//Log.v(TAG, "onStopActivity()");
		if(mDialog != null){
			//Log.v(TAG, "->dismiss");
			mDialog.dismiss();
			mDialog = null;
		}
		mOptions = OPTION_NONE;
	}

	public void onSaveActivityInstance(Context aContext, Bundle out){
		if(mMessage != null)
			out.putString(BUNDLE_MESSAGE, mMessage);
		out.putInt(BUNDLE_OPTIONS, mOptions);
	}
}