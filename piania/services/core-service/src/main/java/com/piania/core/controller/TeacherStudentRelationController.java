package com.piania.core.controller;

import com.piania.core.entity.TeacherStudentRelation;
import com.piania.core.exception.BadRequestException;
import com.piania.core.repository.TeacherStudentRelationRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/piania/teacher-relations")
@RequiredArgsConstructor
public class TeacherStudentRelationController {

    private final TeacherStudentRelationRepository relationRepository;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public void linkToTeacher(@RequestParam @NotBlank String teacherEmail,
                              Authentication authentication) {

        if (relationRepository.findByTeacherEmailAndStudentEmail(
                teacherEmail,
                authentication.getName()).isPresent()) {
            throw new BadRequestException("Relation already exists");
        }

        TeacherStudentRelation relation = TeacherStudentRelation.builder()
                .teacherEmail(teacherEmail)
                .studentEmail(authentication.getName())
                .active(true)
                .build();

        relationRepository.save(relation);
    }

    @GetMapping("/my-students")
    @PreAuthorize("hasRole('TEACHER')")
    public Page<TeacherStudentRelation> listMyStudents(
            Pageable pageable,
            Authentication authentication) {

        Page<TeacherStudentRelation> page =
                relationRepository.findByTeacherEmailAndActiveTrue(authentication.getName(), pageable);

        return new PageImpl<>(
                page.getContent(),
                pageable,
                page.getTotalElements()
        );
    }
}
