package com.legacy.realworld.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainText, String hashed) {
        return BCrypt.checkpw(plainText, hashed);
    }

    private PasswordUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
