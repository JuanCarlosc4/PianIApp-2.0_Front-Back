package com.piania.core.controller;

import com.piania.core.dto.virtualclass.VirtualClassResponse;
import com.piania.core.entity.ClassEnrollment;
import com.piania.core.entity.GroupAvatar;
import com.piania.core.entity.VirtualClass;
import com.piania.core.enums.EnrollmentStatus;
import com.piania.core.exception.BadRequestException;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ChatMessageRepository;
import com.piania.core.repository.ClassEnrollmentRepository;
import com.piania.core.repository.VirtualClassRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/piania/classes")
@RequiredArgsConstructor
public class VirtualClassController {

    private final VirtualClassRepository virtualClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ChatMessageRepository chatMessageRepository;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public VirtualClassResponse createClass(@RequestParam @NotBlank String name,
                                           Authentication authentication) {

        VirtualClass virtualClass = VirtualClass.builder()
                .name(name)
                .teacherEmail(authentication.getName())
                .groupAvatar(com.piania.core.entity.GroupAvatar.GROUP_AVATAR_1)
                .status(com.piania.core.enums.ClassStatus.ACTIVE)
                .build();

        return toResponse(virtualClassRepository.save(virtualClass));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public Page<VirtualClassResponse> listMyClasses(Pageable pageable,
                                                   Authentication authentication) {

        Page<VirtualClass> page =
                virtualClassRepository.findByTeacherEmail(authentication.getName(), pageable);

        List<VirtualClassResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @GetMapping("/enrolled")
    @PreAuthorize("hasRole('USER')")
    public Page<VirtualClassResponse> listEnrolledClasses(Pageable pageable, Authentication authentication) {

        Page<ClassEnrollment> enrollments =
                classEnrollmentRepository.findByStudentEmailAndStatus(
                        authentication.getName(),
                        EnrollmentStatus.ACTIVE,
                        pageable
                );

        List<VirtualClassResponse> classes = enrollments.getContent().stream()
                .map(ClassEnrollment::getVirtualClass)
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(classes, pageable, enrollments.getTotalElements());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public void deleteClass(@PathVariable Long id,
                            Authentication authentication) {

        VirtualClass virtualClass = virtualClassRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (!virtualClass.getTeacherEmail().equals(authentication.getName())) {
            throw new ForbiddenException("Access denied");
        }

        // Important: delete children first to satisfy FK constraints
        chatMessageRepository.deleteByVirtualClass(virtualClass);
        classEnrollmentRepository.deleteByVirtualClass(virtualClass);

        virtualClassRepository.delete(virtualClass);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public VirtualClassResponse updateClass(@PathVariable Long id,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String groupAvatar,
                                            Authentication authentication) {

        VirtualClass virtualClass = virtualClassRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (!virtualClass.getTeacherEmail().equals(authentication.getName())) {
            throw new ForbiddenException("Access denied");
        }

        if (name != null) {
            String trimmed = name.trim();
            if (trimmed.isBlank()) {
                throw new BadRequestException("Class name cannot be blank");
            }
            virtualClass.setName(trimmed);
        }

        if (groupAvatar != null && !groupAvatar.isBlank()) {
            try {
                virtualClass.setGroupAvatar(GroupAvatar.valueOf(groupAvatar.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid group avatar");
            }
        }

        return toResponse(virtualClassRepository.save(virtualClass));
    }

    private VirtualClassResponse toResponse(VirtualClass vc) {
        if (vc == null) return null;
        return VirtualClassResponse.builder()
                .id(vc.getId())
                .name(vc.getName())
                .description(vc.getDescription())
                .teacherEmail(vc.getTeacherEmail())
                .groupAvatar(vc.getGroupAvatar())
                .status(vc.getStatus())
                .createdAt(vc.getCreatedAt())
                .build();
    }
}
