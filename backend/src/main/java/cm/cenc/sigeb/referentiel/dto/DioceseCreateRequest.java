package cm.cenc.sigeb.referentiel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DioceseCreateRequest(
        @NotNull(message = "La province ecclésiastique est obligatoire")
        UUID provinceId,

        @NotBlank(message = "Le code diocèse est obligatoire")
        @Size(max = 10)
        @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Le code doit être en majuscules/chiffres (2 à 10 car.)")
        String code,

        @NotBlank(message = "Le type de diocèse est obligatoire")
        @Pattern(regexp = "^(DIOCESE|ARCHIDIOCESE)$", message = "Type invalide : DIOCESE ou ARCHIDIOCESE")
        String typeDiocese,

        @NotBlank(message = "Le nom en français est obligatoire")
        @Size(max = 150)
        String nomFr,

        @NotBlank(message = "Le nom en anglais est obligatoire")
        @Size(max = 150)
        String nomEn,

        @NotBlank(message = "Le siège est obligatoire")
        @Size(max = 100)
        String siege
) {}
