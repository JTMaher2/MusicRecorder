package io.github.jtmaher2.pianorepertoireapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.jtmaher2.pianorepertoireapp.data.DatabaseDescription;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
                                                                    AdapterView.OnItemSelectedListener {
    private static final int LOADER_TYPE_PIECE = 0;
    private static final int LOADER_TYPE_RECORDING = 1;
    // keys for storing a piece's/recording's Uri in a Bundle passed to the activity
    private static final String PIECE_URI = "piece_uri",
            RECORDING_URIS = "recording_uris",
            RECORDING_URI = "recording_uri";
    private EditText mNameTv, mComposerTv, mNotesTv;
    private RatingBar mRatingBar;
    private Uri pieceUri;

    private static final int PIECE_LOADER = 0; // identifies the Loader

    private Spinner mRecsSpinner;
    private CustomAdapter mRecsSpinnerAdapter;
    private ArrayList<String> mRecsSpinnerElems;
    private ArrayList<Uri> mRecordingUris;
    private ArrayList<Float> mRecRatings;
    private ArrayList<Float> mRecFavs;
    private boolean mRatingChanged, mFavoriteChanged;
    private static final String EXISTING_PIECE_URI = "existing_piece_uri";
    private static final String FOR_EXISTING = "for_existing";
    private Button mEditBtn;
    private ContentValues mUpdateValues;
    private CustomRatingBar mFavoriteStar;
    private String mSelectedItem;

    private final View.OnClickListener editBtnDoneListener = new View.OnClickListener(){
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
    private final View.OnClickListener editBtnClickListener = new View.OnClickListener(){
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
        Button newRecBtn = findViewById(R.id.new_rec_btn_details);
        mEditBtn = findViewById(R.id.edit_btn);
        mNameTv = findViewById(R.id.detailsNameTextView);
        mComposerTv = findViewById(R.id.detailsComposerTextView);
        mNotesTv = findViewById(R.id.detailsNotesTextView);
        mRatingBar = findViewById(R.id.detailsRatingBar);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
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
            }
        });
        mFavoriteStar = findViewById(R.id.detailsFavoriteStar);

        mFavoriteStar.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent me) {
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

                return mFavoriteChanged;
            }

        });
        mRecsSpinner = findViewById(R.id.recs_spinner);

        mRecRatings = new ArrayList<>();
        mRecFavs = new ArrayList<>();
        mRecsSpinnerElems = new ArrayList<>();

        newRecBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), NewRecordingActivity.class);
                i.putExtra(FOR_EXISTING, true);
                i.putExtra(EXISTING_PIECE_URI, pieceUri);
                startActivity(i);
            }
        });

        mEditBtn.setOnClickListener(editBtnClickListener);

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
            } else { // LOADER_TYPE_RECORDING
                if (mSelectedItem == null || data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)).equals(mSelectedItem)) {
                    if (mRecsSpinnerElems.size() < mRecordingUris.size()) {
                        mRecsSpinnerElems.add(data.getString(data.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)));
                    }

                    if (mRecsSpinner.getAdapter() == null && mRecsSpinnerElems.size() == mRecordingUris.size())
                    {
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
}
