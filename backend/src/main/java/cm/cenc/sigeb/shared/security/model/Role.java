package cm.cenc.sigeb.shared.security.model;

/**
 * Fonction / autorité exercée par l'utilisateur à son {@link NiveauEcclesial}.
 * Un SECRETAIRE peut exister à chacun des 4 niveaux, sans jamais détenir le pouvoir
 * canonique de scellement (CURE) ni d'approbation d'avenant majeur (CHANCELIER).
 */
public enum Role {
    ADMIN_NATIONAL,
    RESPONSABLE_PROVINCE,
    CHANCELIER,
    CURE,
    SECRETAIRE
}
