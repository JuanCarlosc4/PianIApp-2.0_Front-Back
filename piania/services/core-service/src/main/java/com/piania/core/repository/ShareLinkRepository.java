package com.piania.core.repository;

import com.piania.core.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);

    List<ShareLink> findByOwnerEmail(String ownerEmail);

    List<ShareLink> findBySheetMusicId(Long sheetMusicId);

    List<ShareLink> findByOwnerEmailAndActiveTrue(String ownerEmail);

}
