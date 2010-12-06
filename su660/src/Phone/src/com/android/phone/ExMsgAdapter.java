package com.android.phone;
//20100804 jongwany.lee@lge.com attached this file CALL UI

import java.util.HashMap;
import java.util.WeakHashMap;

import com.android.phone.ExMsgProviderMetaData.ExMsgTableMetaData;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]  
import android.util.SparseBooleanArray;
import android.widget.ListView;
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]   
public class ExMsgAdapter extends ResourceCursorAdapter {
	public static final String TAG = "ExMsgAdapter";
		
    /**
     * A list of columns containing the data to bind to the UI.
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected int[] mFrom;
    /**
     * A list of View ids representing the views to which the data must be bound.
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected int[] mTo;

    private int mStringConversionColumn = -1;
    private CursorToStringConverter mCursorToStringConverter;
    private Cursor mCursor;
    private ViewBinder mViewBinder;
    private String[] mOriginalFrom;
    private final WeakHashMap<View, View[]> mHolders = new WeakHashMap<View, View[]>();
    private Context mContext;
    private String mSendNumber;
    
    public static boolean MODE_DELETE = false;
    public static boolean CHECK_STATE = false;
    public HashMap<Long, Boolean> checkState = new HashMap<Long, Boolean>();
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN] 
    private ListView mListView;
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END] 

    /**
     * Constructor.
     * 
     * @param context The context where the ListView associated with this
     *            SimpleListItemFactory is running
     * @param layout resource identifier of a layout file that defines the views
     *            for this list item. Thelayout file should include at least
     *            those named views defined in "to"
     * @param c The database cursor.  Can be null if the cursor is not available yet.
     * @param from A list of column names representing the data to bind to the UI.  Can be null 
     *            if the cursor is not available yet.
     * @param to The views that should display column in the "from" parameter.
     *            These should all be TextViews. The first N views in this list
     *            are given the values of the first N columns in the from
     *            parameter.  Can be null if the cursor is not available yet.
     */
    // 20100428 cutestar@lge.com  To support sms-sending icon func. when reject incoming call.
    public ExMsgAdapter(Context context, int layout, Cursor c, String[] from, int[] to, String phonenumber) {
        super(context, layout, c);
        mTo = to;
        mOriginalFrom = from;
        mCursor = c;
        mContext = context;        
        findColumns(from);        
      	mSendNumber = phonenumber;

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return generateViewHolder(super.newView(context, cursor, parent), cursor);
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        return generateViewHolder(super.newDropDownView(context, cursor, parent), cursor);
    }

    private View generateViewHolder(View v, Cursor cursor) {
        final int[] to = mTo;
        final View[] holder = new View[2];  
        mCursor = cursor;
        
        holder[0] = v.findViewById(to[0]);
        holder[1] = v.findViewById(R.id.CheckImgView);
		holder[1].setVisibility(View.GONE);    
        
        mHolders.put(v, holder);

        return v;
    }
    public boolean getMultiSelectMode(){
    	return MODE_DELETE;
    }
    
    public void setMultiSelectMode(boolean state){
    	MODE_DELETE = state;
    }
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN] 
    public void setListViewForReference(ListView lv){
    	mListView = lv;  
    }
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]

    /**
     * Binds all of the field names passed into the "to" parameter of the
     * constructor with their corresponding cursor columns as specified in the
     * "from" parameter.
     *
     * Binding occurs in two phases. First, if a
     * {@link android.widget.SimpleCursorAdapter.ViewBinder} is available,
     * {@link ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)}
     * is invoked. If the returned value is true, binding has occured. If the
     * returned value is false and the view to bind is a TextView,
     * {@link #setViewText(TextView, String)} is invoked. If the returned value is
     * false and the view to bind is an ImageView,
     * {@link #setViewImage(ImageView, String)} is invoked. If no appropriate
     * binding can be found, an {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException if binding cannot occur
     * 
     * @see android.widget.CursorAdapter#bindView(android.view.View,
     *      android.content.Context, android.database.Cursor)
     * @see #getViewBinder()
     * @see #setViewBinder(android.widget.SimpleCursorAdapter.ViewBinder)
     * @see #setViewImage(ImageView, String)
     * @see #setViewText(TextView, String)
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final View[] holder = mHolders.get(view);
        final ViewBinder binder = mViewBinder;
        final int[] from = mFrom;
        
        // 2010-05-24, WBT: Null pointer dereference of 'holder' where null is returned from a map or a collection
        if(holder == null){
        	return;
        }
        
		final View v = holder[0];
		
		if (v != null) {
			boolean bound = false;
			if (binder != null) {
				bound = binder.setViewValue(v, cursor, from[0]);
			}

			if (!bound) {
				String text = null;
				if (cursor.getInt(from[1]) == ExMsgTableMetaData.DEFAULT_ITEM) {
					Resources r= mContext.getResources();
					text = r.getString(Integer.valueOf(cursor.getString(from[0])));				
//					String stringToBeCompared = cursor.getString(from[0]);					
					/* Let find a object from arrays */
//					int matchedIndex = Arrays.binarySearch(excuseMsg, stringToBeCompared);
//					
//					if (matchedIndex >= 0) {
//						Resources r= mContext.getResources();
//						
//						text = r.getString(mResourceIds[matchedIndex]);
//						Log.i(TAG, "matchedIndex = " + matchedIndex + ", got string is " + text);
//					}
				} else {
					text = cursor.getString(from[0]);
				}
				if (text == null) {
					text = "";
				}

				// 20100428 cutestar@lge.com  To support sms-sending icon func. when reject incoming call.
				final Button mSendButton = (Button) view.findViewById(R.id.btn_send);
				
				if( (mSendNumber == null) || getMultiSelectMode() ){
					mSendButton.setVisibility(View.GONE);
				}
				else{
					mSendButton.setVisibility(View.VISIBLE);
					
					if(mSendButton !=  null){

						mSendButton.setTag(text);

						mSendButton.setOnClickListener(new View.OnClickListener(){
				      	  public void onClick(View v) {
				    		  String SendText = (String) v.getTag();
				    		  
				  			  Intent sendIntent = new Intent(Intent.ACTION_VIEW);
							
							  sendIntent.putExtra("address", mSendNumber);
							  sendIntent.putExtra("sms_body", SendText);
							  // LGE_UI_CHANGE : Excuse_msg : BEGIN
                              sendIntent.putExtra("excus_msg",true);
                              // LGE_UI_CHANGE : Excuse_msg : END
							  sendIntent.setType("vnd.android-dir/mms-sms");
							  
							  mContext.startActivity(sendIntent);
				    		}
				    	  });					
					}
				}
			
				if (v instanceof TextView) {
					setViewText((TextView) v, text);					
				} else if (v instanceof ImageView) {
					setViewImage((ImageView) v, text);
				} else {
					throw new IllegalStateException(
							v.getClass().getName()
									+ " is not a "
									+ " view that can be bounds by this SimpleCursorAdapter");
				}
			}
		}
		
//		if(cursor.getString(mFrom[2]).equals(ExMsgTableMetaData.ITEM_IS_CHECKED)){
//			((ImageView) holder[1]).setImageResource(R.drawable.general_check_box_check);
//			
//		} else {
//			((ImageView) holder[1]).setImageResource(R.drawable.general_check_box_empty);
//		}
	    	
		if(getMultiSelectMode()){        	
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][BEGIN]
/*
			Log.i(TAG,"checkState.get(cursor.getLong(0)) = "+checkState.get(cursor.getLong(0)));			
			if( (getInitialCheckBoxState() == true) || (checkState.get(cursor.getLong(0)) == false) ){
                // 20100424 cutestar@lge.com Using Native check-box component. 
                //((ImageView) holder[1]).setImageResource(R.drawable.general_check_box_empty);
				((CheckedTextView) holder[1]).setChecked(false);
				checkState.put(cursor.getLong(0), false);
			} 
			else if(checkState.get(cursor.getLong(0)) == true){
				//((ImageView) holder[1]).setImageResource(R.drawable.general_check_box_check);
				((CheckedTextView) holder[1]).setChecked(true);
			}
*/
			SparseBooleanArray sba = mListView.getCheckedItemPositions();
	    	
			if(sba.get(cursor.getPosition())){
				((CheckedTextView) holder[1]).setChecked(true);
			} 
			else{
				((CheckedTextView) holder[1]).setChecked(false);
			}
// LGE_CHANGE [daehun.ju@lge.com][2010.07.12][END]
			holder[1].setVisibility(View.VISIBLE);
			
        } else {        	
        	holder[1].setVisibility(View.GONE);
        	
        }
		holder[1].invalidate();
		
    }    
    
    public void setInitialCheckBoxState(boolean state){
    	CHECK_STATE = state;
    	
    }

    public boolean getInitialCheckBoxState(){
    	return CHECK_STATE;
    }
    
    public boolean getCheckBoxState(long _id){
    	return checkState.get(_id);
    }
    /**
     * Returns the {@link ViewBinder} used to bind data to views.
     *
     * @return a ViewBinder or null if the binder does not exist
     *
     * @see #bindView(android.view.View, android.content.Context, android.database.Cursor)
     * @see #setViewBinder(android.widget.SimpleCursorAdapter.ViewBinder)
     */
    
    public ViewBinder getViewBinder() {
        return mViewBinder;
    }

    /**
     * Sets the binder used to bind data to views.
     *
     * @param viewBinder the binder used to bind data to views, can be null to
     *        remove the existing binder
     *
     * @see #bindView(android.view.View, android.content.Context, android.database.Cursor)
     * @see #getViewBinder()
     */
    public void setViewBinder(ViewBinder viewBinder) {
        mViewBinder = viewBinder;
    }

    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     *
     * By default, the value will be treated as an image resource. If the
     * value cannot be used as an image resource, the value is used as an
     * image Uri.
     *
     * Intended to be overridden by Adapters that need to filter strings
     * retrieved from the database.
     *
     * @param v ImageView to receive an image
     * @param value the value retrieved from the cursor
     */
    public void setViewImage(ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException nfe) {
            v.setImageURI(Uri.parse(value));
        }
    }

    /**
     * Called by bindView() to set the text for a TextView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an TextView.
     *
     * Intended to be overridden by Adapters that need to filter strings
     * retrieved from the database.
     * 
     * @param v TextView to receive text
     * @param text the text to be set for the TextView
     */    
    public void setViewText(TextView v, String text) {
        v.setText(text);
    }

    /**
     * Return the index of the column used to get a String representation
     * of the Cursor.
     *
     * @return a valid index in the current Cursor or -1
     *
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     * @see #setStringConversionColumn(int) 
     * @see #setCursorToStringConverter(android.widget.SimpleCursorAdapter.CursorToStringConverter)
     * @see #getCursorToStringConverter()
     */
    public int getStringConversionColumn() {
        return mStringConversionColumn;
    }

    /**
     * Defines the index of the column in the Cursor used to get a String
     * representation of that Cursor. The column is used to convert the
     * Cursor to a String only when the current CursorToStringConverter
     * is null.
     *
     * @param stringConversionColumn a valid index in the current Cursor or -1 to use the default
     *        conversion mechanism
     *
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     * @see #getStringConversionColumn()
     * @see #setCursorToStringConverter(android.widget.SimpleCursorAdapter.CursorToStringConverter)
     * @see #getCursorToStringConverter()
     */
    public void setStringConversionColumn(int stringConversionColumn) {
        mStringConversionColumn = stringConversionColumn;
    }

    /**
     * Returns the converter used to convert the filtering Cursor
     * into a String.
     *
     * @return null if the converter does not exist or an instance of
     *         {@link android.widget.SimpleCursorAdapter.CursorToStringConverter}
     *
     * @see #setCursorToStringConverter(android.widget.SimpleCursorAdapter.CursorToStringConverter)
     * @see #getStringConversionColumn()
     * @see #setStringConversionColumn(int)
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     */
    public CursorToStringConverter getCursorToStringConverter() {
        return mCursorToStringConverter;
    }

    /**
     * Sets the converter  used to convert the filtering Cursor
     * into a String.
     *
     * @param cursorToStringConverter the Cursor to String converter, or
     *        null to remove the converter
     *
     * @see #setCursorToStringConverter(android.widget.SimpleCursorAdapter.CursorToStringConverter) 
     * @see #getStringConversionColumn()
     * @see #setStringConversionColumn(int)
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     */
    public void setCursorToStringConverter(CursorToStringConverter cursorToStringConverter) {
        mCursorToStringConverter = cursorToStringConverter;
    }

    /**
     * Returns a CharSequence representation of the specified Cursor as defined
     * by the current CursorToStringConverter. If no CursorToStringConverter
     * has been set, the String conversion column is used instead. If the
     * conversion column is -1, the returned String is empty if the cursor
     * is null or Cursor.toString().
     *
     * @param cursor the Cursor to convert to a CharSequence
     *
     * @return a non-null CharSequence representing the cursor
     */
    @Override
    public CharSequence convertToString(Cursor cursor) {
        if (mCursorToStringConverter != null) {
            return mCursorToStringConverter.convertToString(cursor);
        } else if (mStringConversionColumn > -1) {
            return cursor.getString(mStringConversionColumn);
        }

        return super.convertToString(cursor);
    }

    /**
     * Create a map from an array of strings to an array of column-id integers in mCursor.
     * If mCursor is null, the array will be discarded.
     * 
     * @param from the Strings naming the columns of interest
     */
    private void findColumns(String[] from) {
        if (mCursor != null) {
            int i;
            int count = from.length;
            if (mFrom == null || mFrom.length != count) {
                mFrom = new int[count];
            }
            for (i = 0; i < count; i++) {
                mFrom[i] = mCursor.getColumnIndexOrThrow(from[i]);
            }
        } else {
            mFrom = null;
        }
    }

    @Override
    public void changeCursor(Cursor c) {
        super.changeCursor(c);
        // rescan columns in case cursor layout is different
        findColumns(mOriginalFrom);
    }
    
    /**
     * Change the cursor and change the column-to-view mappings at the same time.
     *  
     * @param c The database cursor.  Can be null if the cursor is not available yet.
     * @param from A list of column names representing the data to bind to the UI.  Can be null 
     *            if the cursor is not available yet.
     * @param to The views that should display column in the "from" parameter.
     *            These should all be TextViews. The first N views in this list
     *            are given the values of the first N columns in the from
     *            parameter.  Can be null if the cursor is not available yet.
     */
    public void changeCursorAndColumns(Cursor c, String[] from, int[] to) {
        mOriginalFrom = from;
        mTo = to;
        super.changeCursor(c);        
        findColumns(mOriginalFrom);
    }

    /**
     * This class can be used by external clients of SimpleCursorAdapter
     * to bind values fom the Cursor to views.
     *
     * You should use this class to bind values from the Cursor to views
     * that are not directly supported by SimpleCursorAdapter or to
     * change the way binding occurs for views supported by
     * SimpleCursorAdapter.
     *
     * @see ExMsgAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
     * @see ExMsgAdapter#setViewImage(ImageView, String) 
     * @see ExMsgAdapter#setViewText(TextView, String)
     */
    public static interface ViewBinder {
        /**
         * Binds the Cursor column defined by the specified index to the specified view.
         *
         * When binding is handled by this ViewBinder, this method must return true.
         * If this method returns false, SimpleCursorAdapter will attempts to handle
         * the binding on its own.
         *
         * @param view the view to bind the data to
         * @param cursor the cursor to get the data from
         * @param columnIndex the column at which the data can be found in the cursor
         *
         * @return true if the data was bound to the view, false otherwise
         */
        boolean setViewValue(View view, Cursor cursor, int columnIndex);
    }

    /**
     * This class can be used by external clients of SimpleCursorAdapter
     * to define how the Cursor should be converted to a String.
     *
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     */
    public static interface CursorToStringConverter {
        /**
         * Returns a CharSequence representing the specified Cursor.
         *
         * @param cursor the cursor for which a CharSequence representation
         *        is requested
         *
         * @return a non-null CharSequence representing the cursor
         */
        CharSequence convertToString(Cursor cursor);
    }	

}

