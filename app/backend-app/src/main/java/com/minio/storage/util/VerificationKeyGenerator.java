package com.minio.storage.util;

import java.security.SecureRandom;

public class VerificationKeyGenerator {

    private static final String ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int KEY_LENGTH = 12;
    private static final SecureRandom random = new SecureRandom();

    private VerificationKeyGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateKey() {
        StringBuilder key = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            int randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
            key.append(ALLOWED_CHARACTERS.charAt(randomIndex));
        }
        return key.toString();
    }

}