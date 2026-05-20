package com.piania.app.data.remote

import com.piania.app.data.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Obtenemos el token de forma síncrona. Esto debe hacerse con precaución
        // ya que bloquea el hilo, pero es necesario para OkHttp.
        val token = runBlocking {
            sessionManager.authToken.first()
        }

        val originalRequest = chain.request()

        // Si tenemos un token, clonamos la petición y añadimos la cabecera Authorization
        val requestBuilder = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
        } else {
            originalRequest.newBuilder()
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}