LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/third_party/fdk-aac-0.1.5/libAACenc/include \
                    $(LOCAL_PATH)/third_party/fdk-aac-0.1.5/libSYS/include

LOCAL_MODULE    := FdkAac-jni
LOCAL_CFLAGS    := -frtti -fexceptions -std=c++11
LOCAL_SRC_FILES := src/main/cpp/FdkAac_jni.cc
LOCAL_SHARED_LIBRARIES := FraunhoferAAC
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)

# 这个需要放在最后引用
include $(LOCAL_PATH)/third_party/fdk-aac-0.1.5/Android.mk