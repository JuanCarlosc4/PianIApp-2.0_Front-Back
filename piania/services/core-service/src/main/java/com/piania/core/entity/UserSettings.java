package com.piania.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_settings")
public class UserSettings extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String userEmail;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private boolean cookiesAccepted;

    @Column(nullable = false)
    private boolean privacyPolicyAccepted;

    @Column(nullable = false)
    private boolean notificationsEnabled;

    @Column(nullable = false)
    private boolean metronomeEnabled;

    @Column(nullable = false)
    private int defaultTempo;

    @Column(nullable = false)
    private boolean adsEnabled;

    @Column(nullable = false)
    private boolean darkMode;
}
