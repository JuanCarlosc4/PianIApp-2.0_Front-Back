package com.piania.app.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.piania.app.data.SessionManager
import com.piania.app.data.remote.PianiaApiService
import com.piania.app.data.repository.AnnouncementRepository
import com.piania.app.data.repository.PracticeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 1. Enseñamos a Hilt a crear el SessionManager
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    // 2. Enseñamos a Hilt a crear GSON (con tu formato de fecha)
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }

    // 3. Enseñamos a Hilt a configurar OkHttp (Aquí usamos el nuevo método fetchAuthToken)
    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {

        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)

            // TU INTERCEPTOR DE AUTH (Lo dejo igual, funciona bien)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()

                val originalPath = originalRequest.url.encodedPath

                // Auth endpoints reales (api-gateway): /piania/auth/login | /piania/auth/register
                val isAuthEndpoint =
                    originalPath == "/piania/auth/login" || originalPath == "/piania/auth/register"

                val token = sessionManager.fetchAuthToken()

                // En login/register SIEMPRE anónimo (aún no hay token).
                // En el resto, añadimos Authorization si existe.
                if (!isAuthEndpoint && !token.isNullOrEmpty()) {
                    android.util.Log.d("API_INTERCEPTOR", "Authorization -> $originalPath")
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                val response = chain.proceed(requestBuilder.build())

                // Quitamos el log de error manual aquí porque el HttpLoggingInterceptor ya lo mostrará mejor
                response
            }
            .build()
    }

    // 4. Enseñamos a Hilt a crear Retrofit
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.piania.app.BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // 5. Finalmente, creamos la API que usará el Repositorio
    @Provides
    @Singleton
    fun providePianiaApiService(retrofit: Retrofit): PianiaApiService {
        return retrofit.create(PianiaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnnouncementRepository(apiService: PianiaApiService): AnnouncementRepository {
        return AnnouncementRepository(apiService)
    }

    @Provides
    @Singleton
    fun providePracticeRepository(apiService: PianiaApiService): PracticeRepository {
        return PracticeRepository(apiService)
    }
}
