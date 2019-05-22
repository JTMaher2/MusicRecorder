package io.github.jtmaher2.pianorepertoireapp;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import io.github.jtmaher2.pianorepertoireapp.data.DatabaseDescription;

public class RemixActivity extends AppCompatActivity implements TimePickerFragment.OnTimeDialogListener {
    private static final String EXISTING_PIECE_RECS = "existing_piece_recs",
                                EXISTING_PIECE_REMS = "existing_piece_rems",
                                PIECE_ID = "piece_id",
                                EXISTING_PIECE_REC_NAMES = "existing_piece_rec_names",
                                EXISTING_PIECE_URI = "existing_piece_uri";

    private static final int NUM_SECS_IN_MIN = 60;
    private static final int NUM_MINS_IN_HOUR = 60;

    private static final double BYTES_PER_SEC = 43478.26086956523 * 2 * 2;
    private static final int BUFFERED_INPUT_STREAM_SIZE = 16384;
    private static final int BYTE_READ_LEN = 1000000;
    private static final String TAG = "RemixActivity";
    private String mCombinedName;
    long mPieceId;
    private ConstraintLayout mConstraintLayout;
    private ArrayList<Uri> mPieceRemixes;
    private Uri mPieceUri;

    RecyclerView mRecyclerView;
    private int[] mStartTimes, mEndTimes;

    // play a selected recording
    void playRec(String recName, String combinedName, int startSec, int endSec)
    {
        byte[] byteData = null,
                copy = null;
        File file = null;
        new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire/").mkdir(); // make dir for this piano repertoire app

        new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire/" + mPieceId + "/").mkdir(); // make dir for this piece

        file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + mPieceId + "/" + recName);

        try {
            if (!file.exists()) {
                boolean fileCreated = file.createNewFile();
                if (fileCreated) {
                    // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"

        /*AudioTrack at = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(android.media.AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_8BIT))
                .build();*/

                    int i = 0;
                    int bufSize = (int) file.length();

                    try
                    {
                        FileInputStream in = new FileInputStream( file );
                        BufferedInputStream bis = new BufferedInputStream(in, BUFFERED_INPUT_STREAM_SIZE);
                        DataInputStream dis = new DataInputStream(bis);
                        File fileOut = new File(Environment.getExternalStorageDirectory() + "/PianoRepertoire/" + mPieceId + "/" + combinedName + ".pcm");
                        FileOutputStream out = new FileOutputStream(fileOut, true);
                        BufferedOutputStream bos = new BufferedOutputStream(out);
                        DataOutputStream dos = new DataOutputStream(bos);
                        byteData = new byte[dis.available()];

                        int numSkipped = dis.skipBytes((int)Math.round(BYTES_PER_SEC * startSec)); // skip the first N seconds, where N is the start time for this rec
                        //at.play();
                        int numSecToPlay = endSec - startSec;
                        double recSecLen = bufSize / BYTES_PER_SEC; // recording total # of seconds
                        int bytesToPlay = (int)Math.round((numSecToPlay / recSecLen) * bufSize); // the number of bytes to read for the specified length
                        if (bytesToPlay > byteData.length)
                        {
                            bytesToPlay = byteData.length; // prevent out of bounds exception
                        }
                        int numBytesWritten = 0;
                        do {
                            int bytesToWrite;

                            if (numBytesWritten + BYTE_READ_LEN <= bytesToPlay)
                            {
                                bytesToWrite = BYTE_READ_LEN;
                            }
                            else
                            {
                                bytesToWrite = bytesToPlay - numBytesWritten;
                            }

                            i = dis.read(byteData, 0, bytesToWrite);
                            dos.write(byteData, 0, i);
                            numBytesWritten += bytesToWrite;
                        } while (i > -1 && numBytesWritten < bytesToPlay);

             /*{

                numBytesWritten++;
            }*/
                        //at.stop();
                        //at.release();
                        dos.close();
                        bos.close();
                        out.close();
                        dis.close();
                        bis.close();
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getStackTrace()[0].toString());
        }
    }

    // saves remix file name to the database
    private void saveRemixFileName(long pieceId) {
        // create ContentValues object containing remix's key-value pairs
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseDescription.Remix.COLUMN_PIECE_ID,
                pieceId);
        contentValues.put(DatabaseDescription.Remix.COLUMN_FILE_NAME,
                mCombinedName + ".pcm");
        contentValues.put(DatabaseDescription.Remix.COLUMN_RATING, 0);
        contentValues.put(DatabaseDescription.Remix.COLUMN_FAVORITE, false);
        contentValues.put(DatabaseDescription.Remix.COLUMN_REC_OR_REM, "rem"); // it's a remix

        // use Activity's ContentResolver to invoke
        // insert on the PianoRepertoireRecordingsContentProvider
        Uri newRemixUri = getContentResolver().insert(
                DatabaseDescription.Remix.CONTENT_URI, contentValues);

        mPieceRemixes.add(newRemixUri); // add to list of URIs

        if (newRemixUri != null) {
            Snackbar.make(mConstraintLayout,
                    R.string.remix_added, Snackbar.LENGTH_LONG).show();
        }
        else {
            Snackbar.make(mConstraintLayout,
                    R.string.remix_not_added, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remix);
        mRecyclerView = findViewById(R.id.start_end_times_recyclerview);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mConstraintLayout = findViewById(R.id.remix_constraint_layout);

        Intent callingIntent = getIntent();

        ArrayList<String> pieceRecordingNames = callingIntent.getStringArrayListExtra(EXISTING_PIECE_REC_NAMES);
        ArrayList<Uri> pieceRecordings = callingIntent.getParcelableArrayListExtra(EXISTING_PIECE_RECS);
        mPieceRemixes = callingIntent.getParcelableArrayListExtra(EXISTING_PIECE_REMS);
        mPieceUri = callingIntent.getParcelableExtra(EXISTING_PIECE_URI);
        int pieceId = callingIntent.getIntExtra(PIECE_ID, -1);
        mPieceId = pieceId;
        if (pieceRecordings != null) {
            int numPieces = pieceRecordings.size();
            mStartTimes = new int[numPieces];
            mEndTimes = new int[numPieces];

            Button combineButton = findViewById(R.id.combine_btn);
            combineButton.setOnClickListener((view) -> {
                mCombinedName = ((EditText) findViewById(R.id.combine_edittext)).getText().toString();
                // prevent saving if there is already remix with this name
                File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + mPieceId + "/" + mCombinedName + ".pcm");
                if (!file.exists()) {
                    // select each recording from the start time to the end time
                    for (int c = 0; c < mRecyclerView.getChildCount(); c++) {
                        View child = mRecyclerView.getChildAt(c);
                        String childName = ((TextView) child.findViewById(R.id.rec_start_label)).getText().toString();
                        childName = childName.substring(0, childName.indexOf(" "));
                        String childStartTime = ((TextView) child.findViewById(R.id.rec_start_time_textview)).getText().toString();
                        int startColonIdx = childStartTime.indexOf(":"),
                                secondStartColonIdx = childStartTime.lastIndexOf(":");
                        int startMin = Integer.parseInt(childStartTime.substring(startColonIdx + 1, secondStartColonIdx)),
                                startSec = Integer.parseInt(childStartTime.substring(secondStartColonIdx + 1));
                        startSec = startMin * NUM_SECS_IN_MIN + startSec;
                        String childEndTime = ((TextView) child.findViewById(R.id.rec_end_time_textview)).getText().toString();
                        int endColonIdx = childEndTime.indexOf(":"),
                                secondEndColonIdx = childEndTime.lastIndexOf(":");
                        int endMin = Integer.parseInt(childStartTime.substring(endColonIdx + 1, secondEndColonIdx)),
                                endSec = Integer.parseInt(childEndTime.substring(secondEndColonIdx + 1));
                        endSec = endMin * NUM_SECS_IN_MIN + endSec;

                        // load the recording
                        playRec(childName, mCombinedName, startSec, endSec);
                    }

                    saveRemixFileName(pieceId); // save to Remixes table

                    Intent allRemixesActivity = new Intent(getApplicationContext(), RemixActivityDetails.class); // go back
                    allRemixesActivity.putExtra(EXISTING_PIECE_RECS, pieceRecordings);
                    allRemixesActivity.putExtra(EXISTING_PIECE_REMS, mPieceRemixes);
                    allRemixesActivity.putExtra(PIECE_ID, pieceId);
                    allRemixesActivity.putExtra(EXISTING_PIECE_URI, mPieceUri);
                    startActivity(allRemixesActivity);
                } else {
                    Snackbar.make(mConstraintLayout,
                            "There is already a remix with this name.", Snackbar.LENGTH_LONG).show();
                }
            });

            mRecyclerView.setAdapter(new MyTimesRecyclerViewAdapter(
                    Arrays.copyOf(pieceRecordingNames.toArray(), numPieces, String[].class), pieceId,this, mStartTimes, mEndTimes));
        }
    }

    @Override
    public void onTimeSet(com.ikovac.timepickerwithseconds.TimePicker view, int hour, int minute, int second, String startOrEnd, int childIdx) {
        if (startOrEnd.equals("start"))
        {
            mStartTimes[childIdx] = second + minute * NUM_SECS_IN_MIN + hour * NUM_MINS_IN_HOUR * NUM_SECS_IN_MIN;
            ((TextView)mRecyclerView.getChildAt(childIdx).findViewById(R.id.rec_start_time_textview)).setText(getString(R.string.hr_min_sec, hour, minute, second));

        } else {
            mEndTimes[childIdx] = second + minute * NUM_SECS_IN_MIN + hour * NUM_MINS_IN_HOUR * NUM_SECS_IN_MIN;
            ((TextView)mRecyclerView.getChildAt(childIdx).findViewById(R.id.rec_end_time_textview)).setText(getString(R.string.hr_min_sec, hour, minute, second));
        }
    }
}
