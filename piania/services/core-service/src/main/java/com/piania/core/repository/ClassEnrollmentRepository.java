package com.piania.core.repository;

import com.piania.core.entity.ClassEnrollment;
import com.piania.core.entity.VirtualClass;
import com.piania.core.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    void deleteByVirtualClass(VirtualClass virtualClass);

    Page<ClassEnrollment> findByVirtualClassAndStatus(VirtualClass virtualClass, EnrollmentStatus status, Pageable pageable);

    Optional<ClassEnrollment> findByVirtualClassAndStudentEmail(VirtualClass virtualClass, String studentEmail);

    Page<ClassEnrollment> findByStudentEmailAndStatus(String studentEmail, EnrollmentStatus status, Pageable pageable);
}
