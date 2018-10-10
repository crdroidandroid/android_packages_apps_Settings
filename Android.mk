LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        $(call all-logtags-files-under, src)

LOCAL_MODULE := settings-logtags

include $(BUILD_STATIC_JAVA_LIBRARY)

# Build the Settings APK
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := Settings
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform
LOCAL_OVERRIDES_PACKAGES := Doze OnePlusDoze XiaomiDoze
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS := optional
LOCAL_USE_AAPT2 := true

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, ../crDroidSettings/src)
LOCAL_ASSET_DIR := packages/apps/crDroidSettings/assets

LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-slices-builders \
    android-slices-core \
    android-slices-view \
    android-support-compat \
    android-support-v4 \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-palette \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v14-preference \

LOCAL_JAVA_LIBRARIES := \
    bouncycastle \
    telephony-common \
    ims-common

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-arch-lifecycle-runtime \
    android-arch-lifecycle-extensions \
    guava \
    jsr305 \
    settings-logtags \
    org.lineageos.platform.internal

LOCAL_STATIC_JAVA_LIBRARIES += \
    android-support-v7-cardview \
    android-support-design \
    libcwac

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    frameworks/support/design/res \
    frameworks/support/v7/preference/res \
    frameworks/support/v14/preference/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/recyclerview/res \
    frameworks/support/v7/cardview/res \
    packages/apps/crDroidSettings/res

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.preference \
    --extra-packages android.support.v14.preference \
    --extra-packages android.support.v17.preference \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages android.support.v7.cardview \
    --extra-packages android.support.design \
    --extra-packages com.crdroid.settings

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include frameworks/opt/setupwizard/library/common-gingerbread.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libcwac:../crDroidSettings/libs/cwac-wakeful-1.1.0.jar

include $(BUILD_MULTI_PREBUILT)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
