package cm.cenc.sigeb.carte.service;

import cm.cenc.sigeb.audit.enums.AuditAction;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.audit.service.AuditService;
import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.bapteme.repository.ActeBaptemeRepository;
import cm.cenc.sigeb.carte.dto.CarteEmiseResponse;
import cm.cenc.sigeb.carte.dto.EmissionCarteRequest;
import cm.cenc.sigeb.carte.entity.CarteEmise;
import cm.cenc.sigeb.carte.enums.MotifEmission;
import cm.cenc.sigeb.carte.enums.StatutCarte;
import cm.cenc.sigeb.carte.enums.TypeEmission;
import cm.cenc.sigeb.carte.repository.CarteEmiseRepository;
import cm.cenc.sigeb.mention.entity.MentionMarginale;
import cm.cenc.sigeb.mention.enums.StatutMention;
import cm.cenc.sigeb.mention.repository.MentionMarginaleRepository;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.*;

@Service
public class CarteEmiseService {

    public static final String TEMPLATE_VERSION_COURANTE = "v1.0";

    private final CarteEmiseRepository carteRepository;
    private final ActeBaptemeRepository acteRepository;
    private final MentionMarginaleRepository mentionRepository;
    private final ParoisseRepository paroisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final QrSignatureService qrSignatureService;
    private final PdfCarteGeneratorService pdfGeneratorService;
    private final CryptoService cryptoService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CarteEmiseService(
            CarteEmiseRepository carteRepository,
            ActeBaptemeRepository acteRepository,
            MentionMarginaleRepository mentionRepository,
            ParoisseRepository paroisseRepository,
            UtilisateurRepository utilisateurRepository,
            QrSignatureService qrSignatureService,
            PdfCarteGeneratorService pdfGeneratorService,
            CryptoService cryptoService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.carteRepository = carteRepository;
        this.acteRepository = acteRepository;
        this.mentionRepository = mentionRepository;
        this.paroisseRepository = paroisseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.qrSignatureService = qrSignatureService;
        this.pdfGeneratorService = pdfGeneratorService;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CarteEmiseResponse emettreCarte(UUID acteId, EmissionCarteRequest req, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.niveau() != NiveauEcclesial.PAROISSE || (caller.role() != Role.CURE && caller.role() != Role.SECRETAIRE)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul le Curé ou le Secrétaire paroissial peut émettre une carte/extrait");
        }

        ActeBapteme acte = acteRepository.findById(acteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACTE_INTROUVABLE", "Acte introuvable"));
        if (acte.getStatut() != StatutActe.SCELLE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ACTE_NON_SCELLE",
                    "Seul un acte scellé peut donner lieu à l'émission d'une carte ou d'un extrait");
        }
        if (!acte.getParoisse().getId().equals(caller.paroisseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Cet acte appartient à une autre paroisse");
        }

        Paroisse paroisse = paroisseRepository.findById(caller.paroisseId()).orElseThrow();
        Utilisateur operateur = utilisateurRepository.findById(caller.id()).orElseThrow();

        int maxDup = carteRepository.findMaxNumeroDuplicata(acte.getId(), req.formatDocument());
        boolean estDuplicata = maxDup >= 0;
        short numeroDuplicata = (short) (estDuplicata ? maxDup + 1 : 0);
        TypeEmission typeEmission = estDuplicata ? TypeEmission.DUPLICATA : TypeEmission.ORIGINALE;

        if (estDuplicata && req.motifEmission() == MotifEmission.PREMIERE_EMISSION) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MOTIF_DUPLICATA_REQUIS",
                    "Une carte originale existe déjà : indiquez un motif de duplicata (PERTE, VOL, MISE_A_JOUR...)");
        }

        // Révocation préalable de l'ancienne carte ACTIVE du même format (contrainte unique partielle ux_carte_active)
        Optional<CarteEmise> precedenteOpt = carteRepository.findByActeBaptemeIdAndFormatDocumentAndStatut(
                acte.getId(), req.formatDocument(), StatutCarte.ACTIVE);
        if (precedenteOpt.isPresent()) {
            CarteEmise prec = precedenteOpt.get();
            prec.setStatut(StatutCarte.REMPLACEE);
            prec.setRevoqueeLe(Instant.now());
            prec.setMotifRevocation("Remplacée par duplicata n°" + numeroDuplicata + " (" + req.motifEmission() + ")");
            carteRepository.saveAndFlush(prec);
        }

        String langue = req.langue() != null ? req.langue().toUpperCase() : "FR";
        List<MentionMarginale> mentions = mentionRepository.findByActeBaptemeIdAndStatutOrderByDateEvenementAsc(
                acte.getId(), StatutMention.INSCRITE);

        Map<String, Object> snapshot = construireSnapshot(acte, mentions, langue);
        String empreinte = calculerEmpreinteJson(snapshot);
        String qrToken = qrSignatureService.genererTokenOpaque();
        QrSignatureService.SignedQr signed = qrSignatureService.signer(qrToken);
        String codeCarte = genererCodeCarte();

        CarteEmise carte = new CarteEmise();
        carte.setActeBapteme(acte);
        carte.setParoisseEmettrice(paroisse);
        carte.setCodeCarte(codeCarte);
        carte.setFormatDocument(req.formatDocument());
        carte.setLangue(langue);
        carte.setTypeEmission(typeEmission);
        carte.setNumeroDuplicata(numeroDuplicata);
        carte.setMotifEmission(req.motifEmission());
        carte.setVersionActeAuTirage(acte.getVersion());
        carte.setNbMentionsAuTirage((short) mentions.size());
        carte.setDonneesImprimees(snapshot);
        carte.setEmpreinteDonnees(empreinte);
        carte.setTemplateVersion(TEMPLATE_VERSION_COURANTE);
        carte.setQrToken(qrToken);
        carte.setQrKid(signed.kid());
        carte.setQrSignature(signed.signatureCourte());
        carte.setStatut(StatutCarte.ACTIVE);
        carte.setEmisPar(operateur);

        carte = carteRepository.saveAndFlush(carte);

        if (precedenteOpt.isPresent()) {
            CarteEmise prec = precedenteOpt.get();
            prec.setRemplaceeParCarte(carte);
            carteRepository.save(prec);
        }

        String motifAudit = req.motifDetail() != null && !req.motifDetail().isBlank()
                ? req.motifEmission() + " : " + req.motifDetail().trim()
                : req.motifEmission().name();
        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                estDuplicata ? AuditAction.DUPLICATA : AuditAction.EMISSION_CARTE,
                "carte_emise", carte.getId(), acte.getIun(),
                motifAudit, null, clientIp
        ));

        return toResponse(carte);
    }

    @Transactional(readOnly = true)
    public byte[] telechargerPdf(UUID carteId) {
        CarteEmise carte = carteRepository.findById(carteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CARTE_INTROUVABLE", "Carte introuvable"));
        String payload = qrSignatureService.construirePayload(carte.getQrKid(), carte.getQrToken(), carte.getQrSignature());
        return pdfGeneratorService.genererPdf(carte, payload);
    }

    @Transactional
    public CarteEmiseResponse revoquerCarte(UUID carteId, String motif, String clientIp) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.CURE && caller.role() != Role.CHANCELIER && caller.role() != Role.ADMIN_NATIONAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Révocation réservée au Curé, Chancelier ou Admin");
        }
        CarteEmise carte = carteRepository.findById(carteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CARTE_INTROUVABLE", "Carte introuvable"));
        if (caller.role() == Role.CURE && !carte.getParoisseEmettrice().getId().equals(caller.paroisseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Carte hors de votre paroisse");
        }
        carte.setStatut(StatutCarte.REVOQUEE);
        carte.setRevoqueeLe(Instant.now());
        carte.setMotifRevocation(motif != null ? motif.trim() : "Révocation manuelle");
        carte = carteRepository.save(carte);

        auditService.enregistrer(new AuditEvent(
                caller.id(), caller.role(), caller.dioceseId(), caller.paroisseId(),
                AuditAction.REVOCATION_CARTE, "carte_emise", carte.getId(), carte.getActeBapteme().getIun(),
                carte.getMotifRevocation(), null, clientIp
        ));
        return toResponse(carte);
    }

    @Transactional(readOnly = true)
    public List<CarteEmiseResponse> listerPourActe(UUID acteId) {
        return carteRepository.findByActeBaptemeIdOrderByEmisLeDesc(acteId).stream()
                .map(this::toResponse).toList();
    }

    private Map<String, Object> construireSnapshot(ActeBapteme a, List<MentionMarginale> mentions, String langue) {
        boolean en = "EN".equals(langue);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("iun", a.getIun());
        map.put("diocese", en ? a.getParoisse().getDiocese().getNomEn() : a.getParoisse().getDiocese().getNomFr());
        map.put("paroisse", en ? a.getParoisse().getNomEn() : a.getParoisse().getNomFr());
        map.put("nom", a.getNom());
        map.put("prenoms", a.getPrenoms());
        map.put("nomBapteme", a.getNomBapteme());
        map.put("sexe", a.getSexe());
        map.put("dateNaissance", a.getDateNaissance().toString());
        map.put("lieuNaissance", a.getLieuNaissance());
        if (!a.isFiliationConfidentielle()) {
            map.put("nomPere", a.getNomPere());
            map.put("nomMere", a.getNomMere());
        }
        map.put("nomParrain", a.getNomParrain());
        map.put("nomMarraine", a.getNomMarraine());
        map.put("dateBapteme", a.getDateBapteme().toString());
        map.put("lieuBapteme", a.getLieuBapteme());
        map.put("ministreCelebrant", a.getMinistreCelebrant());
        map.put("referenceRegistre", "T" + a.getRegistreTome() + " / P" + a.getRegistrePage() + " / N°" + a.getRegistreNumero());
        List<String> mentionsTxt = mentions.stream()
                .map(m -> m.getTypeSacrement() + " (" + m.getDateEvenement() + ", " + m.getLieuEvenement()
                        + (m.getConjointNomComplet() != null ? " — " + m.getConjointNomComplet() : "") + ")")
                .toList();
        map.put("mentions", mentionsTxt);
        return map;
    }

    private String calculerEmpreinteJson(Map<String, Object> snapshot) {
        try {
            return cryptoService.hashSha256(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            throw new IllegalStateException("Erreur sérialisation snapshot carte", e);
        }
    }

    private String genererCodeCarte() {
        String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "CRT-" + Year.now().getValue() + "-" + hex;
    }

    private CarteEmiseResponse toResponse(CarteEmise c) {
        String qrPayload = qrSignatureService.construirePayload(c.getQrKid(), c.getQrToken(), c.getQrSignature());
        return new CarteEmiseResponse(
                c.getId(), c.getActeBapteme().getId(), c.getActeBapteme().getIun(),
                c.getCodeCarte(), c.getFormatDocument(), c.getLangue(),
                c.getTypeEmission(), c.getNumeroDuplicata(), c.getMotifEmission(),
                c.getVersionActeAuTirage(), c.getNbMentionsAuTirage(), c.getTemplateVersion(),
                qrPayload, c.getStatut(),
                c.getRemplaceeParCarte() != null ? c.getRemplaceeParCarte().getId() : null,
                c.getEmisLe(), c.getDonneesImprimees()
        );
    }
}
