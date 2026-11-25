package cm.cenc.sigeb.verification.controller;

import cm.cenc.sigeb.verification.dto.QrVerifyRequest;
import cm.cenc.sigeb.verification.dto.QrVerifyResponse;
import cm.cenc.sigeb.verification.dto.RechercheNationaleItemResponse;
import cm.cenc.sigeb.verification.dto.RechercheNationaleRequest;
import cm.cenc.sigeb.verification.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Vérification QR & Recherche Nationale", description = "Contrôle cryptographique des cartes et consultation inter-paroisses tracée")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/qr/verify")
    @Operation(summary = "Vérifier l'authenticité d'une carte par QR Code (réponse minimale si anonyme, complète si authentifié)")
    public ResponseEntity<QrVerifyResponse> verifierQr(
            @Valid @RequestBody QrVerifyRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(verificationService.verifierQr(req, extractClientIp(httpRequest)));
    }

    @PostMapping("/verification/recherche-nationale")
    @Operation(summary = "Recherche nationale tracée d'un acte scellé avec motif obligatoire (Curé / Chancelier / Admin)")
    public ResponseEntity<List<RechercheNationaleItemResponse>> rechercheNationale(
            @Valid @RequestBody RechercheNationaleRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(verificationService.rechercheNationale(req, extractClientIp(httpRequest)));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
