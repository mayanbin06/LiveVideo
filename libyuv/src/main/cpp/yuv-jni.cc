//
// Created by myb on 12/12/17.
//

#include <jni.h>
#include <stdlib.h>
#include <array>
#include <android/log.h>
#include "libyuv.h"

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

#define LOG_TAG "libyuv_jni"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

#define LIBYUV_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_libyuv_LibYuv_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_com_myb_libyuv_LibYuv_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

using RgbToI420Function = int (*)(const uint8*, int, uint8*,
                                          int, uint8*, int, uint8*,
                                          int, int, int);

using I420ToRgbFunction = int (*)(const uint8*, int,
                                         const uint8*, int,
                                         const uint8*, int,
                                         uint8*, int, int, int);

std::array<RgbToI420Function, 6> RgbToI420FunctionArray = {
    libyuv::ABGRToI420,
    libyuv::RGBAToI420,
    libyuv::ARGBToI420,
    libyuv::BGRAToI420,
    libyuv::RGB24ToI420,
    libyuv::RGB565ToI420
};

std::array<I420ToRgbFunction, 6> I420ToRgbFunctionArray = {
    libyuv::I420ToABGR,
    libyuv::I420ToRGBA,
    libyuv::I420ToARGB,
    libyuv::I420ToBGRA,
    libyuv::I420ToRGB24,
    libyuv::I420ToRGB565
};

LIBYUV_FUNC(jint, RgbToI420, jint format,
            jobject rgba, jint rgba_stride,
            jobject yuv, jint y_stride, jint u_stride,
            jint v_stride, jint width, jint height) {
  if (!rgba || !yuv) {
    LOGE("RgbToI420, input or output buffer must be non-null");
    return -1;
  }
  if (format >= RgbToI420FunctionArray.size() || format < 0) {
    LOGE("RgbToI420, unknown format of rgb.");
    return -1;
  }
  // ByteBuffer backing array.
  uint8_t* rgbData = (uint8_t*)env->GetDirectBufferAddress(rgba);
  uint8_t* yuvData = (uint8_t*)env->GetDirectBufferAddress(yuv);
  size_t ySize = y_stride * height;
  size_t uSize = u_stride * height >> 1;

  // define the func same with java side.
  RgbToI420Function func = RgbToI420FunctionArray[format];
  int ret = func(rgbData, rgba_stride, yuvData, y_stride,
                 yuvData + ySize, u_stride, yuvData + ySize + uSize,
                 v_stride, width, height);
  return ret;
}

LIBYUV_FUNC(jint, I420ToRgb, jint format, jobject yuv,
            jint y_stride, jint u_stride, jint v_stride,
            jobject rgba, jint rgb_stride, jint width, jint height) {
  if (!rgba || !yuv) {
    LOGE("I420ToRgb, input or output buffer must be non-null");
    return -1;
  }
  if (format >= I420ToRgbFunctionArray.size() || format < 0) {
    LOGE("I420ToRgb, unknown rgb format.");
    return -1;
  }
  // ByteBuffer backing array.
  uint8_t* rgbData = (uint8_t*)env->GetDirectBufferAddress(rgba);
  uint8_t* yuvData = (uint8_t*)env->GetDirectBufferAddress(yuv);
  size_t ySize = y_stride * height;
  size_t uSize = u_stride * height >> 1;

  // define the func same with java side.
  I420ToRgbFunction func = I420ToRgbFunctionArray[format];
  int ret = func(yuvData, y_stride, yuvData + ySize, u_stride,
                 yuvData + ySize + uSize, v_stride, rgbData,
                 rgb_stride, width, height);
  return ret;
}

LIBYUV_FUNC(jint, I420ToNV21, jobject yuv420p, jobject yuv420sp,
            jint width, jint height, jboolean swapUV) {
  if (!yuv420p || !yuv420sp) {
    LOGE("I420ToNV21, input or output buffer must be non-null!");
    return -1;
  }

  // ByteBuffer backing array.
  uint8_t* yuv420pData = (uint8_t*)env->GetDirectBufferAddress(yuv420p);
  uint8_t* yuv420spData = (uint8_t*)env->GetDirectBufferAddress(yuv420sp);

  size_t ySize = (size_t) (width * height);
  size_t uSize = (size_t) (width * height >> 2);
  size_t stride[] = {0, uSize};

  int ret = libyuv::I420ToNV21(
      yuv420pData, width,
      yuv420pData + ySize + stride[swapUV ? 1 : 0], width >> 1,
      yuv420pData + ySize + stride[swapUV ? 0 : 1], width >> 1,
      yuv420spData, width, yuv420spData + ySize, width, width, height);
  return ret;
}

LIBYUV_FUNC(jint, NV21ToI420, jobject yuv420sp, jobject yuv420p,
            jint width, jint height, jboolean swapUV) {
  if (!yuv420p || !yuv420sp) {
    LOGE("I420ToNV21, input or output buffer must be non-null!");
    return -1;
  }

  // ByteBuffer backing array.
  uint8_t* yuv420pData = (uint8_t*)env->GetDirectBufferAddress(yuv420p);
  uint8_t* yuv420spData = (uint8_t*)env->GetDirectBufferAddress(yuv420sp);

  size_t ySize = (size_t) (width * height);
  size_t uSize = (size_t) (width * height >> 2);
  size_t stride[] = {0, uSize};

  int ret = libyuv::NV21ToI420(
      yuv420spData, width, yuv420pData + ySize, width,
      yuv420pData, width,
      yuv420pData + ySize + stride[swapUV ? 1 : 0], width >> 1,
      yuv420pData + ySize + stride[swapUV ? 0 : 1], width >> 1,
      width, height);
  return ret;
}

LIBYUV_FUNC(jint, I420Scale, jobject src, jint width, jint height,
            jobject dst, jint dst_width, jint dst_height, jint mode,
            jboolean swapUV) {
  int ySize = width * height;
  int swap[] = {0, ySize >> 2};
  size_t dstYSize = (size_t) (dst_width * dst_height);

  uint8_t* srcData = (uint8_t*)env->GetDirectBufferAddress(src);
  uint8_t * dstData = (uint8_t*)env->GetDirectBufferAddress(dst);

  int ret = libyuv::I420Scale(srcData, width, srcData + ySize, width >> 1,
                    srcData + ySize + (ySize >> 2), width >> 1, width, height,
                    dstData, dst_width, dstData + dstYSize+swap[swapUV], dst_width >> 1,
                    dstData + dstYSize + swap[1-swapUV], dst_width >> 1,
                    dst_width, dst_height, (libyuv::FilterMode) mode);
  return ret;
}

LIBYUV_FUNC(jint, I420Rotate,
            jobject src, jint src_y_stride, jint src_u_stride, jint src_v_stride,
            jobject dst, jint dst_y_stride, jint dst_u_stride, jint dst_v_stride,
            jint width, jint height, jint mode) {
  int ySize = width * height;
  int uSize = width * height >> 2;

  uint8_t* srcData = (uint8_t*)env->GetDirectBufferAddress(src);
  uint8_t * dstData = (uint8_t*)env->GetDirectBufferAddress(dst);

  int ret = libyuv::I420Rotate(srcData, src_y_stride,
                               srcData + ySize, src_u_stride,
                               srcData + ySize + uSize, src_v_stride,
                               dstData, dst_y_stride,
                               dstData + ySize, dst_u_stride,
                               dstData + ySize + uSize, dst_v_stride,
                               width, height, (libyuv::RotationMode)mode);
  return ret;
}