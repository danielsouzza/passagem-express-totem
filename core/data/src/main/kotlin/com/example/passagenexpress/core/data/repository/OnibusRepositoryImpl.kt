package com.example.passagenexpress.core.data.repository

import android.util.Base64
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.data.remote.api.OnibusApi
import com.example.passagenexpress.core.data.remote.api.PedidoApi
import com.example.passagenexpress.core.data.remote.callEnvelope
import com.example.passagenexpress.core.data.remote.callEnvelopeNullable
import com.example.passagenexpress.core.data.remote.dto.onibus.AssentoOnibusDto
import com.example.passagenexpress.core.data.remote.dto.onibus.BloquearAssentosRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.ComodoBloqueadoDto
import com.example.passagenexpress.core.data.remote.dto.onibus.ContatoOnibusDto
import com.example.passagenexpress.core.data.remote.dto.onibus.CriarPedidoOnibusRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.DataComodoOnibusDto
import com.example.passagenexpress.core.data.remote.dto.onibus.DataVoltaOnibusDto
import com.example.passagenexpress.core.data.remote.dto.onibus.DesbloquearAssentosRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.PixOnibusRequestDto
import com.example.passagenexpress.core.data.remote.dto.onibus.SeatLabelDto
import com.example.passagenexpress.core.data.remote.dto.onibus.asDouble
import com.example.passagenexpress.core.data.remote.dto.onibus.assentosArray
import com.example.passagenexpress.core.data.remote.dto.onibus.comodosArray
import com.example.passagenexpress.core.data.remote.dto.onibus.toDomain
import com.example.passagenexpress.core.domain.model.onibus.AssentoBloqueado
import com.example.passagenexpress.core.domain.model.onibus.AssentoOnibus
import com.example.passagenexpress.core.domain.model.onibus.ItemPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.NovoPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PixOnibus
import com.example.passagenexpress.core.domain.model.onibus.StatusPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.Terminal
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import com.example.passagenexpress.core.domain.repository.OnibusRepository
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnibusRepositoryImpl @Inject constructor(
    private val api: OnibusApi,
    private val pedidoApi: PedidoApi,
    private val totemConfigRepository: TotemConfigRepository,
) : OnibusRepository {

    // Decodifica os pedaços lidos como JsonElement (mapa de assentos, comodos do bloqueio).
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    override suspend fun buscarTerminais(search: String): AppResult<List<Terminal>> =
        callEnvelope({ api.getTerminais(search) }) { dtos ->
            dtos.map { it.toDomain() }.filter { it.slug.isNotEmpty() || it.slugTodasEstacoes.isNotEmpty() }
        }

    override suspend fun buscarViagens(from: String, to: String, data: LocalDate): AppResult<List<ViagemOnibus>> =
        callEnvelope({ api.getViagens(from, to, data.format(DateTimeFormatter.ISO_LOCAL_DATE)) }) { resp ->
            // Item malformado é descartado em vez de derrubar a lista inteira.
            resp.trechos.data
                .mapNotNull { runCatching { it.toDomain() }.getOrNull() }
                .sortedBy { it.saida }
        }

    override suspend fun obterTrechoDaPerna(tripUuid: String): AppResult<ViagemOnibus> =
        callEnvelope({ api.getTrechoDaPerna(tripUuid) }) { it.toDomain() }

    override suspend fun listarAssentos(tripUuid: String): AppResult<List<AssentoOnibus>> =
        callEnvelope({ api.getAssentos(tripUuid) }) { data ->
            data.assentosArray().orEmpty().mapNotNull { el ->
                json.decodeFromJsonElement(AssentoOnibusDto.serializer(), el).toDomain()
            }
        }

    override suspend fun bloquearAssentos(
        tripUuid: String,
        labels: List<String>,
        tripLeg: Int?,
    ): AppResult<List<AssentoBloqueado>> =
        callEnvelope({
            api.bloquearAssentos(
                BloquearAssentosRequestDto(
                    tripId = tripUuid,
                    seats = labels.map { SeatLabelDto(it) },
                    tripLeg = tripLeg,
                ),
            )
        }) { data ->
            val comodos = data.comodosArray().orEmpty().mapNotNull { el ->
                json.decodeFromJsonElement(ComodoBloqueadoDto.serializer(), el).toDomain()
            }
            check(comodos.isNotEmpty()) { "Bloqueio sem assentos na resposta" }
            comodos
        }

    override suspend fun desbloquearAssentos(tripUuid: String, seatIds: List<String>): AppResult<Unit> =
        callEnvelopeNullable({
            api.desbloquearAssentos(DesbloquearAssentosRequestDto(tripId = tripUuid, seatIds = seatIds))
        }) { }

    override suspend fun criarPedido(novo: NovoPedidoOnibus): AppResult<PedidoOnibus> {
        // Mesma origem da venda (agente/qr) que o pedido de barco leva — ver PedidoRepositoryImpl.
        val config = runCatching { totemConfigRepository.config.first() }.getOrNull()
        val body = CriarPedidoOnibusRequestDto(
            trecho = novo.tripUuidIda,
            viagem = novo.tripUuidIda,
            contato = ContatoOnibusDto(
                nome = novo.contato.nome,
                email = novo.contato.email,
                telefone = novo.contato.telefone,
            ),
            dataComodos = novo.itensIda.map { it.toDto() },
            dataVolta = novo.tripUuidVolta?.takeIf { novo.itensVolta.isNotEmpty() }?.let { volta ->
                DataVoltaOnibusDto(trecho = volta, viagem = volta, dataComodos = novo.itensVolta.map { it.toDto() })
            },
            agente = config?.agente?.ifBlank { null },
            qr = config?.origemQr?.ifBlank { null },
        )
        return callEnvelope({ api.criarPedido(body) }) { PedidoOnibus(id = it.id, total = it.total.asDouble()) }
    }

    override suspend fun gerarPix(pedidoId: Long, nome: String, cpf: String?): AppResult<PixOnibus> =
        callEnvelope({
            api.gerarPix(PixOnibusRequestDto(pedidoId = pedidoId, nome = nome.ifBlank { null }, cpf = cpf))
        }) { it.toDomain() }

    override suspend fun obterStatus(pedidoId: Long): AppResult<StatusPedidoOnibus> =
        callEnvelope({ pedidoApi.obterStatus(pedidoId) }) { dto ->
            when (dto.status?.trim()?.lowercase()) {
                "pago" -> StatusPedidoOnibus.Pago
                "negado" -> StatusPedidoOnibus.Negado
                "cancelado" -> StatusPedidoOnibus.Cancelado
                else -> StatusPedidoOnibus.Pendente // "Em Venda", "Solicitado"…
            }
        }

    override suspend fun obterBoardingPass(pedidoId: Long): AppResult<ByteArray> =
        callEnvelope({ api.getBoardingPass(pedidoId) }) { dto ->
            val b64 = dto.boardingPassBase64?.substringAfter(",")?.filterNot { it.isWhitespace() }
            check(!b64.isNullOrEmpty()) { "Boarding pass vazio" }
            Base64.decode(b64, Base64.DEFAULT)
        }

    private fun ItemPedidoOnibus.toDto() = DataComodoOnibusDto(
        comodo = assento.seatId,
        valor = assento.valor,
        tipoDoc = passageiro.tipoDoc.apiValue,
        document = passageiro.documento,
        nome = passageiro.nome,
        dataNascimento = passageiro.nascimento.format(DateTimeFormatter.ISO_LOCAL_DATE),
        telefone = passageiro.telefone,
        seatLabel = assento.label,
        tripUuid = tripUuid,
        tripLeg = tripLeg,
        origemSlug = origemSlug,
        destinoSlug = destinoSlug,
        dataEmbarque = dataEmbarque,
    )
}
