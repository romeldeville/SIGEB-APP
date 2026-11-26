package cm.cenc.sigeb.referentiel.service;

import cm.cenc.sigeb.auth.repository.RefreshTokenRepository;
import cm.cenc.sigeb.referentiel.dto.MutationRequest;
import cm.cenc.sigeb.referentiel.dto.UtilisateurCreateRequest;
import cm.cenc.sigeb.referentiel.dto.UtilisateurResponse;
import cm.cenc.sigeb.referentiel.entity.*;
import cm.cenc.sigeb.referentiel.repository.*;
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
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationUtilisateurRepository affectationRepository;
    private final ProvinceRepository provinceRepository;
    private final DioceseRepository dioceseRepository;
    private final ParoisseRepository paroisseRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CryptoService cryptoService;

    private record PerimetreResolu(ProvinceEcclesiastique province, Diocese diocese, Paroisse paroisse) {}

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            AffectationUtilisateurRepository affectationRepository,
            ProvinceRepository provinceRepository,
            DioceseRepository dioceseRepository,
            ParoisseRepository paroisseRepository,
            RefreshTokenRepository refreshTokenRepository,
            CryptoService cryptoService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.affectationRepository = affectationRepository;
        this.provinceRepository = provinceRepository;
        this.dioceseRepository = dioceseRepository;
        this.paroisseRepository = paroisseRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public UtilisateurResponse creerUtilisateur(UtilisateurCreateRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        verifierCoherenceRoleNiveau(req.role(), req.niveau());
        PerimetreResolu p = resoudrePerimetre(req.niveau(), req.provinceId(), req.dioceseId(), req.paroisseId());
        verifierDroitGestion(caller, req.role(), req.niveau(), p);

        String identifiant = req.identifiant().trim().toLowerCase();
        if (utilisateurRepository.findByIdentifiant(identifiant).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "IDENTIFIANT_EXISTANT", "Cet identifiant existe déjà");
        }

        Utilisateur u = new Utilisateur();
        u.setIdentifiant(identifiant);
        u.setMotDePasseHash(cryptoService.hashPassword(req.motDePasseInitial()));
        u.setNomComplet(req.nomComplet().trim());
        u.setTelephone(req.telephone());
        u.setRole(req.role());
        u.setNiveau(req.niveau());
        u.setProvince(p.province());
        u.setDiocese(p.diocese());
        u.setParoisse(p.paroisse());
        u.setLanguePreferee(req.languePreferee() != null ? req.languePreferee() : "FR");
        u.setStatut("ACTIF");
        u.setMdpAChanger(true);

        u = utilisateurRepository.save(u);

        AffectationUtilisateur aff = new AffectationUtilisateur();
        aff.setUtilisateur(u);
        aff.setRole(u.getRole());
        aff.setNiveau(u.getNiveau());
        aff.setProvince(p.province());
        aff.setDiocese(p.diocese());
        aff.setParoisse(p.paroisse());
        aff.setDateDebut(LocalDate.now());
        affectationRepository.save(aff);

        return toResponse(u);
    }

    @Transactional
    public UtilisateurResponse muterUtilisateur(UUID utilisateurId, MutationRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.ADMIN_NATIONAL && caller.role() != Role.CHANCELIER && caller.role() != Role.RESPONSABLE_PROVINCE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Mutation non autorisée pour ce rôle");
        }

        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        PerimetreResolu actuel = new PerimetreResolu(u.getProvince(), u.getDiocese(), u.getParoisse());
        verifierDroitGestion(caller, u.getRole(), u.getNiveau(), actuel);

        verifierCoherenceRoleNiveau(req.nouveauRole(), req.nouveauNiveau());
        PerimetreResolu p = resoudrePerimetre(req.nouveauNiveau(), req.nouvelleProvinceId(), req.nouveauDioceseId(), req.nouvelleParoisseId());
        verifierDroitGestion(caller, req.nouveauRole(), req.nouveauNiveau(), p);

        LocalDate dateEffet = req.dateEffet() != null ? req.dateEffet() : LocalDate.now();
        affectationRepository.findByUtilisateurIdAndDateFinIsNull(u.getId()).ifPresent(courante -> {
            courante.setDateFin(dateEffet);
            affectationRepository.save(courante);
        });

        AffectationUtilisateur nouvelle = new AffectationUtilisateur();
        nouvelle.setUtilisateur(u);
        nouvelle.setRole(req.nouveauRole());
        nouvelle.setNiveau(req.nouveauNiveau());
        nouvelle.setProvince(p.province());
        nouvelle.setDiocese(p.diocese());
        nouvelle.setParoisse(p.paroisse());
        nouvelle.setDateDebut(dateEffet);
        affectationRepository.save(nouvelle);

        u.setRole(req.nouveauRole());
        u.setNiveau(req.nouveauNiveau());
        u.setProvince(p.province());
        u.setDiocese(p.diocese());
        u.setParoisse(p.paroisse());
        utilisateurRepository.save(u);

        refreshTokenRepository.revoquerTousPourUtilisateur(u.getId(), Instant.now());
        return toResponse(u);
    }

    @Transactional
    public UtilisateurResponse changerStatut(UUID utilisateurId, String nouveauStatut) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() == Role.SECRETAIRE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Un secrétaire ne peut pas suspendre de compte");
        }
        if (!"ACTIF".equals(nouveauStatut) && !"SUSPENDU".equals(nouveauStatut)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "STATUT_INVALIDE", "Statut attendu : ACTIF ou SUSPENDU");
        }

        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        if (caller.role() == Role.CURE && (u.getRole() != Role.SECRETAIRE || u.getParoisse() == null || !caller.paroisseId().equals(u.getParoisse().getId()))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Un Curé ne peut gérer que les secrétaires de sa paroisse");
        }
        if (caller.role() == Role.CHANCELIER && (u.getDiocese() == null || !caller.dioceseId().equals(u.getDiocese().getId()))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Utilisateur hors de votre diocèse");
        }
        if (caller.role() == Role.RESPONSABLE_PROVINCE && (u.getProvince() == null || !caller.provinceId().equals(u.getProvince().getId()))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Utilisateur hors de votre province");
        }

        u.setStatut(nouveauStatut);
        utilisateurRepository.save(u);
        if ("SUSPENDU".equals(nouveauStatut)) {
            refreshTokenRepository.revoquerTousPourUtilisateur(u.getId(), Instant.now());
        }
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> listerUtilisateurs() {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        return utilisateurRepository.findAll().stream()
                .filter(u -> switch (caller.niveau()) {
                    case NATIONAL -> true;
                    case PROVINCE -> u.getProvince() != null && caller.provinceId().equals(u.getProvince().getId());
                    case DIOCESE -> u.getDiocese() != null && caller.dioceseId().equals(u.getDiocese().getId());
                    case PAROISSE -> u.getParoisse() != null && caller.paroisseId().equals(u.getParoisse().getId());
                })
                .map(this::toResponse)
                .toList();
    }

    private void verifierCoherenceRoleNiveau(Role role, NiveauEcclesial niveau) {
        boolean valide = switch (role) {
            case ADMIN_NATIONAL -> niveau == NiveauEcclesial.NATIONAL;
            case RESPONSABLE_PROVINCE -> niveau == NiveauEcclesial.PROVINCE;
            case CHANCELIER -> niveau == NiveauEcclesial.DIOCESE;
            case CURE -> niveau == NiveauEcclesial.PAROISSE;
            case SECRETAIRE -> true; // Autorisé à tous les niveaux (National, Province, Diocèse, Paroisse)
        };
        if (!valide) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ROLE_NIVEAU_INCOHERENT",
                    "Le rôle " + role + " n'est pas compatible avec le niveau " + niveau);
        }
    }

    private PerimetreResolu resoudrePerimetre(NiveauEcclesial niveau, UUID provinceId, UUID dioceseId, UUID paroisseId) {
        return switch (niveau) {
            case NATIONAL -> new PerimetreResolu(null, null, null);
            case PROVINCE -> {
                if (provinceId == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "PROVINCE_REQUISE", "La province est obligatoire au niveau PROVINCE");
                }
                ProvinceEcclesiastique prov = provinceRepository.findById(provinceId)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROVINCE_INTROUVABLE", "Province introuvable"));
                yield new PerimetreResolu(prov, null, null);
            }
            case DIOCESE -> {
                if (dioceseId == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "DIOCESE_REQUIS", "Le diocèse est obligatoire au niveau DIOCESE");
                }
                Diocese dio = dioceseRepository.findById(dioceseId)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DIOCESE_INTROUVABLE", "Diocèse introuvable"));
                yield new PerimetreResolu(dio.getProvince(), dio, null);
            }
            case PAROISSE -> {
                if (paroisseId == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "PAROISSE_REQUISE", "La paroisse est obligatoire au niveau PAROISSE");
                }
                Paroisse par = paroisseRepository.findById(paroisseId)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PAROISSE_INTROUVABLE", "Paroisse introuvable"));
                if (dioceseId != null && !par.getDiocese().getId().equals(dioceseId)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "PAROISSE_DIOCESE_INCOHERENT",
                            "Cette paroisse n'appartient pas au diocèse indiqué");
                }
                yield new PerimetreResolu(par.getDiocese().getProvince(), par.getDiocese(), par);
            }
        };
    }

    private void verifierDroitGestion(UserPrincipal caller, Role targetRole, NiveauEcclesial targetNiveau, PerimetreResolu p) {
        switch (caller.role()) {
            case ADMIN_NATIONAL -> { /* tous droits */ }
            case RESPONSABLE_PROVINCE -> {
                if (targetNiveau == NiveauEcclesial.NATIONAL || targetRole == Role.RESPONSABLE_PROVINCE) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Action hors autorité provinciale");
                }
                if (p.province() == null || !p.province().getId().equals(caller.provinceId())) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Hors de votre province ecclésiastique");
                }
            }
            case CHANCELIER -> {
                if (targetNiveau == NiveauEcclesial.NATIONAL || targetNiveau == NiveauEcclesial.PROVINCE || targetRole == Role.CHANCELIER) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                            "Un Chancelier ne peut créer que des Secrétaires diocésains, Curés ou Secrétaires paroissiaux");
                }
                if (p.diocese() == null || !p.diocese().getId().equals(caller.dioceseId())) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Hors de votre diocèse");
                }
            }
            case CURE -> {
                if (targetRole != Role.SECRETAIRE || targetNiveau != NiveauEcclesial.PAROISSE) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                            "Un Curé ne peut créer que des Secrétaires paroissiaux");
                }
                if (p.paroisse() == null || !p.paroisse().getId().equals(caller.paroisseId())) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Hors de votre paroisse");
                }
            }
            case SECRETAIRE -> throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Un secrétaire ne peut pas créer ou muter de comptes");
        }
    }

    private UtilisateurResponse toResponse(Utilisateur u) {
        return new UtilisateurResponse(
                u.getId(), u.getIdentifiant(), u.getNomComplet(), u.getTelephone(),
                u.getRole(), u.getNiveau(),
                u.getProvince() != null ? u.getProvince().getId() : null,
                u.getDiocese() != null ? u.getDiocese().getId() : null,
                u.getParoisse() != null ? u.getParoisse().getId() : null,
                u.getLanguePreferee(), u.getStatut(), u.isTotpActive(), u.getCreeLe()
        );
    }
}
