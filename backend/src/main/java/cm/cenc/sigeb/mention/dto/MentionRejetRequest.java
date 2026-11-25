package cm.cenc.sigeb.mention.dto;

import jakarta.validation.constraints.NotBlank;

public record MentionRejetRequest(
        @NotBlank(message = "Le motif de rejet est obligatoire")
        String motifRejet
) {}
