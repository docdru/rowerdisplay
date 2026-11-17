package io.github.docdru.rowerdisplay.ble

/**
 * The latest known values from the rower. Every field is optional because an FTMS
 * machine sends only the fields its flags announce, sometimes spread over several
 * notifications. Use [mergedWith] to fold a new partial packet into the known state.
 */
data class RowerData(
    /** Strokes per minute, resolution 0.5. */
    val strokeRateSpm: Double? = null,
    val strokeCount: Int? = null,
    val averageStrokeRateSpm: Double? = null,
    /** Metres since the rower's own session start. */
    val totalDistanceM: Int? = null,
    /** Seconds per 500 m. */
    val instantaneousPaceS: Int? = null,
    val averagePaceS: Int? = null,
    val instantaneousPowerW: Int? = null,
    val averagePowerW: Int? = null,
    val resistanceLevel: Int? = null,
    val totalEnergyKcal: Int? = null,
    val energyPerHourKcal: Int? = null,
    val energyPerMinuteKcal: Int? = null,
    val heartRateBpm: Int? = null,
    /** Metabolic equivalent, resolution 0.1. */
    val metabolicEquivalent: Double? = null,
    val elapsedTimeS: Int? = null,
    val remainingTimeS: Int? = null,
) {
    /** Returns a copy where every field present in [newer] replaces the known value. */
    fun mergedWith(newer: RowerData): RowerData = RowerData(
        strokeRateSpm = newer.strokeRateSpm ?: strokeRateSpm,
        strokeCount = newer.strokeCount ?: strokeCount,
        averageStrokeRateSpm = newer.averageStrokeRateSpm ?: averageStrokeRateSpm,
        totalDistanceM = newer.totalDistanceM ?: totalDistanceM,
        instantaneousPaceS = newer.instantaneousPaceS ?: instantaneousPaceS,
        averagePaceS = newer.averagePaceS ?: averagePaceS,
        instantaneousPowerW = newer.instantaneousPowerW ?: instantaneousPowerW,
        averagePowerW = newer.averagePowerW ?: averagePowerW,
        resistanceLevel = newer.resistanceLevel ?: resistanceLevel,
        totalEnergyKcal = newer.totalEnergyKcal ?: totalEnergyKcal,
        energyPerHourKcal = newer.energyPerHourKcal ?: energyPerHourKcal,
        energyPerMinuteKcal = newer.energyPerMinuteKcal ?: energyPerMinuteKcal,
        heartRateBpm = newer.heartRateBpm ?: heartRateBpm,
        metabolicEquivalent = newer.metabolicEquivalent ?: metabolicEquivalent,
        elapsedTimeS = newer.elapsedTimeS ?: elapsedTimeS,
        remainingTimeS = newer.remainingTimeS ?: remainingTimeS,
    )
}

/** FTMS Training Status (0x2AD3) values this app cares about. */
enum class TrainingStatus {
    Idle,
    Active,
    Other,
}
