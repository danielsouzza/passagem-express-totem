package com.example.passagenexpress.core.domain.model

/**
 * Uma passagem materializada após o pagamento (`gerar-passagens`). O backend devolve as
 * passagens agrupadas; o que o totem precisa de cada uma é o `id` (= `passageiro_viagem_id`)
 * usado na rota `passagens/{id}/bilhete-mapeado`. O nome serve só pra UI/log de progresso.
 */
data class PassagemPedido(
    val id: Long,
    val passageiroNome: String = "",
)
