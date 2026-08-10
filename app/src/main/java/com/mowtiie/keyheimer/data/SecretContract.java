package com.mowtiie.keyheimer.data;

public final class SecretContract {

    private SecretContract() { }

    public static final String TABLE_NAME = "secrets";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SALT = "salt";
    public static final String COLUMN_HASH = "hash";
    public static final String COLUMN_ITERATIONS = "iterations";
    public static final String COLUMN_HINT = "hint";
    public static final String COLUMN_INTERVAL_VALUE = "interval_value";
    public static final String COLUMN_INTERVAL_UNIT = "interval_unit";
    public static final String COLUMN_NEXT_TRIGGER_AT = "next_trigger_at";
    public static final String COLUMN_LAST_VERIFIED_AT = "last_verified_at";
    public static final String COLUMN_SUCCESS_COUNT = "success_count";
    public static final String COLUMN_FAIL_COUNT = "fail_count";
    public static final String COLUMN_IS_ACTIVE = "is_active";

    public static final String CREATE_TABLE_SQL =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_SALT + " BLOB NOT NULL, " +
                    COLUMN_HASH + " TEXT NOT NULL, " +
                    COLUMN_ITERATIONS + " INTEGER NOT NULL, " +
                    COLUMN_HINT + " TEXT, " +
                    COLUMN_INTERVAL_VALUE + " INTEGER NOT NULL, " +
                    COLUMN_INTERVAL_UNIT + " TEXT NOT NULL, " +
                    COLUMN_NEXT_TRIGGER_AT + " INTEGER NOT NULL, " +
                    COLUMN_LAST_VERIFIED_AT + " INTEGER, " +
                    COLUMN_SUCCESS_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_FAIL_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1" +
                    ");";

    public static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME;
}