// LGE_PREFERRED_NETWORKS_FEATURE START
/*
 * Copyright (C) 2006 The Android Open Source Project
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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.NetworkInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.app.ListActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.widget.Toast;


import java.util.HashMap;
import java.util.List;

/**
 * "Preferred Networks" settings UI for the Phone app.
 */
public class PreferredNetworksListEditActivity extends ListActivity {

    private static final String LOG_TAG = "PreferredNetworksListEditActivity";
    private static final boolean DBG = false;
    private static final String OPERATOR_NAMES_URI = "content://preferred-networks/names";
    private static final String PREFERRED_NETWORKS_URI = "content://preferred-networks/raw";
    private static final Uri CONTENT_URI = Uri.parse(OPERATOR_NAMES_URI);
    private static final String LONGALPHA_TAG = "alphalong";
    private static final String NUMERIC_TAG = "numeric";
    private static final String ACCESS_TECHNOLOGIES_LIST_TAG = "access_technologies_list";
    
    static final int DIALOG_CHOOSE_RAT = 0;
    private static final int PREFERRED_NETWORK_INFO = 1;
    private static final int PREFERRED_NETWORK_EDIT_BY_ID = 2;
    
    private static final int PICK_PREFERRED_NETWORK_EDIT_SUBACTIVITY = 3;

    public int mPos;
    public String rat;
    
    private class OperatorNamesListAdapter extends CursorAdapter {
        private LayoutInflater mInflater;
        private int mAlphalongId;
        private int mNumericId;
        private Cursor mPreferredNetworksListCursor;        
    	
		public OperatorNamesListAdapter(Context context, Cursor c) {
			super(context, c);
			mAlphalongId = c.getColumnIndexOrThrow(LONGALPHA_TAG);
			mNumericId = c.getColumnIndexOrThrow(NUMERIC_TAG);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

	    public OperatorNamesListAdapter(Context context, Cursor c, boolean autoRequery) {
	        super(context, c, autoRequery);
	        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }
		
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView alphalongview,numericview,xactview;
			String alphalong,numeric,xact;
            alphalongview = (TextView) view.findViewById(R.id.alphalong);
            numericview = (TextView) view.findViewById(R.id.numeric);
            xactview = (TextView) view.findViewById(R.id.xact);
			alphalong = cursor.getString(mAlphalongId);
			if (alphalong == null ||
				alphalong.equals("")) {
				alphalong = "";
				numeric = "";
				xact = "";
			} else {
				numeric = cursor.getString(mNumericId);
				xact = "";
			}
			alphalongview.setText(alphalong);
			numericview.setText(numeric);
			xactview.setText(xact);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
	        View view;
	        view = mInflater.inflate(R.layout.preferred_networks_list_item, parent, false);
	        return view;
		}    	
    }

   	private Cursor mOperatorNamesListCursor;
   	//private Cursor mPreferredNetworksListCursor;
    //private PreferredNetworksListObserver mObserver;
    //private PreferredNetworksListHandler mHandler;//
    

    //implemented for DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        // request that the service stop the query with this callback object.
        finish();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mOperatorNamesListCursor = null;
        registerForContextMenu(getListView());
    }

    /**
     * Override onDestroy() to unbind the query service, avoiding service
     * leak exceptions.
     */
    @Override
    protected void onDestroy() {
    	unregisterForContextMenu(getListView());
        if (mOperatorNamesListCursor != null) {
     	   setListAdapter(null);
           mOperatorNamesListCursor.close();
           mOperatorNamesListCursor = null;
        }
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (mOperatorNamesListCursor == null) {
            mOperatorNamesListCursor = managedQuery(CONTENT_URI, null, null, null);
            if (mOperatorNamesListCursor != null) {
                setListAdapter(new OperatorNamesListAdapter(this,mOperatorNamesListCursor));
            } else {
    	    	Toast.makeText(getApplicationContext(), getString(R.string.operator_names_read_failure), Toast.LENGTH_LONG).show();    			
            }
    	} else {
    		mOperatorNamesListCursor.requery();
	    	CursorAdapter adapter = (CursorAdapter)(getListView().getAdapter());
	    	adapter.notifyDataSetChanged();
    	}
    }
       
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
    	super.onListItemClick(l, v, position, id);
    	if (mOperatorNamesListCursor != null) {
    		Intent startIntent = getIntent();
    		if (startIntent == null) {
    			return;
    		}
    		String entry = startIntent.getStringExtra("entry");
    		if (entry == null) {
    			return;
    		}
    		int pos = mOperatorNamesListCursor.getPosition();
    		if (pos >= 0 && pos < mOperatorNamesListCursor.getCount()) {
    			String longname = mOperatorNamesListCursor.getString(mOperatorNamesListCursor.getColumnIndexOrThrow(LONGALPHA_TAG));
    			String numeric = mOperatorNamesListCursor.getString(mOperatorNamesListCursor.getColumnIndexOrThrow(NUMERIC_TAG));
    			Intent result = new Intent(null,Uri.parse(CONTENT_URI+"/"+String.valueOf(pos)));
                result.putExtra("entry", entry);
    			result.putExtra(LONGALPHA_TAG,longname);
    			result.putExtra(NUMERIC_TAG,numeric);
    			setResult(RESULT_OK,result);
    			finish();
    		}
    	}
    	//getListView().getItemAtPosition(position);
    }

    

    private void log(String msg) {
        Log.d(LOG_TAG, "[NetworksList] " + msg);
    }
}
// LGE_PREFERRED_NETWORKS_FEATURE END
