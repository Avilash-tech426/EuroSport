package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.db.SportsDatabase
import com.example.model.LiveMatch
import com.example.model.MatchEvent
import com.example.model.NotificationLog
import com.example.model.Player
import com.example.model.SportsData
import com.example.model.Team
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

object MatchSimulator {
    private const val TAG = "MatchSimulator"
    private const val CHANNEL_ID = "euro_sports_live_alerts"
    private const val CHANNEL_NAME = "Live Match Goals & Cards"

    private val _matches = MutableStateFlow<List<LiveMatch>>(emptyList())
    val matches: StateFlow<List<LiveMatch>> = _matches.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private var initDone = false
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // In-app floating alert publisher
    private val _inAppNotification = MutableStateFlow<NotificationLog?>(null)
    val inAppNotification: StateFlow<NotificationLog?> = _inAppNotification.asStateFlow()

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    fun init(context: Context) {
        if (initDone) return
        createNotificationChannel(context)
        _teams.value = SportsData.getInitialTeams()
        _matches.value = SportsData.getInitialSchedules()
        initDone = true
        startSimulation(context)
    }

    fun startSimulation(context: Context) {
        if (simulationJob != null && simulationJob?.isActive == true) return
        simulationJob = scope.launch {
            while (true) {
                delay(6000) // update every 6 seconds to feel live
                val currentMatches = _matches.value.map { it.copy() }
                var updated = false

                currentMatches.forEach { match ->
                    if (match.status == "LIVE") {
                        updated = true
                        // Increment match minute
                        match.minute += Random.nextInt(2, 5)
                        if (match.minute >= 90) {
                            match.minute = 90
                            match.status = "FT"
                            // Match ended, update statistics
                            updateStandingsOnMatchEnd(match)
                            scope.launch(Dispatchers.IO) {
                                recordLog(
                                    context,
                                    match.id,
                                    match.homeTeamName,
                                    match.awayTeamName,
                                    "MATCH_END",
                                    90,
                                    "FT: Full-time whistle has blown. ${match.homeTeamName} ${match.homeScore} - ${match.awayScore} ${match.awayTeamName}",
                                    "${match.homeScore}-${match.awayScore}"
                                )
                            }
                        } else {
                            // Check for random live events
                            val chance = Random.nextFloat()
                            when {
                                chance < 0.12f -> { // Goal!
                                    val scoringHome = Random.nextBoolean()
                                    val (scorerTeamId, scorerTeamName) = if (scoringHome) {
                                        match.homeScore++
                                        match.homeTeamId to match.homeTeamName
                                    } else {
                                        match.awayScore++
                                        match.awayTeamId to match.awayTeamName
                                    }

                                    val player = getRandomTeamPlayer(scorerTeamId, scorerTeamName, match.leagueId)
                                    val detail = "Incredible effort! Blasted past the keeper after a superb assist."
                                    val newEvent = MatchEvent(
                                        type = "GOAL",
                                        minute = match.minute,
                                        teamId = scorerTeamId,
                                        teamName = scorerTeamName,
                                        playerName = player.name,
                                        detail = detail
                                    )
                                    match.events = match.events + newEvent
                                    match.commentary = listOf("${match.minute}' - GOAL! ${player.name} scores for $scorerTeamName! Score: ${match.homeTeamName} ${match.homeScore} - ${match.awayScore} ${match.awayTeamName}") + match.commentary

                                    val title = "⚽ GOAL! $scorerTeamName Scores!"
                                    val text = "${player.name} (${match.minute}') brings the score to ${match.homeTeamName} ${match.homeScore} - ${match.awayScore} ${match.awayTeamName}!"
                                    triggerNotification(context, title, text, match.id)

                                    scope.launch(Dispatchers.IO) {
                                        recordLog(
                                            context,
                                            match.id,
                                            match.homeTeamName,
                                            match.awayTeamName,
                                            "GOAL",
                                            match.minute,
                                            "${player.name} scores for $scorerTeamName. $detail",
                                            "${match.homeScore}-${match.awayScore}"
                                        )
                                    }
                                }
                                chance < 0.22f -> { // Yellow Card
                                    val forHome = Random.nextBoolean()
                                    val (teamId, teamName) = if (forHome) match.homeTeamId to match.homeTeamName else match.awayTeamId to match.awayTeamName
                                    val player = getRandomTeamPlayer(teamId, teamName, match.leagueId)
                                    val detail = "Rough sliding tackle in midfield."
                                    val newEvent = MatchEvent(
                                        type = "YELLOW_CARD",
                                        minute = match.minute,
                                        teamId = teamId,
                                        teamName = teamName,
                                        playerName = player.name,
                                        detail = detail
                                    )
                                    match.events = match.events + newEvent
                                    match.commentary = listOf("${match.minute}' - Yellow Card shown to ${player.name} ($teamName) for a reckless challege.") + match.commentary

                                    val title = "🟨 Yellow Card Given!"
                                    val text = "${player.name} ($teamName) booked in the ${match.minute}'."
                                    triggerNotification(context, title, text, match.id)

                                    scope.launch(Dispatchers.IO) {
                                        recordLog(
                                            context,
                                            match.id,
                                            match.homeTeamName,
                                            match.awayTeamName,
                                            "YELLOW_CARD",
                                            match.minute,
                                            "${player.name} is yellow carded. $detail",
                                            "${match.homeScore}-${match.awayScore}"
                                        )
                                    }
                                }
                                chance < 0.25f -> { // Red Card
                                    val forHome = Random.nextBoolean()
                                    val (teamId, teamName) = if (forHome) match.homeTeamId to match.homeTeamName else match.awayTeamId to match.awayTeamName
                                    val player = getRandomTeamPlayer(teamId, teamName, match.leagueId)
                                    val detail = "Dangerous high foot, direct red card!"
                                    val newEvent = MatchEvent(
                                        type = "RED_CARD",
                                        minute = match.minute,
                                        teamId = teamId,
                                        teamName = teamName,
                                        playerName = player.name,
                                        detail = detail
                                    )
                                    match.events = match.events + newEvent
                                    match.commentary = listOf("${match.minute}' - RED CARD! ${player.name} ($teamName) is sent off!") + match.commentary

                                    val title = "🟥 RED CARD! Dismissal"
                                    val text = "${player.name} ($teamName) receives a straight red in the ${match.minute}'!"
                                    triggerNotification(context, title, text, match.id)

                                    scope.launch(Dispatchers.IO) {
                                        recordLog(
                                            context,
                                            match.id,
                                            match.homeTeamName,
                                            match.awayTeamName,
                                            "RED_CARD",
                                            match.minute,
                                            "RED CARD! ${player.name} is sent off: $detail",
                                            "${match.homeScore}-${match.awayScore}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (updated) {
                    _matches.value = currentMatches
                }
            }
        }
    }

    private suspend fun recordLog(
        context: Context,
        matchId: String,
        homeTeam: String,
        awayTeam: String,
        eventType: String,
        minute: Int,
        description: String,
        score: String
    ) {
        val database = SportsDatabase.getDatabase(context)
        val log = NotificationLog(
            matchId = matchId,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            eventType = eventType,
            minute = minute,
            description = description,
            score = score
        )
        database.notificationLogDao.insertLog(log)
        // Publish to in-app notifier
        _inAppNotification.value = log
    }

    private fun updateStandingsOnMatchEnd(match: LiveMatch) {
        val currentTeams = _teams.value.map { it.copy() }
        val homeTeam = currentTeams.find { it.id == match.homeTeamId }
        val awayTeam = currentTeams.find { it.id == match.awayTeamId }

        if (homeTeam != null && awayTeam != null) {
            homeTeam.played++
            awayTeam.played++
            homeTeam.goalsFor += match.homeScore
            homeTeam.goalsAgainst += match.awayScore
            awayTeam.goalsFor += match.awayScore
            awayTeam.goalsAgainst += match.homeScore

            when {
                match.homeScore > match.awayScore -> {
                    homeTeam.won++
                    homeTeam.points += 3
                    awayTeam.lost++
                }
                match.homeScore < match.awayScore -> {
                    awayTeam.won++
                    awayTeam.points += 3
                    homeTeam.lost++
                }
                else -> {
                    homeTeam.drawn++
                    homeTeam.points += 1
                    awayTeam.drawn++
                    awayTeam.points += 1
                }
            }
            _teams.value = currentTeams
        }
    }

    private fun getRandomTeamPlayer(teamId: String, teamName: String, leagueId: String): Player {
        val matched = SportsData.getInitialPlayers().filter { it.teamId == teamId }
        return if (matched.isNotEmpty()) {
            matched[Random.nextInt(matched.size)]
        } else {
            // Generate a random squad player name
            val randomFirst = listOf("Marc", "Luka", "Andres", "Gerard", "Karim", "Toni", "David", "Sandro", "Mateo", "Alex", "Inigo").random()
            val randomLast = listOf("Garcia", "Martinez", "Lopez", "Torres", "Suarez", "Gomez", "Ruiz", "Alonso", "Gutierrez", "Perez").random()
            Player(
                id = UUID.randomUUID().toString(),
                name = "$randomFirst $randomLast",
                teamId = teamId,
                teamName = teamName,
                leagueId = leagueId,
                position = listOf("FW", "MF", "DF").random(),
                goals = 0, assists = 0, yellowCards = 0, redCards = 0, rating = 7.0
            )
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Goal, red card, and yellow card alerts for EuroSports top matches."
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun triggerNotification(context: Context, title: String, text: String, matchId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_MATCH_ID", matchId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // built-in fallback safe icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                NotificationManagerCompat.from(context).notify(Random.nextInt(1000, 9999), builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "Notification permission missing on call", e)
            }
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS permission not granted; falling back to in-app alerts.")
        }
    }

    fun startManualMatch(leagueId: String, homeTeam: Team, awayTeam: Team) {
        val matchId = "LIVE_${homeTeam.id}_${awayTeam.id}"
        val existing = _matches.value.find { it.id == matchId }
        if (existing != null) {
            existing.status = "LIVE"
            existing.minute = 1
            existing.homeScore = 0
            existing.awayScore = 0
            existing.events = emptyList()
            existing.commentary = listOf("1' - Match started between ${homeTeam.name} and ${awayTeam.name}!")
        } else {
            val newMatch = LiveMatch(
                id = matchId,
                leagueId = leagueId,
                homeTeamId = homeTeam.id,
                homeTeamName = homeTeam.name,
                homeTeamBadge = homeTeam.logoChar,
                awayTeamId = awayTeam.id,
                awayTeamName = awayTeam.name,
                awayTeamBadge = awayTeam.logoChar,
                homeScore = 0,
                awayScore = 0,
                status = "LIVE",
                minute = 1,
                events = emptyList(),
                commentary = listOf("1' - Kickoff! Match kicks off at the packed stadium!")
            )
            _matches.value = listOf(newMatch) + _matches.value
        }
    }
}
