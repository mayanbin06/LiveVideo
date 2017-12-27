package com.myb.fdkaac;

public class FdkAacEncode {
	static {
		System.loadLibrary("FdkAac-jni");
	}
	public native long FdkAacInit(int iSampleRate, int iChannel);
	public native byte[] FdkAacEncode(long handle, byte[] buffer);
	public native void FdkAacDeInit(long handle);
}
