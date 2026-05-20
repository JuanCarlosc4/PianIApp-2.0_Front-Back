package com.piania.app.data.model.response

data class TeacherStudentRelationDTO(
    val id: Long,
    val teacherEmail: String,
    val studentEmail: String,
    val active: Boolean,
    val createdAt: String? = null
)
