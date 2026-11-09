package com.example.passagenexpress.core.domain.usecase.onibus

import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.onibus.AssentoBloqueado
import com.example.passagenexpress.core.domain.model.onibus.AssentoOnibus
import com.example.passagenexpress.core.domain.model.onibus.NovoPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.PixOnibus
import com.example.passagenexpress.core.domain.model.onibus.StatusPedidoOnibus
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import com.example.passagenexpress.core.domain.repository.OnibusRepository
import java.time.LocalDate
import javax.inject.Inject

class BuscarViagensOnibusUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(from: String, to: String, data: LocalDate): AppResult<List<ViagemOnibus>> =
        repo.buscarViagens(from, to, data)
}

class ObterTrechoDaPernaUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(tripUuid: String): AppResult<ViagemOnibus> = repo.obterTrechoDaPerna(tripUuid)
}

class ListarAssentosOnibusUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(tripUuid: String): AppResult<List<AssentoOnibus>> = repo.listarAssentos(tripUuid)
}

class BloquearAssentosUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(tripUuid: String, labels: List<String>, tripLeg: Int?): AppResult<List<AssentoBloqueado>> =
        repo.bloquearAssentos(tripUuid, labels, tripLeg)
}

class DesbloquearAssentosUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(tripUuid: String, seatIds: List<String>): AppResult<Unit> =
        if (seatIds.isEmpty()) AppResult.Success(Unit) else repo.desbloquearAssentos(tripUuid, seatIds)
}

class CriarPedidoOnibusUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(novo: NovoPedidoOnibus): AppResult<PedidoOnibus> = repo.criarPedido(novo)
}

class GerarPixOnibusUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(pedidoId: Long, nome: String, cpf: String?): AppResult<PixOnibus> =
        repo.gerarPix(pedidoId, nome, cpf)
}

class ObterStatusPedidoOnibusUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(pedidoId: Long): AppResult<StatusPedidoOnibus> = repo.obterStatus(pedidoId)
}

class ObterBoardingPassUseCase @Inject constructor(private val repo: OnibusRepository) {
    suspend operator fun invoke(pedidoId: Long): AppResult<ByteArray> = repo.obterBoardingPass(pedidoId)
}
