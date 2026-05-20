package com.piania.app.ui.partituras

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.piania.app.ui.ads.AdManager
import com.piania.app.ui.base.BaseLocalizedActivity
import com.piania.app.ui.theme.PianIAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetallePartituraActivity : BaseLocalizedActivity() {

    private val viewModel: DetallePartituraViewModel by viewModels()
    private val practicesViewModel: PracticesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val idPartitura = intent.getLongExtra("ID_PARTITURA", -1)
        val modoString = intent.getStringExtra("MODO") ?: "LECTURA"
        val modoInicial = try {
            ModoPartitura.valueOf(modoString)
        } catch (e: Exception) {
            ModoPartitura.LECTURA
        }

        // Mostrar anuncio cada 2 aperturas antes de renderizar la pantalla
        AdManager.showInterstitialIfReady(this) {
            setContent {
                PianIAppTheme {
                    DetallePartituraScreen(
                        viewModel = viewModel,
                        idPartitura = idPartitura,
                        modoInicial = modoInicial,
                        onBack = { finish() },
                        practicesViewModel = practicesViewModel
                    )
                }
            }
        }
    }
}
