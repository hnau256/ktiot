package hnau.ktiot.scheme.topic

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
        separator = MqttTopicParts.Separator.toString(),
    )

private val toTitleDelimiters: Set<Char> = setOf('_', '-')