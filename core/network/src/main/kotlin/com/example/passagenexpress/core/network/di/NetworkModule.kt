package com.example.passagenexpress.core.network.di

import com.example.passagenexpress.core.network.BuildConfig
import com.example.passagenexpress.core.network.interceptor.AuthInterceptor
import com.example.passagenexpress.core.network.json.NetworkJson
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named(AuthInterceptor.API_TOKEN_QUALIFIER)
    fun provideApiToken(): String = BuildConfig.API_TOKEN

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // Não seguir redirects automaticamente — a spec HTTP converte POST→GET em
            // 301/302/303, o que mascara o problema real. Com isso desligado, qualquer
            // 30x aparece como response e podemos ver pra onde o backend tava mandando.
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(authInterceptor)

        if (BuildConfig.ENABLE_HTTP_LOGGING) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(NetworkJson.asConverterFactory(contentType))
            .build()
    }
}
