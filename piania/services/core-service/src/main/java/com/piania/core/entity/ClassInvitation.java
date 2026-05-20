package com.piania.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_invitations", indexes = {
        @Index(name = "idx_class_invitation_token", columnList = "token", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassInvitation extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "virtual_class_id", nullable = false)
    private VirtualClass virtualClass;

    @Column(nullable = false)
    private String teacherEmail;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime usedAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
