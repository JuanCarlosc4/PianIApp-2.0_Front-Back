package com.piania.core.dto.practice;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PracticeSessionUpdateRequest {

    @Size(max = 5000, message = "studentObservations too long")
    private String studentObservations;

    @Size(max = 5000, message = "teacherCorrections too long")
    private String teacherCorrections;
}
