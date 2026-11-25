package cm.cenc.sigeb.bapteme.dto;

import cm.cenc.sigeb.bapteme.enums.OrigineActe;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ActeBaptemeRequest(
        @NotNull(message = "L'origine de l'acte est obligatoire")
        OrigineActe origine,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 120)
        String nom,

        @NotBlank(message = "Les prénoms sont obligatoires")
        @Size(max = 120)
        String prenoms,

        @Size(max = 120)
        String nomBapteme,

        @NotBlank(message = "Le sexe est obligatoire")
        @Pattern(regexp = "^[MF]$", message = "Le sexe doit être M ou F")
        String sexe,

        @NotNull(message = "La date de naissance est obligatoire")
        @PastOrPresent(message = "La date de naissance ne peut pas être future")
        LocalDate dateNaissance,

        @NotBlank(message = "Le lieu de naissance est obligatoire")
        @Size(max = 150)
        String lieuNaissance,

        @Size(max = 160)
        String nomPere,

        @Size(max = 160)
        String nomMere,

        boolean filiationConfidentielle,

        @Size(max = 160)
        String nomParrain,

        @Size(max = 160)
        String nomMarraine,

        @Size(max = 160)
        String paroisseParrainage,

        @NotNull(message = "La date de baptême est obligatoire")
        @PastOrPresent(message = "La date de baptême ne peut pas être future")
        LocalDate dateBapteme,

        @NotBlank(message = "Le lieu de baptême est obligatoire")
        @Size(max = 160)
        String lieuBapteme,

        @NotBlank(message = "Le ministre célébrant est obligatoire")
        @Size(max = 160)
        String ministreCelebrant,

        @NotBlank(message = "Le tome du registre papier est obligatoire")
        @Size(max = 20)
        String registreTome,

        @NotBlank(message = "La page du registre papier est obligatoire")
        @Size(max = 20)
        String registrePage,

        @NotBlank(message = "Le numéro d'ordre du registre papier est obligatoire")
        @Size(max = 30)
        String registreNumero,

        boolean soumettrePourValidation
) {}
