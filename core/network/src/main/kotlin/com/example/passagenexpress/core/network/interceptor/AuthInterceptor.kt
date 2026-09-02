package com.example.passagenexpress.core.network.interceptor

import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    // O subdomain muda só no setup. Em vez de ler o DataStore (I/O de disco) a cada request via
    // runBlocking, mantemos uma cópia em memória atualizada por uma coleta contínua. `intercept`
    // lê esse campo sem bloquear. `null` = ainda não carregado (só na 1ª request pós-boot, quando
    // fazemos um único fallback bloqueante pra garantir o valor correto).
    @Volatile
    private var cachedSubdomain: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            totemConfigRepository.config.collect { cachedSubdomain = it.subdomain }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val subdomain = cachedSubdomain
            ?: runBlocking { totemConfigRepository.config.first().subdomain }
                .also { cachedSubdomain = it }

        // `subdomain` vai SEMPRE em qualquer método. Quando não há subdomain configurado,
        // mandamos string vazia — `?subdomain=` (essa API é doida e diferencia ausente
        // de vazio, então o param sempre tem que estar presente).
        val urlBuilder = original.url.newBuilder()
        if (original.url.queryParameter(SUBDOMAIN_PARAM) == null) {
            urlBuilder.addQueryParameter(SUBDOMAIN_PARAM, subdomain)
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
