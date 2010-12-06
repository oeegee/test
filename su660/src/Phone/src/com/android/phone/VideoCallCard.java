/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;


//NXP_VT////////////////////////////////////////////////////////////////////////////
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.pim.ContactsAsyncHelper;
import android.provider.Settings;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.phone.videophone.VTAppStateManager;
import com.android.phone.videophone.VTEventHandler;
import com.android.phone.videophone.VTObserable;
import com.android.phone.videophone.VTObserver;
import com.android.phone.videophone.VTPreferences;
import com.android.phone.videophone.VideoTelephonyApp;
import com.android.phone.videophone.VTCallStateInterface.VTCallState;

/**
 * "Call card" UI element: the in-videocall screen contains a tiled layout of videocall
 * cards, each representing the state of a current "videocall" (ie. an active videocall,
 * a videocall on hold, or an incoming videocall.)
 */
public class VideoCallCard extends FrameLayout
        implements CallTime.OnTickListener, CallerInfoAsyncQuery.OnQueryCompleteListener,
                   ContactsAsyncHelper.OnImageLoadCompleteListener, View.OnClickListener, 
                   SurfaceHolder.Callback,VTObserable{

    private static final String LOG_TAG = "VideoCallCard";
    private static final boolean DBG = 
        (SystemProperties.getInt("ro.debuggable", 0) == 1);	
    private static final boolean VT_WORK = false;
	
     
    /**
     * Reference to the InCallScreen activity that owns us.  This may be
     * null if we haven't been initialized yet *or* after the InCallScreen
     * activity has been destroyed.
     */
    public InCallScreen mInCallScreen;

    // Phone app instance
    private PhoneApp mApplication;

    private ViewGroup mCallInfo;
	private ViewGroup mOutgoingCallInfo;
	private LinearLayout mVideoOutgoingLayout;
	//private RelativeLayout mVideoLalyout;
	private LinearLayout mRemoteLayout, mCameraLayout,mRemoteStubContainer, mCameraStubContainer;
//	private FrameLayout mRemoteFrameLayout;//, mCameraFrameLayout;
	private ImageView mRemoteBackground;
	
	// LGE_VT_DIALPAD
	private ViewGroup mVTDialpad;
	private boolean mIsDialpad;

    private TextView mUpperTitle;


    // Text colors, used for various labels / titles
    private int mTextColorDefaultPrimary;
    private int mTextColorDefaultSecondary;
    private int mTextColorConnected;
    private int mTextColorConnectedBluetooth;
    private int mTextColorEnded;
    private int mTextColorOnHold;
    
    //NXP_VT////////////////////////////////////////////////////////////////////////////
    //public static boolean wasAvatarInProgress = false;
	public static boolean wasStreamingInProgress = false;
	public static int featureToStart;
	
	//SurfaceView and Hodler
	////////////////////////////////////////////////////
	public SurfaceView mNearview,mFarview;
	private SurfaceHolder holderNearview,holderFarview;

	//Substitute Image and Hold function
	////////////////////////////////////////////////////
	//Whether Near-substitute is top or bottom
	private boolean mIsNearTop = false;
    //Occured timing issue. The problem that camera surfaceview positions to down at the disconnected.
	//so, cache data at the onFinishInflated().
	private int mCameraview_width_height[] = new int[2];
	private boolean mIsNeedChangeView = false;
	
	private static boolean mIsAvatarStart = false;
	
	/**
	 * Whether a position of near(camera) surfaceview is top or down in the case of the dialing call-state.
	 * */
	private final static boolean IN_VT_SCENARIO = true; 
      
	public static final String subsImagePath = "/system/media/image/vt/default.jpg";
	public static final String holdImagePath = "/system/media/image/vt/vt_in_call_hold_small.jpg";
	public static String sendSubsImagePath;

	public static boolean audioMuted = false;
	public static boolean avatarMovStart = false; 	

	boolean mPreviewRunning = false;
	boolean mPrivateRunning = false;
	boolean mSwapMode = false;

	// The main block of info about the "primary" or "active" videocall,
	// including photo / name / phone number / etc.
	private ImageView mPhoto;
	private ImageView mVTIcon;


	// Info about the "secondary" videocall, which is the "videocall on hold" when
	// two lines are in use.

	// Menu button hint
	private TextView mMenuButtonHint;

	//Outgoing Call: elapse time, name, phone number 
	///////////////////////////////////////////////////
	private TextView mOutElapsedTime,mOutName,mOutPhoneNumber;

	//Incoming Call : name, phone number, label, common social status
	///////////////////////////////////////////////////
	private TextView mCommonName,mCommonPhoneNumber,mCommonLabel,mCommonSocialStatus;

       public TextView mCommonFMC;
	private TextView mDiscElapsedTime;

    // Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [START_LGE_LAB]    
      private TextView mNameSecond;
      private TextView mPhoneNumberSecond;
    // Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [END_LGE_LAB] 

	/** VT Event handler for menu */    
	protected VTEventHandler mEventHandler = null;

	/** Number of buttone in video call  */
	public static final int INCALL_BUTTON_COUNT = 6;    

	/**Incall button*/
	protected Button mIncallButton[] = new Button[INCALL_BUTTON_COUNT];

	/**Incall button*/
	public static final int BUTTON_PRIVATE_SHOW = 0;
	public static final int BUTTON_END 	   = 1;
	public static final int BUTTON_DIALPAD = 2;
	public static final int BUTTON_HOLD = 3;
	public static final int BUTTON_MUTE	   = 4;
	public static final int BUTTON_SPEAKER = 5;   

    //LGE_VT_CAPTURE_START
    protected VTEventHandler mCaptureEventHandler = null;       
    private RelativeLayout mVTControlContainer;
    private View  mCaptureView = null;

    public static final int CAPTURE_BUTTON_COUNT = 3;
    protected Button mCaptureButton[] = new Button[CAPTURE_BUTTON_COUNT];
	public static final int CAPTURE_VT_0 = 0;       
	public static final int CAPTURE_VT_1 = 1;   
	public static final int CAPTURE_VT_2 = 2; 
    
    private static int mCount = 0;
    private Timer capturetimer = null;
       private Timer canceltimer = null;
       Handler CaptureHandler = null;
       public static String[] captureloadingImagePath; 
    private boolean isCaptureloading = false;
    public static boolean isEnableButton[] = new boolean[INCALL_BUTTON_COUNT];
       private boolean preSendsubstitute = false;

	private static final int NEAR_CAPTURE = 1;
	private static final int FAR_CAPTURE = 2;
	private boolean mFarEnd = false;
	private int capture_result;
	private boolean mIsCaptureMenu = false;    
   
	private static final int CAPTURE_MSG_SUCCESS = 1;
	private static final int CAPTURE_MSG_FAIL = 2;
	private static final int CAPTURE_MSG_NO_SD_CARD = 3;
	private static final int CAPTURE_MSG_SD_CARD_NA = 4;
	private static final int CAPTURE_MSG_CANNT_JPG = 5;
       private static final int CAPTURE_MSG_ENCRYPT_FAIL = 6;
       private static final int CAPTURE_MSG_MEMORY_FULL = 7;
       
       //LGE_VT_CAPTURE_END
       
	// LGE_VT_DIALPAD start
	protected VTEventHandler mDialpadEventHandler = null;

	public static final int DIALPAD_BUTTON_COUNT = 12;

	protected ImageButton mDialpadButton[] = new ImageButton[DIALPAD_BUTTON_COUNT];

	public static final int BUTTON_VT_0 = 0;       
	public static final int BUTTON_VT_1 = 1;   
	public static final int BUTTON_VT_2 = 2;     
	public static final int BUTTON_VT_3 = 3;     
	public static final int BUTTON_VT_4 = 4;     
	public static final int BUTTON_VT_5 = 5;     
	public static final int BUTTON_VT_6 = 6;     
	public static final int BUTTON_VT_7 = 7;     
	public static final int BUTTON_VT_8 = 8;     
	public static final int BUTTON_VT_9 = 9;     
	public static final int BUTTON_VT_STAR = 10;     
	public static final int BUTTON_VT_POUND = 11;     
	// LGE_VT_DIALPAD end



	/**VTObserable*/
	private ArrayList<VTObserver> mObservers = null;
	private Iterator<VTObserver> mIterator = null;

	private Context mContext = null;

	/**VideoTelephonyApp - wrapper를 이용한 통합관리 */
	private VideoTelephonyApp mVTApp = null;  

    private  String mSaveName = null;
    private  String mSaveDisplayNumber = null;
    private ImageView mLarge_small_imageview[] = new ImageView[3];
  
  
    // Onscreen hint for the incoming videocall RotarySelector widget.
    private int mRotarySelectorHintTextResId;
    private int mRotarySelectorHintColorResId;

    private CallTime mCallTime;

    // Track the state for the photo.
    private ContactsAsyncHelper.ImageTracker mPhotoTracker;

    // Cached DisplayMetrics density.
    private float mDensity;

    private boolean mHasNameInContact = false;
    
    // the state of between DISCONNECTED event and surfaceDestroyed call-back    
    private static final int STATE_SURFACEVIEW_CREATED = 0;    
    private static final int STATE_SURFACEVIEW_DESTROYED = 1;
    private static final int STATE_SURFACEVIEW_ADDED = 2;
    private static final int STATE_COMES_DISCONNECTED = 3;


    // cnap & phname Toggle
    private static final int TOGGLE_DISPLAY_EVENT=100;
    private static final int TOGGLE_DISPLAY_TIME = 3000;  // 3 sec (same as feature phone)

    private static Call mCall = null;
    private static CallerInfo mCallerInfo = null;

    private static boolean mIsToggling = false;    // during toggling
    private static boolean mToggleValue = false; // cnap and pbname toggle
    private static TextView mNameDisplay = null;
    private static String mCnapName = null;
    private static String mPbName = null;
    
    private boolean canRemoveSurfaceview = false;
    
    private Handler mSurfaceHandler = new Handler(){
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case STATE_SURFACEVIEW_CREATED:
                case STATE_SURFACEVIEW_ADDED:
                {
                    log( "===STATE_SURFACEVIEW = " + msg.what);
                    canRemoveSurfaceview = false;
                }
                    break;
                case STATE_SURFACEVIEW_DESTROYED:
                {
                    log( "===STATE_SURFACEVIEW_DESTROYED! " + msg.what);
                    canRemoveSurfaceview = true;                    
                    sendMessage(obtainMessage(STATE_COMES_DISCONNECTED));
                }
                    break;
                case STATE_COMES_DISCONNECTED:
                {
                    log( "===STATE_COMES_DISCONNECTED, canRemoveSurfaceview = " + canRemoveSurfaceview);
                    if(canRemoveSurfaceview)
                    {                        
                        //VTCallState state = VTAppStateManager.getInstance().getCallState();
                        
                        if(VTAppStateManager.getInstance().isCanRemoveSurface())  
                        {
                            log( "removeSurfaceviews()");
                            
                            //remote the surfaceview container
                            mRemoteLayout.removeView(mRemoteStubContainer);
                            mCameraLayout.removeView(mCameraStubContainer);
//                          mNearview = null;
//                          mFarview = null;
                        }
                    }
                }
                    break;  
            }
        }
    };
    

    static Handler mToggleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TOGGLE_DISPLAY_EVENT:
                    displayToggling();
                    break;
            }
        }
    };
    
    /** When we swap both near and far surfaceview, we have a flushed problem at a remote Surfaceview.
     * i think the cause is timimg issue. so we used this handler 
     * by hgkim 10/12
     * */
	private Handler mSwapHandler = new Handler(){
		public void handleMessage(Message msg)
		{
			//if callstate is dialing , then return.
			VTCallState callState = 
				VTAppStateManager.getInstance().getCallState();
			
			if(callState == VTCallState.DIALING || callState == VTCallState.ALERTING )
			{
			    log( "mSwapHandler, callState = " + callState + ",so return");
				return;
			}
			
			swapView();

			setVisibilityImageView();			
		}
	};
    
	private ProgressBar mReadyProgressBar = null;
	//After DISCONNECTED call-state, It comes ACTIVE call-state. 
	//so, add flag for exception
//	private boolean mIsDisconnected = false;
	
     public VideoCallCard(Context context, AttributeSet attrs) {
        super(context, attrs);

//        int densityDPI = getResources().getDisplayMetrics().densityDpi;
//        Log.i("ABC","- densityDPI: " + densityDPI);
        
//        int qemu_DPI = SystemProperties.getInt("qemu.sf.lcd_density",0);
//        
//        int ro_sf_DPI = SystemProperties.getInt("ro.sf.lcd_density", 0);
//        Log.i("ABC","- qemu_DPI: " + qemu_DPI);
//        Log.i("ABC","- ro_sf_DPI: " + ro_sf_DPI);
        
        
        if (DBG) log("VideoCallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);

        // Inflate the contents of this VideoCallCard, and add it (to ourself) as a child.
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(
                R.layout.videocall_card,  // resource
                this,                // root
                true);
                
        mApplication = PhoneApp.getInstance();

        mCallTime = new CallTime(this);

        // create a new object to track the state for the photo.
        mPhotoTracker = new ContactsAsyncHelper.ImageTracker();

        mDensity = getResources().getDisplayMetrics().density;        
        if (DBG) log("- Density: " + mDensity);

        
        /**@Fixed me - VT 변경사항 적용 by hgkim 07/23*/
        mContext = context;
        
        //for VTObserable
        mObservers = new ArrayList<VTObserver>();
        
        //for VideoTelephonyApp
        mVTApp = VideoTelephonyApp.getInstance(mContext);
        mVTApp.setPhone(mApplication.getPhone());
    }

    void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    public void onTickForCallTimeElapsed(long timeElapsed) {
        // While a videocall is in progress, update the elapsed time shown
        // onscreen.
        updateElapsedTimeWidget(timeElapsed);
    }

    /* package */
    void stopTimer() {
        mCallTime.cancelTimer();
    }

    //@Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG)
            log("VideoCallCard onFinishInflate(this = " + this + ")...");

        // NXP_VT////////////////////////////////////////////////////////////////////////////
        if(mCameraStubContainer == null)
        {
            ViewStub viewStub = (ViewStub) findViewById(R.id.camera_stub);
            if (viewStub != null) {
                ((ViewStub) findViewById(R.id.camera_stub)).inflate();
                mNearview = (SurfaceView) findViewById(R.id.vt_camera);
            }            
            
            //the really container includes camera-surfaceview
            mCameraStubContainer = (LinearLayout)findViewById(R.id.camera_stub_container);
        }

        // mNearview = (SurfaceView) findViewById(R.id.vt_camera);
        holderNearview = mNearview.getHolder();
        holderNearview.addCallback(this);
        holderNearview.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holderNearview.setSizeFromLayout(); // jhlee
        mNearview.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
				if (isDialpad())
				{
					log("Farview onClick => Dialpad state");
					return;
				}
				
                log("VideoCallCard Nearview onClick");

                if(false == VTAppStateManager.getInstance().isRecvFirstFrame())
                    return;
              
                if (mIsAvatarStart) {
                    if(mIsNearTop == false){
                        mLarge_small_imageview[0].setVisibility(View.VISIBLE);
                    } else {
                        mLarge_small_imageview[1].setVisibility(View.VISIBLE);
                    }

                    mSwapHandler.sendEmptyMessageDelayed(0, 100);
                } else {
                    swapView();
                }
            }
        });

        if(mRemoteStubContainer == null)
        {
            ViewStub viewStub = (ViewStub) findViewById(R.id.remote_stub);
            if (viewStub != null) {
                ((ViewStub) findViewById(R.id.remote_stub)).inflate();
                mFarview = (SurfaceView) findViewById(R.id.vt_remote);
            }
            //the really container includes camera-surfaceview
            mRemoteStubContainer = (LinearLayout)findViewById(R.id.remote_stub_container);
        }

//        mFarview = (SurfaceView) findViewById(R.id.vt_remote);
        holderFarview = mFarview.getHolder();
        holderFarview.addCallback(this);
        holderFarview.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holderFarview.setSizeFromLayout();
        mFarview.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
				if (isDialpad())
				{
					log("Farview onClick => Dialpad state");
					return;
				}
				
                log("VideoCallCard Farview onClick");

                if(false == VTAppStateManager.getInstance().isRecvFirstFrame())
                    return;
                
                if (mIsAvatarStart) {
                    if(mIsNearTop == false){
                        mLarge_small_imageview[0].setVisibility(View.VISIBLE);
                    } else {
                        mLarge_small_imageview[1].setVisibility(View.VISIBLE);
                    }

                    mSwapHandler.sendEmptyMessageDelayed(0, 100);
                } else {
                    swapView();
                }
            }
        });

        mCallInfo = (ViewGroup) findViewById(R.id.VideoCallInfo);
        mOutgoingCallInfo = (ViewGroup) findViewById(R.id.outgoingVideoCallInfo);
        mVideoOutgoingLayout = (LinearLayout) mOutgoingCallInfo
                .findViewById(R.id.videocallCardOutgoingInfo);
        // mVideoLalyout =
        // (RelativeLayout)mOutgoingCallInfo.findViewById(R.id.RelativeLayout01);
        mRemoteLayout = (LinearLayout) mOutgoingCallInfo.findViewById(R.id.remoteLayout);
        mCameraLayout = (LinearLayout) mOutgoingCallInfo.findViewById(R.id.cameraLayout);
        mRemoteBackground = (ImageView) mOutgoingCallInfo.findViewById(R.id.remoteBackground);
        
        //Occured timing issue. The problem that camera surfaceview positions to down at the disconnected.  
        android.view.ViewGroup.MarginLayoutParams pre;              
        pre = (MarginLayoutParams)mCameraLayout.getLayoutParams();
        mCameraview_width_height[0] = pre.width;
        mCameraview_width_height[1] = pre.height;

	// LGE_VT_DIALPAD
        mVTDialpad = (ViewGroup) findViewById(R.id.vt_dialpad);
        setDialpad(false);

        // "Upper" and "lower" title widgets
        mUpperTitle = (TextView) mCallInfo.findViewById(R.id.upperTitle);
        // mOutUpperTitle = (TextView)
        // mOutgoingCallInfo.findViewById(R.id.upperTitle);
        // Text colors
        mTextColorDefaultPrimary = // corresponds to textAppearanceLarge
        getResources().getColor(android.R.color.primary_text_dark);
        
        mTextColorDefaultSecondary =  // corresponds to textAppearanceSmall
                getResources().getColor(android.R.color.secondary_text_dark);
                
        mTextColorConnected = getResources().getColor(R.color.incall_textConnected);
        mTextColorConnectedBluetooth = getResources().getColor(
                R.color.incall_textConnectedBluetooth);
        mTextColorEnded = getResources().getColor(R.color.incall_textEnded);
        mTextColorOnHold = getResources().getColor(R.color.incall_textOnHold);


        // "Caller info" area, including photo / name / phone numbers / etc
        mPhoto = (ImageView)  mCallInfo.findViewById(R.id.photo);
        mVTIcon = (ImageView) mCallInfo.findViewById(R.id.vtIcon); 

        // Menu Button hint
        mMenuButtonHint = (TextView) findViewById(R.id.menuButtonHint);
        
        /**@Fixed me - VT elaspe time, name, phone number  by hgkim 07/23*/
        mOutElapsedTime = (TextView)mOutgoingCallInfo.findViewById(R.id.elapsedTime);
        mOutName = (TextView)mOutgoingCallInfo.findViewById(R.id.name);
        mOutPhoneNumber = (TextView)mOutgoingCallInfo.findViewById(R.id.phoneNumber);
        

        mCommonName = (TextView)mCallInfo.findViewById(R.id.name);
        mCommonPhoneNumber = (TextView)mCallInfo.findViewById(R.id.phoneNumber);
        mCommonLabel = (TextView)mCallInfo.findViewById(R.id.label);
        mCommonSocialStatus = (TextView)mCallInfo.findViewById(R.id.socialStatus);

        // Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [START_LGE_LAB] 
        if (System.getProperty("user.country", "unknown").equals("KR"))
        {
        	mNameSecond = (TextView) findViewById(R.id.nameSecond);
        	mPhoneNumberSecond = (TextView) findViewById(R.id.phoneNumberSecond);
	}
        // Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [END_LGE_LAB] 

        if(VTPreferences.OPERATOR.equals("SKT"))
            mCommonFMC = (TextView)mCallInfo.findViewById(R.id.inCallFMC);
        
        mDiscElapsedTime = (TextView)mCallInfo.findViewById(R.id.elapsedTime);

        mVTControlContainer = (RelativeLayout) findViewById(R.id.controlContainer);
                     
        if(mEventHandler == null)
        {
        	mEventHandler = new VTEventHandler.IncallBL(mContext,this);	
        }
        
        mIncallButton[BUTTON_PRIVATE_SHOW] = 
        	(Button) mOutgoingCallInfo.findViewById(R.id.private_button); 
        mIncallButton[BUTTON_END] = 
        	(Button) mOutgoingCallInfo.findViewById(R.id.end_button); 
        mIncallButton[BUTTON_DIALPAD] = 
        	(Button) mOutgoingCallInfo.findViewById(R.id.dialpad_button); 
        mIncallButton[BUTTON_HOLD] = 
        	(Button) mOutgoingCallInfo.findViewById(R.id.hold_button); 
        mIncallButton[BUTTON_MUTE] = 
        	(Button) mOutgoingCallInfo.findViewById(R.id.mute_button); 
        mIncallButton[BUTTON_SPEAKER] = 
        	(Button) mOutgoingCallInfo.findViewById(R.id.speaker_button); 
        
        //add handler & init button state
        initFlags();
        buttonInitialize();

        // LGE_VT_DIALPAD start
        if(mDialpadEventHandler == null)
        {
        	mDialpadEventHandler = new VTEventHandler.DialpadBL(mContext,this);	
        }      

        mDialpadButton[BUTTON_VT_0] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_zero);        
        mDialpadButton[BUTTON_VT_1] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_one); 
        mDialpadButton[BUTTON_VT_2] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_two); 
        mDialpadButton[BUTTON_VT_3] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_three); 
        mDialpadButton[BUTTON_VT_4] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_four); 
        mDialpadButton[BUTTON_VT_5] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_five); 
        mDialpadButton[BUTTON_VT_6] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_six); 
        mDialpadButton[BUTTON_VT_7] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_seven); 
        mDialpadButton[BUTTON_VT_8] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_eight); 
        mDialpadButton[BUTTON_VT_9] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_nine); 
        mDialpadButton[BUTTON_VT_STAR] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_star); 
        mDialpadButton[BUTTON_VT_POUND] = 
        	(ImageButton) mVTDialpad.findViewById(R.id.vt_pound); 
        
        for(int i=0; i<DIALPAD_BUTTON_COUNT;i++){
        	mDialpadButton[i].setOnClickListener(mDialpadEventHandler);
        	mDialpadButton[i].setEnabled(true);
        }   
        // LGE_VT_DIALPAD end      

        captureloadingImagePath = new String[]{"/system/media/image/vt/capture_loading1.jpg",
                                                  "/system/media/image/vt/capture_loading2.jpg",
                                                  "/system/media/image/vt/capture_loading3.jpg",
                                                  "/system/media/image/vt/capture_loading4.jpg",
                                                  "/system/media/image/vt/capture_loading5.jpg",
                                                  "/system/media/image/vt/capture_loading6.jpg"};
       
    }

    /**
     * Updates the state of all UI elements on the VideoCallCard, based on the
     * current state of the phone.
     */
    void updateState(Phone phone) {

        // No UI Update, if call state is DISCONNECTING
        if (phone.getForegroundCall().getState() == Call.State.DISCONNECTING)
        {
            if (DBG) log("- updateScreen: Foregroundcall state is DISCONNECTING no updating UI...");
            return;
        }

        if (phone.getRingingCall().getState() == Call.State.DISCONNECTING)
        {
            if (DBG) log("- updateScreen: Ringingcall state is DISCONNECTING no updating UI...");
            return;
        }

            
        // Update some internal state based on the current state of the phone.

        // TODO: clean up this method to just fully update EVERYTHING in
        // the callcard based on the current phone state: set the overall
        // type of the VideoCallCard, load up the main caller info area, and
        // load up and show or hide the "other videocall" area if necessary.

        Phone.State state = phone.getState();  // IDLE, RINGING, or OFFHOOK

        if (state == Phone.State.RINGING) {
            // A phone videocall is ringing *or* videocall waiting
            // (ie. another call may also be active as well.)
            updateRingingVideoCall(phone);
        } else if (state == Phone.State.OFFHOOK) {
            // The phone is off hook. At least one videocall exists that is
            // dialing, active, or holding, and no calls are ringing or waiting.
            updateForegroundCall(phone);
        } else {
            // The phone state is IDLE!
            //
            // The most common reason for this is if a videocall just
            // ended: the phone will be idle, but we *will* still
            // have a videocall in the DISCONNECTED state:
            Call fgCall = phone.getForegroundCall();
            Call bgCall = phone.getBackgroundCall();
            if(fgCall.getState().isDialing()){ //More_Fast_Dialing
                 log("[VideoCallCard-updateState] fgCall.getState().isDialing() : updateForegroundCall()...");
                 updateForegroundCall(phone);
            }else			
            if ((fgCall.getState() == Call.State.DISCONNECTED)
                || (bgCall.getState() == Call.State.DISCONNECTED)) {
                // In this case, we want the main VideoCallCard to display
                // the "Call ended" state.  The normal "foreground call"
                // code path handles that.
                updateForegroundCall(phone);
            } else 
            {
                // We don't have any DISCONNECTED calls, which means
                // that the phone is *truly* idle.
                //
                // It's very rare to be on the InCallScreen at all in this
                // state, but it can happen in some cases:
                // - A stray onPhoneStateChanged() event came in to the
                //   InCallScreen *after* it was dismissed.
                // - We're allowed to be on the InCallScreen because
                //   an MMI or USSD is running, but there's no actual "call"
                //   to display.
                // - We're displaying an error dialog to the user
                //   (explaining why the call failed), so we need to stay on
                //   the InCallScreen so that the dialog will be visible.
                //
                // In these cases, put the Videocallcard into a sane but "blank" state:
                updateNoCall(phone);
            }
        }
    }

    /**
     * Updates the UI for the state where the phone is in use, but not ringing.
     */
    private void updateForegroundCall(Phone phone) {
        Call fgCall = phone.getForegroundCall();
        Call bgCall = phone.getBackgroundCall();

        if (fgCall.isIdle() && !fgCall.hasConnections()) {
            if (DBG) log("updateForegroundCall: no active videocall, show holding videocall");
            // TODO: make sure this case agrees with the latest UI spec.

            // Display the background videocall in the main info area of the
            // VideoCallCard, since there is no foreground call.  Note that
            // displayMainCallStatus() will notice if the videocall we passed in is on
            // hold, and display the "on hold" indication.
            fgCall = bgCall;

            // And be sure to not display anything in the "on hold" box.
            bgCall = null;
        }

       	displayMainVideoCallStatus(phone, fgCall);	
    }

    /**
     * Updates the UI for the state where an incoming videocall is ringing (or
     * videocall waiting), regardless of whether the phone's already offhook.
     */
    private void updateRingingVideoCall(Phone phone) {
        Call ringingCall = phone.getRingingCall();
        Call fgCall = phone.getForegroundCall();
        Call bgCall = phone.getBackgroundCall();

        // Display caller-id info and photo from the incoming videocall:
        displayMainVideoCallStatus(phone, ringingCall);

        // And even in the Call Waiting case, *don't* show any info about
        // the current ongoing videocall and/or the current videocall on hold.
        // (Since the caller-id info for the incoming videocall totally trumps
        // any info about the current videocall(s) in progress.)
      //secondary_delete displayOnHoldVideoCallStatus(phone, null);

    }

    /**
     * Updates the UI for the state where the phone is not in use.
     * This is analogous to updateForegroundCall() and updateRingingVideoCall(),
     * but for the (uncommon) case where the phone is
     * totally idle.  (See comments in updateState() above.)
     *
     * This puts the Videocallcard into a sane but "blank" state.
     */
    private void updateNoCall(Phone phone) {
        displayMainVideoCallStatus(phone, null);
      //secondary_delete displayOnHoldVideoCallStatus(phone, null);
    }

    /**
     * Updates the main block of caller info on the VideoCallCard
     * (ie. the stuff in the primaryVideoCallInfo block) based on the specified Call.
     */
    private void displayMainVideoCallStatus(Phone phone, Call call) {

        if (call == null) {
            // There's no call to display, presumably because the phone is idle.
            //hgkim: mPrimaryVideoCallInfo.setVisibility(View.GONE);
            mCallInfo.setVisibility(View.GONE);
            mOutgoingCallInfo.setVisibility(View.GONE);

            // jaehun.ryu@lge.com: Notify Idle state to VideoTelephonyApp
            notifyStateChanged();
            return;
        }
        //mPrimaryVideoCallInfo.setVisibility(View.VISIBLE);

        Call.State state = call.getState();

        if (DBG) log("displayMainVideoCallStatus() " + call.getState());
        
        if(notifyStateChanged() || state == Call.State.INCOMING) //VT CallState changed. by hgkim
        { 
         if (DBG) log("really displayMainVideoCallStatus" + " " + state);
            switch (state) {
                case ACTIVE:
                case DISCONNECTING:
                	//@Fixed ME - hgkim 10/14
                	//When connected a someone, the ACTIVE call-state comes continuously.
                	//so, add flag which distinguish between real ACTIVE and dummy ACTIVE.
                	if(state == Call.State.ACTIVE )
                	{     
               	       addSurfaceviews();        	
                	}                	
                	
                	//hgkim: mPrimaryVideoCallInfo.setVisibility(View.VISIBLE);	
                    mCallInfo.setVisibility(View.GONE);
                    mOutgoingCallInfo.setVisibility(View.VISIBLE);
                    // update timer field
                    if (DBG) log("displayMainVideoCallStatus: start periodicUpdateTimer");
                    mCallTime.setActiveCallMode(call);
                    mCallTime.reset();
                    mCallTime.periodicUpdateTimer();
                    break;

                case HOLDING:
                    mCallTime.cancelTimer();
                    break;

                case DISCONNECTED:
            	{
	                // Stop getting timer ticks from this videocall
	                mCallTime.cancelTimer();
	                mCallInfo.setVisibility(View.VISIBLE);
	                mOutgoingCallInfo.setVisibility(View.GONE);
	            		
                        restoreReadyImageView();
//                        if(mIsNearTop == false)
//                        {
//                            //initialize.
//                            swapView();
//                        }
        		        //destroyed
        		        Message msg = mSurfaceHandler.obtainMessage(STATE_COMES_DISCONNECTED);
        		        mSurfaceHandler.sendMessage(msg);
              
            	}                
                break;

                case DIALING:
                case ALERTING:
                {
                    mOutgoingCallInfo.setVisibility(View.VISIBLE);               
                    mCallInfo.setVisibility(View.GONE);
                    // Stop getting timer ticks from a previous videocall
                    mCallTime.cancelTimer();
                }
                    break;

                case INCOMING:
                case WAITING:
                	mCallInfo.setVisibility(View.VISIBLE);
                    mOutgoingCallInfo.setVisibility(View.GONE);
                    // Stop getting timer ticks from a previous videocall
                    mCallTime.cancelTimer();

                    break;

                case IDLE:
                	//mPrimaryVideoCallInfo.setVisibility(View.VISIBLE);
                    // The "main VideoCallCard" should never be trying to display
                    // an idle videocall!  In updateState(), if the phone is idle,
                    // we call updateNoCall(), which means that we shouldn't
                    // have passed a call into this method at all.
                    log( "displayMainCallStatus: IDLE call in the main videocall card!");

                    // (It is possible, though, that we had a valid videocall which
                    // became idle *after* the check in updateState() but
                    // before we get here...  So continue the best we can,
                    // with whatever (stale) info we can get from the
                    // passed-in Call object.)

                    break;

                default:
                    log( "displayMainCallStatus: unexpected videocall state: " + state);
                    break;
            }
            
           updateCardTitleWidgets(phone, call);
        }
        /*//conference_delete 
        if (PhoneUtils.isConferenceCall(call)) {
            // Update onscreen info for a conference call.
            updateDisplayForConference();
        } else 
        *///conference_delete 
        {
            // Update onscreen info for a regular videocall (which presumably
            // has only one connection.)
            Connection conn = null;
            int phoneType = phone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                conn = call.getLatestConnection();
            } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                conn = call.getEarliestConnection();
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }

            if (conn == null) {
                if (DBG) log("displayMainVideoCallStatus: connection is null, using default values.");
                // if the connection is null, we run through the behaviour
                // we had in the past, which breaks down into trivial steps
                // with the current implementation of getCallerInfo and
                // updateDisplayForPerson.
                CallerInfo info = PhoneUtils.getCallerInfo(getContext(), null /* conn */);
                updateDisplayForPerson(info, Connection.PRESENTATION_ALLOWED, false, call);
            } else {
                if (DBG) log("displayMainVideoCallStatus() CONN: " + conn );
                int presentation = conn.getNumberPresentation();
                // make sure that we only make a new query when the current
                // callerinfo differs from what we've been requested to display.
                boolean runQuery = true;
                Object o = conn.getUserData();
                if (o instanceof PhoneUtils.CallerInfoToken) {
                    runQuery = mPhotoTracker.isDifferentImageRequest(
                            ((PhoneUtils.CallerInfoToken) o).currentInfo);
                } else {
                    runQuery = mPhotoTracker.isDifferentImageRequest(conn);
                }

                // Adding a check to see if the update was caused due to a Phone number update
                // or CNAP update. If so then we need to start a new query
// LGE_MERGE_ECLAIR_WARNING
// LGE_COLP START
                if(true /*phoneType == Phone.PHONE_TYPE_CDMA*/) {
// LGE_COLP END
                    String updatedNumber = conn.getAddress();
                    CallerInfo info = null;
                    if (o instanceof PhoneUtils.CallerInfoToken) {
                        info = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                    } else if (o instanceof CallerInfo) {
                        info = (CallerInfo) o;
                    }
                    if (info != null) {
// LGE_COLP START
                        if (updatedNumber != null && !PhoneNumberUtils.compare(info.phoneNumber, updatedNumber)) {
                            if (DBG) log("- displayMainCallStatus: updatedNumber = "
                                    + updatedNumber);
                            conn.setUserData(null); // this is necessary to force new query in PhoneUtils.startGetCallerInfo()
// LGE_COLP END
                            runQuery = true;
                        }
                    }
                }

                if (runQuery) {
                    if (DBG) log("- displayMainVideoCallStatus: starting CallerInfo query...");
                    PhoneUtils.CallerInfoToken info =
                            PhoneUtils.startGetCallerInfo(getContext(), conn, this, call);
                    updateDisplayForPerson(info.currentInfo, presentation, !info.isFinal, call);
                } else {
                    // No need to fire off a new query.  We do still need
                    // to update the display, though (since we might have
                    // previously been in the "conference call" state.)
                    if (DBG) log("- displayMainVideoCallStatus: using data we already have...");
                    if (o instanceof CallerInfo) {
                        CallerInfo ci = (CallerInfo) o;
                        // Update CNAP information if Phone state change occurred
                        ci.cnapName = conn.getCnapName();
                        ci.numberPresentation = conn.getNumberPresentation();
                        ci.namePresentation = conn.getCnapNamePresentation();
// Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [START_LGE_LAB] 
                        if (System.getProperty("user.country", "unknown").equals("KR"))
                        {
                            // for test
                            //ci.cdnipNumber = "010-2222-7777";
                            ci.cdnipNumber = conn.getCdnipNumber();
                           if (DBG) log("- cdnipNumber: " + ci.cdnipNumber);                            
			   }
// Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [END_LGE_LAB]                         
                        if (DBG) log("- displayMainVideoCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                        //@ if (DBG) log("   ==> Got CallerInfo; updating display: ci = " + ci);
                        updateDisplayForPerson(ci, presentation, false, call);
                    } else if (o instanceof PhoneUtils.CallerInfoToken){
                        CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        if (DBG) log("- displayMainVideoCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                        if (DBG) log("   ==> Got CallerInfoToken; updating display: "); //ci = " + ci);
                        updateDisplayForPerson(ci, presentation, true, call);
                    } else {
                        log( "displayMainVideoCallStatus: runQuery was false, "
                              + "but we didn't have a cached CallerInfo object!  o = " + o);
                        // TODO: any easy way to recover here (given that
                        // the VideoCallCard is probably displaying stale info
                        // right now?)  Maybe force the VideoCallCard into the
                        // "Unknown" state?
                    }
                }
            }
        }

        // In some states we override the "photo" ImageView to be an
        // indication of the current state, rather than displaying the
        // regular photo as set above.
        updatePhotoForCallState(call);

        // One special feature of the "number" text field: For incoming
        // calls, while the user is dragging the RotarySelector widget, we
        // use mPhoneNumber to display a hint like "Rotate to answer".
        if (mRotarySelectorHintTextResId != 0) {
            // Display the hint!
            mCommonPhoneNumber.setText(mRotarySelectorHintTextResId);
            mCommonPhoneNumber.setTextColor(getResources().getColor(mRotarySelectorHintColorResId));
            mCommonPhoneNumber.setVisibility(View.VISIBLE);
            mCommonLabel.setVisibility(View.GONE);
        }
        // If we don't have a hint to display, just don't touch
        // mPhoneNumber and mLabel. (Their text / color / visibility have
        // already been set correctly, by either updateDisplayForPerson()
        // or updateDisplayForConference().)
        
        //@Fixed ME - update enable/disable bottom's button 
        updateButtonForCallState(state);
   
    }

    /**
     * Implemented for CallerInfoAsyncQuery.OnQueryCompleteListener interface.
     * refreshes the VideoCallCard data when it called.
     */
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        if (DBG) log("onQueryComplete: token " + token + ", cookie " + cookie /* + ", ci " + ci */);

        if (cookie instanceof Call) {
            // grab the call object and update the display for an individual call,
            // as well as the successive call to update image via call state.
            // If the object is a textview instead, we update it as we need to.
            if (DBG) log("callerinfo query complete, updating ui from displayMainVideoCallStatus()");
            Call call = (Call) cookie;
            Connection conn = call.getEarliestConnection();
            PhoneUtils.CallerInfoToken cit =
                   PhoneUtils.startGetCallerInfo(getContext(), conn, this, null);

            int presentation = Connection.PRESENTATION_ALLOWED;
            if (conn != null) presentation = conn.getNumberPresentation();
            if (DBG) log("- onQueryComplete: presentation=" + presentation
                    + ", contactExists=" + ci.contactExists);

            // Depending on whether there was a contact match or not, we want to pass in different
            // CallerInfo (for CNAP). Therefore if ci.contactExists then use the ci passed in.
            // Otherwise, regenerate the CIT from the Connection and use the CallerInfo from there.
            if (ci.contactExists) {
                updateDisplayForPerson(ci, Connection.PRESENTATION_ALLOWED, false, call);
            } else {
                updateDisplayForPerson(cit.currentInfo, presentation, false, call);
            }
            updatePhotoForCallState(call);

        } else if (cookie instanceof TextView){
            if (DBG) log("callerinfo query complete, updating ui from ongoing or onhold");
            ((TextView) cookie).setText(PhoneUtils.getCompactNameFromCallerInfo(ci, mContext));
        }
    }

    /**
     * Implemented for ContactsAsyncHelper.OnImageLoadCompleteListener interface.
     * make sure that the videocall state is reflected after the image is loaded.
     */
    public void onImageLoadComplete(int token, Object cookie, ImageView iView,
            boolean imagePresent){
        if (cookie != null) {
            updatePhotoForCallState((Call) cookie);
        }
    }

    /**
     * Updates the "card title" (and also elapsed time widget) based on
     * the current state of the videocall.
     */
    private void updateCardTitleWidgets(Phone phone, Call call) {
        // TODO: Still need clearer spec on exactly how title *and* status get
        // set in all states.  (Then, given that info, refactor the code
        // here to be more clear about exactly which widgets on the card
        // need to be set.)

        Call.State state = call.getState();
        String cardTitle;
	    long duration;
        int phoneType = mApplication.phone.getPhoneType();
        if (phoneType == Phone.PHONE_TYPE_CDMA) {
            if (!PhoneApp.getInstance().notifier.getIsCdmaRedialCall()) {
                cardTitle = getTitleForCallCard(call);  // Normal "foreground" videocall card
            } else {
                cardTitle = getContext().getString(R.string.card_title_redialing);
            }
        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
            cardTitle = getTitleForCallCard(call);
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }


        if (DBG) log("updateCardTitleWidgets(): " + cardTitle + "  " + state);


        // Update the title and elapsed time widgets based on the current videocall state.
        switch (state) {
            case ACTIVE:
            case DISCONNECTING:
                final boolean bluetoothActive = mApplication.showBluetoothIndication();
                int ongoingCallIcon = bluetoothActive ? R.drawable.ic_incall_ongoing_bluetooth
                        : R.drawable.ic_incall_ongoing;
                
                int connectedTextColor = bluetoothActive
                        ? mTextColorConnectedBluetooth : mTextColorConnected;

                if (phoneType == Phone.PHONE_TYPE_CDMA) {
                    // Check if the "Dialing" 3Way call needs to be displayed
                    // as the Foreground Call state still remains ACTIVE
                    if (mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                        // Use the "upper title":
                        setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);
                    } else {
                        // Normal "ongoing videocall" state; don't use any "title" at all.

                        // In videocall, setUpperTitle() is used instead of clearUpperTitle()
                        //clearUpperTitle();
                       setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);
                    }
                } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                    // While in the DISCONNECTING state we display a
                    // "Hanging up" message in order to make the UI feel more
                    // responsive.  (In GSM it's normal to see a delay of a
                    // couple of seconds while negotiating the disconnect with
                    // the network, so the "Hanging up" state at least lets
                    // the user know that we're doing something.)
                    // TODO: consider displaying the "Hanging up" state for
                    // CDMA also if the latency there ever gets high enough.
           	
                    setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);                    

                }

                // Use the elapsed time widget to show the current videocall duration.
//                mOutElapsedTime.setVisibility(View.VISIBLE);
                mOutElapsedTime.setText("");
                mOutElapsedTime.setTextColor(connectedTextColor);
        		mOutElapsedTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
            			getResources().getDimension(R.dimen.vt_dialpad_elapsedtime_size));
        		
                duration = CallTime.getCallDuration(call);  // msec
                updateElapsedTimeWidget(duration / 1000);
                // Also see onTickForCallTimeElapsed(), which updates this
                // widget once per second while the videocall is active.
                break;

            case DISCONNECTED:
                // Display "Call ended" (or possibly some error indication;
                // see getCallFailedString()) in the upper title, in red.

                // TODO: display a "videocall ended" icon somewhere, like the old
                // R.drawable.ic_incall_end?

                setUpperTitle(cardTitle, mTextColorEnded, state);

                // In the "Call ended" state, leave the mElapsedTime widget
                // visible, but don't touch it (so  we continue to see the elapsed time of
                // the videocall that just ended.)
                duration = CallTime.getCallDuration(call);  // msec
		  mDiscElapsedTime.setText(DateUtils.formatElapsedTime(duration / 1000));
                mDiscElapsedTime.setVisibility(View.VISIBLE);
                mDiscElapsedTime.setTextColor(mTextColorEnded);
                break;

            case HOLDING:
                // For a single videocall on hold, display the title "On hold" in
                // orange.
                // (But since the upper title overlaps the label of the
                // Hold/Unhold button, we actually use the elapsedTime widget
                // to display the title in this case.)

                // TODO: display an "On hold" icon somewhere, like the old
                // R.drawable.ic_incall_onhold?

                clearUpperTitle();
                mOutElapsedTime.setText(cardTitle);

                // While on hold, the elapsed time widget displays an
                // "on hold" indication rather than an amount of time.
                mOutElapsedTime.setVisibility(View.VISIBLE);
                mOutElapsedTime.setTextColor(mTextColorOnHold);
                break;

            default:
                // All other states (DIALING, INCOMING, etc.) use the "upper title":
                setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);

//                // ...and we don't show the elapsed time.
//                mOutElapsedTime.setVisibility(View.INVISIBLE);
		  mDiscElapsedTime.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * Updates mElapsedTime based on the specified number of seconds.
     * A timeElapsed value of zero means to not show an elapsed time at all.
     */
    private void updateElapsedTimeWidget(long timeElapsed) {
        // if (DBG) log("updateElapsedTimeWidget: " + timeElapsed);
        if (timeElapsed == 0) {
            mOutElapsedTime.setText("");
        } else {
            mOutElapsedTime.setText(DateUtils.formatElapsedTime(timeElapsed));
        }
    }

    /**
     * Returns the "card title" displayed at the top of a foreground
     * ("active") VideoCallCard to indicate the current state of this videocall, like
     * "Dialing" or "In call" or "On hold".  A null return value means that
     * there's no title string for this state.
     */
    private String getTitleForCallCard(Call call) {
        String retVal = null;
        Call.State state = call.getState();
        Context context = getContext();
        int resId;

        switch (state) {
            case IDLE:
                break;

            case ACTIVE:
                // Title is "Call in progress".  (Note this appears in the
                // "lower title" area of the VideoCallCard.)
                int phoneType = mApplication.phone.getPhoneType();
                if (phoneType == Phone.PHONE_TYPE_CDMA) {
                    if (mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                        retVal = context.getString(R.string.card_title_dialing);
                    } else {
                        retVal = context.getString(R.string.card_title_in_progress);
                    }
                } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                    retVal = context.getString(R.string.card_title_in_progress);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
                break;

            case HOLDING:
                retVal = context.getString(R.string.card_title_on_hold);
                // TODO: if this is a conference call on hold,
                // maybe have a special title here too?
                break;

            case DIALING:
            case ALERTING:
                retVal = context.getString(R.string.card_title_dialing);
                break;

            case INCOMING:
            case WAITING:
                    final CallNotifier notifier = PhoneApp.getInstance().notifier;
                    if (PhoneUtils.getIsReserveCall() && !notifier.isRinging()) // if (!notifier.isRinging())
                        retVal = context.getString(R.string.CALLSTR_CALLREJECTING);
                    else
                        retVal = context.getString(R.string.vt_incoming_call);
                break;
                

            case DISCONNECTING:
                retVal = context.getString(R.string.card_title_hanging_up);
                break;

            case DISCONNECTED:
                retVal = getCallFailedString(call);
                break;
        }
        
        if (DBG) log("- getTitleForCallCard() " + call + "  ==> result:" + retVal );

        return retVal;
    }


    private String getCallFailedString(Call call) {
        Connection c = call.getEarliestConnection();
        int resID;

        if (c == null) {
            if (DBG) log("getCallFailedString: connection is null, using default values.");
            // if this connection is null, just assume that the
            // default case occurs.
            resID = R.string.card_title_call_ended;
        } else {
        
            // TODO: The card *title* should probably be "Call ended" in all
            // cases, but if the DisconnectCause was an error condition we should
            // probably also display the specific failure reason somewhere...
            Connection.DisconnectCause cause = c.getDisconnectCause();
            if (VTPreferences.NETWORK_MODE == VTPreferences.NETWORK_CSVT)
            {
                   switch (cause) {
                    case BUSY:
                        resID = R.string.callFailed_userBusy;
                        break;

                    case CONGESTION:
                        resID = R.string.callFailed_congestion;
                        break;

                    case LOST_SIGNAL:
                    case CDMA_DROP:
                        resID = R.string.callFailed_noSignal;
                        break;

                    case LIMIT_EXCEEDED:
                        resID = R.string.callFailed_limitExceeded;
                        break;

                    case POWER_OFF:
                        resID = R.string.callFailed_powerOff;
                        break;

                    case ICC_ERROR:
                        resID = R.string.callFailed_simError;
                        break;

                    case OUT_OF_SERVICE:
                        resID = R.string.callFailed_outOfService;
                        break;

                    // TODO: 잘못된 번호 string change    
                    case INVALID_NUMBER: 
                        resID = R.string.vt_invalid_number;
                        break;
                   //-- end 
                   
                    default:
                        resID = R.string.card_title_call_ended;
                        break;
                }
            }
            else /* For PSVT */
            {
                /* String ID 추가후 아래 문구로 수정 요망 */
                 /*
                                CALL_BARRED         "서비스중지\n고객센터로\n연락해주세요"
                                NO_ANSWER           "상대방의 응답이\n없습니다"
                                BUSY                "상대방이\n통화중입니다"
                                CONGESTION  "영상통화 연결이\n되지 않습니다"
                                POWER_OFF   "상대방의 전화가\n꺼져 있어\n연결되지않습니다"
                                INVALID_NUMBER      "지금거신번호는\n없는번호입니다"
                                BEARER_NOT_AVAIL       "영상통화지역이\n아닙니다\n음성통화전환중"
                                BEARER_NOT_AUTH "상대가 영상통화\n연결되지않아\n음성통화전환중"
                                FORBIDDEN           "서비스중지\n고객센터로\n연락해주세요"
                                BADRESPONSE     "영상통화 연결이\n되지 않습니다\n잠시 후 사용하세요"
                                NORMAL      "영상통화가\r\n종료되었습니다"
                            */
                switch (cause)
                {
                    //@Fix ME: It has to be changed 
                    case CALL_BARRED:
                        resID = R.string.card_title_call_ended;
                        break;

                    //@Fix ME: It has to be changed 
                    case  NO_ANSWER:
                        resID = R.string.card_title_call_ended;
                        break;

                    //@Fix ME: It has to be changed 
                    case BUSY:
                        resID = R.string.callFailed_userBusy;
                        break;
                        
                    //@Fix ME: It has to be changed 
                    case CONGESTION:
                        resID = R.string.callFailed_congestion;
                        break;

                    //@Fix ME: It has to be changed 
                    case POWER_OFF:
                        resID = R.string.callFailed_powerOff;
                        break;

                    //@Fix ME: It has to be changed 
                    // TODO: 잘못된 번호 string change    
                    case INVALID_NUMBER: 
                        resID = R.string.vt_invalid_number;
                        break;

                    //@Fix ME: It has to be changed 
                    case BEARER_NOT_AVAIL:
                        resID = R.string.card_title_call_ended;
                        break;

                    //@Fix ME: It has to be changed 
                    case BEARER_NOT_AUTH:
                        resID = R.string.card_title_call_ended;
                        break;

                    //@Fix ME: It has to be changed 
                    case FORBIDDEN:
                        resID = R.string.card_title_call_ended;
                        break;

                    //@Fix ME: It has to be changed 
                    case BADRESPONSE:
                        resID = R.string.card_title_call_ended;
                        break;

                    case LOST_SIGNAL:
                    case CDMA_DROP:
                        resID = R.string.callFailed_noSignal;
                        break;

                    case LIMIT_EXCEEDED:
                        resID = R.string.callFailed_limitExceeded;
                        break;


                    case ICC_ERROR:
                        resID = R.string.callFailed_simError;
                        break;

                    case OUT_OF_SERVICE:
                        resID = R.string.callFailed_outOfService;
                        break;

                    //-- end 

                    default:
                        resID = R.string.card_title_call_ended;
                        break;
                }
            }
                
                
        }
        return getContext().getString(resID);
    }

    /**
     * Updates the name / photo / number / label fields on the VideoCallCard
     * based on the specified CallerInfo.
     *
     * If the current call is a conference call, use
     * updateDisplayForConference() instead.
     */
    private void updateDisplayForPerson(CallerInfo info,
                                        int presentation,
                                        boolean isTemporary,
                                        Call call) {

    
    if (DBG) log("updateDisplayForPerson( )\npresentation:" +  presentation + " isTemporary:" + isTemporary);

//        if (DBG) log("updateDisplayForPerson(" + info + ")\npresentation:" +
//                     presentation + " isTemporary:" + isTemporary);

	Call.State state = call.getState();

	TextView  mName, mPhoneNumber,mLabel,mSocialStatus;

      
	if((state == Call.State.INCOMING) || (state == Call.State.DISCONNECTED))
	{
		mName = mCommonName;
		mPhoneNumber = mCommonPhoneNumber;
		mLabel = mCommonLabel;
		mSocialStatus = mCommonSocialStatus;
	}
	else
	{
		mName = mOutName;
		mPhoneNumber = mOutPhoneNumber;
		mPhoneNumber.setSelected(true);
              mLabel = mCommonLabel;
	}
	
        // inform the state machine that we are displaying a photo.
        mPhotoTracker.setPhotoRequest(info);
        mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);

        String name = null;
        String displayNumber = null;
        String label = null;
        Uri personUri = null;
        String socialStatusText = null;
        Drawable socialStatusBadge = null;

        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.            
            if (TextUtils.isEmpty(info.name)) {
		  mHasNameInContact = false;
                if (TextUtils.isEmpty(info.phoneNumber)) {
                     if ((state == Call.State.INCOMING) && (presentation != Connection.PRESENTATION_RESTRICTED))
                        name = null;
                     else
                        name =  getPresentationString(presentation);
                     
                     if (!TextUtils.isEmpty(info.cnapName)) {
                        name = info.cnapName;
                        if (info.namePresentation != Connection.PRESENTATION_ALLOWED) {
                            name = getPresentationString(info.namePresentation);
                        }
                    }
                } else if (presentation != Connection.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a phone #
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    name = getPresentationString(presentation);
                } else if (!TextUtils.isEmpty(info.cnapName)) {
                      name = info.cnapName;
                      displayNumber = info.phoneNumber;
                      if (info.namePresentation != Connection.PRESENTATION_ALLOWED) {
                          name = getPresentationString(info.namePresentation);
                      }
                } else {
                    name = info.phoneNumber;
                }
            } else {
                mHasNameInContact = true;
               // if (presentation != Connection.PRESENTATION_ALLOWED) {
                if (presentation != Connection.PRESENTATION_ALLOWED && TextUtils.isEmpty(info.phoneNumber)) {
                    // This case should never happen since the network should never send a name
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    name = getPresentationString(presentation);
                } else {
                    if (call.isRinging() && !TextUtils.isEmpty(info.cnapName)) {
                        startToggling(call, info, mName);
                        name = info.cnapName;
                    } else {
                        stopToggling();
                        
                        if (!TextUtils.isEmpty(info.cnapName))
                            name = info.cnapName;
                        else
                            name = info.name;
                    }
                    displayNumber = info.phoneNumber;
                    label = info.phoneLabel;
                }
            }
            personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, info.person_id);
        } else {
            name =  getPresentationString(presentation);
        }
                    
       mSaveName = name;
       mSaveDisplayNumber = displayNumber;

        if (name != null && !call.isGeneric()) {
            mName.setText(name);
            mName.setVisibility(View.VISIBLE);
        } else {
            mName.setVisibility(View.GONE);
        }

        // Update mPhoto
        // if the temporary flag is set, we know we'll be getting another call after
        // the CallerInfo has been correctly updated.  So, we can skip the image
        // loading until then.

        // If the photoResource is filled in for the CallerInfo, (like with the
        // Emergency Number case), then we can just set the photo image without
        // requesting for an image load. Please refer to CallerInfoAsyncQuery.java
        // for cases where CallerInfo.photoResource may be set.  We can also avoid
        // the image load step if the image data is cached.
        if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
            mPhoto.setVisibility(View.INVISIBLE);
        } else if (info != null && info.photoResource != 0){
            showImage(mPhoto, info.photoResource);
        } else if (!showCachedImage(mPhoto, info)) {
            // Load the image with a callback to update the image state.
            // Use the default unknown picture while the query is running.
            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(
                info, 0, this, call, getContext(), mPhoto, personUri, R.drawable.picture_unknown);
        }

        if( (state == Call.State.DIALING) || (state == Call.State.ALERTING)){
            setPhotoOnDialing();
            if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
                mLarge_small_imageview[1].setVisibility(View.INVISIBLE);
            } else if (info != null && info.photoResource != 0){
                 showBackImage(mLarge_small_imageview[1],info.photoResource);
            } else if ((info != null) && info.isCachedPhotoCurrent) {
                if (info.cachedPhoto != null) {
                     showBackImage(mLarge_small_imageview[1], info.cachedPhoto);
                } else {
                     showBackImage(mLarge_small_imageview[1], R.drawable.picture_unknown);
                }
            } 
        }
        
        // And no matter what, on all devices, we never see the "manage
        // conference" button in this state.
        //mManageConferencePhotoButton.setVisibility(View.INVISIBLE);

        if (displayNumber != null && !call.isGeneric()) {
            mPhoneNumber.setText(displayNumber);
            mPhoneNumber.setTextColor(mTextColorDefaultSecondary);
            mPhoneNumber.setVisibility(View.VISIBLE);
        } else {
            mPhoneNumber.setVisibility(View.GONE);
        }

        if((state == Call.State.INCOMING) || (state == Call.State.DISCONNECTED))
        {
            if (label != null && !call.isGeneric()) {
                mLabel.setText(label);
                mLabel.setVisibility(View.VISIBLE);
            } else {
                mLabel.setVisibility(View.GONE);
            }
        }else
             mLabel.setVisibility(View.GONE);

// Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [START_LGE_LAB] 
        if (System.getProperty("user.country", "unknown").equals("KR"))
        {
            if ((state == Call.State.INCOMING) && !TextUtils.isEmpty(info.cdnipNumber)) {
                mNameSecond.setText(getResources().getString(R.string.CALLSTR_ARRIVEDNUM));
                String displaySecondNumber = PhoneNumberUtils.formatNumber(info.cdnipNumber);
                mPhoneNumberSecond.setText(displaySecondNumber);
                mNameSecond.setVisibility(View.VISIBLE);
                mPhoneNumberSecond.setVisibility(View.VISIBLE);
            } else {
                mNameSecond.setText("");
                mPhoneNumberSecond.setText("");
                mNameSecond.setVisibility(View.GONE);
                mPhoneNumberSecond.setVisibility(View.GONE);
            }
        }        
// Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber) [END_LGE_LAB]
    }

    private String getPresentationString(int presentation) {        
         String name = "";
        
        if (System.getProperty("user.country", "unknown").equals("KR"))
        {
            if (presentation == Connection.PRESENTATION_RESTRICTED) {
                name = getContext().getString(R.string.private_num);
            }  else if (presentation == Connection.PRESENTATION_PAYPHONE) {
                name = getContext().getString(R.string.payphone);
            } else {            
                  if(VTPreferences.OPERATOR.equals("SKT"))
                        name = getContext().getString(R.string.CALLSTR_UNKNOWN_SKT);
                  else if(VTPreferences.OPERATOR.equals("KTF"))
                        name = getContext().getString(R.string.CALLSTR_UNKNOWN_KTF);
                  else
                       name = getContext().getString(R.string.CALLSTR_UNKNOWN_LGT);
            }
         }else
         {
            name = getContext().getString(R.string.unknown);
            if (presentation == Connection.PRESENTATION_RESTRICTED) {
                name = getContext().getString(R.string.private_num);
            } else if (presentation == Connection.PRESENTATION_PAYPHONE) {
                name = getContext().getString(R.string.payphone);
            }
         }  
        return name;
    }

  
    /**
     * Updates the VideoCallCard "photo" IFF the specified Call is in a state
     * that needs a special photo (like "busy" or "dialing".)
     *
     * If the current call does not require a special image in the "photo"
     * slot onscreen, don't do anything, since presumably the photo image
     * has already been set (to the photo of the person we're talking, or
     * the generic "picture_unknown" image, or the "conference call"
     * image.)
     */
    private void updatePhotoForCallState(Call call) {
        if (DBG) log("updatePhotoForCallState()" + call );
        int photoImageResource = 0;

        // Check for the (relatively few) telephony states that need a
        // special image in the "photo" slot.
        Call.State state = call.getState();
        switch (state) {
            case DISCONNECTED:
                // Display the special "busy" photo for BUSY or CONGESTION.
                // Otherwise (presumably the normal "call ended" state)
                // leave the photo alone.
                Connection c = call.getEarliestConnection();
                // if the connection is null, we assume the default case,
                // otherwise update the image resource normally.
                if (c != null) {
                    Connection.DisconnectCause cause = c.getDisconnectCause();
                    if (DBG) log("updatePhotoForCallState() cause= " + cause );
                    if ((cause == Connection.DisconnectCause.BUSY)
                        || (cause == Connection.DisconnectCause.CONGESTION)) {
                        photoImageResource = R.drawable.picture_busy;
                    }
                } else if (DBG) {
                    log("updatePhotoForCallState: connection is null, ignoring.");
                }

                // TODO: add special images for any other DisconnectCauses?
                break;

            case DIALING:
            case ALERTING:
                photoImageResource = R.drawable.picture_dialing;
                break;

            default:
                // Leave the photo alone in all other states.
                // If this call is an individual call, and the image is currently
                // displaying a state, (rather than a photo), we'll need to update
                // the image.
                // This is for the case where we've been displaying the state and
                // now we need to restore the photo.  This can happen because we
                // only query the CallerInfo once, and limit the number of times
                // the image is loaded. (So a state image may overwrite the photo
                // and we would otherwise have no way of displaying the photo when
                // the state goes away.)

                // if the photoResource field is filled-in in the Connection's
                // caller info, then we can just use that instead of requesting
                // for a photo load.

                // look for the photoResource if it is available.
                CallerInfo ci = null;
                {
                    Connection conn = null;
                    int phoneType = mApplication.phone.getPhoneType();
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        conn = call.getLatestConnection();
                    } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                        conn = call.getEarliestConnection();
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }

                    if (conn != null) {
                        Object o = conn.getUserData();
                        if (o instanceof CallerInfo) {
                            ci = (CallerInfo) o;
                        } else if (o instanceof PhoneUtils.CallerInfoToken) {
                            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        }
                    }
                }

                if (ci != null) {
                    photoImageResource = ci.photoResource;
                }

                // If no photoResource found, check to see if this is a conference call. If
                // it is not a conference call:
                //   1. Try to show the cached image
                //   2. If the image is not cached, check to see if a load request has been
                //      made already.
                //   3. If the load request has not been made [DISPLAY_DEFAULT], start the
                //      request and note that it has started by updating photo state with
                //      [DISPLAY_IMAGE].
                // Load requests started in (3) use a placeholder image of -1 to hide the
                // image by default.  Please refer to CallerInfoAsyncQuery.java for cases
                // where CallerInfo.photoResource may be set.
                if (photoImageResource == 0) {
                	//conference_delete  if (!PhoneUtils.isConferenceCall(call)) {
                        if (!showCachedImage(mPhoto, ci) && (mPhotoTracker.getPhotoState() ==
                                ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT)) {
                            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(ci,
                                    getContext(), mPhoto, mPhotoTracker.getPhotoUri(), -1);
                            mPhotoTracker.setPhotoState(
                                    ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                        }
                      //conference_delete   }        
                } else {
                    showImage(mPhoto, photoImageResource);
                    mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                    return;
                }
                break;
        }

        if (photoImageResource != 0) {
            if (DBG) log("- overrriding photo image: " + photoImageResource);
            showImage(mPhoto, photoImageResource);
            // Track the image state.
            mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT);
        }

	// Display VideoCall ICON
	 showImage(mVTIcon, R.drawable.vt_icon);	
    }

    /**
     * Try to display the cached image from the callerinfo object.
     *
     *  @return true if we were able to find the image in the cache, false otherwise.
     */
    private static final boolean showCachedImage(ImageView view, CallerInfo ci) {
        if ((ci != null) && ci.isCachedPhotoCurrent) {
            if (ci.cachedPhoto != null) {
                showImage(view, ci.cachedPhoto);
            } else {
                showImage(view, R.drawable.picture_unknown);
            }
            return true;
        }
        return false;
    }

    /** Helper function to display the resource in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, int resource) {
        view.setImageResource(resource);
        view.setVisibility(View.VISIBLE);
    }

    /** Helper function to display the drawable in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);
        view.setVisibility(View.VISIBLE);
    }

// LGE_UI_INCOMMING_CALL START   NOTE: this code should be removed for Eclair
    /**


     * Returns the "Menu button hint" TextView (which is manipulated
     * directly by the InCallScreen.)
     * @see InCallScreen.updateMenuButtonHint()
     */
    /* package */ TextView getMenuButtonHint() {
        return mMenuButtonHint;
    }

    /**
     * Sets the left and right margins of the specified ViewGroup (whose
     * LayoutParams object which must inherit from
     * ViewGroup.MarginLayoutParams.)
     *
     * TODO: Is there already a convenience method like this somewhere?
     */
    private void setSideMargins(ViewGroup vg, int margin) {
        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) vg.getLayoutParams();
        // Equivalent to setting android:layout_marginLeft/Right in XML
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        vg.setLayoutParams(lp);
    }

    /**
     * Sets the VideoCallCard "upper title".  Also, depending on the passed-in
     * Call state, possibly display an icon along with the title.
     */
    private void setUpperTitle(String title, int color, Call.State state) {

    	boolean isVis = (mUpperTitle.getVisibility() == View.VISIBLE)?true:false;
    	if (DBG) log("setUpperTitle()" + title + "  color = "+ color + "  state = " + state + "  isVis="+ isVis);

    	
        String full_title = title;

        int bluetoothIconId = 0;
        if (!TextUtils.isEmpty(title)
                && ((state == Call.State.INCOMING) || (state == Call.State.WAITING))
                && mApplication.showBluetoothIndication()) {
            // Display the special bluetooth icon also, if this is an incoming
            // call and the audio will be routed to bluetooth.
            bluetoothIconId = R.drawable.ic_incoming_call_bluetooth;
        }
	
        
	if((state == Call.State.INCOMING) || (state == Call.State.DISCONNECTED))
	{	
		mUpperTitle.setVisibility(View.VISIBLE);
    		mUpperTitle.setTextColor(color);
    		mUpperTitle.setText(title);		
	}
	//@Fixed ME - In the Active state of GSM call, upper title has Visible.GONE. by hgkim 08/23
	//in the active state, the near(camera)view positions to down and makes larger than original far(remote)view.   
	else if(state == Call.State.ACTIVE)
	{
		if(mIsAvatarStart == false )
		{		
			hidePhotoOnDialing();
		}
		else
		{
			if(mLarge_small_imageview[1] == null)
			{
				log( "return hidePhotoOnDialing() ");
				return;
			}
			
			mLarge_small_imageview[1].setImageBitmap(mSmallBitmap);
			mLarge_small_imageview[1].setBackgroundDrawable(null);
			mLarge_small_imageview[1].setVisibility(View.VISIBLE);
			
		}
 
		
		Handler h = new Handler(){
			public void handleMessage(Message m)
			{
				if(m.what == 0)
				{
					log( "========= swapView() ==========");
					log( "->mIsNearTop = " + mIsNearTop);
					
					//camera preview is down
					swapView();				
					
					if(mIsAvatarStart == true)
					{
						setVisibilityImageView();
					}
					
					log( "->mIsNearTop2 = " + mIsNearTop);
					sendEmptyMessageDelayed(1, 100);
				}
				else if(m.what == 1)
				{
					log( "showReadyProgressbar()");
					//ready progressbar
					showReadyProgressbar();
                                   disableButton(BUTTON_PRIVATE_SHOW);
				}
			}
		};
		
		h.sendEmptyMessageDelayed(0, 200);
		

//              if(mIsAvatarStart)
//                setVisibilityImageView();
	}
	else
	{
		mOutElapsedTime.setVisibility(View.VISIBLE);
		mOutElapsedTime.setTextColor(color);
		mOutElapsedTime.setText(title);
		mOutElapsedTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
    			getResources().getDimension(R.dimen.vt_dialpad_dialing_size));
		
	
		mUpperTitle.setVisibility(View.GONE);
		mDiscElapsedTime.setVisibility(View.GONE);
        }
	
    }

    /**
     * When received fall-back , we can't receive PHONE_STATE_CHANGED msg and 
     *  only can directly receive PHONE_DISCONNECT msg in the InVideoCallScreen's handleMessage().
     *  
     *  so, we must call this api explicitly.
     * */
    public void restoreSurfaceView()
    {
    	restoreFarView();	
    	restoreNearView();
    }
    
    public void restoreReadyImageView()
    {
    	hidePhotoOnDialing();
    	hideReadyProgressbar();
    }
    
    private void restoreFarView()
    {
    	//change a top margin of the mVideoOutgoingLayout
		ViewGroup.MarginLayoutParams margin = 
			new ViewGroup.MarginLayoutParams(mVideoOutgoingLayout.getLayoutParams());
		
//		//exception process
//		if(margin.topMargin == 0)
//		{
//			return;
//		}
		
		margin.setMargins(0, 0, 0, 0); 
		mVideoOutgoingLayout.setLayoutParams(new LinearLayout.LayoutParams(margin)); 
//		mVideoOutgoingLayout.invalidate();    		
    	
		//1. set large size background(remotebackground)
		/////////////////////////////////////////////////////////		
		android.view.ViewGroup.LayoutParams params;
		params = mRemoteBackground.getLayoutParams();

		params.width = 270;
		params.height = 222;
		//margin
		((MarginLayoutParams)params).leftMargin  = 105; 
		((MarginLayoutParams)params).rightMargin  = 105;
		
		mRemoteBackground.setLayoutParams(params);
//		mRemoteBackground.invalidate();

		//2. set large size remote SurfaceView
		/////////////////////////////////////////////////////////mFarview
		android.view.ViewGroup.LayoutParams remoteParams;
		remoteParams = mFarview.getLayoutParams();
		
		//size
		remoteParams.width = 264;
		remoteParams.height = 216;
		
		//margin
		((MarginLayoutParams)remoteParams).leftMargin  = 108; 
		((MarginLayoutParams)remoteParams).rightMargin  = 108; 
		((MarginLayoutParams)remoteParams).topMargin  = 3; 
		
		mFarview.setLayoutParams(remoteParams);
	
		mFarview.invalidate();
    }
    
    /**
     * When a call-state is active, make large the upper surface-view. 
     * */
    private void changeFarView()
    {
		//change a top margin of the mVideoOutgoingLayout
		ViewGroup.MarginLayoutParams margin = 
			new ViewGroup.MarginLayoutParams(mVideoOutgoingLayout.getLayoutParams());
		
		margin.setMargins(0,40,0,0);

		mVideoOutgoingLayout.setLayoutParams(new LinearLayout.LayoutParams(margin)); 
//		mVideoOutgoingLayout.invalidate(); 
		
		//1. set large size background(remotebackground)
		/////////////////////////////////////////////////////////
		android.view.ViewGroup.LayoutParams params;
		params = mRemoteBackground.getLayoutParams();
		
		//size
		params.width = 358; //358
		params.height =294; //294
		
		//margin
		((MarginLayoutParams)params).leftMargin  = 61; 
		((MarginLayoutParams)params).rightMargin  = 61; 
		((MarginLayoutParams)params).topMargin  = 1;//11;

		mRemoteBackground.setLayoutParams(params);
//		mRemoteBackground.invalidate();
		
		
		//2. set large size remote SurfaceView
		/////////////////////////////////////////////////////////mFarview
		android.view.ViewGroup.LayoutParams remoteParams;
		remoteParams = mFarview.getLayoutParams();
		
		//size
		remoteParams.width = 352;
		remoteParams.height = 288;
		
		//margin
		((MarginLayoutParams)remoteParams).leftMargin  = 64;
		((MarginLayoutParams)remoteParams).rightMargin  = 64; 
		((MarginLayoutParams)remoteParams).topMargin  = 5;//4; 

		
		mFarview.setLayoutParams(remoteParams);
		mFarview.invalidate();
    }
    
    /**
     * When a call-state is active, down y-Postion near surfaceview. 
     * */
    private void changeNearView()
    {
    	log( "changeNearView() called~!");
		android.view.ViewGroup.LayoutParams nearParams;
		nearParams = mNearview.getLayoutParams();
		
		
		log( "topMargin(before) = " + ((MarginLayoutParams)nearParams).topMargin);
		if(((MarginLayoutParams)nearParams).topMargin != 335 )
		{
			//261, 297
			//margin
//			((MarginLayoutParams)nearParams).topMargin  = 
//				((MarginLayoutParams)nearParams).topMargin + 36;//36px add ==> 297
			
			((MarginLayoutParams)nearParams).topMargin  = 335;
			
			mNearview.setLayoutParams(nearParams);
			mNearview.invalidate();	
		}
    				
    }
    
    private void restoreNearView()
    {
		// set restore size camera SurfaceView
		/////////////////////////////////////////////////////////mFarview
		android.view.ViewGroup.LayoutParams remoteParams;
		remoteParams = mNearview.getLayoutParams();

//		//exception process
//		if(((MarginLayoutParams)remoteParams).topMargin == 261)
//		{
//			return;
//		}
//		
		//size
		remoteParams.width = 176;
		remoteParams.height = 144;
		
		//margin
		((MarginLayoutParams)remoteParams).leftMargin  = 262; 
		((MarginLayoutParams)remoteParams).rightMargin  = 0; 
		((MarginLayoutParams)remoteParams).bottomMargin  = 0;
		((MarginLayoutParams)remoteParams).topMargin  = 261; 
		
		mNearview.setLayoutParams(remoteParams);
		mNearview.invalidate();
    }
        
    
    
    /**
     * Clears the VideoCallCard "upper title", for states (like a normal
     * ongoing videocall) where we don't use any "title" at all.
     */
    private void clearUpperTitle() {
    	if (DBG) log("clearUpperTitle() called!");
        setUpperTitle("", 0, Call.State.IDLE);  // Use dummy values for "color" and "state"
    }

    /**
     * Hides the top-level UI elements of the videocall card:  The "main
     * call card" element representing the current active or ringing call,
     * and also the info areas for "ongoing" or "on hold" calls in some
     * states.
     *
     * This is intended to be used in special states where the normal
     * in-call UI is totally replaced by some other UI, like OTA mode on a
     * CDMA device.
     *
     * To bring back the regular VideoCallCard UI, just re-run the normal
     * updateState() videocall sequence.
     */
    public void hideCallCardElements() {
        //hgkim: mPrimaryVideoCallInfo.setVisibility(View.GONE);
      //secondary_delete mSecondaryCallInfo.setVisibility(View.GONE);
        mCallInfo.setVisibility(View.GONE);
        mOutgoingCallInfo.setVisibility(View.GONE);
    }

    /*
     * Updates the hint (like "Rotate to answer") that we display while
     * the user is dragging the incoming call RotarySelector widget.
     */
    /* package */ void setRotarySelectorHint(int hintTextResId, int hintColorResId) {
        mRotarySelectorHintTextResId = hintTextResId;
        mRotarySelectorHintColorResId = hintColorResId;
    }

    // View.OnClickListener implementation
    public void onClick(View view) {
        int id = view.getId();
        if (DBG) log("onClick(View " + view + ", id " + id + ")...");

        switch (id) {
        	/* //conference_delete  
            case R.id.manageConferencePhotoButton:
                // A click on anything here gets forwarded
                // straight to the InCallScreen.
                mInCallScreen.handleOnscreenButtonClick(id);
                break;
			*///conference_delete  
        
            default:
                log( "onClick: unexpected click: View " + view + ", id " + id);
                break;
        }
    }

// LGE_UI_INCOMMING_CALL START
// NOTE: this code block should be removed for Eclair

    // Debugging / testing code

    public String getDisplayPhoneNumer()
    {
       if (mSaveDisplayNumber == null) 
    	{
    		return mSaveName;
    	}
    	return mSaveDisplayNumber;
    }

    public boolean hasNameInContact() {
		return mHasNameInContact;
	}

	public void setHasNameInContact(boolean mHasNameInContact) {
		this.mHasNameInContact = mHasNameInContact;
	}


    private void log(String msg) {
        if(DBG)
        {
            Log.d(LOG_TAG, msg);
        }
    }
	
    private static void logErr(String msg) {
        if(DBG)
        {
            Log.e(LOG_TAG, msg);
        }
    }



	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (DBG) log("[NXP]surfaceChanged() width="+width+" height="+height);

		if (holder.equals(holderNearview)) {
			holderNearview = holder;
			notifyCreatedNearSurfaceView(holderNearview);
		}    		
		  else if( holder.equals(holderFarview))
		  {
			  holderFarview = holder; 
			  notifyCreatedFarSurfaceView(holderFarview);
		  }
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (DBG) log("[NXP]surfaceCreated");
		// TODO Auto-generated method stub
		if (holder.equals(holderNearview)){
			holderNearview = holder;
		}
		if (holder.equals(holderFarview)){
			holderFarview = holder;
		}

		//Surfaceview Created.
        Message msg = mSurfaceHandler.obtainMessage(STATE_SURFACEVIEW_CREATED);
        mSurfaceHandler.sendMessage(msg);

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (DBG)
		{
		    log("[NXP]surfaceDestroyed");
		    
		}
		

		if ( holder.equals(holderNearview)){
			notifyCreatedNearSurfaceView(null);
		}    		
		if ( holder.equals(holderFarview)){
			notifyCreatedFarSurfaceView(null);
		}
		
	      //Surfaceview Deatroyed.
        Message msg = mSurfaceHandler.obtainMessage(STATE_SURFACEVIEW_DESTROYED);
        mSurfaceHandler.sendMessage(msg);

	}

	//NXP_VT////////////////////////////////////////////////////////////////////////////

	/**
	 * Regards as Call-State, the state of button is Enable or Disable.
	 * @param call_state Call-State(ex. Activie , Dialing...)
	 * */	
	private void updateButtonForCallState(Call.State inState)
	{
		//setting disable 
		switch (inState) {
		case ACTIVE:
               // Move to received first video frame
		//	mIncallButton[BUTTON_DIALPAD].setEnabled(true);
		//	mIncallButton[BUTTON_HOLD].setEnabled(true);

			break;
		case DIALING:
		case ALERTING:
		{
			mIncallButton[BUTTON_DIALPAD].setEnabled(false);
			mIncallButton[BUTTON_HOLD].setEnabled(false);
			mIncallButton[BUTTON_MUTE].setEnabled(true);
			mIncallButton[BUTTON_PRIVATE_SHOW].setEnabled(true);
		}
		break;
              case INCOMING:
              {
             		mIncallButton[BUTTON_DIALPAD].setEnabled(false);
        		mIncallButton[BUTTON_HOLD].setEnabled(false);
                     mIncallButton[BUTTON_PRIVATE_SHOW].setEnabled(false);
               }
              break;
		default:
		{
	//					mIncallButton[BUTTON_DIALPAD].setEnabled(true);
	//					mIncallButton[BUTTON_CAPTURE].setEnabled(true);	
		}
		break;
		}
	}	



	/**
	 * 대체 영상 설정
	 * @see VideoCallHandler
	 * */
	public void showSubstitute(boolean isShow, boolean isHold)
	{	
		if (DBG) log("showSubstitute isshow = "+isShow + "  " + isHold);
		if(isShow)
		{		
			//if(mIsAvatarStart)
			//	return;

			if (isCaptureloading) {
				makeCaptureloadingImage();
				mLarge_small_imageview[2].setVisibility(View.VISIBLE);
			} else {
				makeSubstituteImage(isHold);
				setVisibilityImageView();
			}

			mIsAvatarStart = true;
		}
		else
		{
		       if(isCaptureloading)
                            return;

                     if(preSendsubstitute)
                     {
                          preSendsubstitute = false;
                          return;
                     }
			if(mLarge_small_imageview[0] != null)
			{
				mLarge_small_imageview[0].setImageBitmap(null);
				mLarge_small_imageview[0].setBackgroundDrawable(null);
				mLarge_small_imageview[0].setVisibility(View.GONE);
			}
			
			if(mLarge_small_imageview[1] != null)
			{
				mLarge_small_imageview[1].setImageBitmap(null);
				VTCallState callState = 
					VTAppStateManager.getInstance().getCallState();
				
				if(!(callState == VTCallState.DIALING || callState == VTCallState.ALERTING) )
				{
					mLarge_small_imageview[1].setBackgroundDrawable(null);	
				}				
				
				mLarge_small_imageview[1].setVisibility(View.GONE);	
			}

                     if(mLarge_small_imageview[2] != null)
                     {
                            mLarge_small_imageview[2].setImageBitmap(null);
                            mLarge_small_imageview[2].setVisibility(View.GONE);	
                     }
			mIsAvatarStart = false;
		}
	}

	/**
	 * Only called in the dialing call-state.
	 * @param nLarge (Large-substitute image if nLarge is 1, otherwise 0) 
	 * @see  showSubstitute() is active call-state 
	 * */
	public void showSubstituteOnDialing(int nLarge, boolean isShow)
	{	
		if (DBG) log("showSubstituteOnDialing isShow = "+isShow);		
		
		makeSubstituteImageOnDialing(nLarge);
		
		if(isShow)
		{	
			mLarge_small_imageview[nLarge].setVisibility(View.VISIBLE);	
			mIsAvatarStart = true;
		}
		else
		{
			//make hide
			mLarge_small_imageview[nLarge].setImageBitmap(null);			
			mLarge_small_imageview[nLarge].setVisibility(View.GONE);
			
			mIsAvatarStart = false;
		}
	}
	
	public void showMovSubstitute(boolean isShow)
	{
	       if (DBG) log("showMovSubstitute isshow = "+isShow);
		if(isShow)
			avatarMovStart = true;
		else
			avatarMovStart = false;
	}	
	//VTObserable Implements Methods
	////////////////////////////////////////////////////////////////	
	//@Override
	public void attach(VTObserver o) {
		mObservers.add(o);		
	}

	//@Override
	public void detach(VTObserver o) {
		if (mIterator != null) {
			mIterator.remove();
		} else {
			mObservers.remove(o);
		}	
	}

	//@Override
	public void notifyCreatedFarSurfaceView(SurfaceHolder farHolder) {
		mIterator = mObservers.iterator();
		Phone phone = ((InVideoCallScreen)mContext).getPhone();
		try {
			while (mIterator.hasNext()) {
				mIterator.next().onFarSurfaceViewCreated(farHolder); 
			}
		} finally {
			mIterator = null;
		}
	}

	//@Override
	public void notifyCreatedNearSurfaceView(SurfaceHolder nearHoler) {
		mIterator = mObservers.iterator();
		Phone phone = ((InVideoCallScreen)mContext).getPhone();
		try {
			while (mIterator.hasNext()) {
				mIterator.next().onNearSurfaceViewCreated(nearHoler); 
			}
		} finally {
			mIterator = null;
		}
	}

	//@Override
	public boolean notifyStateChanged() {
	    boolean bRet = false;
		mIterator = mObservers.iterator();
		Phone phone = ((InVideoCallScreen)mContext).getPhone();

		if (DBG) log("notifyStateChanged() call.state: " + phone.getForegroundCall().getState());

		try {
			while (mIterator.hasNext()) {
				bRet = mIterator.next().onUpdateVTState(phone); 
			}
		} finally {
			mIterator = null;
		}
              return  bRet;
	}

	// LGE_VT_DIALPAD start
	public void setDialpad(boolean bSet)
	{
		if (null == mVTDialpad) return ;

		if (bSet)
		{
			mVTDialpad.setVisibility(View.VISIBLE);					
		}
		else
		{
			mVTDialpad.setVisibility(View.GONE);
		}

		mIsDialpad = bSet;
	}

	public boolean isDialpad()
	{
		return mIsDialpad;
	}
	// LGE_VT_DIALPAD end

	/**
	 * make substitute image
	 * */
	private Bitmap mLargeBitmap = null;
	private Bitmap mSmallBitmap = null;
	private ImageView[] makeSubstituteImage(boolean isHold)
	{
		Bitmap sHoldBitmap = null;
		Bitmap sPrivateBitmap = null;

		Bitmap bm = null;
		
		if(mLarge_small_imageview[0] == null)
		{
			mLarge_small_imageview[0] = 
				(ImageView)mOutgoingCallInfo.findViewById(R.id.subsimage_large);
		}
		
		if(mLarge_small_imageview[1] == null)
		{
			mLarge_small_imageview[1] = 
				(ImageView)mOutgoingCallInfo.findViewById(R.id.subsimage_small);
		}
		
		if(isHold == false)
		{
			sendSubsImagePath = Settings.System.getString(mContext.getContentResolver(), Settings.System.VT_PRIVATE_NAME);
			bm = BitmapFactory.decodeFile(sendSubsImagePath);	   			
		}
		else
		{
			if(sHoldBitmap == null)
			{
                            sHoldBitmap = BitmapFactory.decodeFile(holdImagePath);
			}
			bm = sHoldBitmap;
		}

		//BitmapFactory.Options options = new BitmapFactory.Options(); 
		//options.inSampleSize = 2;  


//		mLargeBitmap = Bitmap.createScaledBitmap(bm, 352, 288, true);
		mLargeBitmap = Bitmap.createScaledBitmap(bm, 394, 324, true);
		mLarge_small_imageview[0].setImageBitmap(mLargeBitmap);
//		BitmapDrawable bd = new BitmapDrawable(mLargeBitmap);
//		mLarge_small_imageview[0].setBackgroundDrawable(bd);
		
		mSmallBitmap = Bitmap.createScaledBitmap(bm, 178, 144, true);
		mLarge_small_imageview[1].setImageBitmap(mSmallBitmap);
//		BitmapDrawable bd_s = new BitmapDrawable(mSmallBitmap);
//		mLarge_small_imageview[0].setBackgroundDrawable(bd_s);

              bm.recycle();
              bm = null;

		if (DBG) 
			log("makeImage: width="+ mNearview.getWidth() + "height=" + mNearview.getHeight());
		
		return mLarge_small_imageview;
	}
	
	/**
	 * Only called in the dialing call-state.
	 * @param nLarge (Large-substitute image if nLarge is 1, otherwise 0)
	 * @see  showSubstitute() is active call-state
	 * */
	private void makeSubstituteImageOnDialing(int nLarge)
	{

		Bitmap bm = null;
		
		if(mLarge_small_imageview[nLarge] == null)
		{
			int id = (nLarge == 0)?R.id.subsimage_large:R.id.subsimage_small;
			mLarge_small_imageview[nLarge] = 
				(ImageView)mOutgoingCallInfo.findViewById(id);
		}
		
		sendSubsImagePath = Settings.System.getString(mContext.getContentResolver(), Settings.System.VT_PRIVATE_NAME);
		bm = BitmapFactory.decodeFile(sendSubsImagePath);	   			

		if(nLarge == 0)
		{
//			mLargeBitmap = Bitmap.createScaledBitmap(bm, 352, 288, true);
		    mLargeBitmap = Bitmap.createScaledBitmap(bm, 394, 324, true);
			mLarge_small_imageview[0].setImageBitmap(mLargeBitmap);			
		}
		else
		{
			mSmallBitmap = Bitmap.createScaledBitmap(bm, 178, 144, true);
			mLarge_small_imageview[1].setImageBitmap(mSmallBitmap);	
		}		

              bm.recycle();
              bm = null;
	}

	/**
	 * set a visibility of Substitute image
	 * @See The flag of mIsNearTop is false , do the function swapView(). 
	 * */
	private void setVisibilityImageView()
	{	
	    if(mIsNearTop == false)
		{
			mLarge_small_imageview[0].setVisibility(View.GONE);
			mLarge_small_imageview[1].setVisibility(View.VISIBLE);
		}
		else
		{			
			mLarge_small_imageview[0].setVisibility(View.VISIBLE);
			mLarge_small_imageview[1].setVisibility(View.GONE);
		}

		//the problem - When a setting private func,mLarge_small_imageview[1] must be shown continuously.  
//		int nValue = Settings.System.getInt(mContext.getContentResolver(),
//				Settings.System.VT_USE_PRIVATE, 0);
		
//		if(nValue == 1)
		if(mIsAvatarStart == true && InVideoCallScreen.isSubstituImage == 1)
		{
		    if(mIsNearTop == false)
			{
				mLarge_small_imageview[1].setImageBitmap(mSmallBitmap);
				mLarge_small_imageview[1].setBackgroundDrawable(null);
				mLarge_small_imageview[1].setVisibility(View.VISIBLE);						

			}
			else
			{
				mLarge_small_imageview[0].setImageBitmap(mLargeBitmap);
				mLarge_small_imageview[0].setBackgroundDrawable(null);
				mLarge_small_imageview[0].setVisibility(View.VISIBLE);						
			}
		}

		
		
	}
       public void makeMovSubstitute()
      {
            if (DBG) log("makeMovSubstitute");
      }

/*
	private void makeFileFromResource(boolean isSubs) throws IOException{
		Resources r = getResources();
		InputStream is;
		File file;

		if(isSubs)
			is = r.openRawResource(R.raw.vt_in_call_private_small);
		else
			is = r.openRawResource(R.raw.vt_in_call_hold_small);

		int size = 0;
		size = (int)is.available();

		byte[] data = null;

		if(size > 0 )
		{
			data = new byte[size];
			try {
				is.read(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if(isSubs)
			file = new File(subsImagePath);
		else
			file = new File(holdImagePath);

		if(!file.exists())
		{
			file.createNewFile();
		}

		FileOutputStream fos = new FileOutputStream(file);
		fos.write(data);
		fos.flush();
		fos.close();
	}
*/
	/**
	 * toogle a string - Private or Show
	 * @since 2010/08/20
	 * @author hgkim@ubivelox.com
	 * */
	public void changePrivateButtonText()
	{
		if(null != mIncallButton[BUTTON_PRIVATE_SHOW])
		{
			CharSequence oldString = mIncallButton[BUTTON_PRIVATE_SHOW].getText();
			String privateString = getResources().getString(R.string.vt_Private);
			String showString = getResources().getString(R.string.vt_Show);

			//if old string is "Private" then change string "Show" 
			if(oldString.equals(privateString))
			{
				mIncallButton[BUTTON_PRIVATE_SHOW].setText(showString);
		    	
				//if the locale is ko, adjusts font size.
		    	//english: "en_US" , korean: "ko" or "ko_KR"
		        if(getResources().getConfiguration().locale.toString().equals("ko_KR"))
		        {	
		        	mIncallButton[BUTTON_PRIVATE_SHOW].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
		        			getResources().getDimension(R.dimen.vt_dialpad_ko_font_size));
		        }		        
				disableButton(BUTTON_HOLD);
			}
			else
			{
				mIncallButton[BUTTON_PRIVATE_SHOW].setText(privateString);
	        	mIncallButton[BUTTON_PRIVATE_SHOW].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
	        			getResources().getDimension(R.dimen.vt_dialpad_en_font_size));

                       if(VTAppStateManager.getInstance().isRecvFirstFrame())
		        {
			    enableButton(BUTTON_HOLD);
        		 }
			}			
		}
	}

	public void changeHoldButtonText()
	{
		if(null != mIncallButton[BUTTON_HOLD])
		{
			CharSequence oldString = mIncallButton[BUTTON_HOLD].getText();
			String holdString = getResources().getString(R.string.vt_Hold);
			String unholdString = getResources().getString(R.string.vt_unHold);

			//if old string is "Private" then change string "Show" 
			if(oldString.equals(holdString))
			{
				mIncallButton[BUTTON_HOLD].setText(unholdString);
		    	
				//if the locale is ko, adjusts font size.
        		    	//english: "en_US" , korean: "ko" or "ko_KR"
        		        if(getResources().getConfiguration().locale.toString().equals("ko_KR"))
        		        {	
        		        	mIncallButton[BUTTON_HOLD].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
        		        			getResources().getDimension(R.dimen.vt_dialpad_ko_font_size));
        		        }		        
			}
			else
			{
				mIncallButton[BUTTON_HOLD].setText(holdString);
        		        if(getResources().getConfiguration().locale.toString().equals("ko_KR"))
        		        {	                
        	        	       mIncallButton[BUTTON_HOLD].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
	        			getResources().getDimension(R.dimen.vt_dialpad_ko_font_size));
          		         }
			}			
		}
	}

	/**
	 *  disable button.   
	 * */
	public void disableButton(int nIndex)
	{
		if(null != mIncallButton[nIndex] )
		{			
			mIncallButton[nIndex].setEnabled(false);
		}	
	}

	/**
	 * enable button.   
	 * */
	public void enableButton(int nIndex)
	{
		if(null != mIncallButton[nIndex] )
		{			
			mIncallButton[nIndex].setEnabled(true);
		}		
	}	 

       public boolean getEnableButton(int nIndex)
      {
		if(null != mIncallButton[nIndex] )
		{			
			return mIncallButton[nIndex].isEnabled();
		}	
              return true;
       }
	/**
	 * Initialize flags
	 * */
	public void initFlags()
	{
		log( "------ initFlags() -------");
		if(IN_VT_SCENARIO)
		{
		    mIsNeedChangeView = false;
		    
	        android.view.ViewGroup.MarginLayoutParams pre;              
	        pre = (MarginLayoutParams)mCameraLayout.getLayoutParams();
	        if(mCameraview_width_height[0] != pre.width && 
	                mCameraview_width_height[1] != pre.height) 
            {
	            log( "camera-surfaceview is down. so swap()");
	            mIsNeedChangeView = true;
                swapView();
                mIsNeedChangeView = false;
                pre = (MarginLayoutParams)mCameraLayout.getLayoutParams();
                log( "mCameraLayout width =" + pre.width +",height= "+pre.height);
            }
		    
			mIsNearTop = true;			
		}
		else
		{
			mIsNearTop = false;
		}

		mIsAvatarStart = false;
		
		log( "------ mIsDisconnected = false -------");
//		mIsDisconnected = false;
	}
	public void invisilbleButton(int nIndex)
	{
		if(null != mIncallButton[nIndex] )
		{			
			mIncallButton[nIndex].setVisibility(View.INVISIBLE);
		}
	}

	public void visibleButton(int nIndex)
	{
		if(null != mIncallButton[nIndex] )
		{			
			mIncallButton[nIndex].setVisibility(View.VISIBLE);
		}
	}

	public void setTextButton(int nIndex, String txt)
	{
		if(null != mIncallButton[nIndex] )
		{			
			mIncallButton[nIndex].setText(txt);
		}
	}

	public void setBackgroundButton(int nIndex, boolean changed)
	{
		if(null != mIncallButton[nIndex] )
		{			
			if(changed)
				mIncallButton[nIndex].setBackgroundResource(R.drawable.vt_invideocall_dialpad_hold_button);
			else
			{
				boolean selected =  ((VTEventHandler.IncallBL)mEventHandler).getButtonState(nIndex);
				if(selected)
				{
					mIncallButton[nIndex].setBackgroundResource(R.drawable.vt_invideocall_on_button);
				}
				else
				{
					mIncallButton[nIndex].setBackgroundResource(R.drawable.vt_invideocall_button);
				}	
			}
		}
	}

	/**
	 * When a Call State is Incoming or Disconnected, do initialize button state. 
	 * */
	public void buttonInitialize()
	{
		//as a locale, adjust a font size. 
    	Locale locale = mContext.getResources().getConfiguration().locale;
//    	int dimenId = R.dimen.vt_dialpad_en_font_size;
    	boolean isKorean = false;
    	
    	//english: "en_US" , korean: "ko" or "ko_KR"
        if(getResources().getConfiguration().locale.toString().equals("ko_KR"))
        {
        	isKorean = true;
        }

		
		
		for(int i=0; i<INCALL_BUTTON_COUNT;i++){
			{
				mIncallButton[i].setOnClickListener(mEventHandler);
				mIncallButton[i].setEnabled(true);
			} 

			//adjust font size of dialpad and hold buttons
			if(isKorean)
			{
				if(i == BUTTON_PRIVATE_SHOW )
				{
					int nValue = Settings.System.getInt(mContext.getContentResolver(),
							Settings.System.VT_USE_PRIVATE, 0);
					//button text is "SHOW'
					if(nValue == 1)
					{
			        	mIncallButton[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
			        			getResources().getDimension(R.dimen.vt_dialpad_ko_font_size));	
					}
					else
					{
						mIncallButton[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
			        			getResources().getDimension(R.dimen.vt_dialpad_en_font_size));
					}
						
				}
				else if(i == BUTTON_DIALPAD || i == BUTTON_HOLD)
				{
		        	mIncallButton[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
		        			mContext.getResources().getDimension(R.dimen.vt_dialpad_ko_font_size));				
				}				
				else
				{
		        	mIncallButton[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
		        			mContext.getResources().getDimension(R.dimen.vt_dialpad_en_font_size));
				}
			}
			else
			{
	        	mIncallButton[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 
	        			mContext.getResources().getDimension(R.dimen.vt_dialpad_en_font_size));				
			}

			//default background 
			if(i == BUTTON_PRIVATE_SHOW || i == BUTTON_MUTE || i == BUTTON_SPEAKER || i == BUTTON_HOLD)
			{
				//default string - "PRIVATE"
				if(i == BUTTON_PRIVATE_SHOW)
				{
					mIncallButton[i].setText(mContext.getResources().getString(R.string.vt_Private));
				}
                
				if(i == BUTTON_HOLD)
				{
					mIncallButton[i].setText(mContext.getResources().getString(R.string.vt_Hold));
				}
				mIncallButton[i].setFocusable(false);
				mIncallButton[i].setBackgroundResource(R.drawable.vt_invideocall_button);
			}

			//default flag( isSelected)
			((VTEventHandler.IncallBL)mEventHandler).initButtonState();
			
		}	
	}

	public void updateButtonUI(int nIndex, boolean bSelected, String text)
	{
		if(null != mIncallButton[nIndex] )
		{
			if(text != null && text != "")
				mIncallButton[nIndex].setText(text);

			if(bSelected)
				mIncallButton[nIndex].setBackgroundResource(R.drawable.vt_invideocall_on_button);
			else
				mIncallButton[nIndex].setBackgroundResource(R.drawable.vt_invideocall_button);

			((VTEventHandler.IncallBL)mEventHandler).setButtonState(nIndex, bSelected);
		}
	}
		
	public void swapView()
	{
		//if callstate is dialing , then return.
		VTCallState callState = 
			VTAppStateManager.getInstance().getCallState();
		log( "------ callState = " + callState);
		
		if(!mIsNeedChangeView && (callState == VTCallState.DIALING || callState == VTCallState.ALERTING || IsCaptureMenu()))
		{
            log( "swapView(), callState = " + callState + " or IsCaptureMenu() = "
                    +IsCaptureMenu() + ",so return");
			return;
		}
        /*
		if(CaptureHandler != null)
			return;
		
*/
		log( "------ swapView() called! -------");
		
		//swapView start!
		android.view.ViewGroup.MarginLayoutParams far;
		far = (MarginLayoutParams)mRemoteLayout.getLayoutParams();
		android.view.ViewGroup.MarginLayoutParams pre;				
		pre = (MarginLayoutParams)mCameraLayout.getLayoutParams();				

		int orig_width = far.width;
		int orig_height = far.height;
        int orig_leftMargin = far.leftMargin;
        int orig_topMargin = far.topMargin;
        int orig_rightMargin = far.rightMargin;
        int orig_bottomMargin = far.bottomMargin;
        
		far.width = pre.width;
		far.height = pre.height;
		far.leftMargin = pre.leftMargin;
		far.topMargin = pre.topMargin;
		far.rightMargin = pre.rightMargin;
		far.bottomMargin = pre.bottomMargin;

		pre.width = orig_width;
		pre.height = orig_height;
		pre.leftMargin = orig_leftMargin;
		pre.topMargin = orig_topMargin;
		pre.rightMargin = orig_rightMargin;
		pre.bottomMargin = orig_bottomMargin;
		
		mRemoteLayout.setLayoutParams(far);				
		mCameraLayout.setLayoutParams(pre);
		
		mIsNearTop = !mIsNearTop;
		
		log( "------ swapView() end! -------");
	}
	
	/**
	 * whether avatarStart is true or false.
	 * @return mIsAvatarStart (if avatar setted then return true , otherwise fase) 
	 * */
	public boolean isShowAvatarStart()
	{
		return mIsAvatarStart;
	}
	
	/**
	 * whether nearSurfaceView is top or down
	 * @return mIsNearTop ((if Near-SurfaceView positions top then return true , otherwise fase)
	 * */
	public boolean isTopNearView()
	{
		return mIsNearTop;
	}

       public void onStartCaptureloading()
       {
            if (DBG) log("onStartCaptureloading");
            
            if(!(VTAppStateManager.getInstance().isCanCommand() && VTAppStateManager.getInstance().isActive()))
                    return;    
            
             isCaptureloading = true;
             mCount = 0;

             if(mIsAvatarStart)
                    preSendsubstitute = true;
             
             TimerTask  Task = new TimerTask(){
                    public void run(){
                            if (DBG) log("TimerTask: mCount="+mCount);

                            if(mCount <captureloadingImagePath.length - 1)
                            {
                            	boolean bResult;
                                   bResult = mVTApp.startImageSharinig(captureloadingImagePath[mCount]);

                                if(bResult)
                                     mCount++;
                            }
                            else
                            {
                                    mCount = 0;
                                    onCancelCaptureloading();
                                    ((InVideoCallScreen)mContext).onSendCaptureMessage();
                            }
                        }
            };

            capturetimer = new Timer();
            capturetimer.schedule(Task, 100, 1000);
       }
       
       public void onCreateFarCaptureButton()
       {         
             if (DBG) log("onCreateFarCaptureButton");
             mVTControlContainer.setVisibility(View.GONE);
/*
             if(isTopNearView())
             {
                    swapView();
                    mSwapMode = true;
             }
*/          
             if(mCaptureView == null)
            {
                mCaptureView  = ((ViewStub)findViewById(R.id.capture_button)).inflate();

                mCaptureButton[CAPTURE_VT_0] = (Button) mCaptureView.findViewById(R.id.capture_done);
                mCaptureButton[CAPTURE_VT_1]= (Button) mCaptureView.findViewById(R.id.capture_delete);
                mCaptureButton[CAPTURE_VT_2]  = (Button) mCaptureView.findViewById(R.id.capture_cancel);

                if(mCaptureEventHandler == null)
                {
                	mCaptureEventHandler = new VTEventHandler.CaptureBL(mContext,this);	   
                }
                for(int i=0; i<CAPTURE_BUTTON_COUNT;i++){
            	        mCaptureButton[i].setOnClickListener(mCaptureEventHandler);
            	        mCaptureButton[i].setEnabled(false);
                }   
                mCaptureButton[CAPTURE_VT_2].setEnabled(true);
            }
            else
            {
                mCaptureButton[CAPTURE_VT_0].setEnabled(false);
                mCaptureButton[CAPTURE_VT_1].setEnabled(false);
                mCaptureButton[CAPTURE_VT_2].setEnabled(true);
             }
            SetCaptureMenu(true);  

       }

       public void onCancelCaptureloading()
       {
            if (DBG) log("onCancelCaptureloading");

            if(capturetimer != null)
            {
               capturetimer.cancel();
               capturetimer = null;
            } 

            if(!(VTAppStateManager.getInstance().isCanCommand() && VTAppStateManager.getInstance().isActive()) 
                && canceltimer != null)
            { 
                canceltimer.cancel();    
                canceltimer = null;
                return;
            }
            
            //wait until engine state is been IMAGESHARING
             TimerTask  Task = new TimerTask(){
            	 public void run(){
                       if(VTAppStateManager.getInstance().isSharing())
	                {
	                    if (DBG) log("onCancelCaptureloading => Sharing Type is IMAGESHARING");

                           if(preSendsubstitute)
                           {                   
                                if (DBG) log("onCancelCaptureloading => Hold or subs image start");
                                if(mVTApp.isHold())
                                    mVTApp.startImageSharinig(holdImagePath);
                                else
                                    mVTApp.startImageSharinig(sendSubsImagePath);
                           }
                           else
	                        mVTApp.stopImageSharinig();

                           canceltimer.cancel();    
                           canceltimer = null;
	                    isCaptureloading = false;                  
	                }
	            }
              };

            canceltimer = new Timer();
            canceltimer.schedule(Task, 0, 100);
            //mLarge_small_imageview[3] = null;
       }
       
	 public void AfterCaptureButtonInitialize()
	 {
	        if (DBG) log("AfterCaptureButtonInitialize");

               mVTControlContainer.setVisibility(View.VISIBLE);
               SetCaptureMenu(false);            

               if(capturetimer != null)
                    capturetimer.cancel();  

               if(isCaptureloading)
                    onCancelCaptureloading();
/*         
              if(mSwapMode)
                  swapView();
*/
	 }
        private void makeCaptureloadingImage()
       {
		Bitmap mBitmap = null;
		Bitmap bm = null;

              mLarge_small_imageview[2] = null;

              bm = BitmapFactory.decodeFile(captureloadingImagePath[mCount - 1]);	
              
              if(isTopNearView())
              {
                    mLarge_small_imageview[2] = 
				(ImageView)mOutgoingCallInfo.findViewById(R.id.subsimage_large);
//                    mBitmap = Bitmap.createScaledBitmap(bm, 352, 288, true);
                    mBitmap = Bitmap.createScaledBitmap(bm, 394, 324, true);
              }
              else
              {
			mLarge_small_imageview[2] = 
				(ImageView)mOutgoingCallInfo.findViewById(R.id.subsimage_small);
                    mBitmap = Bitmap.createScaledBitmap(bm, 178, 144, true);
		}   			

              bm.recycle();
              bm = null;
		//BitmapFactory.Options options = new BitmapFactory.Options(); 
		//options.inSampleSize = 2;  
				
		mLarge_small_imageview[2].setImageBitmap(mBitmap);           
        }

	public void  onStartCapture(boolean bFarEnd)
	{
		if (DBG) log("onStartCapture");
		int index;
        	String capturing;    
		mFarEnd = bFarEnd;

        	capturing = getResources().getString(R.string.vt_capturing);
        	Toast.makeText((InVideoCallScreen)mContext, capturing, 2000).show();

		if(mFarEnd == false)
		{
			for(index=0;index<INCALL_BUTTON_COUNT;index++)  
                    {         
                      isEnableButton[index] = getEnableButton(index);
                      disableButton(index);
                    }
		}
		else
        		mCaptureButton[CAPTURE_VT_2].setEnabled(false);

        	capture_result = mVTApp.startCapture(bFarEnd, null);

		CaptureHandler  = new Handler();
		CaptureHandler.postDelayed(new Runnable(){		
			public void run() {
				int index;
        			String bResult; 

        			if(capture_result == CAPTURE_MSG_SUCCESS)          
        				bResult = getResources().getString(R.string.vt_success);
        			else if(capture_result == CAPTURE_MSG_MEMORY_FULL) 
        				bResult = getResources().getString(R.string.vt_memoryfull_full);
        			else if(capture_result == CAPTURE_MSG_NO_SD_CARD)
        				bResult = getResources().getString(R.string.vt_interSD_mount);
        			else             
        				bResult = getResources().getString(R.string.vt_fail);

        			Toast.makeText((InVideoCallScreen)mContext, bResult, 1000).show();
				//mVideoCallCard.AfterCaptureButtonInitialize();

				if(mFarEnd == true)
				{
					mCaptureButton[CAPTURE_VT_0].setEnabled(true);
					mCaptureButton[CAPTURE_VT_1].setEnabled(true);
					mCaptureButton[CAPTURE_VT_2].setEnabled(false);
				}
				else
				{
					for(index=0;index<INCALL_BUTTON_COUNT;index++)
                                   {            
                                          if(isEnableButton[index])
						        enableButton(index);
                                          else
                                                disableButton(index);
                                    }
				}
			}
        	},3000);
	}	
	
	public boolean IsCaptureMenu()
	{
		return mIsCaptureMenu;
	}

	public void SetCaptureMenu(boolean bSet)
	{
		mIsCaptureMenu = bSet;
	}
    
    /**
	 * Set a image at the camera(near) view in the dialing call-state.
	 * */
    private void setPhotoOnDialing() {
    	log( "setPhotoOnDialing() called!");
    	
    	if(mLarge_small_imageview[1] == null)
    	{
    		mLarge_small_imageview[1] = 
    			(ImageView)mOutgoingCallInfo.findViewById(R.id.subsimage_small);
    	}
    	else
    	{
    		//May be. The Active call-state event might be comes many times. 
    		boolean isShowReadyFarImage = 
    			(mLarge_small_imageview[1].getVisibility() == View.VISIBLE )? true:false;  

    		log( "mLarge_small_imageview[1] is show? =>  " + isShowReadyFarImage);
    		
    		if(isShowReadyFarImage)
    		{
    			log( "return setPhotoOnDialing() ");
    			return;
    		}
    	}
    	
    	mLarge_small_imageview[1].setImageBitmap(null);        
    }

    /** Helper function to display the resource in the imageview AND ensure its visibility.*/
    private static final void showBackImage(ImageView view, int resource) {
        if(view!= null)
        {
            view.setBackgroundResource(resource);
            view.setVisibility(View.VISIBLE);
        }
    }

    /** Helper function to display the drawable in the imageview AND ensure its visibility.*/
    private static final void showBackImage(ImageView view, Drawable drawable) {
        if(view!= null)
        {
            view.setBackgroundDrawable(drawable);
            view.setVisibility(View.VISIBLE);
        }
    }
    
	/**
	 * Hides the small imageview . at the active call-State. 
	 * */
	private void hidePhotoOnDialing()
	{
		log( "hidePhotoOnDialing() called!");
		if(mLarge_small_imageview[1] == null)
		{
			log( "return hidePhotoOnDialing() ");
			return;
		}
		
        //not null substitute image
        if(mIsAvatarStart)
        {
            log( "mIsAvatarStart is true so, we don't make [1] View.GONE.");
//            mLarge_small_imageview[0].setImageBitmap(null);
            return;
        }
        
		mLarge_small_imageview[1].setBackgroundDrawable(null);
		mLarge_small_imageview[1].setVisibility(View.GONE);			
	}
	
	/**
	 * When we received active call-state, show a ready progressbar to top large imageview.
	 * */
	private void showReadyProgressbar()
	{
		log( "showReadyProgressbar() called!");
		
		if(mLarge_small_imageview[0] == null)
		{
			mLarge_small_imageview[0] = 
				(ImageView)mOutgoingCallInfo.findViewById(R.id.subsimage_large);
		}
		else
		{
			log( "mLarge_small_imageview[0] is show? =>  " + 
					(mLarge_small_imageview[0].getVisibility() == View.VISIBLE));

			//May be. The Active call-state event might be comes many times.
			if(mReadyProgressBar != null &&
					mReadyProgressBar.getVisibility() == View.VISIBLE)
			{
				log( "mReadyProgressBar is show? =>  " + 
						(mReadyProgressBar.getVisibility() == View.VISIBLE));
				
				log( "return showReadyProgressbar() ");
				return;
			}
			
//			boolean isShowReadyProgressbar = 
//				(mLarge_small_imageview[0].getVisibility() == View.VISIBLE )? true:false;  
//
//			if(isShowReadyProgressbar)
//			{
//				return;
//			}
		}
		
		mLarge_small_imageview[0].setImageBitmap(null);
		mLarge_small_imageview[0].setBackgroundResource(R.drawable.vt_in_call_ready_big);
		
		mLarge_small_imageview[0].setVisibility(View.VISIBLE);
		
		if(mReadyProgressBar == null)
		{
			//inflate viewStub
		      ViewStub viewStub = (ViewStub)findViewById(R.id.readyProgress);
		      if(viewStub != null)
		      {
		    	  ((ViewStub)findViewById(R.id.readyProgress)).inflate();
		    	  mReadyProgressBar = (ProgressBar)findViewById(R.id.ProgressBar01);
		      }				
		}
		else
		{
			mReadyProgressBar.setVisibility(View.VISIBLE);
		}
	}
	
	public void hideReadyProgressbar()
	{
		if(VTAppStateManager.getInstance().isSharing() == false)
	        {
                    mIncallButton[BUTTON_HOLD].setEnabled(true);
	        }
     		    mIncallButton[BUTTON_DIALPAD].setEnabled(true);
            
		log( "hideReadyProgressbar() called!");
		if(mReadyProgressBar == null)
		{
			log( "return hideReadyProgressbar() ");
			return;
		}

		mReadyProgressBar.setVisibility(View.GONE);
		
	      //not null substitute image
        if(mIsAvatarStart)
        {
            log( "mIsAvatarStart is true so, we don't make [0] View.GONE.");
//            mLarge_small_imageview[0].setImageBitmap(null);
            return;
        }
        
        mLarge_small_imageview[0].setBackgroundDrawable(null);
        mLarge_small_imageview[0].setVisibility(View.GONE);	
	}
	
	/**
	 * Each time we try outgoing call, add surfaceviews(far,remote) to parent viewgroup.  
	 * */
	public void addSurfaceviews()
	{
        //if(!mIsFirstStart && mNearview == null && mFarview == null)
	    if(mRemoteStubContainer.getParent() == null && 
	            mCameraStubContainer.getParent() == null)
        {
            log( "addSurfaceviews()");
            
            //add the surfaceview container 
            mRemoteLayout.addView(mRemoteStubContainer);
            mCameraLayout.addView(mCameraStubContainer);   
            
            //settting a informations
            mNearview = (SurfaceView) mCameraStubContainer.findViewById(R.id.vt_camera);
            holderNearview = mNearview.getHolder();
            holderNearview.addCallback(this);
            holderNearview.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            holderNearview.setSizeFromLayout();
            mNearview.setVisibility(View.VISIBLE);
            
            mFarview = (SurfaceView) mRemoteStubContainer.findViewById(R.id.vt_remote);
            holderFarview = mFarview.getHolder();
            holderFarview.addCallback(this);
            holderFarview.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            holderFarview.setSizeFromLayout();
            mFarview.setVisibility(View.VISIBLE);
            
            //ADDed
            Message msg = mSurfaceHandler.obtainMessage(STATE_SURFACEVIEW_ADDED);
            mSurfaceHandler.sendMessage(msg);
            
        }
	    else
	    {
	        log( "mRemoteStubContainer's Parent != null or " +
	        		"mCameraStubContainer's Parent == null");
	    }
	}
	
	/**
	 * Make large surfaceview either Near or Far
	 * @param isNearSurfaceview - TRUE if the view move to top position is near surfaceview, or FALSE 
	 * */
	private void makeLargeSurfaceView(boolean isNearSurfaceview)
	{
	    //Make large a my Camera-surfaceview	    
	    if(isNearSurfaceview)
	    {
	        if(mIsNearTop == false){
	            swapView();
	        }	        
	    }
	    //Make large a someone remote-surfaceview  
	    else
	    {
	        if(mIsNearTop == true){
	            swapView();
	        }
	    }
	}
    
	public String getName()
	{
		return mSaveName;
	}
	
	public void engineCallbackCallstop()
	{
		log("engineDestroyCallback callstate : " + VTAppStateManager.getInstance().getCallState()
				+ ", enginestate" + VTAppStateManager.getInstance().getEngineState());	
        Message msg = mSurfaceHandler.obtainMessage(STATE_COMES_DISCONNECTED);
        mSurfaceHandler.sendMessage(msg);		
	}

        private static void startToggling(Call call, CallerInfo info, TextView mName) {
            if (!mIsToggling) {
                mCall = call;
                mCallerInfo = info;

                mIsToggling = true;
                mToggleValue = true;
                mNameDisplay = mName;
                mCnapName = info.cnapName;
                mPbName = info.name;
                mToggleHandler.sendEmptyMessageDelayed(TOGGLE_DISPLAY_EVENT, TOGGLE_DISPLAY_TIME);
            }
        }

        private static void displayToggling() {
            if ((mCall == null) || ((mCall != null) && !mCall.isRinging())) {
                stopToggling();
            } else {
                mToggleValue = !mToggleValue;
                if (mToggleValue)
                    mNameDisplay.setText(mCnapName);
                else
                    mNameDisplay.setText(mPbName);
                mToggleHandler.sendEmptyMessageDelayed(TOGGLE_DISPLAY_EVENT, TOGGLE_DISPLAY_TIME);
            }
        }

        private static void stopToggling() {
            if (mIsToggling) {
                mCall = null;
                mCallerInfo = null;

                mIsToggling = false;
                mToggleValue = false;
                mNameDisplay = null;
                mCnapName = null;
                mPbName = null;
                mToggleHandler.removeMessages(TOGGLE_DISPLAY_EVENT);
            }
        }    
}



