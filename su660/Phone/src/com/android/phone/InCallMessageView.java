package com.android.phone;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import com.android.phone.InCallMessageViewCursorAdapter;
import com.lge.config.StarConfig;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class InCallMessageView extends RelativeLayout implements OnTouchListener {
	
	public static final String TAG = "Flicking SMS";
	public static final boolean DBG = true;
	
	public static final int ST_HIDE = 0;
	public static final int ST_LOCK_VIEW = 1;
	public static final int ST_UNLOCK_VIEW = 2;
	
	public static final int MODE_LOCK = 0;
	public static final int MODE_UNLOCK = 1;
	
	int cuMode;
	
	Context context;
	Activity mActivity;
	InCallTouchUi mCallUi;
	
	// var for UI
	public static final int topPos = -87; 
	static final int statusHeight = 113;
	int displayWidth,displayHeight;
	float cuHeight;
	float downPosY;
	float prevPosY;
	float dragOffsetY;
	float dragSpeed = 50;
	
	public static final int MOVE_UP = 0;
	public static final int MOVE_DOWN = 1;
	private int cuDirection;
	Button flickBtn;
	
	private LoopHandler mLoopHandler = new LoopHandler();
	
	class LoopHandler extends Handler{
		private boolean bStop;
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if(bStop == false)
				InCallMessageView.this.loop();
			super.handleMessage(msg);
		}
		private void sleep(long delayMillis){
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
		private void stop(){
			bStop = true;
		}
		private void start(){
			bStop = false;
			InCallMessageView.this.loop();
		}
	};
	
	// var for data
	private static final String DATABASE_NAME = "excuseMessages.db";
	private static final String DATABASE_TABLE = "excuseMessagesMaster";

	Button bt_new_message_incall;
  Button bt_send;
    TextView tv_no_messages;
	ListView lv;
	
	SQLiteDatabase myDatabase;
	Cursor mCursor;
	ListAdapter adapter;
	int _id;
	MenuItem menuItem;
	Menu menu;
    
	public InCallMessageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		if (DBG) Log.i(TAG,"constructor call");
		
		this.context = context;
		
		LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.incall_msg_view, this, true);
	}

	void initMessageView(InCallTouchUi callUi)
  {

    this.mActivity = callUi.getActivity();
    this.mCallUi = callUi;
    createDataBase();

    //LGE_S kim.jungtae : 2010-06-30
    //translating message Language by Locale
    changeMessagesByLocale();
    //LGE_E kim.jungtae : 2010-06-30

    setDisplaySize(mActivity.getWindowManager().getDefaultDisplay().getWidth(),mActivity.getWindowManager().getDefaultDisplay().getHeight());

    bt_new_message_incall = (Button)findViewById(R.id.bt_new_message_incall);
    bt_new_message_incall.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v){

      mCallUi.onSendSms(null, false);
      }
    });

    tv_no_messages = (TextView)findViewById(R.id.tv_no_messages);

    mCursor	=	getExcuseMessagesList();
    mActivity.startManagingCursor(mCursor);

    adapter	=	new InCallMessageViewCursorAdapter(context, R.layout.excuse_messages_list_item_send, mCursor, 
      new String[]{"message"}, 
      new int[]{R.id.tv_message },
      mCallUi);

    lv = (ListView)findViewById(R.id.listView);
    lv.setAdapter(adapter);


    lv.setOnItemClickListener(new OnItemClickListener()
    {
      public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

      //cursor set
      mCursor.moveToPosition(pos);
      _id = mCursor.getInt(mCursor.getColumnIndexOrThrow("_id"));
      String message = mCursor.getString(mCursor.getColumnIndexOrThrow("message"));
      Log.d("KisanMAN","onSendSMS ListClick");
      mCallUi.onSendSms(message, false);
    }
    });
  }

	public void loop() {
		// TODO Auto-generated method stub
		
		if (cuDirection == MOVE_UP)
		{
			if (cuHeight > topPos)
			{
				if (cuHeight - dragSpeed <= topPos)
				{
					cuHeight = topPos;
					dragSpeed = 50;
				}
				else
				{
					cuHeight -= dragSpeed;
				}
				setHeight(cuHeight);
				mLoopHandler.sleep(10);
			}
			else
			{
				updateState(ST_UNLOCK_VIEW);
			} 
		}
		else if (cuDirection == MOVE_DOWN)
		{
			if (cuHeight < this.displayHeight - statusHeight)
			{
				if (cuHeight + dragSpeed > this.displayHeight - statusHeight)
				{
					cuHeight = this.displayHeight - statusHeight;
					dragSpeed = 50;
				}
				else
				{
					cuHeight += dragSpeed;
				}
				
				setHeight(cuHeight);
				mLoopHandler.sleep(10);
			}
			else
			{
				updateState(ST_LOCK_VIEW);
				//cuHeight = this.displayHeight - statusHeight;
				//setHeight(cuHeight);
			}
		}
		
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		
		flickBtn = (Button)findViewById(R.id.flickBtn);
		flickBtn.setOnTouchListener(this);
	}
	
	

	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		cuHeight = this.displayHeight - statusHeight;
		setHeight(cuHeight);
	}
	
	private void setHeight(float height)
	{
		ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(this.getLayoutParams());
        margin.setMargins(0, (int)height, 0, (int)(height * -1) + topPos);
        this.setLayoutParams(new RelativeLayout.LayoutParams(margin));
        
        if (DBG) Log.i(TAG,"setHeight call : " + height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		
		if (DBG) Log.i(TAG,"onLayout call");
		super.onLayout(changed, l, t, r, b);
	}

	public void setDisplaySize(int width, int height) {
		// TODO Auto-generated method stub
		this.displayWidth = width;
		this.displayHeight = height;
	}

	public boolean onTouch(View v, MotionEvent e) {
		// TODO Auto-generated method stub
		
		if (v.equals(this.flickBtn) && ((KeyguardManager)mActivity.getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode())
		{
			switch (e.getAction())
			{
				case MotionEvent.ACTION_UP:
				{
					if (DBG) Log.i(TAG,"up");
					cuHeight = cuHeight - dragOffsetY;			
					mLoopHandler.start();
				}
				break;
				
				case MotionEvent.ACTION_DOWN:
				{
					if (DBG) Log.i(TAG,"down");
					downPosY = e.getRawY();
				}
				break;
				
				case MotionEvent.ACTION_MOVE:
				{
					if (DBG) Log.i(TAG,"move");
					dragOffsetY = downPosY - e.getRawY();
					setHeight(cuHeight - dragOffsetY);
					
					if (prevPosY - e.getRawY() > 20)
					{
						cuDirection = MOVE_UP;
					}
					else
					{
						cuDirection = MOVE_DOWN;
					}
					
					prevPosY = e.getRawY();
				}
				break;
			}
		}
			
		return false;
	}

	// Data Functions
	//message list query
	private Cursor getExcuseMessagesList() {
		String[] result_columns = new String[] {"_id", "opt", "message", "modified", "modified_time" } ;
		return myDatabase.query(true, DATABASE_TABLE, result_columns, null, null, null, null, null, null);
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
		myDatabase	=	this.mActivity.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
		
		try{
			myDatabase.query(DATABASE_TABLE, null, null, null, null, null, null);
		}catch (Exception e){
			myDatabase.execSQL(TABLE_CREATE);

			//LGE_S kim.jungtae : insert default reserved messages
			ContentValues cVals	=	new ContentValues();
			cVals.put("opt", "");
			
			//<!--//20100929 sumi920.kim@lge.com change default excusemessage list [STAR_LGE_LAB1] 
			if(StarConfig.COUNTRY.equals("KR"))
			{
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_0) );
				cVals.put("modified", 0);
				cVals.put("modified_time", System.currentTimeMillis());
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_1) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_2) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_3) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_4) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);					
			}
			else
			{
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_defaultmsg_0) );
				cVals.put("modified", 0);
				cVals.put("modified_time", System.currentTimeMillis());
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_defaultmsg_1) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_defaultmsg_2) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_defaultmsg_3) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);
				
				cVals.put("message", this.getResources().getString(R.string.excuse_messages_defaultmsg_4) );
				myDatabase.insert(DATABASE_TABLE, null, cVals);
			}
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
			
			//<!--//20100929 sumi920.kim@lge.com change default excusemessage list [STAR_LGE_LAB1] 
			String excuse_1;
			String excuse_2;
			String excuse_3;
			String excuse_4;
			String excuse_5;
			
			if(StarConfig.COUNTRY.equals("KR"))
			{
				excuse_1	=	this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_0);
				excuse_2	=	this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_1);
				excuse_3	=	this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_2);
				excuse_4	=	this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_3);
				excuse_5	=	this.getResources().getString(R.string.excuse_messages_kor_defaultmsg_4);				
			}
			else
			//<!--//20100929 sumi920.kim@lge.com change default excusemessage list [END_LGE_LAB1] 
			{
				excuse_1	=	this.getResources().getString(R.string.excuse_messages_defaultmsg_0);
				excuse_2	=	this.getResources().getString(R.string.excuse_messages_defaultmsg_1);
				excuse_3	=	this.getResources().getString(R.string.excuse_messages_defaultmsg_2);
				excuse_4	=	this.getResources().getString(R.string.excuse_messages_defaultmsg_3);
				excuse_5	=	this.getResources().getString(R.string.excuse_messages_defaultmsg_4);	
			}
			String exuse[]	=	{excuse_1 , excuse_2 , excuse_3 , excuse_4, excuse_5 };
			
			for(int i=1;i<6;i++){
				String selection = "_id="+i;
				c	=	myDatabase.query(DATABASE_TABLE, modifiedColumn, selection, null, null, null, null);
				
				this.mActivity.startManagingCursor(c);
				c.moveToFirst();
				
				if (c.getCount()>0){
					messageVal	=	c.getString(0);
					modifiedVal	=	c.getInt(1);
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
			c.close();
			
		}catch (Exception e){
		}
	}

	void updateState(int status) {
		// TODO Auto-generated method stub
		this.setClickable(true);
        this.setFocusableInTouchMode(true);
        
		if (DBG) Log.i(TAG,"updateState call status : " + status);
		
		if (mCallUi.getUserNumber() == null)
		{
			if (DBG) Log.i(TAG,"getUserNumber() is null returned");
			//status = ST_HIDE;
		}
		
		switch (status)
		{
			case ST_HIDE:
			{
				this.flickBtn.setBackgroundResource(R.drawable.sms_handle_up);
				this.setVisibility(GONE);
				cuHeight = this.displayHeight - statusHeight;
				this.setHeight(cuHeight);
			}
			break;
			case ST_LOCK_VIEW:
			{
				this.flickBtn.setBackgroundResource(R.drawable.sms_handle_up);
				this.setVisibility(VISIBLE);
				cuHeight = this.displayHeight - statusHeight;
				this.setHeight(cuHeight);
			}
			break;
			
			case ST_UNLOCK_VIEW:
			{
				Log.v(TAG,"2222222222222222");
				this.flickBtn.setBackgroundResource(R.drawable.sms_handle_down);
				mCallUi.handleSilentCall();
				this.setVisibility(VISIBLE);
				cuHeight = topPos;
				this.setHeight(cuHeight);
			}
			break;
		}
	}

	public void setMode(int modeLock) {
		// TODO Auto-generated method stub
		cuMode = modeLock;
	}
	
	public int getMode()
	{
		return cuMode;
	}

  public void closeDataBase()
  {
    myDatabase.close();
  }
}