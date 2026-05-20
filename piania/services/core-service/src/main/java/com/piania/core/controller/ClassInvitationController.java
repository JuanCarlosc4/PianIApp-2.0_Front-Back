package com.piania.core.controller;

import com.piania.core.dto.classinvitation.ClassInvitationResponse;
import com.piania.core.entity.ClassEnrollment;
import com.piania.core.entity.ClassInvitation;
import com.piania.core.entity.TeacherStudentRelation;
import com.piania.core.entity.VirtualClass;
import com.piania.core.enums.EnrollmentStatus;
import com.piania.core.exception.BadRequestException;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ClassEnrollmentRepository;
import com.piania.core.repository.ClassInvitationRepository;
import com.piania.core.repository.TeacherStudentRelationRepository;
import com.piania.core.repository.VirtualClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/piania/class-invitations")
@RequiredArgsConstructor
public class ClassInvitationController {

    private final ClassInvitationRepository invitationRepository;
    private final VirtualClassRepository virtualClassRepository;
    private final TeacherStudentRelationRepository relationRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ClassInvitationResponse createInvitation(@RequestParam Long classId,
                                                    @RequestParam(defaultValue = "168") int hoursValid,
                                                    Authentication authentication) {

        VirtualClass virtualClass = virtualClassRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        String teacherEmail = authentication.getName();
        if (!virtualClass.getTeacherEmail().equals(teacherEmail)) {
            throw new ForbiddenException("Only the class owner can create invitations");
        }

        int safeHours = Math.max(1, Math.min(hoursValid, 720)); // 1h .. 30d
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(safeHours);

        String token = UUID.randomUUID().toString().replace("-", "");

        ClassInvitation invitation = ClassInvitation.builder()
                .token(token)
                .virtualClass(virtualClass)
                .teacherEmail(teacherEmail)
                .expiresAt(expiresAt)
                .build();

        invitationRepository.save(invitation);

        // Deep link: piania://class-invite/{token}
        String url = "piania://class-invite/" + token;

        return ClassInvitationResponse.builder()
                .token(token)
                .url(url)
                .expiresAt(expiresAt)
                .classId(classId)
                .build();
    }

    /**
     * Aceptar un link de clase.
     *
     * Idempotente: si el usuario ya está matriculado, OK. El token queda marcado como usado.
     *
     * Importante: para matricularse por link, el alumno debe estar vinculado al profesor
     * (misma restricción que addStudent) para mantener el modelo actual.
     */
    @PostMapping("/{token}/accept")
    @PreAuthorize("hasRole('USER')")
    public void accept(@PathVariable String token, Authentication authentication) {

        ClassInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid invitation token"));

        if (invitation.isExpired()) {
            throw new BadRequestException("Invitation token expired");
        }

        VirtualClass virtualClass = invitation.getVirtualClass();
        if (virtualClass == null) {
            throw new BadRequestException("Invitation is not linked to a class");
        }

        // Anti-tampering: la invitación guarda teacherEmail y la clase también.
        if (!virtualClass.getTeacherEmail().equals(invitation.getTeacherEmail())) {
            throw new BadRequestException("Invalid invitation");
        }

        String studentEmail = authentication.getName();
        String teacherEmail = invitation.getTeacherEmail();

        TeacherStudentRelation relation = relationRepository
                .findByTeacherEmailAndStudentEmail(teacherEmail, studentEmail)
                .orElseThrow(() -> new BadRequestException("Student is not linked to this teacher"));

        if (!relation.isActive()) {
            throw new BadRequestException("Teacher-student relation is not active");
        }

        ClassEnrollment enrollment = enrollmentRepository
                .findByVirtualClassAndStudentEmail(virtualClass, studentEmail)
                .orElse(null);

        if (enrollment == null) {
            enrollment = ClassEnrollment.builder()
                    .virtualClass(virtualClass)
                    .studentEmail(studentEmail)
                    .status(EnrollmentStatus.ACTIVE)
                    .build();
        } else if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
        }

        enrollmentRepository.save(enrollment);

        if (!invitation.isUsed()) {
            invitation.setUsedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
        }
    }
}
