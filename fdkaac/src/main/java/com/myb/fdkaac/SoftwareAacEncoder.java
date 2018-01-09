package com.myb.fdkaac;

/**
 * Created by myb on 1/10/18.
 */

public class SoftwareAacEncoder extends AacEncoder {
  private long nativeEncoder = 0l;
  private long firstEncodeTimeStamp = 0;

  public SoftwareAacEncoder(int sampleRate, int channels) {
    super(sampleRate, channels);
  }

  @Override
  public AacFrame encode(byte[] src) {
    maybeInitEncoder();
    long pTimeMs;
    if (firstEncodeTimeStamp == 0l) {
      pTimeMs = firstEncodeTimeStamp = System.currentTimeMillis();
    } else {
      pTimeMs = System.currentTimeMillis() - firstEncodeTimeStamp;
    }
    // before encode, format the params.
    byte[] data =  FdkAacEncoder.FdkAacEncode(nativeEncoder, src);
    if (data == null) {
      return null;
    }
    return new AacFrame(data, pTimeMs, pTimeMs);
  }

  @Override
  public void release() {
    super.release();
    releaseEncoder();
  }

  private void maybeInitEncoder() {
    // Has no encoder now.
    if (nativeEncoder != 0l) {
      return;
    }
    nativeEncoder = FdkAacEncoder.FdkAacInit(this.simpleRate, this.channels);
  }

  private void releaseEncoder() {
    if (nativeEncoder == 0l) {
      return;
    }
    FdkAacEncoder.FdkAacDeInit(nativeEncoder);
    nativeEncoder = 0l;
  }

  @Override
  protected void finalize() throws Throwable {
    releaseEncoder();
    super.finalize();
  }
}
