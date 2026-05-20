package com.piania.core.entity;

import com.piania.core.enums.SubscriptionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription extends BaseEntity {

    @Column(nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionType planType;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime expiresAt;
}
