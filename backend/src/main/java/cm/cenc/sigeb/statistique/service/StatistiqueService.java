package cm.cenc.sigeb.statistique.service;

import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import cm.cenc.sigeb.statistique.dto.StatistiqueGlobaleResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatistiqueService {

    private final JdbcTemplate jdbcTemplate;

    public StatistiqueService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public StatistiqueGlobaleResponse tableauDeBord() {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        String filtreParoisseSql;
        Object[] params;

        switch (caller.niveau()) {
            case NATIONAL -> {
                filtreParoisseSql = "TRUE";
                params = new Object[]{};
            }
            case PROVINCE -> {
                filtreParoisseSql = "p.diocese_id IN (SELECT id FROM diocese WHERE province_id = ?)";
                params = new Object[]{caller.provinceId()};
            }
            case DIOCESE -> {
                filtreParoisseSql = "p.diocese_id = ?";
                params = new Object[]{caller.dioceseId()};
            }
            case PAROISSE -> {
                filtreParoisseSql = "p.id = ?";
                params = new Object[]{caller.paroisseId()};
            }
            default -> throw new IllegalStateException("Niveau inconnu");
        }

        long scelles = countSql("""
            SELECT COUNT(*) FROM acte_bapteme a JOIN paroisse p ON p.id = a.paroisse_id
            WHERE a.statut = 'SCELLE' AND\s""" + filtreParoisseSql, params);

        long enAttente = countSql("""
            SELECT COUNT(*) FROM acte_bapteme a JOIN paroisse p ON p.id = a.paroisse_id
            WHERE a.statut = 'EN_ATTENTE_VALIDATION' AND\s""" + filtreParoisseSql, params);

        long brouillons = countSql("""
            SELECT COUNT(*) FROM acte_bapteme a JOIN paroisse p ON p.id = a.paroisse_id
            WHERE a.statut IN ('BROUILLON','A_CORRIGER') AND\s""" + filtreParoisseSql, params);

        long originales = countSql("""
            SELECT COUNT(*) FROM carte_emise c JOIN paroisse p ON p.id = c.paroisse_emettrice_id
            WHERE c.type_emission = 'ORIGINALE' AND\s""" + filtreParoisseSql, params);

        long duplicatas = countSql("""
            SELECT COUNT(*) FROM carte_emise c JOIN paroisse p ON p.id = c.paroisse_emettrice_id
            WHERE c.type_emission = 'DUPLICATA' AND\s""" + filtreParoisseSql, params);

        Map<String, Long> mentionsParType = new LinkedHashMap<>();
        jdbcTemplate.query("""
            SELECT m.type_sacrement, COUNT(*) AS nb
            FROM mention_marginale m
            JOIN acte_bapteme a ON a.id = m.acte_bapteme_id
            JOIN paroisse p ON p.id = a.paroisse_id
            WHERE m.statut = 'INSCRITE' AND\s""" + filtreParoisseSql + " GROUP BY m.type_sacrement ORDER BY nb DESC",
                rs -> { mentionsParType.put(rs.getString("type_sacrement"), rs.getLong("nb")); },
                params
        );

        Map<Integer, Long> baptemesParAnnee = new LinkedHashMap<>();
        jdbcTemplate.query("""
            SELECT EXTRACT(YEAR FROM a.date_bapteme)::int AS annee, COUNT(*) AS nb
            FROM acte_bapteme a
            JOIN paroisse p ON p.id = a.paroisse_id
            WHERE a.statut = 'SCELLE' AND\s""" + filtreParoisseSql + " GROUP BY annee ORDER BY annee DESC LIMIT 10",
                rs -> { baptemesParAnnee.put(rs.getInt("annee"), rs.getLong("nb")); },
                params
        );

        return new StatistiqueGlobaleResponse(
                caller.niveau().name(), scelles, enAttente, brouillons,
                originales, duplicatas, mentionsParType, baptemesParAnnee
        );
    }

    private long countSql(String sql, Object[] params) {
        Long val = jdbcTemplate.queryForObject(sql, Long.class, params);
        return val != null ? val : 0L;
    }
}
