package com.piania.core.dto.teacherstudent;

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
public class TeacherStudentRelationResponse {

    private Long id;
    private String teacherEmail;
    private String studentEmail;
    private boolean active;
    private LocalDateTime createdAt;
}
