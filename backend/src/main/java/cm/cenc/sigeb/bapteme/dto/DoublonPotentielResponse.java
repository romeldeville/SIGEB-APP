package cm.cenc.sigeb.bapteme.dto;

import cm.cenc.sigeb.bapteme.enums.StatutActe;
import java.time.LocalDate;
import java.util.UUID;

public record DoublonPotentielResponse(
        UUID id,
        String iun,
        String nom,
        String prenoms,
        LocalDate dateNaissance,
        LocalDate dateBapteme,
        String paroisseNomFr,
        StatutActe statut
) {}
