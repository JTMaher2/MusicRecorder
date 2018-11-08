package io.github.jtmaher2.pianorepertoireapp;

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

    // determine if a recording with a specific name is the first recording for a piece with a particular ID
    private boolean isFirstRec(String rec) {
        ArrayList<Uri> allFirstRecs = DatabaseDescription.Recording.buildDistinctRecordingUris(new PianoRepertoireDatabaseHelper(getApplicationContext()).getReadableDatabase());

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
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        String[] allRecs = new File(
                Environment.getExternalStorageDirectory().getPath() +
                        "/PianoRepertoire/")
                .list();
        ArrayList<String> firstRecNames = new ArrayList<>();
        for (String rec : allRecs) {
            if (isFirstRec(rec)) {
                firstRecNames.add(rec);
            }
        }
        mRecyclerView.setAdapter(new MyPiecesRecyclerViewAdapter(
                Arrays.copyOf(firstRecNames.toArray(), firstRecNames.size(), String[].class), getApplicationContext()));

        FloatingActionButton fab = findViewById(R.id.new_rec_btn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), NewRecordingActivity.class));
            }
        });
    }
}
