package com.myb.fdkaac;

public class FdkAacEncoder {
	static {
		System.loadLibrary("FdkAac-jni");
	}
	public static native long FdkAacInit(int iSampleRate, int iChannel);
	public static native byte[] FdkAacEncode(long handle, byte[] buffer);
	public static native void FdkAacDeInit(long handle);
}
