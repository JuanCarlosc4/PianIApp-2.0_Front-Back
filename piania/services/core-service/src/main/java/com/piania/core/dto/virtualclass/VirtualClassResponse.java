package com.piania.core.dto.virtualclass;

import com.piania.core.enums.ClassStatus;
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
public class VirtualClassResponse {

    private Long id;
    private String name;
    private String description;
    private String teacherEmail;
    private com.piania.core.entity.GroupAvatar groupAvatar;
    private ClassStatus status;
    private Integer maxStudents;
    private LocalDateTime createdAt;
}
