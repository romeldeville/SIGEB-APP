package cm.cenc.sigeb;

import cm.cenc.sigeb.audit.dto.IntegriteAuditResponse;
import cm.cenc.sigeb.audit.dto.JournalAuditResponse;
import cm.cenc.sigeb.audit.enums.AuditAction;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.audit.service.AuditService;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.idempotence.entity.RequeteIdempotente;
import cm.cenc.sigeb.shared.idempotence.service.IdempotenceService;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.NiveauEcclesial;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import cm.cenc.sigeb.statistique.dto.StatistiqueGlobaleResponse;
import cm.cenc.sigeb.statistique.service.StatistiqueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuditStatistiqueIdempotenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry r) {
        r.add("spring.flyway.user", pg::getUsername);
        r.add("spring.flyway.password", pg::getPassword);
    }

    @Autowired AuditService auditService;
    @Autowired StatistiqueService statistiqueService;
    @Autowired IdempotenceService idempotenceService;
    @Autowired UtilisateurRepository utilisateurRepository;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void auditChaineIntegriteStatistiquesEtIdempotence() {
        Utilisateur admin = utilisateurRepository.findByIdentifiant("admin@sigeb.cm").orElseThrow();
        TenantContext.setCurrentUser(new UserPrincipal(
                admin.getId(), admin.getIdentifiant(), "", Role.ADMIN_NATIONAL, NiveauEcclesial.NATIONAL,
                null, null, null, admin.getNomComplet(), "FR", true, false, false));

        // 1. Audit + vérification d'intégrité de la chaîne SHA-256
        auditService.enregistrer(AuditEvent.simple(admin.getId(), Role.ADMIN_NATIONAL, AuditAction.CONNEXION, "Test 1", "127.0.0.1"));
        auditService.enregistrer(AuditEvent.simple(admin.getId(), Role.ADMIN_NATIONAL, AuditAction.CONNEXION, "Test 2", "127.0.0.1"));

        List<JournalAuditResponse> logs = auditService.consulterAudit(null, "CONNEXION", 10);
        assertThat(logs).hasSizeGreaterThanOrEqualTo(2);

        IntegriteAuditResponse integrite = auditService.verifierIntegriteChaine();
        assertThat(integrite.chaineValide()).isTrue();
        assertThat(integrite.nombreEntreesVerifiees()).isGreaterThanOrEqualTo(2);
        assertThat(integrite.premiereRuptureId()).isNull();

        // 2. Statistiques nationales
        StatistiqueGlobaleResponse stats = statistiqueService.tableauDeBord();
        assertThat(stats.niveau()).isEqualTo("NATIONAL");
        assertThat(stats.totalActesScelles()).isGreaterThanOrEqualTo(0);

        // 3. Idempotence : mémorisation, rejeu autorisé, conflit si corps différent
        UUID cle = UUID.randomUUID();
        String hashA = "a".repeat(64);
        String hashB = "b".repeat(64);

        assertThat(idempotenceService.verifier(admin.getId(), cle, hashA)).isEmpty();
        idempotenceService.memoriser(admin.getId(), cle, hashA, 201, Map.of("id", "acte-1"));

        Optional<RequeteIdempotente> replay = idempotenceService.verifier(admin.getId(), cle, hashA);
        assertThat(replay).isPresent();
        assertThat(replay.get().getStatutHttp()).isEqualTo((short) 201);

        assertThatThrownBy(() -> idempotenceService.verifier(admin.getId(), cle, hashB))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("contenu de requête différent");
    }
}
