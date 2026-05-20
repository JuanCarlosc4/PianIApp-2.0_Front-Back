package com.piania.app.data.model.request

data class RegistroRequestDTO(
    val email: String,
    val password: String, // La contraseña en texto plano

    // Backend nuevo (auth-service) espera "fullName"
    val fullName: String,

    // USER | TEACHER | ADMIN (en UI tratamos STUDENT como USER)
    val accountType: String = "USER"
)
