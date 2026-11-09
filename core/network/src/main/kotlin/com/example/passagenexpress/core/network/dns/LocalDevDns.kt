package com.example.passagenexpress.core.network.dns

import java.net.InetAddress
import okhttp3.Dns

/**
 * Resolve hosts locais (Traefik/dev) que o emulador/dispositivo não enxerga via DNS normal.
 * `*.localhost` resolve pra loopback automaticamente no seu PC (RFC 6761) e no navegador,
 * mas o emulador tem pilha de rede própria e não compartilha esse loopback — sem isso, o app
 * nem consegue abrir a conexão. Mantém o Host da URL (`techrios.localhost`) intacto, então o
 * Traefik ainda roteia certo pelo header Host; só troca o IP usado pro socket.
 */
class LocalDevDns(
    private val overrides: Map<String, String> = emptyMap(),
    /** IP usado pra qualquer `localhost`/`*.localhost` sem entrada em [overrides]; null = desliga. */
    private val localhostIp: String? = null,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val overrideIp = overrides[hostname]
            ?: localhostIp?.takeIf { hostname == "localhost" || hostname.endsWith(".localhost") }
            ?: return Dns.SYSTEM.lookup(hostname)
        return listOf(InetAddress.getByName(overrideIp))
    }
}
