package com.myb.rtmppush;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RtmpSessionManager {
  private final String TAG = "RtmpSessionManager";
  private Queue<byte[]> videoDataQueue = new LinkedList<byte[]>();
  private Lock videoDataQueueLock = new ReentrantLock();

  private Queue<byte[]> audioDataQueue = new LinkedList<byte[]>();
  private Lock audioDataQueueLock = new ReentrantLock();

  private com.myb.rtmp.SrsRtmp rtmpSession = null;
  private long rtmpHandle = 0;
  private String rtmpUrl = null;

  private Boolean bStartFlag = false;

  // audio params.
  private int sampleRate;
  private int channelCount;
  private int sampleSize;
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

    @Override
    public void run() {
      while (!h264EncoderThread.interrupted() && (bStartFlag)) {
        if (rtmpHandle == 0) {
          rtmpHandle = rtmpSession.Connect(rtmpUrl);
          if (rtmpHandle == 0) {
            if (!WaitforReConnect()) {
              break;
            }
            continue;
          }
        } else {
          if (rtmpSession.IsConnect(rtmpHandle) == false) {
            rtmpHandle = rtmpSession.Connect(rtmpUrl);
            if (rtmpHandle == 0) {
              if (!WaitforReConnect()) {
                break;
              }
              continue;
            }
          }
        }

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
          byte[] audioData = GetAndReleaseAudioQueue();
          if (audioData == null) {
            break;
          }
          //Log.i(TAG, "###RtmpSendAudioData:"+audioData.length);
          rtmpSession.SendAacData(rtmpHandle, audioData, audioData.length);
        }

        byte[] videoData = GetAndReleaseVideoQueue();
        if (videoData != null) {
          //Log.i(TAG, "$$$RtmpSendVideoData:"+videoData.length);
          rtmpSession.SendH264Data(rtmpHandle, videoData, videoData.length);
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

  public void InsertVideoData(byte[] videoData) {
    if (!bStartFlag) {
      return;
    }
    videoDataQueueLock.lock();
    if (videoDataQueue.size() > 50) {
      videoDataQueue.clear();
    }
    videoDataQueue.offer(videoData);
    videoDataQueueLock.unlock();
  }

  public void InsertAudioData(byte[] videoData) {
    if (!bStartFlag) {
      return;
    }
    audioDataQueueLock.lock();
    if (audioDataQueue.size() > 50) {
      audioDataQueue.clear();
    }
    audioDataQueue.offer(videoData);
    audioDataQueueLock.unlock();
  }

  public byte[] GetAndReleaseVideoQueue() {
    videoDataQueueLock.lock();
    byte[] videoData = videoDataQueue.poll();
    videoDataQueueLock.unlock();

    return videoData;
  }

  public byte[] GetAndReleaseAudioQueue() {
    audioDataQueueLock.lock();
    byte[] audioData = audioDataQueue.poll();
    audioDataQueueLock.unlock();

    return audioData;
  }

  public void SetVideoParams(int frameRate) {
    this.frameRate = frameRate;
  }

  public void SetAudioParams(int sampleRate, int channelCount, int sampleSize) {
    this.sampleRate = sampleRate;
    this.channelCount = channelCount;
    this.sampleSize = sampleSize;
  }
}
