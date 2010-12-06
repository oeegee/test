/*
 * Copyright (C) 2010 LG Electronics. Inc
 *
 */

package com.android.phone;


import com.lge.provider.LGProvider;
import android.text.TextUtils;
import android.content.ContentResolver;

import android.telephony.PhoneNumberUtils;
import android.util.Log;

//LGE_CHANGED_S  - yongjin.her 2010.03.29 - remove RadioStatusChecker Activity
import android.content.Context;
import android.telephony.TelephonyManager;
//LGE_CHANGED_E  - yongjin.her 2010.03.29 - remove RadioStatusChecker Activity



//LGE_CHANGED_E  - yongjin.her 2010.01.29 for Call Lock
/**
 */
public class LockScreenDecider {

    private static final String LOG_TAG = "LockScreenDecider";

    private static final String HYPHEN = "-";


    //Prefix number
    private static final String PREFIX_1 = "*23#";
    private static final String PREFIX_2 = "#31#";
    private static final String PREFIX_3 = "*31#";


    //STR
    private static final String STR_00         = "00";
    private static final String STR_PLUS_82    = "+82";

    private static final String STR_060        = "060";
//	private static final String STR_OZ070      = "070";
    private static final String STR_700        = "700";
    private static final String STR_02700      = "02700";
    private static final String STR_011700     = "011700";
    private static final String STR_016700     = "016700";
    private static final String STR_017700     = "017700";
    private static final String STR_018700     = "018700";
    private static final String STR_019700     = "019700";
    private static final String STR_010700     = "010700";



    //CHAR
    private static final char CHAR_0          = '0';
    private static final char CHAR_3          = '3';
    private static final char CHAR_6          = '6';
    private static final char CHAR_SLASH_0    = '\0';
    private static final char CHAR_PLUS       = '+';

    private static final char CHAR_STAR       = '*';
    private static final char CHAR_SHAP       = '#';
    private static final char CHAR_P          = 'P';
    private static final char CHAR_W          = 'W';




    //Lock setting value;
    private boolean mIsLockAllCall              = false;
    private boolean mIsLockInternationalCall    = false;
    private boolean mIsLock700Call              = false;
    private boolean mIsLock060Call              = false;
//    private boolean mIsLockOZ070Call            = false;
	


    //
    //LGE_CHANGED_S  - yongjin.her 2010.03.29 - remove RadioStatusChecker Activity
    private Context         mCtx    = null;
    //LGE_CHANGED_E  - yongjin.her 2010.03.29 - remove RadioStatusChecker Activity
    private ContentResolver mCr     = null;
    

//LGE_CHANGED_S  - yongjin.her 2010.03.29 - remove RadioStatusChecker Activity
//    public LockScreenDecider(/*Context ctx,*/ ContentResolver cr) {
    public LockScreenDecider(Context ctx) {
//LGE_CHANGED_E  - yongjin.her 2010.03.29 - remove RadioStatusChecker Activity
        mCtx = ctx;
        mCr  = ctx.getContentResolver();

        readSettingValue();
    }


	/**
	 * remove the Hyphen from origin string and return no-Hyphen string
	 * 
	 * @param pPhoneNum
	 * @return String that has no Hyphen('-')
	 * 
	 */
    private String removeHyphen(String origin) {

        if(TextUtils.isEmpty(origin)) return null;

        String   return_str = null;
        String[] origin_split = origin.split(HYPHEN);

        for(int i=0; i < origin_split.length ; i++) 
        {
            if(return_str==null)
            {
                return_str = origin_split[i];
            }
            else
            {
                return_str = return_str + origin_split[i];
            }
        }

        return return_str;
    }
        

	/**
	 * read Configure value from LGT setting or other.
	 * Configure value will be saved in this class's member variable.
	 * 
	 */
    private void readSettingValue () {
        /*

            mIsLockAllCall       = false;
            mIsLockInternationalCall = false;
            mIsLock700Call       = false;
            mIsLock060Call       = false;
    */

        mIsLockAllCall
            = LGProvider.Lock.getCallAll(mCr);
    
        mIsLockInternationalCall
            = LGProvider.Lock.getCallNational(mCr);
        
        mIsLock700Call
            = LGProvider.Lock.getCall700(mCr);
        
        mIsLock060Call
            = LGProvider.Lock.getCall060(mCr);
/* jaeyoung.ha@lge.com 2010.10.12 070 -> OZ070 changed : Dialer 俊辑 贸府
//START youngmi.uhm@lge.com 2010. 8. 05. LAB1_CallUI OZ070 ( settings request )		
		final String m_serviceProvider	= android.os.SystemProperties.get("ro.telephony.service_provider", "null");
				
		if ("LGT".equals(m_serviceProvider)){
        	mIsLockOZ070Call
            	= LGProvider.Lock.getCallOZ070(mCr);
		}
//END youngmi.uhm@lge.com2010. 8. 04. LAB1_CallUI OZ070 ( settings request )
*/
    }
        


    public boolean isSendingLock(String phoneNumber) {

        if(TextUtils.isEmpty(phoneNumber)) return false;
        

        String tmpNum       = null;
        String tmpPhoneNum  = null;

        //strcpy( tmpNum, pPhoneNum );
        //
        //VoiceCallLib_RemoveHyphen( tmpNum );

        tmpNum = removeHyphen(phoneNumber);



        if ( PhoneNumberUtils.isEmergencyNumber(tmpNum) /*|| System_IsUserEmergencyCall( tmpNum ) */)
        {
            return false;
        }


        if(tmpNum.length() > 4 
            && ( tmpNum.startsWith(PREFIX_1)
                || tmpNum.startsWith(PREFIX_2)
                || tmpNum.startsWith(PREFIX_3))
                )
        {
            tmpPhoneNum = tmpNum.substring(4);
        }
        else
        {
            tmpPhoneNum = tmpNum;
        }

        //LGE_CHANGED_E - 2010.02.02 yongjing.her@lge.com - Moved To Constructor
        //readSettingValue(); //read Setting values



        // 1. Lock All Call ---------------------------------------------------------------------------
        if( mIsLockAllCall )
            return true;

        // 2. Lock International Call ----------------------------------------------------------------------------
        if  ( mIsLockInternationalCall )
        {
            
            TelephonyManager telephonyManager = (TelephonyManager)PhoneApp.getInstance().getSystemService(Context.TELEPHONY_SERVICE);

        
            //Add a condition about the international phone_number start with '+' (exception : in the domestic network)
            if( ((tmpPhoneNum.startsWith(STR_00)) && (tmpPhoneNum.charAt(2)!=CHAR_0) && (tmpPhoneNum.charAt(2)!=CHAR_SLASH_0) )
                //only WCDMA
                || ( (telephonyManager.getPhoneType()==TelephonyManager.PHONE_TYPE_GSM) 
                        && (tmpPhoneNum.startsWith(STR_PLUS_82) && (tmpPhoneNum.charAt(0)==CHAR_PLUS))
                        )
            )
            {
                return true;
            }
        }

        
        // 3. Lock 700 Call-----------------------------------------------------------------------------
        if  ( mIsLock700Call )
        {
            int i , length_tmpPhoneNum ;
            
            for( i = 0, length_tmpPhoneNum = tmpPhoneNum.length() ; i< length_tmpPhoneNum; i++)
            {
                if ( (tmpPhoneNum.charAt(i) == CHAR_STAR) || (tmpPhoneNum.charAt(i) == CHAR_SHAP) )
                {
                    return false;
                }
                else if ( (tmpPhoneNum.charAt(i)== CHAR_P) || (tmpPhoneNum.charAt(i)== CHAR_W) )
                {

                    tmpPhoneNum = tmpPhoneNum.substring(0, i+1 );
                    break;
                }
            }
    
            if  (   
                    ( tmpPhoneNum.charAt(0)== CHAR_0 && tmpPhoneNum.charAt(1)>= CHAR_3 && tmpPhoneNum.charAt(1)<= CHAR_6 &&
                       tmpPhoneNum.startsWith(STR_700,3) && tmpPhoneNum.length()== 10 ) ||
                    ( tmpPhoneNum.startsWith(STR_02700)  && tmpPhoneNum.length()==  9 ) ||      //case : in Seoul case,  compare directrly
                    ( tmpPhoneNum.startsWith(STR_700)    && tmpPhoneNum.length()==  7 ) ||      //case : input locale number + 700,
                    ( tmpPhoneNum.startsWith(STR_011700) && tmpPhoneNum.length()== 10 ) ||      //case :  business +  700
                    ( tmpPhoneNum.startsWith(STR_016700) && tmpPhoneNum.length()== 10 ) ||
                    ( tmpPhoneNum.startsWith(STR_017700) && tmpPhoneNum.length()== 10 ) ||
                    ( tmpPhoneNum.startsWith(STR_018700) && tmpPhoneNum.length()== 10 ) ||
                    ( tmpPhoneNum.startsWith(STR_019700) && tmpPhoneNum.length()== 10 ) ||
                    ( tmpPhoneNum.startsWith(STR_010700) && tmpPhoneNum.length()== 10 )
                )
            {
                return true;
            }
        }



        //4. Lock 060 Call------------------------------------------------------------------------------
        if  ( mIsLock060Call)
        {
            if ( tmpPhoneNum.startsWith(STR_060))
                return true;
        }
/* jaeyoung.ha@lge.com 2010.10.12 070 -> OZ070 changed : Dialer 俊辑 贸府
//START youngmi.uhm@lge.com 2010. 8. 05. LAB1_CallUI OZ070 ( settings request )
		//5. Lock OZ070 Call------------------------------------------------------------------------------
		final String m_serviceProvider	= android.os.SystemProperties.get("ro.telephony.service_provider", "null");
				
		if ("LGT".equals(m_serviceProvider)){
	        if  ( mIsLockOZ070Call)
	        {
	            if ( tmpPhoneNum.startsWith(STR_OZ070))
	                return true;
	        }
		}
 //END youngmi.uhm@lge.com2010. 8. 04. LAB1_CallUI OZ070 ( settings request )       
 */

        //THE END
        return false;

    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}
