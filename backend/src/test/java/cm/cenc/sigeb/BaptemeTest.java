package cm.cenc.sigeb;

import cm.cenc.sigeb.auth.dto.ChangePinRequest;
import cm.cenc.sigeb.auth.service.AuthService;
import cm.cenc.sigeb.bapteme.dto.*;
import cm.cenc.sigeb.bapteme.enums.GraviteAvenant;
import cm.cenc.sigeb.bapteme.enums.OrigineActe;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.bapteme.enums.StatutAvenant;
import cm.cenc.sigeb.bapteme.service.ActeBaptemeService;
import cm.cenc.sigeb.bapteme.service.AvenantService;
import cm.cenc.sigeb.referentiel.dto.*;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;
import cm.cenc.sigeb.referentiel.service.ReferentielService;
import cm.cenc.sigeb.referentiel.service.UtilisateurService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class BaptemeTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry r) {
        r.add("spring.flyway.user", pg::getUsername);
        r.add("spring.flyway.password", pg::getPassword);
    }

    @Autowired ReferentielService referentielService;
    @Autowired UtilisateurService utilisateurService;
    @Autowired AuthService authService;
    @Autowired ActeBaptemeService acteService;
    @Autowired AvenantService avenantService;
    @Autowired UtilisateurRepository utilisateurRepository;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void cycleCompletActeScellementIunEtAvenants() {
        // 1. Admin National crée le diocèse YDE, la paroisse P001 et les acteurs
        Utilisateur admin = utilisateurRepository.findByIdentifiant("admin@sigeb.cm").orElseThrow();
        seConnecterComme(admin.getId(), admin.getIdentifiant(), Role.ADMIN_NATIONAL, null, null);

        ProvinceResponse provYde = referentielService.listerProvinces().stream()
                .filter(p -> "YDE".equals(p.code())).findFirst().orElseThrow();

        DioceseResponse dio = referentielService.creerDiocese(new DioceseCreateRequest(
                provYde.id(), "YDE", "ARCHIDIOCESE", "Archidiocèse de Yaoundé", "Archdiocese of Yaounde", "Yaoundé"));

        ParoisseResponse par = referentielService.creerParoisse(new ParoisseCreateRequest(
                dio.id(), "P001", "PAROISSE", "Paroisse Saint-Joseph", "Saint Joseph Parish", "Mvog-Ada", null));

        UtilisateurResponse chancelier = utilisateurService.creerUtilisateur(new UtilisateurCreateRequest(
                "chancelier.yde@sigeb.cm", "Chancelier2026!", "Abbé Chancelier", null,
                Role.CHANCELIER, dio.id(), null, "FR"));
        UtilisateurResponse cure = utilisateurService.creerUtilisateur(new UtilisateurCreateRequest(
                "cure.p001@sigeb.cm", "CureParoisse2026!", "Abbé Curé", null,
                Role.CURE, dio.id(), par.id(), "FR"));
        UtilisateurResponse secretaire = utilisateurService.creerUtilisateur(new UtilisateurCreateRequest(
                "sec.p001@sigeb.cm", "Secretaire2026!", "Marie Secrétaire", null,
                Role.SECRETAIRE, dio.id(), par.id(), "FR"));

        // Configuration des PIN de signature du Curé et du Chancelier
        seConnecterComme(cure.id(), cure.identifiant(), Role.CURE, dio.id(), par.id());
        authService.changePin(new ChangePinRequest("CureParoisse2026!", "1234"));

        seConnecterComme(chancelier.id(), chancelier.identifiant(), Role.CHANCELIER, dio.id(), null);
        authService.changePin(new ChangePinRequest("Chancelier2026!", "5678"));

        // 2. Le Secrétaire saisit un acte et le soumet pour validation
        seConnecterComme(secretaire.id(), secretaire.identifiant(), Role.SECRETAIRE, dio.id(), par.id());

        ActeBaptemeRequest req1 = new ActeBaptemeRequest(
                OrigineActe.NOUVEAU, "MBARGA", "Jean-Baptiste", "Jean", "M",
                LocalDate.of(2025, 11, 10), "Yaoundé", "Paul MBARGA", "Anne ONDOA", false,
                "Pierre ESSOMBA", null, "Paroisse Sainte-Anne",
                LocalDate.of(2026, 2, 14), "Église Saint-Joseph", "Abbé Curé",
                "T12", "P04", "001", true
        );
        ActeBaptemeResponse brouillon = acteService.creerActe(req1, "127.0.0.1");
        assertThat(brouillon.statut()).isEqualTo(StatutActe.EN_ATTENTE_VALIDATION);
        assertThat(brouillon.iun()).isNull();

        // Vérification anti-doublon
        List<DoublonPotentielResponse> doublons = acteService.verifierDoublons("mbarga", "jean-baptiste", LocalDate.of(2025, 11, 10));
        assertThat(doublons).hasSize(1);

        // Le Secrétaire ne peut pas sceller
        assertThatThrownBy(() -> acteService.scellerActe(brouillon.id(), new ScellementRequest("1234"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class);

        // 3. Le Curé scelle l'acte avec son PIN -> IUN atomique généré
        seConnecterComme(cure.id(), cure.identifiant(), Role.CURE, dio.id(), par.id());
        ActeBaptemeResponse scelle = acteService.scellerActe(brouillon.id(), new ScellementRequest("1234"), "127.0.0.1");
        assertThat(scelle.statut()).isEqualTo(StatutActe.SCELLE);
        assertThat(scelle.iun()).isEqualTo("CM-YDE-P001-2026-00001");

        // Un acte scellé est immuable directement
        assertThatThrownBy(() -> acteService.modifierActe(scelle.id(), req1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("immuable");

        // 4. Avenant MINEUR approuvé directement par le Curé (version scellée -> +1)
        int versionAvantAvenant = scelle.version();
        AvenantResponse avMineur = avenantService.creerAvenant(scelle.id(), new AvenantCreateRequest(
                GraviteAvenant.MINEURE, Map.of("lieuNaissance", "Yaoundé 2e"),
                "Précision arrondissement", "Acte état civil #44", "1234"
        ), "127.0.0.1");
        assertThat(avMineur.statut()).isEqualTo(StatutAvenant.APPROUVE);
        assertThat(avMineur.versionCible()).isEqualTo(versionAvantAvenant + 1);
        assertThat(avMineur.anciennesValeurs()).containsEntry("lieuNaissance", "Yaoundé");

        // 5. Avenant MAJEUR soumis par le Curé puis approuvé par le Chancelier (version +2)
        AvenantResponse avMajeur = avenantService.creerAvenant(scelle.id(), new AvenantCreateRequest(
                GraviteAvenant.MAJEURE, Map.of("nom", "MBARGA-ATANGANA"),
                "Rectification patronyme sur jugement", "Jugement TGI #102", null
        ), "127.0.0.1");
        assertThat(avMajeur.statut()).isEqualTo(StatutAvenant.EN_ATTENTE_CHANCELIER);

        seConnecterComme(chancelier.id(), chancelier.identifiant(), Role.CHANCELIER, dio.id(), null);
        AvenantResponse avApprouve = avenantService.approuverAvenantMajeur(
                avMajeur.id(), new ScellementRequest("5678"), "127.0.0.1");
        assertThat(avApprouve.statut()).isEqualTo(StatutAvenant.APPROUVE);

        ActeBaptemeResponse acteFinal = acteService.consulter(scelle.id());
        assertThat(acteFinal.nom()).isEqualTo("MBARGA-ATANGANA");
        assertThat(acteFinal.lieuNaissance()).isEqualTo("Yaoundé 2e");
        assertThat(acteFinal.version()).isEqualTo(versionAvantAvenant + 2);
    }

    private void seConnecterComme(UUID id, String identifiant, Role role, UUID dioceseId, UUID paroisseId) {
        TenantContext.setCurrentUser(new UserPrincipal(
                id, identifiant, "", role, dioceseId, paroisseId, identifiant, "FR", true, false, false));
    }
}
