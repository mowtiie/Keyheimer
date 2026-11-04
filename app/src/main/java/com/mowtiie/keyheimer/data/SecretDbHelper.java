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
        for (int version = oldVersion; version < newVersion; version++) {
            migrate(db, version);
        }
    }

    // When DATABASE_VERSION is bumped, add `case <old version>:` running the ALTER TABLE
    // statements that take the schema to the next version, and update SecretContract.CREATE_TABLE_SQL.
    private void migrate(SQLiteDatabase db, int fromVersion) {
        switch (fromVersion) {
            default:
                throw new IllegalStateException("No migration from database version " + fromVersion);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}