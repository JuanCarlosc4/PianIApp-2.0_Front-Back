package com.piania.app

import android.app.Application
import com.piania.app.util.WavBeepGeneratorInit
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PianiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializa el generador de WAV para el metrónomo (necesita un cacheDir estable)
        WavBeepGeneratorInit.init(cacheDir.absolutePath)
    }
}
