// LGE_BARRING START
package com.android.phone;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;


public class SetCallBarringPass extends Activity {
    private static final int BAR_PASSWORD_LENGTH = 4;
    private static final int BAR_FACILITY = 8;
    private Phone mPhone;
    private static final int MSG_OK = 100;
    private static final int MSG_EXCEPTION = 110;
    private static final int EVENT_BAR_PASS_SET  = 200;
    private Button mButtonOk;
    private Button mButtonCancel;
    private EditText mOldPass;
    private EditText mNewPass;
    private EditText mConfirmPass;
    private int flag;

	protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
       super.onCreate(savedInstanceState);
       requestWindowFeature(Window.FEATURE_LEFT_ICON);

       mPhone= PhoneFactory.getDefaultPhone();
       setContentView(R.layout.barring_change_pass_dialog);
       mButtonOk = (Button)findViewById(R.id.barring_pass_ok);
       mButtonCancel = (Button)findViewById(R.id.barring_pass_cancel);
       mOldPass = (EditText)findViewById(R.id.cb_old_password);
       mNewPass = (EditText)findViewById(R.id.cb_new_password);
       mConfirmPass = (EditText)findViewById(R.id.cb_confirm_password);
       mButtonOk.setOnClickListener(new View.OnClickListener(){
    	   public void onClick(View v){
    	       checkPasswords();
    	      // finish();
    	   }
       });
       mButtonCancel.setOnClickListener(new View.OnClickListener(){
    	   public void onClick(View v){
    		   finish();
    	   }
       });
    }
	private void checkPasswords() {
        hideError();

        String oldPasswd = mOldPass.getText().toString();
        String newPasswd = mNewPass.getText().toString();
        String confirmPasswd = mConfirmPass.getText().toString();

        if (TextUtils.isEmpty(oldPasswd)) {
            showError(R.string.cb_password_empty_error);
        }

        if (TextUtils.isEmpty(newPasswd)
                && TextUtils.isEmpty(confirmPasswd)) {
            showError(R.string.cb_passwords_empty_error);
        }

        if (!verifyPassword(newPasswd)) {
        	showError(R.string.cb_passwords_error);
        } else if (!newPasswd.equals(confirmPasswd)) {
            showError(R.string.cb_passwords_error);
        }

        else {
        	Log.d("setpass", "old: "+ oldPasswd + " new: "+ newPasswd);
            SetPassMessage(oldPasswd,newPasswd);
        }
    }
	private boolean verifyPassword(String passwd) {
        if (passwd == null) {
            showError(R.string.cb_passwords_empty_error);
            return false;
        } else if ((passwd.length() < BAR_PASSWORD_LENGTH)
                || passwd.contains(" ")) {
            showError(R.string.cb_password_verification_error);
            return false;
        } else {
            return true;
        }
    }
    private TextView showError(int messageId) {
        TextView v = (TextView)findViewById(R.id.cb_error);
        v.setText(messageId);
        if (v != null) v.setVisibility(View.VISIBLE);
        return v;
    }

     private void hideError() {
    	 TextView v = (TextView)findViewById(R.id.cb_error);
		 v.setText("");
         if (v != null) v.setVisibility(View.GONE);
     }
     private Handler mSetPass = new Handler(){
     	@Override
     	public void handleMessage(Message msg) {
     		boolean bHandled = false;
             int status = MSG_OK;
             if (msg.what == EVENT_BAR_PASS_SET){
             	 status = SetBarrPass((AsyncResult) msg.obj);
                 bHandled = true;
             }
             if (status != MSG_OK){
             	showError(R.string.bar_net_err);
             } else {
            	 flag ++;
            	 if (flag == BAR_FACILITY){
            		 finish();
            	 }else showError(R.string.bar_net_err);
             }

     	}
     };

     private void SetPassMessage(String oPass, String nPass){
    	Log.d("setpassinto", "old: " + oPass+" new: "+nPass);
    	flag = 0;
     	mPhone.setCallBarringPass("AI", oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"AI"));
     	mPhone.setCallBarringPass("IR",oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"IR"));
     	mPhone.setCallBarringPass("AO",oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"AO"));
     	mPhone.setCallBarringPass("IO",oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"IO"));
     	mPhone.setCallBarringPass("OX",oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"OX"));
     	mPhone.setCallBarringPass("AC",oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"AC"));
     	mPhone.setCallBarringPass("AG",oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"AG"));
     	mPhone.setCallBarringPass("AB",oPass, nPass,
 				Message.obtain(mSetPass,EVENT_BAR_PASS_SET,"AB"));

     };
     private int SetBarrPass(AsyncResult ar){

    	 if (ar.exception != null){
    		 return MSG_EXCEPTION;


    	 }
    	return MSG_OK;
     }
}
// LGE_BARRING END
