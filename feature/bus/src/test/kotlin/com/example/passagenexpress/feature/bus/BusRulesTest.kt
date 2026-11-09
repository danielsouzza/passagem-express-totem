package com.example.passagenexpress.feature.bus

import com.example.passagenexpress.core.domain.model.onibus.AssentoBloqueado
import com.example.passagenexpress.core.domain.model.onibus.ContatoOnibus
import com.example.passagenexpress.core.domain.model.onibus.EscalaOnibus
import com.example.passagenexpress.core.domain.model.onibus.PassageiroOnibus
import com.example.passagenexpress.core.domain.model.onibus.TipoDocOnibus
import com.example.passagenexpress.core.domain.model.onibus.TipoViagemOnibus
import com.example.passagenexpress.core.domain.model.onibus.ViagemOnibus
import com.example.passagenexpress.feature.bus.passengers.cpfValido
import com.example.passagenexpress.feature.bus.passengers.parseNascimento
import com.example.passagenexpress.feature.bus.payment.BusPaymentViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BusRulesTest {

    @Test
    fun `cpf - digitos verificadores`() {
        assertTrue(cpfValido("52998224725"))
        assertFalse(cpfValido("52998224724"))
        assertFalse(cpfValido("11111111111"))
        assertFalse(cpfValido("123"))
    }

    @Test
    fun `nascimento - data valida e nao futura`() {
        assertEquals(LocalDate.of(1990, 5, 20), parseNascimento("20051990"))
        assertNull(parseNascimento("31021990"))
        assertNull(parseNascimento("0105199"))
        val amanha = LocalDate.now().plusDays(1)
        assertNull(parseNascimento("%02d%02d%04d".format(amanha.dayOfMonth, amanha.monthValue, amanha.year)))
    }

    @Test
    fun `pedido - passageiros espelhados por indice em todas as pernas e na volta`() {
        val saida = LocalDateTime.of(2026, 11, 20, 8, 0)
        fun viagem(uuid: String, legs: Int) = ViagemOnibus(
            tripUuid = uuid, empresaNome = "X", empresaLogo = null, classe = "", origemNome = "A",
            origemSlug = "a", destinoNome = "B", destinoSlug = "b", saida = saida, chegada = null,
            dataEmbarqueRaw = "2026-11-20T08:00:00-03:00", duracao = "", valor = 100.0,
            assentosDisponiveis = 10, tipo = if (legs > 1) TipoViagemOnibus.Conexao else TipoViagemOnibus.Direta,
            escalas = (1..legs).map { EscalaOnibus("$uuid-$it", it) },
        )
        fun perna(uuid: String, leg: Int, vararg seats: String) = PernaSelecionada(
            tripUuid = uuid, tripLeg = leg, origemSlug = "o$leg", destinoSlug = "d$leg", origemNome = "",
            destinoNome = "", dataEmbarque = "2026-11-20", saida = saida,
            assentos = seats.map { AssentoBloqueado(seatId = "s-$it", label = it, numeracao = it, valor = 100.0) },
        )
        val p1 = PassageiroOnibus("Maria Souza", TipoDocOnibus.Cpf, "52998224725", LocalDate.of(1990, 5, 20), "92999990000")
        val p2 = PassageiroOnibus("João Lima", TipoDocOnibus.Rg, "123456", LocalDate.of(1985, 1, 2), "")
        val state = BusSaleState(
            ida = SelecaoViagem(viagem("ida", 2), listOf(perna("ida-1", 1, "10", "11"), perna("ida-2", 2, "5", "6"))),
            volta = SelecaoViagem(viagem("volta", 1), listOf(perna("volta", 1, "30", "31"))),
            passageiros = listOf(p1, p2),
            contato = ContatoOnibus("Maria Souza", "maria@exemplo.com", "92999990000"),
        )

        val pedido = BusPaymentViewModel.montarPedido(state)

        assertEquals("ida", pedido.tripUuidIda)
        assertEquals(4, pedido.itensIda.size) // 2 pernas × 2 passageiros
        assertEquals(listOf("10", "11", "5", "6"), pedido.itensIda.map { it.assento.label })
        assertEquals(listOf(p1, p2, p1, p2), pedido.itensIda.map { it.passageiro })
        assertEquals(listOf(1, 1, 2, 2), pedido.itensIda.map { it.tripLeg })
        assertEquals("volta", pedido.tripUuidVolta)
        assertEquals(listOf(p1, p2), pedido.itensVolta.map { it.passageiro })
        // Total exibido = 6 assentos × R$ 100 + 13%.
        assertEquals(678.0, state.totalExibido, 0.001)
    }
}
