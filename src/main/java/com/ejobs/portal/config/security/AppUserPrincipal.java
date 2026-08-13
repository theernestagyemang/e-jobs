package com.ejobs.portal.config.security;

import com.ejobs.portal.model.Role;
import com.ejobs.portal.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * UserDetails that carries the user's UUID.
 *
 * <p>Spring's stock {@code User} only exposes the username, so every controller had to
 * look the id up by email on each request. Holding it on the principal removes that
 * query - the JWT filter has already loaded the row.
 */
public class AppUserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public AppUserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.active = user.isActive();
    }

    /** Id of the caller, or null when the request is anonymous. */
    public static UUID idOrNull(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof AppUserPrincipal principal
                ? principal.getId()
                : null;
    }

    /** Id of the caller on an endpoint the filter chain has already secured. */
    public static UUID requireId(Authentication authentication) {
        UUID id = idOrNull(authentication);
        if (id == null) {
            throw new IllegalStateException(
                    "No authenticated principal - this endpoint should be secured");
        }
        return id;
    }

    public UUID getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ROLE_" prefix so hasRole("EMPLOYER") / hasRole("JOB_SEEKER") match.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** A deactivated account is disabled - this is what makes the admin kill switch bite. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
