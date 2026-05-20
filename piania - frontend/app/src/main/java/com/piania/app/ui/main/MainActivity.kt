package com.piania.app.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.piania.app.ui.base.BaseLocalizedActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.ump.UserMessagingPlatform
import com.piania.app.R
import com.piania.app.data.JwtUtils
import com.piania.app.data.SessionManager
import com.piania.app.data.model.request.UserSettingsRequestDTO
import com.piania.app.data.model.response.ShareLinkResponseDTO
import com.piania.app.data.repository.ClassRepository
import com.piania.app.ui.ads.AdManager
import com.piania.app.ui.auth.AuthActivity
import com.piania.app.ui.menu.UserSettingsViewModel
import com.piania.app.ui.auth.UserProfileViewModel
import com.piania.app.ui.chat.ChatViewModel
import com.piania.app.ui.classes.ClassesViewModel
import com.piania.app.ui.menu.AnnouncementsViewModel
import com.piania.app.ui.partituras.DetallePartituraScreen
import com.piania.app.ui.partituras.DetallePartituraViewModel
import com.piania.app.ui.partituras.ModoPartitura
import com.piania.app.ui.partituras.PartiturasScreen
import com.piania.app.ui.partituras.PartiturasViewModel
import com.piania.app.ui.partituras.PracticesViewModel
import com.piania.app.ui.partituras.PracticesPanel
import com.piania.app.ui.partituras.RegistroPartituraScreen
import com.piania.app.ui.partituras.RegistroPartituraViewModel
import com.piania.app.ui.student.LinkTeacherViewModel
import com.piania.app.ui.theme.PianIAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class MainDestination {
    MENU,
    PARTITURAS,
    PARTITURA_REGISTRO,
    PARTITURA_DETALLE,
    PRACTICES_HISTORY,
    ANNOUNCEMENTS,
    LINK_TEACHER,
    CLASSES,
    CHAT,
    GROUP_INFO,
    CLASS_INVITE_ACCEPT,
}

@AndroidEntryPoint
class MainActivity : BaseLocalizedActivity() {

    fun restartMainActivityClean() {
        // Evita Activity.recreate() (crashea en algunos Android 14/15 con ActivityImpl ClassCastException)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
        // Pequeño delay para asegurar que el finish se procese antes de terminar el task (más robusto en algunos OEMs)
        Handler(Looper.getMainLooper()).post { }
    }

    private val settingsViewModel: UserSettingsViewModel by viewModels()
    private val partiturasViewModel: PartiturasViewModel by viewModels()
    private val registroPartituraViewModel: RegistroPartituraViewModel by viewModels()
    private val detallePartituraViewModel: DetallePartituraViewModel by viewModels()
    private val practicesViewModel: PracticesViewModel by viewModels()
    private val announcementsViewModel: com.piania.app.ui.menu.AnnouncementsViewModel by viewModels()
    private val linkTeacherViewModel: LinkTeacherViewModel by viewModels()
    private val classesViewModel: com.piania.app.ui.classes.ClassesViewModel by viewModels()
    private val chatViewModel: com.piania.app.ui.chat.ChatViewModel by viewModels()
    private val userProfileViewModel: UserProfileViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var teacherRepository: com.piania.app.data.repository.TeacherRepository

    @Inject
    lateinit var classRepository: ClassRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AdManager.loadInterstitial(this)

        val classInviteTokenFromSplash: String? = intent?.getStringExtra("classInviteToken")

        setContent {
            // Cargar ajustes al entrar para aplicar tema/idioma inmediatamente
            LaunchedEffect(Unit) { settingsViewModel.load() }

            val settings by settingsViewModel.settings.observeAsState()

            PianIAppTheme(darkTheme = settings?.darkMode ?: androidx.compose.foundation.isSystemInDarkTheme()) {
                MainHost(
                    settingsViewModel = settingsViewModel,
                    partiturasViewModel = partiturasViewModel,
                    linkTeacherViewModel = linkTeacherViewModel,
                    studentClassesViewModel = classesViewModel,
                    chatViewModel = chatViewModel,
                    userProfileViewModel = userProfileViewModel,
                    sessionManager = sessionManager,
                    onLogout = { cerrarSesion() },
                    onOpenTeacherArea = {
                        startActivity(Intent(this, com.piania.app.ui.teacher.TeacherAreaActivity::class.java))
                    },
                    announcementsViewModel = announcementsViewModel,
                    registroPartituraViewModel = registroPartituraViewModel,
                    detallePartituraViewModel = detallePartituraViewModel,
                    practicesViewModel = practicesViewModel,
                    classInviteToken = classInviteTokenFromSplash
                )
            }
        }
    }

    private fun cerrarSesion() {
        lifecycleScope.launch {
            sessionManager.clearSession()
            val intent = Intent(this@MainActivity, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainHost(
    settingsViewModel: UserSettingsViewModel,
    partiturasViewModel: PartiturasViewModel,
    linkTeacherViewModel: LinkTeacherViewModel,
    studentClassesViewModel: ClassesViewModel,
    chatViewModel: ChatViewModel,
    userProfileViewModel: UserProfileViewModel,
    sessionManager: SessionManager,
    onLogout: () -> Unit,
    onOpenTeacherArea: () -> Unit,
    announcementsViewModel: AnnouncementsViewModel,
    registroPartituraViewModel: RegistroPartituraViewModel,
    detallePartituraViewModel: DetallePartituraViewModel,
    practicesViewModel: PracticesViewModel,
    classInviteToken: String?
) {
    var destination by rememberSaveable { mutableStateOf(MainDestination.MENU) }
    var selectedClassId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Deep link flow: invitación a clase
    var pendingClassInviteToken by rememberSaveable { mutableStateOf(classInviteToken) }
    LaunchedEffect(pendingClassInviteToken) {
        if (!pendingClassInviteToken.isNullOrBlank()) {
            destination = MainDestination.CLASS_INVITE_ACCEPT
        }
    }
    var selectedClassName by rememberSaveable { mutableStateOf("") }

    // Partituras flow (sin Activities dedicadas)
    var selectedSheetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedSheetMode by rememberSaveable { mutableStateOf(ModoPartitura.LECTURA) }
    var selectedPracticeSheetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedClassAvatarIndex by rememberSaveable { mutableIntStateOf(1) }
    var selectedClassCreatedAt by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingScrollToMessageId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Share-to-chat: enlace generado desde DetallePartitura -> Chat
    var pendingShareLink by rememberSaveable { mutableStateOf<ShareLinkResponseDTO?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity

    val isTeacher = remember { JwtUtils.isTeacher(sessionManager.fetchAuthToken()) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Anuncios sin leer: lo refrescamos al volver al menú, para que el punto rojo se actualice
    val hasUnreadAnnouncements by announcementsViewModel.hasUnread.observeAsState(false)
    LaunchedEffect(destination) {
        if (destination == MainDestination.MENU) {
            announcementsViewModel.load(markAsSeen = false)
        }
    }

    // Solo se usa para selector de avatar, etc.
    val myProfile by userProfileViewModel.profile.observeAsState()
    LaunchedEffect(Unit) { userProfileViewModel.loadMe() }

    // Reacción a token caducado desde PartiturasViewModel
    val sessionExpired by partiturasViewModel.sessionExpired.observeAsState(false)
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // En Chat/Info usamos cabecera tipo WhatsApp (avatar+nombre) integrada en TopAppBar
                    if (destination == MainDestination.CHAT || destination == MainDestination.GROUP_INFO) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = destination == MainDestination.CHAT) {
                                    destination = MainDestination.GROUP_INFO
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val avatarRes = classGroupAvatarRes(selectedClassAvatarIndex)
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = avatarRes),
                                contentDescription = stringResource(R.string.group_avatar_description),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedClassName,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (destination == MainDestination.CHAT) stringResource(R.string.tap_to_see_info) else stringResource(R.string.group_info),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            when (destination) {
                                MainDestination.MENU -> stringResource(R.string.piania_title)
                                MainDestination.PARTITURAS -> stringResource(R.string.my_sheet_music)
                                MainDestination.PARTITURA_REGISTRO -> stringResource(R.string.register_sheet_music_title)
                                MainDestination.PARTITURA_DETALLE -> stringResource(R.string.sheet_music)
                                MainDestination.PRACTICES_HISTORY -> stringResource(R.string.practices)
                                MainDestination.ANNOUNCEMENTS -> stringResource(R.string.announcements_title)
                                MainDestination.LINK_TEACHER -> stringResource(R.string.link_with_teacher)
                                MainDestination.CLASSES -> stringResource(R.string.classes)
                                MainDestination.CHAT -> ""
                                MainDestination.GROUP_INFO -> ""
                                MainDestination.CLASS_INVITE_ACCEPT -> stringResource(R.string.accept_invitation)
                            }
                        )
                    }
                },
                navigationIcon = {
                    if (destination != MainDestination.MENU) {
                        IconButton(
                            onClick = {
                                destination =
                                    when (destination) {
                                        MainDestination.CHAT -> MainDestination.CLASSES
                                        MainDestination.GROUP_INFO -> MainDestination.CHAT
                                        MainDestination.PARTITURA_REGISTRO -> MainDestination.PARTITURAS
                                        MainDestination.PARTITURA_DETALLE -> MainDestination.PARTITURAS
                                        else -> MainDestination.MENU
                                    }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    // En pantalla de chat/info ocultamos el icono de ajustes (estilo WhatsApp)
                    if (destination != MainDestination.CHAT && destination != MainDestination.GROUP_INFO) {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (destination) {
                MainDestination.MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.welcome_pianist), style = MaterialTheme.typography.headlineSmall)

                        MenuCard(
                            titulo = stringResource(R.string.my_sheet_music),
                            descripcion = stringResource(R.string.view_and_play_songs),
                            icono = Icons.Default.LibraryMusic,
                            onClick = { destination = MainDestination.PARTITURAS }
                        )

                        Box {
                            MenuCard(
                                titulo = stringResource(R.string.announcements_title),
                                descripcion = stringResource(R.string.announcements_desc),
                                icono = Icons.Default.Settings,
                                onClick = { destination = MainDestination.ANNOUNCEMENTS }
                            )

                            if (hasUnreadAnnouncements) {
                                val announcementsDesc = stringResource(R.string.new_announcements_desc)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                        .size(12.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .semantics { contentDescription = announcementsDesc },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                }
                            }
                        }

                        MenuCard(
                            titulo = stringResource(R.string.classes),
                            descripcion = if (isTeacher) {
                                stringResource(R.string.teacher_classes_desc)
                            } else {
                                stringResource(R.string.student_classes_desc)
                            },
                            icono = Icons.Default.Settings,
                            onClick = { destination = MainDestination.CLASSES }
                        )

                        if (!isTeacher) {
                            MenuCard(
                                titulo = stringResource(R.string.link_with_teacher),
                                descripcion = stringResource(R.string.link_with_teacher_desc),
                                icono = Icons.Default.Settings,
                                onClick = { destination = MainDestination.LINK_TEACHER }
                            )
                        }

                        if (isTeacher) {
                            MenuCard(
                                titulo = stringResource(R.string.teacher_area_title),
                                descripcion = stringResource(R.string.teacher_area_desc),
                                icono = Icons.Default.Settings,
                                onClick = onOpenTeacherArea
                            )
                        }
                    }
                }

                MainDestination.PARTITURAS -> {

                    val navigateSheetId by practicesViewModel.navigateToHistory.collectAsState()

                    LaunchedEffect(navigateSheetId) {
                        navigateSheetId?.let { sheetId ->
                            selectedPracticeSheetId = sheetId
                            practicesViewModel.loadBySheetMusic(sheetId)
                            practicesViewModel.consumeNavigation()
                            destination = MainDestination.PRACTICES_HISTORY
                        }
                    }

                    PartiturasScreen(
                        viewModelDetallePartitura = detallePartituraViewModel,
                        viewModel = partiturasViewModel,
                        practicesViewModel = practicesViewModel,
                        onNavigateToAdd = { destination = MainDestination.PARTITURA_REGISTRO },
                        onOpenSheet = { sheetId, mode ->
                            selectedSheetId = sheetId
                            selectedSheetMode = mode
                            destination = MainDestination.PARTITURA_DETALLE
                        },
                        onShareToChat = { shareLink ->
                            pendingShareLink = shareLink
                            destination = MainDestination.CLASSES
                        }
                    )
                }

                MainDestination.PARTITURA_REGISTRO -> {
                    RegistroPartituraScreen(
                        viewModel = registroPartituraViewModel,
                        onUploadSuccess = {
                            partiturasViewModel.loadPartituras()
                            destination = MainDestination.PARTITURAS
                        },
                        onNavigateBack = { destination = MainDestination.PARTITURAS }
                    )
                }

                MainDestination.PARTITURA_DETALLE -> {
                    DetallePartituraScreen(
                        viewModel = detallePartituraViewModel,
                        idPartitura = selectedSheetId ?: -1L,
                        modoInicial = selectedSheetMode,
                        onBack = { destination = MainDestination.PARTITURAS },
                        onShareToChat = { shareLink ->
                            pendingShareLink = shareLink
                            destination = MainDestination.CLASSES
                        }
                    )
                }

                MainDestination.PRACTICES_HISTORY -> {
                    val state by practicesViewModel.state.collectAsState()
                    val detailState by practicesViewModel.detailState.collectAsState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        when (state) {
                            is PracticesViewModel.UiState.Loading -> {
                                CircularProgressIndicator()
                            }
                            is PracticesViewModel.UiState.Error -> {
                                Text(
                                    (state as PracticesViewModel.UiState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            is PracticesViewModel.UiState.Loaded -> {
                                val items = (state as PracticesViewModel.UiState.Loaded).items
                                PracticesPanel(
                                    items = items,
                                    detailState = detailState,
                                    onOpenDetail = { practicesViewModel.openPracticeDetail(it) },
                                    onSaveNotes = { id, student, teacher ->
                                        practicesViewModel.saveNotes(id, student, teacher)
                                    },
                                    onCloseDetail = { practicesViewModel.closePracticeDetail() }
                                )
                            }
                            else -> {}
                        }
                    }
                }

                MainDestination.ANNOUNCEMENTS -> {
                    com.piania.app.ui.menu.AnnouncementsScreen(viewModel = announcementsViewModel)
                }

                MainDestination.LINK_TEACHER -> {
                    LinkTeacherScreen(
                        viewModel = linkTeacherViewModel,
                        onBack = { destination = MainDestination.MENU }
                    )
                }

                MainDestination.CLASSES -> {
                    StudentClassesScreen(
                        viewModel = studentClassesViewModel,
                        isTeacher = isTeacher,
                        onBack = { destination = MainDestination.MENU },
                        onOpenChat = { classId ->
                            selectedClassId = classId
                            // Recuperamos info de la clase para la cabecera (nombre + avatar)
                            studentClassesViewModel.classes.value?.firstOrNull { it.id == classId }?.let { c ->
                                selectedClassName = c.name
                                selectedClassAvatarIndex = c.groupAvatarIndex()
                                selectedClassCreatedAt = c.createdAt
                            }
                            // Limpiar scroll pendiente: al entrar al chat siempre haremos scroll al "primero sin leer"
                            // (si no hay, al final). Evita colisiones con salto desde pinned.
                            pendingScrollToMessageId = null
                            chatViewModel.loadMessages(classId)
                            destination = MainDestination.CHAT
                        }
                    )
                }

                MainDestination.CHAT -> {
                    ChatScreen(
                        viewModel = chatViewModel,
                        sessionManager = sessionManager,
                        className = selectedClassName,
                        classAvatarIndex = selectedClassAvatarIndex,
                        classCreatedAt = selectedClassCreatedAt,
                        scrollToMessageId = pendingScrollToMessageId,
                        onConsumedScrollToMessage = { pendingScrollToMessageId = null },
                        onBack = { destination = MainDestination.CLASSES },
                        onOpenGroupInfo = { destination = MainDestination.GROUP_INFO },
                        pendingShareLink = pendingShareLink,
                        onConsumedShareLink = { pendingShareLink = null }
                    )
                }

                MainDestination.GROUP_INFO -> {
                    GroupInfoScreen(
                        classId = selectedClassId,
                        className = selectedClassName,
                        classAvatarIndex = selectedClassAvatarIndex,
                        isTeacher = isTeacher,
                        chatViewModel = chatViewModel,
                        // Repo vía Hilt (lo inyectamos en MainActivity y lo pasamos a Compose)
                        teacherRepository = (context as MainActivity).teacherRepository,
                        classRepository = (context as MainActivity).classRepository,
                        classesViewModel = studentClassesViewModel,
                        onJumpToMessage = { messageId ->
                            // Volvemos al chat y pedimos saltar al mensaje fijado
                            pendingScrollToMessageId = messageId
                            destination = MainDestination.CHAT
                        }
                    )
                }

                MainDestination.CLASS_INVITE_ACCEPT -> {
                    ClassInviteAcceptScreen(
                        token = pendingClassInviteToken,
                        classRepository = (context as MainActivity).classRepository,
                        onAccepted = {
                            pendingClassInviteToken = null
                            // refrescar listado y volver a Clases
                            studentClassesViewModel.loadClasses(isTeacher = isTeacher)
                            destination = MainDestination.CLASSES
                        },
                        onCancel = {
                            pendingClassInviteToken = null
                            destination = MainDestination.MENU
                        },
                        onRequireLogin = {
                            // Si algo va mal por sesión caducada, forzamos logout (irá a AuthActivity)
                            onLogout()
                        }
                    )
                }
            }
        }

        if (showSettingsDialog) {
            SettingsDialogCompat(
                settingsViewModel = settingsViewModel,
                userProfileViewModel = userProfileViewModel,
                currentAvatar = myProfile?.avatar,
                myProfile = myProfile,
                onDismiss = { showSettingsDialog = false },
                onLogout = {
                    showSettingsDialog = false
                    onLogout()
                },
                onNavigateHome = { destination = MainDestination.MENU },
                activity = activity
            )
        }
    }
}

@Composable
private fun SettingsDialogCompat(
    settingsViewModel: UserSettingsViewModel,
    userProfileViewModel: UserProfileViewModel,
    currentAvatar: String?,
    myProfile: com.piania.app.data.model.response.UserProfileResponseDTO?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
    activity: Activity?
) {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // La lógica ya se maneja en el onChange del Switch
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val settings by settingsViewModel.settings.observeAsState()
    val isLoading by settingsViewModel.isLoading.observeAsState(false)


    var language by remember(settings) { mutableStateOf(settings?.language ?: "es") }
    var darkMode by remember(settings) { mutableStateOf(settings?.darkMode ?: false) }
    var notifications by remember(settings) { mutableStateOf(settings?.notificationsEnabled ?: true) }
    var defaultTempo by remember(settings) { mutableStateOf((settings?.defaultTempo ?: 120).toString()) }
    var metronomeEnabled by remember(settings) { mutableStateOf(settings?.metronomeEnabled ?: true) }
    var volumen by remember { mutableStateOf(0.5f) }

    // Avatar: backend espera "AVATAR_1"..
    var selectedAvatar by remember(currentAvatar) { mutableStateOf(currentAvatar ?: "AVATAR_1") }
    val isAvatarUpdating by userProfileViewModel.isUpdatingAvatar.observeAsState(false)

    val consentInformation = UserMessagingPlatform.getConsentInformation(LocalContext.current)
    val showPrivacyOptions =
        consentInformation.privacyOptionsRequirementStatus ==
            com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Perfil (mostrar nombre, no email)
                    if (myProfile != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = myProfile?.fullName ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(stringResource(R.string.app_volume), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = volumen, onValueChange = { volumen = it })

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.avatar), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedAvatar == "AVATAR_1",
                            onClick = { selectedAvatar = "AVATAR_1" },
                            label = { Text("1") },
                            enabled = !isAvatarUpdating
                        )
                        FilterChip(
                            selected = selectedAvatar == "AVATAR_2",
                            onClick = { selectedAvatar = "AVATAR_2" },
                            label = { Text("2") },
                            enabled = !isAvatarUpdating
                        )
                        FilterChip(
                            selected = selectedAvatar == "AVATAR_3",
                            onClick = { selectedAvatar = "AVATAR_3" },
                            label = { Text("3") },
                            enabled = !isAvatarUpdating
                        )
                        FilterChip(
                            selected = selectedAvatar == "AVATAR_4",
                            onClick = { selectedAvatar = "AVATAR_4" },
                            label = { Text("4") },
                            enabled = !isAvatarUpdating
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { userProfileViewModel.updateAvatar(selectedAvatar) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAvatarUpdating
                    ) {
                        if (isAvatarUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text(stringResource(R.string.save_avatar))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == "es",
                            onClick = { language = "es" },
                            label = { Text(stringResource(R.string.spanish)) }
                        )
                        FilterChip(
                            selected = language == "en",
                            onClick = { language = "en" },
                            label = { Text(stringResource(R.string.english)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.dark_mode))
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.notifications))
                        Switch(
                            checked = notifications,
                            onCheckedChange = { newValue ->
                                if (newValue && !hasNotificationPermission()) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    notifications = hasNotificationPermission()
                                } else {
                                    notifications = newValue
                                }
                            }
                        )
                    }

                    OutlinedTextField(
                        value = defaultTempo,
                        onValueChange = { defaultTempo = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text(stringResource(R.string.default_tempo)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.metronome_enabled))
                        Switch(checked = metronomeEnabled, onCheckedChange = { metronomeEnabled = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val tempoInt = defaultTempo.toIntOrNull() ?: 120

                            // Aplicar idioma inmediatamente para que la UI cambie al instante
                            val tags = language.trim().ifEmpty { "es" }
                            val appLocales = androidx.core.os.LocaleListCompat.forLanguageTags(tags)
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocales)

                            // Persistencia local: fallback para cuando AppCompat no aplica locales en runtime
                            // (en algunos dispositivos devuelve vacío y no cambia Resources).
                            context.getSharedPreferences("piania_local_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("forced_language", tags)
                                .apply()

                            settingsViewModel.save(
                                UserSettingsRequestDTO(
                                    language = language,
                                    notificationsEnabled = notifications,
                                    metronomeEnabled = metronomeEnabled,
                                    defaultTempo = tempoInt,
                                    darkMode = darkMode,
                                    cookiesAccepted = settings?.cookiesAccepted ?: false,
                                    privacyPolicyAccepted = settings?.privacyPolicyAccepted ?: false,
                                    adsEnabled = settings?.adsEnabled ?: true
                                )
                            )

                            // Forzar refresh de recursos/Compose tras cambiar locales
                            // Importante: NO llamamos a recreate(); en algunos dispositivos/ROMs está provocando
                            // relaunch raro (ver logcat ActivityImpl ClassCastException) y puede impedir que
                            // el cambio de idioma se note.
                            onDismiss()
                            onNavigateHome()

                            // Diagnóstico en logcat: locales que cree AppCompat que están activos
                            Log.d("LOCALE_DEBUG", "AppCompat locales after save=" + androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags())
                            Log.d("LOCALE_DEBUG", "selectedLanguage(tags)=" + tags)

                            // Aplicar cambios de idioma sin recreate()
                            (activity as? MainActivity)?.restartMainActivityClean()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp))
                        } else {
                            Text(stringResource(R.string.save_settings))
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (showPrivacyOptions) {
                        OutlinedButton(
                            onClick = {
                                activity?.let { act ->
                                    UserMessagingPlatform.showPrivacyOptionsForm(act) { error ->
                                        if (error != null) Log.e("UMP", "Error: ${error.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.manage_cookies))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.logout), color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close_menu))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    titulo: String,
    descripcion: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = descripcion, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LinkTeacherScreen(
    viewModel: LinkTeacherViewModel,
    onBack: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    val isLoading by viewModel.isLoading.observeAsState(false)
    val message by viewModel.message.observeAsState()

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            // Se muestra inline, pero limpiamos para no repetir en recomposiciones
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.link_with_teacher_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = stringResource(R.string.link_with_teacher_instructions),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.teacher_email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.linkToTeacher(email) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text(stringResource(R.string.link_with_teacher_action))
            }
        }

        if (!message.isNullOrBlank()) {
            Text(
                text = message!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.back))
        }
    }
}
