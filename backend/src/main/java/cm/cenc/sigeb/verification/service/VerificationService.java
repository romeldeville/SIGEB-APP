package cm.cenc.sigeb.verification.service;

import cm.cenc.sigeb.audit.enums.AuditAction;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.audit.service.AuditService;
import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.bapteme.repository.ActeBaptemeRepository;
import cm.cenc.sigeb.carte.entity.CarteEmise;
import cm.cenc.sigeb.carte.enums.StatutCarte;
import cm.cenc.sigeb.carte.repository.CarteEmiseRepository;
import cm.cenc.sigeb.carte.service.QrSignatureService;
import cm.cenc.sigeb.mention.entity.MentionMarginale;
import cm.cenc.sigeb.mention.enums.StatutMention;
import cm.cenc.sigeb.mention.enums.TypeSacrement;
import cm.cenc.sigeb.mention.repository.MentionMarginaleRepository;
import cm.cenc.sigeb.shared.crypto.service.CryptoService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import cm.cenc.sigeb.verification.dto.QrVerifyRequest;
import cm.cenc.sigeb.verification.dto.QrVerifyResponse;
import cm.cenc.sigeb.verification.dto.RechercheNationaleItemResponse;
import cm.cenc.sigeb.verification.dto.RechercheNationaleRequest;
import cm.cenc.sigeb.verification.enums.ResultatVerificationQr;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VerificationService {

    private final CarteEmiseRepository carteRepository;
    private final ActeBaptemeRepository acteRepository;
    private final MentionMarginaleRepository mentionRepository;
    private final QrSignatureService qrSignatureService;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    public VerificationService(
            CarteEmiseRepository carteRepository,
            ActeBaptemeRepository acteRepository,
            MentionMarginaleRepository mentionRepository,
            QrSignatureService qrSignatureService,
            CryptoService cryptoService,
            AuditService auditService
    ) {
        this.carteRepository = carteRepository;
        this.acteRepository = acteRepository;
        this.mentionRepository = mentionRepository;
        this.qrSignatureService = qrSignatureService;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public QrVerifyResponse verifierQr(QrVerifyRequest req, String clientIp) {
        Optional<UserPrincipal> callerOpt = TenantContext.getCurrentUser();
        boolean authentifie = callerOpt.isPresent();
        String ipAudit = authentifie ? clientIp : cryptoService.hashSha256(clientIp != null ? clientIp : "unknown");

        String[] parts = req.qrPayload().trim().split(":");
        if (parts.length != 5 || !"SIGEB".equals(parts[0]) || !"v2".equals(parts[1])) {
            enregistrerScan(callerOpt, null, null, "Format QR invalide", ipAudit);
            return invalide("QR Code non reconnu ou falsifié");
        }

        String kid = parts[2];
        String qrToken = parts[3];
        String sig = parts[4];

        if (!qrSignatureService.verifierSignature(kid, qrToken, sig)) {
            enregistrerScan(callerOpt, null, null, "Signature cryptographique invalide", ipAudit);
            return invalide("Signature cryptographique invalide");
        }

        Optional<CarteEmise> carteOpt = carteRepository.findByQrToken(qrToken);
        if (carteOpt.isEmpty()) {
            enregistrerScan(callerOpt, null, null, "Jeton QR inconnu en base", ipAudit);
            return invalide("Carte introuvable dans le registre national");
        }

        CarteEmise carte = carteOpt.get();
        ActeBapteme acte = carte.getActeBapteme();
        enregistrerScan(callerOpt, carte, acte.getIun(), "Scan QR (" + carte.getStatut() + ")", ipAudit);

        // 1. Carte révoquée ou remplacée par un duplicata
        if (carte.getStatut() == StatutCarte.REVOQUEE || carte.getStatut() == StatutCarte.REMPLACEE) {
            String codeRemplacante = carte.getRemplaceeParCarte() != null ? carte.getRemplaceeParCarte().getCodeCarte() : null;
            return new QrVerifyResponse(
                    false,
                    ResultatVerificationQr.REVOQUEE_OU_REMPLACEE,
                    carte.getMotifRevocation() != null ? carte.getMotifRevocation() : "Carte révoquée ou remplacée",
                    carte.getCodeCarte(),
                    masquerNomSiAnonyme(acte, authentifie),
                    authentifie ? acte.getIun() : null,
                    authentifie ? acte.getParoisse().getNomFr() : null,
                    authentifie ? acte.getDateBapteme() : null,
                    null,
                    List.of(),
                    codeRemplacante
            );
        }

        // 2. Contrôle de fraîcheur (version acte & nombre de mentions inscrites)
        List<MentionMarginale> mentions = mentionRepository.findByActeBaptemeIdAndStatutOrderByDateEvenementAsc(
                acte.getId(), StatutMention.INSCRITE);
        boolean majDispo = !acte.getVersion().equals(carte.getVersionActeAuTirage())
                || mentions.size() != carte.getNbMentionsAuTirage();

        ResultatVerificationQr resultat = majDispo
                ? ResultatVerificationQr.AUTHENTIQUE_MISE_A_JOUR_DISPONIBLE
                : ResultatVerificationQr.AUTHENTIQUE_A_JOUR;

        String msg = majDispo
                ? "Carte authentique — des mises à jour (avenant ou mention marginale) existent dans le registre"
                : "Carte authentique et à jour";

        if (!authentifie) {
            // Réponse minimale pour scan public anonyme (confidentialité canonique)
            return new QrVerifyResponse(
                    true, resultat, msg, carte.getCodeCarte(),
                    masquerNomSiAnonyme(acte, false),
                    null, null, null, null, List.of(), null
            );
        }

        boolean etatLibre = calculerEtatLibre(mentions);
        List<String> mentionsTxt = mentions.stream()
                .map(m -> m.getTypeSacrement() + " — " + m.getDateEvenement() + " (" + m.getLieuEvenement() + ")")
                .toList();

        return new QrVerifyResponse(
                true, resultat, msg, carte.getCodeCarte(),
                acte.getNom() + " " + acte.getPrenoms(),
                acte.getIun(),
                acte.getParoisse().getNomFr(),
                acte.getDateBapteme(),
                etatLibre,
                mentionsTxt,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<RechercheNationaleItemResponse> rechercheNationale(RechercheNationaleRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() == Role.SECRETAIRE && caller.niveau() == cm.cenc.sigeb.shared.security.model.NiveauEcclesial.PAROISSE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "La recherche nationale inter-paroisses est réservée aux Curés, Chanceliers, Secrétariats supérieurs et Admin National");
        }

        List<ActeBapteme> resultats;
        if (req.iun() != null && !req.iun().isBlank()) {
            resultats = acteRepository.findByIun(req.iun().trim().toUpperCase())
                    .filter(a -> a.getStatut() == StatutActe.SCELLE)
                    .map(List::of)
                    .orElse(List.of());
        } else if (req.nom() != null && !req.nom().isBlank() && req.prenoms() != null && !req.prenoms().isBlank() && req.dateNaissance() != null) {
            resultats = acteRepository.findDoublonsPotentiels(req.nom().trim(), req.prenoms().trim(), req.dateNaissance())
                    .stream()
                    .filter(a -> a.getStatut() == StatutActe.SCELLE)
                    .toList();
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CRITERES_INSUFFISANTS",
                    "Fournissez soit un IUN exact, soit le triplet (nom, prénoms, date de naissance)");
        }

        // Traçabilité obligatoire (motif + résultat)
        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                AuditAction.RECHERCHE_NATIONALE, "acte_bapteme", null,
                req.iun() != null ? req.iun().trim().toUpperCase() : null,
                req.motif().trim(), null, clientIp
        ));

        return resultats.stream().map(a -> {
            List<MentionMarginale> mentions = mentionRepository.findByActeBaptemeIdAndStatutOrderByDateEvenementAsc(
                    a.getId(), StatutMention.INSCRITE);
            return new RechercheNationaleItemResponse(
                    a.getId(), a.getIun(), a.getNom(), a.getPrenoms(),
                    a.getDateNaissance(), a.getDateBapteme(),
                    a.getParoisse().getDiocese().getNomFr(),
                    a.getParoisse().getNomFr(),
                    calculerEtatLibre(mentions)
            );
        }).toList();
    }

    private boolean calculerEtatLibre(List<MentionMarginale> mentions) {
        long mariages = mentions.stream().filter(m -> m.getTypeSacrement() == TypeSacrement.MARIAGE).count();
        long nullites = mentions.stream().filter(m -> m.getTypeSacrement() == TypeSacrement.NULLITE_MARIAGE).count();
        boolean ordonneOuVoeux = mentions.stream().anyMatch(m ->
                m.getTypeSacrement() == TypeSacrement.ORDINATION_DIACONAT
                        || m.getTypeSacrement() == TypeSacrement.ORDINATION_PRESBYTERAT
                        || m.getTypeSacrement() == TypeSacrement.VOEUX_PERPETUELS);
        return (mariages <= nullites) && !ordonneOuVoeux;
    }

    private String masquerNomSiAnonyme(ActeBapteme acte, boolean authentifie) {
        if (authentifie) return acte.getNom() + " " + acte.getPrenoms();
        String n = acte.getNom();
        String p = acte.getPrenoms();
        return (n.length() > 2 ? n.substring(0, 2) + "***" : n) + " "
                + (p.length() > 1 ? p.substring(0, 1) + "." : p);
    }

    private QrVerifyResponse invalide(String message) {
        return new QrVerifyResponse(
                false, ResultatVerificationQr.INVALIDE_OU_FALSIFIE, message,
                null, null, null, null, null, null, List.of(), null
        );
    }

    private void enregistrerScan(Optional<UserPrincipal> callerOpt, CarteEmise carte, String iun, String motif, String ipAudit) {
        UserPrincipal caller = callerOpt.orElse(null);
        auditService.enregistrer(new AuditEvent(
                caller != null ? caller.id() : null,
                caller != null ? caller.role() : null,
                caller != null ? caller.dioceseId() : null,
                caller != null ? caller.paroisseId() : null,
                AuditAction.SCAN_QR,
                "carte_emise",
                carte != null ? carte.getId() : null,
                iun,
                motif,
                null,
                ipAudit
        ));
    }
}
