package com.myb.rtmp;

public class SrsRtmp {
    static {
        System.loadLibrary("SrsRtmp-jni");
    }
    public native static long Connect(String url);
    public native static boolean IsConnect(long nativePtr);
    public native static boolean Disconnect(long nativePtr);
    public native static int SendH264Data(long nativePtr, byte[] data, long len, long timeStamp);
    public native static int SendAacData(long nativePtr, byte[] data, long len, long timeStamp);
    public native static void SetAudioParams(long nativePtr, int sampleRate, int channelCount, int sampleSize);
    public native static void SetVideoParams(long nativePtr, int frameRate);
}
