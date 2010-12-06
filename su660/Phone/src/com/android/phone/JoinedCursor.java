// LGE_PHONEBOOK_EXTENSION START
package com.android.phone;

import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.provider.BaseColumns;
import java.util.*;

import android.util.Log;

/**
 * @author arybalki
 *
 */

class JoinedCursor extends AbstractCursor {

    private Cursor mPrimary;

    private Cursor mSecondary;

    private String[] mProjection;

    private int mTotal;

    private int mPrimaryCount;

    private Cursor mCurrent;

    private Cursor[] mCursorsArray;
    private int[] mIndexesArray;

    private static final class NameEntry{
        String str;

        NameEntry(String str){
            this.str = str;
        }
    }

    private final class CaseInsensitiveComparator implements
            Comparator<NameEntry> {

        /**
         * Compare the two objects to determine the relative ordering.
         *
         * @param o1
         *            an Object to compare
         * @param o2
         *            an Object to compare
         * @return an int < 0 if object1 is less than object2, 0 if they are
         *         equal, and > 0 if object1 is greater
         *
         * @exception ClassCastException
         *                if objects are not the correct type
         */
        public int compare(NameEntry o1, NameEntry o2) {
            if (o1.str == o2.str){ //this check is needed if both of them - null;
                return 0;
            }
            if (o1.str == null){ //null is less then any other value;
                return -1;
            }
            if (o2.str == null){ //null is less then any other value;
                return 1;
            }
            return o1.str.compareToIgnoreCase(o2.str);
        }
    }

    public JoinedCursor(Cursor primary, Cursor secondary, String[] projection){
        super();
        mPrimary = primary;
        mSecondary = secondary;
        mProjection = projection;

        init();
    }

    private void init(){
        mPrimaryCount = mPrimary.getCount();
        int secondaryCount = mSecondary.getCount();
        mTotal = mPrimaryCount + secondaryCount;

        mCursorsArray = new Cursor[mTotal];
        mIndexesArray = new int[mTotal];

        ArrayList sortArr = new ArrayList(mTotal);
        HashMap<NameEntry, Integer> primaryMap = new HashMap<NameEntry, Integer>(mPrimaryCount);
        HashMap<NameEntry, Integer> secondaryMap = new HashMap<NameEntry, Integer>(secondaryCount);

        int index = mPrimary.getColumnIndex("display_name");

        while(mPrimary.moveToNext()){
            NameEntry ne = new NameEntry(mPrimary.getString(index));
            sortArr.add(ne);
            primaryMap.put(ne, mPrimary.getPosition());
        }
        while(mSecondary.moveToNext()){
            NameEntry ne = new NameEntry(mSecondary.getString(index));
            sortArr.add(ne);
            secondaryMap.put(ne, mSecondary.getPosition());
        }
        Collections.sort(sortArr, new CaseInsensitiveComparator());

        int i = 0;
        for(Object ne : sortArr){
            if (primaryMap.containsKey(ne)){
                mCursorsArray[i] = mPrimary;
                mIndexesArray[i++] = primaryMap.get(ne);
            }else{
                mCursorsArray[i] = mSecondary;
                mIndexesArray[i++] = secondaryMap.get(ne);
            }
        }
    }
    
    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getColumnNames()
     */
    @Override
    public String[] getColumnNames() {
        return mProjection;
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getCount()
     */
    @Override
    public int getCount() {
        return mTotal;
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getDouble(int)
     */
    @Override
    public double getDouble(int column) {
        return mCurrent.getDouble(column);
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getFloat(int)
     */
    @Override
    public float getFloat(int column) {
        return mCurrent.getFloat(column);
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getInt(int)
     */
    @Override
    public int getInt(int column) {
        return mCurrent.getInt(column);
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getLong(int)
     */
    @Override
    public long getLong(int column) {
        return mCurrent.getLong(column);
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getShort(int)
     */
    @Override
    public short getShort(int column) {
        return mCurrent.getShort(column);
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getString(int)
     */
    @Override
    public String getString(int column) {
        // blob can't be converted to string, so it causes exception. return
        // null in this case
        try{
            return mCurrent.getString(column);
        }catch(Exception e){
            return null;
        }
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#isNull(int)
     */
    @Override
    public boolean isNull(int column) {
        return mCurrent.isNull(column);
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#close()
     */
    @Override
    public void close() {
        super.close();
        mPrimary.close();
        mSecondary.close();
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#deactivate()
     */
    @Override
    public void deactivate() {
        super.deactivate();
        mPrimary.deactivate();
        mSecondary.deactivate();
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#getBlob(int)
     */
    @Override
    public byte[] getBlob(int column) {
        return mCurrent.getBlob(column);
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#requery()
     */
    @Override
    public boolean requery() {
        if (isClosed()) {
            return false;
        }

        mPrimary.requery();
        mSecondary.requery();
        
        init();

        return super.requery();
    }

    /**
     * (non-Javadoc)
     * @see android.database.AbstractCursor#onMove(int, int)
     */
    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        mCurrent = mCursorsArray[newPosition];
        return mCurrent.moveToPosition(mIndexesArray[newPosition]);
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mPrimary.registerContentObserver(observer);
        mSecondary.registerContentObserver(observer);
    }
    
    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mPrimary.unregisterContentObserver(observer);
        mSecondary.unregisterContentObserver(observer);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mPrimary.registerDataSetObserver(observer);
        mSecondary.registerDataSetObserver(observer);
    }
    
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mPrimary.unregisterDataSetObserver(observer);
        mSecondary.unregisterDataSetObserver(observer);
    }
}
// LGE_PHONEBOOK_EXTENSION END
