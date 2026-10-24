package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameSoundEngine
import com.example.engine.GamePhysicsEngine
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameUiState(
    val currentMode: GameMode = GameMode.BATTLE_ROYALE,
    val isMatchActive: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val aliveCount: Int = 50,
    val player: PlayerState = PlayerState(),
    val killFeed: List<KillFeedEntry> = emptyList(),
    val isArabic: Boolean = true,
    val graphicsQuality: String = "Ultra 60FPS",
    val vehicleControlScheme: String = "PEDALS", // JOYSTICK or PEDALS
    val soundEnabled: Boolean = true,
    val selectedVehicleModel: VehicleModel = VehicleModel.UAZ_4X4,
    val selectedWeaponModel: WeaponModel = WeaponModel.M416,
    // Dedicated Server & Netcode Telemetry
    val weather: TimeOfDay = TimeOfDay.DAY,
    val serverTickRate: Int = 60,
    val pingMs: Int = 24,
    val packetLossPercent: Float = 0.0f,
    val lastRewindMs: Int = 24,
    val isNetcodeHudVisible: Boolean = true,
    val serverRegion: ServerRegion = ServerRegion.ME_SOUTH,
    val matchmakingStatus: MatchmakingStatus = MatchmakingStatus(),
    val isMatchmakingDialogVisible: Boolean = false,
    // Career overall stats
    val totalMatches: Int = 14,
    val totalWins: Int = 6,
    val totalKills: Int = 58,
    val totalRoadkills: Int = 19,
    val topDriftScore: Int = 4280,
    val totalDistanceKm: Float = 64.5f
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val soundEngine = GameSoundEngine(application.applicationContext)

    val physicsEngine = GamePhysicsEngine(soundEngine) { entry ->
        _uiState.update { state ->
            val updatedFeed = (listOf(entry) + state.killFeed).take(6)
            state.copy(killFeed = updatedFeed)
        }
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Real-time control inputs
    var inputSteer = 0f
    var inputAccel = 0f
    var inputBrake = false
    var inputHandbrake = false
    var inputNitro = false
    var inputShooting = false

    private var gameLoopJob: Job? = null

    fun startMatch(mode: GameMode = _uiState.value.currentMode) {
        gameLoopJob?.cancel()
        physicsEngine.initializeMatch(mode)

        val freshPlayer = PlayerState(
            x = 2000f,
            y = 2000f,
            health = 100f,
            helmetLevel = 2,
            vestLevel = 2,
            primaryWeapon = _uiState.value.selectedWeaponModel,
            secondaryWeapon = WeaponModel.AKM
        )

        _uiState.update {
            it.copy(
                currentMode = mode,
                isMatchActive = true,
                isGameOver = false,
                isVictory = false,
                player = freshPlayer,
                killFeed = emptyList(),
                aliveCount = if (mode == GameMode.BATTLE_ROYALE) 26 else if (mode == GameMode.VEHICLE_WARFARE) 13 else 5
            )
        }

        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceIn(0.005f, 0.05f)
                lastTime = now

                val player = _uiState.value.player
                if (player.health <= 0f) {
                    onPlayerDied()
                    break
                }

                val livingBots = physicsEngine.bots.count { it.isAlive }
                val totalAlive = livingBots + 1

                if (livingBots == 0 && _uiState.value.currentMode != GameMode.FIRING_RANGE) {
                    onPlayerVictory()
                    break
                }

                physicsEngine.update(
                    dt = dt,
                    player = player,
                    steerInput = inputSteer,
                    accelInput = inputAccel,
                    brakeInput = inputBrake,
                    handbrakeInput = inputHandbrake,
                    nitroInput = inputNitro,
                    shootInput = inputShooting,
                    mode = _uiState.value.currentMode
                )

                _uiState.update {
                    it.copy(
                        player = player,
                        aliveCount = totalAlive
                    )
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private fun onPlayerDied() {
        soundEngine.playExplosion()
        _uiState.update {
            it.copy(
                isGameOver = true,
                isVictory = false,
                totalMatches = it.totalMatches + 1,
                totalKills = it.totalKills + it.player.kills,
                totalRoadkills = it.totalRoadkills + it.player.roadkills,
                totalDistanceKm = it.totalDistanceKm + (it.player.distanceDrivenMeters / 1000f),
                topDriftScore = maxOf(it.topDriftScore, it.player.totalDriftScore.toInt())
            )
        }
    }

    private fun onPlayerVictory() {
        soundEngine.playVictory()
        _uiState.update {
            it.copy(
                isGameOver = true,
                isVictory = true,
                totalMatches = it.totalMatches + 1,
                totalWins = it.totalWins + 1,
                totalKills = it.totalKills + it.player.kills,
                totalRoadkills = it.totalRoadkills + it.player.roadkills,
                totalDistanceKm = it.totalDistanceKm + (it.player.distanceDrivenMeters / 1000f),
                topDriftScore = maxOf(it.topDriftScore, it.player.totalDriftScore.toInt())
            )
        }
    }

    fun exitMatch() {
        gameLoopJob?.cancel()
        _uiState.update { it.copy(isMatchActive = false) }
    }

    fun toggleVehicleEntry() {
        physicsEngine.toggleVehicleEntry(_uiState.value.player)
    }

    fun honkHorn() {
        physicsEngine.honkVehicleHorn(_uiState.value.player)
    }

    fun triggerReload() {
        physicsEngine.startPlayerReload(_uiState.value.player)
    }

    fun throwGrenade() {
        if (physicsEngine.throwGrenade(_uiState.value.player)) {
            _uiState.update { it.copy(player = it.player) }
        }
    }

    fun fireFlareGun() {
        if (physicsEngine.fireFlareGun(_uiState.value.player)) {
            _uiState.update { it.copy(player = it.player) }
        }
    }

    fun toggleCrouch() {
        physicsEngine.toggleCrouch(_uiState.value.player)
        _uiState.update { it.copy(player = it.player) }
    }

    fun toggleProne() {
        physicsEngine.toggleProne(_uiState.value.player)
        _uiState.update { it.copy(player = it.player) }
    }

    fun togglePerspective() {
        physicsEngine.togglePerspective(_uiState.value.player)
        _uiState.update { it.copy(player = it.player) }
    }

    fun triggerJumpOrVault() {
        physicsEngine.tryVaultOrMantle(_uiState.value.player)
        _uiState.update { it.copy(player = it.player) }
    }

    fun switchWeapon(slot: Int) {
        _uiState.update {
            val p = it.player
            p.activeWeaponSlot = slot
            p.isReloading = false
            it.copy(player = p)
        }
        soundEngine.playReload()
    }

    fun useFirstAid() {
        val p = _uiState.value.player
        if (p.firstAidKits > 0 && p.health < 75f) {
            p.firstAidKits--
            p.health = 75f
            soundEngine.playHeal()
            _uiState.update { it.copy(player = p) }
        }
    }

    fun useMedKit() {
        val p = _uiState.value.player
        if (p.medKits > 0 && p.health < 100f) {
            p.medKits--
            p.health = 100f
            soundEngine.playHeal()
            _uiState.update { it.copy(player = p) }
        }
    }

    fun useEnergyDrink() {
        val p = _uiState.value.player
        if (p.energyDrinks > 0) {
            p.energyDrinks--
            p.boostPercent = (p.boostPercent + 40f).coerceAtMost(100f)
            soundEngine.playHeal()
            _uiState.update { it.copy(player = p) }
        }
    }

    fun toggleScope() {
        val p = _uiState.value.player
        p.isScoping = !p.isScoping
        _uiState.update { it.copy(player = p) }
    }

    fun cycleScopeType() {
        val p = _uiState.value.player
        p.currentScope = when (p.currentScope) {
            ScopeType.IRON_SIGHT -> ScopeType.RED_DOT
            ScopeType.RED_DOT -> ScopeType.SCOPE_4X
            ScopeType.SCOPE_4X -> ScopeType.SCOPE_8X
            ScopeType.SCOPE_8X -> ScopeType.RED_DOT
        }
        _uiState.update { it.copy(player = p) }
    }

    fun toggleLanguage() {
        _uiState.update { it.copy(isArabic = !it.isArabic) }
    }

    fun toggleSound() {
        val newState = !_uiState.value.soundEnabled
        soundEngine.setMuted(!newState)
        _uiState.update { it.copy(soundEnabled = newState) }
    }

    fun setVehicleControlScheme(scheme: String) {
        _uiState.update { it.copy(vehicleControlScheme = scheme) }
    }

    fun selectVehicleModel(model: VehicleModel) {
        _uiState.update { it.copy(selectedVehicleModel = model) }
    }

    fun selectWeaponModel(model: WeaponModel) {
        _uiState.update { it.copy(selectedWeaponModel = model) }
    }

    fun toggleWeather() {
        _uiState.update {
            val nextWeather = when (it.weather) {
                TimeOfDay.DAY -> TimeOfDay.SUNSET
                TimeOfDay.SUNSET -> TimeOfDay.NIGHT
                TimeOfDay.NIGHT -> TimeOfDay.DAY
            }
            it.copy(weather = nextWeather)
        }
    }

    fun toggleNetcodeHud() {
        _uiState.update { it.copy(isNetcodeHudVisible = !it.isNetcodeHudVisible) }
    }

    fun equipLoadout(
        weapon: WeaponModel,
        scope: ScopeType,
        muzzle: AttachmentMuzzle,
        grip: AttachmentGrip,
        mag: AttachmentMag
    ) {
        _uiState.update { state ->
            val p = state.player
            p.primaryWeapon = weapon
            p.currentScope = scope
            p.muzzleAttachment = muzzle
            p.gripAttachment = grip
            p.magAttachment = mag
            p.ammoInMagPrimary = weapon.magCapacity + mag.extraCapacity
            state.copy(
                selectedWeaponModel = weapon,
                player = p
            )
        }
    }

    private var matchmakingJob: Job? = null

    fun setServerRegion(region: ServerRegion) {
        _uiState.update {
            it.copy(
                serverRegion = region,
                pingMs = region.basePingMs,
                lastRewindMs = region.basePingMs,
                matchmakingStatus = it.matchmakingStatus.copy(serverRegion = region)
            )
        }
    }

    fun openMatchmakingDialog(open: Boolean) {
        _uiState.update { it.copy(isMatchmakingDialogVisible = open) }
    }

    fun startMatchmaking(mode: GameMode, onMatchReady: () -> Unit) {
        matchmakingJob?.cancel()
        val targetPlayers = if (mode == GameMode.BATTLE_ROYALE) 100 else if (mode == GameMode.VEHICLE_WARFARE) 50 else 10
        _uiState.update {
            it.copy(
                isMatchmakingDialogVisible = true,
                currentMode = mode,
                matchmakingStatus = MatchmakingStatus(
                    isSearching = true,
                    playersFound = 1,
                    maxPlayers = targetPlayers,
                    stage = MatchmakingStage.FINDING_PLAYERS,
                    serverRegion = it.serverRegion
                )
            )
        }

        matchmakingJob = viewModelScope.launch {
            val stepCounts = if (mode == GameMode.BATTLE_ROYALE) listOf(24, 62, 88, 100) else listOf(12, 28, 41, 50)
            for (count in stepCounts) {
                delay(350)
                _uiState.update {
                    it.copy(matchmakingStatus = it.matchmakingStatus.copy(playersFound = count))
                }
            }

            // Dedicated Server Spinup
            _uiState.update {
                it.copy(matchmakingStatus = it.matchmakingStatus.copy(stage = MatchmakingStage.ALLOCATING_SERVER))
            }
            delay(450)

            // UE5 Lumen & World Partition Streaming
            _uiState.update {
                it.copy(matchmakingStatus = it.matchmakingStatus.copy(stage = MatchmakingStage.SYNCING_UE5_LUMEN))
            }
            delay(450)

            // Ready
            _uiState.update {
                it.copy(matchmakingStatus = it.matchmakingStatus.copy(stage = MatchmakingStage.READY))
            }
            delay(250)

            _uiState.update { it.copy(isMatchmakingDialogVisible = false) }
            startMatch(mode)
            onMatchReady()
        }
    }

    fun cancelMatchmaking() {
        matchmakingJob?.cancel()
        _uiState.update {
            it.copy(
                isMatchmakingDialogVisible = false,
                matchmakingStatus = MatchmakingStatus(
                    isSearching = false,
                    stage = MatchmakingStage.IDLE,
                    serverRegion = it.serverRegion
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        matchmakingJob?.cancel()
    }
}
