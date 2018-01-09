package com.myb.librtmp;

public interface RtmpSession {
    public long RtmpConnect(String rtmpUrl);
    public boolean RtmpIsConnect(long handle);
    public int RtmpSendVideoData(long handle, byte[] videoData, long lLen, long timeStamp);
    public int RtmpSendAudioData(long handle, byte[] audioData, long lLen, long timeStamp);
    public void RtmpDisconnect(long handle);
}
