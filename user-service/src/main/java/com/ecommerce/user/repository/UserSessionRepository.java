package com.ecommerce.user.repository;

import com.ecommerce.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    Optional<UserSession> findByToken(String token);
    List<UserSession> findByUserIdAndActiveTrue(Long userId);
}
