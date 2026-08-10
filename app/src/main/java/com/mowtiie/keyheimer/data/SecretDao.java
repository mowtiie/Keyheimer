package com.mowtiie.keyheimer.data;

import static com.mowtiie.keyheimer.data.SecretContract.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class SecretDao {

    private final SecretDbHelper dbHelper;

    public SecretDao(Context context) {
        this.dbHelper = SecretDbHelper.getInstance(context);
    }

    public void insert(Secret secret) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insert(TABLE_NAME, null, toContentValues(secret));
    }

    public void update(Secret secret) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(
                TABLE_NAME,
                toContentValues(secret),
                COLUMN_ID + " = ?",
                new String[]{secret.getId()}
        );
    }

    public void delete(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{id});
    }

    public Secret getById(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_NAME, null,
                COLUMN_ID + " = ?", new String[]{id},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        }
    }

    public List<Secret> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Secret> results = new ArrayList<>();
        try (Cursor cursor = db.query(
                TABLE_NAME, null,
                null, null,
                null, null,
                COLUMN_NAME + " COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                results.add(fromCursor(cursor));
            }
        }
        return results;
    }

    public List<Secret> getActiveDueBefore(long timestampMillis) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Secret> results = new ArrayList<>();
        String selection = COLUMN_IS_ACTIVE + " = 1 AND " + COLUMN_NEXT_TRIGGER_AT + " <= ?";
        try (Cursor cursor = db.query(
                TABLE_NAME, null,
                selection, new String[]{String.valueOf(timestampMillis)},
                null, null,
                COLUMN_NEXT_TRIGGER_AT + " ASC")) {
            while (cursor.moveToNext()) {
                results.add(fromCursor(cursor));
            }
        }
        return results;
    }

    public void updateVerificationResult(String id, boolean success, long newNextTriggerAt) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NEXT_TRIGGER_AT, newNextTriggerAt);
        if (success) {
            values.put(COLUMN_LAST_VERIFIED_AT, System.currentTimeMillis());
        }
        Secret current = getById(id);
        if (current != null) {
            values.put(COLUMN_SUCCESS_COUNT, current.getSuccessCount() + (success ? 1 : 0));
            values.put(COLUMN_FAIL_COUNT, current.getFailCount() + (success ? 0 : 1));
        }
        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{id});
    }

    public void setActive(String id, boolean active) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_ACTIVE, active ? 1 : 0);
        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{id});
    }

    private ContentValues toContentValues(Secret secret) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, secret.getId());
        values.put(COLUMN_NAME, secret.getName());
        values.put(COLUMN_SALT, secret.getSalt());
        values.put(COLUMN_HASH, secret.getHash());
        values.put(COLUMN_ITERATIONS, secret.getIterations());
        values.put(COLUMN_HINT, secret.getHint());
        values.put(COLUMN_INTERVAL_VALUE, secret.getIntervalValue());
        values.put(COLUMN_INTERVAL_UNIT, secret.getIntervalUnit().name());
        values.put(COLUMN_NEXT_TRIGGER_AT, secret.getNextTriggerAt());
        if (secret.getLastVerifiedAt() != null) {
            values.put(COLUMN_LAST_VERIFIED_AT, secret.getLastVerifiedAt());
        } else {
            values.putNull(COLUMN_LAST_VERIFIED_AT);
        }
        values.put(COLUMN_SUCCESS_COUNT, secret.getSuccessCount());
        values.put(COLUMN_FAIL_COUNT, secret.getFailCount());
        values.put(COLUMN_IS_ACTIVE, secret.isActive() ? 1 : 0);
        return values;
    }

    private Secret fromCursor(Cursor cursor) {
        Secret secret = new Secret();
        secret.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        secret.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
        secret.setSalt(cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_SALT)));
        secret.setHash(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HASH)));
        secret.setIterations(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITERATIONS)));
        secret.setHint(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HINT)));
        secret.setIntervalValue(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INTERVAL_VALUE)));
        secret.setIntervalUnit(Secret.IntervalUnit.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTERVAL_UNIT))));
        secret.setNextTriggerAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NEXT_TRIGGER_AT)));

        int lastVerifiedIndex = cursor.getColumnIndexOrThrow(COLUMN_LAST_VERIFIED_AT);
        secret.setLastVerifiedAt(cursor.isNull(lastVerifiedIndex) ? null : cursor.getLong(lastVerifiedIndex));

        secret.setSuccessCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUCCESS_COUNT)));
        secret.setFailCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAIL_COUNT)));
        secret.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) != 0);
        return secret;
    }
}