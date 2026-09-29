package com.example.oniongrade

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.oniongrade.ai.OnionClassifier
import com.example.oniongrade.data.database.AnalysisEntity
import com.example.oniongrade.data.model.BatchInfo
import com.example.oniongrade.data.model.DetectionResult
import com.example.oniongrade.data.model.UserSession
import com.example.oniongrade.data.repository.AnalysisRepository
import com.example.oniongrade.data.repository.AuthRepository
import com.example.oniongrade.ui.analysis.AnalysisProcessingScreen
import com.example.oniongrade.ui.capture.ImageCaptureScreen
import com.example.oniongrade.ui.dashboard.DashboardScreen
import com.example.oniongrade.ui.history.HistoryScreen
import com.example.oniongrade.ui.login.LoginScreen
import com.example.oniongrade.ui.profile.ProfileScreen
import com.example.oniongrade.ui.reports.ReportsScreen
import com.example.oniongrade.ui.results.AnalysisResultScreen
import com.example.oniongrade.ui.theme.NahidaGradeTheme
import com.example.oniongrade.ui.theme.OnionPurple
import com.example.oniongrade.utils.ImageUtils
import com.example.oniongrade.utils.PdfGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class Screen {
    LOGIN,
    DASHBOARD,
    CAPTURE,
    PROCESSING,
    RESULT,
    HISTORY,
    REPORTS,
    PROFILE
}

enum class BottomTab(val route: Screen, val label: String, val icon: ImageVector) {
    HOME(Screen.DASHBOARD, "Home", Icons.Default.Home),
    ANALYZE(Screen.CAPTURE, "Analyze", Icons.Default.CameraAlt),
    HISTORY(Screen.HISTORY, "History", Icons.Default.History),
    REPORTS(Screen.REPORTS, "Reports", Icons.Default.Description),
    PROFILE(Screen.PROFILE, "Profile", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NahidaGradeTheme {
                NahidaGradeApp()
            }
        }
    }
}

@Composable
fun NahidaGradeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authRepository = remember { AuthRepository(context) }
    val analysisRepository = remember { AnalysisRepository(context) }

    val userSession by authRepository.userSession.collectAsState(initial = UserSession())

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }

    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedBatchInfo by remember { mutableStateOf(BatchInfo()) }

    var activeResultRecord by remember { mutableStateOf<AnalysisEntity?>(null) }
    var activeResultBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var analysisJob by remember { mutableStateOf<Job?>(null) }

    // If not logged in, force Login screen
    if (!userSession.isLoggedIn) {
        LoginScreen(
            authRepository = authRepository,
            onLoginSuccess = {
                currentScreen = Screen.DASHBOARD
                currentTab = BottomTab.HOME
            }
        )
        return
    }

    // Back button behavior
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        if (currentScreen == Screen.PROCESSING) {
            analysisJob?.cancel()
            currentScreen = Screen.CAPTURE
        } else {
            currentScreen = Screen.DASHBOARD
            currentTab = BottomTab.HOME
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.LOGIN && currentScreen != Screen.PROCESSING) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = OnionPurple
                ) {
                    BottomTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab && (
                                currentScreen == tab.route ||
                                        (tab == BottomTab.ANALYZE && (currentScreen == Screen.CAPTURE || currentScreen == Screen.RESULT))
                                )
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                currentTab = tab
                                currentScreen = tab.route
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnionPurple,
                                selectedTextColor = OnionPurple,
                                indicatorColor = Color(0xFFEEECFF)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    userSession = userSession,
                    analysisRepository = analysisRepository,
                    onNavigateToCapture = {
                        currentScreen = Screen.CAPTURE
                        currentTab = BottomTab.ANALYZE
                    },
                    onNavigateToHistory = {
                        currentScreen = Screen.HISTORY
                        currentTab = BottomTab.HISTORY
                    },
                    onNavigateToReports = {
                        currentScreen = Screen.REPORTS
                        currentTab = BottomTab.REPORTS
                    },
                    onNavigateToProfile = {
                        currentScreen = Screen.PROFILE
                        currentTab = BottomTab.PROFILE
                    },
                    onSelectRecord = { record ->
                        activeResultRecord = record
                        activeResultBitmap = if (record.imagePath.isNotBlank()) {
                            BitmapFactory.decodeFile(record.imagePath)
                        } else null
                        currentScreen = Screen.RESULT
                    }
                )

                Screen.CAPTURE -> ImageCaptureScreen(
                    userRole = userSession.role,
                    onStartAnalysis = { bitmap, batchInfo ->
                        selectedImageBitmap = bitmap
                        selectedBatchInfo = batchInfo
                        currentScreen = Screen.PROCESSING

                        analysisJob = scope.launch {
                            try {
                                val result: DetectionResult = OnionClassifier.classify(context, bitmap)
                                val annotatedBitmap = ImageUtils.drawBoundingBoxes(bitmap, result.boxes)

                                val savedRecord = analysisRepository.saveAnalysisRecord(
                                    session = userSession,
                                    batchInfo = batchInfo,
                                    result = result,
                                    annotatedBitmap = annotatedBitmap
                                )

                                activeResultRecord = savedRecord
                                activeResultBitmap = annotatedBitmap
                                currentScreen = Screen.RESULT
                            } catch (e: Exception) {
                                e.printStackTrace()
                                currentScreen = Screen.CAPTURE
                            }
                        }
                    }
                )

                Screen.PROCESSING -> AnalysisProcessingScreen(
                    onCancel = {
                        analysisJob?.cancel()
                        currentScreen = Screen.CAPTURE
                    }
                )

                Screen.RESULT -> {
                    if (activeResultRecord != null) {
                        AnalysisResultScreen(
                            record = activeResultRecord!!,
                            bitmap = activeResultBitmap,
                            onAnalyzeAnother = {
                                currentScreen = Screen.CAPTURE
                                currentTab = BottomTab.ANALYZE
                            },
                            onGeneratePdf = {
                                scope.launch {
                                    val file = analysisRepository.generatePdf(activeResultRecord!!, activeResultBitmap)
                                    if (file != null) {
                                        PdfGenerator.openPdf(context, file)
                                    }
                                }
                            },
                            onShareReport = {
                                scope.launch {
                                    val file = analysisRepository.generatePdf(activeResultRecord!!, activeResultBitmap)
                                    if (file != null) {
                                        PdfGenerator.sharePdf(context, file)
                                    }
                                }
                            }
                        )
                    } else {
                        currentScreen = Screen.DASHBOARD
                    }
                }

                Screen.HISTORY -> HistoryScreen(
                    analysisRepository = analysisRepository,
                    onSelectRecord = { record ->
                        activeResultRecord = record
                        activeResultBitmap = if (record.imagePath.isNotBlank()) {
                            BitmapFactory.decodeFile(record.imagePath)
                        } else null
                        currentScreen = Screen.RESULT
                    }
                )

                Screen.REPORTS -> ReportsScreen(
                    analysisRepository = analysisRepository,
                    onSelectRecord = { record ->
                        activeResultRecord = record
                        activeResultBitmap = if (record.imagePath.isNotBlank()) {
                            BitmapFactory.decodeFile(record.imagePath)
                        } else null
                        currentScreen = Screen.RESULT
                    }
                )

                Screen.PROFILE -> ProfileScreen(
                    userSession = userSession,
                    authRepository = authRepository,
                    onLogout = {
                        currentScreen = Screen.LOGIN
                    }
                )

                Screen.LOGIN -> LoginScreen(
                    authRepository = authRepository,
                    onLoginSuccess = {
                        currentScreen = Screen.DASHBOARD
                        currentTab = BottomTab.HOME
                    }
                )
            }
        }
    }
}
