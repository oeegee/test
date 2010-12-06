//LGE_PHONEBOOK_EXTENSION START
package com.android.phone;

import static android.view.Window.PROGRESS_VISIBILITY_OFF;
import static android.view.Window.PROGRESS_VISIBILITY_ON;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import com.android.internal.telephony.IccPhoneBook;

/**
 * SIM Address Book UI for the Phone app.
 */
public class PhoneContacts extends ContactsList implements DialogInterface.OnClickListener{
    private static final String LOG_TAG = "PhoneContacts";

    static final ContentValues sEmptyContentValues = new ContentValues();

    protected static final String[] COLUMN_NAMES = new String[] {
        Contacts._ID,
        Contacts.DISPLAY_NAME
    };

    protected static final int NAME_COLUMN = 1;

    protected static final String WHERE = Contacts.HAS_PHONE_NUMBER + "!= 0 AND " + Contacts.IN_VISIBLE_GROUP + "!= 0";
    protected static final String[] DATA_PROJ = {Data.DATA1, Data.DATA2, Data.DATA3};
    
    private static final int MENU_SELECT_ALL = 1;
    private static final int MENU_UNSELECT_ALL = 2;
    private static final int MENU_MOVE = 3;
    private static final int MENU_COPY = 4;
    private boolean mMoveFlag = false;
    
    private ProgressDialog mProgressDialog;
    
    private Account mAccount;


    private AlertDialog mSimWarnDialog;
    private AlertDialog mNoMemoryDialog;
    private AlertDialog mDuplicateDialog;

    private int mDuplicates = 0;
    private int mNumContacts = 0;

    private class CalculateSpaceTask extends AsyncTask<Void, Void, Integer>{

        private ProgressDialog mProgress = null;
        
        protected void onPreExecute() {
            mProgress = new ProgressDialog(PhoneContacts.this);
            mProgress.setMessage(getString(R.string.enable_in_progress));
            mProgress.show();
        }

        protected Integer doInBackground(Void... args) {
            SimpleCursorAdapter adapter = getCurrentAdapter();
            int size = adapter.getCount();
            int length = IccPhoneBook.getInstance().getNameLength();
            ListView lv = getListView();
            SparseBooleanArray spa = lv.getCheckedItemPositions();
            int k = 0;
            int free = IccPhoneBook.getInstance().getRecordsLeft();
            ArrayList<String> simNames = new ArrayList<String>();
            mNumContacts = 0;

            Cursor names = getContentResolver().query(IccPhoneBook.CONTENT_URI, new String[]{Contacts.DISPLAY_NAME}, null, null, Contacts.DISPLAY_NAME);
            while (names.moveToNext()){
                simNames.add(names.getString(0));
            }
            names.close();
            mDuplicates = 0;

            for (int i = 0; i < size && k < free; i++) {
                if (spa.get(i)) {
                    long id = adapter.getItemId(i);
                    final Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, id);

                    if (IccPhoneBook.getInstance().getType() != IccPhoneBook.TYPE_USIM){
                        final Cursor name = getContentResolver().query(uri, new String[] {Contacts.DISPLAY_NAME}, null, null, null);
                        if (name != null && name.moveToFirst()){
                            if (Collections.binarySearch(simNames, cut(name.getString(0), length)) >= 0){
                                mDuplicates++;
                            }
                            name.close();
                        }
                        final Cursor phoneCursor = queryData(id, Phone.CONTENT_ITEM_TYPE);
                        while (phoneCursor.moveToNext()){
                            k++;
                        }
                        phoneCursor.close();
                    }else{
                        final Cursor name = queryData(id, StructuredName.CONTENT_ITEM_TYPE);
                        if (name != null && name.moveToFirst()){
                            final String fName = cut(name.getString(1), length);
                            final String lName = cut(name.getString(2), length);
                            final String dName = fName == null ? lName : (lName == null ? fName : fName + " " + lName);
                            name.close();
                            if (dName == null){
                                continue;
                            }
                            if (Collections.binarySearch(simNames, dName) >= 0){
                                mDuplicates++;
                            }
                        }
                        final Cursor phoneCursor = queryData(id, Phone.CONTENT_ITEM_TYPE);
                        final Cursor emailCursor = queryData(id, Email.CONTENT_ITEM_TYPE);

                        while (phoneCursor.moveToNext()){
                            phoneCursor.moveToNext();
                            emailCursor.moveToNext();
                            k++;
                        }
                        while (emailCursor.moveToNext()){
                            k++;
                        }                
                        phoneCursor.close();
                        emailCursor.close();
                    }

                    mNumContacts++;
                }
            }
            if (k > free){
                mNumContacts--;
            }
            if (mDuplicates > mNumContacts){
                mDuplicates = mNumContacts;
            }

            return mNumContacts;
        }

        protected void onPostExecute(Integer numc) {
            mProgress.dismiss();
            int rid = IccPhoneBook.getInstance().getType() == IccPhoneBook.TYPE_SIM ?
                R.string.sim_warning: R.string.usim_warning;
            mSimWarnDialog = new AlertDialog.Builder(PhoneContacts.this)
            .setTitle(R.string.attention)
            .setPositiveButton(R.string.ok, PhoneContacts.this)
            .setNegativeButton(R.string.cancel, PhoneContacts.this)
            .setMessage(rid)
            .create();
            mSimWarnDialog.show();
        }

    } 

    private class ImportAllSimContactsThread extends Thread
            implements OnCancelListener, OnClickListener {
        boolean mCanceled = false;
        
        public ImportAllSimContactsThread() {
            super("ImportAllSimContactsThread");
        }
        
        @Override
        public void run() {
            SimpleCursorAdapter adapter = getCurrentAdapter();
            int size = adapter.getCount();
            long ids[] = new long[size];
            int length = IccPhoneBook.getInstance().getNameLength();

            for(int i = 0; i < size; i++){
                ids[i] = adapter.getItemId(i);
            }
            ListView lv = getListView();
            SparseBooleanArray spa = lv.getCheckedItemPositions();

            for (int i = 0; i < size && !mCanceled && mNumContacts > 0; i++, mNumContacts--) {
                if (spa.get(i)) {
                    long id = ids[i];
                    final Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, id);
                    ContentValues cvs = new ContentValues();
                    if (IccPhoneBook.getInstance().getType() != IccPhoneBook.TYPE_USIM){
                        final Cursor name = getContentResolver().query(uri, new String[] {Contacts.DISPLAY_NAME}, null, null, null);
                        if (name != null && name.moveToFirst()){
                            cvs.put(IccPhoneBook.FIRST_NAME, cut(name.getString(0), length));
                            name.close();
                        }
                        final Cursor phoneCursor = queryData(id, Phone.CONTENT_ITEM_TYPE);
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM START
                        if (phoneCursor != null && phoneCursor.moveToFirst()){
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM END
                            cvs.put(IccPhoneBook.NUMBER1, cut(PhoneNumberUtils.stripSeparators(phoneCursor.getString(0)), length));
                            getContentResolver().insert(IccPhoneBook.CONTENT_URI,cvs);
                        }                
                        phoneCursor.close();
                    }else{
                        final Cursor name = queryData(id, StructuredName.CONTENT_ITEM_TYPE);
                        if (name != null && name.moveToFirst()){
                            final String fName = cut(name.getString(1), length);
                            final String lName = cut(name.getString(2), length);
                            if (fName == null && lName == null){
                                name.close();
                                continue;
                            }
                            cvs.put(IccPhoneBook.FIRST_NAME, fName);
                            cvs.put(IccPhoneBook.LAST_NAME, lName);
                            name.close();
                        }
                        final Cursor phoneCursor = queryData(id, Phone.CONTENT_ITEM_TYPE);
                        final Cursor emailCursor = queryData(id, Email.CONTENT_ITEM_TYPE);
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM START
                        final Cursor groupCursor = queryData(id, Groups.CONTENT_ITEM_TYPE);
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM END

                        if (phoneCursor != null && phoneCursor.moveToFirst()){
                            cvs.put(IccPhoneBook.NUMBER1, cut(PhoneNumberUtils.stripSeparators(phoneCursor.getString(0)), length));
                            if (phoneCursor.moveToNext()){
                                cvs.put(IccPhoneBook.NUMBER2, cut(PhoneNumberUtils.stripSeparators(phoneCursor.getString(0)), length));
                            }
                        }

                        if (emailCursor != null && emailCursor.moveToFirst()){
                                cvs.put(IccPhoneBook.EMAIL, cut(emailCursor.getString(0), length));
                            }
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM START
                        if (groupCursor != null && groupCursor.moveToFirst()){
                            cvs.put(IccPhoneBook.GROUP, cut(groupCursor.getString(0), length));
                        }
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM END

                            getContentResolver().insert(IccPhoneBook.CONTENT_URI,cvs);

                        phoneCursor.close();
                        emailCursor.close();
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM START
                        groupCursor.close();
                        // LGE_TELECA_CR:673_EXPORT_CONTACT_TO_SIM END
                    }

                    if (mMoveFlag){
                        getContentResolver().delete(uri, null, null);
                    }
                    mProgressDialog.incrementProgressBy(1);
                }
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
                new String[] { Contacts.DISPLAY_NAME }, new int[] { android.R.id.text1 });
    }

    @Override
    protected void query() {
        if (DBG) log("query: starting an async query");
        mQueryHandler.startQuery(QUERY_TOKEN, null, Contacts.CONTENT_URI, COLUMN_NAMES,
                WHERE, null, Contacts.DISPLAY_NAME);
        displayProgress(true);
    }

    @Override
    protected void displayProgress(boolean flag) {
        if (DBG) log("displayProgress: " + flag);
        mEmptyText.setText(flag ? R.string.phoneContacts_emptyLoading: R.string.phoneContacts_empty);
        getWindow().setFeatureInt(
                Window.FEATURE_INDETERMINATE_PROGRESS,
                flag ? PROGRESS_VISIBILITY_ON : PROGRESS_VISIBILITY_OFF);
    }

    protected String cut(String input, int length){
        if (input == null || input.length() <= length)
            return input;
        return input.substring(0, length);
            
    }

    public void onClick (DialogInterface dialog, int which){
        int step = 0;
        if (which == DialogInterface.BUTTON_NEGATIVE) return;
        if (dialog == mSimWarnDialog){
            if (mNumContacts!= getCheckedItems()){
                mNoMemoryDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.attention)
                .setNegativeButton(R.string.cancel, this)
                .setMessage(getResources().getQuantityString(R.plurals.no_space, mNumContacts, mNumContacts))
                .create();

                if (mNumContacts != 0){
                    mNoMemoryDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), this);
                }

                mNoMemoryDialog.show();
                return;
            }
            step = 1;
        }
        if (dialog == mNoMemoryDialog || step == 1){
            if (mDuplicates > 0){
                mDuplicateDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.attention)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .setMessage(getResources().getQuantityString(R.plurals.matches_found_on_sim, mDuplicates, mDuplicates))
                .create();

                mDuplicateDialog.show();
                return;
            }
            step = 2;
        }
        if (dialog == mDuplicateDialog || step == 2){
            CharSequence title = mMoveFlag ? getString(R.string.moveContacts) : getString(R.string.copyContacts);
            CharSequence message = getString(R.string.importingSimContacts); 

            ImportAllSimContactsThread thread = new ImportAllSimContactsThread();

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(message);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.cancel), thread);
            mProgressDialog.setProgress(0);
            mProgressDialog.setMax(mNumContacts);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();

            thread.start();
        }
    }

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
            case MENU_COPY:
                mMoveFlag = false;
                break;
            case MENU_MOVE:
                mMoveFlag = true;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        if (getCheckedItems() == 0){
            Toast.makeText(this, R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return true;
        }

        new CalculateSpaceTask().execute();

        return true;
    }
    
    private Cursor queryData(long contactId, String type) {
        final Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        final Uri dataUri = Uri.withAppendedPath(baseUri, Contacts.Data.CONTENT_DIRECTORY);

        Cursor c = getContentResolver().query(dataUri,
                DATA_PROJ, Data.MIMETYPE + "=?", new String[] {type}, null);
        return c;
    }
}
//LGE_PHONEBOOK_EXTENSION END
