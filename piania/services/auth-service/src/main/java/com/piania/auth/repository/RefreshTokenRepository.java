package com.piania.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.piania.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(String userId);
}
