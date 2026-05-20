package com.piania.app.ui.partituras

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import com.piania.app.R
import com.piania.app.data.model.response.PartituraResponseDTO
import com.piania.app.data.model.response.ShareLinkResponseDTO
import com.piania.app.ui.FormatoFecha

/**
 * Pantalla de listado de partituras en Compose.
 * Antes estaba embebida en PartiturasActivity (legacy). Ahora vive aquí para que MainActivity la pueda usar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiturasScreen(
    viewModelDetallePartitura: DetallePartituraViewModel,
    viewModel: PartiturasViewModel,
    practicesViewModel: PracticesViewModel? = null,
    onNavigateToAdd: () -> Unit,
    onOpenSheet: (sheetId: Long, mode: ModoPartitura) -> Unit,
    onShareToChat: (ShareLinkResponseDTO) -> Unit = {}
) {
    val partituras by viewModel.partituras.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.errorMessage.observeAsState(null)
    val pullRefreshState = rememberPullToRefreshState()

    // Carga inicial / polling
    LaunchedEffect(Unit) { viewModel.loadPartituras() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            state = pullRefreshState,
            onRefresh = { viewModel.loadPartituras() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isLoading && partituras.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(partituras) { partitura ->
                        PartituraRow(
                            context = LocalContext.current,
                            viewModelDetallePartitura = viewModelDetallePartitura,
                            practicesViewModel = practicesViewModel,
                            partitura = partitura,
                            onOpen = { modo -> onOpenSheet(partitura.idPartitura, modo) },
                            onShareToChat = onShareToChat
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PartituraRow(
    context: Context,
    viewModelDetallePartitura: DetallePartituraViewModel,
    practicesViewModel: PracticesViewModel?,
    partitura: PartituraResponseDTO,
    onOpen: (ModoPartitura) -> Unit,
    onShareToChat: (ShareLinkResponseDTO) -> Unit
) {
    val statusIcon = when {
        partitura.tieneError -> Icons.Default.Error
        partitura.procesada -> Icons.Default.CheckCircle
        else -> Icons.Default.HourglassTop
    }

    val statusColor = when {
        partitura.tieneError -> MaterialTheme.colorScheme.error
        partitura.procesada -> Color(0xFF2E7D32)
        else -> Color(0xFFF9A825)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(ModoPartitura.LECTURA) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, contentDescription = null, tint = statusColor)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partitura.titulo ?: "-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Partitura subida el: "+FormatoFecha.formatearFecha(partitura.fechaSubida) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.reading_mode),
                    modifier = Modifier
                        .clickable { onOpen(ModoPartitura.LECTURA) }
                        .padding(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.recording_mode),
                    modifier = Modifier
                        .clickable { onOpen(ModoPartitura.GRABACION) }
                        .padding(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.analysis_mode),
                    modifier = Modifier
                        .clickable { onOpen(ModoPartitura.ANALISIS) }
                        .padding(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.practices),
                    modifier = Modifier
                        .clickable {
                            practicesViewModel?.selectSheetAndNavigate(partitura.idPartitura)
                        }
                        .padding(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.share),
                    modifier = Modifier
                        .clickable {
                            viewModelDetallePartitura.createShareLinkForSheetMusic(
                                sheetMusicId = partitura.idPartitura,
                                accessType = "PUBLIC"
                            ) { link ->
                                onShareToChat(link)
                                Toast.makeText(context, "Selecciona la clase donde compartirla", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
