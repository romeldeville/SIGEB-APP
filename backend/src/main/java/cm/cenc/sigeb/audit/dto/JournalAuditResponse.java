package cm.cenc.sigeb.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record JournalAuditResponse(
        Long id,
        Instant horodatage,
        UUID utilisateurId,
        String roleUtilisateur,
        UUID provinceId,
        UUID dioceseId,
        UUID paroisseId,
        String typeAction,
        String entiteCible,
        UUID entiteId,
        String iunConcerne,
        String motif,
        String adresseIp,
        String hashLigne
) {}
