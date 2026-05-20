package com.piania.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.piania.core.entity.AuditLog;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {
}
