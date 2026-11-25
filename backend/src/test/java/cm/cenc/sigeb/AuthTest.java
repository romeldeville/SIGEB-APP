package cm.cenc.sigeb;

import cm.cenc.sigeb.auth.service.AuthService;
import cm.cenc.sigeb.auth.dto.*;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;
import cm.cenc.sigeb.shared.crypto.service.TotpService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuthTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry r) {
        r.add("spring.flyway.user", pg::getUsername);
        r.add("spring.flyway.password", pg::getPassword);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private TotpService totpService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void bootstrapAdminEstCreeEtPeutSeConnecter() {
        AuthResponse response = authService.login(
                new LoginRequest("admin@sigeb.cm", "AdminSigeb2026!", null),
                "127.0.0.1"
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("ADMIN_NATIONAL");
        assertThat(response.mdpAChanger()).isTrue();
    }

    @Test
    void echecsRepetesProvoquentLeVerrouillageDuCompte() {
        LoginRequest badRequest = new LoginRequest("admin@sigeb.cm", "MauvaisMotDePasse", null);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(badRequest, "127.0.0.1"))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // Le 5ème échec verrouille le compte
        assertThatThrownBy(() -> authService.login(badRequest, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        Utilisateur admin = utilisateurRepository.findByIdentifiant("admin@sigeb.cm").orElseThrow();
        assertThat(admin.isVerrouille()).isTrue();

        // Une tentative subséquente renvoie une exception de compte verrouillé
        assertThatThrownBy(() -> authService.login(badRequest, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("temporairement verrouillé");

        // Nettoyage pour les autres tests
        admin.setTentativesEchec((short) 0);
        admin.setVerrouilleJusqua(null);
        utilisateurRepository.save(admin);
    }

    @Test
    void rotationEtRejeuDuRefreshToken() {
        AuthResponse loginResp = authService.login(
                new LoginRequest("admin@sigeb.cm", "AdminSigeb2026!", null),
                "127.0.0.1"
        );

        String originalRefresh = loginResp.refreshToken();
        AuthResponse refreshResp = authService.refresh(new RefreshRequest(originalRefresh));

        assertThat(refreshResp.accessToken()).isNotBlank();
        assertThat(refreshResp.refreshToken()).isNotEqualTo(originalRefresh);

        // Tentative de rejeu de l'ancien refresh token révoqué -> détection et blocage immédiat
        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(originalRefresh)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Session invalidée");
    }

    @Test
    void activationEtValidationTotp2FA() {
        Utilisateur admin = utilisateurRepository.findByIdentifiant("admin@sigeb.cm").orElseThrow();

        TenantContext.setCurrentUser(new UserPrincipal(
                admin.getId(),
                admin.getIdentifiant(),
                admin.getMotDePasseHash(),
                Role.ADMIN_NATIONAL,
                null,
                null,
                admin.getNomComplet(),
                "FR",
                true,
                false,
                false
        ));

        TotpSetupResponse setup = authService.setupTotp();
        assertThat(setup.secret()).isNotBlank();
        assertThat(setup.otpAuthUri()).contains("otpauth://totp/");

        long currentWindow = Instant.now().getEpochSecond() / 30;
        int validCode = totpService.generateCode(setup.secret(), currentWindow);
        String codeStr = String.format("%06d", validCode);

        authService.confirmTotp(new TotpVerifyRequest(codeStr));

        Utilisateur updated = utilisateurRepository.findByIdentifiant("admin@sigeb.cm").orElseThrow();
        assertThat(updated.isTotpActive()).isTrue();

        // Connexion avec TOTP requis
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@sigeb.cm", "AdminSigeb2026!", null), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2FA");

        // Connexion avec code TOTP valide
        int newCode = totpService.generateCode(setup.secret(), Instant.now().getEpochSecond() / 30);
        AuthResponse authSuccess = authService.login(
                new LoginRequest("admin@sigeb.cm", "AdminSigeb2026!", String.format("%06d", newCode)),
                "127.0.0.1"
        );
        assertThat(authSuccess.accessToken()).isNotBlank();
        assertThat(authSuccess.totpActive()).isTrue();

        // Réinitialisation de l'état 2FA pour propreté
        updated.setTotpActive(false);
        updated.setTotpSecretChiffre(null);
        utilisateurRepository.save(updated);
    }
}
