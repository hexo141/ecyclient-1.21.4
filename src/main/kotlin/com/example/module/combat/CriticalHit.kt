package com.example.module.combat

import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import net.minecraft.client.gui.DrawContext

object CriticalHit : GameModule {
    override val metadata = ModuleMetadata(
        id = "critical_hit",
        name = "刀刀暴击",
        description = "每次攻击时自动跳跃实现暴击效果",
        category = ModuleCategory.COMBAT
    )
    
    override var state: ModuleState = ModuleState.DISABLED
    
    override fun onEnable() {
    }
    
    override fun onDisable() {
    }
    
    override fun onTick() {
    }
    
    override fun onRenderWorld(context: DrawContext, tickDelta: Float) {
    }
}
