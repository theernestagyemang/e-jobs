package com.ejobs.portal.config.security;

import com.ejobs.portal.model.User;
import com.ejobs.portal.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * FR-AUTH-03: resolves the principal from the users table. The username is the email.
 *
 * <p>Returns an {@link AppUserPrincipal} so downstream code can read the user's UUID
 * straight off the SecurityContext instead of querying for it again.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));

        return new AppUserPrincipal(user);
    }
}
