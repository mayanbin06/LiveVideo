package com.myb.h264;

import android.graphics.ImageFormat;

public class OpenH264Encoder {

  public static final int NV21_TYPE = 17;
	public static final int YUV12_TYPE   = 24;
	public static final int YUV420_TYPE  = 23;
  public static native long InitEncode(int width, int hight, int bitrate, int framerate);
  public static native byte[] EncodeH264frame(long nativePtr, byte[] src, int format, int rotation, long timeStamp);
  public static native void DeInitEncode(long handle);

  static {
      System.loadLibrary("OpenH264-jni");
  }
}
