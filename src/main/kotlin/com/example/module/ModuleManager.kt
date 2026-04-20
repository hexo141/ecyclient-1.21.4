package com.example.module

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import org.slf4j.LoggerFactory
import java.io.File

object ModuleManager {
    private val logger = LoggerFactory.getLogger("ecyclient-module")
    private val modules = mutableListOf<GameModule>()

    fun register(module: GameModule) {
        modules.add(module)
        ConfigManager.load(module)
        logger.info("模块已注册: ${module.metadata.name} (${module.metadata.id})")
    }

    fun enable(module: GameModule) {
        if (module.state == ModuleState.ENABLED) return

        try {
            module.onEnable()
            module.state = ModuleState.ENABLED
            ConfigManager.save(module)
            logger.info("模块已启用: ${module.metadata.name}")
        } catch (e: Exception) {
            module.state = ModuleState.ERROR
            logger.error("启用模块失败: ${module.metadata.name}", e)
        }
    }

    fun disable(module: GameModule) {
        if (module.state == ModuleState.DISABLED) return

        try {
            module.onDisable()
            module.state = ModuleState.DISABLED
            ConfigManager.save(module)
            logger.info("模块已禁用: ${module.metadata.name}")
        } catch (e: Exception) {
            module.state = ModuleState.ERROR
            logger.error("禁用模块失败: ${module.metadata.name}", e)
        }
    }

    fun toggle(module: GameModule) {
        if (module.state == ModuleState.ENABLED) disable(module)
        else enable(module)
    }

    fun onTick() {
        val client = MinecraftClient.getInstance()
        if (client.world == null || client.player == null) return

        modules.forEach {
            if (it.state == ModuleState.ENABLED) {
                safeRun(it) { it.onTick() }
            }
        }
    }

    fun onRender(context: DrawContext, tickDelta: Float) {
        val client = MinecraftClient.getInstance()
        if (client.world == null || client.player == null) return

        modules.forEach {
            if (it.state == ModuleState.ENABLED) {
                safeRun(it) { it.onRenderWorld(context, tickDelta) }
            }
        }
    }

    fun getModules(): List<GameModule> = modules.toList()

    fun getModuleById(id: String): GameModule? = modules.find { it.metadata.id == id }

    fun getModulesByCategory(category: ModuleCategory): List<GameModule> = 
        modules.filter { it.metadata.category == category }

    private inline fun safeRun(module: GameModule, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            module.state = ModuleState.ERROR
            logger.error("模块执行异常: ${module.metadata.name}", e)
        }
    }
}