package com.myb.rtmp;

import java.lang.String;

/**
 * Created by myb on 1/2/18.
 */

public class RtmpSessionImpl implements RtmpSession {

  private long handle = 0l;
  public RtmpSessionImpl() {}

  public int RtmpConnect(String rtmpUrl) {
    if (handle > 0) {
      // already connect.
      return -1;
    }
    handle = SrsRtmp.Connect(rtmpUrl);
    return handle > 0 ? 0 : -1;
  }

  public boolean RtmpIsConnect() {
    return SrsRtmp.IsConnect(handle);
  }

  public int RtmpSendVideoData(byte[] videoData, long lLen) {
    return SrsRtmp.SendH264Data(handle, videoData, lLen, 0);
  }

  public int RtmpSendAudioData(byte[] audioData, long lLen) {
    return SrsRtmp.SendAacData(handle, audioData, lLen, 0);
  }

  public void RtmpDisconnect(){
    SrsRtmp.Disconnect(handle);
    handle = 0l;
  }

  public void RtmpSetAudioParams(int sampleRate, int channelCount, int sampleSize) {
    SrsRtmp.SetAudioParams(handle, sampleRate, channelCount, sampleSize);
  }

  public void RtmpSetVideoParams(int frameRate) {
    SrsRtmp.SetVideoParams(handle, frameRate);
  }
}
