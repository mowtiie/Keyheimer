package com.mowtiie.keyheimer.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mowtiie.keyheimer.util.IntervalConverter;

import java.util.ArrayList;
import java.util.List;

import static com.mowtiie.keyheimer.data.SecretContract.*;

public class SecretDao {

    private final SecretDbHelper dbHelper;

    public SecretDao(Context context) {
        this.dbHelper = SecretDbHelper.getInstance(context);
    }

    public boolean insert(Secret secret) {
        long now = System.currentTimeMillis();
        secret.setCreatedAt(now);
        secret.setUpdatedAt(now);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(TABLE_NAME, null, toContentValues(secret)) != -1L;
    }

    public void insertAll(List<Secret> secrets) {
        long now = System.currentTimeMillis();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Secret secret : secrets) {
                secret.setCreatedAt(now);
                secret.setUpdatedAt(now);
                db.insertOrThrow(TABLE_NAME, null, toContentValues(secret));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean update(Secret secret) {
        secret.setUpdatedAt(System.currentTimeMillis());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(
                TABLE_NAME,
                toContentValues(secret),
                COLUMN_ID + " = ?",
                new String[]{secret.getId()}
        ) > 0;
    }

    public List<Secret> repairIntervals() {
        List<Secret> repaired = new ArrayList<>();
        for (Secret secret : getAll()) {
            if (IntervalConverter.isValidInterval(secret.getIntervalValue(), secret.getIntervalUnit())) {
                continue;
            }
            secret.setIntervalValue(IntervalConverter.clampInterval(
                    secret.getIntervalValue(), secret.getIntervalUnit()));
            secret.setNextTriggerAt(IntervalConverter.computeNextTriggerAt(
                    secret.getIntervalValue(), secret.getIntervalUnit(),
                    secret.getReminderHour(), secret.getReminderMinute()));
            update(secret);
            repaired.add(secret);
        }
        return repaired;
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

    public void recordSuccess(String id, long newNextTriggerAt) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET "
                        + COLUMN_SUCCESS_COUNT + " = " + COLUMN_SUCCESS_COUNT + " + 1, "
                        + COLUMN_LAST_VERIFIED_AT + " = ?, "
                        + COLUMN_NEXT_TRIGGER_AT + " = ? WHERE " + COLUMN_ID + " = ?",
                new Object[]{System.currentTimeMillis(), newNextTriggerAt, id});
    }

    public void recordFailure(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET "
                        + COLUMN_FAIL_COUNT + " = " + COLUMN_FAIL_COUNT + " + 1 WHERE " + COLUMN_ID + " = ?",
                new Object[]{id});
    }

    public void updateCredentials(String id, byte[] salt, String hash, int iterations) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SALT, salt);
        values.put(COLUMN_HASH, hash);
        values.put(COLUMN_ITERATIONS, iterations);
        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{id});
    }

    public boolean restore(Secret secret) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(TABLE_NAME, null, toContentValues(secret)) != -1L;
    }

    public void updateNextTrigger(String id, long nextTriggerAt) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NEXT_TRIGGER_AT, nextTriggerAt);
        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{id});
    }

    public void activate(String id, long nextTriggerAt) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_ACTIVE, 1);
        values.put(COLUMN_NEXT_TRIGGER_AT, nextTriggerAt);
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
        values.put(COLUMN_CREATED_AT, secret.getCreatedAt());
        values.put(COLUMN_UPDATED_AT, secret.getUpdatedAt());
        values.put(COLUMN_REMINDER_HOUR, secret.getReminderHour());
        values.put(COLUMN_REMINDER_MINUTE, secret.getReminderMinute());
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
        secret.setIntervalUnit(Secret.IntervalUnit.valueOf(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTERVAL_UNIT))));
        secret.setNextTriggerAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NEXT_TRIGGER_AT)));

        int lastVerifiedIndex = cursor.getColumnIndexOrThrow(COLUMN_LAST_VERIFIED_AT);
        secret.setLastVerifiedAt(cursor.isNull(lastVerifiedIndex) ? null : cursor.getLong(lastVerifiedIndex));

        secret.setSuccessCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUCCESS_COUNT)));
        secret.setFailCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAIL_COUNT)));
        secret.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) != 0);
        secret.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        secret.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
        secret.setReminderHour(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_HOUR)));
        secret.setReminderMinute(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_MINUTE)));
        return secret;
    }
}