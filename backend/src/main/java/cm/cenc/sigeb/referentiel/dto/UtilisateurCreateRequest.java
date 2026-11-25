package cm.cenc.sigeb.referentiel.dto;

import cm.cenc.sigeb.shared.security.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UtilisateurCreateRequest(
        @NotBlank(message = "L'identifiant est obligatoire")
        @Size(max = 120)
        String identifiant,

        @NotBlank(message = "Le mot de passe initial est obligatoire")
        @Size(min = 10, message = "Le mot de passe doit contenir au moins 10 caractères")
        String motDePasseInitial,

        @NotBlank(message = "Le nom complet est obligatoire")
        @Size(max = 160)
        String nomComplet,

        @Size(max = 30)
        String telephone,

        @NotNull(message = "Le rôle est obligatoire")
        Role role,

        UUID dioceseId,
        UUID paroisseId,

        @Pattern(regexp = "^(FR|EN)$", message = "Langue invalide : FR ou EN")
        String languePreferee
) {}
