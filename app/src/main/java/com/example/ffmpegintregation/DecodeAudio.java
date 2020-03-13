package com.example.ffmpegintregation;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.Arrays;

public class DecodeAudio implements JNIListener{

    private final String filepath;
    private byte[] rawData;
    private AudioTrack audioTrack;

    public DecodeAudio(String filepath){
        this.filepath = filepath;

        int sampleRateInHz = 44100;
        int channelCount = 2;
        int channelConfig = channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT) * 2,
                AudioTrack.MODE_STREAM);

        audioTrack.play();

        Log.d("Native", "DecodeAudio: " + decodeFile(filepath,this));
    }


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;

        Log.d("Decode", "setRawData length: " + rawData.length + " values " + Arrays.toString(rawData));
        if(rawData !=null){
            audioTrack.write(rawData,0,rawData.length);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native int decodeFile(String filepath, JNIListener listener);


    @Override
    public void setFrameData(byte[] rawData) {

    }

}
