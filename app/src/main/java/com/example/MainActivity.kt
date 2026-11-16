package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.model.AppRoute
import com.example.ui.screens.CampaignScreen
import com.example.ui.screens.ContentManagerScreen
import com.example.ui.screens.EngineSettingsScreen
import com.example.ui.screens.LauncherScreen
import com.example.ui.screens.MissionHudScreen
import com.example.ui.theme.ProjectIgiTheme
import com.example.viewmodel.IgiViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: IgiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProjectIgiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val gyroSnapshot by viewModel.gyroSnapshot.collectAsStateWithLifecycle()
                val gamepadSnapshot by viewModel.gamepadSnapshot.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                val gameFolderPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        viewModel.onGameFolderPicked(uri)
                    }
                }

                val contentPackPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        viewModel.onContentPackPicked(uri)
                    }
                }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    viewModel.onVoiceMicPermissionResult(granted)
                }

                LaunchedEffect(uiState.toastMessage) {
                    val msg = uiState.toastMessage
                    if (!msg.isNullOrBlank()) {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        viewModel.consumeToast()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Launcher
                ) {
                    composable<AppRoute.Launcher> {
                        LauncherScreen(
                            uiState = uiState,
                            backgroundBitmap = viewModel.launcherBackgroundBitmap,
                            onSelectRenderer = viewModel::setRendererBackend,
                            onSelectGameFolder = { gameFolderPicker.launch(null) },
                            onPlayClicked = {
                                navController.navigate(AppRoute.Campaign)
                            },
                            onInstallContentPack = { contentPackPicker.launch(null) },
                            onDisableAllContent = viewModel::disableAllContent,
                            onOpenCampaignMenu = {
                                navController.navigate(AppRoute.Campaign)
                            }
                        )
                    }

                    composable<AppRoute.Campaign> {
                        CampaignScreen(
                            uiState = uiState,
                            missionBitmapProvider = viewModel::getMissionBitmap,
                            onBackToLauncher = { navController.popBackStack() },
                            onSelectMission = viewModel::selectMission,
                            onLaunchMission = { missionNumber ->
                                navController.navigate(AppRoute.MissionHud(missionNumber))
                            },
                            onOpenContentManager = {
                                navController.navigate(AppRoute.ContentManager)
                            },
                            onOpenEngineSettings = {
                                navController.navigate(AppRoute.EngineSettings)
                            },
                            onToggleRainAudio = viewModel::setRainAudioEnabled,
                            onUpdateCheats = viewModel::updateCheats
                        )
                    }

                    composable<AppRoute.ContentManager> {
                        ContentManagerScreen(
                            layers = uiState.contentLayers,
                            onBack = { navController.popBackStack() },
                            onToggleLayer = viewModel::toggleLayerEnabled,
                            onMoveLayer = viewModel::moveLayerPriority,
                            onRescan = viewModel::rescanContentLayers,
                            onInstallPack = { contentPackPicker.launch(null) },
                            onDisableAll = viewModel::disableAllContent
                        )
                    }

                    composable<AppRoute.EngineSettings> {
                        EngineSettingsScreen(
                            uiState = uiState,
                            gyroSnapshot = gyroSnapshot,
                            gamepadSnapshot = gamepadSnapshot,
                            onBack = { navController.popBackStack() },
                            onSelectRenderer = viewModel::setRendererBackend,
                            onSetTargetFps = viewModel::setTargetFps,
                            onToggleGyro = viewModel::setGyroEnabled,
                            onResetGyro = viewModel.hardware::resetGyroOrientation,
                            onToggleRainAudio = viewModel::setRainAudioEnabled,
                            onRequestMicPermission = {
                                val alreadyGranted = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (alreadyGranted) {
                                    viewModel.onVoiceMicPermissionResult(true)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onExportLogcat = viewModel::exportLogcat,
                            onSelectAimAssistMode = viewModel::setAimAssistMode,
                            onUpdateAimAssistConfig = viewModel::updateAimAssistConfig
                        )
                    }

                    composable<AppRoute.MissionHud> { backStackEntry ->
                        val route = backStackEntry.toRoute<AppRoute.MissionHud>()
                        MissionHudScreen(
                            missionNumber = route.missionNumber,
                            mission = viewModel.getMissionInfo(route.missionNumber),
                            missionBitmap = viewModel.getMissionBitmap(route.missionNumber),
                            uiState = uiState,
                            gyroSnapshot = gyroSnapshot,
                            gamepadSnapshot = gamepadSnapshot,
                            onSyncAudio = viewModel::syncRainAudioForScene,
                            onSelectAimAssistMode = viewModel::setAimAssistMode,
                            onExitMission = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.hardware.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.hardware.stop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (viewModel.hardware.onKeyEvent(keyCode, event, isDown = true)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (viewModel.hardware.onKeyEvent(keyCode, event, isDown = false)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (viewModel.hardware.onMotionEvent(event)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}
