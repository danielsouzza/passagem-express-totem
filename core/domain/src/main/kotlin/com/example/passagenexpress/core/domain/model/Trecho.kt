package com.example.passagenexpress.core.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Trecho(
    val id: Long,
    val idViagem: Long,
    val dataEmbarque: LocalDate,
    val horario: LocalTime,
    val tempoViagemMinutos: Int,
    val valor: Double,
    val taxaDeEmbarque: Double,
    val embarcacao: String,
    val poltronaLivre: Boolean,
    val linhas: Int,
    val colunas: Int,
    val tiposComodos: List<TipoComodo>,
    val municipioOrigem: Municipio,
    val municipioDestino: Municipio,
    val desconto: Desconto? = null,
    /** Preço específico por tipo de cômodo. Esparso: um tipo sem registro aqui usa `valor`. */
    val valores: List<ValorPorTipo> = emptyList(),
)

/** Preço de um tipo de cômodo específico dentro do trecho (ver [valorPorTipo]). */
data class ValorPorTipo(
    val tipoComodidadeId: Long,
    val valor: Double,
)

/**
 * Desconto promocional aplicado a um trecho. `valor` é o **valor absoluto** em reais
 * a subtrair do preço base (ex: 90.0 = R$ 90,00 off). `nome` é o código/label da promo
 * (ex: "PROMO-STM-MAO-120") quando o backend manda.
 *
 * Pode valer só para alguns tipos de cômodo (`valoresPorTipo`/`tiposComodidadeIds`); quando
 * nenhuma das duas listas vem preenchida, é o formato antigo e cobre todos os tipos
 * (retrocompatibilidade — ver [descontoCobreTipo]/[descontoPorTipo]).
 */
data class Desconto(
    val id: Long,
    val valor: Double,
    val nome: String? = null,
    val valoresPorTipo: List<DescontoValorPorTipo> = emptyList(),
    val tiposComodidadeIds: List<Long> = emptyList(),
)

/** Valor de desconto específico de um tipo de cômodo, dentro de [Desconto.valoresPorTipo]. */
data class DescontoValorPorTipo(
    val tipoComodidadeId: Long,
    val valorDesconto: Double,
)

/** id 1 = poltrona; id 2 = rede; id 3 = suíte; id 4 = camarote; id 6 = poltrona executiva/veículo */
data class TipoComodo(
    val id: Long,
    val nome: String,
)

private const val TIPO_REDE = 2L
private const val TIPO_POLTRONA = 1L
private const val TIPO_CAMAROTE = 4L
private const val TIPO_SUITE = 3L

/**
 * Valor-base (sem desconto, sem taxa) de um tipo de cômodo: o registro específico em
 * [Trecho.valores] para aquele tipo, ou — quando não existe registro — o valor padrão do
 * trecho. Nunca cai no valor de outro tipo nem no preço de vitrine.
 */
fun Trecho.valorPorTipo(tipoComodidadeId: Long): Double =
    valores.find { it.tipoComodidadeId == tipoComodidadeId }?.valor ?: valor

/**
 * O desconto do trecho cobre este tipo de cômodo? Formato novo restringe aos tipos listados
 * em `valoresPorTipo`/`tiposComodidadeIds`; formato antigo (nenhuma das duas listas) cobre
 * todos os tipos.
 */
fun Trecho.descontoCobreTipo(tipoComodidadeId: Long): Boolean {
    val d = desconto ?: return false
    if (d.valoresPorTipo.isNotEmpty()) {
        return d.valoresPorTipo.any { it.tipoComodidadeId == tipoComodidadeId }
    }
    if (d.tiposComodidadeIds.isNotEmpty()) {
        return d.tiposComodidadeIds.contains(tipoComodidadeId)
    }
    return true
}

/** Valor a subtrair (em reais) para este tipo, ou 0.0 quando o desconto não cobre/não existe. */
fun Trecho.descontoPorTipo(tipoComodidadeId: Long): Double {
    val d = desconto ?: return 0.0
    if (d.valoresPorTipo.isNotEmpty()) {
        return d.valoresPorTipo.find { it.tipoComodidadeId == tipoComodidadeId }?.valorDesconto ?: 0.0
    }
    if (d.tiposComodidadeIds.isNotEmpty()) {
        return if (d.tiposComodidadeIds.contains(tipoComodidadeId)) d.valor else 0.0
    }
    return d.valor
}

/**
 * Preço-base (SEM desconto, SEM taxa) de um cômodo já reservado/selecionado: o preço da
 * própria instância (`comodoValor`, pivot `comodo_trechos` — ex.: camarote específico) quando
 * o backend devolve um, senão o preço do tipo ([valorPorTipo], com fallback ao valor padrão).
 */
fun Trecho.precoBaseComodo(comodoValor: Double?, tipoComodidadeId: Long): Double =
    comodoValor ?: valorPorTipo(tipoComodidadeId)

/**
 * Preço final exibido/cobrado de um cômodo (SEM taxa): preço-base menos o desconto do tipo.
 * Camarote (id 4) tem preço próprio e, por convenção, não recebe o desconto "flat" do trecho.
 */
fun Trecho.precoDoComodo(comodoValor: Double?, tipoComodidadeId: Long): Double {
    val base = precoBaseComodo(comodoValor, tipoComodidadeId)
    if (tipoComodidadeId == TIPO_CAMAROTE) return base
    return (base - descontoPorTipo(tipoComodidadeId)).coerceAtLeast(0.0)
}

/** `{ base, desconto }` da vitrine (sem taxa) — ver [valorVitrine]. */
data class ValorVitrine(val base: Double, val desconto: Double)

/**
 * Escolhe o item de vitrine em camadas por prioridade: Rede(2)/Assento-Poltrona(1) primeiro
 * (preferindo o que tem desconto; empate ou nenhum desempata por prioridade, Rede antes) —
 * Camarote/Suíte não competem nessa camada. Só se essa camada estiver vazia, cai em
 * Camarote(4)/Suíte(3) por prioridade pura. Ignora registros de `valores` para tipos que o
 * trecho não oferece (fora de `tiposComodos`).
 */
private fun Trecho.comodoVitrine(): ValorPorTipo? {
    val tiposIds = tiposComodos.map { it.id }.toSet()
    val candidatos = valores.filter { it.tipoComodidadeId in tiposIds }
    if (candidatos.isEmpty()) return null

    fun escolher(ordem: List<Long>): List<ValorPorTipo> =
        ordem.mapNotNull { id -> candidatos.find { it.tipoComodidadeId == id } }

    val camada1 = escolher(listOf(TIPO_REDE, TIPO_POLTRONA))
    if (camada1.isNotEmpty()) {
        val comDesconto = camada1.filter { descontoPorTipo(it.tipoComodidadeId) > 0.0 }
        return comDesconto.ifEmpty { camada1 }.first()
    }
    val camada2 = escolher(listOf(TIPO_CAMAROTE, TIPO_SUITE))
    return camada2.firstOrNull()
}

/**
 * Preço de vitrine (card fechado, "a partir de"): valor de **um** tipo representativo,
 * escolhido por [comodoVitrine], sem taxa de embarque. Sem candidato (trecho sem `valores`
 * ou nenhum deles pertencendo aos tipos oferecidos), cai no valor-base do trecho com o
 * desconto "flat" (formato antigo), se houver.
 */
fun Trecho.valorVitrine(): ValorVitrine {
    val item = comodoVitrine()
    return if (item != null) {
        ValorVitrine(base = item.valor, desconto = descontoPorTipo(item.tipoComodidadeId))
    } else {
        ValorVitrine(base = valor, desconto = desconto?.valor ?: 0.0)
    }
}
