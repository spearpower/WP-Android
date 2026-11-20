package com.workspace.disciplehub.ui.common

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Today · 7:45 PM", "Tomorrow · 7:45 PM", "Wed 25 Nov · 4:45 PM" in the phone's own time zone. */
fun sessionWhen(startIso: String): String {
    val start = runCatching { OffsetDateTime.parse(startIso).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() ?: return startIso
    val today = LocalDate.now(ZoneId.systemDefault())
    val time = start.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    val day = when (start.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> start.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
    }
    return "$day · $time"
}

/** Minutes until a start time (negative once it has begun). */
fun minutesUntil(startIso: String): Long? = runCatching {
    java.time.Duration.between(OffsetDateTime.now(), OffsetDateTime.parse(startIso)).toMinutes()
}.getOrNull()
