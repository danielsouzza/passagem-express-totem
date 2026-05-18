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
            is JsonArray -> if (element.isEmpty()) emptyMap() else error(
                "Expected JSON object for map, got non-empty array: $element",
            )
            is JsonObject -> jsonDecoder.json.decodeFromJsonElement(mapDelegate, element)
            else -> error("Expected object or empty array, got $element")
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, V>) {
        mapDelegate.serialize(encoder, value)
    }
}

/** Convenience for the common `Map<String, List<ComodoDto>>` shape. */
fun <T> phpAssocListMapSerializer(itemSerializer: KSerializer<T>): KSerializer<Map<String, List<T>>> =
    PhpAssocMapSerializer(ListSerializer(itemSerializer))
