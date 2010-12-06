// LGE_TWO_LINE_SERVICE START
package com.android.phone;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gsm.GSMPhone;

import android.content.Context;
import android.os.AsyncResult;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.TelephonyIntents;

public class TwoLineServicePreference extends ListPreference {
    
    private static final String LOG_TAG = "TwoLineServicePreference";
    private final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);
    private MyHandler mHandler = new MyHandler();
    Phone phone;
    TimeConsumingPreferenceListener tcpListener;
    private int lineServiceCount;
    private final int FIRST_LINE = 1;
    private final int SECOND_LINE = 2;
    private Context mContext;
    private int mLastGoodValue;


    public boolean isTwoLineServiceSupported() {
        if (!phone.equals(null)) {
            if (DBG) Log.d(LOG_TAG, "isTwoLineServiceSupported");
            return phone.isTwoLineSupported();
        }
        return false;
    }
    public TwoLineServicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        phone = PhoneFactory.getDefaultPhone();
        lineServiceCount = 0;
        mContext = context;
    }

    public TwoLineServicePreference(Context context) {
        this(context, null);
        mContext = context;
    }
    public int getLineServiceCount() {
        return lineServiceCount;
        
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
         if (DBG) Log.d(LOG_TAG, "onDialogClosed: positiveResult = " + positiveResult );
          if (DBG) Log.d(LOG_TAG, "onDialogClosed:  index = " + findIndexOfValue(getValue()));
        phone.setServiceLine(findIndexOfValue(getValue()) + 1,
            mHandler.obtainMessage(MyHandler.MESSAGE_SET_LINE));
        if (DBG) Log.d(LOG_TAG, "onDialogClosed: setServiceLine");
        if (tcpListener != null) {
            if (DBG) Log.d(LOG_TAG, "onDialogClosed: tcpListener != null ");
            tcpListener.onStarted(this, false);
             if (DBG) Log.d(LOG_TAG, "onDialogClosed: onStarted");
        }
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading) {
        tcpListener = listener;
        if (DBG) Log.d(LOG_TAG, "init: skipReading = " + skipReading );
        if (!skipReading) {
            phone.getServiceLine(mHandler.obtainMessage(MyHandler.MESSAGE_GET_LINE,
                    MyHandler.MESSAGE_GET_LINE, MyHandler.MESSAGE_GET_LINE));
            if (tcpListener != null) {
                tcpListener.onStarted(this, true);
            }
        }
    }

    void handleGetTwoLineServiceResult(int tmpLineServiceCount) {
        lineServiceCount = tmpLineServiceCount;
        if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResult: lineServiceCount = " + lineServiceCount);
        int value = CommandsInterface.FIRST_LINE;
        switch (tmpLineServiceCount) {
            case 1:
                value = CommandsInterface.FIRST_LINE;
                break;
            case 2:
                value = CommandsInterface.SECOND_LINE;
                break;
            default:
                value = CommandsInterface.FIRST_LINE;
                break;
        }
        mLastGoodValue = value;
        if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResult: value = " + value  + " mLastGoodValue = " + mLastGoodValue );
     
        setValueIndex(value);

        // set the string summary to reflect the value
        int summary = R.string.no_selected_line;
        if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResult: summary = " + summary );
        switch (value) {
            case CommandsInterface.FIRST_LINE:
                summary = R.string.first_line_is_selected;
                break;
            case CommandsInterface.SECOND_LINE:
                summary = R.string.second_line_is_selected;
                break;
        }
          if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResult: setSummaryBegin = ");
        setSummary(summary);
          if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResult: setSummaryEnd = ");
    }

    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_LINE = 0;
        private static final int MESSAGE_SET_LINE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_LINE:
                    handleGetTwoLineServiceResponse(msg);
                    break;
                case MESSAGE_SET_LINE:
                    handleSetTwoLineServiceResponse(msg);
                    break;
        }
    }

        private void handleGetTwoLineServiceResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResponse: msg = " + msg + "msg.arg1 = " + msg.arg1);
            if (msg.arg1 == MESSAGE_SET_LINE) {
                tcpListener.onFinished(TwoLineServicePreference.this, false);
            } else {
                tcpListener.onFinished(TwoLineServicePreference.this, true);
            }
            lineServiceCount = 0;
            if (ar.exception != null) {
                if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResponse: ar.exception="+ar.exception);
                setEnabled(false);
                tcpListener.onError(TwoLineServicePreference.this, EXCEPTION_ERROR);
            } else if (ar.userObj instanceof Throwable) {
                tcpListener.onError(TwoLineServicePreference.this, RESPONSE_ERROR);
            } else {
                int lineServiceCount = ((int[])ar.result)[0];
                if (DBG) Log.d(LOG_TAG, "handleGetTwoLineServiceResponse: twoLineService successfully queried, lineServiceCount="
                        + lineServiceCount);
                handleGetTwoLineServiceResult(lineServiceCount);
            }
        }

        private void handleSetTwoLineServiceResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (DBG) Log.d(LOG_TAG, "handleSetTwoLineServiceResponse: msg = " + msg + "ar.exception = " + ar.exception);
            if (ar.exception != null) {
                if (DBG) Log.d(LOG_TAG, "handleSetTwoLineServiceResponse: ar.exception="+ar.exception);
                setValueIndex(mLastGoodValue);
            }
            if (ar.exception == null) {
                Intent twoLineServiceStateChanged = null;
                if (DBG) Log.d(LOG_TAG, "handleSetTwoLineServiceResponse: lineServiceCount = " + lineServiceCount);
                switch(lineServiceCount) {
                    case FIRST_LINE:
                        twoLineServiceStateChanged = new Intent(TelephonyIntents.ACTION_SET_FIRST_LINE);
                        if (DBG) Log.d(LOG_TAG, "handleSetTwoLineServiceResponse: lineServiceCount == FIRST_LINE");
                        break;
                    case SECOND_LINE:
                        twoLineServiceStateChanged = new Intent(TelephonyIntents.ACTION_SET_SECOND_LINE);
                        if (DBG) Log.d(LOG_TAG, " handleSetTwoLineServiceResponse: lineServiceCount == SECOND_LINE");
                        break;
                    default:
                        if (DBG) Log.d(LOG_TAG, "handleSetTwoLineServiceResponse: default case");
                        break;  
                }
                if (twoLineServiceStateChanged != null) {
                    mContext.sendBroadcast(twoLineServiceStateChanged);
                    if (DBG) Log.d(LOG_TAG, "handleSetTwoLineServiceResponse: sendBroadcast");
                }
            }
            if (DBG) Log.d(LOG_TAG, "handleSetTwoLineServiceResponse: re get");
            phone.getServiceLine(obtainMessage(MESSAGE_GET_LINE,
                    MESSAGE_SET_LINE, MESSAGE_SET_LINE, ar.exception));
            ((PhoneProxy)phone).notifyCallForwardingIndicator();
        }
    }
}
// LGE_TWO_LINE_SERVICE END
