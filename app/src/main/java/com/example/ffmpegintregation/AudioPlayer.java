package com.example.ffmpegintregation;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.io.IOException;

public class AudioPlayer implements FrameListener {

    private DecodeAudio decoder;
    private AudioTrack audioTrack;
    public AudioPlayer(String filePath) throws IOException {

        init(filePath);

    }

    public void init(String filePath) throws IOException {
        decoder = new DecodeAudio();
        decoder.setFilepath(filePath);
        decoder.setFrameListener(this);
        int ret = decoder.prepareFileToDecode();

        if(ret==0){
            initAudioTrack();
        }else{
            throw new IOException();
        }
    }

    private void initAudioTrack() {

        int sampleRateInHz = decoder.getSamplingRate();
        int channelConfig = (decoder.getChannels() == 1) ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT) * 2,
                AudioTrack.MODE_STREAM
        );

    }

    public void start(){

        if(audioTrack!=null){
            audioTrack.play();
        }
        if(decoder!=null){
            decoder.start();
        }
    }

    public void release(){

        if(audioTrack!=null) {
            audioTrack.release();
        }
    }

    public void pause(){
        if(audioTrack!=null){
            audioTrack.pause();
        }
    }

    public void stop(){

        if(decoder!=null){
            decoder.stop();
        }
        if(audioTrack!=null) {
            audioTrack.stop();
        }

    }

    @Override
    public void setFrameData(byte[] rawData) {

        if(audioTrack!= null && rawData!=null){
            audioTrack.write(rawData,0,rawData.length);
        }else{
            decoder.stop();
        }
    }

}
