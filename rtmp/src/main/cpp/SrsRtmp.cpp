#include "SrsRtmp.h"

#define LOG_TAG "RTMP"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, \
                   __VA_ARGS__))

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, \
                   __VA_ARGS__))

SrsRtmp::SrsRtmp(const std::string& url, int time_out)
    : url_(url), time_out_(time_out), connected_(false) {
}

SrsRtmp::~SrsRtmp() {
  Stop();
}

bool SrsRtmp::IsConnect() {
  return connected_;
}

bool SrsRtmp::Connect() {
  srs_rtmp_t rtmp = srs_rtmp_create(url_.c_str());

  if (srs_rtmp_handshake(rtmp) != 0
      || srs_rtmp_connect_app(rtmp) != 0
      || srs_rtmp_publish_stream(rtmp) != 0) {
    srs_rtmp_destroy(rtmp);
    return false;
  }

  rtmp_ = rtmp;
  connected_ = true;
  return true;
}

bool SrsRtmp::Stop() {
  if (connected_) {
    srs_rtmp_destroy(rtmp_);
    rtmp_ = NULL;
    connected_ = false;
  }
}

int SrsRtmp::SendH264Data(uint8_t* data, int len, long timestamp) {
  if (!connected_) {
    return -1;
  }

  int dts = timestamp, pts = timestamp, ret = 0;
  ret = srs_h264_write_raw_frames(rtmp_, (char*)data, len, dts, pts);
  if (ret != 0) {
    if (srs_h264_is_dvbsp_error(ret)
        || srs_h264_is_duplicated_sps_error(ret)
        || srs_h264_is_duplicated_pps_error(ret)) {
      LOGI(LOG_TAG, "ignore error, code = %d", ret);
    } else {
      LOGE(LOG_TAG, "fatal error, code = %d", ret);
      Stop();
      return ret;
    }
  }
  int nb_start_code = 4;
  // 5bits, 7.3.1 NAL unit syntax,
  // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
  //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame, 9: AUD, 6: SEI
  int nut = data[nb_start_code] & 0x1f;
  LOGI("sent packet: type=%s, time=%d, size=%d, fps=%.2f, b[%d]=%#x(%s)",
     srs_human_flv_tag_type2string(SRS_RTMP_TYPE_VIDEO), dts, len, frame_rate_,
     nb_start_code, (char)data[nb_start_code],
     (nut == 7? "SPS":(nut == 8? "PPS":(nut == 5? "I":
            (nut == 1? "P":(nut == 9? "AUD":(nut == 6? "SEI":"Unknown")))))));
  return 0;
}

// https://github.com/ossrs/srs/blob/master/trunk/research/librtmp/srs_aac_raw_publish.c
int SrsRtmp::SendAacData(uint8_t* data, int len, long timestamp) {
  if (!connected_) {
    return -1;
  }
  // 0 = Linear PCM, platform endian
  // 1 = ADPCM
  // 2 = MP3
  // 7 = G.711 A-law logarithmic PCM
  // 8 = G.711 mu-law logarithmic PCM
  // 10 = AAC
  // 11 = Speex
  char sound_format = 10;
  // 2 = 22 kHz
  char sound_rate = 2;
  // 1 = 16-bit samples
  char sound_size = 1;
  // 1 = Stereo sound
  char sound_type = 1;

  int ret = 0;
  if ((ret = srs_audio_write_raw_frame(
      rtmp_, sound_format, sound_rate,
      sound_size, sound_type,
      (char*)data, len, timestamp)) != 0) {
    Stop();
    return -1;
  }
  LOGI("sent packet: type=%s, time=%d, size=%d, codec=%d, rate=%d, sample=%d, channel=%d", 
      srs_human_flv_tag_type2string(SRS_RTMP_TYPE_AUDIO), timestamp, len, sound_format,
      sound_rate, sound_size, sound_type);
  return ret;
}

void SrsRtmp::SetAudioParams(int sample_rate, int channel_count, int sample_format) {
  sample_rate_ = sample_rate;
  channel_count_ = channel_count;
  sample_format_ = sample_format;
}
void SrsRtmp::SetVideoParams(int frame_rate) {
  frame_rate_ = frame_rate;
}
