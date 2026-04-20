package com.example.module

import net.minecraft.client.gui.DrawContext

interface GameModule {

    val metadata: ModuleMetadata
    var state: ModuleState

    fun onEnable() {}
    fun onDisable() {}

    fun onTick() {}
    fun onRenderWorld(context: DrawContext, tickDelta: Float) {}
}