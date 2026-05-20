package com.piania.core.controller;

import com.piania.core.dto.classenrollment.ClassEnrollmentRequest;
import com.piania.core.dto.classenrollment.ClassEnrollmentResponse;
import com.piania.core.entity.ClassEnrollment;
import com.piania.core.mapper.ClassEnrollmentMapper;
import com.piania.core.service.ClassEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/piania/class-enrollments")
@RequiredArgsConstructor
public class ClassEnrollmentController {

    private final ClassEnrollmentService enrollmentService;
    private final ClassEnrollmentMapper enrollmentMapper;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ClassEnrollmentResponse addStudent(@Valid @RequestBody ClassEnrollmentRequest request,
                                              Authentication authentication) {
        ClassEnrollment enrollment =
                enrollmentService.addStudent(authentication.getName(), request);
        return enrollmentMapper.toResponse(enrollment);
    }

    @GetMapping("/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Page<ClassEnrollmentResponse> listStudents(@PathVariable Long classId,
                                                      Pageable pageable,
                                                      Authentication authentication) {
        Page<ClassEnrollment> page =
                enrollmentService.listEnrollments(authentication.getName(), classId, pageable);
        return new PageImpl<>(
                page.getContent().stream()
                        .map(enrollmentMapper::toResponse)
                        .toList(),
                pageable,
                page.getTotalElements()
        );
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public void removeStudent(@PathVariable Long enrollmentId,
                              Authentication authentication) {
        enrollmentService.removeStudent(authentication.getName(), enrollmentId);
    }
}
