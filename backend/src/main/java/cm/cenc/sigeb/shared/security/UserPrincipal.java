package cm.cenc.sigeb.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String identifiant,
        String motDePasseHash,
        Role role,
        UUID dioceseId,
        UUID paroisseId,
        String nomComplet,
        String langue,
        boolean actif,
        boolean mdpAChanger,
        boolean totpActive
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasseHash;
    }

    @Override
    public String getUsername() {
        return identifiant;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return actif;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
