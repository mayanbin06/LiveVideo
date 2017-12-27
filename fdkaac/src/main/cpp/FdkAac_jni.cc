//
// Created by myb on 12/5/17.
//
#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include <aacenc_lib.h>

extern "C" {
#ifdef __cplusplus
#define __STDC_CONSTANT_MACROS
#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#include <string.h>
#include <assert.h>
#endif
}

#include "aacenc_lib.h"

#define LOG_TAG "FdaAac_jni"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

#define FDKAAC_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_fdkaac_FdkAacEncode_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_fdkaac_FdkAacEncode_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

// init and return the handle.
FDKAAC_FUNC(jlong, FdkAacInit, jint sampleRate, jint channels) {
  HANDLE_AACENCODER m_aacEncHandle;
  AACENC_InfoStruct info = { 0 };

  CHANNEL_MODE mode = MODE_1;
  switch (channels) {
    case 1: mode = MODE_1;       break;
    case 2: mode = MODE_2;       break;
    case 3: mode = MODE_1_2;     break;
    case 4: mode = MODE_1_2_1;   break;
    case 5: mode = MODE_1_2_2;   break;
    case 6: mode = MODE_1_2_2_1; break;
  }

  if (aacEncOpen(&m_aacEncHandle, 0, channels) != AACENC_OK) {
    LOGE("Unable to open fdkaac encoder");
    return -1;
  }

  if (aacEncoder_SetParam(m_aacEncHandle, AACENC_AOT, 2) != AACENC_OK) {  //aac lc
    LOGE("Unable to set the AOT");
    return -1;
  }

  if (aacEncoder_SetParam(m_aacEncHandle, AACENC_SAMPLERATE, sampleRate) != AACENC_OK) {
    LOGE("Unable to set the AOT");
    return -1;
  }

  if (aacEncoder_SetParam(m_aacEncHandle, AACENC_CHANNELMODE, mode) != AACENC_OK) {  //2 channle
    LOGE("Unable to set the channel mode");
    return -1;
  }

  //http://wiki.hydrogenaud.io/index.php?title=Fraunhofer_FDK_AAC#Bitrate_Modes
  if (aacEncoder_SetParam(m_aacEncHandle, AACENC_BITRATE, 40000) != AACENC_OK) {
    LOGE("Unable to set the bitrate\n");
    return -1;
  }

  if (aacEncoder_SetParam(m_aacEncHandle, AACENC_TRANSMUX, 2) != AACENC_OK) { //0-raw 2-adts
    LOGE("Unable to set the ADTS transmux");
    return -1;
  }

  if (aacEncEncode(m_aacEncHandle, NULL, NULL, NULL, NULL) != AACENC_OK) {
    LOGE("Unable to initialize the encoder");
    return -1;
  }

  if (aacEncInfo(m_aacEncHandle, &info) != AACENC_OK) {
    LOGE("Unable to get the encoder info");
    return -1;
  }

  //LOGE("m_aacEncHandle = %p", m_aacEncHandle);
  return reinterpret_cast<jlong>(m_aacEncHandle);
}

uint8_t m_aacOutbuf[20480];

FDKAAC_FUNC(jbyteArray, FdkAacEncode, jlong handle, jbyteArray buffer) {

  assert(handle > 0);

  AACENC_BufDesc in_buf = { 0 }, out_buf = { 0 };
  AACENC_InArgs in_args = { 0 };
  AACENC_OutArgs out_args = { 0 };
  int in_identifier = IN_AUDIO_DATA;
  int in_elem_size = 2;

  int size = env->GetArrayLength(buffer);
  void * pData = env->GetByteArrayElements(buffer, nullptr);

  in_args.numInSamples = size / 2;  //size为pcm字节数
  in_buf.numBufs = 1;
  in_buf.bufs = &pData;  //pData为pcm数据指针
  in_buf.bufferIdentifiers = &in_identifier;
  in_buf.bufSizes = &size;
  in_buf.bufElSizes = &in_elem_size;

  int out_identifier = OUT_BITSTREAM_DATA;
  void *out_ptr = m_aacOutbuf;
  int out_size = sizeof(m_aacOutbuf);
  int out_elem_size = 1;
  out_buf.numBufs = 1;
  out_buf.bufs = &out_ptr;
  out_buf.bufferIdentifiers = &out_identifier;
  out_buf.bufSizes = &out_size;
  out_buf.bufElSizes = &out_elem_size;

  if ((aacEncEncode((HANDLE_AACENCODER)handle, &in_buf, &out_buf, &in_args, &out_args)) != AACENC_OK) {
    LOGE("Encoding aac failed\n");
    return NULL;
  }
  if (out_args.numOutBytes == 0) {
    return NULL;
  }

  jbyteArray arr = env->NewByteArray(out_args.numOutBytes);
  jbyte* jby = env->GetByteArrayElements(arr, nullptr);
  memcpy(jby, m_aacOutbuf, out_args.numOutBytes);

  env->ReleaseByteArrayElements(arr, jby, 0);
  return arr;
}

FDKAAC_FUNC(void, FdkAacDeInit, jlong handle) {
  assert(handle > 0);
  HANDLE_AACENCODER aacHandle = reinterpret_cast<HANDLE_AACENCODER>(handle);
  aacEncClose(&aacHandle);
}
