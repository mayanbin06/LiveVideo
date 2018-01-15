package com.myb.rtmppush.ui;

import android.app.Activity;
import android.os.Bundle;

import com.myb.fdkaac.AacEncoder;
import com.myb.fdkaac.FdkAacEncoder;
import com.myb.fdkaac.SoftwareAacEncoder;
import com.myb.h264.H264Encoder;
import com.myb.h264.SoftwareH264Encoder;
import com.myb.rtmppush.RtmpSessionManager;
import com.myb.rtmppush.R;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.content.Intent;
import android.view.KeyEvent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;

public class PushActivity extends Activity {

  private final static int ID_RTMP_PUSH_START = 100;
  private final static int ID_RTMP_PUSH_EXIT = 101;

  //实际的 长度和宽度 在Camera Preview Sizes 里选一个。
  private final int WIDTH_DEF = 640;
  private final int HEIGHT_DEF = 480;

  private final int FRAMERATE_DEF = 25;
  private final int BITRATE_DEF = 800 * 1000;

  // use real sample_rate when audio record init.
  private int SAMPLE_RATE_DEF = 22050;
  // 以录音设备支持为准
  private int CHANNEL_NUMBER_DEF = 2;
  private int AUDIO_FORMAT_DEF = AudioFormat.ENCODING_PCM_16BIT;
  private int AUDIO_SOUND_TYPE = AudioFormat.CHANNEL_IN_MONO;

  private final String LOG_TAG = "RTMP-MAIN-ACTIVITY";
  private final boolean DEBUG_ENABLE = true;

  private String rtmpUrl = "";

  private PowerManager.WakeLock wakeLock;
  private DataOutputStream outputStream = null;

  // audio
  private AudioRecord audioRecorder = null;
  private byte[] recorderBuffer = null;
  private AacEncoder aacEnc = null;

  // video
  public SurfaceView surfaceView = null;
  private Camera camera = null;
  private Camera.CameraInfo cameraInfo;
  private SoftwareH264Encoder swEncH264 = null;
  private int degrees = 0;

  private int recorderBufferSize = 0;
  private int cameraCodecType = android.graphics.ImageFormat.NV21;
  private boolean startFlag = false;
  private boolean bIsFront = true;
  private Button switchCameraBtn = null;

  private RtmpSessionManager rtmpSessionMgr = null;

  private Queue<byte[]> YUVQueue = new LinkedList<byte[]>();
  private Lock yuvQueueLock = new ReentrantLock();

  private Thread h264EncoderThread = null;

  private Runnable h264Runnable = new Runnable() {
    @Override
    public void run() {
      while (!h264EncoderThread.interrupted() && startFlag) {
        int iSize = YUVQueue.size();
        if (iSize > 0) {
          yuvQueueLock.lock();
          byte[] yuvData = YUVQueue.poll();
          if (iSize > 9) {
            Log.i(LOG_TAG, "###YUV Queue len=" + YUVQueue.size() + ", YUV length=" + yuvData.length);
          }

          yuvQueueLock.unlock();
          if (yuvData == null) {
            continue;
          }

          int rotate = 0;
          switch (degrees) {
            case 0:
              // 正方向,竖直
              break;
            case 90:
              rotate = bIsFront ? 90 : 270;
            case 180:
              rotate = 180;
            case 270:
              rotate = bIsFront ? 270 : 90;
          }

          H264Encoder.H264Frame h264Data = swEncH264.encode(yuvData, cameraCodecType, WIDTH_DEF, HEIGHT_DEF, rotate);

          if (h264Data != null) {
            rtmpSessionMgr.InsertVideoData(h264Data);
            if (DEBUG_ENABLE) {
              try {
                outputStream.write(h264Data.data);
                int iH264Len = h264Data.data.length;
              } catch (IOException e1) {
                e1.printStackTrace();
              }
            }
          }
        }
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      YUVQueue.clear();
    }
  };


  private Runnable aacEncoderRunnable = new Runnable() {
    @Override
    public void run() {
      DataOutputStream outputStream = null;
      if (DEBUG_ENABLE) {
        File saveDir = Environment.getExternalStorageDirectory();
        String strFilename = saveDir + "/aaa.aac";
        try {
          outputStream = new DataOutputStream(new FileOutputStream(strFilename));
        } catch (FileNotFoundException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }

      long lSleepTime = SAMPLE_RATE_DEF * 16 * 2 / recorderBuffer.length;

      while (!aacEncoderThread.interrupted() && startFlag) {
        int iPCMLen = audioRecorder.read(recorderBuffer, 0, recorderBuffer.length); // Fill buffer
        if ((iPCMLen != audioRecorder.ERROR_BAD_VALUE) && (iPCMLen != 0)) {
          AacEncoder.AacFrame frame = aacEnc.encode(recorderBuffer);
          if (frame != null) {
            long lLen = frame.data.length;

            rtmpSessionMgr.InsertAudioData(frame);
            //Log.i(LOG_TAG, "fdk aac length="+lLen+" from pcm="+iPCMLen);
            if (DEBUG_ENABLE) {
              try {
                outputStream.write(frame.data);
              } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        } else {
          Log.i(LOG_TAG, "######fail to get PCM data");
        }
        try {
          Thread.sleep(lSleepTime / 10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      Log.i(LOG_TAG, "AAC Encoder Thread ended ......");
    }
  };
  private Thread aacEncoderThread = null;

  private int getDispalyRotation() {
    int i = getWindowManager().getDefaultDisplay().getRotation();
    switch (i) {
      case Surface.ROTATION_0:
        return 0;
      case Surface.ROTATION_90:
        return 90;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_270:
        return 270;
    }
    return 0;
  }

  private int getDisplayOritation(int degrees, int cameraId) {
    Camera.CameraInfo info = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, info);
    int result = 0;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (info.orientation + degrees) % 360;
      result = (360 - result) % 360;
    } else {
      result = (info.orientation - degrees + 360) % 360;
    }
    return result;
  }

  private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

    @Override
    public void onPreviewFrame(byte[] yuvData, Camera camera) {
      if (!startFlag) {
        return;
      }
      // 将耗时操作放到Encode线程中。
      yuvQueueLock.lock();
      // 队列变长，弱网丢帧
      // 其他可行的策略，降低preview的帧率，减低编码码率。
      if (YUVQueue.size() > 1) {
        Log.i(LOG_TAG, "clear yuvQueue = " + YUVQueue.size());
        YUVQueue.clear();
      }
      YUVQueue.offer(yuvData);
      yuvQueueLock.unlock();
    }
  };

  public void InitCamera() {
    Camera.Parameters p = camera.getParameters();

    Size prevewSize = p.getPreviewSize();
    Log.i(LOG_TAG, "Original Width:" + prevewSize.width + ", height:" + prevewSize.height);

    List<Size> PreviewSizeList = p.getSupportedPreviewSizes();
    List<Integer> PreviewFormats = p.getSupportedPreviewFormats();
    Log.i(LOG_TAG, "Listing all supported preview sizes");

    for (Camera.Size size : PreviewSizeList) {
      Log.i(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
    }

    Log.i(LOG_TAG, "Listing all supported preview formats");
    Integer iNV21Flag = 0;
    Integer iYV12Flag = 0;
    for (Integer yuvFormat : PreviewFormats) {
      Log.i(LOG_TAG, "preview formats:" + yuvFormat);
      if (yuvFormat == android.graphics.ImageFormat.YV12) {
        iYV12Flag = android.graphics.ImageFormat.YV12;
      }
      if (yuvFormat == android.graphics.ImageFormat.NV21) {
        iNV21Flag = android.graphics.ImageFormat.NV21;
      }
    }

    if (iNV21Flag != 0) {
      cameraCodecType = iNV21Flag;
    } else if (iYV12Flag != 0) {
      cameraCodecType = iYV12Flag;
    }
    p.setPreviewSize(WIDTH_DEF, HEIGHT_DEF);
    p.setPreviewFormat(cameraCodecType);
    p.setPreviewFrameRate(FRAMERATE_DEF);

    // 设置相机角度。。。
    camera.setDisplayOrientation(degrees);
    p.setRotation(degrees);
    camera.setPreviewCallback(previewCallback);
    camera.setParameters(p);
    try {
      camera.setPreviewDisplay(surfaceView.getHolder());
    } catch (Exception e) {
      return;
    }
    camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。

    camera.startPreview();
  }

  private final class SurfaceCallBack implements SurfaceHolder.Callback {
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      camera.autoFocus(new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
          if (success) {
            InitCamera();
            camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
          }
        }
      });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      degrees = getDisplayOritation(getDispalyRotation(), 0);
      if (camera != null) {
        InitCamera();
        return;
      }
      camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
      InitCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
  }

  private void Start() {
    if (DEBUG_ENABLE) {
      File saveDir = Environment.getExternalStorageDirectory();
      String strFilename = saveDir + "/aaa.h264";
      try {
        outputStream = new DataOutputStream(new FileOutputStream(strFilename));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }

    rtmpSessionMgr = new RtmpSessionManager();
    // srs rtmp support 8 and 16, 0-mono, 1-stero.
    rtmpSessionMgr.SetAudioParams(SAMPLE_RATE_DEF, CHANNEL_NUMBER_DEF,
        AUDIO_FORMAT_DEF == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8,
        AUDIO_SOUND_TYPE == AudioFormat.CHANNEL_IN_MONO ? 0 : 1);
    rtmpSessionMgr.SetVideoParams(FRAMERATE_DEF);
    rtmpSessionMgr.Start(rtmpUrl);

    int iFormat = cameraCodecType;
    swEncH264 = new SoftwareH264Encoder(WIDTH_DEF, HEIGHT_DEF, FRAMERATE_DEF, BITRATE_DEF);
    //swEncH264.start(iFormat);

    startFlag = true;

    h264EncoderThread = new Thread(h264Runnable);
    h264EncoderThread.setPriority(Thread.MAX_PRIORITY);
    h264EncoderThread.start();

    audioRecorder.startRecording();
    aacEncoderThread = new Thread(aacEncoderRunnable);
    aacEncoderThread.setPriority(Thread.MAX_PRIORITY);
    aacEncoderThread.start();
  }

  private void Stop() {
    startFlag = false;

    aacEncoderThread.interrupt();
    h264EncoderThread.interrupt();

    audioRecorder.stop();
    //swEncH264.stop();

    rtmpSessionMgr.Stop();

    yuvQueueLock.lock();
    YUVQueue.clear();
    yuvQueueLock.unlock();

    if (DEBUG_ENABLE) {
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private OnClickListener switchCameraOnClickedEvent = new OnClickListener() {
    @Override
    public void onClick(View arg0) {
      if (camera == null) {
        return;
      }
      camera.setPreviewCallback(null);
      camera.stopPreview();
      camera.release();
      camera = null;

      if (bIsFront) {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
      } else {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
      }
      bIsFront = !bIsFront;
      InitCamera();
    }
  };

  private void InitAudioRecord() {
    recorderBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_DEF,
        AUDIO_SOUND_TYPE,
        AUDIO_FORMAT_DEF);
    audioRecorder = new AudioRecord(AudioSource.MIC,
        SAMPLE_RATE_DEF, AUDIO_SOUND_TYPE,
        AUDIO_FORMAT_DEF, recorderBufferSize);
    recorderBuffer = new byte[recorderBufferSize];

    if (audioRecorder.getState() == audioRecorder.STATE_UNINITIALIZED) {
      Log.e(LOG_TAG, "Failed to init audio record, make sure have the right params!");
    } else {
      SAMPLE_RATE_DEF = audioRecorder.getSampleRate();
      CHANNEL_NUMBER_DEF = audioRecorder.getChannelCount();
      AUDIO_FORMAT_DEF = audioRecorder.getAudioFormat();
    }

    aacEnc = new SoftwareAacEncoder(SAMPLE_RATE_DEF, CHANNEL_NUMBER_DEF);
  }

  public Handler mHandler = new Handler() {
    public void handleMessage(android.os.Message msg) {
      Bundle b = msg.getData();
      int ret;
      switch (msg.what) {
        case ID_RTMP_PUSH_START: {
          Start();
          break;
        }
      }
    }
  };

  private void RtmpStartMessage() {
    Message msg = new Message();
    msg.what = ID_RTMP_PUSH_START;
    Bundle b = new Bundle();
    b.putInt("ret", 0);
    msg.setData(b);
    mHandler.sendMessage(msg);
  }

  private void InitAll() {
    WindowManager wm = this.getWindowManager();

    int width = wm.getDefaultDisplay().getWidth();
    int height = wm.getDefaultDisplay().getHeight();
    int iNewWidth = (int) (height * 3.0 / 4.0);

    RelativeLayout rCameraLayout = (RelativeLayout) findViewById(R.id.cameraRelative);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
        RelativeLayout.LayoutParams.MATCH_PARENT);
    int iPos = width - iNewWidth;
    layoutParams.setMargins(iPos, 0, 0, 0);

    surfaceView = (SurfaceView) this.findViewById(R.id.surfaceViewEx);
    surfaceView.getHolder().setFixedSize(WIDTH_DEF, HEIGHT_DEF);
    surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    surfaceView.getHolder().setKeepScreenOn(true);
    surfaceView.getHolder().addCallback(new SurfaceCallBack());
    surfaceView.setLayoutParams(layoutParams);

    InitAudioRecord();

    switchCameraBtn = (Button) findViewById(R.id.SwitchCamerabutton);
    switchCameraBtn.setOnClickListener(switchCameraOnClickedEvent);

    RtmpStartMessage();//开始推流
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // 确保摄像头，录音的权限。
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_push);
    // 竖屏显示， 录像一般只是竖屏，和拍照一样。
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    Intent intent = getIntent();
    rtmpUrl = intent.getStringExtra("push_url");

    InitAll();

    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "LiveVideo");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    //getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  protected void onDestroy() {
    super.onDestroy();
    Log.i(LOG_TAG, "PushActivity onDestroy...");
  }

  protected void onResume() {
    super.onResume();
    wakeLock.acquire();
  }

  protected void onPause() {
    super.onPause();
    wakeLock.release();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      AlertDialog isExit = new AlertDialog.Builder(this).create();
      isExit.setTitle("系统提示");
      isExit.setMessage("确定要退出吗");
      isExit.setButton("确定", listener);
      isExit.setButton2("取消", listener);
      isExit.show();
    }

    return false;
  }

  DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int which) {
      switch (which) {
        case AlertDialog.BUTTON_POSITIVE: {// "确认"按钮退出程序
          if (camera != null) {
            try {
              camera.setPreviewCallback(null);
              camera.setPreviewDisplay(null);
            } catch (IOException e) {
              e.printStackTrace();
            }
            camera.stopPreview();
            camera.release();
            camera = null;
          }
          if (startFlag) {
            Stop();
          }
          PushActivity.this.finish();

          break;
        }
        case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框
          break;
        default:
          break;
      }
    }
  };
}
