package cm.cenc.sigeb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Le nom complet est obligatoire")
        @Size(min = 2, max = 160, message = "Le nom complet doit contenir entre 2 et 160 caractères")
        String nomComplet,

        @Size(max = 30, message = "Le numéro de téléphone ne doit pas dépasser 30 caractères")
        String telephone,

        @NotBlank(message = "La langue préférée est obligatoire")
        @Pattern(regexp = "^(FR|EN)$", message = "La langue doit être FR ou EN")
        String languePreferee
) {}
