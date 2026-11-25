package cm.cenc.sigeb.referentiel.dto;

import java.util.UUID;

public record DioceseResponse(
        UUID id,
        UUID provinceId,
        String provinceCode,
        String code,
        String typeDiocese,
        String nomFr,
        String nomEn,
        String siege,
        boolean actif
) {}
