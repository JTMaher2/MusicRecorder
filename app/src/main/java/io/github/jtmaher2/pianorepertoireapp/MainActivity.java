package io.github.jtmaher2.pianorepertoireapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int COUNT = 524288;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private FileOutputStream os;
    private String filePath;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private AudioRecord recorder;
    private static Timer timer;
    private TextView timerTv, pieceNameTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(), "The app needs permission to record audio in order to make recordings.", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
        }

        timer = new Timer();

        timerTv = findViewById(R.id.record_timer);
        timerTv.setEnabled(false);

        pieceNameTv = findViewById(R.id.piece_name_textview);
        /*Button playBtn = findViewById(R.id.playbtn);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int intSize = android.media.AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);
                byte[] byteData = null;
                File file = null;
                file = new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire/" + pieceNameTv.getText().toString() + ".pcm");

                byteData = new byte[COUNT];

                FileInputStream in = null;
                try {
                    in = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                int bytesread = 0, ret = 0;
                long size = file.length();
                at.play();
                while (bytesread < size) {
                    if (in != null) {
                        try {
                            ret = in.read(byteData, 0, COUNT);
                            if (ret != -1) {
                                at.write(byteData, 0, ret);
                                bytesread += ret;
                            } else {
                                break;
                            }
                            in.close();
                            at.stop();
                            at.release();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });*/
        final int[] elapsedTime = new int[]{0};
        Button startRecBtn = findViewById(R.id.start_recording_btn);
        final SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", Locale.getDefault());
        final Date date = new Date();
        startRecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorder != null) {
                    recorder.startRecording();
                    isRecording = true;
                    recordingThread = new Thread(new Runnable() {
                        public void run() {
                            writeAudioDataToFile(pieceNameTv.getText().toString());
                        }
                    }, "AudioRecorder Thread");
                    recordingThread.start();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            elapsedTime[0]++;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    date.setTime(elapsedTime[0] * 1000);
                                    timerTv.setText(sdf.format(date));
                                }
                            });
                        }
                    }, 0L, 1000L);
                }
            }
        });

        Button stopRecBtn = findViewById(R.id.stop_recording_btn);
        stopRecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // stops the recording activity
                if (null != recorder) {
                    isRecording = false;
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                    recordingThread = null;
                    timer.cancel();
                }
            }
        });

        Button doneBtn = findViewById(R.id.done_btn);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), PieceListActivity.class));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
                break;
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                try {
                    os = new FileOutputStream(filePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void writeAudioDataToFile(final String pieceName) {
        new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire").mkdir();
        filePath = Environment.getExternalStorageDirectory().getPath() + "/PianoRepertoire/" + pieceName + ".pcm";
        short sData[] = new short[BufferElements2Rec];

        os = null;

        try {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Toast.makeText(getApplicationContext(), "The app needs permission to record audio in order to make recordings.", Toast.LENGTH_SHORT).show();
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                // Permission has already been granted
                os = new FileOutputStream(filePath);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            try {
                byte bData[] = short2byte(sData);
                assert os != null;
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            assert os != null;
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    /*protected static void startTimer() {
        isTimerRunning = true;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                elapsedTime += 1; //increase every sec
                mHandler.obtainMessage(1).sendToTarget();
            }
        }, 0, 1000);
    }*/

    /*public Handler mHandler = new Handler() {
        public void handleMessage(NotificationCompat.MessagingStyle.Message msg) {
            timerTv.setText(post(elapsedTime)); //this is the textview
        }
    };*/
}
