package com.piania.app.ui.partituras

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.piania.app.ui.base.BaseLocalizedActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piania.app.R
import com.piania.app.ui.theme.PianIAppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

private const val SHEET_UPLOAD_CHANNEL_ID = "sheet_uploads"
private const val SHEET_UPLOAD_NOTIFICATION_ID = 2001

@AndroidEntryPoint
class RegistroPartituraActivity : BaseLocalizedActivity() {
    private val viewModel: RegistroPartituraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PianIAppTheme {
                RegistroPartituraScreen(
                    viewModel = viewModel,
                    onUploadSuccess = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroPartituraScreen(
    viewModel: RegistroPartituraViewModel,
    onUploadSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val archivoNombre by viewModel.archivoNombre.collectAsStateWithLifecycle()

    var titulo by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var uploadSuccessHandled by remember { mutableStateOf(false) }
    var pendingNotificationTitle by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showSheetUploadedNotification(context, pendingNotificationTitle)
        }
        onUploadSuccess()
    }

    fun notifyUploadAndReturnToList(title: String) {
        pendingNotificationTitle = title
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            showSheetUploadedNotification(context, title)
            onUploadSuccess()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(safeUri) ?: ""

            val fileToUpload: File? = if (mimeType.startsWith("image")) {
                ImageUtils.prepararImagenParaSubida(context, safeUri)
            } else {
                try {
                    val inputStream = contentResolver.openInputStream(safeUri)

                    val displayName: String? = runCatching {
                        val cursor = contentResolver.query(safeUri, null, null, null, null)
                        cursor?.use {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0 && it.moveToFirst()) it.getString(nameIndex) else null
                        }
                    }.getOrNull()

                    val extensionFromMime = when (mimeType.lowercase()) {
                        "application/pdf" -> ".pdf"
                        "application/vnd.recordare.musicxml+xml" -> ".mxl"
                        "text/xml", "application/xml" -> ".musicxml"
                        else -> ".bin"
                    }

                    val baseName = (displayName?.substringBeforeLast('.') ?: "upload_doc")
                        .takeIf { it.isNotBlank() }
                        ?: "upload_doc"

                    val extension = displayName?.substringAfterLast('.', missingDelimiterValue = "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { ".$it" }
                        ?: extensionFromMime

                    val tempFile = File.createTempFile(baseName + "_", extension, context.cacheDir)

                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (fileToUpload != null) {
                viewModel.setArchivoSeleccionado(fileToUpload)
            } else {
                Toast.makeText(context, context.getString(R.string.error_could_not_process_file), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            val file = ImageUtils.prepararImagenParaSubida(context, photoUri!!)

            if (file != null && file.length() > 0) {
                viewModel.setArchivoSeleccionado(file)
                Toast.makeText(context, context.getString(R.string.photo_processed_ok), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.photo_processed_error), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.photo_not_taken), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is RegistroPartituraViewModel.UploadState.Processing -> {
                Toast.makeText(context, context.getString(R.string.processing_sheet_music), Toast.LENGTH_SHORT).show()
            }
            is RegistroPartituraViewModel.UploadState.Success -> {
                if (!uploadSuccessHandled) {
                    uploadSuccessHandled = true
                    val data = (uploadState as RegistroPartituraViewModel.UploadState.Success).data
                    Toast.makeText(context, context.getString(R.string.sheet_music_processed), Toast.LENGTH_LONG).show()
                    notifyUploadAndReturnToList(data.titulo.ifBlank { titulo })
                }
            }
            is RegistroPartituraViewModel.UploadState.Error -> {
                val msg = (uploadState as RegistroPartituraViewModel.UploadState.Error).message
                Toast.makeText(context, context.getString(R.string.error_prefix, msg), Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(stringResource(R.string.register_sheet_music_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text(stringResource(R.string.t_tulo_de_la_obra)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uploadState !is RegistroPartituraViewModel.UploadState.Loading,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "text/xml",
                            "application/xml",
                            "application/vnd.recordare.musicxml+xml",
                            "image/*"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uploadState !is RegistroPartituraViewModel.UploadState.Loading
            ) {
                Text(stringResource(R.string.upload_file))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "partitura_${System.currentTimeMillis()}.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PianIA")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    photoUri = uri
                    if (uri != null) takePhotoLauncher.launch(uri)
                    else Toast.makeText(context, context.getString(R.string.error_create_image_file), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uploadState !is RegistroPartituraViewModel.UploadState.Loading
            ) { Text(stringResource(R.string.take_photo)) }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (archivoNombre.isEmpty()) stringResource(R.string.ning_n_archivo_seleccionado) else stringResource(R.string.ready_to_upload_format, archivoNombre),
                style = MaterialTheme.typography.bodyMedium,
                color = if (archivoNombre.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.subirPartituraDesdeCompose(titulo) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = uploadState !is RegistroPartituraViewModel.UploadState.Loading &&
                    uploadState !is RegistroPartituraViewModel.UploadState.Processing &&
                    archivoNombre.isNotBlank() &&
                    titulo.isNotBlank()
            ) {
                if (uploadState is RegistroPartituraViewModel.UploadState.Loading ||
                    uploadState is RegistroPartituraViewModel.UploadState.Processing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (uploadState is RegistroPartituraViewModel.UploadState.Processing)
                                stringResource(R.string.processing)
                            else
                                stringResource(R.string.uploading),
                            fontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                    }
                } else {
                    Text(stringResource(R.string.subir_partitura))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateBack, enabled = uploadState !is RegistroPartituraViewModel.UploadState.Loading) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

private fun showSheetUploadedNotification(context: Context, title: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            SHEET_UPLOAD_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val openAppIntent = Intent(context, com.piania.app.ui.main.MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, SHEET_UPLOAD_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_sheet_music)
        .setContentTitle(context.getString(R.string.notification_title_uploaded))
        .setContentText(if (title.isBlank()) context.getString(R.string.notification_text_uploaded_generic) else context.getString(R.string.notification_text_uploaded_format, title))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) {
        NotificationManagerCompat.from(context).notify(SHEET_UPLOAD_NOTIFICATION_ID, notification)
    }
}
