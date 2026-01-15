package com.softropic.promora.security.repository;

import com.softropic.promora.security.domain.Authority;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA persistence for the Authority entity.
 */
public interface AuthorityRepository extends JpaRepository<Authority, UUID> {

    Optional<Authority> findOneByName(String name);
}
