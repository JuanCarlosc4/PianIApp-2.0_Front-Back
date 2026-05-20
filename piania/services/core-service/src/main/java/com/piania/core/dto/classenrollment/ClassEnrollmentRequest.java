package com.piania.core.dto.classenrollment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentRequest {

    @NotNull
    private Long classId;

    @NotNull
    @Email
    private String studentEmail;
}
