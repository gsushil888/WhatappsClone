package com.whatsapp.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionUtil {

    private static final String AES_ALGO = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGO = "HmacSHA256";

    public static String decrypt(String base64Payload, String secret) throws Exception {
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32);
        byte[] combined = Base64.getDecoder().decode(base64Payload);

        byte[] iv = Arrays.copyOfRange(combined, 0, 16);
        byte[] ciphertext = Arrays.copyOfRange(combined, 16, combined.length);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    public static String encrypt(String plaintext, String secret) throws Exception {
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32);
        byte[] iv = new byte[16];
        new java.security.SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[16 + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, 16);
        System.arraycopy(ciphertext, 0, combined, 16, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    public static boolean verifyHmac(String payload, String receivedSignature, String secret) throws Exception {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(keyBytes, HMAC_ALGO));
        byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        byte[] received = Base64.getDecoder().decode(receivedSignature);
        return java.security.MessageDigest.isEqual(expected, received);
    }
}
