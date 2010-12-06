// LGE_MERGE_S
//20100804 jongwany.lee@lge.com attached this file CALL UI
package com.android.phone;

import android.app.AlertDialog;
import android.app.ListActivity;
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
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ExcuseMessagesMulti extends ListActivity implements OnItemClickListener {
    /** Called when the activity is first created. */
	
	private static final String DATABASE_NAME = "excuseMessages.db";
	private static final String DATABASE_TABLE = "excuseMessagesMaster";
	
	// 2010-09-04, cutestar@lge.com  modified for UI scenario 
	private static final int MENU_ITEM_SELECT_ALL = 1;	
	private static final int MENU_ITEM_DESELECT_ALL = 2;
	SQLiteDatabase myDatabase;
	Cursor mCursor;
	ExcuseMessagesAdapter adapter;
	Intent intent ;
	int _id;
	MenuItem menuItem;
	Menu menu;
	CheckBox cb_message;
	CheckedTextView ctv_message;
	String whereArgs = "(0=1)";
	String temp;
	String tag = "ExcuseMessages";
	ListView mListView;
	
	boolean chkDeleteDlg;
	
	// 2010-09-29, cutestar@lge.com applied disabled button in MultiSelect Mode.
	Button mBt_delete;
	Button mBt_cancel;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Log.i("withwind.park","onCreate");
        setContentView(R.layout.excuse_messages_list_multi_kor);
        
        //resize listView height
        TableLayout table_listview	=	(TableLayout)findViewById(R.id.table_listview);
        table_listview.setPadding(0, 0, 0, 70);
        
        mListView = (ListView)findViewById(R.id.listView);
        
        //show specific menu
        LinearLayout tl_multi_menu	=	(LinearLayout)findViewById(R.id.tl_multi_menu);
        //tl_multi_menu.setVisibility(8);	//GONE
        tl_multi_menu.setVisibility(View.VISIBLE);		// VISIBLE
        
        LinearLayout tl_multi_menu_add	= (LinearLayout)findViewById(R.id.tl_multi_menu_add);
        tl_multi_menu_add.setVisibility(View.INVISIBLE);
        //show title name is changes
//        TextView tv_title = (TextView)findViewById(R.id.tv_title);
//        tv_title.setText(R.string.excuse_messages_multi_select);
        
        //delete button is clicked 
        mBt_delete	=	(Button)findViewById(R.id.bt_delete);
        mBt_delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
            	
            	/*
            	if (whereArgs.equals("(0=1)")){
            		//nothing
            	}else{
            		createAlertDlg();
            	}
            	*/
            	createAlertDlg();
            }
        });
        
        //cancel button is clicked than finished
        mBt_cancel	=	(Button)findViewById(R.id.bt_cancel);
        mBt_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                finish();
            }
        });
        
        // 2010-09-29, cutestar@lge.com applied disabled button in MultiSelect Mode.
        mBt_delete.setEnabled(false);
        // 20100614 withwind.park use dbOpen method
        dbOpen();
        mCursor	=	getExcuseMessagesList();
        startManagingCursor(mCursor);
        
        adapter	=	new ExcuseMessagesAdapter(this, R.layout.excuse_messages_multiselect, mCursor, 
        			new String[]{"message"}, 
        	      new int[]{R.id.ExcuseMsg });        

        mListView.setVisibility(View.VISIBLE);
        mListView.setAdapter(adapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setOnItemClickListener(this);
        
        adapter.setListViewForReference(mListView);
        
    }
    
    // withwind.park 20100616 add checkedText check function start
    private boolean selectCheck()
    {
    	Log.i("withwind.park","called selectCheck");
    	
    	boolean revalue = false;
    	
    	SparseBooleanArray sba = mListView.getCheckedItemPositions();
    	
    	for (int i=0; i < sba.size();i++)
    	{
    		Log.i("withwind.park","is checked : " + i);
    		
    		if (sba.valueAt(i))
    		{
    			revalue = true;
    			//break;
    		}
    	}

    	return revalue;
    }
    // withwind.park 20100616 add checkedText check function end
	
    public void onResume(){
    	super.onResume();
    	//Log.i("withwind.park","onResume");
    	if (!myDatabase.isOpen())
    	{
    		dbOpen();
    		mCursor	=	getExcuseMessagesList();
            startManagingCursor(mCursor);
            
            adapter	=	new ExcuseMessagesAdapter(this, R.layout.excuse_messages_multiselect, mCursor, 
            			new String[]{"message"}, 
          	      new int[]{R.id.ExcuseMsg });        

            mListView.setVisibility(View.VISIBLE);
            mListView.setAdapter(adapter);
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
          adapter.setListViewForReference(mListView);
    	}
    	
    adapter.setOldCheck(mListView.getCheckedItemPositions());
    	
    	//Log.i("withwind.park","chkDeleteDlg : " + chkDeleteDlg);
    	
  	//if (chkDeleteDlg) createAlertDlg();
    }
    
    public void onPause() {
    	//Log.i("withwind.park","onPause");
    	super.onPause();
    	//this.finish();
    }
    
    public void onStop(){
    	//Log.i("withwind.park","onStop");
		if (myDatabase.isOpen()){
	    	myDatabase.close();	
	    }
    	super.onStop();
    	
    }
    
	//message list query
	private Cursor getExcuseMessagesList() {
		
		// 20100614 withwind.park use dbOpen method
		if (!myDatabase.isOpen()) dbOpen();
		
		String[] result_columns = new String[] {"_id", "opt", "message", "modified", "modified_time" } ;
		return myDatabase.query(true, DATABASE_TABLE, result_columns, null, null, null, null, null, null);
	}


	protected void createAlertDlg(){
		
		Log.i("withwind.park","called createAlertDlg");
		
		if (!selectCheck()) return;
		
		Log.i("withwind.park","selectCheck() : " + selectCheck());

		chkDeleteDlg = true;
		
		AlertDialog.Builder adb = new AlertDialog.Builder(this); 
		adb.setIcon(android.R.drawable.ic_dialog_alert);
		adb.setTitle(this.getResources().getString(R.string.excuse_messages_delete_question));
		adb.setMessage(R.string.excuse_messages_delete_information);

		adb.setPositiveButton(this.getResources().getString(R.string.ok), new OnClickListener(){
			public void onClick(DialogInterface arg0, int arg1){
				chkDeleteDlg = false;
				deleteSelectedExcuseMessage(whereArgs);
			}
		});

		adb.setNegativeButton(this.getResources().getString(R.string.cancel), new OnClickListener(){
			public void onClick(DialogInterface arg0, int arg1){
				chkDeleteDlg = false;
				//nothing
			}
		});

		adb.setCancelable(true);
        adb.setOnCancelListener(new OnCancelListener() {
           public void onCancel(DialogInterface dialog) {
        	   chkDeleteDlg = false;
        	   //nothing
           }
        });

        //adb.setMessage("Shall i delete selected items ?");
        adb.show();
	}

	// 20100616 withwind.park bug fix start
    public void deleteSelectedExcuseMessage(String whereArgs) {
    	
    	SparseBooleanArray sba = mListView.getCheckedItemPositions();
    	
    	for (int i=0; i < sba.size();i++)
    	{
    		if (sba.valueAt(i))
    		{
    			mCursor.moveToPosition(sba.keyAt(i));
				_id = mCursor.getInt(mCursor.getColumnIndexOrThrow("_id"));
				
				whereArgs += " or _id=" + _id;
    		}
    	}

    	
    	// 20100614 withwind.park use dbOpen method
    	if (!myDatabase.isOpen()) dbOpen();
    	
		myDatabase.delete(DATABASE_TABLE, whereArgs, null);

        // 2010-09-04, cutestar@lge.com  modified for UI scenario 
		this.finish();		// go to the previous activity
		Toast.makeText(this, getString(R.string.excuse_messages_deleted), Toast.LENGTH_SHORT).show();
	}
    // 20100616 withwind.park bug fix end
	
    //on key press BACK_KEY or MENU_KEY
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

    	switch(keyCode) {
		case KeyEvent.KEYCODE_MENU:
			return super.onKeyDown(keyCode, event);
		case KeyEvent.KEYCODE_BACK:
			finish();
			return true;
		}
		return false;
	}
    
    // 20100614 withwind.park add dbOpen method start
    private void dbOpen()
    {
    	myDatabase	=	openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
    }
    // 20100614 withwind.park add dbOpen method end
	public void onItemClick(AdapterView<?> listView, View item, int position, long id) {
		ExcuseMessagesAdapter exMsgAdapter = (ExcuseMessagesAdapter)mListView.getAdapter();
			    mCursor.moveToPosition(position);

		// 2010-09-29, cutestar@lge.com applied disabled button in MultiSelect Mode.
		SparseBooleanArray sba = mListView.getCheckedItemPositions();
		boolean existCheckboxData = false;
    	for (int i=0; i < sba.size();i++)
    	{
    		if (sba.valueAt(i))
    		{
    			existCheckboxData = true;
    			break;
    		}
    	}
    	mBt_delete.setEnabled(existCheckboxData);
		
			    exMsgAdapter.notifyDataSetChanged();
	}	
	
	// 2010-09-04, cutestar@lge.com  modified for UI scenario 
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		
		int groupId	=	0;								//group id
		int menuItemOrder = menu.NONE;					//item order position
		
		int menuItemText1 = R.string.excuse_messages_select_all;		
		int menuItemText2 = R.string.excuse_messages_deselect_all;		

		menu.add(groupId, MENU_ITEM_SELECT_ALL, menuItemOrder, menuItemText1).setIcon(R.drawable.ic_menu_selectall);
		menu.add(groupId, MENU_ITEM_DESELECT_ALL, menuItemOrder, menuItemText2).setIcon(R.drawable.ic_menu_deselect);

		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		
		super.onOptionsItemSelected(item);
		//what menuItem selected
		
		SparseBooleanArray sba = mListView.getCheckedItemPositions();
		ExcuseMessagesAdapter adapter;
		
		switch (item.getItemId()){
		
		case MENU_ITEM_SELECT_ALL :
			adapter = (ExcuseMessagesAdapter)(mListView.getAdapter());

			for(int i = 0; i < mListView.getCount(); i++){
				sba.append(i, true);
			}
			
			// 2010-09-29, cutestar@lge.com applied disabled button in MultiSelect Mode.
			mBt_delete.setEnabled(true);
			
			adapter.notifyDataSetChanged();
			
			return true;

		case MENU_ITEM_DESELECT_ALL :	
			adapter = (ExcuseMessagesAdapter)(mListView.getAdapter());

			sba.clear();
			
			// 2010-09-29, cutestar@lge.com applied disabled button in MultiSelect Mode.
			mBt_delete.setEnabled(false);
			
			adapter.notifyDataSetChanged();

			return true;
		}
		return false;
	}
    
}
// LGE_MERGE_S
