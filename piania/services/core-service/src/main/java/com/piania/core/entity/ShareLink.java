package com.piania.core.entity;

import com.piania.core.enums.ShareAccessType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "share_links")
public class ShareLink extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sheet_music_id", nullable = false)
    private SheetMusic sheetMusic;

    @Column(nullable = false)
    private String ownerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareAccessType accessType;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    public void generateTokenIfMissing() {
        if (this.token == null) {
            this.token = UUID.randomUUID().toString();
        }
        if (!this.active) {
            this.active = true;
        }
    }
}
