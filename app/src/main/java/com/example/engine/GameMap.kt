package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.model.*
import kotlin.random.Random

/**
 * World and Map definition for Battle Apex.
 * Generates Erangel-inspired battleground with paved drifting circuits,
 * military compound, Pochinki town, gas stations, and stunt ramps.
 */
object GameMap {
    const val WORLD_WIDTH = 4000f
    const val WORLD_HEIGHT = 4000f

    // Major Road Segments (asphalt for high-speed drifting)
    data class RoadSegment(val start: Offset, val end: Offset, val width: Float = 90f)

    val roads: List<RoadSegment> = listOf(
        // Outer Highway Ring
        RoadSegment(Offset(500f, 500f), Offset(3500f, 500f)),
        RoadSegment(Offset(3500f, 500f), Offset(3500f, 3500f)),
        RoadSegment(Offset(3500f, 3500f), Offset(500f, 3500f)),
        RoadSegment(Offset(500f, 3500f), Offset(500f, 500f)),

        // Cross Highways through Center
        RoadSegment(Offset(500f, 2000f), Offset(3500f, 2000f)),
        RoadSegment(Offset(2000f, 500f), Offset(2000f, 3500f)),

        // Diagonal Highway to Military Island
        RoadSegment(Offset(1000f, 1000f), Offset(2000f, 2000f)),
        RoadSegment(Offset(2000f, 2000f), Offset(3000f, 3000f)),
        RoadSegment(Offset(3000f, 1000f), Offset(2000f, 2000f)),
        RoadSegment(Offset(2000f, 2000f), Offset(1000f, 3000f))
    )

    // Compounds and Obstacles
    val obstacles: List<Obstacle> = buildList {
        // Center Pochinki City
        add(Obstacle(1850f, 1850f, 120f, 90f, ObstacleType.BUILDING, "Pochinki HQ"))
        add(Obstacle(2050f, 1850f, 110f, 85f, ObstacleType.BUILDING, "Residential A"))
        add(Obstacle(1850f, 2050f, 100f, 100f, ObstacleType.BUILDING, "Warehouse"))
        add(Obstacle(2050f, 2050f, 130f, 90f, ObstacleType.BUILDING, "Supermarket"))

        // Military Base (South-East)
        add(Obstacle(3000f, 3000f, 220f, 140f, ObstacleType.HANGAR, "Military Hangar"))
        add(Obstacle(3280f, 2900f, 60f, 60f, ObstacleType.WATCHTOWER, "Watchtower Alpha"))
        add(Obstacle(2900f, 3200f, 60f, 60f, ObstacleType.WATCHTOWER, "Watchtower Bravo"))
        add(Obstacle(2800f, 2800f, 180f, 20f, ObstacleType.WALL, "Perimeter Fence"))
        add(Obstacle(3300f, 2800f, 20f, 250f, ObstacleType.WALL, "Concrete Barrier"))

        // Coastal Harbor (North-West)
        add(Obstacle(800f, 800f, 180f, 110f, ObstacleType.HANGAR, "Cargo Terminal"))
        add(Obstacle(1050f, 850f, 90f, 80f, ObstacleType.BUILDING, "Harbor Office"))

        // Desert Stunt Park (North-East) - Stunt Ramps for epic car jumps!
        add(Obstacle(2900f, 1000f, 70f, 40f, ObstacleType.STUNT_RAMP, "Mega Ramp 1"))
        add(Obstacle(3150f, 1000f, 70f, 40f, ObstacleType.STUNT_RAMP, "Mega Ramp 2"))
        add(Obstacle(2700f, 1200f, 70f, 40f, ObstacleType.STUNT_RAMP, "Loop Ramp"))

        // Gas Stations (Refueling pads on highways)
        add(Obstacle(1950f, 600f, 80f, 80f, ObstacleType.GAS_STATION_PAD, "North Fuel Stop"))
        add(Obstacle(1950f, 3400f, 80f, 80f, ObstacleType.GAS_STATION_PAD, "South Fuel Stop"))
        add(Obstacle(600f, 1950f, 80f, 80f, ObstacleType.GAS_STATION_PAD, "West Fuel Stop"))
        add(Obstacle(3400f, 1950f, 80f, 80f, ObstacleType.GAS_STATION_PAD, "East Fuel Stop"))

        // Natural obstacles (Trees & Rocks scattered along terrain)
        val random = Random(42)
        for (i in 0 until 40) {
            val tx = random.nextFloat() * 3400f + 300f
            val ty = random.nextFloat() * 3400f + 300f
            // Avoid road center
            if (kotlin.math.abs(tx - 2000f) > 80f && kotlin.math.abs(ty - 2000f) > 80f) {
                add(Obstacle(tx, ty, 35f, 35f, ObstacleType.TREE))
            }
        }
        for (i in 0 until 25) {
            val rx = random.nextFloat() * 3400f + 300f
            val ry = random.nextFloat() * 3400f + 300f
            add(Obstacle(rx, ry, 45f, 35f, ObstacleType.ROCK))
        }
    }

    /**
     * Spawns iconic vehicles across the map.
     */
    fun spawnInitialVehicles(): List<VehicleInstance> {
        return listOf(
            // UAZ 4x4 near Military Base
            VehicleInstance(
                id = "veh_uaz_1",
                model = VehicleModel.UAZ_4X4,
                x = 2850f,
                y = 2950f,
                angleDeg = 45f
            ),
            // Desert Buggy near Stunt Area
            VehicleInstance(
                id = "veh_buggy_1",
                model = VehicleModel.DESERT_BUGGY,
                x = 2750f,
                y = 1050f,
                angleDeg = 90f
            ),
            // Dacia Sedan at Pochinki Town
            VehicleInstance(
                id = "veh_dacia_1",
                model = VehicleModel.SPORT_DACIA,
                x = 1950f,
                y = 1950f,
                angleDeg = 0f
            ),
            // Tactical Bike near West Outpost
            VehicleInstance(
                id = "veh_bike_1",
                model = VehicleModel.COMBAT_BIKE,
                x = 1050f,
                y = 1950f,
                angleDeg = 180f
            ),
            // Extra vehicles around perimeter
            VehicleInstance(
                id = "veh_uaz_2",
                model = VehicleModel.UAZ_4X4,
                x = 750f,
                y = 900f,
                angleDeg = 270f
            ),
            VehicleInstance(
                id = "veh_buggy_2",
                model = VehicleModel.DESERT_BUGGY,
                x = 2100f,
                y = 3350f,
                angleDeg = 0f
            )
        )
    }

    /**
     * Spawns loot items across compounds.
     */
    fun spawnLoot(): List<LootDrop> {
        val list = mutableListOf<LootDrop>()
        var idCounter = 1

        val lootSpots = listOf(
            Offset(1900f, 1900f) to "M416",
            Offset(2100f, 1900f) to "AKM",
            Offset(1900f, 2100f) to "First Aid",
            Offset(2100f, 2100f) to "Energy Drink",
            Offset(3050f, 3050f) to "Level 3 Helmet",
            Offset(3200f, 3050f) to "Level 3 Vest",
            Offset(2950f, 3100f) to "5.56mm Ammo",
            Offset(2850f, 3200f) to "7.62mm Ammo",
            Offset(850f, 850f) to "Jerry Can",
            Offset(1100f, 900f) to "Painkiller",
            Offset(2850f, 950f) to "Nitro Booster",
            Offset(3200f, 950f) to "Groza AR",
            Offset(1980f, 620f) to "Med Kit",
            Offset(620f, 1980f) to "Flare Gun"
        )

        for ((pos, title) in lootSpots) {
            val (type, emoji) = when {
                title.contains("M416") || title.contains("AKM") || title.contains("Groza") -> "WEAPON" to "🔫"
                title.contains("Ammo") -> "AMMO" to "📦"
                title.contains("Helmet") || title.contains("Vest") -> "ARMOR" to "🛡️"
                title.contains("First Aid") || title.contains("Med Kit") || title.contains("Drink") || title.contains("Painkiller") -> "MED" to "💊"
                title.contains("Jerry") || title.contains("Nitro") -> "FUEL" to "⛽"
                title.contains("Flare") -> "FLARE" to "🚀"
                else -> "LOOT" to "🎁"
            }
            list.add(LootDrop("loot_${idCounter++}", pos.x, pos.y, title, type, emoji))
        }
        return list
    }

    /**
     * Checks if coordinates are on an asphalt road (provides high speed & drift traction).
     */
    fun isOnRoad(x: Float, y: Float): Boolean {
        for (road in roads) {
            val dist = pointToSegmentDistance(Offset(x, y), road.start, road.end)
            if (dist <= road.width / 2f) return true
        }
        return false
    }

    private fun pointToSegmentDistance(p: Offset, a: Offset, b: Offset): Float {
        val ab = b - a
        val ap = p - a
        val abLenSq = ab.x * ab.x + ab.y * ab.y
        if (abLenSq == 0f) return (p - a).getDistance()
        val t = ((ap.x * ab.x + ap.y * ab.y) / abLenSq).coerceIn(0f, 1f)
        val proj = Offset(a.x + t * ab.x, a.y + t * ab.y)
        return (p - proj).getDistance()
    }
}
