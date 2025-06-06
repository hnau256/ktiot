package hnau.ktiot.client.projector.utils

import hnau.ktiot.scheme.topic.ChildTopic
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.MqttTopicParts

fun ChildTopic.toTitle(): String = when (this) {
    is ChildTopic.Absolute -> MqttTopicParts.Companion.Separator + topic.parts.toTitle()
    is ChildTopic.Relative -> child.parts.toTitle()
}

@Deprecated("Use ChildTopic.toTitle")
fun MqttTopic.Relative.toTitle(): String =
    parts.toTitle()

@Deprecated("Use ChildTopic.toTitle")
fun MqttTopic.Absolute.toTitle(): String =
    MqttTopicParts.Companion.Separator + parts.toTitle()

@Deprecated("Use ChildTopic.toTitle")
fun MqttTopic.toTitle(): String = when (this) {
    is MqttTopic.Absolute -> toTitle()
    is MqttTopic.Relative -> toTitle()
}

fun MqttTopicParts.toTitle(): String = parts
    .map {
        it
            .fold(
                initial = "",
            ) { result, char ->
                val nextChar = when {
                    char.isWhitespace() -> ' '
                    char in toTitleDelimiters -> ' '
                    else -> char
                }
                if (result.isEmpty() && nextChar == ' ') {
                    return@fold result
                }
                val casedChar = when (result.isEmpty()) {
                    true -> nextChar.uppercaseChar()
                    false -> nextChar.lowercaseChar()
                }
                result + casedChar
            }
            .trimEnd()
    }
    .joinToString(
        separator = MqttTopicParts.Companion.Separator.toString(),
    )

private val toTitleDelimiters: Set<Char> = setOf('_', '-')