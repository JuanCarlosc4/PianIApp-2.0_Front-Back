package com.piania.app.ui.menu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piania.app.R
import com.piania.app.data.model.response.AnnouncementResponseDTO
import com.piania.app.ui.theme.PianIAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnnouncementsActivity : ComponentActivity() {

    private val viewModel: AnnouncementsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PianIAppTheme {
                AnnouncementsScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    viewModel: AnnouncementsViewModel
) {
    val isLoading by viewModel.isLoading.observeAsState(false)
    val items by viewModel.items.observeAsState(emptyList())
    val error by viewModel.error.observeAsState(null)
    val adminMsg by viewModel.adminActionMessage.observeAsState(null)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedAnnouncement by remember { mutableStateOf<AnnouncementResponseDTO?>(null) }

    // Al entrar a la pantalla, marcamos como vistos los anuncios
    LaunchedEffect(Unit) { viewModel.load(markAsSeen = true) }

    LaunchedEffect(adminMsg) {
        if (!adminMsg.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(adminMsg!!) }
            viewModel.clearAdminMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))

                val isAuthError = (error ?: "").contains("Sesión no válida", ignoreCase = true) ||
                        (error ?: "").contains("Inicia sesión", ignoreCase = true)

                if (isAuthError) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(
                                    ctx,
                                    com.piania.app.ui.auth.AuthActivity::class.java
                                ).addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.iniciar_sesion))
                    }
                } else {
                    Button(onClick = { viewModel.load() }) {
                        Text(stringResource(R.string.retry))
                    }
                }

                return@Column
            }

            if (items.isEmpty()) {
                Text(stringResource(R.string.no_announcements))
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.load() }) {
                    Text(stringResource(R.string.retry))
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items) { ann ->
                    AnnouncementCard(
                        ann = ann,
                        onClick = { selectedAnnouncement = ann }
                    )
                }
            }
        }
    }

    selectedAnnouncement?.let { announcement ->
        AnnouncementDetailDialog(
            ann = announcement,
            onDismiss = { selectedAnnouncement = null }
        )
    }
}

@Composable
private fun AnnouncementCard(
    ann: AnnouncementResponseDTO,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = ann.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
    }
}

@Composable
private fun AnnouncementDetailDialog(
    ann: AnnouncementResponseDTO,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = ann.title,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = ann.message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accept))
            }
        }
    )
}
