package cm.cenc.sigeb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "L'ancien mot de passe est obligatoire")
        String ancienMotDePasse,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 10, message = "Le mot de passe doit contenir au moins 10 caractères")
        String nouveauMotDePasse
) {}
