package com.example.ffmpegintregation;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaExtractor;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {


    DecodeAudio decodeAudio = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String path = "/storage/emulated/0/SampleAudios/perception.mp3";
        File file = new File(path);

        decodeAudio = new DecodeAudio(path);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
//        if(extractor.getTrackCount() == 1) {
//            tv.setText(path);
//        }
    }


}
