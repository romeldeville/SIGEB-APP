package cm.cenc.sigeb.referentiel.dto;

import cm.cenc.sigeb.shared.security.model.Role;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record MutationRequest(
        @NotNull(message = "Le nouveau rôle est obligatoire")
        Role nouveauRole,
        UUID nouveauDioceseId,
        UUID nouvelleParoisseId,
        LocalDate dateEffet
) {}
