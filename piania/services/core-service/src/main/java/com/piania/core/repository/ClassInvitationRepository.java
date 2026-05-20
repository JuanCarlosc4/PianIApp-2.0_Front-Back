package com.piania.core.repository;

import com.piania.core.entity.ClassInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassInvitationRepository extends JpaRepository<ClassInvitation, Long> {

    Optional<ClassInvitation> findByToken(String token);
}
