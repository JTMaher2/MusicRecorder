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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import io.github.jtmaher2.pianorepertoireapp.data.DatabaseDescription;
import io.github.jtmaher2.pianorepertoireapp.data.PianoRepertoireContentProvider;
import io.github.jtmaher2.pianorepertoireapp.data.PianoRepertoireDatabaseHelper;

public class RemixActivityDetails extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemSelectedListener {
    private static final int LOADER_TYPE_PIECE = 0;
    private static final int LOADER_TYPE_REMIX = 1000;
    private static final int LOADER_TYPE_RECORDING = 2000;

    // keys for storing a piece's/recording's Uri in a Bundle passed to the activity
    private static final String PIECE_URI = "piece_uri",
            REMIX_URIS = "remix_uris",
            REMIX_URI = "REMIX_URI",
            REC_URI = "REC_URI",
            PIECE_ID = "piece_id",
            RECORDING_URIS = "recording_uris";
    private EditText mNameTv, mComposerTv, mNotesTv;
    private RatingBar mRatingBar;
    private Uri mPieceUri;

    private static final int PIECE_LOADER = 0, // identifies the Loader
        REC_LOADER = 1;

    private Spinner mRemixesSpinner;
    private CustomAdapter mRemixesSpinnerAdapter;
    private ArrayList<String> mRemixesSpinnerElems,
                                mRecordingsSpinnerElems;
    private ArrayList<Uri> mRemixUris,
                            mRecUris;
    private ArrayList<Float> mRemixRatings;
    private ArrayList<Float> mRemixFavs;
    private boolean mRatingChanged, mFavoriteChanged;
    private static final String EXISTING_PIECE_REMS = "existing_piece_rems",
            EXISTING_PIECE_URI = "existing_piece_uri",
            EXISTING_PIECE_RECS = "existing_piece_recs",
            EXISTING_PIECE_REC_NAMES = "existing_piece_rec_names";
    private static final String FOR_EXISTING = "for_existing";
    private ContentValues mUpdateValues;
    private CustomRatingBar mFavoriteStar;
    private String mSelectedItem;
    private int mPieceId;

    private final OnClickListener editBtnDoneListener = new OnClickListener(){
        @Override
        public void onClick(View view) {
            mNameTv.setEnabled(false);
            mComposerTv.setEnabled(false);
            mNotesTv.setEnabled(false);
            mUpdateValues.clear();
            mUpdateValues.put(DatabaseDescription.Piece.COLUMN_NAME, mNameTv.getText().toString());
            mUpdateValues.put(DatabaseDescription.Piece.COLUMN_COMPOSER, mComposerTv.getText().toString());
            mUpdateValues.put(DatabaseDescription.Piece.COLUMN_NOTES, mNotesTv.getText().toString());
            getContentResolver().update(mPieceUri, mUpdateValues, null, null);
        }
    };
    private final OnClickListener editBtnClickListener = new OnClickListener(){
        @Override
        public void onClick(View view) {
            mNameTv.setEnabled(true);
            mComposerTv.setEnabled(true);
            mNotesTv.setEnabled(true);
        }
    };

    private final OnClickListener remixBtnClickListener = new OnClickListener(){
        @Override
        public void onClick(View view) {
            Intent i = new Intent(getApplicationContext(), RemixActivity.class);
            i.putStringArrayListExtra(EXISTING_PIECE_REMS, mRemixesSpinnerElems);
            startActivity(i);
        }
    };

    // play a selected recording
    void playRec()
    {
        byte[] byteData = null;
        File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + mRemixesSpinnerElems.get(mRemixesSpinner.getSelectedItemPosition()));

        // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"

        AudioTrack at = new AudioTrack.Builder()
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
                .build();

        int i = 0;
        int bufSize = (int) file.length();
        byteData = new byte[bufSize];

        try
        {
            FileInputStream in = new FileInputStream( file );
            BufferedInputStream bis = new BufferedInputStream(in, 8000);
            DataInputStream dis = new DataInputStream(bis);

                /*while (dis.available() > 0)
                {
                    byteData[i] = dis.readByte();
                    i++;
                }*/
            at.play();
            while ((i = dis.read(byteData, 0, bufSize)) > -1)
                at.write(byteData, 0, i);
            at.stop();
            at.release();
            dis.close();
            bis.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        // go back to details activity
        Intent i = new Intent(getApplicationContext(), DetailsActivity.class);
        i.putExtra(RECORDING_URIS, mRecUris);
        i.putExtra(REMIX_URIS, mRemixUris);
        i.putExtra(PIECE_ID, mPieceId);
        i.putExtra(PIECE_URI, mPieceUri);
        startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remix_details);
        ConstraintLayout constraintLayout = findViewById(R.id.rem_det_constraint_layout);
        Intent i = getIntent();
        mRatingChanged = false;
        mFavoriteChanged = false;
        mUpdateValues = new ContentValues();
        // Uris of selected piece/recording
        mRemixUris = i.getParcelableArrayListExtra(EXISTING_PIECE_REMS);
        mRecUris = i.getParcelableArrayListExtra(EXISTING_PIECE_RECS);
        mPieceId = i.getIntExtra(PIECE_ID, -1);
        mPieceUri = i.getParcelableExtra(EXISTING_PIECE_URI);
        Button newRemixBtn = findViewById(R.id.new_rec_btn_details);
        Button playBtn = findViewById(R.id.play_btn);

        playBtn.setOnClickListener((view)-> playRec());

        AdapterView.OnItemSelectedListener onItemSelectedListener = this;

        Button delRemBtn = findViewById(R.id.delete_remix_btn);
        delRemBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String remixName = mRemixesSpinnerElems.get(mRemixesSpinner.getSelectedItemPosition());
                File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + remixName);
                boolean deleted = file.delete();
                if (deleted) {
                    int dbDeleted = getContentResolver().delete(DatabaseDescription.Remix.buildRemixUriForRemixWithName(new PianoRepertoireDatabaseHelper(getApplicationContext()).getReadableDatabase(), remixName), null, null);

                    // remove deleted remix from spinner
                    mRemixesSpinnerElems.remove(remixName);
                    Object[] elemsArray = mRemixesSpinnerElems.toArray();
                    if (elemsArray != null) {
                        mRemixesSpinnerAdapter = new CustomAdapter(getApplicationContext(), Arrays.copyOf(elemsArray, elemsArray.length, String[].class));
                    }
                    mRemixesSpinner.setAdapter(mRemixesSpinnerAdapter);
                    mRemixesSpinner.setOnItemSelectedListener(onItemSelectedListener);

                    if (dbDeleted > 0) {
                        Snackbar.make(constraintLayout,
                                R.string.remix_deleted, Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(constraintLayout,
                                R.string.remix_not_deleted, Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(constraintLayout,
                            R.string.remix_not_deleted, Snackbar.LENGTH_LONG).show();
                }
            }
        });
        mRatingBar = findViewById(R.id.detailsRatingBar);
        mRatingBar.setOnRatingBarChangeListener((ratingBar, v, b) -> {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseDescription.Recording.COLUMN_RATING, v);

            Cursor c = getContentResolver().query(DatabaseDescription.Recording.CONTENT_URI, new String[]{DatabaseDescription.Recording._ID}, DatabaseDescription.Recording.COLUMN_FILE_NAME + " = '" + mRemixesSpinnerElems.get(mRemixesSpinner.getSelectedItemPosition()) + "'", null, null, null);

            if (c != null) {
                c.moveToFirst();
                int recId = c.getInt(0);
                getContentResolver().update(DatabaseDescription.Recording.CONTENT_URI,contentValues,DatabaseDescription.Recording._ID+"=?",new String[] {String.valueOf(recId)}); //id is the id of the row you wan to update
                c.close();
            }

            mRatingChanged = true;
        });
        mFavoriteStar = findViewById(R.id.detailsFavoriteStar);

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
                Cursor c = getContentResolver().query(DatabaseDescription.Recording.CONTENT_URI, new String[]{DatabaseDescription.Recording._ID}, DatabaseDescription.Recording.COLUMN_FILE_NAME + " = '" + mRemixesSpinnerElems.get(mRemixesSpinner.getSelectedItemPosition()) + "'", null, null, null);

                if (c != null) {
                    c.moveToFirst();
                    int recId = c.getInt(0);
                    getContentResolver().update(DatabaseDescription.Recording.CONTENT_URI, vals, DatabaseDescription.Recording._ID + "=?", new String[]{String.valueOf(recId)}); // get position of selected item
                    c.close();
                }

                mFavoriteChanged = true;
            }

            v.performClick();

            return mFavoriteChanged;
        });
        mRemixesSpinner = findViewById(R.id.recs_spinner);

        mRemixRatings = new ArrayList<>();
        mRemixFavs = new ArrayList<>();
        mRemixesSpinnerElems = new ArrayList<>();

        newRemixBtn.setOnClickListener(view -> {
            Intent i1 = new Intent(getApplicationContext(), RemixActivity.class);
            i1.putExtra(PIECE_ID, mPieceId);
            i1.putStringArrayListExtra(EXISTING_PIECE_REC_NAMES, mRecordingsSpinnerElems);
            i1.putParcelableArrayListExtra(EXISTING_PIECE_RECS, mRecUris);
            i1.putParcelableArrayListExtra(EXISTING_PIECE_REMS, mRemixUris);
            i1.putExtra(EXISTING_PIECE_URI, mPieceUri);
            startActivity(i1);
        });

        LoaderManager lm = LoaderManager.getInstance(this);

        // attempt to load recordings
        if (mRecUris != null) {
            //LoaderManager.getInstance(this).initLoader(LOADER_TYPE_PIECE, null, this);

            for (int l = 0; l < mRecUris.size(); l++) {
                Bundle b = new Bundle();
                b.putParcelable(REC_URI, mRecUris.get(l));
                lm.initLoader(l + LOADER_TYPE_RECORDING, b, this);
            }
        }

        // attempt to load remixes
        if (mRemixUris != null) {
            //LoaderManager.getInstance(this).initLoader(LOADER_TYPE_PIECE, null, this);

            for (int l = 0; l < mRemixUris.size(); l++) {
                Bundle b = new Bundle();
                b.putParcelable(REMIX_URI, mRemixUris.get(l));
                lm.initLoader(l + LOADER_TYPE_REMIX, b, this);
            }
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
                        mPieceUri, // Uri of piece to display
                        null, // null projection returns all columns
                        null, // null selection returns all rows
                        null, // no selection arguments
                        null); // sort order

                break;
            default: // recording or remix loader
                if (args.get("REC_URI") != null) {
                    // it's a recording
                    Uri recUri = args.getParcelable(REC_URI);
                    cursorLoader = new CursorLoader(this,
                            recUri == null ? new Uri.Builder().build() : recUri, // Uri of recording to display
                            null, // null projection returns all columns
                            null, // null selection returns all rows
                            null, // no selection arguments
                            null); // sort order
                } else {
                    // it's a remix
                    Uri remUri = args.getParcelable(REMIX_URI);
                    cursorLoader = new CursorLoader(this,
                            remUri == null ? new Uri.Builder().build() : remUri, // Uri of remix to display
                            null, // null projection returns all columns
                            null, // null selection returns all rows
                            null, // no selection arguments
                            null); // sort order
                }

                break;
        }

        return cursorLoader;
    }

    // called by LoaderManager when loading completes
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            // get the column index for each data item
            int loaderId = loader.getId();
            if (loaderId == LOADER_TYPE_PIECE)
            {
                int nameIndex = data.getColumnIndex(DatabaseDescription.Piece.COLUMN_NAME);
                int composerIndex = data.getColumnIndex(DatabaseDescription.Piece.COLUMN_COMPOSER);
                int notesIndex = data.getColumnIndex(DatabaseDescription.Piece.COLUMN_NOTES);

                // fill TextViews with the retrieved data
                mNameTv.setText(data.getString(nameIndex));
                mComposerTv.setText(data.getString(composerIndex));
                mNotesTv.setText(data.getString(notesIndex));
            } else{
                String recOrRem = data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_REC_OR_REM));
                if (recOrRem.equals("rem")) { // LOADER_TYPE_REMIX
                    if (mSelectedItem == null || data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)).equals(mSelectedItem)) {
                        if (mRemixesSpinnerElems.size() < mRemixUris.size()) {
                            mRemixesSpinnerElems.add(data.getString(data.getColumnIndex(DatabaseDescription.Remix.COLUMN_FILE_NAME)));
                        }

                        if (mRemixesSpinner.getAdapter() == null && mRemixesSpinnerElems.size() == mRemixUris.size())
                        {
                            // if this was the last spinner element to be added to the list,
                            // create adapter and add it to the spinner
                            Object[] elemsArray = mRemixesSpinnerElems.toArray();
                            if (elemsArray != null)
                                mRemixesSpinnerAdapter = new CustomAdapter(getApplicationContext(), Arrays.copyOf(elemsArray, elemsArray.length, String[].class));
                            mRemixesSpinner.setAdapter(mRemixesSpinnerAdapter);
                            mRemixesSpinner.setOnItemSelectedListener(this);
                        }
                        int ratingIndex = data.getColumnIndex(DatabaseDescription.Recording.COLUMN_RATING);
                        int favoriteIndex = data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FAVORITE);

                        // fill TextViews with the retrieved data
                        if (mRatingChanged) { // rating was changed
                            float newRating = Float.parseFloat(data.getString(ratingIndex));
                            mRatingBar.setRating(newRating);
                            mRemixRatings.set(mRemixesSpinner.getSelectedItemPosition(), newRating);
                            mRatingChanged = false;
                        } else if (mFavoriteChanged) { // favorite was changed
                            float newFav = data.getFloat(favoriteIndex);
                            mFavoriteStar.setRating(newFav);
                            mRemixFavs.set(mRemixesSpinner.getSelectedItemPosition(), newFav);
                            mFavoriteChanged = false;
                        } else { // nothing was changed
                            if (mRemixRatings.size() < mRemixesSpinnerElems.size()) {
                                mRemixRatings.add(data.getFloat(ratingIndex));
                            }
                            if (mRemixFavs.size() < mRemixesSpinnerElems.size()) {
                                mRemixFavs.add(data.getFloat(favoriteIndex));
                            }
                        }
                    }
                } else { // LOADER_TYPE_RECORDING
                    if (mSelectedItem == null || data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)).equals(mSelectedItem)) {
                        if (mRecordingsSpinnerElems == null) {
                            mRecordingsSpinnerElems = new ArrayList<>();
                        }

                        if (mRecordingsSpinnerElems.size() < mRecUris.size()) {
                            mRecordingsSpinnerElems.add(data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        mRatingBar.setRating(mRemixRatings.get(pos));
        mFavoriteStar.setRating(mRemixFavs.get(pos));
        mSelectedItem = mRemixesSpinnerElems.get(pos);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}
