package com.softropic.promora.security.repository;

import com.softropic.promora.security.domain.PersistentToken;
import com.softropic.promora.security.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA persistence for the PersistentToken entity.
 */
public interface PersistentTokenRepository extends JpaRepository<PersistentToken, String> {

    List<PersistentToken> findByUser(final User user);

    List<PersistentToken> findByTokenDateBefore(final LocalDate localDate);

}
