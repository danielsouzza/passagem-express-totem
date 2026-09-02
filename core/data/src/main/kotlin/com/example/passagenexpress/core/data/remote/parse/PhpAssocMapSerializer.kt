package com.example.passagenexpress.core.data.remote.parse

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

/**
 * PHP serializes empty associative arrays as `[]` instead of `{}`. This serializer accepts both
 * `{}` / `{ "key": [...] }` (real map) and `[]` (empty marker), turning the latter into an empty map.
 */
class PhpAssocMapSerializer<V>(
    private val valueSerializer: KSerializer<V>,
) : KSerializer<Map<String, V>> {
    private val mapDelegate = MapSerializer(String.serializer(), valueSerializer)
    override val descriptor: SerialDescriptor = mapDelegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, V> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return mapDelegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonObject -> jsonDecoder.json.decodeFromJsonElement(mapDelegate, element)
            // `[]` = map vazio (quirk do PHP). Qualquer outra forma inesperada (array não-vazio,
            // primitivo, null) é tolerada como mapa vazio em vez de lançar — o totem não pode
            // fechar por causa de um formato fora do padrão.
            else -> {
                if (!(element is JsonArray && element.isEmpty())) {
                    android.util.Log.w(
                        "PhpAssocMapSerializer",
                        "Esperado objeto ou array vazio para o map, recebido: $element — usando mapa vazio",
                    )
                }
                emptyMap()
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, V>) {
        mapDelegate.serialize(encoder, value)
    }
}

/** Convenience for the common `Map<String, List<ComodoDto>>` shape. */
fun <T> phpAssocListMapSerializer(itemSerializer: KSerializer<T>): KSerializer<Map<String, List<T>>> =
    PhpAssocMapSerializer(ListSerializer(itemSerializer))
