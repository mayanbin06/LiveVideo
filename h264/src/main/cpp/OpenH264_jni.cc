//
// Created by myb on 12/5/17.
// 参考 Chromium WebRTC 中 H264EncoderImpl的实现。
//

#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include <codec_app_def.h>
#include <sstream>

extern "C" {
#ifdef __cplusplus
#define __STDC_CONSTANT_MACROS
#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#include <string.h>
#include "codec_api.h"
#endif
}

//#define DEBUG

#define LOG_TAG "OpenH264_jni"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

#define OPENH264_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_h264_OpenH264Encoder_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_h264_OpenH264Encoder_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

#define H264_FRAME_CLASS "com/myb/h264/H264Encoder$H264Frame"
struct EncoderContext {
    int width;
    int height;
    ISVCEncoder* encoder;
    uint8_t * data;
    size_t data_length;
    size_t data_size;
    int frame_encoded;
};

using CALLBACK_FUCTION = void (*)(void* context, int level, const char* message);
void TraceCallback(void* context, int level, const char* message) {
  LOGE("%s", message);
}

// init and return the handle.
OPENH264_FUNC(jlong, InitEncode, jint nImgWidth,
            jint nImgHight, jint nBitrate,
            jint frameRate) {

  ISVCEncoder *encoder = NULL;

  if (WelsCreateSVCEncoder(&encoder) != cmResultSuccess)
  {
      LOGE("Failed to create SVCEncoder!!");
      return 0;
  }
  SEncParamBase param = {};

  param.iUsageType = CAMERA_VIDEO_REAL_TIME;
  param.fMaxFrameRate = frameRate > 30 ? 30: frameRate;
  param.iPicWidth = nImgWidth;
  param.iPicHeight = nImgHight;
  param.iRCMode = RC_BITRATE_MODE;
  param.iTargetBitrate = nBitrate;


  int pixel_format = videoFormatI420;
  int idr_interval = 16; // IDR interval seconds. 好像没有起作用
  int log_level = WELS_LOG_ERROR;
  CALLBACK_FUCTION callback = &TraceCallback;
  encoder->SetOption(ENCODER_OPTION_DATAFORMAT, &pixel_format);
  encoder->SetOption(ENCODER_OPTION_IDR_INTERVAL, &idr_interval);
  encoder->SetOption(ENCODER_OPTION_TRACE_LEVEL, &log_level);
  encoder->SetOption(ENCODER_OPTION_TRACE_CALLBACK, &callback);

  if (encoder->Initialize (&param) != cmResultSuccess) {
    LOGE("Failed to init encoder params!!");
    return 0;
  }

  EncoderContext* c = new EncoderContext();
  // while processing, image size should not change.
  // whatever, if changed, malloc the data large enough with new image size.
  c->width = nImgWidth;
  c->height = nImgHight;
  c->encoder = encoder;

  LOGE("Success to create SVCEncoder");
  return (jlong)c;
}

jobject CreateH264Frame(JNIEnv* env, jbyteArray data, jlong pTimeMs, long eTimeMs) {
  jclass h264FrameClass = env->FindClass(H264_FRAME_CLASS);
  //javap -s class_name
  jmethodID constructor = env->GetMethodID(h264FrameClass, "<init>", "([BJJ)V");
  return env->NewObject(h264FrameClass, constructor, data, pTimeMs, eTimeMs);
}

OPENH264_FUNC(jobject, EncodeH264frame, jlong handle,
              jbyteArray buffer, jint format, jint rotation, jlong timeStamp) {

  EncoderContext *c = (EncoderContext *) handle;
  if (c == NULL) {
    return NULL;
  }

  int frameSize = c->width * c->height * 3 / 2;
  jbyte *data = (jbyte *) env->GetByteArrayElements(buffer, 0);

  SSourcePicture pic = {};
  pic.iPicWidth = c->width;
  pic.iPicHeight = c->height;
  pic.iColorFormat = videoFormatI420;
  pic.iStride[0] = pic.iPicWidth;
  pic.iStride[1] = pic.iStride[2] = pic.iPicWidth >> 1;
  pic.pData[0] = (uint8_t *) data;
  pic.pData[1] = pic.pData[0] + c->width * c->height;
  pic.pData[2] = pic.pData[1] + (c->width * c->height >> 2);
  pic.iColorFormat = videoFormatI420;
  pic.uiTimeStamp = timeStamp;

  SFrameBSInfo info = {};

  // 每50帧插入一个I-frame，
  // 若一直不发关键帧，生成的HLS文件中ts文件很大，且每个ts之间都有discontinue tag，
  // RTMP服务器那边以I-Frame来分割TS?
  if (c->frame_encoded % 20 == 0) {
    c->encoder->ForceIntraFrame(true);
  }
  c->frame_encoded ++;
  int rv = c->encoder->EncodeFrame(&pic, &info);

  if (rv != cmResultSuccess) {
    LOGE("Encoder Frame Failed!!");
    return NULL;
  }
  if (info.eFrameType == videoFrameTypeSkip) {
    return NULL;
  }

  size_t required_size = 0;
  size_t fragments_count = 0;
  for (int layer = 0; layer < info.iLayerNum; ++layer) {
    const SLayerBSInfo &layerInfo = info.sLayerInfo[layer];
    for (int nal = 0; nal < layerInfo.iNalCount; ++nal, ++fragments_count) {
      required_size += layerInfo.pNalLengthInByte[nal];
    }
  }

  //env->NewDirectByteBuffer(); for few bytes, use copy is faster than DirectBuffer.
  // 这样创建的内存不在虚拟机的管理范围，而且需要同步Native和Java Object，保证数据可用。
  // see https://stackoverflow.com/questions/28791827/does-newdirectbytebuffer-create-a-copy-in-native-code
  //jobject directBuffer = env->NewDirectByteBuffer(c->data, required_size);

  jbyteArray arr = env->NewByteArray(required_size);
  jbyte *arrPtr = env->GetByteArrayElements(arr, NULL);

#if defined(DEBUG)
  LOGE("%p, type = %d, layer nums = %d, encoder time = %lld", c, info.eFrameType, info.iLayerNum,
       info.uiTimeStamp);
#endif

  // the first IDR contains the sps and pps.
  int length = 0;
  for (int layer = 0; layer < info.iLayerNum; ++layer) {
    const SLayerBSInfo& layerInfo = info.sLayerInfo[layer];
    size_t layer_len = 0;
    for (int nal = 0; nal < layerInfo.iNalCount; ++nal) {
        layer_len += layerInfo.pNalLengthInByte[nal];
    }
    // Copy the entire layer's data (including start codes).
    memcpy(arrPtr + length, layerInfo.pBsBuf, layer_len);
    //memcpy(c->data + length, layerInfo.pBsBuf, layer_len);
    length += layer_len;
  }

  jobject frame = CreateH264Frame(env, arr, pic.uiTimeStamp, info.uiTimeStamp);
  env->ReleaseByteArrayElements(buffer, data, 0);
  env->ReleaseByteArrayElements(arr, arrPtr, 0);

  return frame;
}

OPENH264_FUNC(void, DeInitEncode, jlong handle) {
    EncoderContext *c = (EncoderContext*)handle;
    c->encoder->Uninitialize();
    WelsDestroySVCEncoder(c->encoder);
    delete c->data;
    delete c;
}
