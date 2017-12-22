package com.myb.rtmppush;

public class RtmpSession {
    static {
        System.loadLibrary("RtmpSession-jni");
    }
    public native long RtmpConnect(String rtmpUrl);
    public native boolean RtmpIsConnect(long handle);
    public native int RtmpSendVideoData(long handle, byte[] videoData, long lLen);
    public native int RtmpSendAudioData(long handle, byte[] audioData, long lLen);
    public native void RtmpDisconnect(long handle);
}
