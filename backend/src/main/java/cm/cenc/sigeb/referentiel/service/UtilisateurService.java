package cm.cenc.sigeb.referentiel.service;

import cm.cenc.sigeb.auth.repository.RefreshTokenRepository;
import cm.cenc.sigeb.referentiel.dto.MutationRequest;
import cm.cenc.sigeb.referentiel.dto.UtilisateurCreateRequest;
import cm.cenc.sigeb.referentiel.dto.UtilisateurResponse;
import cm.cenc.sigeb.referentiel.entity.AffectationUtilisateur;
import cm.cenc.sigeb.referentiel.entity.Diocese;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.AffectationUtilisateurRepository;
import cm.cenc.sigeb.referentiel.repository.DioceseRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationUtilisateurRepository affectationRepository;
    private final DioceseRepository dioceseRepository;
    private final ParoisseRepository paroisseRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CryptoService cryptoService;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            AffectationUtilisateurRepository affectationRepository,
            DioceseRepository dioceseRepository,
            ParoisseRepository paroisseRepository,
            RefreshTokenRepository refreshTokenRepository,
            CryptoService cryptoService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.affectationRepository = affectationRepository;
        this.dioceseRepository = dioceseRepository;
        this.paroisseRepository = paroisseRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public UtilisateurResponse creerUtilisateur(UtilisateurCreateRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        verifierDroitCreation(caller, req.role(), req.dioceseId(), req.paroisseId());

        String identifiant = req.identifiant().trim().toLowerCase();
        if (utilisateurRepository.findByIdentifiant(identifiant).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "IDENTIFIANT_EXISTANT", "Cet identifiant existe déjà");
        }

        Diocese diocese = resolveDiocese(req.role(), req.dioceseId());
        Paroisse paroisse = resolveParoisse(req.role(), diocese, req.paroisseId());

        Utilisateur u = new Utilisateur();
        u.setIdentifiant(identifiant);
        u.setMotDePasseHash(cryptoService.hashPassword(req.motDePasseInitial()));
        u.setNomComplet(req.nomComplet().trim());
        u.setTelephone(req.telephone());
        u.setRole(req.role());
        u.setDiocese(diocese);
        u.setParoisse(paroisse);
        u.setLanguePreferee(req.languePreferee() != null ? req.languePreferee() : "FR");
        u.setStatut("ACTIF");
        u.setMdpAChanger(true);

        u = utilisateurRepository.save(u);

        AffectationUtilisateur aff = new AffectationUtilisateur();
        aff.setUtilisateur(u);
        aff.setRole(u.getRole());
        aff.setDiocese(diocese);
        aff.setParoisse(paroisse);
        aff.setDateDebut(LocalDate.now());
        affectationRepository.save(aff);

        return toResponse(u);
    }

    @Transactional
    public UtilisateurResponse muterUtilisateur(UUID utilisateurId, MutationRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.ADMIN_NATIONAL && caller.role() != Role.CHANCELIER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Mutation réservée à l'Admin National ou au Chancelier");
        }

        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        verifierDroitCreation(caller, req.nouveauRole(), req.nouveauDioceseId(), req.nouvelleParoisseId());

        Diocese nouveauDiocese = resolveDiocese(req.nouveauRole(), req.nouveauDioceseId());
        Paroisse nouvelleParoisse = resolveParoisse(req.nouveauRole(), nouveauDiocese, req.nouvelleParoisseId());
        LocalDate dateEffet = req.dateEffet() != null ? req.dateEffet() : LocalDate.now();

        // Clôturer l'affectation en cours sans altérer l'historique
        affectationRepository.findByUtilisateurIdAndDateFinIsNull(u.getId()).ifPresent(courante -> {
            courante.setDateFin(dateEffet);
            affectationRepository.save(courante);
        });

        // Ouvrir la nouvelle affectation
        AffectationUtilisateur nouvelle = new AffectationUtilisateur();
        nouvelle.setUtilisateur(u);
        nouvelle.setRole(req.nouveauRole());
        nouvelle.setDiocese(nouveauDiocese);
        nouvelle.setParoisse(nouvelleParoisse);
        nouvelle.setDateDebut(dateEffet);
        affectationRepository.save(nouvelle);

        u.setRole(req.nouveauRole());
        u.setDiocese(nouveauDiocese);
        u.setParoisse(nouvelleParoisse);
        utilisateurRepository.save(u);

        // Révocation immédiate des sessions existantes pour forcer le renouvellement du périmètre JWT
        refreshTokenRepository.revoquerTousPourUtilisateur(u.getId(), Instant.now());

        return toResponse(u);
    }

    @Transactional
    public UtilisateurResponse changerStatut(UUID utilisateurId, String nouveauStatut) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() == Role.SECRETAIRE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Action non autorisée");
        }
        if (!"ACTIF".equals(nouveauStatut) && !"SUSPENDU".equals(nouveauStatut)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "STATUT_INVALIDE", "Statut attendu : ACTIF ou SUSPENDU");
        }

        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Utilisateur introuvable"));

        if (caller.role() == Role.CURE && (u.getRole() != Role.SECRETAIRE || !caller.paroisseId().equals(u.getParoisse().getId()))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Un Curé ne peut gérer que les secrétaires de sa paroisse");
        }
        if (caller.role() == Role.CHANCELIER && (u.getDiocese() == null || !caller.dioceseId().equals(u.getDiocese().getId()))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Utilisateur hors de votre diocèse");
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
                .filter(u -> {
                    if (caller.role() == Role.ADMIN_NATIONAL) return true;
                    if (caller.role() == Role.CHANCELIER) {
                        return u.getDiocese() != null && caller.dioceseId().equals(u.getDiocese().getId());
                    }
                    if (caller.role() == Role.CURE) {
                        return u.getParoisse() != null && caller.paroisseId().equals(u.getParoisse().getId());
                    }
                    return u.getId().equals(caller.id());
                })
                .map(this::toResponse)
                .toList();
    }

    private void verifierDroitCreation(UserPrincipal caller, Role targetRole, UUID dioceseId, UUID paroisseId) {
        switch (caller.role()) {
            case ADMIN_NATIONAL -> { /* tous droits */ }
            case CHANCELIER -> {
                if (targetRole == Role.ADMIN_NATIONAL || targetRole == Role.CHANCELIER) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                            "Un Chancelier ne peut créer que des Curés ou Secrétaires");
                }
                if (dioceseId == null || !dioceseId.equals(caller.dioceseId())) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Hors de votre diocèse");
                }
            }
            case CURE -> {
                if (targetRole != Role.SECRETAIRE) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                            "Un Curé ne peut créer que des Secrétaires paroissiaux");
                }
                if (paroisseId == null || !paroisseId.equals(caller.paroisseId())) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE", "Hors de votre paroisse");
                }
            }
            case SECRETAIRE -> throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Action interdite");
        }
    }

    private Diocese resolveDiocese(Role role, UUID dioceseId) {
        if (role == Role.ADMIN_NATIONAL) return null;
        if (dioceseId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DIOCESE_REQUIS", "Le diocèse est obligatoire pour ce rôle");
        }
        return dioceseRepository.findById(dioceseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DIOCESE_INTROUVABLE", "Diocèse introuvable"));
    }

    private Paroisse resolveParoisse(Role role, Diocese diocese, UUID paroisseId) {
        if (role == Role.ADMIN_NATIONAL || role == Role.CHANCELIER) return null;
        if (paroisseId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PAROISSE_REQUISE", "La paroisse est obligatoire pour ce rôle");
        }
        Paroisse p = paroisseRepository.findById(paroisseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PAROISSE_INTROUVABLE", "Paroisse introuvable"));
        if (!p.getDiocese().getId().equals(diocese.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PAROISSE_DIOCESE_INCOHERENT",
                    "Cette paroisse n'appartient pas au diocèse indiqué");
        }
        return p;
    }

    private UtilisateurResponse toResponse(Utilisateur u) {
        return new UtilisateurResponse(
                u.getId(), u.getIdentifiant(), u.getNomComplet(), u.getTelephone(), u.getRole(),
                u.getDiocese() != null ? u.getDiocese().getId() : null,
                u.getParoisse() != null ? u.getParoisse().getId() : null,
                u.getLanguePreferee(), u.getStatut(), u.isTotpActive(), u.getCreeLe()
        );
    }
}
