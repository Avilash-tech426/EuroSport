package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.model.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AiDeskState
import com.example.viewmodel.EuroSportsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: EuroSportsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EuroSportsApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EuroSportsApp(viewModel: EuroSportsViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.selectedTab.collectAsState()
    val activeLeagueId by viewModel.selectedLeagueId.collectAsState()
    val inAppAlert by viewModel.inAppAlert.collectAsState()

    // Notification permission dispatcher for Android 13+
    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Live sports push alerts enabled! ⭐", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Simulated alerts will display inside the app.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Dismiss custom sliding alert after 5.5 seconds
    LaunchedEffect(inAppAlert) {
        if (inAppAlert != null) {
            delay(5500)
            viewModel.dismissInAppAlert()
        }
    }

    // Outer layout with custom color background (Dark graphite theme)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1113))
    ) {
        // --- 1. SLIDING IN-APP PUSH NOTIFICATION BANNER ---
        AnimatedVisibility(
            visible = inAppAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter)
                .zIndex(100f)
        ) {
            inAppAlert?.let { alert ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E2124),
                        contentColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(
                        2.dp,
                        when (alert.eventType) {
                            "GOAL" -> Color(0xFF4CD137)
                            "RED_CARD" -> Color(0xFFE84118)
                            "YELLOW_CARD" -> Color(0xFFFBC531)
                            else -> Color(0xFF00A8FF)
                        }
                    ),
                    modifier = Modifier
                        .clickable {
                            viewModel.selectTab("LIVE")
                            viewModel.dismissInAppAlert()
                        }
                        .testTag("in_app_banner_push")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (alert.eventType) {
                                        "GOAL" -> Color(0x304CD137)
                                        "RED_CARD" -> Color(0x30E84118)
                                        "YELLOW_CARD" -> Color(0x30FBC531)
                                        else -> Color(0x3000A8FF)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (alert.eventType) {
                                    "GOAL" -> "⚽"
                                    "RED_CARD" -> "🟥"
                                    "YELLOW_CARD" -> "🟨"
                                    else -> "📢"
                                },
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (alert.eventType) {
                                    "GOAL" -> "GOAL ALERT!"
                                    "RED_CARD" -> "RED CARD! DISMISSAL"
                                    "YELLOW_CARD" -> "YELLOW CARD ALERT"
                                    else -> "LIVE MATCH EVENT"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (alert.eventType) {
                                    "GOAL" -> Color(0xFF4CD137)
                                    "RED_CARD" -> Color(0xFFE84118)
                                    "YELLOW_CARD" -> Color(0xFFFBC531)
                                    else -> Color(0xFF00A8FF)
                                },
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.description,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${alert.homeTeam} vs ${alert.awayTeam} (${alert.score})",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(
                            onClick = { viewModel.dismissInAppAlert() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close banner",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. MAIN APPLICATION CONTENT SCAFFOLD ---
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⚽ EURO",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                "SPORTS",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color(0xFFEA2027) // Athletic active red
                            )
                        }
                    },
                    navigationIcon = {
                        // Flashing Live Radar Dot to represent background simulation active
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(pulseAlpha)
                                    .background(Color(0xFF4CD137), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "LIVE TICK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "EuroSports Live Simulation Running smoothly!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Sim active",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF14171A),
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF14171A),
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "LAL_SPEC",
                        onClick = { viewModel.selectTab("LAL_SPEC") },
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "La Liga Spotlight"
                            )
                        },
                        label = { Text("La Liga Spot", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFEA2027),
                            selectedTextColor = Color(0xFFEA2027),
                            indicatorColor = Color(0x1AEA2027),
                            unselectedTextColor = Color.Gray,
                            unselectedIconColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_spotlight")
                    )

                    NavigationBarItem(
                        selected = currentTab == "LIVE",
                        onClick = { viewModel.selectTab("LIVE") },
                        icon = {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Match Center"
                            )
                        },
                        label = { Text("Match Center", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFEA2027),
                            selectedTextColor = Color(0xFFEA2027),
                            indicatorColor = Color(0x1AEA2027),
                            unselectedTextColor = Color.Gray,
                            unselectedIconColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_match_center")
                    )

                    NavigationBarItem(
                        selected = currentTab == "STANDINGS",
                        onClick = { viewModel.selectTab("STANDINGS") },
                        icon = {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Standings"
                            )
                        },
                        label = { Text("Standings", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFEA2027),
                            selectedTextColor = Color(0xFFEA2027),
                            indicatorColor = Color(0x1AEA2027),
                            unselectedTextColor = Color.Gray,
                            unselectedIconColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_standings")
                    )

                    val logs by viewModel.notificationLogs.collectAsState()
                    NavigationBarItem(
                        selected = currentTab == "ALERTS",
                        onClick = { viewModel.selectTab("ALERTS") },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (logs.isNotEmpty()) {
                                        Badge(
                                            containerColor = Color(0xFFEA2027),
                                            contentColor = Color.White
                                        ) {
                                            Text(logs.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Inbox Alerts"
                                )
                            }
                        },
                        label = { Text("Inbox Logs", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFEA2027),
                            selectedTextColor = Color(0xFFEA2027),
                            indicatorColor = Color(0x1AEA2027),
                            unselectedTextColor = Color.Gray,
                            unselectedIconColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_alerts")
                    )
                }
            },
            containerColor = Color(0xFF0F1113)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    "LAL_SPEC" -> LaLigaSpotlightScreen(viewModel)
                    "LIVE" -> MatchCenterScreen(viewModel)
                    "STANDINGS" -> StandingsScreen(viewModel, activeLeagueId)
                    "ALERTS" -> NotificationLogsScreen(viewModel)
                }
            }
        }
    }
}

// ============================================
// tab 1: LA LIGA SPOTLIGHT SCREEN
// ============================================
@Composable
fun LaLigaSpotlightScreen(viewModel: EuroSportsViewModel) {
    val context = LocalContext.current
    val matches by viewModel.allMatches.collectAsState()
    val teams by viewModel.leagueTeams.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val laLigaTrivia by viewModel.leagueTrivia.collectAsState()
    val isTriviaLoading by viewModel.isTriviaLoading.collectAsState()

    // Filter La Liga specific games
    val laLigaMatches = matches.filter { it.leagueId == "LAL" }

    // Kickoff simulator state
    var homeSelectorOpen by remember { mutableStateOf(false) }
    var awaySelectorOpen by remember { mutableStateOf(false) }
    var selectedHomeTeam by remember { mutableStateOf<Team?>(null) }
    var selectedAwayTeam by remember { mutableStateOf<Team?>(null) }

    LaunchedEffect(teams) {
        if (teams.isNotEmpty()) {
            selectedHomeTeam = teams.getOrNull(0)
            selectedAwayTeam = teams.getOrNull(1)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Graphic Card ---
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFEA2027), Color(0xFFFFC312))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🇪🇸",
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "LALIGA SPOTLIGHT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Exclusive coverage & instant gameplay engine",
                                    fontSize = 11.sp,
                                    color = Color(0xCCFFFFFF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- AI Trivia Board ---
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                border = BorderStroke(1.dp, Color(0x30FFFFFF))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "AI Trivia Fact",
                                tint = Color(0xFFFFC312),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "LA LIGA TRIVIA DIRECT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC312),
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.loadLeagueTrivia("LAL") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Trivia",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isTriviaLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFEA2027),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Text(
                            text = laLigaTrivia,
                            fontSize = 13.sp,
                            color = Color.LightGray,
                            lineHeight = 18.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        // --- MANUAL MATCH SIMULATOR CONTROL ---
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                border = BorderStroke(1.dp, Color(0xFFEA2027))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔴 SIMULATE LIVE CLASICO / CUP GAME",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        "Test live goals and card alerts instantly by starting a match on-the-fly!",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home selector card
                        Card(
                            onClick = { homeSelectorOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E3236)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(selectedHomeTeam?.logoChar ?: "⚽", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedHomeTeam?.name ?: "Home",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("Home Team", fontSize = 9.sp, color = Color.Gray)
                            }
                        }

                        Text(
                            "VS",
                            fontWeight = FontWeight.Black,
                            color = Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Away selector card
                        Card(
                            onClick = { awaySelectorOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E3236)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(selectedAwayTeam?.logoChar ?: "⚽", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedAwayTeam?.name ?: "Away",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("Away Team", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val h = selectedHomeTeam
                            val a = selectedAwayTeam
                            if (h != null && a != null) {
                                if (h.id == a.id) {
                                    Toast.makeText(context, "Please select two different teams!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.startManualMatch("LAL", h, a)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA2027)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("kickoff_sim_game_button")
                    ) {
                        Text("KICKOFF LIVE MATCH simulation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- LA LIGA FIXTURES & HISTORY ---
        item {
            Text(
                "🇪🇸 LALIGA FIXTURES COVERAGE",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        if (laLigaMatches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124))
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No matches active.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(laLigaMatches) { match ->
                MatchRowItem(match, viewModel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // --- Dropping team selectors dialogs ---
    if (homeSelectorOpen) {
        SelectorDialog(
            title = "Select Home Team",
            options = teams,
            onDismiss = { homeSelectorOpen = false },
            onSelect = {
                selectedHomeTeam = it
                homeSelectorOpen = false
            }
        )
    }

    if (awaySelectorOpen) {
        SelectorDialog(
            title = "Select Away Team",
            options = teams,
            onDismiss = { awaySelectorOpen = false },
            onSelect = {
                selectedAwayTeam = it
                awaySelectorOpen = false
            }
        )
    }
}

@Composable
fun SelectorDialog(
    title: String,
    options: List<Team>,
    onDismiss: () -> Unit,
    onSelect: (Team) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
            border = BorderStroke(1.dp, Color.Gray),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options) { team ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(team) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(team.logoChar, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                team.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// tab 2: UNIFIED MATCH CENTER SCREEN
// ============================================
@Composable
fun MatchCenterScreen(viewModel: EuroSportsViewModel) {
    val liveMatches by viewModel.liveMatches.collectAsState()
    val allMatchesList by viewModel.allMatches.collectAsState()
    val activeLeagueId by viewModel.selectedLeagueId.collectAsState()

    var showOnlyLiveGames by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // League horizontally scrollable selectors
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF14171A))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SportsData.LEAGUES) { league ->
                val isSelected = activeLeagueId == league.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectLeague(league.id) },
                    label = { Text("${league.code} ${league.name}", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEA2027),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF2E3236),
                        labelColor = Color.LightGray
                    ),
                    border = null
                )
            }
        }

        // Live vs All toggle button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${SportsData.LEAGUES.find { it.id == activeLeagueId }?.name ?: "League"} Fixtures",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Only Live",
                    fontSize = 11.sp,
                    color = if (showOnlyLiveGames) Color(0xFF4CD137) else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = showOnlyLiveGames,
                    onCheckedChange = { showOnlyLiveGames = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CD137),
                        checkedTrackColor = Color(0x504CD137)
                    ),
                    modifier = Modifier.scale(0.8f).testTag("live_filter_switch")
                )
            }
        }

        // Filter games as lists
        val displayedList = if (showOnlyLiveGames) {
            allMatchesList.filter { it.status == "LIVE" && it.leagueId == activeLeagueId }
        } else {
            allMatchesList.filter { it.leagueId == activeLeagueId }
        }

        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No fixture",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (showOnlyLiveGames) "No games currently live in this league!" else "No fixtures scheduled.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Go back to the La Liga page to kickoff custom simulated matches!",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedList) { match ->
                    MatchRowItem(match, viewModel)
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}



// ============================================
// SINGLE MATCH FIXTURE TICKER ROW
// ============================================
@Composable
fun MatchRowItem(match: LiveMatch, viewModel: EuroSportsViewModel) {
    var detailPanelOpen by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1E21)),
        border = if (match.status == "LIVE") BorderStroke(1.5.dp, Color(0xFFEA2027)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { detailPanelOpen = true }
            .testTag("match_card_${match.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Live pulsing banner or kickoff time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // League Indicator
                Text(
                    text = SportsData.LEAGUES.find { it.id == match.leagueId }?.name ?: "Top 5 Leagues",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                // Match Progression Status badge
                if (match.status == "LIVE") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CD137), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE - ${match.minute}'",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4CD137)
                        )
                    }
                } else {
                    Text(
                        text = if (match.status == "FT") "FULL TIME" else "SUNDAY 20:00",
                        fontSize = 10.sp,
                        color = if (match.status == "FT") Color.LightGray else Color(0xFF00A8FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scoreboard display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(match.homeTeamBadge, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        match.homeTeamName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Scores
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    if (match.status == "UPCOMING") {
                        Text(
                            "vs",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "${match.homeScore}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (match.status == "LIVE") Color(0xFF4CD137) else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "-",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${match.awayScore}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (match.status == "LIVE") Color(0xFF4CD137) else Color.White
                        )
                    }
                }

                // Away side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        match.awayTeamName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(match.awayTeamBadge, fontSize = 20.sp)
                }
            }

            // Quick display of the last event if live
            if (match.status == "LIVE" && match.events.isNotEmpty()) {
                val lastEvent = match.events.last()
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF222428), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (lastEvent.type) {
                            "GOAL" -> "⚽"
                            "YELLOW_CARD" -> "🟨"
                            "RED_CARD" -> "🟥"
                            else -> "📢"
                        },
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${lastEvent.minute}' - ${lastEvent.playerName} (${lastEvent.teamName})",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // --- Detail Bottom-sheet Dialogue ---
    if (detailPanelOpen) {
        MatchDetailDialog(
            match = match,
            viewModel = viewModel,
            onDismiss = {
                detailPanelOpen = false
                viewModel.resetAiState()
            }
        )
    }
}

// ============================================
// LIVE FIXTURES GRAPHIC DETAILS VIEW DIALOG
// ============================================
@Composable
fun MatchDetailDialog(
    match: LiveMatch,
    viewModel: EuroSportsViewModel,
    onDismiss: () -> Unit
) {
    val aiState by viewModel.aiInsightState.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1113)),
            color = Color(0xFF0F1113)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14171A))
                        .statusBarsPadding()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "EURO MATCH DESK",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (match.status == "LIVE") {
                        Button(
                            onClick = { viewModel.triggerQuickGoalSimulation(match.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CD137)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).displayCutoutPadding()
                        ) {
                            Text("Force Goal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }

                // Scoreboard card
                Card(
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14171A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = SportsData.LEAGUES.find { it.id == match.leagueId }?.name?.uppercase() ?: "CHAMPIONSHIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA2027),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Home
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(match.homeTeamBadge, fontSize = 52.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    match.homeTeamName,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text("HOME CLUB", fontSize = 9.sp, color = Color.Gray)
                            }

                            // Score display
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (match.status == "UPCOMING") {
                                    Text(
                                        "VS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 32.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    Text(
                                        text = "${match.homeScore} - ${match.awayScore}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 44.sp,
                                        color = if (match.status == "LIVE") Color(0xFF4CD137) else Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (match.status == "LIVE") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0x304CD137), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(Color(0xFF4CD137), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "LIVE ${match.minute}'",
                                            color = Color(0xFF4CD137),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (match.status == "FT") "FULL TIME" else "SCHEDULED FIXTURE",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Away
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(match.awayTeamBadge, fontSize = 52.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    match.awayTeamName,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text("AWAY CLUB", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Dialog Tabs: Match statistics & AI RECAP
                Column(modifier = Modifier.padding(16.dp)) {
                    // --- GEMINI AI ANALYST DESK ---
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                        border = BorderStroke(1.2.dp, Color(0xFFEA2027)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "AI Expert",
                                    tint = Color(0xFFFFC312),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "GEMINI SPORTS ANALYST DESK",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            when (val state = aiState) {
                                is AiDeskState.Idle -> {
                                    Text(
                                        "Click the review button to trigger Gemini's real-time tactical breakdown, manager critique, and scoreline interpretation.",
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.fetchAiRecap(match) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA2027)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(36.dp).fillMaxWidth()
                                    ) {
                                        Text("Request AI Tactical Recap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is AiDeskState.Loading -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFFEA2027), strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Sports Desk Drafting analysis...", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                is AiDeskState.Success -> {
                                    Text(
                                        text = state.response,
                                        fontSize = 13.sp,
                                        color = Color.LightGray,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Analysis powered dynamically by Gemini 3.5 Flash.",
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // MATCH FEED TICKER TIMELINE
                    Text(
                        "LIVE EVENT FEED",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (match.events.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1E21)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Waiting for kickoff. Match starts on schedule.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Chronological events stack
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                match.events.forEach { event ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFF2E3236), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (event.type) {
                                                    "GOAL" -> "⚽"
                                                    "YELLOW_CARD" -> "🟨"
                                                    "RED_CARD" -> "🟥"
                                                    else -> "📢"
                                                },
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${event.minute}' - ${event.playerName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${event.teamName})",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                text = event.detail,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // DETAILED MINUTES TEXT COMMENTARY
                    if (match.commentary.isNotEmpty()) {
                        Text(
                            "LIVE MINUTE COMMENTARY",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                match.commentary.forEachIndexed { idx, line ->
                                    Text(
                                        text = line,
                                        color = if (idx == 0 && match.status == "LIVE") Color(0xFF4CD137) else Color.LightGray,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    if (idx < match.commentary.size - 1) {
                                        HorizontalDivider(color = Color(0x20FFFFFF), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ============================================
// tab 3: REAL-TIME LEAGUES STANDINGS & PLAYER STATS SCREEN
// ============================================
@Composable
fun StandingsScreen(viewModel: EuroSportsViewModel, activeLeagueId: String) {
    val context = LocalContext.current
    val standings by viewModel.standings.collectAsState()
    val players by viewModel.leaguePlayers.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val activeStatsCategory by viewModel.playerStatsCategory.collectAsState()

    var showScreenTab by remember { mutableStateOf("TABLE") } // TABLE or PLAYER_STATS

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle buttons for Table vs Players
        TabRow(
            selectedTabIndex = if (showScreenTab == "TABLE") 0 else 1,
            containerColor = Color(0xFF14171A),
            contentColor = Color.White
        ) {
            Tab(
                selected = showScreenTab == "TABLE",
                onClick = { showScreenTab = "TABLE" },
                text = { Text("Standing Table", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = Color(0xFFEA2027),
                unselectedContentColor = Color.Gray
            )
            Tab(
                selected = showScreenTab == "PLAYERS",
                onClick = { showScreenTab = "PLAYERS" },
                text = { Text("Player Statistics", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = Color(0xFFEA2027),
                unselectedContentColor = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (showScreenTab == "TABLE") {
            // Standing Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(Color(0xFF222428), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                Text("CLUB", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("PL", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
                Text("GD", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                Text("PTS", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                Spacer(modifier = Modifier.width(42.dp)) // star placeholder
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(standings) { index, team ->
                    val isFavorite = favorites.any { it.id == team.id }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFavorite) Color(0xFF2E1B1B) else Color(0xFF1C1E21)
                        ),
                        border = if (isFavorite) BorderStroke(1.dp, Color(0x60EA2027)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Position
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = if (index < 4) Color(0xFF4CD137) else Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.width(28.dp)
                            )

                            // Badge & Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(team.logoChar, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    team.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // PL
                            Text(
                                text = "${team.played}",
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(32.dp)
                            )

                            // GD
                            val gdSign = if (team.goalDifference > 0) "+" else ""
                            Text(
                                text = "$gdSign${team.goalDifference}",
                                fontSize = 12.sp,
                                color = if (team.goalDifference > 0) Color(0xFF4CD137) else if (team.goalDifference < 0) Color(0xFFE84118) else Color.LightGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )

                            // PTS
                            Text(
                                text = "${team.points}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(40.dp)
                            )

                            // Follow/Favorite Trigger
                            IconButton(
                                onClick = {
                                    viewModel.toggleFavoriteTeam(team.id, team.name)
                                    val act = if (isFavorite) "Removed" else "Added"
                                    Toast.makeText(context, "$act ${team.name} Alerts!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorite star",
                                    tint = if (isFavorite) Color(0xFFFFC312) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Player stats list sub-tab filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "GOALS" to "⚽ Goals",
                    "ASSISTS" to "🅰️ Assists",
                    "PASSES" to "🎯 Passes",
                    "RATING" to "⭐ Rating"
                ).forEach { (catId, label) ->
                    val isSelected = activeStatsCategory == catId
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectPlayerCategory(catId) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEA2027),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1C1E21),
                            labelColor = Color.LightGray
                        ),
                        border = null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(players) { index, player ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank
                            Text(
                                "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.width(24.dp)
                            )

                            // Name & Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    player.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    "${player.teamName} • ${player.position}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            // Stat Value Column
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF2E3236), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (activeStatsCategory) {
                                        "GOALS" -> "${player.goals} Goals"
                                        "ASSISTS" -> "${player.assists} Assists"
                                        "PASSES" -> "${player.passes} Passes"
                                        else -> "${player.rating} Rating"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFC312)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// tab 4: INBOX ALERTS SIMULATED PERSISTENT LOGS
// ============================================
@Composable
fun NotificationLogsScreen(viewModel: EuroSportsViewModel) {
    val logs by viewModel.notificationLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "ALERT INBOX LOGS",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Chronological push notifications history log.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            if (logs.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllNotificationHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear List", tint = Color(0xFFEA2027))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", color = Color(0xFFEA2027), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = "Empty Desk logo",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Your Alert Ticker Box is clean!",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Text(
                        "Goal alerts and booking cards will save here as the live simulator ticks.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                        border = BorderStroke(
                            0.5.dp,
                            when (log.eventType) {
                                "GOAL" -> Color(0xFF4CD137)
                                "RED_CARD" -> Color(0xFFE84118)
                                "YELLOW_CARD" -> Color(0xFFFBC531)
                                else -> Color(0xFF00A8FF)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (log.eventType) {
                                            "GOAL" -> "⚽ GOAL ALERTER"
                                            "RED_CARD" -> "🟥 RED CARD"
                                            "YELLOW_CARD" -> "🟨 YELLOW CARD"
                                            "MATCH_END" -> "🏁 FULL TIME"
                                            else -> "📢 EVENT"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = when (log.eventType) {
                                            "GOAL" -> Color(0xFF4CD137)
                                            "RED_CARD" -> Color(0xFFE84118)
                                            "YELLOW_CARD" -> Color(0xFFFBC531)
                                            else -> Color(0xFF00A8FF)
                                        }
                                    )
                                }

                                Text(
                                    text = "${log.minute}' Minute",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                log.description,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${log.homeTeam} vs ${log.awayTeam}",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2E3236), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        log.score,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
