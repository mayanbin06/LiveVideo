package com.myb.h264;

import com.myb.libyuv.LibYuv;

/**
 * Created by myb on 12/27/17.
 * Use OpenH264Encoder
 */

public class SoftwareH264Encoder extends H264Encoder {

  private long firstEncodeTimeStamp = 0;
  private long nativeEncoder = 0l;
  private byte[] editBuffer;

  public SoftwareH264Encoder(int width, int height, int framerate, int bitrate) {
    super(width, height, framerate, bitrate);
  }

  @Override
  public byte[] encode(byte[] src, int format, int width, int height, int rotation) {
    int requestWidth = width;
    int requestHeight = height;
    int requestFormat = format;
    int requestSize = width * height * 3/2;
    // OpenH264需要I420的标准YUV格式, 计算I420所需空间, 转化为I420.
    if (editBuffer == null || editBuffer.length < requestSize) {
      editBuffer = new byte[width * height * 3/2];
    }
    switch (format) {
      case NV21:
        LibYuv.NV21ToI420(src, editBuffer, width, height, false);
        break;
      case I420:
        break;
      case YV12:
        // @TODO support convert others to I420.
      default:
        throw new RuntimeException("Not support format");
    }
    if (rotation == 90 || rotation == 270) {
      // rotation 会改变视频的长宽, 重新初始化Encoder，如何下次还是同样的角度，则不需要再初始化
      LibYuv.I420Rotate(editBuffer, src, width, height, rotation);
      requestWidth = height;
      requestHeight = width;
    }
    // 参数改变
    if (width != requestWidth || height != requestHeight) {
      this.width = requestWidth;
      this.height = requestHeight;
      releaseEncoder();
    }
    maybeInitEncoder();

    if (firstEncodeTimeStamp == 0l) {
      // 重新初始化解码器，这个时间戳是否要重置？
      firstEncodeTimeStamp = System.currentTimeMillis();
    }
    // before encode, format the params.
    return OpenH264Encoder.EncodeH264frame(nativeEncoder, src, I420, 0,
        System.currentTimeMillis() - firstEncodeTimeStamp);
  }

  private void maybeInitEncoder() {
    // Has no encoder now.
    if (nativeEncoder != 0l) {
      return;
    }
    nativeEncoder = OpenH264Encoder.InitEncode(width, height, bitrate, framerate);
  }

  private void releaseEncoder() {
    if (nativeEncoder == 0l) {
      return;
    }
    OpenH264Encoder.DeInitEncode(nativeEncoder);
    nativeEncoder = 0l;
  }

  @Override
  protected void finalize() throws Throwable {
    releaseEncoder();
    super.finalize();
  }
}
