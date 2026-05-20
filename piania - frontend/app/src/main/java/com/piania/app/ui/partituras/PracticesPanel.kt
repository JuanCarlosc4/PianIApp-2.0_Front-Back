package com.piania.app.ui.partituras

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piania.app.BuildConfig
import com.piania.app.R
import com.piania.app.data.model.response.PracticeFeedbackResponseDTO
import com.piania.app.ui.FormatoFecha

@Composable
fun PracticesPanel(
    items: List<PracticesViewModel.PracticeItemUi>,
    detailState: PracticesViewModel.DetailState?,
    onOpenDetail: (practiceId: Long) -> Unit,
    onSaveNotes: (practiceId: Long, studentObservations: String?, teacherCorrections: String?) -> Unit,
    onPlaybackStart: (PracticeFeedbackResponseDTO?) -> Unit = {},
    onCloseDetail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.practices),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { it ->
                PracticeRow(it, onClick = { onOpenDetail(it.id) })
            }
        }

        when (val ds = detailState) {
            is PracticesViewModel.DetailState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PracticesViewModel.DetailState.Error -> {
                Text(ds.message, color = MaterialTheme.colorScheme.error)
            }

            is PracticesViewModel.DetailState.Loaded -> {
                PracticeDetail(
                    practiceId = ds.practice.id,
                    audioUrl = ds.practice.audioUrl,
                    score = ds.practice.score,
                    durationSeconds = ds.practice.durationSeconds,
                    createdAt = ds.practice.createdAt,
                    feedbackSummary = null,
                    feedbackJson = ds.feedback?.detailedReport,
                    initialStudentObservations = ds.practice.studentObservations,
                    initialTeacherCorrections = ds.practice.teacherCorrections,
                    onSaveNotes = onSaveNotes,
                    onPlaybackStart = { onPlaybackStart(ds.feedback) },
                    onClose = onCloseDetail
                )
            }

            else -> Unit
        }
    }
}

private fun normalizeMediaUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    return BuildConfig.BASE_URL.trimEnd('/') + "/" + trimmed.trimStart('/')
}

@Composable
private fun PracticeRow(
    item: PracticesViewModel.PracticeItemUi,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.session_id_format, item.id.toString()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (item.score != null) {
                Text(
                    text = "${item.score}/100",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.score >= 80) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(text = FormatoFecha.formatearFecha(item.createdAt) ?: "-", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(text = stringResource(R.string.duration_format, item.durationSeconds), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PracticeDetail(
    practiceId: Long,
    audioUrl: String,
    score: Int?,
    durationSeconds: Int,
    createdAt: String?,
    feedbackSummary: String?,
    feedbackJson: String?,
    initialStudentObservations: String?,
    initialTeacherCorrections: String?,
    onSaveNotes: (practiceId: Long, studentObservations: String?, teacherCorrections: String?) -> Unit,
    onPlaybackStart: () -> Unit,
    onClose: () -> Unit
) {
    var studentObs by remember(initialStudentObservations) { mutableStateOf(initialStudentObservations ?: "") }
    var teacherCorr by remember(initialTeacherCorrections) { mutableStateOf(initialTeacherCorrections ?: "") }
    val playableAudioUrl = remember(audioUrl) { normalizeMediaUrl(audioUrl) }

    // --- Reproducción (simple) del audio remoto ---
    val player = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var playerPrepared by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(audioUrl) {
        // Reiniciamos el player si cambia el audio
        runCatching {
            player.reset()
            playerPrepared = false
            isPlaying = false
            playerError = null

            if (playableAudioUrl.isNotBlank()) {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                player.setDataSource(playableAudioUrl)
                player.setOnPreparedListener {
                    playerPrepared = true
                }
                player.setOnCompletionListener {
                    isPlaying = false
                }
                player.setOnErrorListener { _, what, extra ->
                    playerError = "Error reproduciendo audio (what=$what, extra=$extra)"
                    isPlaying = false
                    true
                }
                player.prepareAsync()
            }
        }.onFailure { e ->
            playerError = e.message ?: "Error preparando audio"
        }

        onDispose {
            runCatching { player.stop() }
            runCatching { player.reset() }
            runCatching { player.release() }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.session_detail_format, practiceId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        }

        Text(text = stringResource(R.string.date_format, FormatoFecha.formatearFecha(createdAt) ?: "-"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = stringResource(R.string.duration_format, durationSeconds), style = MaterialTheme.typography.bodySmall)
        Text(text = stringResource(R.string.audio_url_format, playableAudioUrl), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

        if (playableAudioUrl.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    enabled = playerPrepared && !isPlaying,
                    onClick = {
                        if (playerPrepared) {
                            runCatching {
                                player.start()
                                isPlaying = true
                                onPlaybackStart()
                            }.onFailure { e ->
                                playerError = e.message ?: "Error al reproducir"
                            }
                        }
                    }
                ) { Text(stringResource(R.string.play_action)) }

                OutlinedButton(
                    enabled = playerPrepared && isPlaying,
                    onClick = {
                        runCatching {
                            player.pause()
                            isPlaying = false
                        }.onFailure { e ->
                            playerError = e.message ?: "Error al pausar"
                        }
                    }
                ) { Text(stringResource(R.string.pause_action)) }

                OutlinedButton(
                    enabled = playerPrepared,
                    onClick = {
                        runCatching {
                            player.pause()
                            player.seekTo(0)
                            isPlaying = false
                        }.onFailure { e ->
                            playerError = e.message ?: "Error al parar"
                        }
                    }
                ) { Text(stringResource(R.string.stop_action)) }
            }

            if (!playerError.isNullOrBlank()) {
                Text(text = playerError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else if (!playerPrepared) {
                Text(text = stringResource(R.string.loading_audio), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        if (score != null) {
            Text(text = stringResource(R.string.score_format, score), fontWeight = FontWeight.SemiBold)
        }

        if (!feedbackSummary.isNullOrBlank() || !feedbackJson.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.feedback), fontWeight = FontWeight.SemiBold)
            if (!feedbackSummary.isNullOrBlank()) {
                Text(text = feedbackSummary, style = MaterialTheme.typography.bodySmall)
            }
            if (!feedbackJson.isNullOrBlank()) {
                Text(text = feedbackJson, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.student_notes), fontWeight = FontWeight.SemiBold)
        TextField(
            value = studentObs,
            onValueChange = { studentObs = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false
        )

        Text(stringResource(R.string.teacher_corrections), fontWeight = FontWeight.SemiBold)
        TextField(
            value = teacherCorr,
            onValueChange = { teacherCorr = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { onSaveNotes(practiceId, studentObs, teacherCorr) }) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}
