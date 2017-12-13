package com.myb.rtmppush;

import java.nio.ByteBuffer;

public class OpenH264Encoder {
	public static final int YUV12_TYPE   = 24;
	public static final int YUV420_TYPE  = 23;
  public native long InitEncode(int nImgWidth, int nImgHight, int nBitrate, int Frmrate, int iYUVType);
  public native byte[] EncodeH264frame(long iHandle, ByteBuffer inbuf_ptr);
  public native void DeInitEncode(long iHandle);
  static {
      System.loadLibrary("OpenH264_jni");
  }
}
