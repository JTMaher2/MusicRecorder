package io.github.jtmaher2.pianorepertoireapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import io.github.jtmaher2.pianorepertoireapp.data.DatabaseDescription;
import io.github.jtmaher2.pianorepertoireapp.data.PianoRepertoireDatabaseHelper;

public class PieceListActivity extends AppCompatActivity {
    private PianoRepertoireDatabaseHelper mDbHelper;
    // determine if a recording with a specific name is the first recording for a piece with a particular ID
    private boolean isFirstRec(String rec) {
        ArrayList<Uri> allFirstRecs = DatabaseDescription.Recording.buildDistinctRecordingUris(mDbHelper.getReadableDatabase());

        for (Uri recUri : allFirstRecs) {
            // query URI for file name
            Cursor c = getContentResolver().query(recUri,new String[]{DatabaseDescription.Recording.COLUMN_FILE_NAME},null,null, null); //id is the id of the row you wan to update

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                if (c.getString(c.getColumnIndex(DatabaseDescription.Recording.COLUMN_FILE_NAME)).equals(rec))
                {
                    c.close();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piece_list);
        RecyclerView mRecyclerView = findViewById(R.id.my_recycler_view);
        mDbHelper = new PianoRepertoireDatabaseHelper(this);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        String[] allRecs = new File(
                Environment.getExternalStorageDirectory().getPath() +
                        "/PianoRepertoire/")
                .list();
        /*ArrayList<String> firstRecNames = new ArrayList<>();
        if (allRecs != null) {
            for (String rec : allRecs) {
                if (isFirstRec(rec)) {
                    firstRecNames.add(rec);
                }
            }
        }*/
        ArrayList<Uri> pieceUris = DatabaseDescription.Piece.buildPieceUris(mDbHelper.getReadableDatabase());
        ArrayList<String> pieceNames = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Cursor c;
        for (int i = 0; i < pieceUris.size(); i++) {
            c = contentResolver.query(pieceUris.get(i), new String[]{DatabaseDescription.Piece.COLUMN_NAME}, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                pieceNames.add(c.getString(c.getColumnIndex(DatabaseDescription.Piece.COLUMN_NAME)));
            }
            if (c != null) {
                c.close();
            }
        }


        mRecyclerView.setAdapter(new MyPiecesRecyclerViewAdapter(
                Arrays.copyOf(/*firstRec*/pieceNames.toArray(), /*firstRec*/pieceNames.size(), String[].class), getApplicationContext()));

        FloatingActionButton fab = findViewById(R.id.new_rec_btn);
        fab.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), NewRecordingActivity.class)));
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }
}
