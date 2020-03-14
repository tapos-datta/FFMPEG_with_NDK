package com.example.ffmpegintregation;

import java.io.File;
import java.io.FileNotFoundException;

public class DecodeAudio{

    private String filepath = "";
    private int samplingRate = 0;
    private int channels = 0;
    private long durationMs = 0;
    private FrameListener frameListener = null;
    private int isInitSuccess = 0;


    public DecodeAudio(){ }

    public void setFrameListener(FrameListener frameListener) {
        this.frameListener = frameListener;
    }

    public void setFilepath(String filepath) throws FileNotFoundException {

        if(!(new File(filepath)).exists()){
            throw new FileNotFoundException(filepath);
        }

        this.filepath = filepath;
    }

    public int prepareFileToDecode(){

        if(filepath.isEmpty()){
            return -2;
        }
        int ret = prepare(filepath);

        isInitSuccess = ret;

        return ret;
    }


    public void start(){

        if(isInitSuccess==0){
            startDecoding();
        }

    }

    public void stop(){
        release();
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getChannels() {
        return channels;
    }

    protected void setAudioInfo(int samplingRate, int channels,long duration){
        this.samplingRate = samplingRate;
        this.channels = channels;
        this.durationMs = duration;
    }

    protected void setRawData(byte[] rawData) {

        if(frameListener!=null){
            frameListener.setFrameData( rawData!=null ? rawData.clone() : null);
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    private native int prepare(String filepath);

    private native void startDecoding();

    private native void release();

}
