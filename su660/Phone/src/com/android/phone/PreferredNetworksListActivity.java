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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.app.ListActivity;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.widget.Toast;



/**
 * "Preferred Networks" settings UI for the Phone app.
 */
public class PreferredNetworksListActivity extends ListActivity {

    private static final String LOG_TAG = "PreferredNetworksActivity";
    private static final boolean DBG = false;
    private static final String PREFERRED_NETWORKS_URI = "content://preferred-networks/raw";
    private static final String OPERATOR_NAMES_URI = "content://preferred-networks/names";
    private static final Uri CONTENT_URI = Uri.parse(PREFERRED_NETWORKS_URI);
    private static final Uri CONTENT_NAMES_URI = Uri.parse(OPERATOR_NAMES_URI);
    private static final String LONGALPHA_TAG = "alphalong";
    private static final String SHORTALPHA_TAG = "alphashort";
    private static final String NUMERIC_TAG = "numeric";
    private static final String ACCESS_TECHNOLOGIES_LIST_TAG = "access_technologies_list";
    private static final String ACCESS_TECHNOLOGIES_MASK_TAG = "access_technologies_mask";
    private static final int PREFERRED_NETWORK_EDIT = 1;
    private static final int PREFERRED_NETWORK_DELETE = 2;
    private static final int PREFERRED_NETWORK_EDIT_BY_ID = 3;
    private static final int PREFERRED_NETWORK_INFO = 4;
    
    static final int DIALOG_CHOOSE_RAT = 0;
    
    public int mPos;
    
    /**/
    private static final int  PICK_PREFERRED_NETWORK_SUBACTIVITY = 1;
    private static final int PICK_PREFERRED_NETWORK_EDIT_SUBACTIVITY = 2;


    private class PreferredNetworksListAdapter extends CursorAdapter {
        private LayoutInflater mInflater;
        private int mAlphalongId;
        private int mNumericId;
        private int mXactId;
        private String mEmptyName;
    	
		public PreferredNetworksListAdapter(Context context, Cursor c) {
			super(context, c);
			mAlphalongId = c.getColumnIndexOrThrow(LONGALPHA_TAG);
			mNumericId = c.getColumnIndexOrThrow(NUMERIC_TAG);
			mXactId = c.getColumnIndexOrThrow(ACCESS_TECHNOLOGIES_LIST_TAG);
			mEmptyName = context.getString(R.string.preferred_networks_list_item_empty);
	        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

	    public PreferredNetworksListAdapter(Context context, Cursor c, boolean autoRequery) {
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
				alphalong = mEmptyName;
				numeric = "";
				xact = "";
			} else {
				numeric = cursor.getString(mNumericId);
				xact = cursor.getString(mXactId);
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

	private Cursor mPreferredNetworksListCursor;
    private PreferredNetworksListObserver mObserver;
    private PreferredNetworksListHandler mHandler;

    

    //implemented for DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        // request that the service stop the query with this callback object.
        finish();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPreferredNetworksListCursor = null;
        // ListView listView = getListView();
        // listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        registerForContextMenu(getListView());
        mHandler = new PreferredNetworksListHandler();
        if (mHandler != null) {
           mObserver = new PreferredNetworksListObserver(mHandler);
        }
    }

    /**
     * Override 
     */
    @Override
    protected void onDestroy() {
    	unregisterForContextMenu(getListView());
    	if (mPreferredNetworksListCursor != null) {
    		mPreferredNetworksListCursor.close();
    		mPreferredNetworksListCursor = null;
    		setListAdapter(null);
    	}
        super.onDestroy();
        mObserver = null;
        mHandler = null;
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (mPreferredNetworksListCursor == null) {
            mPreferredNetworksListCursor = managedQuery(CONTENT_URI, null, null, null);
            if (mPreferredNetworksListCursor != null) {
                setListAdapter(new PreferredNetworksListAdapter(this,mPreferredNetworksListCursor));
            } else {
    	    	Toast.makeText(getApplicationContext(), getString(R.string.preferred_networks_read_failure), Toast.LENGTH_LONG).show();    			
            }
    	} else {
	    	mPreferredNetworksListCursor.requery();
	    	CursorAdapter adapter = (CursorAdapter)(getListView().getAdapter());
	    	adapter.notifyDataSetChanged();
    	}
    	/* start observing list change */
    	if (mObserver != null) {
           getContentResolver().registerContentObserver(CONTENT_URI,
                   true, mObserver);
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if (mObserver != null) {
    	   getContentResolver().unregisterContentObserver(mObserver);
    	}
    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
    	
   		super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(0, PREFERRED_NETWORK_EDIT, 0, R.string.menu_preferred_network_edit);
    	menu.add(0, PREFERRED_NETWORK_DELETE, 0, R.string.menu_preferred_network_delete);    	
    }
   
    @Override
    public boolean onContextItemSelected(MenuItem item){
    	 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
         
    	switch(item.getItemId()) {
    	case PREFERRED_NETWORK_EDIT:
    		handleEditPreferredNetwork();
            return true;
    	case PREFERRED_NETWORK_DELETE:
    		handleDeletePreferredNetwork();
    		return true;    	
    	}
    	return super.onContextItemSelected(item);
    }
    

    /**
     * W
     * 
     */
    private void handleEditPreferredNetwork(){
       	Intent intent;
    	int pos;
    	if (mPreferredNetworksListCursor != null) {
    		pos = mPreferredNetworksListCursor.getPosition();
    	    if (pos >= 0 && pos < mPreferredNetworksListCursor.getCount()) {
      	        intent = new Intent(this,PreferredNetworkEditActivity.class);
      	        intent.putExtra("entry", PREFERRED_NETWORKS_URI+"/"+String.valueOf(pos));
      	        String longname = mPreferredNetworksListCursor.getString(mPreferredNetworksListCursor.getColumnIndexOrThrow(LONGALPHA_TAG));
      	        String numeric = mPreferredNetworksListCursor.getString(mPreferredNetworksListCursor.getColumnIndexOrThrow(NUMERIC_TAG));
// LGE_PREFERRED_NETWORKS_FEATURE START
      	        int rat = mPreferredNetworksListCursor.getInt(mPreferredNetworksListCursor.getColumnIndexOrThrow(ACCESS_TECHNOLOGIES_MASK_TAG));
// LGE_PREFERRED_NETWORKS_FEATURE END
      	        if (longname.equals("") || longname == null) {
      	        	longname = "";
      	        	numeric = "";      	        	
      	        } 
      	        intent.putExtra(LONGALPHA_TAG,longname);
	        	intent.putExtra(NUMERIC_TAG, numeric);
// LGE_PREFERRED_NETWORKS_FEATURE START
	        	intent.putExtra(ACCESS_TECHNOLOGIES_MASK_TAG, rat);
// LGE_PREFERRED_NETWORKS_FEATURE END
    	        startActivityForResult(intent,PICK_PREFERRED_NETWORK_SUBACTIVITY);
    	    }
    	}
    }
    
    @Override
    public void onActivityResult(int requestCode,
    		                     int resultCode,
    		                     Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch(requestCode) {
    	case PICK_PREFERRED_NETWORK_SUBACTIVITY:
    	    if (resultCode == Activity.RESULT_OK) {
                String entry = data.getStringExtra("entry");
    	    	String longname = data.getStringExtra(LONGALPHA_TAG);
	    	    String numeric = data.getStringExtra(NUMERIC_TAG);
	    	    int rat = data.getIntExtra(ACCESS_TECHNOLOGIES_MASK_TAG, 0);
   	    	    if (entry != null && 
   	    	    		(longname != null || numeric != null || rat != 0)) {
   	    		    /* update network */
   		            ContentValues newValues = new ContentValues();
		            if (!numeric.equals("")) {
   		                newValues.put(NUMERIC_TAG, numeric);
   		            } else if (!longname.equals("")) {
   		                newValues.put(LONGALPHA_TAG, longname);
   		            } 
		            if (rat != 0) {
		            	newValues.put(ACCESS_TECHNOLOGIES_MASK_TAG, rat);
		            }
   		            Uri updateUri = Uri.parse(entry);
   		            int updated = getContentResolver().update(updateUri,newValues,null,null);
   		            /* exactly one row should be updated */
       		        if (updated == 1) {
       	    	        Toast.makeText(getApplicationContext(), getString(R.string.preferred_network_update_success), Toast.LENGTH_SHORT).show();
       	    	        mPreferredNetworksListCursor.requery();
       	    	        CursorAdapter adapter = (CursorAdapter)(getListView().getAdapter());
       	    	        adapter.notifyDataSetChanged();
       		        } else {
       	    	        Toast.makeText(getApplicationContext(), getString(R.string.preferred_network_update_failure), Toast.LENGTH_LONG).show();    			
       		        }    		            
    	    	}
    	    }
    	    break;  
    	
    	case PICK_PREFERRED_NETWORK_EDIT_SUBACTIVITY:{
    		if (resultCode == Activity.RESULT_OK) {
                String entry = data.getStringExtra("entry");
    	    	String longname = data.getStringExtra(LONGALPHA_TAG);
	    	    String numeric = data.getStringExtra(NUMERIC_TAG);
	    	    int rat = data.getIntExtra(ACCESS_TECHNOLOGIES_MASK_TAG, 0);
	    	    if (entry != null && (longname != null || numeric != null || rat != 0)) {
   	    		    /* update network */
   		            ContentValues newValues = new ContentValues();
   		            if (longname != "") {
   		                newValues.put(LONGALPHA_TAG, longname);
   		            } else if (numeric != "") {
   		                newValues.put(NUMERIC_TAG, numeric);
   		            }
   		            if (rat != 0) {
		            	newValues.put(ACCESS_TECHNOLOGIES_MASK_TAG, rat);
		            }
   		            Uri updateUri = Uri.parse(entry);
   		            int updated = getContentResolver().update(updateUri,newValues,null,null);
   		            /* exactly one row should be updated */
       		        if (updated == 1) {
       	    	        Toast.makeText(getApplicationContext(), getString(R.string.preferred_network_update_success), Toast.LENGTH_SHORT).show();
       	    	        mPreferredNetworksListCursor.requery();
       	    	        CursorAdapter adapter = (CursorAdapter)(getListView().getAdapter());
       	    	        adapter.notifyDataSetChanged();
       		        } else {
       	    	        Toast.makeText(getApplicationContext(), getString(R.string.preferred_network_update_failure), Toast.LENGTH_LONG).show();    			
       		        }    		            
    	    	}
    		}
    		break;
    	}
    	}
    }
    
    
    private void handleDeletePreferredNetwork() {
    	int pos;
    	int id,deleted;
    	Uri deleteUri;
    	
    	if (mPreferredNetworksListCursor != null) {
    		pos = mPreferredNetworksListCursor.getPosition();
    	    if (pos >= 0 && pos < mPreferredNetworksListCursor.getCount()) {
    		    /* valid position */
    	    	if (!mPreferredNetworksListCursor.getString(mPreferredNetworksListCursor.getColumnIndexOrThrow(LONGALPHA_TAG)).equals("")) {
    	    	    /* update preferred network entry to empty strings */
    		        ContentValues newValues = new ContentValues();
    		        newValues.put(LONGALPHA_TAG, "");
    		        newValues.put(SHORTALPHA_TAG, "");
    		        newValues.put(NUMERIC_TAG, "");
    		        deleteUri = Uri.parse(PREFERRED_NETWORKS_URI+"/"+String.valueOf(pos));
    		        deleted = getContentResolver().update(deleteUri,newValues,null,null);
    		        /* exactly one row should be updated */
    		        if (deleted == 1) {
    	    	        Toast.makeText(getApplicationContext(), getString(R.string.preferred_network_delete_success), Toast.LENGTH_SHORT).show();
    	    	        mPreferredNetworksListCursor.requery();
    	    	        CursorAdapter adapter = (CursorAdapter)(getListView().getAdapter());
    	    	        adapter.notifyDataSetChanged();
    		        } else {
    	    	        Toast.makeText(getApplicationContext(), getString(R.string.preferred_network_delete_failure), Toast.LENGTH_LONG).show();    			
    		        }
    	    	}
    	    }
    	}
    }

    

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
    	super.onListItemClick(l, v, position, id);
    	handleEditPreferredNetwork();
    	//getListView().getItemAtPosition(position);
    }

    /**
     * Redraws list if preferred networks list is changed outside of activity
     */
    private void onPreferredNetworksListChanged() {
    	if (mPreferredNetworksListCursor != null) {
    	   mPreferredNetworksListCursor.requery();
    	   CursorAdapter adapter = (CursorAdapter)(getListView().getAdapter());
    	   adapter.notifyDataSetChanged();
    	} else {
            mPreferredNetworksListCursor = managedQuery(CONTENT_URI, null, null, null);
            if (mPreferredNetworksListCursor != null) {
                setListAdapter(new PreferredNetworksListAdapter(this,mPreferredNetworksListCursor));
            }
    	}
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[NetworksList] " + msg);
    }
    
    /**
     * Observer class for preferred networks list change detection
     *
     */
    private class PreferredNetworksListObserver extends ContentObserver {
        public PreferredNetworksListObserver(Handler handler) {
            super(handler);
        }
        public void onChange(boolean selfChange) {
            onPreferredNetworksListChanged();
        }
    }
    
    private class PreferredNetworksListHandler extends Handler {

        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            default:
            	log("uknown message " + msg.what + ".");
                break;
            }
        }
    };

}
// LGE_PREFERRED_NETWORKS_FEATURE END
