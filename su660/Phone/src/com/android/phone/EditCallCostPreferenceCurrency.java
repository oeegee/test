// LGE_CALL_COSTS START

package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;


import java.util.Map;

/**
 * Class similar to the com.android.settings.EditPinPreference
 * class, with a couple of modifications, including a different layout 
 * for the dialog.
 */
public class EditCallCostPreferenceCurrency extends EditTextPreference {

    private static EditText editTextCurrency;
    
    interface OnCallCostEnteredListenerCurrency {
        void onCallCostEntered(EditCallCostPreferenceCurrency preference, boolean positiveResult);
    }
    
    private OnCallCostEnteredListenerCurrency mCallCostListener;

    public void setOnCallCostEnteredListenerCurrency(OnCallCostEnteredListenerCurrency listener) {
        mCallCostListener = listener;
    }
    
    public EditCallCostPreferenceCurrency(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditCallCostPreferenceCurrency(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected View onCreateDialogView() {
        // set the dialog layout
        setDialogLayoutResource(R.layout.pref_dialog_call_cost_curr);
       
        View dialog = super.onCreateDialogView();
        
        editTextCurrency = (EditText) dialog.findViewById(R.id.call_cost_currency);
        
        return dialog;
    }
    
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

    }
    
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mCallCostListener != null) {
            mCallCostListener.onCallCostEntered(this, positiveResult);
        }
    }
    
    /**
     * Returns the {@link EditText} widget that will be shown in the dialog.
     * 
     * @return The {@link EditText} widget that will be shown in the dialog.
     */
    public EditText getEditText() {
        return editTextCurrency;
    }
    
    /**
     * Gets the text from the {@link SharedPreferences}.
     * 
     * @return The current preference value.
     */
    public String getText() {
        return editTextCurrency.getText().toString();
    }
    
    /**
     * Saves the text to the {@link SharedPreferences}.
     * 
     * @param text The text to save
     */
    /*public void setText(String text) {

    	editTextCurrency.setText(text);

    }*/
    
}
//LGE_CALL_COSTS END