package com.piania.app.ui.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    private var interstitialAd: InterstitialAd? = null

    private const val PREFS_NAME = "ad_prefs"
    private const val KEY_OPEN_COUNT = "score_open_count"

    /**
     * Cargar anuncio interstitial en memoria.
     */
    fun loadInterstitial(context: Context) {

        val adRequest = AdRequest.Builder().build()

        // ID DE PRUEBA DE GOOGLE (cambiar en producción)
        InterstitialAd.load(
            context,
            "ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Mostrar anuncio cada 2 aperturas de partitura.
     */
    fun showInterstitialIfReady(
        activity: Activity,
        onAdClosedOrNotShown: () -> Unit
    ) {

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentCount = prefs.getInt(KEY_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_OPEN_COUNT, currentCount).apply()

        val shouldShow = (currentCount % 2 == 0)

        if (shouldShow && interstitialAd != null) {

            interstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {

                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitial(activity)
                        onAdClosedOrNotShown()
                    }

                    override fun onAdFailedToShowFullScreenContent(
                        adError: com.google.android.gms.ads.AdError
                    ) {
                        onAdClosedOrNotShown()
                    }
                }

            interstitialAd?.show(activity)

        } else {
            onAdClosedOrNotShown()
        }
    }
}
