package cm.cenc.sigeb.verification.dto;

import jakarta.validation.constraints.NotBlank;

public record QrVerifyRequest(
        @NotBlank(message = "Le contenu du QR Code est obligatoire")
        String qrPayload
) {}
