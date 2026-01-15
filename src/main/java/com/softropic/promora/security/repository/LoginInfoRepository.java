package com.softropic.promora.security.repository;



import com.softropic.promora.security.common.domain.LoginData;
import com.softropic.promora.security.domain.LoginInfo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA persistence for the Login entity.
 */
public interface LoginInfoRepository extends JpaRepository<LoginInfo, Long> {

    Optional<LoginData> findOneById(Long id);
}
