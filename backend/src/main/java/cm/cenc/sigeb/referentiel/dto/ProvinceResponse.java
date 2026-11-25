package cm.cenc.sigeb.referentiel.dto;

import java.util.UUID;

public record ProvinceResponse(
        UUID id,
        String code,
        String nomFr,
        String nomEn
) {}
