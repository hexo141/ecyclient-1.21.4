package com.example.module

interface GameModule {
    val metadata: ModuleMetadata
    var state: ModuleState
    
    fun onEnable()
    fun onDisable()
    fun onTick() {}
}
