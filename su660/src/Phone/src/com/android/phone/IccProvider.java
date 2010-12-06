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

// LGE_PHONEBOOK_EXTENSION START

import static com.android.internal.telephony.IccPhoneBook._ID;
import static com.android.internal.telephony.IccPhoneBook.getTotalRecords;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.content.Entity.NamedContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Calendar;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;
import android.widget.Toast;
import android.provider.ContactsContract.Settings;

import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.gsm.PhonebookEntry;

import com.google.android.collect.Lists;
import com.lge.config.StarConfig;

import android.content.BroadcastReceiver;

/**
 * ICC address book content provider.
 */
public class IccProvider extends com.android.internal.telephony.IccProvider {
// LGE_PHONEBOOK_EXTENSION START

    private static final String TAG = "IccProvider";

    // [bboguri.kim@lge.com] 2010. 10. 4. start
    public static final String ACTION_REFRESH_SIMSEARCH_REQUEST =
        "com.android.contacts.ACTION_REFRESH_SIMSEARCH_REQUEST";
    public static final String ACTION_REFRESH_SIMSEARCH_STATUS =
        "com.android.phone.ACTION_REFRESH_SIMSEARCH_STATUS";
    // [bboguri.kim@lge.com] 2010. 10. 4. end

    // [bboguri.kim@lge.com] 2010. 8. 20. [contacs2.db]
    public static final String ICC_CONTACT_ACCOUNT_TYPE = "com.android.contacts.sim";
    public static final String ICC_CONTACT_ACCOUNT_NAME = "sim";

    public static final String TAGNAME = "tag";
    public static final String NUMBER = "number";
    public static final String ANRS = "anrs";
    public static final String EMAILS = "emails";
    public static final String NEW_TAGNAME = "newTag";
    public static final String NEW_NUMBER = "newNumber";
    public static final String NEW_ANRS = "newAnrs";
    public static final String NEW_EMAILS = "newEmails";

    // Sim Index Test
    // 2010. 11. 1. park.jongjin@lge.com use sim index [START_LGE_LAB1]
    public static final String SIM_INDEX = RawContacts.SOURCE_ID;
    public static final String RAW_CONTACT_ID = Data.RAW_CONTACT_ID;
    // 2010. 11. 1. park.jongjin@lge.com  [END_LGE_LAB1]

    // [bboguri.kim@lge.com] 2010. 9. 28. start : column of simphonebook table
    public static final String FIRST_NAME = "first_name";
    public static final String NUMBER1 = "number1";
    public static final String NUMBER2 = "number2";
    public static final String EMAIL = "email";
    // [bboguri.kim@lge.com] 2010. 9. 28. end

    public static final String RAW_CONTACTS = "raw_contacts";
    public static final String DATA = "data";
    public static final String PRESENCE = "presence";
    public static final String ACCOUNTS = "accounts";
    public static final String SETTINGS = "settings";

//    private String P_RAW_CONTACT_ID = "presence_raw_contact_id";
//    private String P_CONTACT_ID = "presence_contact_id";

    // [bboguri.kim@lge.com] 2010. 8. 20. [contacs2.db] end

    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//    private static final String DATABASE_NAME = "simphonebook.db";
//    private static final int DATABASE_VERSION = 1;
//    private static final String CONTACTS_TABLE = "contacts";
//    private static final String GROUPS_TABLE = "groups";
    // [greenfield@lge.com]2010. 9. 8. end

    private boolean mInitialized = false;

    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//    private static HashMap<String, String> sContactsProjectionMap;
//    private static HashMap<String, String> sPhone1ProjectionMap;
//    private static HashMap<String, String> sGroupsProjectionMap;
    // [greenfield@lge.com]2010. 9. 8. end
    private static boolean[] sSimMap;

    private static final int CONTACTS = 1;
    private static final int CONTACT_ID = 2;
    // Sim Index Test
    // 2010. 11. 2. park.jongjin@lge.com use sim index [START_LGE_LAB1]
    private static final int SIM_INDEX_RAW_CONTACT_ID = 3;

    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//    private static final int JOINED = 3;
//    private static final int JOINED_FILTER = 4;
//    private static final int GROUPS = 5;
//    private static final int PHONES = 6;
    // [greenfield@lge.com]2010. 9. 8. end
    private static final int INIT = 7;

    private static final UriMatcher sUriMatcher;
    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//    private static final String DISPLAY_NAME_SQL =
//          "(CASE WHEN (last_name IS NOT NULL AND first_name IS NOT NULL) "
//                + "THEN first_name || ' ' || last_name "
//            + "ELSE "
//                +"(CASE WHEN (first_name IS NOT NULL) "
//                    + "THEN first_name "
//                + "ELSE (CASE WHEN (last_name IS NOT NULL) "
//                        + "THEN last_name "
//                    + "ELSE "
//                        + "number1 "
//                    + "END) "
//                + "END) "
//            + "END)";
    // [greenfield@lge.com]2010. 9. 8. end

    private static final Object mutex = new Object();


// LGE_PHONEBOOK_EXTENSION END

    public static boolean isKR = StarConfig.COUNTRY.equals("KR");

    public IccProvider() {
        super();
        mInitialized = false;
    }

// LGE_PHONEBOOK_EXTENSION START
    /**
     * This class helps open, create, and upgrade the database file.
     */
    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//    private class DatabaseHelper extends SQLiteOpenHelper {
//
//        DatabaseHelper(Context context) {
//            super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        }
//
//        @Override
//        public void onCreate(SQLiteDatabase db) {
//
//            db.execSQL("CREATE TABLE " + CONTACTS_TABLE + " ("
//                    + _ID + " INTEGER PRIMARY KEY,"
//                    + FIRST_NAME + " TEXT,"
//                    + LAST_NAME + " TEXT,"
//                    + NUMBER1 + " TEXT,"
//                    + NUMBER2 + " TEXT,"
//                    + EMAIL + " TEXT,"
//                    + GROUP + " TEXT NOT NULL DEFAULT '',"
//                    + Contacts.HAS_PHONE_NUMBER + " INTEGER NOT NULL DEFAULT 0,"
//                    + HAS_EMAIL_ADDRESS + " INTEGER NOT NULL DEFAULT 0,"
//                    + VISIBLE + " INTEGER NOT NULL DEFAULT 1"
//                    + ");");
//
//            db.execSQL("CREATE INDEX contacts_visible_index ON " + CONTACTS_TABLE + " (" +
//                    VISIBLE + "," +
//                    FIRST_NAME + " COLLATE LOCALIZED" + "," +
//                    LAST_NAME + " COLLATE LOCALIZED" + ");");
//
//            db.execSQL("CREATE INDEX contacts_has_phone_index ON " + CONTACTS_TABLE + " (" +
//                    Contacts.HAS_PHONE_NUMBER + ");");
//
//            db.execSQL("CREATE INDEX contacts_has_email_index ON " + CONTACTS_TABLE + " (" +
//                    HAS_EMAIL_ADDRESS +  ");");
//
//            db.execSQL("CREATE TABLE " + GROUPS_TABLE + " ("
//                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
//                    + GROUP + " TEXT UNIQUE NOT NULL,"
//                    + VISIBLE + " INTEGER DEFAULT 1"
//                    + ");");
//
//            db.execSQL("CREATE TRIGGER " + CONTACTS_TABLE + "_group_inserted AFTER INSERT ON " + CONTACTS_TABLE
//                    + " WHEN (0 = (SELECT COUNT(_id) FROM " + GROUPS_TABLE
//                    + "     WHERE " + GROUP + "=new." + GROUP + "))"
//                    + " BEGIN "
//                    + "   INSERT INTO " + GROUPS_TABLE + " (" + GROUP + ")"
//                    + "     VALUES (new." + GROUP + ")" + ";"
//                    + " END");
//
//            db.execSQL("CREATE TRIGGER " + CONTACTS_TABLE + "_inserted AFTER INSERT ON " + CONTACTS_TABLE
//                    + " WHEN (0 != (SELECT COUNT(_id) FROM " + GROUPS_TABLE
//                    + "     WHERE " + GROUP + "=new." + GROUP + "))"
//                    + " BEGIN "
//                    + "   UPDATE " + CONTACTS_TABLE
//                    + "     SET " + VISIBLE + "="
//                    + "      (SELECT " + VISIBLE + " FROM " + GROUPS_TABLE
//                    + "       WHERE " + GROUP + "=" + "new." + GROUP + ")"
//                    + "     WHERE _id= new._id;"
//                    + " END");
//
//            db.execSQL("CREATE TRIGGER " + CONTACTS_TABLE + "_group_updated AFTER UPDATE"
//                    + " OF " + GROUP + " ON " + CONTACTS_TABLE
//                    + " WHEN (0 = (SELECT COUNT(_id) FROM " + GROUPS_TABLE
//                    + "     WHERE " + GROUP + "=new." + GROUP + "))"
//                    + " BEGIN "
//                    + "   INSERT INTO " + GROUPS_TABLE + " (" + GROUP + ")"
//                    + "     VALUES (new." + GROUP + ")" + ";"
//                    + " END");
//
//            db.execSQL("CREATE TRIGGER " + CONTACTS_TABLE + "_updated AFTER UPDATE"
//                    + " OF " + GROUP + " ON " + CONTACTS_TABLE
//                    + " WHEN (0 != (SELECT COUNT(_id) FROM " + GROUPS_TABLE
//                    + "     WHERE " + GROUP + "=new." + GROUP + "))"
//                    + " BEGIN "
//                    + "   UPDATE " + CONTACTS_TABLE
//                    + "     SET " + VISIBLE + "="
//                    + "      (SELECT " + VISIBLE + " FROM " + GROUPS_TABLE
//                    + "       WHERE " + GROUP + "=" + "new." + GROUP + ")"
//                    + "     WHERE _id= new._id;"
//                    + " END");
//
//            db.execSQL("CREATE TRIGGER " + CONTACTS_TABLE + "_phone_inserted AFTER INSERT ON " + CONTACTS_TABLE
//                    + " BEGIN "
//                    + "   UPDATE " + CONTACTS_TABLE
//                    + "     SET " + Contacts.HAS_PHONE_NUMBER + "="
//                    + "      (CASE WHEN ((new.number1 IS NOT NULL AND new.number1 != '')"
//                    + "               OR (new.number2 IS NOT NULL AND new.number2 != '')) "
//                    + "       THEN 1 "
//                    + "       ELSE 0 "
//                    + "       END) "
//                    + "     WHERE _id= new._id;"
//                    + "   UPDATE " + CONTACTS_TABLE
//                    + "     SET " + HAS_EMAIL_ADDRESS + "="
//                    + "      (CASE WHEN (new.email IS NOT NULL AND new.email != '')"
//                    + "       THEN 1 "
//                    + "       ELSE 0 "
//                    + "       END) "
//                    + "     WHERE _id= new._id;"
//                    + " END");
//
//            db.execSQL("CREATE TRIGGER " + CONTACTS_TABLE + "_phone_update AFTER UPDATE ON " + CONTACTS_TABLE
//                    + " BEGIN "
//                    + "   UPDATE " + CONTACTS_TABLE
//                    + "     SET " + Contacts.HAS_PHONE_NUMBER + "="
//                    + "      (CASE WHEN ((new.number1 IS NOT NULL AND new.number1 != '')"
//                    + "               OR (new.number2 IS NOT NULL AND new.number2 != '')) "
//                    + "       THEN 1 "
//                    + "       ELSE 0 "
//                    + "       END) "
//                    + "     WHERE _id= new._id;"
//                    + "   UPDATE " + CONTACTS_TABLE
//                    + "     SET " + HAS_EMAIL_ADDRESS + "="
//                    + "      (CASE WHEN (new.email IS NOT NULL AND new.email != '')"
//                    + "       THEN 1 "
//                    + "       ELSE 0 "
//                    + "       END) "
//                    + "     WHERE _id= new._id;"
//                    + " END");
//
//            db.execSQL("CREATE TRIGGER " + GROUPS_TABLE + "_updated AFTER UPDATE ON " + GROUPS_TABLE
//                    + " BEGIN "
//                    + "   UPDATE " + CONTACTS_TABLE
//                    + "     SET " + VISIBLE + "=new." + VISIBLE
//                    + "     WHERE " + GROUP + "= new." + GROUP + ";"
//                    + " END");
//
//            db.execSQL("INSERT INTO groups (group_name) VALUES ('')"); //We always should have at least one group
//        }
//
//        @Override
//        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        }
//
//        @Override
//        public void onOpen(SQLiteDatabase db) {
//        }
//    }
//
//    private DatabaseHelper mOpenHelper;
    // [greenfield@lge.com]2010. 9. 10. end

    @Override
    public boolean onCreate() {
        // [greenfield@lge.com]2010. 9. 10. modified reason : Unused code
//        mOpenHelper = new DatabaseHelper(getContext());
//        return super.onCreate();
        // [greenfield@lge.com]2010. 9. 8. end

        // [bboguri.kim@lge.com] 2010. 10. 4. start : communicate with ContactsListActivity
        IntentFilter SIMIntetFilter = new IntentFilter(ACTION_REFRESH_SIMSEARCH_REQUEST);
        getContext().registerReceiver(simReceiver , SIMIntetFilter );
        // [bboguri.kim@lge.com] 2010. 10. 5. end
        // [greenfield@lge.com]2010. 9. 10. modified reason : Temporary reset code
        resetSimData();
        return super.onCreate();
        // [greenfield@lge.com]2010. 9. 10. end
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
//        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String mSelection;
        if(selection == null)
            mSelection  = "";

        switch (sUriMatcher.match(uri)) {
            // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//            case GROUPS:
//                qb.setTables(GROUPS_TABLE);
//                qb.setProjectionMap(sGroupsProjectionMap);
//                qb.setDistinct(true);
//                break;
//            case JOINED:{
//                final Cursor sim = query(CONTENT_URI, projection, selection, selectionArgs, sortOrder);
//                final Cursor phone = getContext().getContentResolver().query(Contacts.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
//                return new JoinedCursor(phone, sim, projection);
//                }
//            case JOINED_FILTER:{
//                String filter = null;
//                if (uri.getPathSegments().size() > 2) {
//                    filter = uri.getLastPathSegment();
//                }
//
//                String where = null;
//                if (filter != null){
//                    where = "(first_name LIKE '" + filter +"%' OR last_name LIKE '" + filter + "%')";
//                }
//                if (selection != null){
//                    where += " AND (" + selection + ")";
//                }
//
//                final Cursor sim = query(CONTENT_URI, projection, where, selectionArgs, sortOrder);
//
//                Uri newUri = filter == null ? Contacts.CONTENT_URI : Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(filter));
//                final Cursor phone = getContext().getContentResolver().query(newUri, projection, selection, selectionArgs, sortOrder);
//
//                return new JoinedCursor(phone, sim, projection);
//                }
//            case CONTACTS:
//                qb.setTables(CONTACTS_TABLE);
//                qb.setProjectionMap(sContactsProjectionMap);
//                break;
//
//            case CONTACT_ID:
//                qb.setTables(CONTACTS_TABLE);
//                qb.setProjectionMap(sContactsProjectionMap);
//                qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
//                break;
//
//            case PHONES:
//                qb.setTables(CONTACTS_TABLE);
//                qb.setProjectionMap(sPhone1ProjectionMap);
//
//                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
//                Cursor sim = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
//
//                // Tell the cursor what uri to watch, so it knows when its source data changes
//                sim.setNotificationUri(getContext().getContentResolver(), uri);
//
//                final Cursor phone = getContext().getContentResolver().query(Phone.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
//                return new JoinedCursor(phone, sim, projection);
            // [greenfield@lge.com]2010. 9. 8. end
            // [bboguri.kim@lge.com] 2010. 9. 14. start
            case INIT:
                 return init();

            case CONTACTS:
                mSelection = convertFromPhoneBookSelectoinToContactSelection(selection);
                Cursor rawCursor = getContext().getContentResolver().query(Data.CONTENT_URI,
                        new String[]{Data.RAW_CONTACT_ID}, mSelection, selectionArgs, sortOrder);

                StringBuilder selectionBuilder = new StringBuilder();

                selectionBuilder.setLength(0);
                selectionBuilder.append(RawContacts._ID + " IN (");
                int i = 0;
                while(rawCursor.moveToNext()) {
                    if(i != 0) {
                        selectionBuilder.append(", ");
                        selectionBuilder.append(rawCursor.getLong(0));
                        i++;
                    }
                }
                selectionBuilder.append(')');

                rawCursor.close();

                String tmpselection = "";

                if(i == 0)
                    tmpselection = RawContacts._ID + " IN ( 0 ) ";
                else
                    tmpselection = selectionBuilder.toString();

                final Cursor  c  = getContext().getContentResolver().query(RawContactsEntity.CONTENT_URI,
                        projection, tmpselection, selectionArgs, sortOrder);

                if (c == null) {
                    return null;
                }
                return  new IccContactCursor(c);

            case CONTACT_ID:
                String contactId = Long.toString(ContentUris.parseId(uri));
                selection += RawContacts.ACCOUNT_TYPE  + " =  '" + ICC_CONTACT_ACCOUNT_TYPE + "' AND "
                    + RawContacts.CONTACT_ID + "='" + contactId + "' ";

                final Cursor d = getContext().getContentResolver().query(RawContacts.CONTENT_URI,
                        projection, selection, selectionArgs, sortOrder);
                if (d == null) {
                    return null;
                }
                return  new IccContactCursor(d);
                // [bboguri.kim@lge.com] 2010. 9. 14. end
            default:
                return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }

        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        // Get the database and run the query
//        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
//        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
//
//        // Tell the cursor what uri to watch, so it knows when its source data changes
//        c.setNotificationUri(getContext().getContentResolver(), uri);
//        return c;
        // [greenfield@lge.com]2010. 9. 8. end
    }

    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//    @Override
//    public String getType(Uri uri) {
//        switch (sUriMatcher.match(uri)) {
//            case CONTACTS:
//                if (!mInitialized){
//                    return null;
//                }
//                return "vnd.android.cursor.dir/internal.sim-contact";
//            case CONTACT_ID:
//                return "vnd.android.cursor.item/internal.sim-contact";
//
//            default:
//                return super.getType(uri);
//        }
//    }
    // [greenfield@lge.com]2010. 9. 8. end

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) == CONTACTS) {
            if (!mInitialized){
                return null;
            }

            int i = 0;
            for (; i < sSimMap.length; i++){
                if (!sSimMap[i]) break;
            }

            if (i == sSimMap.length){
                throw new IndexOutOfBoundsException("There is no free space on sim card");
            }

            PhonebookEntry pe = new PhonebookEntry();
            pe.setIndex1(i+1);
            // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//            if (initialValues.getAsString(FIRST_NAME) != null){
//                pe.setText(initialValues.getAsString(FIRST_NAME));
//            }
//            if (initialValues.getAsString(LAST_NAME) != null){
//                pe.setSecondtext(initialValues.getAsString(LAST_NAME));
//            }
//            if (initialValues.getAsString(NUMBER1) != null){
//                pe.setNumber(initialValues.getAsString(NUMBER1));
//                pe.setType(detectType(initialValues.getAsString(NUMBER1)));
//            }
//            if (initialValues.getAsString(NUMBER2) != null){
//                pe.setAdnumber(initialValues.getAsString(NUMBER2));
//                pe.setAdtype(detectType(initialValues.getAsString(NUMBER2)));
//            }
//            if (initialValues.getAsString(EMAIL) != null){
//                pe.setEmail(initialValues.getAsString(EMAIL));
//            }
//            if (initialValues.getAsString(GROUP) != null){
//                pe.setGroup(initialValues.getAsString(GROUP));
//            }
            // [greenfield@lge.com]2010. 9. 8. end

            // [bboguri.kim@lge.com] 2010. 11. 4. start : optimization code
            String name = initialValues.getAsString(TAGNAME);
            String number = initialValues.getAsString(NUMBER);
            String anrs = initialValues.getAsString(ANRS);
            String emails = initialValues.getAsString(EMAILS);

            if (name != null){
                pe.setText(name);
            }
            if (number != null){
                pe.setNumber(number);
                pe.setType(detectType(number));
            }
            if (anrs != null){
                pe.setAdnumber(anrs);
                pe.setAdtype(detectType(anrs));
            }
            if (emails != null){
                pe.setEmail(emails);
            }
            // [bboguri.kim@lge.com] 2010. 11. 4. end

            if (!write(pe)){
                throw new SQLException("Failed to insert row into " + uri);
            }

            sSimMap[i] = true;//레코드 사용유무 flag 값 변경

            // 2010. 11. 2. park.jongjin@lge.com 내수에서는 다시 read하지 않음 [LGE_LAB1]
            if (isKR == false) {
                // Read new entry and put actual info in table
                IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                            ServiceManager.getService("simphonebook"));
                try {
                //sSimMap[i] = true;
                    if (iccIpb != null) {
                        final PhonebookEntry pes[] = iccIpb.readPhonebookEntry(i+1, i+1);
                        if (pes == null){
                            throw new SQLException("Failed to insert row into " + uri);
                        }
                        pe = pes[0];
                    // [bboguri.kim@lge.com] 2010. 11. 5. start : input-possible separator
                    pe.setNumber(convertValidSeparators(pe.getNumber()));
                    pe.setAdnumber(convertValidSeparators(pe.getAdnumber()));
                    // [bboguri.kim@lge.com] 2010. 11. 5. end
                    }
                } catch (RemoteException ex) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
            }

            // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//            long rowId = db.insert(CONTACTS_TABLE, FIRST_NAME, getValues(pe));
            // [greenfield@lge.com]2010. 9. 8. end

            // [bboguri.kim@lge.com] 2010. 8. 26. start
            ArrayList<ContentProviderOperation> ops =  new ArrayList<ContentProviderOperation>();
            ops.clear();
            int rawId = ops.size();

            ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                        .withValue(RawContacts.ACCOUNT_NAME, ICC_CONTACT_ACCOUNT_NAME)
                        .withValue(RawContacts.ACCOUNT_TYPE, ICC_CONTACT_ACCOUNT_TYPE)
                        .withValue(RawContacts.AGGREGATION_MODE, String.valueOf(RawContacts.AGGREGATION_MODE_DISABLED))
                        .withValue("is_restricted", "99999")
                        .withValue(RawContacts.SOURCE_ID, pe.getIndex1())
                        .build());

            // 2010. 11. 2. park.jongjin@lge.com PhonebookEntry에 있는 한글이 깨지므로 ContentValues에 있는 데이터를 사용 [START_LGE_LAB1]
            if (isKR) {
                if (TextUtils.isEmpty(name) == false) {
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(StructuredName.GIVEN_NAME, name)
                            .withValue(StructuredName.FAMILY_NAME, "")
                            .build());
                }

                if (TextUtils.isEmpty(number) == false) {
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, number)
                            .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                            .withValue(Phone.LABEL, 1)
                            .build());
                }

                if (TextUtils.isEmpty(anrs) == false) {
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, anrs)
                            .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                            .withValue(Phone.LABEL, 2)
                            .build());
                }

                if (TextUtils.isEmpty(emails) == false) {
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                            .withValue(Email.DATA, emails)
                            .withValue(Email.TYPE, Email.TYPE_OTHER)
                            .build());
                }
                // 2010. 11. 2. park.jongjin@lge.com  [END_LGE_LAB1]
            } else {
                if (!pe.getText().equals("") || !pe.getSecondtext().equals("")){
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(StructuredName.GIVEN_NAME, pe.getText())
                            .withValue(StructuredName.FAMILY_NAME, "")
                            .build());
                }

                if (!pe.getNumber().equals("")){
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, pe.getNumber())
                            .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                            .withValue(Phone.LABEL, 1)
                            .build());
                }

                if (!pe.getAdnumber().equals("")){
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, pe.getAdnumber())
                            .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                            .withValue(Phone.LABEL, 2)
                            .build());
                }

                if (!pe.getEmail().equals("")){
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                            .withValue(Email.DATA, pe.getEmail())
                            .withValue(Email.TYPE, Email.TYPE_OTHER)
                            .build());
                }
            }

            ContentProviderResult result = null;
            long rowId = -1;

            try {
                result = getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops)[0];
                rowId = ContentUris.parseId(result.uri);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // [bboguri.kim@lge.com] 2010. 8. 26. end

            if (rowId > 0) {
            // [bboguri.kim@lge.com] 2010. 8. 26. start
//              Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
//                getContext().getContentResolver().notifyChange(noteUri, null);
//               return noteUri;
                return result.uri;
            // [bboguri.kim@lge.com] 2010. 8. 26. end
            }
        }
        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        else if (sUriMatcher.match(uri) == GROUPS) {
//            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//            long rowId = db.insert(GROUPS_TABLE, GROUP, initialValues);
//            if (rowId > 0) {
//                Uri noteUri = ContentUris.withAppendedId(GROUP_CONTENT_URI, rowId);
//                getContext().getContentResolver().notifyChange(noteUri, null);
//                return noteUri;
//            }
//        }
        // [greenfield@lge.com]2010. 9. 8. end
        else {
            return super.insert(uri, initialValues);
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    // [bboguri.kim@lge.com] 2010. 8. 26. start
    public int deleteSimData(Uri url, String where, String[] whereArgs, String rawcontactId) {
        int count = 0;

        if(rawcontactId != null && rawcontactId != "") { // if rawcontact Id exist, delete rawcontact directly.
            String rawContactsSelection = RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE
            + " AND " + RawContacts._ID +" = '" + rawcontactId + "' ";
            count = getContext().getContentResolver().delete(RawContacts.CONTENT_URI,
                                                     rawContactsSelection,null);
            return count;
        }

        count = getContext().getContentResolver().delete(RawContacts.CONTENT_URI,
                where, null);
        return count;

        /*
        StringBuilder selectionBuilder = new StringBuilder();
        cursor = getContext().getContentResolver().query(Data.CONTENT_URI,
                new String[]{Data.RAW_CONTACT_ID}, where, null, null);

            if (cursor.getCount() > 1) {
                selectionBuilder.setLength(0);
                selectionBuilder.append(RawContacts._ID + " IN(");
                int i = 0;
                while (cursor.moveToNext()) {
                    if (i != 0) {
                        selectionBuilder.append(',');
                    }
                    selectionBuilder.append(cursor.getLong(0));
                    i++;
                }
                selectionBuilder.append(')');
                cursor.close();
                cursor = getContext().getContentResolver().query(RawContactsEntity.CONTENT_URI,
                                        null,selectionBuilder.toString(),null, null);
                EntityIterator iterator = RawContacts.newEntityIterator(cursor);
                long rawId ;
                boolean check ;
                while (iterator.hasNext()) {
                    final Entity items = iterator.next();
                    check = false;
                    if (emails == null && anrs == null) {
                        check = true;
                        for (NamedContentValues namedValues : items.getSubValues()) {
                            ContentValues item = namedValues.values;
                            String mimeType = item.getAsString(Data.MIMETYPE);
                            if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                                check = false;
                                break;
                            }
                            else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                                if(!item.containsKey(Phone.TYPE)) {
                                    check = false;
                                    break;
                                }
                            }
                        }
                    }
                    else if (emails != null && anrs != null) {
                        boolean anrsCheck = false;
                        boolean emailCheck = false;
                        check = false;
                        for (NamedContentValues namedValues : items.getSubValues()) {
                            ContentValues item = namedValues.values;
                            String mimeType = item.getAsString(Data.MIMETYPE);
                            if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                                if (emails.equals(item.getAsString(Email.ADDRESS))) {
                                    emailCheck = true;
                                }
                            }
                            else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                                if(!item.containsKey(Phone.TYPE)) {
                                    if (anrs.equals(item.getAsString(Phone.NUMBER))) {
                                        anrsCheck = true;
                                    }
                                }
                            }
                        }
                        if (emailCheck&& anrsCheck) {
                                check = true;
                        }
                    }

                    else if (emails != null) {
                        check = false;
                        for (NamedContentValues namedValues : items.getSubValues()) {
                            ContentValues item = namedValues.values;
                            String mimeType = item.getAsString(Data.MIMETYPE);
                            if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                                if (emails.equals(item.getAsString(Email.ADDRESS))) {
                                    check = true;
                                }
                            }
                            else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                                if(!item.containsKey(Phone.TYPE)) {
                                    check = false;
                                    break;
                                }
                            }
                        }
                    }

                    else {
                        check = false;
                        for (NamedContentValues namedValues : items.getSubValues()) {
                            ContentValues item = namedValues.values;
                            String mimeType = item.getAsString(Data.MIMETYPE);
                            if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                                check = false;
                                break;
                            }
                            else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                                if(!item.containsKey(Phone.TYPE)) {
                                    if(anrs.equals(item.getAsString(Phone.NUMBER))) {
                                        check = true;
                                    }
                                }
                            }
                        }
                    }

                    if (check == true) {
                        rawId = items.getEntityValues().getAsLong(RawContacts._ID);
                        String rawContactsSelection = RawContacts._ID +" = " +rawId;
                        count = getContext().getContentResolver().delete(RawContacts.CONTENT_URI,
                                                          rawContactsSelection,null);
                        break;
                    }
                }
            }

            else {
                if (cursor.moveToFirst()) {
                    String rawContactsSelection = RawContacts._ID +" = " +cursor.getLong(0);
                    count = getContext().getContentResolver().delete(RawContacts.CONTENT_URI,
                                                             rawContactsSelection,null);
                }
            }

        if( cursor != null)
            cursor.close();
        */
    }

    // Sim Index Test
    // 2010. 11. 2. park.jongjin@lge.com use sim index [START_LGE_LAB1]
    public int deleteSimDataByRawContactId(long rawContactId, int simIndex) {
        String where = RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE
                       + "' AND " + RawContacts._ID +" = '" + rawContactId + "' ";
        return getContext().getContentResolver().delete(RawContacts.CONTENT_URI,where,null);
    }
    // 2010. 11. 2. park.jongjin@lge.com  [END_LGE_LAB1]

    public Uri getUriForSIMContactQuery(ContentValues cv)
    {
        String tag = cv.getAsString(TAGNAME);
        String number = cv.getAsString(NUMBER);
        String emails = cv.getAsString(EMAILS);
        String anrs = cv.getAsString(ANRS);

        /*
        selection = RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE +"' AND "
        + Data.MIMETYPE + " = '" +Phone.CONTENT_ITEM_TYPE
        +"' AND " +Phone.NUMBER +" = '" +number +"' AND "
        +Phone.TYPE + " = " +Phone.TYPE_MOBILE +" AND "
        +"display_name = '" +tag +"'";
        */

        Uri mUri = Uri.withAppendedPath(Data.CONTENT_URI, "siminfo").buildUpon()
            .appendQueryParameter("name", TextUtils.isEmpty(tag)? "" : tag)
            .appendQueryParameter("phone1", TextUtils.isEmpty(number)? "" : number)
            .appendQueryParameter("phone2", TextUtils.isEmpty(anrs)? "" : anrs)
            .appendQueryParameter("email", TextUtils.isEmpty(emails)? "" : emails)
            .build();

        return mUri;
//!        return selection;
        // [bboguri.kim@lge.com] 2010. 10. 8. end
    }

    // [bboguri.kim@lge.com] 2010. 10. 8. start  :  find sourceid that match data.
    public Uri getUriForSIMContactQuery(String where) {
        String tag = "";
        String number = "";
        String emails = "";
        String anrs = "";

        String[] tokens = where.split("AND");
        int n = tokens.length;
        while (--n >= 0) {
            String param = tokens[n];
            String[] pair = param.split("=");
            if (pair.length != 2) {
                continue;
            }
            String key = pair[0].trim();
            String val = pair[1].trim();
            if (TAGNAME.equals(key)) {
                tag = normalizeValue(val);
            } else if (NUMBER.equals(key)) {
                number = normalizeValue(val);
            } else if (EMAILS.equals(key)) {
                emails = normalizeValue(val);
            } else if (ANRS.equals(key)) {
                anrs = normalizeValue(val);
            }
        }

        /*
        selection = RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE +"' AND "
        + Data.MIMETYPE + " = '" +Phone.CONTENT_ITEM_TYPE
        +"' AND " +Phone.NUMBER +" = '" +number +"' AND "
        +Phone.TYPE + " = " +Phone.TYPE_MOBILE +" AND "
        +"display_name = '" +tag +"'";
        */

        Uri mUri = Uri.withAppendedPath(Data.CONTENT_URI, "siminfo").buildUpon()
            .appendQueryParameter("name", TextUtils.isEmpty(tag)? "" : tag)
            .appendQueryParameter("phone1", TextUtils.isEmpty(number)? "" : number)
            .appendQueryParameter("phone2", TextUtils.isEmpty(anrs)? "" : anrs)
            .appendQueryParameter("email", TextUtils.isEmpty(emails)? "" : emails)
            .build();

        return mUri;
    }

    public String convertFromPhoneBookSelectoinToContactSelection(String where) {
        String selection = "";
        String first_name = "";
        String number1 = "";
        String number2 = "";
        String email = "";

        String[] tokens = where.split("OR");
        int n = tokens.length;
        while (--n >= 0) {
            String param = tokens[n];
            String[] pair = param.split("=");
            if (pair.length != 2) {
                continue;
            }
            String key = pair[0].trim();
            String val = pair[1].trim();
            if (FIRST_NAME.equals(key)) {
                first_name = normalizeValue(val);
            } else if (NUMBER1.equals(key)) {
                number1 = normalizeValue(val);
            } else if (NUMBER2.equals(key)) {
                number2 = normalizeValue(val);
            } else if (EMAIL.equals(key)) {
                email = normalizeValue(val);
            }
        }

//        selection = RawContacts.ACCOUNT_TYPE  + " =  '" + ICC_CONTACT_ACCOUNT_TYPE + "' "
////            + Data.MIMETYPE + " = '" +Phone.CONTENT_ITEM_TYPE + "' "
//            +" AND (" +Phone.NUMBER +" = '" +number1 +"' OR  " +Phone.NUMBER +" = '" +number2 +"') AND "
//            +Phone.TYPE + " = " +Phone.TYPE_MOBILE;
//            if(!first_name.equals(""))
//                selection += " AND display_name = '" +first_name +"'";
            boolean needortag = false;
            selection = RawContacts.ACCOUNT_TYPE  + " =  '" + ICC_CONTACT_ACCOUNT_TYPE + "' ";

            selection += " AND (";
            if(!first_name.equals("")) {
                selection += " ("
                    + Data.MIMETYPE + " = '" +StructuredName.CONTENT_ITEM_TYPE + "' "
                    + " AND " + StructuredName.DISPLAY_NAME +" = '" +first_name +"'  AND " +  StructuredName.DATA2 + " = " + first_name
                    + ") ";
                needortag = true;
            }

          if(!number1.equals("")) {
              if(needortag)
                  selection += " OR ";
              selection += " ("
                   + Data.MIMETYPE + " = '" +Phone.CONTENT_ITEM_TYPE + "' "
                   + " AND " + Phone.NUMBER +" = '" +number1 +"'  AND " + Phone.TYPE + " = " + Phone.TYPE_MOBILE
                   + ") ";
              needortag = true;
          }

          if(!number2.equals("")) {
              if(needortag)
                  selection += " OR ";
              selection += " ("
                   + Data.MIMETYPE + " = '" +Phone.CONTENT_ITEM_TYPE + "' "
                   + " AND " + Phone.NUMBER +" = '" +number2 +"'  AND " + Phone.TYPE + " = " + Phone.TYPE_MOBILE
                   + ") ";
              needortag = true;
          }

          if(!email.equals("")) {
              if(needortag)
                  selection += " OR ";
              selection += " ("
                   + Data.MIMETYPE + " = '" +Email.CONTENT_ITEM_TYPE + "' "
                   + " AND " + Email.ADDRESS +" = '" + email +"'  AND " + Email.TYPE + " = " + Email.TYPE_OTHER
                   + ") ";
          }

          if(needortag)
              selection += " AND 1 = 1) ";

        return selection;
    }
    // [bboguri.kim@lge.com] 2010. 8. 26. end


    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        // [bboguri.kim@lge.com] 2010. 8. 26. start
        String mSelection = "";
        String mSourceid = "";
        Uri mUri = null;
        Cursor c = null;
        boolean simdeleteresult = false;

        if (!mInitialized)
            return 0;

        // Sim Index Test
        // [bboguri.kim@lge.com] 2010. 8. 26. end

        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        // [greenfield@lge.com]2010. 9. 8. end

        int count = 0;
        PhonebookEntry pe = new PhonebookEntry();
        switch (sUriMatcher.match(uri)) {
        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        case GROUPS:
//            count = db.delete(GROUPS_TABLE, where, whereArgs);
//            ContentValues cvs = new ContentValues();
//            cvs.put(GROUP, "");
//            db.update(CONTACTS_TABLE, cvs, where, whereArgs);
//            break;
        // [greenfield@lge.com]2010. 9. 8. end

        case CONTACTS:
            // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//            Cursor c = query(uri, new String[]{_ID}, where, whereArgs, null);
//            if (mInitialized){
//                while(c.moveToNext()){
//                    sSimMap[(int)c.getLong(0) - 1] = false;
//                    pe.setIndex1((int)c.getLong(0));
//                    write(pe);
//                }
//            }
//            c.close();
//
//            count = db.delete(CONTACTS_TABLE, where, whereArgs);
            // [greenfield@lge.com]2010. 9. 8. end
            // [bboguri.kim@lge.com] 2010. 8. 26. start
//          if (isKR == false) {
            if(TextUtils.isEmpty(where))
                return 0;
//        }

            mUri  = getUriForSIMContactQuery(where);
            c = getContext().getContentResolver().query(mUri, new String[]{ RawContacts.SOURCE_ID }, null, null, null);

            if(c.moveToFirst()){
                    sSimMap[(int)c.getLong(0) - 1] = false;
                    pe.setIndex1((int)c.getLong(0));
                    simdeleteresult =  write(pe);
                    mSourceid = c.getString(0);
                    c.close();

                if (simdeleteresult) {
                    mSelection = RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE
                        + "' AND " + RawContacts.SOURCE_ID + " = '" + mSourceid + "'";
                    count = deleteSimData(uri, mSelection, whereArgs, null);
                }
            }
            // [bboguri.kim@lge.com] 2010. 8. 26. end
            break;

          case CONTACT_ID:
              String rawcontactId = Long.toString(ContentUris.parseId(uri));
              mSelection = RawContacts.CONTACT_ID + "='" + rawcontactId + "' AND " +
                  RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE + "' ";
              c = getContext().getContentResolver().query(Data.CONTENT_URI, new String[]{RawContacts.SOURCE_ID}, mSelection, null, null);
              c.moveToFirst();

              sSimMap[(int)c.getLong(0) - 1] = false;
              pe.setIndex1((int)c.getLong(0));
              simdeleteresult =  write(pe);

              if (simdeleteresult) {
                  count = deleteSimData(uri, mSelection, whereArgs, rawcontactId);
              }
              break;

          // Sim Index Test
          // 2010. 11. 2. park.jongjin@lge.com use sim index [START_LGE_LAB1]
          case SIM_INDEX_RAW_CONTACT_ID: {
              final long rawContactId = ContentUris.parseId(uri);
              final int simIndex = Integer.parseInt(uri.getPathSegments().get(1));

              pe.setIndex1(simIndex);

              if (write(pe)) {
                  sSimMap[simIndex - 1] = false;
                  count = deleteSimDataByRawContactId(rawContactId, simIndex);
              }
              break;
          }
          // 2010. 11. 2. park.jongjin@lge.com use sim index [END_LGE_LAB1]

          // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        case CONTACT_ID:
//            String contactId = uri.getPathSegments().get(1);
//            count = db.delete(CONTACTS_TABLE, _ID + "=" + contactId
//                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
//            if (count != 0 && mInitialized){
//                sSimMap[Integer.valueOf(contactId) - 1] = false;
//                pe.setIndex1(Integer.valueOf(contactId));
//                write(pe);
//            }
//            break;

            // [bboguri.kim@lge.com] 2010. 8. 26. start
//            count = 0;
//            if (!where.equals("alldelete")) {
//                count = getContext().getContentResolver().delete(RawContacts.CONTENT_URI,
//                        RawContacts.ACCOUNT_TYPE + "='" + ICC_CONTACT_ACCOUNT_TYPE + "' AND " +
//                        RawContacts._ID + "=" + contactId +
//                        (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
//                        whereArgs);
//            }
//
//            if (count != 0 && mInitialized){
//                sSimMap[Integer.valueOf(contactId) - 1] = false;
//                pe.setIndex1(Integer.valueOf(contactId));
//                write(pe);
//            }
//
//            if (where.equals("alldelete")) {
//                for (int i = 0; i < sSimMap.length; i++){
//                    sSimMap[i] = false;
//                    pe.setIndex1(i + 1);
//                    write(pe);
//                }
//            }
//            break;
            // [bboguri.kim@lge.com] 2010. 8. 26. end
         // [greenfield@lge.com]2010. 9. 8. end

        default:
            return super.delete(uri, where, whereArgs);
        }
        // [bboguri.kim@lge.com] 2010. 8. 26. start
//        getContext().getContentResolver().notifyChange(uri, null);
        // [bboguri.kim@lge.com] 2010. 8. 26. end
        return count;
    }

    // [bboguri.kim@lge.com] 2010. 8. 26. start
    private String normalizeValue(String inVal) {
        int len = inVal.length();
        String retVal = inVal;
        if (inVal.charAt(0) == '\'' && inVal.charAt(len-1) == '\'') {
            retVal = inVal.substring(1, len-1);
        }
        return retVal;
    }

    public int updateSimData(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int update = 1; // case of  update failure, if fail the update chagne to 0 ~!!
        ArrayList<ContentProviderOperation> ops = Lists.newArrayList();
        String selection = null;
        Cursor cursor = null;
        StringBuilder selectionBuilder = new StringBuilder();

        long rawId = 0;
        String tag = values.getAsString(TAGNAME);
//        String number = values.getAsString(NUMBER);
        String emails = values.getAsString(EMAILS);
        String anrs = values.getAsString(ANRS);

        /*
        selection = RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE +"' AND "
        + Data.MIMETYPE + " = '" +Phone.CONTENT_ITEM_TYPE
        +"' AND " +Phone.NUMBER +" = '" +number +"' AND "
        +Phone.TYPE + " = " +Phone.TYPE_MOBILE +" AND "
        +"display_name = '" +tag +"'";
        */
       selection = where;
       cursor = getContext().getContentResolver().query(RawContacts.CONTENT_URI,
//                new String[]{Data.RAW_CONTACT_ID}, selection, null, null);
               new String[]{RawContacts._ID}, selection, null, null);

        if (cursor.getCount() > 1) {
            selectionBuilder.setLength(0);
            selectionBuilder.append(RawContacts._ID + " IN(");
            int i = 0;
            while (cursor.moveToNext()) {
                if (i != 0) {
                    selectionBuilder.append(',');
                }
                selectionBuilder.append(cursor.getLong(0));
                i++;
            }
            selectionBuilder.append(')');
            cursor.close();
            cursor = getContext().getContentResolver().query(RawContactsEntity.CONTENT_URI,
                                    null,selectionBuilder.toString(),null, null);
            EntityIterator iterator = RawContacts.newEntityIterator(cursor);
            boolean check ;
            while (iterator.hasNext()) {
                final Entity items = iterator.next();
                check = false;
                if (emails == null && anrs == null) {
                    check = true;
                    for (NamedContentValues namedValues : items.getSubValues()) {
                        ContentValues item = namedValues.values;
                        String mimeType = item.getAsString(Data.MIMETYPE);
                        if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                            check = false;
                            break;
                        }
                        else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                            if(!item.containsKey(Phone.TYPE)) {
                                check = false;
                                break;
                            }
                        }
                    }
                }
                else if (emails != null && anrs != null) {
                    boolean anrsCheck = false;
                    boolean emailCheck = false;
                    check = false;
                    for (NamedContentValues namedValues : items.getSubValues()) {
                        ContentValues item = namedValues.values;
                        String mimeType = item.getAsString(Data.MIMETYPE);
                        if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                            if (emails.equals(item.getAsString(Email.ADDRESS))) {
                                emailCheck = true;
                            }
                        }
                        else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                            if(!item.containsKey(Phone.TYPE)) {
                                if (anrs.equals(item.getAsString(Phone.NUMBER))) {
                                    anrsCheck = true;
                                }
                            }
                        }
                    }
                    if (emailCheck&& anrsCheck) {
                            check = true;
                    }
                }

                else if (emails != null) {
                    check = false;
                    for (NamedContentValues namedValues : items.getSubValues()) {
                        ContentValues item = namedValues.values;
                        String mimeType = item.getAsString(Data.MIMETYPE);
                        if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                            if (emails.equals(item.getAsString(Email.ADDRESS))) {
                                check = true;
                            }
                        }
                        else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                            if(!item.containsKey(Phone.TYPE)) {
                                check = false;
                                break;
                            }
                        }
                    }
                }

                else {
                    check = false;
                    for (NamedContentValues namedValues : items.getSubValues()) {
                        ContentValues item = namedValues.values;
                        String mimeType = item.getAsString(Data.MIMETYPE);
                        if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                            check = false;
                            break;
                        }
                        else if(mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                            if(!item.containsKey(Phone.TYPE)) {
                                if(anrs.equals(item.getAsString(Phone.NUMBER))) {
                                    check = true;
                                }
                            }
                        }
                    }
                }

                if (check == true) {
                    rawId = items.getEntityValues().getAsLong(RawContacts._ID);
                    break;
                }
            }
        } else  {
            if (cursor.moveToFirst()) {
                rawId = cursor.getLong(0);
            }
        }

        if (values.getAsString(NEW_TAGNAME) != null) {
            // [bboguri.kim@lge.com] 2010. 11. 3. start : add insert operation for tag data
            if(TextUtils.isEmpty(tag)) {
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(Data.RAW_CONTACT_ID, rawId)
                        .withValue(StructuredName.GIVEN_NAME, values.getAsString(NEW_TAGNAME))
                        .withValue(StructuredName.FAMILY_NAME, "")
                        .build());
              } else {
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID + " = " + rawId +" AND "
                                      +Data.MIMETYPE + " = '"+ StructuredName.CONTENT_ITEM_TYPE +"'"
                                       , null)
                        .withValue(StructuredName.DISPLAY_NAME,
                                   values.getAsString(NEW_TAGNAME)).build());

              }
            // [bboguri.kim@lge.com] 2010. 11. 3. end
        }

        if (values.getAsString(NEW_NUMBER) != null) {
            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + " = " + rawId +" AND "
                                   +Data.MIMETYPE + " = '"+ Phone.CONTENT_ITEM_TYPE + "' AND "
                                   +Phone.TYPE +" = '" +Phone.TYPE_MOBILE + "' AND "
                                   +Phone.LABEL + " = '1' AND "
                                   +Phone.NUMBER + " = '" + values.getAsString(NUMBER)+ "'"
                                   , null)
                    .withValue(Phone.NUMBER,values.getAsString(NEW_NUMBER)).build());
        }
        if (values.getAsString(ANRS) != null) {
            if (values.getAsString(NEW_ANRS) != null) {
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawId +" AND "
                                       +Data.MIMETYPE + " = '"+ Phone.CONTENT_ITEM_TYPE + "' AND "
                                       +Phone.TYPE +" = '" +Phone.TYPE_MOBILE + "' AND "
                                       +Phone.LABEL + " = '2' AND "
                                       +Phone.NUMBER + " = '" + values.getAsString(ANRS) + "'"
                                       , null)
                        .withValue(Phone.NUMBER,values.getAsString(NEW_ANRS)).build());
            }else {
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawId +" AND "
                                       +Data.MIMETYPE + " = '"+ Phone.CONTENT_ITEM_TYPE + "' AND "
                                       +Phone.TYPE +" = '" +Phone.TYPE_MOBILE + "' AND "
                                       +Phone.LABEL + " = '2' AND "
                                       +Phone.NUMBER + " = '" + values.getAsString(ANRS) + "'"
                                       , null).build());
            }
        } else {
            if (values.getAsString(NEW_ANRS) != null) {
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Data.RAW_CONTACT_ID, rawId)
                        .withValue(Phone.NUMBER, values.getAsString(NEW_ANRS))
                        .withValue(Phone.LABEL, "2")
                        .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
            }
        }
        if (values.getAsString(EMAILS) != null) {
            if (values.getAsString(NEW_EMAILS) != null) {
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawId +" AND " +
                                       Data.MIMETYPE + " = '"+ Email.CONTENT_ITEM_TYPE +"'"
                                       , null)
                        .withValue(Email.ADDRESS,values.getAsString(NEW_EMAILS)).build());
            }else {
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawId +" AND " +
                                       Data.MIMETYPE + " = '"+ Email.CONTENT_ITEM_TYPE +"'"
                                       , null).build());
            }
        } else {
            if (values.getAsString(NEW_EMAILS) != null) {
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                        .withValue(Data.RAW_CONTACT_ID, rawId)
                        .withValue(Email.ADDRESS,values.getAsString(NEW_EMAILS)).build());
            }
        }
        try {
            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            update = 0; // case of  update failure
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            update = 0; // case of  update failure
            e.printStackTrace();
        }

        cursor.close();
        return update;
    }
    // [bboguri.kim@lge.com] 2010. 8. 26. end


    // Sim Index Test
    /**
     * 변경사항을 주소록DB에 반영. 내수 only.
     * @param rawContactId
     * @param values
     * @return
     */
    public int updateSimDataByRawContactId(long rawContactId, ContentValues values) {
        ArrayList<ContentProviderOperation> ops = Lists.newArrayList();

        if (values.getAsString(TAGNAME) != null) {
            if (values.getAsString(NEW_TAGNAME) != null) {
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID + " = " + rawContactId +" AND "
                                      +Data.MIMETYPE + " = '"+ StructuredName.CONTENT_ITEM_TYPE +"'"
                                       , null)
                        .withValue(StructuredName.DISPLAY_NAME,
                                   values.getAsString(NEW_TAGNAME)).build());
            } else {
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID + " = " + rawContactId +" AND "
                                      +Data.MIMETYPE + " = '"+ StructuredName.CONTENT_ITEM_TYPE +"'"
                                       , null).build());
            }
        } else {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(StructuredName.DISPLAY_NAME,
                               values.getAsString(NEW_TAGNAME)).build());
        }

        if (values.getAsString(NUMBER) != null) {
            if (values.getAsString(NEW_NUMBER) != null) {
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID + " = " + rawContactId +" AND "
                                       +Data.MIMETYPE + " = '"+ Phone.CONTENT_ITEM_TYPE + "' AND "
                                       +Phone.TYPE +" = '" +Phone.TYPE_MOBILE + "' AND "
                                       +Phone.LABEL + " = '1' AND "
                                       +Phone.NUMBER + " = '" + values.getAsString(NUMBER)+ "'"
                                       , null)
                        .withValue(Phone.NUMBER,values.getAsString(NEW_NUMBER)).build());
            } else {
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID + " = " + rawContactId +" AND "
                                       +Data.MIMETYPE + " = '"+ Phone.CONTENT_ITEM_TYPE + "' AND "
                                       +Phone.TYPE +" = '" +Phone.TYPE_MOBILE + "' AND "
                                       +Phone.LABEL + " = '1' AND "
                                       +Phone.NUMBER + " = '" + values.getAsString(NUMBER)+ "'"
                                       , null).build());
            }
        } else {
            if (values.getAsString(NEW_NUMBER) != null) {
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Phone.NUMBER, values.getAsString(NEW_NUMBER))
                        .withValue(Phone.LABEL, "1")
                        .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
            }
        }

        if (values.getAsString(ANRS) != null) {
            if (values.getAsString(NEW_ANRS) != null) {
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawContactId +" AND "
                                       +Data.MIMETYPE + " = '"+ Phone.CONTENT_ITEM_TYPE + "' AND "
                                       +Phone.TYPE +" = '" +Phone.TYPE_MOBILE + "' AND "
                                       +Phone.LABEL + " = '2' AND "
                                       +Phone.NUMBER + " = '" + values.getAsString(ANRS) + "'"
                                       , null)
                        .withValue(Phone.NUMBER,values.getAsString(NEW_ANRS)).build());
            }else {
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawContactId +" AND "
                                       +Data.MIMETYPE + " = '"+ Phone.CONTENT_ITEM_TYPE + "' AND "
                                       +Phone.TYPE +" = '" +Phone.TYPE_MOBILE + "' AND "
                                       +Phone.LABEL + " = '2' AND "
                                       +Phone.NUMBER + " = '" + values.getAsString(ANRS) + "'"
                                       , null).build());
            }
        } else {
            if (values.getAsString(NEW_ANRS) != null) {
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Phone.NUMBER, values.getAsString(NEW_ANRS))
                        .withValue(Phone.LABEL, "2")
                        .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
            }
        }

        if (values.getAsString(EMAILS) != null) {
            if (values.getAsString(NEW_EMAILS) != null) {
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawContactId +" AND " +
                                       Data.MIMETYPE + " = '"+ Email.CONTENT_ITEM_TYPE +"'"
                                       , null)
                        .withValue(Email.ADDRESS,values.getAsString(NEW_EMAILS)).build());
            }else {
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID+ " = " + rawContactId +" AND " +
                                       Data.MIMETYPE + " = '"+ Email.CONTENT_ITEM_TYPE +"'"
                                       , null).build());
            }
        } else {
            if (values.getAsString(NEW_EMAILS) != null) {
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Email.ADDRESS,values.getAsString(NEW_EMAILS)).build());
            }
        }

        try {
            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // case of  update failure
        }

        return 1; // update success
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        // [greenfield@lge.com]2010. 9. 8. end
        int count = 0;

        String mSelection = "";
        Uri mUri = null;
        String msourceid = "";

        switch (sUriMatcher.match(uri)) {
        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        case GROUPS:
//            count = db.update(GROUPS_TABLE, values, where, whereArgs);
//            if (!values.containsKey(GROUP)){
//                getContext().getContentResolver().notifyChange(uri, null);
//                return count;
//            }
//            //Fall thru to update groups on sim card
//            mUri = CONTENT_URI;
        // [greenfield@lge.com]2010. 9. 8. end
        case CONTACTS:

            // Sim Index Test
            // 2010. 11. 1. park.jongjin@lge.com use sim index [START_LGE_LAB1]
//            if (isKR) {
            int simIndex = values.getAsInteger(SIM_INDEX);
            if(simIndex > 0) {
                //if (simIndex < 1) return 0;
                PhonebookEntry pe = new PhonebookEntry();
                pe.setIndex1(simIndex);

                String name = values.getAsString(NEW_TAGNAME);
                String number = values.getAsString(NEW_NUMBER);
                String anrs = values.getAsString(NEW_ANRS);
                String emails = values.getAsString(NEW_EMAILS);

                if (name != null){
                    pe.setText(name);
                }
                if (number != null){
                    pe.setNumber(number);
                    pe.setType(detectType(number));
                }
                if (anrs != null){
                    pe.setAdnumber(anrs);
                    pe.setAdtype(detectType(anrs));
                }
                if (emails != null){
                    pe.setEmail(emails);
                }

                if (write(pe)) {
                    long rawContactId = values.getAsLong(RAW_CONTACT_ID);
                    count = updateSimDataByRawContactId(rawContactId, values);
                }

                return count;
            } else {
                // 2010. 11. 1. park.jongjin@lge.com  [END_LGE_LAB1]

                // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
    //            selection = where;
                // [greenfield@lge.com]2010. 9. 8. end
                // [greenfield@lge.com]2010. 9. 1. modified reason : IFX SIM Update Code
                mUri = getUriForSIMContactQuery(values);

                Cursor c = getContext().getContentResolver().query(mUri, new String[]{RawContacts.SOURCE_ID}, null, null, null);
                PhonebookEntry pe = null;

                if(c.moveToFirst()) {
                    pe = new PhonebookEntry();
                    pe.setIndex1((int)c.getLong(0));
                    msourceid = c.getString(0);

                    String name = values.getAsString(NEW_TAGNAME);
                    String number = values.getAsString(NEW_NUMBER);
                    String anrs = values.getAsString(NEW_ANRS);
                    String emails = values.getAsString(NEW_EMAILS);

                    if (name != null) {
                        pe.setText(name);
                    } else {
                        pe.setText("");
                    }

                    if (number != null) {
                        pe.setNumber(number);
                        pe.setType(detectType(number));
                    } else {
                        pe.setNumber("");
                        pe.setType(detectType(""));
                    }

                    if (anrs != null) {
                        pe.setAdnumber(anrs);
                        pe.setAdtype(detectType(anrs));
                    } else {
                        pe.setAdnumber("");
                        pe.setAdtype(detectType(""));
                    }

                    if (emails != null){
                        pe.setEmail(emails);
                    }else{
                        pe.setEmail("");
                    }

                    pe.setSecondtext("");
                    pe.setGroup("");

                    if(c != null)
                        c.close();

                    if(write(pe)){
                        mSelection = RawContacts.ACCOUNT_TYPE +" = '" + ICC_CONTACT_ACCOUNT_TYPE
                        // [bboguri.kim@lge.com] 2010. 11. 2. start : add selection string
                        + "' AND " + RawContacts.SOURCE_ID + " = '" + msourceid + "'  AND deleted = 0";
                        // [bboguri.kim@lge.com] 2010. 11. 2. end
                        count = updateSimData(uri, values, mSelection, whereArgs);
                    }
                }
            }
            // [greenfield@lge.com]2010. 9. 1. end
                break;

        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        case CONTACT_ID:
//            String contactId = uri.getPathSegments().get(1);
//            selection = _ID + "=" + contactId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
//            break;
        // [greenfield@lge.com]2010. 9. 8. end

            default:
                return super.update(uri, values, where, whereArgs);
        }
        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        Cursor c = query(mUri, null, selection, whereArgs, null);
//        while(c.moveToNext()){
//            PhonebookEntry pe = new PhonebookEntry();
//            pe.setIndex1((int)c.getLong(c.getColumnIndex(_ID)));
//
//            if (values.getAsString(FIRST_NAME) != null){
//                pe.setText(values.getAsString(FIRST_NAME));
//            }else{
//                pe.setText(getColumn(c, FIRST_NAME));
//            }
//            if (values.getAsString(LAST_NAME) != null){
//                pe.setSecondtext(values.getAsString(LAST_NAME));
//            }else{
//                pe.setSecondtext(getColumn(c, LAST_NAME));
//            }
//            if (values.getAsString(NUMBER1) != null){
//                pe.setNumber(values.getAsString(NUMBER1));
//                pe.setType(detectType(values.getAsString(NUMBER1)));
//            }else{
//                pe.setNumber(getColumn(c, NUMBER1));
//                pe.setType(detectType(getColumn(c, NUMBER1)));
//            }
//            if (values.getAsString(NUMBER2) != null){
//                pe.setAdnumber(values.getAsString(NUMBER2));
//                pe.setAdtype(detectType(values.getAsString(NUMBER2)));
//            }else{
//                pe.setAdnumber(getColumn(c, NUMBER2));
//                pe.setAdtype(detectType(getColumn(c, NUMBER2)));
//            }
//            if (values.getAsString(EMAIL) != null){
//                pe.setEmail(values.getAsString(EMAIL));
//            }else{
//                pe.setEmail(getColumn(c, EMAIL));
//            }
//            if (values.getAsString(GROUP) != null){
//                pe.setGroup(values.getAsString(GROUP));
//            }else{
//                pe.setGroup(getColumn(c, GROUP));
//            }
//            if (!write(pe)){
//                c.close();
//                throw new SQLException("Failed to update contact");
//            }
//        }
//        c.close();
//
//        count = db.update(CONTACTS_TABLE, values, selection, whereArgs);
//        getContext().getContentResolver().notifyChange(uri, null);
        // [greenfield@lge.com]2010. 9. 8. end
        return count;
    }

    // [bboguri.kim@lge.com] 2010. 11. 6. start - modified reason : Unused code
//    private String getColumn(Cursor c, String name){
//        // [bboguri.kim@lge.com] 2010. 8. 26. start
//        String value = null;
//
//        c.moveToFirst();
//
//        do {
//            if (name.equals(TAGNAME)) {
//                if (c.getString(c.getColumnIndex(Data.MIMETYPE)).equals(StructuredName.CONTENT_ITEM_TYPE)) {
//                    value = c.getString(c.getColumnIndex(StructuredName.GIVEN_NAME));
//                }
//            } else if (name.equals(NUMBER)) {
//                if (c.getString(c.getColumnIndex(Data.MIMETYPE)).equals(Phone.CONTENT_ITEM_TYPE) &&
//                        c.getString(c.getColumnIndex(Phone.LABEL)).equals("1")) {
//                    value = c.getString(c.getColumnIndex(Phone.NUMBER));
//                }
//            } else if (name.equals(ANRS)) {
//                if (c.getString(c.getColumnIndex(Data.MIMETYPE)).equals(Phone.CONTENT_ITEM_TYPE) &&
//                        c.getString(c.getColumnIndex(Phone.LABEL)).equals("2")) {
//                    value = c.getString(c.getColumnIndex(Phone.NUMBER));
//                }
//            } else if (name.equals(EMAILS)) {
//                if (c.getString(c.getColumnIndex(Data.MIMETYPE)).equals(Email.CONTENT_ITEM_TYPE)) {
//                    value = c.getString(c.getColumnIndex(Email.DATA));
//                }
//            }
//        } while (c.moveToNext());
//
//        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
////        String value = c.getString(c.getColumnIndex(name));
//        // [greenfield@lge.com]2010. 9. 8. end
//        // [bboguri.kim@lge.com] 2010. 8. 26. end
//
//        return value == null ? "" : value;
//    }
    // [bboguri.kim@lge.com] 2010. 11. 6. end

    private boolean write(PhonebookEntry pe){
        IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                        ServiceManager.getService("simphonebook"));
        try {
            if (iccIpb != null) {
                return iccIpb.writePhonebookEntry(pe);
            }
        } catch (RemoteException ex) {
            return false;
        }
        return false;
    }

    // [bboguri.kim@lge.com] 2010. 11. 5. start
    public static String convertValidSeparators(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if(c == 'p') {
                ret.append(',');
            }  else if(c == 'w') {
                ret.append(';');
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }
    // [bboguri.kim@lge.com] 2010. 11. 5. end

    // [bboguri.kim@lge.com] 2010. 11. 6. start
//    private ContentValues getValues(PhonebookEntry pe){
//        ContentValues cvs = new ContentValues();
//        cvs.put(_ID, pe.getIndex1());
//
//        if (!pe.getText().equals("")){
//            cvs.put(TAGNAME, pe.getText());
//        }
//        if (!pe.getNumber().equals("")){
//            cvs.put(NUMBER, pe.getNumber());
//        }
//        if (!pe.getAdnumber().equals("")){
//            cvs.put(ANRS, pe.getAdnumber());
//        }
//        if (!pe.getEmail().equals("")){
//            cvs.put(EMAILS, pe.getEmail());
//
//        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        if (!pe.getText().equals("")){
//            cvs.put(FIRST_NAME, pe.getText());
//        }
//        if (!pe.getNumber().equals("")){
//            cvs.put(NUMBER1, pe.getNumber());
//        }
//        if (!pe.getAdnumber().equals("")){
//            cvs.put(NUMBER2, pe.getAdnumber());
//        }
//        if (!pe.getSecondtext().equals("")){
//            cvs.put(LAST_NAME, pe.getSecondtext());
//        }
//        if (!pe.getGroup().equals("")){
//            cvs.put(GROUP, pe.getGroup());
//        }
//        if (!pe.getEmail().equals("")){
//            cvs.put(EMAIL, pe.getEmail());
//        }
//        // [greenfield@lge.com]2010. 9. 8. end
//        }
//        return cvs;
//    }
    // [bboguri.kim@lge.com] 2010. 11. 6. end


    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//    private void addBinds(ArrayList binds, PhonebookEntry pe){
//        binds.add(new Long(pe.getIndex1()));
//        binds.add(pe.getText());
//        binds.add(pe.getSecondtext());
//        binds.add(pe.getNumber());
//        binds.add(pe.getAdnumber());
//        binds.add(pe.getEmail());
//        binds.add(pe.getGroup());
//    }
    // [greenfield@lge.com]2010. 9. 8. end

    // [bboguri.kim@lge.com] 2010. 8. 26. start :
    public void resetSimData() {
        Uri baseUri = Uri.parse("content://" + ContactsContract.AUTHORITY + "/_account_delete_");
        Uri resetAccountUri = baseUri.buildUpon()
                    .appendQueryParameter(RawContacts.ACCOUNT_NAME, ICC_CONTACT_ACCOUNT_NAME)
                    .appendQueryParameter(RawContacts.ACCOUNT_TYPE, ICC_CONTACT_ACCOUNT_TYPE)
                    .build();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation
                .newDelete(resetAccountUri)
                .build());

        try {
            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // [bboguri.kim@lge.com] 2010. 8. 26. end

    // [bboguri.kim@lge.com] 2010. 8. 20. [] start : prepaerate data for rowcontacts table
    private void addBinds( ArrayList<ContentProviderOperation> ops,
            PhonebookEntry  pe) {
        int rawId = ops.size();

        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_NAME, ICC_CONTACT_ACCOUNT_NAME)
                    .withValue(RawContacts.ACCOUNT_TYPE, ICC_CONTACT_ACCOUNT_TYPE)
                    .withValue(RawContacts.AGGREGATION_MODE, String.valueOf(RawContacts.AGGREGATION_MODE_DISABLED))
                    .withValue("is_restricted", "99999")
                    .withValue(RawContacts.SOURCE_ID, pe.getIndex1())
                    .build());

        if (!pe.getText().equals("") || !pe.getSecondtext().equals("")){
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.GIVEN_NAME, pe.getText())
                    .withValue(StructuredName.FAMILY_NAME, "")
                    .build());
        }

        if (!pe.getNumber().equals("")){
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, pe.getNumber())
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .withValue(Phone.LABEL, 1)
                    .build());
        }

        if (!pe.getAdnumber().equals("")){
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, pe.getAdnumber())
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .withValue(Phone.LABEL, 2)
                    .build());
        }

        if (!pe.getEmail().equals("")){
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawId)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.DATA, pe.getEmail())
                    .withValue(Email.TYPE, Email.TYPE_OTHER)
                    .build());
        }

        // [bboguri.kim@lge.com] 2010. 9. 14. start : Unused code - for init function performance
//        try {
//            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
//        } catch (Exception e) {
//
//        }
    }
    // [bboguri.kim@lge.com] 2010. 8. 20. [] end

    // [bboguri.kim@lge.com] 2010.10. 5. start
    private final BroadcastReceiver simReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,  "IccProvider.java_enclosing_method >> received quest signal from contacts~!!!!!!!!!!!!!!!!!!!");
            String action = intent.getAction();
             if (action.equals(ACTION_REFRESH_SIMSEARCH_REQUEST)) {
                 Intent myIntent = new Intent(ACTION_REFRESH_SIMSEARCH_STATUS);
                 myIntent.putExtra("mInitialized", mInitialized);
                 getContext().sendBroadcast(myIntent);

             }
        }
    };
    // [bboguri.kim@lge.com] 2010.10. 5. end

    // [bboguri.kim@lge.com] 2010. 10. 13. start : Read ths sim visibale setting from db
    private void settingSimContactsVisible() {
        Uri mSettingsUri = ContactsContract.Settings.CONTENT_URI;
        String[] SETTINGS_PROJECTION = new String[] {
                Settings.ACCOUNT_TYPE,
                Settings.UNGROUPED_VISIBLE,
                Settings.ACCOUNT_NAME,
        };
        String mQuerySelection = ContactsContract.Settings.ACCOUNT_TYPE + " = '" + ICC_CONTACT_ACCOUNT_TYPE +"'";
        // sim Account data check
        Cursor settingsCursor = getContext().getContentResolver().query(mSettingsUri, SETTINGS_PROJECTION,
                mQuerySelection, null, null);

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        // if not present sim account data in setting table,than insert sim account data.
        if ( settingsCursor == null || settingsCursor.getCount() == 0 ) {
            Log.w(TAG,"#####  settingsCursor is null ");
            ops.add(ContentProviderOperation
                    .newInsert(Settings.CONTENT_URI)
                    .withValue(Settings.UNGROUPED_VISIBLE, 1)
                    .withValue(Settings.ACCOUNT_NAME, ICC_CONTACT_ACCOUNT_NAME)
                    .withValue(Settings.ACCOUNT_TYPE, ICC_CONTACT_ACCOUNT_TYPE)
                    .withValue(Settings.SHOULD_SYNC, 1)
                    .build());
        }
        try {
            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // [bboguri.kim@lge.com] 2010. 10. 13. end

    private Cursor init(){
        // [bboguri.kim@lge.com] 2010. 10. 13. start : Read ths sim visibale setting from db
        settingSimContactsVisible();
        // [bboguri.kim@lge.com] 2010. 10. 13. end

        synchronized(mutex){
            new Thread(new Runnable() {
                public void run() {
                    mInitialized = false;
                    // [bboguri.kim@lge.com] 2010. 8. 26. start
        //            delete(CONTENT_URI, null, null);
                    // [bboguri.kim@lge.com] 2010. 8. 26. end
                    resetSimData();

                    int total = getTotalRecords();
                    if (total == 0)  return;

                    /*
                     * Let use INSERT SELECT UNION ALL statement instead of usual INSERT to improve initialization perfomance
                     */
                    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
        //            final String insert_start= "INSERT INTO " + CONTACTS_TABLE + " ("
        //                    + _ID + ","
        //                    + FIRST_NAME + ","
        //                    + LAST_NAME + ","
        //                    + NUMBER1 + ","
        //                    + NUMBER2 + ","
        //                    + EMAIL + ","
        //                    + GROUP + ")";
        //            final String insert_mask = " SELECT ?,?,?,?,?,?,? UNION ALL";
        //            String insert_string = "" + insert_start;
        //            ArrayList binds = new ArrayList();
                    // [greenfield@lge.com]2010. 9. 8. end

                    // [heewogi.son@lge.com] 2010. 9. 7. support a applyBatch 50ea.
                    ArrayList<ContentProviderOperation> ops =  new ArrayList<ContentProviderOperation>();
                    final int MAX = 450;
                    // [heewogi.son@lge.com] 2010. 9. 7. end

                    sSimMap = new boolean[total];
                    IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                                    ServiceManager.getService("simphonebook"));
                    PhonebookEntry pe[] = null;
                    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
        //            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                    // [greenfield@lge.com]2010. 9. 8. end

                    boolean mCachingStopped = false;

                    for (int j = 1; j <= total; j++){
                        try {
                            if (iccIpb != null) {
                                pe = iccIpb.readPhonebookEntry(j, j);
                                // [bboguri.kim@lge.com]2010. 10. 30.  :  apply batch test code
        //                        pe = testReadTempPhonebookEntry(j);
                            }

                            if (pe == null){
                                sSimMap[j - 1] = true; // Problem entry let's don't use it
                                continue;
                            }
                        } catch (RemoteException ex) {
                            sSimMap[j - 1] = true;  // Problem entry let's don't use it
                            continue;
                        }

                        for (int i = 0; i < pe.length; i++){
                            if (pe[i] != null){
                                if (!"".equals(pe[i].getNumber()) || !"".equals(pe[i].getText())){
                                    // [bboguri.kim@lge.com] 2010. 8. 20. start
                                    // [bboguri.kim@lge.com] 2010. 11. 5. start : input-possible separator
                                    pe[i].setNumber(convertValidSeparators(pe[i].getNumber()));
                                    pe[i].setAdnumber(convertValidSeparators(pe[i].getAdnumber()));
                                    // [bboguri.kim@lge.com] 2010. 11. 5. end
                                    addBinds(ops, pe[i]);
                                    // [bboguri.kim@lge.com] 2010. 8. 20. end
                                    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
        //                            insert_string += insert_mask;
        //                            addBinds(binds, pe[i]);
                                    /* Intermediate data drop every 100 contacts */
                                    /* This if statement can be fully removed for the best perfomance */
                                    /* But in this case insert_string + binds can eat up to 200k RAM. Be care */
                                    // [bboguri.kim@lge.com]2010. 11. 1. update code : update apply batch code & use thread
                                        try {
                                            if(ops.size() > MAX) {  // support a applyBatch 100ea
                                                if (!ops.isEmpty() && !mCachingStopped) {
                                                    getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                                                    Log.d(TAG,"final applyBatched!!");
                                                    ops.clear();
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "[init] Insertion fail");
                                        }

        //                                insert_string = insert_string.substring(0, insert_string.length()-10); //remove last UNION ALL
        //                                try{
        //                                    db.execSQL(insert_string, binds.toArray());
        //                                }catch(SQLException e){
        //                                    e.printStackTrace();
        //                                }
        //                                insert_string = "" + insert_start;
        //                                binds = new ArrayList();
                                        // [greenfield@lge.com]2010. 8. 31. start
        ////!                                getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        //                                // [greenfield@lge.com]2010. 8. 31. end
        //                            }
                                    // [greenfield@lge.com]2010. 9. 8. end
                                    /* long rowId = db.insert(CONTACTS_TABLE, FIRST_NAME, getValues(pe[i]));
                                    if (rowId > 0) {
                                        final Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                                        getContext().getContentResolver().notifyChange(noteUri, null);
                                    } */
                                    sSimMap[pe[i].getIndex1() - 1] = true;
                                } else {
                                    sSimMap[pe[i].getIndex1() - 1] = false;
                                }
                            }
                        }
                    }
                    // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
        //            /* Final data drop into table */
        //            if (binds.size() > 0){
        //                insert_string = insert_string.substring(0, insert_string.length()-10); //remove last UNION ALL
        //                try{
        //                    db.execSQL(insert_string, binds.toArray());
        //                }catch(SQLException e){
        //                    e.printStackTrace();
        //                }
        //
        //                // [greenfield@lge.com]2010. 8. 31. start
        ////!                getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        //                // [greenfield@lge.com]2010. 8. 31. end
        //            }
                    // [greenfield@lge.com]2010. 9. 8. end

                 // [heewogi.son@lge.com] 2010. 9. 7. support a applyBatch 50ea.
                    try {
                        if (!ops.isEmpty() && !mCachingStopped) {
                            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                            Log.d(TAG,"final applyBatched!!");
                            ops.clear();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[insertADNToDB] Insertion fail");
                    }

                    mInitialized = true;

                // [bboguri.kim@lge.com] 2010. 10. 6 start : communicate with ContactListAcitivity
                Log.w("TESTSIMLOG","INTENTSTART");
                Intent myIntent = new Intent(ACTION_REFRESH_SIMSEARCH_STATUS);
                myIntent.putExtra("mInitialized", mInitialized);
                myIntent.putExtra("mIsFirstToast", mInitialized);
                Log.w("TESTSIMLOG","mInitialized= "+ mInitialized);
                getContext().sendBroadcast(myIntent);
                Log.w("TESTSIMLOG","INTENTEND");
                // [bboguri.kim@lge.com] 2010. 10. 6 end
                }
            }, "Iccprovider init").start();
        }
        return null;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        sUriMatcher.addURI("icc", "joinedphonebook", JOINED);
//        sUriMatcher.addURI("icc", "joinedphonebook/filter/*", JOINED_FILTER);
        // [greenfield@lge.com]2010. 9. 8. end
        // [bboguri.kim@lge.com] 2010. 8. 20.
        sUriMatcher.addURI("icc", "adn", CONTACTS);
        // [bboguri.kim@lge.com] 2010. 8. 20. end
        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
        sUriMatcher.addURI("icc", "phonebook", CONTACTS);
        sUriMatcher.addURI("icc", "phonebook/#", CONTACT_ID);
//        sUriMatcher.addURI("icc", "groups", GROUPS);
//        sUriMatcher.addURI("icc", "phones", PHONES);
        // [greenfield@lge.com]2010. 9. 8. end
        sUriMatcher.addURI("icc", "init", INIT);
        // Sim Index Test
        sUriMatcher.addURI("icc", "adn/*/#", SIM_INDEX_RAW_CONTACT_ID);

        // [greenfield@lge.com]2010. 9. 8. modified reason : Unused code
//        sContactsProjectionMap = new HashMap<String, String>();
//        sContactsProjectionMap.put(_ID, _ID);
//        sContactsProjectionMap.put(FIRST_NAME, FIRST_NAME);
//        sContactsProjectionMap.put(LAST_NAME, LAST_NAME);
//        sContactsProjectionMap.put(NUMBER1, NUMBER1);
//        sContactsProjectionMap.put(NUMBER2, NUMBER2);
//        sContactsProjectionMap.put(EMAIL, EMAIL);
//        sContactsProjectionMap.put(GROUP, GROUP);
//        sContactsProjectionMap.put(Contacts.DISPLAY_NAME_ALTERNATIVE, "NULL AS " + Contacts.DISPLAY_NAME_ALTERNATIVE);
//        sContactsProjectionMap.put(Contacts.PHONETIC_NAME, "NULL AS " + Contacts.PHONETIC_NAME);
//        sContactsProjectionMap.put(Contacts.SORT_KEY_PRIMARY, "NULL AS " + Contacts.SORT_KEY_PRIMARY);
//        sContactsProjectionMap.put(Contacts.DISPLAY_NAME, DISPLAY_NAME_SQL + " AS " + Contacts.DISPLAY_NAME);
//        sContactsProjectionMap.put(Contacts.STARRED, "0 AS " + Contacts.STARRED);
//        sContactsProjectionMap.put(Contacts.TIMES_CONTACTED, "NULL AS " + Contacts.TIMES_CONTACTED);
//        sContactsProjectionMap.put(Contacts.PHOTO_ID, "NULL AS " + Contacts.PHOTO_ID);
//        sContactsProjectionMap.put(Contacts.CONTACT_PRESENCE, "NULL AS " + Contacts.CONTACT_PRESENCE);
//        sContactsProjectionMap.put(Contacts.LOOKUP_KEY, "NULL AS " + Contacts.LOOKUP_KEY);
//        sContactsProjectionMap.put(Contacts.HAS_PHONE_NUMBER, Contacts.HAS_PHONE_NUMBER);
//        sContactsProjectionMap.put(HAS_EMAIL_ADDRESS, HAS_EMAIL_ADDRESS);
//        sContactsProjectionMap.put(VISIBLE, VISIBLE);
//        sContactsProjectionMap.put(SIM_CONTACT, "1 AS " + SIM_CONTACT);
//
//        sPhone1ProjectionMap = new HashMap<String, String>();
//        sPhone1ProjectionMap.put(_ID, _ID);
//        sPhone1ProjectionMap.put(Contacts.DISPLAY_NAME, DISPLAY_NAME_SQL + " AS " + Contacts.DISPLAY_NAME);
//        sPhone1ProjectionMap.put(Phone.NUMBER, NUMBER1 + " AS " + Phone.NUMBER);
//        sPhone1ProjectionMap.put(Phone.TYPE, Phone.TYPE_OTHER + " AS " + Phone.TYPE);
//        sPhone1ProjectionMap.put(Phone.LABEL, "NULL AS " + Phone.LABEL);
//        sPhone1ProjectionMap.put(Phone.CONTACT_ID, _ID + " AS "+ Phone.CONTACT_ID);
//
//        sGroupsProjectionMap = new HashMap<String, String>();
//        sGroupsProjectionMap.put(_ID, _ID);
//        sGroupsProjectionMap.put(GROUP, GROUP);
//        sGroupsProjectionMap.put(VISIBLE, VISIBLE);
        // [greenfield@lge.com]2010. 9. 8. end
  }
// LGE_PHONEBOOK_EXTENSION END
}

