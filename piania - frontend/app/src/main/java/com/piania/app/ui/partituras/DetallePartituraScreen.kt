package com.piania.app.ui.partituras

import android.Manifest
import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.piania.app.R
import com.piania.app.data.model.response.AnalisisResponseDTO
import com.piania.app.ui.ads.AdManager
import com.piania.app.util.AudioRecorder
import com.piania.app.util.Metronome
import com.piania.app.data.model.response.ShareLinkResponseDTO
import java.io.File

/**
 * Pantalla Compose reutilizable para el detalle de una partitura.
 * Sustituye el uso de DetallePartituraActivity para poder navegar dentro de MainActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePartituraScreen(
    viewModel: DetallePartituraViewModel,
    idPartitura: Long,
    modoInicial: ModoPartitura,
    onBack: () -> Unit,
    practicesViewModel: PracticesViewModel? = null,
    onShareToChat: (ShareLinkResponseDTO) -> Unit = {}
) {
    val context = LocalContext.current
    val audioNoEncontrado=stringResource(R.string.audio_not_found)
    val audioListoParaEnviar=stringResource(R.string.audio_ready_send)
    val errorPrefijo=stringResource(R.string.error_prefix)
    val grabando=stringResource(R.string.recording)
    val permisosMicro=stringResource(R.string.mic_permission_needed)
    val errorCarga=stringResource(R.string.load_error)

    if (idPartitura == -1L) {
        // Estado inválido: evitamos crashear
        LaunchedEffect(Unit) {
            Toast.makeText(context, errorCarga, Toast.LENGTH_SHORT).show()
            onBack()
        }
        return
    }

    // Premium eliminado: la carga de anuncios se gestiona desde la Activity

    // --- 1. CONFIGURACIÓN DE GRABACIÓN ---
    val recorder = remember { AudioRecorder(context) }
    var archivoGrabado by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingFinished by remember { mutableStateOf(false) }

    // --- METRÓNOMO (MVP) ---
    var metronomeEnabled by remember { mutableStateOf(true) }
    var metronomeBpm by remember { mutableStateOf(90) }
    val composeScope = rememberCoroutineScope()
    val metronome = remember { Metronome(composeScope) }
    DisposableEffect(Unit) {
        onDispose { metronome.release() }
    }

    // --- 1.1 GESTOR DE PERMISOS ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isRecording = true
                val fileName = "rec_${idPartitura}_${System.currentTimeMillis()}.m4a"
                recorder.startRecording(fileName)
                Toast.makeText(context, grabando, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, permisosMicro, Toast.LENGTH_LONG).show()
            }
        }
    )

    // --- 2. ESTADOS DE UI ---
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var visorListo by remember { mutableStateOf(false) }
    var isPlayingUi by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    // Nunca abrimos el sheet automáticamente.
    // Solo se abre cuando el usuario pulsa "Ver Detalles".
    var showAnalysisSheet by remember { mutableStateOf(false) }
    var showPracticesSheet by remember { mutableStateOf(false) }
    var analisisData by remember { mutableStateOf<AnalisisResponseDTO?>(null) }
    var analisisCargado by remember { mutableStateOf(false) }

    // Para que deje de sonar la partitura después de salirse del modo de vista
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.evaluateJavascript("stop()", null)
        }
    }

    // Si estamos en LECTURA/GRABACION, nunca debe aparecer la bottom sheet de análisis.
    // Esto arregla el “se queda cargando” (sheet en blanco con spinner) aunque se vea la partitura por debajo.
    LaunchedEffect(modoInicial) {
        if (modoInicial != ModoPartitura.ANALISIS) {
            showAnalysisSheet = false
        }
    }

    // Estados prácticas
    val practicesState = practicesViewModel?.state?.collectAsState()
    val practiceDetailState = practicesViewModel?.detailState?.collectAsState()

    // Carga inicial
    LaunchedEffect(idPartitura) {
        visorListo = false
        isPlayingUi = false
        viewModel.cargarPartitura(idPartitura)

        // Cargamos prácticas cuando entramos a la partitura (si se inyecta el VM).
        practicesViewModel?.loadBySheetMusic(idPartitura)

        // NO cargamos análisis automáticamente al entrar.
        // Solo cuando el usuario lo solicita explícitamente.
    }

    // A) RESPUESTA DEL ANÁLISIS TEÓRICO (Music21)
    LaunchedEffect(viewModel.feedbackState) {
        when (val state = viewModel.feedbackState) {
            is FeedbackState.Exito -> {
                analisisData = state.analisis
                analisisCargado = true
                showAnalysisSheet = true
                viewModel.resetFeedbackState()
            }

            is FeedbackState.Error -> {
                analisisData = null
                analisisCargado = true
                Toast.makeText(context, state.mensaje, Toast.LENGTH_LONG).show()
            }

            is FeedbackState.Vacio -> {
                analisisData = null
                analisisCargado = true
            }

            else -> Unit
        }
    }

    // B) RESPUESTA DE LA PRÁCTICA (Audio)
    when (val estado = viewModel.estadoPractica) {
        is EstadoPractica.Cargando -> {
            AlertDialog(
                onDismissRequest = {},
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.evaluating_performance))
                    }
                },
                confirmButton = {}
            )
        }

        is EstadoPractica.Exito -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetEstadoPractica() },
                title = { Text(stringResource(R.string.results)) },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.score_format, estado.feedback.puntuacionGeneral),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (estado.feedback.puntuacionGeneral > 80) Color.Green else Color.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.note_errors_format, estado.feedback.erroresNota))
                        Text(stringResource(R.string.rhythm_errors_format, estado.feedback.erroresRitmo))
                        val comentarios = estado.feedback.comentarios

                        if (!comentarios.isNullOrEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.comments_format, comentarios),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.resetEstadoPractica()
                        practicesViewModel?.loadBySheetMusic(idPartitura)
                    }) { Text(stringResource(R.string.accept)) }
                }
            )
        }

        is EstadoPractica.Error -> {
            LaunchedEffect(estado.mensaje) {
                Toast.makeText(
                    context,
                    errorPrefijo.format(estado.mensaje),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetEstadoPractica()
            }
        }

        else -> Unit
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(
                    when (modoInicial) {
                        ModoPartitura.GRABACION -> if (isRecording) 140.dp else 96.dp
                        else -> 88.dp
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (modoInicial) {
                        ModoPartitura.LECTURA -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    androidx.compose.material3.FloatingActionButton(
                                        onClick = {
                                            if (isPlayingUi) {
                                                webViewRef?.evaluateJavascript("pause()", null)
                                                isPlayingUi = false
                                            } else {
                                                webViewRef?.evaluateJavascript("play()", null)
                                                isPlayingUi = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingUi) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingUi) "Pause" else "Play"
                                        )
                                    }

                                    androidx.compose.material3.FloatingActionButton(
                                        onClick = {
                                            webViewRef?.evaluateJavascript("stop()", null)
                                            isPlayingUi = false
                                        },
                                        containerColor = Color.Red,
                                        contentColor = Color.White
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                                    }

                                    androidx.compose.material3.FloatingActionButton(
                                        onClick = {
                                            webViewRef?.evaluateJavascript("zoomOut()", null)
                                        }
                                    ) {
                                        Text("-")
                                    }

                                    androidx.compose.material3.FloatingActionButton(
                                        onClick = {
                                            webViewRef?.evaluateJavascript("zoomIn()", null)
                                        }
                                    ) {
                                        Text("+")
                                    }
                                }
                            }
                        }

                        ModoPartitura.GRABACION -> {
                            if (!recordingFinished) {
                                if (isRecording) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    stringResource(R.string.recording),
                                                    color = Color.Red,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    stringResource(R.string.bpm_format, metronomeBpm),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }

                                            androidx.compose.material3.FloatingActionButton(
                                                onClick = {
                                                    isRecording = false
                                                    recordingFinished = true
                                                    metronome.stop()
                                                    archivoGrabado = recorder.stopRecording()

                                                    if (archivoGrabado != null) {
                                                        Toast.makeText(
                                                            context,
                                                            audioListoParaEnviar,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                containerColor = Color.Red,
                                                contentColor = Color.White
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = "Detener")
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    if (metronomeBpm > 40) {
                                                        metronomeBpm -= 1
                                                        metronome.setTempo(metronomeBpm)
                                                    }
                                                }
                                            ) {
                                                Text("-")
                                            }

                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    metronomeEnabled = !metronomeEnabled
                                                    if (!metronomeEnabled) {
                                                        metronome.stop()
                                                    } else {
                                                        metronome.start(metronomeBpm)
                                                    }
                                                }
                                            ) {
                                                Text(if (metronomeEnabled) "♪ ON" else "♪ OFF")
                                            }

                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    if (metronomeBpm < 240) {
                                                        metronomeBpm += 1
                                                        metronome.setTempo(metronomeBpm)
                                                    }
                                                }
                                            ) {
                                                Text("+")
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MetronomeControls(
                                                bpm = metronomeBpm,
                                                enabled = metronomeEnabled,
                                                onBpmChange = {
                                                    metronomeBpm = it
                                                    metronome.setTempo(it)
                                                },
                                                onToggle = { metronomeEnabled = !metronomeEnabled }
                                            )

                                            ExtendedFloatingActionButton(
                                                onClick = {
                                                    if (
                                                        ContextCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.RECORD_AUDIO
                                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    ) {
                                                        isRecording = true
                                                        val fileName = "rec_${idPartitura}_${System.currentTimeMillis()}.m4a"
                                                        recorder.startRecording(fileName)
                                                        if (metronomeEnabled) metronome.start(metronomeBpm)
                                                    } else {
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Mic, "micrófono") },
                                                text = { Text(stringResource(R.string.start_recording)) },
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = {
                                                recordingFinished = false
                                                archivoGrabado = null
                                            }
                                        ) {
                                            Text(stringResource(R.string.repeat))
                                        }

                                        Button(
                                            onClick = {
                                                archivoGrabado?.let { file ->
                                                    viewModel.subirGrabacion(idPartitura, file)
                                                } ?: Toast.makeText(
                                                    context,
                                                    audioNoEncontrado,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.get_feedback))
                                        }
                                    }
                                }
                            }
                        }

                        ModoPartitura.ANALISIS -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        analisisCargado = false
                                        analisisData = null
                                        showAnalysisSheet = true

                                        if (viewModel.feedbackState !is FeedbackState.Enviando) {
                                            viewModel.cargarAnalisisHistorico(idPartitura)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Info, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.view_details))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (viewModel.xmlContent != null) {
                VisorPartitura(
                    xmlContent = viewModel.xmlContent,
                    modifier = Modifier.fillMaxSize(),
                    onWebViewCreated = { webViewRef = it },
                    onRendered = { visorListo = true }
                )

                // Eliminamos spinner adicional del WebView para evitar falsos "cargando"
                // El propio WebView renderiza cuando está listo.
            } else if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (!viewModel.errorMessage.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(viewModel.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            }
        }

        // --- HOJA DE ANÁLISIS ---
        if (showAnalysisSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAnalysisSheet = false },
                sheetState = sheetState
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !analisisCargado -> {
                            CircularProgressIndicator()
                        }

                        analisisData == null -> {
                            Text(
                                "No hay análisis previo disponible para esta partitura.",
                                color = Color.Gray
                            )
                        }

                        else -> {
                            PanelResultadosAnalisis(analisis = analisisData!!)
                        }
                    }
                }
            }
        }

        // --- HOJA / PANEL DE PRÁCTICAS ---
        // Solo se muestra si se inyecta PracticesViewModel.
        if (practicesViewModel != null && practicesState != null && showPracticesSheet) {
            when (val s = practicesState.value) {
                is PracticesViewModel.UiState.Loading -> {
                    ModalBottomSheet(
                        onDismissRequest = { showPracticesSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) { CircularProgressIndicator() }
                    }
                }

                is PracticesViewModel.UiState.Error -> {
                    ModalBottomSheet(
                        onDismissRequest = { showPracticesSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                    ) {
                        Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                is PracticesViewModel.UiState.Loaded -> {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showPracticesSheet = false
                            practicesViewModel.closePracticeDetail()
                        },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                    ) {
                        PracticesPanel(
                            items = s.items,
                            detailState = practiceDetailState?.value,
                            onOpenDetail = { practicesViewModel.openPracticeDetail(it) },
                            onSaveNotes = { id, obs, corr -> practicesViewModel.saveNotes(id, obs, corr) },
                            onPlaybackStart = { feedback ->
                                val score = feedback?.precisionGeneral ?: 0
                                val noteErrors = feedback?.noteErrors ?: 0
                                val rhythmErrors = feedback?.rhythmErrors ?: 0
                                webViewRef?.evaluateJavascript(
                                    "window.PianIA && window.PianIA.showPracticeFeedback($score,$noteErrors,$rhythmErrors)",
                                    null
                                )
                            },
                            onCloseDetail = { practicesViewModel.closePracticeDetail() }
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun MetronomeControls(
    bpm: Int,
    enabled: Boolean,
    onBpmChange: (Int) -> Unit,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.material3.OutlinedButton(
            onClick = { onBpmChange((bpm - 5).coerceAtLeast(30)) }
        ) { Text("-") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.bpm_format, bpm), style = MaterialTheme.typography.labelMedium)
            Slider(
                value = bpm.toFloat(),
                onValueChange = { onBpmChange(it.toInt().coerceIn(30, 240)) },
                valueRange = 30f..240f,
                steps = 41,
                modifier = Modifier.width(96.dp)
            )
        }
        androidx.compose.material3.OutlinedButton(
            onClick = { onBpmChange((bpm + 5).coerceAtMost(240)) }
        ) { Text("+") }
        androidx.compose.material3.OutlinedButton(onClick = onToggle) {
            Text(if (enabled) "ON" else "OFF")
        }
    }
}

@Composable
private fun ControlBoton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun PanelResultadosAnalisis(analisis: AnalisisResponseDTO) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.performance_analysis), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(analisis.fechaAnalisis ?: stringResource(R.string.just_analyzed), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DatoCard(stringResource(R.string.key_signature), analisis.tonalidad ?: "-", Icons.Default.MusicNote, Modifier.weight(1f))
            DatoCard(stringResource(R.string.time_signature), analisis.compas ?: "-", Icons.Default.Timer, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.detected_tempo), style = MaterialTheme.typography.labelMedium)
                    Text(
                        stringResource(R.string.bpm_format, analisis.tempoDetectado ?: 0),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.estimated_difficulty), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        val dificultad = analisis.dificultadEstimada.toFloat()
        val progreso = (dificultad / 10f).coerceIn(0f, 1f)

        LinearProgressIndicator(
            progress = { progreso },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color =
                if (progreso > 0.7f) Color.Red else if (progreso > 0.4f) Color(0xFFFF9800) else Color(0xFF4CAF50),
        )
        Text("$dificultad / 10", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.End))
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DatoCard(
    titulo: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VisorPartitura(
    xmlContent: String?,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onRendered: () -> Unit
) {

    AndroidView(

        modifier = modifier,

        factory = { context ->

            WebView(context).apply {

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {

                    javaScriptEnabled = true

                    domStorageEnabled = true

                    allowFileAccess = true

                    loadWithOverviewMode = true

                    useWideViewPort = true

                    builtInZoomControls = true

                    displayZoomControls = false

                    mediaPlaybackRequiresUserGesture = false
                }

                webViewClient = object : WebViewClient() {

                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {

                        super.onPageFinished(view, url)

                        if (!xmlContent.isNullOrEmpty()) {

                            cargarXmlEnJs(this@apply, xmlContent)

                            onRendered()
                        }
                    }
                }

                loadUrl("file:///android_asset/visor.html")

                onWebViewCreated(this)
            }
        }
    )
}

private fun cargarXmlEnJs(webView: WebView, xml: String) {
    try {
        val encodedXml = Base64.encodeToString(xml.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val jsCommand = "if(typeof cargarPartitura === 'function') { cargarPartitura('$encodedXml'); }"
        webView.evaluateJavascript(jsCommand, null)
    } catch (e: Exception) {
        Log.e("VISOR", "Error encoding XML: ${e.message}")
    }
}
