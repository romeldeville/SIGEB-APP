package cm.cenc.sigeb.shared.config;

import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Active la double barrière Row-Level Security (RLS) PostgreSQL sur chaque connexion :
 * injecte les variables de session app.role, app.niveau, app.province_id, app.diocese_id, app.paroisse_id
 * depuis le TenantContext (et les réinitialise à vide pour les connexions recyclées du pool).
 */
@Configuration
public class RlsDataSourceConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (bean instanceof DataSource ds && !(bean instanceof RlsAwareDataSource)) {
            return new RlsAwareDataSource(ds);
        }
        return bean;
    }

    static final class RlsAwareDataSource extends DelegatingDataSource {

        RlsAwareDataSource(DataSource targetDataSource) {
            super(targetDataSource);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = super.getConnection();
            applyRlsSession(conn);
            return conn;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection conn = super.getConnection(username, password);
            applyRlsSession(conn);
            return conn;
        }

        private void applyRlsSession(Connection conn) throws SQLException {
            Optional<UserPrincipal> userOpt = TenantContext.getCurrentUser();
            boolean nationalOverride = TenantContext.isNationalOverride();

            String role = userOpt.map(u -> u.role().name()).orElse("");
            String niveau = nationalOverride ? "NATIONAL" : userOpt.map(u -> u.niveau().name()).orElse("");
            String provinceId = userOpt.map(u -> u.provinceId() != null ? u.provinceId().toString() : "").orElse("");
            String dioceseId = userOpt.map(u -> u.dioceseId() != null ? u.dioceseId().toString() : "").orElse("");
            String paroisseId = userOpt.map(u -> u.paroisseId() != null ? u.paroisseId().toString() : "").orElse("");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT set_config('app.role', ?, false), " +
                            "set_config('app.niveau', ?, false), " +
                            "set_config('app.province_id', ?, false), " +
                            "set_config('app.diocese_id', ?, false), " +
                            "set_config('app.paroisse_id', ?, false)"
            )) {
                ps.setString(1, role);
                ps.setString(2, niveau);
                ps.setString(3, provinceId);
                ps.setString(4, dioceseId);
                ps.setString(5, paroisseId);
                ps.execute();
            }
        }
    }
}
