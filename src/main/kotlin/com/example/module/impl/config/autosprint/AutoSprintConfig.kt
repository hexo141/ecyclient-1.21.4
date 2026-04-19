package com.example.module.impl.config.autosprint

object AutoSprintConfig {
    var allowInCombat: Boolean = true
    var stopWhenHungry: Boolean = false
    var hungerThreshold: Int = 6
    var allowInWater: Boolean = false

    fun loadDefaults() {
        allowInCombat = true
        stopWhenHungry = false
        hungerThreshold = 6
        allowInWater = false
    }

    fun toMap(): Map<String, Any> = mapOf(
        "allowInCombat" to allowInCombat,
        "stopWhenHungry" to stopWhenHungry,
        "hungerThreshold" to hungerThreshold,
        "allowInWater" to allowInWater
    )
}
