package com.ejobs.portal.repository;

import com.ejobs.portal.model.Role;
import com.ejobs.portal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Admin user list, filtered by role. */
    Page<User> findAllByRole(Role role, Pageable pageable);

    /** Platform stats. */
    long countByRole(Role role);
}
