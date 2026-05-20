package com.piania.core.repository;

import com.piania.core.entity.ChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatReadStateRepository extends JpaRepository<ChatReadState, Long> {

    Optional<ChatReadState> findByVirtualClassIdAndUserEmail(Long virtualClassId, String userEmail);
}
