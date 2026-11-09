package com.example.passagenexpress.core.data.remote.dto.onibus

import com.example.passagenexpress.core.domain.model.onibus.TipoViagemOnibus
import com.example.passagenexpress.core.network.envelope.ApiEnvelope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Fixtures em `src/test/resources/onibus` são respostas reais capturadas do backend local
 * (subdomain `yjaraviagens`, rota SP→RJ) em 2026-11-09.
 */
class OnibusDtosTest {

    // Mesma configuração do NetworkJson usado pelo Retrofit.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("onibus/$name")) { "fixture $name" }.readText()

    @Test
    fun `viagens - item real vira ViagemOnibus`() {
        val env = json.decodeFromString(
            ApiEnvelope.serializer(ViagensOnibusResponseDto.serializer()),
            fixture("viagens.json"),
        )
        val viagem = env.data!!.trechos.data.single().toDomain()

        assertEquals("4c551c0c-e792-3b32-a78a-aaaf69c98ad8", viagem.tripUuid)
        assertEquals("Expresso Do Sul", viagem.empresaNome)
        assertEquals("Executivo", viagem.classe)
        assertEquals("sao-paulo-tiete-sp", viagem.origemSlug)
        assertEquals("rio-de-janeiro-rj", viagem.destinoSlug)
        assertEquals(LocalDateTime.of(2026, 11, 20, 0, 1), viagem.saida)
        assertEquals(LocalDateTime.of(2026, 11, 20, 6, 31), viagem.chegada)
        // data_embarque volta intacta (com offset) — é o que vai no pedido.
        assertEquals("2026-11-20T00:01:00-03:00", viagem.dataEmbarqueRaw)
        assertEquals(189.99, viagem.valor, 0.001)
        assertEquals(10, viagem.assentosDisponiveis)
        assertEquals(TipoViagemOnibus.Direta, viagem.tipo)
        assertEquals(1, viagem.escalas.size)
        assertFalse(viagem.temConexao)
    }

    @Test
    fun `assentos - mapa real com is_ocupado 0-1 e corredor ausente`() {
        val env = json.decodeFromString(ApiEnvelope.serializer(JsonElement.serializer()), fixture("assentos.json"))
        val assentos = env.data!!.assentosArray()!!.mapNotNull {
            json.decodeFromJsonElement(AssentoOnibusDto.serializer(), it).toDomain()
        }

        assertTrue(assentos.isNotEmpty())
        val a01 = assentos.first { it.label == "01" }
        assertTrue(a01.ocupado)
        assertEquals(0, a01.linha)
        assertEquals(1, a01.coluna)
        assertEquals(189.99, a01.valor, 0.001)
        val a03 = assentos.first { it.label == "03" }
        assertFalse(a03.ocupado)
        // Corredor: a linha 2 não tem assento no mapa.
        assertTrue(assentos.none { it.linha == 2 })
    }

    @Test
    fun `terminais - item agregado vem com terminal null`() {
        val env = json.decodeFromString(
            ApiEnvelope.serializer(ListSerializer(TerminalDto.serializer())),
            fixture("terminais.json"),
        )
        val manaus = env.data!!.single().toDomain()
        assertEquals("manaus-am", manaus.slug)
        assertEquals("manaus-am-todos", manaus.slugTodasEstacoes)
        assertEquals("Manaus", manaus.cidade)
        assertNotNull(manaus.terminal)
    }

    @Test
    fun `bloqueio - comodos aninhados em data-data`() {
        // Formato da spec (clickbus-frontend-prompt.md §4): envelope.data = {data:{comodos…}, formas_pagamento}.
        val raw = """
            {"success":true,"data":{"data":{"comodos":[{"id":"seat_abc123","seat_id":"seat_abc123",
            "label":"25","numeracao":"25","valor":135.90,"is_ocupado":1}],"taxa_embarque":0,"trecho":null},
            "formas_pagamento":[{"id":1,"nome":"Cartão de Crédito","codigo":"credito"}]}}
        """.trimIndent()
        val env = json.decodeFromString(ApiEnvelope.serializer(JsonElement.serializer()), raw)
        val comodos = env.data!!.comodosArray()!!.mapNotNull {
            json.decodeFromJsonElement(ComodoBloqueadoDto.serializer(), it).toDomain()
        }
        val c = comodos.single()
        assertEquals("seat_abc123", c.seatId)
        assertEquals("25", c.label)
        assertEquals(135.90, c.valor, 0.001)
    }

    @Test
    fun `requests omitem opcionais nulos como o site`() {
        val bloqueio = json.encodeToString(
            BloquearAssentosRequestDto.serializer(),
            BloquearAssentosRequestDto(tripId = "t1", seats = listOf(SeatLabelDto("12"))),
        )
        assertEquals("""{"trip_id":"t1","seats":[{"label":"12"}]}""", bloqueio)

        val conexao = json.encodeToString(
            BloquearAssentosRequestDto.serializer(),
            BloquearAssentosRequestDto(tripId = "t1", seats = listOf(SeatLabelDto("12")), tripLeg = 2),
        )
        assertTrue(conexao.contains("\"trip_leg\":2"))

        val pedido = json.encodeToString(
            CriarPedidoOnibusRequestDto.serializer(),
            CriarPedidoOnibusRequestDto(
                trecho = "t1",
                viagem = "t1",
                contato = ContatoOnibusDto("Maria Souza", "m@x.com", "92999990000"),
                dataComodos = emptyList(),
            ),
        )
        assertFalse(pedido.contains("dataVolta"))
        assertFalse(pedido.contains("agente"))
        assertFalse(pedido.contains("\"qr\""))
        assertTrue(pedido.contains("\"origem\":3"))
    }

    @Test
    fun `valores tolerantes - string BR, numero e nulo`() {
        assertEquals(189.99, json.parseToJsonElement("\"R$ 189,99\"").asDouble(), 0.001)
        assertEquals(189.99, json.parseToJsonElement("\"189.99\"").asDouble(), 0.001)
        assertEquals(189.99, json.parseToJsonElement("189.99").asDouble(), 0.001)
        assertEquals(0.0, null.asDouble(), 0.0)
        assertTrue(json.parseToJsonElement("1").asBoolean())
        assertFalse(json.parseToJsonElement("0").asBoolean())
        assertNull(json.parseToJsonElement("null").asStringOrNull())
    }

    @Test
    fun `pix - expiration ISO com offset`() {
        val dto = PixOnibusResponseDto(
            qrCode = "data:image/png;base64,AAAA",
            pixCopiaCola = "000201...",
            amount = json.parseToJsonElement("407.04"),
            expiration = "2026-08-04T15:30:00-03:00",
        )
        val pix = dto.toDomain()
        assertEquals(407.04, pix.amount, 0.001)
        assertEquals("2026-08-04T18:30:00Z", pix.expiraEm.toString())
    }
}
