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


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.net.Uri;
import android.widget.ListView;


public class BlackListSelectActivity extends ListActivity implements OnClickListener {
    private ListView mView;

    ArrayAdapter<String> adapter;
    private static final String LOG_TAG = "BlackLIstSelectActivity";
    private static String number;

    public ArrayAdapter<String> createAdapter(Context ctx, String[] ear, int resId, int dropDownId)
    {
        String[] ar = new String[ear.length];

        for(int i=0; i<ear.length; i++)
            ar[i] = new String(ear[i]);

        ArrayAdapter<String> adapter_temp = new ArrayAdapter<String>(ctx, resId, ar);
         return adapter_temp;
    }


    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        Log.d(LOG_TAG,"onCreate");
        setContentView(R.layout.blacklist);
        mView = getListView();
        number = getIntent().getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        adapter = this.createAdapter(mView.getContext(),BlackList.values(), android.R.layout.simple_list_item_multiple_choice, 0);
        mView.setAdapter(adapter);
        mView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mView.setTextFilterEnabled(true);

        final Button button_done = (Button) findViewById(R.id.btndone);
        button_done.setOnClickListener(this);
    }

	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
        case R.id.btndone:
            remove_close();
            break;
        default:
            Log.w(LOG_TAG,"Got click from unexpected View ID " + id + " (View = " + view + ")");
            break;
       }

	}


	private void remove_close() {
		SparseBooleanArray arr =  mView.getCheckedItemPositions();
        int size = arr.size();
        int j = 0;
        for(int i=0; i<size; ++i) {
            if(!arr.valueAt(i)) continue;
            BlackList.remove(arr.keyAt(i)-j);
            j++;

         }
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel",number,null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
		finish();
	}
}
// LGE_AUTO_REDIAL END



