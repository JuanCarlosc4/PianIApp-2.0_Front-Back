package com.piania.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    private String method;

    private String endpoint;

    private LocalDateTime timestamp;
}
