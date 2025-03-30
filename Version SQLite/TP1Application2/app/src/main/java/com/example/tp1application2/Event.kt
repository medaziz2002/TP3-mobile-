package com.example.tp1application2


data class Event(
    val id: Long,
    val userId: Long,
    val date: String,        // Format: "YYYY-MM-DD"
    val startTime: String,   // Format: "HH:mm"
    val endTime: String,     // Format: "HH:mm"
    val description: String
) {
    fun isValid(): Boolean {
        return try {
            val start = java.time.LocalTime.parse(startTime)
            val end = java.time.LocalTime.parse(endTime)
            start.isBefore(end)
        } catch (e: Exception) {
            false
        }
    }

    fun getFormattedTimeRange(): String {
        return "$startTime - $endTime"
    }
}