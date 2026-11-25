package cm.cenc.sigeb.referentiel.dto;

import java.util.UUID;

public record ParoisseResponse(
        UUID id,
        UUID dioceseId,
        String dioceseCode,
        String code,
        String typeParoisse,
        String nomFr,
        String nomEn,
        String localite,
        String enteteOfficiel,
        boolean actif
) {}
