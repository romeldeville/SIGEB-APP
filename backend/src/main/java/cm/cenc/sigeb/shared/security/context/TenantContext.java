package cm.cenc.sigeb.shared.security.context;

import cm.cenc.sigeb.shared.security.model.NiveauEcclesial;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UserPrincipal> CURRENT_USER = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentUser(UserPrincipal user) {
        CURRENT_USER.set(user);
    }

    public static Optional<UserPrincipal> getCurrentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static UserPrincipal requireCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new IllegalStateException("Aucun utilisateur authentifié dans le contexte"));
    }

    public static Role getRole() {
        return getCurrentUser().map(UserPrincipal::role).orElse(null);
    }

    public static NiveauEcclesial getNiveau() {
        return getCurrentUser().map(UserPrincipal::niveau).orElse(null);
    }

    public static UUID getProvinceId() {
        return getCurrentUser().map(UserPrincipal::provinceId).orElse(null);
    }

    public static UUID getDioceseId() {
        return getCurrentUser().map(UserPrincipal::dioceseId).orElse(null);
    }

    public static UUID getParoisseId() {
        return getCurrentUser().map(UserPrincipal::paroisseId).orElse(null);
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
