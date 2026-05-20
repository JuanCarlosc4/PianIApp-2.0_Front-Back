package com.piania.core.repository;

import com.piania.core.entity.TeacherStudentRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherStudentRelationRepository extends JpaRepository<TeacherStudentRelation, Long> {

    Page<TeacherStudentRelation> findByTeacherEmailAndActiveTrue(String teacherEmail, Pageable pageable);

    Optional<TeacherStudentRelation> findByTeacherEmailAndStudentEmail(String teacherEmail, String studentEmail);
}
