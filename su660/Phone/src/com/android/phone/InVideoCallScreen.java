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

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.util.EventLog;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.phone.OtaUtils.CdmaOtaScreenState; // LGE_CALL_COSTS START
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneFactory;
import android.media.ToneGenerator; // LGE_CALL_COSTS END

import com.android.phone.videophone.VideoTelephonyApp;
import com.android.phone.videophone.menu.OnScreenSettings;
import com.android.phone.videophone.menu.Parameters;
import com.android.phone.videophone.menu.CameraSettings;
import com.android.phone.R;
import com.android.phone.videophone.VTAppStateManager;
import com.android.phone.videophone.VTPreferences;

import java.io.File;
import java.util.List; //20100802 jongwany.lee@lge.com	send SMS in Silent incoming [START_LGE]
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.RAD.RoamingPrefixAppender;
import com.android.internal.telephony.RAD.RoamingPrefixAppenderFactory;

//20100802 jongwany.lee@lge.com	send SMS in Silent incoming [END_LGE]

/**
 * Phone app "in video call" screen.
 */
public class InVideoCallScreen extends InCallScreen implements
View.OnClickListener, OnScreenSettings.OnVisibilityChangedListener,
OnSharedPreferenceChangeListener {
	private static final String LOG_TAG = "InVideoCallScreen";

	private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 1)
	&& (SystemProperties.getInt("ro.debuggable", 0) == 1);
	private static final boolean VDBG = (PhoneApp.DBG_LEVEL >= 2);
	// END_CALL Menu sendSMS
	private static final String SCHEME_SMSTO = "smsto";

	// Amount of time (in msec) that we display the "Call ended" state.
	// The "short" value is for calls ended by the local user, and the
	// "long" value is for calls ended by the remote caller.
	private static final int CALL_ENDED_SHORT_DELAY = 200; // msec
	// sumi920.kim@lge.com 2010.10.07  LAB1_CallUI --> Change  CALL_ENDED_LONG_DELAY time 2000msec to 3000sec
	private static final int CALL_ENDED_LONG_DELAY = 3000; // msec

	// Amount of time (in msec) that we keep the in-call menu onscreen
	// *after* the user changes the state of one of the toggle buttons.
	private static final int MENU_DISMISS_DELAY = 1000; // msec

	private static final int FALLBACK_DELAY = 2000; // msec

	private static final int VTRETRY_DELAY = 100; // msec
	
	// Message codes; see mHandler below.
	// Note message codes < 100 are reserved for the PhoneApp.
	private static final int PHONE_STATE_CHANGED = 101;
	private static final int PHONE_DISCONNECT = 102;
	private static final int EVENT_HEADSET_PLUG_STATE_CHANGED = 103;
	private static final int DELAYED_CLEANUP_AFTER_DISCONNECT = 104;
	private static final int DISMISS_MENU = 105;
	private static final int ALLOW_SCREEN_ON = 106;
	private static final int REQUEST_UPDATE_BLUETOOTH_INDICATION = 107;
	private static final int REQUEST_UPDATE_TOUCH_UI = 108;
	// LGE_CALL_COSTS START
	private static final int AOC_QUERY_ICC_ACM_MAX = 109;
	private static final int AOC_QUERY_ICC_ACM = 110;
	// LGE_CALL_COSTS END
	private static final int AUTOMATIC_FALLBACK = 111;

	// LGE_VT_IMS START
	private static final int EVENT_PSVT_MEDIA_NEGO_RESULT = 112;
	private static final int EVENT_PSVT_FAST_FRAME_UPDATE = 113;
	//--- LGE_VT_IMS END
       private static final int REQUEST_UPDATE_CALLCARD_UI = 114;

       //LGE_VT_CAPTURE
	public static final int EVTENT_START_CAPTURE = 115;

        //LGE_VT_LOWBATTERY
	public static final int EVTENT_LOW_BATTERY = 116;

     	private static final int EVTENT_VTRETRY = 117;

	private InCallScreenMode mInCallScreenMode = InCallScreenMode.UNDEFINED;

	// Possible error conditions that can happen on startup.
	// These are returned as status codes from the various helper
	// functions we call from onCreate() and/or onResume().
	// See syncWithPhoneState() and checkIfOkToInitiateOutgoingCall() for
	// details.
	private InCallInitStatus mInCallInitialStatus; // see onResume()

	private boolean mRegisteredForPhoneStates;
	private boolean mNeedShowCallLostDialog;

	private Phone mPhone;
	private Call mForegroundCall;
	private Call mBackgroundCall;
	private Call mRingingCall;

	// LGE_CALL_COSTS START
	private CommandsInterface mCmdIf = PhoneFactory.getCommandsInterface();
	private static int acm = 0;
	private static int acmMax = 0;
	// LGE_CALL_COSTS END

	private BluetoothHandsfree mBluetoothHandsfree;
	private BluetoothHeadset mBluetoothHeadset;
	private boolean mBluetoothConnectionPending;
	private long mBluetoothConnectionRequestTime;

	// Main in-call UI ViewGroups
	private ViewGroup mMainFrame_videocall;
	private ViewGroup mInVideoCallPanel;

	// VideoCall UI
	private VideoCallCard mVideoCallCard;

	private String dialNumber = null;

	// UI controls:
	private InCallTouchUi mInCallTouchUi; // used on some devices

	// Various dialogs we bring up (see dismissAllDialogs()).
	// TODO: convert these all to use the "managed dialogs" framework.
	//
	// The MMI started dialog can actually be one of 2 items:
	// 1. An alert dialog if the MMI code is a normal MMI
	// 2. A progress dialog if the user requested a USSD
	private AlertDialog mGenericErrorDialog;
	// NOTE: if you add a new dialog here, be sure to add it to
	// dismissAllDialogs() also.

	// TODO: If the Activity class ever provides an easy way to get the
	// current "activity lifecycle" state, we can remove these flags.
	private boolean mIsDestroyed = false;
	private boolean mIsForegroundActivity = false;

	// Flag indicating whether or not we should bring up the Call Log when
	// exiting the in-call UI due to the Phone becoming idle. (This is
	// true if the most recently disconnected Call was initiated by the
	// user, or false if it was an incoming call.)
	// This flag is used by delayedCleanupAfterDisconnect(), and is set by
	// onDisconnect() (which is the only place that either posts a
	// DELAYED_CLEANUP_AFTER_DISCONNECT event *or* calls
	// delayedCleanupAfterDisconnect() directly.)
	private boolean mShowCallLogAfterDisconnect;

	private AlertDialog mFallbackDialog;
	public static int isSubstituImage = 0;

       //LGE_VT_CAPTURE
	private static final int NEAR_CAPTURE = 1;
	private static final int FAR_CAPTURE = 2;

       private int mRetryCount = 0;
	private Handler mHandler = new Handler() {
		// ////@Override
		public void handleMessage(Message msg) {
			if (mIsDestroyed) {
				log("Handler: ignoring message " + msg
							+ "; we're destroyed!");
				return;
			}
			if (!mIsForegroundActivity) {
				log("Handler: handling message " + msg
							+ " while not in foreground");
				// Continue anyway; some of the messages below *want* to
				// be handled even if we're not the foreground activity
				// (like DELAYED_CLEANUP_AFTER_DISCONNECT), and they all
				// should at least be safe to handle if we're not in the
				// foreground...
			}

			PhoneApp app = PhoneApp.getInstance();

			switch (msg.what) {
			case PHONE_STATE_CHANGED:
			    if(DBG)
				Log.w(LOG_TAG, "handleMessage(PHONE_STATE_CHANGED) called!");
				onPhoneStateChanged((AsyncResult) msg.obj);
				break;

			case PHONE_DISCONNECT:

			     // When received fall-back , we can't receive PHONE_STATE_CHANGED msg and 
			     // only can directly receive PHONE_DISCONNECT msg in the InVideoCallScreen's handleMessage().
			     // so, we must call this api explicitly.

				if(mVideoCallCard != null)
				{
				    if(DBG)
					Log.w(LOG_TAG, "handleMessage(PHONE_DISCONNECT) called!");
					mVideoCallCard.restoreReadyImageView();
//					mVideoCallCard.restoreSurfaceView();
				}
				onDisconnect((AsyncResult) msg.obj);
				break;

			case EVENT_HEADSET_PLUG_STATE_CHANGED:
				// Update the in-call UI, since some UI elements (in
				// particular the "Speaker" menu button) change state
				// depending on whether a headset is plugged in.
				// TODO: A full updateScreen() is overkill here, since
				// the value of PhoneApp.isHeadsetPlugged() only affects a
				// single menu item. (But even a full updateScreen()
				// is still pretty cheap, so let's keep this simple
				// for now.)
				if (!isBluetoothAudioConnected()) {
					if (msg.arg1 == 1) {
						 log("headset pluged...");
						VideoTelephonyApp.getInstance().setAudioPath(VideoTelephonyApp.VTSNDPATH_EARMIC);				
						PhoneUtils.turnOnHeadset(InVideoCallScreen.this, true,false);

						if(PhoneUtils.isSpeakerOn(InVideoCallScreen.this))
							PhoneUtils.turnOnSpeaker(InVideoCallScreen.this, false,false);

						// Update Button UI & State
						if (mVideoCallCard != null)
					       {
                                                 mVideoCallCard.disableButton(VideoCallCard.BUTTON_SPEAKER);
						 }
					}
					else {
						 log("headset unpluged...");
                                          if (mVideoCallCard != null)
                                                mVideoCallCard.enableButton(VideoCallCard.BUTTON_SPEAKER);      
                                          
						if(VideoTelephonyApp.getInstance().getPreAudioPath() == VideoTelephonyApp.VTSNDPATH_SPEAKER)
						{
							VideoTelephonyApp.getInstance().setAudioPath(VideoTelephonyApp.VTSNDPATH_SPEAKER);	
							PhoneUtils.turnOnSpeaker(InVideoCallScreen.this, true, false);

							// Update Button UI & State
							if (mVideoCallCard != null)
								mVideoCallCard.updateButtonUI(VideoCallCard.BUTTON_SPEAKER,	true, "");
						}else
						{
							VideoTelephonyApp.getInstance().setAudioPath(VideoTelephonyApp.VTSNDPATH_RECEIVER);	
							PhoneUtils.turnOnHeadset(InVideoCallScreen.this,false, false);
							// Update Button UI & State
							if (mVideoCallCard != null)
								mVideoCallCard.updateButtonUI(VideoCallCard.BUTTON_SPEAKER,	false, "");
						}
					}
				}
				updateScreen();
				break;

			case DELAYED_CLEANUP_AFTER_DISCONNECT:
				// LGE_AUTO_REDIAL START
				// add parameter
				delayedCleanupAfterDisconnect((Connection.DisconnectCause) msg.obj);
				// LGE_AUTO_REDIAL END
				break;

			case DISMISS_MENU:
				// dismissMenu() has no effect if the menu is already closed.
				dismissMenu(true); // dismissImmediate = true
				break;

			case ALLOW_SCREEN_ON:
				log("ALLOW_SCREEN_ON message...");
				// Undo our previous call to preventScreenOn(true).
				// (Note this will cause the screen to turn on
				// immediately, if it's currently off because of a
				// prior preventScreenOn(true) call.)
				app.preventScreenOn(false);
				break;

			case REQUEST_UPDATE_BLUETOOTH_INDICATION:
				log("REQUEST_UPDATE_BLUETOOTH_INDICATION...");
				// The bluetooth headset state changed, so some UI
				// elements may need to update. (There's no need to
				// look up the current state here, since any UI
				// elements that care about the bluetooth state get it
				// directly from PhoneApp.showBluetoothIndication().)
				updateScreen();
				break;

			case REQUEST_UPDATE_TOUCH_UI:
				updateInCallTouchUi();
				break;
                    case REQUEST_UPDATE_CALLCARD_UI:
                            updateInCallCallCardUi();
                            break;
				// LGE_CALL_COSTS START
			case AOC_QUERY_ICC_ACM_MAX: {
				log("Received AOC_QUERY_ICC_ACM_MAX event");

				AsyncResult arAcmMax = (AsyncResult) msg.obj;
				if (arAcmMax.exception != null) {
						log("Failed to get ACM Max");
						log(arAcmMax.exception.getMessage());
				} else {
					String acmMaxStr = (String) (arAcmMax.result);

					try {
						acmMax = Integer.parseInt(acmMaxStr);
					} catch (NumberFormatException e) {
							log("Failed to parse acmMax : " + e.getMessage());
					}
				}
				break;
			}

			case AOC_QUERY_ICC_ACM: {
					log("Received AOC_QUERY_ICC_ACM event");

				AsyncResult arAcm = (AsyncResult) msg.obj;
				if (arAcm.exception != null) {
						log("Failed to get ACM");
						log(arAcm.exception.getMessage());
				} else {
					String acmStr = (String) (arAcm.result);

					try {
						acm = Integer.parseInt(acmStr);
					} catch (NumberFormatException e) {
						if (DBG)
							log("Failed to parse acm : " + e.getMessage());

						break;
					}
				}

				if ((acmMax > 0) && (acm > acmMax)) {
					if (DBG)
						log("Out of Credit Limit. Hangup calls!");

					ToneGenerator toneGenerator = new ToneGenerator(
							AudioManager.STREAM_VOICE_CALL,
							ToneGenerator.MAX_VOLUME >> 1);
					toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
					toneGenerator.stopTone();
					Toast.makeText(InVideoCallScreen.this,
							R.string.call_cost_limit_exceed_error,
							Toast.LENGTH_LONG).show();
					PhoneUtils.hangup(mPhone);
				}
				break;
			}
			// LGE_CALL_COSTS END
			case AUTOMATIC_FALLBACK:			    
				log("handleMessage(AUTOMATIC_FALLBACK) called!");
				retryVoiceCall();
				mHandler.removeMessages(AUTOMATIC_FALLBACK);
				break;

				// LGE_VT_IMS START
			case EVENT_PSVT_FAST_FRAME_UPDATE:
				VideoTelephonyApp.getInstance().fastFrameUpdate();
				break;

			case EVENT_PSVT_MEDIA_NEGO_RESULT:
				VideoTelephonyApp.getInstance().mediaNegoResult((AsyncResult) msg.obj);
				break;
				//--- LGE_VT_IMS END

			case EVTENT_START_CAPTURE:
				mVideoCallCard.onStartCapture(true);
				break;
                     case EVTENT_LOW_BATTERY:
                            // call end when received low battery event on VT
                            if(mRingingCall.isIdle())
                            {
                            	Toast.makeText(InVideoCallScreen.this, R.string.vt_lowbattery_callend, 1000).show();
                                    internalHangup();
                             }
                            break;
                     case EVTENT_VTRETRY:
                                retryVideoCall();
                            break;
			}
		}
	};

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		// ////@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
				// Listen for ACTION_HEADSET_PLUG broadcasts so that we
				// can update the onscreen UI when the headset state changes.
				// if (DBG) log("mReceiver: ACTION_HEADSET_PLUG");
				// if (DBG) log("==> intent: " + intent);
				// if (DBG) log("    state: " + intent.getIntExtra("state", 0));
				// if (DBG) log("    name: " + intent.getStringExtra("name"));
				// send the event and add the state as an argument.
				Message message = Message.obtain(mHandler,
						EVENT_HEADSET_PLUG_STATE_CHANGED, intent.getIntExtra(
								"state", 0), 0);
				mHandler.sendMessage(message);
			} else if(action.equals(Intent.ACTION_BATTERY_LOW)){
		      		Message msg = mHandler.obtainMessage(EVTENT_LOW_BATTERY);
                            mHandler.sendMessage(msg);
			}			
		}
	};

	// VT menu ???
	private OnScreenSettings mSettings;
	private Parameters mParameters;
	// private Parameters mInitialParams;
	private SharedPreferences mPreferences;
	private PreferenceScreen mScreen;

	// ////@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	protected void onSubCreate(Bundle icicle) {
		log("=================== InVideoCallScreen onSubCreate()" + this);

		Profiler.callScreenOnCreate();

		final PhoneApp app = PhoneApp.getInstance();
		app.setInCallScreenInstance(this);

		// set this flag so this activity will stay in front of the keyguard
		int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
		if (app.getPhoneState() == Phone.State.OFFHOOK) {
			// While we are in call, the in-call screen should dismiss the
			// keyguard.
			// This allows the user to press Home to go directly home without
			// going through
			// an insecure lock screen.
			// But we do not want to do this if there is no active call so we do
			// not
			// bypass the keyguard if the call is not answered or declined.
			flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
		}
		getWindow().addFlags(flags);

		setPhone(app.phone); // Sets mPhone and
		// mForegroundCall/mBackgroundCall/mRingingCall

		mBluetoothHandsfree = app.getBluetoothHandsfree();
			log("- mBluetoothHandsfree: " + mBluetoothHandsfree);

		if (mBluetoothHandsfree != null) {
			// The PhoneApp only creates a BluetoothHandsfree instance in the
			// first place if BluetoothAdapter.getDefaultAdapter()
			// succeeds. So at this point we know the device is BT-capable.
			mBluetoothHeadset = new BluetoothHeadset(this, null);
			
			log("- Got BluetoothHeadset: " + mBluetoothHeadset);
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Inflate everything in invideocall_screen.xml and add it to the
		// screen.
		setContentView(R.layout.invideocall_screen);

		initInCallScreen();

		// No need to change wake state here; that happens in onResume() when we
		// are actually displayed.

		// Handle the Intent we were launched with, but only if this is the
		// the very first time we're being launched (ie. NOT if we're being
		// re-initialized after previously being shut down.)
		// Once we're up and running, any future Intents we need
		// to handle will come in via the onNewIntent() method.
		if (icicle == null) {
			log("onCreate(): this is our very first launch, checking intent...");

			// Stash the result code from internalResolveIntent() in the
			// mInCallInitialStatus field. If it's an error code, we'll
			// handle it in onResume().
			mInCallInitialStatus = internalResolveIntent(getIntent());
			log("onCreate(): mInCallInitialStatus = "
						+ mInCallInitialStatus);
			if (mInCallInitialStatus != InCallInitStatus.SUCCESS) {
			log("onCreate: status " + mInCallInitialStatus
						+ " from internalResolveIntent()");
				// See onResume() for the actual error handling.
			}
		} else {
			mInCallInitialStatus = InCallInitStatus.SUCCESS;
		}

		Profiler.callScreenCreated();

		//Register Contextmenu for image capture	
		registerForContextMenu(mVideoCallCard.mNearview);
		registerForContextMenu(mVideoCallCard.mFarview);
		///////
		log("onCreate(): exit");
	}

	/**
	 * Sets the Phone object used internally by the InCallScreen.
	 * 
	 * In normal operation this is called from onCreate(), and the passed-in
	 * Phone object comes from the PhoneApp. For testing, test classes can use
	 * this method to inject a test Phone instance.
	 */
	/* package */void setPhone(Phone phone) {
		mPhone = phone;
		// Hang onto the three Call objects too; they're singletons that
		// are constant (and never null) for the life of the Phone.
		mForegroundCall = mPhone.getForegroundCall();
		mBackgroundCall = mPhone.getBackgroundCall();
		mRingingCall = mPhone.getRingingCall();
	}

	// ////@Override
	protected void onStart() {
		super.onStart();
	}

	protected void onSubStart() {
        
		log("=================== InVideoCallScreen onSubStart() ===================");
        
		registerForPhoneStates();

		dismissVTUI();
	}

	// ////@Override
	protected void onResume() {
		super.onResume();
	}

	protected void onSubResume() {
		log("=================== InVideoCallScreen onSubResume() ===================");

              PhoneApp.mLowbatteryHangup = false;
		mIsForegroundActivity = true;

		final PhoneApp app = PhoneApp.getInstance();
              app.setInCallScreenInstance(this);
              
		// LGE_CALL_COSTS START
		mCmdIf = PhoneFactory.getCommandsInterface();
		// LGE_CALL_COSTS END

		app.disableStatusBar();

		// Touch events are never considered "user activity" while the
		// InCallScreen is active, so that unintentional touches won't
		// prevent the device from going to sleep.
		app.setIgnoreTouchUserActivity(true);

		// Disable the status bar "window shade" the entire time we're on
		// the in-call screen.
		NotificationMgr.getDefault().getStatusBarMgr().enableExpandedView(false);

		// Listen for broadcast intents that might affect the onscreen UI.		
		IntentFilter filter = new IntentFilter();
              filter.addAction(Intent.ACTION_HEADSET_PLUG);
              filter.addAction(Intent.ACTION_BATTERY_LOW);
              registerReceiver(mReceiver, filter);
              
              // disable Alarm
    		Intent intent = new Intent();
    		intent.setAction("voice_video_record_playing");
    		sendBroadcast(intent);
            
		// Check for any failures that happened during onCreate() or
		// onNewIntent().
		if (DBG)
			log("- onResume: initial status = " + mInCallInitialStatus);
        
		if (mInCallInitialStatus != InCallInitStatus.SUCCESS) {
			if (DBG) log("- onResume: failure during startup: "	+ mInCallInitialStatus);

			// Don't bring up the regular Phone UI! Instead bring up
			// something more specific to let the user deal with the
			// problem.
			handleStartupError(mInCallInitialStatus);


            //jaehun.ryu 2010.11.04 : If  CALL_FAILED then do not process
            // keep  mInCallInitialStatus state until the Popup is shown. 

			// But it *is* OK to continue with the rest of onResume(),
			// since any further setup steps (like updateScreen() and the
			// CallCard setup) will fall back to a "blank" state if the
			// phone isn't in use.
			//@ mInCallInitialStatus = InCallInitStatus.SUCCESS;

             //jaehun.ryu 2010.11.04 : do not process on Resume
            return;
		}

		// Set the volume control handler while we are in the foreground.
		final boolean bluetoothConnected = isBluetoothAudioConnected();

		if (bluetoothConnected) {
			setVolumeControlStream(AudioManager.STREAM_BLUETOOTH_SCO);
		} else {
			setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		}

		takeKeyEvents(true);
		if (mPhone.getState() == Phone.State.RINGING) {
			mInCallTouchUi.SetIncomingWidget();
		}

		// Always start off in NORMAL mode
		setInCallScreenMode(InCallScreenMode.NORMAL);

		// Before checking the state of the phone, clean up any
		// connections in the DISCONNECTED state.
		// (The DISCONNECTED state is used only to drive the "call ended"
		// UI; it's totally useless when *entering* the InCallScreen.)
		mPhone.clearDisconnected();

		InCallInitStatus status = syncWithPhoneState();
		if (status != InCallInitStatus.SUCCESS) {
			if (DBG)
				log("- syncWithPhoneState failed! status = " + status);
			// Couldn't update the UI, presumably because the phone is totally
			// idle. But don't endInCallScreenSession immediately, since we
			// might still
			// have an error dialog up that the user needs to see.
			// (And in that case, the error dialog is responsible for calling
			// endInCallScreenSession when the user dismisses it.)
		}

		// InCallScreen is now active.
		EventLog.writeEvent(EventLogTags.PHONE_UI_ENTER);

		// Update the poke lock and wake lock when we move to
		// the foreground.
		//
		// But we need to do something special if we're coming
		// to the foreground while an incoming call is ringing:
		if (mPhone.getState() == Phone.State.RINGING) {
			// If the phone is ringing, we *should* already be holding a
			// full wake lock (which we would have acquired before
			// firing off the intent that brought us here; see
			// PhoneUtils.showIncomingCallUi().)
			//
			// We also called preventScreenOn(true) at that point, to
			// avoid cosmetic glitches while we were being launched.
			// So now we need to post an ALLOW_SCREEN_ON message to
			// (eventually) undo the prior preventScreenOn(true) call.
			//
			// (In principle we shouldn't do this until after our first
			// layout/draw pass. But in practice, the delay caused by
			// simply waiting for the end of the message queue is long
			// enough to avoid any flickering of the lock screen before
			// the InCallScreen comes up.)
			if (DBG)
				log("- posting ALLOW_SCREEN_ON message...");
			mHandler.removeMessages(ALLOW_SCREEN_ON);
			mHandler.sendEmptyMessage(ALLOW_SCREEN_ON);

			// TODO: There ought to be a more elegant way of doing this,
			// probably by having the PowerManager and ActivityManager
			// work together to let apps request that the screen on/off
			// state be synchronized with the Activity lifecycle.
			// (See bug 1648751.)
		} else {
			// The phone isn't ringing; this is either an outgoing call, or
			// we're returning to a call in progress. There *shouldn't* be
			// any prior preventScreenOn(true) call that we need to undo,
			// but let's do this just to be safe:
			app.preventScreenOn(false);
		}
		app.updateWakeState();

		// Restore the mute state if the last mute state change was NOT
		// done by the user.
		if (app.getRestoreMuteOnInCallResume()) {
			PhoneUtils.restoreMuteState(mPhone);
			app.setRestoreMuteOnInCallResume(false);
		}

		if (mInCallScreenMode == InCallScreenMode.CALL_ENDED) {
			updateEndCallTouchUi(true);
		} else {
			updateEndCallTouchUi(false);
		}

		//Always Every call start... then initialize VideoCallCard UI
		mVideoCallCard.initFlags();
              mVideoCallCard.buttonInitialize();
        
        // VT - camerasetting XML preference initialize
        setInitPreference();


		InitSndPath();

		isSubstituImage = Settings.System.getInt(getContentResolver(),
				Settings.System.VT_USE_PRIVATE, 0);
              File file;
              file = new File(Settings.System.getString(getContentResolver(),Settings.System.VT_PRIVATE_NAME));
              if(!file.exists())
                    Settings.System.putString(getContentResolver(),Settings.System.VT_PRIVATE_NAME, mVideoCallCard.subsImagePath);
              
		if (isSubstituImage == 1) {
			mVideoCallCard.showSubstitute(true, false);

			// Update Button UI & State
			if (mVideoCallCard != null)
				mVideoCallCard.updateButtonUI(
						VideoCallCard.BUTTON_PRIVATE_SHOW, true, getResources()
						.getString(R.string.vt_Show));
		} else {
			if (mVideoCallCard != null)
				mVideoCallCard.showSubstitute(false, false);
		}

        if(PhoneApp.getInstance().isCallFMC())    //LGE_FMC
            mVideoCallCard.mCommonFMC.setVisibility(View.VISIBLE);
	 else
           app.setCallFMCState(1);
        Profiler.profileViewCreate(getWindow(), InVideoCallScreen.class
				.getName());
		if (DBG)
			log("onResume() done.");
	}

	// onPause is guaranteed to be called when the InCallScreen goes
	// in the background.
	// ////@Override
	protected void onPause() {
		super.onPause();
	}

	protected void onSubPause() {
		if (DBG) log("=================== InVideoCallScreen onSubPause() ===================");
        
		//When this Activity is pause or stop, the animation effect occured cracked-UI(surfaceview).
		//so, we choke animation effect off. - by hgkim 10/29
		overridePendingTransition(0,0);

              PhoneApp.mLowbatteryHangup = false;
		mIsForegroundActivity = false;

		final PhoneApp app = PhoneApp.getInstance();

		// A safety measure to disable proximity sensor in case call failed
		// and the telephony state did not change.
		app.setBeginningCall(false);

		// If the device is put to sleep as the phone call is ending,
		// we may see cases where the DELAYED_CLEANUP_AFTER_DISCONNECT
		// event gets handled AFTER the device goes to sleep and wakes
		// up again.

		// This is because it is possible for a sleep command
		// (executed with the End Call key) to come during the 2
		// seconds that the "Call Ended" screen is up. Sleep then
		// pauses the device (including the cleanup event) and
		// resumes the event when it wakes up.

		// To fix this, we introduce a bit of code that pushes the UI
		// to the background if we pause and see a request to
		// DELAYED_CLEANUP_AFTER_DISCONNECT.

		// Note: We can try to finish directly, by:
		// 1. Removing the DELAYED_CLEANUP_AFTER_DISCONNECT messages
		// 2. Calling delayedCleanupAfterDisconnect directly

		// However, doing so can cause problems between the phone
		// app and the keyguard - the keyguard is trying to sleep at
		// the same time that the phone state is changing. This can
		// end up causing the sleep request to be ignored.
		if (mHandler.hasMessages(DELAYED_CLEANUP_AFTER_DISCONNECT)
				&& mPhone.getState() != Phone.State.RINGING) {
			if (DBG)
				log("DELAYED_CLEANUP_AFTER_DISCONNECT detected, moving UI to background.");
			endInCallScreenSession();
		}

		EventLog.writeEvent(EventLogTags.PHONE_UI_EXIT);

		// Clean up the menu, in case we get paused while the menu is up
		// for some reason.
		dismissMenu(true); // dismiss immediately

		// Dismiss any dialogs we may have brought up, just to be 100%
		// sure they won't still be around when we get back here.
		dismissAllDialogs();

		// Re-enable the status bar (which we disabled in onResume().)
		NotificationMgr.getDefault().getStatusBarMgr().enableExpandedView(true);

		// Unregister for broadcast intents. (These affect the visible UI
		// of the InCallScreen, so we only care about them while we're in the
		// foreground.)
		unregisterReceiver(mReceiver);

            // enable Alarm
    		Intent intent = new Intent();
    		intent.setAction("voice_video_record_finish");
    		sendBroadcast(intent);
            
		// Re-enable "user activity" for touch events.
		// We actually do this slightly *after* onPause(), to work around a
		// race condition where a touch can come in after we've paused
		// but before the device actually goes to sleep.
		// TODO: The PowerManager itself should prevent this from happening.
		mHandler.postDelayed(new Runnable() {
			public void run() {
				app.setIgnoreTouchUserActivity(false);
			}
		}, 500);
		app.reenableStatusBar();

		// Make sure we revert the poke lock and wake lock when we move to
		// the background.
		app.updateWakeState();
		// clear the dismiss keyguard flag so we are back to the default state
		// when we next resume
		updateKeyguardPolicy(false);
	}

	// ////@Override
	protected void onStop() {
		super.onStop();
	}

	protected void onSubStop() {
		if (DBG) log("=================== InVideoCallScreen onSubStop() ===================");

		unregisterForPhoneStates();
		stopTimer();

		Phone.State state = mPhone.getState();
		if (DBG)
			log("onStop: state = " + state);

		if (state == Phone.State.IDLE) {
			final PhoneApp app = PhoneApp.getInstance();
			// when OTA Activation, OTA Success/Failure dialog or OTA SPC
			// failure dialog is running, do not destroy inCallScreen. Because
			// call
			// is already ended and dialog will not get redrawn on slider event.
			if ((app.cdmaOtaProvisionData != null)
					&& (app.cdmaOtaScreenState != null)
					&& ((app.cdmaOtaScreenState.otaScreenState != CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION)
							&& (app.cdmaOtaScreenState.otaScreenState != CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG) && (!app.cdmaOtaProvisionData.inOtaSpcState))) {
				// we don't want the call screen to remain in the activity
				// history
				// if there are not active or ringing calls.
				if (DBG)
					log("- onStop: calling finish() to clear activity history...");
				moveTaskToBack(true);
			}
		}
		PhoneApp.mIsVideoCall = false;
		PhoneUtils.turnOnSpeaker(InVideoCallScreen.this, false, false);
		updateEndCallTouchUi(false);
	}

	// ////@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void onSubDestroy() {
		if (DBG) log("=================== InVideoCallScreen onSubDestroy() ===================");

		// Set the magic flag that tells us NOT to handle any handler
		// messages that come in asynchronously after we get destroyed.
		mIsDestroyed = true;

		final PhoneApp app = PhoneApp.getInstance();
		app.setInCallScreenInstance(null);

		// Clear out the InCallScreen references in various helper objects
		// (to let them know we've been destroyed).
		if (mVideoCallCard != null) {
			mVideoCallCard.setInCallScreenInstance(null);
		}

		if (mInCallTouchUi != null) {
			mInCallTouchUi.setInCallScreenInstance(null);
		}

		unregisterForPhoneStates();
		// No need to change wake state here; that happens in onPause() when we
		// are moving out of the foreground.

		if (mBluetoothHeadset != null) {
			mBluetoothHeadset.close();
			mBluetoothHeadset = null;
		}

              if(VideoTelephonyApp.mSelf != null)
                    VideoTelephonyApp.mSelf = null;

              if(VTAppStateManager.manager != null)
                    VTAppStateManager.manager = null;
              
		// Dismiss all dialogs, to be absolutely sure we won't leak any of
		// them while changing orientation.
		dismissAllDialogs();
	}

	/**
	 * Dismisses the in-call screen.
	 * 
	 * We never *really* finish() the InCallScreen, since we don't want to get
	 * destroyed and then have to be re-created from scratch for the next call.
	 * Instead, we just move ourselves to the back of the activity stack.
	 * 
	 * This also means that we'll no longer be reachable via the BACK button
	 * (since moveTaskToBack() puts us behind the Home app, but the home app
	 * doesn't allow the BACK key to move you any farther down in the history
	 * stack.)
	 * 
	 * (Since the Phone app itself is never killed, this basically means that
	 * we'll keep a single InCallScreen instance around for the entire uptime of
	 * the device. This noticeably improves the UI responsiveness for incoming
	 * calls.)
	 */
	// ////@Override
	public void finish() {
		if (DBG)
			log("finish()...");
		moveTaskToBack(true);
	}

	/**
	 * End the current in call screen session.
	 * 
	 * This must be called when an InCallScreen session has complete so that the
	 * next invocation via an onResume will not be in an old state.
	 */
	public void endInCallScreenSession() {
		if (DBG)
			log(" endInCallScreenSession()...");
        
		moveTaskToBack(true);
		setInCallScreenMode(InCallScreenMode.UNDEFINED);
	}

	/* package */boolean isForegroundActivity() {
         	if(mInCallScreenMode == InCallScreenMode.DIALING || mInCallScreenMode == InCallScreenMode.NORMAL)
    		    return true;
    	        else
		    return mIsForegroundActivity;
	}

	/* package */void updateKeyguardPolicy(boolean dismissKeyguard) {
		if (dismissKeyguard) {
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		} else {
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		}
	}

	private void registerForPhoneStates() {
		if (!mRegisteredForPhoneStates) {
			mPhone.registerForPreciseCallStateChanged(mHandler,
					PHONE_STATE_CHANGED, null);
			mPhone.registerForDisconnect(mHandler, PHONE_DISCONNECT, null);

			// LGE_VT_IMS START
			if (VTPreferences.NETWORK_MODE == VTPreferences.NETWORK_PSVT)
			{
				mPhone.registerForVtCodecNegoResult(mHandler, EVENT_PSVT_MEDIA_NEGO_RESULT, null);
				mPhone.registerForVTFastFrameUpdate(mHandler, EVENT_PSVT_FAST_FRAME_UPDATE, null);
			}
			//--- LGE_VT_IMS END

			mRegisteredForPhoneStates = true;
		}
	}

	private void unregisterForPhoneStates() {
		mPhone.unregisterForPreciseCallStateChanged(mHandler);
		mPhone.unregisterForDisconnect(mHandler);

		// LGE_VT_IMS START
		if (VTPreferences.NETWORK_PSVT == VTPreferences.NETWORK_MODE)
		{
			mPhone.unRegisterForVtCodecNegoResult(mHandler); 
			mPhone.unRegisterForFastFrameUpdate(mHandler); 
		}
		//--- LGE_VT_IMS END

		mRegisteredForPhoneStates = false;
	}

	/* package */void updateAfterRadioTechnologyChange() {
		if (DBG)
			Log.d(LOG_TAG, "updateAfterRadioTechnologyChange()...");
		// Unregister for all events from the old obsolete phone
		unregisterForPhoneStates();

		// (Re)register for all events relevant to the new active phone
		registerForPhoneStates();

		// Update mPhone and m{Foreground,Background,Ringing}Call
		PhoneApp app = PhoneApp.getInstance();
		setPhone(app.phone);
	}

	// ////@Override
	protected void onNewIntent(Intent intent) {
		if (DBG)
			log("onNewIntent: intent=" + intent);

		// We're being re-launched with a new Intent. Since we keep
		// around a single InCallScreen instance for the life of the phone
		// process (see finish()), this sequence will happen EVERY time
		// there's a new incoming or outgoing call except for the very
		// first time the InCallScreen gets created. This sequence will
		// also happen if the InCallScreen is already in the foreground
		// (e.g. getting a new ACTION_CALL intent while we were already
		// using the other line.)

		// Stash away the new intent so that we can get it in the future
		// by calling getIntent(). (Otherwise getIntent() will return the
		// original Intent from when we first got created!)
		setIntent(intent);

		// Activities are always paused before receiving a new intent, so
		// we can count on our onResume() method being called next.

		// Just like in onCreate(), handle this intent, and stash the
		// result code from internalResolveIntent() in the
		// mInCallInitialStatus field. If it's an error code, we'll
		// handle it in onResume().
		mInCallInitialStatus = internalResolveIntent(intent);
		if (mInCallInitialStatus != InCallInitStatus.SUCCESS) {
		    if(DBG)
			Log.w(LOG_TAG, "onNewIntent: status " + mInCallInitialStatus
					+ " from internalResolveIntent()");
			// See onResume() for the actual error handling.
		}
	}

	InCallInitStatus internalResolveIntent(Intent intent) {
		if (intent == null || intent.getAction() == null) {
			return InCallInitStatus.SUCCESS;
		}

		String action = intent.getAction();
		if (DBG)
			log("internalResolveIntent: action=" + action);

		// The calls to setRestoreMuteOnInCallResume() inform the phone
		// that we're dealing with new connections (either a placing an
		// outgoing call or answering an incoming one, and NOT handling
		// an aborted "Add Call" request), so we should let the mute state
		// be handled by the PhoneUtils phone state change handler.
		final PhoneApp app = PhoneApp.getInstance();

		if (action.equals(Intent.ACTION_ANSWER)) {
			internalAnswerCall();
			app.setRestoreMuteOnInCallResume(false);
			return InCallInitStatus.SUCCESS;
		} else if (action.equals(Intent.ACTION_VIDEO_CALL)) {
			// TODO: LGE, Video Telephony code
			// app.setRestoreMuteOnInCallResume(false);
			if (DBG)
				log("VT_AHJ : placeVideoCall(intent)");
			return placeVideoCall(intent);
		} else {
		    if(DBG)
			Log.w(LOG_TAG, "internalResolveIntent: unexpected intent action: "
					+ action);
			// But continue the best we can (basically treating this case
			// like ACTION_MAIN...)
			return InCallInitStatus.SUCCESS;
		}
	}

	private void stopTimer() {
		if (mVideoCallCard != null)
			mVideoCallCard.stopTimer();
	}

	private void initInCallScreen() {
		if (DBG)
			log("initInCallScreen()...");
		// Have the WindowManager filter out touch events that are "too fat".
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

		// Run in a 32-bit window, which improves the appearance of some
		// semitransparent artwork in the in-call UI (like the CallCard
		// photo borders).
		getWindow().setFormat(PixelFormat.RGBX_8888);

		mMainFrame_videocall = (ViewGroup) findViewById(R.id.mainFrame_videocall);
		mInVideoCallPanel = (ViewGroup) findViewById(R.id.inVideoCallPanel);

		// Initialize the VideoCallCard.
		mVideoCallCard = (VideoCallCard) findViewById(R.id.videocallCard);
		if (DBG)
			log("  - mVideoCallCard = " + mVideoCallCard);
		mVideoCallCard.setInCallScreenInstance(this);

		// Onscreen touch UI elements (used on some platforms)
		initInCallTouchUi();

		// VT Observer register by hgkim
		mVideoCallCard.attach(VideoTelephonyApp.getInstance(this));
	}

	/**
	 * Returns true if the phone is "in use", meaning that at least one line is
	 * active (ie. off hook or ringing or dialing). Conversely, a return value
	 * of false means there's currently no phone activity at all.
	 */
	private boolean phoneIsInUse() {
		return mPhone.getState() != Phone.State.IDLE;
	}

	// ////@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	public boolean onSubBackPressed() {
		if (DBG)
			log("onBackPressed()...");

		// To consume this BACK press, the code here should just do
		// something and return. Otherwise, call super.onBackPressed() to
		// get the default implementation (which simply finishes the
		// current activity.)

		// LGE_MERGE_S
		// 20100710 withwind.park sliding sms control
		// 20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
            if (this.mInCallTouchUi.mInCallMsg.getMode() == InCallMessageView.MODE_UNLOCK
            	&& mInCallTouchUi.mInCallMsg.isShown())
            {
            	mInCallTouchUi.mInCallMsg.updateState(InCallMessageView.ST_HIDE);
            	return false;
            }
       
            //20101024 sumi920.kim@lge.com porting [START_LGE_LAB1]
            // jaeyoung.ha@lge.com 2010.10.12 : Case2) BackPress key in MODE_LOCK => ST_LOCK_VIEW : QM1 45046
            else if (this.mInCallTouchUi.mInCallMsg.getMode() == InCallMessageView.MODE_LOCK
            		&& mInCallTouchUi.mInCallMsg.isShown())
            {
                if(DBG)log("HJY MODE_LOCK");        
            	mInCallTouchUi.mInCallMsg.updateState(InCallMessageView.ST_LOCK_VIEW);
            	return false;
            }
        
		if (!mRingingCall.isIdle()) {
			// While an incoming call is ringing, BACK behaves just like
			// ENDCALL: it stops the ringing and rejects the current call.
			// (This is only enabled on some platforms, though.)
			if (getResources().getBoolean(
					R.bool.allow_back_key_to_reject_incoming_call)) {
				if (DBG)
					log("BACK key while ringing: reject the call");
				internalHangupRingingCall();

				// Don't consume the key; instead let the BACK event *also*
				// get handled normally by the framework (which presumably
				// will cause us to exit out of this activity.)
				return true;
			} else {
				// The BACK key is disabled; don't reject the call, but
				// *do* consume the keypress (otherwise we'll exit out of
				// this activity.)
				if (DBG)
					log("BACK key while ringing: ignored");
				return false;
			}
		}

		// BACK is also used to exit out of any "special modes" of the
		// in-call UI:

		// LGE_VT_DIALPAD
		if (mVideoCallCard.isDialpad()) {
			mVideoCallCard.setDialpad(false);
			return false;
		}

              if(mVideoCallCard.IsCaptureMenu()){
                    mVideoCallCard.AfterCaptureButtonInitialize();
                    return false;
               }

		// Nothing special to do. Fall back to the default behavior.
		// ignore back key on vt
		return false;
	}

	/**
	 * Handles the green CALL key while in-call.
	 * 
	 * @return true if we consumed the event.
	 */
	private boolean handleCallKey() {
		if (!mRingingCall.isIdle()) {
			// If an incoming call is ringing, the CALL button is actually
			// handled by the PhoneWindowManager. (We do this to make
			// sure that we'll respond to the key even if the InCallScreen
			// hasn't come to the foreground yet.)
			//
			// We'd only ever get here in the extremely rare case that the
			// incoming call started ringing *after*
			// PhoneWindowManager.interceptKeyTq() but before the event
			// got here, or else if the PhoneWindowManager had some
			// problem connecting to the ITelephony service.
		    if(DBG)
			Log.w(LOG_TAG, "handleCallKey: incoming call is ringing!"
					+ " (PhoneWindowManager should have handled this key.)");
			// But go ahead and handle the key as normal, since the
			// PhoneWindowManager presumably did NOT handle it:

			// There's an incoming ringing call: CALL means "Answer".
			internalAnswerCall();
		} else {
			// The most common case: there's only one line in use, and
			// it's an active call (i.e. it's not on hold.)
			// In this case CALL is a no-op.
			// (This used to be a shortcut for "add call", but that was a
			// bad idea because "Add call" is so infrequently-used, and
			// because the user experience is pretty confusing if you
			// inadvertently trigger it.)
			if (DBG)
				log("handleCallKey: call in foregound ==> ignoring.");
			// But note we still consume this key event; see below.
		}

		// We *always* consume the CALL key, since the system-wide default
		// action ("go to the in-call screen") is useless here.
		return true;
	}

	private boolean handleShortHookKey() {

		final boolean hasRingingCall = !mRingingCall.isIdle();
		final boolean hasActiveCall = !mForegroundCall.isIdle();

		int phoneType = mPhone.getPhoneType();
		if (phoneType == Phone.PHONE_TYPE_GSM) {
			if (hasRingingCall) {
				internalAnswerCall();
			} else if (hasActiveCall) {
				internalHangup();
			} else {
				internalHangup();
			}
		} else {
			throw new IllegalStateException("Unexpected phone type: "
					+ phoneType);
		}
		return true;
	}

	private boolean handleLongHookKey() {
		final Call.State state = mRingingCall.getState();

		int phoneType = mPhone.getPhoneType();
		if (phoneType == Phone.PHONE_TYPE_GSM) {
			if (state == Call.State.INCOMING) {
				internalHangupRingingCall();
			} else {
				if (DBG)
					log("handleCallKey: call in foregound ==> ignoring.");
			}
		} else {
			throw new IllegalStateException("Unexpected phone type: "
					+ phoneType);
		}

		return true;
	}

	boolean isKeyEventAcceptableDTMF(KeyEvent event) {
		return false;
	}

	/**
	 * Overriden to track relevant focus changes.
	 * 
	 * If a key is down and some time later the focus changes, we may NOT
	 * recieve the keyup event; logically the keyup event has not occured in
	 * this window. This issue is fixed by treating a focus changed event as an
	 * interruption to the keydown, making sure that any code that needs to be
	 * run in onKeyUp is ALSO run here.
	 * 
	 * Note, this focus change event happens AFTER the in-call menu is
	 * displayed, so mIsMenuDisplayed should always be correct by the time this
	 * method is called in the framework, please see: {@link onCreatePanelView},
	 * {@link onOptionsMenuClosed}
	 */
	// ////@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	}

	// ////@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return super.dispatchKeyEvent(event);
	}

	public boolean dispatchSubKeyEvent(KeyEvent event) {
		return false;
	}

	// ////@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event);
	}

	public boolean onSubKeyUp(int keyCode, KeyEvent event) {
		// if (DBG) log("onKeyUp(keycode " + keyCode + ")...");

		if (keyCode == KeyEvent.KEYCODE_CALL) {
			// Always consume CALL to be sure the PhoneWindow won't do anything
			// with it
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
			if (!isLongHookKeyDown) {
				boolean hookhandled = handleShortHookKey();
				if (!hookhandled) {
					Log
					.w(LOG_TAG,
							"InVideoCallScreen should always handle KEYCODE_HEADSETHOOK in onKeyUp");
				}
				return true;
			}
			isLongHookKeyDown = false;
		}
		return false;
	}

	// 20100516 yongwoon.choi@lge.com Earjack hook key scenario [START_LGE]
	private boolean isLongHookKeyDown = false;

	// ////@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
	    if(DBG)
	        log("onKeyLongPress(keycode " + keyCode + ")...");
		switch (keyCode) {
		case KeyEvent.KEYCODE_HEADSETHOOK:
			boolean hookhandled = handleLongHookKey();
			isLongHookKeyDown = true;
			if (!hookhandled) {
				Log
				.w(LOG_TAG,
						"InVideoCallScreen should always handle KEYCODE_HEADSETHOOK in onKeyLongPress");
			}
			return true;
		}
		return false; // super.onKeyLongPress(keyCode, event);
	}

	// 20100516 yongwoon.choi@lge.com Earjack hook key scenario [END_LGE]

	// ////@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	public boolean onSubKeyDown(int keyCode, KeyEvent event) {
		// if (DBG) log("onKeyDown(keycode " + keyCode + ")...");

		switch (keyCode) {
		case KeyEvent.KEYCODE_CALL:
			boolean handled = handleCallKey();
			if (!handled) {
				Log
				.w(LOG_TAG,
						"InVideoCallScreen should always handle KEYCODE_CALL in onKeyDown");
			}
			// Always consume CALL to be sure the PhoneWindow won't do anything
			// with it
			return true;
			// 20100516 yongwoon.choi@lge.com Earjack hook key scenario
			// [START_LGE]

		case KeyEvent.KEYCODE_HEADSETHOOK:
			event.startTracking();
			return true;
			// 20100516 yongwoon.choi@lge.com Earjack hook key scenario
			// [END_LGE]
			// Note there's no KeyEvent.KEYCODE_ENDCALL case here.
			// The standard system-wide handling of the ENDCALL key
			// (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
			// already implements exactly what the UI spec wants,
			// namely (1) "hang up" if there's a current active call,
			// or (2) "don't answer" if there's a current ringing call.

		case KeyEvent.KEYCODE_CAMERA:
			// Disable the CAMERA button while in-call since it's too
			// easy to press accidentally.
			return true;

		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (mPhone.getState() == Phone.State.RINGING) {
				// If an incoming call is ringing, the VOLUME buttons are
				// actually handled by the PhoneWindowManager. (We do
				// this to make sure that we'll respond to them even if
				// the InVideoCallScreen hasn't come to the foreground yet.)
				//
				// We'd only ever get here in the extremely rare case that the
				// incoming call started ringing *after*
				// PhoneWindowManager.interceptKeyTq() but before the event
				// got here, or else if the PhoneWindowManager had some
				// problem connecting to the ITelephony service.
				Log
				.w(
						LOG_TAG,
						"VOLUME key: incoming call is ringing!"
						+ " (PhoneWindowManager should have handled this key.)");
				// But go ahead and handle the key as normal, since the
				// PhoneWindowManager presumably did NOT handle it:

				final CallNotifier notifier = PhoneApp.getInstance().notifier;
				if (notifier.isRinging()) {
					// ringer is actually playing, so silence it.
					PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_IDLE);
					if (DBG)
						log("VOLUME key: silence ringer");
					notifier.silenceRinger();
				}

				// As long as an incoming call is ringing, we always
				// consume the VOLUME keys.
				return true;
			}
			break;

		case KeyEvent.KEYCODE_MENU:
			break;

		case KeyEvent.KEYCODE_MUTE:
			PhoneUtils.setMute(mPhone, !PhoneUtils.getMute(mPhone));
			return true;

			// Various testing/debugging features, enabled ONLY when DBG ==
			// true.
		case KeyEvent.KEYCODE_SLASH:
			if (DBG) {
				log("----------- InVideoCallScreen View dump --------------");
				// Dump starting from the top-level view of the entire activity:
				Window w = this.getWindow();
				View decorView = w.getDecorView();
				decorView.debug();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_EQUALS:
			if (DBG) {
				log("----------- InVideoCallScreen call state dump --------------");
				PhoneUtils.dumpCallState(mPhone);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_GRAVE:
			if (DBG) {
				// Placeholder for other misc temp testing
				log("------------ Temp testing -----------------");
				return true;
			}
			break;
		}

		return false;
	}

	/**
	 * Something has changed in the phone's state. Update the UI.
	 */
	private void onPhoneStateChanged(AsyncResult r) {
		if (DBG)
			log("onPhoneStateChanged()...");

		// There's nothing to do here if we're not the foreground activity.
		// (When we *do* eventually come to the foreground, we'll do a
		// full update then.)
		if (!mIsForegroundActivity) {
			if (DBG)
				log("onPhoneStateChanged: Activity not in foreground! Bailing out...");
			// More_Fast_Dialing return;
		}

		updateScreen();

		// Make sure we update the poke lock and wake lock when certain
		// phone state changes occur.
		PhoneApp.getInstance().updateWakeState();
	}

	/**
	 * Updates the UI after a phone connection is disconnected, as follows:
	 * 
	 * - If this was a missed or rejected incoming call, and no other calls are
	 * active, dismiss the in-call UI immediately. (The CallNotifier will still
	 * create a "missed call" notification if necessary.)
	 * 
	 * - With any other disconnect cause, if the phone is now totally idle,
	 * display the "Call ended" state for a couple of seconds.
	 * 
	 * - Or, if the phone is still in use, stay on the in-call screen (and
	 * update the UI to reflect the current state of the Phone.)
	 * 
	 * @param r
	 *            r.result contains the connection that just ended
	 */
	private void onDisconnect(AsyncResult r) {
		Connection c = (Connection) r.result;
		Connection.DisconnectCause cause = c.getDisconnectCause();
		if (DBG)
			log("onDisconnect() " + c + ", cause=" + cause);

		dialNumber = c.getAddress();
		boolean currentlyIdle = !phoneIsInUse();
		final PhoneApp app = PhoneApp.getInstance();

		// Under certain call disconnected states, we want to alert the user
		// with a dialog instead of going through the normal disconnect
		// routine.
		if (cause == Connection.DisconnectCause.CALL_BARRED) {
			showGenericErrorDialog(R.string.callFailed_cb_enabled, false);
			return;
		} else if (cause == Connection.DisconnectCause.FDN_BLOCKED) {
			showGenericErrorDialog(R.string.callFailed_fdn_only, false);
			return;
		} else if (cause == Connection.DisconnectCause.CS_RESTRICTED) {
			showGenericErrorDialog(R.string.callFailed_dsac_restricted, false);
			return;
		} else if (cause == Connection.DisconnectCause.CS_RESTRICTED_EMERGENCY) {
			showGenericErrorDialog(
					R.string.callFailed_dsac_restricted_emergency, false);
			return;
		} else if (cause == Connection.DisconnectCause.CS_RESTRICTED_NORMAL) {
			showGenericErrorDialog(R.string.callFailed_dsac_restricted_normal,
					false);
			return;
		} else if (cause == Connection.DisconnectCause.BEARER_NOT_AUTH) {
			showFallbackDialog(R.string.vt_disconnect_cause_BEARER_NOT_AUTH);
			return;
		} else if (cause == Connection.DisconnectCause.BEARER_NOT_AVAIL) {
			showFallbackDialog(R.string.vt_disconnect_cause_BEARER_NOT_AVAIL);
			return;
		} else if ((cause == Connection.DisconnectCause.INCOMING_REJECTED) && PhoneApp.mLowbatteryHangup){
        		PhoneApp.mLowbatteryHangup = false;
			showGenericErrorDialog(R.string.vt_lowbattery_callend,false);
			return;
	       }

		// Explicitly clean up up any DISCONNECTED connections
		// in a conference call.
		// [Background: Even after a connection gets disconnected, its
		// Connection object still stays around for a few seconds, in the
		// DISCONNECTED state. With regular calls, this state drives the
		// "call ended" UI. But when a single person disconnects from a
		// conference call there's no "call ended" state at all; in that
		// case we blow away any DISCONNECTED connections right now to make sure
		// the UI updates instantly to reflect the current state.]
		Call call = c.getCall();
		if (call != null) {
			// We only care about situation of a single caller
			// disconnecting from a conference call. In that case, the
			// call will have more than one Connection (including the one
			// that just disconnected, which will be in the DISCONNECTED
			// state) *and* at least one ACTIVE connection. (If the Call
			// has *no* ACTIVE connections, that means that the entire
			// conference call just ended, so we *do* want to show the
			// "Call ended" state.)
			List<Connection> connections = call.getConnections();
			if (connections != null && connections.size() > 1) {
				for (Connection conn : connections) {
					if (conn.getState() == Call.State.ACTIVE) {
						// This call still has at least one ACTIVE connection!
						// So blow away any DISCONNECTED connections
						// (including, presumably, the one that just
						// disconnected from this conference call.)

						// We also force the wake state to refresh, just in
						// case the disconnected connections are removed
						// before the phone state change.
						if (DBG)
							log("- Still-active conf call; clearing DISCONNECTED...");
						app.updateWakeState();
						mPhone.clearDisconnected(); // This happens
						// synchronously.
						break;
					}
				}
			}
		}

		// Retrieve the emergency call retry count from this intent, in
		// case we need to retry the call again.
		int emergencyCallRetryCount = getIntent().getIntExtra(
				EmergencyCallHandler.EMERGENCY_CALL_RETRY_KEY,
				EmergencyCallHandler.INITIAL_ATTEMPT);

		// Note: see CallNotifier.onDisconnect() for some other behavior
		// that might be triggered by a disconnect event, like playing the
		// busy/congestion tone.

		// Keep track of whether this call was user-initiated or not.
		// (This affects where we take the user next; see
		// delayedCleanupAfterDisconnect().)

            //20101009 sumi920.kim@lge.com thunder portion [LGE_LAB1]
            // LGE_CHANGE_S [yujung.lee@lge.com] 2010-02-17 : do not Show Call History
            //      mShowCallLogAfterDisconnect = !c.isIncoming();
            mShowCallLogAfterDisconnect = false;
            //LGE_CHANGE_E [yujung.lee@lge.com] 2010-02-17 : do not Show Call History
        
		// We bail out immediately (and *don't* display the "call ended"
		// state at all) in a couple of cases, including those where we
		// are waiting for the radio to finish powering up for an
		// emergency call:
		boolean bailOutImmediately = ((cause == Connection.DisconnectCause.INCOMING_MISSED)
				|| (cause == Connection.DisconnectCause.INCOMING_REJECTED) || ((cause == Connection.DisconnectCause.OUT_OF_SERVICE) && (emergencyCallRetryCount > 0)))
				&& currentlyIdle;

		if (bailOutImmediately) {
			if (DBG)
				log("- onDisconnect: bailOutImmediately...");
			// Exit the in-call UI!
			// (This is basically the same "delayed cleanup" we do below,
			// just with zero delay. Since the Phone is currently idle,
			// this call is guaranteed to immediately finish this activity.)
			//delayedCleanupAfterDisconnect(null);
                     mPhone.clearDisconnected();
                     endInCallScreenSession();
			// Retry the call, by resending the intent to the emergency
			// call handler activity.
			if ((cause == Connection.DisconnectCause.OUT_OF_SERVICE)
					&& (emergencyCallRetryCount > 0)) {
				startActivity(getIntent().setClassName(this,
						EmergencyCallHandler.class.getName()));
			}
		} else {
			if (DBG)
				log("- onDisconnect: delayed bailout...");
			// Stay on the in-call screen for now. (Either the phone is
			// still in use, or the phone is idle but we want to display
			// the "call ended" state for a couple of seconds.)

			// Force a UI update in case we need to display anything
			// special given this connection's DisconnectCause (see
			// CallCard.getCallFailedString()).
			updateScreen();

			// Display the special "Call ended" state when the phone is idle
			// but there's still a call in the DISCONNECTED state:
			if (currentlyIdle
					&& ((mForegroundCall.getState() == Call.State.DISCONNECTED) || (mBackgroundCall
							.getState() == Call.State.DISCONNECTED))) {
				if (DBG)
					log("- onDisconnect: switching to 'Call ended' state...");

				setInCallScreenMode(InCallScreenMode.CALL_ENDED);
				dismissVTUI();
				updateInCallBackground();
				mVideoCallCard.updateState(mPhone);
				updateEndCallTouchUi(true);
			}


			// Some other misc cleanup that we do if the call that just
			// disconnected was the foreground call.
			final boolean hasActiveCall = !mForegroundCall.isIdle();
			if (!hasActiveCall) {
				if (DBG)
					log("- onDisconnect: cleaning up after FG call disconnect...");
			}

			// Updating the screen wake state is done in onPhoneStateChanged().

			// Finally, arrange for delayedCleanupAfterDisconnect() to get
			// called after a short interval (during which we display the
			// "call ended" state.) At that point, if the
			// Phone is idle, we'll finish out of this activity.
			/*
			 * int callEndedDisplayDelay = (cause ==
			 * Connection.DisconnectCause.LOCAL) ? CALL_ENDED_SHORT_DELAY :
			 * CALL_ENDED_LONG_DELAY;
			 */
			// VT is only use "long" value, because remove disconnecting stage.
			int callEndedDisplayDelay = CALL_ENDED_LONG_DELAY;
			mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
			// LGE_AUTO_REDIAL START
			Message msg = mHandler.obtainMessage(
					DELAYED_CLEANUP_AFTER_DISCONNECT, cause);
			mHandler.sendMessageDelayed(msg, callEndedDisplayDelay);
			// LGE_AUTO_REDIAL END
		}
	}

	/**
	 * Updates the state of the in-call UI based on the current state of the
	 * Phone.
	 */
	private void updateScreen() {
		if (DBG)
			log("updateScreen()...");

		// Don't update anything if we're not in the foreground (there's
		// no point updating our UI widgets since we're not visible!)
		// Also note this check also ensures we won't update while we're
		// in the middle of pausing, which could cause a visible glitch in
		// the "activity ending" transition.
		if (!mIsForegroundActivity) {
			if (DBG)
				log("- updateScreen: not the foreground Activity! Bailing out...");
			// More_Fast_Dialing return;
		}

		final PhoneApp app = PhoneApp.getInstance();

		if (mInCallScreenMode == InCallScreenMode.CALL_ENDED) {
			if (DBG)
				log("- updateScreen: call ended state (NOT updating in-call UI)...");
			// Actually we do need to update one thing: the background.

			updateInCallBackground();
			updateEndCallTouchUi(true);
			return;
		} else {
			updateEndCallTouchUi(false);
		}

		if (DBG)
			log("- updateScreen: updating the in-call UI...");
		dismissVTUI();
		mVideoCallCard.updateState(mPhone);
		updateInCallTouchUi();
		updateMenuButtonHint();
		updateInCallBackground();

		// Forcibly take down all dialog if an incoming call is ringing.
		if (!mRingingCall.isIdle()) {
			dismissAllDialogs();
		}
	}

	/**
	 * (Re)synchronizes the onscreen UI with the current state of the Phone.
	 * 
	 * @return InCallInitStatus.SUCCESS if we successfully updated the UI, or
	 *         InCallInitStatus.PHONE_NOT_IN_USE if there was no phone state to
	 *         sync with (ie. the phone was completely idle). In the latter
	 *         case, we shouldn't even be in the in-call UI in the first place,
	 *         and it's the caller's responsibility to bail out of this activity
	 *         by calling endInCallScreenSession if appropriate.
	 */
	private InCallInitStatus syncWithPhoneState() {
		boolean updateSuccessful = false;
		if (DBG)
			log("syncWithPhoneState()...");
		if (DBG)
			PhoneUtils.dumpCallState(mPhone);
		if (DBG)
			dumpBluetoothState();

		// Make sure the Phone is "in use". (If not, we shouldn't be on
		// this screen in the first place.)
		if (!mForegroundCall.isIdle() || !mBackgroundCall.isIdle()
				|| !mRingingCall.isIdle()
				|| !mPhone.getPendingMmiCodes().isEmpty()) {
			if (DBG)
				log("syncWithPhoneState: it's ok to be here; update the screen...");
			updateScreen();
			return InCallInitStatus.SUCCESS;
		}

		if (DBG)
			log("syncWithPhoneState: phone is idle; we shouldn't be here!");
		return InCallInitStatus.PHONE_NOT_IN_USE;
	}

	/**
	 * Given the Intent we were initially launched with, figure out the actual
	 * phone number we should dial.
	 * 
	 * @return the phone number corresponding to the specified Intent, or null
	 *         if the Intent is not an ACTION_CALL intent or if the intent's
	 *         data is malformed or missing.
	 * 
	 * @throws VoiceMailNumberMissingException
	 *             if the intent contains a "voicemail" URI, but there's no
	 *             voicemail number configured on the device.
	 */
	private String getInitialNumber(Intent intent)
	throws PhoneUtils.VoiceMailNumberMissingException {
		String action = intent.getAction();

		if (action == null) {
			return null;
		}

		if (action != null && action.equals(Intent.ACTION_CALL)
				&& intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
			return intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		}

		return PhoneUtils.getNumberFromIntent(this, mPhone, intent);
	}

	/**
	 * Make a Video call to whomever the intent tells us to.
	 * 
	 * @param intent
	 *            the Intent we were launched with
	 * @return InCallInitStatus.SUCCESS if we successfully initiated an outgoing
	 *         call. If there was some kind of failure, return one of the other
	 *         InCallInitStatus codes indicating what went wrong.
	 */
	
	private InCallInitStatus placeVideoCall(Intent intent) {
		if (DBG)
			log("placeVideoCall() " + intent);

		String number;

//LGE_VT_FIXED START- 2010/11/09
//Move this logic to OutgoingCallBroadcast.java - onCreate()
		
		// Check the current ServiceState to make sure it's OK
		// to even try making a call.
//		InCallInitStatus okToCallStatus = InCallInitStatus.OUT_OF_SERVICE;
        
//		// For Loopback call, return InCallInitStatus.SUCCESS
//        if (true == VideoTelephonyApp.getInstance().getLoopbackCall()) {
//            okToCallStatus = InCallInitStatus.SUCCESS;
//        }		
//LGE_VT_FIXED END- 2010/11/09 		

		try {
			number = getInitialNumber(intent);
		} catch (PhoneUtils.VoiceMailNumberMissingException ex) {
			// If the call status is NOT in an acceptable state, it
			// may effect the way the voicemail number is being
			// retrieved. Mask the VoiceMailNumberMissingException
			// with the underlying issue of the phone state.
//LGE_VT_FIXED START- 2010/11/09
			//if (okToCallStatus != InCallInitStatus.SUCCESS) {
			//	if (DBG)
			//		log("Voicemail number not reachable in current SIM card state.");
			//	return okToCallStatus;
			//}
//LGE_VT_FIXED END- 2010/11/09		    
		    
			if (DBG)
				log("VoiceMailNumberMissingException from getInitialNumber()");
			return InCallInitStatus.VOICEMAIL_NUMBER_MISSING;
		}

		if (number == null) {
		    if(DBG)
			{
		        Log.w(LOG_TAG,"placeVideoCall: couldn't get a phone number from Intent "
	                    + intent);
			}
					
			return InCallInitStatus.NO_PHONE_NUMBER_SUPPLIED;
		}

        String origin_number = null;

        boolean ignoreRad = intent.getBooleanExtra(RoamingPrefixAppender.INTENT_EXTRA_IGNORE_RAD,
                                            RoamingPrefixAppender.IGNORE_RAD_DEFAULT);

        if (ignoreRad==false) {
            RoamingPrefixAppender roamingPrefixAppender 
                        = RoamingPrefixAppenderFactory.getDefaultRoamingPrefixAppender(getBaseContext(), getContentResolver(), intent);
                
            if (roamingPrefixAppender!=null) {
                if (roamingPrefixAppender.isAddPrefix( number)) {
                    origin_number = number;
                    number = roamingPrefixAppender.appendPrefix(number);

                    if(DBG)
                    {
                        Log.v(LOG_TAG, 
                            "Roaming Auto Dialing origin_num = " + origin_number+ " new roaming number = " + number );
                    }
                }
            }
        }
        

//LGE_VT_FIXED START- 2010/11/09 
//Move this logic to OutgoingCallBroadcast.java - onCreate()		
		
//        /** Check network status */
//        if (okToCallStatus != InCallInitStatus.SUCCESS) {
//            return okToCallStatus;
//        }
        
//        /** Check dialing number: vt only  */
//        if (InCallInitStatus.SUCCESS != checkIfOkToDialNumber(number))
//        {
//           return InCallInitStatus.INVALID_NUMBER;
//        }
////LGE_VT_FIXED END- 2010/11/09		

        //When a not first call.
        //Each time we try outgoing call, add surfaceviews(far,remote) to parent viewgroup.
        //by hgkim 10/25
        if(mVideoCallCard != null)
        {   
            if(DBG)log(" == mVideoCallCard.addSurfaceviews() == ");
            mVideoCallCard.addSurfaceviews();
        }
        
		//updateScreen(); // More_Fast_Dialing

		// We have a valid number, so try to actually place a call:
		// make sure we pass along the URI as a reference to the contact.
		boolean bLoopbackCall = VideoTelephonyApp.getInstance(this).getLoopbackCall();
		int videoCallStatus = PhoneUtils.placeVideoCall(mPhone, number, intent
				.getData(), bLoopbackCall);


        // call cost is avalible only for VIDEO_CALL_STATUS_DIALED or VIDEO_CALL_STATUS_DIALED_MMI
        if (videoCallStatus != PhoneUtils.VIDEO_CALL_STATUS_FAILED)
        {
    		// LGE_CALL_COSTS START
    		Message msgGetAcmMax = mHandler.obtainMessage(AOC_QUERY_ICC_ACM_MAX);
    		mCmdIf.getAccumulatedCallMeterMax(msgGetAcmMax);
    		Message msgGetAcm = mHandler.obtainMessage(AOC_QUERY_ICC_ACM);
    		mCmdIf.getAccumulatedCallMeter(msgGetAcm);
    		// LGE_CALL_COSTS END
        }

		switch (videoCallStatus) {
    		case PhoneUtils.VIDEO_CALL_STATUS_DIALED:
    			if (DBG)log("placeVideoCall: PhoneUtils.placeVideoCall() succeeded for video call '"+ number + "'.");
    			return InCallInitStatus.SUCCESS;
                
    		case PhoneUtils.VIDEO_CALL_STATUS_DIALED_MMI:
    			if (DBG) log("placeVideoCall: specified number was an MMI code: '"+ number + "'.");
    			return InCallInitStatus.DIALED_MMI;
                
    		case PhoneUtils.VIDEO_CALL_STATUS_FAILED:
    			if (DBG) log("placeVideoCall: PhoneUtils.placeVideoCall() FAILED for number '"+ number + "'.");
    			return InCallInitStatus.CALL_FAILED;
                
    		default:
    	        if(DBG)
    			{
    	              Log.w(LOG_TAG, "placeVideoCall: unknown videoCallStatus "
    					+ videoCallStatus
    					+ " from PhoneUtils.placeVideoCall() for number '" + number
    					+ "'.");
    			}
    			return InCallInitStatus.SUCCESS; // Try to continue anyway...
		}
	}

	
//LGE_VT_FIXED START- 2010/11/09 
//Move this logic to OutgoingCallBroadcast.java 
	
//	/**
//	 * Checks the current ServiceState to make sure it's OK to try making an
//	 * outgoing call to the specified number.
//	 * 
//	 * @return InCallInitStatus.SUCCESS if it's OK to try calling the specified
//	 *         number. If not, like if the radio is powered off or we have no
//	 *         signal, return one of the other InCallInitStatus codes indicating
//	 *         what the problem is.
//	 */
//	private InCallInitStatus checkIfOkToInitiateOutgoingCall() {
//		// Watch out: do NOT use PhoneStateIntentReceiver.getServiceState()
//		// here;
//		// that's not guaranteed to be fresh. To synchronously get the
//		// CURRENT service state, ask the Phone object directly:
//		int state = mPhone.getServiceState().getState();
//		if (DBG)
//			log("checkIfOkToInitiateOutgoingCall: ServiceState = " + state);
//
//		// For Loopback call, return InCallInitStatus.SUCCESS
//		if (true == VideoTelephonyApp.getInstance().getLoopbackCall()) {
//			return InCallInitStatus.SUCCESS;
//		}
//
//		if (PhoneApp.getInstance().isLowBattery()) {
//			return InCallInitStatus.LOW_BATTERY;
//		}     
//
//		switch (state) {
//		case ServiceState.STATE_IN_SERVICE:
//			// Normal operation. It's OK to make outgoing calls.
//			return InCallInitStatus.SUCCESS;
//
//		case ServiceState.STATE_POWER_OFF:
//			// Radio is explictly powered off.
//			return InCallInitStatus.POWER_OFF;
//
//		case ServiceState.STATE_EMERGENCY_ONLY:
//			// LGE_NETWORK_STATUS START
//		case ServiceState.STATE_SERVICE_DETACHED:
//			// LGE_NETWORK_STATUS END
//			// The phone is registered, but locked. Only emergency
//			// numbers are allowed.
//			// Note that as of Android 2.0 at least, the telephony layer
//			// does not actually use ServiceState.STATE_EMERGENCY_ONLY,
//			// mainly since there's no guarantee that the radio/RIL can
//			// make this distinction. So in practice the
//			// InCallInitStatus.EMERGENCY_ONLY state and the string
//			// "incall_error_emergency_only" are totally unused.
//			return InCallInitStatus.EMERGENCY_ONLY;
//		case ServiceState.STATE_OUT_OF_SERVICE:
//			// No network connection.
//			return InCallInitStatus.OUT_OF_SERVICE;
//
//		default:
//			throw new IllegalStateException("Unexpected ServiceState: " + state);
//		}
//	}


//	/**
//	 * Checks the current dialing number is correct 
//	 * 
//	 * @return InCallInitStatus.SUCCESS if it's OK to try calling the specified
//	 *         number. If not, like if dialing string is wrong number.
//	 */
//	private InCallInitStatus checkIfOkToDialNumber(String number) 
//	{
//        if (number.compareToIgnoreCase("*") == 0
//            || number.compareToIgnoreCase("#") == 0
//            )
//        {
//            return InCallInitStatus.INVALID_NUMBER;
//        }            
//        else
//        {
//            // TODO: Add following conditions if needs --> Be careful if you add conditions
//            //  Check the dial number contains only numberic and #,  * , -, +
//            //   
//            //-- end        
//           
//            return InCallInitStatus.SUCCESS;
//        }
//	}
//LGE_VT_FIXED END- 2010/11/09

	/**
	 * Do some delayed cleanup after a Phone call gets disconnected.
	 * 
	 * This method gets called a couple of seconds after any DISCONNECT event
	 * from the Phone; it's triggered by the DELAYED_CLEANUP_AFTER_DISCONNECT
	 * message we send in onDisconnect().
	 * 
	 * If the Phone is totally idle right now, that means we've already shown
	 * the "call ended" state for a couple of seconds, and it's now time to
	 * endInCallScreenSession this activity.
	 * 
	 * If the Phone is *not* idle right now, that probably means that one call
	 * ended but the other line is still in use. In that case, we *don't* exit
	 * the in-call screen, but we at least turn off the backlight (which we
	 * turned on in onDisconnect().)
	 */
	// LGE_AUTO_REDIAL START
	// add parameter
	private void delayedCleanupAfterDisconnect(Connection.DisconnectCause data) {
		// LGE_AUTO_REDIAL END
		if (DBG)
			log("+++++++++ delayedCleanupAfterDisconnect()...  Phone state = "
					+ mPhone.getState());

		// Clean up any connections in the DISCONNECTED state.
		//
		// [Background: Even after a connection gets disconnected, its
		// Connection object still stays around, in the special
		// DISCONNECTED state. This is necessary because we we need the
		// caller-id information from that Connection to properly draw the
		// "Call ended" state of the CallCard.
		// But at this point we truly don't need that connection any
		// more, so tell the Phone that it's now OK to to clean up any
		// connections still in that state.]
		mPhone.clearDisconnected();
		if (!phoneIsInUse()) {

			// Phone is idle! We should exit this screen now.
			if (DBG)
				log("- delayedCleanupAfterDisconnect: phone is idle...");

			// And (finally!) exit from the in-call screen
			// (but not if we're already in the process of pausing...)
			if (mIsForegroundActivity) {
				
				
				// If this is a call that was initiated by the user, and
				// we're *not* in emergency mode, finish the call by
				// taking the user to the Call Log.
				// Otherwise we simply call endInCallScreenSession, which will
				// take us
				// back to wherever we came from.
				if (mShowCallLogAfterDisconnect && !isPhoneStateRestricted()) {
					if (DBG)
						log("- Show Call Log after disconnect...");
					final Intent intent = PhoneApp.createCallLogIntent();
					intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
					startActivity(intent);
					// Even in this case we still call endInCallScreenSession
					// (below),
					// to make sure we don't stay in the activity history.
				}
				
				endInCallScreenSession();

			}
		} else {
			// The phone is still in use. Stay here in this activity, but
			// we don't need to keep the screen on.

			if (DBG)
				PhoneUtils.dumpCallState(mPhone);
		}
		
	}

	public void onSpeakerClick() {
		if (DBG)
			log("onSpeakerClick()...");

		// TODO: Turning on the speaker seems to enable the mic
		// whether or not the "mute" feature is active!
		// Not sure if this is an feature of the telephony API
		// that I need to handle specially, or just a bug.
		boolean newSpeakerState = !PhoneUtils.isSpeakerOn(this);
		if (newSpeakerState && isBluetoothAvailable()
				&& isBluetoothAudioConnected()) {
			disconnectBluetoothAudio();
		}

		if(newSpeakerState)
			VideoTelephonyApp.getInstance(this).setAudioPath(VideoTelephonyApp.VTSNDPATH_SPEAKER);
		else
			VideoTelephonyApp.getInstance(this).setAudioPath(VideoTelephonyApp.VTSNDPATH_RECEIVER);

		PhoneUtils.turnOnSpeaker(this, newSpeakerState, true);
	}

	private void onBluetoothClick() {
		if (DBG)
			log("onBluetoothClick()...");

		if (isBluetoothAvailable()) {
			// Toggle the bluetooth audio connection state:
			if (isBluetoothAudioConnected()) {
				disconnectBluetoothAudio();
			} else {
				// Manually turn the speaker phone off, instead of allowing the
				// Bluetooth audio routing handle it. This ensures that the rest
				// of the speakerphone code is executed, and reciprocates the
				// menuSpeaker code above in onClick(). The onClick() code
				// disconnects the active bluetooth headsets when the
				// speakerphone is turned on.
				if (PhoneUtils.isSpeakerOn(this)) {
					PhoneUtils.turnOnSpeaker(this, false, true);
				}

				VideoTelephonyApp.getInstance(this).setAudioPath(VideoTelephonyApp.VTSNDPATH_BLUETOOTH);
				connectBluetoothAudio();
			}
		} else {
			// Bluetooth isn't available; the "Audio" button shouldn't have
			// been enabled in the first place!
            if(DBG)
			{
                Log.w(LOG_TAG,
                "Got onBluetoothClick, but bluetooth is unavailable");
			}
			
		}
	}

	/**
	 * Handles button clicks from the InCallTouchUi widget.
	 */
	/* package */void handleOnscreenButtonClick(int id) {
		if (DBG)
			log("handleOnscreenButtonClick(id " + id + ")...");

		switch (id) {
		// TODO: since every button here corresponds to a menu item that we
		// already handle in onClick(), maybe merge the guts of these two
		// methods into a separate helper that takes an ID (of either a menu
		// item *or* touch button) and does the appropriate user action.

		// Actions while an incoming call is ringing:
		case R.id.answerButton:
                        if(PhoneApp.getInstance().getCallFMCState() == 2) //LGE_FMC
                        {
                            if(DBG)
                                log("FMC Call is Active");
                             mVideoCallCard.mCommonFMC.setVisibility(View.INVISIBLE);
                             PhoneApp.getInstance().EndCallFMC();
                        }
                        else  
                        {
                             if(VTPreferences.OPERATOR.equals("SKT"))
                                    mVideoCallCard.mCommonFMC.setVisibility(View.GONE);
                            internalAnswerCall();
                        }
			break;
		case R.id.rejectButton:
			internalHangupRingingCall();
			break;

			// 20100322 SilentIncoming Start
		case R.id.btn_accept:
                        if(PhoneApp.getInstance().getCallFMCState() == 2) //LGE_FMC
                        {
                             if(DBG)
                                 log("FMC Call is Active");
                             mVideoCallCard.mCommonFMC.setVisibility(View.INVISIBLE);
                             PhoneApp.getInstance().EndCallFMC();
                        }
                        else
                        {
                             if(VTPreferences.OPERATOR.equals("SKT"))
                                    mVideoCallCard.mCommonFMC.setVisibility(View.GONE);
                            internalAnswerCall();
                        }
			break;
		case R.id.btn_send_sms:
			internalSendSMS();
			break;
		case R.id.btn_reject:
			// internalHangupRingingCall();
			internalHangup();
			break;
			// 20100322 SilentIncoming End

		case R.id.newAccount:       
			mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
			insertContact();
			mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
			break;
		case R.id.addAccount:           
			mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
			addContact();
			mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
			break;
		case R.id.voiceCall:        
			mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
			retryVoiceCall();
			break;
		case R.id.videoCall:                    
			if(mIsForegroundActivity == false)
				return;

			mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
			retryVideoCall();
			break;
		case R.id.message:       
			mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
			sendSMS();
			mHandler.sendEmptyMessage(DELAYED_CLEANUP_AFTER_DISCONNECT);
			break;

			// The other regular (single-tap) buttons used while in-call:
		case R.id.holdButton:
		case R.id.swapButton:
		case R.id.endButton:
		case R.id.dialpadButton:
		case R.id.bluetoothButton:
		case R.id.muteButton:
		case R.id.speakerButton:
		case R.id.addButton:
		case R.id.mergeButton:
		case R.id.cdmaMergeButton:
		case R.id.manageConferencePhotoButton:
		case R.id.transferButton:
			return;

		default:
		    if(DBG)
			Log.w(LOG_TAG, "handleOnscreenButtonClick: unexpected ID " + id);
			break;
		}

		// Just in case the user clicked a "stateful" menu item (i.e. one
		// of the toggle buttons), we force the in-call buttons to update,
		// to make sure the user sees the *new* current state.
		//
		// (But note that some toggle buttons may *not* immediately change
		// the state of the Phone, in which case the updateInCallTouchUi()
		// call here won't have any visible effect. Instead, those
		// buttons will get updated by the updateScreen() call that gets
		// triggered when the onPhoneStateChanged() event comes in.)
		//
		// TODO: updateInCallTouchUi() is overkill here; it would be
		// more efficient to update *only* the affected button(s).
		// Consider adding API for that. (This is lo-pri since
		// updateInCallTouchUi() is pretty cheap already...)
		updateInCallTouchUi();
	}

	// END_CALL send SMS, addContact
	void sendSMS() {
        	String number = mVideoCallCard.getDisplayPhoneNumer();
              if (number == null || !TextUtils.isGraphic(number)) {
                    Toast.makeText(this, "Error!! phone number is null", 1000).show();
			return;
		}
              
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
				SCHEME_SMSTO, number, null));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private void addContact() {
        	String number = mVideoCallCard.getDisplayPhoneNumer();
              if (number == null || !TextUtils.isGraphic(number)) {
                    Toast.makeText(this, "Error!! phone number is null", 1000).show();
			return;
		}
              
		//Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT); // "com.android.contacts.action.PICK_AND_EDIT"
	    	Intent intent = new Intent("com.android.contacts.action.INSERT_OR_EDIT");
		intent.putExtra(Insert.PHONE, number);
		intent.setType(Contacts.CONTENT_ITEM_TYPE);
		startActivity(intent);
	}

	private void insertContact() {
        	String number = mVideoCallCard.getDisplayPhoneNumer();
              if (number == null || !TextUtils.isGraphic(number)) {
                    Toast.makeText(this, "Error!! phone number is null", 1000).show();
			return;
		}
              
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.putExtra(Insert.PHONE, number);
		intent.setType(Contacts.CONTENT_TYPE);
		startActivity(intent);
	}

    	private void retryVoiceCall() {
		mShowCallLogAfterDisconnect = false;		
        	String number = mVideoCallCard.getDisplayPhoneNumer();

              if (number == null || !TextUtils.isGraphic(number)) {
                    Toast.makeText(this, "Error!! phone number is null", 1000).show();
			return;
		}
              
		delayedCleanupAfterDisconnect(null);
		unregisterForPhoneStates();
		stopTimer();

		PhoneApp.mIsVideoCall = false;
              mNeedShowCallLostDialog = false;
        
              updateScreen();
        	Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
              Uri.fromParts("tel", number, null));

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();

		if (DBG)
			log("retryVoiceCall number=" + number);		
	}

	private void retryVideoCall() {
	   if (DBG) log(" == reteryVideoCall()" );
        
              if(VTAppStateManager.getInstance().isCanRetry() == false)
              {
                    if(mRetryCount > 5)
                    {
                            mHandler.removeMessages(EVTENT_VTRETRY);
                            mRetryCount = 0;
                            return;
                     }
                    
            		mHandler.removeMessages(EVTENT_VTRETRY);
			Message msgFallback = mHandler.obtainMessage(EVTENT_VTRETRY);
			mHandler.sendMessageDelayed(msgFallback, VTRETRY_DELAY);
                     mRetryCount++;
                     if (DBG)log(" Delay reteryVideoCall " + mRetryCount);
                     return;
              }
              mRetryCount = 0;
              
              if (mInCallScreenMode == InCallScreenMode.CALL_ENDED)
              {
                      InCallInitStatus sInCallInitialStatus; 
        		mShowCallLogAfterDisconnect = false;
        		setInCallScreenMode(InCallScreenMode.NORMAL);

        		String number = mVideoCallCard.getDisplayPhoneNumer();

        		if (number == null || !TextUtils.isGraphic(number)) {
        			return;
        		}
                
        		Intent intent = new Intent(Intent.ACTION_VIDEO_CALL, Uri
        				.fromParts("tel", number, null));

                      sInCallInitialStatus =  placeVideoCall(intent);
        		if (InCallInitStatus.SUCCESS != sInCallInitialStatus)
        		{
                    	    handleStartupError(sInCallInitialStatus);
        		    return;
        		}


        		//It does't call onResume() in retryVideoCall, 
        		//so we call explicitly.
        		setInitPreference();
        		
                	//Always Every call start... then initialize VideoCallCard UI
        		mVideoCallCard.initFlags();
                     mVideoCallCard.buttonInitialize();

        		InitSndPath();

        		isSubstituImage = Settings.System.getInt(getContentResolver(),
        				Settings.System.VT_USE_PRIVATE, 0);
                      File file;
                      file = new File(Settings.System.getString(getContentResolver(),Settings.System.VT_PRIVATE_NAME));
                      if(!file.exists())
                            Settings.System.putString(getContentResolver(),Settings.System.VT_PRIVATE_NAME, mVideoCallCard.subsImagePath);
                      
        		if (isSubstituImage == 1) {
        			mVideoCallCard.showSubstitute(true, false);

        			// Update Button UI & State
        			if (mVideoCallCard != null)
        				mVideoCallCard.updateButtonUI(
        						VideoCallCard.BUTTON_PRIVATE_SHOW, true, getResources()
        						.getString(R.string.vt_Show));
        		} else {
        			if (mVideoCallCard != null)
        				mVideoCallCard.showSubstitute(false, false);
        		}

             		if (DBG)
        			log("retryVideoCall number=" + number);	  
              }
	}

	//

	/**
	 * Updates the "Press Menu for more options" hint based on the current state
	 * of the Phone.
	 */
	private void updateMenuButtonHint() {
	}

	/**
	 * Brings up UI to handle the various error conditions that can occur when
	 * first initializing the in-call UI. This is called from onResume() if we
	 * encountered an error while processing our initial Intent.
	 * 
	 * @param status
	 *            one of the InCallInitStatus error codes.
	 */
	private void handleStartupError(InCallInitStatus status) {
		if (DBG)
			log("handleStartupError(): status = " + status);

		// NOTE that the regular Phone UI is in an uninitialized state at
		// this point, so we don't ever want the user to see it.
		// That means:
		// - Any cases here that need to go to some other activity should
		// call startActivity() AND immediately call endInCallScreenSession
		// on this one.
		// - Any cases here that bring up a Dialog must ensure that the
		// Dialog handles both OK *and* cancel by calling
		// endInCallScreenSession.
		// Activity. (See showGenericErrorDialog() for an example.)

		switch (status) {
		case POWER_OFF:
			// Radio is explictly powered off.

			// TODO: This UI is ultra-simple for 1.0. It would be nicer
			// to bring up a Dialog instead with the option "turn on radio
			// now". If selected, we'd turn the radio on, wait for
			// network registration to complete, and then make the call.

			showGenericErrorDialog(R.string.incall_error_power_off, true);
			break;

		case EMERGENCY_ONLY:
			// Only emergency numbers are allowed, but we tried to dial
			// a non-emergency number.
			showGenericErrorDialog(R.string.incall_error_emergency_only, true);
			break;

		case OUT_OF_SERVICE:
			// No network connection.
			showGenericErrorDialog(R.string.incall_error_out_of_service, true);
			break;

		case PHONE_NOT_IN_USE:
			// This error is handled directly in onResume() (by bailing
			// out of the activity.) We should never see it here.
		    if(DBG)
			Log.w(LOG_TAG,
			"handleStartupError: unexpected PHONE_NOT_IN_USE status");
			break;

		case NO_PHONE_NUMBER_SUPPLIED:
			// The supplied Intent didn't contain a valid phone number.
			// TODO: Need UI spec for this failure case; for now just
			// show a generic error.
			showGenericErrorDialog(
					R.string.incall_error_no_phone_number_supplied, true);
			break;

		case DIALED_MMI:
			// Our initial phone number was actually an MMI sequence.
			// There's no real "error" here, but we do bring up the
			// a Toast (as requested of the New UI paradigm).
			//
			// In-call MMIs do not trigger the normal MMI Initiate
			// Notifications, so we should notify the user here.
			// Otherwise, the code in PhoneUtils.java should handle
			// user notifications in the form of Toasts or Dialogs.
			if (mPhone.getState() == Phone.State.OFFHOOK) {
				Toast.makeText(this, R.string.incall_status_dialed_mmi,
						Toast.LENGTH_SHORT).show();
			}
			break;

		case CALL_FAILED:
			// We couldn't successfully place the call; there was some
			// failure in the telephony layer.
			// TODO: Need UI spec for this failure case; for now just
			// show a generic error.
			showGenericErrorDialog(R.string.incall_error_call_failed, true);
			break;


//LGE_VT_FIXED START- 2010/11/09 
//Move this logic to OutgoingCallBroadcast.java - onCreate()			
//              case INVALID_NUMBER:
//                    showGenericErrorDialog(R.string.vt_invalid_number, true);
//                    break;
//LGE_VT_FIXED END- 2010/11/09

            case LOW_BATTERY:
                showGenericErrorDialog(R.string.vt_lowbattery, true);
                break;
                
		default:
		    if(DBG)
			Log.w(LOG_TAG, "handleStartupError: unexpected status code "
					+ status);
			showGenericErrorDialog(R.string.incall_error_call_failed, true);
			break;
		}
	}

	/**
	 * Utility function to bring up a generic "error" dialog, and then bail out
	 * of the in-call UI when the user hits OK (or the BACK button.)
	 */
	private void showGenericErrorDialog(int resid, boolean isStartupError) {
		CharSequence msg = getResources().getText(resid);
		if (DBG)
			log("showGenericErrorDialog()" + msg + isStartupError);

		// create the clicklistener and cancel listener as needed.
		DialogInterface.OnClickListener clickListener;
		OnCancelListener cancelListener;
		if (isStartupError) {
			clickListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					bailOutAfterErrorDialog();
				}
			};
			cancelListener = new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					bailOutAfterErrorDialog();
				}
			};
		} else {
			clickListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					delayedCleanupAfterDisconnect(null);
				}
			};
			cancelListener = new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					delayedCleanupAfterDisconnect(null);
				}
			};
		}

		// TODO: Consider adding a setTitle() call here (with some generic
		// "failure" title?)
		mGenericErrorDialog = new AlertDialog.Builder(this).setMessage(msg)
		.setPositiveButton(R.string.ok, clickListener)
		.setOnCancelListener(cancelListener).create();

		// When the dialog is up, completely hide the in-call UI
		// underneath (which is in a partially-constructed state).
		mGenericErrorDialog.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DIM_BEHIND);

		mGenericErrorDialog.show();
	}

	private void bailOutAfterErrorDialog() {
		if (mGenericErrorDialog != null) {
			log("bailOutAfterErrorDialog: DISMISSING mGenericErrorDialog.");
			mGenericErrorDialog.dismiss();
			mGenericErrorDialog = null;
		}

        // Jaehun.ryu 2010.11.04 : Do not exit the Phone app when mInCallInitialStatus is CALL_FAILED
        //  CALL_FAILED is the only case for invalid call state for dialing
        if (mInCallInitialStatus != InCallInitStatus.CALL_FAILED)
        {
            log("bailOutAfterErrorDialog(): end InVideoCallScreen session...");
            // restore mInCallInitialStatus 
            mInCallInitialStatus = InCallInitStatus.SUCCESS; 
            //-- end 
            
            endInCallScreenSession();
        }
	}

	/**
	 * Dismisses (and nulls out) all persistent Dialogs managed by the
	 * InVideoCallScreen. Useful if (a) we're about to bring up a dialog and want to
	 * pre-empt any currently visible dialogs, or (b) as a cleanup step when the
	 * Activity is going away.
	 */
	private void dismissAllDialogs() {
		log("dismissAllDialogs()...");

		// Note it's safe to dismiss() a dialog that's already dismissed.
		// (Even if the AlertDialog object(s) below are still around, it's
		// possible that the actual dialog(s) may have already been
		// dismissed by the user.)
		if (mGenericErrorDialog != null) {
		
			log("- DISMISSING mGenericErrorDialog.");
			mGenericErrorDialog.dismiss();
			mGenericErrorDialog = null;

                         if ((mPhone.getState() == Phone.State.RINGING) && (PhoneApp.mIsVideoCall == false)){
                        	mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
            			endInCallScreenSession();
                        }
		}

		if (mFallbackDialog != null) {
			if (DBG)
				log("- DISMISSING mFallbackDialog.");
			mFallbackDialog.dismiss();
			mFallbackDialog = null;
            
                         if ((mPhone.getState() == Phone.State.RINGING) && (PhoneApp.mIsVideoCall == false)){
                    		mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
        			mHandler.removeMessages(AUTOMATIC_FALLBACK);
        			endInCallScreenSession();
                    }
		}
	}

	//
	// Helper functions for answering incoming calls.
	//

	/**
	 * Answer a ringing call. This method does nothing if there's no ringing or
	 * waiting call.
	 */
	/* package */void internalAnswerCall() {
		// if (DBG) log("internalAnswerCall()...");
		// if (DBG) PhoneUtils.dumpCallState(mPhone);

		final boolean hasRingingCall = !mRingingCall.isIdle();

      		if (PhoneApp.getInstance().isLowBattery() && hasRingingCall) {
                  PhoneApp.mLowbatteryHangup = true;
                  internalHangupRingingCall();
		    return;
      		} 

		if (hasRingingCall) {
			int phoneType = mPhone.getPhoneType();
			if (phoneType == Phone.PHONE_TYPE_CDMA) {
				if (DBG)
					log("internalAnswerCall: answering (CDMA)...");
				// In CDMA this is simply a wrapper around
				// PhoneUtils.answerCall().
				PhoneUtils.answerCall(mPhone); // Automatically holds the
				// current active call,
				// if there is one
			} else if (phoneType == Phone.PHONE_TYPE_GSM) {
				// GSM: this is usually just a wrapper around
				// PhoneUtils.answerCall(), *but* we also need to do
				// something special for the "both lines in use" case.

				final boolean hasActiveCall = !mForegroundCall.isIdle();
				final boolean hasHoldingCall = !mBackgroundCall.isIdle();

				if (hasActiveCall && hasHoldingCall) {
					if (DBG)
						log("internalAnswerCall: answering (both lines in use!)...");
					// The relatively rare case where both lines are
					// already in use. We "answer incoming, end ongoing"
					// in this case, according to the current UI spec.
					PhoneUtils.answerAndEndActive(mPhone);

					// Alternatively, we could use
					// PhoneUtils.answerAndEndHolding(mPhone);
					// here to end the on-hold call instead.
				} else {
					if (DBG)
						log("internalAnswerCall: answering...");
					PhoneUtils.answerCall(mPhone); // Automatically holds the
					// current active call,
					// if there is one
				}
			} else {
				throw new IllegalStateException("Unexpected phone type: "
						+ phoneType);
			}
		}
	}

	/**
	 * Answer the ringing call *and* hang up the ongoing call.
	 */
	/* package */void internalAnswerAndEnd() {
		if (DBG)
			log("internalAnswerAndEnd()...");
		// if (DBG) PhoneUtils.dumpCallState(mPhone);
		PhoneUtils.answerAndEndActive(mPhone);
	}

	/**
	 * Hang up the ringing call (aka "Don't answer").
	 */
	/* package */void internalHangupRingingCall() {
		if (DBG)
			log("internalHangupRingingCall()...");
		PhoneUtils.hangupRingingCall(mPhone);
	}

	/**
	 * Hang up the current active call.
	 */
	/* package */void internalHangup() {
		if (DBG)
			log("internalHangup()...");
		PhoneUtils.hangup(mPhone);
	}

	/**
	 * Sets the current high-level "mode" of the in-call UI.
	 * 
	 * NOTE: if newMode is CALL_ENDED, the caller is responsible for posting a
	 * delayed DELAYED_CLEANUP_AFTER_DISCONNECT message, to make sure the
	 * "call ended" state goes away after a couple of seconds.
	 */
	private void setInCallScreenMode(InCallScreenMode newMode) {
		if (DBG)
			log("setInCallScreenMode: " + newMode);
		mInCallScreenMode = newMode;
		switch (mInCallScreenMode) {
		case MANAGE_CONFERENCE:
			break;

		case CALL_ENDED:
			// Display the CallCard (in the "Call ended" state)
			// and hide all other UI.
			updateMenuButtonHint(); // Hide the Menu button hint

			// Make sure the CallCard (which is a child of mInCallPanel) is
			// visible.
			if (!(mInVideoCallPanel == null))
				mInVideoCallPanel.setVisibility(View.VISIBLE);

			break;

		case NORMAL:
			if (!(mInVideoCallPanel == null))
				mInVideoCallPanel.setVisibility(View.VISIBLE);
			break;

		case OTA_NORMAL:
			break;

		case OTA_ENDED:
			break;

		case UNDEFINED:
			break;
		}

		// Update the in-call touch UI on any state change (since it may
		// need to hide or re-show itself.)
		updateInCallTouchUi();
	}

	/**
	 * Initializes the in-call touch UI on devices that need it.
	 */
	private void initInCallTouchUi() {
		if (DBG)
			log("initInCallTouchUi()...");
		// TODO: we currently use the InCallTouchUi widget in at least
		// some states on ALL platforms. But if some devices ultimately
		// end up not using *any* onscreen touch UI, we should make sure
		// to not even inflate the InCallTouchUi widget on those devices.
		mInCallTouchUi = (InCallTouchUi) findViewById(R.id.inCallTouchUi);
		mInCallTouchUi.setInCallScreenInstance(this);
	}

	/**
	 * Updates the state of the in-call touch UI.
	 * 
	 * @param emergencyFlag2
	 */
	private void updateInCallTouchUi() {
		if (mInCallTouchUi != null) {
			// 20100804 jongwany.lee@lge.com attached it for CALL UI
			if (mInCallScreenMode != InCallScreenMode.CALL_ENDED)
				mInCallTouchUi.updateState(mPhone, false);
		}
	}

	private void updateEndCallTouchUi(boolean isVisible) {
		if (DBG)
			log("updateEndCallTouchUi isVisible..." + isVisible);

		if (mInCallTouchUi != null) {
			if (mVideoCallCard != null) {
				mInCallTouchUi.setNameInContact(mVideoCallCard
						.hasNameInContact());

                            if(isVisible)
                            {
                                if(mVideoCallCard.getName().equals(getString(R.string.private_num)))
                                {
                                	isVisible = false;
                                }

                                String number = mVideoCallCard.getDisplayPhoneNumer();
                                if (number == null || !TextUtils.isGraphic(number)) {
                                      isVisible = false;
                                }                               
                           }
			} 
			mInCallTouchUi.UpdateEndCallControls_VT(isVisible);
		}
	}

	/**
	 * @return true if the onscreen touch UI is enabled (for regular
	 *         "ongoing call" states) on the current device.
	 */
	public boolean isTouchUiEnabled() {
		return (mInCallTouchUi != null) && mInCallTouchUi.isTouchUiEnabled();
	}

	/**
	 * @return true if the onscreen touch UI is enabled for the "incoming call"
	 *         state on the current device.
	 */
	public boolean isIncomingCallTouchUiEnabled() {
		return (mInCallTouchUi != null)
		&& mInCallTouchUi.isIncomingCallTouchUiEnabled();
	}

	/**
	 * Posts a handler message telling the InVideoCallScreen to update the onscreen
	 * in-call touch UI.
	 * 
	 * This is just a wrapper around updateInCallTouchUi(), for use by the rest
	 * of the phone app or from a thread other than the UI thread.
	 */
	/* package */void requestUpdateTouchUi() {
		if (DBG)
			log("requestUpdateTouchUi()...");

		mHandler.removeMessages(REQUEST_UPDATE_TOUCH_UI);
		mHandler.sendEmptyMessage(REQUEST_UPDATE_TOUCH_UI);
	}

    private void updateInCallCallCardUi() {
        if (mVideoCallCard != null) {
            if (DBG) log("updateInCallCallCardUi()...");
            mVideoCallCard.updateState(mPhone);
        }
    }

    void requestUpdateCallCardUi() {
        if (DBG) log("requestUpdateCallCardUi()...");

        mHandler.removeMessages(REQUEST_UPDATE_CALLCARD_UI);
        mHandler.sendEmptyMessage(REQUEST_UPDATE_CALLCARD_UI);
    }
    
	/**
	 * @return true if it's OK to display the in-call touch UI, given the
	 *         current state of the InVideoCallScreen.
	 */
	/* package */boolean okToShowInCallTouchUi() {
		// Note that this method is concerned only with the internal state
		// of the InVideoCallScreen. (The InCallTouchUi widget has separate
		// logic to make sure it's OK to display the touch UI given the
		// current telephony state, and that it's allowed on the current
		// device in the first place.)

		// The touch UI is NOT available if:
		// - we're in some InCallScreenMode other than NORMAL
		// (like CALL_ENDED or one of the OTA modes)
		return (mInCallScreenMode == InCallScreenMode.NORMAL);
	}

	/**
	 * @return true if we're in restricted / emergency dialing only mode.
	 */
	public boolean isPhoneStateRestricted() {
		// TODO: This needs to work IN TANDEM with the KeyGuardViewMediator
		// Code.
		// Right now, it looks like the mInputRestricted flag is INTERNAL to the
		// KeyGuardViewMediator and SPECIFICALLY set to be FALSE while the
		// emergency
		// phone call is being made, to allow for input into the InVideoCallScreen.
		// Having the InVideoCallScreen judge the state of the device from this flag
		// becomes meaningless since it is always false for us. The mediator
		// should
		// have an additional API to let this app know that it should be
		// restricted.
		return ((mPhone.getServiceState().getState() == ServiceState.STATE_EMERGENCY_ONLY)
				|| (mPhone.getServiceState().getState() == ServiceState.STATE_OUT_OF_SERVICE) || (PhoneApp
						.getInstance().getKeyguardManager()
						.inKeyguardRestrictedInputMode()));
	}

	//
	// In-call menu UI
	//

	/**
	 * Override onCreatePanelView(), in order to get complete control over the
	 * UI that comes up when the user presses MENU.
	 * 
	 * This callback allows me to return a totally custom View hierarchy (with
	 * custom layout and custom "item" views) to be shown instead of a standard
	 * android.view.Menu hierarchy.
	 * 
	 * This gets called (with featureId == FEATURE_OPTIONS_PANEL) every time we
	 * need to bring up the menu. (And in cases where we return non-null, that
	 * means that the "standard" menu callbacks onCreateOptionsMenu() and
	 * onPrepareOptionsMenu() won't get called at all.)
	 */
	// ////@Override
	public View onCreatePanelView(int featureId) {
		if (DBG)
			log("onCreatePanelView(featureId = " + featureId + ")...");
		return null;
	}

	/**
	 * Dismisses the menu panel (see onCreatePanelView().)
	 * 
	 * @param dismissImmediate
	 *            If true, hide the panel immediately. If false, leave the menu
	 *            visible onscreen for a brief interval before dismissing it (so
	 *            the user can see the state change resulting from his original
	 *            click.)
	 */
	/* package */void dismissMenu(boolean dismissImmediate) {
		if (DBG)
			log("dismissMenu(immediate = " + dismissImmediate + ")...");

		if (dismissImmediate) {
			closeOptionsMenu();
		} else {
			mHandler.removeMessages(DISMISS_MENU);
			mHandler.sendEmptyMessageDelayed(DISMISS_MENU, MENU_DISMISS_DELAY);
			// This will result in a dismissMenu(true) call shortly.
		}
	}

	/**
	 * Override onPanelClosed() to capture the panel closing event, allowing us
	 * to set the poke lock correctly whenever the option menu panel goes away.
	 */
	// ////@Override
	public void onPanelClosed(int featureId, Menu menu) {
		super.onPanelClosed(featureId, menu);
	}

	public void onSubPanelClosed(int featureId, Menu menu) {
		if (DBG)
			log("onPanelClosed(featureId = " + featureId + ")...");

		// We only want this special behavior for the "options panel"
		// feature (i.e. the standard menu triggered by the MENU button.)
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			// TODO: May need to return to the original wake state here
			// if onCreatePanelView ends up changing the wake state.
		}
	}

	//
	// Bluetooth helper methods.
	//
	// - BluetoothAdapter is the Bluetooth system service. If
	// getDefaultAdapter() returns null
	// then the device is not BT capable. Use BluetoothDevice.isEnabled()
	// to see if BT is enabled on the device.
	//
	// - BluetoothHeadset is the API for the control connection to a
	// Bluetooth Headset. This lets you completely connect/disconnect a
	// headset (which we don't do from the Phone UI!) but also lets you
	// get the address of the currently active headset and see whether
	// it's currently connected.
	//
	// - BluetoothHandsfree is the API to control the audio connection to
	// a bluetooth headset. We use this API to switch the headset on and
	// off when the user presses the "Bluetooth" button.
	// Our BluetoothHandsfree instance (mBluetoothHandsfree) is created
	// by the PhoneApp and will be null if the device is not BT capable.
	//

	/**
	 * @return true if the Bluetooth on/off switch in the UI should be available
	 *         to the user (i.e. if the device is BT-capable and a headset is
	 *         connected.)
	 */
	/* package */boolean isBluetoothAvailable() {
		if (mBluetoothHandsfree == null) {
			// Device is not BT capable.
			if (DBG)
				log("isBluetoothAvailable() ==> FALSE (not BT capable) mBluetoothHandsfree is null");
			return false;
		}


		// There's no need to ask the Bluetooth system service if BT is enabled:
		//
		// BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		// if ((adapter == null) || !adapter.isEnabled()) {
		// if (DBG) log("  ==> FALSE (BT not enabled)");
		// return false;
		// }
		// if (DBG) log("  - BT enabled!  device name " + adapter.getName()
		// + ", address " + adapter.getAddress());
		//
		// ...since we already have a BluetoothHeadset instance. We can just
		// call isConnected() on that, and assume it'll be false if BT isn't
		// enabled at all.

		// Check if there's a connected headset, using the BluetoothHeadset API.
		boolean isConnected = false;
		if (mBluetoothHeadset != null) {
			BluetoothDevice headset = mBluetoothHeadset.getCurrentHeadset();
			if (headset != null) {
				isConnected = mBluetoothHeadset.isConnected(headset);
			}
		}

		if (DBG)
			log(" isBluetoothAvailable() isConnected=" + isConnected);
		return isConnected;
	}

	/**
	 * @return true if a BT device is available, and its audio is currently
	 *         connected.
	 */
	/* package */boolean isBluetoothAudioConnected() {
		if (mBluetoothHandsfree == null) {
			if (DBG)
				log("isBluetoothAudioConnected: ==> FALSE (null mBluetoothHandsfree)");
			return false;
		}
		boolean isAudioOn = mBluetoothHandsfree.isAudioOn();
		if (DBG)
			log("isBluetoothAudioConnected: ==> isAudioOn = " + isAudioOn);
		return isAudioOn;
	}

	/**
	 * Helper method used to control the state of the green LED in the
	 * "Bluetooth" menu item.
	 * 
	 * @return true if a BT device is available and its audio is currently
	 *         connected, <b>or</b> if we issued a
	 *         BluetoothHandsfree.userWantsAudioOn() call within the last 5
	 *         seconds (which presumably means that the BT audio connection is
	 *         currently being set up, and will be connected soon.)
	 */
	/* package */boolean isBluetoothAudioConnectedOrPending() {
		if (isBluetoothAudioConnected()) {
			if (DBG)
				log("isBluetoothAudioConnectedOrPending: ==> TRUE (really connected)");
			return true;
		}

		// If we issued a userWantsAudioOn() call "recently enough", even
		// if BT isn't actually connected yet, let's still pretend BT is
		// on. This is how we make the green LED in the menu item turn on
		// right away.
		if (mBluetoothConnectionPending) {
			long timeSinceRequest = SystemClock.elapsedRealtime()
			- mBluetoothConnectionRequestTime;
			if (timeSinceRequest < 5000 /* 5 seconds */) {
				if (DBG)
					log("isBluetoothAudioConnectedOrPending: ==> TRUE (requested "
							+ timeSinceRequest + " msec ago)");
				return true;
			} else {
				if (DBG)
					log("isBluetoothAudioConnectedOrPending: ==> FALSE (request too old: "
							+ timeSinceRequest + " msec ago)");
				mBluetoothConnectionPending = false;
				return false;
			}
		}

		if (DBG)
			log("isBluetoothAudioConnectedOrPending: ==> FALSE");
		return false;
	}

	/**
	 * Posts a message to our handler saying to update the onscreen UI based on
	 * a bluetooth headset state change.
	 */
	/* package */void requestUpdateBluetoothIndication() {
		if (DBG)
			log("requestUpdateBluetoothIndication()...");
		// No need to look at the current state here; any UI elements that
		// care about the bluetooth state (i.e. the CallCard) get
		// the necessary state directly from PhoneApp.showBluetoothIndication().
		mHandler.removeMessages(REQUEST_UPDATE_BLUETOOTH_INDICATION);
		mHandler.sendEmptyMessage(REQUEST_UPDATE_BLUETOOTH_INDICATION);
	}

	private void dumpBluetoothState() {
	    if(DBG)
	    {
	        log("============== dumpBluetoothState() =============");
	        log("= isBluetoothAvailable: " + isBluetoothAvailable());
	        log("= isBluetoothAudioConnected: " + isBluetoothAudioConnected());
	        log("= isBluetoothAudioConnectedOrPending: "
	                + isBluetoothAudioConnectedOrPending());
	        log("= PhoneApp.showBluetoothIndication: "
	                + PhoneApp.getInstance().showBluetoothIndication());
	        log("=");	        
	    }
		if (mBluetoothHandsfree != null) {
		    if(DBG)
			    log("= BluetoothHandsfree.isAudioOn: " + mBluetoothHandsfree.isAudioOn());
			if (mBluetoothHeadset != null) {
				BluetoothDevice headset = mBluetoothHeadset.getCurrentHeadset();
				if(DBG)
				    log("= BluetoothHeadset.getCurrentHeadset: " + headset);
				if (headset != null) {
				    if(DBG)
					{
				        log("= BluetoothHeadset.isConnected: "
				                + mBluetoothHeadset.isConnected(headset));
					}	
				}
			} else {
                if(DBG)
                    log("= mBluetoothHeadset is null");
			}
		} else {
            if(DBG)
                log("= mBluetoothHandsfree is null; device is not BT capable");
		}
	}

	/* package */void connectBluetoothAudio() {
		if (DBG)
			log("connectBluetoothAudio()...");
		if (mBluetoothHandsfree != null) {
			mBluetoothHandsfree.userWantsAudioOn();
		}

		// Watch out: The bluetooth connection doesn't happen instantly;
		// the userWantsAudioOn() call returns instantly but does its real
		// work in another thread. Also, in practice the BT connection
		// takes longer than MENU_DISMISS_DELAY to complete(!) so we need
		// a little trickery here to make the menu item's green LED update
		// instantly.
		// (See isBluetoothAudioConnectedOrPending() above.)
		mBluetoothConnectionPending = true;
		mBluetoothConnectionRequestTime = SystemClock.elapsedRealtime();
	}

	/* package */void disconnectBluetoothAudio() {
		if (DBG)
			log("disconnectBluetoothAudio()...");
		if (mBluetoothHandsfree != null) {
			mBluetoothHandsfree.userWantsAudioOff();
		}
		mBluetoothConnectionPending = false;
	}

	// Any user activity while the dialpad is up, but not locked, should
	// reset the touch lock timer back to the full delay amount.
	// ////@Override
	public void onUserInteraction() {
	}

	/**
	 * Updates the background of the InVideoCallScreen to indicate the state of the
	 * current call(s).
	 */
	private void updateInCallBackground() {
		final boolean hasRingingCall = !mRingingCall.isIdle();
		final boolean hasActiveCall = !mForegroundCall.isIdle();
		final boolean hasHoldingCall = !mPhone.getBackgroundCall().isIdle();
		final PhoneApp app = PhoneApp.getInstance();
		final boolean bluetoothActive = app.showBluetoothIndication();

		int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;

		// Possible states of the background are:
		// - bg_in_call_gradient_bluetooth.9.png // blue
		// - bg_in_call_gradient_connected.9.png // green
		// - bg_in_call_gradient_ended.9.png // red
		// - bg_in_call_gradient_on_hold.9.png // orange
		// - bg_in_call_gradient_unidentified.9.png // gray

		if (hasRingingCall) {
			// There's an INCOMING (or WAITING) call.
			if (bluetoothActive) {
				backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
			} else {
				backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
			}
		} else if (hasHoldingCall && !hasActiveCall) {
			// No foreground call, but there is a call on hold.
			backgroundResId = R.drawable.bg_in_call_gradient_on_hold;
		} else {
			// In all cases other than "ringing" and "on hold", the state
			// of the foreground call determines the background.
			final Call.State fgState = mForegroundCall.getState();
			switch (fgState) {
			case ACTIVE:
			case DISCONNECTING: // Call will disconnect soon, but keep showing
				// the normal "connected" background for now.
				if (bluetoothActive) {
					backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
				} else {
					backgroundResId = R.drawable.bg_in_call_gradient_connected;
				}
				break;

			case DISCONNECTED:
				backgroundResId = R.drawable.bg_in_call_gradient_ended;
				break;

			case DIALING:
			case ALERTING:
				if (bluetoothActive) {
					backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
				} else {
					backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
				}
				break;

			default:
				// Foreground call is (presumably) IDLE.
				// We're not usually here at all in this state, but
				// this *does* happen in some unusual cases (like
				// while displaying an MMI result).
				// Use the most generic background.
				backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
				break;
			}
		}
		mMainFrame_videocall.setBackgroundResource(backgroundResId);
		if (mForegroundCall.getState() == Call.State.DISCONNECTED) {
			updateEndCallTouchUi(true);
		}
	}

	public void resetInCallScreenMode() {
		if (DBG)
			log("resetInCallScreenMode - InCallScreenMode set to UNDEFINED");
		setInCallScreenMode(InCallScreenMode.UNDEFINED);
	}

	/**
	 * Updates the onscreen hint displayed while the user is dragging one of the
	 * handles of the RotarySelector widget used for incoming calls.
	 * 
	 * @param hintTextResId
	 *            resource ID of the hint text to display, or 0 if no hint
	 *            should be visible.
	 * @param hintColorResId
	 *            resource ID for the color of the hint text
	 */
	/* package */void updateSlidingTabHint(int hintTextResId, int hintColorResId) {
		if (DBG)
			log("updateRotarySelectorHint(" + hintTextResId + ")...");
		if (mVideoCallCard != null) {
			mVideoCallCard.setRotarySelectorHint(hintTextResId, hintColorResId);
			mVideoCallCard.updateState(mPhone);
			// TODO: if hintTextResId == 0, consider NOT clearing the onscreen
			// hint right away, but instead post a delayed handler message to
			// keep it onscreen for an extra second or two. (This might make
			// the hint more helpful if the user quickly taps one of the
			// handles without dragging at all...)
			// (Or, maybe this should happen completely within the
			// RotarySelector
			// widget, since the widget itself probably wants to keep the
			// colored
			// arrow visible for some extra time also...)
		}
	}

	// ////@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		return super.dispatchPopulateAccessibilityEvent(event);
	}

	public void dispatchSubPopulateAccessibilityEvent(AccessibilityEvent event) {
	}

	private void log(String msg) {
	    if(DBG)
	    {
	        Log.d(LOG_TAG, msg);    
	    }
	}

	// LGE_AUTO_REDIAL START
	public boolean disableLog() {
		return false;
	}

	// LGE_AUTO_REDIAL END

	public Phone getPhone() {
		return mPhone;
	}

	/**
	 * get Video Call UI instance - VideoCallCard.
	 * 
	 * @See
	 * */
	public VideoCallCard getVideoCallCard() {
		return mVideoCallCard;
	}

	// ///////////////////////////////////////////////////////////////
	// Menu Test
	// ////////////////////////////////////////////////////////////////
	public boolean onCreateOptionsMenu(Menu menu) {
		if (DBG)
			log("onCreateOptionsMenu");
        
		// 100820 by hgkim
		// Only use a menu in Active state
		// Disable Menu if DialPad is enabled
		VTAppStateManager stateManager = VTAppStateManager.getInstance();
		if (false == stateManager.isActive()
            || (false == stateManager.isRecvFirstFrame())
            ||  mVideoCallCard.IsCaptureMenu()
            ||  mVideoCallCard.isDialpad() )  
        {
            log("onCreateOptionsMenu() is returned " );
            return true;
        }
              
		MenuItem menu1 = menu.add(0, 10, 0, getResources().getString(
				R.string.vt_option_use_2nd_camera));
              menu1.setIcon(R.drawable.vt_ic_menu_swap_camera);
              
		MenuItem menu2 = menu.add(1, 20, 1, getResources().getString(
				R.string.vt_option_capture));
              menu2.setIcon(R.drawable.vt_ic_menu_capture);
              
              if(isBluetoothAudioConnected())
             {
	            MenuItem menu3 = menu.add(2, 30, 2, getResources().getString(
			R.string.incallmenu_uncheck_bluetooth));                        
                    menu3.setIcon(R.drawable.vt_ic_menu_bluetooth_off);
             }
              else
             {
	             MenuItem menu3 = menu.add(2, 30, 2, getResources().getString(
			R.string.incallmenu_check_bluetooth));                         
                    menu3.setIcon(R.drawable.vt_ic_menu_bluetooth);
             }

		MenuItem menu4 = menu.add(3, 40, 3, getResources().getString(
				R.string.vt_option_camera_setting));
              menu4.setIcon(R.drawable.vt_ic_menu_camera_setting);

		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {

		if (DBG)
			log("onPrepareOptionsMenu " + menu.size());

		// 100820 by hgkim
		// Only use a menu in Active state
		// Disable Menu if DialPad is enabled
		VTAppStateManager stateManager = VTAppStateManager.getInstance();
		if (false == stateManager.isActive()
            || (false == stateManager.isRecvFirstFrame())
            ||  mVideoCallCard.IsCaptureMenu()
            ||  mVideoCallCard.isDialpad() )  
        {
		    if(DBG)
                log("onPrepareOptionsMenu() is returned " );
            // do not change return value : 20101030 by taesung.lee
            return false; 
        }
              
		if (menu.size() == 0) {
			MenuItem menu1 = menu.add(0, 10, 0, getResources().getString(
					R.string.vt_option_use_2nd_camera));
                    menu1.setIcon(R.drawable.vt_ic_menu_swap_camera);

			MenuItem menu2 = menu.add(1, 20, 1, getResources().getString(
					R.string.vt_option_capture));
                     menu2.setIcon(R.drawable.vt_ic_menu_capture);

                      if(isBluetoothAudioConnected())
                     {
			    MenuItem menu3 = menu.add(2, 30, 2, getResources().getString(
					R.string.incallmenu_uncheck_bluetooth));                        
                            menu3.setIcon(R.drawable.vt_ic_menu_bluetooth_off);
                     }
                      else
                     {
			    MenuItem menu3 = menu.add(2, 30, 2, getResources().getString(
					R.string.incallmenu_check_bluetooth));                         
                            menu3.setIcon(R.drawable.vt_ic_menu_bluetooth);
                     }
              
			MenuItem menu4 = menu.add(3, 40, 3, getResources().getString(
					R.string.vt_option_camera_setting));
                     menu4.setIcon(R.drawable.vt_ic_menu_camera_setting);
		}

		// option menu(blooth) state - enable or disable.
		if (false == isBluetoothAvailable()) {
			menu.setGroupEnabled(2, false);
		} else {
			menu.setGroupEnabled(2, true);
		}

		// option menu(2nd camera) string toggle
		MenuItem item = menu.findItem(10);
		if (null != item) {
			CharSequence title = item.getTitle();
			String szSecondCamera = getResources().getString(
					R.string.vt_option_use_2nd_camera);
			String szMainCamera = getResources().getString(
					R.string.vt_option_use_main_camera);

			if(VideoTelephonyApp.getInstance(this).getSensorID() == VTPreferences.SENSOR_ID_FRONT_PORTRAIT)
			{
				item.setTitle(szSecondCamera);
			}else
				item.setTitle(szMainCamera);
		}

               // Bluetooth menu icon toggle
              MenuItem itemBT = menu.findItem(30);
		if (null != itemBT) {          
                     if(isBluetoothAudioConnected())
                     {
                            itemBT.setTitle(R.string.incallmenu_uncheck_bluetooth);
                            itemBT.setIcon(R.drawable.vt_ic_menu_bluetooth_off);
                      }
                      else
                      {
                           itemBT.setTitle(R.string.incallmenu_check_bluetooth);
                            itemBT.setIcon(R.drawable.vt_ic_menu_bluetooth);
                       }
		}
		
		// in private state, then do not use camera-setting
		if(mVideoCallCard != null)
		{
			boolean isAvatarStart = mVideoCallCard.isShowAvatarStart();
			if(isAvatarStart)
			{
				menu.setGroupEnabled(0, false);
                            menu.setGroupEnabled(3, false);
			}
			else 
			{
				menu.setGroupEnabled(0, true);
				menu.setGroupEnabled(3, true);
			}			
		}
		
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 10:
			VideoTelephonyApp.getInstance(this).switchCamera();
			setInitPreference();
			break;
		case 20:
			mVideoCallCard.onCreateFarCaptureButton();
			mVideoCallCard.onStartCaptureloading();
			break;
		case 30:
			onBluetoothClick();
			break;
		case 40:
			showOnScreenSettings();
			break;
		}
		return true;
	}
	
	private void showOnScreenSettings() {
		if (DBG)
			log("showOnScreenSettings() called!");

		// if (mSettings == null)
		// {
		mSettings = new OnScreenSettings(findViewById(R.id.vt_remote));

		CameraSettings helper = new CameraSettings(this, mParameters);
		mScreen = helper.getPreferenceScreen(R.xml.camera_preferences);

		mSettings.setPreferenceScreen(mScreen);
		mSettings.setOnVisibilityChangedListener(this);

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		// }
		mSettings.setVisible(true);
		// show 2depth
		mSettings.initSubView();
	}

	/**
	 * Initialize a VT camera-setting XML data in the every new call.
	 * @see onSubResume(), retryVideoCall()
	 * */
	public void setInitPreference() {
		if (DBG)
			log("setInitPreference()");

		if (mPreferences == null) {
			mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		}
         
       
		SharedPreferences.Editor editor = mPreferences.edit();

        String[] keys = new String[] { getString(R.string.camera_bright_key), 
	              getString(R.string.camera_whitebalance_key),
	              getString(R.string.camera_colour_key),	              
	              getString(R.string.camera_zoom_key),
	              getString(R.string.camera_nightmode_key)};


	      String[] value = new String[] { getString(R.string.pref_camera_brightness_default),
	              getString(R.string.pref_camera_whitebalance_default), 
	              getString(R.string.pref_camera_coloureffect_default),
	              getString(R.string.pref_camera_zoom_default), 
	              getString(R.string.pref_camera_night_default)};

	      
		for (int i = 0; i < keys.length; i++) {
			editor.putString(keys[i], value[i]);
			editor.commit();
		}
	}

	/**
	 * When Camera setting menu was shown or hided , it called.
	 * */
	private static boolean isSwapView = false;
	public void onVisibilityChanged(boolean visible) {
		if (DBG)
			log("onVisibilityChanged(" + visible + ") called!");		
		 
		if (visible) 
		{
			mPreferences.registerOnSharedPreferenceChangeListener(this);
    		if(mVideoCallCard != null)
    		{
    			if(mVideoCallCard.isTopNearView() == false)
    			{
    				//after swapView(), the return value of isTopNearView() is change.
    				mVideoCallCard.swapView();
    				isSwapView = true;
    			}
    		}
			
		} else 
		{
			mPreferences.unregisterOnSharedPreferenceChangeListener(this);
			mSettings.finalize();
    		
			if(mVideoCallCard != null)
    		{
    			if(isSwapView == true)
    			{
    				mVideoCallCard.swapView();
    				isSwapView = false;
    			}
    		}
		}
	}

	/**
	 * When preference is changed, it called
	 * 
	 * */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Call setCameraParameters to take effect now.
		// setCameraParameters();

		if (DBG)
			log("onSharedPreferenceChanged(), key = " + key);

		String value = "";
		String defaultValue = "";
		
		if (key.equals(getString(R.string.camera_bright_key))) {
			// defaultValue = mParameters.getWhiteBalance();
			defaultValue = Parameters.WHITE_BALANCE_AUTO;
		} else if (key.equals(getString(R.string.camera_whitebalance_key))) {
			defaultValue = Parameters.BRIGHTNESS_SELECT_HIGH;
		} else if (key.equals(getString(R.string.camera_zoom_key))) {
			defaultValue = Parameters.ZOOM_MODE_1X;
		} else if (key.equals(getString(R.string.camera_nightmode_key))) {
			defaultValue = Parameters.NIGHT_MODE_ON;
		} else if (key.equals(getString(R.string.camera_colour_key))) {			
			defaultValue = Parameters.COLOUR_EFFECT_OFF;
		}

		value = sharedPreferences.getString(key, defaultValue);

		String key_whiteBalance = getString(R.string.camera_whitebalance_key);
		String key_colourEffect =  getString(R.string.camera_colour_key);
		
		if (key.equals(key_whiteBalance)
				&& (value.equals(Parameters.WHITE_BALANCE_AUTO) == false)) {
			
			if (sharedPreferences.getString(key_colourEffect, Parameters.COLOUR_EFFECT_OFF).equals(Parameters.COLOUR_EFFECT_OFF) == false)
			{
				Toast.makeText(this, getResources().getString(R.string.vt_set_ce_none)
						, Toast.LENGTH_SHORT).show();
				VideoTelephonyApp.getInstance(this).setCameraParameters(key_colourEffect, Parameters.COLOUR_EFFECT_OFF);
				mSettings.setPreferenceValue(key_colourEffect, Parameters.COLOUR_EFFECT_OFF);				
			}
		}
		
		if (key.equals(key_colourEffect)
				&& (value.equals(Parameters.COLOUR_EFFECT_OFF) == false)) {			
			if (sharedPreferences.getString(key_whiteBalance, Parameters.WHITE_BALANCE_AUTO).equals(Parameters.WHITE_BALANCE_AUTO) == false)
			{				
				Toast.makeText(this, getResources().getString(R.string.vt_set_wb_auto)
						, Toast.LENGTH_SHORT).show();
				VideoTelephonyApp.getInstance(this).setCameraParameters(key_whiteBalance, Parameters.WHITE_BALANCE_AUTO);
				mSettings.setPreferenceValue(key_whiteBalance, Parameters.WHITE_BALANCE_AUTO);
			}
		}
		
		if (DBG)
			log("onSharedPreferenceChanged(), value = " + key
					+ "defaultValue = " + defaultValue);

		VideoTelephonyApp.getInstance(this).setCameraParameters(key, value);
	}

	// LGE_MERGE_S
	// LGE_S kim.jungtae 20100421
	void gotoExcuseMessages() {
		// goto ExcuseMessagesList
		String rName = "";
		String rNumber = "";
		rNumber = mRingingCall.getEarliestConnection().getAddress();

		// Log.d(LOG_TAG, "InVideoCallScreen.java:rName:"+rName);
		if(DBG)
		Log.d(LOG_TAG, "InVideoCallScreen.java:rNumber:" + rNumber);

		Intent intentSms = new Intent(this, InCallMessagesList.class);
		intentSms.putExtra("rName", rName);
		intentSms.putExtra("rNumber", rNumber);
		startActivity(intentSms);
	}

	// 20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
	// LGE_E kim.jungtae 20100421
	// LGE_MERGE_E
	String getUserNumber() {
		if (mRingingCall == null
				|| mRingingCall.getEarliestConnection() == null
				|| mRingingCall.getEarliestConnection().getAddress() == null
				|| mRingingCall.getEarliestConnection().getAddress().trim()
				.equals("")
				|| mRingingCall.getEarliestConnection().getAddress().trim()
				.length() == 0)
			return null;
		else
			return mRingingCall.getEarliestConnection().getAddress();
	}

	// LGE_MERGE_E

	// 20100507 yongwoon.choi@lge.com send SMS in Silent incoming [START_LGE]
	private static final int SUBACTIVITY_EXCUSE_MESSAGE = 1;
	private static boolean DontKeyguardLock = false;

	private void internalSendSMS() {
		CallerInfo info = PhoneUtils.getCallerInfo(this, mPhone
				.getRingingCall().getEarliestConnection());
		// Intent intent = new
		// Intent("com.lge.execusemsg.action.GET_EXCUSE_MSG");
		if(DBG)
		Log.d(LOG_TAG, "internalSendSMS info.phoneNumber : "	+ info.phoneNumber);
		if (info.phoneNumber != null) {
          		Intent intent = new Intent(Intent.ACTION_MAIN);
			DontKeyguardLock = true;
			mInCallTouchUi.handleSilentCall();
			intent.setClassName(this, ExcuseMessages.class.getName());
			intent.putExtra("excusemsg.phoneNumber", info.phoneNumber);
			intent.putExtra("excusemsg.fromInCall", true);
			startActivityForResult(intent, SUBACTIVITY_EXCUSE_MESSAGE);
		} else {
			Toast toast = Toast.makeText(this, R.string.private_num,
					Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SUBACTIVITY_EXCUSE_MESSAGE:
			if (resultCode == RESULT_OK) {
				internalHangupRingingCall();
			}
			break;
		}
	}

	// 20100507 yongwoon.choi@lge.com send SMS in Silent incoming [END_LGE]

	public boolean isEndCallState() {
		return (mInCallScreenMode == InCallScreenMode.CALL_ENDED);
	}

	private void dismissVTUI() {

		if ((mForegroundCall.getState() == Call.State.DISCONNECTED)
				|| (mBackgroundCall.getState() == Call.State.DISCONNECTED)) {
			if (DBG)
				log("dismissVTUI");

			dismissMenu(true);

			if (mSettings != null) {
				if (mSettings.isVisible())
					mSettings.setVisible(false);

//				// xml preference initialize
//				setInitPreference(); -->move to onResume()

				// init
				mSettings = null;

				mParameters = null;
				mScreen = null;

			}

			if (mVideoCallCard != null) {
				if (mVideoCallCard.isDialpad()) {
					mVideoCallCard.setDialpad(false);
				}
				// VideoCallCard
				// initalize a bottom-buttons state at outgoing and active UI
//				mVideoCallCard.initFlags(); -->onSubResume()
				mVideoCallCard.buttonInitialize();

                            if(mVideoCallCard.IsCaptureMenu())
                                mVideoCallCard.AfterCaptureButtonInitialize();
			}
		}

	}

	private void InitSndPath(){
		if (DBG)
			log("InitSndPath()...");
        
		// Set Speaker phone according to the current soundpath
		int isSpeakerPhone = Settings.System.getInt(getContentResolver(),
				Settings.System.VT_SET_SPEAKER, 0);

		if (isSpeakerPhone == 1)
		{
			VideoTelephonyApp.getInstance(this).InitAudioPath(VideoTelephonyApp.VTSNDPATH_SPEAKER);
			// Update Button UI & State
			if (mVideoCallCard != null)
				mVideoCallCard.updateButtonUI(VideoCallCard.BUTTON_SPEAKER,	true, "");            
		}
		else
		{
			VideoTelephonyApp.getInstance(this).InitAudioPath(VideoTelephonyApp.VTSNDPATH_RECEIVER);
				// Update Button UI & State
				if (mVideoCallCard != null)
					mVideoCallCard.updateButtonUI(VideoCallCard.BUTTON_SPEAKER,	false, "");
		}

		if((PhoneApp.getInstance().isHeadsetPlugged() == false) && (isBluetoothAvailable() == false))
		{
			if (isSpeakerPhone == 1) {
				VideoTelephonyApp.getInstance(this).setAudioPath(VideoTelephonyApp.VTSNDPATH_SPEAKER);

				PhoneUtils.turnOnSpeaker(InVideoCallScreen.this, true, false);

				// Update Button UI & State
				if (mVideoCallCard != null)
					mVideoCallCard.updateButtonUI(VideoCallCard.BUTTON_SPEAKER,	true, "");
			}else{
				VideoTelephonyApp.getInstance(this).setAudioPath(VideoTelephonyApp.VTSNDPATH_RECEIVER);
			}
		}else if(isBluetoothAvailable() == true)
		{
			VideoTelephonyApp.getInstance(this).setAudioPath(VideoTelephonyApp.VTSNDPATH_BLUETOOTH);			
			// Update Button UI & State
			if (mVideoCallCard != null)
				mVideoCallCard.updateButtonUI(VideoCallCard.BUTTON_SPEAKER,	false, "");
            
		}else if(PhoneApp.getInstance().isHeadsetPlugged() == true)
		{
			VideoTelephonyApp.getInstance(this).setAudioPath(VideoTelephonyApp.VTSNDPATH_EARMIC);			
			// Update Button UI & State
			if (mVideoCallCard != null)
		       {
                           mVideoCallCard.disableButton(VideoCallCard.BUTTON_SPEAKER);
			 }            
		}
	}

	private void showFallbackDialog(int resid) {
		CharSequence msg = getResources().getText(resid);
		if (DBG)
			log("showFallbackDialog('" + msg + "')...");

		// create the clicklistener and cancel listener as needed.
		DialogInterface.OnClickListener clickListener;
		DialogInterface.OnClickListener cancelListener;

		clickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
				retryVoiceCall();
			}
		};

		cancelListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
				delayedCleanupAfterDisconnect(null);
			}
		};

		int isAutoFallback = Settings.System.getInt(getContentResolver(),
				Settings.System.VT_AUTO_FALLBACK, 0);

		if (isAutoFallback == 1) {
			mFallbackDialog = new AlertDialog.Builder(this).setMessage(msg)
			.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mHandler
					.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
					mHandler.removeMessages(AUTOMATIC_FALLBACK);
					delayedCleanupAfterDisconnect(null);
				}
			}).create();

			// When the dialog is up, completely hide the in-call UI
			// underneath (which is in a partially-constructed state).
			mFallbackDialog.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_DIM_BEHIND);

			mFallbackDialog.show();

			mHandler.removeMessages(AUTOMATIC_FALLBACK);
			Message msgFallback = mHandler.obtainMessage(AUTOMATIC_FALLBACK);
			mHandler.sendMessageDelayed(msgFallback, FALLBACK_DELAY);
		} else {
		    if (DBG)
			Log.w(LOG_TAG, "showFallbackDialog() isAutoFallback !=1 .....start!!!!");
			
			mFallbackDialog = new AlertDialog.Builder(this).setMessage(msg)
			.setPositiveButton(R.string.ok, clickListener)
			.setNegativeButton(R.string.cancel, cancelListener)
			.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
				    if (DBG)
					Log.w(LOG_TAG, "showFallbackDialog() onCancel() ");
					mHandler
					.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
					if (DBG)
					Log.w(LOG_TAG, "delayedCleanupAfterDisconnect() start!");
					delayedCleanupAfterDisconnect(null);
					if (DBG)
					Log.w(LOG_TAG, "delayedCleanupAfterDisconnect() end!");
				}
			}).create();

			// When the dialog is up, completely hide the in-call UI
			// underneath (which is in a partially-constructed state).
			mFallbackDialog.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}
		mFallbackDialog.show();
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
              VTAppStateManager stateManager = VTAppStateManager.getInstance();
              
              if(mVideoCallCard.IsCaptureMenu())
                     return; 

              if (stateManager.isCanCommand() && stateManager.isActive() && stateManager.isRecvFirstFrame())
                     return;
                     
		if(v == mVideoCallCard.mFarview)  //Temp
              {      
                     if(mVideoCallCard.isShowAvatarStart() || VideoTelephonyApp.getInstance().isHold())
                          return;
			menu.add(0, NEAR_CAPTURE, 0, R.string.vt_near_image_capture);
              }
		else
			menu.add(0, FAR_CAPTURE, 0, R.string.vt_far_image_capture);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case NEAR_CAPTURE:
			mVideoCallCard.onStartCapture(false);
			return true;
		case FAR_CAPTURE:
			mVideoCallCard.onCreateFarCaptureButton();
			mVideoCallCard.onStartCaptureloading();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	public void onSendCaptureMessage()
	{
		if (DBG) log("onSendCaptureMessage");
		Message msg = mHandler.obtainMessage(EVTENT_START_CAPTURE);
		mHandler.sendMessageDelayed(msg, 1000);

	}
}
