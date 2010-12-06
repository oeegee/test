package com.mtelo.visualexpression;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.telephony.Call;
import com.android.phone.PhoneApp;
import com.lge.config.StarConfig;

public class VEUtils {
	
	private static String LOG_TAG = "VEUtils";
	private static final boolean DBG = true;
	
	public enum IsVEPlayerRunning {
		YES,
		NO,
	}
	
	private static IsVEPlayerRunning mIsVEPlayerRunning = IsVEPlayerRunning.NO;
	
	/**
	 * 
	 * @return
	 */
/*	public static boolean isVECallAvailabe(){
		boolean ret = false;
		
		if( ViewGroup.GONE == PhoneApp.getInstance().getVEVisibility() ){
			ret = true;
		}
		
		return ret;
	}*/
	
	
	public static boolean isVoiceCall(){
		boolean ret = false;
		
		if(!PhoneApp.mIsVideoCall){
			ret = true;
		}
		
		return ret;
	}
	
	
	public static boolean isMainCall(){
		boolean ret = false;
		Call fgCall = PhoneApp.getInstance().getPhone().getForegroundCall();
		if(fgCall.isIdle() && !fgCall.hasConnections()){
			ret = true;
		}
		return ret;
	}
	
	
	public static boolean initVEContent(Handler mHandler, String url){
		boolean ret = false;
		
		if(null == url){
			url = "http://211.115.5.71:7072/MRBT/123/A/123/A12300000517_41_42.am3"; 
		}
		
		if( mIsVEPlayerRunning == IsVEPlayerRunning.NO){
			VE_ContentManager.getInstance().init(PhoneApp.getInstance().getApplicationContext(), mHandler, url);//
//	        ve_contentmgr = new VE_ContentManager(PhoneApp.getInstance().getApplicationContext(), mHandler, null);
//	        ve_contentmgr.doCheck();
			mIsVEPlayerRunning = IsVEPlayerRunning.YES;
		}
		return ret;
	}
	
	public static IsVEPlayerRunning getVEPlayerStatus(){
		return mIsVEPlayerRunning;
	}
	
	public static void setVEPlayerStatus(IsVEPlayerRunning type){
		mIsVEPlayerRunning = type;
	}
	
    //jundj@mo2.co.kr START
    public static final int CALLCARD_PERSONINFO_DEFAULT = 0;
    public static final int CALLCARD_PERSONINFO_VE = 1;
    
    /**
     * switch callcard each other - default callcard and ve callcard.
     * @param type
     */
    public static void setVisibleCallCardPersonInfo(int type){
    	if (DBG) Log.d(LOG_TAG, "VE: call setVisibleCallCardPersonInfo()");
		PhoneApp phoneapp = PhoneApp.getInstance();
		if(null == phoneapp.mInCallScreen 
			|| null == phoneapp.mInCallScreen.mCallCard
			|| null == phoneapp.mInCallScreen.mCallCard.callCardPersonInfo
			|| null == phoneapp.mInCallScreen.mCallCard.callCardPersonInfoVE){
			return;
		}
    	
		try{
			if(type == CALLCARD_PERSONINFO_DEFAULT){
				PhoneApp.getInstance().mInCallScreen.mCallCard.callCardPersonInfo.setVisibility(View.VISIBLE);
				PhoneApp.getInstance().mInCallScreen.mCallCard.callCardPersonInfoVE.setVisibility(View.GONE);
			}else{
				PhoneApp.getInstance().mInCallScreen.mCallCard.callCardPersonInfo.setVisibility(View.GONE);
				PhoneApp.getInstance().mInCallScreen.mCallCard.callCardPersonInfoVE.setVisibility(View.VISIBLE);
			}
		}catch(Exception e){
			// skip NullPointerExceptioin
    	}
    }
    
    /**
     * apply SKT feature only.
     */
    public static boolean isSKTFeature(){
    	boolean ret = false;
    	if(StarConfig.COUNTRY.equals("KR") && StarConfig.OPERATOR.equals("SKT")){
    		ret = true;
    	}
    	return ret;
    }
	
}
