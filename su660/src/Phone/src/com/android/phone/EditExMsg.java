package com.android.phone;

//20100804 jongwany.lee@lge.com attached this file CALL UI
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class EditExMsg extends Activity implements OnClickListener {
	
public static final String TAG = "EditExMsg";	
public static EditText mEdit;
public static Button mExMsgSaveButton;
public static Button mExMsgCancelButton;

public static final String EXTRA_NEW_OR_EDIT_EXCUSE_MESSAGE = "New or Edit Excuse Message";
public static final String EXTRA_ITEM_ID = "Extra Item Id";
public static final String EXTRA_EDIT_MSG = "Extra Edit Msg";
public static final String RESULT_MSG = "Result Msg";

public static final int REQUEST_CODE_NEW_EXCUSE_MESSAGE = 0;
public static final int REQUEST_CODE_EDIT_EXCUSE_MESSAGE = 1;
public static final int RESULT_CODE_SAVE = 0;
public static final int RESULT_CODE_CANCEL = 1;

public static long mItemId=0;
public static InputMethodManager mImeManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.excuse_messages_new_excuse_message);
		
		initialize();		
	}

	@Override
	protected void onResume() {
		Log.d(TAG,"onResume");
		mImeManager = (InputMethodManager) this.getSystemService("input_method");		
		mImeManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_NOT_ALWAYS);
		super.onResume();
	}
	
	public void initialize(){
		mEdit = (EditText)findViewById(R.id.EditExMsg);
		mExMsgSaveButton = (Button)findViewById(R.id.EditExMsgSave);
		mExMsgCancelButton = (Button)findViewById(R.id.EditExMsgCancel);
		
		mExMsgSaveButton.setOnClickListener(this);
		mExMsgCancelButton.setOnClickListener(this);		
		
		Intent intent = getIntent();
		int editMode = intent.getIntExtra(EXTRA_NEW_OR_EDIT_EXCUSE_MESSAGE, 0);
		
		switch(editMode){
		case REQUEST_CODE_NEW_EXCUSE_MESSAGE:
			break;
			
		case REQUEST_CODE_EDIT_EXCUSE_MESSAGE:
			mItemId = intent.getLongExtra(EXTRA_ITEM_ID, 0);
			CharSequence editMsg = intent.getCharSequenceExtra(EXTRA_EDIT_MSG);
			mEdit.setText(""+editMsg);
			break;
		}
	}

	public void onClick(View v) {
		if(v==mExMsgSaveButton){
			Intent result = new Intent();
			Editable newMsg = mEdit.getText();			
			result.putExtra(RESULT_MSG,""+newMsg);
			result.putExtra(EXTRA_ITEM_ID, mItemId);
			setResult(RESULT_CODE_SAVE, result);
			
		} else if(v==mExMsgCancelButton){
			Intent result = new Intent();
			setResult(RESULT_CODE_CANCEL, result);
			
		} 		
		mImeManager.hideSoftInputFromWindow(mEdit.getApplicationWindowToken(), 0);
		finish();		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			Intent result = new Intent();
			setResult(RESULT_CODE_CANCEL, result);
			finish();
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		mImeManager.hideSoftInputFromWindow(mEdit.getApplicationWindowToken(), 0);
		super.onPause();
	}

}
