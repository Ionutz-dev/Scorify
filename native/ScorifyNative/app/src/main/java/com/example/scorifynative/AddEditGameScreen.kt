package com.example.scorifynative

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGameScreen(
    gameId: Int?,
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val game = gameId?.let { viewModel.getGameById(it) }
    val isEditMode = game != null

    val isLoading by viewModel.isLoading.collectAsState()

    var homeTeam by remember { mutableStateOf(game?.homeTeam ?: "") }
    var awayTeam by remember { mutableStateOf(game?.awayTeam ?: "") }
    var homeScore by remember { mutableStateOf(game?.homeScore?.toString() ?: "0") }
    var awayScore by remember { mutableStateOf(game?.awayScore?.toString() ?: "0") }
    var location by remember { mutableStateOf(game?.location ?: "") }
    var sportType by remember { mutableStateOf(game?.sportType ?: "Football") }
    var status by remember { mutableStateOf(game?.status ?: "Scheduled") }
    var notes by remember { mutableStateOf(game?.notes ?: "") }
    var selectedDate by remember { mutableStateOf(game?.date ?: Date()) }

    var homeTeamError by remember { mutableStateOf(false) }
    var awayTeamError by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }
    var scoreError by remember { mutableStateOf(false) }

    val sportTypes = listOf("Football", "Basketball", "Tennis", "Baseball", "Hockey", "Other")
    val statuses = listOf("Scheduled", "In Progress", "Completed", "Cancelled")

    var expandedSport by remember { mutableStateOf(false) }
    var expandedStatus by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Game" else "Add New Game",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF37474F),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Sport Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedSport,
                    onExpandedChange = { expandedSport = it }
                ) {
                    OutlinedTextField(
                        value = sportType,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSport) },
                        enabled = !isLoading
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSport,
                        onDismissRequest = { expandedSport = false }
                    ) {
                        sportTypes.forEach { sport ->
                            DropdownMenuItem(
                                text = { Text(sport) },
                                onClick = {
                                    sportType = sport
                                    expandedSport = false
                                }
                            )
                        }
                    }
                }

                Text(
                    "Home Team",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                OutlinedTextField(
                    value = homeTeam,
                    onValueChange = {
                        homeTeam = it
                        homeTeamError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter home team name") },
                    isError = homeTeamError,
                    enabled = !isLoading,
                    supportingText = {
                        if (homeTeamError) {
                            Text("Home team is required", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Text(
                    "Away Team",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                OutlinedTextField(
                    value = awayTeam,
                    onValueChange = {
                        awayTeam = it
                        awayTeamError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter away team name") },
                    isError = awayTeamError,
                    enabled = !isLoading,
                    supportingText = {
                        if (awayTeamError) {
                            Text("Away team is required", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Home Score",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = homeScore,
                            onValueChange = {
                                homeScore = it
                                scoreError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = scoreError,
                            enabled = !isLoading
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Away Score",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = awayScore,
                            onValueChange = {
                                awayScore = it
                                scoreError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = scoreError,
                            enabled = !isLoading
                        )
                    }
                }
                if (scoreError) {
                    Text(
                        "Invalid score values",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Text(
                    "Date & Time",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )

                val context = LocalContext.current
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate

                Button(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(year, month, dayOfMonth)

                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        selectedDate = calendar.time
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0E0E0),
                        contentColor = Color(0xFF212121)
                    )
                ) {
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy, HH:mm", Locale.getDefault())
                    Text(dateFormat.format(selectedDate))
                }

                Text(
                    "Location",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it
                        locationError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter location") },
                    isError = locationError,
                    enabled = !isLoading,
                    supportingText = {
                        if (locationError) {
                            Text("Location is required", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Text(
                    "Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedStatus,
                    onExpandedChange = { expandedStatus = it }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                        enabled = !isLoading
                    )
                    ExposedDropdownMenu(
                        expanded = expandedStatus,
                        onDismissRequest = { expandedStatus = false }
                    ) {
                        statuses.forEach { stat ->
                            DropdownMenuItem(
                                text = { Text(stat) },
                                onClick = {
                                    status = stat
                                    expandedStatus = false
                                }
                            )
                        }
                    }
                }

                Text(
                    "Notes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Additional notes (optional)") },
                    maxLines = 4,
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        var hasError = false

                        if (homeTeam.isBlank()) {
                            homeTeamError = true
                            hasError = true
                        }
                        if (awayTeam.isBlank()) {
                            awayTeamError = true
                            hasError = true
                        }
                        if (location.isBlank()) {
                            locationError = true
                            hasError = true
                        }

                        val homeScoreInt = homeScore.toIntOrNull()
                        val awayScoreInt = awayScore.toIntOrNull()
                        if (homeScoreInt == null || awayScoreInt == null) {
                            scoreError = true
                            hasError = true
                        }

                        if (!hasError) {
                            val newGame = Game(
                                id = gameId ?: 0,
                                homeTeam = homeTeam.trim(),
                                awayTeam = awayTeam.trim(),
                                homeScore = homeScoreInt!!,
                                awayScore = awayScoreInt!!,
                                date = selectedDate,
                                location = location.trim(),
                                sportType = sportType,
                                status = status,
                                notes = notes.trim()
                            )

                            if (isEditMode) {
                                viewModel.updateGame(newGame)
                            } else {
                                viewModel.addGame(newGame)
                            }

                            onSaveSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF26A69A)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (isEditMode) "Update" else "Add",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}