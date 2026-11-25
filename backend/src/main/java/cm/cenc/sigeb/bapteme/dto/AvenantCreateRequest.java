package cm.cenc.sigeb.bapteme.dto;

import cm.cenc.sigeb.bapteme.enums.GraviteAvenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AvenantCreateRequest(
        @NotNull(message = "La gravité de la rectification est obligatoire (MINEURE ou MAJEURE)")
        GraviteAvenant gravite,

        @NotEmpty(message = "Au moins un champ à rectifier doit être fourni")
        Map<String, Object> nouvellesValeurs,

        @NotBlank(message = "Le motif de rectification est obligatoire")
        String motif,

        @Size(max = 180)
        String referenceJustificatif,

        String pinSignature
) {}
