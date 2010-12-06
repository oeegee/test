package com.mtelo.visualexpression;

import android.util.Log;


public class MyLog 
{
	private static int mLogCount = 0;
	private static boolean mEnableLog;
	
	private static String mFilter = "";
	
	public static void set_LogEnable(boolean b, String filter)
	{
		mEnableLog = b;
		mFilter = filter;
	}
	
	public static void write(int priority, String tag, String msg)
	{
		if (mEnableLog)
		{
			Log.println(priority, tag, ++mLogCount + " : " + mFilter + " | " + msg);
		}
		else
		{
			if ( priority == Log.ERROR )
				Log.println(priority, tag, ++mLogCount + " : " + mFilter + " | " + msg);
		}
	}
}
