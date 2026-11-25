package cm.cenc.sigeb.verification.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RechercheNationaleItemResponse(
        UUID acteId,
        String iun,
        String nom,
        String prenoms,
        LocalDate dateNaissance,
        LocalDate dateBapteme,
        String dioceseNomFr,
        String paroisseNomFr,
        boolean etatLibreMatrimonial
) {}
