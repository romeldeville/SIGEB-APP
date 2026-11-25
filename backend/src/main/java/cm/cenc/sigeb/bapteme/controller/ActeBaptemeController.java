package cm.cenc.sigeb.bapteme.controller;

import cm.cenc.sigeb.bapteme.dto.*;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.bapteme.service.ActeBaptemeService;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/actes")
@Tag(name = "Actes de Baptême", description = "Saisie, contrôle anti-doublon, validation et scellement canonique IUN")
public class ActeBaptemeController {

    private final ActeBaptemeService acteService;

    public ActeBaptemeController(ActeBaptemeService acteService) {
        this.acteService = acteService;
    }

    @GetMapping("/doublons")
    @Operation(summary = "Vérification anti-doublon avant saisie (nom, prénoms, date de naissance)")
    public ResponseEntity<List<DoublonPotentielResponse>> verifierDoublons(
            @RequestParam String nom,
            @RequestParam String prenoms,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateNaissance
    ) {
        TenantContext.setNationalOverride(true);
        try {
            return ResponseEntity.ok(acteService.verifierDoublons(nom, prenoms, dateNaissance));
        } finally {
            TenantContext.setNationalOverride(false);
        }
    }

    @GetMapping
    @Operation(summary = "Liste des actes de la paroisse (filtrable par statut)")
    public ResponseEntity<List<ActeBaptemeResponse>> lister(@RequestParam(required = false) StatutActe statut) {
        return ResponseEntity.ok(acteService.listerParoisse(statut));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un acte de baptême dans son périmètre")
    public ResponseEntity<ActeBaptemeResponse> consulter(@PathVariable UUID id) {
        return ResponseEntity.ok(acteService.consulter(id));
    }

    @PostMapping
    @Operation(summary = "Saisir un nouvel acte (ou reprise d'archive)")
    public ResponseEntity<ActeBaptemeResponse> creer(
            @Valid @RequestBody ActeBaptemeRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(acteService.creerActe(req, extractClientIp(httpRequest)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un acte non encore scellé")
    public ResponseEntity<ActeBaptemeResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody ActeBaptemeRequest req
    ) {
        return ResponseEntity.ok(acteService.modifierActe(id, req));
    }

    @PostMapping("/{id}/renvoyer")
    @Operation(summary = "Renvoyer un acte au secrétariat pour correction (Curé)")
    public ResponseEntity<ActeBaptemeResponse> renvoyer(
            @PathVariable UUID id,
            @Valid @RequestBody RejetCorrectionRequest req
    ) {
        return ResponseEntity.ok(acteService.renvoyerPourCorrection(id, req));
    }

    @PostMapping("/{id}/sceller")
    @Operation(summary = "Valider et sceller l'acte avec PIN du Curé (génération atomique de l'IUN)")
    public ResponseEntity<ActeBaptemeResponse> sceller(
            @PathVariable UUID id,
            @Valid @RequestBody ScellementRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(acteService.scellerActe(id, req, extractClientIp(httpRequest)));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
