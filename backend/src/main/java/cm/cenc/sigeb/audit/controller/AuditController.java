package cm.cenc.sigeb.audit.controller;

import cm.cenc.sigeb.audit.dto.IntegriteAuditResponse;
import cm.cenc.sigeb.audit.dto.JournalAuditResponse;
import cm.cenc.sigeb.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Journal d'Audit", description = "Consultation tracée et vérification cryptographique de la chaîne d'audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Consulter le journal d'audit dans le périmètre ecclésial de l'utilisateur")
    public ResponseEntity<List<JournalAuditResponse>> consulter(
            @RequestParam(required = false) String iun,
            @RequestParam(required = false) String typeAction,
            @RequestParam(defaultValue = "50") int limite
    ) {
        return ResponseEntity.ok(auditService.consulterAudit(iun, typeAction, limite));
    }

    @GetMapping("/integrite")
    @Operation(summary = "Vérifier l'intégrité SHA-256 de toute la chaîne d'audit (Admin National)")
    public ResponseEntity<IntegriteAuditResponse> verifierIntegrite() {
        return ResponseEntity.ok(auditService.verifierIntegriteChaine());
    }
}
