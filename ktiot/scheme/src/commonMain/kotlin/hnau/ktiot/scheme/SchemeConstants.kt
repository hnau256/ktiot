package hnau.ktiot.scheme

import hnau.common.kotlin.Loadable
import hnau.common.kotlin.mapper.Mapper
import hnau.common.kotlin.mapper.bytesToString
import hnau.common.kotlin.mapper.plus
import hnau.common.kotlin.mapper.toMapper
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object SchemeConstants {

    val schemeTopic = MqttTopic.Relative("ktiot")

    val mapper: Mapper<ByteArray, Loadable<List<Element>>> = Mapper.bytesToString + Json.toMapper(
        serializer = Loadable.serializer(
            typeSerial0 = ListSerializer(
                elementSerializer = Element.serializer()
            )
        )
    )
}