package cm.cenc.sigeb.shared.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Implémentation TOTP (RFC 6238 / RFC 4226) standard 6 chiffres, pas de 30 secondes, HMAC-SHA1.
 */
@Service
public class TotpService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int MODULUS = (int) Math.pow(10, DIGITS);
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[20]; // 160 bits
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean verifyCode(String secretBase64, String codeStr) {
        if (secretBase64 == null || codeStr == null || !codeStr.matches("\\d{6}")) {
            return false;
        }
        int code = Integer.parseInt(codeStr);
        long currentWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        // Fenêtre de tolérance de ±1 pas (±30s) pour absorber les dérives d'horloge
        for (long i = -1; i <= 1; i++) {
            if (generateCode(secretBase64, currentWindow + i) == code) {
                return true;
            }
        }
        return false;
    }

    public int generateCode(String secretBase64, long timeWindow) {
        try {
            byte[] key = Base64.getUrlDecoder().decode(secretBase64);
            byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % MODULUS;
        } catch (Exception e) {
            throw new IllegalStateException("Erreur calcul TOTP", e);
        }
    }

    public String getOtpAuthUri(String secret, String accountName, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, accountName, secret, issuer);
    }
}
