package com.example.module.impl


import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import com.example.module.config.ConfigPersistence
import com.example.module.impl.config.autosprint.AutoSprintConfig
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient

object AutoSprint : GameModule {
    override val metadata = ModuleMetadata(
        id = "auto_sprint",
        name = "Auto Sprint",
        version = "1.0.0",
        description = "Automatically keeps the player sprinting",
        enabled = true,
        category = ModuleCategory.ASSISTANT
    )

    override var state: ModuleState = ModuleState.DISABLED

    private val client: MinecraftClient get() = MinecraftClient.getInstance()

    override fun onEnable() {
        loadSavedConfig()
    }

    override fun onDisable() {
        val player = client.player ?: return
        player.isSprinting = false
    }
    
    private fun loadSavedConfig() {
        val savedConfig = ConfigPersistence.loadConfig("auto_sprint") ?: return
        
        val allowInCombat = savedConfig.get("allowInCombat")?.asBoolean ?: AutoSprintConfig.allowInCombat
        val stopWhenHungry = savedConfig.get("stopWhenHungry")?.asBoolean ?: AutoSprintConfig.stopWhenHungry
        val hungerThreshold = savedConfig.get("hungerThreshold")?.asInt ?: AutoSprintConfig.hungerThreshold
        val allowInWater = savedConfig.get("allowInWater")?.asBoolean ?: AutoSprintConfig.allowInWater
        
        configure(
            allowInCombat = allowInCombat,
            stopWhenHungry = stopWhenHungry,
            hungerThreshold = hungerThreshold,
            allowInWater = allowInWater
        )
    }

    override fun onTick() {
        if (state != ModuleState.LOADED) return

        val player = client.player ?: return

        if (AutoSprintConfig.stopWhenHungry && player.hungerManager.foodLevel <= AutoSprintConfig.hungerThreshold) {
            return
        }

        if (!AutoSprintConfig.allowInWater && player.isTouchingWater) {
            return
        }

        if (player.input.movementForward > 0f && !player.isUsingItem) {
            if (!player.isSprinting) {
                player.isSprinting = true
            }
        }
    }

    fun configure(
        allowInCombat: Boolean = true,
        stopWhenHungry: Boolean = false,
        hungerThreshold: Int = 6,
        allowInWater: Boolean = false
    ) {
        AutoSprintConfig.allowInCombat = allowInCombat
        AutoSprintConfig.stopWhenHungry = stopWhenHungry
        AutoSprintConfig.hungerThreshold = hungerThreshold
        AutoSprintConfig.allowInWater = allowInWater
    }

    fun getConfig(): Map<String, Any> = AutoSprintConfig.toMap()
}
