package io.github.jtmaher2.pianorepertoireapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PianoRepertoireDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PianoRepertoire.db";
    private static final int DATABASE_VERSION = 1;

    // constructor
    public PianoRepertoireDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // creates the pieces and recordings tables when the database is created
    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL for creating the pieces table
        final String CREATE_PIECES_TABLE =
                "CREATE TABLE " + DatabaseDescription.Piece.TABLE_NAME + "(" +
                        DatabaseDescription.Piece._ID + " integer primary key, " +
                        DatabaseDescription.Piece.COLUMN_NAME + " TEXT, " +
                        DatabaseDescription.Piece.COLUMN_COMPOSER + " TEXT, " +
                        DatabaseDescription.Piece.COLUMN_NOTES + " TEXT);";
        db.execSQL(CREATE_PIECES_TABLE); // create the pieces table

        // SQL for creating the recordings table
        final String CREATE_RECORDINGS_TABLE =
                "CREATE TABLE " + DatabaseDescription.Recording.TABLE_NAME + "(" +
                        DatabaseDescription.Recording._ID + " integer primary key, " +
                        DatabaseDescription.Recording.COLUMN_PIECE_ID + " integer, " +
                        DatabaseDescription.Recording.COLUMN_FILE_NAME + " TEXT, " +
                        DatabaseDescription.Recording.COLUMN_RATING + " integer, " +
                        DatabaseDescription.Recording.COLUMN_FAVORITE + " integer);";
        db.execSQL(CREATE_RECORDINGS_TABLE); // create the recordings table
    }

    // normally defines how to upgrade the database when the schema changes
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) { }
}
