// LGE_MERGE_S
//20100804 jongwany.lee@lge.com attached this file CALL UI
package com.android.phone;


import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView; 
import android.text.Html;
import android.widget.TextView.OnEditorActionListener;
import android.text.InputFilter;
import com.android.phone.ByteLengthFilter;
import com.android.phone.ByteLengthFilter.OnMaxLengthListener;
import com.android.phone.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.Window;

public class ExcuseMessagesEdit extends Activity {
    
	private static final Boolean DBG = true;
	private static final String TAG = "ExcuseMessages";
	private static final String DATABASE_NAME = "excuseMessages.db";
	private static final String DATABASE_TABLE = "excuseMessagesMaster";
	private static final int MAX_LEN = 80;
	private static final int DIALOG_ID_EXCEED_LEN = 3;
	private static final String CONV_STR = "EUC-KR";
	private boolean mLengthFilterTriggerable = false;
	ByteLengthFilter filter;
	
	private SQLiteDatabase myDatabase;
	private EditText et_message;
	private Button bt_save;
	private Button bt_cancel;
	private TextView mLeftTitle;
	private boolean clickbtn;
	int _id;
	int Len;
	private String message;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.excuse_messages_edit_kor);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.list_title);
        
        ExcuseMessagesList EmessagesList = new ExcuseMessagesList();
    
        Intent	intent = this.getIntent();
        clickbtn = intent.getBooleanExtra("Click_Button", false);
        
        mLeftTitle = (TextView)findViewById(R.id.title_left_text); 
        
        if(clickbtn) {
        	 mLeftTitle.setText(R.string.add_excuse_message);
        } else {
        	 mLeftTitle.setText(R.string.edit_excuse_message);
        }
        bt_save	=	(Button)findViewById(R.id.bt_save);
        bt_save.setEnabled(false);
        
        et_message = (EditText)findViewById(R.id.et_message);

		et_message.setHint(R.string.rejectcall_edit_hint);
	filter = new ByteLengthFilter(this, MAX_LEN);
	filter.setOnMaxLengthListener(
		new OnMaxLengthListener(){
			public void onMaxLength(){
						if(mLengthFilterTriggerable)
				showDialog(DIALOG_ID_EXCEED_LEN);
				}
			}
		);
		et_message.setFilters(new InputFilter[]{filter});
		
        bt_cancel	=	(Button)findViewById(R.id.bt_cancel);
        
        _id 		= intent.getIntExtra("_id", -1);
        message 	= intent.getStringExtra("message");

        
    	// 20100614 withwind.park added dbOpen method start
        dbOpen();
    	//myDatabase	=	openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
        // 20100614 withwind.park added dbOpen method end

        // 20100721 withwind.park space check start
        et_message.addTextChangedListener(new TextWatcher(){

			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				
			}

			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
						mLengthFilterTriggerable = true;
				// TODO Auto-generated method stub
				if (et_message.getText().toString().trim().length() > 0) {
					bt_save.setEnabled(true);
//					if(et_message.getText().toString().length() == Len) {	
//						showDialog(DIALOG_ID_EXCEED_LEN);
//					}		
				} else {
					bt_save.setEnabled(false);
				}
			}
        	
        });
        // 20100721 withwind.park space check end
        
		/*et_message.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub

				int cnt = et_message.getLineCount();
				if (cnt>7){
					et_message.setHeight(et_message.getHeight());
				}
				return false;
			}
        });*/

        //save button for Saving message in EditText.
        
        bt_save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
            	if (et_message.getText().toString().length()>0)	//editText text size check
            	{
	            	if (_id>=0){
                	if (message.equals(et_message.getText().toString()))
                  { 
                    finish();
                    return;
                  }
	            		updateExcuseMessage();
	            	}else{
	            		insertExcuseMessage();
	            	}
            	}
            	else
            	{
            		if (DBG) Log.i(TAG,"warning !! ");
            	}
            }
        });
        
        //cancel button is clicked than finished
        
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                finish();
            }
        });
        
        //ready for update
        if (message != null){	//for keeping the new Message. 
        	et_message.setText(message);
        }
		
		//editText is pressed
		et_message.requestFocusFromTouch();		
    }
    
    public void onResume(){
    	super.onResume();    	
    }
    
    @Override
    protected void onPause() {
    	super.onPause();	
    }
    
    public void onStop(){
		if (myDatabase.isOpen()){
	    	myDatabase.close();	
	    }
    	super.onStop();
    }
    
	//saving message
    public void insertExcuseMessage() {
		ContentValues cVals	=	new ContentValues();
		cVals.put("opt", "");
		cVals.put("message", et_message.getText().toString());
		cVals.put("modified", 1);
		cVals.put("modified_time", System.currentTimeMillis());
		
		if (!myDatabase.isOpen()) dbOpen();
		
		myDatabase.insert(DATABASE_TABLE, null, cVals);

		this.finish();
		Toast.makeText(ExcuseMessagesEdit.this, getString(R.string.excuse_messages_saved), Toast.LENGTH_SHORT).show();
	}
    
	//saving message
    public void updateExcuseMessage() {
		ContentValues cVals	=	new ContentValues();
		cVals.put("opt", "");
		cVals.put("message", et_message.getText().toString());
		cVals.put("modified", 1);
		cVals.put("modified_time", System.currentTimeMillis());
		String whereArgs	=	"_id = "+_id;
		
		if (!myDatabase.isOpen()) dbOpen();
		myDatabase.update(DATABASE_TABLE, cVals, whereArgs, null );

		Toast.makeText(ExcuseMessagesEdit.this, getString(R.string.excuse_messages_saved), Toast.LENGTH_SHORT).show();
		this.finish();
	}
    
    // 20100614 withwind.park add dbOpen method start
    private void dbOpen()
    {
    	myDatabase	=	openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
    }
    // 20100614 withwind.park add dbOpen method end
    
	void log(String msg)
	{
		Log.d(TAG,msg);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		et_message.setFocusable(true);
	}
    
	protected Dialog onCreateDialog(int id) {
	        if (id == DIALOG_ID_EXCEED_LEN) {	        	 	
	        	return new AlertDialog.Builder(ExcuseMessagesEdit.this)		    	
		    	.setMessage(getResources().getString(R.string.excuse_messages_len))				
				.setNegativeButton(R.string.close_dialog, null)
				.create();				
	        }
		return null;
	}
}
// LGE_MERGE_E










