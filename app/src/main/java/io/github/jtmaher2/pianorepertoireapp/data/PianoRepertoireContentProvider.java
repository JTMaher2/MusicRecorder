package io.github.jtmaher2.pianorepertoireapp.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

import io.github.jtmaher2.pianorepertoireapp.R;

public class PianoRepertoireContentProvider extends ContentProvider {
    // used to access the database
    private PianoRepertoireDatabaseHelper dbHelper;

    // UriMatcher helps ContentProvider determine operation to perform
    private static final UriMatcher uriMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);

    // constants used with UriMatcher to determine operation to perform
    private static final int ONE_PIECE = 1; // manipulate one piece
    private static final int PIECES = 2; // manipulate pieces table
    private static final int ONE_RECORDING = 3; // manipulate one recording
    private static final int RECORDINGS = 4; // manipulate recordings table

    // static block to configure this ContentProvider's UriMatcher
    static {
        // Uri for Piece with the specified id (#)
        uriMatcher.addURI(DatabaseDescription.AUTHORITY,
                DatabaseDescription.Piece.TABLE_NAME + "/#", ONE_PIECE);

        // Uri for Pieces table
        uriMatcher.addURI(DatabaseDescription.AUTHORITY,
                DatabaseDescription.Piece.TABLE_NAME, PIECES);

        // Uri for Recording with the specified id (#)
        uriMatcher.addURI(DatabaseDescription.AUTHORITY,
                DatabaseDescription.Recording.TABLE_NAME + "/#", ONE_RECORDING);

        // Uri for Recordings table
        uriMatcher.addURI(DatabaseDescription.AUTHORITY,
                DatabaseDescription.Recording.TABLE_NAME, RECORDINGS);
    }

    // called when the PianoRepertoireContentProvider is created
    @Override
    public boolean onCreate() {
        // create the PianoRepertoireDatabaseHelper
        dbHelper = new PianoRepertoireDatabaseHelper(getContext());
        return true; // ContentProvider successfully created
    }

    // required method: Not used in this app, so we return null
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    // query the pieces or recordings table in the database
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
                        String selection, String[] selectionArgs, String sortOrder) {

        // create SQLiteQueryBuilder for querying pieces or recordings table
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case ONE_PIECE: // piece with specified id will be selected
                queryBuilder.setTables(DatabaseDescription.Piece.TABLE_NAME);
                queryBuilder.appendWhere(
                        DatabaseDescription.Piece._ID + "=" + uri.getLastPathSegment());
                break;
            case PIECES: // all pieces will be selected
                queryBuilder.setTables(DatabaseDescription.Piece.TABLE_NAME);
                break;
            case ONE_RECORDING: // recording with specified id will be selected
                queryBuilder.setTables(DatabaseDescription.Recording.TABLE_NAME);
                queryBuilder.appendWhere(
                        DatabaseDescription.Recording._ID + "=" + uri.getLastPathSegment());
                break;
            case RECORDINGS: // all recordings will be selected
                queryBuilder.setTables(DatabaseDescription.Recording.TABLE_NAME);
                break;
            default:
                Context c = getContext();
                String error = "";
                if (c != null) {
                    error = c.getString(R.string.invalid_query_uri);
                }
                throw new UnsupportedOperationException(
                        error + uri);
        }

        // execute the query to select one or all pieces/recordings
        Cursor cursor = queryBuilder.query(dbHelper.getReadableDatabase(),
                projection, selection, selectionArgs, null, null, sortOrder);

        Context c = getContext();
        if (c != null){
            // configure to watch for content changes
            cursor.setNotificationUri(c.getContentResolver(), uri);
        }

        return cursor;
    }

    // insert a new piece/recording in the database
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri newUri;
        Context c = getContext();
        switch (uriMatcher.match(uri)) {
            case PIECES:
                // insert the new piece--success yields new piece's row id
                long rowId = dbHelper.getWritableDatabase().insert(
                        DatabaseDescription.Piece.TABLE_NAME, null, values);
                // if the piece was inserted, create an appropriate Uri;
                // otherwise, throw an exception
                if (rowId > 0) { // SQLite row IDs start at 1
                    newUri = DatabaseDescription.Piece.buildPieceUri(rowId);

                    if (c != null) {
                        // notify observers that the database changed
                        c.getContentResolver().notifyChange(uri, null);
                    }
                }
                else {
                    String error = "";
                    if (c != null) {
                        error = c.getString(R.string.insert_failed);
                    }
                    throw new SQLException(
                            error + uri);
                }
                break;
            case RECORDINGS:
                // insert the new recording--success yields new recording's row id
                rowId = dbHelper.getWritableDatabase().insert(
                        DatabaseDescription.Recording.TABLE_NAME, null, values);

                // if the recording was inserted, create an appropriate Uri;
                // otherwise, throw an exception
                if (rowId > 0) { // SQLite row IDs start at 1
                    newUri = DatabaseDescription.Recording.buildNewRecordingUri(rowId);

                    if (c != null) {
                        // notify observers that the database changed
                        c.getContentResolver().notifyChange(uri, null);
                    }
                }
                else {
                    String error = "";
                    if (c != null) {
                        error = c.getString(R.string.insert_failed);
                    }
                    throw new SQLException(
                            error + uri);
                }
                break;
            default:
                String error = "";
                if (c != null) {
                    error = c.getString(R.string.invalid_insert_uri);
                }
                throw new UnsupportedOperationException(
                        error + uri);
        }

        return newUri;
    }

    // update an existing piece/recording in the database
    @Override
    public int update(@NonNull Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {
        int numberOfRowsUpdated; // 1 if update successful; 0 otherwise

        switch (uriMatcher.match(uri)) {
            case ONE_PIECE:
                // get from the uri the id of piece to update
                String id = uri.getLastPathSegment();

                // update the piece
                numberOfRowsUpdated = dbHelper.getWritableDatabase().update(
                        DatabaseDescription.Piece.TABLE_NAME, values, DatabaseDescription.Piece._ID + "=" + id,
                        selectionArgs);
                break;
            case ONE_RECORDING:
                // update the recording
                numberOfRowsUpdated = dbHelper.getWritableDatabase().update(
                        DatabaseDescription.Recording.TABLE_NAME, values, DatabaseDescription.Recording._ID + "=?",
                        selectionArgs);
                break;
            case RECORDINGS:
                // insert the new recording--success yields new recording's row id
                numberOfRowsUpdated = dbHelper.getWritableDatabase().update(
                        DatabaseDescription.Recording.TABLE_NAME, values, DatabaseDescription.Recording._ID + "=?", selectionArgs);
                break;
            default:
                Context c = getContext();
                String error = "";
                if (c != null) {
                    error = c.getString(R.string.invalid_update_uri);
                }
                throw new UnsupportedOperationException(error + uri);
        }

        // if changes were made, notify observers that the database changed
        if (numberOfRowsUpdated != 0) {
            Context c = getContext();
            if (c != null) {
                c.getContentResolver().notifyChange(uri, null);
            }
        }

        return numberOfRowsUpdated;
    }

    // delete an existing piece/recording from the database
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int numberOfRowsDeleted;

        switch (uriMatcher.match(uri)) {
            case ONE_PIECE:
                // get from the uri the id of piece to update
                String id = uri.getLastPathSegment();

                // delete the piece
                numberOfRowsDeleted = dbHelper.getWritableDatabase().delete(
                        DatabaseDescription.Piece.TABLE_NAME, DatabaseDescription.Piece._ID + "=" + id, selectionArgs);
                break;
            case ONE_RECORDING:
                // get from the uri the id of recording to update
                id = uri.getLastPathSegment();

                // delete the recording
                numberOfRowsDeleted = dbHelper.getWritableDatabase().delete(
                        DatabaseDescription.Recording.TABLE_NAME, DatabaseDescription.Recording._ID + "=" + id, selectionArgs);
                break;
            default:
                Context c = getContext();
                String error = "";
                if (c != null) {
                    error = c.getString(R.string.invalid_delete_uri);
                }
                throw new UnsupportedOperationException(error + uri);
        }

        // notify observers that the database changed
        if (numberOfRowsDeleted != 0) {
            Context c = getContext();
            if (c != null) {
                c.getContentResolver().notifyChange(uri, null);
            }
        }

        return numberOfRowsDeleted;
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
