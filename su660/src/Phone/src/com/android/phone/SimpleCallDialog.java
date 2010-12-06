// LGE_CALL_TRANSFER START
// LGE_CALL_DEFLECTION START
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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts.PhonesColumns;
import android.telephony.PhoneNumberUtils;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneApp;

public class SimpleCallDialog extends Activity {
    
    private static final String TAG = "SimpleCallDialog";
    private EditText mTextField;
    private final int SIMPLE_DIALOG = 1;
    private Intent mContactListIntent;
    private ImageButton mContactPickButton;
    private PhoneApp mApp;
    private int mContactRequestCode = 1;
    private AlertDialog.Builder mSimpleCallDialog;
    public static boolean mIsColdTransfer = false;
    private enum DialogType {
        UNKNOWN(0),
        CALL_DEFLECTION(1),
        CALL_TRANSFER(2);
        private int mId;
        DialogType(int id) {
            mId = id;
        }
        public int id() { return mId; }
    };
    private DialogType mDialogType;
    /** Called when the activity is first created. */ 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        mDialogType = DialogType.UNKNOWN;
        Intent intent = getIntent();
        int code = (intent.getIntExtra(mDialogType.CALL_DEFLECTION.name(), 
                    mDialogType.UNKNOWN.id()));
        if (code != mDialogType.UNKNOWN.id()) {
            if (code == mDialogType.CALL_DEFLECTION.id()) {
                mDialogType = DialogType.CALL_DEFLECTION;
            }
        }
        code = (intent.getIntExtra(mDialogType.CALL_TRANSFER.name(), 
                 mDialogType.UNKNOWN.id()));
        if (code != mDialogType.UNKNOWN.id()) {
            if (code == mDialogType.CALL_TRANSFER.id()) {
                 mDialogType = DialogType.CALL_TRANSFER;
            }
        }
        mContactListIntent = new Intent(Intent.ACTION_GET_CONTENT);
        mContactListIntent.setType(android.provider.Contacts.Phones.CONTENT_ITEM_TYPE);
        mApp = PhoneApp.getInstance();
        mTextField = new EditText(this);
        showDialog(SIMPLE_DIALOG);
    }
   
    protected Dialog onCreateDialog(int id) {  
        Log.d(TAG,"onCreateDialog: id = "+ id);
        mSimpleCallDialog = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.simple_call_dialog, null);
        mSimpleCallDialog.setIcon(R.drawable.picture_unknown);
        mSimpleCallDialog.setMessage(R.string.call_deflect_dialog_message);
        mSimpleCallDialog.setView(textEntryView);
        mContactPickButton = (ImageButton) textEntryView.findViewById(R.id.select_contact);
        ViewGroup container = (ViewGroup) textEntryView.findViewById(R.id.edit_container);
        // add the edittext to the container.
        if (container != null) {
            container.addView(mTextField, ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        mTextField.setRawInputType(InputType.TYPE_CLASS_PHONE);
        final Button okButton = (Button) textEntryView.findViewById(R.id.ok_button);
        int res_title = R.string.call_deflect_dialog;
        int res_buttonText = R.string.deflect;
        switch (mDialogType) {
            case CALL_DEFLECTION:
                res_title = R.string.call_deflect_dialog;
                res_buttonText = R.string.deflect;
                break;
            case CALL_TRANSFER:
                res_title = R.string.call_transfer_dialog;
                res_buttonText = R.string.transfer;
                break;
                
        }
        mSimpleCallDialog.setTitle(res_title);
        okButton.setText(res_buttonText);
        okButton.setOnClickListener(new OnClickListener () {
            public void onClick(View v) {
                if (mTextField.getText().toString().equals("")) {
                    Toast.makeText(SimpleCallDialog.this,R.string.empty_call_number,Toast.LENGTH_LONG).show();
                }
                else {
                    try {
                        switch (mDialogType) {
                            case CALL_DEFLECTION:
                                mApp.phone.setCallDeflection(mTextField.getText().toString());
                                break;
                            case CALL_TRANSFER:
                                mIsColdTransfer = true;
                                mApp.phone.dial(mTextField.getText().toString());
                                mApp.phone.explicitCallTransfer(true); 
                                break;
                        }
                        finish();
                    }
                    catch(CallStateException exc) {
                        Log.e(TAG,"CallStateException: okButton");
                        int msg = R.string.incall_error_supp_service_deflection;
                        switch (mDialogType) {
                            case CALL_DEFLECTION:
                                msg = R.string.incall_error_supp_service_deflection;
                                break;
                            case CALL_TRANSFER:
                                mIsColdTransfer = false;
                                msg = R.string.incall_error_supp_service_transfer;
                                break;
                        }
                        Toast.makeText(SimpleCallDialog.this, msg, Toast.LENGTH_LONG).show();
                    }
                }
            }
            
        });
        final Button cancelButton = (Button) textEntryView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener () {
            public void onClick(View v) {
                finish();
            }
        });
        mSimpleCallDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (mSimpleCallDialog != null) {
                    removeDialog(SIMPLE_DIALOG);
                    mSimpleCallDialog = null;
                }
                finish();
            }
        });
 //set contact picker 
        mContactPickButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivityForResult(mContactListIntent, mContactRequestCode);
                }
                catch(RuntimeException exc) {
                    Log.e(TAG,"mContactPickButton: RuntimeException");
                    return;
                }
            }
        });      
        AlertDialog alert = mSimpleCallDialog.create();
        return alert;
    
    }
     
    // asynchronous result call after contacts are selected.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       Log.d(TAG,"onActivityResult");
       if (data == null) { 
           Log.e(TAG,"onActivityResult: bad data");
           return;
       }
       final String NUM_PROJECTION[] = {PhonesColumns.NUMBER};
       Cursor cursor = getContentResolver().query(data.getData(), NUM_PROJECTION, null, null, null);
       try{
    	   if ((cursor == null) || (!cursor.moveToFirst())) {
    		   Log.e(TAG,"onActivityResult: bad contact data, no results found");
    		   return;
    	   }
    	   if  (requestCode == mContactRequestCode) {
    		   onPickActivityResult(cursor.getString(0));
    	   }
    	   cursor.close();
       }finally{
    	   cursor.close();
       }
    }

    //Notify the preference that the pick activity is complete.
    public void onPickActivityResult(String pickedValue) {
        Log.d(TAG,"onPickActivityResult");
        String newDialString = PhoneNumberUtils.stripSeparators(pickedValue);
        if (!newDialString.equals("")) {
            mTextField.setText(newDialString);  
        }
        else {
            Toast.makeText(SimpleCallDialog.this,R.string.wrong_cont_numb,Toast.LENGTH_LONG).show();
        }
    }
}
// LGE_CALL_DEFLECTION END
// LGE_CALL_TRANSFER END