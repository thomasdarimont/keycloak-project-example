package com.github.thomasdarimont.keycloak.custom.migration.acmecred;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class AcmePasswordValidator {

    public static boolean validateLegacyPassword(String password, AcmeCredentialModel acmeCredentialModel) {
        String hashAndSalt = acmeCredentialModel.getAcmeSecretData().getValue();
        String hash = hashAndSalt.substring(0, hashAndSalt.lastIndexOf(':'));
        String salt = hashAndSalt.substring(hashAndSalt.lastIndexOf(':') + 1);
        return verifyPasswordSha1(password, hash, salt);
    }

    private static boolean verifyPasswordSha1(String password, String expectedPasswordHash, String salt) {

        String passwordHash = encodePassword(password, salt);

        return expectedPasswordHash.equals(passwordHash);
    }

    public static String encodePassword(String password, String salt) {
        char[] passwordChars = password.toCharArray();
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);

        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(passwordChars));
        byteBuffer.rewind();
        byte[] passwordBytes = byteBuffer.array();

        byte[] hashedPasswordBytes = createHashedPassword(passwordBytes, saltBytes);
        return HexFormat.of().formatHex(hashedPasswordBytes, 0, hashedPasswordBytes.length);
    }

    private static byte[] createHashedPassword(byte[] passwordBytes, byte[] saltBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(passwordBytes, 0, passwordBytes.length);
            digest.update(saltBytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new RuntimeException(noSuchAlgorithmException);
        }
    }
}
