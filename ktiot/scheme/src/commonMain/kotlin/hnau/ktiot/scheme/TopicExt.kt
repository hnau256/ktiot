package hnau.ktiot.scheme

import hnau.common.mqtt.types.topic.Topic

val Topic.Absolute.ktiotElements: Topic.Absolute
    get() = plus(SchemeConstants.schemeTopic)