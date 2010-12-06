// LGE_PREFERRED_NETWORKS_FEATURE START

/* Copyright (C) 2006 The Android Open Source Project
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
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Toast;
import android.os.Bundle;
import android.widget.Button;
import com.android.internal.telephony.gsm.PreferredNetworkInfo;
// LGE_PREFERRED_NETWORKS_FEATURE START
import android.util.Log;
// LGE_PREFERRED_NETWORKS_FEATURE END

/**
 * "Preferred Networks" settings UI for the Phone app.
 */
public class PreferredNetworkEditActivity extends Activity {
// LGE_PREFERRED_NETWORKS_FEATURE START	
	private static final String LOG_TAG = "PreferredNetworkEditActivity";  
// LGE_PREFERRED_NETWORKS_FEATURE END
    private static final String LONGALPHA_TAG = "alphalong";    
    private static final String NUMERIC_TAG = "numeric";
    private static final String ACCESS_TECHNOLOGIES_LIST_TAG = "access_technologies_list";
    private static final String ACCESS_TECHNOLOGIES_MASK_TAG = "access_technologies_mask";
    private static final int  PICK_PREFERRED_NETWORK_SUBACTIVITY = 1;
	private EditText mNumericText;
	private TextView mLongAlpha;
	private int rat;
// LGE_PREFERRED_NETWORKS_FEATURE START
	private boolean umtsChecked;
	private boolean gsmChecked;
// LGE_PREFERRED_NETWORKS_FEATURE END
	
	
	protected void onCreate(Bundle icicle){
		super.onCreate(icicle);	
// LGE_PREFERRED_NETWORKS_FEATURE START
		umtsChecked = true;
		gsmChecked = true;
// LGE_PREFERRED_NETWORKS_FEATURE END
		setContentView(R.layout.preferred_network_item_edit);
		setTitle("Edit Preferred Networks List");
		mNumericText = (EditText) findViewById(R.id.network);
		mLongAlpha = (TextView) findViewById(R.id.alphalong);
		final CheckBox umts_box = (CheckBox) findViewById(R.id.rat_umts);
		final CheckBox gsm_box = (CheckBox) findViewById(R.id.rat_gsm);
		Intent startIntent = getIntent();
		mLongAlpha.setText(startIntent.getStringExtra(LONGALPHA_TAG));
		mNumericText.setText(startIntent.getStringExtra(NUMERIC_TAG));
// LGE_PREFERRED_NETWORKS_FEATURE START
		if (startIntent.getIntExtra(ACCESS_TECHNOLOGIES_MASK_TAG, PreferredNetworkInfo.RAT_ANY) 
				== PreferredNetworkInfo.RAT_UMTS) {
			gsm_box.setChecked(false);
			gsmChecked = false;
		}
		if (startIntent.getIntExtra(ACCESS_TECHNOLOGIES_MASK_TAG, PreferredNetworkInfo.RAT_ANY) 
				== PreferredNetworkInfo.RAT_GSM) {
			umts_box.setChecked(false);
			umtsChecked = false;
		}
// LGE_PREFERRED_NETWORKS_FEATURE END
		final Button confirmButton = (Button) findViewById(R.id.confirm);		
		Button cancelButton = (Button) findViewById(R.id.cancel);
		Button browseButton = (Button) findViewById(R.id.browse);
// LGE_PREFERRED_NETWORKS_FEATURE START		
		umts_box.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				Log.d(LOG_TAG, "umts_box click");
				if (umts_box.isChecked()) umtsChecked = true;
				else umtsChecked = false;
				if (!umtsChecked && !gsmChecked) confirmButton.setEnabled(false);
				else confirmButton.setEnabled(true);
			}
		});
		
		gsm_box.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				Log.d(LOG_TAG, "gsm_box click");
				if (gsm_box.isChecked()) gsmChecked = true;
				else gsmChecked = false;
				if (!umtsChecked && !gsmChecked) confirmButton.setEnabled(false);
				else confirmButton.setEnabled(true);
			}
		});
// LGE_PREFERRED_NETWORKS_FEATURE END				
		confirmButton.setOnClickListener(new View.OnClickListener() {

        	public void onClick(View view) {
        		        		
        		Intent startIntent = getIntent();
        		if (startIntent == null) {
        			return;
        		}
        		String entry = startIntent.getStringExtra("entry");
        		if (entry == null) {
        			return;
        		}
        		String longalpha = mLongAlpha.getText().toString();
        		String numeric = mNumericText.getText().toString();
        		Intent result = new Intent(null,Uri.parse(entry));
                result.putExtra("entry", entry);
    			result.putExtra(NUMERIC_TAG,numeric);
    			if (longalpha != null) {
    				result.putExtra(LONGALPHA_TAG, longalpha);
    			} else {
    				result.putExtra(LONGALPHA_TAG, "");
    			}
// LGE_PREFERRED_NETWORKS_FEATURE START    			
    			if (umtsChecked){
    				rat = PreferredNetworkInfo.RAT_UMTS;
    			} else 
    				if (gsmChecked){
    					rat = PreferredNetworkInfo.RAT_GSM;
    				} 
    			if (umtsChecked && gsmChecked)
// LGE_PREFERRED_NETWORKS_FEATURE END
    				rat = PreferredNetworkInfo.RAT_ANY;
    			result.putExtra(ACCESS_TECHNOLOGIES_MASK_TAG, rat);
    			setResult(RESULT_OK,result);
        		finish();
        	}
	});
		cancelButton.setOnClickListener(new View.OnClickListener(){
			public void onClick(View view){
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		browseButton.setOnClickListener(new View.OnClickListener() {

        	public void onClick(View view) {
        		handleBrowseList();     		
            	    }
	});
		
	}
	private void handleBrowseList(){

		Intent startIntent = getIntent();
		if (startIntent == null) {
			return;
		}
		String entry = startIntent.getStringExtra("entry");
		if (entry == null) {
			return;
		}
		Intent intent = new Intent(this, PreferredNetworksListEditActivity.class);
		intent.putExtra("entry", entry);		
      	startActivityForResult(intent,PICK_PREFERRED_NETWORK_SUBACTIVITY);
	}
	
		
	@Override
    public void onActivityResult(int requestCode,
    		                     int resultCode,
    		                     Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch(requestCode) {
    	case PICK_PREFERRED_NETWORK_SUBACTIVITY:
    	    if (resultCode == Activity.RESULT_OK) {
                String longname = data.getStringExtra(LONGALPHA_TAG);
	    	    String numeric = data.getStringExtra(NUMERIC_TAG);
	    	    if  (longname != null || numeric != null) {
	    	    	mLongAlpha.setText(longname);
	    	    	mNumericText.setText(numeric);          
       		        		            
    	    	}
    	    }
    	    break;  
    
    	}
    }
        
}
// LGE_PREFERRED_NETWORKS_FEATURE END

