package com.mowtiie.keyheimer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SecretDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "keyheimer.db";
    private static final int DATABASE_VERSION = 1;

    private static volatile SecretDbHelper instance;

    private SecretDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static SecretDbHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (SecretDbHelper.class) {
                if (instance == null) {
                    instance = new SecretDbHelper(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SecretContract.CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SecretContract.DROP_TABLE_SQL);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}