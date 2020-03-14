package com.example.ffmpegintregation;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {


    AudioPlayer audioPlayer = null;
    private  final  int STORAGE_PERMISSION_REQUEST_CODE = 8888;
    private Button chooser;
    private Button play;
    private TextView filePath;
    private boolean isPlayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chooser = findViewById(R.id.add);
        play = findViewById(R.id.play);
        filePath = findViewById(R.id.fileName);


        chooser.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                boolean permission = checkPermission();

                if(permission) {
                    loadAudioFile();
                }
            }
        });


        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlayed = !isPlayed;

                if(isPlayed){
                    chooser.setEnabled(false);

                    play.setText("Stop!");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                if(filePath.getText().toString().isEmpty()){
                                    throw new IOException();
                                }
                                audioPlayer = new AudioPlayer(filePath.getText().toString());
                                audioPlayer.start();

                            } catch (IOException e) {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Selected file is not an audio source\n Cann't be decoded!!", Toast.LENGTH_LONG).show();
                                        play.performClick();
                                        play.setPressed(true);
                                    }
                                });
                            }

                        }
                    }).start();
                }
                else{

                    if(audioPlayer!=null){
                        audioPlayer.stop();
                        audioPlayer.release();
                    }
                    chooser.setEnabled(true);
                    play.setText("Play!");
                }


            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        else if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            return false;
        }
        else {
            return true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case STORAGE_PERMISSION_REQUEST_CODE:
                Log.d("MainAc", "onRequestPermissionsResult: " + Arrays.toString(grantResults));
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "permission has been grunted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "[WARN] permission is not grunted.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void loadAudioFile() {
        FileChooser fileChooser = new FileChooser(MainActivity.this);

        fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                // ....do something with the file
                final String filename = file.getAbsolutePath();
                Log.i("File Name", filename);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        filePath.setText(filename);
                    }
                });

                // then actually do something in another module

            }
        });
        fileChooser.showDialog();
    }


}
