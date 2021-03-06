//
// Created by myb on 12/7/17.
//

#ifndef LIVEVIDEO_SRS_RTMP_H
#define LIVEVIDEO_SRS_RTMP_H

#include <string>
#include <memory>

#ifdef __cplusplus
extern "C" {
#endif
// include c header
#include <sys/types.h>
#include <sys/stat.h>
#include "android/log.h"
#include "time.h"
#include "srs_librtmp.h"
#ifdef __cplusplus
}
#endif

#define NAL_SLICE  1
#define NAL_SLICE_DPA  2
#define NAL_SLICE_DPB  3
#define NAL_SLICE_DPC  4
#define NAL_SLICE_IDR  5
#define NAL_SEI  6
#define NAL_SPS  7
#define NAL_PPS  8
#define NAL_AUD  9
#define NAL_FILLER  12

#define STREAM_CHANNEL_METADATA  0x03
#define STREAM_CHANNEL_VIDEO     0x04
#define STREAM_CHANNEL_AUDIO     0x05

class SrsRtmp {

public:
  virtual int SendH264Data(uint8_t *data, int len, long timestamp);
  virtual int SendAacData(uint8_t *data, int len,long timestamp);
  virtual bool Stop();
  virtual bool Connect();
  virtual bool IsConnect();
  virtual void SetAudioParams(int sample_rate, int channel_count, int sample_format, int sound_type);
  virtual void SetVideoParams(int frame_rate);
  virtual ~SrsRtmp();
  virtual std::string url() { return url_;}

  SrsRtmp(const std::string& url, int time_out);

private:
  srs_rtmp_t rtmp_;
  std::string url_;
  int time_out_;
  bool connected_;
  int sample_rate_;
  int channel_count_;
  int sample_format_;
  int sound_type_;
  int frame_rate_;
};


#endif //LIVEVIDEO_SRS_RTMP_H
