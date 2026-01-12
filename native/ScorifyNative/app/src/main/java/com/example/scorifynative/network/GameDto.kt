package com.example.scorifynative.network

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Game entity
 * Used for network communication with the server
 */
data class GameDto(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("homeTeam")
    val homeTeam: String,

    @SerializedName("awayTeam")
    val awayTeam: String,

    @SerializedName("homeScore")
    val homeScore: Int,

    @SerializedName("awayScore")
    val awayScore: Int,

    @SerializedName("date")
    val date: Long,  // Unix timestamp in milliseconds

    @SerializedName("location")
    val location: String,

    @SerializedName("sportType")
    val sportType: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("notes")
    val notes: String = ""
)

/**
 * Generic API Response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("message")
    val message: String? = null
)

/**
 * WebSocket message structure
 */
data class WebSocketMessage(
    @SerializedName("type")
    val type: String,  // CREATE, UPDATE, DELETE

    @SerializedName("data")
    val data: GameDto? = null
)