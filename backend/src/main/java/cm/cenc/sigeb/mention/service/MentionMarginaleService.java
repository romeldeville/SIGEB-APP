package cm.cenc.sigeb.mention.service;

import cm.cenc.sigeb.audit.enums.AuditAction;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.audit.service.AuditService;
import cm.cenc.sigeb.bapteme.dto.ScellementRequest;
import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.bapteme.repository.ActeBaptemeRepository;
import cm.cenc.sigeb.mention.dto.MentionCreateRequest;
import cm.cenc.sigeb.mention.dto.MentionRejetRequest;
import cm.cenc.sigeb.mention.dto.MentionResponse;
import cm.cenc.sigeb.mention.entity.MentionMarginale;
import cm.cenc.sigeb.mention.enums.StatutMention;
import cm.cenc.sigeb.mention.enums.TypeSacrement;
import cm.cenc.sigeb.mention.repository.MentionMarginaleRepository;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.ParoisseRepository;
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
import java.util.List;
import java.util.UUID;

@Service
public class MentionMarginaleService {

    private final MentionMarginaleRepository mentionRepository;
    private final ActeBaptemeRepository acteRepository;
    private final ParoisseRepository paroisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    public MentionMarginaleService(
            MentionMarginaleRepository mentionRepository,
            ActeBaptemeRepository acteRepository,
            ParoisseRepository paroisseRepository,
            UtilisateurRepository utilisateurRepository,
            CryptoService cryptoService,
            AuditService auditService
    ) {
        this.mentionRepository = mentionRepository;
        this.acteRepository = acteRepository;
        this.paroisseRepository = paroisseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
    }

    @Transactional
    public MentionResponse declarerMention(MentionCreateRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CURE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul le Curé de la paroisse célébrante peut déclarer une mention marginale");
        }

        ActeBapteme acte = acteRepository.findByIun(req.iunActeBapteme().trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACTE_INTROUVABLE", "Aucun acte trouvé pour cet IUN"));
        if (acte.getStatut() != StatutActe.SCELLE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ACTE_NON_SCELLE", "Impossible d'ajouter une mention sur un acte non scellé");
        }
        if ((req.typeSacrement() == TypeSacrement.MARIAGE || req.typeSacrement() == TypeSacrement.NULLITE_MARIAGE)
                && (req.conjointNomComplet() == null || req.conjointNomComplet().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CONJOINT_REQUIS",
                    "Le nom complet du conjoint est obligatoire pour un mariage ou une nullité");
        }

        Paroisse paroisseDeclarante = paroisseRepository.findById(caller.paroisseId()).orElseThrow();
        Utilisateur cureDeclarant = utilisateurRepository.findById(caller.id()).orElseThrow();

        MentionMarginale m = new MentionMarginale();
        m.setActeBapteme(acte);
        m.setParoisseDeclarante(paroisseDeclarante);
        m.setTypeSacrement(req.typeSacrement());
        m.setDateEvenement(req.dateEvenement());
        m.setLieuEvenement(req.lieuEvenement().trim());
        m.setMinistreCelebrant(req.ministreCelebrant().trim());
        m.setConjointNomComplet(req.conjointNomComplet() != null ? req.conjointNomComplet().trim() : null);
        m.setRefRegistreLocal(req.refRegistreLocal() != null ? req.refRegistreLocal().trim() : null);
        m.setDeclarePar(cureDeclarant);

        // Si la paroisse célébrante est la paroisse d'origine, la mention est inscrite directement
        if (acte.getParoisse().getId().equals(paroisseDeclarante.getId())) {
            m.setStatut(StatutMention.INSCRITE);
            m.setValidePar(cureDeclarant);
            m.setValideLe(Instant.now());
        } else {
            m.setStatut(StatutMention.EN_ATTENTE_ORIGINE);
        }

        m = mentionRepository.save(m);

        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                AuditAction.MENTION_DECLAREE, "mention_marginale", m.getId(), acte.getIun(),
                "Déclaration " + req.typeSacrement(), null, clientIp
        ));

        return toResponse(m);
    }

    @Transactional
    public MentionResponse validerMention(UUID mentionId, ScellementRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CURE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul le Curé de la paroisse d'origine peut inscrire la mention en marge");
        }

        MentionMarginale m = mentionRepository.findById(mentionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MENTION_INTROUVABLE", "Mention introuvable"));
        if (!m.getActeBapteme().getParoisse().getId().equals(caller.paroisseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE",
                    "Seule la paroisse d'origine du baptême peut valider cette mention");
        }
        if (m.getStatut() != StatutMention.EN_ATTENTE_ORIGINE) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATUT_INVALIDE", "Cette mention a déjà été traitée");
        }

        Utilisateur cure = utilisateurRepository.findById(caller.id()).orElseThrow();
        if (cure.getPinSignatureHash() == null || !cryptoService.matchesPassword(req.pinSignature(), cure.getPinSignatureHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "PIN_SIGNATURE_INVALIDE", "PIN de signature invalide");
        }

        m.setStatut(StatutMention.INSCRITE);
        m.setValidePar(cure);
        m.setValideLe(Instant.now());
        m = mentionRepository.save(m);

        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                AuditAction.MENTION_VALIDEE, "mention_marginale", m.getId(), m.getActeBapteme().getIun(),
                "Inscription mention " + m.getTypeSacrement(), null, clientIp
        ));

        return toResponse(m);
    }

    @Transactional
    public MentionResponse rejeterMention(UUID mentionId, MentionRejetRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CURE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Action réservée au Curé d'origine");
        }
        MentionMarginale m = mentionRepository.findById(mentionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MENTION_INTROUVABLE", "Mention introuvable"));
        if (!m.getActeBapteme().getParoisse().getId().equals(caller.paroisseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Hors de votre paroisse");
        }
        if (m.getStatut() != StatutMention.EN_ATTENTE_ORIGINE) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATUT_INVALIDE", "Cette mention a déjà été traitée");
        }
        Utilisateur cure = utilisateurRepository.findById(caller.id()).orElseThrow();
        m.setStatut(StatutMention.REJETEE);
        m.setMotifRejet(req.motifRejet().trim());
        m.setValidePar(cure);
        m.setValideLe(Instant.now());
        return toResponse(mentionRepository.save(m));
    }

    @Transactional(readOnly = true)
    public List<MentionResponse> listerBoiteReceptionParoisseOrigine() {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        List<MentionMarginale> mentions = switch (caller.niveau()) {
            case NATIONAL -> mentionRepository.findByStatutOrderByDeclareLeDesc(StatutMention.EN_ATTENTE_ORIGINE);
            case PROVINCE -> mentionRepository.findByActeBaptemeParoisseDioceseProvinceIdAndStatutOrderByDeclareLeDesc(
                    caller.provinceId(), StatutMention.EN_ATTENTE_ORIGINE);
            case DIOCESE -> mentionRepository.findByActeBaptemeParoisseDioceseIdAndStatutOrderByDeclareLeDesc(
                    caller.dioceseId(), StatutMention.EN_ATTENTE_ORIGINE);
            case PAROISSE -> {
                if (caller.paroisseId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "PAROISSE_REQUISE", "Paroisse requise");
                }
                yield mentionRepository.findByActeBaptemeParoisseIdAndStatutOrderByDeclareLeDesc(
                        caller.paroisseId(), StatutMention.EN_ATTENTE_ORIGINE);
            }
        };
        return mentions.stream().map(this::toResponse).toList();
    }

    private MentionResponse toResponse(MentionMarginale m) {
        return new MentionResponse(
                m.getId(), m.getActeBapteme().getId(), m.getActeBapteme().getIun(),
                m.getParoisseDeclarante().getId(), m.getParoisseDeclarante().getNomFr(),
                m.getTypeSacrement(), m.getDateEvenement(), m.getLieuEvenement(),
                m.getMinistreCelebrant(), m.getConjointNomComplet(), m.getRefRegistreLocal(),
                m.getStatut(), m.getDeclareLe(), m.getValideLe(), m.getMotifRejet()
        );
    }
}
