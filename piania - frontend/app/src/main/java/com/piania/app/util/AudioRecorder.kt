package com.piania.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startRecording(fileName: String) {
        Log.d("AudioRecorder", "--- INTENTANDO INICIAR GRABACIÓN ---")

        // 1. Crear archivo
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        audioFile = File(cacheDir, fileName)

        Log.d("AudioRecorder", "Ruta del archivo: ${audioFile?.absolutePath}")

        // 2. Configurar Recorder
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                setAudioEncodingBitRate(128000) // Calidad decente
                setAudioSamplingRate(44100)     // Estándar

                prepare()
                start()

                Log.d("AudioRecorder", "✅ GRABACIÓN INICIADA CORRECTAMENTE")
            } catch (e: IOException) {
                Log.e("AudioRecorder", "❌ ERROR en prepare() o start(): ${e.message}")
                e.printStackTrace()
                audioFile = null // Invalidamos el archivo si falló
            } catch (e: Exception) {
                Log.e("AudioRecorder", "❌ ERROR GENERAL al iniciar: ${e.message}")
                e.printStackTrace()
                audioFile = null
            }
        }
    }

    fun stopRecording(): File? {
        Log.d("AudioRecorder", "--- INTENTANDO PARAR GRABACIÓN ---")
        return try {
            if (recorder != null) {
                recorder?.stop()
                recorder?.release()
                recorder = null
                Log.d("AudioRecorder", "✅ RECORDER PARADO. Archivo guardado.")

                // Verificamos si el archivo existe y tiene tamaño
                if (audioFile != null && audioFile!!.exists() && audioFile!!.length() > 0) {
                    Log.d("AudioRecorder", "Archivo final: ${audioFile?.absolutePath} (Tamaño: ${audioFile?.length()} bytes)")
                    audioFile
                } else {
                    Log.e("AudioRecorder", "❌ EL ARCHIVO ESTÁ VACÍO O ES NULL")
                    null
                }
            } else {
                Log.e("AudioRecorder", "❌ RECORDER ERA NULL (No se había iniciado bien)")
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "❌ ERROR AL PARAR: ${e.message}")
            recorder = null
            null
        }
    }
}