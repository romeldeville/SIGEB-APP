package cm.cenc.sigeb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePinRequest(
        @NotBlank(message = "Le mot de passe actuel est requis pour changer le PIN")
        String motDePasse,

        @NotBlank(message = "Le code PIN est obligatoire")
        @Pattern(regexp = "\\d{4,6}", message = "Le PIN doit comporter entre 4 et 6 chiffres")
        String pin
) {}
