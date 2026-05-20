package com.piania.app.ui.splash

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.piania.app.ui.base.BaseLocalizedActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.piania.app.R
import com.piania.app.data.SessionManager
import com.piania.app.ui.auth.AuthActivity
import com.piania.app.ui.main.MainActivity
import com.piania.app.ui.theme.PianIAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseLocalizedActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    // Deep link: piania://class-invite/{token}
    private var classInviteTokenFromDeepLink: String? = null

    // Variable para evitar que se navegue dos veces por error
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var startTime: Long = 0
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Independientemente de si fue aceptado o no, continuar con la app
            initializeApp()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Guardamos la hora de inicio para calcular el delay estético después
        startTime = System.currentTimeMillis()

        // Deep link: piania://class-invite/{token}
        classInviteTokenFromDeepLink = intent?.data?.let { uri ->
            if (uri.scheme == "piania" && uri.host == "class-invite") {
                uri.lastPathSegment
            } else {
                null
            }
        }

        setContent {
            PianIAppTheme {
                SplashScreenContent()
            }
        }

        // Verificar si necesitamos pedir permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Continuar con la lógica normal
        initializeApp()
    }

    private fun initializeApp() {
        // --- INICIO DE LÓGICA UMP ---
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w("Splash", "Error formulario: ${loadAndShowError.message}")
                    }

                    // Inicializar anuncios si tenemos permiso
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }

                    // SEA COMO SEA (Aceptó, rechazó o error), ahora navegamos
                    navegarALaSiguientePantalla()
                }
            },
            { requestConsentError ->
                Log.w("Splash", "Error pidiendo consentimiento: ${requestConsentError.message}")
                // Si falla el consentimiento, navegamos igual
                navegarALaSiguientePantalla()
            }
        )
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        MobileAds.initialize(this) {}
    }

    // Esta es la ÚNICA función que decide adónde ir
    private fun navegarALaSiguientePantalla() {
        lifecycleScope.launch {
            // 1. CÁLCULO INTELIGENTE DEL TIEMPO
            val tiempoPasado = System.currentTimeMillis() - startTime
            val tiempoRestante = 1500 - tiempoPasado

            if (tiempoRestante > 0) {
                delay(tiempoRestante)
            }

            // 2. CHECK DE SESIÓN
            val token = sessionManager.fetchAuthToken()

            if (!token.isNullOrEmpty()) {
                // -> USUARIO LOGUEADO
                startActivity(
                    Intent(this@SplashActivity, MainActivity::class.java).apply {
                        if (!classInviteTokenFromDeepLink.isNullOrBlank()) {
                            putExtra("classInviteToken", classInviteTokenFromDeepLink)
                        }
                    }
                )
            } else {
                // -> USUARIO NUEVO
                startActivity(
                    Intent(this@SplashActivity, AuthActivity::class.java).apply {
                        if (!classInviteTokenFromDeepLink.isNullOrBlank()) {
                            putExtra("classInviteToken", classInviteTokenFromDeepLink)
                        }
                    }
                )
            }

            // 3. CERRAR
            finish()
        }
    }

}

@Composable
fun SplashScreenContent() {
    val isDarkTheme = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_playstore), // Asegúrate de tener esta imagen
            contentDescription = stringResource(R.string.logo_piania),
            modifier = Modifier.size(150.dp),
            colorFilter = if (isDarkTheme) {
                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )))
            } else {
                null
            }
        )
    }
}
