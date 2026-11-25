package cm.cenc.sigeb.mention.dto;

import cm.cenc.sigeb.mention.enums.StatutMention;
import cm.cenc.sigeb.mention.enums.TypeSacrement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MentionResponse(
        UUID id,
        UUID acteBaptemeId,
        String iun,
        UUID paroisseDeclaranteId,
        String paroisseDeclaranteNomFr,
        TypeSacrement typeSacrement,
        LocalDate dateEvenement,
        String lieuEvenement,
        String ministreCelebrant,
        String conjointNomComplet,
        String refRegistreLocal,
        StatutMention statut,
        Instant declareLe,
        Instant valideLe,
        String motifRejet
) {}
