package com.android.phone;

import android.content.ContentResolver;
import android.database.AbstractCursor;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

public class IccContactCursor extends AbstractCursor {
    private Cursor mCursor = null;

    public  IccContactCursor(Cursor c)  {
        super();
        mCursor = c;
        mCursor.moveToFirst();
    }

    public void close() {
        mCursor.close();
    }

    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        mCursor.copyStringToBuffer(columnIndex, buffer);
    }

    public void deactivate() {
        mCursor.deactivate();
    }

    public byte[] getBlob(int columnIndex) {
        return mCursor.getBlob(columnIndex);
    }

    public int getColumnCount() {
        return mCursor.getColumnCount();
    }

    public int getColumnIndex(String columnName) {
        return mCursor.getColumnIndex(columnName);
    }

    public int getColumnIndexOrThrow(String columnName)
            throws IllegalArgumentException {
        return mCursor.getColumnIndexOrThrow(columnName);
    }

    public String getColumnName(int columnIndex) {
        return mCursor.getColumnName(columnIndex);
    }

    public String[] getColumnNames() {
        return mCursor.getColumnNames();
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public double getDouble(int columnIndex) {
        return mCursor.getDouble(columnIndex);
    }

    public Bundle getExtras() {
        // TODO Auto-generated method stub
        return mCursor.getExtras();
    }

    public float getFloat(int columnIndex) {
        // TODO Auto-generated method stub
        return mCursor.getFloat(columnIndex);
    }

    public int getInt(int columnIndex) {
        // TODO Auto-generated method stub
        return mCursor.getInt(columnIndex);
    }

    public long getLong(int columnIndex) {
        // TODO Auto-generated method stub
        return mCursor.getLong(columnIndex);
    }
    public short getShort(int columnIndex) {
        // TODO Auto-generated method stub
        return mCursor.getShort(columnIndex);
    }

    public String getString(int columnIndex) {
        // TODO Auto-generated method stub
        return mCursor.getString(columnIndex);
    }

    public boolean getWantsAllOnMoveCalls() {
        // TODO Auto-generated method stub
        return mCursor.getWantsAllOnMoveCalls();
    }

    public boolean isClosed() {
        // TODO Auto-generated method stub
        return mCursor.isClosed();
    }

    public boolean isNull(int columnIndex) {
        // TODO Auto-generated method stub
        return mCursor.isNull(columnIndex);
    }

    public void registerContentObserver(ContentObserver observer) {
        // TODO Auto-generated method stub
        mCursor.registerContentObserver(observer);
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub
        mCursor.registerDataSetObserver(observer);
    }

    public boolean requery() {
        // TODO Auto-generated method stub
        return mCursor.requery();
    }

    public Bundle respond(Bundle extras) {
        // TODO Auto-generated method stub
        return mCursor.respond(extras);
    }

    public void setNotificationUri(ContentResolver cr, Uri uri) {
        // TODO Auto-generated method stub
        mCursor.setNotificationUri(cr, uri);
    }

    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub
        mCursor.unregisterDataSetObserver(observer);
    }
}
