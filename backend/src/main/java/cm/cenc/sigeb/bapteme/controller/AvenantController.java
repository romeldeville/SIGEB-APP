package cm.cenc.sigeb.bapteme.controller;

import cm.cenc.sigeb.bapteme.dto.AvenantCreateRequest;
import cm.cenc.sigeb.bapteme.dto.AvenantResponse;
import cm.cenc.sigeb.bapteme.dto.ScellementRequest;
import cm.cenc.sigeb.bapteme.service.AvenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Avenants de Rectification", description = "Rectifications tracées sur actes scellés (Curé / Chancelier)")
public class AvenantController {

    private final AvenantService avenantService;

    public AvenantController(AvenantService avenantService) {
        this.avenantService = avenantService;
    }

    @GetMapping("/actes/{acteId}/avenants")
    @Operation(summary = "Historique des avenants d'un acte")
    public ResponseEntity<List<AvenantResponse>> lister(@PathVariable UUID acteId) {
        return ResponseEntity.ok(avenantService.listerPourActe(acteId));
    }

    @PostMapping("/actes/{acteId}/avenants")
    @Operation(summary = "Créer un avenant (MINEURE approuvé directement par le Curé, MAJEURE soumis au Chancelier)")
    public ResponseEntity<AvenantResponse> creer(
            @PathVariable UUID acteId,
            @Valid @RequestBody AvenantCreateRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(avenantService.creerAvenant(acteId, req, extractClientIp(httpRequest)));
    }

    @PostMapping("/avenants/{avenantId}/approuver")
    @Operation(summary = "Approuver un avenant majeur avec PIN (Chancelier diocésain)")
    public ResponseEntity<AvenantResponse> approuver(
            @PathVariable UUID avenantId,
            @Valid @RequestBody ScellementRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(avenantService.approuverAvenantMajeur(avenantId, req, extractClientIp(httpRequest)));
    }

    @PostMapping("/avenants/{avenantId}/rejeter")
    @Operation(summary = "Rejeter un avenant majeur (Chancelier diocésain)")
    public ResponseEntity<AvenantResponse> rejeter(@PathVariable UUID avenantId) {
        return ResponseEntity.ok(avenantService.rejeterAvenantMajeur(avenantId));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
