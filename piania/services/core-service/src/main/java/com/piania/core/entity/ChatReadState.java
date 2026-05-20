package com.piania.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "chat_read_state",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_read_state_class_user", columnNames = {"virtual_class_id", "user_email"})
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadState extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "virtual_class_id", nullable = false)
    private VirtualClass virtualClass;

    /**
     * Email del usuario (profesor o alumno) al que pertenece este estado.
     */
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    /**
     * Último messageId visto por el usuario en esta clase.
     * Puede ser null si aún no ha visto ninguno.
     */
    @Column(name = "last_seen_message_id")
    private Long lastSeenMessageId;
}
