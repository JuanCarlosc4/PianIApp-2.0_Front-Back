package com.piania.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.piania.app.R
import com.piania.app.data.repository.ClassRepository

@Composable
fun ClassInviteAcceptScreen(
    token: String?,
    classRepository: ClassRepository,
    onAccepted: () -> Unit,
    onCancel: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val invalidLinkMsg = stringResource(R.string.error_could_not_process_file) // Reuse or add new

    // Si el token viene vacío, salimos con mensaje
    LaunchedEffect(token) {
        if (token.isNullOrBlank()) {
            message = context.getString(R.string.error_could_not_process_file)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Invitación a clase",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "¿Quieres unirte a esta clase?",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = message.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (token.isNullOrBlank() || isLoading) return@Button
                isLoading = true
                message = null
            },
            enabled = !isLoading && !token.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp))
            } else {
                Text(stringResource(R.string.accept_invitation))
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.cancel))
        }

        // Ejecutar llamada cuando pasamos a loading por pulsar aceptar
        LaunchedEffect(isLoading) {
            if (!isLoading) return@LaunchedEffect
            if (token.isNullOrBlank()) {
                isLoading = false
                return@LaunchedEffect
            }

            val result = classRepository.acceptClassInvitation(token)

            result.onSuccess {
                isLoading = false
                onAccepted()
            }.onFailure { e ->
                isLoading = false
                val msg = e.message.orEmpty()

                // Heurística simple: si se quedó sin token (401/403) forzamos login
                if (msg.contains("401") || msg.contains("403") || msg.contains("Unauthorized", ignoreCase = true)) {
                    onRequireLogin()
                } else {
                    message = if (msg.isNotBlank()) msg else context.getString(R.string.load_error)
                }
            }
        }
    }
}
