package cm.cenc.sigeb.carte.controller;

import cm.cenc.sigeb.carte.dto.CarteEmiseResponse;
import cm.cenc.sigeb.carte.dto.EmissionCarteRequest;
import cm.cenc.sigeb.carte.service.CarteEmiseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Cartes & Duplicatas", description = "Émission originale/duplicata persistée en BD et génération PDF/QR à la volée")
public class CarteController {

    private final CarteEmiseService carteService;

    public CarteController(CarteEmiseService carteService) {
        this.carteService = carteService;
    }

    @GetMapping("/actes/{acteId}/cartes")
    @Operation(summary = "Historique de toutes les cartes/extraits (originale et duplicatas) d'un acte")
    public ResponseEntity<List<CarteEmiseResponse>> lister(@PathVariable UUID acteId) {
        return ResponseEntity.ok(carteService.listerPourActe(acteId));
    }

    @PostMapping("/actes/{acteId}/cartes")
    @Operation(summary = "Émettre une carte originale ou un duplicata (révoque automatiquement la précédente)")
    public ResponseEntity<CarteEmiseResponse> emettre(
            @PathVariable UUID acteId,
            @Valid @RequestBody EmissionCarteRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carteService.emettreCarte(acteId, req, extractClientIp(httpRequest)));
    }

    @GetMapping(value = "/cartes/{carteId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Générer et télécharger le PDF officiel à la volée depuis la base de données")
    public ResponseEntity<byte[]> telechargerPdf(@PathVariable UUID carteId) {
        byte[] pdf = carteService.telechargerPdf(carteId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"carte-bapteme-" + carteId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/cartes/{carteId}/revoquer")
    @Operation(summary = "Révoquer manuellement une carte (perte, vol, fraude)")
    public ResponseEntity<CarteEmiseResponse> revoquer(
            @PathVariable UUID carteId,
            @RequestParam(required = false) String motif,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(carteService.revoquerCarte(carteId, motif, extractClientIp(httpRequest)));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
