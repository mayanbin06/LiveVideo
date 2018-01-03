//
// Created by myb on 12/7/17.
//

#include <jni.h>
#include <stdlib.h>
#include <string>
#include <android/log.h>
#include "SrsRtmp.h"

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

#define LOG_TAG "SrsRtmp"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

#define SRSRTMP_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_rtmp_SrsRtmp_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_rtmp_SrsRtmp_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

std::string ConvertJStringToString(JNIEnv* env, jstring src) {
    if (!src) {
        return NULL;
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

SRSRTMP_FUNC(jlong, Connect, jstring jurl) {

    const char* url = env->GetStringUTFChars(jurl, 0);
    SrsRtmp* session = new SrsRtmp(url, 3000);
    env->ReleaseStringUTFChars(jurl, url);
    if (!session->Connect()) {
        LOGE("Init RtmpSession Failed, [%s] ", session->url().c_str());
        return 0;
    }
    return reinterpret_cast<long> (session);
}

SRSRTMP_FUNC(jboolean, IsConnect, jlong handle) {
    SrsRtmp* session = reinterpret_cast<SrsRtmp*>(handle);
    return session ? session->IsConnect() ? JNI_TRUE : JNI_FALSE : JNI_FALSE;
}

SRSRTMP_FUNC(jint, SendH264Data, jlong handle, jbyteArray buffer, jlong len) {
    jbyte* data = env->GetByteArrayElements(buffer, 0);
    SrsRtmp* session = reinterpret_cast<SrsRtmp*>(handle);
    int ret = session->SendH264Data((uint8_t *)data, len, 0);
    env->ReleaseByteArrayElements(buffer, data, 0);
    return ret;
}

SRSRTMP_FUNC(jboolean, SendAacData, jlong handle, jbyteArray buffer, jlong len) {
    jbyte* data = env->GetByteArrayElements(buffer, 0);
    SrsRtmp* session = reinterpret_cast<SrsRtmp*>(handle);
    int ret = session->SendAacData((uint8_t *)data, len, 0);
    env->ReleaseByteArrayElements(buffer, data, 0);
    return ret;
}

SRSRTMP_FUNC(jboolean, SetAudioParams, jlong handle,
             jint sampleRate, jint channelCount, jint sampleFormat) {
    SrsRtmp* session = reinterpret_cast<SrsRtmp*>(handle);
    session->SetAudioParams(sampleRate, channelCount, sampleFormat);
    return 0;
}

SRSRTMP_FUNC(jboolean, SetVideoParams, jlong handle, jint frameRate) {
  SrsRtmp* session = reinterpret_cast<SrsRtmp*>(handle);
  session->SetVideoParams(frameRate);
  return 0;
}

SRSRTMP_FUNC(void, Disconnect, jlong handle) {
    SrsRtmp* session = reinterpret_cast<SrsRtmp*>(handle);
    //session->Stop();
    delete session;
}
