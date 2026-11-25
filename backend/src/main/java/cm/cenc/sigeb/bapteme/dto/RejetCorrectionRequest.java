package cm.cenc.sigeb.bapteme.dto;

import jakarta.validation.constraints.NotBlank;

public record RejetCorrectionRequest(
        @NotBlank(message = "Le motif de renvoi pour correction est obligatoire")
        String motifCorrection
) {}
