package cm.cenc.sigeb.verification.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record RechercheNationaleRequest(
        String iun,
        String nom,
        String prenoms,
        LocalDate dateNaissance,

        @NotBlank(message = "Le motif canonique de recherche nationale est obligatoire pour la traçabilité")
        String motif
) {}
