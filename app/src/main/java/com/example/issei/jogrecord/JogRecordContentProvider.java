package com.example.issei.jogrecord;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

public class JogRecordContentProvider extends ContentProvider {
    private DatabaseHelper mDbHelper;

    private static final int JOGRECORD = 10;
    private static final int JOGRECORD_ID = 20;
    private static final String AUTHORITY = "com.example.issei.jogrecord.JogRecordContentProvider";

    private static final String BASE_PATH = "jogrecord";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static{
        uriMatcher.addURI(AUTHORITY, BASE_PATH, JOGRECORD);  //                                    1
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", JOGRECORD_ID);//                      2
    }
    @Override
    public boolean onCreate() {
        Log.v("aaaa","onCreate JogRecordContentProvider");
        mDbHelper = new DatabaseHelper(getContext());
        Log.v("aaaa","mDbHelper = " + mDbHelper);
        return false;
    }
    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder){ //                              3
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); //                             4

        queryBuilder.setTables(DatabaseHelper.TABLE_JOGRECORD);

        int uriType = uriMatcher.match(uri); //                                                    5
        Log.v("aaaa","uriType = " + uriType);
        switch(uriType) {
            case JOGRECORD:
                break;
            case JOGRECORD_ID:
                queryBuilder.appendWhere(DatabaseHelper.COLUMN_ID + "="
                        + uri.getLastPathSegment());  //                                               6
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Log.v("aaaa","db = " + db);
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder); //                                  7
        Log.v("aaaa","cursor = " + cursor);

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = mDbHelper.getWritableDatabase();
        long id = 0;
        switch(uriType){
            case JOGRECORD:
                id = sqlDB.insert(DatabaseHelper.TABLE_JOGRECORD,null, values); //   8
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.withAppendedPath(uri, String.valueOf(id));
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }
    @Nullable
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
