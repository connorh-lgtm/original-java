package com.legacy.realworld.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainText, String hashed) {
        try {
            return BCrypt.checkpw(plainText, hashed);
        } catch (IllegalArgumentException e) {
            // Stored password is not a valid BCrypt hash (e.g. legacy plain-text).
            // Return false so the caller returns 401 instead of 500.
            return false;
        }
    }

    private PasswordUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
