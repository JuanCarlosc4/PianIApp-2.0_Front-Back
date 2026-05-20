package com.piania.app.ui.main

import android.widget.Toast
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.piania.app.R
import com.piania.app.data.JwtUtils
import com.piania.app.data.SessionManager
import com.piania.app.ui.chat.ChatViewModel
import com.piania.app.ui.classes.ClassesViewModel
import com.piania.app.data.model.response.ShareLinkResponseDTO

@Composable
fun StudentClassesScreen(
    viewModel: ClassesViewModel,
    isTeacher: Boolean,
    onBack: () -> Unit,
    onOpenChat: (classId: Long) -> Unit
) {
    val classes by viewModel.classes.observeAsState(emptyList())
    val unreadCounts by viewModel.unreadCounts.observeAsState(emptyMap())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()

    LaunchedEffect(isTeacher) {
        viewModel.loadClasses(isTeacher = isTeacher)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.my_classes),
            style = MaterialTheme.typography.headlineSmall
        )

        if (isLoading) CircularProgressIndicator()

        if (!error.isNullOrBlank()) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(classes) { c ->
                val unread = unreadCounts[c.id] ?: 0

                OutlinedButton(
                    onClick = { onOpenChat(c.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val avatarRes = classGroupAvatarRes(c.groupAvatarIndex())
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = stringResource(R.string.group_avatar_description),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = c.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.open_chat),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (unread > 0) {
                            val label = if (unread > 99) "99+" else unread.toString()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.loadClasses(isTeacher = isTeacher) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.refresh))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

private fun extractSheetMusicShareToken(message: String, prefix: String): String? {
    if (!message.startsWith(prefix)) return null
    val token = message.removePrefix(prefix).trim()
    if (token.length !in 6..200) return null
    return token.takeIf { it.all { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' } }
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    sessionManager: SessionManager,
    className: String,
    classAvatarIndex: Int,
    classCreatedAt: String?,
    scrollToMessageId: Long?,
    onConsumedScrollToMessage: () -> Unit,
    onBack: () -> Unit,
    onOpenGroupInfo: () -> Unit,
    pendingShareLink: ShareLinkResponseDTO?,
    onConsumedShareLink: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.messages.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val importingSheetMusic by viewModel.importingSheetMusic.observeAsState(false)
    val importSheetMusicResult by viewModel.importSheetMusicResult.observeAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.detenerPolling() }
    }

    val inferredClassId: Long? = remember(messages) { messages.firstOrNull()?.classId }
    val orderedMessages: List<com.piania.app.data.model.response.ChatMessageResponseDTO> =
        remember(messages) { messages.sortedBy { it.createdAt } }

    val groupCreatedFormat = stringResource(R.string.group_created_format)
    val groupCreatedLabel = remember(classCreatedAt, orderedMessages, groupCreatedFormat) {
        val deviceZone = ZoneId.systemDefault()
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val baseDate = classCreatedAt?.let { createdAt ->
            runCatching { Instant.parse(createdAt).atZone(deviceZone).toLocalDate() }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(createdAt).atZoneSameInstant(deviceZone).toLocalDate() }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(createdAt)
                        .atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(deviceZone)
                        .toLocalDate()
                }.getOrNull()
        } ?: orderedMessages.firstOrNull()?.createdAt?.let { firstMsgCreatedAt ->
            runCatching { Instant.parse(firstMsgCreatedAt).atZone(deviceZone).toLocalDate() }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(firstMsgCreatedAt).atZoneSameInstant(deviceZone).toLocalDate() }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(firstMsgCreatedAt)
                        .atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(deviceZone)
                        .toLocalDate()
                }.getOrNull()
        }

        baseDate?.let { d -> groupCreatedFormat.format(d.format(dateFormatter)) }
    }

    val isTeacher = remember { JwtUtils.isTeacher(sessionManager.fetchAuthToken()) }
    val myEmail = remember { JwtUtils.getEmail(sessionManager.fetchAuthToken()) }

    var input by rememberSaveable { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(importSheetMusicResult) {
        val msg = importSheetMusicResult ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearImportSheetMusicResult()
    }

    val sharePrefix = stringResource(R.string.sent_sheet_music_prefix)
    LaunchedEffect(pendingShareLink) {
        val link = pendingShareLink ?: return@LaunchedEffect
        input = buildString {
            append(sharePrefix)
            append(link.token)
        }
        onConsumedShareLink()
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val lastSeenMessageId: Long? = remember(inferredClassId) {
        inferredClassId?.let { sessionManager.getChatLastSeenMessageId(it) }
    }

    val firstUnreadMessageId: Long? = remember(orderedMessages, lastSeenMessageId) {
        if (lastSeenMessageId == null) return@remember null
        val lastSeenIndex = orderedMessages.indexOfFirst { it.id == lastSeenMessageId }
        if (lastSeenIndex < 0) return@remember null
        orderedMessages.getOrNull(lastSeenIndex + 1)?.id
    }

    LaunchedEffect(scrollToMessageId, orderedMessages) {
        val targetId = scrollToMessageId ?: return@LaunchedEffect
        val index = orderedMessages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            coroutineScope.launch { listState.animateScrollToItem(index + 1) }
        }
        onConsumedScrollToMessage()
    }

    LaunchedEffect(orderedMessages, firstUnreadMessageId, scrollToMessageId) {
        if (scrollToMessageId != null) return@LaunchedEffect
        if (orderedMessages.isEmpty()) return@LaunchedEffect

        val targetIndex =
            if (firstUnreadMessageId != null) orderedMessages.indexOfFirst { it.id == firstUnreadMessageId }
            else orderedMessages.lastIndex

        if (targetIndex >= 0) coroutineScope.launch { listState.scrollToItem(targetIndex + 1) }
    }

    LaunchedEffect(orderedMessages.size) {
        if (orderedMessages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(orderedMessages.lastIndex + 1) }
        }
    }

    val floatingDayLabel by remember(listState, orderedMessages) {
        derivedStateOf {
            val msgIndex = listState.firstVisibleItemIndex - 1
            val msg = orderedMessages.getOrNull(msgIndex) ?: return@derivedStateOf null

            val deviceZone = ZoneId.systemDefault()
            val msgDate: LocalDate = runCatching {
                Instant.parse(msg.createdAt).atZone(deviceZone).toLocalDate()
            }.getOrNull()
                ?: runCatching {
                    OffsetDateTime.parse(msg.createdAt).atZoneSameInstant(deviceZone).toLocalDate()
                }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(msg.createdAt)
                        .atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(deviceZone)
                        .toLocalDate()
                }.getOrNull()
                ?: return@derivedStateOf null

            val today = LocalDate.now(deviceZone)
            if (msgDate == today) return@derivedStateOf null

            val locale = Locale.getDefault()
            val dayName = msgDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
            "${dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }}, ${msgDate.format(dateFormatter)}"
        }
    }

    val unreadMessagesLabel = stringResource(R.string.unread_messages)
    val userLabel = stringResource(R.string.user_label)
    val unpinLabel = stringResource(R.string.unpin)
    val pinLabel = stringResource(R.string.pin)
    val deleteLabel = stringResource(R.string.delete)
    val avatarDescription = stringResource(R.string.avatar)
    val saveSheetLabel = stringResource(R.string.save_sheet_music)
    val messagePlaceholder = stringResource(R.string.message_placeholder)
    val sendDescription = stringResource(R.string.send_description)
    val savingSheetLabel = stringResource(R.string.saving_sheet_music)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) CircularProgressIndicator()

            if (!error.isNullOrBlank()) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    if (!groupCreatedLabel.isNullOrBlank() && orderedMessages.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 1.dp
                            ) {
                                Text(
                                    text = groupCreatedLabel!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                items(items = orderedMessages, key = { it.id }) { m ->
                    if (firstUnreadMessageId != null && m.id == firstUnreadMessageId) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Text(
                                text = unreadMessagesLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }

                    val isMine = !myEmail.isNullOrBlank() && m.senderEmail.equals(myEmail, ignoreCase = true)

                    var menuExpanded by remember { mutableStateOf(false) }
                    val canPin = isTeacher
                    val canDelete = isMine || isTeacher

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { if (canPin || canDelete) menuExpanded = true }
                            ),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isMine) {
                            val avatarRes = userAvatarRes(m.senderAvatar)
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = avatarDescription,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .padding(vertical = 2.dp),
                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                        ) {
                            if (!isMine) {
                                Text(
                                    text = m.senderName?.takeIf { it.isNotBlank() } ?: userLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color =
                                        if (m.senderIsTeacher == true) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 2.dp)
                                )
                            }

                            val timeText: String? = remember(m.createdAt) {
                                val deviceZone = ZoneId.systemDefault()
                                val formatter = DateTimeFormatter.ofPattern("HH:mm")

                                runCatching {
                                    Instant.parse(m.createdAt)
                                        .atZone(deviceZone)
                                        .toLocalTime()
                                        .format(formatter)
                                }.getOrNull()
                                    ?: runCatching {
                                        OffsetDateTime.parse(m.createdAt)
                                            .atZoneSameInstant(deviceZone)
                                            .toLocalTime()
                                            .format(formatter)
                                    }.getOrNull()
                                    ?: runCatching {
                                        LocalDateTime.parse(m.createdAt)
                                            .atZone(ZoneId.of("UTC"))
                                            .withZoneSameInstant(deviceZone)
                                            .toLocalTime()
                                            .format(formatter)
                                    }.getOrNull()
                                    ?: runCatching { m.createdAt.takeLast(5) }.getOrNull()
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isMine && !timeText.isNullOrBlank()) {
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color =
                                        if (isMine) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (m.pinned) {
                                                Text(text = "📌", style = MaterialTheme.typography.labelSmall)
                                            }

                                            Spacer(Modifier.weight(1f, fill = true))

                                            Box(modifier = Modifier.semantics { contentDescription = "Acciones mensaje" }) {
                                                DropdownMenu(
                                                    expanded = menuExpanded,
                                                    onDismissRequest = { menuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text(if (m.pinned) unpinLabel else pinLabel) },
                                                        enabled = canPin,
                                                        onClick = {
                                                            menuExpanded = false
                                                            if (m.pinned) viewModel.unpinMessage(m.id) else viewModel.pinMessage(m.id)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(deleteLabel) },
                                                        enabled = canDelete,
                                                        onClick = {
                                                            menuExpanded = false
                                                            viewModel.deleteMessage(m.id)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = m.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color =
                                                if (isMine) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val shareToken = remember(m.message, sharePrefix) {
                                            extractSheetMusicShareToken(m.message, sharePrefix)
                                        }
                                        if (!shareToken.isNullOrBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedButton(
                                                onClick = { viewModel.importSheetMusicFromToken(shareToken) },
                                                enabled = !importingSheetMusic
                                            ) {
                                                Text(saveSheetLabel)
                                            }
                                        }
                                    }
                                }

                                if (!isMine) {
                                    Text(
                                        text = timeText?.takeIf { it.isNotBlank() } ?: "--:--",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip,
                                        modifier = Modifier
                                            .widthIn(min = 42.dp)
                                            .padding(bottom = 2.dp)
                                            .align(Alignment.Bottom)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(messagePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = {
                        viewModel.sendMessage(input)
                        input = ""

                        inferredClassId?.let { classId ->
                            val lastId = orderedMessages.lastOrNull()?.id
                            if (lastId != null) {
                                coroutineScope.launch { sessionManager.setChatLastSeenMessageId(classId, lastId) }
                            }
                        }
                    },
                    enabled = input.isNotBlank() && !isLoading && !importingSheetMusic,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = sendDescription }
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color =
                            if (input.isNotBlank() && !isLoading && !importingSheetMusic) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("➤", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            if (importingSheetMusic) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = savingSheetLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!floatingDayLabel.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = floatingDayLabel!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
