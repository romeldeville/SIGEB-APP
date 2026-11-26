package cm.cenc.sigeb.bapteme.service;

import cm.cenc.sigeb.audit.enums.AuditAction;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.audit.service.AuditService;
import cm.cenc.sigeb.bapteme.dto.*;
import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.bapteme.repository.ActeBaptemeRepository;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.ParoisseRepository;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;
import cm.cenc.sigeb.shared.crypto.service.CryptoService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.NiveauEcclesial;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ActeBaptemeService {

    private final ActeBaptemeRepository acteRepository;
    private final ParoisseRepository paroisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final IunGeneratorService iunGeneratorService;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    public ActeBaptemeService(
            ActeBaptemeRepository acteRepository,
            ParoisseRepository paroisseRepository,
            UtilisateurRepository utilisateurRepository,
            IunGeneratorService iunGeneratorService,
            CryptoService cryptoService,
            AuditService auditService
    ) {
        this.acteRepository = acteRepository;
        this.paroisseRepository = paroisseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.iunGeneratorService = iunGeneratorService;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DoublonPotentielResponse> verifierDoublons(String nom, String prenoms, LocalDate dateNaissance) {
        return acteRepository.findDoublonsPotentiels(nom.trim(), prenoms.trim(), dateNaissance).stream()
                .map(a -> new DoublonPotentielResponse(
                        a.getId(), a.getIun(), a.getNom(), a.getPrenoms(),
                        a.getDateNaissance(), a.getDateBapteme(),
                        a.getParoisse().getNomFr(), a.getStatut()
                ))
                .toList();
    }

    @Transactional
    public ActeBaptemeResponse creerActe(ActeBaptemeRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.niveau() != NiveauEcclesial.PAROISSE || (caller.role() != Role.CURE && caller.role() != Role.SECRETAIRE)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul le Curé ou le Secrétaire paroissial peut saisir un acte de baptême");
        }
        validerReglesCanoniques(req);

        Paroisse paroisse = paroisseRepository.findById(caller.paroisseId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PAROISSE_INTROUVABLE", "Paroisse introuvable"));
        Utilisateur operateur = utilisateurRepository.findById(caller.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        if (acteRepository.existsByParoisseIdAndRegistreTomeAndRegistrePageAndRegistreNumero(
                paroisse.getId(), req.registreTome().trim(), req.registrePage().trim(), req.registreNumero().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "REFERENCE_REGISTRE_EXISTANTE",
                    "Cette référence de registre papier (Tome / Page / N°) existe déjà dans cette paroisse");
        }

        ActeBapteme acte = new ActeBapteme();
        acte.setParoisse(paroisse);
        acte.setSaisiPar(operateur);
        appliquerChamps(acte, req);
        acte.setStatut(req.soumettrePourValidation() ? StatutActe.EN_ATTENTE_VALIDATION : StatutActe.BROUILLON);

        acte = acteRepository.save(acte);

        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                AuditAction.CREATION_ACTE, "acte_bapteme", acte.getId(), null,
                "Saisie acte (" + acte.getOrigine() + ")", null, clientIp
        ));

        return toResponse(acte);
    }

    @Transactional
    public ActeBaptemeResponse modifierActe(UUID acteId, ActeBaptemeRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        ActeBapteme acte = chargerActeDansPerimetre(acteId, caller);

        if (acte.getStatut() == StatutActe.SCELLE) {
            throw new BusinessException(HttpStatus.CONFLICT, "ACTE_SCELLE_IMMUABLE",
                    "Un acte scellé est immuable. Utilisez une demande d'avenant de rectification.");
        }
        if (caller.niveau() != NiveauEcclesial.PAROISSE || (caller.role() != Role.CURE && caller.role() != Role.SECRETAIRE)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Action réservée à la paroisse");
        }

        validerReglesCanoniques(req);
        appliquerChamps(acte, req);
        if (req.soumettrePourValidation()) {
            acte.setStatut(StatutActe.EN_ATTENTE_VALIDATION);
            acte.setMotifCorrection(null);
        }

        return toResponse(acteRepository.save(acte));
    }

    @Transactional
    public ActeBaptemeResponse renvoyerPourCorrection(UUID acteId, RejetCorrectionRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CURE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Seul le Curé peut renvoyer un acte pour correction");
        }
        ActeBapteme acte = chargerActeDansPerimetre(acteId, caller);
        if (acte.getStatut() == StatutActe.SCELLE) {
            throw new BusinessException(HttpStatus.CONFLICT, "ACTE_DEJA_SCELLE", "Impossible de renvoyer un acte déjà scellé");
        }

        acte.setStatut(StatutActe.A_CORRIGER);
        acte.setMotifCorrection(req.motifCorrection().trim());
        return toResponse(acteRepository.save(acte));
    }

    @Transactional
    public ActeBaptemeResponse scellerActe(UUID acteId, ScellementRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CURE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul le Curé de la paroisse peut valider et sceller un acte de baptême");
        }

        ActeBapteme acte = chargerActeDansPerimetre(acteId, caller);
        if (acte.getStatut() == StatutActe.SCELLE) {
            throw new BusinessException(HttpStatus.CONFLICT, "ACTE_DEJA_SCELLE", "Cet acte est déjà scellé");
        }

        Utilisateur cure = utilisateurRepository.findById(caller.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Curé introuvable"));

        if (cure.getPinSignatureHash() == null || !cryptoService.matchesPassword(req.pinSignature(), cure.getPinSignatureHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "PIN_SIGNATURE_INVALIDE",
                    "Code PIN de signature invalide ou non configuré");
        }

        String iun = iunGeneratorService.genererIun(acte.getParoisse(), acte.getDateBapteme().getYear());
        acte.setIun(iun);
        acte.setStatut(StatutActe.SCELLE);
        acte.setScellePar(cure);
        acte.setScelleLe(Instant.now());
        acte.setMotifCorrection(null);
        acte.setEmpreinteScellement(calculerEmpreinteCanonique(acte));

        acte = acteRepository.saveAndFlush(acte);

        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                AuditAction.SCELLEMENT, "acte_bapteme", acte.getId(), iun,
                "Scellement canonique de l'acte", null, clientIp
        ));

        return toResponse(acte);
    }

    @Transactional(readOnly = true)
    public ActeBaptemeResponse consulter(UUID acteId) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        return toResponse(chargerActeDansPerimetre(acteId, caller));
    }

    @Transactional(readOnly = true)
    public List<ActeBaptemeResponse> listerParoisse(StatutActe statut) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        List<ActeBapteme> actes = switch (caller.niveau()) {
            case NATIONAL -> statut != null
                    ? acteRepository.findByStatutOrderByCreeLeDesc(statut)
                    : acteRepository.findAllByOrderByCreeLeDesc();
            case PROVINCE -> statut != null
                    ? acteRepository.findByParoisseDioceseProvinceIdAndStatutOrderByCreeLeDesc(caller.provinceId(), statut)
                    : acteRepository.findByParoisseDioceseProvinceIdOrderByCreeLeDesc(caller.provinceId());
            case DIOCESE -> statut != null
                    ? acteRepository.findByParoisseDioceseIdAndStatutOrderByCreeLeDesc(caller.dioceseId(), statut)
                    : acteRepository.findByParoisseDioceseIdOrderByCreeLeDesc(caller.dioceseId());
            case PAROISSE -> {
                if (caller.paroisseId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "PAROISSE_REQUISE",
                            "Aucune paroisse affectée à ce compte");
                }
                yield statut != null
                        ? acteRepository.findByParoisseIdAndStatutOrderByCreeLeDesc(caller.paroisseId(), statut)
                        : acteRepository.findByParoisseIdOrderByCreeLeDesc(caller.paroisseId());
            }
        };
        return actes.stream().map(this::toResponse).toList();
    }

    public String calculerEmpreinteCanonique(ActeBapteme a) {
        String payload = String.join("|",
                safe(a.getIun()),
                a.getParoisse().getId().toString(),
                safe(a.getNom()),
                safe(a.getPrenoms()),
                safe(a.getNomBapteme()),
                safe(a.getSexe()),
                a.getDateNaissance().toString(),
                safe(a.getLieuNaissance()),
                safe(a.getNomPere()),
                safe(a.getNomMere()),
                safe(a.getNomParrain()),
                safe(a.getNomMarraine()),
                a.getDateBapteme().toString(),
                safe(a.getLieuBapteme()),
                safe(a.getMinistreCelebrant()),
                safe(a.getRegistreTome()),
                safe(a.getRegistrePage()),
                safe(a.getRegistreNumero())
        );
        return cryptoService.hashSha256(payload);
    }

    private void validerReglesCanoniques(ActeBaptemeRequest req) {
        if (req.dateBapteme().isBefore(req.dateNaissance())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DATES_INCOHERENTES",
                    "La date de baptême ne peut pas être antérieure à la date de naissance");
        }
        boolean hasParrain = req.nomParrain() != null && !req.nomParrain().isBlank();
        boolean hasMarraine = req.nomMarraine() != null && !req.nomMarraine().isBlank();
        if (!hasParrain && !hasMarraine) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PARRAINAGE_REQUIS",
                    "Au moins un parrain ou une marraine doit être renseigné (Droit canonique)");
        }
    }

    private void appliquerChamps(ActeBapteme acte, ActeBaptemeRequest req) {
        acte.setOrigine(req.origine());
        acte.setNom(req.nom().trim());
        acte.setPrenoms(req.prenoms().trim());
        acte.setNomBapteme(trimOrNull(req.nomBapteme()));
        acte.setSexe(req.sexe().trim().toUpperCase());
        acte.setDateNaissance(req.dateNaissance());
        acte.setLieuNaissance(req.lieuNaissance().trim());
        acte.setNomPere(trimOrNull(req.nomPere()));
        acte.setNomMere(trimOrNull(req.nomMere()));
        acte.setFiliationConfidentielle(req.filiationConfidentielle());
        acte.setNomParrain(trimOrNull(req.nomParrain()));
        acte.setNomMarraine(trimOrNull(req.nomMarraine()));
        acte.setParoisseParrainage(trimOrNull(req.paroisseParrainage()));
        acte.setDateBapteme(req.dateBapteme());
        acte.setLieuBapteme(req.lieuBapteme().trim());
        acte.setMinistreCelebrant(req.ministreCelebrant().trim());
        acte.setRegistreTome(req.registreTome().trim());
        acte.setRegistrePage(req.registrePage().trim());
        acte.setRegistreNumero(req.registreNumero().trim());
    }

    private ActeBapteme chargerActeDansPerimetre(UUID acteId, UserPrincipal caller) {
        ActeBapteme acte = acteRepository.findById(acteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACTE_INTROUVABLE", "Acte de baptême introuvable"));
        boolean autorise = switch (caller.niveau()) {
            case NATIONAL -> true;
            case PROVINCE -> acte.getParoisse().getDiocese().getProvince().getId().equals(caller.provinceId());
            case DIOCESE -> acte.getParoisse().getDiocese().getId().equals(caller.dioceseId());
            case PAROISSE -> acte.getParoisse().getId().equals(caller.paroisseId());
        };
        if (!autorise) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Cet acte est hors de votre périmètre ecclésial");
        }
        return acte;
    }

    private ActeBaptemeResponse toResponse(ActeBapteme a) {
        return new ActeBaptemeResponse(
                a.getId(), a.getIun(), a.getParoisse().getId(), a.getParoisse().getNomFr(),
                a.getOrigine(), a.getStatut(), a.getVersion(),
                a.getNom(), a.getPrenoms(), a.getNomBapteme(), a.getSexe(),
                a.getDateNaissance(), a.getLieuNaissance(),
                a.getNomPere(), a.getNomMere(), a.isFiliationConfidentielle(),
                a.getNomParrain(), a.getNomMarraine(), a.getParoisseParrainage(),
                a.getDateBapteme(), a.getLieuBapteme(), a.getMinistreCelebrant(),
                a.getRegistreTome(), a.getRegistrePage(), a.getRegistreNumero(),
                a.getMotifCorrection(), a.getScelleLe(), a.getCreeLe()
        );
    }

    private static String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
