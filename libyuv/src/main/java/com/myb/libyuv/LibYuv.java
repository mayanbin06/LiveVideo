package com.myb.libyuv;

import java.nio.ByteBuffer;
/**
 * Created by myb on 12/12/17.
 * define the java interface of libyuv.
 */

public class LibYuv {
  static {
    System.loadLibrary("yuv-jni");
  }
  // RGB FORMAT
  public static final int ABGR = 0;
  public static final int RGBA = 1;
  public static final int ARGB = 2;
  public static final int BGRA = 3;
  public static final int RGB24 = 4;
  public static final int RGB565 = 5;

  // scale mode.
  public static final int SCALE_MODE_NONE = 0;
  public static final int SCALE_MODE_LINEAR = 1;
  public static final int SCALE_MODE_BI_LINEAR = 2;
  public static final int SCALE_MODE_BOX = 3;

  // rotation mode.
  public static final int ROTATE_0 = 0;
  public static final int ROTATE_90 = 1;
  public static final int ROTATE_180 = 2;
  public static final int ROTATE_270 = 3;

  // use ByteBuffer a shared memory with native.
  // convert with stride(which means stride not same with width)
  public static native int RgbToI420(int format,
      ByteBuffer rgb, int stride, ByteBuffer yuv,
      int y_stride, int u_stride, int v_stride,
      int width, int height);

  // stride same with width.
  public static int RgbaToI420(ByteBuffer rgb, ByteBuffer yuv, int width, int height) {
    // rgba - 4bytes per piexl, so stride = 4 * width;
    int rgb_stride = 4 * width;
    //  y y y y
    //  y y y y
    //  u u
    //  v v
    int y_stride = width;
    int u_stride = width >> 1;
    int v_stride = width >> 1;
    return RgbToI420(RGBA, rgb, rgb_stride, yuv, y_stride, u_stride, v_stride, width, height);
  }

  public static int Rgb565ToI420(ByteBuffer rgb, ByteBuffer yuv, int width, int height) {
    // rgb565 - 2bytes per piexl, so stride = 2 * width;
    int rgb_stride = 2 * width;
    int y_stride = width;
    int u_stride = width >> 1;
    int v_stride = width >> 1;
    return RgbToI420(RGB565, rgb, rgb_stride, yuv, y_stride, u_stride, v_stride, width, height);
  }

  public static native int I420ToRgb(int format,
      ByteBuffer yuv, int y_stride, int u_stride,
      int v_stride, ByteBuffer rgb, int stride,
      int width, int height);

  public static int I420ToRgba(ByteBuffer yuv, ByteBuffer rgb, int width, int height) {
    // rgba - 4bytes per piexl, so stride = 4 * width;
    int rgb_stride = 4 * width;
    //  y y y y
    //  y y y y
    //  u u
    //  v v
    int y_stride = width;
    int u_stride = width >> 1;
    int v_stride = width >> 1;
    return I420ToRgb(RGBA, yuv, y_stride, u_stride, v_stride, rgb, rgb_stride, width, height);
  }

  public static int I420ToRgb565(ByteBuffer yuv, ByteBuffer rgb, int width, int height) {
    // rgba - 4bytes per piexl, so stride = 4 * width;
    int rgb_stride = 2 * width;
    //  y y y y
    //  y y y y
    //  u u
    //  v v
    int y_stride = width;
    int u_stride = width >> 1;
    int v_stride = width >> 1;
    return I420ToRgb(RGB565, yuv, y_stride, u_stride, v_stride, rgb, rgb_stride, width, height);
  }

  public static native int I420ToNV21(ByteBuffer yuv420p, ByteBuffer yuv420sp,
                                       int width, int height, boolean swapUV);

  public static native int NV21ToI420(ByteBuffer yuv420sp, ByteBuffer yuv420p,
                                       int width, int height, boolean swapUV);

  public static native int I420Scale(ByteBuffer src_data, int width, int height,
                                      ByteBuffer dst_data, int dst_width, int dst_height,
                                      int mode, boolean swapUV);

  public static int I420Rotate(ByteBuffer src, ByteBuffer dst, int width, int height, int mode) {
    int y_stride = width;
    int u_stride = width >> 1;
    int v_stride = width >> 1;
    return I420Rotate(src, y_stride, u_stride, v_stride,
        dst, y_stride, u_stride, v_stride, width, height, mode);
  }

  public static native int I420Rotate(ByteBuffer src, int src_y_stride, int src_u_stride,
                                       int src_v_stride, ByteBuffer dst, int dst_y_stride,
                                       int dst_u_stride, int dst_v_stride,
                                       int width, int height, int mode);
}
