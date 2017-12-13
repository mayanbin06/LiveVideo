//
// Created by myb on 12/5/17.
// 参考 Chromium WebRTC 中 H264EncoderImpl的实现。
//

#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include <wels/codec_app_def.h>

extern "C" {
#ifdef __cplusplus
#define __STDC_CONSTANT_MACROS
#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#include <string.h>
#include "wels/codec_api.h"
#endif
}

#define LOG_TAG "OpenH264_jni"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

#define OPENH264_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_rtmppush_OpenH264Encoder_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_rtmppush_OpenH264Encoder_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

struct EncoderContext {
    int width;
    int height;
    ISVCEncoder* encoder;
    uint8_t * data;
    size_t data_length;
    size_t data_size;
};
// init and return the handle.
OPENH264_FUNC(jlong, InitEncode, jint nImgWidth,
            jint nImgHight, jint nBitrate,
            jint frameRate, jint iYUVType) {

    ISVCEncoder *encoder = NULL;

    int rv = WelsCreateSVCEncoder(&encoder);
    if (encoder == NULL || rv != 0)
    {
        LOGE("Failed to create SVCEncoder!!");
        return 0;
    }

    SEncParamBase param;
    memset (&param, 0, sizeof (SEncParamBase));
    param.iUsageType = CAMERA_VIDEO_REAL_TIME;
    param.fMaxFrameRate = frameRate;
    param.iPicWidth = nImgWidth;
    param.iPicHeight = nImgHight;
    param.iTargetBitrate = nBitrate;
    encoder->Initialize (&param);

    int videoFormat = videoFormatI420; // just support this?
    encoder->SetOption(ENCODER_OPTION_DATAFORMAT, &iYUVType);

    EncoderContext* c = new EncoderContext();
    // while processing, image size should not change.
    // whatever, if changed, malloc the data large enough with new image size.
    c->width = nImgWidth;
    c->height = nImgHight;
    c->encoder = encoder;
    c->data = (uint8_t*) new uint8_t[c->width * c->height * 3 / 2];
    c->data_length = 0;
    c->data_size = c->width * c->height * 3 / 2;
    return (jlong)c;
}

OPENH264_FUNC(jbyteArray, EncodeH264frame, jlong handle, jobject buffer) {

    EncoderContext* c = (EncoderContext*)handle;
    if (c == NULL) {
        return NULL;
    }

    int frameSize = c->width * c->height * 3 / 2;
    jbyte* data = (jbyte*)env->GetDirectBufferAddress(buffer);

    SFrameBSInfo info;
    memset (&info, 0, sizeof (SFrameBSInfo));
    SSourcePicture pic;
    memset (&pic, 0, sizeof (SSourcePicture));
    pic.iPicWidth = c->width;
    pic.iPicHeight = c->height;
    pic.iColorFormat = videoFormatI420;
    pic.iStride[0] = pic.iPicWidth;
    pic.iStride[1] = pic.iStride[2] = pic.iPicWidth >> 1;
    pic.pData[0] = (uint8_t*)data;
    pic.pData[1] = pic.pData[0] + c->width * c->height;
    pic.pData[2] = pic.pData[1] + (c->width * c->height >> 2);

    int rv = c->encoder->EncodeFrame (&pic, &info);
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
        const SLayerBSInfo& layerInfo = info.sLayerInfo[layer];
        for (int nal = 0; nal < layerInfo.iNalCount; ++nal, ++fragments_count) {
            required_size += layerInfo.pNalLengthInByte[nal];
        }
    }

    // increase data size.
    if (required_size > c->data_size) {
        delete c->data;
        c->data = new uint8_t[required_size];
        c->data_size = required_size;
        c->data_length = 0;
    }

    jbyteArray arr = env->NewByteArray(required_size);
    env->SetByteArrayRegion(arr, 0, required_size, (jbyte*)c->data);

    c->data_length = 0;
    for (int layer = 0; layer < info.iLayerNum; ++layer) {
        const SLayerBSInfo& layerInfo = info.sLayerInfo[layer];
        size_t layer_len = 0;
        for (int nal = 0; nal < layerInfo.iNalCount; ++nal) {
            layer_len += layerInfo.pNalLengthInByte[nal];
        }
        // Copy the entire layer's data (including start codes).
        memcpy(c->data + c->data_length, layerInfo.pBsBuf, layer_len);
        c->data_length += layer_len;
    }

    return arr;
}

OPENH264_FUNC(void, DeInitEncode, jlong handle) {
    EncoderContext *c = (EncoderContext*)handle;
    c->encoder->Uninitialize();
    WelsDestroySVCEncoder(c->encoder);
    delete c->data;
    delete c;
}
