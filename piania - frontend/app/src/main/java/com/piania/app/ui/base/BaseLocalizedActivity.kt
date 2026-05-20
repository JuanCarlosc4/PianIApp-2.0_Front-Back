package com.piania.app.ui.base

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import java.util.Locale

/**
 * Fallback robusto para per-app language.
 *
 * En algunos dispositivos AppCompatDelegate.setApplicationLocales() no aplica el locale
 * correctamente (getApplicationLocales() puede devolver vacío).
 *
 * Aquí forzamos el locale leyendo la preferencia local "forced_language" (p.ej. "es" o "en"),
 * de modo que Resources/Compose resuelvan strings desde values-xx en TODAS las Activities.
 */
abstract class BaseLocalizedActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = try {
            val prefs = newBase.getSharedPreferences("piania_local_prefs", Context.MODE_PRIVATE)
            prefs.getString("forced_language", null)
        } catch (_: Throwable) {
            null
        }

        if (lang.isNullOrBlank()) {
            super.attachBaseContext(newBase)
            return
        }

        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }
}
