package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.SportsDatabase
import com.example.db.SportsRepository
import com.example.model.Favorite
import com.example.model.LiveMatch
import com.example.model.NotificationLog
import com.example.model.Player
import com.example.model.SportsData
import com.example.model.Team
import com.example.service.GeminiClient
import com.example.service.MatchSimulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EuroSportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SportsRepository

    // Active screen navigation/tab selection
    private val _selectedTab = MutableStateFlow("LAL_SPEC") // LAL spotlight, LIVE, LEAGUES, PUSH_ALERTS
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    // Active selected league (starts as La Liga "LAL")
    private val _selectedLeagueId = MutableStateFlow("LAL")
    val selectedLeagueId: StateFlow<String> = _selectedLeagueId.asStateFlow()

    // Standings calculation
    val standings: StateFlow<List<Team>> = combine(
        MatchSimulator.teams,
        _selectedLeagueId
    ) { allTeams, leagueId ->
        allTeams.filter { it.leagueId == leagueId }
            .sortedWith(compareByDescending<Team> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenBy { it.name }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available teams inside selected league
    val leagueTeams: StateFlow<List<Team>> = combine(
        MatchSimulator.teams,
        _selectedLeagueId
    ) { allTeams, leagueId ->
        allTeams.filter { it.leagueId == leagueId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered matches list for active league (including Schedules or Live Matches)
    val filteredMatches: StateFlow<List<LiveMatch>> = combine(
        MatchSimulator.matches,
        _selectedLeagueId
    ) { allMatches, leagueId ->
        allMatches.filter { it.leagueId == leagueId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Live Matches across all leagues
    val liveMatches: StateFlow<List<LiveMatch>> = MatchSimulator.matches.combine(MutableStateFlow(true)) { allMatches, _ ->
        allMatches.filter { it.status == "LIVE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Master schedule lists
    val allMatches: StateFlow<List<LiveMatch>> = MatchSimulator.matches

    // Filtered players stats
    private val _playerStatsCategory = MutableStateFlow("GOALS") // GOALS, ASSISTS, RATING, PASSES
    val playerStatsCategory: StateFlow<String> = _playerStatsCategory.asStateFlow()

    val leaguePlayers: StateFlow<List<Player>> = combine(
        _selectedLeagueId,
        _playerStatsCategory
    ) { leagueId, category ->
        val players = SportsData.getInitialPlayers().filter { it.leagueId == leagueId }
        when (category) {
            "GOALS" -> players.sortedByDescending { it.goals }
            "ASSISTS" -> players.sortedByDescending { it.assists }
            "PASSES" -> players.sortedByDescending { it.passes }
            else -> players.sortedByDescending { it.rating }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites and notification logs from Room
    val favorites: StateFlow<List<Favorite>>
    val notificationLogs: StateFlow<List<NotificationLog>>
    val inAppAlert: StateFlow<NotificationLog?> = MatchSimulator.inAppNotification

    // AI Sports Analyst content state
    private val _aiInsightState = MutableStateFlow<AiDeskState>(AiDeskState.Idle)
    val aiInsightState: StateFlow<AiDeskState> = _aiInsightState.asStateFlow()

    // League trivia fact state
    private val _leagueTrivia = MutableStateFlow<String>("Select a league to discover historic football trivia.")
    val leagueTrivia: StateFlow<String> = _leagueTrivia.asStateFlow()

    private val _isTriviaLoading = MutableStateFlow(false)
    val isTriviaLoading: StateFlow<Boolean> = _isTriviaLoading.asStateFlow()

    init {
        val database = SportsDatabase.getDatabase(application)
        repository = SportsRepository(database)
        favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        notificationLogs = repository.notificationLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        MatchSimulator.init(application)
        loadLeagueTrivia("LAL") // Load La Liga trivia by default
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun selectLeague(leagueId: String) {
        _selectedLeagueId.value = leagueId
        loadLeagueTrivia(leagueId)
    }

    fun selectPlayerCategory(category: String) {
        _playerStatsCategory.value = category
    }

    fun toggleFavoriteTeam(teamId: String, teamName: String) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.id == teamId }
            if (isFav) {
                repository.removeFavorite(teamId)
            } else {
                repository.addFavorite(teamId, teamName, "TEAM")
            }
        }
    }

    fun loadLeagueTrivia(leagueId: String) {
        val name = SportsData.LEAGUES.find { it.id == leagueId }?.name ?: "La Liga"
        viewModelScope.launch {
            _isTriviaLoading.value = true
            val trivia = GeminiClient.getLeagueTrivia(name)
            _leagueTrivia.value = trivia
            _isTriviaLoading.value = false
        }
    }

    fun fetchAiRecap(match: LiveMatch) {
        viewModelScope.launch {
            _aiInsightState.value = AiDeskState.Loading
            val leagueName = SportsData.LEAGUES.find { it.id == match.leagueId }?.name ?: "Top League"
            val eventsSummary = if (match.events.isEmpty()) {
                "Intense battle on the midfield with no major cards or goal actions."
            } else {
                match.events.joinToString { "${it.minute}' ${it.type} by ${it.playerName} (${it.detail})" }
            }

            val analysis = GeminiClient.getMatchAnalysis(
                homeTeam = match.homeTeamName,
                awayTeam = match.awayTeamName,
                homeScore = match.homeScore,
                awayScore = match.awayScore,
                league = leagueName,
                events = eventsSummary
            )
            _aiInsightState.value = AiDeskState.Success(analysis)
        }
    }

    fun resetAiState() {
        _aiInsightState.value = AiDeskState.Idle
    }

    fun triggerQuickGoalSimulation(matchId: String) {
        // Find match and force a simulation check instantly
        val matchesList = MatchSimulator.matches.value
        val target = matchesList.find { it.id == matchId }
        if (target != null && target.status == "LIVE") {
            // Trigger rapid simulation of this match for instant gratification
            viewModelScope.launch {
                // Instantly force a goal
                target.homeScore++
                val newEvent = com.example.model.MatchEvent(
                    type = "GOAL",
                    minute = target.minute + 1,
                    teamId = target.homeTeamId,
                    teamName = target.homeTeamName,
                    playerName = "Sim Scorer",
                    detail = "Spectacular shot triggered by EuroSports fan center!"
                )
                target.events = target.events + newEvent
                MatchSimulator.dismissInAppNotification() // refresh in-app state flow
            }
        }
    }

    fun startManualMatch(leagueId: String, home: Team, away: Team) {
        MatchSimulator.startManualMatch(leagueId, home, away)
        _selectedTab.value = "LIVE" // Hop over to match center to view the game!
    }

    fun clearAllNotificationHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun dismissInAppAlert() {
        MatchSimulator.dismissInAppNotification()
    }
}

sealed interface AiDeskState {
    object Idle : AiDeskState
    object Loading : AiDeskState
    data class Success(val response: String) : AiDeskState
}
