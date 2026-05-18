package com.example.passagenexpress.core.network.json

import kotlinx.serialization.json.Json

val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    // explicitNulls = true (default) — emite `"foo": null` no body, alinhado com o web,
    // que envia ex.: `desconto_id: null` quando o trecho não tem desconto
    // (`home.vue#populateComodos`). Coerções de input lidam com PHP omitindo campos.
    encodeDefaults = true
}
