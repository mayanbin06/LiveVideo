package com.myb.librtmp;

public class RtmpSessionImpl implements RtmpSession {
    static {
        System.loadLibrary("RtmpSession-jni");
    }
    public native long RtmpConnect(String rtmpUrl);
    public native boolean RtmpIsConnect(long handle);
    public native int RtmpSendVideoData(long handle, byte[] videoData, long lLen, long timeStamp);
    public native int RtmpSendAudioData(long handle, byte[] audioData, long lLen, long timeStamp);
    public native void RtmpDisconnect(long handle);
}
