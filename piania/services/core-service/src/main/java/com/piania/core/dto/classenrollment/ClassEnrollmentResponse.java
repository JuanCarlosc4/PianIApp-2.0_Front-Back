package com.piania.core.dto.classenrollment;

import com.piania.core.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentResponse {

    private Long id;
    private Long virtualClassId;
    private String virtualClassName;
    private String studentEmail;
    private EnrollmentStatus status;
    private LocalDateTime createdAt;
}
