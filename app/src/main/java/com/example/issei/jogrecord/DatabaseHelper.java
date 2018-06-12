package com.example.issei.jogrecord;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {                                          //①
    private static final String DBNAME = "jogrecord.db";
    private static final int DBVERSION = 2;
    public static final String TABLE_JOGRECORD = "jogrecord";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_ELAPSEDTIME = "eltime";
    public static final String COLUMN_DISTANCE = "distance";
    public static final String COLUMN_SPEED = "speed";
    public static final String COLUMN_ADDRESS = "address";
    private static final String CREATE_TABLE_SQL =
            "create table " + TABLE_JOGRECORD + " "
                    + "(" + COLUMN_ID + " integer primary key autoincrement,"
                    + COLUMN_DATE + " text not null,"
                    + COLUMN_ELAPSEDTIME + " text not null,"
                    + COLUMN_DISTANCE + " real not null,"
                    + COLUMN_SPEED + " real not null,"
                    + COLUMN_ADDRESS + " text not null)";         //教科書は　"text null)"

    public DatabaseHelper(Context context) {
        super(context, DBNAME, null, DBVERSION);                                       //　②
        Log.v("aaaa","databaseHelper");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {                                                 //  ③
        db.execSQL(CREATE_TABLE_SQL);
        Log.v("aaaa","onCreate(SQLiteDatabase)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.v("aaaa","onUpgrade(SQLiteDatabase)");

    }
}