package com.mowtiie.keyheimer.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.mowtiie.keyheimer.data.Secret;

import org.json.JSONException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class BackupManagerTest {

    private static final int ITERATIONS = 10_000;

    private static Secret secret() {
        byte[] salt = HashUtil.generateSalt();
        Secret secret = new Secret();
        secret.setId("original-id");
        secret.setName("Vault master");
        secret.setHint("the first pet");
        secret.setSalt(salt);
        secret.setIterations(ITERATIONS);
        secret.setHash(HashUtil.hash("passphrase".toCharArray(), salt, ITERATIONS));
        secret.setIntervalValue(2);
        secret.setIntervalUnit(Secret.IntervalUnit.WEEK);
        secret.setReminderHour(7);
        secret.setReminderMinute(30);
        secret.setNextTriggerAt(1_800_000_000_000L);
        secret.setLastVerifiedAt(1_700_000_000_000L);
        secret.setSuccessCount(4);
        secret.setFailCount(1);
        secret.setActive(true);
        return secret;
    }

    private static byte[] export(Secret secret) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BackupManager.exportToJson(Collections.singletonList(secret), out);
        return out.toByteArray();
    }

    private static List<Secret> importFrom(byte[] json) throws Exception {
        return BackupManager.importFromJson(new ByteArrayInputStream(json));
    }

    private static List<Secret> importFrom(String json) throws Exception {
        return importFrom(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void roundTrip_preservesFieldsAndAssignsAFreshId() throws Exception {
        Secret original = secret();

        Secret imported = importFrom(export(original)).get(0);

        assertNotEquals(original.getId(), imported.getId());
        assertEquals(original.getName(), imported.getName());
        assertEquals(original.getHint(), imported.getHint());
        assertArrayEquals(original.getSalt(), imported.getSalt());
        assertEquals(original.getHash(), imported.getHash());
        assertEquals(original.getIterations(), imported.getIterations());
        assertEquals(original.getIntervalValue(), imported.getIntervalValue());
        assertEquals(original.getIntervalUnit(), imported.getIntervalUnit());
        assertEquals(original.getReminderHour(), imported.getReminderHour());
        assertEquals(original.getReminderMinute(), imported.getReminderMinute());
        assertEquals(original.getNextTriggerAt(), imported.getNextTriggerAt());
        assertEquals(original.getLastVerifiedAt(), imported.getLastVerifiedAt());
        assertEquals(original.getSuccessCount(), imported.getSuccessCount());
        assertEquals(original.getFailCount(), imported.getFailCount());
        assertTrue(imported.isActive());
    }

    @Test
    public void roundTrip_keepsNullHintAndLastVerified() throws Exception {
        Secret original = secret();
        original.setHint(null);
        original.setLastVerifiedAt(null);

        Secret imported = importFrom(export(original)).get(0);

        assertNull(imported.getHint());
        assertNull(imported.getLastVerifiedAt());
    }

    @Test
    public void importedPassphraseStillVerifies() throws Exception {
        Secret imported = importFrom(export(secret())).get(0);

        assertTrue(HashUtil.verify("passphrase".toCharArray(), imported.getSalt(),
                imported.getIterations(), imported.getHash()));
    }

    @Test
    public void import_rejectsAnUnsupportedSchemaVersion() {
        assertThrows(JSONException.class, () -> importFrom("{\"schemaVersion\":99,\"secrets\":[]}"));
    }

    @Test
    public void import_rejectsAnIterationCountThatIsTooLowOrTooHigh() throws Exception {
        Secret low = secret();
        low.setIterations(1);
        Secret high = secret();
        high.setIterations(Integer.MAX_VALUE);

        byte[] lowJson = export(low);
        byte[] highJson = export(high);

        assertThrows(JSONException.class, () -> importFrom(lowJson));
        assertThrows(JSONException.class, () -> importFrom(highJson));
    }

    @Test
    public void import_rejectsAnOutOfRangeInterval() throws Exception {
        Secret zero = secret();
        zero.setIntervalValue(0);
        Secret huge = secret();
        huge.setIntervalValue(Integer.MAX_VALUE);

        byte[] zeroJson = export(zero);
        byte[] hugeJson = export(huge);

        assertThrows(JSONException.class, () -> importFrom(zeroJson));
        assertThrows(JSONException.class, () -> importFrom(hugeJson));
    }

    @Test
    public void import_rejectsAnInvalidReminderTime() throws Exception {
        Secret badHour = secret();
        badHour.setReminderHour(24);
        Secret badMinute = secret();
        badMinute.setReminderMinute(60);

        byte[] hourJson = export(badHour);
        byte[] minuteJson = export(badMinute);

        assertThrows(JSONException.class, () -> importFrom(hourJson));
        assertThrows(JSONException.class, () -> importFrom(minuteJson));
    }

    @Test
    public void import_rejectsAHashOfTheWrongLength() throws Exception {
        Secret shortHash = secret();
        shortHash.setHash("AAAA");

        byte[] json = export(shortHash);

        assertThrows(JSONException.class, () -> importFrom(json));
    }

    @Test
    public void import_rejectsABlankName() throws Exception {
        Secret blank = secret();
        blank.setName("   ");

        byte[] json = export(blank);

        assertThrows(JSONException.class, () -> importFrom(json));
    }
}
