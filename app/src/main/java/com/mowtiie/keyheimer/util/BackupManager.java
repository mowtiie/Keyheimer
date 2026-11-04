package com.mowtiie.keyheimer.util;

import java.util.Base64;

import com.mowtiie.keyheimer.data.Secret;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BackupManager {

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_IMPORT_CHARS = 5_000_000;
    private static final int MAX_IMPORT_SECRETS = 1000;
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_HINT_LENGTH = 1000;
    private static final int MIN_SALT_BYTES = 8;
    private static final int MAX_SALT_BYTES = 64;
    private static final int MIN_ITERATIONS = 10_000;
    private static final int MAX_ITERATIONS = 2_000_000;
    private static final int HASH_BYTES = 32;

    private BackupManager() {
    }

    public static void exportToJson(List<Secret> secrets, OutputStream outputStream)
            throws JSONException, IOException {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("exportedAt", System.currentTimeMillis());

        JSONArray secretsArray = new JSONArray();
        for (Secret secret : secrets) {
            secretsArray.put(toJson(secret));
        }
        root.put("secrets", secretsArray);

        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(root.toString(2));
        }
    }

    public static List<Secret> importFromJson(InputStream inputStream) throws JSONException, IOException {
        StringBuilder builder = new StringBuilder();
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
                if (builder.length() > MAX_IMPORT_CHARS) {
                    throw new IOException("Backup file is too large");
                }
            }
        }

        JSONObject root = new JSONObject(builder.toString());
        int schemaVersion = root.optInt("schemaVersion", 1);
        if (schemaVersion < 1 || schemaVersion > SCHEMA_VERSION) {
            throw new JSONException("Unsupported schema version: " + schemaVersion);
        }
        JSONArray secretsArray = root.getJSONArray("secrets");
        if (secretsArray.length() > MAX_IMPORT_SECRETS) {
            throw new JSONException("Too many secrets");
        }

        List<Secret> imported = new ArrayList<>();
        for (int i = 0; i < secretsArray.length(); i++) {
            imported.add(fromJson(secretsArray.getJSONObject(i)));
        }
        return imported;
    }

    private static JSONObject toJson(Secret secret) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("name", secret.getName());
        object.put("hint", secret.getHint() == null ? JSONObject.NULL : secret.getHint());
        object.put("salt", Base64.getEncoder().encodeToString(secret.getSalt()));
        object.put("hash", secret.getHash());
        object.put("iterations", secret.getIterations());
        object.put("intervalValue", secret.getIntervalValue());
        object.put("intervalUnit", secret.getIntervalUnit().name());
        object.put("reminderHour", secret.getReminderHour());
        object.put("reminderMinute", secret.getReminderMinute());
        object.put("nextTriggerAt", secret.getNextTriggerAt());
        object.put("lastVerifiedAt", secret.getLastVerifiedAt() == null ? JSONObject.NULL : secret.getLastVerifiedAt());
        object.put("successCount", secret.getSuccessCount());
        object.put("failCount", secret.getFailCount());
        object.put("active", secret.isActive());
        return object;
    }

    private static Secret fromJson(JSONObject object) throws JSONException {
        Secret secret = new Secret();
        secret.setId(UUID.randomUUID().toString());
        secret.setName(object.getString("name"));
        secret.setHint(object.isNull("hint") ? null : object.getString("hint"));
        secret.setSalt(Base64.getDecoder().decode(object.getString("salt")));
        secret.setHash(object.getString("hash"));
        secret.setIterations(object.getInt("iterations"));
        secret.setIntervalValue(object.getInt("intervalValue"));
        secret.setIntervalUnit(Secret.IntervalUnit.valueOf(object.getString("intervalUnit")));
        secret.setReminderHour(object.getInt("reminderHour"));
        secret.setReminderMinute(object.getInt("reminderMinute"));
        secret.setNextTriggerAt(object.getLong("nextTriggerAt"));
        secret.setLastVerifiedAt(object.isNull("lastVerifiedAt") ? null : object.getLong("lastVerifiedAt"));
        secret.setSuccessCount(object.optInt("successCount", 0));
        secret.setFailCount(object.optInt("failCount", 0));
        secret.setActive(object.getBoolean("active"));
        validate(secret);
        return secret;
    }

    private static void validate(Secret secret) throws JSONException {
        String name = secret.getName().trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new JSONException("Invalid name");
        }
        secret.setName(name);
        if (secret.getHint() != null && secret.getHint().length() > MAX_HINT_LENGTH) {
            throw new JSONException("Invalid hint");
        }
        if (secret.getSalt().length < MIN_SALT_BYTES || secret.getSalt().length > MAX_SALT_BYTES) {
            throw new JSONException("Invalid salt");
        }
        if (secret.getIterations() < MIN_ITERATIONS || secret.getIterations() > MAX_ITERATIONS) {
            throw new JSONException("Invalid iteration count");
        }
        try {
            if (Base64.getDecoder().decode(secret.getHash()).length != HASH_BYTES) {
                throw new JSONException("Invalid hash");
            }
        } catch (IllegalArgumentException e) {
            throw new JSONException("Invalid hash");
        }
        if (!IntervalConverter.isValidInterval(secret.getIntervalValue(), secret.getIntervalUnit())) {
            throw new JSONException("Invalid interval");
        }
        if (secret.getReminderHour() < 0 || secret.getReminderHour() > 23
                || secret.getReminderMinute() < 0 || secret.getReminderMinute() > 59) {
            throw new JSONException("Invalid reminder time");
        }
        if (secret.getSuccessCount() < 0 || secret.getFailCount() < 0) {
            throw new JSONException("Invalid counters");
        }
    }
}