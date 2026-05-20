package com.piania.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.piania.auth.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
