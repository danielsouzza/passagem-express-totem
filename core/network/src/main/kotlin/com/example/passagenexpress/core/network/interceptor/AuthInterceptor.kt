package com.example.passagenexpress.core.network.interceptor

import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    @Named(API_TOKEN_QUALIFIER) private val apiToken: String,
    private val totemConfigRepository: TotemConfigRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val subdomain = runBlocking { totemConfigRepository.config.first().subdomain }

        val urlBuilder = original.url.newBuilder()
        if (subdomain.isNotEmpty() && original.method.equals("GET", ignoreCase = true)) {
            if (original.url.queryParameter(SUBDOMAIN_PARAM) == null) {
                urlBuilder.addQueryParameter(SUBDOMAIN_PARAM, subdomain)
            }
        }

        val builder = original.newBuilder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")
        if (apiToken.isNotEmpty()) {
            builder.header(HEADER_API_KEY, apiToken)
        }

        return chain.proceed(builder.build())
    }

    companion object {
        const val API_TOKEN_QUALIFIER = "passagenexpress.api_token"
        const val HEADER_API_KEY = "X-API-KEY"
        const val SUBDOMAIN_PARAM = "subdomain"
    }
}
