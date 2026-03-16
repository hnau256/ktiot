package org.hnau.ktiot.scheme

import org.hnau.ktiot.mqtt.types.topic.Topic

val Topic.Absolute.ktiotElements: Topic.Absolute
    get() = plus(SchemeConstants.schemeTopic)