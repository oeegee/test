package com.android.phone;

//20100804 jongwany.lee@lge.com attached this file CALL UI
import com.android.phone.ExMsgProviderMetaData.ExMsgTableMetaData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ExcuseMessages extends Activity implements OnClickListener, OnItemClickListener {
	public static final String TAG = "ExcuseMsg";
	public static LinearLayout mNewMessageButtonLayout;
	public static Button mNewMessageButton;
	public static LinearLayout mButtonLayout;
	public static Button mDeleteButton;
	public static Button mCancelButton;
	
	public static final String EXTRA_NEW_OR_EDIT_EXCUSE_MESSAGE = "New or Edit Excuse Message";
	public static final String EXTRA_ITEM_ID = "Extra Item Id";
	public static final String EXTRA_EDIT_MSG = "Extra Edit Msg";
	public static final String RESULT_MSG = "Result Msg";
	
	public static final int REQUEST_CODE_NEW_EXCUSE_MESSAGE = 0;
	public static final int REQUEST_CODE_EDIT_EXCUSE_MESSAGE = 1;
	
	public static final int RESULT_CODE_SAVE = 0;
	public static final int RESULT_CODE_CANCEL = 1;
	
	public static final int RESULT_CODE_SELECTED_MESSAGE = 0;
	public static final int RESULT_CODE_SELECTED_MESSAGE_CANCEL =1;
	
	public static final int DELETE_DIALOG = 0;
	public static final int DELETE_ALL_DIALOG = 1;
	
	public static final String EXTRA_PARENT = "incomming call";
	public static final String EXTRA_VALUE = "SMS";
	
	public static ContentResolver mResolver;
	public static Cursor mCursor;
	public static String mExtra = null;
	
	ListView lv;
    
	private String mData;			// Excuse Message 
	private boolean mbExecutedByInCall = false;
	private String mPhoneNumber;
	
	private static final int MENU_NEW_MESSAGE = 1;
	private static final int MENU_MULTISELECT = 2;
	private static final int MENU_DELETE_ALL = 3;
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]	
	private static final int MENU_ITEM_SELECT_ALL 	= 4;	
	private static final int MENU_ITEM_DESELECT_ALL = 5;
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
		
    public HashMap<Long, Boolean> multiSelect_selectedItem = new HashMap<Long, Boolean>();
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.excuse_messages_layout);        

        mPhoneNumber = null; 
        mbExecutedByInCall = false;
        //initialize();
        
// Sender  
//  		Intent intent = new Intent("com.lge.execusemsg.action.GET_EXCUSE_MSG");
//  		intent.putExtra("excusemsg.phoneNumber", mPhoneNumber);
//  		intent.putExtra("excusemsg.fromInCall", true);
        
        // Get Intent Data
        Intent intent = getIntent();
        mPhoneNumber = intent.getStringExtra("excusemsg.phoneNumber");
        mbExecutedByInCall = intent.getBooleanExtra("excusemsg.fromInCall", false);
        
        Log.i("ExcuseMsg","(ExcuseMsg) mPhoneNumber: " + mPhoneNumber );
        
//        if ( mbExecutedByInCall != null){
//            if( mExecutedByInCall.equalsIgnoreCase("executedByInCall") ){
//            	mbExecutedByInCall = true;
//            }
//        }
        
        initialize();
        
        mNewMessageButton = (Button) findViewById(R.id.IncomingCall_NewMessage);
        mNewMessageButton.setTag(mPhoneNumber);
        
        mNewMessageButton.setOnClickListener(new View.OnClickListener(){
        	  public void onClick(View v) {
        		  String phoneNumber = (String) v.getTag();
        		  
        		  //SmsManager smsManager = SmsManager.getDefault();
        		  //smsManager.sendTextMessage(phoneNumber, null, "My message Text", null, null);
        		  //smsManager.sendTextMessage("5556", null, "My message Text", null, null);
        		  
      			  Intent sendIntent = new Intent(Intent.ACTION_VIEW);
    			
    			  sendIntent.putExtra("address", phoneNumber);
    			  sendIntent.setType("vnd.android-dir/mms-sms");
    			
    			  startActivity(sendIntent);
        		}
        	  });
        
        if(mbExecutedByInCall == true){
        	mNewMessageButtonLayout.setVisibility(View.VISIBLE);
        }
        else
        {
        	mNewMessageButtonLayout.setVisibility(View.GONE);
        }
        

    }

	@Override
	protected void onResume() {
		super.onResume();
		
	}

	public void initialize(){
		if (mCursor!= null) {
			/* Because "managedQuery" make a ManageCursor that has a ContentObserver, if the ManagedCursor doesn't be release,
			 ** it make a GREF leakage. So, we try to release the ManagedCursor by calling "stopManagingCursor" */
			((Activity)getBaseContext()).stopManagingCursor(mCursor);
			mCursor = null;
		}

		    mCursor = managedQuery(ExMsgTableMetaData.CONTENT_URI, null, null, null, null);
        lv = (ListView)findViewById(R.id.ExMsgList);
        String[] s = {ExMsgTableMetaData.KEY_MSG_CONTENT,
        			ExMsgTableMetaData.KEY_IS_DEFAULT, 
        			};
        int[] i = {R.id.ExcuseMsg};
         
        // 20100428 cutestar@lge.com  To support sms-sending icon func. when reject incoming call.
        // mbExecutedByInCall = false;
        ExMsgAdapter exMsgAdapter = new ExMsgAdapter(this, R.layout.excuse_messages_list_item_view, mCursor, s, i, mPhoneNumber);
        exMsgAdapter.setMultiSelectMode(false);
        
        lv.setAdapter(exMsgAdapter);
        lv.setOnItemClickListener(this);
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
		exMsgAdapter.setListViewForReference(lv);
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
        mNewMessageButtonLayout = (LinearLayout)findViewById(R.id.NewMessageLayout);
        mButtonLayout = (LinearLayout)findViewById(R.id.ButtonLayout);
        mDeleteButton = (Button)findViewById(R.id.Button_Delete);
        mCancelButton = (Button)findViewById(R.id.Button_Cancel);
        
        mDeleteButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        
        mButtonLayout.setVisibility(View.GONE);
        mNewMessageButtonLayout.setVisibility(View.GONE);
        
        mResolver = getContentResolver();
        
        startManagingCursor(mCursor);
	}

    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		// return super.onPrepareOptionsMenu(menu);
    	
    	//super.onPrepareOptionsMenu(menu);
    	onOptionsMenuClosed(menu);

		menu.removeItem(MENU_NEW_MESSAGE);
		menu.removeItem(MENU_MULTISELECT);
        menu.removeItem(MENU_DELETE_ALL);
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN] 
        menu.removeItem(MENU_ITEM_SELECT_ALL);		
		menu.removeItem(MENU_ITEM_DESELECT_ALL);
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]

        // 20100424 cutestar@lge.com Added Option Menu icon.
        ExMsgAdapter exMsgAdapter = (ExMsgAdapter)(lv.getAdapter());
        
        if(exMsgAdapter.getMultiSelectMode() == false){ // Do not show OptionMenu in Multiselect Mode
	    	if(mbExecutedByInCall == true){
	        		menu.add( 2, MENU_MULTISELECT, 2, R.string.excuse_messages_multi_select)
	                  .setIcon(R.drawable.ic_menu_multiselect);
	        		menu.add( 3, MENU_DELETE_ALL, 3, R.string.excuse_messages_delete_all)
	        		  .setIcon(android.R.drawable.ic_menu_delete); 
	    	}
	    	else{
	        		menu.add( 1, MENU_NEW_MESSAGE, 1, R.string.excuse_messages_new_message)
	        		  .setIcon(R.drawable.ic_menu_compose);
	        		menu.add( 2, MENU_MULTISELECT, 2, R.string.excuse_messages_multi_select)
	        		  .setIcon(R.drawable.ic_menu_multiselect);
	        		menu.add( 3, MENU_DELETE_ALL, 3, R.string.excuse_messages_delete_all)
	        		  .setIcon(android.R.drawable.ic_menu_delete);
	        	}
    	}
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
        else{
            menu.add(0, MENU_ITEM_SELECT_ALL, 0, R.string.excuse_messages_select_all)
                .setIcon(R.drawable.ic_menu_selectall);
            
		    menu.add(0, MENU_ITEM_DESELECT_ALL, 0, R.string.excuse_messages_deselect_all)
		        .setIcon(R.drawable.ic_menu_deselect);
        }
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
    	return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		menu.add( 1, MENU_NEW_MESSAGE, 1, R.string.excuse_messages_new_message);
		menu.add( 2, MENU_MULTISELECT, 2, R.string.excuse_messages_multi_select);
		menu.add( 3, MENU_DELETE_ALL, 3, R.string.excuse_messages_delete_all);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
        SparseBooleanArray sba = lv.getCheckedItemPositions();	
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
		if(item.getItemId() == MENU_NEW_MESSAGE){
			Intent intent = new Intent();
			
			intent.setClassName(this, "com.android.phone.EditExMsg");
			intent.putExtra(EXTRA_NEW_OR_EDIT_EXCUSE_MESSAGE, REQUEST_CODE_NEW_EXCUSE_MESSAGE);
			
			startActivityForResult(intent, REQUEST_CODE_NEW_EXCUSE_MESSAGE);
			
		} else if(item.getItemId() == MENU_MULTISELECT){
			ExMsgAdapter exMsgAdapter = (ExMsgAdapter)(lv.getAdapter());
			if (!exMsgAdapter.getMultiSelectMode()) {
				exMsgAdapter.setMultiSelectMode(true);
				setCheckBoxInitialize();
				mButtonLayout.setVisibility(View.VISIBLE);
			}			
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
			sba.clear(); 
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
			
		} else if(item.getItemId() == MENU_DELETE_ALL){
			showDialog(DELETE_ALL_DIALOG);
		}	
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
		else if(item.getItemId() == MENU_ITEM_SELECT_ALL){
			ExMsgAdapter exMsgAdapter = (ExMsgAdapter)(lv.getAdapter());

			for(int i = 0; i < lv.getCount(); i++){
				sba.append(i, true);
			}

        	exMsgAdapter.notifyDataSetChanged();
		}	
		else if(item.getItemId() == MENU_ITEM_DESELECT_ALL){
			ExMsgAdapter exMsgAdapter = (ExMsgAdapter)(lv.getAdapter());
        	sba.clear();
			exMsgAdapter.notifyDataSetChanged();
		}	
//LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
		return true;
	}

	public void onClick(View v) {
		ExMsgAdapter exMsgAdapter = (ExMsgAdapter)lv.getAdapter();
		if(v == mDeleteButton){
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
/*
            // 20100426 cutestar@lge.com Added isEmpty check func.  
			//SparseBooleanArray sba = lv.getCheckedItemPositions();
			
			//if (exMsgAdapter.getMultiSelectMode() && sba.indexOfValue(true) >= 0) {
			if (exMsgAdapter.getMultiSelectMode() && !multiSelect_selectedItem.isEmpty()) {
				showDialog(DELETE_DIALOG);
			}
*/
			SparseBooleanArray sba = lv.getCheckedItemPositions();
			boolean isChecked = false;
			
			for(int i = 0; i< lv.getCount(); i++){
				if(sba.get(i)){
					isChecked = true;
					break;
				}
			}
			
			if(lv.getCount() <= 0 || !isChecked)
				return;
			
			showDialog(DELETE_DIALOG);
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
		} else if(v == mCancelButton){
			mButtonLayout.setVisibility(View.GONE);
			exMsgAdapter.setMultiSelectMode(false);
			exMsgAdapter.notifyDataSetChanged();
		}
		
	}

	public void onItemClick(AdapterView<?> listView, View item, int position, long id) {
		ExMsgAdapter exMsgAdapter = (ExMsgAdapter)lv.getAdapter();
		
		// To Support Incoming-Call Status.
		if( (mbExecutedByInCall == true) && !(exMsgAdapter.getMultiSelectMode())){
		    TextView childTextView = (TextView) item.findViewById(R.id.ExcuseMsg);
			Intent sendIntent = new Intent(Intent.ACTION_VIEW);

			// Log.i("ExcuseMsg","(ExcuseMsg) mPhoneNumber: " + mPhoneNumber + " Text: " + childTextView.getText());
			
			sendIntent.putExtra("address", mPhoneNumber);
			sendIntent.putExtra("sms_body", childTextView.getText());
			sendIntent.setType("vnd.android-dir/mms-sms");
			
			startActivity(sendIntent);
		}
		else{
		    if (exMsgAdapter.getMultiSelectMode()) {
			    mCursor.moveToPosition(position);

			    Log.i(TAG,"exMsgAdapter.checkState.get(id) = "+exMsgAdapter.checkState.get(id));
			    if(exMsgAdapter.getInitialCheckBoxState()){
			    	exMsgAdapter.setInitialCheckBoxState(false);
			    }
/*			    
			    if(exMsgAdapter.checkState.get(id)==false){
			    	exMsgAdapter.checkState.put(id, true);
			    } 
                else if(exMsgAdapter.checkState.get(id)==true){
			    	//exMsgAdapter.checkState.put(id, false);
                	exMsgAdapter.checkState.remove(id);
			    }
*/			    
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
/*
			    if( exMsgAdapter.checkState.get(id) == false ){
			    	exMsgAdapter.checkState.put(id, true);
			    	multiSelect_selectedItem.put(id, true);
			    } 
                else if( exMsgAdapter.checkState.get(id) == true ){
			    	exMsgAdapter.checkState.put(id, false);
                	//exMsgAdapter.checkState.remove(id);
                	multiSelect_selectedItem.remove(id);
			    }
*/
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
			    exMsgAdapter.notifyDataSetChanged();
			}
			else {
			
				TextView childTextView = (TextView) item.findViewById(R.id.ExcuseMsg);
			    Intent intent = new Intent();
				intent.setClassName(this, "com.android.phone.EditExMsg");
				intent.putExtra(EXTRA_NEW_OR_EDIT_EXCUSE_MESSAGE,
						REQUEST_CODE_EDIT_EXCUSE_MESSAGE);			
			    intent.putExtra(EXTRA_ITEM_ID, id);
			    intent.putExtra(EXTRA_EDIT_MSG, childTextView.getText());
			    startActivityForResult(intent, REQUEST_CODE_EDIT_EXCUSE_MESSAGE);			
		    }		
		}
	}	
	
	public void deleteCheckedList(){		
		ExMsgAdapter exMsgAdapter = (ExMsgAdapter)lv.getAdapter();
		exMsgAdapter.setMultiSelectMode(false);
		mButtonLayout.setVisibility(View.GONE);
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
/*
		long id;
		String forDelete;
		StringBuffer _IDs = new StringBuffer();
		
		//_IDs.append(" IN (");
		_IDs.append("_id " + " IN(");
	    
		// To obtain the "checkState Values" without Key-Values.
        Map.Entry<Long,Boolean> mapEntry;
        Set<Map.Entry<Long,Boolean>> set; 
        Iterator<Map.Entry<Long,Boolean>> iterator;
		
        set = multiSelect_selectedItem.entrySet();
        iterator = set.iterator(); 
        int i = 0;
        
        while(iterator.hasNext()){
        	mapEntry = (Map.Entry<Long,Boolean>)iterator.next();
        	
        	if( i != 0 ){
        		_IDs.append(",");
        	}
        		
        	//if("1".equals(mapEntry.getValue())){
        	if(mapEntry.getValue()){
        		_IDs.append(mapEntry.getKey());
        		i++;
        	}
        }
        
        _IDs.append(")");
        
        forDelete = _IDs.toString();
        
        Log.i("CallHistoryDetailDeleteActivity","selection : _IDs : " + forDelete );
		
		
//		String forDelete = "";
//		mCursor.moveToFirst();
		
//		do{
//			id = mCursor.getLong(0);
//			if(exMsgAdapter.checkState.get(id)){
//				forDelete+=" "+id;
//			}
			
//		}while(mCursor.moveToNext());		
		
//		exMsgAdapter.setMultiSelectMode(false);

		
		//int deletedCount = mResolver.delete(ExMsgTableMetaData.CONTENT_URI,	"_id =" + forDelete, null);
        int deletedCount = mResolver.delete(ExMsgTableMetaData.CONTENT_URI,	forDelete, null);
*/
		long thID = -1;
		int maxList = lv.getCount();
		int checkCnt =0;
				
		SparseBooleanArray sba = lv.getCheckedItemPositions();
		Cursor cursor = exMsgAdapter.getCursor();
		for(int i = 0; i< maxList; i++)
		{
			if(sba.get(i))
			{
				thID = lv.getItemIdAtPosition(i);
				
				if (thID != -1) {
					if (cursor != null) {
						cursor.moveToPosition(i - checkCnt);
						cursor.deleteRow();
						checkCnt++;
					}
		// mDeleteUri = ContentUris.withAppendedId(Calls.CONTENT_URI, thID);
		// Log.d(TAG, "[JOYPARK] thID : " + thID + " mDeleteUri _call: "+ mDeleteUri);
		// getContentResolver().delete(Calls.CONTENT_URI, null, null);
		// startQuery();
		// mQueryHandler.startDelete(DELETE_CONVERSATION_TOKEN, null, mDeleteUri, null, null);
				}
			}
		}		
		sba.clear();
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]	
	}
	
	public void deleteAll(){
		ExMsgProvider.deleteAllItems();
		Cursor c = managedQuery(ExMsgTableMetaData.CONTENT_URI, null, null, null, null);		
		ExMsgAdapter adapter = (ExMsgAdapter) lv.getAdapter();
		adapter.changeCursor(c);	
		lv.invalidate();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DELETE_DIALOG:
			return new AlertDialog.Builder(ExcuseMessages.this)
			        .setIcon(android.R.drawable.ic_dialog_alert)			
			        .setTitle(R.string.excuse_messages_delete)
			        .setMessage(getResources().getString(R.string.excuse_messages_delete_information))
			        .setPositiveButton(R.string.excuse_messages_btn_delete,
			        	new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int whichButton) {
								deleteCheckedList();
								displayMessage(R.string.excuse_messages_deleted);
							}
					})
					.setNegativeButton(R.string.excuse_messages_btn_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
							}
					}).create();
		
		case DELETE_ALL_DIALOG:
			return new AlertDialog.Builder(ExcuseMessages.this)
			        .setIcon(android.R.drawable.ic_dialog_alert)			
			    	.setTitle(R.string.excuse_messages_delete_all)
			    	.setMessage(getResources().getString(R.string.excuse_messages_delete_information))
					.setPositiveButton(R.string.excuse_messages_btn_delete, 
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int whichButton) {
								deleteAll();
								displayMessage(R.string.excuse_messages_deleted);
							}
					})
					.setNegativeButton(R.string.excuse_messages_btn_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int whichButton) {
						}
					}).create();
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_NEW_EXCUSE_MESSAGE:
			if (resultCode == RESULT_CODE_SAVE) {
				ContentValues cv = new ContentValues();
				cv.put(ExMsgTableMetaData.KEY_IS_DEFAULT, ExMsgTableMetaData.NON_DEFAULT_ITEM);
				cv.put(ExMsgTableMetaData.KEY_MSG_CONTENT, data.getStringExtra(RESULT_MSG));
				mResolver.insert(ExMsgTableMetaData.CONTENT_URI, cv);			

				displayMessage(R.string.excuse_messages_saved);

			} else if (resultCode == RESULT_CODE_CANCEL) {			

			}
			break;

		case REQUEST_CODE_EDIT_EXCUSE_MESSAGE:
			if (resultCode == RESULT_CODE_SAVE) {
				long itemId = 0;
				ContentValues cv = new ContentValues();
				cv.put(ExMsgTableMetaData.KEY_MSG_CONTENT, data.getStringExtra(RESULT_MSG));
				cv.put(ExMsgTableMetaData.KEY_IS_DEFAULT, ExMsgTableMetaData.NON_DEFAULT_ITEM);
				
				itemId = data.getLongExtra(EXTRA_ITEM_ID, 0);
				mResolver.update(ExMsgTableMetaData.CONTENT_URI, cv, "_id = "+itemId, null);
			
			} else if (resultCode == RESULT_CODE_CANCEL) {			

			}
			break;

		}
	}
	
	public void setCheckBoxInitialize (){    	 
		ExMsgAdapter exMsgAdapter = (ExMsgAdapter)lv.getAdapter();
		exMsgAdapter.setInitialCheckBoxState(true);
		
		// James
		exMsgAdapter.checkState.clear();
		multiSelect_selectedItem.clear();
		
		exMsgAdapter.notifyDataSetChanged();
    }

	@Override
	protected void onStop() {
		super.onStop();
	}

	// 20100430 cutestar@lge.com  To show toast msg.
    public void displayMessage(int msg_id) {
        Toast.makeText(this, getString(msg_id), Toast.LENGTH_SHORT).show();
    }
	
}
