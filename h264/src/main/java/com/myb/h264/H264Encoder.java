package com.myb.h264;

import android.graphics.ImageFormat;

/**
 * Created by myb on 12/27/17.
 */

abstract public class H264Encoder {
  public static class H264Frame {
    public byte[] data;
    public long presentationTimeMs;
    public long encodedTimeMs;
    public H264Frame(byte[] data, long pTimeMs, long eTimeMs) {
      this.data = data;
      this.presentationTimeMs = pTimeMs;
      this.encodedTimeMs = eTimeMs;
    }
  }
  protected int width;
  protected int height;
  protected int framerate;
  protected int bitrate;

  public static final int NV21 = ImageFormat.NV21;
  public static final int YV12 = ImageFormat.YV12;
  public static final int I420 = ImageFormat.YUV_420_888;

  public H264Encoder(int width, int height, int framerate, int bitrate) {
    this.width = width;
    this.height = height;
    this.framerate = framerate;
    this.bitrate = bitrate;
  }

  // thus width or height changed, we may re-init encoder.
  public H264Frame encode(byte[] src, int format, int width, int height, int rotation) {
    return null;
  }
}
