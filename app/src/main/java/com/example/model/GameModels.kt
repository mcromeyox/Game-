package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Models and data structures for Battle Apex (PUBG combat & vehicle dynamics).
 */

enum class GameMode(val titleEn: String, val titleAr: String, val descriptionEn: String, val descriptionAr: String) {
    BATTLE_ROYALE(
        titleEn = "Battle Royale",
        titleAr = "بقاء رويال (50 لاعب)",
        descriptionEn = "Drop into Erangel, loot weapons, drive vehicles, survive shrinking blue zone to win Chicken Dinner!",
        descriptionAr = "اهبط في الجزيرة، اجمع الأسلحة، قد المركبات، وانجُ من الزون لتفوز بعشاء الدجاج!"
    ),
    VEHICLE_WARFARE(
        titleEn = "Vehicle Warfare",
        titleAr = "حرب المركبات والدريفت",
        descriptionEn = "High-speed pursuit, stunt ramps, nitro drifts, drive-by battles and vehicle ramming combat!",
        descriptionAr = "سباقات مطاردة سريعة، قفزات بهلوانية، دريفت نيترو وقتال ناري بالمركبات!"
    ),
    FIRING_RANGE(
        titleEn = "Training Grounds",
        titleAr = "ميدان التدريب والتجارب",
        descriptionEn = "Unlimited ammo & nitro. Test all weapons, scopes, and master drifting mechanics.",
        descriptionAr = "ذخيرة ونيترو لا نهائي. اختبر جميع الأسلحة والمناظير وتدرب على احتراف قيادة المركبات."
    )
}

enum class VehicleModel(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val maxSpeedKmh: Float,
    val acceleration: Float,
    val driftRating: Float,
    val armor: Float,
    val mass: Float,
    val iconEmoji: String,
    val defaultColor: Color
) {
    UAZ_4X4(
        id = "uaz",
        nameEn = "UAZ 4x4 Off-Road",
        nameAr = "جيب UAZ مصفح",
        maxSpeedKmh = 110f,
        acceleration = 4.2f,
        driftRating = 0.55f,
        armor = 1400f,
        mass = 2200f,
        iconEmoji = "🚙",
        defaultColor = Color(0xFF2E4057)
    ),
    DESERT_BUGGY(
        id = "buggy",
        nameEn = "Desert Stunt Buggy",
        nameAr = "باجي الصحراء البهلواني",
        maxSpeedKmh = 135f,
        acceleration = 6.8f,
        driftRating = 0.92f,
        armor = 800f,
        mass = 950f,
        iconEmoji = "🏎️",
        defaultColor = Color(0xFFD97706)
    ),
    SPORT_DACIA(
        id = "dacia",
        nameEn = "Dacia Speed Sedan",
        nameAr = "سيدان داشيا الرياضية",
        maxSpeedKmh = 150f,
        acceleration = 6.0f,
        driftRating = 0.82f,
        armor = 1000f,
        mass = 1250f,
        iconEmoji = "🚗",
        defaultColor = Color(0xFF0284C7)
    ),
    COMBAT_BIKE(
        id = "bike",
        nameEn = "Tactical Motorcycle",
        nameAr = "دراجة نارية قتالية",
        maxSpeedKmh = 165f,
        acceleration = 8.5f,
        driftRating = 0.98f,
        armor = 500f,
        mass = 400f,
        iconEmoji = "🏍️",
        defaultColor = Color(0xFFE11D48)
    )
}

enum class WeaponModel(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val damage: Float,
    val fireRateMs: Long,
    val magCapacity: Int,
    val bulletSpeed: Float,
    val recoil: Float,
    val ammoType: String,
    val isSniper: Boolean = false,
    val isMelee: Boolean = false
) {
    M416(
        id = "m416",
        nameEn = "M416 Assault Rifle",
        nameAr = "بندقية M416 الهجومية",
        damage = 41f,
        fireRateMs = 90L,
        magCapacity = 30,
        bulletSpeed = 38f,
        recoil = 0.45f,
        ammoType = "5.56mm"
    ),
    AKM(
        id = "akm",
        nameEn = "AKM Heavy Rifle",
        nameAr = "بندقية AKM المدمرة",
        damage = 48f,
        fireRateMs = 110L,
        magCapacity = 30,
        bulletSpeed = 34f,
        recoil = 0.70f,
        ammoType = "7.62mm"
    ),
    AWM(
        id = "awm",
        nameEn = "AWM Crate Sniper",
        nameAr = "قناصة AWM الصندوق الأسطورية",
        damage = 105f,
        fireRateMs = 900L,
        magCapacity = 5,
        bulletSpeed = 55f,
        recoil = 0.95f,
        ammoType = ".300 Mag",
        isSniper = true
    ),
    GROZA(
        id = "groza",
        nameEn = "Groza Bullpup AR",
        nameAr = "بندقية غروزا الصندوق",
        damage = 49f,
        fireRateMs = 80L,
        magCapacity = 30,
        bulletSpeed = 36f,
        recoil = 0.60f,
        ammoType = "7.62mm"
    ),
    PAN(
        id = "pan",
        nameEn = "Cast Iron Pan",
        nameAr = "مقلاة الدرع الأسطورية",
        damage = 80f,
        fireRateMs = 400L,
        magCapacity = 1,
        bulletSpeed = 0f,
        recoil = 0f,
        ammoType = "Melee",
        isMelee = true
    )
}

enum class ScopeType(val magnification: Float, val label: String) {
    IRON_SIGHT(1.0f, "Iron Sight"),
    RED_DOT(1.3f, "Red Dot"),
    SCOPE_4X(2.0f, "4x Scope"),
    SCOPE_8X(3.2f, "8x Sniper Scope")
}

enum class TimeOfDay(val labelEn: String, val labelAr: String) {
    DAY("Daylight", "نهار ساطع ☀️"),
    SUNSET("Golden Sunset", "غروب ذهبي 🌅"),
    NIGHT("Night Ops", "عمليات ليلية 🌙")
}

enum class AttachmentMuzzle(val label: String, val recoilReduction: Float) {
    NONE("No Muzzle", 0f),
    COMPENSATOR("Compensator (-25% Recoil)", 0.25f),
    SUPPRESSOR("Suppressor (-10% Recoil)", 0.10f),
    FLASH_HIDER("Flash Hider (-15% Recoil)", 0.15f)
}

enum class AttachmentGrip(val label: String, val recoilReduction: Float) {
    NONE("No Grip", 0f),
    VERTICAL_GRIP("Vertical Foregrip (-20% Recoil)", 0.20f),
    ANGLED_GRIP("Angled Grip (-15% Recoil)", 0.15f)
}

enum class AttachmentMag(val label: String, val extraCapacity: Int, val reloadSpeedMultiplier: Float) {
    STANDARD("Standard Mag (30 Rnds)", 0, 1.0f),
    EXTENDED("Extended Mag (40 Rnds)", 10, 1.0f),
    QUICKDRAW("Quickdraw Mag (Fast Reload)", 0, 0.70f),
    EXTENDED_QUICKDRAW("Ext. Quickdraw Mag (40 Rnds + Fast)", 10, 0.70f)
}

enum class TacticalStance {
    STANDING,
    CROUCHING,
    PRONE
}

enum class CameraPerspective {
    TPP,
    FPP
}

data class VehicleInstance(
    val id: String,
    val model: VehicleModel,
    var x: Float,
    var y: Float,
    var angleDeg: Float = 0f,
    var speed: Float = 0f, // in pixels/frame (or km/h scaled)
    var steerAngle: Float = 0f,
    var driftAngle: Float = 0f,
    var driftScore: Float = 0f,
    var health: Float = model.armor,
    val maxHealth: Float = model.armor,
    var fuel: Float = 100f,
    var nitro: Float = 100f,
    var isAccelerating: Boolean = false,
    var isReversing: Boolean = false,
    var isHandbraking: Boolean = false,
    var isBoosting: Boolean = false,
    var headlightsOn: Boolean = true,
    var driverId: String? = null,
    var isDestroyed: Boolean = false,
    var airTime: Float = 0f // For stunt jumps
)

data class PlayerState(
    var x: Float = 2000f,
    var y: Float = 2000f,
    var angleDeg: Float = 0f,
    var health: Float = 100f,
    val maxHealth: Float = 100f,
    var boostPercent: Float = 0f, // Energy drink boost
    var helmetLevel: Int = 2,
    var helmetDurability: Float = 150f,
    var vestLevel: Int = 2,
    var vestDurability: Float = 200f,
    var backpackLevel: Int = 2,
    var primaryWeapon: WeaponModel = WeaponModel.M416,
    var secondaryWeapon: WeaponModel = WeaponModel.AKM,
    var activeWeaponSlot: Int = 0, // 0 for primary, 1 for secondary, 2 for pan
    var ammoInMagPrimary: Int = 30,
    var ammoInMagSecondary: Int = 30,
    var reserve556: Int = 180,
    var reserve762: Int = 120,
    var reserve300Mag: Int = 15,
    var firstAidKits: Int = 3,
    var medKits: Int = 1,
    var energyDrinks: Int = 4,
    var painKillers: Int = 2,
    var jerryCans: Int = 1,
    var grenadesCount: Int = 3,
    var flareGunsCount: Int = 1,
    var activeVehicleId: String? = null,
    var currentScope: ScopeType = ScopeType.SCOPE_4X,
    var isScoping: Boolean = false,
    var isReloading: Boolean = false,
    var reloadProgress: Float = 0f,
    var kills: Int = 0,
    var damageDealt: Float = 0f,
    var distanceDrivenMeters: Float = 0f,
    var totalDriftScore: Float = 0f,
    var roadkills: Int = 0,
    var screenShake: Float = 0f,
    var stance: TacticalStance = TacticalStance.STANDING,
    var perspective: CameraPerspective = CameraPerspective.TPP,
    var isVaulting: Boolean = false,
    var vaultProgress: Float = 0f,
    var vaultStartX: Float = 0f,
    var vaultStartY: Float = 0f,
    var vaultTargetX: Float = 0f,
    var vaultTargetY: Float = 0f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var muzzleAttachment: AttachmentMuzzle = AttachmentMuzzle.COMPENSATOR,
    var gripAttachment: AttachmentGrip = AttachmentGrip.VERTICAL_GRIP,
    var magAttachment: AttachmentMag = AttachmentMag.EXTENDED_QUICKDRAW,
    var continuousShotCount: Int = 0,
    var recoilPitchRecovery: Float = 0f,
    var shotsFired: Int = 0,
    var shotsHit: Int = 0,
    var headshotsCount: Int = 0,
    var lastHitEvent: HitMarkerEvent? = null
)

data class BotEntity(
    val id: String,
    val name: String,
    var x: Float,
    var y: Float,
    var angleDeg: Float = 0f,
    var health: Float = 100f,
    val weapon: WeaponModel = WeaponModel.AKM,
    var inVehicleId: String? = null,
    var targetX: Float = 2000f,
    var targetY: Float = 2000f,
    var aiStateTimer: Float = 0f,
    var aiAction: String = "PATROL", // PATROL, HUNT, DRIVE, RETREAT
    var isAlive: Boolean = true
)

data class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var dropZ: Float = 0f, // Gravity drop over distance
    val damage: Float,
    val shooterId: String,
    val isFromPlayer: Boolean,
    var distanceTraveled: Float = 0f,
    val maxDistance: Float = 950f,
    val isHeadshot: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val size: Float,
    val isTireSkid: Boolean = false
)

data class Airdrop(
    var x: Float,
    var y: Float,
    var altitude: Float = 400f,
    var isLanded: Boolean = false,
    var flareTimer: Float = 60f,
    val contents: List<String> = listOf("AWM", "Lvl3 Helmet", "Lvl3 Vest", "Ghillie Suit", "Nitro Canister")
)

data class LootDrop(
    val id: String,
    val x: Float,
    val y: Float,
    val title: String,
    val type: String, // WEAPON, AMMO, MED, ARMOR, FUEL
    val iconEmoji: String
)

data class CircleZone(
    var whiteCenterX: Float = 2000f,
    var whiteCenterY: Float = 2000f,
    var whiteRadius: Float = 1400f,
    var blueCenterX: Float = 2000f,
    var blueCenterY: Float = 2000f,
    var blueRadius: Float = 1900f,
    var targetRadius: Float = 1400f,
    var phase: Int = 1,
    var timeRemainingSec: Int = 90,
    var isShrinking: Boolean = false,
    var damagePerSec: Float = 1.5f
)

data class KillFeedEntry(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val killer: String,
    val victim: String,
    val weapon: String,
    val isPlayerInvolved: Boolean,
    val isRoadkill: Boolean = false
)

data class Obstacle(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: ObstacleType,
    val label: String = ""
)

enum class ObstacleType {
    BUILDING,
    HANGAR,
    WATCHTOWER,
    WALL,
    TREE,
    ROCK,
    STUNT_RAMP, // Ramp boosts vehicle airborne!
    GAS_STATION_PAD // Refuels vehicles
}

data class GrenadeEntity(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var timer: Float = 2.4f,
    var isExploded: Boolean = false
)

data class RedZone(
    var x: Float = 2000f,
    var y: Float = 2000f,
    var radius: Float = 360f,
    var active: Boolean = false,
    var timer: Float = 30f,
    var bombTimer: Float = 1.0f
)

data class FootstepPing(
    val x: Float,
    val y: Float,
    val isVehicle: Boolean,
    var alpha: Float = 1.0f
)

enum class ServerRegion(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val flagEmoji: String,
    val basePingMs: Int,
    val tickRate: Int = 60,
    val dataCenter: String
) {
    ME_SOUTH(
        id = "me_south",
        nameEn = "Middle East (Bahrain)",
        nameAr = "الشرق الأوسط (البحرين)",
        flagEmoji = "🇸🇦",
        basePingMs = 24,
        tickRate = 60,
        dataCenter = "aws-me-south-1"
    ),
    EU_CENTRAL(
        id = "eu_central",
        nameEn = "Europe (Frankfurt)",
        nameAr = "أوروبا (فرانكفورت)",
        flagEmoji = "🇪🇺",
        basePingMs = 44,
        tickRate = 60,
        dataCenter = "aws-eu-central-1"
    ),
    US_EAST(
        id = "us_east",
        nameEn = "North America (Virginia)",
        nameAr = "أمريكا الشمالية (فرجينيا)",
        flagEmoji = "🇺🇸",
        basePingMs = 95,
        tickRate = 60,
        dataCenter = "aws-us-east-1"
    ),
    ASIA_EAST(
        id = "asia_east",
        nameEn = "Asia Pacific (Tokyo)",
        nameAr = "آسيا والمحيط الهادئ (طوكيو)",
        flagEmoji = "🇯🇵",
        basePingMs = 125,
        tickRate = 60,
        dataCenter = "aws-ap-northeast-1"
    )
}

enum class MatchmakingStage {
    IDLE,
    FINDING_PLAYERS,
    ALLOCATING_SERVER,
    SYNCING_UE5_LUMEN,
    READY
}

data class MatchmakingStatus(
    val isSearching: Boolean = false,
    val playersFound: Int = 1,
    val maxPlayers: Int = 100,
    val stage: MatchmakingStage = MatchmakingStage.IDLE,
    val estimatedTimeSec: Int = 3,
    val elapsedTimeSec: Int = 0,
    val serverRegion: ServerRegion = ServerRegion.ME_SOUTH
)

data class HitMarkerEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val isHeadshot: Boolean,
    val damage: Float,
    val rewindMs: Int,
    val serverTick: Int = 60,
    val victimName: String
)

