package cm.cenc.sigeb.shared.security.model;

/**
 * Niveau d'affectation territoriale dans la hiérarchie de l'Église Catholique du Cameroun.
 * Combiné avec {@link Role}, il permet de distinguer un secrétariat national (CENC),
 * provincial, diocésain (chancellerie) ou paroissial (paroisse / cathédrale / mission).
 */
public enum NiveauEcclesial {
    NATIONAL,
    PROVINCE,
    DIOCESE,
    PAROISSE
}
