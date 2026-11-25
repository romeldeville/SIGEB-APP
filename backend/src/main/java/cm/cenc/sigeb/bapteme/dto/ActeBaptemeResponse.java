package cm.cenc.sigeb.bapteme.dto;

import cm.cenc.sigeb.bapteme.enums.OrigineActe;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActeBaptemeResponse(
        UUID id,
        String iun,
        UUID paroisseId,
        String paroisseNomFr,
        OrigineActe origine,
        StatutActe statut,
        Integer version,
        String nom,
        String prenoms,
        String nomBapteme,
        String sexe,
        LocalDate dateNaissance,
        String lieuNaissance,
        String nomPere,
        String nomMere,
        boolean filiationConfidentielle,
        String nomParrain,
        String nomMarraine,
        String paroisseParrainage,
        LocalDate dateBapteme,
        String lieuBapteme,
        String ministreCelebrant,
        String registreTome,
        String registrePage,
        String registreNumero,
        String motifCorrection,
        Instant scelleLe,
        Instant creeLe
) {}
