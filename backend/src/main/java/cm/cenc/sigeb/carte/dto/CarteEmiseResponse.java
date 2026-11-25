package cm.cenc.sigeb.carte.dto;

import cm.cenc.sigeb.carte.enums.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CarteEmiseResponse(
        UUID id,
        UUID acteBaptemeId,
        String iun,
        String codeCarte,
        FormatDocument formatDocument,
        String langue,
        TypeEmission typeEmission,
        short numeroDuplicata,
        MotifEmission motifEmission,
        Integer versionActeAuTirage,
        short nbMentionsAuTirage,
        String templateVersion,
        String qrPayload,
        StatutCarte statut,
        UUID remplaceeParCarteId,
        Instant emisLe,
        Map<String, Object> donneesImprimees
) {}
