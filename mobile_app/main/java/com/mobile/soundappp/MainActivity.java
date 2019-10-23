package com.mobile.soundappp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;
    private int mRecorderBufferSize;
    private byte[] mAudioData;
    // audio argument
    private int mSampleRateInHZ = 8000; // rate
    private int mAudioFormat = AudioFormat.ENCODING_PCM_8BIT;  // format
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;   // channel

    private boolean isRecording = false;
    /**
     * records save
     */
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    private boolean runFlag;
    private NotificationUtils notificationUtils;
    //Handler UI modification
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String data = (String)msg.obj;
            Log.e("net", "get message" + msg.what);
            switch(msg.what){
                case 1:
                    // send notifications
                    notificationUtils.sendNotification("Answer Question", data);
                    break;
                case 2:
                    // open link
                    Uri uri = Uri.parse(data);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        notificationUtils  = new NotificationUtils(this);
        // start service
        runFlag = true;
        startLoop();
        initData();
        final Button button = (Button)findViewById(R.id.start_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRecording){
                    button.setText("Stop Recode");
                    onClickStart();
                }else{
                    button.setText("Start Recode");
                    onClickEnd();
                }
            }
        });
    }

    void startLoop(){
        new Thread(){
            @Override
            public void run() {
                //Time Controller
                while (runFlag) {
                    NetWorkHelper.getInstance().loopServer(MainActivity.this, handler);
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        runFlag = false;
    }

    private void initData() {
        mRecorderBufferSize = AudioRecord.getMinBufferSize(mSampleRateInHZ, mChannelConfig, mAudioFormat);
        mAudioData = new byte[320];
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mSampleRateInHZ, mChannelConfig, mAudioFormat, mRecorderBufferSize);
    }

    private void showToast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    private File tmpFile;

    void onClickStart() {
        showToast("start recoding");
        String tmpName = System.currentTimeMillis() + "_" + mSampleRateInHZ + "";
        tmpFile = createFile(tmpName + ".pcm");

        isRecording = true;
        mAudioRecord.startRecording();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream outputStream = new FileOutputStream(tmpFile.getAbsoluteFile());
                    while (isRecording) {
                        int readSize = mAudioRecord.read(mAudioData, 0, mAudioData.length);
                        outputStream.write(mAudioData);
                    }
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private File createFile(String name) {
        String dirPath = Environment.getExternalStorageDirectory().getPath() + "/AudioRecord/";
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String filePath = dirPath + name;
        File objFile = new File(filePath);
        if (!objFile.exists()) {
            try {
                objFile.createNewFile();
                return objFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    void onClickEnd() {
        isRecording = false;
        mAudioRecord.stop();
        try {
            NetWorkHelper.getInstance().upload(tmpFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
