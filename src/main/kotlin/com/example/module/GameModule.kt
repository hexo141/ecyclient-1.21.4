package com.example.module

import net.minecraft.client.gui.DrawContext

interface GameModule {

    val metadata: ModuleMetadata
    var state: ModuleState

    fun onEnable() {}
    fun onDisable() {}

    fun onTick() {}
    fun onRenderWorld(context: DrawContext, tickDelta: Float) {}
    
    /**
     * 获取模块的配置参数
     */
    fun getConfig(): Map<String, Any> = emptyMap()
    
    /**
     * 应用模块的配置参数
     */
    fun applyConfig(config: Map<String, Any>) {}
}