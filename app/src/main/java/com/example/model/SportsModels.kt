package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val id: String, // team ID or league ID
    val name: String,
    val type: String // "TEAM" or "LEAGUE"
)

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: String,
    val homeTeam: String,
    val awayTeam: String,
    val eventType: String, // "GOAL", "YELLOW_CARD", "RED_CARD", "MATCH_START", "MATCH_END"
    val minute: Int,
    val description: String,
    val score: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class League(
    val id: String,
    val name: String,
    val country: String,
    val code: String,
    val accentColor: String // Hex color for the theme accent
)

data class Team(
    val id: String,
    val name: String,
    val shortName: String,
    val leagueId: String,
    val logoChar: String, // character representation or emoji for badge
    var played: Int = 0,
    var won: Int = 0,
    var drawn: Int = 0,
    var lost: Int = 0,
    var goalsFor: Int = 0,
    var goalsAgainst: Int = 0,
    var points: Int = 0
) {
    val goalDifference: Int
        get() = goalsFor - goalsAgainst
}

data class Player(
    val id: String,
    val name: String,
    val teamId: String,
    val teamName: String,
    val leagueId: String,
    val position: String, // "FW", "MF", "DF", "GK"
    val goals: Int,
    val assists: Int,
    val yellowCards: Int,
    val redCards: Int,
    val rating: Double,
    val passes: Int = 0
)

data class MatchEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "GOAL", "YELLOW_CARD", "RED_CARD"
    val minute: Int,
    val teamId: String,
    val teamName: String,
    val playerName: String,
    val detail: String // e.g., "Assisted by Pedri", "Tactical Foul"
)

data class LiveMatch(
    val id: String,
    val leagueId: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val homeTeamBadge: String,
    val awayTeamId: String,
    val awayTeamName: String,
    val awayTeamBadge: String,
    var homeScore: Int,
    var awayScore: Int,
    var status: String, // "UPCOMING", "LIVE", "FT" (Full Time)
    var minute: Int = 0,
    var events: List<MatchEvent> = emptyList(),
    var commentary: List<String> = emptyList()
)

object SportsData {
    val LEAGUES = listOf(
        League("LAL", "La Liga", "Spain", "🇪🇸", "#EA2027"),
        League("EPL", "Premier League", "England", "🇬🇧", "#5758BB"),
        League("SERIA", "Serie A", "Italy", "🇮🇹", "#12CBC4"),
        League("BUND", "Bundesliga", "Germany", "🇩🇪", "#EE5A24"),
        League("LIGUE1", "Ligue 1", "France", "🇫🇷", "#9980FA")
    )

    fun getInitialTeams(): List<Team> {
        return listOf(
            // La Liga
            Team("FCB", "Barcelona", "FCB", "LAL", "⚽", 28, 20, 5, 3, 68, 24, 65),
            Team("RMA", "Real Madrid", "RMA", "LAL", "👑", 28, 19, 6, 3, 62, 22, 63),
            Team("ATM", "Atletico Madrid", "ATM", "LAL", "🛡️", 28, 16, 5, 7, 48, 28, 53),
            Team("RSO", "Real Sociedad", "RSO", "LAL", "⚪", 28, 14, 7, 7, 40, 26, 49),
            Team("ATH", "Athletic Club", "ATH", "LAL", "🦁", 28, 14, 6, 8, 42, 31, 48),
            Team("GIR", "Girona", "GIR", "LAL", "❤️", 28, 13, 7, 8, 38, 30, 46),

            // Premier League
            Team("MCI", "Manchester City", "MCI", "EPL", "🩵", 28, 21, 4, 3, 70, 28, 67),
            Team("ARS", "Arsenal", "ARS", "EPL", "🔴", 28, 20, 5, 3, 65, 20, 65),
            Team("LIV", "Liverpool", "LIV", "EPL", "🟥", 28, 19, 6, 3, 63, 25, 63),
            Team("AVL", "Aston Villa", "AVL", "EPL", "🦁", 28, 15, 6, 7, 52, 38, 51),
            Team("TOT", "Tottenham", "TOT", "EPL", "🐓", 28, 14, 5, 9, 49, 42, 47),

            // Serie A
            Team("INT", "Inter Milan", "INT", "SERIA", "🖤", 28, 22, 4, 2, 68, 14, 70),
            Team("MIL", "AC Milan", "MIL", "SERIA", "❤️", 28, 18, 5, 5, 53, 32, 59),
            Team("JUV", "Juventus", "JUV", "SERIA", "🦓", 28, 17, 7, 4, 45, 21, 58),
            Team("BOL", "Bologna", "BOL", "SERIA", "🔵", 28, 14, 9, 5, 41, 24, 51),
            Team("ROM", "AS Roma", "ROM", "SERIA", "🐺", 28, 14, 6, 8, 47, 35, 48),

            // Bundesliga
            Team("BYN", "Bayern Munich", "BYN", "BUND", "🔴", 28, 19, 3, 6, 78, 36, 60),
            Team("LEV", "Bayer Leverkusen", "LEV", "BUND", "🦁", 28, 22, 5, 1, 69, 19, 71),
            Team("VFB", "VfB Stuttgart", "VFB", "BUND", "⚪", 28, 18, 3, 7, 60, 31, 57),
            Team("BVB", "Dortmund", "BVB", "BUND", "🐝", 28, 15, 8, 5, 55, 32, 53),
            Team("RBL", "RB Leipzig", "RBL", "BUND", "🐂", 28, 15, 5, 8, 57, 34, 50),

            // Ligue 1
            Team("PSG", "Paris Saint-Germain", "PSG", "LIGUE1", "🗼", 28, 18, 8, 2, 62, 23, 62),
            Team("BRE", "Brest", "BRE", "LIGUE1", "⚓", 28, 14, 8, 6, 37, 20, 50),
            Team("MON", "Monaco", "MON", "LIGUE1", "👑", 28, 14, 8, 6, 45, 34, 50),
            Team("LOSC", "Lille", "LIL", "LIGUE1", "🐕", 28, 13, 10, 5, 39, 21, 49),
            Team("NCE", "Nice", "NCE", "LIGUE1", "🦅", 28, 12, 8, 8, 28, 22, 44)
        )
    }

    fun getInitialPlayers(): List<Player> {
        return listOf(
            // La Liga tops
            Player("P1", "Robert Lewandowski", "FCB", "Barcelona", "LAL", "FW", 18, 8, 2, 0, 8.1, 420),
            Player("P2", "Jude Bellingham", "RMA", "Real Madrid", "LAL", "MF", 16, 7, 4, 0, 8.4, 1150),
            Player("P3", "Vinicius Junior", "RMA", "Real Madrid", "LAL", "FW", 14, 9, 5, 1, 8.3, 580),
            Player("P4", "Antoine Griezmann", "ATM", "Atletico Madrid", "LAL", "FW", 15, 8, 1, 0, 8.2, 890),
            Player("P5", "Raphinha", "FCB", "Barcelona", "LAL", "FW", 12, 10, 3, 0, 7.9, 620),
            Player("P6", "Artem Dovbyk", "GIR", "Girona", "LAL", "FW", 20, 5, 2, 0, 7.8, 310),
            Player("P7", "Pedri", "FCB", "Barcelona", "LAL", "MF", 4, 8, 1, 0, 8.0, 1450),
            Player("P8", "Toni Kroos", "RMA", "Real Madrid", "LAL", "MF", 2, 11, 2, 0, 8.3, 1920),

            // Premier League tops
            Player("P9", "Erling Haaland", "MCI", "Manchester City", "EPL", "FW", 25, 5, 1, 0, 8.2, 280),
            Player("P10", "Mohamed Salah", "LIV", "Liverpool", "EPL", "FW", 21, 10, 2, 0, 8.3, 740),
            Player("P11", "Bukayo Saka", "ARS", "Arsenal", "EPL", "FW", 16, 12, 3, 0, 8.1, 810),
            Player("P12", "Kevin De Bruyne", "MCI", "Manchester City", "EPL", "MF", 6, 15, 1, 0, 8.5, 980),
            Player("P13", "Phil Foden", "MCI", "Manchester City", "EPL", "FW", 17, 8, 2, 0, 8.0, 1210),

            // Serie A
            Player("P14", "Lautaro Martinez", "INT", "Inter Milan", "SERIA", "FW", 23, 4, 3, 0, 8.1, 490),
            Player("P15", "Marcus Thuram", "INT", "Inter Milan", "SERIA", "FW", 12, 11, 1, 0, 7.8, 430),
            Player("P16", "Olivier Giroud", "MIL", "AC Milan", "SERIA", "FW", 14, 8, 2, 1, 7.7, 340),
            Player("P17", "Hakan Calhanoglu", "INT", "Inter Milan", "SERIA", "MF", 11, 7, 5, 0, 8.0, 1680),

            // Bundesliga
            Player("P18", "Harry Kane", "BYN", "Bayern Munich", "BUND", "FW", 31, 9, 2, 0, 8.6, 520),
            Player("P19", "Florian Wirtz", "LEV", "Bayer Leverkusen", "BUND", "MF", 11, 12, 1, 0, 8.4, 1310),
            Player("P20", "Serhou Guirassy", "BVB", "Dortmund", "BUND", "FW", 25, 3, 3, 0, 8.0, 290),
            Player("P21", "Jamal Musiala", "BYN", "Bayern Munich", "BUND", "MF", 10, 8, 0, 0, 8.2, 940),

            // Ligue 1
            Player("P22", "Kylian Mbappe", "PSG", "Paris Saint-Germain", "LIGUE1", "FW", 26, 7, 2, 0, 8.5, 680),
            Player("P23", "Jonathan David", "LOSC", "Lille", "LIGUE1", "FW", 16, 4, 1, 0, 7.6, 290),
            Player("P24", "Ousmane Dembele", "PSG", "Paris Saint-Germain", "LIGUE1", "FW", 8, 12, 2, 0, 8.1, 740),
            Player("P25", "Vitinha", "PSG", "Paris Saint-Germain", "LIGUE1", "MF", 7, 5, 4, 0, 7.8, 1540)
        )
    }

    fun getInitialSchedules(): List<LiveMatch> {
        return listOf(
            // La Liga Matches
            LiveMatch("M_FCB_RMA", "LAL", "FCB", "Barcelona", "⚽", "RMA", "Real Madrid", "👑", 2, 1, "FT", 90, listOf(
                MatchEvent(type = "GOAL", minute = 12, teamId = "RMA", teamName = "Real Madrid", playerName = "Jude Bellingham", detail = "Powerful header from a corner"),
                MatchEvent(type = "GOAL", minute = 43, teamId = "FCB", teamName = "Barcelona", playerName = "Robert Lewandowski", detail = "Clinical volley assisted by Pedri"),
                MatchEvent(type = "YELLOW_CARD", minute = 55, teamId = "RMA", teamName = "Real Madrid", playerName = "Vinicius Junior", detail = "Argument with referee"),
                MatchEvent(type = "GOAL", minute = 88, teamId = "FCB", teamName = "Barcelona", playerName = "Raphinha", detail = "Stunning curler into top-left corner")
            ), listOf(
                "90' - Full time whistle blows! Barcelona takes El Clasico 2-1!",
                "88' - GOAL! Raphinha curls a beautiful shot into the corner!",
                "55' - Yellow card given to Vinicius Jr for protesting.",
                "45' - Halftime. Honors even at 1-1.",
                "43' - GOAL! Lewandowski equalises with an acrobatic volley!",
                "12' - GOAL! Jude Bellingham puts Real Madrid ahead early on!"
            )),
            LiveMatch("M_ATM_ATH", "LAL", "ATM", "Atletico Madrid", "🛡️", "ATH", "Athletic Club", "🦁", 1, 0, "FT", 90, listOf(
                MatchEvent(type = "GOAL", minute = 67, teamId = "ATM", teamName = "Atletico Madrid", playerName = "Antoine Griezmann", detail = "Low drive into the bottom corner")
            ), listOf(
                "90' - Full time. Atletico secures a trademark 1-0 win.",
                "67' - GOAL! Antoine Griezmann blasts it low past the keeper!"
            )),
            // Live Match (Simulating live activity)
            LiveMatch("M_GIR_RSO", "LAL", "GIR", "Girona", "❤️", "RSO", "Real Sociedad", "⚪", 0, 0, "LIVE", 34, listOf(
                MatchEvent(type = "YELLOW_CARD", minute = 22, teamId = "RSO", teamName = "Real Sociedad", playerName = "Yago", detail = "Tactical foul in midfield")
            ), listOf(
                "34' - Girona pressing hard but the Sociedad defense stands robust.",
                "22' - Yellow Card shown to Sociedad's defender for stopping a counter-attack."
            )),

            // Premier League Matches
            LiveMatch("M_MCI_LIV", "EPL", "MCI", "Manchester City", "🩵", "LIV", "Liverpool", "🟥", 2, 2, "FT", 90),
            LiveMatch("M_ARS_TOT", "EPL", "ARS", "Arsenal", "🔴", "TOT", "Tottenham", "🐓", 3, 1, "FT", 90),
            LiveMatch("M_AVL_MCI", "EPL", "AVL", "Aston Villa", "🦁", "MCI", "Manchester City", "🩵", 0, 0, "UPCOMING", 0),

            // Serie A
            LiveMatch("M_INT_MIL", "SERIA", "INT", "Inter Milan", "🖤", "MIL", "AC Milan", "❤️", 1, 1, "LIVE", 72, listOf(
                MatchEvent(type = "GOAL", minute = 34, teamId = "MIL", teamName = "AC Milan", playerName = "Olivier Giroud", detail = "Close range tap-in"),
                MatchEvent(type = "GOAL", minute = 58, teamId = "INT", teamName = "Inter Milan", playerName = "Lautaro Martinez", detail = "Brilliant swivel and finish")
            ), listOf(
                "72' - Fierce battle in the Milan Derby. Both teams going for the winner.",
                "58' - GOAL! Lautaro Martinez equalises with a phenomenal strike!",
                "34' - GOAL! Olivier Giroud scores off a neat cross!"
            )),

            // Bundesliga
            LiveMatch("M_BYN_BVB", "BUND", "BYN", "Bayern Munich", "🔴", "BVB", "Dortmund", "🐝", 4, 1, "FT", 90),
            LiveMatch("M_LEV_RBL", "BUND", "LEV", "Bayer Leverkusen", "🦁", "RBL", "RB Leipzig", "🐂", 0, 0, "UPCOMING", 0),

            // Ligue 1
            LiveMatch("M_PSG_MON", "LIGUE1", "PSG", "Paris Saint-Germain", "🗼", "MON", "Monaco", "👑", 2, 0, "LIVE", 15, listOf(
                MatchEvent(type = "GOAL", minute = 8, teamId = "PSG", teamName = "Paris Saint-Germain", playerName = "Kylian Mbappe", detail = "Pulls past defender and slots it home")
            ), listOf(
                "15' - PSG in total control at the Parc des Princes.",
                "8' - GOAL! Kylian Mbappe shows electric pace to open the scoring! 1-0."
            ))
        )
    }
}
