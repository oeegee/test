/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.accounts.Account;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.Log;
//LGE_PHONEBOOK_EXTENSION START
//import android.view.ContextMenu;
//LGE_PHONEBOOK_EXTENSION END
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
//LGE_PHONEBOOK_EXTENSION START
//import android.view.View;
//import android.widget.AdapterView;
//LGE_PHONEBOOK_EXTENSION END
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.view.View;
import android.widget.SimpleCursorAdapter;
//LGE_PHONEBOOK_EXTENSION START
//import android.widget.TextView;
//LGE_PHONEBOOK_EXTENSION END

import java.util.ArrayList;

//LGE_PHONEBOOK_EXTENSION START
import android.app.AlertDialog;
import android.content.ContentUris;
import android.provider.ContactsContract.Contacts;
import android.util.SparseBooleanArray;
import android.widget.Toast;
import com.android.internal.telephony.IccPhoneBook;
//LGE_PHONEBOOK_EXTENSION END

/**
 * SIM Address Book UI for the Phone app.
 */
//LGE_PHONEBOOK_EXTENSION START
//public class SimContacts extends ADNList {
public class SimContacts extends ContactsList {
//LGE_PHONEBOOK_EXTENSION END
    private static final String LOG_TAG = "SimContacts";

    static final ContentValues sEmptyContentValues = new ContentValues();

//LGE_PHONEBOOK_EXTENSION START
//    private static final int MENU_IMPORT_ONE = 1;
//    private static final int MENU_IMPORT_ALL = 2;
    private static final int MENU_SELECT_ALL = 0;
    private static final int MENU_UNSELECT_ALL = 1;
    private static final int MENU_MOVE = 2;
    private static final int MENU_COPY = 3;
    private boolean mMoveFlag = false;
//LGE_PHONEBOOK_EXTENSION END
    
    private ProgressDialog mProgressDialog;

    private Account mAccount;

    private static class NamePhoneTypePair {
        final String name;
        final int phoneType;
        public NamePhoneTypePair(String nameWithPhoneType) {
            // Look for /W /H /M or /O at the end of the name signifying the type
            int nameLen = nameWithPhoneType.length();
            if (nameLen - 2 >= 0 && nameWithPhoneType.charAt(nameLen - 2) == '/') {
                char c = Character.toUpperCase(nameWithPhoneType.charAt(nameLen - 1));
                if (c == 'W') {
                    phoneType = Phone.TYPE_WORK;
                } else if (c == 'M' || c == 'O') {
                    phoneType = Phone.TYPE_MOBILE;
                } else if (c == 'H') {
                    phoneType = Phone.TYPE_HOME;
                } else {
                    phoneType = Phone.TYPE_OTHER;
                }
                name = nameWithPhoneType.substring(0, nameLen - 2);
            } else {
                phoneType = Phone.TYPE_OTHER;
                name = nameWithPhoneType;
            }
        }
    }

    private class ImportAllSimContactsThread extends Thread
            implements OnCancelListener, OnClickListener {

        boolean mCanceled = false;
//LGE_PHONEBOOK_EXTENSION START
        int[] positionsArray = getPositions();
        SimpleCursorAdapter adapter = getCurrentAdapter();
//LGE_PHONEBOOK_EXTENSION END

        public ImportAllSimContactsThread() {
            super("ImportAllSimContactsThread");
        }

        @Override
        public void run() {
//LGE_PHONEBOOK_EXTENSION START
//            final ContentValues emptyContentValues = new ContentValues();
//            final ContentResolver resolver = getContentResolver();
//
//            mCursor.moveToPosition(-1);
//            while (!mCanceled && mCursor.moveToNext()) {
//                actuallyImportOneSimContact(mCursor, resolver, mAccount);
            int k = 0;
            int num = getCheckedItems();
            long[] ids = new long[num];
            if (mMoveFlag){
                for (int i =0 ; i < num; i++){
                    ids[i]= adapter.getItemId(positionsArray[i]);
                }
            }
            while (!mCanceled && k != num) {
                if (mMoveFlag) {
                    moveOneSimContact(ids[k++]);
                } else {
                    importOneSimContact(positionsArray[k++]);
                }
//LGE_PHONEBOOK_EXTENSION END
                mProgressDialog.incrementProgressBy(1);
            }

            mProgressDialog.dismiss();
            finish();
        }

        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCanceled = true;
                mProgressDialog.dismiss();
            } else {
                Log.e(LOG_TAG, "Unknown button event has come: " + dialog.toString());
            }
        }
    }

    // From HardCodedSources.java in Contacts app.
    // TODO: fix this.
    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    private static final String GOOGLE_MY_CONTACTS_GROUP = "System Group: My Contacts";

    private static void actuallyImportOneSimContact(
            final Cursor cursor, final ContentResolver resolver, Account account) {
        final NamePhoneTypePair namePhoneTypePair =
            new NamePhoneTypePair(cursor.getString(NAME_COLUMN));
        final String name = namePhoneTypePair.name;
        final int phoneType = namePhoneTypePair.phoneType;
//LGE_PHONEBOOK_EXTENSION START
//        final String phoneNumber = cursor.getString(NUMBER_COLUMN);
        final String[] phoneNumbers = {cursor.getString(NUMBER_COLUMN), cursor.getString(NUMBER2_COLUMN)};
//LGE_PHONEBOOK_EXTENSION END
        final String emailAddresses = cursor.getString(EMAIL_COLUMN);
        final String[] emailAddressArray;
        if (!TextUtils.isEmpty(emailAddresses)) {
            emailAddressArray = emailAddresses.split(",");
        } else {
            emailAddressArray = null;
        }

        final ArrayList<ContentProviderOperation> operationList =
            new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        String myGroupsId = null;
        if (account != null) {
            builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
            builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);

            // TODO: temporal fix for "My Groups" issue. Need to be refactored.
            if (ACCOUNT_TYPE_GOOGLE.equals(account.type)) {
                final Cursor tmpCursor = resolver.query(Groups.CONTENT_URI, new String[] {
                        Groups.SOURCE_ID },
                        Groups.TITLE + "=?", new String[] {
                        GOOGLE_MY_CONTACTS_GROUP }, null);
                try {
                    if (tmpCursor != null && tmpCursor.moveToFirst()) {
                        myGroupsId = tmpCursor.getString(0);
                    }
                } finally {
                    if (tmpCursor != null) {
                        tmpCursor.close();
                    }
                }
            }
        } else {
            builder.withValues(sEmptyContentValues);
        }
        operationList.add(builder.build());

        builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
        builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        builder.withValue(StructuredName.DISPLAY_NAME, name);
        operationList.add(builder.build());

//LGE_PHONEBOOK_EXTENSION START
//        builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
//        builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
//        builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
//        builder.withValue(Phone.TYPE, phoneType);
//        builder.withValue(Phone.NUMBER, phoneNumber);
//        builder.withValue(Data.IS_PRIMARY, 1);
//        operationList.add(builder.build());
        for (String phoneNumber : phoneNumbers) {
            if (phoneNumber != null){
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                builder.withValue(Phone.TYPE, phoneType);
                builder.withValue(Phone.NUMBER, phoneNumber);
                operationList.add(builder.build());
            }
        }
//LGE_PHONEBOOK_EXTENSION END

        if (emailAddressArray != null) {
            for (String emailAddress : emailAddressArray) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
//LGE_PHONEBOOK_EXTENSION START
//                builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
                builder.withValue(Email.TYPE, Email.TYPE_CUSTOM);
//LGE_PHONEBOOK_EXTENSION END
                builder.withValue(Email.DATA, emailAddress);
                operationList.add(builder.build());
            }
        }

        if (myGroupsId != null) {
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(GroupMembership.RAW_CONTACT_ID, 0);
            builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
            builder.withValue(GroupMembership.GROUP_SOURCE_ID, myGroupsId);
            operationList.add(builder.build());
        }

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
    }

    private void importOneSimContact(int position) {
        final ContentResolver resolver = getContentResolver();
        if (mCursor.moveToPosition(position)) {
            actuallyImportOneSimContact(mCursor, resolver, mAccount);
        } else {
            Log.e(LOG_TAG, "Failed to move the cursor to the position \"" + position + "\"");
        }
    }

//LGE_PHONEBOOK_EXTENSION START
    private void moveOneSimContact(long id) {
        final ContentResolver resolver = getContentResolver();
        final Cursor cur = resolver.query(ContentUris.withAppendedId(IccPhoneBook.CONTENT_URI, id), COLUMN_NAMES, null, null, null);
        if (cur.moveToFirst()) {
            actuallyImportOneSimContact(cur, resolver, mAccount);
            getContentResolver().delete(ContentUris.withAppendedId(IccPhoneBook.CONTENT_URI, id), null,null);
        } else {
            Log.e(LOG_TAG, "Failed to move the cursor ");
        }
        cur.close();
    }
//LGE_PHONEBOOK_EXTENSION END

    /* Followings are overridden methods */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent != null) {
            final String accountName = intent.getStringExtra("account_name");
            final String accountType = intent.getStringExtra("account_type");
            if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                mAccount = new Account(accountName, accountType);
            }
        }

        registerForContextMenu(getListView());
     }
    
    @Override
    protected CursorAdapter newAdapter() {
        return new SimpleCursorAdapter(this, R.layout.sim_import_list_entry, mCursor,
//LGE_PHONEBOOK_EXTENSION START
                new String[] { "display_name" }, new int[] { android.R.id.text1 });
//LGE_PHONEBOOK_EXTENSION END
    }

    @Override
    protected Uri resolveIntent() {
        Intent intent = getIntent();
//LGE_PHONEBOOK_EXTENSION START
        intent.setData(IccPhoneBook.CONTENT_URI);
//LGE_PHONEBOOK_EXTENSION END
        if (Intent.ACTION_PICK.equals(intent.getAction())) {
            // "index" is 1-based
            mInitialSelection = intent.getIntExtra("index", 0) - 1;
        }
        return intent.getData();
    }
//LGE_PHONEBOOK_EXTENSION START
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
 
        menu.add(0, MENU_SELECT_ALL, 0, R.string.selectAllSimEntries);
        menu.add(0, MENU_UNSELECT_ALL, 0, R.string.unselectAllSimEntries);
        menu.add(0, MENU_MOVE, 0, R.string.moveSimEntries);
        menu.add(0, MENU_COPY, 0, R.string.copySimEntries);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SELECT_ALL: {
                SimpleCursorAdapter adapter = getCurrentAdapter();
                int size = adapter.getCount();
                ListView lv = getListView();
                for (int i = 0; i < size; i ++) {
                    lv.setItemChecked(i, true);
                }
                return true;
            }
            case MENU_UNSELECT_ALL: {
                SimpleCursorAdapter adapter = getCurrentAdapter();
                int size = adapter.getCount();
                ListView lv = getListView();
                lv.clearChoices();
                for (int i = 0; i < size; i ++) {
                    lv.setItemChecked(i, false);
                }
                return true;
            }
            case MENU_COPY: {
                mMoveFlag = false;
                break;
            }
            case MENU_MOVE: {
                mMoveFlag = true;
                break;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

        int size = getCheckedItems();
        if (size == 0){
            Toast.makeText(this, R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return true;
        }

        CharSequence title = mMoveFlag ? getString(R.string.moveContacts) : getString(R.string.copyContacts);
        CharSequence message = mMoveFlag ? getString(R.string.movingContacts) : getString(R.string.copyingContacts);

        final ImportAllSimContactsThread thread = new ImportAllSimContactsThread();
//LGE_PHONEBOOK_EXTENSION END

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(message);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel), thread);
        mProgressDialog.setProgress(0);
//LGE_PHONEBOOK_EXTENSION START
        mProgressDialog.setMax(size);
        mProgressDialog.setCancelable(true);

        int[] positionsArray = getPositions();
        StringBuilder where = new StringBuilder(Contacts.DISPLAY_NAME + " IN ('");
        for (int i = 0; i < size; i++){
            mCursor.moveToPosition(positionsArray[i]);
            where.append(mCursor.getString(NAME_COLUMN)).append("','");
        }
        where.append("')");
        final Cursor c = getContentResolver().query(Contacts.CONTENT_URI, new String[] {"_id"}, where.toString(), null, null);
        if (c.getCount() != 0){
            new AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mProgressDialog.show();

                    thread.start();

                    /* User clicked OK so do some stuff */
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked Cancel so do some stuff */
                }
            })
            .setMessage(getResources().getQuantityString(R.plurals.matches_found, c.getCount(), c.getCount()))
            .create().show();
            c.close();
            return true;
        }
        c.close();
        
        mProgressDialog.show();

        thread.start();
        return true;
//LGE_PHONEBOOK_EXTENSION END
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        importOneSimContact(position);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                if (mCursor != null && mCursor.moveToPosition(getSelectedItemPosition())) {
                    String phoneNumber = mCursor.getString(NUMBER_COLUMN);
//LGE_PHONEBOOK_EXTENSION START
                    if (phoneNumber == null){
                        phoneNumber = mCursor.getString(NUMBER2_COLUMN);
                    }
//LGE_PHONEBOOK_EXTENSION END
                    if (phoneNumber == null || !TextUtils.isGraphic(phoneNumber)) {
                        // There is no number entered.
                        //TODO play error sound or something...
                        return true;
                    }
                    Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts("tel", phoneNumber, null));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                          | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    startActivity(intent);
                    finish();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
