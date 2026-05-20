package com.piania.core.repository;

import com.piania.core.entity.ChatMessage;
import com.piania.core.entity.VirtualClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    void deleteByVirtualClass(VirtualClass virtualClass);

    Page<ChatMessage> findByVirtualClassOrderByCreatedAtDesc(VirtualClass virtualClass, Pageable pageable);

    Optional<ChatMessage> findFirstByVirtualClassAndPinnedTrueOrderByCreatedAtDesc(VirtualClass virtualClass);

    @Modifying
    @Query("update ChatMessage m set m.pinned = false where m.virtualClass = :virtualClass and m.pinned = true")
    int unpinAllInClass(VirtualClass virtualClass);
}
