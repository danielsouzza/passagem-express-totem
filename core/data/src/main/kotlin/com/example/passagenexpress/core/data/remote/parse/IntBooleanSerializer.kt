package com.example.passagenexpress.core.data.remote.parse

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Backend returns `poltrona_livre` (and similar flags) as `0`/`1` instead of JSON booleans.
 * This serializer accepts either form.
 */
object IntBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val json = (decoder as? JsonDecoder)?.decodeJsonElement() as? JsonPrimitive
            ?: return decoder.decodeBoolean()
        json.booleanOrNull?.let { return it }
        json.intOrNull?.let { return it != 0 }
        return json.content.equals("true", ignoreCase = true) || json.content == "1"
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}
