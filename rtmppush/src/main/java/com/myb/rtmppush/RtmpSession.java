package com.myb.rtmppush;

public interface RtmpSession {
    public long RtmpConnect(String rtmpUrl);
    public boolean RtmpIsConnect(long handle);
    public int RtmpSendVideoData(long handle, byte[] videoData, long lLen);
    public int RtmpSendAudioData(long handle, byte[] audioData, long lLen);
    public void RtmpDisconnect(long handle);
}
