// LGE_MERGE_S
//20100804 jongwany.lee@lge.com attached this file CALL UI
package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import com.lge.config.StarConfig;

public class ExcuseMessagesList extends Activity {
	
	static final boolean DBG = false;
	static final String TAG = "ExcuseMessagesList";
    
	private static final String DATABASE_NAME = "excuseMessages.db";
	private static final String DATABASE_TABLE = "excuseMessagesMaster";
	
	private SQLiteDatabase myDatabase;
	private ListView lv;
	private Cursor mCursor;
	private ListAdapter adapter;
	private Intent intent;
	int _id;
	private MenuItem menuItem;
	private Menu menu;
	boolean isDeleteAll;
	// 2010-11-04, cutestar@lge.com added command btn for LGT Hub (referred from Scenario)
	private Button mBt_add;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	if ( StarConfig.COUNTRY.equals("KR") ){
	        setContentView(R.layout.excuse_messages_list_kor);
	}else{
	        setContentView(R.layout.excuse_messages_list);
	}        
        //resize listView height
        TableLayout table_listview	=	(TableLayout)findViewById(R.id.table_listview);
        table_listview.setPadding(0, 0, 0, 0);
        
        //make database and table to use ExucuseMessages 
        createDataBase();
        
        //LGE_S kim.jungtae : 2010-06-30
        //translating message Language by Locale
        changeMessagesByLocale();
        //LGE_E kim.jungtae : 2010-06-30

    	//getExcuseMessagesList
        mCursor	=	getExcuseMessagesList();
        startManagingCursor(mCursor);
        
        adapter	=	new SimpleCursorAdapter(this, R.layout.excuse_messages_list_item, mCursor, 
        		new String[]{"message"}, 
        		new int[]{R.id.tv_message });
        
        lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(adapter);
        
        intent = new Intent(this, ExcuseMessagesEdit.class);
        
        lv.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				
				//cursor set
				mCursor.moveToPosition(pos);
				_id = mCursor.getInt(mCursor.getColumnIndexOrThrow("_id"));
				String message = mCursor.getString(mCursor.getColumnIndexOrThrow("message"));
				
				Intent intent=new Intent(getApplicationContext(),ExcuseMessagesEdit.class);
				intent.putExtra("_id", _id);
				intent.putExtra("message", message);
				startActivity(intent);
			}
        });
	if ( StarConfig.COUNTRY.equals("KR") ){
//20101105 hangyul.park@lge.com Add Btn [START_LGE_LAB1]
        //specific menu visibility check and execution
        LinearLayout tl_multi_menu	=	(LinearLayout)findViewById(R.id.tl_multi_menu);
        tl_multi_menu.setVisibility(View.GONE);
        
        LinearLayout tl_multi_menu_add	= (LinearLayout)findViewById(R.id.tl_multi_menu_add);
        tl_multi_menu_add.setVisibility(View.VISIBLE);
        
        mBt_add	=	(Button)findViewById(R.id.bt_add);
        mBt_add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
    			Intent intent_add = new Intent(ExcuseMessagesList.this, ExcuseMessagesEdit.class);
    			intent_add.putExtra("Click_Button", true);
    			ExcuseMessagesList.this.startActivity(intent_add);
            }
        });
//20101105 hangyul.park@lge.com Add Btn [END_LGE_LAB1]


	}else{        
	        //specific menu visivility check and execution
	        TableLayout tl_multi_menu	=	(TableLayout)findViewById(R.id.tl_multi_menu);
	}
    }
    public void onDestroy()
    {
      super.onDestroy();
      if(mCursor != null) mCursor.close();
      if (myDatabase.isOpen())
      	myDatabase.close();
    }

    public void onResume(){
    	super.onResume();

        TextView tv_no_messages = (TextView)findViewById(R.id.tv_no_messages);
        
        if (lv.getCount()==0){
        	tv_no_messages.setVisibility(0);// VISIBLE
        }else{
        	tv_no_messages.setVisibility(8);//GONE
        }
        
        if (isDeleteAll) createAlertDlg();
    }
    
    public void onPause() {
    	super.onPause();

    }
    
    public void onStop(){
    	super.onStop();
    	
    }
    
    
    
    //crateTable syntax
	private static final String TABLE_CREATE = 
		"	create table " + DATABASE_TABLE +
		"	(										"+
		"	_id integer primary key autoincrement,	"+
		"	opt text ,								"+
		"	message text ,							"+
		"	modified integer ,						"+
		"	modified_time integer 					"+
		"	)										";
	
	//make database and table to use ExucuseMessages
	private void createDataBase() {
		myDatabase	=	openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
		
		try{
			//check for db existence by generic query
			myDatabase.query(DATABASE_TABLE, null, null, null, null, null, null);
			
		}catch (Exception e){
			myDatabase.execSQL(TABLE_CREATE);

			//LGE_S kim.jungtae : insert default reserved messages
			ContentValues cVals	=	new ContentValues();
			cVals.put("opt", "");
			cVals.put("message", this.getResources().getString(R.string.excuse_1) );
			cVals.put("modified", 0);
			cVals.put("modified_time", System.currentTimeMillis());
			myDatabase.insert(DATABASE_TABLE, null, cVals);
			
			cVals.put("message", this.getResources().getString(R.string.excuse_2) );
			myDatabase.insert(DATABASE_TABLE, null, cVals);
			
			cVals.put("message", this.getResources().getString(R.string.excuse_3) );
			myDatabase.insert(DATABASE_TABLE, null, cVals);
			
			cVals.put("message", this.getResources().getString(R.string.excuse_4) );
			myDatabase.insert(DATABASE_TABLE, null, cVals);
			
			cVals.put("message", this.getResources().getString(R.string.excuse_5) );
			myDatabase.insert(DATABASE_TABLE, null, cVals);
			
			//LGE_E kim.jungtae : insert default reserved messages
			
		}
	}
	
	//LGE_S kim.jungtae : 2010-06-30
	//translating message Language by Locale
	private void changeMessagesByLocale() {
		try{
			//check columns "modified" and modify messages by present locale. 
			String[] modifiedColumn	=	{"message", "modified"};
			Cursor c=null;
			String messageVal;
			int modifiedVal;

			String excuse_1	=	this.getResources().getString(R.string.excuse_1);
			String excuse_2	=	this.getResources().getString(R.string.excuse_2);
			String excuse_3	=	this.getResources().getString(R.string.excuse_3);
			String excuse_4	=	this.getResources().getString(R.string.excuse_4);
			String excuse_5	=	this.getResources().getString(R.string.excuse_5);
			String exuse[]	=	{excuse_1 , excuse_2 , excuse_3 , excuse_4, excuse_5 };
			
			if (DBG) Log.i(TAG,excuse_1);
			
			for(int i=1;i<6;i++){
				String selection = "_id="+i;
				c	=	myDatabase.query(DATABASE_TABLE, modifiedColumn, selection, null, null, null, null);
				
				startManagingCursor(c);
				c.moveToFirst();
				
				if (c.getCount()>0){
					messageVal	=	c.getString(0);
					modifiedVal	=	c.getInt(1);
					
					if (DBG) Log.i(TAG,exuse[i-1]);
					if (DBG) Log.i(TAG,messageVal.toString());
					if (DBG) Log.i(TAG,""+modifiedVal);

					if (	modifiedVal == 0 && !exuse[i-1].equals(messageVal)	){
						ContentValues cVals	=	new ContentValues();
						cVals.put("opt", "");
						cVals.put("message", exuse[i-1]);
						cVals.put("modified", 0);
						cVals.put("modified_time", System.currentTimeMillis());
						String whereArgs	=	selection;
						myDatabase.update(DATABASE_TABLE, cVals, whereArgs, null );
					}					
				}
			}
			if(c != null) c.close();
			
		}catch (Exception e){
			if (DBG) Log.i(TAG,"changeMessagesByLocale error");
		}
	}
	//LGE_E kim.jungtae : 2010-06-30
	
	//message list query
	private Cursor getExcuseMessagesList() {
		String[] result_columns = new String[] {"_id", "opt", "message", "modified", "modified_time" } ;
		return myDatabase.query(true, DATABASE_TABLE, result_columns, null, null, null, null, null, null);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		
		int groupId	=	0;								//group id
		int menuItemOrder = menu.NONE;					//item order position
		
	if ( StarConfig.COUNTRY.equals("KR") ){

//20101105 hangyul.park@lge.com Add Btn [START_LGE_LAB1]
		int menuItemText2 = R.string.excuse_messages_delete;		       //text, to be printed at this item.
		int menuItemText3 = R.string.excuse_messages_delete_all;		   //text, to be printed at this item.

		menu.add(groupId, 2, menuItemOrder, menuItemText2).setIcon(android.R.drawable.ic_menu_delete);
		menu.add(groupId, 3, menuItemOrder, menuItemText3).setIcon(R.drawable.ic_menu_delete_all);
//20101105 hangyul.park@lge.com Add Btn [END_LGE_LAB1]

	}else{
			int menuItemText1 = R.string.excuse_messages_new_message;		//text, to be printed at this item.
			int menuItemText2 = R.string.excuse_messages_multi_select;		//text, to be printed at this item.
			int menuItemText3 = R.string.excuse_messages_delete_all;		//text, to be printed at this item.

			menu.add(groupId, 1, menuItemOrder, menuItemText1).setIcon(R.drawable.ic_menu_compose);
			menu.add(groupId, 2, menuItemOrder, menuItemText2).setIcon(R.drawable.ic_menu_multiselect);
			menu.add(groupId, 3, menuItemOrder, menuItemText3).setIcon(R.drawable.ic_menu_delete_all);
	}	
			
		//Toast.makeText(this, menuItem.getItemId()+"", Toast.LENGTH_SHORT).show();
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		
		int groupId	=	0;								//group id
		int menuItemOrder = menu.NONE;					//item order position
		
		if (lv.getCount() == 0){
			if ( StarConfig.COUNTRY.equals("KR") ){
				menu.findItem(2).setEnabled(false);
				menu.findItem(3).setEnabled(false);
			}else{
				menu.findItem(2).setVisible(false);
				menu.findItem(3).setVisible(false);
			}
		}else{
			if ( StarConfig.COUNTRY.equals("KR") ){
				menu.findItem(2).setEnabled(true);
				menu.findItem(3).setEnabled(true);
			}else{
				menu.findItem(2).setVisible(true);
				menu.findItem(3).setVisible(true);
			}

		}
		//Toast.makeText(this, menuItem.getItemId()+"", Toast.LENGTH_SHORT).show();
		return true;

	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		
		super.onOptionsItemSelected(item);
		//what menuItem selected
		switch (item.getItemId()){
		
		
		case 1 :	//New message
			intent = new Intent(this, ExcuseMessagesEdit.class);
			this.startActivity(intent);
			return true;
		case 2 :	//MultiSelect
			intent = new Intent(this, ExcuseMessagesMulti.class);
			this.startActivity(intent);
			return true;
		case 3 :	//Delete All
			createAlertDlg();
			isDeleteAll = false;
		}
		return false;
	}
	

	protected void createAlertDlg(){
		
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		
		adb.setCancelable(true);
		adb.setTitle(R.string.excuse_messages_delete_all);
		adb.setIcon(android.R.drawable.ic_dialog_alert);
		
		adb.setPositiveButton(R.string.ok, new OnClickListener(){
			public void onClick(DialogInterface arg0, int arg1){
				deleteAllExcuseMessage();
			}
		});
		adb.setNegativeButton(R.string.cancel,new OnClickListener(){
			public void onClick(DialogInterface arg0, int arg1){
				isDeleteAll = false;
			}
		});
		
        adb.setOnCancelListener(new OnCancelListener() {
           public void onCancel(DialogInterface dialog) {
        	 //nothing
        	  isDeleteAll = false;
           }
        });
        
		adb.setMessage(R.string.excuse_messages_delete_all_information);
        adb.show();
		
	}
	
	//deleting message
    public void deleteAllExcuseMessage() {

		myDatabase.delete(DATABASE_TABLE, null, null);
		
		Intent intent = new Intent(this, ExcuseMessagesList.class);
		this.startActivity(intent);
		this.finish();
	}
	
    //on key press BACK_KEY or MENU_KEY
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

    	switch(keyCode) {
		case KeyEvent.KEYCODE_MENU:
			return super.onKeyDown(keyCode, event);
		case KeyEvent.KEYCODE_BACK:
	    	if (myDatabase.isOpen()){
	    		myDatabase.close();	
	    	}
			finish();
			return true;
		}

		return false;
	}
}
// LGE_MERGE_E
