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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.telecom.Call;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private Uri mPieceUri, mNewRecordingUri;
    private static final String EXISTING_PIECE_REMS = "existing_piece_rems",
            EXISTING_PIECE_RECS = "existing_piece_recs",
            EXISTING_PIECE_REC_NAMES = "existing_piece_rec_names",
            PIECE_ID = "piece_id";

    private static final String PIECE_URI = "piece_uri",
            REMIX_URIS = "remix_uris",
            REMIX_URI = "REMIX_URI",
            REC_URI = "REC_URI",
            RECORDING_URIS = "recording_uris";

    private ArrayList<Uri> mRemixUris,
            mRecUris;

    private int mPieceId;

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
    private boolean m_RecordingsDirCreated;
    private File m_RecordingsDir;

    private String mPieceName;
    private boolean mForExisting;
    private Uri mExistingPieceUri;

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
        mRemixUris = i.getParcelableArrayListExtra(EXISTING_PIECE_REMS);
        mRecUris = i.getParcelableArrayListExtra(EXISTING_PIECE_RECS);
        mPieceId = i.getIntExtra(PIECE_ID, -1);
        mPieceUri = i.getParcelableExtra(EXISTING_PIECE_URI);

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
                ActivityCompat.requestPermissions(NewRecordingActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
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
        startRecBtn.setOnClickListener(view -> {
            if (recorder != null) {
                recorder.startRecording();
                isRecording = true;
                recordingThread = new Thread(() -> {
                    //Looper.prepare();
                    mPieceName = pieceNameTv.getText().toString();
                    mForExisting = forExisting;
                    mExistingPieceUri = existingPieceUri;
                    NewRecordingActivity.this.writeAudioDataToFile();
                }, "AudioRecorder Thread");
                recordingThread.start();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        elapsedTime[0]++;
                        runOnUiThread(() -> {
                            date.setTime(elapsedTime[0] * 1000);
                            timerTv.setText(sdf.format(date));
                        });
                    }
                }, 0L, 1000L);
            }
        });

        Button stopRecBtn = findViewById(R.id.stop_recording_btn);
        stopRecBtn.setOnClickListener(view -> {
            // stops the recording activity
            if (null != recorder) {
                isRecording = false;
                recorder.stop();
                recorder.release();
                recorder = null;
                recordingThread = null;
                timer.cancel();

                if (mNewRecordingUri != null) {
                    if (forExisting) {
                        Snackbar recAddedSnackbar = Snackbar.make(constraintLayout,
                                R.string.recording_added, Snackbar.LENGTH_LONG);
                        recAddedSnackbar.addCallback(new Snackbar.Callback() {

                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                //see Snackbar.Callback docs for event details
                                // go back
                                if (forExisting) {
                                    // existing piece, go to this piece's details activity
                                    Intent details = new Intent(getApplicationContext(), DetailsActivity.class);
                                    mRecUris.add(mNewRecordingUri); // add new URI to list before passing it back
                                    details.putExtra(RECORDING_URIS, mRecUris);
                                    details.putExtra(REMIX_URIS, mRemixUris);
                                    details.putExtra(PIECE_ID, mPieceId);
                                    details.putExtra(PIECE_URI, mPieceUri);

                                    startActivity(details);
                                } else {
                                    // new piece, go to list
                                    startActivity(new Intent(getApplicationContext(), PieceListActivity.class));
                                }
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                            }
                        });
                        recAddedSnackbar.show();
                    } else {
                        Snackbar pieceAddedSnackbar = Snackbar.make(constraintLayout,
                                R.string.piece_added, Snackbar.LENGTH_LONG);
                        pieceAddedSnackbar.addCallback(new Snackbar.Callback() {

                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                //see Snackbar.Callback docs for event details
                                // go back

                                // new piece, go to list
                                startActivity(new Intent(getApplicationContext(), PieceListActivity.class));
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                            }
                        });
                        pieceAddedSnackbar.show();
                    }
                }
                else {
                    if (forExisting) {
                        Snackbar recNotAddedSnackbar = Snackbar.make(constraintLayout,
                                R.string.recording_not_added, Snackbar.LENGTH_LONG);
                        recNotAddedSnackbar.addCallback(new Snackbar.Callback() {

                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                //see Snackbar.Callback docs for event details
                                // go back
                                if (forExisting) {
                                    // existing piece, go to this piece's details activity
                                    Intent details = new Intent(getApplicationContext(), DetailsActivity.class);
                                    mRecUris.add(mNewRecordingUri); // add new URI to list before passing it back
                                    details.putExtra(RECORDING_URIS, mRecUris);
                                    details.putExtra(REMIX_URIS, mRemixUris);
                                    details.putExtra(PIECE_ID, mPieceId);
                                    details.putExtra(PIECE_URI, mPieceUri);

                                    startActivity(details);
                                } else {
                                    // new piece, go to list
                                    startActivity(new Intent(getApplicationContext(), PieceListActivity.class));
                                }
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                            }
                        });
                        recNotAddedSnackbar.show();
                    } else {
                        Snackbar pieceNotAddedSnackbar = Snackbar.make(constraintLayout,
                                R.string.piece_not_added, Snackbar.LENGTH_LONG);
                        pieceNotAddedSnackbar.addCallback(new Snackbar.Callback() {

                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                //see Snackbar.Callback docs for event details
                                // go back

                                // new piece, go to list
                                startActivity(new Intent(getApplicationContext(), PieceListActivity.class));
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                            }
                        });
                        pieceNotAddedSnackbar.show();
                    }
                }
            }
        });

        /*Button doneBtn = findViewById(R.id.done_btn);
        doneBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

            }
        });*/
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
                writePieceToExternalStorage();
                //m_RecordingsDirCreated = m_RecordingsDir.mkdirs();
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
            //listener.onAddEditCompleted(newPieceUri);
            String id = pieceUri.getLastPathSegment();
            if (id != null)
            {
                return Long.parseLong(id);
            }
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
        contentValues.put(DatabaseDescription.Remix.COLUMN_REC_OR_REM, "rec"); // it's a recording

        // use Activity's ContentResolver to invoke
        // insert on the PianoRepertoireRecordingsContentProvider
        mNewRecordingUri = getContentResolver().insert(
                DatabaseDescription.Recording.CONTENT_URI, contentValues);
    }

    private void writeAudioDataToFile() {
        // create a directory for this app, if it doesn't already exist
        m_RecordingsDir = new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire");

        if (m_RecordingsDir.isDirectory()) {
            //m_RecordingsDirCreated = true; // the "Piano Repertoire" directory has already been created
            writePieceToExternalStorage();
        } else {
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
                    ActivityCompat.requestPermissions(NewRecordingActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
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
                writePieceToExternalStorage();
            }
        }

        /*if (m_RecordingsDirCreated) {

        } else {
            Toast.makeText(getApplicationContext(), "Error creating recordings directory.", Toast.LENGTH_SHORT).show();
        }*/
    }

    private void writePieceToExternalStorage()
    {
        File dir = new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire");
        if (!dir.exists()) {
            boolean dirCreated = dir.mkdir();
            if (!dirCreated) {
                Snackbar.make(constraintLayout,
                        "error creating directory", Snackbar.LENGTH_LONG).show();
            }
        }

        // make a new file for this recording in the above directory
        filePath = Environment.getExternalStorageDirectory().getPath() + "/PianoRepertoire/" + mPieceName + ".pcm";

        PianoRepertoireDatabaseHelper prdh = new PianoRepertoireDatabaseHelper(getApplicationContext());
        SQLiteDatabase sqldb = SQLiteDatabase.create((sqLiteDatabase, sqLiteCursorDriver, s, sqLiteQuery) -> null);

        prdh.onCreate(sqldb); // Create the PIECES and RECORDINGS tables if they don't already exist
        long pieceId = savePiece(mForExisting, mExistingPieceUri);
        saveRecordingFileName(pieceId);

        short sData[] = new short[BufferElements2Rec];

        os = null;

        try {
            os = new FileOutputStream(filePath);
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
            if (os != null) {
                os.close();
            }
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
