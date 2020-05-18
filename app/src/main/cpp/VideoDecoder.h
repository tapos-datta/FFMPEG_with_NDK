//
// Created by Tapos Datta on 2020-04-18.
//


#include <jni.h>
#include <string>

extern "C"{
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
#include <libavutil/samplefmt.h>
#include "libswscale/swscale.h"
#include <android/log.h>
#include <libavdevice/avdevice.h>
#include <libavutil/imgutils.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
}


AVFormatContext *frtContext;
AVCodec *videoCodec = NULL;
AVCodecContext *videoCodecContext = NULL;
int videoStreamIndex = -1;

int _getVideoStreamInformation();

AVCodecContext *_getVideoCodecContext(int index);

int videoFrameExtract();
