package com.myb.rtmp;

public interface RtmpSession {
    public int RtmpConnect(String rtmpUrl);
    public boolean RtmpIsConnect();
    public void RtmpSetAudioParams(int sampleRate, int channelCount, int sampleSize);
    public int RtmpSendVideoData(byte[] videoData, long lLen);
    public int RtmpSendAudioData(byte[] audioData, long lLen);
    public void RtmpDisconnect();
}
