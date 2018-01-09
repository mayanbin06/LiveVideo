package com.myb.fdkaac;

/**
 * Created by myb on 1/10/18.
 */

public abstract class AacEncoder {
  public static class AacFrame {
    public byte[] data;
    public long presentationTimeMs;
    public long encodedTimeMs;
    public AacFrame(byte[] data, long pTimeMs, long eTimeMs) {
      this.data = data;
      this.presentationTimeMs = pTimeMs;
      this.encodedTimeMs = eTimeMs;
    }
  }

  protected int simpleRate;
  protected int channels;

  public AacEncoder(int simpleRate, int channels) {
    this.simpleRate = simpleRate;
    this.channels = channels;
  }

  public AacFrame encode(byte[] src) {
    return null;
  }

  public void release() {
    // release
  }
}
