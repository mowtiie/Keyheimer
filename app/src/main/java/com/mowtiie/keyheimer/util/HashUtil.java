package com.mowtiie.keyheimer.util;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class HashUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    public static final int DEFAULT_ITERATIONS = 210_000;

    private HashUtil() {
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // Caller should Arrays.fill(passphrase, '\0') after use to avoid
    // leaving the plaintext passphrase resident in memory.
    public static String hash(char[] passphrase, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hashBytes = factory.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hashBytes, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Unable to hash passphrase", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verify(char[] attempt, byte[] salt, int iterations, String storedHash) {
        String attemptHash = hash(attempt, salt, iterations);
        byte[] a = Base64.decode(attemptHash, Base64.NO_WRAP);
        byte[] b = Base64.decode(storedHash, Base64.NO_WRAP);
        boolean result = MessageDigest.isEqual(a, b);
        Arrays.fill(a, (byte) 0);
        Arrays.fill(b, (byte) 0);
        return result;
    }
}