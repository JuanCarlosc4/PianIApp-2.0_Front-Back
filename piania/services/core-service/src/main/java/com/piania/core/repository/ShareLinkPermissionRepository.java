package com.piania.core.repository;

import com.piania.core.entity.ShareLinkPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareLinkPermissionRepository extends JpaRepository<ShareLinkPermission, Long> {

    List<ShareLinkPermission> findByShareLinkId(Long shareLinkId);

    Optional<ShareLinkPermission> findByShareLinkIdAndAllowedEmail(Long shareLinkId, String allowedEmail);

    void deleteByShareLinkIdAndAllowedEmail(Long shareLinkId, String allowedEmail);
}
