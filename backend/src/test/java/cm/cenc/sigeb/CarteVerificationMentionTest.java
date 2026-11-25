package cm.cenc.sigeb;

import cm.cenc.sigeb.auth.dto.ChangePinRequest;
import cm.cenc.sigeb.auth.service.AuthService;
import cm.cenc.sigeb.bapteme.dto.ActeBaptemeRequest;
import cm.cenc.sigeb.bapteme.dto.ActeBaptemeResponse;
import cm.cenc.sigeb.bapteme.dto.ScellementRequest;
import cm.cenc.sigeb.bapteme.enums.OrigineActe;
import cm.cenc.sigeb.bapteme.service.ActeBaptemeService;
import cm.cenc.sigeb.carte.dto.CarteEmiseResponse;
import cm.cenc.sigeb.carte.dto.EmissionCarteRequest;
import cm.cenc.sigeb.carte.enums.FormatDocument;
import cm.cenc.sigeb.carte.enums.MotifEmission;
import cm.cenc.sigeb.carte.enums.TypeEmission;
import cm.cenc.sigeb.carte.service.CarteEmiseService;
import cm.cenc.sigeb.carte.service.QrSignatureService;
import cm.cenc.sigeb.mention.dto.MentionCreateRequest;
import cm.cenc.sigeb.mention.dto.MentionResponse;
import cm.cenc.sigeb.mention.enums.StatutMention;
import cm.cenc.sigeb.mention.enums.TypeSacrement;
import cm.cenc.sigeb.mention.service.MentionMarginaleService;
import cm.cenc.sigeb.referentiel.dto.*;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;
import cm.cenc.sigeb.referentiel.service.ReferentielService;
import cm.cenc.sigeb.referentiel.service.UtilisateurService;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.NiveauEcclesial;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import cm.cenc.sigeb.verification.dto.QrVerifyRequest;
import cm.cenc.sigeb.verification.dto.QrVerifyResponse;
import cm.cenc.sigeb.verification.dto.RechercheNationaleItemResponse;
import cm.cenc.sigeb.verification.dto.RechercheNationaleRequest;
import cm.cenc.sigeb.verification.enums.ResultatVerificationQr;
import cm.cenc.sigeb.verification.service.VerificationService;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CarteVerificationMentionTest {

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
    @Autowired CarteEmiseService carteService;
    @Autowired QrSignatureService qrSignatureService;
    @Autowired MentionMarginaleService mentionService;
    @Autowired VerificationService verificationService;
    @Autowired UtilisateurRepository utilisateurRepository;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void emissionCartePdfRotationCleMentionEtDuplicata() {
        // 1. Setup Paroisse A (origine) & Paroisse B (mariage)
        Utilisateur admin = utilisateurRepository.findByIdentifiant("admin@sigeb.cm").orElseThrow();
        seConnecter(admin.getId(), admin.getIdentifiant(), Role.ADMIN_NATIONAL, NiveauEcclesial.NATIONAL, null, null, null);

        ProvinceResponse provDla = referentielService.listerProvinces().stream()
                .filter(p -> "DLA".equals(p.code())).findFirst().orElseThrow();
        DioceseResponse dio = referentielService.creerDiocese(new DioceseCreateRequest(
                provDla.id(), "DLA", "ARCHIDIOCESE", "Archidiocèse de Douala", "Archdiocese of Douala", "Douala"));
        ParoisseResponse parA = referentielService.creerParoisse(new ParoisseCreateRequest(
                dio.id(), "P010", "PAROISSE", "Paroisse Saints-Pierre-et-Paul", "SS Peter & Paul Parish", "Akwa", null));
        ParoisseResponse parB = referentielService.creerParoisse(new ParoisseCreateRequest(
                dio.id(), "P020", "PAROISSE", "Paroisse Notre-Dame", "Our Lady Parish", "Bonapriso", null));

        UtilisateurResponse cureA = utilisateurService.creerUtilisateur(new UtilisateurCreateRequest(
                "cure.a@sigeb.cm", "CureAPass2026!", "Abbé Cure A", null,
                Role.CURE, NiveauEcclesial.PAROISSE, provDla.id(), dio.id(), parA.id(), "FR"));
        UtilisateurResponse cureB = utilisateurService.creerUtilisateur(new UtilisateurCreateRequest(
                "cure.b@sigeb.cm", "CureBPass2026!", "Abbé Cure B", null,
                Role.CURE, NiveauEcclesial.PAROISSE, provDla.id(), dio.id(), parB.id(), "FR"));

        seConnecter(cureA.id(), cureA.identifiant(), Role.CURE, NiveauEcclesial.PAROISSE, provDla.id(), dio.id(), parA.id());
        authService.changePin(new ChangePinRequest("CureAPass2026!", "1111"));

        // 2. Saisie & scellement de l'acte dans Paroisse A
        ActeBaptemeResponse acte = acteService.creerActe(new ActeBaptemeRequest(
                OrigineActe.NOUVEAU, "NDONGO", "Claire", "Claire", "F",
                LocalDate.of(2000, 3, 15), "Douala", "Jean NDONGO", "Marie EBOA", false,
                null, "Sophie MANGA", "Paroisse Akwa",
                LocalDate.of(2000, 6, 20), "Cathédrale", "Mgr X",
                "T05", "P18", "042", true
        ), "127.0.0.1");
        ActeBaptemeResponse scelle = acteService.scellerActe(acte.id(), new ScellementRequest("1111"), "127.0.0.1");

        // 3. Émission carte ORIGINALE + génération PDF à la volée (100 % BD)
        CarteEmiseResponse originale = carteService.emettreCarte(scelle.id(), new EmissionCarteRequest(
                FormatDocument.CARTE, "FR", MotifEmission.PREMIERE_EMISSION, null
        ), "127.0.0.1");
        assertThat(originale.typeEmission()).isEqualTo(TypeEmission.ORIGINALE);
        assertThat(originale.numeroDuplicata()).isEqualTo((short) 0);
        assertThat(originale.qrPayload()).startsWith("SIGEB:v2:K2026A:");

        byte[] pdfBytes = carteService.telechargerPdf(originale.id());
        assertThat(pdfBytes.length).isGreaterThan(1000);
        assertThat(new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        // 4. Scan QR anonyme -> réponse minimale (aucune donnée sensible exposée)
        TenantContext.clear();
        QrVerifyResponse scanAnonyme = verificationService.verifierQr(new QrVerifyRequest(originale.qrPayload()), "10.0.0.5");
        assertThat(scanAnonyme.valide()).isTrue();
        assertThat(scanAnonyme.resultat()).isEqualTo(ResultatVerificationQr.AUTHENTIQUE_A_JOUR);
        assertThat(scanAnonyme.nomAffiche()).isEqualTo("ND*** C.");
        assertThat(scanAnonyme.iun()).isNull();

        // 5. Rotation de clé QR (K2026A -> VERIFY_ONLY, K2026B -> ACTIVE) : l'ancienne carte reste valide !
        qrSignatureService.effectuerRotationCle("K2026B");
        QrVerifyResponse scanApresRotation = verificationService.verifierQr(new QrVerifyRequest(originale.qrPayload()), "10.0.0.5");
        assertThat(scanApresRotation.valide()).isTrue();

        // 6. Mention marginale inter-paroisses : Paroisse B déclare un MARIAGE, Paroisse A valide avec PIN
        seConnecter(cureB.id(), cureB.identifiant(), Role.CURE, NiveauEcclesial.PAROISSE, provDla.id(), dio.id(), parB.id());
        MentionResponse mentionDecl = mentionService.declarerMention(new MentionCreateRequest(
                scelle.iun(), TypeSacrement.MARIAGE, LocalDate.of(2026, 4, 10),
                "Paroisse Notre-Dame Bonapriso", "Abbé Cure B", "Marc FOUDA", "REG-MAR-2026-08"
        ), "127.0.0.1");
        assertThat(mentionDecl.statut()).isEqualTo(StatutMention.EN_ATTENTE_ORIGINE);

        seConnecter(cureA.id(), cureA.identifiant(), Role.CURE, NiveauEcclesial.PAROISSE, provDla.id(), dio.id(), parA.id());
        MentionResponse mentionVal = mentionService.validerMention(mentionDecl.id(), new ScellementRequest("1111"), "127.0.0.1");
        assertThat(mentionVal.statut()).isEqualTo(StatutMention.INSCRITE);

        // Scan authentifié de l'ancienne carte -> AUTHENTIQUE_MISE_A_JOUR_DISPONIBLE & état libre = false
        QrVerifyResponse scanAvecMaj = verificationService.verifierQr(new QrVerifyRequest(originale.qrPayload()), "127.0.0.1");
        assertThat(scanAvecMaj.resultat()).isEqualTo(ResultatVerificationQr.AUTHENTIQUE_MISE_A_JOUR_DISPONIBLE);
        assertThat(scanAvecMaj.etatLibreMatrimonial()).isFalse();
        assertThat(scanAvecMaj.mentionsInscrites()).hasSize(1);

        // 7. Émission d'un DUPLICATA (signé K2026B) -> révoque automatiquement l'originale
        CarteEmiseResponse duplicata = carteService.emettreCarte(scelle.id(), new EmissionCarteRequest(
                FormatDocument.CARTE, "FR", MotifEmission.MISE_A_JOUR, "Inscription mariage"
        ), "127.0.0.1");
        assertThat(duplicata.typeEmission()).isEqualTo(TypeEmission.DUPLICATA);
        assertThat(duplicata.numeroDuplicata()).isEqualTo((short) 1);
        assertThat(duplicata.qrPayload()).startsWith("SIGEB:v2:K2026B:");

        QrVerifyResponse scanOriginaleRevoquee = verificationService.verifierQr(new QrVerifyRequest(originale.qrPayload()), "127.0.0.1");
        assertThat(scanOriginaleRevoquee.valide()).isFalse();
        assertThat(scanOriginaleRevoquee.resultat()).isEqualTo(ResultatVerificationQr.REVOQUEE_OU_REMPLACEE);
        assertThat(scanOriginaleRevoquee.remplaceeParCodeCarte()).isEqualTo(duplicata.codeCarte());

        QrVerifyResponse scanDuplicata = verificationService.verifierQr(new QrVerifyRequest(duplicata.qrPayload()), "127.0.0.1");
        assertThat(scanDuplicata.valide()).isTrue();
        assertThat(scanDuplicata.resultat()).isEqualTo(ResultatVerificationQr.AUTHENTIQUE_A_JOUR);

        // 8. QR falsifié -> INVALIDE_OU_FALSIFIE
        QrVerifyResponse scanFalsifie = verificationService.verifierQr(new QrVerifyRequest(duplicata.qrPayload() + "ff"), "10.0.0.9");
        assertThat(scanFalsifie.valide()).isFalse();
        assertThat(scanFalsifie.resultat()).isEqualTo(ResultatVerificationQr.INVALIDE_OU_FALSIFIE);

        // 9. Recherche nationale tracée
        seConnecter(cureB.id(), cureB.identifiant(), Role.CURE, NiveauEcclesial.PAROISSE, provDla.id(), dio.id(), parB.id());
        List<RechercheNationaleItemResponse> res = verificationService.rechercheNationale(
                new RechercheNationaleRequest(scelle.iun(), null, null, null, "Vérification dossier parrainage"), "127.0.0.1");
        assertThat(res).hasSize(1);
        assertThat(res.get(0).etatLibreMatrimonial()).isFalse();
    }

    private void seConnecter(UUID id, String identifiant, Role role, NiveauEcclesial niveau,
                             UUID provinceId, UUID dioceseId, UUID paroisseId) {
        TenantContext.setCurrentUser(new UserPrincipal(
                id, identifiant, "", role, niveau, provinceId, dioceseId, paroisseId, identifiant, "FR", true, false, false));
    }
}
