package com.frescuratotal.erp.common;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private PasswordHasher() {}

    public static boolean matches(String password, String salt, String expected) {
        PBEKeySpec spec =
                new PBEKeySpec(password.toCharArray(), HexFormat.of().parseHex(salt), 120000, 256);
        try {
            byte[] actual =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                            .generateSecret(spec)
                            .getEncoded();
            return MessageDigest.isEqual(actual, HexFormat.of().parseHex(expected));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Password verification is unavailable", error);
        } finally {
            spec.clearPassword();
        }
    }
}
