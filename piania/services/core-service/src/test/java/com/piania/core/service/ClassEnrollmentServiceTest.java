package com.piania.core.service;

import com.piania.core.dto.classenrollment.ClassEnrollmentRequest;
import com.piania.core.entity.ClassEnrollment;
import com.piania.core.entity.TeacherStudentRelation;
import com.piania.core.entity.VirtualClass;
import com.piania.core.exception.BadRequestException;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ClassEnrollmentRepository;
import com.piania.core.repository.TeacherStudentRelationRepository;
import com.piania.core.repository.VirtualClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassEnrollmentServiceTest {

    @Mock
    private ClassEnrollmentRepository enrollmentRepository;

    @Mock
    private VirtualClassRepository virtualClassRepository;

    @Mock
    private TeacherStudentRelationRepository relationRepository;

    @InjectMocks
    private ClassEnrollmentService enrollmentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldThrowWhenClassNotFound() {

        when(virtualClassRepository.findById(1L))
                .thenReturn(Optional.empty());

        ClassEnrollmentRequest request = ClassEnrollmentRequest.builder()
                .classId(1L)
                .studentEmail("student@test.com")
                .build();

        assertThrows(NotFoundException.class,
                () -> enrollmentService.addStudent("teacher@test.com", request));
    }

    @Test
    void shouldThrowWhenTeacherNotOwner() {

        VirtualClass virtualClass = VirtualClass.builder()
                .id(1L)
                .teacherEmail("other@test.com")
                .build();

        when(virtualClassRepository.findById(1L))
                .thenReturn(Optional.of(virtualClass));

        ClassEnrollmentRequest request = ClassEnrollmentRequest.builder()
                .classId(1L)
                .studentEmail("student@test.com")
                .build();

        assertThrows(ForbiddenException.class,
                () -> enrollmentService.addStudent("teacher@test.com", request));
    }

    @Test
    void shouldThrowWhenRelationNotFound() {

        VirtualClass virtualClass = VirtualClass.builder()
                .id(1L)
                .teacherEmail("teacher@test.com")
                .build();

        when(virtualClassRepository.findById(1L))
                .thenReturn(Optional.of(virtualClass));

        when(relationRepository.findByTeacherEmailAndStudentEmail(
                "teacher@test.com", "student@test.com"))
                .thenReturn(Optional.empty());

        ClassEnrollmentRequest request = ClassEnrollmentRequest.builder()
                .classId(1L)
                .studentEmail("student@test.com")
                .build();

        assertThrows(BadRequestException.class,
                () -> enrollmentService.addStudent("teacher@test.com", request));
    }

    @Test
    void shouldEnrollStudentSuccessfully() {

        VirtualClass virtualClass = VirtualClass.builder()
                .id(1L)
                .teacherEmail("teacher@test.com")
                .build();

        TeacherStudentRelation relation = TeacherStudentRelation.builder()
                .teacherEmail("teacher@test.com")
                .studentEmail("student@test.com")
                .active(true)
                .build();

        when(virtualClassRepository.findById(1L))
                .thenReturn(Optional.of(virtualClass));

        when(relationRepository.findByTeacherEmailAndStudentEmail(
                "teacher@test.com", "student@test.com"))
                .thenReturn(Optional.of(relation));

        when(enrollmentRepository.findByVirtualClassAndStudentEmail(
                virtualClass, "student@test.com"))
                .thenReturn(Optional.empty());

        when(enrollmentRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClassEnrollmentRequest request = ClassEnrollmentRequest.builder()
                .classId(1L)
                .studentEmail("student@test.com")
                .build();

        ClassEnrollment enrollment =
                enrollmentService.addStudent("teacher@test.com", request);

        assertNotNull(enrollment);
        verify(enrollmentRepository).save(any());
    }
}
