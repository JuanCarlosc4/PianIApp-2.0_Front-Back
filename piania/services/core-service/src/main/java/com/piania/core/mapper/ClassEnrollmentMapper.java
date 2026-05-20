package com.piania.core.mapper;

import com.piania.core.dto.classenrollment.ClassEnrollmentResponse;
import com.piania.core.entity.ClassEnrollment;
import org.springframework.stereotype.Component;

@Component
public class ClassEnrollmentMapper {

    public ClassEnrollmentResponse toResponse(ClassEnrollment enrollment) {
        return ClassEnrollmentResponse.builder()
                .id(enrollment.getId())
                .virtualClassId(enrollment.getVirtualClass().getId())
                .virtualClassName(enrollment.getVirtualClass().getName())
                .studentEmail(enrollment.getStudentEmail())
                .status(enrollment.getStatus())
                .createdAt(enrollment.getCreatedAt())
                .build();
    }
}
