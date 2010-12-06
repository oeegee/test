
// LGE_MERGE_S
//20100804 jongwany.lee@lge.com attached it for EXCUSE MESSAGE
/* Made by Kim.jungtae
 * 20100428
 * This class is used by Excuse Messages after Calling. 
 *
*/

package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

// LGE_MERGE_S sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message
import android.view.WindowManager;
// LGE_MERGE_E sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message

public class InCallMessagesList extends Activity {
	
	private static final String TAG = "InCallMessagesList";
	private static final boolean DBG = true;
    
	private static final String DATABASE_NAME = "excuseMessages.db";
	private static final String DATABASE_TABLE = "excuseMessagesMaster";
	
	SQLiteDatabase myDatabase;
	ListView lv;
	Cursor mCursor;
	ListAdapter adapter;
	Intent intent ;
	int _id;
	MenuItem menuItem;
	Menu menu;
    String rName;
    String rNumber;
    Button bt_new_message_incall;
    Button bt_send;
    TextView tv_message;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (DBG) log("start onCreate");

        // LGE_MERGE_S sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        // LGE_MERGE_E sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message

        setContentView(R.layout.excuse_messages_list);
        
        //resize listView height
        TableLayout table_listview	=	(TableLayout)findViewById(R.id.table_listview);
        table_listview.setPadding(0, 80, 0, 0);
        
        intent =	this.getIntent();
        rName = intent.getStringExtra("rName");			//caller's name
        rNumber = intent.getStringExtra("rNumber");		//caller's phone number
        
        //show newMessage Button
        bt_new_message_incall = (Button)findViewById(R.id.bt_new_message_incall);
        bt_new_message_incall.setVisibility(View.VISIBLE);
        
        //make database and table to use ExucuseMessages 
        createDataBase();
        
        //LGE_S kim.jungtae : 2010-06-30
        //translating message Language by Locale
        changeMessagesByLocale();
        //LGE_E kim.jungtae : 2010-06-30

    	//getExcuseMessagesList
        mCursor	=	getExcuseMessagesList();
        startManagingCursor(mCursor);
        
        adapter	=	new SimpleCursorAdapter(this, R.layout.excuse_messages_list_item_send, mCursor, 
        		new String[]{"message"}, 
        		new int[]{R.id.tv_message });
        
        lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(adapter);
        
        lv.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				
				
				//cursor set
				mCursor.moveToPosition(pos);
				_id = mCursor.getInt(mCursor.getColumnIndexOrThrow("_id"));
				String message = mCursor.getString(mCursor.getColumnIndexOrThrow("message"));
				
				//notify that private number is not available.
				if (	rNumber == null ||	(rNumber+"").length()==0	||	"+".equals(rNumber+"")	){	//caller's number is unavailable as private.
		    		Toast.makeText(InCallMessagesList.this, "Caller's number is not available.", Toast.LENGTH_SHORT).show();
		    	}else{
					
					Intent intent = new Intent(Intent.ACTION_SENDTO);
			        intent.setData(Uri.parse("smsto:" + rNumber));
			        intent.putExtra("sms_body", message);
			        intent.putExtra("fromActivity", "ExcuseMessages");
			        startActivity(intent);
                             // LGE_MERGE_S sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message
                             finish();
                             // LGE_MERGE_S sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message
		    	}
			}
        });
        

        
        //specific menu visivility check and execution
        //TableLayout tl_multi_menu	=	(TableLayout)findViewById(R.id.tl_multi_menu);
        
    }

    public void onResume(){
    	super.onPause();
    	
    	if (DBG) log("start onResume");
    	
        TextView tv_no_messages = (TextView)findViewById(R.id.tv_no_messages);
        
        if (DBG) log("lv.getCount : " + lv.getCount());
        
        if (lv.getCount()==0){
        	tv_no_messages.setVisibility(View.VISIBLE);// VISIBLE
        }else{
        	tv_no_messages.setVisibility(View.GONE);//GONE
        }
    	
        //new message button click
        bt_new_message_incall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
            	
            	
				//notify that private number is not available.
		    	if (	rNumber == null ||	(rNumber+"").length()==0	||	"+".equals(rNumber+"")	){	//caller's number is unavailable as private.
		    		Toast.makeText(InCallMessagesList.this, "Caller's number is not available.", Toast.LENGTH_SHORT).show();
		    	}else{
					Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", rNumber, null));
					intent.putExtra("fromActivity", "ExcuseMessages");	//this property means for Exception process in MMS modele (made by taeseok)
					startActivity(intent);
                                   // LGE_MERGE_S sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message
                                   finish();
                                   // LGE_MERGE_S sanghoon.roh@lge.com 2010/06/04 dismiss keyguard for the excuse message
		    	}
            }
        });
        

        
    }
    
    
    public void onPause() {
    	super.onPause();
    	//Log.d("kim.jungtae", "List_onPause");

    }
    
    public void onStop(){
    	//Log.d("kim.jungtae", "List_onPause");
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
			
			
			if(DBG)log("modifedCheck:start ^^");
			for(int i=1;i<6;i++){
				String selection = "_id="+i;
				c	=	myDatabase.query(DATABASE_TABLE, modifiedColumn, selection, null, null, null, null);
				
				startManagingCursor(c);
				c.moveToFirst();
				
				if (c.getCount()>0){
					messageVal	=	c.getString(0);
					modifiedVal	=	c.getInt(1);
					//if(DBG)log("modifiedVal:"+modifiedVal+":"+exuse[i-1]);
					if(DBG)log("modifiedVal:"+messageVal+":"+exuse[i-1]);
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
			if(DBG)log("modifedCheck:end ^^");
			c.close();
			
		}catch (Exception e){
			if(DBG)log("modifedCheck: What the shoot!!!");
			if(DBG)log("err : "+e);
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
		
		if (DBG) log("call onCreateOptionsMenu");
		
		super.onCreateOptionsMenu(menu);
		
		int groupId	=	0;								//group id
		int menuItemOrder = menu.NONE;					//item order position
		
		int menuItemText1 = R.string.menu_item1;		//text, to be printed at this item.
		int menuItemText2 = R.string.menu_item2;		//text, to be printed at this item.
		int menuItemText3 = R.string.menu_item3;		//text, to be printed at this item.

		menu.add(groupId, 1, menuItemOrder, menuItemText1);
		menu.add(groupId, 2, menuItemOrder, menuItemText2);
		menu.add(groupId, 3, menuItemOrder, menuItemText3);
			
		//Toast.makeText(this, menuItem.getItemId()+"", Toast.LENGTH_SHORT).show();
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		
		int groupId	=	0;								//group id
		int menuItemOrder = menu.NONE;					//item order position
		
		menu.findItem(1).setVisible(false);
		
		if (lv.getCount() == 0){
			menu.findItem(2).setVisible(false);
			menu.findItem(3).setVisible(false);

		}else{
			menu.findItem(2).setVisible(true);
			menu.findItem(3).setVisible(true);

		}
		return true;

	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		
		super.onOptionsItemSelected(item);
		//what menuItem selected
		switch (item.getItemId()){
		
		
		case 1 :	//New message
			/*intent = new Intent(this, InCallMessagesComposer.class);
			this.startActivity(intent);*/
			return true;
		case 2 :	//MultiSelect
			intent = new Intent(this, ExcuseMessagesMulti.class); 
			this.startActivity(intent);
			return true;
		case 3 :	//Delete All
			createAlertDlg();

		}
		return false;
	}
	

	protected void createAlertDlg(){
		
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setTitle("Delete All?                               ");
		
		adb.setPositiveButton("OK", new OnClickListener(){
			public void onClick(DialogInterface arg0, int arg1){
				deleteAllExcuseMessage();
			}
		});

		adb.setNegativeButton("Cancel", new OnClickListener(){
			public void onClick(DialogInterface arg0, int arg1){
				//nothing
			}
		});
		
		adb.setCancelable(true);
        adb.setOnCancelListener(new OnCancelListener() {
           public void onCancel(DialogInterface dialog) {
        	 //nothing
           }
        });
        
        //adb.setMessage("Shall i delete All of these?");
        adb.show();
		
	}
	
	//deleting message
    public void deleteAllExcuseMessage() {

		myDatabase.delete(DATABASE_TABLE, null, null);
		
		//Intent intent = new Intent(this, InCallMessagesList.class);
		this.finish();
		startActivity(intent);
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
    
    void log(String msg)
    {
    	Log.i(TAG,msg);
    }
}
// LGE_MERGE_E














