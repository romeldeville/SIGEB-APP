package cm.cenc.sigeb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpVerifyRequest(
        @NotBlank(message = "Le code TOTP est obligatoire")
        @Pattern(regexp = "\\d{6}", message = "Le code TOTP doit comporter 6 chiffres")
        String code
) {}
