/*
 * Copyright (C) 2009 The Android Open Source Project
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

// LGE_MERGER_EXCUSE_CALL_UI_START
// jongwany.lee@lge.com START FUNCTION FOR CALL_UI
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
// jongwany.lee@lge.com END FUNCTION FOR CALL_UI
// LGE_MERGER_EXCUSE_CALL_UI_END

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone; //20100321 SlidingTab Start
import com.android.internal.telephony.Call.State;
//import com.android.internal.widget.RotarySelector;
import com.android.internal.widget.SlidingTab;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
//20100421 Incoming Animation Start
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.widget.ImageView;

//20100421 Incoming Animation End

import com.lge.config.StarConfig; //<!--//20100916 sumi920.kim@lge.com InCall-Recording [LGE_LAB1]	 -->				
/**
 * In-call onscreen touch UI elements, used on some platforms.
 *
 * This widget is a fullscreen overlay, drawn on top of the
 * non-touch-sensitive parts of the in-call UI (i.e. the call card).
 */
public class InCallTouchUi extends FrameLayout
        implements View.OnClickListener, SlidingTab.OnTriggerListener {
    private static final int IN_CALL_WIDGET_TRANSITION_TIME = 250; // in ms
    private static final String LOG_TAG = "InCallTouchUi";
    private static final boolean DBG = true; //hyojin.an 101020 (PhoneApp.DBG_LEVEL >= 2);

    /**
     * Reference to the InCallScreen activity that owns us.  This may be
     * null if we haven't been initialized yet *or* after the InCallScreen
     * activity has been destroyed.
     */
//20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
//20100804 jongwany.lee@lge.com attached it for CALL UI
    InCallMessageView mInCallMsg;
    private InCallScreen mInCallScreen;
    private View mMessageView;
    
    
    
    // Phone app instance
    private PhoneApp mApplication;

    // UI containers / elements
    private SlidingTab mIncomingCallWidget;  // UI used for an incoming call
    private View mInCallControls;  // UI elements while on a regular call
//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
	// 20100322 SilentIncoming Start
	private View mSilentIncomingCallWidget;
	private Button mAcceptButton;
	private Button mSendSMSButton;
	private Button mRejectButton;
	// 20100322 SilentIncoming End
    private Button mAddButton;
    private Button mMergeButton;

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
    private Button mSendButton;
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

    private Button mEndButton;
    private Button mDialpadButton;
    private ToggleButton mBluetoothButton;
    private ToggleButton mMuteButton;
    private ToggleButton mSpeakerButton;
    //<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]	 -->
    private Button 	mRecordButton;
    private Button 	mStopButton;
    //<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]	 -->						 
// LGE_RIL_SPEAKERPHONE_SUPPORT START
    /**
    *  mSpeakerphoneButtonForcedDisabled is true when RIL request to 
    *  turn on/off speakerphone was already sent to modem, but response is not
    *  received yet.
    */
    private boolean mSpeakerphoneButtonForcedDisabled = false;
// LGE_RIL_SPEAKERPHONE_SUPPORT END
    //
    
    //
    private Button mNewAccount;
    private Button mAddAccount;
    private Button mVoiceCall;
    private Button mVideoCall;
    private Button mMessage;
// LGE_CALL_TRANSFER START
    private View mTransferButtonContainer;
    private ImageButton mTransferButton;
    private TextView mTransferButtonLabel;
// LGE_CALL_TRANSFER END 
    
    private View mHoldButtonContainer;
    private ImageButton mHoldButton;
    private TextView mHoldButtonLabel;
    private View mSwapButtonContainer;
    private ImageButton mSwapButton;
    private TextView mSwapButtonLabel;
    private View mCdmaMergeButtonContainer;
    private ImageButton mCdmaMergeButton;
    //
    private Drawable mHoldIcon;
    private Drawable mUnholdIcon;
    private Drawable mShowDialpadIcon;
    private Drawable mHideDialpadIcon;

    // Time of the most recent "answer" or "reject" action (see updateState())
    private long mLastIncomingCallActionTime;  // in SystemClock.uptimeMillis() time base

    // Overall enabledness of the "touch UI" features
    private boolean mAllowIncomingCallTouchUi;
    private boolean mAllowInCallTouchUi;

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
    private boolean mAllowDialpadSendKey;
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]


//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
 // 20100322 SilentIncoming Start
	private boolean mSilentIncoming = false;
	// 20100322 SilentIncoming End
	// 20100421 Incoming Animation Start
	private ImageView aniAccept, aniSilent, aniReject;
	private AnimationDrawable arrowAniAccept, arrowAniSilent, arrowAniReject;
	private TextView txtAccept, txtSilent, txtReject;
	// 20100421 Incoming Animation End
	// 20100425 yongwoon.choi@lge.com Second Incoming UI [START_LGE]
	private boolean mSecondIncomingCall = true;
	// 20100425 yongwoon.choi@lge.com Second Incoming UI [END_LGE]

	// 20100521 yongwoon.choi@lge.com the bug fix for Incall screen blank when
	// it selected silent.
	private boolean requestSilentAction = false;
	private boolean mShowIncomingCallControls;
	private boolean mNameInContact = false;

	// 20101019 sumi920.kim@lge.com Receive HDVideoCall Event From HDVideoCall App [START_LGE_LAB1]
	private InCallReceiver inCallReceiver;
	private boolean 	mHDVisibilty = false;
	// 20101019 sumi920.kim@lge.com Receive HDVideoCall Event From HDVideoCall App [END_LGE_LAB1]
	
    public boolean isNameInContact() {
		return mNameInContact;
	}

	public void setNameInContact(boolean mNameInContact) {
		this.mNameInContact = mNameInContact;
	}

	public InCallTouchUi(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (DBG) log("InCallTouchUi constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);

        // Inflate our contents, and add it (to ourself) as a child.
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(
                R.layout.incall_touch_ui,  // resource
                this,                      // root
                true);

        mApplication = PhoneApp.getInstance();

        // The various touch UI features are enabled on a per-product
        // basis.  (These flags in config.xml may be overridden by
        // product-specific overlay files.)

        mAllowIncomingCallTouchUi = getResources().getBoolean(R.bool.allow_incoming_call_touch_ui);
        if (DBG) log("- incoming call touch UI: "
                     + (mAllowIncomingCallTouchUi ? "ENABLED" : "DISABLED"));
        mAllowInCallTouchUi = getResources().getBoolean(R.bool.allow_in_call_touch_ui);
        if (DBG) log("- regular in-call touch UI: "
                     + (mAllowInCallTouchUi ? "ENABLED" : "DISABLED"));

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
        if(StarConfig.OPERATOR.equals("SKT")){
           mAllowDialpadSendKey = getResources().getBoolean(R.bool.support_in_call_dialpad_sendkey);
           if (DBG) log("- dialpadSendKey in-call touch UI: "
                        + (mAllowDialpadSendKey ? "ENABLED" : "DISABLED"));
	}
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]
        mAllowInCallTouchUi = true;
    }

    void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
		// LGE_MERGE_S
		// 20100728 jongwany.lee sms message view setting
		mInCallMsg = (InCallMessageView) findViewById(R.id.messageView);
		if (mInCallScreen != null) {

			mInCallMsg.initMessageView(this);

		}
		// LGE_MERGE_E
		//20101019 sumi920.kim@lge.com Receive HDVideoCall Enable Event From HDVideoCall App. [START_LGE_LAB1]	 -->	
		//mHDVisibilty = false;
		inCallReceiver.setInCallTouchUi(this);
    }
    
    //20100723 yongwoon.choi@lge.com Lock/UnLock Incoming Scenario [START_LGE]
    public void setInCallMessageViewInstance() {
    	if (mInCallScreen != null)
    	{
    		mInCallMsg = (InCallMessageView) findViewById(R.id.messageView);
    		mInCallMsg.initMessageView(this);
    	}
    }
    //20100723 yongwoon.choi@lge.com Lock/UnLock Incoming Scenario [END_LGE]

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (DBG) log("InCallTouchUi onFinishInflate(this = " + this + ")...");
        
// Look up the various UI elements.
// 20100421 Incoming Animation Start
//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
		aniAccept = (ImageView) findViewById(R.id.arrowAniAccept);
		aniSilent = (ImageView) findViewById(R.id.arrowAniSilent);
		aniReject = (ImageView) findViewById(R.id.arrowAniReject);

		aniAccept.setBackgroundResource(R.anim.incoming_accept_ani);
		arrowAniAccept = (AnimationDrawable) aniAccept.getBackground();

		aniSilent.setBackgroundResource(R.anim.incoming_silent_ani);
		arrowAniSilent = (AnimationDrawable) aniSilent.getBackground();

		aniReject.setBackgroundResource(R.anim.incoming_reject_ani);
		arrowAniReject = (AnimationDrawable) aniReject.getBackground();

		txtAccept = (TextView) findViewById(R.id.textSlidingAccept);
		txtSilent = (TextView) findViewById(R.id.textSlidingSilent);
		txtReject = (TextView) findViewById(R.id.textSlidingReject);
		// 20100421 Incoming Animation End
        
     
		mSecondIncomingCall = true;
        mIncomingCallWidget = (SlidingTab) findViewById(R.id.incomingCallWidget);
        mIncomingCallWidget.setLeftTabResources(
                R.drawable.ic_jog_dial_answer,
                com.android.internal.R.drawable.jog_tab_target_green,
                com.android.internal.R.drawable.jog_tab_bar_left_answer,
                com.android.internal.R.drawable.jog_tab_left_answer
                );
        mIncomingCallWidget.setRightTabResources(
                R.drawable.ic_jog_dial_decline,
                com.android.internal.R.drawable.jog_tab_target_red,
                com.android.internal.R.drawable.jog_tab_bar_right_decline,
                com.android.internal.R.drawable.jog_tab_right_decline
                );

		// For now, we only need to show two states: answer and decline.
		mIncomingCallWidget.setLeftHintText(R.string.slide_to_answer_hint);

//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
		// LGE_MERGE_S
		// [LGE_CALL_UI]kyounghee.choi 2010.05.11 -- add silent call option
		// mIncomingCallWidget.setRightHintText(R.string.slide_to_decline_hint);
		// //original_code

//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> answer and decline [START_LGE_LAB]
        mIncomingCallWidget.setLeftHintText(R.string.slide_to_answer_hint);
//20101007 wonho.moon@lge.com <mailto:wonho.moon@lge.com> answer and decline [END_LGE_LAB]

		mIncomingCallWidget.setRightHintText(R.string.slide_to_decline_hint);
		// LGE_MERGE_E
        mIncomingCallWidget.setOnTriggerListener(this);

        // Container for the UI elements shown while on a regular call.
        mInCallControls = findViewById(R.id.inCallControls);
//20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
        mMessageView = findViewById(R.id.messageView);

        // Regular (single-tap) buttons, where we listen for click events:
        // Main cluster of buttons:
        mAddButton = (Button) mInCallControls.findViewById(R.id.addButton);
        mAddButton.setOnClickListener(this);
        mMergeButton = (Button) mInCallControls.findViewById(R.id.mergeButton);
        mMergeButton.setOnClickListener(this);

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
        mSendButton = (Button) mInCallControls.findViewById(R.id.sendButton);
        mSendButton.setVisibility(View.GONE);
        if(StarConfig.OPERATOR.equals("SKT")){
        	mSendButton.setOnClickListener(this);
	}
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

        mEndButton = (Button) mInCallControls.findViewById(R.id.endButton);
        mEndButton.setOnClickListener(this);
        mDialpadButton = (Button) mInCallControls.findViewById(R.id.dialpadButton);
        mDialpadButton.setOnClickListener(this);
        mBluetoothButton = (ToggleButton) mInCallControls.findViewById(R.id.bluetoothButton);
        mBluetoothButton.setOnClickListener(this);
        mMuteButton = (ToggleButton) mInCallControls.findViewById(R.id.muteButton);
        mMuteButton.setOnClickListener(this);
        mSpeakerButton = (ToggleButton) mInCallControls.findViewById(R.id.speakerButton);
        mSpeakerButton.setOnClickListener(this);

        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]   -->  
        mRecordButton = (Button) mInCallControls.findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(this);
        mStopButton = (Button) mInCallControls.findViewById(R.id.stopButton);
        mStopButton.setOnClickListener(this);
        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]	 -->						 

        // End Call View button
        mNewAccount = (Button) mInCallControls.findViewById(R.id.newAccount);
        mNewAccount.setOnClickListener(this);
        mAddAccount = (Button) mInCallControls.findViewById(R.id.addAccount);
        mAddAccount.setOnClickListener(this);
        mVoiceCall = (Button) mInCallControls.findViewById(R.id.voiceCall);
        mVoiceCall.setOnClickListener(this);
        mVideoCall = (Button) mInCallControls.findViewById(R.id.videoCall);
        mVideoCall.setOnClickListener(this);
        mMessage = (Button) mInCallControls.findViewById(R.id.message);
        mMessage.setOnClickListener(this);
        //
        
        // Upper corner buttons:
        mHoldButtonContainer = mInCallControls.findViewById(R.id.holdButtonContainer);
        mHoldButton = (ImageButton) mInCallControls.findViewById(R.id.holdButton);
        mHoldButton.setOnClickListener(this);
        mHoldButtonLabel = (TextView) mInCallControls.findViewById(R.id.holdButtonLabel);
        //
        mSwapButtonContainer = mInCallControls.findViewById(R.id.swapButtonContainer);
        mSwapButton = (ImageButton) mInCallControls.findViewById(R.id.swapButton);
        mSwapButton.setOnClickListener(this);
        mSwapButtonLabel = (TextView) mInCallControls.findViewById(R.id.swapButtonLabel);
// LGE_CALL_TRANSFER START
        mTransferButtonContainer = mInCallControls.findViewById(R.id.transferButtonContainer);
        mTransferButton = (ImageButton) mInCallControls.findViewById(R.id.transferButton);
        mTransferButton.setOnClickListener(this);
        mTransferButtonLabel = (TextView) mInCallControls.findViewById(R.id.transferButtonLabel);
// LGE_CALL_TRANSFER END
        if (PhoneApp.getInstance().phone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
            // In CDMA we use a generalized text - "Manage call", as behavior on selecting
            // this option depends entirely on what the current call state is.
            mSwapButtonLabel.setText(R.string.onscreenManageCallsText);
        } else {
            mSwapButtonLabel.setText(R.string.onscreenSwapCallsText);
        }
        //
        mCdmaMergeButtonContainer = mInCallControls.findViewById(R.id.cdmaMergeButtonContainer);
        mCdmaMergeButton = (ImageButton) mInCallControls.findViewById(R.id.cdmaMergeButton);
        mCdmaMergeButton.setOnClickListener(this);

//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
// 20100322 SilentIncoming Start
		mSilentIncomingCallWidget = findViewById(R.id.silentIncomingWidget);
		mAcceptButton = (Button) mSilentIncomingCallWidget.findViewById(R.id.btn_accept);
		mAcceptButton.setOnClickListener(this);
		mSendSMSButton = (Button) mSilentIncomingCallWidget.findViewById(R.id.btn_send_sms);
		mSendSMSButton.setOnClickListener(this);
		mRejectButton = (Button) mSilentIncomingCallWidget.findViewById(R.id.btn_reject);
		mRejectButton.setOnClickListener(this);
		// 20100322 SilentIncoming End
		
        // Add a custom OnTouchListener to manually shrink the "hit
        // target" of some buttons.
        // (We do this for a few specific buttons which are vulnerable to
        // "false touches" because either (1) they're near the edge of the
        // screen and might be unintentionally touched while holding the
        // device in your hand, or (2) they're in the upper corners and might
        // be touched by the user's ear before the prox sensor has a chance to
        // kick in.)
		
        /*View.OnTouchListener smallerHitTargetTouchListener = new SmallerHitTargetTouchListener();
        mAddButton.setOnTouchListener(smallerHitTargetTouchListener);
        mMergeButton.setOnTouchListener(smallerHitTargetTouchListener);
        mDialpadButton.setOnTouchListener(smallerHitTargetTouchListener);
        mBluetoothButton.setOnTouchListener(smallerHitTargetTouchListener);
        mSpeakerButton.setOnTouchListener(smallerHitTargetTouchListener);
        mHoldButton.setOnTouchListener(smallerHitTargetTouchListener);
        mSwapButton.setOnTouchListener(smallerHitTargetTouchListener);
        mCdmaMergeButton.setOnTouchListener(smallerHitTargetTouchListener);
        mSpeakerButton.setOnTouchListener(smallerHitTargetTouchListener);*/

        // Icons we need to change dynamically.  (Most other icons are specified
        // directly in incall_touch_ui.xml.)
        mHoldIcon = getResources().getDrawable(R.drawable.ic_in_call_touch_round_hold);
        mUnholdIcon = getResources().getDrawable(R.drawable.ic_in_call_touch_round_unhold);
        mShowDialpadIcon = getResources().getDrawable(R.drawable.ic_in_call_touch_dialpad);
        mHideDialpadIcon = getResources().getDrawable(R.drawable.ic_in_call_touch_dialpad_close);
    }

    /**
     * Updates the visibility and/or state of our UI elements, based on
     * the current state of the phone.
     */
//20100804 jongwany.lee@lge.com attached it for CALL UI
    void updateState(Phone phone, boolean inCallControl_GONE_Flage) {
        if (DBG) log("updateState(" + phone + ")...");

        if (mInCallScreen == null) {
            log("- updateState: mInCallScreen has been destroyed; bailing out...");
            return;
        }
//20100804 jongwany.lee@lge.com attached it for CALL UI
        final CallNotifier notifier = PhoneApp.getInstance().notifier; // VT_AHJ
        
        Phone.State state = phone.getState();  // IDLE, RINGING, or OFFHOOK
        if (DBG) log("- updateState: phone state is " + state);

        boolean showIncomingCallControls = false;
        boolean showInCallControls = false;
//20100804 jongwany.lee@lge.com attached it for CALL UI
		// 20100322 SilentIncoming Start
		boolean showSilentIncomingCallControls = false;
		// 20100322 SilentIncoming End

		//20101026 sumi920.kim@lge.com porting [LGE_LAB1]
		// LGE_UPDATE_S sanghoon.roh@lge.com 2010/06/03 ignore the excuse message for the private number
		mSendSMSButton.setVisibility(View.INVISIBLE);
		// LGE_UPDATE_E sanghoon.roh@lge.com 2010/06/03 ignore the excuse message for the private number


        if (state == Phone.State.RINGING) {
            // A phone call is ringing *or* call waiting.
            if (mAllowIncomingCallTouchUi) {
                // Watch out: even if the phone state is RINGING, it's
                // possible for the ringing call to be in the DISCONNECTING
                // state.  (This typically happens immediately after the user
                // rejects an incoming call, and in that case we *don't* show
                // the incoming call controls.)
                final Call ringingCall = phone.getRingingCall();
		//20101108 sumi920.kim@lge.com Alerting -> reject button -> disappear Accept/SMS/Reject Button in disconnecting  [LGE_LAB1]
		//if (ringingCall.getState().isAlive()
		if (ringingCall.getState().isAlive() || ringingCall.getState() == Call.State.DISCONNECTING ) {
			/*if (DBG) log("- updateState: RINGING!  Showing incoming call controls...");
                    showIncomingCallControls = true;
                }

                // Ugly hack to cover up slow response from the radio:
                // if we attempted to answer or reject an incoming call
                // within the last 500 msec, *don't* show the incoming call
                // UI even if the phone is still in the RINGING state.
                long now = SystemClock.uptimeMillis();
                if (now < mLastIncomingCallActionTime + 500) {
                    log("updateState: Too soon after last action; not drawing!");
                    showIncomingCallControls = false;
                }

                // TODO: UI design issue: if the device is NOT currently
                // locked, we probably don't need to make the user
                // double-tap the "incoming call" buttons.  (The device
                // presumably isn't in a pocket or purse, so we don't need
                // to worry about false touches while it's ringing.)
                // But OTOH having "inconsistent" buttons might just make
                // it *more* confusing.
            }
        } else {
            if (mAllowInCallTouchUi) {
                // Ok, the in-call touch UI is available on this platform,
                // so make it visible (with some exceptions):
                if (mInCallScreen.okToShowInCallTouchUi()) {
                    showInCallControls = true;
                } else {
                    if (DBG) log("- updateState: NOT OK to show touch UI; disabling...");
                }
            }
        }

        if (showInCallControls) {
            updateInCallControls(phone);
        }

        if (showIncomingCallControls && showInCallControls) {
            throw new IllegalStateException(
                "'Incoming' and 'in-call' touch controls visible at the same time!");
        }

        if (showIncomingCallControls) {
            showIncomingCallWidget();
        } else {
            hideIncomingCallWidget();
        }

        mInCallControls.setVisibility(showInCallControls ? View.VISIBLE : View.GONE);
// LGE_CALL_DEFLECTION START
        mDeflectionButtonContainer.setVisibility(
                showIncomingCallControls ? View.VISIBLE : View.GONE);
// LGE_CALL_DEFLECTION END 

        // TODO: As an optimization, also consider setting the visibility
        // of the overall InCallTouchUi widget to GONE if *nothing at all*
        // is visible right now.
    }*/

                	/*
					 * if (DBG)log(
					 * "- updateState: RINGING!  Showing incoming call controls..."
					 * ); // 20100322 SilentIncoming Start //
					 * showIncomingCallControls = true;
					 * 
					 * // 20100521 yongwoon.choi@lge.com the bug fix for Incall
					 * // screen blank when it selected silent.[Start] // if
					 * (PhoneUtils.getAudioMode() == PhoneUtils.AUDIO_IDLE) //
					 * //20100409 Silent call change if (requestSilentAction ==
					 * true) // 20100521 yongwoon.choi@lge.com the bug fix for
					 * Incall // screen blank when it selected silent.[End] {
					 * showIncomingCallControls = false;
					 * showSilentIncomingCallControls = true;
					 * 
					 * } else { showIncomingCallControls = true;
					 * showSilentIncomingCallControls = false; } // 20100322
					 * SilentIncoming End }
					 * 
					 * // Ugly hack to cover up slow response from the radio: //
					 * if we attempted to answer or reject an incoming call //
					 * within the last 500 msec, *don't* show the incoming call
					 * // UI even if the phone is still in the RINGING state.
					 */
//20100804 jongwany.lee@lge.com attached it for UNLOCK , LOCK WIDGET
					if (DBG)
						log("- updateState: RINGING!  Showing incoming call controls...");

					// LGE_MERGE_S
					// 20100708 withwind.park
					if (((KeyguardManager) mInCallScreen.getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode()) {
						// is Lock screen mode
						log("- updateState: is Lock screen mode...");
						showIncomingCallControls = true;
						showSilentIncomingCallControls = false;

						mInCallMsg.setMode(InCallMessageView.MODE_LOCK);

						if (PhoneApp.getInstance().getCallFMCState() ==  2) // LGE_FMC
							mInCallMsg.updateState(InCallMessageView.ST_HIDE);
						else {
							Connection c = ringingCall.getEarliestConnection();
							if (c != null) {
								String number = c.getAddress();
								if (number == null || number.equals("+") || number.equals("")) {
									mInCallMsg.flickBtn.setVisibility(INVISIBLE);
								} else {
									mInCallMsg.setMode(InCallMessageView.MODE_LOCK);
									mInCallMsg.updateState(InCallMessageView.ST_LOCK_VIEW);
									mInCallMsg.flickBtn.setVisibility(VISIBLE);

								}
							}
						}
					} else {
						// is unLock screen mode
						log("- updateState: is unLock screen mode...");
						Connection c = ringingCall.getEarliestConnection();
						String number = c.getAddress();
						
							if (c != null && c.getNumberPresentation() == Connection.PRESENTATION_ALLOWED){                  
                                                 if(PhoneApp.getInstance().getCallFMCState() == 2) //LGE_FMC
                                                	        mSendSMSButton.setVisibility(View.INVISIBLE);
                                                 else if(number == null || number.equals("+") || number.equals("") ){
                 									mSendSMSButton.setVisibility(View.INVISIBLE);
                 								}else
                                                    mSendSMSButton.setVisibility(View.VISIBLE);
                                                 
						// LGE_UPDATE_E sanghoon.roh@lge.com 2010/06/03 ignore
						// the excuse message for the private number
							}
						showIncomingCallControls = false;
						showSilentIncomingCallControls = true;
						mInCallMsg.setMode(InCallMessageView.MODE_UNLOCK);
						mInCallMsg.updateState(InCallMessageView.ST_HIDE);
	
					}
					
							
					// LGE_MERGE_E

					
					//20101026 sumi920.kim@lge.com porting [START_LGE_LAB1]
					// LGE_UPDATE_S sanghoon.roh@lge.com 2010/06/03 ignore the excuse message for the private number
					Connection c = ringingCall.getEarliestConnection();
					if (c != null && c.getNumberPresentation() != Connection.PRESENTATION_ALLOWED)
					{
						mInCallMsg.setMode(InCallMessageView.MODE_UNLOCK);
						//mSendSMSButton.setVisibility(View.GONE);
						mInCallMsg.updateState(InCallMessageView.ST_HIDE);
					}
					//20101026 sumi920.kim@lge.com porting [END_LGE_LAB1]

					/*
					 * prev code in 20100707 // LGE_MERGE_S //20100322
					 * SilentIncoming Start from HUB(P950)
					 * //showIncomingCallControls = true;//original_code
					 * 
					 * //LGE_S kim.jungtae 20100510 //if
					 * (PhoneUtils.getAudioMode() == PhoneUtils.AUDIO_IDLE)
					 * //20100409 Silent call change if (requestSilentAction ==
					 * true) //LGE_S kim.jungtae 20100510 { // LGE_UPDATE_S
					 * sanghoon.roh@lge.com 2010/06/03 ignore the excuse message
					 * for the private number Connection c =
					 * ringingCall.getEarliestConnection(); if (c != null &&
					 * c.getNumberPresentation() ==
					 * Connection.PRESENTATION_ALLOWED)
					 * mSendSMSButton.setVisibility(View.VISIBLE); //
					 * LGE_UPDATE_E sanghoon.roh@lge.com 2010/06/03 ignore the
					 * excuse message for the private number
					 * 
					 * showIncomingCallControls = false;
					 * showSilentIncomingCallControls = true; } else {
					 * showIncomingCallControls = true;
					 * showSilentIncomingCallControls = false; }
					 */

					if (DBG)
						log("- updateState: requestSilentAction =  " + requestSilentAction);
					if (DBG)
						log("- updateState: showIncomingCallControls =  " + showIncomingCallControls);
					if (DBG)
						log("- updateState: showSilentIncomingCallControls =  " + showSilentIncomingCallControls);
					// 20100322 SilentIncoming End
					// LGE_MERGE_E
				}
				long now = SystemClock.uptimeMillis();
				if (now < mLastIncomingCallActionTime + 500) {
					log("updateState: Too soon after last action; not drawing!");
					showIncomingCallControls = false;
				}

                              //Only FMC Call is disconnected
                              if(PhoneApp.getInstance().getCallFMCState() == 3)  //LGE_FMC
                              {
                                    log("updateState: FMC State initialize~~!!!!");
                                    Connection c = ringingCall.getEarliestConnection();
                                    String number = c.getAddress();
                                    
                                    showIncomingCallControls = false;
                                    showSilentIncomingCallControls = true;
                                    
                                    if (!(number == null && number.equals("+") && number.equals(""))) {
				            mSendSMSButton.setVisibility(View.VISIBLE);
                                    }
                                    PhoneApp.getInstance().setCallFMCState(1);
                              }
                              
				// TODO: UI design issue: if the device is NOT currently
				// locked, we probably don't need to make the user
				// double-tap the "incoming call" buttons. (The device
				// presumably isn't in a pocket or purse, so we don't need
				// to worry about false touches while it's ringing.)
				// But OTOH having "inconsistent" buttons might just make
				// it *more* confusing.
			}
		} else {
			//20101026 sumi920.kim@lge.com porting [LGE_LAB1]
			// 20100709 withwind.park sliding sms control
			mInCallMsg.updateState(InCallMessageView.ST_HIDE);
			// 20100709 withwind.park sliding sms control
			// LGE_MERGE_E

			if (mAllowInCallTouchUi) {
				// Ok, the in-call touch UI is available on this platform,
				// so make it visible (with some exceptions):
                if (mInCallScreen.okToShowInCallTouchUi() || mInCallScreen.isEndCallState()) {
//20100804 jongwany.lee@lge.com attached it for CALL UI
					if (!PhoneApp.mIsVideoCall && !inCallControl_GONE_Flage)
						showInCallControls = true;
				} else {
					if (DBG)
						log("- updateState: NOT OK to show touch UI; disabling...");
				}
			}
			mInCallScreen.updateSlidingTabHint(0, 0);	
		}
		
//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
		mShowIncomingCallControls = showIncomingCallControls;

		if (showInCallControls) {
			updateInCallControls(phone);
		}

		if (showIncomingCallControls && showInCallControls) {
			throw new IllegalStateException("'Incoming' and 'in-call' touch controls visible at the same time!");
		}
		// 20100425 yongwoon.choi@lge.com Second Incoming UI [START_LGE]
		if (showIncomingCallControls) {
                     log("- updateState: mSecondIncomingCall = true...");
			if (mApplication.phone.getRingingCall().getState() == Call.State.WAITING  ) {
				mSecondIncomingCall = true;
				SetIncomingWidget();
			} else {
				mSecondIncomingCall = true;
				SetIncomingWidget();
			}
		}
	    if (showIncomingCallControls) {
            showIncomingCallWidget();
        } else {
            hideIncomingCallWidget();
        }
//20100804 jongwany.lee@lge.com attached it for CALL UI
// 20100425 yongwoon.choi@lge.com Second Incoming UI [END_LGE]
		mIncomingCallWidget.setVisibility(showIncomingCallControls ? View.VISIBLE : View.GONE);

                if(PhoneApp.getInstance().getCallFMCState() == 2)  //LGE_FMC
			mInCallMsg.updateState(InCallMessageView.ST_HIDE); 
                else
		{
		
		//20101026 sumi920.kim@lge.com porting [START_LGE_LAB1]
		// LGE_UPDATE_S sanghoon.roh@lge.com 2010/06/03 ignore the excuse message for the private number
		
		final Call ringingCall = phone.getRingingCall();
		Connection c = ringingCall.getEarliestConnection();
		if (c != null && c.getNumberPresentation() != Connection.PRESENTATION_ALLOWED)
		{
			if(mInCallMsg.getMode() == InCallMessageView.ST_LOCK_VIEW)
				mMessageView.setVisibility(View.GONE);
			else 
				mMessageView.setVisibility(showIncomingCallControls ? View.VISIBLE : View.GONE);
		}
		else //20101026 sumi920.kim@lge.com porting [END_LGE_LAB1]
		
                    mMessageView.setVisibility(showIncomingCallControls ? View.VISIBLE : View.GONE);
		}              
//		if (showIncomingCallControls == true) {
//			((InCallMessageView) mMessageView).updateState(InCallMessageView.ST_INIT);
//		}
		// mMessageView.setVisibility(View.VISIBLE );
		// ((InCallMessageView)mMessageView).updateState(InCallMessageView.ST_INIT);

		mInCallControls.setVisibility(showInCallControls ? View.VISIBLE : View.GONE);

		// 20100322 SilentIncoming Start
		mSilentIncomingCallWidget.setVisibility(showSilentIncomingCallControls ? View.VISIBLE : View.GONE);
		// 20100322 SilentIncoming End
		// 20100421 Incoming Animation Start
		bottomTextDisplayControl(showIncomingCallControls);
		
		
		// 20100421 Incoming Animation End
		// Call deflection button removed from silentIncomingWidget and added in
		// the right side of the main screen for incoming calls

		// TODO: As an optimization, also consider setting the visibility
		// of the overall InCallTouchUi widget to GONE if *nothing at all*
		// is visible right now.
	}
          	
                	
                	
                	
    // View.OnClickListener implementation
    public void onClick(View view) {
        int id = view.getId();
        if (DBG) log("onClick(View " + view + ", id " + id + ")...");

        switch (id) {
		case R.id.addButton:
		case R.id.mergeButton:
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
            case R.id.sendButton:
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

		case R.id.endButton:
		case R.id.dialpadButton:
		case R.id.bluetoothButton:
		case R.id.muteButton:
		case R.id.speakerButton:
		case R.id.holdButton:
		case R.id.swapButton:
		case R.id.cdmaMergeButton:
//20100804 jongwany.lee@lge.com attached it for UNLOCK WIDGET
// 20100322 SilentIncoming Start
		case R.id.btn_accept:
//		case R.id.btn_send_sms: //<!--[sumi920.kim@lge.com] 2010.10.01	 LAB1_CallUI --> Change ExcuseMessage
		case R.id.btn_reject:
// 20100322 SilentIncoming End
// LGE_CALL_TRANSFER START
//		case R.id.transferButton:
			// LGE_CALL_TRANSFER END
// END_CALL Button Event START
        case R.id.newAccount:
        case R.id.addAccount:
        case R.id.voiceCall:
        case R.id.videoCall:
        case R.id.message:
        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]   -->
        case R.id.recordButton :
        case R.id.stopButton :
        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]   -->    			
// END_CALL Button Event END            	
		// Clicks on the regular onscreen buttons get forwarded
		// straight to the InCallScreen.
            mInCallScreen.handleOnscreenButtonClick(id);
            break;
        //<!--[sumi920.kim@lge.com] 2010.10.01	 LAB1_CallUI --> Change ExcuseMessage [START_LGE] -->
        case R.id.btn_send_sms :
    		if(id == R.id.btn_send_sms)
    		{
    			mInCallMsg.updateState(InCallMessageView.ST_UNLOCK_VIEW);
    			handleSilentCall();
    			return;
    		}
        break;
       	//<!--[sumi920.kim@lge.com] 2010.10.01  LAB1_CallUI --> Change ExcuseMessage [END_LGE] -->			 
            default:
                Log.w(LOG_TAG, "onClick: unexpected click: View " + view + ", id " + id);
                break;
        }
    }

    /**
     * Updates the enabledness and "checked" state of the buttons on the
     * "inCallControls" panel, based on the current telephony state.
     */
    void updateInCallControls(Phone phone) {
        int phoneType = phone.getPhoneType();
        // Note we do NOT need to worry here about cases where the entire
        // in-call touch UI is disabled, like during an OTA call or if the
        // dtmf dialpad is up.  (That's handled by updateState(), which
        // calls InCallScreen.okToShowInCallTouchUi().)
        //
        // If we get here, it *is* OK to show the in-call touch UI, so we
        // now need to update the enabledness and/or "checked" state of
        // each individual button.
        //

        // The InCallControlState object tells us the enabledness and/or
        // state of the various onscreen buttons:
        InCallControlState inCallControlState = mInCallScreen.getUpdatedInCallControlState();

        // "Add" or "Merge":
        // These two buttons occupy the same space onscreen, so only
        // one of them should be available at a given moment.
        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]	 -->
        if(StarConfig.COUNTRY.equals("KR") && !PhoneApp.mIsVideoCall)
        {
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
           if ( mAllowDialpadSendKey && mInCallScreen.isDialerOpened()){
                mRecordButton.setVisibility(View.GONE);
                mStopButton.setVisibility(View.GONE);
                mAddButton.setVisibility(View.GONE);
                mMergeButton.setVisibility(View.GONE);
                mSendButton.setVisibility(View.VISIBLE);
            }
           else{
                mSendButton.setVisibility(View.GONE);            
                updateRecAreaCtrl();
            }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]
        }
        else
        //<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]	 -->
        {
        if (inCallControlState.canAddCall && !mInCallScreen.isEndCallState()) {
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
            mAddButton.setVisibility(View.VISIBLE);
            mAddButton.setEnabled(true);
            mMergeButton.setVisibility(View.GONE);

            if(StarConfig.OPERATOR.equals("SKT")){
                mSendButton.setVisibility(View.GONE);
            }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

        } else if (inCallControlState.canMerge && !mInCallScreen.isEndCallState()) {
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                // In CDMA "Add" option is always given to the user and the
                // "Merge" option is provided as a button on the top left corner of the screen,
                // we always set the mMergeButton to GONE
                mMergeButton.setVisibility(View.GONE);

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
                if(StarConfig.OPERATOR.equals("SKT")){
                    mSendButton.setVisibility(View.GONE);
                }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

            } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                mMergeButton.setVisibility(View.VISIBLE);
                mMergeButton.setEnabled(true);
                mAddButton.setVisibility(View.GONE);

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
                if(StarConfig.OPERATOR.equals("SKT")){
                    mSendButton.setVisibility(View.GONE);
                }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        } else {
            // Neither "Add" nor "Merge" is available.  (This happens in
            // some transient states, like while dialing an outgoing call,
            // and in other rare cases like if you have both lines in use
            // *and* there are already 5 people on the conference call.)
            // Since the common case here is "while dialing", we show the
            // "Add" button in a disabled state so that there won't be any
            // jarring change in the UI when the call finally connects.
        	if(!mInCallScreen.isEndCallState())
        	{
	            mAddButton.setVisibility(View.VISIBLE);
	            mAddButton.setEnabled(false);
	            mMergeButton.setVisibility(View.GONE);
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
                if(StarConfig.OPERATOR.equals("SKT")){
                    mSendButton.setVisibility(View.GONE);
                }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]
        	}
        	}
        }
        if (inCallControlState.canAddCall && inCallControlState.canMerge) {
            if (phoneType == Phone.PHONE_TYPE_GSM) {
                // Uh oh, the InCallControlState thinks that "Add" *and* "Merge"
                // should both be available right now.  This *should* never
                // happen with GSM, but if it's possible on any
                // future devices we may need to re-layout Add and Merge so
                // they can both be visible at the same time...
                Log.w(LOG_TAG, "updateInCallControls: Add *and* Merge enabled," +
                        " but can't show both!");
            } else if (phoneType == Phone.PHONE_TYPE_CDMA) {
                // In CDMA "Add" option is always given to the user and the hence
                // in this case both "Add" and "Merge" options would be available to user
                if (DBG) log("updateInCallControls: CDMA: Add and Merge both enabled");
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }

        // "End call": this button has no state and it's always enabled.
        mEndButton.setEnabled(true);

        // "Dialpad": Enabled only when it's OK to use the dialpad in the
        // first place.
        mDialpadButton.setEnabled(inCallControlState.dialpadEnabled);
        //
        if (inCallControlState.dialpadVisible) {
            // Show the "hide dialpad" state.
            mDialpadButton.setText(R.string.onscreenHideDialpadText);
            mDialpadButton.setCompoundDrawablesWithIntrinsicBounds(
                null, mHideDialpadIcon, null, null);
        } else {
            // Show the "show dialpad" state.
            mDialpadButton.setText(R.string.onscreenShowDialpadText);
            mDialpadButton.setCompoundDrawablesWithIntrinsicBounds(
                    null, mShowDialpadIcon, null, null);
        }

        // "Bluetooth"
       	// 20101019 sumi920.kim@lge.com Receive HDVideoCall Event From HDVideoCall App
        if(StarConfig.COUNTRY.equals("KR"))
        {
        	mBluetoothButton.setEnabled(true);
        	mBluetoothButton.setChecked(mHDVisibilty);    
        	
        	Configuration config = getResources().getConfiguration();
        	String  temp=config.locale.toString();
        	Resources res = getResources();
        	
        	mBluetoothButton.setChecked(mHDVisibilty);       
        	mBluetoothButton.setEnabled(mHDVisibilty);

        	log("1. BluetoothButton.setEnabled = " + mHDVisibilty);
        	mBluetoothButton.setText(res.getString(R.string.incall_button_hd_videocall));
        	mBluetoothButton.setTextOn(res.getString(R.string.incall_button_hd_videocall));
        	mBluetoothButton.setTextOn(res.getString(R.string.incall_button_hd_videocall));
        	
        	mBluetoothButton.setHeight(54);
        	if(temp.indexOf("ko_KR") != -1)
        	{
        		mBluetoothButton.setTextSize(11);        	
        	}
        	else
        	{
        		mBluetoothButton.setTextSize(10);
        	}
        }
        else
        {
        //20101010 sumi920.kim@lge.com HD VideoCall InCall [END_LGE_LAB1]
        	mBluetoothButton.setEnabled(inCallControlState.bluetoothEnabled);
        	mBluetoothButton.setChecked(inCallControlState.bluetoothIndicatorOn);
        }
        // "Mute"
        mMuteButton.setEnabled(inCallControlState.canMute);
        mMuteButton.setChecked(inCallControlState.muteIndicatorOn);

        // "Speaker"
// LGE_RIL_SPEAKERPHONE_SUPPORT START
        if (!mSpeakerphoneButtonForcedDisabled) {
        mSpeakerButton.setEnabled(inCallControlState.speakerEnabled);
        } else {
            mSpeakerButton.setEnabled(false);
        }
// LGE_RIL_SPEAKERPHONE_SUPPORT END
        mSpeakerButton.setChecked(inCallControlState.speakerOn);

        // "Hold"
        // (Note "Hold" and "Swap" are never both available at
        // the same time.  That's why it's OK for them to both be in the
        // same position onscreen.)
        // This button is totally hidden (rather than just disabled)
        // when the operation isn't available.
        mHoldButtonContainer.setVisibility(
                inCallControlState.canHold ? View.VISIBLE : View.GONE);
        if (inCallControlState.canHold) {
            // The Hold button icon and label (either "Hold" or "Unhold")
            // depend on the current Hold state.
            if (inCallControlState.onHold) {
                mHoldButton.setImageDrawable(mUnholdIcon);
                mHoldButtonLabel.setText(R.string.onscreenUnholdText);
            } else {
                mHoldButton.setImageDrawable(mHoldIcon);
                mHoldButtonLabel.setText(R.string.onscreenHoldText);
            }
        }

        // "Swap"
        // This button is totally hidden (rather than just disabled)
        // when the operation isn't available.
        mSwapButtonContainer.setVisibility(
                inCallControlState.canSwap ? View.VISIBLE : View.GONE);

        if (phone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
            // "Merge"
            // This button is totally hidden (rather than just disabled)
            // when the operation isn't available.
            mCdmaMergeButtonContainer.setVisibility(
                    inCallControlState.canMerge ? View.VISIBLE : View.GONE);
        }

        if (inCallControlState.canSwap && inCallControlState.canHold) {
            // Uh oh, the InCallControlState thinks that Swap *and* Hold
            // should both be available.  This *should* never happen with
            // either GSM or CDMA, but if it's possible on any future
            // devices we may need to re-layout Hold and Swap so they can
            // both be visible at the same time...
            Log.w(LOG_TAG, "updateInCallControls: Hold *and* Swap enabled, but can't show both!");
        }

        if (phoneType == Phone.PHONE_TYPE_CDMA) {
            if (inCallControlState.canSwap && inCallControlState.canMerge) {
                // Uh oh, the InCallControlState thinks that Swap *and* Merge
                // should both be available.  This *should* never happen with
                // CDMA, but if it's possible on any future
                // devices we may need to re-layout Merge and Swap so they can
                // both be visible at the same time...
                Log.w(LOG_TAG, "updateInCallControls: Merge *and* Swap" +
                        "enabled, but can't show both!");
            }
        }

        // One final special case: if the dialpad is visible, that trumps
        // *any* of the upper corner buttons:
        if (inCallControlState.dialpadVisible) {
            mHoldButtonContainer.setVisibility(View.GONE);
            mSwapButtonContainer.setVisibility(View.GONE);
            mCdmaMergeButtonContainer.setVisibility(View.GONE);
// LGE_CALL_TRANSFER START
            mTransferButtonContainer.setVisibility(View.GONE);
// LGE_CALL_TRANSFER END
        }
// LGE_CALL_TRANSFER START
        mTransferButtonContainer.setVisibility(
                inCallControlState.canTransfer? View.GONE : View.GONE);
// LGE_CALL_TRANSFER END
    }
    
    public void UpdateEndCallControls(Phone mPhone, boolean isVisible)
    {
    	if(isVisible)
    	{
    		boolean isVisibleEndCallUI = !mInCallScreen.getCallCard().getName().equals(mInCallScreen.getString(R.string.private_num));
    		if(mNameInContact)
    		{
    			mNewAccount.setVisibility(View.GONE);
        		mAddAccount.setVisibility(View.GONE);
    		}
    		else
    		{
    			if(isVisibleEndCallUI)
    			{
	    			mNewAccount.setVisibility(View.VISIBLE);
		    		mAddAccount.setVisibility(View.VISIBLE);
    			}
    			else
    			{
    				mNewAccount.setVisibility(View.GONE);
		    		mAddAccount.setVisibility(View.GONE);
    			}
    		}
    		
    		if(isVisibleEndCallUI)
    		{
	    		mVoiceCall.setVisibility(View.VISIBLE);
	    		mVideoCall.setVisibility(View.VISIBLE);
	    		mMessage.setVisibility(View.VISIBLE);
    		}
    		else
    		{
    			mVoiceCall.setVisibility(View.GONE);
    			mVideoCall.setVisibility(View.GONE);
	    		mMessage.setVisibility(View.GONE);
    		}
    		
    		mAddButton.setVisibility(View.GONE);
    	    mMergeButton.setVisibility(View.GONE);
    	    mEndButton.setVisibility(View.GONE);
    	    mDialpadButton.setVisibility(View.GONE);
    	    mBluetoothButton.setVisibility(View.GONE);
    	    mMuteButton.setVisibility(View.GONE);
    	    mSpeakerButton.setVisibility(View.GONE);	
    	    mRecordButton.setVisibility(View.GONE);//<!--//20100916 sumi920.kim@lge.com InCall-Recording [LGE_LAB1]
    	    mStopButton.setVisibility(View.GONE);//<!--//20100916 sumi920.kim@lge.com InCall-Recording [LGE_LAB1]
    	    
    	}
    	else
    	{
    		mNewAccount.setVisibility(View.GONE);
    		mAddAccount.setVisibility(View.GONE);
    		mVoiceCall.setVisibility(View.GONE);
    		mVideoCall.setVisibility(View.GONE);
    		mMessage.setVisibility(View.GONE);
    		
    		InCallControlState inCallControlState = mInCallScreen.getUpdatedInCallControlState();
            int phoneType = mPhone.getPhoneType();
            if (inCallControlState.canAddCall && !mInCallScreen.isEndCallState()) {
              mAddButton.setVisibility(View.VISIBLE);
              mAddButton.setEnabled(true);
              mMergeButton.setVisibility(View.GONE);
	          } else if (inCallControlState.canMerge && !mInCallScreen.isEndCallState()) {
	              if (phoneType == Phone.PHONE_TYPE_CDMA) {
	                  // In CDMA "Add" option is always given to the user and the
	                  // "Merge" option is provided as a button on the top left corner of the screen,
	                  // we always set the mMergeButton to GONE
	                  mMergeButton.setVisibility(View.GONE);
	              } else if (phoneType == Phone.PHONE_TYPE_GSM) {
	                  mMergeButton.setVisibility(View.VISIBLE);
	                  mMergeButton.setEnabled(true);
	                  mAddButton.setVisibility(View.GONE);
	              } else {
	                  throw new IllegalStateException("Unexpected phone type: " + phoneType);
	              }
	          } else {
	              // Neither "Add" nor "Merge" is available.  (This happens in
	              // some transient states, like while dialing an outgoing call,
	              // and in other rare cases like if you have both lines in use
	              // *and* there are already 5 people on the conference call.)
	              // Since the common case here is "while dialing", we show the
	              // "Add" button in a disabled state so that there won't be any
	              // jarring change in the UI when the call finally connects.
	              if(!mInCallScreen.isEndCallState())
	              {
	                  mAddButton.setVisibility(View.VISIBLE);
	                  mAddButton.setEnabled(false);
	                  mMergeButton.setVisibility(View.GONE);
	              }
	          }

    	    mEndButton.setVisibility(View.VISIBLE);
    	    mDialpadButton.setVisibility(View.VISIBLE);
    	    mBluetoothButton.setVisibility(View.VISIBLE);
    	    mMuteButton.setVisibility(View.VISIBLE);
    	    mSpeakerButton.setVisibility(View.VISIBLE);
    	    //<!--//20100916 sumi920.kim@lge.com InCall-Recording [START_LGE_LAB1]
    	    if(StarConfig.COUNTRY.equals("KR"))
    	    {
    	    	if(PhoneUtils.isSoundRecording())
    	    	{
      	    		mRecordButton.setVisibility(View.GONE);
    	    		mStopButton.setVisibility(View.VISIBLE);
     	    	}
    	    	else
    	    	{
    	    		mRecordButton.setVisibility(View.VISIBLE);
    	    		mStopButton.setVisibility(View.GONE); 
     	    	}
    	    }
    	    else
    	    {
    	    	mRecordButton.setVisibility(View.GONE);
    	    	mStopButton.setVisibility(View.GONE);    	    
    	    }
    	  //<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]
    	    
    	}
    }

    public void UpdateEndCallControls_VT(boolean isVisible)
    {

       mInCallControls.setVisibility(isVisible ? View.VISIBLE : View.GONE);

	mAddButton.setVisibility(View.GONE);
	mMergeButton.setVisibility(View.GONE);
	mEndButton.setVisibility(View.GONE);
	mDialpadButton.setVisibility(View.GONE);
	mBluetoothButton.setVisibility(View.GONE);
	mMuteButton.setVisibility(View.GONE);
	mSpeakerButton.setVisibility(View.GONE);	   
	
    	if(isVisible)
    	{
    		if(mNameInContact)
    		{
    			mNewAccount.setVisibility(View.GONE);
        		mAddAccount.setVisibility(View.GONE);
    		}
    		else
    		{
    			mNewAccount.setVisibility(View.VISIBLE);
	    		mAddAccount.setVisibility(View.VISIBLE);
    		}
    		
    		mVoiceCall.setVisibility(View.VISIBLE);
    		mVideoCall.setVisibility(View.VISIBLE);
    		mMessage.setVisibility(View.VISIBLE);

    		//<!--//20101007 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]
    		mRecordButton.setVisibility(View.GONE);
    		mStopButton.setVisibility(View.GONE);
    		//<!--//20101007 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]

    	}
    	else
    	{
    		mNewAccount.setVisibility(View.GONE);
    		mAddAccount.setVisibility(View.GONE);
    		mVoiceCall.setVisibility(View.GONE);
    		mVideoCall.setVisibility(View.GONE);
    		mMessage.setVisibility(View.GONE);
    		//<!--//20101007 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]
    		mRecordButton.setVisibility(View.GONE);
    		mStopButton.setVisibility(View.GONE);
    		//<!--//20101007 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]
    	}
    }
    //
    // InCallScreen API
    //

    /**
     * @return true if the onscreen touch UI is enabled (for regular
     * "ongoing call" states) on the current device.
     */
    /* package */ boolean isTouchUiEnabled() {
        return mAllowInCallTouchUi;
    }

//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [START_LGE_LAB]
    /* package */ boolean isDialpadSendKeyEnabled() {
        return mAllowDialpadSendKey;
    }
//20101004 yusei.koh@lge.com <mailto:yusei.koh@lge.com> call ui : send/delete btn on InCallScreen dialpad [END_LGE_LAB]

    /**
     * @return true if the onscreen touch UI is enabled for
     * the "incoming call" state on the current device.
     */
    /* package */ boolean isIncomingCallTouchUiEnabled() {
        return mAllowIncomingCallTouchUi;
    }

    //
    // SlidingTab.OnTriggerListener implementation
    //

    /**
     * Handles "Answer" and "Reject" actions for an incoming call.
     * We get this callback from the SlidingTab
     * when the user triggers an action.
     *
     * To answer or reject the incoming call, we call
     * InCallScreen.handleOnscreenButtonClick() and pass one of the
     * special "virtual button" IDs:
     *   - R.id.answerButton to answer the call
     * or
     *   - R.id.rejectButton to reject the call.
     */
    public void onTrigger(View v, int whichHandle) {
        log("onDialTrigger(whichHandle = " + whichHandle + ")...");

        switch (whichHandle) {
            case SlidingTab.OnTriggerListener.LEFT_HANDLE:
                if (DBG) log("LEFT_HANDLE: answer!");

// LGE_MERGER_EXCUSE_CALL_UI_START
// jongwany.lee@lge.com START FUNCTION FOR CALL_UI
    		            hideIncomingCallWidget();

    			// ...and also prevent it from reappearing right away.
    			// (This covers up a slow response from the radio; see
    			// updateState().)
    			mLastIncomingCallActionTime = SystemClock.uptimeMillis();

    			// Do the appropriate action.
    			if (mInCallScreen != null) {
    				// 20100421 Incoming Animation Start    
                            {            
			            stopAni();
			            bottomTextDisplayControl(false);
                            }
    				// 20100421 Incoming Animation End
    				// Send this to the InCallScreen as a virtual "button click"
    				// event:
    				mInCallScreen.handleOnscreenButtonClick(R.id.answerButton);
//20101101 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Incoming Animation End [START_LGE_LAB] 
					stopAni();
					bottomTextDisplayControl(false);
//20101101 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Incoming Animation End [END_LGE_LAB] 
    			} else {
    				Log.e(LOG_TAG, "answer trigger: mInCallScreen is null");
    			}
    			break;
// jongwany.lee@lge.com END FUNCTION FOR CALL_UI
// LGE_MERGER_EXCUSE_CALL_UI_END	

            case SlidingTab.OnTriggerListener.RIGHT_HANDLE:
                if (DBG) log("RIGHT_HANDLE: reject!");

                hideIncomingCallWidget();

                // ...and also prevent it from reappearing right away.
                // (This covers up a slow response from the radio; see updateState().)
                mLastIncomingCallActionTime = SystemClock.uptimeMillis();

                // Do the appropriate action.
                if (mInCallScreen != null) {
                    // Send this to the InCallScreen as a virtual "button click" event:
                    mInCallScreen.handleOnscreenButtonClick(R.id.rejectButton);
                } else {
                    Log.e(LOG_TAG, "reject trigger: mInCallScreen is null");
                }
                break;

            default:
                Log.e(LOG_TAG, "onDialTrigger: unexpected whichHandle value: " + whichHandle);
                break;
        }

        // Regardless of what action the user did, be sure to clear out
        // the hint text we were displaying while the user was dragging.
        mInCallScreen.updateSlidingTabHint(0, 0);
    }

    /**
     * Apply an animation to hide the incoming call widget.
     */
    private void hideIncomingCallWidget() {
        if (mIncomingCallWidget.getVisibility() != View.VISIBLE
                || mIncomingCallWidget.getAnimation() != null) {
            // Widget is already hidden or in the process of being hidden
            return;
        }
        log("hideIncomingCallWidget()...");
        // Hide the incoming call screen with a transition
        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(IN_CALL_WIDGET_TRANSITION_TIME);
        anim.setAnimationListener(new AnimationListener() {

            public void onAnimationStart(Animation animation) {

            }

            public void onAnimationRepeat(Animation animation) {

            }

            public void onAnimationEnd(Animation animation) {
                // hide the incoming call UI.
                mIncomingCallWidget.clearAnimation();
                mIncomingCallWidget.setVisibility(View.GONE);
            }
        });
        mIncomingCallWidget.startAnimation(anim);
    }

    /**
     * Shows the incoming call widget and cancels any animation that may be fading it out.
     */
    private void showIncomingCallWidget() {
        log("showIncomingCallWidget()...");
        Animation anim = mIncomingCallWidget.getAnimation();
        if (anim != null) {
            anim.reset();
            mIncomingCallWidget.clearAnimation();
        }
        mIncomingCallWidget.reset(false);
        mIncomingCallWidget.setVisibility(View.VISIBLE);
    }

    /**
     * Handles state changes of the SlidingTabSelector widget.  While the user
     * is dragging one of the handles, we display an onscreen hint; see
     * CallCard.getRotateWidgetHint().
     */
    public void onGrabbedStateChange(View v, int grabbedState) {
        log("onGrabbedStateChange()..State="+grabbedState);
        if (mInCallScreen != null) {
            // Look up the hint based on which handle is currently grabbed.
            // (Note we don't simply pass grabbedState thru to the InCallScreen,
            // since *this* class is the only place that knows that the left
            // handle means "Answer" and the right handle means "Decline".)
            int hintTextResId, hintColorResId;
            switch (grabbedState) {
                case SlidingTab.OnTriggerListener.NO_HANDLE:
                    hintTextResId = 0;
                    hintColorResId = 0;
//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
                 // 20100421 Incoming Animation Start
    				stopAni();
//20101101 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Incoming Animation End [START_LGE_LAB] 
    				//bottomTextDisplayControl(false);
					bottomTextDisplayControl(true);
//20101101 wonho.moon@lge.com <mailto:wonho.moon@lge.com> Incoming Animation End [START_LGE_LAB] 
    				// 20100421 Incoming Animation End
                    break;
                case SlidingTab.OnTriggerListener.LEFT_HANDLE:
                    // TODO: Use different variants of "Slide to answer" in some cases
                    // depending on the phone state, like slide_to_answer_and_hold
                    // for a call waiting call, or slide_to_answer_and_end_active or
                    // slide_to_answer_and_end_onhold for the 2-lines-in-use case.
                    // (Note these are GSM-only cases, though.)
                    hintTextResId = R.string.slide_to_answer;
                    hintColorResId = R.color.incall_textConnected;  // green
//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
                 // 20100421 Incoming Animation Start
    				startAcceptAni();
    				bottomTextDisplayControl(false);
    				// 20100421 Incoming Animation End
                    break;
                case SlidingTab.OnTriggerListener.RIGHT_HANDLE:
                    hintTextResId = R.string.slide_to_decline;
                    hintColorResId = R.color.incall_textEnded;  // red
//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
                    if (mSecondIncomingCall)
    					startRejectAni();
    				else
    					startSilentAni();
    				bottomTextDisplayControl(false);
    				// 20100425 yongwoon.choi@lge.com Second Incoming UI [END_LGE]
                    break;
                default:
                    Log.e(LOG_TAG, "onGrabbedStateChange: unexpected grabbedState: "
                          + grabbedState);
                    hintTextResId = 0;
    				hintColorResId = 0;
    				// 20100421 Incoming Animation Start
    				stopAni();
    				bottomTextDisplayControl(false);
    				// 20100421 Incoming Animation End
                    break;
            }

            // Tell the InCallScreen to update the CallCard and force the
            // screen to redraw.
            mInCallScreen.updateSlidingTabHint(hintTextResId, hintColorResId);
        }
    }
//20100804 jongwany.lee@lge.com attached it for INCOMING CALL WIDGET
 // 20100321 SlidingTab End
	// 20100328 Addition to sound off in case of silence~[Start]
	void handleSilentCall() {
		if (DBG)
			log("handleSilentCall()...");

		final CallNotifier notifier = PhoneApp.getInstance().notifier;
		if (notifier.isRinging()) {
			PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_IDLE);
			notifier.silenceRinger();
		}
	}

	// 20100328 Addition to sound off in case of silence~[End]
	// 20100421 Incoming Animation Start
	private void startAcceptAni() {
	        log("startAcceptAni()...");
		arrowAniAccept.start();
		aniAccept.setVisibility(VISIBLE);
	}

	private void startSilentAni() {
         log("startSilentAni()...");
		arrowAniSilent.start();
		aniSilent.setVisibility(VISIBLE);
	}

	private void startRejectAni() {
         log("startRejectAni()...");
		arrowAniReject.start();
		aniReject.setVisibility(VISIBLE);
	}

	private void stopAcceptAni() {
         log("stopAcceptAni()...");
		aniAccept.setVisibility(GONE);
	}

	private void stopSilentAni() {
         log("stopSilentAni()...");
		aniSilent.setVisibility(GONE);
	}

	private void stopRejectAni() {
         log("stopRejectAni()...");
		aniReject.setVisibility(GONE);
	}

	private void stopAni() {
         log("stopAni()...");
		aniAccept.setVisibility(GONE);
		aniSilent.setVisibility(GONE);
		aniReject.setVisibility(GONE);
	}

	private void bottomTextDisplayControl(boolean incomingState) {
         log("bottomTextDisplayControl() => " + incomingState);
		txtAccept.setVisibility(incomingState ? View.VISIBLE : View.GONE);
		if (mSecondIncomingCall)
			txtReject.setVisibility(incomingState ? View.VISIBLE : View.GONE);
		/*else
			txtSilent.setVisibility(incomingState ? View.VISIBLE : View.GONE);*/
	}

	// LGE_MERGER_EXCUSE_CALL_UI_START
	// jongwany.lee@lge.com START FUNCTION FOR CALL_UI
	// True and False XML are same because we will only use True
	
	public void SetIncomingWidget() {
		if (mIncomingCallWidget != null) {
			mIncomingCallWidget.setRightTabResources(mSecondIncomingCall ? R.drawable.ic_jog_dial_decline : R.drawable.ic_jog_dial_silence_ringer,
					mSecondIncomingCall ? com.android.internal.R.drawable.jog_tab_target_red : com.android.internal.R.drawable.jog_tab_target_gray,
					mSecondIncomingCall ? com.android.internal.R.drawable.jog_tab_bar_right_decline : com.android.internal.R.drawable.jog_tab_bar_right_decline,
					mSecondIncomingCall ? com.android.internal.R.drawable.jog_tab_right_decline : com.android.internal.R.drawable.jog_tab_right_decline // 20100409
					// Framework
					// Resource
					// file
					// moving
					// for
					// CTS
					);

			mIncomingCallWidget.setRightHintText(mSecondIncomingCall ? R.string.slide_to_decline_hint : R.string.slide_to_decline_hint);
		}
	}

	// jongwany.lee@lge.com END FUNCTION FOR CALL_UI
	// LGE_MERGER_EXCUSE_CALL_UI_END
	 
    
    /**
     * OnTouchListener used to shrink the "hit target" of some onscreen
     * buttons.
     */
    class SmallerHitTargetTouchListener implements View.OnTouchListener {
        /**
         * Width of the allowable "hit target" as a percentage of
         * the total width of this button.
         */
        private static final int HIT_TARGET_PERCENT_X = 50;

        /**
         * Height of the allowable "hit target" as a percentage of
         * the total height of this button.
         *
         * This is larger than HIT_TARGET_PERCENT_X because some of
         * the onscreen buttons are wide but not very tall and we don't
         * want to make the vertical hit target *too* small.
         */
        private static final int HIT_TARGET_PERCENT_Y = 80;

        // Size (percentage-wise) of the "edge" area that's *not* touch-sensitive.
        private static final int X_EDGE = (100 - HIT_TARGET_PERCENT_X) / 2;
        private static final int Y_EDGE = (100 - HIT_TARGET_PERCENT_Y) / 2;
        // Min/max values (percentage-wise) of the touch-sensitive hit target.
        private static final int X_HIT_MIN = X_EDGE;
        private static final int X_HIT_MAX = 100 - X_EDGE;
        private static final int Y_HIT_MIN = Y_EDGE;
        private static final int Y_HIT_MAX = 100 - Y_EDGE;

        // True if the most recent DOWN event was a "hit".
        boolean mDownEventHit;

        /**
         * Called when a touch event is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * @return True if the listener has consumed the event, false otherwise.
         *         (In other words, we return true when the touch is *outside*
         *         the "smaller hit target", which will prevent the actual
         *         button from handling these events.)
         */
        public boolean onTouch(View v, MotionEvent event) {
            // if (DBG) log("SmallerHitTargetTouchListener: " + v + ", event " + event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Note that event.getX() and event.getY() are already
                // translated into the View's coordinates.  (In other words,
                // "0,0" is a touch on the upper-left-most corner of the view.)
                int touchX = (int) event.getX();
                int touchY = (int) event.getY();

                int viewWidth = v.getWidth();
                int viewHeight = v.getHeight();

                // Touch location as a percentage of the total button width or height.
                int touchXPercent = (int) ((float) (touchX * 100) / (float) viewWidth);
                int touchYPercent = (int) ((float) (touchY * 100) / (float) viewHeight);
                // if (DBG) log("- percentage:  x = " + touchXPercent + ",  y = " + touchYPercent);

                // TODO: user research: add event logging here of the actual
                // hit location (and button ID), and enable it for dogfooders
                // for a few days.  That'll give us a good idea of how close
                // to the center of the button(s) most touch events are, to
                // help us fine-tune the HIT_TARGET_PERCENT_* constants.

                if (touchXPercent < X_HIT_MIN || touchXPercent > X_HIT_MAX
                        || touchYPercent < Y_HIT_MIN || touchYPercent > Y_HIT_MAX) {
                    // Missed!
                    // if (DBG) log("  -> MISSED!");
                    mDownEventHit = false;
                    return true;  // Consume this event; don't let the button see it
                } else {
                    // Hit!
                    // if (DBG) log("  -> HIT!");
                    mDownEventHit = true;
                    return false;  // Let this event through to the actual button
                }
            } else {
                // This is a MOVE, UP or CANCEL event.
                //
                // We only do the "smaller hit target" check on DOWN events.
                // For the subsequent MOVE/UP/CANCEL events, we let them
                // through to the actual button IFF the previous DOWN event
                // got through to the actual button (i.e. it was a "hit".)
                return !mDownEventHit;
            }
        }
    }
 // LGE_MERGER_EXCUSE_MESSAGES_START
 // jongwany.lee@lge.com START FUNCTION FOR EXCUSE MESSAGE
	Activity getActivity() {
		return this.mInCallScreen;
	}

	String getUserNumber() {
		return mInCallScreen.getUserNumber();
	}

	//<!--//20100929 sumi920.kim@lge.com change excusemessage Intent [STAR_LGE_LAB1] 
    void onSendSms(String msg, boolean instantSend) {
    	String usrNumber = mInCallScreen.getUserNumber();
    	
    	if (usrNumber == null)
    	{
    	//	mInCallScreen.showToast(R.string.private_num, Toast.LENGTH_SHORT);
    		if (DBG) log("incoming number is null or nothing");
    		return;
    	}
 
    	this.mInCallScreen.updateKeyguardPolicy(true);
    		
    	Intent intent = new Intent(Intent.ACTION_SENDTO);
    	
    	intent.setData(Uri.parse("smsto:" + usrNumber));
    	intent.putExtra("sms_body", msg);
    	
    	if(StarConfig.COUNTRY.equals("KR"))
    	{
    		if (instantSend)
    		{
    			intent.putExtra("exit_on_sent", true); 
    		}
    		else
    		{
    			intent.putExtra("exit_on_sent", false);
    		}
    		mInCallScreen.startActivity(intent);
    		mInCallScreen.internalHangupRingingCall();
    	}
    	else
    	{
    		intent.putExtra("fromActivity", "ExcuseMessages");
    		mInCallScreen.internalHangupRingingCall();
    		mInCallScreen.startActivity(intent);
    	}

    }

//20101013 wonho.moon@lge.com <mailto:wonho.moon@lge.com> 3D Gesture Flip [START_LGE_LAB]
	public void showSilentIncomingUI()
	{
			handleSilentCall();
	}
//20101013 wonho.moon@lge.com <mailto:wonho.moon@lge.com> 3D Gesture Flip [END_LGE_LAB]


	//<!--//20100929 sumi920.kim@lge.com modifies excusemessage Intent [END_LGE_LAB1]
	// jongwany.lee@lge.com end function for Excuse Message
	// LGE_MERGER_EXCUSE_MESSAGES_END
    // LGE_RIL_SPEAKERPHONE_SUPPORT START
    public void disableSpeakerphoneButton() {
        log("disableSpeakerphoneButton()");
        mSpeakerButton.setEnabled(false);
        mSpeakerphoneButtonForcedDisabled = true;
     }

    public void enableSpeakerphoneButton() {
        log("enableSpeakerphoneButton()");
        mSpeakerButton.setEnabled(true);
        mSpeakerphoneButtonForcedDisabled = false;
     }

    public void setSpeakerphoneButtonForcedDisabled(boolean forceDisabled) {
        log("setSpeakerphoneButtonForcedDisabled()");
        mSpeakerphoneButtonForcedDisabled = forceDisabled;
     }
// LGE_RIL_SPEAKERPHONE_SUPPORT END
    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
//20100806 yongwoon.choi@lge.com turn down the ringtone [END_LGE]
    private void updateRecAreaCtrl(){
    	if(!mInCallScreen.isEndCallState())
    	{
    		if(PhoneUtils.isSoundRecording())
    		{
    			mStopButton.setVisibility(View.VISIBLE);
    			mStopButton.setEnabled(true);
    				
    			mRecordButton.setVisibility(View.GONE);
    			mAddButton.setVisibility(View.GONE);
    			mMergeButton.setVisibility(View.GONE);
    		}
    		else
    		{
    			InCallControlState inCallControlState = mInCallScreen.getUpdatedInCallControlState();
    			
    			
    			if(inCallControlState.canAddCall || inCallControlState.canMerge) //1 call Active or 3way call
    			{
    				mRecordButton.setVisibility(View.VISIBLE);
    				if(inCallControlState.bluetoothEnabled && inCallControlState.bluetoothIndicatorOn )
    					mRecordButton.setEnabled(false);
    				else
    					mRecordButton.setEnabled(true);
    			}
    			else
    			{
    				mRecordButton.setVisibility(View.VISIBLE);
    				mRecordButton.setEnabled(false);
    			}
    			
    			mStopButton.setVisibility(View.GONE);
    			mAddButton.setVisibility(View.GONE);
    			mMergeButton.setVisibility(View.GONE);
    			if(!inCallControlState.canAddCall && !inCallControlState.canMerge) //1 call Active or 3way call
    			{
			    // LGE_S joy.park 2010/06/08, patch for invalid animation on incoming call
			    stopAni();
			    // LGE_E joy.park 2010/06/08, patch for invalid animation on incoming call			
    			}
    		}
    	}
    	else
    	{
    		mRecordButton.setVisibility(View.GONE);
    		mStopButton.setVisibility(View.GONE);
    		mAddButton.setVisibility(View.GONE);
    		mMergeButton.setVisibility(View.GONE);
    	}
    }
//<!--//20100916 sumi920.kim@lge.com InCall-Recording [END_LGE_LAB1]	 -->	
//20101019 sumi920.kim@lge.com Receive HDVideoCall Enable Event From HDVideoCall App. [START_LGE_LAB1]	 -->	
	public void setHDVideoCallStatus(boolean bHDAvailable) {
		// TODO Auto-generated method stub
		mHDVisibilty = bHDAvailable;
//		mBluetoothButton.setChecked(bHDAvailable); 
		mBluetoothButton.setEnabled(bHDAvailable);
		PhoneUtils.setHDVideoCallStatus(mHDVisibilty);
		log("BluetoothButton.setEnabled = " + mHDVisibilty);
	}
	
	public boolean isHDVideoCallAvailable(){
		return mHDVisibilty;
	}
	//20101019 sumi920.kim@lge.com Receive HDVideoCall Enable Event From HDVideoCall App. [_LGE_LAB1]	 -->	
}
