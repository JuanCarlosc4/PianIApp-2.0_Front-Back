package com.piania.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "virtual_class_id", nullable = false)
    private VirtualClass virtualClass;

    @Column(nullable = false)
    private String senderEmail;

    /**
     * Mensaje cifrado (AES/GCM).
     */
    @Lob
    @Column(nullable = false, columnDefinition = "BLOB")
    private byte[] cipherText;

    /**
     * IV/nonce para AES/GCM.
     */
    @Column(nullable = false, length = 32)
    private String ivBase64;

    /**
     * Mensaje fijado (pinned) por el profesor dentro de la clase.
     */
    @Column(nullable = false)
    private boolean pinned;
}
