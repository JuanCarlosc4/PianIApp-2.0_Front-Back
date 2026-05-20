package com.piania.core.service;

import com.piania.core.dto.classenrollment.ClassEnrollmentRequest;
import com.piania.core.entity.ClassEnrollment;
import com.piania.core.entity.TeacherStudentRelation;
import com.piania.core.entity.VirtualClass;
import com.piania.core.enums.EnrollmentStatus;
import com.piania.core.exception.BadRequestException;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ClassEnrollmentRepository;
import com.piania.core.repository.TeacherStudentRelationRepository;
import com.piania.core.repository.VirtualClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassEnrollmentService {

    private final ClassEnrollmentRepository enrollmentRepository;
    private final VirtualClassRepository virtualClassRepository;
    private final TeacherStudentRelationRepository relationRepository;

    public ClassEnrollment addStudent(String teacherEmail,
                                      ClassEnrollmentRequest request) {

        VirtualClass virtualClass = virtualClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (!virtualClass.getTeacherEmail().equals(teacherEmail)) {
            throw new ForbiddenException("Only the class owner can add students");
        }

        TeacherStudentRelation relation = relationRepository
                .findByTeacherEmailAndStudentEmail(teacherEmail, request.getStudentEmail())
                .orElseThrow(() -> new BadRequestException("Student is not linked to this teacher"));

        if (!relation.isActive()) {
            throw new BadRequestException("Teacher-student relation is not active");
        }

        if (enrollmentRepository.findByVirtualClassAndStudentEmail(virtualClass,
                request.getStudentEmail()).isPresent()) {
            throw new BadRequestException("Student already enrolled in this class");
        }

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .virtualClass(virtualClass)
                .studentEmail(request.getStudentEmail())
                .status(EnrollmentStatus.ACTIVE)
                .build();

        return enrollmentRepository.save(enrollment);
    }

    public Page<ClassEnrollment> listEnrollments(String teacherEmail,
                                                 Long classId,
                                                 Pageable pageable) {

        VirtualClass virtualClass = virtualClassRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (!virtualClass.getTeacherEmail().equals(teacherEmail)) {
            throw new ForbiddenException("Access denied");
        }

        return enrollmentRepository.findByVirtualClassAndStatus(virtualClass, EnrollmentStatus.ACTIVE, pageable);
    }

    public void removeStudent(String teacherEmail,
                              Long enrollmentId) {

        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

        if (!enrollment.getVirtualClass().getTeacherEmail().equals(teacherEmail)) {
            throw new ForbiddenException("Access denied");
        }

        enrollment.setStatus(EnrollmentStatus.REMOVED);
        enrollmentRepository.save(enrollment);
    }
}
