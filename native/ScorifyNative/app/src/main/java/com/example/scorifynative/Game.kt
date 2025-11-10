package com.example.scorifynative

import java.util.Date

data class Game(
    val id: Int,
    var homeTeam: String,
    var awayTeam: String,
    var homeScore: Int,
    var awayScore: Int,
    var date: Date,
    var location: String,
    var sportType: String,
    var status: String,
    var notes: String = ""
)