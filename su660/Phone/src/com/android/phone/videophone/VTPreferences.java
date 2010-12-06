package com.android.phone.videophone;

import com.android.phone.PhoneApp;

import android.os.SystemProperties;
import android.util.Log;

public class VTPreferences {
	private static final String LOG_TAG = "VTPreferences";	
	private static final boolean DBG =
		(PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

	/** Video Telephony Mode */	
	public static final int NETWORK_CSVT = 0;
	public static final int NETWORK_PSVT = 1;
    public static final int NETWORK_DUMMY = 2; /* For dummy NXP lib */

    
	/** Network mode CSVT(UMTS) or PSVT (CDMA)*/ 	
	public static int NETWORK_MODE = NETWORK_CSVT;

	/** Sensor List */	
	public static int SENSOR_ID_FRONT_PORTRAIT;
	public static int SENSOR_ID_BACK_PORTRAIT;
	public static int SENSOR_ID_FRONT_LANDSCAPE;
	public static int SENSOR_ID_BACK_LANDSCAPE;	
	/** Default Sensor */
	public static int SENSOR_ID_DEFAULT = SENSOR_ID_FRONT_PORTRAIT;

	/** Encoder Option */
	public static boolean ENCODER_FLIP_FRONT_PORTRAIT;
	public static boolean ENCODER_FLIP_FRONT_LANDSCAPE;

       /** FrameRate*/
	public static final int[] VIDEO_FRAME_RATE = new int[]{25, 50, 75, 100, 125, 150};	
       
       /** Encoding bitrate */
	public static final int[] VIDEO_ENCODING_BITRATE = new int[]{20000, 24000, 28000, 40000, 48000, 52000,56000};

       /** I-period */
	public static final int[] INTRA_FRAME_INTERVAL = new int[]{2, 4, 6, 8, 10};
       
	/** Default Value Setting */
	public static int video_frame_rate;
	public static int intra_frame_interval;  
       public static int video_encoding_bitrate;         
       public static int audio_encoding_bitrate;
       
	public static boolean ENABLE_DEBLOCKING_FILTER;
	public static boolean ENABLE_DTX;

	/** Codec Type */
	public static final int CODEC_MPEG4_PREFERED = 0;	
	public static final int CODEC_H263_PREFERED = 1;
	public static final int CODEC_MPEG4_ONLY = 2;
	public static final int CODEC_H263_ONLY = 3;		
    
	/** Default Codec */
	public static int CODEC_DEFAULT;
      
	/** Vendor ID */
    public static final int VENDOR_ID_TYPE_OBJECT_ID      = 0x00;
    public static final int VENDOR_ID_TYPE_NONSTANDARD    = 0x01;
    public static int VENDOR_ID_TYPE = VENDOR_ID_TYPE_NONSTANDARD;

    /** VendorID for OID type */
    public static String VENDOR_ID_OBJECT_ID;

    /** VendorID for h221 type */    
    public static byte VENDOR_ID_T35CountryCode=0;
    public static byte VENDOR_ID_T35Extension=0;
    public static int VENDOR_ID_ManufacturerCode=0;
    
    /** VendorID */
	public static String VENDOR_ID_PRODUCT_NUM;
	public static String VENDOR_ID_PRODUCT_VER;


    public static final int UII_BUFFER_SIZE = 512;
    
	public static final String OPERATOR = System.getProperty("user.operator", "unknown");
	public static final String MODEL_NAME = SystemProperties.get("ro.product.model","unknown");
    public static final String VERSION = SystemProperties.get("ro.build.lge.version.release", "unknown");

	static {

		// ---------------------------------------------------
		// Common
		// ---------------------------------------------------
		/** Network mode CSVT(UMTS) or PSVT (CDMA)*/ 
		NETWORK_MODE = NETWORK_CSVT;

        SENSOR_ID_FRONT_PORTRAIT = 2;
        SENSOR_ID_BACK_PORTRAIT = 3;
        SENSOR_ID_FRONT_LANDSCAPE = 4;
        SENSOR_ID_BACK_LANDSCAPE = 5;
        
		/** Default Sensor */
		SENSOR_ID_DEFAULT = SENSOR_ID_FRONT_PORTRAIT;

		/** Encoder Option */
		ENCODER_FLIP_FRONT_PORTRAIT = false;
		ENCODER_FLIP_FRONT_LANDSCAPE = false;

		/** Codec Option */
		//VIDEO_FRAME_RATE = 10;
		//INTRA_FRAME_INTERVAL = 2;
		ENABLE_DEBLOCKING_FILTER = true;
		ENABLE_DTX = true;

		/** Codec Type */		
		//CODEC_DEFAULT = CODEC_H263_ONLY;

		/** Vendor ID */
        VENDOR_ID_TYPE = VENDOR_ID_TYPE_NONSTANDARD;
        if (VENDOR_ID_TYPE == VENDOR_ID_TYPE_NONSTANDARD)
        {
            VENDOR_ID_T35CountryCode=0;
            VENDOR_ID_T35Extension=0;
            VENDOR_ID_ManufacturerCode=0;
        }
        else
        {
            VENDOR_ID_OBJECT_ID="0.0.0";
        }
		
		VENDOR_ID_PRODUCT_NUM = "LGE-VideoTelephony";
		VENDOR_ID_PRODUCT_VER = "1.3.0";

		if (DBG) Log.i(LOG_TAG, "VT Preference Operator ["+ OPERATOR
				+ "] Model [" + MODEL_NAME + "]");

		// ---------------------------------------------------
		// Operator
		// ---------------------------------------------------		
		if (OPERATOR.equals("SKT"))
		{
			NETWORK_MODE = NETWORK_CSVT;
          
            VENDOR_ID_TYPE   = VENDOR_ID_TYPE_NONSTANDARD;
            VENDOR_ID_T35CountryCode=(byte) 0x61;
            VENDOR_ID_T35Extension=0;
            VENDOR_ID_ManufacturerCode=0;
           
			VENDOR_ID_PRODUCT_NUM = MODEL_NAME;

            // skt control number is zero
            //  Bit (LSB --> MSB)
            //  0 : SFS         미지원               
            //  1 : VTC        채팅 미지원 
            //  2 : SFCv2    영상컬러링 미지원
            //  3 : VT noti   영상통화 알림 미지원 
            //  4 : mode control 모드제어 미지원 
            //  5 : recording control 미지원 
            //  6 : touchYou 미지원 
			VENDOR_ID_PRODUCT_VER = "SKT 0 LG-"+ VERSION;
		}
		else if (OPERATOR.equals("KTF"))
		{
			NETWORK_MODE = NETWORK_CSVT;

                    VENDOR_ID_TYPE = VENDOR_ID_TYPE_NONSTANDARD;
                    VENDOR_ID_T35CountryCode=(byte) 0x61;
            VENDOR_ID_T35Extension=(byte) 0x16;
            VENDOR_ID_ManufacturerCode=0x8000;

            VENDOR_ID_PRODUCT_NUM = "KTF-VT_Phone";
            VENDOR_ID_PRODUCT_VER = "2.4.0";            
		}
		else if (OPERATOR.equals("LGT"))
		{
			NETWORK_MODE = NETWORK_PSVT;	
		}		


		// ---------------------------------------------------
		// Model
		// ---------------------------------------------------	
		if (MODEL_NAME.equals("LG-P990") || MODEL_NAME.equals("LG-SU660") )
		{
			SENSOR_ID_FRONT_PORTRAIT = 2;
			SENSOR_ID_BACK_PORTRAIT = 3;
			SENSOR_ID_FRONT_LANDSCAPE = 4;
			SENSOR_ID_BACK_LANDSCAPE = 5;

			/** Default Sensor */
			SENSOR_ID_DEFAULT = SENSOR_ID_FRONT_PORTRAIT;

			/** Encoder Option */
			ENCODER_FLIP_FRONT_PORTRAIT = false;
		}


        //@ Enable following code if you want remove NXP lib 
//@        NETWORK_MODE = NETWORK_DUMMY;
        //@ end 
	}	
}
