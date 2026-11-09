package com.example.passagenexpress.core.domain.repository

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.onibus.AssentoBloqueado
import com.example.passagenexpress.core.domain.model.onibus.AssentoOnibus
import com.example.passagenexpress.core.domain.model.onibus.NovoPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PixOnibus
import com.example.passagenexpress.core.domain.model.onibus.StatusPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.Terminal
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import java.time.LocalDate

/**
 * Venda rodoviária (`api/onibus/...`). Isolado do fluxo hidroviário: nada aqui é usado pelas telas
 * de barco. Contrato espelhado do site Yjara (`site-yjara/src/services/fetch.js`).
 */
interface OnibusRepository {
    /** `GET api/onibus/terminais?search=` — exige ≥ 3 caracteres (regra do back/site). */
    suspend fun buscarTerminais(search: String): AppResult<List<Terminal>>

    /** `GET api/onibus/viagens?from&to&departureDate`. */
    suspend fun buscarViagens(from: String, to: String, data: LocalDate): AppResult<List<ViagemOnibus>>

    /** `GET api/onibus/viagens/{tripUuid}/trecho` — dados de uma perna de viagem com conexão. */
    suspend fun obterTrechoDaPerna(tripUuid: String): AppResult<ViagemOnibus>

    /** `GET api/onibus/viagens/{tripUuid}` — mapa de assentos. */
    suspend fun listarAssentos(tripUuid: String): AppResult<List<AssentoOnibus>>

    /**
     * `POST api/onibus/reservas/bloquear-assentos`. [tripLeg] só vai em conexões (igual ao site).
     * Falha de negócio (assento já vendido) volta como `AppError.Validation` com a mensagem do back.
     */
    suspend fun bloquearAssentos(tripUuid: String, labels: List<String>, tripLeg: Int?): AppResult<List<AssentoBloqueado>>

    /** `DELETE api/onibus/reservas/desbloquear-assentos` — best-effort (o bloqueio expira em 10 min). */
    suspend fun desbloquearAssentos(tripUuid: String, seatIds: List<String>): AppResult<Unit>

    /** `POST api/onibus/pedidos`. */
    suspend fun criarPedido(novo: NovoPedidoOnibus): AppResult<PedidoOnibus>

    /** `POST api/onibus/pagamentos/pix` — visitante manda `nome` + `cpf` do pagador. */
    suspend fun gerarPix(pedidoId: Long, nome: String, cpf: String?): AppResult<PixOnibus>

    /** `GET api/pedidos/{id}/status` (mesmo endpoint do barco), lido pelo `data.status` top-level. */
    suspend fun obterStatus(pedidoId: Long): AppResult<StatusPedidoOnibus>

    /** `GET api/onibus/pedidos/{id}/boarding-pass` → PDF decodificado. */
    suspend fun obterBoardingPass(pedidoId: Long): AppResult<ByteArray>
}
