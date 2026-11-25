package cm.cenc.sigeb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Vérifie les migrations Flyway, la RLS et l'audit append-only sur un vrai PostgreSQL 16. */
@SpringBootTest
@Testcontainers
class SchemaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @org.springframework.test.context.DynamicPropertySource
    static void flywayEtDatasource(org.springframework.test.context.DynamicPropertyRegistry r) {
        r.add("spring.flyway.user", pg::getUsername);
        r.add("spring.flyway.password", pg::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @Test
    void migrationsCreentLesTablesEtLeSeed() {
        Integer tables = jdbc.queryForObject("""
            SELECT count(*) FROM information_schema.tables
            WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'""", Integer.class);
        assertThat(tables).isEqualTo(14);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM province_ecclesiastique", Integer.class)).isEqualTo(5);
    }

    @Test
    void rlsCloisonneLesActesParParoisse() throws SQLException {
        UUID dio = jdbc.queryForObject("""
            INSERT INTO diocese(province_id, code, type_diocese, nom_fr, nom_en, siege)
            SELECT id, 'TST', 'DIOCESE', 'Test', 'Test', 'Yaoundé' FROM province_ecclesiastique WHERE code='YDE'
            RETURNING id""", UUID.class);
        UUID pA = paroisse(dio, "PA");
        UUID pB = paroisse(dio, "PB");
        UUID user = jdbc.queryForObject("""
            INSERT INTO utilisateur(identifiant, mot_de_passe_hash, nom_complet, role, diocese_id, paroisse_id)
            VALUES ('cure-a@test', 'x', 'Curé A', 'CURE', ?, ?) RETURNING id""", UUID.class, dio, pA);
        acte(pA, user, "1");
        acte(pB, user, "2");

        try (Connection c = java.sql.DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())) {
            JdbcTemplate app = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(c, true));
            app.execute("SET ROLE sigeb_app");
            app.execute("SELECT set_config('app.role','CURE',false), set_config('app.paroisse_id','" + pA + "',false)");
            assertThat(app.queryForObject("SELECT count(*) FROM acte_bapteme", Integer.class)).isEqualTo(1);

            app.execute("SELECT set_config('app.role','',false)");
            assertThat(app.queryForObject("SELECT count(*) FROM acte_bapteme", Integer.class)).isZero();
        }
    }

    @Test
    void auditEstChaineEtAppendOnly() throws SQLException {
        jdbc.update("""
            INSERT INTO journal_audit(role_utilisateur, type_action) VALUES ('CURE','CONNEXION')""");
        jdbc.update("""
            INSERT INTO journal_audit(role_utilisateur, type_action) VALUES ('CURE','CONNEXION')""");
        var lignes = jdbc.queryForList(
            "SELECT id, hash_precedent, hash_ligne FROM journal_audit ORDER BY id");
        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).get("hash_precedent")).isEqualTo("0".repeat(64));
        assertThat(lignes.get(1).get("hash_precedent")).isEqualTo(lignes.get(0).get("hash_ligne"));

        try (Connection c = java.sql.DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())) {
            JdbcTemplate app = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(c, true));
            app.execute("SET ROLE sigeb_app");
            assertThatThrownBy(() -> app.update("UPDATE journal_audit SET motif = 'x'"))
                .rootCause().hasMessageContaining("permission denied");
            assertThatThrownBy(() -> app.update("DELETE FROM journal_audit"))
                .rootCause().hasMessageContaining("permission denied");
        }
    }

    private UUID paroisse(UUID dio, String code) {
        return jdbc.queryForObject("""
            INSERT INTO paroisse(diocese_id, code, type_paroisse, nom_fr, nom_en, localite)
            VALUES (?, ?, 'PAROISSE', ?, ?, 'Yaoundé') RETURNING id""", UUID.class, dio, code, code, code);
    }

    private void acte(UUID paroisse, UUID user, String numero) {
        jdbc.update("""
            INSERT INTO acte_bapteme(paroisse_id, origine, statut, nom, prenoms, sexe, date_naissance,
              lieu_naissance, nom_parrain, date_bapteme, lieu_bapteme, ministre_celebrant,
              registre_tome, registre_page, registre_numero, saisi_par_id)
            VALUES (?, 'NOUVEAU', 'BROUILLON', 'NKOLO', 'Jean', 'M', '2020-01-01',
              'Yaoundé', 'Parrain', '2020-06-01', 'Église', 'Abbé X', 'T1', 'P1', ?, ?)""",
            paroisse, numero, user);
    }
}
