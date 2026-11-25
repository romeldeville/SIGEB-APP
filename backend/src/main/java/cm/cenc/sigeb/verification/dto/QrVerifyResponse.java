package cm.cenc.sigeb.verification.dto;

import cm.cenc.sigeb.verification.enums.ResultatVerificationQr;
import java.time.LocalDate;
import java.util.List;

public record QrVerifyResponse(
        boolean valide,
        ResultatVerificationQr resultat,
        String message,
        String codeCarte,
        String nomAffiche,
        String iun,
        String paroisseOrigine,
        LocalDate dateBapteme,
        Boolean etatLibreMatrimonial,
        List<String> mentionsInscrites,
        String remplaceeParCodeCarte
) {}
