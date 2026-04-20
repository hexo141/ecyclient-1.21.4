package com.example.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.sound.SoundEvents
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object HudManager {
    private val logger = LoggerFactory.getLogger("ecyclient-hud")
    
    // RSHIFT 键绑定
    private val hudKeyBinding = KeyBinding(
        "key.ecyclient.hud",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_RIGHT_SHIFT,
        "category.ecyclient"
    )
    
    fun getKeyBinding(): KeyBinding = hudKeyBinding
    
    fun isVisible(): Boolean {
        val client = MinecraftClient.getInstance()
        return client.currentScreen is ModuleHudScreen
    }
    
    fun toggleHud() {
        val client = MinecraftClient.getInstance()
        
        if (client.currentScreen is ModuleHudScreen) {
            // 如果当前已经是HUD屏幕，则关闭它
            client.setScreen(null)
            playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.8f)
            logger.info("HUD 关闭")
        } else {
            // 否则打开HUD屏幕
            client.setScreen(ModuleHudScreen())
            playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.2f)
            logger.info("HUD 打开")
        }
    }
    
    private fun playSound(soundEvent: net.minecraft.sound.SoundEvent, volume: Float, pitch: Float) {
        val client = MinecraftClient.getInstance()
        client.soundManager.play(net.minecraft.client.sound.PositionedSoundInstance.master(soundEvent, pitch, volume))
    }
}