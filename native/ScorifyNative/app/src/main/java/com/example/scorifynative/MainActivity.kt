package com.example.scorifynative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.scorifynative.ui.theme.ScorifyNativeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScorifyNativeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScorifyApp()
                }
            }
        }
    }
}

@Composable
fun ScorifyApp() {
    val navController = rememberNavController()
    val viewModel: GameViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "gameList"
    ) {
        composable("gameList") {
            val games by viewModel.games.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val successMessage by viewModel.successMessage.collectAsState()

            GameListScreen(
                games = games,
                isLoading = isLoading,
                errorMessage = errorMessage,
                successMessage = successMessage,
                onGameClick = { gameId ->
                    navController.navigate("editGame/$gameId")
                },
                onAddClick = {
                    navController.navigate("addGame")
                },
                onDeleteClick = { game ->
                    viewModel.deleteGame(game.id)
                },
                onErrorDismiss = {
                    viewModel.clearErrorMessage()
                },
                onSuccessDismiss = {
                    viewModel.clearSuccessMessage()
                }
            )
        }

        composable("addGame") {
            AddEditGameScreen(
                gameId = null,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "editGame/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId")
            AddEditGameScreen(
                gameId = gameId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}