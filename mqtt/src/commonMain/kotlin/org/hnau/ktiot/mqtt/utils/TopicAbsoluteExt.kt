package org.hnau.ktiot.mqtt.utils

import org.hnau.ktiot.mqtt.types.topic.Topic
import org.hnau.ktiot.mqtt.types.topic.TopicParts
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus

private val topicAbsoluteRawMapper: Mapper<String, Topic.Absolute> =
    TopicParts.stringMapper + Mapper(Topic::Absolute, Topic.Absolute::parts)

internal val Topic.Absolute.Companion.rawMapper: Mapper<String, Topic.Absolute>
    get() = topicAbsoluteRawMapper

val Topic.Absolute.raw: String
    get() = Topic.Absolute.rawMapper.reverse(this)