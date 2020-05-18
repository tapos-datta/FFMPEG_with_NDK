package com.example.ffmpegintregation;

import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

public class VideoActivity extends AppCompatActivity {

    private GLSurfaceView surfaceView = null;
    SurfaceHolder holder;
    private Surface surface = null;
    private FrameRenderer renderer = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        surfaceView = findViewById(R.id.glView);
        surfaceView.setEGLContextClientVersion(2);
        renderer = new FrameRenderer(surfaceView);
        surfaceView.setRenderer(renderer);

        init();
    }

    private void init() {

        findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        initWithSurface(renderer.getSurface());
                        invoke();
                    }
                }).start();

            }
        });
    }

    static {
        System.loadLibrary("native-lib");
    }

    private native void invoke();

    private native void initWithSurface(Surface surface);
}
