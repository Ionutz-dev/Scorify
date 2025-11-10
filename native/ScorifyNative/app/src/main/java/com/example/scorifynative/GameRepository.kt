package com.example.scorifynative

import java.text.SimpleDateFormat
import java.util.*

object GameRepository {
    private val games = mutableListOf<Game>()
    private var nextId = 1

    init {
        // Initialize with fake data
        val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())

        games.add(
            Game(
                id = nextId++,
                homeTeam = "Lakers",
                awayTeam = "Warriors",
                homeScore = 98,
                awayScore = 102,
                date = dateFormat.parse("10/12/2025 19:30")!!,
                location = "Staples Center",
                sportType = "Basketball",
                status = "Completed"
            )
        )

        games.add(
            Game(
                id = nextId++,
                homeTeam = "Real Madrid",
                awayTeam = "Barcelona",
                homeScore = 1,
                awayScore = 3,
                date = dateFormat.parse("10/14/2025 20:00")!!,
                location = "Santiago Bernabéu",
                sportType = "Football",
                status = "In Progress"
            )
        )

        games.add(
            Game(
                id = nextId++,
                homeTeam = "Djokovic",
                awayTeam = "Alcaraz",
                homeScore = 0,
                awayScore = 0,
                date = dateFormat.parse("10/20/2025 15:00")!!,
                location = "Wimbledon",
                sportType = "Tennis",
                status = "Scheduled"
            )
        )
    }

    fun getAllGames(): List<Game> {
        return games.toList()
    }

    fun getGameById(id: Int): Game? {
        return games.find { it.id == id }
    }

    fun addGame(game: Game): Game {
        val newGame = game.copy(id = nextId++)
        games.add(newGame)
        return newGame
    }

    fun updateGame(game: Game): Boolean {
        val index = games.indexOfFirst { it.id == game.id }
        return if (index != -1) {
            games[index] = game
            true
        } else {
            false
        }
    }

    fun deleteGame(id: Int): Boolean {
        return games.removeIf { it.id == id }
    }
}