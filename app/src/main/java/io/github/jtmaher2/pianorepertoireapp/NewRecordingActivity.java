package io.github.jtmaher2.pianorepertoireapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.github.jtmaher2.pianorepertoireapp.data.DatabaseDescription;
import io.github.jtmaher2.pianorepertoireapp.data.PianoRepertoireDatabaseHelper;

public class NewRecordingActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private FileOutputStream os;
    private String filePath;

    private final int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private final int BytesPerElement = 2; // 2 bytes in 16bit format
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private AudioRecord recorder;
    private static Timer timer;
    private TextView timerTv, pieceNameTv, composerTv, notesTv;
    private ConstraintLayout constraintLayout;
    private static final String EXISTING_PIECE_URI = "existing_piece_uri";
    private static final String FOR_EXISTING = "for_existing";

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        return new Loader<>(getApplicationContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = getIntent();
        final boolean forExisting = i.getBooleanExtra(FOR_EXISTING, false);
        final Uri existingPieceUri = i.getParcelableExtra(EXISTING_PIECE_URI);
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(NewRecordingActivity.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(NewRecordingActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(), "The app needs permission to record audio in order to make recordings.", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(NewRecordingActivity.this,
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
        composerTv = findViewById(R.id.composer_textview);
        notesTv = findViewById(R.id.notes_textview);
        constraintLayout = findViewById(R.id.constraint_layout);

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
                            writeAudioDataToFile(pieceNameTv.getText().toString(), forExisting, existingPieceUri);
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

    // saves piece information to the database
    private long savePiece(boolean forExisting, Uri existingPieceUri) {
        Uri pieceUri;
        if (!forExisting) {
            // create ContentValues object containing new piece's key-value pairs
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseDescription.Piece.COLUMN_NAME,
                    pieceNameTv.getText().toString());
            contentValues.put(DatabaseDescription.Piece.COLUMN_COMPOSER,
                    composerTv.getText().toString());
            contentValues.put(DatabaseDescription.Piece.COLUMN_NOTES,
                    notesTv.getText().toString());

            // use Activity's ContentResolver to invoke
            // insert on the PianoRepertoirePiecesContentProvider
            pieceUri = getContentResolver().insert(
                    DatabaseDescription.Piece.CONTENT_URI, contentValues);
        } else {
            pieceUri = existingPieceUri;
        }

        if (pieceUri != null) {
            Snackbar.make(constraintLayout,
                    R.string.piece_added, Snackbar.LENGTH_LONG).show();
            //listener.onAddEditCompleted(newPieceUri);
            String id = pieceUri.getLastPathSegment();
            if (id != null)
            {
                return Long.parseLong(id);
            }
        }
        else {
            Snackbar.make(constraintLayout,
                    R.string.piece_not_added, Snackbar.LENGTH_LONG).show();
        }

        return -1;
    }

    // saves recording file name to the database
    private void saveRecordingFileName(long pieceId) {
        // create ContentValues object containing recording's key-value pairs
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseDescription.Recording.COLUMN_PIECE_ID,
                pieceId);
        contentValues.put(DatabaseDescription.Recording.COLUMN_FILE_NAME,
                pieceNameTv.getText().toString() + ".pcm");
        contentValues.put(DatabaseDescription.Recording.COLUMN_RATING, 0);
        contentValues.put(DatabaseDescription.Recording.COLUMN_FAVORITE, false);

        // use Activity's ContentResolver to invoke
        // insert on the PianoRepertoireRecordingsContentProvider
        Uri newRecordingUri = getContentResolver().insert(
                DatabaseDescription.Recording.CONTENT_URI, contentValues);

        if (newRecordingUri != null) {
            Snackbar.make(constraintLayout,
                    R.string.recording_added, Snackbar.LENGTH_LONG).show();
        }
        else {
            Snackbar.make(constraintLayout,
                    R.string.recording_not_added, Snackbar.LENGTH_LONG).show();
        }
    }

    private void writeAudioDataToFile(final String pieceName, boolean forExisting, Uri existingPieceId) {
        // create a directory for this app, if it doesn't already exist
        File dir = new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire");
        boolean dirExists;
        if (dir.isDirectory()) {
            dirExists = true;
        } else {
            dirExists = dir.mkdir();
        }
        if (dirExists) {
            // make a new file for this recording in the above directory
            filePath = Environment.getExternalStorageDirectory().getPath() + "/PianoRepertoire/" + pieceName + ".pcm";

            PianoRepertoireDatabaseHelper prdh = new PianoRepertoireDatabaseHelper(getApplicationContext());
            SQLiteDatabase sqldb = SQLiteDatabase.create(new SQLiteDatabase.CursorFactory() {
                @Override
                public Cursor newCursor(SQLiteDatabase sqLiteDatabase, SQLiteCursorDriver sqLiteCursorDriver, String s, SQLiteQuery sqLiteQuery) {
                    return null;
                }
            });

            prdh.onCreate(sqldb); // Create the PIECES and RECORDINGS tables if they don't already exist
            long pieceId = savePiece(forExisting, existingPieceId);
            saveRecordingFileName(pieceId);

            short sData[] = new short[BufferElements2Rec];

            os = null;

            try {
                // Here, thisActivity is the current activity
                if (ContextCompat.checkSelfPermission(NewRecordingActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Permission is not granted
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(NewRecordingActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        Toast.makeText(getApplicationContext(), "The app needs permission to record audio in order to make recordings.", Toast.LENGTH_SHORT).show();
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(NewRecordingActivity.this,
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
