//
// Created by myb on 12/7/17.
//

#include <jni.h>
#include <stdlib.h>
#include <string>
#include <android/log.h>
#include "RtmpSession.h"

extern "C" {
#ifdef __cplusplus
#define __STDC_CONSTANT_MACROS
#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#include <string.h>
#endif
}

#include "../../../libs/FdkAac/include/aacenc_lib.h"

#define LOG_TAG "FdaAac_jni"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

#define RTMPSESSION_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_rtmppush_RtmpSession_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_rtmppush_RtmpSession_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

std::string ConvertJStringToString(JNIEnv* env, jstring src) {
    if (!src) {
        return nullptr;
    }

    const jsize length = env->GetStringLength(src);
    if (length == 0) {
        return std::string("");
    }

    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("GB2312");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray)env->CallObjectMethod(src, mid, strencode);
    jbyte* ba = env->GetByteArrayElements(barr,JNI_FALSE);

    char* rtn = (char*)malloc(length + 1);
    memcpy(rtn, ba, length);
    rtn[length]=0;

    env->ReleaseByteArrayElements(barr, ba, 0);
    std::string stemp(rtn);
    free(rtn);
    return stemp;
}

RTMPSESSION_FUNC(jlong, RtmpConnect, jstring jurl) {

    const char* url = env->GetStringUTFChars(jurl, 0);
    RtmpSession* session = new RtmpSession();
    int ret = session->Init(url, 3000);
    env->ReleaseStringUTFChars(jurl, url);
    if (ret != 0) {
        LOGE("Init RtmpSession Failed.");
        return 0;
    }
    return reinterpret_cast<long> (session);
}

RTMPSESSION_FUNC(jboolean, RtmpIsConnect, jlong handle) {
    RtmpSession* session = reinterpret_cast<RtmpSession*>(handle);
    return session ? session->IsConnect() ? JNI_TRUE : JNI_FALSE : JNI_FALSE;
}

RTMPSESSION_FUNC(jint, RtmpSendVideoData, jlong handle, jbyteArray buffer, jlong len) {
    jbyte* data = env->GetByteArrayElements(buffer, 0);
    RtmpSession* session = reinterpret_cast<RtmpSession*>(handle);
    int ret = session->SendVideoData((uint8_t *)data, len, 0);
    env->ReleaseByteArrayElements(buffer, data, 0);
    return ret;
}

RTMPSESSION_FUNC(jboolean, RtmpSendAudioData, jlong handle, jbyteArray buffer, jlong len) {
    jbyte* data = env->GetByteArrayElements(buffer, 0);
    RtmpSession* session = reinterpret_cast<RtmpSession*>(handle);
    int ret = session->SendAacData((uint8_t *)data, len, 0);
    env->ReleaseByteArrayElements(buffer, data, 0);
    return ret;
}

RTMPSESSION_FUNC(void, RtmpDisconnect, jlong handle) {
    RtmpSession* session = reinterpret_cast<RtmpSession*>(handle);
    session->Stop();
    delete session;
}