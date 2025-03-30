package com.example.tp1application2


data class Event(
    val id: String,       // ID du document Firebase (toujours String)
    val userId: Comparable<*>,     // ID utilisateur comme Long
    val date: String,
    val startTime: String,
    val endTime: String,
    val description: String
)