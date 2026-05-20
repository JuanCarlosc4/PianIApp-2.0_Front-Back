package com.piania.core.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ChatCryptoService {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    @Value("${piania.chat.master-key:}")
    private String masterKeyBase64;

    @Value("${jwt.secret:}")
    private String jwtSecretFallback;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes;

        if (masterKeyBase64 != null && !masterKeyBase64.isBlank()) {
            keyBytes = Base64.getDecoder().decode(masterKeyBase64);
        } else {
            // Fallback dev: derivar 32 bytes de jwt.secret (NO ideal, pero evita configuración extra en dev).
            byte[] src = (jwtSecretFallback == null ? "dev" : jwtSecretFallback).getBytes(StandardCharsets.UTF_8);
            keyBytes = new byte[32];
            for (int i = 0; i < keyBytes.length; i++) {
                keyBytes[i] = src[i % src.length];
            }
        }

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("piania.chat.master-key debe ser AES (16/24/32 bytes) en Base64");
        }

        this.secretKey = new SecretKeySpec(keyBytes, ALGO);
    }

    public EncryptedPayload encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return new EncryptedPayload(
                    cipherText,
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting chat message", e);
        }
    }

    public String decrypt(byte[] cipherText, String ivBase64) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting chat message", e);
        }
    }

    public record EncryptedPayload(byte[] cipherText, String ivBase64) {}
}
