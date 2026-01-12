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
    var notes: String = "",

    // Server synchronization fields
    var serverId: Int? = null,              // ID from server (null if not synced yet)
    var pendingSync: Boolean = false,       // True if waiting to sync with server
    var syncOperation: String? = null       // "CREATE", "UPDATE", "DELETE", or null
)