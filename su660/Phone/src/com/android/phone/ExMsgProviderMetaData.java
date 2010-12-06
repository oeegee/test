package com.android.phone;
//20100804 jongwany.lee@lge.com attached this file CALL UI

import android.net.Uri;
import android.provider.BaseColumns;

public class ExMsgProviderMetaData{
	public static final String AUTHORITY = "com.lge.provider.callsettings";
	public static final String DATABASE_NAME = "excusemsg.db";
	public static final int DATABASE_VERSION =1;
	public static final String EXMSG_TABLE_NAME = "excusemsgs";
	
	private ExMsgProviderMetaData(){}
	
	public static final class ExMsgTableMetaData implements BaseColumns {
		private ExMsgTableMetaData(){}
		public static final String TABLE_NAME = EXMSG_TABLE_NAME;
		
		public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/excusemsgs");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.androidexcusemsg.excusemsg";

		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.androidexcusemsg.excusemsg";

		public static final String DEFAULT_SORT_ORDER = "_id";

		/* Column names for the table */
		public static final String KEY_MSG_CONTENT = "msg";
		public static final String KEY_IS_DEFAULT = "defaultmsg";
//		public static final String KEY_CHECKED = "checkBox";
		
		/* Column index for the table */
		public static final int COLUMN_MSG_CONTENT = 1;	
//		public static final int COLUMN_IS_CHECKED = 2;
		public static final int COLUMN_IS_DEFAULT = 2;
		
		
		
		
		public static final int DEFAULT_ITEM = 1;
		public static final int NON_DEFAULT_ITEM = 2;
		
//		public static final String ITEM_IS_UNCHECKED = "false";
//		public static final String ITEM_IS_CHECKED = "true";
		
//		public static final String ID = "id";
	}
}