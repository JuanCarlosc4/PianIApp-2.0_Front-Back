package com.piania.core.repository;

import com.piania.core.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUserEmail(String userEmail);

}
