package com.prof18.feedflow.feedsync.greader.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = StreamItemsContentsDTOSerializer::class)
internal data class StreamItemsContentsDTO(
    val items: List<ItemContentDTO> = emptyList(),
    val continuation: String? = null,
)

internal object StreamItemsContentsDTOSerializer : KSerializer<StreamItemsContentsDTO> {
    private val itemsSerializer = ListSerializer(ItemContentDTO.serializer())

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("StreamItemsContentsDTO") {
            element("items", itemsSerializer.descriptor)
            element<String>("continuation", isOptional = true)
        }

    override fun deserialize(decoder: Decoder): StreamItemsContentsDTO {
        val jsonDecoder = decoder as? JsonDecoder ?: error("StreamItemsContentsDTO supports only JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonArray -> StreamItemsContentsDTO(
                items = jsonDecoder.json.decodeFromJsonElement(itemsSerializer, element),
            )

            is JsonObject -> {
                val itemsElement = element["items"] as? JsonArray ?: JsonArray(emptyList())
                val continuation = element["continuation"]?.jsonPrimitive?.contentOrNull
                StreamItemsContentsDTO(
                    items = jsonDecoder.json.decodeFromJsonElement(itemsSerializer, itemsElement),
                    continuation = continuation,
                )
            }

            else -> StreamItemsContentsDTO()
        }
    }

    override fun serialize(encoder: Encoder, value: StreamItemsContentsDTO) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("StreamItemsContentsDTO supports only JSON encoding")
        val json = buildJsonObject {
            put("items", jsonEncoder.json.encodeToJsonElement(itemsSerializer, value.items))
            value.continuation?.let { put("continuation", JsonPrimitive(it)) }
        }
        jsonEncoder.encodeJsonElement(json)
    }
}
