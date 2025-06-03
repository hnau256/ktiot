package hnau.ktiot.client.projector.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import hnau.ktiot.client.projector.Res
import hnau.ktiot.client.projector.days_short
import hnau.ktiot.client.projector.hours_short
import hnau.ktiot.client.projector.minutes_short
import hnau.ktiot.client.projector.seconds_short
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

@Composable
fun Duration.format(): String {
    val wholeSeconds = inWholeSeconds
    val daysTitle = stringResource(Res.string.days_short)
    val hoursTitle = stringResource(Res.string.hours_short)
    val minutesTitle = stringResource(Res.string.minutes_short)
    val secondsTitle = stringResource(Res.string.seconds_short)
    return remember(
        wholeSeconds,
        daysTitle,
        hoursTitle,
        minutesTitle,
        secondsTitle,
    ) {
        var seconds = wholeSeconds
        var minutes = seconds / 60
        seconds -= minutes * 60
        var hours = minutes / 60
        minutes -= hours * 60
        val days = hours / 24
        hours -= days * 24
        listOf(
            days to daysTitle,
            hours to hoursTitle,
            minutes to minutesTitle,
            seconds to secondsTitle,
        )
            .withIndex()
            .toList()
            .let { parts ->
                parts.dropWhile { (i, countWithTitle) ->
                    i < parts.lastIndex && countWithTitle.first <= 0
                }
            }
            .map { it.value }
            .joinToString(
                separator = " ",
            ) { (count, title) ->
                "$count$title"
            }
    }
}