package cm.cenc.sigeb.referentiel.dto;

import cm.cenc.sigeb.shared.security.model.NiveauEcclesial;
import cm.cenc.sigeb.shared.security.model.Role;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record MutationRequest(
        @NotNull(message = "Le nouveau rôle est obligatoire")
        Role nouveauRole,

        @NotNull(message = "Le nouveau niveau ecclésial est obligatoire")
        NiveauEcclesial nouveauNiveau,

        UUID nouvelleProvinceId,
        UUID nouveauDioceseId,
        UUID nouvelleParoisseId,
        LocalDate dateEffet
) {}
