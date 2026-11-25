package cm.cenc.sigeb.shared.crypto.service;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class CryptoService {

    // Argon2id conforme OWASP : salt 16B, hash 32B, parallélisme 1, mémoire 65536 KiB (64 MB), itérations 3
    private final Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);

    public String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        return argon2.encode(rawPassword);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return argon2.matches(rawPassword, encodedPassword);
    }

    public String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non supporté", e);
        }
    }
}
