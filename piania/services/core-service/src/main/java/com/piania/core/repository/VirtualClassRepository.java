package com.piania.core.repository;

import com.piania.core.entity.VirtualClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirtualClassRepository extends JpaRepository<VirtualClass, Long> {

    Page<VirtualClass> findByTeacherEmail(String teacherEmail, Pageable pageable);
}
