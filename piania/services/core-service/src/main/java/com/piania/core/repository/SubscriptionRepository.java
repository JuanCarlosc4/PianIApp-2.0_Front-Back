package com.piania.core.repository;

import com.piania.core.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findTopByUserEmailOrderByCreatedAtDesc(String userEmail);
}
