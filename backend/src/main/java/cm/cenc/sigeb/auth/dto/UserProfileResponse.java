package cm.cenc.sigeb.auth.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String identifiant,
        String nomComplet,
        String telephone,
        String languePreferee,
        String role,
        String niveau,
        UUID provinceId,
        String provinceNom,
        UUID dioceseId,
        String dioceseNom,
        UUID paroisseId,
        String paroisseNom,
        String statut,
        boolean totpActive,
        boolean mdpAChanger,
        boolean pinConfigure
) {}
