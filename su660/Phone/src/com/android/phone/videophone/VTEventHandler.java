
package com.android.phone.videophone;

//import com.android.internal.telephony.Phone;
//import com.android.phone.InVideoCallScreen;
//import com.android.phone.PhoneApp;
//import com.android.phone.PhoneUtils;

import com.android.phone.InVideoCallScreen;
import com.android.phone.R;
import com.android.phone.VideoCallCard;
import com.android.phone.videophone.VTCallStateInterface.VTCallState;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.FrameLayout;

/**
 * In-Call 화면에서의 사용자 UI 이벤트를 처리한다. 
 * @author hogyun.kim
 *
 */
public interface VTEventHandler extends OnClickListener {

	public static final String LOG_TAG = "VTEentHandler";
    public static final boolean DBG = 
        (SystemProperties.getInt("ro.debuggable", 0) == 1);

	//@Override  
	public void onClick(View v);


	/**
	 * Listener Class of the front Buttons in outgoing/InCall screen. 
	 * When you button pressed, handles the button event.  
	 * */
	public class IncallBL implements VTEventHandler{
		protected Context mContext = null;
		protected VideoCallCard mVideoCallCard = null;
		protected VideoTelephonyApp mVideoApp = null;


		/**if the buttons is pressed */
		private boolean mButtonIsSelect[] = new boolean[VideoCallCard.INCALL_BUTTON_COUNT];

		protected IncallBL(){}

		public IncallBL(Context c,FrameLayout layout)
		{
			mContext = c;
			mVideoCallCard = (VideoCallCard)layout;
			for(int i=0;i<6;i++)
			{
				mButtonIsSelect[i] = false;
			}

			/**
			 * When a using VTWrapper (VTEngine) and VTAppStateManager,
			 * manage a flag which needs some feature. by hgkim 
			 */
			//			mVideoApp = mVideoCallCard.getVideoTelephonyApp();
			mVideoApp = VideoTelephonyApp.getInstance(c);

		}
		public void onClick(View v)
		{
			if (mVideoCallCard.isDialpad())
			{
			    if(DBG)
				Log.i(LOG_TAG,"IncallBL => Dialpad state");
				return;
			}

			if(DBG)
			Log.i(LOG_TAG,"IncallBL => onClick()!");
			CharSequence labelText = ((Button)v).getText();

			int nWhatButton = changePressState(v,labelText);

			if(nWhatButton == VideoCallCard.BUTTON_PRIVATE_SHOW)
			{
				VTAppStateManager manager = VTAppStateManager.getInstance();
				boolean isResult = false;

				VTCallState callState = 
					VTAppStateManager.getInstance().getCallState();
				if(DBG)
				Log.w(LOG_TAG, "------ callState = " + callState);
				
				
				boolean isSharing = manager.isSharing();
				//int nSettedSubstitute = ((InVideoCallScreen)mContext).isSubstituImage ;
				boolean isAvatar = mVideoCallCard.isShowAvatarStart();

				if ( isSharing || isAvatar)
				{
					isResult = mVideoApp.stopImageSharinig();
				}
				else
				{	
					mVideoCallCard.sendSubsImagePath = Settings.System.getString(mContext.getContentResolver(), Settings.System.VT_PRIVATE_NAME);
					isResult = mVideoApp.startImageSharinig(mVideoCallCard.sendSubsImagePath);
				}


				//Change button text
				if(isResult)
				{
					mVideoCallCard.changePrivateButtonText();
					changeBackgroundResource(v, VideoCallCard.BUTTON_PRIVATE_SHOW);
				}

			}
			else if(nWhatButton == VideoCallCard.BUTTON_END)
			{
				mVideoApp.userEndCall();
			}
			else if(nWhatButton == VideoCallCard.BUTTON_DIALPAD)
			{				
				callDialpad();
			}
			else if(nWhatButton == VideoCallCard.BUTTON_HOLD)
			{
				boolean isResult = false;

				isResult = mVideoApp.toggleHold();

				if(isResult)
				{
					if(mVideoApp.isHold() && !VTAppStateManager.getInstance().isSharing())
					{
						// disable private and mute
						mVideoCallCard.disableButton(VideoCallCard.BUTTON_PRIVATE_SHOW);
						mVideoCallCard.disableButton(VideoCallCard.BUTTON_MUTE);
					}else
					{
						// enadle private and mute
						mVideoCallCard.enableButton(VideoCallCard.BUTTON_PRIVATE_SHOW);
						mVideoCallCard.enableButton(VideoCallCard.BUTTON_MUTE);					
						mVideoCallCard.updateButtonUI(VideoCallCard.BUTTON_MUTE,false, "");					
					}
					mVideoCallCard.changeHoldButtonText();
					changeBackgroundResource(v, VideoCallCard.BUTTON_HOLD);
				}
			}
			else if(nWhatButton == VideoCallCard.BUTTON_MUTE)
			{
				mVideoApp.toggleMute();				
			} 
			else if(nWhatButton == VideoCallCard.BUTTON_SPEAKER)
			{
				// For SpkPhone menu, member of InVideoCallScreen is re-used.  
				//mVideoApp.toggleSpeaker();
				((InVideoCallScreen)mContext).onSpeakerClick();
			}
		}

		/**
		 * chage button state -
		 * */
		private int changePressState(View view, CharSequence text)
		{
			int nIndex = 0;

			//Change the "PRIVATE" button state 
			if(text.equals(mContext.getResources().getString(R.string.vt_Private)) ||
					text.equals(mContext.getResources().getString(R.string.vt_Show)))
			{
				nIndex = VideoCallCard.BUTTON_PRIVATE_SHOW;				 
			}
			else if(text.equals(mContext.getResources().getString(R.string.vt_end)) )
			{
				nIndex = VideoCallCard.BUTTON_END;								
			}
			else if(text.equals(mContext.getResources().getString(R.string.vt_Dialpad)) )
			{
				nIndex = VideoCallCard.BUTTON_DIALPAD;					
			}
			else if(text.equals(mContext.getResources().getString(R.string.vt_Hold)) ||
                                    text.equals(mContext.getResources().getString(R.string.vt_unHold)))
			{
				nIndex = VideoCallCard.BUTTON_HOLD;					
			}
			else if(text.equals(mContext.getResources().getString(R.string.vt_Mute)) )
			{
				nIndex = VideoCallCard.BUTTON_MUTE;				
			}
			else if(text.equals(mContext.getResources().getString(R.string.vt_Speaker)) )
			{
				nIndex = VideoCallCard.BUTTON_SPEAKER;					
			}
			//switch style function is available in "PRIVATE,MUTE,SPEAKER" 
			if(/*nIndex == VideoCallCard.BUTTON_PRIVATE_SHOW ||
                                   nIndex == VideoCallCard.BUTTON_HOLD  ||*/
					nIndex == VideoCallCard.BUTTON_MUTE ||
					nIndex == VideoCallCard.BUTTON_SPEAKER
			)
			{
				changeBackgroundResource(view, nIndex);
			}

			return nIndex;
		}

		private void changeBackgroundResource(View view, int nIndex)
		{
			//Change it
			mButtonIsSelect[nIndex] = !mButtonIsSelect[nIndex];

			if(mButtonIsSelect[nIndex] == true)
			{
				view.setBackgroundResource(R.drawable.vt_invideocall_on_button);
			}
			else
			{
				view.setBackgroundResource(R.drawable.vt_invideocall_button);
			}	
		}

		/** 
		 * dialpad function 
		 * */
		private void callDialpad()
		{
			// LGE_VT_DIALPAD
			mVideoCallCard.setDialpad(true);
			if(DBG)
			Log.i(LOG_TAG,"callDialpad()");
		}

		/**
		 * Initialize background of buttons-private/mute/speaek  
		 * */
		public void initButtonState()
		{
			for(int i=0;i<6;i++)
			{
				mButtonIsSelect[i] = false;
			}
		}

		public void setButtonState(int nIndex, boolean bState)
		{
			mButtonIsSelect[nIndex] = bState;
		}

		public boolean getButtonState(int nIndex)
		{
			return mButtonIsSelect[nIndex];
		}
        
	}//END OF "IncallBL" class

	// LGE_VT_DIALPAD start
	public class DialpadBL implements VTEventHandler{
		protected Context mContext = null;
		protected VideoCallCard mVideoCallCard = null;
		protected VideoTelephonyApp mVideoApp = null;

		protected DialpadBL(){}

		public DialpadBL(Context c,FrameLayout layout)
		{
			mContext = c;
			mVideoCallCard = (VideoCallCard)layout;

			/**
			 * When a using VTWrapper (VTEngine) and VTAppStateManager,
			 * manage a flag which needs some feature. by hgkim 
			 */
			mVideoApp = VideoTelephonyApp.getInstance(c);

		}

		public void onClick(View v)
		{
		    if(DBG)
		        Log.i(LOG_TAG,"DialpadBL => onClick()!");
			int nIndex = -1;

			int nID = ((ImageButton)v).getId();

			if(nID == R.id.vt_one)
			{
				nIndex = VideoCallCard.BUTTON_VT_1;					
			}		
			else if(nID == R.id.vt_two)
			{
				nIndex = VideoCallCard.BUTTON_VT_2;					
			}						
			else if(nID == R.id.vt_three)
			{
				nIndex = VideoCallCard.BUTTON_VT_3;					
			}						
			else if(nID == R.id.vt_four )
			{
				nIndex = VideoCallCard.BUTTON_VT_4;					
			}						
			else if(nID == R.id.vt_five)
			{
				nIndex = VideoCallCard.BUTTON_VT_5;					
			}						
			else if(nID == R.id.vt_six)
			{
				nIndex = VideoCallCard.BUTTON_VT_6;					
			}						
			else if(nID == R.id.vt_seven)
			{
				nIndex = VideoCallCard.BUTTON_VT_7;					
			}						
			else if(nID == R.id.vt_eight)
			{
				nIndex = VideoCallCard.BUTTON_VT_8;					
			}						
			else if(nID == R.id.vt_nine)
			{
				nIndex = VideoCallCard.BUTTON_VT_9;					
			}						
			else if(nID == R.id.vt_star)
			{
				nIndex = VideoCallCard.BUTTON_VT_STAR;					
			}						
			else if(nID == R.id.vt_zero)
			{
				nIndex = VideoCallCard.BUTTON_VT_0;					
			}						
			else if(nID == R.id.vt_pound)
			{
				nIndex = VideoCallCard.BUTTON_VT_POUND;					
			}	

			if (nIndex >=  VideoCallCard.BUTTON_VT_0 && nIndex <= VideoCallCard.BUTTON_VT_POUND)
			{
				String strDTMF;
				switch (nIndex)
				{
				case 0: strDTMF = "0"; break;
				case 1: strDTMF = "1"; break;
				case 2: strDTMF = "2"; break;
				case 3: strDTMF = "3"; break;
				case 4: strDTMF = "4"; break;
				case 5: strDTMF = "5"; break;
				case 6: strDTMF = "6"; break;
				case 7: strDTMF = "7"; break;
				case 8: strDTMF = "8"; break;
				case 9: strDTMF = "9"; break;
				case 10: strDTMF = "*"; break;
				case 11: strDTMF = "#"; break;
				default: strDTMF ="";	break;
				}
				if(DBG)
				Log.i(LOG_TAG,"SEND DTMF : " + nIndex+" "+ strDTMF );

				mVideoApp.sendDTMFString(strDTMF);
			}
		}
	}// LGE_VT_DIALPAD end

        public class CaptureBL implements VTEventHandler{
		protected Context mContext = null;
		protected VideoCallCard mVideoCallCard = null;
		protected VideoTelephonyApp mVideoApp = null;
        
		protected CaptureBL(){}

		public CaptureBL(Context c,FrameLayout layout)
		{
			mContext = c;
			mVideoCallCard = (VideoCallCard)layout;

			/**
			 * When a using VTWrapper (VTEngine) and VTAppStateManager,
			 * manage a flag which needs some feature. by hgkim 
			 */
			mVideoApp = VideoTelephonyApp.getInstance(c);
		}

               public void onClick(View v)
		 {
                   if(DBG)
		       Log.i(LOG_TAG,"CaptureBL => onClick()!");
			int nIndex = -1;

			CharSequence labelText = ((Button)v).getText();

			if(labelText.equals(mContext.getResources().getString(R.string.vt_capture_done)) )
                    {
                            mVideoCallCard.AfterCaptureButtonInitialize();
                    }
			else if(labelText.equals(mContext.getResources().getString(R.string.vt_capture_del)) )
                    {
			       ContentResolver resolver = mContext.getContentResolver();
                            resolver.delete(Images.Media.EXTERNAL_CONTENT_URI, Images.Media._ID + "=" + mVideoApp.getCapturedIndex(), null);

				mVideoCallCard.AfterCaptureButtonInitialize();   
			}
			else if(labelText.equals(mContext.getResources().getString(R.string.vt_capture_cancel)) )
			{
				//mVideoCallCard.onCancelCaptureloading();
				mVideoCallCard.AfterCaptureButtonInitialize();
			}

               }
        }
}//end of "VTEventHandler"
