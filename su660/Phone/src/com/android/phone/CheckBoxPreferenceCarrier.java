package com.android.phone;

import com.lge.IBuildInfo;
import com.lge.ICarrier;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CheckBoxPreferenceCarrier extends CheckBoxPreference {

	public CheckBoxPreferenceCarrier(Context context) {
		super(context, null);
	}

	public CheckBoxPreferenceCarrier(Context context, AttributeSet attrs) {
		super(context, attrs, com.android.internal.R.attr.checkBoxPreferenceStyle);
	}

	public CheckBoxPreferenceCarrier(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		if (IBuildInfo.Carrier == ICarrier.KT) {
	        // Sync the summary view
	        TextView summaryView = (TextView) view.findViewById(com.android.internal.R.id.summary);
	        summaryView.setMaxLines(3);
		}
	}

}