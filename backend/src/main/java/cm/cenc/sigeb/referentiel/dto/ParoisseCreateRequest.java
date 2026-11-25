package cm.cenc.sigeb.referentiel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ParoisseCreateRequest(
        @NotNull(message = "Le diocèse est obligatoire")
        UUID dioceseId,

        @NotBlank(message = "Le code paroisse est obligatoire")
        @Size(max = 10)
        @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Le code doit être en majuscules/chiffres (2 à 10 car.)")
        String code,

        @NotBlank(message = "Le type de paroisse est obligatoire")
        @Pattern(regexp = "^(PAROISSE|CATHEDRALE|QUASI_PAROISSE|MISSION)$", message = "Type invalide")
        String typeParoisse,

        @NotBlank(message = "Le nom en français est obligatoire")
        @Size(max = 160)
        String nomFr,

        @NotBlank(message = "Le nom en anglais est obligatoire")
        @Size(max = 160)
        String nomEn,

        @NotBlank(message = "La localité est obligatoire")
        @Size(max = 120)
        String localite,

        @Size(max = 255)
        String enteteOfficiel
) {}
