package com.example.scorifynative.network

import retrofit2.http.*

/**
 * Retrofit API Service interface
 * Defines all REST API endpoints for Game operations
 */
interface ApiService {

    /**
     * Get all games from server
     */
    @GET("api/games")
    suspend fun getAllGames(): ApiResponse<List<GameDto>>

    /**
     * Get a single game by ID
     */
    @GET("api/games/{id}")
    suspend fun getGameById(@Path("id") id: Int): ApiResponse<GameDto>

    /**
     * Create a new game on the server
     * Server will assign the ID
     */
    @POST("api/games")
    suspend fun createGame(@Body game: GameDto): ApiResponse<GameDto>

    /**
     * Update an existing game
     */
    @PUT("api/games/{id}")
    suspend fun updateGame(
        @Path("id") id: Int,
        @Body game: GameDto
    ): ApiResponse<GameDto>

    /**
     * Delete a game by ID
     */
    @DELETE("api/games/{id}")
    suspend fun deleteGame(@Path("id") id: Int): ApiResponse<Map<String, Int>>
}