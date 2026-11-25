package cm.cenc.sigeb.audit.dto;

import java.time.Instant;

public record IntegriteAuditResponse(
        boolean chaineValide,
        long nombreEntreesVerifiees,
        Long premiereRuptureId,
        Instant verifieLe
) {}
