package cm.cenc.sigeb.mention.controller;

import cm.cenc.sigeb.bapteme.dto.ScellementRequest;
import cm.cenc.sigeb.mention.dto.MentionCreateRequest;
import cm.cenc.sigeb.mention.dto.MentionRejetRequest;
import cm.cenc.sigeb.mention.dto.MentionResponse;
import cm.cenc.sigeb.mention.service.MentionMarginaleService;
import cm.cenc.sigeb.shared.security.context.TenantContext;
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
@RequestMapping("/api/v1/mentions")
@Tag(name = "Mentions Marginales", description = "Déclaration inter-paroisses (Paroisse B) et inscription en marge (Paroisse A)")
public class MentionMarginaleController {

    private final MentionMarginaleService mentionService;

    public MentionMarginaleController(MentionMarginaleService mentionService) {
        this.mentionService = mentionService;
    }

    @GetMapping("/boite-reception")
    @Operation(summary = "Mentions en attente de validation par le Curé de la paroisse d'origine")
    public ResponseEntity<List<MentionResponse>> boiteReception() {
        return ResponseEntity.ok(mentionService.listerBoiteReceptionParoisseOrigine());
    }

    @PostMapping
    @Operation(summary = "Déclarer une mention marginale (Confirmation, Mariage, Ordination, Vœux...)")
    public ResponseEntity<MentionResponse> declarer(
            @Valid @RequestBody MentionCreateRequest req,
            HttpServletRequest httpRequest
    ) {
        TenantContext.setNationalOverride(true);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mentionService.declarerMention(req, extractClientIp(httpRequest)));
        } finally {
            TenantContext.setNationalOverride(false);
        }
    }

    @PostMapping("/{id}/valider")
    @Operation(summary = "Valider et inscrire en marge avec PIN (Curé de la paroisse d'origine)")
    public ResponseEntity<MentionResponse> valider(
            @PathVariable UUID id,
            @Valid @RequestBody ScellementRequest req,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(mentionService.validerMention(id, req, extractClientIp(httpRequest)));
    }

    @PostMapping("/{id}/rejeter")
    @Operation(summary = "Rejeter une déclaration de mention marginale avec motif obligatoire")
    public ResponseEntity<MentionResponse> rejeter(
            @PathVariable UUID id,
            @Valid @RequestBody MentionRejetRequest req
    ) {
        return ResponseEntity.ok(mentionService.rejeterMention(id, req));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
