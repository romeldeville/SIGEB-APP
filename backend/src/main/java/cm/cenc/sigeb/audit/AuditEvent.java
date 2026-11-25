package cm.cenc.sigeb.audit;

import cm.cenc.sigeb.shared.security.Role;

import java.util.UUID;

public record AuditEvent(
        UUID utilisateurId,
        Role roleUtilisateur,
        UUID dioceseId,
        UUID paroisseId,
        AuditAction typeAction,
        String entiteCible,
        UUID entiteId,
        String iunConcerne,
        String motif,
        String detailsJson,
        String adresseIp
) {
    public static AuditEvent simple(UUID userId, Role role, AuditAction action, String motif, String ip) {
        return new AuditEvent(userId, role, null, null, action, null, null, null, motif, null, ip);
    }
}
