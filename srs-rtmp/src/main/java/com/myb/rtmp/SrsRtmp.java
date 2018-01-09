package com.myb.rtmp;

// SrsRtmp 可以直接发送H264流和AAC，但是好像有问题，生成的m3u8文件每段都是#EXT-X-DISCONTINUITY， 是否是时间戳有问题？

//这个因为IP摄像头在每个I帧前都插入sps和pps，这些重复的sps和pps会导致hls频繁的插入discontinue信息，
//所以srs-librtmp只有在sps和pps都变化时才发送新的sequence header包，而不是每次都发送。所以sps重复时会返回一个错误码，用户忽略这个错误即可。
// 先用pc 直接发送 .h264文件测试一下。
public class SrsRtmp {
    static {
        System.loadLibrary("SrsRtmp-jni");
    }
    public native static long Connect(String url);
    public native static boolean IsConnect(long nativePtr);
    public native static boolean Disconnect(long nativePtr);
    public native static int SendH264Data(long nativePtr, byte[] data, long len, long timeStamp);
    public native static int SendAacData(long nativePtr, byte[] data, long len, long timeStamp);
    public native static void SetAudioParams(long nativePtr, int sampleRate, int channelCount, int sampleSize, int soundType);
    public native static void SetVideoParams(long nativePtr, int frameRate);
}
