package io.github.jtmaher2.pianorepertoireapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import io.github.jtmaher2.pianorepertoireapp.data.DatabaseDescription;
import io.github.jtmaher2.pianorepertoireapp.data.PianoRepertoireDatabaseHelper;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
                                                                    AdapterView.OnItemSelectedListener {
    private static final int LOADER_TYPE_PIECE = 0;
    private static final int LOADER_TYPE_RECORDING = 1;
    private static final int BUFFER_SIZE = 8000;
    private static final int SAMPLE_RATE = 44100;
    // keys for storing a piece's/recording's Uri in a Bundle passed to the activity
    private static final String PIECE_URI = "piece_uri",
            RECORDING_URIS = "recording_uris",
            RECORDING_URI = "recording_uri",
            REMIX_URIS = "remix_uris";
    private EditText mNameTv, mComposerTv, mNotesTv;
    private RatingBar mRatingBar;
    private Uri pieceUri;

    private static final int PIECE_LOADER = 0; // identifies the Loader
    private static final String TAG = "DetailsActivity";

    private Spinner mRecsSpinner;
    private CustomAdapter mRecsSpinnerAdapter;
    private ArrayList<String> mRecsSpinnerElems,
                                mRemsSpinnerElems;
    private ArrayList<Uri> mRecordingUris,
            mRemixUris;
    private ArrayList<Float> mRecRatings;
    private ArrayList<Float> mRecFavs;
    private boolean mRatingChanged, mFavoriteChanged;
    private static final String EXISTING_PIECE_REMS = "existing_piece_rems",
                                EXISTING_PIECE_RECS = "existing_piece_recs",
                                EXISTING_PIECE_URI = "existing_piece_uri",
                                PIECE_ID = "piece_id";
    private static final String FOR_EXISTING = "for_existing";
    private Button mEditBtn, mPlayBtn;
    private ContentValues mUpdateValues;
    private CustomRatingBar mFavoriteStar;
    private String mSelectedItem;
    private int mPieceId;
    private AudioTrack mAudioTrack;
    private BufferedInputStream mBis;
    private FileInputStream mFin;
    private static final int ONE_SECOND = 1000;

    private final OnClickListener editBtnDoneListener = new OnClickListener(){
        @Override
        public void onClick(View view) {
            mNameTv.setEnabled(false);
            mComposerTv.setEnabled(false);
            mNotesTv.setEnabled(false);
            mEditBtn.setText(R.string.edit);
            mEditBtn.setBackgroundColor(ResourcesCompat.getColor(getResources(), android.R.color.holo_orange_light, null));
            mUpdateValues.clear();
            mUpdateValues.put(DatabaseDescription.Piece.COLUMN_NAME, mNameTv.getText().toString());
            mUpdateValues.put(DatabaseDescription.Piece.COLUMN_COMPOSER, mComposerTv.getText().toString());
            mUpdateValues.put(DatabaseDescription.Piece.COLUMN_NOTES, mNotesTv.getText().toString());
            getContentResolver().update(pieceUri, mUpdateValues, null, null);
            mEditBtn.setOnClickListener(editBtnClickListener);
        }
    };
    private final OnClickListener editBtnClickListener = new OnClickListener(){
        @Override
        public void onClick(View view) {
            mNameTv.setEnabled(true);
            mComposerTv.setEnabled(true);
            mNotesTv.setEnabled(true);
            mEditBtn.setText(R.string.done);
            mEditBtn.setBackgroundColor(Color.BLUE);
            mEditBtn.setTextColor(Color.WHITE);
            mEditBtn.setOnClickListener(editBtnDoneListener);
        }
    };

    private final OnClickListener remixBtnClickListener = new OnClickListener(){
      @Override
      public void onClick(View view) {
          Intent i = new Intent(getApplicationContext(), RemixActivityDetails.class);
          i.putParcelableArrayListExtra(EXISTING_PIECE_RECS, mRecordingUris);
          i.putExtra(PIECE_ID, mPieceId);
          i.putParcelableArrayListExtra(EXISTING_PIECE_REMS, mRemixUris);
          i.putExtra(EXISTING_PIECE_URI, pieceUri);
          startActivity(i);
      }
    };

    // stop playing a recording
    void stopPlaying()
    {
        //mAudioTrack.flush();
        //mAudioTrack.stop();
        //mAudioTrack.release();
        mAudioTrack.pause();
        mAudioTrack.flush();

        mPlayBtn.setText("Play");
        mPlayBtn.setOnClickListener((view) -> playRec());
    }

    Thread m_playThread;

    Runnable m_playGenerator = new Runnable()
    {
        public void run()
        {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            int i;

            try {
                while ((i = mDis.read(mByteData, 0, mBufSize)) > -1)
                    mAudioTrack.write(mByteData, 0, i);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            mAudioTrack.stop(); // stop after last buffer is played
            mAudioTrack.release();
            try {
                mDis.close();
                mBis.close();
                mFin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    mPlayBtn.setText("Play");
                    mPlayBtn.setOnClickListener((view) -> playRec());

                }
            });
        }
    };

    private int mBufSize;
    private byte[] mByteData;
    private DataInputStream mDis;

    // play a selected recording
    void playRec()
    {
        mPlayBtn.setText("Stop");
        mPlayBtn.setOnClickListener((view)-> stopPlaying());

        File file = null;
        file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + mRecsSpinnerElems.get(mRecsSpinner.getSelectedItemPosition()));

        // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"

        mAudioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(android.media.AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_8BIT))
                .build();

        mBufSize = (int) file.length();
        mByteData = new byte[mBufSize];

        //AudioTrack at = (AudioTrack)params[0];
        int i = 0;
        //DataInputStream dis = (DataInputStream)params[1];
        //byte[] byteData = (byte[])params[2];
        //int bufSize = (int)params[3];

        try {
            mFin = new FileInputStream( file );
            mBis = new BufferedInputStream(mFin, BUFFER_SIZE);
            mDis = new DataInputStream(mBis);

            mAudioTrack.play();

            m_playThread = new Thread(m_playGenerator);
            m_playThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(() -> mFavoriteStar.setEnabled(true));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Intent i = getIntent();
        mRatingChanged = false;
        mFavoriteChanged = false;
        mUpdateValues = new ContentValues();
        pieceUri = i.getParcelableExtra(PIECE_URI);
        // Uris of selected piece/recording
        mRecordingUris = i.getParcelableArrayListExtra(RECORDING_URIS);
        mRemixUris = i.getParcelableArrayListExtra(REMIX_URIS);
        mPieceId = i.getIntExtra(PIECE_ID, -1);
        Button newRecBtn = findViewById(R.id.new_rec_btn_details);
        mEditBtn = findViewById(R.id.edit_btn);
        Button remixBtn = findViewById(R.id.remix_btn);
        mPlayBtn = findViewById(R.id.play_btn);
        mPlayBtn.setOnClickListener((view)-> playRec());
        ConstraintLayout constraintLayout = findViewById(R.id.details_constraint_layout);
        AdapterView.OnItemSelectedListener onItemSelectedListener = this;
        Button delRecBtn = findViewById(R.id.del_rec_btn);
        delRecBtn.setOnClickListener(v -> {
            String remixName = mRecsSpinnerElems.get(mRecsSpinner.getSelectedItemPosition());
            File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + remixName);
            boolean deleted = file.delete();
            if (deleted) {
                int dbDeleted = getContentResolver().delete(DatabaseDescription.Recording.buildRecordingUriForRecWithName(new PianoRepertoireDatabaseHelper(getApplicationContext()).getReadableDatabase(), remixName), null, null);

                // remove deleted remix from spinner
                mRecsSpinnerElems.remove(remixName);
                Object[] elemsArray = mRecsSpinnerElems.toArray();
                if (elemsArray != null) {
                    mRecsSpinnerAdapter = new CustomAdapter(getApplicationContext(), Arrays.copyOf(elemsArray, elemsArray.length, String[].class));
                }
                mRecsSpinner.setAdapter(mRecsSpinnerAdapter);
                mRecsSpinner.setOnItemSelectedListener(onItemSelectedListener);

                if (dbDeleted > 0) {
                    Snackbar.make(constraintLayout,
                            R.string.recording_deleted, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(constraintLayout,
                            R.string.recording_not_deleted, Snackbar.LENGTH_LONG).show();
                }
            } else {
                Snackbar.make(constraintLayout,
                        R.string.recording_not_deleted, Snackbar.LENGTH_LONG).show();
            }
        });
        mNameTv = findViewById(R.id.detailsNameTextView);
        mComposerTv = findViewById(R.id.detailsComposerTextView);
        mNotesTv = findViewById(R.id.detailsNotesTextView);
        mRatingBar = findViewById(R.id.detailsRatingBar);
        mRatingBar.setOnRatingBarChangeListener((ratingBar, v, b) -> {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseDescription.Recording.COLUMN_RATING, v);

            Cursor c = getContentResolver().query(DatabaseDescription.Recording.CONTENT_URI, new String[]{DatabaseDescription.Recording._ID}, DatabaseDescription.Recording.COLUMN_FILE_NAME + " = '" + mRecsSpinnerElems.get(mRecsSpinner.getSelectedItemPosition()) + "'", null, null, null);

            if (c != null) {
                c.moveToFirst();
                int recId = c.getInt(0);
                getContentResolver().update(DatabaseDescription.Recording.CONTENT_URI,contentValues,DatabaseDescription.Recording._ID+"=?",new String[] {String.valueOf(recId)}); //id is the id of the row you wan to update
                c.close();
            }

            mRatingChanged = true;
        });
        mFavoriteStar = findViewById(R.id.detailsFavoriteStar);

        // timer to prevent favorite star from being clicked multiple times at once
        Timer mFavoriteStarTimer = new Timer(true);

        mFavoriteStar.setOnTouchListener((v, me) -> {
            if (!mFavoriteChanged) {
                ContentValues vals = new ContentValues();
                RatingBar rb = (RatingBar) v;
                float rating = rb.getRating();
                if (rating == 1.0f) {
                    vals.put(DatabaseDescription.Recording.COLUMN_FAVORITE, false);
                    rb.setRating(0.0f);
                } else {
                    vals.put(DatabaseDescription.Recording.COLUMN_FAVORITE, true);
                    rb.setRating(1.0f);
                }
                Cursor c = getContentResolver().query(DatabaseDescription.Recording.CONTENT_URI, new String[]{DatabaseDescription.Recording._ID}, DatabaseDescription.Recording.COLUMN_FILE_NAME + " = '" + mRecsSpinnerElems.get(mRecsSpinner.getSelectedItemPosition()) + "'", null, null, null);

                if (c != null) {
                    c.moveToFirst();
                    int recId = c.getInt(0);
                    getContentResolver().update(DatabaseDescription.Recording.CONTENT_URI, vals, DatabaseDescription.Recording._ID + "=?", new String[]{String.valueOf(recId)}); // get position of selected item
                    c.close();
                }

                mFavoriteChanged = true;
            }

            v.performClick();

            // prevent favorite star from being clicked multiple times at once
            mFavoriteStar.setEnabled(false); // remove the listener
            mFavoriteStarTimer.schedule(new MyTimerTask(), ONE_SECOND);

            return mFavoriteChanged;
        });
        mRecsSpinner = findViewById(R.id.recs_spinner);

        mRecRatings = new ArrayList<>();
        mRecFavs = new ArrayList<>();
        mRecsSpinnerElems = new ArrayList<>();
        mRemsSpinnerElems = new ArrayList<>();

        newRecBtn.setOnClickListener(view -> {
            Intent i1 = new Intent(getApplicationContext(), NewRecordingActivity.class);
            i1.putExtra(FOR_EXISTING, true);
            i1.putExtra(EXISTING_PIECE_URI, pieceUri);
            i1.putParcelableArrayListExtra(EXISTING_PIECE_RECS, mRecordingUris);
            i1.putExtra(PIECE_ID, mPieceId);
            i1.putParcelableArrayListExtra(EXISTING_PIECE_REMS, mRemixUris);
            startActivity(i1);
        });

        mEditBtn.setOnClickListener(editBtnClickListener);
        remixBtn.setOnClickListener(remixBtnClickListener);

        // load piece
        LoaderManager.getInstance(this).initLoader(LOADER_TYPE_PIECE, null, this);

        // load recordings
        for (int l = 0; l < mRecordingUris.size(); l++) {
            Bundle b = new Bundle();
            b.putParcelable(RECORDING_URI, mRecordingUris.get(l));
            LoaderManager.getInstance(this).initLoader(l + LOADER_TYPE_RECORDING, b, this);
        }
    }

    // called by LoaderManager to create a Loader
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // create an appropriate CursorLoader based on the id argument;
        // only one Loader in this fragment, so the switch is unnecessary
        CursorLoader cursorLoader;

        switch (id) {
            case PIECE_LOADER:
                cursorLoader = new CursorLoader(this,
                        pieceUri, // Uri of piece to display
                        null, // null projection returns all columns
                        null, // null selection returns all rows
                        null, // no selection arguments
                        null); // sort order

                break;
            default: // recording loader
                Uri recUri = args.getParcelable(RECORDING_URI);
                cursorLoader = new CursorLoader(this,
                        recUri == null ? new Uri.Builder().build() : recUri, // Uri of recording to display
                        null, // null projection returns all columns
                        null, // null selection returns all rows
                        null, // no selection arguments
                        null); // sort order

                break;
        }

        return cursorLoader;
    }

    // called by LoaderManager when loading completes
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            // get the column index for each data item
            if (loader.getId() == LOADER_TYPE_PIECE)
            {
                int nameIndex = data.getColumnIndex(DatabaseDescription.Piece.COLUMN_NAME);
                int composerIndex = data.getColumnIndex(DatabaseDescription.Piece.COLUMN_COMPOSER);
                int notesIndex = data.getColumnIndex(DatabaseDescription.Piece.COLUMN_NOTES);

                // fill TextViews with the retrieved data
                mNameTv.setText(data.getString(nameIndex));
                mComposerTv.setText(data.getString(composerIndex));
                mNotesTv.setText(data.getString(notesIndex));
            } else {
                String recOrRem = data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_REC_OR_REM));
                if (recOrRem.equals("rec")) { // LOADER_TYPE_REMIX
                    if (mSelectedItem == null || data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)).equals(mSelectedItem)) {
                        if (mRecsSpinnerElems.size() < mRecordingUris.size()) {
                            mRecsSpinnerElems.add(data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)));
                        }

                        if (mRecsSpinner.getAdapter() == null && mRecsSpinnerElems.size() == mRecordingUris.size()) {
                            // if this was the last spinner element to be added to the list,
                            // create adapter and add it to the spinner
                            Object[] elemsArray = mRecsSpinnerElems.toArray();
                            if (elemsArray != null)
                                mRecsSpinnerAdapter = new CustomAdapter(getApplicationContext(), Arrays.copyOf(elemsArray, elemsArray.length, String[].class));
                            mRecsSpinner.setAdapter(mRecsSpinnerAdapter);
                            mRecsSpinner.setOnItemSelectedListener(this);
                        }
                        int ratingIndex = data.getColumnIndex(DatabaseDescription.Recording.COLUMN_RATING);
                        int favoriteIndex = data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FAVORITE);

                        // fill TextViews with the retrieved data
                        if (mRatingChanged) { // rating was changed
                            float newRating = Float.parseFloat(data.getString(ratingIndex));
                            mRatingBar.setRating(newRating);
                            mRecRatings.set(mRecsSpinner.getSelectedItemPosition(), newRating);
                            mRatingChanged = false;
                        } else if (mFavoriteChanged) { // favorite was changed
                            float newFav = data.getFloat(favoriteIndex);
                            mFavoriteStar.setRating(newFav);
                            mRecFavs.set(mRecsSpinner.getSelectedItemPosition(), newFav);
                            mFavoriteChanged = false;
                        } else { // nothing was changed
                            if (mRecRatings.size() < mRecsSpinnerElems.size()) {
                                mRecRatings.add(data.getFloat(ratingIndex));
                            }
                            if (mRecFavs.size() < mRecsSpinnerElems.size()) {
                                mRecFavs.add(data.getFloat(favoriteIndex));
                            }
                        }
                    }
                } else { // LOADER_TYPE_REMIX
                    mRemsSpinnerElems.add(data.getString(data.getColumnIndex(DatabaseDescription.Remix.COLUMN_FILE_NAME)));
                }
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        mRatingBar.setRating(mRecRatings.get(pos));
        mFavoriteStar.setRating(mRecFavs.get(pos));
        mSelectedItem = mRecsSpinnerElems.get(pos);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), PieceListActivity.class));
    }
}
