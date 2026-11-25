package cm.cenc.sigeb.bapteme.dto;

import cm.cenc.sigeb.bapteme.enums.GraviteAvenant;
import cm.cenc.sigeb.bapteme.enums.StatutAvenant;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AvenantResponse(
        UUID id,
        UUID acteBaptemeId,
        String iun,
        Integer versionCible,
        GraviteAvenant gravite,
        Map<String, Object> anciennesValeurs,
        Map<String, Object> nouvellesValeurs,
        String motif,
        String referenceJustificatif,
        StatutAvenant statut,
        Instant demandeLe,
        Instant valideLe
) {}
