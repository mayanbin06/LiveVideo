package com.myb.rtmppush.ui;

import android.app.Activity;
import android.os.Bundle;

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

import com.myb.fdkaac.FdkAacEncode;
import com.myb.rtmppush.RtmpSessionManager;
import com.myb.h264.SoftwareH264Encoder;

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

  private final String LOG_TAG = "RTMP-MAIN-ACTIVITY";
  private final boolean DEBUG_ENABLE = true;

  private String _rtmpUrl = "";

  PowerManager.WakeLock _wakeLock;
  private DataOutputStream _outputStream = null;

  // audio
  private AudioRecord _AudioRecorder = null;
  private byte[] _RecorderBuffer = null;
  private FdkAacEncode _fdkaacEnc = null;
  private long _fdkaacHandle = 0;

  // video
  public SurfaceView _mSurfaceView = null;
  private Camera _mCamera = null;
  private Camera.CameraInfo cameraInfo;
  private boolean _bIsFront = true;
  private SoftwareH264Encoder _swEncH264 = null;
  private int _iDegrees = 0;

  private int _iRecorderBufferSize = 0;

  private Button _SwitchCameraBtn = null;

  private boolean _bStartFlag = false;

  private int _iCameraCodecType = android.graphics.ImageFormat.NV21;

  private byte[] _yuvNV21 = new byte[(WIDTH_DEF * HEIGHT_DEF * 3 / 2)];
  private byte[] _yuvEdit = new byte[(WIDTH_DEF * HEIGHT_DEF * 3 / 2)];

  private RtmpSessionManager _rtmpSessionMgr = null;

  private Queue<byte[]> _YUVQueue = new LinkedList<byte[]>();
  private Lock _yuvQueueLock = new ReentrantLock();

  private Thread _h264EncoderThread = null;

  private void dumpYuvData(byte[] yuvData, String name, int width, int height) {
    DataOutputStream outputStream = null;
    File saveDir = Environment.getExternalStorageDirectory();
    String strFilename = saveDir + File.separator + name;
    try {
      //int[] stride = new int[] {width, width >> 1, width >> 1};
      //YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.YUV_420_888, width, height, stride);
      outputStream = new DataOutputStream(new FileOutputStream(strFilename));
      outputStream.write(yuvData, 0, yuvData.length);
      //yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, outputStream);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (outputStream != null) outputStream.close();
      } catch (Exception e) {
      }
    }
  }

  private Runnable _h264Runnable = new Runnable() {
    @Override
    public void run() {
      while (!_h264EncoderThread.interrupted() && _bStartFlag) {
        int iSize = _YUVQueue.size();
        if (iSize > 0) {
          _yuvQueueLock.lock();
          byte[] yuvData = _YUVQueue.poll();
          if (iSize > 9) {
            Log.i(LOG_TAG, "###YUV Queue len=" + _YUVQueue.size() + ", YUV length=" + yuvData.length);
          }

          _yuvQueueLock.unlock();
          if (yuvData == null) {
            continue;
          }

          int rotate = 0;
          switch (_iDegrees) {
            case 0:
              // 正方向,竖直
              break;
            case 90:
              rotate = _bIsFront ? 90 : 270;
            case 180:
              rotate = 180;
            case 270:
              rotate = _bIsFront ? 270 : 90;
          }

          byte[] h264Data = _swEncH264.encode(yuvData, _iCameraCodecType, WIDTH_DEF, HEIGHT_DEF, rotate);

          if (h264Data != null) {
            _rtmpSessionMgr.InsertVideoData(h264Data);
            if (DEBUG_ENABLE) {
              try {
                _outputStream.write(h264Data);
                int iH264Len = h264Data.length;
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
      _YUVQueue.clear();
    }
  };


  private Runnable _aacEncoderRunnable = new Runnable() {
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

      long lSleepTime = SAMPLE_RATE_DEF * 16 * 2 / _RecorderBuffer.length;

      while (!_AacEncoderThread.interrupted() && _bStartFlag) {
        int iPCMLen = _AudioRecorder.read(_RecorderBuffer, 0, _RecorderBuffer.length); // Fill buffer
        if ((iPCMLen != _AudioRecorder.ERROR_BAD_VALUE) && (iPCMLen != 0)) {
          if (_fdkaacHandle != 0) {
            byte[] aacBuffer = _fdkaacEnc.FdkAacEncode(_fdkaacHandle, _RecorderBuffer);
            if (aacBuffer != null) {
              long lLen = aacBuffer.length;

              _rtmpSessionMgr.InsertAudioData(aacBuffer);
              //Log.i(LOG_TAG, "fdk aac length="+lLen+" from pcm="+iPCMLen);
              if (DEBUG_ENABLE) {
                try {
                  outputStream.write(aacBuffer);
                } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
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
  private Thread _AacEncoderThread = null;

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

  private Camera.PreviewCallback _previewCallback = new Camera.PreviewCallback() {

    @Override
    public void onPreviewFrame(byte[] yuvData, Camera camera) {
      if (!_bStartFlag) {
        return;
      }
      // 将耗时操作放到Encode线程中。
      _yuvQueueLock.lock();
      // 队列变长，弱网丢帧
      // 其他可行的策略，降低preview的帧率，减低编码码率。
      if (_YUVQueue.size() > 1) {
        _YUVQueue.clear();
      }
      _YUVQueue.offer(yuvData);
      _yuvQueueLock.unlock();
    }
  };

  public void InitCamera() {
    Camera.Parameters p = _mCamera.getParameters();

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
      _iCameraCodecType = iNV21Flag;
    } else if (iYV12Flag != 0) {
      _iCameraCodecType = iYV12Flag;
    }
    p.setPreviewSize(WIDTH_DEF, HEIGHT_DEF);
    p.setPreviewFormat(_iCameraCodecType);
    p.setPreviewFrameRate(FRAMERATE_DEF);

    // 设置相机角度。。。
    _mCamera.setDisplayOrientation(_iDegrees);
    p.setRotation(_iDegrees);
    _mCamera.setPreviewCallback(_previewCallback);
    _mCamera.setParameters(p);
    try {
      _mCamera.setPreviewDisplay(_mSurfaceView.getHolder());
    } catch (Exception e) {
      return;
    }
    _mCamera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。

    _mCamera.startPreview();
  }

  private final class SurceCallBack implements SurfaceHolder.Callback {
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      _mCamera.autoFocus(new Camera.AutoFocusCallback() {
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
      _iDegrees = getDisplayOritation(getDispalyRotation(), 0);
      if (_mCamera != null) {
        InitCamera();
        return;
      }
      _mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
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
        _outputStream = new DataOutputStream(new FileOutputStream(strFilename));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }

    _rtmpSessionMgr = new RtmpSessionManager();
    _rtmpSessionMgr.SetAudioParams(SAMPLE_RATE_DEF, CHANNEL_NUMBER_DEF, AUDIO_FORMAT_DEF);
    _rtmpSessionMgr.SetVideoParams(FRAMERATE_DEF);
    _rtmpSessionMgr.Start(_rtmpUrl);

    int iFormat = _iCameraCodecType;
    _swEncH264 = new SoftwareH264Encoder(WIDTH_DEF, HEIGHT_DEF, FRAMERATE_DEF, BITRATE_DEF);
    //_swEncH264.start(iFormat);

    _bStartFlag = true;

    _h264EncoderThread = new Thread(_h264Runnable);
    _h264EncoderThread.setPriority(Thread.MAX_PRIORITY);
    _h264EncoderThread.start();

    _AudioRecorder.startRecording();
    _AacEncoderThread = new Thread(_aacEncoderRunnable);
    _AacEncoderThread.setPriority(Thread.MAX_PRIORITY);
    _AacEncoderThread.start();
  }

  private void Stop() {
    _bStartFlag = false;

    _AacEncoderThread.interrupt();
    _h264EncoderThread.interrupt();

    _AudioRecorder.stop();
    //_swEncH264.stop();

    _rtmpSessionMgr.Stop();

    _yuvQueueLock.lock();
    _YUVQueue.clear();
    _yuvQueueLock.unlock();

    if (DEBUG_ENABLE) {
      if (_outputStream != null) {
        try {
          _outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private OnClickListener _switchCameraOnClickedEvent = new OnClickListener() {
    @Override
    public void onClick(View arg0) {
      if (_mCamera == null) {
        return;
      }
      _mCamera.setPreviewCallback(null);
      _mCamera.stopPreview();
      _mCamera.release();
      _mCamera = null;

      if (_bIsFront) {
        _mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
      } else {
        _mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
      }
      _bIsFront = !_bIsFront;
      InitCamera();
    }
  };

  private void InitAudioRecord() {
    _iRecorderBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_DEF,
        AudioFormat.CHANNEL_IN_MONO,
        AUDIO_FORMAT_DEF);
    _AudioRecorder = new AudioRecord(AudioSource.MIC,
        SAMPLE_RATE_DEF, AudioFormat.CHANNEL_IN_MONO,
        AUDIO_FORMAT_DEF, _iRecorderBufferSize);
    _RecorderBuffer = new byte[_iRecorderBufferSize];

    if (_AudioRecorder.getState() == _AudioRecorder.STATE_UNINITIALIZED) {
      Log.e(LOG_TAG, "Failed to init audio record, make sure have the right params!");
    } else {
      SAMPLE_RATE_DEF = _AudioRecorder.getSampleRate();
      CHANNEL_NUMBER_DEF = _AudioRecorder.getChannelCount();
      AUDIO_FORMAT_DEF = _AudioRecorder.getAudioFormat();
    }

    _fdkaacEnc = new FdkAacEncode();
    _fdkaacHandle = _fdkaacEnc.FdkAacInit(SAMPLE_RATE_DEF, CHANNEL_NUMBER_DEF);
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

    _mSurfaceView = (SurfaceView) this.findViewById(R.id.surfaceViewEx);
    _mSurfaceView.getHolder().setFixedSize(WIDTH_DEF, HEIGHT_DEF);
    _mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    _mSurfaceView.getHolder().setKeepScreenOn(true);
    _mSurfaceView.getHolder().addCallback(new SurceCallBack());
    _mSurfaceView.setLayoutParams(layoutParams);

    InitAudioRecord();

    _SwitchCameraBtn = (Button) findViewById(R.id.SwitchCamerabutton);
    _SwitchCameraBtn.setOnClickListener(_switchCameraOnClickedEvent);

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
    _rtmpUrl = intent.getStringExtra("push_url");

    InitAll();

    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    _wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "LiveVideo");
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
    _wakeLock.acquire();
  }

  protected void onPause() {
    super.onPause();
    _wakeLock.release();
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
          if (_mCamera != null) {
            try {
              _mCamera.setPreviewCallback(null);
              _mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
              e.printStackTrace();
            }
            _mCamera.stopPreview();
            _mCamera.release();
            _mCamera = null;
          }
          if (_bStartFlag) {
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
