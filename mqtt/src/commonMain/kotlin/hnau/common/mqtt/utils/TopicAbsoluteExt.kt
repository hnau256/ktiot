package hnau.common.mqtt.utils

import hnau.common.mqtt.types.topic.Topic
import hnau.common.mqtt.types.topic.TopicParts
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus

private val topicAbsoluteRawMapper: Mapper<String, Topic.Absolute> =
    TopicParts.stringMapper + Mapper(Topic::Absolute, Topic.Absolute::parts)

internal val Topic.Absolute.Companion.rawMapper: Mapper<String, Topic.Absolute>
    get() = topicAbsoluteRawMapper

val Topic.Absolute.raw: String
    get() = Topic.Absolute.rawMapper.reverse(this)