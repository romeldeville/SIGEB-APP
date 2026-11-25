package cm.cenc.sigeb.bapteme.service;

import cm.cenc.sigeb.audit.enums.AuditAction;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.audit.service.AuditService;
import cm.cenc.sigeb.bapteme.dto.AvenantCreateRequest;
import cm.cenc.sigeb.bapteme.dto.AvenantResponse;
import cm.cenc.sigeb.bapteme.dto.ScellementRequest;
import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.bapteme.entity.AvenantRectification;
import cm.cenc.sigeb.bapteme.enums.GraviteAvenant;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.bapteme.enums.StatutAvenant;
import cm.cenc.sigeb.bapteme.repository.ActeBaptemeRepository;
import cm.cenc.sigeb.bapteme.repository.AvenantRectificationRepository;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;
import cm.cenc.sigeb.shared.crypto.service.CryptoService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AvenantService {

    private final ActeBaptemeRepository acteRepository;
    private final AvenantRectificationRepository avenantRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ActeBaptemeService acteBaptemeService;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    public AvenantService(
            ActeBaptemeRepository acteRepository,
            AvenantRectificationRepository avenantRepository,
            UtilisateurRepository utilisateurRepository,
            ActeBaptemeService acteBaptemeService,
            CryptoService cryptoService,
            AuditService auditService
    ) {
        this.acteRepository = acteRepository;
        this.avenantRepository = avenantRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.acteBaptemeService = acteBaptemeService;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
    }

    @Transactional
    public AvenantResponse creerAvenant(UUID acteId, AvenantCreateRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CURE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul le Curé peut initier un avenant de rectification");
        }

        ActeBapteme acte = acteRepository.findById(acteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACTE_INTROUVABLE", "Acte introuvable"));

        if (!acte.getParoisse().getId().equals(caller.paroisseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Acte hors de votre paroisse");
        }
        if (acte.getStatut() != StatutActe.SCELLE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ACTE_NON_SCELLE",
                    "Les avenants ne s'appliquent qu'aux actes scellés");
        }
        if (avenantRepository.existsByActeBaptemeIdAndStatut(acte.getId(), StatutAvenant.EN_ATTENTE_CHANCELIER)) {
            throw new BusinessException(HttpStatus.CONFLICT, "AVENANT_EN_COURS",
                    "Un avenant majeur est déjà en attente de validation du Chancelier pour cet acte");
        }

        Utilisateur cure = utilisateurRepository.findById(caller.id()).orElseThrow();
        Map<String, Object> anciennes = extraireValeursActuelles(acte, req.nouvellesValeurs());

        AvenantRectification avenant = new AvenantRectification();
        avenant.setActeBapteme(acte);
        avenant.setVersionCible(acte.getVersion() + 1);
        avenant.setGravite(req.gravite());
        avenant.setAnciennesValeurs(anciennes);
        avenant.setNouvellesValeurs(req.nouvellesValeurs());
        avenant.setMotif(req.motif().trim());
        avenant.setReferenceJustificatif(req.referenceJustificatif());
        avenant.setDemandePar(cure);

        if (req.gravite() == GraviteAvenant.MINEURE) {
            verifierPin(cure, req.pinSignature());
            appliquerRectifications(acte, req.nouvellesValeurs());
            acte.setEmpreinteScellement(acteBaptemeService.calculerEmpreinteCanonique(acte));
            acteRepository.saveAndFlush(acte);

            avenant.setStatut(StatutAvenant.APPROUVE);
            avenant.setValidePar(cure);
            avenant.setValideLe(Instant.now());
            avenant = avenantRepository.save(avenant);

            auditService.enregistrer(new AuditEvent(
                    caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                    AuditAction.RECTIFICATION, "avenant_rectification", avenant.getId(), acte.getIun(),
                    req.motif().trim(), null, clientIp
            ));
        } else {
            avenant.setStatut(StatutAvenant.EN_ATTENTE_CHANCELIER);
            avenant = avenantRepository.save(avenant);
        }

        return toResponse(avenant);
    }

    @Transactional
    public AvenantResponse approuverAvenantMajeur(UUID avenantId, ScellementRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CHANCELIER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul le Chancelier diocésain peut approuver un avenant majeur");
        }

        AvenantRectification avenant = avenantRepository.findById(avenantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AVENANT_INTROUVABLE", "Avenant introuvable"));
        ActeBapteme acte = avenant.getActeBapteme();

        if (!acte.getParoisse().getDiocese().getId().equals(caller.dioceseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Cet avenant est hors de votre diocèse");
        }
        if (avenant.getStatut() != StatutAvenant.EN_ATTENTE_CHANCELIER) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATUT_INVALIDE", "Cet avenant a déjà été traité");
        }

        Utilisateur chancelier = utilisateurRepository.findById(caller.id()).orElseThrow();
        verifierPin(chancelier, req.pinSignature());

        appliquerRectifications(acte, avenant.getNouvellesValeurs());
        acte.setEmpreinteScellement(acteBaptemeService.calculerEmpreinteCanonique(acte));
        acteRepository.saveAndFlush(acte);

        avenant.setStatut(StatutAvenant.APPROUVE);
        avenant.setValidePar(chancelier);
        avenant.setValideLe(Instant.now());
        avenant = avenantRepository.save(avenant);

        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), acte.getParoisse().getId(),
                AuditAction.RECTIFICATION, "avenant_rectification", avenant.getId(), acte.getIun(),
                avenant.getMotif(), null, clientIp
        ));

        return toResponse(avenant);
    }

    @Transactional
    public AvenantResponse rejeterAvenantMajeur(UUID avenantId) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CHANCELIER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Action réservée au Chancelier");
        }
        AvenantRectification avenant = avenantRepository.findById(avenantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AVENANT_INTROUVABLE", "Avenant introuvable"));
        if (!avenant.getActeBapteme().getParoisse().getDiocese().getId().equals(caller.dioceseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Hors de votre diocèse");
        }
        if (avenant.getStatut() != StatutAvenant.EN_ATTENTE_CHANCELIER) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATUT_INVALIDE", "Cet avenant a déjà été traité");
        }
        Utilisateur chancelier = utilisateurRepository.findById(caller.id()).orElseThrow();
        avenant.setStatut(StatutAvenant.REJETE);
        avenant.setValidePar(chancelier);
        avenant.setValideLe(Instant.now());
        return toResponse(avenantRepository.save(avenant));
    }

    @Transactional(readOnly = true)
    public List<AvenantResponse> listerPourActe(UUID acteId) {
        return avenantRepository.findByActeBaptemeIdOrderByDemandeLeDesc(acteId).stream()
                .map(this::toResponse).toList();
    }

    private void verifierPin(Utilisateur user, String pin) {
        if (pin == null || user.getPinSignatureHash() == null || !cryptoService.matchesPassword(pin, user.getPinSignatureHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "PIN_SIGNATURE_INVALIDE",
                    "Code PIN de signature invalide ou non configuré");
        }
    }

    private Map<String, Object> extraireValeursActuelles(ActeBapteme acte, Map<String, Object> nouvelles) {
        Map<String, Object> anciennes = new LinkedHashMap<>();
        for (String champ : nouvelles.keySet()) {
            Object val = switch (champ) {
                case "nom" -> acte.getNom();
                case "prenoms" -> acte.getPrenoms();
                case "nomBapteme" -> acte.getNomBapteme();
                case "lieuNaissance" -> acte.getLieuNaissance();
                case "dateNaissance" -> acte.getDateNaissance().toString();
                case "nomPere" -> acte.getNomPere();
                case "nomMere" -> acte.getNomMere();
                case "nomParrain" -> acte.getNomParrain();
                case "nomMarraine" -> acte.getNomMarraine();
                case "lieuBapteme" -> acte.getLieuBapteme();
                case "ministreCelebrant" -> acte.getMinistreCelebrant();
                default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "CHAMP_NON_RECTIFIABLE",
                        "Champ non rectifiable par avenant : " + champ);
            };
            anciennes.put(champ, val);
        }
        return anciennes;
    }

    private void appliquerRectifications(ActeBapteme acte, Map<String, Object> nouvelles) {
        for (var entry : nouvelles.entrySet()) {
            String val = entry.getValue() != null ? entry.getValue().toString().trim() : null;
            switch (entry.getKey()) {
                case "nom" -> acte.setNom(val);
                case "prenoms" -> acte.setPrenoms(val);
                case "nomBapteme" -> acte.setNomBapteme(val);
                case "lieuNaissance" -> acte.setLieuNaissance(val);
                case "dateNaissance" -> acte.setDateNaissance(LocalDate.parse(val));
                case "nomPere" -> acte.setNomPere(val);
                case "nomMere" -> acte.setNomMere(val);
                case "nomParrain" -> acte.setNomParrain(val);
                case "nomMarraine" -> acte.setNomMarraine(val);
                case "lieuBapteme" -> acte.setLieuBapteme(val);
                case "ministreCelebrant" -> acte.setMinistreCelebrant(val);
                default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "CHAMP_NON_RECTIFIABLE",
                        "Champ non rectifiable : " + entry.getKey());
            }
        }
    }

    private AvenantResponse toResponse(AvenantRectification a) {
        return new AvenantResponse(
                a.getId(), a.getActeBapteme().getId(), a.getActeBapteme().getIun(),
                a.getVersionCible(), a.getGravite(), a.getAnciennesValeurs(), a.getNouvellesValeurs(),
                a.getMotif(), a.getReferenceJustificatif(), a.getStatut(), a.getDemandeLe(), a.getValideLe()
        );
    }
}
