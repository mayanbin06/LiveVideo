package com.myb.rtmppush;

import com.myb.fdkaac.AacEncoder;
import com.myb.h264.H264Encoder;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RtmpSessionManager {
  private final String TAG = "RtmpSessionManager";
  private Queue<H264Encoder.H264Frame> videoDataQueue = new LinkedList<H264Encoder.H264Frame>();
  private Lock videoDataQueueLock = new ReentrantLock();

  private Queue<AacEncoder.AacFrame> audioDataQueue = new LinkedList<AacEncoder.AacFrame>();
  private Lock audioDataQueueLock = new ReentrantLock();

  private com.myb.rtmp.SrsRtmp rtmpSession = null;
  private long rtmpHandle = 0;
  private String rtmpUrl = null;

  private Boolean bStartFlag = false;

  // audio params.
  private int sampleRate;
  private int channelCount;
  private int sampleSize;
  private int soundType;
  // video params.
  private int frameRate;

  private long audioTimeStamp = 0;
  private long videoTimeStamp = 0;

  private Thread h264EncoderThread = new Thread(new Runnable() {

    private Boolean WaitforReConnect() {
      for (int i = 0; i < 500; i++) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (h264EncoderThread.interrupted() || (!bStartFlag)) {
          return false;
        }
      }
      return true;
    }

    private boolean guarenteeConnected() {
      // need connect
      while (rtmpHandle == 0 || !rtmpSession.IsConnect(rtmpHandle)) {
        rtmpHandle = rtmpSession.Connect(rtmpUrl);
        if (rtmpHandle != 0) {
          // connect success.
          rtmpSession.SetVideoParams(rtmpHandle, frameRate);
          rtmpSession.SetAudioParams(rtmpHandle, sampleRate, channelCount, sampleSize, soundType);
          return true;
        } else {
          // connect failed, do sleep and check start flag.
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          if (h264EncoderThread.interrupted() || (!bStartFlag)) {
            return false;
          }
        }
      }
      return true;
    }

    @Override
    public void run() {
      while (!h264EncoderThread.interrupted() && (bStartFlag)) {

        if (!guarenteeConnected()) {
          break;
        }
//        if (rtmpHandle == 0) {
//          rtmpHandle = rtmpSession.Connect(rtmpUrl);
//          if (rtmpHandle == 0) {
//            if (!WaitforReConnect()) {
//              break;
//            }
//            continue;
//          }
//        } else {
//          if (rtmpSession.IsConnect(rtmpHandle) == false) {
//            rtmpHandle = rtmpSession.Connect(rtmpUrl);
//            if (rtmpHandle == 0) {
//              if (!WaitforReConnect()) {
//                break;
//              }
//              continue;
//            }
//          }
//        }

        if ((videoDataQueue.size() == 0) && (audioDataQueue.size() == 0)) {
          try {
            Thread.sleep(30);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          continue;
        }
        //Log.i(TAG, "VideoQueue length="+_videoDataQueue.size()+", AudioQueue length="+_audioDataQueue.size());
        for (int i = 0; i < 100; i++) {
          AacEncoder.AacFrame aacFrame = GetAndReleaseAudioQueue();
          if (aacFrame == null) {
            break;
          }
          //Log.i(TAG, "###RtmpSendAudioData:"+audioData.length);
          rtmpSession.SendAacData(rtmpHandle, aacFrame.data, aacFrame.data.length, aacFrame.encodedTimeMs);
        }

        H264Encoder.H264Frame h264Frame = GetAndReleaseVideoQueue();
        if (h264Frame != null) {
          //Log.i(TAG, "$$$RtmpSendVideoData:"+videoData.length);
          rtmpSession.SendH264Data(rtmpHandle, h264Frame.data, h264Frame.data.length, h264Frame.encodedTimeMs);
        }
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      videoDataQueueLock.lock();
      videoDataQueue.clear();
      videoDataQueueLock.unlock();
      audioDataQueueLock.lock();
      audioDataQueue.clear();
      audioDataQueueLock.unlock();

      if ((rtmpHandle != 0) && (rtmpSession != null)) {
        rtmpSession.Disconnect(rtmpHandle);
      }
      rtmpHandle = 0;
      rtmpSession = null;
    }
  });

  public int Start(String rtmpUrl) {
    int iRet = 0;

    this.rtmpUrl = rtmpUrl;
    this.rtmpSession = new com.myb.rtmp.SrsRtmp();

    bStartFlag = true;
    h264EncoderThread.setPriority(Thread.MAX_PRIORITY);
    h264EncoderThread.start();

    return iRet;
  }

  public void Stop() {
    bStartFlag = false;
    h264EncoderThread.interrupt();
  }

  public void InsertVideoData(H264Encoder.H264Frame frame) {
    if (!bStartFlag) {
      return;
    }
    videoDataQueueLock.lock();
    if (videoDataQueue.size() > 50) {
      videoDataQueue.clear();
    }
    videoDataQueue.offer(frame);
    videoDataQueueLock.unlock();
  }

  public void InsertAudioData(AacEncoder.AacFrame frame) {
    if (!bStartFlag) {
      return;
    }
    audioDataQueueLock.lock();
    if (audioDataQueue.size() > 50) {
      audioDataQueue.clear();
    }
    audioDataQueue.offer(frame);
    audioDataQueueLock.unlock();
  }

  public H264Encoder.H264Frame GetAndReleaseVideoQueue() {
    videoDataQueueLock.lock();
    H264Encoder.H264Frame frame = videoDataQueue.poll();
    videoDataQueueLock.unlock();

    return frame;
  }

  public AacEncoder.AacFrame GetAndReleaseAudioQueue() {
    audioDataQueueLock.lock();
    AacEncoder.AacFrame frame = audioDataQueue.poll();
    audioDataQueueLock.unlock();

    return frame;
  }

  public void SetVideoParams(int frameRate) {
    this.frameRate = frameRate;
  }

  public void SetAudioParams(int sampleRate, int channelCount, int sampleSize, int soundType) {
    this.sampleRate = sampleRate;
    this.channelCount = channelCount;
    this.sampleSize = sampleSize;
    this.soundType = soundType;
  }
}
