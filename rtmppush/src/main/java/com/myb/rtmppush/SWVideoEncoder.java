package com.myb.rtmppush;

import java.nio.ByteBuffer;
import com.myb.libyuv.LibYuv;


public class SWVideoEncoder {
	private int _iWidth;
	private int _iHeight;
	private int _iFrameRate;
	private int _iBitRate;
	private long _iHandle = 0;
	private int _iFormatType = android.graphics.ImageFormat.NV21;
	private ByteBuffer _YUV420 = null;
	private ByteBuffer _yuvTmp = null;
	
	private OpenH264Encoder _OpenH264Encoder = null;
	
  public SWVideoEncoder(int width, int height, int framerate, int bitrate){
    _iWidth = width;
    _iHeight = height;
    _iFrameRate = framerate;
    _iBitRate = bitrate;
    	
    _YUV420 = ByteBuffer.allocateDirect(_iWidth * _iHeight * 3/2);
    _yuvTmp = ByteBuffer.allocateDirect(_iWidth * _iHeight * 1/2);
  }
    
  public boolean start(int iFormateType){
    int iType = OpenH264Encoder.YUV420_TYPE;

    if(iFormateType == android.graphics.ImageFormat.YV12){
      iType = OpenH264Encoder.YUV12_TYPE;
    }else{
      iType = OpenH264Encoder.YUV420_TYPE;
    }
    _OpenH264Encoder = new OpenH264Encoder();
    _iHandle = _OpenH264Encoder.InitEncode(_iWidth, _iHeight, _iBitRate, _iFrameRate, iType);
    if(_iHandle == 0){
      return false;
    }

    _iFormatType = iFormateType;
    return true;
  }
    
  public void stop(){
    if(_iHandle != 0){
      _OpenH264Encoder.DeInitEncode(_iHandle);
    }
  }

  public byte[] EncoderH264(ByteBuffer YUVOrigin){
    if(YUVOrigin == null){
      return null;
    }
    if(YUVOrigin.capacity() != _iWidth*_iHeight * 3/2){
      return null;
    }

    byte[] h264data = _OpenH264Encoder.EncodeH264frame(_iHandle, /*_YUV420*/YUVOrigin);
    return h264data;
  }
    
  public void YUV420spRotateNegative90(byte[] dst, byte[] src,
    int srcWidth, int height) {
  int nWidth = 0, nHeight = 0;
  int wh = 0;
  int uvHeight = 0;
  if (srcWidth != nWidth || height != nHeight) {
    nWidth = srcWidth;
    nHeight = height;
    wh = srcWidth * height;
    uvHeight = height >> 1;// uvHeight = height / 2
  }

  // 旋转Y
  int k = 0;
  for (int i = 0; i < srcWidth; i++) {
    int nPos = srcWidth - 1;
    for (int j = 0; j < height; j++) {
      dst[k] = src[nPos - i];
      k++;
      nPos += srcWidth;
    }
  }

  for (int i = 0; i < srcWidth; i += 2) {
    int nPos = wh + srcWidth - 1;
    for (int j = 0; j < uvHeight; j++) {
      dst[k] = src[nPos - i - 1];
      dst[k + 1] = src[nPos - i];
      k += 2;
      nPos += srcWidth;
    }
  }

  return;
}

	public int YUV420pRotate90(ByteBuffer src, ByteBuffer dst, int width, int height) {
    	return LibYuv.I420Rotate(src, dst, width, height, LibYuv.ROTATE_90);
	}

  public int YUV420pRotate270(ByteBuffer src, ByteBuffer dst, int width, int height) {
    return LibYuv.I420Rotate(src, dst, width, height, LibYuv.ROTATE_270);
  }

  public void YUV420spRotate90(byte[] des, final byte[] src, int width,
			int height) {
		int n = 0;
		int hw = width / 2;
		int hh = height / 2;
		// copy y
		for (int j = 0; j < width; j++) {
			for (int i = height - 1; i >= 0; i--) {
				des[n++] = src[width * i + j];
			}
		}

		int pos = width*height;
		for (int j = 0; j < width; j+=2) {
			for (int i = hh -1; i >= 0; i--) {
				des[n++] = src[pos + width*i + j];		// copy v
				des[n++] = src[pos + width*i + j + 1];	// copy u
			}
		}
	}
	
  public void YUV420pRotate180(byte[] des, byte[] src, int width, int height) {
		int n = 0;
		int hw = width / 2;
		int hh = height / 2;
		// copy y
		for (int j = height - 1; j >= 0; j--) {
			for (int i = width; i > 0; i--) {
				des[n++] = src[width * j + i];
			}
		}

		// copy u
		int uPos = width * height;
		for (int j = hh - 1; j >= 0; j--) {
			for (int i = hw; i > 0; i--) {
				des[n++] = src[uPos + hw * i + j];
			}
		}

		// copy v
		int vPos = uPos + width * height / 4;
		for (int j = hh - 1; j >= 0; j--) {
			for (int i = hw; i > 0; i--) {
				des[n++] = src[vPos + hw * i + j];
			}
		}
	}
	
  public void swapYV12toI420_Ex(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
    int iSize = width*height;
    System.arraycopy(yv12bytes, iSize, _yuvTmp, 0, iSize/4);//U-->tmp
    	
    System.arraycopy(yv12bytes, 0, i420bytes, 0, iSize);//Y
    System.arraycopy(yv12bytes, iSize+iSize/4, i420bytes, iSize,iSize/4);//U
    System.arraycopy(_yuvTmp, 0, i420bytes, iSize+iSize/4, iSize/4);//V

  }

  public int swapNV21toI420(ByteBuffer src, ByteBuffer dst, int width, int height)
  {
    return LibYuv.NV21ToI420(src, dst, width, height, true);
  }
    
  public void swapNV21toNV12(byte[] nv21bytes, byte[] nv12bytes, int width, int height) {
    byte bTmp = 0;
    final int iSize = width*height;
    for (int i = iSize; i < iSize+iSize/2; i += 2) {
      bTmp = nv21bytes[i+1];
      nv21bytes[i+1] = nv21bytes[i];
      nv21bytes[i] = bTmp;
    }
    System.arraycopy(nv21bytes, 0, nv12bytes, 0, nv21bytes.length);
  }
 }
