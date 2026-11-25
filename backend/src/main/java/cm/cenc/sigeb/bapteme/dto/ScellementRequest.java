package cm.cenc.sigeb.bapteme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ScellementRequest(
        @NotBlank(message = "Le code PIN de signature du Curé est obligatoire")
        @Pattern(regexp = "\\d{4,6}", message = "Le PIN doit comporter entre 4 et 6 chiffres")
        String pinSignature
) {}
