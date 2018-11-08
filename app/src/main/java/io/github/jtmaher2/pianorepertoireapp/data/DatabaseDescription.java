package io.github.jtmaher2.pianorepertoireapp.data;

import android.content.ContentUris;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class DatabaseDescription {
    // ContentProvider's name: typically the package name
    static final String AUTHORITY =
            "io.github.jtmaher2.pianorepertoireapp.data";

    // base URI used to interact with the ContentProvider
    private static final Uri BASE_CONTENT_URI =
            Uri.parse("content://" + AUTHORITY);

    // nested class defines contents of the pieces table
    public static final class Piece implements BaseColumns {
        static final String TABLE_NAME = "pieces"; // table's name

        // Uri for the pieces table
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();

        // column names for pieces table's columns
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_COMPOSER = "composer";
        public static final String COLUMN_NOTES = "notes";

        // creates a Uri for a specific piece
        public static Uri buildPieceUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    // nested class defines contents of the recordings table-
    public static final class Recording implements BaseColumns {
        static final String TABLE_NAME = "recordings"; // table's name

        // Uri for the recordings table
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();

        // column names for recordings table's columns
        public static final String COLUMN_PIECE_ID = "piece_id";
        public static final String COLUMN_FILE_NAME = "file_name";
        public static final String COLUMN_RATING = "rating";
        public static final String COLUMN_FAVORITE = "favorite";

        // creates a Uri for a specific recording
        static Uri buildNewRecordingUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        // creates a Uri for a specific recording
        public static ArrayList<Uri> buildRecordingUris(SQLiteDatabase db, long id) {
            // find IDs of all recordings that belong to a piece
            Cursor recs = db.query(DatabaseDescription.Recording.TABLE_NAME, new String[]{Recording._ID}, Recording.COLUMN_PIECE_ID + " = " + id, null, null, null, null, null); // select recording(s)
            List<Integer> ids = new ArrayList<>();
            while (recs.moveToNext()) {
                ids.add(recs.getInt(0));
            }
            recs.close();
            ArrayList<Uri> uris = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                uris.add(ContentUris.withAppendedId(CONTENT_URI, ids.get(i)));
            }
            return uris;
        }

        // creates Uris for all recordings that are the first recording for a particular piece
        public static ArrayList<Uri> buildDistinctRecordingUris(SQLiteDatabase db) {
            Cursor recs = db.query(DatabaseDescription.Recording.TABLE_NAME, new String[]{Recording._ID}, DatabaseDescription.Recording._ID + " IN (SELECT DISTINCT " + DatabaseDescription.Recording.COLUMN_PIECE_ID +
                    " FROM " + DatabaseDescription.Recording.TABLE_NAME + ")", null, null, null, null, null); // select recording(s)
            List<Integer> ids = new ArrayList<>();
            while (recs.moveToNext()) {
                ids.add(recs.getInt(0));
            }
            recs.close();
            ArrayList<Uri> uris = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                uris.add(ContentUris.withAppendedId(CONTENT_URI, ids.get(i)));
            }
            return uris;
        }
    }
}

/* *************************************************************************
 * (C) Copyright 1992-2016 by Deitel & Associates, Inc. and               *
 * Pearson Education, Inc. All Rights Reserved.                           *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 **************************************************************************/