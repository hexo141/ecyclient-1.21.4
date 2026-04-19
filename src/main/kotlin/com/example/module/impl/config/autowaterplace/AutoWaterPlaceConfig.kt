package com.example.module.impl.config.autowaterplace

object AutoWaterPlaceConfig {
    var minFallDistance: Float = 3f
    var placeDistance: Double = 2.0
    var maxGroundDistance: Double = 10.0
    var cooldownTime: Int = 20
    var requireWaterBucket: Boolean = true

    fun loadDefaults() {
        minFallDistance = 3f
        placeDistance = 2.0
        maxGroundDistance = 10.0
        cooldownTime = 20
        requireWaterBucket = true
    }

    fun toMap(): Map<String, Any> = mapOf(
        "minFallDistance" to minFallDistance,
        "placeDistance" to placeDistance,
        "maxGroundDistance" to maxGroundDistance,
        "cooldownTime" to cooldownTime,
        "requireWaterBucket" to requireWaterBucket
    )
}
