// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-06 : Phone Carrier Utils for "KT"

package com.android.phone;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-23 : not save call db
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyProperties;
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-23 : not save call db

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-14 : CNAP - if CNAP & PhoneBook, toggling display
import android.os.Handler;
import android.os.Message;
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-14 : CNAP - if CNAP & PhoneBook, toggling display
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-23 : not save call db
import android.os.SystemProperties;
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-23 : not save call db
import android.pim.ContactsAsyncHelper;
import android.pim.ContactsAsyncHelper.OnImageLoadCompleteListener;
import android.provider.CallLog;
import android.provider.ContactsContract.Contacts;
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-17 : Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber)
import android.telephony.PhoneNumberUtils;
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-17 : Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber)
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
import android.content.ContentResolver;
import com.android.internal.telephony.PhoneFactory;
import android.telephony.ServiceState;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
import com.android.internal.telephony.RAD.RADCarrierUtil;
import com.android.internal.telephony.RAD.RADCarrierUtilProxy;
import com.android.internal.telephony.RAD.RADCarrierUtilFactory;


public class PhoneCarrierUtils 
{
    private static final String LOG_TAG = "PhoneCarrierUtils(KT)";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);


    private PhoneCarrierUtils(){
    }

    static boolean updateDisplayForPerson(CallerInfo info, int presentation, boolean isTemporary, Call call
    		, CallCard mCallCard, float mDensity, int mTextColorDefaultSecondary
    		, ImageView mPhoto, Button mManageConferencePhotoButton
    		, TextView mName, TextView mPhoneNumber, TextView mLabel, TextView mSocialStatus
    		, TextView mNameSecond, TextView mPhoneNumberSecond) {
        if (DBG) log("updateDisplayForPerson(" + info + ")\npresentation:" +
                     presentation + " isTemporary:" + isTemporary);

        String name = null;
        String displayNumber = null;
        String label = null;
        Uri personUri = null;
        String socialStatusText = null;
        Drawable socialStatusBadge = null;

        Call.State state = call.getState();

        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

// for Test  CNIP, CDNIP, CNAP
//info.cnapName = "CNAP Test!!!!!";
//info.name = "PhoneBook Name";
//info.phoneNumber = "010-1111-7777";
//presentation = Connection.PRESENTATION_UNKNOWN;
//info.cdnipNumber = "010-2222-7777";
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : presentation = " + presentation);
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : info.phoneNumber = " + info.phoneNumber);
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : info.cnapName = " + info.cnapName);
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : info.name = " + info.name);
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : info.cdnipNumber = " + info.cdnipNumber);
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : info.orignalNumber = " + info.orignalNumber);
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : info.contactNumber = " + info.contactNumber);
if (DBG) log("PhoneCarrierUtils(KT) updateDisplayForPerson() : info.displayNumber = " + info.displayNumber);
// for Test  CNIP, CDNIP, CNAP


            if (TextUtils.isEmpty(info.name)) {
                if (TextUtils.isEmpty(info.phoneNumber)) {
                    name =  getPresentationString(mCallCard.getContext(), presentation);

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-04 : KT CLIRO (Call Line Identification Restriction Overide)
                //} else if (presentation != Connection.PRESENTATION_ALLOWED) {
                } else if (presentation != Connection.PRESENTATION_ALLOWED && TextUtils.isEmpty(info.phoneNumber)) {
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-04 : KT CLIRO (Call Line Identification Restriction Overide)
                    // This case should never happen since the network should never send a phone #
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    name = getPresentationString(mCallCard.getContext(), presentation);

                } else if (!TextUtils.isEmpty(info.cnapName)) {
                    name = info.cnapName;
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-14 : CNAP - for CNAP Handling of CallHistory, add comment
                    //info.name = info.cnapName;
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-14 : CNAP - for CNAP Handling of CallHistory, add comment
                    displayNumber = info.phoneNumber;

                } else {
                    name = info.phoneNumber;
                }
            } else {
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-04 : KT CLIRO (Call Line Identification Restriction Overide)
                //if (presentation != Connection.PRESENTATION_ALLOWED) {
                if (presentation != Connection.PRESENTATION_ALLOWED && TextUtils.isEmpty(info.phoneNumber)) {
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-04 : KT CLIRO (Call Line Identification Restriction Overide)
                    // This case should never happen since the network should never send a name
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    name = getPresentationString(mCallCard.getContext(), presentation);

                } else {
// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-14 : CNAP - if CNAP & PhoneBook, toggling display
                    if (call.isRinging() && !TextUtils.isEmpty(info.cnapName)) {
                        startToggling(call, info, mName);
                        name = info.cnapName;
                    } else {
                        stopToggling();
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-14 : CNAP - if CNAP & PhoneBook, toggling display
                        name = info.name;
                    }

                    displayNumber = info.phoneNumber;
                    label = info.phoneLabel;
                }
            }
            personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, info.person_id);
        } else {
            name =  getPresentationString(mCallCard.getContext(), presentation);
        }

        if (call.isGeneric()) {
            mName.setText(R.string.card_title_in_call);
        } else {
            mName.setText(name);
        }
        mName.setVisibility(View.VISIBLE);

        // Update mPhoto
        // if the temporary flag is set, we know we'll be getting another call after
        // the CallerInfo has been correctly updated.  So, we can skip the image
        // loading until then.

        // If the photoResource is filled in for the CallerInfo, (like with the
        // Emergency Number case), then we can just set the photo image without
        // requesting for an image load. Please refer to CallerInfoAsyncQuery.java
        // for cases where CallerInfo.photoResource may be set.  We can also avoid
        // the image load step if the image data is cached.
        if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
            mPhoto.setVisibility(View.INVISIBLE);
        } else if (info != null && info.photoResource != 0){
            CallCard.showImage(mPhoto, info.photoResource);
        } else if (!CallCard.showCachedImage(mPhoto, info)) {
            // Load the image with a callback to update the image state.
            // Use the default unknown picture while the query is running.
            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(
                info, 0, (OnImageLoadCompleteListener)mCallCard, call, mCallCard.getContext(), mPhoto, personUri, R.drawable.picture_unknown);
        }
        // And no matter what, on all devices, we never see the "manage
        // conference" button in this state.
        mManageConferencePhotoButton.setVisibility(View.INVISIBLE);

        if (displayNumber != null && !call.isGeneric()) {
            mPhoneNumber.setText(displayNumber);
            mPhoneNumber.setTextColor(mTextColorDefaultSecondary);
            mPhoneNumber.setVisibility(View.VISIBLE);
        } else {
            mPhoneNumber.setVisibility(View.GONE);
        }

        if (label != null && !call.isGeneric()) {
            mLabel.setText(label);
            mLabel.setVisibility(View.VISIBLE);
        } else {
            mLabel.setVisibility(View.GONE);
        }

        // "Social status": currently unused.
        // Note socialStatus is *only* visible while an incoming
        // call is ringing, never in any other call state.
        if ((socialStatusText != null) && call.isRinging() && !call.isGeneric()) {
            mSocialStatus.setVisibility(View.VISIBLE);
            mSocialStatus.setText(socialStatusText);
            mSocialStatus.setCompoundDrawablesWithIntrinsicBounds(
                    socialStatusBadge, null, null, null);
            mSocialStatus.setCompoundDrawablePadding((int) (mDensity * 6));
        } else {
            mSocialStatus.setVisibility(View.GONE);
        }

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-17 : Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber)
// for test
//info.cdnipNumber = "010-2222-7777";
// for test
        if ((state == Call.State.INCOMING || state == Call.State.WAITING) && !TextUtils.isEmpty(info.cdnipNumber)) {
            mNameSecond.setText(mCallCard.getContext().getString(R.string.CALLSTR_ARRIVEDNUM));
            String displaySecondNumber = PhoneNumberUtils.formatNumber(info.cdnipNumber);
            mPhoneNumberSecond.setText(displaySecondNumber);
            mNameSecond.setVisibility(View.VISIBLE);
            mPhoneNumberSecond.setVisibility(View.VISIBLE);
        } else {
            mNameSecond.setText("");
            mPhoneNumberSecond.setText("");
            mNameSecond.setVisibility(View.GONE);
            mPhoneNumberSecond.setVisibility(View.GONE);
        }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-17 : Second Number Display (SKT NumberPlus, KT TwoNumber, LGT DualNumber)

        return true;
    }

    static String getPresentationString(Context context, int presentation) {
        String pi_string = "";

        if (presentation == Connection.PRESENTATION_RESTRICTED) {
            pi_string = context.getString(R.string.private_num);
        } else if (presentation == Connection.PRESENTATION_UNKNOWN) {
            pi_string = context.getString(R.string.CALLSTR_UNKNOWN_KTF);
        } /*else if (presentation == Connection.PRESENTATION_PAYPHONE) {
            pi_string = getContext().getString(R.string.payphone);
        }*/ else {
            pi_string = context.getString(R.string.CALLSTR_UNKNOWN_KTF);
        }
        return pi_string;
    }

    static int getChangedCallLogType(int callLogType, final int presentation, final String number, final String cdnipNumber){
        if (DBG) log("getChangedCallLogTypeKT() : callLogType = " + callLogType);

        /*final*/ int changedCallLogType = callLogType;

        switch(callLogType){
            case CallLog.Calls.INCOMING_TYPE: // Android Native Call Type
            case CallLog.Calls.CALLTYPE_RECE_CALLEDBY:
                if((presentation == Connection.PRESENTATION_RESTRICTED) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLEDBY_RES;
                else if((presentation == Connection.PRESENTATION_UNKNOWN) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLEDBY_UNA;
                else if(number.length() == 0)
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLEDBY_NONUM;
                break;

            case CallLog.Calls.MISSED_TYPE: // Android Native Call Type
            case CallLog.Calls.CALLTYPE_MISS_CALLEDBY:
                if((presentation == Connection.PRESENTATION_RESTRICTED) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLEDBY_RES;
                else if((presentation == Connection.PRESENTATION_UNKNOWN) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLEDBY_UNA;
                else if(number.length() == 0)
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLEDBY_NONUM;
                break;

            case CallLog.Calls.CALLTYPE_RECE_CALLWAITING:
                if((presentation == Connection.PRESENTATION_RESTRICTED) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLWAITING_RES;
                else if((presentation == Connection.PRESENTATION_UNKNOWN) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLWAITING_UNA;
                else if(number.length() == 0)
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLWAITING_NONUM;
                break;

            case CallLog.Calls.CALLTYPE_MISS_CALLWAITING:
                if((presentation == Connection.PRESENTATION_RESTRICTED) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLWAITING_RES;
                else if((presentation == Connection.PRESENTATION_UNKNOWN) && (number.length() > 0))
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLWAITING_UNA;
                else if(number.length() == 0)
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLWAITING_NONUM;
                break;

            default:
                break;
        }

        if(isKTFCiss()){
            switch(changedCallLogType){
                case CallLog.Calls.INCOMING_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_NONUM;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_RES;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_UNA;
                    break;
                case CallLog.Calls.MISSED_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_NONUM;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_RES;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_UNA;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_NONUM;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_RES;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_UNA;
                    break;
                case CallLog.Calls.REJECTED_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_RECE_CALLREJECT:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLREJECT;
                    break;
                default:
                    break;
            }
        }

        if(isKTFMMCiss()){
            switch(changedCallLogType){
                case CallLog.Calls.INCOMING_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY:	
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLEDBY;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_NONUM:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLEDBY_NONUM;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_RES:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLEDBY_RES;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_UNA:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLEDBY_UNA;
                    break;
                case CallLog.Calls.MISSED_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY	:
                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_MMCISSCALLEDBY;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_NONUM:
                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_MMCISSCALLEDBY_NONUM;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_RES:
                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_MMCISSCALLEDBY_RES;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_UNA:
                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_MMCISSCALLEDBY_UNA;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLWAITING;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_NONUM:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLWAITING_NONUM;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_RES:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_RES	:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLWAITING_RES;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_UNA:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_UNA	:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLWAITING_UNA;
                    break;
                case CallLog.Calls.REJECTED_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_RECE_CALLREJECT:
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLREJECT:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_MMCISSCALLREJECT;
                    break;
                default	:
                    break;
            }
        }

        if(!TextUtils.isEmpty(cdnipNumber)){
            switch( changedCallLogType ) {
                case CallLog.Calls.INCOMING_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLEDBY_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLEDBY_NONUM_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLEDBY_RES_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLEDBY_UNA_PLUS;
                    break;

                case CallLog.Calls.MISSED_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLEDBY_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLEDBY_NONUM_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLEDBY_RES_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLEDBY_UNA_PLUS;
                    break;

                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLWAITING_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLWAITING_NONUM_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLWAITING_RES_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CALLWAITING_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLWAITING_UNA_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLWAITING:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLWAITING_PLUS;
                    break;

                case CallLog.Calls.REJECTED_TYPE: // Android Native Call Type
                case CallLog.Calls.CALLTYPE_RECE_CALLREJECT:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CALLREJECT_PLUS;
                    break;

                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_NONUM_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_RES_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLEDBY_UNA_PLUS;
                    break;

                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_NONUM_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_RES_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CISSCALLEDBY_UNA_PLUS;
                    break;

                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_NONUM:	
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_NONUM_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_RES_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLWAITING_UNA_PLUS;
                    break;

                case CallLog.Calls.CALLTYPE_MISS_CALLWAITING_NONUM:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLWAITING_NONUM_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLWAITING_RES:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLWAITING_RES_PLUS;
                    break;
                case CallLog.Calls.CALLTYPE_MISS_CALLWAITING_UNA:
                    changedCallLogType = CallLog.Calls.CALLTYPE_MISS_CALLWAITING_UNA_PLUS;
                    break;

                case CallLog.Calls.CALLTYPE_RECE_CISSCALLREJECT:
                    changedCallLogType = CallLog.Calls.CALLTYPE_RECE_CISSCALLREJECT_PLUS;
                    break;

                default	:
                    break;
            }
        }

        if (DBG) log("getChangedCallLogTypeKT() : changedCallLogType = " + changedCallLogType);
        return changedCallLogType;
    }

    private static boolean isKTFCiss(){
/*        if(lge_IsCissText() || lge_IsCissMelody() || lge_GetCNAPLen())
            return true;
        else*/
            return false;
    }

    private static boolean isKTFMMCiss(){
        return false;
    }

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-17 : SKT CallMeFree and MessageCall
    static String removeSymbol(int changedCallLogType, final String number){
        return number;
    }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-17 : SKT CallMeFree and MessageCall

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : display number
    static void setCallerInfoDisplayNumber(Connection conn, CallerInfo ci) {
        if (conn == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid Connection");
            return;
        }

        String connPhoneNumber = conn.getAddress();

        if (TextUtils.isEmpty(connPhoneNumber)) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid connection phone number");
            return;
        }

        if (ci == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid CallerInfo");
            return;
        }

        if (DBG) log("setCallerInfoDisplayNumber() phoneNumber : " + ci.phoneNumber + " , displayNumber : " + ci.displayNumber);
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-07 : call : roaming : process number : display number

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number
    static void processSpecialCenterNumber(Connection conn, CallerInfo ci) {
        if (conn == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid Connection");
            return;
        }

        String connPhoneNumber = conn.getAddress();

        if (TextUtils.isEmpty(connPhoneNumber)) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid connection phone number");
            return;
        }

        if (ci == null) {
            if (DBG) log("setCallerInfoDisplayNumber(): invalid CallerInfo");
            return;
        }

        String originalNumber = conn.getOriginalNumber();

        if (DBG) log("processSpecialCenterNumber() connPhoneNumber : " + connPhoneNumber + " , originalNumber : " + originalNumber + " , CallerInfo : " + ci);
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-13 : call : roaming : process number : special center number

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-07-06 : call : roaming : process number : call feature code
    static void processCallFeatureCode(Connection conn, CallerInfo ci) {
        if (conn == null) {
            if (DBG) log("processCallFeatureCode(): invalid Connection");
            return;
        }

        String connPhoneNumber = conn.getAddress();

        if (TextUtils.isEmpty(connPhoneNumber)) {
            if (DBG) log("processCallFeatureCode(): invalid connection phone number");
            return;
        }

        if (ci == null) {
            if (DBG) log("processCallFeatureCode(): invalid CallerInfo");
            return;
        }
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-07-06 : call : roaming : process number : call feature code

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-14 : CNAP - if CNAP & PhoneBook, toggling display
    private static final int TOGGLE_DISPLAY_TIME = 3000;  // 3 sec (same as feature phone)
    private static final int TOGGLE_DISPLAY_EVENT = 100;

    private static Call mCall = null;
    private static CallerInfo mCallerInfo = null;

    private static boolean mIsToggling = false;    // during toggling
    private static boolean mToggleValue = false; // cnap and pbname toggle
    private static TextView mNameDisplay = null;
    private static String mCnapName = null;
    private static String mPbName = null;

    static Handler mToggleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TOGGLE_DISPLAY_EVENT:
                    displayToggling();
                    break;
            }
        }
    };

    private static void startToggling(Call call, CallerInfo info, TextView mName) {
        if (!mIsToggling) {
            mCall = call;
            mCallerInfo = info;

            mIsToggling = true;
            mToggleValue = true;
            mNameDisplay = mName;
            mCnapName = info.cnapName;
            mPbName = info.name;
            mToggleHandler.sendEmptyMessageDelayed(TOGGLE_DISPLAY_EVENT, TOGGLE_DISPLAY_TIME);

            if (DBG) log("startToggling(PhoneCarrierUtils)");
        }
    }

    private static void displayToggling() {
        if ((mCall == null) || ((mCall != null) && !mCall.isRinging())) {
            stopToggling();
        } else {
            mToggleValue = !mToggleValue;
            if (mToggleValue)
                mNameDisplay.setText(mCnapName);
            else
                mNameDisplay.setText(mPbName);
            mToggleHandler.sendEmptyMessageDelayed(TOGGLE_DISPLAY_EVENT, TOGGLE_DISPLAY_TIME);

            if (DBG) log("displayToggling(PhoneCarrierUtils) : mToggleValue = " + mToggleValue);
        }
    }

    private static void stopToggling() {
        if (mIsToggling) {
            mCall = null;
            mCallerInfo = null;

            mIsToggling = false;
            mToggleValue = false;
            mNameDisplay = null;
            mCnapName = null;
            mPbName = null;
            mToggleHandler.removeMessages(TOGGLE_DISPLAY_EVENT);

            if (DBG) log("stopToggling(PhoneCarrierUtils)");
        }
    }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-14 : CNAP - if CNAP & PhoneBook, toggling display

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-17 : Reject Message
    static boolean sendRejectMsg(Activity activity, String phoneNumber, String rejectMsg) {
        Intent intent = new Intent(android.provider.Telephony.Sms.Intents.SEND_SMS_REQUST_BACKGROUND_ACTION);
        intent. putExtra("address", phoneNumber);
        intent. putExtra("sms_body", rejectMsg);
        activity.sendBroadcast(intent, "android.permission.RECEIVE_SMS");

        return true; // true : need to setInCallScreenMode(NORMAL), false : not need to setInCallScreenMode(NORMAL)
    }

    static void editRejectMsg(Activity activity, String phoneNumber, String rejectMsg) {
        Intent rejectMsgIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mmssms", phoneNumber, null));
        rejectMsgIntent. putExtra("sms_body", rejectMsg);
        rejectMsgIntent. putExtra("exit_on_sent", true);
        activity.startActivity(rejectMsgIntent);
    }

    static void newRejectMsg(Activity activity, String phoneNumber) {
        Intent rejectMsgIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mmssms", phoneNumber, null));
        rejectMsgIntent. putExtra("sms_body", "");
        rejectMsgIntent. putExtra("exit_on_sent", true);
        activity.startActivity(rejectMsgIntent);
    }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-14 : Reject Message

// LGE_CHANGE_S [shinhae.lee@lge.com] 2010-05-23 : not save call db
    static boolean getIsNoSaveCallDB(Phone phone) {
// LGE_CHANGE_S [yongjin.her@lge.com] 2010-04-01 : not add test call
        if ("1".equals(SystemProperties.get(TelephonyProperties.PROPERTY_TEST_CALL))) {
            log("getIsNoSaveCallDB() : test mode = " + SystemProperties.get(TelephonyProperties.PROPERTY_TEST_CALL));
            return true;
// LGE_CHANGE_E [yongjin.her@lge.com] 2010-04-01 : not add test call
        } else {
            // TODO
        }

        return false;
    }
// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-23 : not save call db

// LGE_CHANGE_S [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call
    static String doServiceStatusCheck(Context context, Intent intent) {
        String msgReasonNotAble = null;

        if (context == null || intent == null) {
            if (DBG) log("doServiceStatusCheck() Invalid Parameter : " + context + ", " + intent);
            return msgReasonNotAble;
        }

        String action = intent.getAction();
        String phoneNumber;

        if (action == null) {
            phoneNumber = null;
        } else if (action.equals(Intent.ACTION_CALL) && intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        } else {
            phoneNumber = PhoneNumberUtils.getNumberFromIntent(intent, context);
        }

        ContentResolver contentResolver = context.getContentResolver();
        ServiceState curServiceState = PhoneFactory.getDefaultPhone().getServiceState();
        boolean isEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(phoneNumber);

        if (Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
            msgReasonNotAble = PhoneApp.getInstance().getResources().getString(R.string.FlightModeSetAsOn);
        } else if (curServiceState.getState() != ServiceState.STATE_IN_SERVICE) {
            if (curServiceState.getState() == ServiceState.STATE_POWER_OFF) {
                msgReasonNotAble = PhoneApp.getInstance().getResources().getString(R.string.ServiceRequired);
            } else if (isEmergencyNumber == false) {
                msgReasonNotAble = PhoneApp.getInstance().getResources().getString(R.string.NrcNoServiceArea);
            }
        }

        return msgReasonNotAble;
    }
// LGE_CHANGE_E [idisjhwan.lee@lge.com] 2010-05-27 : call : reject outgoing voice call

// LGE_CHANGE_S [janghun.cheong@lge.com]	
    public static String formatIccPhoneNumberToDisplay(String aPhoneNumber){
        if(TextUtils.isEmpty(aPhoneNumber) == false){
           String phoneNumber = aPhoneNumber.trim();

           if(phoneNumber.indexOf("+82") == 0)
              return "0" + phoneNumber.substring(3);
        }
        return aPhoneNumber;
    }
// LGE_CHANGE_E [janghun.cheong@lge.com]

///////////////////////////////////////////////////////////////////////////////////////////////////
// log
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}

// LGE_CHANGE_E [shinhae.lee@lge.com] 2010-05-06 : Phone Carrier Utils for "KT"
