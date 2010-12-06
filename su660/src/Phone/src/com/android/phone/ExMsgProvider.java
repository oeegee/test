package com.android.phone;
//20100804 jongwany.lee@lge.com attached this file CALL UI

import java.util.HashMap;

import com.android.phone.ExMsgProviderMetaData.ExMsgTableMetaData;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
//20101002 hangyul.park@lge.com Modify Excuse Msg dor Korean Region [END_LGE_LAB1]
import com.lge.config.StarConfig;
//20101002 hangyul.park@lge.com Modify Excuse Msg dor Korean Region [END_LGE_LAB1]
public class ExMsgProvider extends ContentProvider {
	private static final String TAG = "ExMsgProvider";


		private static Integer[] mResourceIdsKor = {
		R.string.excuse_messages_kor_defaultmsg_0,
		R.string.excuse_messages_kor_defaultmsg_1,
		R.string.excuse_messages_kor_defaultmsg_2,
		R.string.excuse_messages_kor_defaultmsg_3,
		R.string.excuse_messages_kor_defaultmsg_4		
		};	


		private static Integer[] mResourceIds = {
		R.string.excuse_messages_defaultmsg_0,
		R.string.excuse_messages_defaultmsg_1,
		R.string.excuse_messages_defaultmsg_2,
		R.string.excuse_messages_defaultmsg_3,
		R.string.excuse_messages_defaultmsg_4		
		};	

	
	private static HashMap<String, String> sExcuseMsgProjectionMap;
	static{
		sExcuseMsgProjectionMap = new HashMap<String, String>();
		sExcuseMsgProjectionMap.put(ExMsgTableMetaData._ID, ExMsgTableMetaData._ID);
		sExcuseMsgProjectionMap.put(ExMsgTableMetaData.KEY_MSG_CONTENT, ExMsgTableMetaData.KEY_MSG_CONTENT);
		sExcuseMsgProjectionMap.put(ExMsgTableMetaData.KEY_IS_DEFAULT, ExMsgTableMetaData.KEY_IS_DEFAULT);
//		sExcuseMsgProjectionMap.put(ExMsgTableMetaData.KEY_CHECKED, ExMsgTableMetaData.KEY_CHECKED);

	}
	private static final UriMatcher sUriMatcher;
	private static final int INCOMING_EXMSG_COLLECTION_URI_INDICATOR =1;
	private static final int INCOMING_SINGLE_EXMSG_URI_INDICATOR = 2;
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(ExMsgProviderMetaData.AUTHORITY, 
							"excusemsgs", INCOMING_EXMSG_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(ExMsgProviderMetaData.AUTHORITY, 
							"excusemsgs/#", INCOMING_SINGLE_EXMSG_URI_INDICATOR);
		
	}
	
	private static DatabaseHelper mOpenHelper;
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch(sUriMatcher.match(uri)){
		case INCOMING_EXMSG_COLLECTION_URI_INDICATOR:
			count = db.delete(ExMsgTableMetaData.TABLE_NAME, where, whereArgs);
			break;
			
		case INCOMING_SINGLE_EXMSG_URI_INDICATOR:
			String rowId = uri.getPathSegments().get(1);
			count = db.delete(ExMsgTableMetaData.TABLE_NAME, ExMsgTableMetaData._ID+"="+rowId
					+(!TextUtils.isEmpty(where)? " AND ("+where+')':""), whereArgs);
			break;
		
		default:
			throw new IllegalArgumentException("Unknown URI "+uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)){
		case INCOMING_EXMSG_COLLECTION_URI_INDICATOR:
			return ExMsgTableMetaData.CONTENT_TYPE;
			
		case INCOMING_SINGLE_EXMSG_URI_INDICATOR:
			return ExMsgTableMetaData.CONTENT_ITEM_TYPE;
				
		default:
			throw new IllegalArgumentException("UnKnown URI" + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if(sUriMatcher.match(uri) != INCOMING_EXMSG_COLLECTION_URI_INDICATOR){
			throw new IllegalArgumentException("Unknown URI "+uri);
		}
		
		if(values.containsKey(ExMsgTableMetaData.KEY_MSG_CONTENT) == false){
			throw new SQLException("Failed to insert row because Msg is needed"+uri);
		}
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(ExMsgTableMetaData.TABLE_NAME, ExMsgTableMetaData.KEY_MSG_CONTENT, values);
		if(rowId>0){
			Uri insertedExMsgUri = ContentUris.withAppendedId(ExMsgTableMetaData.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(insertedExMsgUri, null);
			return insertedExMsgUri;
		}
		
		throw new SQLException("Failed to insert row into "+uri);
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (sUriMatcher.match(uri)){
		case INCOMING_EXMSG_COLLECTION_URI_INDICATOR:
			qb.setTables(ExMsgTableMetaData.TABLE_NAME);
			qb.setProjectionMap(sExcuseMsgProjectionMap);
			break;
		
		case INCOMING_SINGLE_EXMSG_URI_INDICATOR:
			qb.setTables(ExMsgTableMetaData.TABLE_NAME);
			qb.setProjectionMap(sExcuseMsgProjectionMap);
			qb.appendWhere(ExMsgTableMetaData._ID + "="+uri.getPathSegments().get(1));
			break;
			
			default:
				throw new IllegalArgumentException("Unknown URI "+uri);
		}
		
		String orderBy;
		
		if(TextUtils.isEmpty(sortOrder)){
			orderBy = ExMsgTableMetaData.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}
		
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		
//		int i = c.getCount();
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)){
		case INCOMING_EXMSG_COLLECTION_URI_INDICATOR:
			count = db.update(ExMsgTableMetaData.TABLE_NAME, values, where, whereArgs);
			
			break;
		case INCOMING_SINGLE_EXMSG_URI_INDICATOR:
			String rowId = uri.getPathSegments().get(1);
			count = db.update(ExMsgTableMetaData.TABLE_NAME, values, ExMsgTableMetaData._ID+"="+rowId
					+(!TextUtils.isEmpty(where)?" AND ("+where+')' : ""), whereArgs);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI"+uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	public static void deleteAllItems() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		mOpenHelper.dropAndRecreateTable(db);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper{

		public DatabaseHelper(Context context) {
			super(context, ExMsgProviderMetaData.DATABASE_NAME, null, ExMsgProviderMetaData.DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "+ExMsgTableMetaData.TABLE_NAME + " ("
					+ExMsgProviderMetaData.ExMsgTableMetaData._ID
					+" INTEGER PRIMARY KEY, "
					+ExMsgTableMetaData.KEY_MSG_CONTENT + " TEXT, "		
//					+ExMsgTableMetaData.KEY_CHECKED + " TEXT, "
					+ExMsgTableMetaData.KEY_IS_DEFAULT + " INTEGER"					
					+");"
					);
			//20101002 hangyul.park@lge.com Modify Excuse Msg dor Korean Region [LGE_LAB1]
			if ( StarConfig.COUNTRY.equals("KR") )
			{
				for (int i = 0; i < 5; i++) {
				db.execSQL("insert into " + ExMsgTableMetaData.TABLE_NAME
						+ " (msg, defaultmsg) values (" 
						+ mResourceIdsKor[i] + ", "
						+ ExMsgTableMetaData.DEFAULT_ITEM						
						+");");
			}
			}
			else
			{
				for (int i = 0; i < 5; i++) {
				db.execSQL("insert into " + ExMsgTableMetaData.TABLE_NAME
						+ " (msg, defaultmsg) values (" 
						+ mResourceIds[i] + ", "
//						+ ExMsgTableMetaData.ITEM_IS_UNCHECKED
//						+"', "
						+ ExMsgTableMetaData.DEFAULT_ITEM						
						+");");
			}
			}
			
			
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
		public void dropAndRecreateTable(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS " + ExMsgProviderMetaData.EXMSG_TABLE_NAME);
			onReCreate(db);
		}
		
		public void onReCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "+ExMsgTableMetaData.TABLE_NAME + " ("
					+ExMsgProviderMetaData.ExMsgTableMetaData._ID
					+" INTEGER PRIMARY KEY, "
					+ExMsgTableMetaData.KEY_MSG_CONTENT + " TEXT,"
//					+ExMsgTableMetaData.KEY_CHECKED + " TEXT,"
					+ExMsgTableMetaData.KEY_IS_DEFAULT + " INTEGER"					
					+");"
					);
		}		
	}

}
