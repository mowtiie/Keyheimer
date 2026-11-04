package com.mowtiie.keyheimer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;

public class HashUtilTest {

    private static final int TEST_ITERATIONS = 1_000;

    @Test
    public void verify_acceptsTheCorrectPassphrase() {
        byte[] salt = HashUtil.generateSalt();
        String hash = HashUtil.hash("correct horse".toCharArray(), salt, TEST_ITERATIONS);

        assertTrue(HashUtil.verify("correct horse".toCharArray(), salt, TEST_ITERATIONS, hash));
    }

    @Test
    public void verify_rejectsAWrongPassphrase() {
        byte[] salt = HashUtil.generateSalt();
        String hash = HashUtil.hash("correct horse".toCharArray(), salt, TEST_ITERATIONS);

        assertFalse(HashUtil.verify("battery staple".toCharArray(), salt, TEST_ITERATIONS, hash));
    }

    @Test
    public void verify_failsWhenIterationCountDiffers() {
        byte[] salt = HashUtil.generateSalt();
        String hash = HashUtil.hash("correct horse".toCharArray(), salt, TEST_ITERATIONS);

        assertFalse(HashUtil.verify("correct horse".toCharArray(), salt, TEST_ITERATIONS + 1, hash));
    }

    @Test
    public void verify_returnsFalseForCorruptStoredValues() {
        byte[] salt = HashUtil.generateSalt();
        char[] attempt = "anything".toCharArray();

        assertFalse(HashUtil.verify(attempt, salt, TEST_ITERATIONS, "not base64 !!"));
        assertFalse(HashUtil.verify(attempt, salt, TEST_ITERATIONS, ""));
        assertFalse(HashUtil.verify(attempt, new byte[0], TEST_ITERATIONS, "AAAA"));
        assertFalse(HashUtil.verify(attempt, salt, 0, "AAAA"));
    }

    @Test
    public void hash_differsBetweenSalts() {
        char[] passphrase = "same passphrase".toCharArray();

        String first = HashUtil.hash(passphrase, HashUtil.generateSalt(), TEST_ITERATIONS);
        String second = HashUtil.hash(passphrase, HashUtil.generateSalt(), TEST_ITERATIONS);

        assertNotEquals(first, second);
    }

    @Test
    public void hash_producesA256BitValue() {
        String hash = HashUtil.hash("x".toCharArray(), HashUtil.generateSalt(), TEST_ITERATIONS);

        assertEquals(32, Base64.getDecoder().decode(hash).length);
    }

    @Test
    public void generateSalt_isSixteenRandomBytes() {
        byte[] first = HashUtil.generateSalt();
        byte[] second = HashUtil.generateSalt();

        assertEquals(16, first.length);
        assertFalse(Arrays.equals(first, second));
    }
}
