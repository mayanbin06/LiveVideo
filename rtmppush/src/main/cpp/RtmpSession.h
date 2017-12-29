//
// Created by myb on 12/7/17.
//

#ifndef LIVEVIDEO_RTMP_H
#define LIVEVIDEO_RTMP_H

#include <string>
#include <memory>
#include "string.h"
#include "RtmpSession.h"

#ifdef __cplusplus
extern "C" {
#endif
// include c header
#include "librtmp/rtmp.h"
#include "librtmp/rtmp_sys.h"
#include "librtmp/log.h"
#include "android/log.h"
#include "time.h"

#ifdef __cplusplus
}
#endif

#define RTMP_HEAD_SIZE (sizeof(RTMPPacket) + RTMP_MAX_HEADER_SIZE)
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

/*音视频播放的重要参数，RTMP的时间戳在发送音视频前都为零，发送音视频消息只要保证时间戳是单增等间隔的就可以正常播放音视频。

音视频时间戳就根据帧率，音频参数设定：
视频帧根据帧率，在同一时间基上累加，如，25帧每秒，则按毫秒计，1000/25=40ms,在首帧pts上进行累加
    音频根据采样率及样本个数，在同一时间基上累加,如，1024个样本(1024个采样为一帧)，44100采样率（即1秒钟有44100个采样），
以毫秒计，1000*1024/44100=23.21995464852607709750566893424 ms

 这里需要将音频的参数，声道，采样率，采样个数，传进来。
 视频需要将帧率，传进来。
 重构这个接口类。
 */
class RtmpSession {

public:
    virtual int Init(std::string url, int timeOut);
    virtual int SendSpsAndPps(uint8_t *sps, int spsLen, uint8_t *pps, int ppsLen,
                              long timestamp);
    virtual int SendVideoData(uint8_t *data, int len, long timestamp);
    //发送音频关键帧
    virtual int SendAacSpec(uint8_t *data, int len);
    virtual int SendAacData(uint8_t *data, int len,long timestamp);
    virtual int Stop() const;
    virtual bool IsConnect();

    virtual ~RtmpSession();

private:
    RTMP* rtmp;
};


#endif //LIVEVIDEO_RTMP_H
