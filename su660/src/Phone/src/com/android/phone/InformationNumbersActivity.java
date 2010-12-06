// LGE_CPHS_INFO_NUMBERS START
/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.phone;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_ALPHA_TAG;
import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_EXTENSION_RECORD;
import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_INDEX_ENTRY;
import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_INDEX_LEVEL;
import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_NETWORK_SPECIFIC;
import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_NUMBER;
import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_NUMBER_TYPE;
import static com.android.internal.telephony.gsm.CphsInfoNumbers.IN_PREMIUM_SERVICE;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * The class InformationNumbersActivity represents the application
 * which provides the access to the information numbers from SIM card
 * with ability to call
 */
public class InformationNumbersActivity extends ListActivity {
    private static final String TAG = "InformationNumbersActivity";
    private static final boolean DBG = true;

    /**
     * Tree creation process results
     */
    private static final int INA_SUCCESS = 0;
    private static final int INA_FAIL = -1;

    /**
     * Information numbers records storage (generic tree)
     */
    private GenericTreeNode<InfoNumberItem> mInfoNumbersTree;
    /**
     * Current level node in the tree, like a cursor
     */
    private GenericTreeNode<InfoNumberItem> mCurrentNode;
    /**
     * Current level items list
     */
    private ArrayList<InfoNumberItem> mCurrentLevelItems;
    /**
     * Tree path indexes
     */
    private Stack<Integer> mPath;

    /**
     * Level cursor for creating items tree
     */
    private byte mItemLevel;
    /**
     * Roaming state indicator for creating items tree
     */
    private boolean mIsRoaming;

    /**
     * ListView for list items accessing
     */
    private ListView mListView;
    /**
     * DisplayAlert dialog
     */
    private AlertDialog mDisplayAlertDialog = null;

    /**
     * Indicates the state of the activity
     * true - all is ok, false do not perform any action except exit
     */
    private boolean mIsInitializedState;

    /**
     * Generic tree node structure for information numbers records storing
     *
     * @param <T> Type of the node value objects
     */
    public class GenericTreeNode<T> {

        /**
         * Reference to parent node, null for root
         */
        private GenericTreeNode<T> mParent = null;
        /**
         * Children list
         */
        public GenericTreeNodeList<T> mChildren = null;
        /**
         * Node value object
         */
        public T mValue = null;

        /**
         * Returns the parent node for current
         *
         * @return Parent node
         */
        public GenericTreeNode<T> getParent() {
            return mParent;
        }

        /**
         * Set the parent for the current node and children
         *
         * @param parent Node to set as the parent
         */
        public void setParent(GenericTreeNode<T> parent) {
            if (parent == mParent) {
                return;
            }
            if (mParent != null) {
                mParent.mChildren.Remove(this);
            }
            if (parent != null && !parent.mChildren.contains(this)) {
                parent.mChildren.Add(this);
            }
            mParent = parent;
        }

        /**
         * Returns the root of the tree
         *
         * @return Root node
         */
        public GenericTreeNode<T> getRoot() {
            GenericTreeNode<T> node = this;
            while (node.getParent() != null) {
                node = node.getParent();
            }
            return node;
        }

        /**
         * Constructor
         */
        public GenericTreeNode() {
            mChildren = new GenericTreeNodeList<T>(this);
        }

        /**
         * Constructor, set the node data
         *
         * @param value Data object
         */
        public GenericTreeNode(T value) {
            mValue = value;
            mChildren = new GenericTreeNodeList<T>(this);
        }

        /**
         * Constructor, set the parent to this node
         *
         * @param parent Parent node
         */
        public GenericTreeNode(GenericTreeNode<T> parent) {
            mParent = parent;
            mChildren = new GenericTreeNodeList<T>(this);
        }

        /**
         * Constructor, set the children to this node
         *
         * @param children List with children nodes
         */
        public GenericTreeNode(GenericTreeNodeList<T> children) {
            mChildren = children;
            children.mParent = this;
        }

        /**
         * Constructor, set the parent and the children to this node
         *
         * @param Parent   Parent node
         * @param Children List with children nodes
         */
        public GenericTreeNode(GenericTreeNode<T> Parent, GenericTreeNodeList<T> Children) {
            mParent = Parent;
            mChildren = Children;
            mChildren.mParent = this;
        }

        /**
         * Reports a depth of nesting in the tree, starting at 0 for the root.
         *
         * @return Depth level
         */
        public int getDepth() {
            int depth = 0;
            GenericTreeNode<T> node = this;
            while (node.getParent() != null) {
                node = node.getParent();
                depth++;
            }
            return depth;
        }

        @Override
        /**
         * Creates string with tabbed structure of the node with the children
         * getRoot().toString prints out all the tree
         */
        public String toString() {
            String prefix = "";
            StringBuffer buffer = new StringBuffer();

            if (getRoot() == this) {
                buffer.append("Root\n");
            }

            int depth = getDepth();

            while (depth-- > 0) {
                prefix += "\t";
            }

            buffer.append(prefix);
            buffer.append("[Depth=");
            buffer.append(getDepth());
            buffer.append(", Children=");
            buffer.append(mChildren.size());
            buffer.append("]\n");
            buffer.append((mValue == null ? prefix + "<null>" : mValue.toString()));
            buffer.append("\n\n");
            buffer.append(mChildren.toString());

            return buffer.toString();
        }
    }

    /**
     * Generic tree node list structure for information numbers records storing
     *
     * @param <T> Type of the node value objects
     */
    public class GenericTreeNodeList<T> extends ArrayList<GenericTreeNode<T>> {
        /**
         * Reference to parent node
         */
        public GenericTreeNode<T> mParent;

        /**
         * Constructor, set the parent to this node
         *
         * @param parent Parent node
         */
        public GenericTreeNodeList(GenericTreeNode<T> parent) {
            if (parent != null) {
                mParent = parent;
            }
        }

        /**
         * Adds new node to the list
         *
         * @param node New node
         * @return Added node if success and null othrwise
         */
        public GenericTreeNode<T> Add(GenericTreeNode<T> node) {
            if (node != null) {
                add(node);
                node.setParent(mParent);
            }
            return node;
        }

        /**
         * Adds new node with specified data object to the list
         *
         * @param value Data object to store in the tree node
         * @return Added node if success and null othrwise
         */
        public GenericTreeNode<T> Add(T value) {
            if (value != null) {
                GenericTreeNode<T> node = new GenericTreeNode<T>(mParent);
                node.mValue = value;
                return node;
            }
            return null;
        }

        /**
         * Removes node from the list
         *
         * @param node Node to remove
         */
        public void Remove(GenericTreeNode<T> node) {
            if (node != null) {
                node.setParent(null);
            }
            remove(node);
        }

        /**
         * Gets the list with values of children nodes
         *
         * @return List of InfoNumberItem objects
         */
        public List<T> getValues() {
            List<T> values = new ArrayList<T>();
            ListIterator<GenericTreeNode<T>> it = listIterator();
            while (it.hasNext()) {
                values.add((it.next()).mValue);
            }
            return values;
        }

        @Override
        /**
         * Creates string with tabbed structure of the node with the children
         */
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            ListIterator it = listIterator();

            while (it.hasNext()) {
                buffer.append(it.next().toString());
            }

            return buffer.toString();
        }
    }

    /**
     * Information number record storage class
     */
    private class InfoNumberItem {
        /**
         * Information number record raw data
         */
        private Hashtable mData;
        /**
         * Index in level
         */
        public byte mIndex;
        /**
         * Level index
         */
        public byte mLevel;
        /**
         * Indicator needed for enabling/disabling list item
         * Depends from roaming and network specific indicators
         */
        public boolean isEnabled;

        /**
         * Constructor
         */
        public InfoNumberItem() {
            isEnabled = false;
            mIndex = 0;
            mLevel = 0;
            mData = null;
        }

        /**
         * Constructor, set the item data
         *
         * @param table Data object with information number record
         */
        public InfoNumberItem(Hashtable table) {
            mIndex = 0;
            mLevel = 0;
            mData = table;
            setEnabled();
        }

        /**
         * Constructor, set the item data
         *
         * @param index Order index on the level
         * @param level Level index
         * @param table Data object with information number record
         */
        public InfoNumberItem(byte index, byte level, Hashtable table) {
            mIndex = index;
            mLevel = level;
            mData = table;
            setEnabled();
        }

        /**
         * Get boolean value from data record
         *
         * @param name Name of the information number record field
         * @return Boolean value
         */
        private boolean getBoolean(Object name) {
            boolean result = false;
            if (mData != null) {
                Boolean value = (Boolean) mData.get(name);
                result = value != null && value;
            }
            return result;
        }

        /**
         * Set the enabled indicator
         */
        public void setEnabled() {
            isEnabled = !(mIsRoaming & isNetworkSpecific());
        }

        /**
         * Return IN_ALPHA_TAG record value
         *
         * @return IN_ALPHA_TAG string
         */
        public String getAlphaTag() {
            return mData == null ? "" : (String) mData.get(IN_ALPHA_TAG);
        }

        /**
         * Return IN_NETWORK_SPECIFIC record value
         *
         * @return IN_NETWORK_SPECIFIC boolean
         */
        public boolean isNetworkSpecific() {
            return getBoolean(IN_NETWORK_SPECIFIC);
        }

        /**
         * Return IN_PREMIUM_SERVICE record value
         *
         * @return IN_PREMIUM_SERVICE boolean
         */
        public boolean isPremiumService() {
            return getBoolean(IN_PREMIUM_SERVICE);
        }

        /**
         * Return IN_INDEX_ENTRY record value
         *
         * @return IN_INDEX_ENTRY boolean
         */
        public boolean isIndexEntry() {
            return getBoolean(IN_INDEX_ENTRY);
        }

        /**
         * Return IN_INDEX_LEVEL record value
         *
         * @return IN_INDEX_LEVEL byte
         */
        public byte getIndexLevel() {
            return mData == null ? 0 : (Byte) mData.get(IN_INDEX_LEVEL);
        }

        /**
         * Return IN_NUMBER record value
         *
         * @return IN_NUMBER string
         */
        public String getNumber() {
            return mData == null ? "" : (String) mData.get(IN_NUMBER);
        }

        /**
         * Return IN_NUMBER_TYPE record value
         *
         * @return IN_NUMBER_TYPE byte
         */
        public byte getNumberType() {
            return mData == null ? 0 : (Byte) mData.get(IN_NUMBER_TYPE);
        }

        /**
         * Return IN_EXTENSION_RECORD record value
         *
         * @return IN_EXTENSION_RECORD byte
         */
        public byte getExtensionRecord() {
            return mData == null ? 0 : (Byte) mData.get(IN_EXTENSION_RECORD);
        }

        /**
         * Creates string with tabbed structure of the node with the children
         *
         * @return String
         */
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            String prefix = "";

            Byte il = mLevel;
            while (il-- > 0) {
                prefix += "\t";
            }

            buffer.append(prefix);
            buffer.append("ALPHA_TAG: ");
            buffer.append(getAlphaTag());
            buffer.append("\n");
            buffer.append(prefix);
            buffer.append("NETWORK_SPECIFIC: ");
            buffer.append(isNetworkSpecific());
            buffer.append("\n");
            buffer.append(prefix);
            buffer.append("PREMIUM_SERVICE: ");
            buffer.append(isPremiumService());
            buffer.append("\n");
            buffer.append(prefix);
            buffer.append("INDEX_ENTRY: ");
            buffer.append(isIndexEntry());
            buffer.append("\n");
            buffer.append(prefix);
            buffer.append("INDEX_LEVEL: ");
            buffer.append(getIndexLevel());
            buffer.append("\n");
            buffer.append(prefix);
            buffer.append("NUMBER: ");
            buffer.append(getNumber());
            buffer.append("\n");
            buffer.append(prefix);
            buffer.append("NUMBER_TYPE: ");
            buffer.append(getNumberType());
            buffer.append("\n");
            buffer.append(prefix);
            buffer.append("EXTENSION_RECORD: ");
            buffer.append(getExtensionRecord());

            return buffer.toString();
        }
    }

    /**
     * Adapter class to represent information numbers items in
     * list view object
     */
    class InfoNumbersAdapter extends ArrayAdapter<InfoNumberItem> {
        protected LayoutInflater mInflater;
        private static final int mResource = R.layout.info_numbers_row;

        /**
         * Constructor
         *
         * @param context The current context
         * @param items   The list of the information numbers items to display at the current level
         */
        InfoNumbersAdapter(ListActivity context, List<InfoNumberItem> items) {
            super(context, mResource, items);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * Prepare a View that displays the data at the specified position in the data set
         *
         * @param position    The position of the item within the adapter's data set of the item whose view we want
         * @param convertView The old view to reuse, if possible
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView alphaTag;
            TextView number;
            ImageView numberIcon;
            ImageView networkSpecific;
            ImageView premiumService;

            View view;
            if (convertView == null) {
                view = mInflater.inflate(mResource, parent, false);
            } else {
                view = convertView;
            }

            // Get current InfoNumberItem object
            InfoNumberItem item = getItem(position);

            // Get number and number icon fields
            number = (TextView) view.findViewById(R.id.in_number);
            numberIcon = (ImageView) view.findViewById(R.id.in_number_icon);

            // Set/hide alpha tag, number and number icon
            if (item.isIndexEntry()) {
                // Get alpha tag field
                alphaTag = (TextView) view.findViewById(R.id.in_alpha_tag_index);
                // Hile number fields
                numberIcon.setVisibility(ImageView.GONE);
                number.setVisibility(TextView.GONE);
            } else {
                // Get alpha tag field
                alphaTag = (TextView) view.findViewById(R.id.in_alpha_tag_number);
                // Set number field
                number.setText(item.getNumber());
            }

            // Set alpha tag text
            alphaTag.setText(item.getAlphaTag());
            // Set alpha tag visible
            alphaTag.setVisibility(TextView.VISIBLE);

            // Hide premium service icon
            premiumService = (ImageView) view.findViewById(R.id.in_premium_service_icon);
            if (!item.isPremiumService()) {
                premiumService.setVisibility(ImageView.GONE);
            }

            // Hide network specific resource icon
            networkSpecific = (ImageView) view.findViewById(R.id.in_network_specific_icon);
            if (!item.isNetworkSpecific()) {
                networkSpecific.setVisibility(ImageView.GONE);
            }

            // Gray alpha tag if item is disabled
            if (!item.isEnabled) {
                // Set text to gray
                alphaTag.setTextColor(Color.GRAY);
            }
            view.setEnabled(item.isEnabled);
            view.invalidate();

            return view;
        }
    }

    /**
     * Class to handle click on alert dialog
     */
    private static class ADCancelListener implements AlertDialog.OnClickListener {
        private boolean mIsFinish;
        private Activity mParent;

        public ADCancelListener(boolean isFinish, Activity parent) {
            mParent = parent;
            mIsFinish = isFinish;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (mIsFinish) {
                mParent.finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.info_numbers);
        // Call initialization and set initialized state
        mIsInitializedState = init();
    }

    /**
     * Initialization procedure, receives the information numbers data from SIM,
     * creates list items tree, creates initial view adapter and prepare internal data
     *
     * @return True if all is ok, false if any error
     */
    private boolean init() {
        // Get the default phone
        Phone mPhone = PhoneFactory.getDefaultPhone();
        // Get roaming indicator
        mIsRoaming = mPhone.getServiceState().getRoaming();

        // Get information numbers data
        List infoNumbers = mPhone.getInformationNumbers();
        // Check information numbers data validity
        if (infoNumbers == null || infoNumbers.size() == 0) {
            if (DBG) {
                Log.d(TAG, "getInformationNumbers:" + getString(R.string.info_numbers_receive_error));
            }
            displayAlert(getString(R.string.info_numbers_receive_error), true);
            return false;
        }

        // Set initial values for creating information numbers tree
        mItemLevel = 0;
        // Create information numbers tree with empty InfoNumberItem object as root
        mInfoNumbersTree = new GenericTreeNode<InfoNumberItem>(new InfoNumberItem());
        // Call recursive tree creator
        ListIterator it = infoNumbers.listIterator();
        if (INA_SUCCESS != createInfoNumbersTree(it, mInfoNumbersTree)) {
            if (DBG) {
                Log.d(TAG, "createInfoNumbersTree:" + getString(R.string.info_numbers_init_error));
            }
            displayAlert(getString(R.string.info_numbers_init_error), true);
            return false;
        }

        // Create list for path indexes
        mPath = new Stack<Integer>();
        // Create list for current level items
        mCurrentLevelItems = new ArrayList<InfoNumberItem>();
        // Init current node
        mCurrentNode = mInfoNumbersTree;
        // Get current nodes list
        if (INA_SUCCESS != getChildrenLevel()) {
            if (DBG) {
                Log.d(TAG, "getChildrenLevel:" + getString(R.string.info_numbers_init_error));
            }
            displayAlert(getString(R.string.info_numbers_init_error), true);
            return false;
        }
        // Get list view
        mListView = getListView();
        // Create the adapter to bind the array to the listview
        InfoNumbersAdapter adapter = new InfoNumbersAdapter(this, mCurrentLevelItems);
        // Bind the adapter to the listview.
        mListView.setAdapter(adapter);

        return true;
    }

    /**
     * Show the alert window with the specified message
     *
     * @param infoMsg  Message to show
     * @param isFinish Indicator to finish the application
     */
    private void displayAlert(String infoMsg, boolean isFinish) {
        if (DBG) {
            Log.d(TAG, infoMsg);
        }
        // Dismiss previous dialog if exists
        if (mDisplayAlertDialog != null) {
            mDisplayAlertDialog.dismiss();
        }
        // Displaying system alert dialog on the screen instead of
        // using another activity to display the message. This
        // places the message at the forefront of the UI.
        mDisplayAlertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.info_numbers)
                .setMessage(infoMsg)
                .setPositiveButton(android.R.string.ok, new ADCancelListener(isFinish, this))
                .setCancelable(true)
                .create();
        // Set dialog style
        mDisplayAlertDialog.getWindow().setType(
                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        mDisplayAlertDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

        mDisplayAlertDialog.show();

    }

    /**
     * Set the mCurrentNode to the specified position on current level
     *
     * @param position Position index
     * @return INA_FAIL if any error, INA_SUCCESS otherwise
     */
    private int setCurrentItem(int position) {
        // Check initialized state
        if (!mIsInitializedState) {
            if (DBG) {
                Log.d(TAG, "setCurrentItem: Application is not initialized");
            }
            return INA_FAIL;
        }
        // Check for valadity
        if (mCurrentNode == null || mCurrentNode.getParent() == null) {
            if (DBG) {
                Log.d(TAG, "setCurrentItem: mCurrentNode is invalid");
            }
            return INA_FAIL;
        }
        if (position < 0) {
            if (DBG) {
                Log.d(TAG, "setCurrentItem: position is negative");
            }
            return INA_FAIL;
        }
        // Get node parent
        GenericTreeNode<InfoNumberItem> parent = mCurrentNode.getParent();
        try {
            if (position >= parent.mChildren.size()) {
                if (DBG) {
                    Log.d(TAG, "setCurrentItem: position is out of bounds");
                }
                return INA_FAIL;
            }
            // Set new current by position
            mCurrentNode = parent.mChildren.get(position);
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Uups!!! Smthg wrong
            if (DBG) {
                Log.d(TAG, "setCurrentItem:" + ex.getMessage());
            }
            return INA_FAIL;
        }
        return INA_SUCCESS;
    }

    /**
     * Fill the mCurrentLevelItems with the children of the current node
     * and set mCurrentNode to the first child
     *
     * @return INA_FAIL if any error, INA_SUCCESS otherwise
     */
    private int getChildrenLevel() {
        // Check for index entry type and childrens quantity
        if (mCurrentNode != mInfoNumbersTree &&
                (!mCurrentNode.mValue.isIndexEntry() || mCurrentNode.mChildren.size() == 0)) {
            if (DBG) {
                Log.d(TAG, "getChildrenLevel: no children at node");
            }
            return INA_FAIL;
        }
        // Clear previous list
        if (null != mCurrentLevelItems && mCurrentLevelItems.size() > 0) {
            mCurrentLevelItems.clear();
        }
        // Fill the list with information numbers records
        mCurrentLevelItems.addAll(mCurrentNode.mChildren.getValues());
        // Set current node to the first child
        mCurrentNode = mCurrentNode.mChildren.get(0);

        return INA_SUCCESS;
    }

    /**
     * Fill the mCurrentLevelItems with the parent level items of the current node
     * and set mCurrentNode to the first item on the parent level
     *
     * @return INA_FAIL if any error, INA_SUCCESS otherwise
     */
    private int getParentLevel() {
        // Check for parent and root (upper level)
        if (mCurrentNode.getParent() == null || mCurrentNode.getParent() == mCurrentNode.getRoot()) {
            if (DBG) {
                Log.d(TAG, "getParentLevel: no valid parent at node");
            }
            return INA_FAIL;
        }
        // Clear previous list
        if (null != mCurrentLevelItems && mCurrentLevelItems.size() > 0) {
            mCurrentLevelItems.clear();
        }
        // Get parent node (parentOfParent) of the current node parent to obtain the nodes list of parent
        // level as a children of the parentOfParent
        GenericTreeNode<InfoNumberItem> parentOfParent = mCurrentNode.getParent().getParent();
        // Fill the string tags array
        mCurrentLevelItems.addAll(parentOfParent.mChildren.getValues());
        // Set current node to the first child
        mCurrentNode = parentOfParent.mChildren.get(0);

        return INA_SUCCESS;
    }

    /**
     * Create information numbers tree
     *
     * @param it   Iterator of the information numbers record list
     * @param root Root node of the tree
     * @return INA_FAIL if any error, INA_SUCCESS otherwise
     */
    private int createInfoNumbersTree(ListIterator it, GenericTreeNode<InfoNumberItem> root) {
        // Current value of the index level
        byte currentIndex = 0;
        // Record temporary storage
        Hashtable record;

        // Increase level (before first call must be initialized to 0)
        mItemLevel++;
        // Loop by all items with the same index level starting from
        // incoming list index
        while (it.hasNext()) {
            // Get hashtable with information number data (record)
            record = (Hashtable) it.next();
            // If record is null the return fail indicator
            if (record == null) {
                if (DBG) {
                    Log.d(TAG, "createInfoNumbersTree: data record is null");
                }
                return INA_FAIL;
            }
            // Get value of the index level of the read record
            Byte indexLevel = (Byte) record.get(IN_INDEX_LEVEL);
            // Check record value
            if (indexLevel == null) {
                if (DBG) {
                    Log.d(TAG, "createInfoNumbersTree: data record is invalid");
                }
                return INA_FAIL;
            }
            // !!! Exit condition check !!!
            // If read record has upper level then
            if (mItemLevel > indexLevel) {
                // Set upper level
                mItemLevel--;
                // Go back by the list iterator
                if (it.hasPrevious()) {
                    it.previous();
                }
                // Stop this loop
                break;
            }
            // Create new tree node
            GenericTreeNode<InfoNumberItem> node = new GenericTreeNode<InfoNumberItem>();
            // Create new information number item
            InfoNumberItem item = new InfoNumberItem(currentIndex, indexLevel, record);
            // Set node data
            node.mValue = item;
            // Set node parent with adding the curent node to the parents children
            node.setParent(root);
            // Increase item index
            currentIndex++;
            // If item is index then start the fall down operation
            if (item.isIndexEntry()) {
                // Check returned result and return fail indicator if error
                if (INA_SUCCESS != createInfoNumbersTree(it, node)) {
                    if (DBG) {
                        Log.d(TAG, "createInfoNumbersTree: fall down operation is failed");
                    }
                    return INA_FAIL;
                }
            }
        }
        return INA_SUCCESS;
    }

    /**
     * Calls the currently selected/clicked number item.
     *
     * @param position Current list item position
     * @return True if the call was initiated, false otherwise
     */
    private boolean callNumber(int position) {
        // Get item at clicked position
        InfoNumberItem item = (InfoNumberItem) mListView.getItemAtPosition(position);
        // Check for null
        if (item != null) {
            // Get phone number
            String number = item.getNumber();
            // Check for validity
            if (number.length() == 0) {
                if (DBG) {
                    Log.d(TAG, "callNumber: There is no phone number");
                }
                // There is no phone number.
                displayAlert("There is no phone number", false);
                return false;
            }
            // Create dialing intent and try to call this number
            Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                    Uri.fromParts("tel", number, null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
            // Finish our application
            finish();
            return true;
        }
        return false;
    }

    /**
     * Conmmon onClick actions
     *
     * @param position Current list item position
     * @return True if processed and false otherwise
     */
    private boolean onClick(int position) {
        boolean result = false;

        // Set current node to the selected/clicked position
        if (INA_SUCCESS != setCurrentItem(position)) {
            if (DBG) {
                Log.d(TAG, "onClick: setCurrentItem is failed");
            }
            return result;
        }
        // Check enabled indicator of the selected item
        if (mCurrentNode.mValue.isEnabled) {
            // Check for index/number entry
            if (mCurrentNode.mValue.isIndexEntry()) {
                // Store current node alpha tag for title setting
                String title = " - " + mCurrentNode.mValue.getAlphaTag();
                // Create items list of the children level
                if (INA_SUCCESS == getChildrenLevel()) {
                    // Create adapter
                    InfoNumbersAdapter adapter = new InfoNumbersAdapter(this, mCurrentLevelItems);
                    // Bind the array adapter to the listview.
                    mListView.setAdapter(adapter);
                    // Init selection
                    mListView.setSelection(0);
                    // Add new node to path
                    mPath.push(position);
                    // Set title
                    setTitle(getString(R.string.info_numbers) + title);
                    result = true;
                } else {
                    if (DBG) {
                        Log.d(TAG, "onClick: getChildrenLevel is failed");
                    }
                }
            } else {
                // Call number
                result = callNumber(position);
            }
        }
        return result;
    }

    @Override
    /**
     * Overriden handler
     */
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Check initialized state
        if (!mIsInitializedState) {
            if (DBG) {
                Log.d(TAG, "onListItemClick: Application is not initialized");
            }
            return;
        }
        // Check control focus
        if (l.hasFocus()) {
            onClick(position);
        }
    }

    @Override
    /**
     * Overriden handler
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = false;

        // Check initialized state
        if (!mIsInitializedState) {
            if (DBG) {
                Log.d(TAG, "onKeyDown: Application is not initialized");
            }
            return false;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                result = true;
                // Get item by selected position
                int position = mListView.getSelectedItemPosition();
                if (position < 0) {
                    // If no selected item then get first visible
                    position = mListView.getFirstVisiblePosition();
                }
                // Call item click
                onClick(position);
                break;
            case KeyEvent.KEYCODE_BACK:
                // Check for upper level existence
                if (INA_SUCCESS == getParentLevel()) {
                    // Create adapter
                    InfoNumbersAdapter adapter = new InfoNumbersAdapter(this, mCurrentLevelItems);
                    // Bind the adapter to the listview.
                    mListView.setAdapter(adapter);
                    // Get previous selected parent level node index
                    position = mPath.pop();
                    // Select parent level node
                    mListView.setSelection(position);
                    // Set current item to previous selected/clicked parent node
                    if (INA_SUCCESS == setCurrentItem(position)) {
                        // Get parent item of the new current node for title setting
                        InfoNumberItem item = mCurrentNode.getParent().mValue;
                        // Check item
                        if (null != item) {
                            // Get parent alpha tag
                            String title = item.getAlphaTag();
                            // Check alpha tag value
                            if (title.length() > 0) {
                                // Set title
                                setTitle(getString(R.string.info_numbers) + " - " + title);
                            } else {
                                // Set initial title
                                setTitle(getString(R.string.info_numbers));
                            }
                        }
                        result = true;
                    } else {
                        if (DBG) {
                            Log.d(TAG, "onKeyDown: setCurrentItem is failed");
                        }
                        setTitle(getString(R.string.info_numbers));
                    }
                } else {
                    if (DBG) {
                        Log.d(TAG, "onKeyDown: getParentLevel is failed");
                    }
                }
                break;
        }
        if (!result) {
            result = super.onKeyDown(keyCode, event);
        }
        return result;
    }
}
// LGE_CPHS_INFO_NUMBERS END