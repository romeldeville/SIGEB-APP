package cm.cenc.sigeb.auth.service;

import cm.cenc.sigeb.auth.entity.RefreshToken;
import cm.cenc.sigeb.auth.repository.RefreshTokenRepository;

import cm.cenc.sigeb.audit.enums.AuditAction;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.audit.service.AuditService;
import cm.cenc.sigeb.auth.dto.*;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;
import cm.cenc.sigeb.shared.crypto.service.CryptoService;
import cm.cenc.sigeb.shared.crypto.service.TotpService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.service.JwtService;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CryptoService cryptoService;
    private final TotpService totpService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshExpirationDays;

    public AuthService(
            UtilisateurRepository utilisateurRepository,
            RefreshTokenRepository refreshTokenRepository,
            CryptoService cryptoService,
            TotpService totpService,
            JwtService jwtService,
            AuditService auditService,
            LoginAttemptService loginAttemptService,
            @Value("${sigeb.security.jwt.refresh-expiration-days:7}") long refreshExpirationDays
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cryptoService = cryptoService;
        this.totpService = totpService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.loginAttemptService = loginAttemptService;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        Utilisateur user = utilisateurRepository.findByIdentifiant(request.identifiant().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Identifiant ou mot de passe incorrect"));

        if (!"ACTIF".equals(user.getStatut())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "COMPTE_SUSPENDU", "Votre compte a été suspendu");
        }

        if (user.isVerrouille()) {
            throw new BusinessException(HttpStatus.LOCKED, "COMPTE_VERROUILLE",
                    "Compte temporairement verrouillé suite à plusieurs tentatives. Veuillez patienter.");
        }

        if (!cryptoService.matchesPassword(request.motDePasse(), user.getMotDePasseHash())) {
            loginAttemptService.enregistrerEchec(user.getId());
            throw new BadCredentialsException("Identifiant ou mot de passe incorrect");
        }

        // Si TOTP est actif, on exige le code
        if (user.isTotpActive()) {
            if (request.totpCode() == null || request.totpCode().isBlank()) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "2FA_REQUIS", "Code de double authentification (2FA) requis");
            }
            String secret = new String(user.getTotpSecretChiffre(), StandardCharsets.UTF_8);
            if (!totpService.verifyCode(secret, request.totpCode().trim())) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "2FA_INVALIDE", "Code 2FA invalide ou expiré");
            }
        }

        // Succès : réinitialisation des compteurs d'échec
        loginAttemptService.reinitialiserEchecs(user.getId());

        UserPrincipal principal = toPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = generateSecureToken();
        String tokenHash = cryptoService.hashSha256(rawRefreshToken);

        UUID familleId = UUID.randomUUID();
        Instant expiry = Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(user, tokenHash, familleId, expiry);
        refreshTokenRepository.save(refreshToken);

        auditService.enregistrer(new AuditEvent(
                user.getId(),
                user.getRole(),
                user.getDiocese() != null ? user.getDiocese().getId() : null,
                user.getParoisse() != null ? user.getParoisse().getId() : null,
                AuditAction.CONNEXION,
                "utilisateur",
                user.getId(),
                null,
                "Connexion réussie",
                null,
                clientIp
        ));

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                user.getRole().name(),
                user.getNiveau().name(),
                user.getNomComplet(),
                user.getProvince() != null ? user.getProvince().getId() : null,
                user.getDiocese() != null ? user.getDiocese().getId() : null,
                user.getParoisse() != null ? user.getParoisse().getId() : null,
                user.isMdpAChanger(),
                user.isTotpActive()
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = cryptoService.hashSha256(request.refreshToken().trim());
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALIDE", "Jeton de rafraîchissement inconnu"));

        // Détection de rejeu : si un jeton déjà révoqué est réutilisé, toute la famille est compromise !
        if (currentToken.getRevoqueLe() != null) {
            refreshTokenRepository.revoquerFamille(currentToken.getFamilleId(), Instant.now());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "REFRESH_COMPROMIS",
                    "Tentative de réutilisation d'un jeton révoqué. Session invalidée.");
        }

        if (currentToken.getExpireLe().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "REFRESH_EXPIRE", "Jeton de rafraîchissement expiré");
        }

        Utilisateur user = currentToken.getUtilisateur();
        if (!"ACTIF".equals(user.getStatut())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "COMPTE_SUSPENDU", "Compte suspendu");
        }

        // Révocation de l'ancien jeton
        currentToken.setRevoqueLe(Instant.now());
        refreshTokenRepository.save(currentToken);

        // Émission du nouveau refresh token dans la même famille
        String newRawRefresh = generateSecureToken();
        String newTokenHash = cryptoService.hashSha256(newRawRefresh);
        Instant expiry = Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS);
        RefreshToken nextToken = new RefreshToken(user, newTokenHash, currentToken.getFamilleId(), expiry);
        refreshTokenRepository.save(nextToken);

        UserPrincipal principal = toPrincipal(user);
        String newAccessToken = jwtService.generateAccessToken(principal);

        return new AuthResponse(
                newAccessToken,
                newRawRefresh,
                user.getRole().name(),
                user.getNiveau().name(),
                user.getNomComplet(),
                user.getProvince() != null ? user.getProvince().getId() : null,
                user.getDiocese() != null ? user.getDiocese().getId() : null,
                user.getParoisse() != null ? user.getParoisse().getId() : null,
                user.isMdpAChanger(),
                user.isTotpActive()
        );
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = TenantContext.requireCurrentUser();
        Utilisateur user = utilisateurRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        if (!cryptoService.matchesPassword(request.ancienMotDePasse(), user.getMotDePasseHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ANCIEN_MDP_INCORRECT", "L'ancien mot de passe est incorrect");
        }

        if (cryptoService.matchesPassword(request.nouveauMotDePasse(), user.getMotDePasseHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MDP_IDENTIQUE", "Le nouveau mot de passe doit être différent de l'actuel");
        }

        user.setMotDePasseHash(cryptoService.hashPassword(request.nouveauMotDePasse()));
        user.setMdpAChanger(false);
        utilisateurRepository.save(user);

        // Invalidation de toutes les autres sessions actives
        refreshTokenRepository.revoquerTousPourUtilisateur(user.getId(), Instant.now());
    }

    @Transactional
    public void changePin(ChangePinRequest request) {
        UserPrincipal principal = TenantContext.requireCurrentUser();
        Utilisateur user = utilisateurRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        if (!cryptoService.matchesPassword(request.motDePasse(), user.getMotDePasseHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MDP_INCORRECT", "Mot de passe incorrect");
        }

        user.setPinSignatureHash(cryptoService.hashPassword(request.pin()));
        utilisateurRepository.save(user);
    }

    @Transactional
    public TotpSetupResponse setupTotp() {
        UserPrincipal principal = TenantContext.requireCurrentUser();
        Utilisateur user = utilisateurRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        String secret = totpService.generateSecret();
        user.setTotpSecretChiffre(secret.getBytes(StandardCharsets.UTF_8));
        user.setTotpActive(false); // Sera activé après vérification
        utilisateurRepository.save(user);

        String uri = totpService.getOtpAuthUri(secret, user.getIdentifiant(), "SIGEB-Cameroun");
        return new TotpSetupResponse(secret, uri);
    }

    @Transactional
    public void confirmTotp(TotpVerifyRequest request) {
        UserPrincipal principal = TenantContext.requireCurrentUser();
        Utilisateur user = utilisateurRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        if (user.getTotpSecretChiffre() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TOTP_NON_CONFIGURE", "Configuration 2FA non initiée");
        }

        String secret = new String(user.getTotpSecretChiffre(), StandardCharsets.UTF_8);
        if (!totpService.verifyCode(secret, request.code())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CODE_INVALIDE", "Code de validation 2FA incorrect");
        }

        user.setTotpActive(true);
        utilisateurRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        UserPrincipal principal = TenantContext.requireCurrentUser();
        Utilisateur user = utilisateurRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request, String clientIp) {
        UserPrincipal principal = TenantContext.requireCurrentUser();
        Utilisateur user = utilisateurRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        String nouveauNom = request.nomComplet().trim();
        String nouveauTel = request.telephone() != null && !request.telephone().isBlank()
                ? request.telephone().trim()
                : null;
        String nouvelleLangue = request.languePreferee().trim().toUpperCase();

        user.setNomComplet(nouveauNom);
        user.setTelephone(nouveauTel);
        user.setLanguePreferee(nouvelleLangue);
        utilisateurRepository.save(user);

        auditService.enregistrer(new AuditEvent(
                user.getId(),
                user.getRole(),
                user.getDiocese() != null ? user.getDiocese().getId() : null,
                user.getParoisse() != null ? user.getParoisse().getId() : null,
                AuditAction.RECTIFICATION,
                "utilisateur",
                user.getId(),
                null,
                "Mise à jour du profil personnel",
                null,
                clientIp
        ));

        return toProfileResponse(user);
    }

    private UserProfileResponse toProfileResponse(Utilisateur user) {
        boolean en = "EN".equalsIgnoreCase(user.getLanguePreferee());
        return new UserProfileResponse(
                user.getId(),
                user.getIdentifiant(),
                user.getNomComplet(),
                user.getTelephone(),
                user.getLanguePreferee(),
                user.getRole().name(),
                user.getNiveau().name(),
                user.getProvince() != null ? user.getProvince().getId() : null,
                user.getProvince() != null ? (en ? user.getProvince().getNomEn() : user.getProvince().getNomFr()) : null,
                user.getDiocese() != null ? user.getDiocese().getId() : null,
                user.getDiocese() != null ? (en ? user.getDiocese().getNomEn() : user.getDiocese().getNomFr()) : null,
                user.getParoisse() != null ? user.getParoisse().getId() : null,
                user.getParoisse() != null ? (en ? user.getParoisse().getNomEn() : user.getParoisse().getNomFr()) : null,
                user.getStatut(),
                user.isTotpActive(),
                user.isMdpAChanger(),
                user.getPinSignatureHash() != null
        );
    }

    private UserPrincipal toPrincipal(Utilisateur user) {
        return new UserPrincipal(
                user.getId(),
                user.getIdentifiant(),
                user.getMotDePasseHash(),
                user.getRole(),
                user.getNiveau(),
                user.getProvince() != null ? user.getProvince().getId() : null,
                user.getDiocese() != null ? user.getDiocese().getId() : null,
                user.getParoisse() != null ? user.getParoisse().getId() : null,
                user.getNomComplet(),
                user.getLanguePreferee(),
                user.isActif(),
                user.isMdpAChanger(),
                user.isTotpActive()
        );
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
