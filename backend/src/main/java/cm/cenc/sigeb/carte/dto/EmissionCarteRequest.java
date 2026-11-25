package cm.cenc.sigeb.carte.dto;

import cm.cenc.sigeb.carte.enums.FormatDocument;
import cm.cenc.sigeb.carte.enums.MotifEmission;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EmissionCarteRequest(
        @NotNull(message = "Le format du document est obligatoire (CARTE ou EXTRAIT_A4)")
        FormatDocument formatDocument,

        @Pattern(regexp = "^(FR|EN)$", message = "Langue invalide : FR ou EN")
        String langue,

        @NotNull(message = "Le motif d'émission est obligatoire")
        MotifEmission motifEmission,

        String motifDetail
) {}
