//
// Created by Tapos Datta on 2020-04-18.
//

#include <unistd.h>
#include "VideoDecoder.h"

jobject gSurface = nullptr;
JNIEnv *jniEnv;

extern "C" JNIEXPORT void JNICALL
Java_com_example_ffmpegintregation_VideoActivity_initWithSurface(JNIEnv *env,jobject obj, jobject surface){

    gSurface = env->NewGlobalRef(surface);
    jniEnv = env;

    return;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_ffmpegintregation_VideoActivity_invoke(JNIEnv *env, jobject obj){

    int ret_code = 0;
    const char *filename = "/storage/emulated/0/DCIM/Camera/tuli pohela boishakh.mp4";
//    const char *filename = "/storage/emulated/0/DCIM/Camera/20190814_2321061577206917827.mp4";
    frtContext = avformat_alloc_context();

    if(avformat_open_input(&frtContext, filename, NULL,NULL) < 0){
        printf("Error : Invalid file");
        avformat_close_input(&frtContext);
        return -2;
    }

    if((ret_code = _getVideoStreamInformation())!=0){
        return ret_code;
    }

    if((videoCodecContext = _getVideoCodecContext(videoStreamIndex))==NULL){
        ret_code = -5;
        return ret_code;
    }

    if((avcodec_open2(videoCodecContext, videoCodec, NULL)) != 0) {
        avcodec_close(videoCodecContext);
        avcodec_free_context(&videoCodecContext);
        avformat_close_input(&frtContext);
        ret_code = -6;
        return ret_code;
    }

    ret_code = videoFrameExtract();

    return ret_code;
}

AVCodecContext *_getVideoCodecContext(int index) {
    videoCodec = avcodec_find_decoder(frtContext->streams[index]->codecpar->codec_id);
    if (videoCodec == NULL) {
        printf("Error: invalid audio codec\n");
        avformat_close_input(&frtContext);
        return NULL;
    }

    // Initialize codec context for the decoder.
    AVCodecContext* codecContext = avcodec_alloc_context3(videoCodec);
    if (codecContext == NULL) {
        // Something went wrong. Cleaning up...
        avformat_close_input(&frtContext);
        fprintf(stderr, "Could not allocate a decoding context.\n");
    }

    // Fill the codecCtx with the parameters of the codec used in the read file.
    if ((avcodec_parameters_to_context(codecContext, frtContext->streams[index]->codecpar)) != 0) {
        // Something went wrong. Cleaning up...
        avcodec_close(codecContext);
        avcodec_free_context(&codecContext);
        avformat_close_input(&frtContext);
        printf("Error setting codec context parameters.");
    }

    return codecContext;
}

int _getVideoStreamInformation() {
    // find the first audio stream
    if(avformat_find_stream_info(frtContext, NULL) < 0){
        printf("Error: It wasn't possible to find stream information in the audio file.\n");
        return -3;
    }

    for(int i=0;i<frtContext->nb_streams;i++){

        if(frtContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO ){
            videoStreamIndex = i;
            break;
        }
    }

    if(videoStreamIndex == -1){
        printf("Error: There is no audio stream in the specified file.\n");
        return -4;
    }
    return 0;
}

int videoFrameExtract(){

    AVPacket *packet = av_packet_alloc();

    AVFrame *yuv_frame = av_frame_alloc();
    AVFrame *rgb_frame = av_frame_alloc();

    // The parameters used for transcoding (scaling), the width and height before the turn, the width and height after the turn, the format, etc.
    struct SwsContext *swr_ctx = sws_getContext(videoCodecContext->width, videoCodecContext->height,
                                                videoCodecContext->pix_fmt,
                                                videoCodecContext->width, videoCodecContext->height,
                                                AV_PIX_FMT_RGBA,
                                                SWS_BICUBIC, NULL, NULL, NULL);

    // create nativewindow pointer from surface where frame data will be drawn

    ANativeWindow* nativeWindow = ANativeWindow_fromSurface(jniEnv, gSurface);
    ANativeWindow_setBuffersGeometry(nativeWindow, videoCodecContext->width, videoCodecContext->height, WINDOW_FORMAT_RGBA_8888);
    //Buffer when drawing
    ANativeWindow_Buffer out_buffer;

    int width = videoCodecContext->width;
    int height = videoCodecContext->height;
    int bufferImageSize = av_image_get_buffer_size(AV_PIX_FMT_RGBA,width,height,1);

    uint8_t * buffer = (uint8_t*)av_mallocz(bufferImageSize);

    rgb_frame->width = width;
    rgb_frame->height = height;
    rgb_frame->format = AV_PIX_FMT_RGBA;

    bool is_seek_need = true;
    int ret = 0;
    while(av_read_frame(frtContext,packet) >= 0){

        if(packet->stream_index != videoStreamIndex){
            av_packet_unref(packet);
            continue;
        }

        if(is_seek_need) {
            //trying to seek audio Stream
            int64_t target_dts_usecs =
                    packet->dts +
                    (int64_t) (70 / av_q2d(frtContext->streams[videoStreamIndex]->time_base));

            int k = av_seek_frame(frtContext, videoStreamIndex, target_dts_usecs,
                                  AVSEEK_FLAG_BACKWARD);
            if (k < 0) {
                //
            } else {
                avcodec_flush_buffers(videoCodecContext);
                is_seek_need = false;
                continue;
            }
        }

        ret = avcodec_send_packet(videoCodecContext,packet);

        if(ret < 0){
            continue;
        }
        while (ret >= 0){
            ret = avcodec_receive_frame(videoCodecContext,yuv_frame);
            if(ret == AVERROR(EAGAIN) || ret == AVERROR_EOF){
                break;
            }
            else if(ret < 0){
                break;
            }else{

                sws_scale(swr_ctx,yuv_frame->data,yuv_frame->linesize,0,videoCodecContext->height,rgb_frame->data,rgb_frame->linesize);

                //lock native window before drawing
                ANativeWindow_lock(nativeWindow, &out_buffer, NULL);

                // Initialize the buffer
                // Set the properties, pixel format, width and height
                av_image_fill_arrays(rgb_frame->data,rgb_frame->linesize,buffer,AV_PIX_FMT_RGBA,rgb_frame->width,rgb_frame->height,1);
                memcpy(out_buffer.bits, buffer,  width * height * 4);

                //The buffer of rgb_frame is the buffer of Window, the same one, it will be drawn when unlocked.
                //unlock window
                ANativeWindow_unlockAndPost(nativeWindow);
            }
        }
        usleep(60000);
    }

    free(buffer);

    av_frame_unref(yuv_frame);
    av_frame_unref(rgb_frame);
    av_packet_unref(packet);

    return 0;

}