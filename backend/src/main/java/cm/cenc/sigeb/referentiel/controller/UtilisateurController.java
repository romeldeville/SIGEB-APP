package cm.cenc.sigeb.referentiel.controller;

import cm.cenc.sigeb.referentiel.dto.MutationRequest;
import cm.cenc.sigeb.referentiel.dto.UtilisateurCreateRequest;
import cm.cenc.sigeb.referentiel.dto.UtilisateurResponse;
import cm.cenc.sigeb.referentiel.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utilisateurs")
@Tag(name = "Utilisateurs & Affectations", description = "Gestion des comptes ecclésiastiques, mutations et suspensions")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @Operation(summary = "Liste des utilisateurs dans le périmètre de l'utilisateur connecté")
    public ResponseEntity<List<UtilisateurResponse>> lister() {
        return ResponseEntity.ok(utilisateurService.listerUtilisateurs());
    }

    @PostMapping
    @Operation(summary = "Créer un compte utilisateur (RBAC hiérarchique)")
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody UtilisateurCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurService.creerUtilisateur(req));
    }

    @PostMapping("/{id}/muter")
    @Operation(summary = "Muter un prêtre ou chancelier avec historisation de l'affectation et révocation de session")
    public ResponseEntity<UtilisateurResponse> muter(@PathVariable UUID id, @Valid @RequestBody MutationRequest req) {
        return ResponseEntity.ok(utilisateurService.muterUtilisateur(id, req));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Activer ou suspendre un compte utilisateur")
    public ResponseEntity<UtilisateurResponse> changerStatut(@PathVariable UUID id, @RequestParam String statut) {
        return ResponseEntity.ok(utilisateurService.changerStatut(id, statut));
    }
}
