package cm.cenc.sigeb.mention.dto;

import cm.cenc.sigeb.mention.enums.TypeSacrement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MentionCreateRequest(
        @NotBlank(message = "L'IUN de l'acte de baptême est obligatoire")
        String iunActeBapteme,

        @NotNull(message = "Le type de sacrement est obligatoire")
        TypeSacrement typeSacrement,

        @NotNull(message = "La date de l'événement est obligatoire")
        @PastOrPresent(message = "La date ne peut pas être future")
        LocalDate dateEvenement,

        @NotBlank(message = "Le lieu de célébration est obligatoire")
        @Size(max = 160)
        String lieuEvenement,

        @NotBlank(message = "Le ministre célébrant est obligatoire")
        @Size(max = 160)
        String ministreCelebrant,

        @Size(max = 180)
        String conjointNomComplet,

        @Size(max = 80)
        String refRegistreLocal
) {}
