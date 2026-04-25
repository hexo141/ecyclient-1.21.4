package com.example.module.render

import com.example.effect.MotionBlurEffect
import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState

object MotionBlur : GameModule {
    override val metadata = ModuleMetadata(
        id = "motion_blur",
        name = "动态模糊",
        description = "为游戏画面添加动态模糊效果",
        category = ModuleCategory.RENDER
    )

    override var state: ModuleState = ModuleState.DISABLED

    var strength: Float = 0.5f

    override fun onEnable() {
        MotionBlurEffect.init()
    }

    override fun onDisable() {
        MotionBlurEffect.cleanup()
    }

    override fun onTick() {
        strength = strength.coerceIn(0.0f, 0.95f)
    }

    override fun onRenderWorld(context: net.minecraft.client.gui.DrawContext, tickDelta: Float) {
    }

    fun isEnabled(): Boolean = state == ModuleState.ENABLED

    override fun getConfig(): Map<String, Any> {
        return mapOf(
            "strength" to strength
        )
    }

    override fun applyConfig(config: Map<String, Any>) {
        (config["strength"] as? Number)?.toFloat()?.let { strength = it }
    }
}
