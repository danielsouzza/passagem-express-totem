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

    // O subdomain (e o flag isSuper) mudam só no setup/configurações. Em vez de ler o DataStore
    // (I/O de disco) a cada request via runBlocking, mantemos uma cópia em memória atualizada por
    // uma coleta contínua. `intercept` lê esses campos sem bloquear. `cachedSubdomain == null` =
    // ainda não carregado (só na 1ª request pós-boot, quando fazemos um único fallback bloqueante
    // pra garantir o valor correto).
    @Volatile
    private var cachedSubdomain: String? = null

    @Volatile
    private var cachedIsSuper: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            totemConfigRepository.config.collect {
                cachedSubdomain = it.subdomain
                cachedIsSuper = it.isSuper
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val subdomain = cachedSubdomain
            ?: runBlocking { totemConfigRepository.config.first() }
                .also { cachedSubdomain = it.subdomain; cachedIsSuper = it.isSuper }
                .subdomain

        // `subdomain` vai SEMPRE em qualquer método. Quando não há subdomain configurado,
        // mandamos string vazia — `?subdomain=` (essa API é doida e diferencia ausente
        // de vazio, então o param sempre tem que estar presente).
        val urlBuilder = original.url.newBuilder()
        if (original.url.queryParameter(SUBDOMAIN_PARAM) == null) {
            urlBuilder.addQueryParameter(SUBDOMAIN_PARAM, subdomain)
        }
        // `venda_super` só vai quando a empresa/agência é "super" (`is_super` de `api/empresa/tema`)
        // — sinaliza o backend a usar os meios de pagamento daquela empresa/agência.
        if (cachedIsSuper && original.url.queryParameter(VENDA_SUPER_PARAM) == null) {
            urlBuilder.addQueryParameter(VENDA_SUPER_PARAM, "1")
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
        const val VENDA_SUPER_PARAM = "venda_super"
    }
}
