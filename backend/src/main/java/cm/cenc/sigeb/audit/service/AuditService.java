package cm.cenc.sigeb.audit.service;

import cm.cenc.sigeb.audit.dto.IntegriteAuditResponse;
import cm.cenc.sigeb.audit.dto.JournalAuditResponse;
import cm.cenc.sigeb.audit.event.AuditEvent;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            UUID provinceId = TenantContext.getProvinceId();
            jdbcTemplate.update("""
                INSERT INTO journal_audit (
                    utilisateur_id, role_utilisateur, province_id, diocese_id, paroisse_id,
                    type_action, entite_cible, entite_id, iun_concerne,
                    motif, details_json, adresse_ip
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
                    event.utilisateurId(),
                    event.roleUtilisateur() != null ? event.roleUtilisateur().name() : "ANONYME",
                    provinceId,
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

    @Transactional(readOnly = true)
    public List<JournalAuditResponse> consulterAudit(String iun, String typeAction, int limite) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() == Role.SECRETAIRE && caller.niveau().name().equals("PAROISSE")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "La consultation du journal d'audit est réservée au Curé et aux niveaux supérieurs");
        }

        StringBuilder sql = new StringBuilder("""
            SELECT id, horodatage, utilisateur_id, role_utilisateur, province_id, diocese_id, paroisse_id,
                   type_action, entite_cible, entite_id, iun_concerne, motif, adresse_ip, hash_ligne
            FROM journal_audit WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();

        switch (caller.niveau()) {
            case NATIONAL -> { /* accès complet */ }
            case PROVINCE -> {
                sql.append(" AND province_id = ?");
                params.add(caller.provinceId());
            }
            case DIOCESE -> {
                sql.append(" AND diocese_id = ?");
                params.add(caller.dioceseId());
            }
            case PAROISSE -> {
                sql.append(" AND paroisse_id = ?");
                params.add(caller.paroisseId());
            }
        }

        if (iun != null && !iun.isBlank()) {
            sql.append(" AND iun_concerne = ?");
            params.add(iun.trim().toUpperCase());
        }
        if (typeAction != null && !typeAction.isBlank()) {
            sql.append(" AND type_action = ?");
            params.add(typeAction.trim().toUpperCase());
        }

        int safeLimit = Math.min(Math.max(limite, 1), 200);
        sql.append(" ORDER BY id DESC LIMIT ").append(safeLimit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("horodatage");
            return new JournalAuditResponse(
                    rs.getLong("id"),
                    ts != null ? ts.toInstant() : null,
                    rs.getObject("utilisateur_id", UUID.class),
                    rs.getString("role_utilisateur"),
                    rs.getObject("province_id", UUID.class),
                    rs.getObject("diocese_id", UUID.class),
                    rs.getObject("paroisse_id", UUID.class),
                    rs.getString("type_action"),
                    rs.getString("entite_cible"),
                    rs.getObject("entite_id", UUID.class),
                    rs.getString("iun_concerne"),
                    rs.getString("motif"),
                    rs.getString("adresse_ip"),
                    rs.getString("hash_ligne")
            );
        }, params.toArray());
    }

    @Transactional(readOnly = true)
    public IntegriteAuditResponse verifierIntegriteChaine() {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.ADMIN_NATIONAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "La vérification d'intégrité globale de la chaîne d'audit est réservée à l'Admin National");
        }

        List<Object[]> rows = jdbcTemplate.query(
                "SELECT id, hash_precedent, hash_ligne FROM journal_audit ORDER BY id ASC",
                (rs, rowNum) -> new Object[]{rs.getLong("id"), rs.getString("hash_precedent"), rs.getString("hash_ligne")}
        );

        String attendu = "0".repeat(64);
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            String prec = ((String) row[1]).trim();
            String cour = ((String) row[2]).trim();
            if (!attendu.equals(prec)) {
                return new IntegriteAuditResponse(false, rows.size(), id, Instant.now());
            }
            attendu = cour;
        }
        return new IntegriteAuditResponse(true, rows.size(), null, Instant.now());
    }
}
