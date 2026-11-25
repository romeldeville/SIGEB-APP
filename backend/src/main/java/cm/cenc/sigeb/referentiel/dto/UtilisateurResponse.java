package cm.cenc.sigeb.referentiel.dto;

import cm.cenc.sigeb.shared.security.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UtilisateurResponse(
        UUID id,
        String identifiant,
        String nomComplet,
        String telephone,
        Role role,
        UUID dioceseId,
        UUID paroisseId,
        String languePreferee,
        String statut,
        boolean totpActive,
        Instant creeLe
) {}
