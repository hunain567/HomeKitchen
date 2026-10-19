package com.nexolite.homekitchen.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rupeeFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale("en", "PK"))
private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
private val shortDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val weekdayFormat = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

fun rupees(amount: Long): String = "Rs " + rupeeFormat.format(amount)

fun today(): Long = LocalDate.now().toEpochDay()

fun dayLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFormat)

fun shortDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortDayFormat)

fun weekday(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(weekdayFormat)

fun monthLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(monthFormat)

fun timeLabel(minutes: Int): String = LocalTime.of(minutes / 60, minutes % 60).format(timeFormat)

/** Describes a day relative to today: "Today", "Tomorrow", "Yesterday" or the date. */
fun relativeDay(epochDay: Long): String = when (epochDay - today()) {
    0L -> "Today"
    1L -> "Tomorrow"
    -1L -> "Yesterday"
    else -> dayLabel(epochDay)
}

fun monthStart(epochDay: Long): Long = LocalDate.ofEpochDay(epochDay).withDayOfMonth(1).toEpochDay()

fun monthEnd(epochDay: Long): Long {
    val d = LocalDate.ofEpochDay(epochDay)
    return d.withDayOfMonth(d.lengthOfMonth()).toEpochDay()
}
