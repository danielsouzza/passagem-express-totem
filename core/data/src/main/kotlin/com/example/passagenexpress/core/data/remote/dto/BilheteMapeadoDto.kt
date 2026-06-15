package com.example.passagenexpress.core.data.remote.dto

import com.example.passagenexpress.core.domain.model.BilheteAgencia
import com.example.passagenexpress.core.domain.model.BilheteEmbarcacao
import com.example.passagenexpress.core.domain.model.BilheteEmbarque
import com.example.passagenexpress.core.domain.model.BilheteEmpresa
import com.example.passagenexpress.core.domain.model.BilheteMapeado
import com.example.passagenexpress.core.domain.model.BilheteTrecho
import com.example.passagenexpress.core.domain.model.BilheteValores
import com.example.passagenexpress.core.domain.model.BilheteViagem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Resposta de `GET passagens/{id}/bilhete-mapeado`. Os textos já vêm formatados pelo
 * backend. `formasPagamento` é um map `{ "Pix": 121 }` — mas, pelo quirk do backend
 * (map vazio vira `[]`), é tipado como [JsonElement] e normalizado em [toDomain].
 */
@Serializable
data class BilheteMapeadoDto(
    val agencia: BilheteAgenciaDto? = null,
    val empresa: BilheteEmpresaDto? = null,
    val embarcacao: BilheteEmbarcacaoDto? = null,
    val trecho: BilheteTrechoDto? = null,
    val viagem: BilheteViagemDto? = null,
    val embarque: BilheteEmbarqueDto? = null,
    val comodo: String? = null,
    val valores: BilheteValoresDto? = null,
    val formasPagamento: JsonElement? = null,
    val troco: String? = null,
    val passageiros: List<String> = emptyList(),
    val bilhete: String? = null,
    val codigoBarras: String? = null,
    val validacao: String? = null,
    val tributos: String? = null,
    val observacoes: List<String> = emptyList(),
    val pdf417: String? = null,
)

@Serializable
data class BilheteAgenciaDto(
    val cnpj: String? = null,
    val nome: String? = null,
    val endereco: String? = null,
    @SerialName("endereco_1") val endereco1: String? = null,
    @SerialName("endereco_2") val endereco2: String? = null,
)

@Serializable
data class BilheteEmpresaDto(
    val nome: String? = null,
    @SerialName("cnpj_cpf") val cnpjCpf: String? = null,
    val ie: String? = null,
    val endereco: String? = null,
    @SerialName("endereco_1") val endereco1: String? = null,
    @SerialName("endereco_2") val endereco2: String? = null,
)

@Serializable
data class BilheteEmbarcacaoDto(val nome: String? = null)

@Serializable
data class BilheteTrechoDto(
    val portoOrigem: String? = null,
    val municipioOrigem: String? = null,
    val municipioDestino: String? = null,
)

@Serializable
data class BilheteViagemDto(
    val linha: String? = null,
    val tipo: String? = null,
)

@Serializable
data class BilheteEmbarqueDto(
    val data: String? = null,
    val horario: String? = null,
    val observacao: String? = null,
)

@Serializable
data class BilheteValoresDto(
    val tarifa: String? = null,
    val taxaEmbarque: String? = null,
    val valorTotal: String? = null,
    val desconto: String? = null,
    val taxaAdicional: String? = null,
    val valorAPagar: String? = null,
)

fun BilheteMapeadoDto.toDomain(): BilheteMapeado = BilheteMapeado(
    agencia = BilheteAgencia(
        cnpj = agencia?.cnpj.orEmpty(),
        nome = agencia?.nome.orEmpty(),
        endereco = agencia?.endereco.orEmpty(),
        endereco1 = agencia?.endereco1.orEmpty(),
        endereco2 = agencia?.endereco2.orEmpty(),
    ),
    empresa = BilheteEmpresa(
        nome = empresa?.nome.orEmpty(),
        cnpjCpf = empresa?.cnpjCpf.orEmpty(),
        ie = empresa?.ie.orEmpty(),
        endereco = empresa?.endereco.orEmpty(),
        endereco1 = empresa?.endereco1.orEmpty(),
        endereco2 = empresa?.endereco2.orEmpty(),
    ),
    embarcacao = BilheteEmbarcacao(nome = embarcacao?.nome.orEmpty()),
    trecho = BilheteTrecho(
        portoOrigem = trecho?.portoOrigem.orEmpty(),
        municipioOrigem = trecho?.municipioOrigem.orEmpty(),
        municipioDestino = trecho?.municipioDestino.orEmpty(),
    ),
    viagem = BilheteViagem(
        linha = viagem?.linha.orEmpty(),
        tipo = viagem?.tipo.orEmpty(),
    ),
    embarque = BilheteEmbarque(
        data = embarque?.data.orEmpty(),
        horario = embarque?.horario.orEmpty(),
        observacao = embarque?.observacao.orEmpty(),
    ),
    comodo = comodo.orEmpty(),
    valores = BilheteValores(
        tarifa = valores?.tarifa.orEmpty(),
        taxaEmbarque = valores?.taxaEmbarque.orEmpty(),
        valorTotal = valores?.valorTotal.orEmpty(),
        desconto = valores?.desconto.orEmpty(),
        taxaAdicional = valores?.taxaAdicional.orEmpty(),
        valorAPagar = valores?.valorAPagar.orEmpty(),
    ),
    formasPagamento = formasPagamento.toStringMap(),
    troco = troco.orEmpty(),
    passageiros = passageiros,
    bilhete = bilhete.orEmpty(),
    codigoBarras = codigoBarras.orEmpty(),
    validacao = validacao.orEmpty(),
    tributos = tributos.orEmpty(),
    observacoes = observacoes,
    pdf417 = pdf417?.takeIf { it.isNotBlank() },
)

/** Aceita JsonObject (`{"Pix": 121}`) e ignora `[]`/null (map vazio do backend). */
private fun JsonElement?.toStringMap(): Map<String, String> {
    val obj = this as? JsonObject ?: return emptyMap()
    return obj.mapValues { (_, value) ->
        (value as? JsonPrimitive)?.content ?: value.toString()
    }
}
