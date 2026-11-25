package cm.cenc.sigeb.statistique.dto;

import java.util.Map;

public record StatistiqueGlobaleResponse(
        String niveau,
        long totalActesScelles,
        long totalActesEnAttente,
        long totalBrouillonsOuACorriger,
        long totalCartesOriginales,
        long totalDuplicatas,
        Map<String, Long> mentionsParType,
        Map<Integer, Long> baptemesParAnnee
) {}
