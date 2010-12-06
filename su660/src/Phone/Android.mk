LOCAL_PATH:= $(call my-dir)

#===============================
# add image files
#===============================
include $(CLEAR_VARS)

target_dir := $(PRODUCT_OUT)/system/media/image/vt
src_dir := $(LOCAL_PATH)/Image

define copy-files
  $(shell mkdir -p $(2)) \
  $(shell cp -a $(1)/* $(2))
endef

$(call copy-files,$(src_dir),$(target_dir))

# Static library with some common classes for the phone apps.
# To use it add this line in your Android.mk
#  LOCAL_STATIC_JAVA_LIBRARIES := com.android.phone.common
include $(CLEAR_VARS)

#Loca library path for NXP
LOCAL_LIB_PATH:= $(LOCAL_PATH)/libs
LOCAL_MODULE_TAGS := eng user debug

LOCAL_SRC_FILES := \
	src/com/android/phone/ButtonGridLayout.java \
	src/com/android/phone/CallLogAsync.java \
	src/com/android/phone/HapticFeedback.java

LOCAL_MODULE := com.android.phone.common
include $(BUILD_STATIC_JAVA_LIBRARY)

# Build the Phone app which includes the emergency dialer. See Contacts
# for the 'other' dialer.
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
        src/com/android/phone/EventLogTags.logtags \
        src/com/android/phone/INetworkQueryService.aidl \
        src/com/android/phone/INetworkQueryServiceCallback.aidl

LOCAL_PACKAGE_NAME := Phone
LOCAL_CERTIFICATE := platform

#Call Settings Korean SKT
ifeq ($(TARGET_STAR_COUNTRY), KR)
$(shell mv -f $(LOCAL_PATH)/src/com/android/phone/ExcuseMessagesEditKor.java $(LOCAL_PATH)/src/com/android/phone/ExcuseMessagesEdit.java)
$(shell mv -f $(LOCAL_PATH)/res/drawable-hdpi/ic_in_call_touch_message_ko.png $(LOCAL_PATH)/res/drawable-hdpi/ic_in_call_touch_message.png)
$(shell mv -f $(LOCAL_PATH)/res/drawable-hdpi/btn_end_call_msg_pressed_ko.png $(LOCAL_PATH)/res/drawable-hdpi/btn_end_call_msg_pressed.png)
$(shell mv -f $(LOCAL_PATH)/res/drawable-hdpi/btn_end_call_msg_normal_ko.png $(LOCAL_PATH)/res/drawable-hdpi/btn_end_call_msg_normal.png)
else
$(shell rm -rf $(LOCAL_PATH)/src/com/android/phone/ExcuseMessagesEditKor.java)
endif


### 20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> for Using Carrier ("SKT", "KT", "LGT")  [START_LGE_LAB] ###
#$(shell rm -rf $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java; cp -rf $(LOCAL_PATH)/CARRIER/Default/src/com/android/phone/PhoneCarrierUtils.java $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java)
#$(shell rm -rf $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java; cp -rf $(LOCAL_PATH)/CARRIER/$(BUILD_CARRIER)/src/com/android/phone/PhoneCarrierUtils.java $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java)
ifeq ($(TARGET_STAR_COUNTRY), KR)
$(shell cp -rf $(LOCAL_PATH)/CARRIER/Default/src/com/android/phone/PhoneCarrierUtils.java $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java)
ifeq ($(TARGET_STAR_OPERATOR), SKT)
$(shell cp -rf $(LOCAL_PATH)/CARRIER/SKT/src/com/android/phone/PhoneCarrierUtils.java $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java)
endif
ifeq ($(TARGET_STAR_OPERATOR), KT)
$(shell cp -rf $(LOCAL_PATH)/CARRIER/KT/src/com/android/phone/PhoneCarrierUtils.java $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java)
endif
ifeq ($(TARGET_STAR_OPERATOR), LGT)
$(shell cp -rf $(LOCAL_PATH)/CARRIER/LGT/src/com/android/phone/PhoneCarrierUtils.java $(LOCAL_PATH)/src/com/android/phone/PhoneCarrierUtils.java)
endif
endif
### 20101020 wonho.moon@lge.com <mailto:wonho.moon@lge.com> for Using Carrier ("SKT", "KT", "LGT")  [END_LGE_LAB] ###


#Video call nxp jar
LOCAL_CLASSPATH := $(LOCAL_LIB_PATH)/com.lifevibes.videotelephony.jar
LOCAL_STATIC_JAVA_LIBRARIES := libVTnxp
include $(BUILD_PACKAGE)

#Video call nxp jar
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libVTnxp:/libs/com.lifevibes.videotelephony.jar
include $(BUILD_MULTI_PREBUILT)


# include videotelephony lib
include $(CLEAR_VARS)
LOCAL_PREBUILT_LIBS := libs/liblifevibes_videotelephony.so
include $(BUILD_MULTI_PREBUILT)


# include libamf-lib
ifeq ($(TARGET_STAR_OPERATOR), SKT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_LIBS := libs/libamf-lib.so
include $(BUILD_MULTI_PREBUILT)
endif

# Build the test package
include $(call all-makefiles-under,$(LOCAL_PATH))
