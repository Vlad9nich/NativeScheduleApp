package ru.kernelordexter.app

data class JsonClassInfo(
    val start: String,
    val end: String,
    val subject: String,
    val teacher: String,
    val room: String
)

data class JsonDaySchedule(
    val dateStr: String?,
    val isoDate: String?,
    val classes: List<JsonClassInfo> = emptyList()
)

data class JsonWeekSchedule(
    val weekName: String,
    val startDate: String?,
    val days: Map<String, JsonDaySchedule> = emptyMap()
)
