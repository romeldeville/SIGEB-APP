package cm.cenc.sigeb.carte.service;

import cm.cenc.sigeb.carte.entity.CleSignature;
import cm.cenc.sigeb.carte.enums.StatutCleSignature;
import cm.cenc.sigeb.carte.repository.CleSignatureRepository;
import cm.cenc.sigeb.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class QrSignatureService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final CleSignatureRepository cleRepository;
    private final SecretKeySpec masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public record SignedQr(String kid, String signatureCourte, String qrPayload) {}

    public QrSignatureService(
            CleSignatureRepository cleRepository,
            @Value("${sigeb.security.master-key:sigeb-master-key-cameroon-2026-change-in-prod}") String masterKeyRaw
    ) {
        this.cleRepository = cleRepository;
        this.masterKey = deriveAes256Key(masterKeyRaw);
    }

    @Transactional
    public void initialiserCleSiAbsente() {
        if (cleRepository.findByStatut(StatutCleSignature.ACTIVE).isEmpty()) {
            creerNouvelleCleActive("K2026A");
        }
    }

    @Transactional
    public String effectuerRotationCle(String nouveauKid) {
        String kid = nouveauKid.trim().toUpperCase();
        if (cleRepository.existsById(kid)) {
            throw new BusinessException(HttpStatus.CONFLICT, "KID_EXISTANT", "Cet identifiant de clé existe déjà");
        }
        cleRepository.findByStatut(StatutCleSignature.ACTIVE).ifPresent(active -> {
            active.setStatut(StatutCleSignature.VERIFY_ONLY);
            cleRepository.saveAndFlush(active);
        });
        creerNouvelleCleActive(kid);
        return kid;
    }

    public String genererTokenOpaque() {
        byte[] bytes = new byte[24]; // 192 bits >= 128 bits exigés
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional(readOnly = true)
    public SignedQr signer(String qrToken) {
        CleSignature active = cleRepository.findByStatut(StatutCleSignature.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Aucune clé de signature QR active"));
        byte[] secret = dechiffrerSecret(active.getSecretChiffre());
        String sigCourte = calculerHmacCourt(secret, active.getKid(), qrToken);
        String payload = construirePayload(active.getKid(), qrToken, sigCourte);
        return new SignedQr(active.getKid(), sigCourte, payload);
    }

    @Transactional(readOnly = true)
    public boolean verifierSignature(String kid, String qrToken, String signatureCourte) {
        Optional<CleSignature> opt = cleRepository.findById(kid);
        if (opt.isEmpty()) return false;
        CleSignature cle = opt.get();
        if (cle.getStatut() == StatutCleSignature.RETIREE) return false;

        byte[] secret = dechiffrerSecret(cle.getSecretChiffre());
        String attendue = calculerHmacCourt(secret, kid, qrToken);
        return MessageDigest.isEqual(
                attendue.getBytes(StandardCharsets.UTF_8),
                signatureCourte.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String construirePayload(String kid, String qrToken, String signatureCourte) {
        return "SIGEB:v2:" + kid + ":" + qrToken + ":" + signatureCourte;
    }

    private void creerNouvelleCleActive(String kid) {
        byte[] rawSecret = new byte[32]; // 256 bits
        secureRandom.nextBytes(rawSecret);

        CleSignature cle = new CleSignature();
        cle.setKid(kid);
        cle.setSecretChiffre(chiffrerSecret(rawSecret));
        cle.setStatut(StatutCleSignature.ACTIVE);
        cleRepository.save(cle);
    }

    private String calculerHmacCourt(byte[] secret, String kid, String qrToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] full = mac.doFinal(("v2:" + kid + ":" + qrToken).getBytes(StandardCharsets.UTF_8));
            // 16 octets = 32 caractères hex (128 bits de sécurité, compact pour QR PVC)
            return HexFormat.of().formatHex(full, 0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur calcul HMAC QR", e);
        }
    }

    private byte[] chiffrerSecret(byte[] plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Erreur chiffrement clé QR", e);
        }
    }

    private byte[] dechiffrerSecret(byte[] stored) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(stored, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ct = new byte[stored.length - GCM_IV_LENGTH];
            System.arraycopy(stored, GCM_IV_LENGTH, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ct);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur déchiffrement clé QR", e);
        }
    }

    private static SecretKeySpec deriveAes256Key(String raw) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Erreur dérivation MASTER_KEY", e);
        }
    }
}
