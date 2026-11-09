package com.example.passagenexpress.core.domain.model.onibus

import com.example.passagenexpress.core.domain.model.Modalidade
import com.example.passagenexpress.core.domain.model.TotemConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnibusModelTest {

    @Test
    fun `preco exibido soma 13 por cento como o site`() {
        assertEquals(214.69, PrecoOnibus.comTaxa(189.99), 0.001)
        assertEquals(0.0, PrecoOnibus.comTaxa(0.0), 0.0)
    }

    @Test
    fun `opcoes de terminal - agregado uma vez por cidade, sem repetir slug`() {
        val terminais = listOf(
            Terminal(1, "Sao Paulo, SP - TODOS", "SP", "São Paulo", null, "sao-paulo-sp-todos", "sao-paulo-sp-todos"),
            Terminal(4017, "Sao Paulo, SP - Tiete", "SP", "São Paulo", "Rodoviária do Tietê", "sao-paulo-tiete-sp", "sao-paulo-sp-todos"),
            Terminal(4014, "Sao Paulo, SP - Barra Funda", "SP", "São Paulo", "Barra Funda", "sao-paulo-barra-funda-sp", "sao-paulo-sp-todos"),
        )
        val opcoes = terminais.toOpcoes { cidade, uf -> "Todos de $cidade - $uf" }
        assertEquals(
            listOf("sao-paulo-sp-todos", "sao-paulo-tiete-sp", "sao-paulo-barra-funda-sp"),
            opcoes.map { it.slug },
        )
        assertEquals("Todos de São Paulo - SP", opcoes.first().nome)
    }

    @Test
    fun `totem so hidro continua com a regra antiga de porto ou embarcacao`() {
        val base = TotemConfig.Empty.copy(setupComplete = true)
        assertFalse(base.isOperacional)
        assertTrue(base.copy(portoSlug = "porto-x").isOperacional)
        assertTrue(base.copy(embarcacaoId = 7L).isOperacional)
    }

    @Test
    fun `cada modalidade ativa exige sua origem`() {
        val rodo = TotemConfig.Empty.copy(setupComplete = true, modalidades = setOf(Modalidade.Rodoviario))
        assertFalse(rodo.isOperacional)
        assertTrue(rodo.copy(terminalSlug = "manaus-am").isOperacional)

        val ambas = rodo.copy(modalidades = setOf(Modalidade.Hidroviario, Modalidade.Rodoviario), terminalSlug = "manaus-am")
        assertFalse(ambas.isOperacional) // falta porto/embarcação
        assertTrue(ambas.copy(portoSlug = "porto-x").isOperacional)

        assertFalse(rodo.copy(modalidades = emptySet(), terminalSlug = "manaus-am").isOperacional)
    }
}
