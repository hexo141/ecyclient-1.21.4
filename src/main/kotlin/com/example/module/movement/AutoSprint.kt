package com.example.module.movement

import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity

object AutoSprint : GameModule {
    override val metadata = ModuleMetadata(
        id = "auto_sprint",
        name = "自动疾跑",
        description = "自动保持疾跑状态，无需按住前进键",
        category = ModuleCategory.MOVEMENT
    )
    
    override var state: ModuleState = ModuleState.DISABLED
    
    override fun onEnable() {
        // 模块启用时的初始化逻辑
    }
    
    override fun onDisable() {
        // 模块禁用时的清理逻辑
    }
    
    override fun onTick() {
        val client = MinecraftClient.getInstance()
        val player = client.player
        
        if (player == null || client.world == null) return
        
        // 自动疾跑逻辑
        if (canSprint(player)) {
            player.isSprinting = true
        }
    }
    
    override fun onRenderWorld(context: net.minecraft.client.gui.DrawContext, tickDelta: Float) {
        // 不需要渲染逻辑
    }
    
    private fun canSprint(player: ClientPlayerEntity): Boolean {
        // 检查是否可以疾跑的条件
        return player.isOnGround && 
               player.forwardSpeed > 0 && 
               !player.horizontalCollision && 
               player.hungerManager.foodLevel > 6
    }
}