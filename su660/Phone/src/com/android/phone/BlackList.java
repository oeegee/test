// LGE_AUTO_REDIAL START
/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class BlackList {
	private static ArrayList<String> list = new ArrayList<String>();
	private static final String LOG_TAG = "BlackList";
	final int MAX_LIST = 8;
	final static long ONE_DAY = 60*60*24*1000;
	private static Timer timer;
	private static TimerTask task;
	private static boolean timerStarted = false;


	public BlackList() {

	}

	public static void add(String entry){
		if (list.contains(entry) == false){
		     list.add(entry);
		}
		Log.d(LOG_TAG,"Add entry" );
		startTimer();
	}
// LGE_CR2082 START
	public static boolean notContain(String entry){
		if (list.contains(entry) == false){
			Log.d(LOG_TAG,"Number NOT in blacklist" );
		    return true;
		}
		else{
			Log.d(LOG_TAG,"Number already in blacklist" );
		    return false;
			}
	}
// LGE_CR2082 END

	public static boolean remove(int location) {
		try {
			list.remove(location);
			Log.d(LOG_TAG,"remove entry" );
            return true;
		}   catch (Exception ex) {
            return false;
        }
	}
    public int size(){
    	return list.size();
    }
    public boolean freePlace(){
    	return (list.size()<MAX_LIST)?true:false;
    }
    public static  String[] values(){
    	String[] a = new String[list.size()];
    	list.toArray(a);
    	return a;
    }
    public String entry(int n){
    	return list.get(n);
    }

    public static void startTimer(){
		if (timerStarted) {
	 		task.cancel();
			timer.cancel();
			timerStarted = false;
		}
		Log.w(LOG_TAG,"Start timer" );
		timer = new Timer();
		task = new TimerTask(){
			public void run(){
				Log.w(LOG_TAG,"Clear BlackList" );
				list.clear();
			}
		};
		timerStarted = true;
		timer.schedule(task, ONE_DAY);
    }
}
// LGE_AUTO_REDIAL END
