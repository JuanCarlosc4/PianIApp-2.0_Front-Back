package com.piania.app.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piania.app.R
import com.piania.app.data.model.response.ChatMessageResponseDTO
import com.piania.app.data.model.response.ClassEnrollmentResponseDTO
import com.piania.app.data.repository.ClassRepository
import com.piania.app.data.repository.TeacherRepository
import com.piania.app.ui.chat.ChatViewModel
import com.piania.app.ui.classes.ClassesViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen de “Info del grupo” (estilo WhatsApp).
 *
 * Qué muestra:
 * - Avatar del grupo + nombre
 * - Profesores
 * - Alumnos
 * - Mensajes fijados / importantes (pinned)
 *
 * Notas/limitaciones:
 * - El backend actual permite listar alumnos de una clase sólo si eres TEACHER (ClassEnrollmentController).
 * - En el modelo de clase actual normalmente hay 1 teacherEmail (no lista). Si en un futuro hay varios,
 *   se podrá ampliar con un endpoint de participantes.
 * - Los mensajes importantes se obtienen del chat: se filtran por pinned=true sobre los mensajes
 *   ya devueltos por el endpoint paginado del chat (por simplicidad en esta primera versión).
 */
@Composable
fun GroupInfoScreen(
    classId: Long?,
    className: String,
    classAvatarIndex: Int,
    isTeacher: Boolean,
    chatViewModel: ChatViewModel,
    teacherRepository: TeacherRepository,
    classRepository: ClassRepository,
    classesViewModel: ClassesViewModel,
    onJumpToMessage: (messageId: Long) -> Unit,
) {
    val context = LocalContext.current

    val classes by classesViewModel.classes.observeAsState(emptyList())
    val currentClass = remember(classes, classId) { classes.firstOrNull { it.id == classId } }

    val students = remember { mutableStateOf<List<ClassEnrollmentResponseDTO>>(emptyList()) }

    val inviteLoading = remember { mutableStateOf(false) }
    val inviteError = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val chatMessages by chatViewModel.messages.observeAsState(emptyList())
    val pinnedMessages = remember(chatMessages) {
        chatMessages
            .filter { it.pinned }
            .sortedBy { it.createdAt }
    }

    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(classId, isTeacher) {
        if (classId == null) return@LaunchedEffect

        students.value = emptyList()
        error.value = null

        // alumnos (sólo profesor)
        if (isTeacher) {
            isLoading.value = true
            val result = teacherRepository.listClassStudents(classId)
            isLoading.value = false

            result
                .onSuccess { students.value = it }
                .onFailure { error.value = it.message ?: context.getString(R.string.load_error) }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header grande (avatar + nombre)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarRes = classGroupAvatarRes(classAvatarIndex)
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = stringResource(R.string.group_avatar_description),
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = className,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.group_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isLoading.value) {
            CircularProgressIndicator()
        }

        if (!error.value.isNullOrBlank()) {
            Text(text = error.value!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        if (isTeacher) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !inviteLoading.value,
                onClick = {
                    if (classId == null) return@Button
                    inviteError.value = null
                    inviteLoading.value = true

                    coroutineScope.launch {
                        val result = classRepository.createClassInvitation(classId = classId)
                        inviteLoading.value = false

                        result
                            .onSuccess { inv ->
                                // El backend devuelve una URL lista para usar
                                val url = inv.url
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, url)
                                }
                                val chooser = Intent.createChooser(sendIntent, "Compartir enlace de invitación")
                                context.startActivity(chooser)
                            }
                            .onFailure { e ->
                                inviteError.value = e.message ?: "Error creando invitación"
                            }
                    }
                }
            ) {
                if (inviteLoading.value) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                }
                Text(stringResource(R.string.invite_student_generate_link))
            }

            if (!inviteError.value.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text = inviteError.value!!, color = MaterialTheme.colorScheme.error)
            }
        }

        // Profesores
        Text(
            text = "Profesores",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val teacherEmail = currentClass?.teacherEmail
        if (!teacherEmail.isNullOrBlank()) {
            Text(
                text = teacherEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = stringResource(R.string.account_type_teacher_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        // Alumnos
        Text(
            text = stringResource(R.string.students),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (classId == null) {
            Text(stringResource(R.string.no_class_selected))
            return
        }

        if (isTeacher) {
            if (students.value.isEmpty() && !isLoading.value) {
                Text(stringResource(R.string.no_students_in_class))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students.value) { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = stringResource(R.string.student),
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.student), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = s.studentEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "No se pueden listar compañeros desde cuenta de alumno con el backend actual.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        // Mensajes importantes/fijados
        Text(
            text = "Mensajes fijados / importantes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (pinnedMessages.isEmpty()) {
            Text(
                text = "No hay mensajes fijados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pinnedMessages) { m ->
                    PinnedMessageCard(
                        message = m,
                        onClick = { onJumpToMessage(m.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedMessageCard(
    message: ChatMessageResponseDTO,
    onClick: () -> Unit
) {
    val deviceZone = remember { ZoneId.systemDefault() }
    val dateTimeText = remember(message.createdAt) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        runCatching {
            Instant.parse(message.createdAt).atZone(deviceZone).format(formatter)
        }.getOrNull()
            ?: runCatching {
                OffsetDateTime.parse(message.createdAt).atZoneSameInstant(deviceZone).format(formatter)
            }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(message.createdAt)
                    .atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(deviceZone)
                    .format(formatter)
            }.getOrNull()
            ?: message.createdAt
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📌", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dateTimeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
