package hnau.ktiot.scheme

import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.bytesToString
import org.hnau.commons.kotlin.mapper.plus
import org.hnau.commons.kotlin.mapper.toMapper
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