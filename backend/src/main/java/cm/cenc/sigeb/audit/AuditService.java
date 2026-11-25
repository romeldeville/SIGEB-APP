package cm.cenc.sigeb.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        enregistrer(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(AuditEvent event) {
        try {
            jdbcTemplate.update("""
                INSERT INTO journal_audit (
                    utilisateur_id, role_utilisateur, diocese_id, paroisse_id,
                    type_action, entite_cible, entite_id, iun_concerne,
                    motif, details_json, adresse_ip
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
                    event.utilisateurId(),
                    event.roleUtilisateur() != null ? event.roleUtilisateur().name() : "ANONYME",
                    event.dioceseId(),
                    event.paroisseId(),
                    event.typeAction().name(),
                    event.entiteCible(),
                    event.entiteId(),
                    event.iunConcerne(),
                    event.motif(),
                    event.detailsJson(),
                    event.adresseIp()
            );
        } catch (Exception e) {
            log.error("Échec écriture journal d'audit : {}", e.getMessage(), e);
            throw e;
        }
    }
}
