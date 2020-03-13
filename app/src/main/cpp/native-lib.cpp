/**
 * ref : https://github.com/xiahongze/ffmpeg-audio-decode-example/blob/master/main.c
 * ref : https://stackoverflow.com/questions/27486618/audio-playback-using-ffmpeg-and-audiotrack-android
 */

#include <jni.h>
#include <string>


extern "C"{
    #include <libavcodec/avcodec.h>
    #include <libavformat/avformat.h>
    #include <libavutil/avutil.h>
    #include <libswresample/swresample.h>
    #include <libavutil/opt.h>
    #include <libavutil/samplefmt.h>
    #include <android/log.h>
}
#define  LOG_TAG    "testjni"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

AVFormatContext *formatContext = NULL;
SwrContext *swr_ctx = NULL;
int audioStreamIndex = -1;
AVCodec *codec = NULL;
typedef uint8_t DATA_TYPE;
int64_t max_dst_nb_samples, dst_nb_samples;
uint8_t* dst_data =NULL;
int dst_linesize;
int dst_bufsize;

int _getStreamInformation();

AVCodecContext* _getCodecContext(int index);

void _setUpSWR(AVFrame *pFrame);


extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ffmpegintregation_DecodeAudio_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}




extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ffmpegintregation_DecodeAudio_decodeFile(JNIEnv *env, jobject obj, jstring filePath,jobject listener){


    const char *filename = env->GetStringUTFChars(filePath,NULL);

    jclass cls = env->GetObjectClass(obj);
    jmethodID methodId  = env->GetMethodID(cls,"setRawData","([B)V");

  //  printf("file path %s \n", filename);
    DATA_TYPE* data = NULL;  // output of the audio data
    size_t size = 0;  // size of the output
    int ret_code;     // checking return code at each step
    bool isSetupSWR = false;

    int flag = 0;
    AVCodecContext *codecContext= NULL;
    formatContext = avformat_alloc_context();

    if(avformat_open_input(&formatContext, filename, NULL,NULL) < 0){
        printf("Error : Invalid file");
        ret_code = -2;
        return reinterpret_cast<jstring>(ret_code);
    }

    if((ret_code = _getStreamInformation())!=0){
        return reinterpret_cast<jstring>(ret_code);
    }

    if((codecContext = _getCodecContext(audioStreamIndex))==NULL){
        ret_code = -5;
        return reinterpret_cast<jstring>(ret_code);
    }

    // Initialize the decoder.
    if((avcodec_open2(codecContext, codec, NULL)) != 0) {
        avcodec_close(codecContext);
        avcodec_free_context(&codecContext);
        avformat_close_input(&formatContext);
        ret_code = -6;
        return reinterpret_cast<jstring>(ret_code);
    }


    //prepare the packet.
    AVPacket *packet = av_packet_alloc();
    //set default values

    while (av_read_frame(formatContext, packet) >= 0) {
        if (packet->stream_index != audioStreamIndex) {
            av_packet_unref(packet);
            continue;
        }

        ret_code = avcodec_send_packet(codecContext, packet);

        if (ret_code < 0) {
            fprintf(stderr, "warning: failed to send packet to codec (%d)\n", ret_code);
            av_packet_unref(packet);
            continue;
        }
        AVFrame* frame = av_frame_alloc();
        ret_code = avcodec_receive_frame(codecContext, frame);
        if (ret_code == AVERROR(EAGAIN)) {
            // output not available at this stage, try again
            av_frame_unref(frame);
            av_packet_unref(packet);
            continue;
        }
        if (ret_code < 0) {
            fprintf(stderr, "error: failed to receive frame from codec (%d)\n", ret_code);
            av_frame_unref(frame);
            av_packet_unref(packet);
            flag = -8;
            break;
        }


        if(!isSetupSWR){
            _setUpSWR(frame);
            isSetupSWR = true;
        }

        jbyteArray samples_byte_array;
        uint8_t *output_Buff = NULL;
        dst_bufsize = 0;

        if(frame->format != AV_SAMPLE_FMT_S16){

            /* compute the number of converted samples: buffering is avoided
             * ensuring that the output buffer will contain at least all the
             * converted input samples */
            max_dst_nb_samples = dst_nb_samples =
                        av_rescale_rnd(frame->nb_samples, frame->sample_rate, frame->sample_rate, AV_ROUND_UP);

            int ret = av_samples_alloc(&dst_data, &dst_linesize, frame->channels,
                                   dst_nb_samples, AV_SAMPLE_FMT_S16, 0);
            if (ret < 0) {
                //   LOGV("Could not allocate destination samples\n");
                //goto end;
            }

            /* compute destination number of samples */
            dst_nb_samples = av_rescale_rnd(swr_get_delay(swr_ctx, frame->sample_rate) +
                                            frame->nb_samples, frame->sample_rate, frame->sample_rate, AV_ROUND_UP);

            /* convert to destination format */
            ret = swr_convert(swr_ctx, &dst_data, dst_nb_samples, (const uint8_t **)frame->data, frame->nb_samples);

            dst_bufsize = av_samples_get_buffer_size(&dst_linesize, frame->channels,
                                                         ret, AV_SAMPLE_FMT_S16, 1);
            if (dst_bufsize < 0) {
                    //LOGV("Could not get sample buffer size\n");
                    //goto end;
            }
            output_Buff = { &dst_data[0] };

        }else{

            dst_bufsize = av_samples_get_buffer_size(NULL,frame->channels , frame->nb_samples,AV_SAMPLE_FMT_S16, 1);

            output_Buff = {frame->extended_data[0]};
        }

        if(output_Buff == NULL){
            break;
        }

        samples_byte_array = env->NewByteArray(dst_bufsize);
        if (samples_byte_array == NULL) {
          //  LOGV("Cannot Allocate byte array");
            break;
        }
        jbyte *jni_samples = env->GetByteArrayElements(samples_byte_array, NULL);
        //LOGV("Coping Data %d ",dst_bufsize);
        memcpy(jni_samples,output_Buff, dst_bufsize);
        env->SetByteArrayRegion(samples_byte_array, 0, dst_bufsize, jni_samples);
        //LOGV("Releasing jni_samples");
        env->CallVoidMethod(obj,methodId,samples_byte_array);
        env->ReleaseByteArrayElements(samples_byte_array, jni_samples, 0);

        size += dst_bufsize;
        av_frame_unref(frame);
        av_packet_unref(packet);
    }
    

    // Close the context and free all data associated to it, but not the context itself.
    avcodec_close(codecContext);

    // Free the context itself.
    avcodec_free_context(&codecContext);

    // We are done here. Close the input.
    avformat_close_input(&formatContext);

    if(swr_ctx !=NULL){
        swr_free(&swr_ctx);
    }

    return 0;

}

void _setUpSWR(AVFrame *pFrame) {

    if(swr_ctx == NULL){
        swr_ctx = swr_alloc();
    }
    //set options
    av_opt_set_int(swr_ctx, "in_channel_layout",    pFrame->channel_layout, 0);
    av_opt_set_int(swr_ctx, "in_sample_rate",       pFrame->sample_rate, 0);
    av_opt_set_sample_fmt(swr_ctx, "in_sample_fmt", static_cast<AVSampleFormat>(pFrame->format), 0);

    av_opt_set_int(swr_ctx, "out_channel_layout",    pFrame->channel_layout, 0);
    av_opt_set_int(swr_ctx, "out_sample_rate",       pFrame->sample_rate, 0);
    av_opt_set_sample_fmt(swr_ctx, "out_sample_fmt", AV_SAMPLE_FMT_S16, 0);

    swr_init(swr_ctx);
}

AVCodecContext* _getCodecContext(int streamIndex) {

    codec = avcodec_find_decoder(formatContext->streams[streamIndex]->codecpar->codec_id);
    if (codec == NULL) {
        printf("Error: invalid audio codec\n");
        avformat_close_input(&formatContext);
        return NULL;
    }

    // Initialize codec context for the decoder.
    AVCodecContext* codecContext = avcodec_alloc_context3(codec);
    if (codecContext == NULL) {
        // Something went wrong. Cleaning up...
        avformat_close_input(&formatContext);
        fprintf(stderr, "Could not allocate a decoding context.\n");
    }

    // Fill the codecCtx with the parameters of the codec used in the read file.
    if ((avcodec_parameters_to_context(codecContext, formatContext->streams[streamIndex]->codecpar)) != 0) {
        // Something went wrong. Cleaning up...
        avcodec_close(codecContext);
        avcodec_free_context(&codecContext);
        avformat_close_input(&formatContext);
        printf("Error setting codec context parameters.");
    }

    return codecContext;
}



int _getStreamInformation() {

    // find the first audio stream
    if(avformat_find_stream_info(formatContext, NULL) < 0){
        printf("Error: It wasn't possible to find stream information in the audio file.\n");
        return -3;
    }

    for(int i=0;i<formatContext->nb_streams;i++){

        if(formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO ){
            audioStreamIndex = i;
            break;
        }
    }

    if(audioStreamIndex == -1){
        printf("Error: There is no audio stream in the specified file.\n");
        return -4;
    }
    return 0;
}


