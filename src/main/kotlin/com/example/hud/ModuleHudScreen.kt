package com.example.hud

import com.example.module.ModuleCategory
import com.example.module.ModuleManager
import com.example.module.ModuleState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class ModuleHudScreen : Screen(Text.translatable("ecyclient.modules.hud")) {
    
    private val windowWidth = 150
    private val titleBarHeight = 16
    private val buttonHeight = 14
    private val categoryHeaderHeight = 12
    private val padding = 2
    private val backgroundColor = 0xBB000000.toInt()
    private val titleBarColor = 0xDD222222.toInt()
    private val categoryHeaderColor = 0xAA333333.toInt()
    private val textColor = 0xFFFFFFFF.toInt()
    private val titleTextColor = 0xFFAAAAAA.toInt()
    
    private var animationProgress = 0f
    private var isClosing = false
    private val animationDuration = 0.25f
    private var lastRenderTime = 0L
    
    private class CategoryWindow(
        val category: ModuleCategory,
        var x: Int,
        var y: Int,
        var minimized: Boolean = false,
        val buttons: MutableList<ModuleButton> = mutableListOf(),
        private val titleBarHeight: Int,
        private val categoryHeaderHeight: Int,
        private val buttonHeight: Int
    ) {
        fun getHeight(): Int {
            return if (minimized) {
                titleBarHeight
            } else {
                titleBarHeight + categoryHeaderHeight + buttons.size * buttonHeight
            }
        }
    }
    
    private data class ModuleButton(
        val name: String,
        val id: String,
        var enabled: Boolean = false,
        var x: Int = 0,
        var y: Int = 0,
        var width: Int = 0,
        var height: Int = 0
    )
    
    private val windows = mutableListOf<CategoryWindow>()
    private var dragWindow: CategoryWindow? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    
    override fun init() {
        super.init()
        windows.clear()
        animationProgress = 0f
        isClosing = false
        lastRenderTime = System.currentTimeMillis()
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.2f)
        
        val modules = ModuleManager.getModules()
        if (modules.isEmpty()) {
            val defaultWindow = CategoryWindow(ModuleCategory.MISC, width / 2 - windowWidth / 2, height / 4, titleBarHeight = titleBarHeight, categoryHeaderHeight = categoryHeaderHeight, buttonHeight = buttonHeight)
            windows.add(defaultWindow)
            return
        }
        
        val categories = listOf(ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC)
        var startY = height / 6
        
        for (category in categories) {
            val categoryModules = modules.filter { it.metadata.category == category }
            if (categoryModules.isEmpty()) continue
            
            val window = CategoryWindow(category, width / 2 - windowWidth / 2, startY, titleBarHeight = titleBarHeight, categoryHeaderHeight = categoryHeaderHeight, buttonHeight = buttonHeight)
            
            for (module in categoryModules) {
                val isEnabled = module.state == ModuleState.ENABLED
                
                window.buttons.add(ModuleButton(
                    name = module.metadata.name,
                    id = module.metadata.id,
                    enabled = isEnabled
                ))
            }
            
            windows.add(window)
            startY += window.getHeight() + 10
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastRenderTime) / 1000f
        lastRenderTime = currentTime
        
        if (isClosing) {
            animationProgress -= deltaTime / animationDuration
            if (animationProgress <= 0f) {
                animationProgress = 0f
                close()
                return
            }
        } else {
            animationProgress += deltaTime / animationDuration
            if (animationProgress > 1f) {
                animationProgress = 1f
            }
        }
        
        for (window in windows) {
            for (button in window.buttons) {
                val module = ModuleManager.getModuleById(button.id)
                button.enabled = module?.state == ModuleState.ENABLED
            }
        }
        
        val easedProgress = easeOutCubic(animationProgress)
        val backgroundAlpha = if (isClosing) {
            easeOutCubic(animationProgress)
        } else {
            easeInCubic(animationProgress.coerceIn(0f, 0.5f) / 0.5f)
        }
        
        renderBlurredBackground(context, mouseX, mouseY, delta, backgroundAlpha)
        
        val overlayAlpha = (0x80 * animationProgress).toInt().coerceIn(0, 0x80)
        val overlayColor = (overlayAlpha shl 24) or 0x000000
        context.fill(0, 0, width, height, overlayColor)
        
        context.matrices.push()
        
        val centerIndex = windows.size / 2
        val centerWindow = if (windows.isNotEmpty()) windows[centerIndex] else null
        val windowCenterX = centerWindow?.let { it.x + windowWidth / 2f } ?: width / 2f
        val windowCenterY = centerWindow?.let { it.y + it.getHeight() / 2f } ?: height / 2f
        
        context.matrices.translate(windowCenterX, windowCenterY, 0f)
        context.matrices.scale(easedProgress, easedProgress, 1f)
        context.matrices.translate(-windowCenterX, -windowCenterY, 0f)
        
        for ((windowIndex, window) in windows.withIndex()) {
            renderWindowFrame(context, window)
            
            if (!window.minimized) {
                var buttonIndex = 0
                for (button in window.buttons) {
                    button.x = window.x + padding
                    button.y = window.y + titleBarHeight + categoryHeaderHeight + buttonIndex * buttonHeight
                    button.width = windowWidth - padding * 2
                    button.height = buttonHeight
                    
                    val buttonDelay = 0.1f + buttonIndex * 0.03f
                    val buttonProgress = ((animationProgress - buttonDelay) / (1f - buttonDelay)).coerceIn(0f, 1f)
                    val easedButtonProgress = easeOutCubic(buttonProgress)
                    
                    val bgColor = if (button.enabled) 0xFF00AA00.toInt() else 0xFF333333.toInt()
                    val indicatorColor = if (button.enabled) 0xFF00FF00.toInt() else 0xFF666666.toInt()
                    
                    if (animationProgress >= 1f) {
                        context.fill(button.x, button.y, button.x + button.width, button.y + button.height, bgColor)
                        context.fill(button.x, button.y, button.x + 2, button.y + button.height, indicatorColor)
                        
                        context.drawText(
                            textRenderer,
                            button.name,
                            button.x + 4,
                            button.y + (buttonHeight - textRenderer.fontHeight) / 2,
                            textColor,
                            true
                        )
                        
                        val hasConfig = false // 暂时不支持配置
                        if (hasConfig) {
                            val gearText = "⚙"
                            val gearWidth = textRenderer.getWidth(gearText)
                            context.drawText(
                                textRenderer,
                                gearText,
                                button.x + button.width - gearWidth - 4,
                                button.y + (buttonHeight - textRenderer.fontHeight) / 2,
                                0xFFAAAAAA.toInt(),
                                true
                            )
                        }
                    } else {
                        val buttonAlpha = (easedButtonProgress * 255).toInt().coerceIn(0, 255)
                        
                        val bgWithAlpha = (bgColor and 0x00FFFFFF) or (buttonAlpha shl 24)
                        context.fill(button.x, button.y, button.x + button.width, button.y + button.height, bgWithAlpha)
                        
                        val indicatorAlpha = (easedButtonProgress * 255).toInt().coerceIn(0, 255)
                        val colorWithAlpha = (indicatorColor and 0x00FFFFFF) or (indicatorAlpha shl 24)
                        context.fill(button.x, button.y, button.x + 2, button.y + button.height, colorWithAlpha)
                        
                        val textAlpha = (easedButtonProgress * 255).toInt().coerceIn(0, 255)
                        val textColorWithAlpha = (0xFFFFFF or (textAlpha shl 24))
                        context.drawText(
                            textRenderer,
                            button.name,
                            button.x + 4,
                            button.y + (buttonHeight - textRenderer.fontHeight) / 2,
                            textColorWithAlpha,
                            true
                        )
                        
                        val hasConfig = false // 暂时不支持配置
                        if (hasConfig) {
                            val gearText = "⚙"
                            val gearWidth = textRenderer.getWidth(gearText)
                            val gearAlpha = (easedButtonProgress * 170).toInt().coerceIn(0, 170)
                            val gearColor = (0xAAAAAA or (gearAlpha shl 24))
                            context.drawText(
                                textRenderer,
                                gearText,
                                button.x + button.width - gearWidth - 4,
                                button.y + (buttonHeight - textRenderer.fontHeight) / 2,
                                gearColor,
                                true
                            )
                        }
                    }
                    
                    buttonIndex++
                }
            }
        }
        
        context.matrices.pop()
    }
    
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    }
    
    private fun renderWindowFrame(context: DrawContext, window: CategoryWindow) {
        val windowHeight = window.getHeight()
        
        context.fill(window.x - 1, window.y - 1, window.x + windowWidth + 1, window.y + windowHeight + 1, 0xFF000000.toInt())
        
        context.fill(window.x, window.y, window.x + windowWidth, window.y + titleBarHeight, titleBarColor)
        
        val title = "${window.category.name} ${if (window.minimized) "+" else "-"}"
        context.drawText(textRenderer, title, window.x + 4, window.y + (titleBarHeight - textRenderer.fontHeight) / 2, titleTextColor, true)
        
        if (!window.minimized) {
            context.fill(window.x, window.y + titleBarHeight, window.x + windowWidth, window.y + titleBarHeight + categoryHeaderHeight, categoryHeaderColor)
            context.drawText(textRenderer, "Modules", window.x + 4, window.y + titleBarHeight + 1, 0xFF888888.toInt(), true)
        }
    }
    
    private fun renderBlurredBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float, alpha: Float) {
        val blurAmount = 8
        
        val baseAlpha = (0x70 * alpha).toInt().coerceIn(0, 0x70)
        context.fill(0, 0, width, height, (baseAlpha shl 24) or 0x000000)
        
        if (alpha > 0.1f) {
            for (x in 0 until width step blurAmount) {
                for (y in 0 until height step blurAmount) {
                    val blurAlpha = (0x30 * alpha).toInt()
                    val blurColor = (blurAlpha shl 24) or 0x000000
                    context.fill(x, y, x + blurAmount, y + blurAmount, blurColor)
                }
            }
        }
    }
    
    private fun easeOutCubic(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        val v = 1f - clamped
        return 1f - v * v * v
    }
    
    private fun easeInCubic(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return clamped * clamped * clamped
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        
        if (button == 0) {
            for (window in windows.reversed()) {
                if (mx >= window.x && mx <= window.x + windowWidth &&
                    my >= window.y && my <= window.y + titleBarHeight) {
                    
                    dragWindow = window
                    dragOffsetX = mx - window.x
                    dragOffsetY = my - window.y
                    return true
                }
                
                if (!window.minimized) {
                    for (btn in window.buttons) {
                        if (mx >= btn.x && mx <= btn.x + btn.width &&
                            my >= btn.y && my <= btn.y + btn.height) {
                            
                            val newState = !btn.enabled
                            btn.enabled = newState
                            
                            val module = ModuleManager.getModuleById(btn.id)
                            if (module != null) {
                                if (newState) {
                                    ModuleManager.enable(module)
                                    playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f)
                                } else {
                                    ModuleManager.disable(module)
                                    playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f)
                                }
                            }
                            return true
                        }
                    }
                }
            }
        } else if (button == 1) {
            for (window in windows.reversed()) {
                if (!window.minimized) {
                    for (btn in window.buttons) {
                        if (mx >= btn.x && mx <= btn.x + btn.width &&
                            my >= btn.y && my <= btn.y + btn.height) {
                            
                            val hasConfig = false // 暂时不支持配置
                            if (hasConfig) {
                                // 未来可以打开配置界面
                                return true
                            }
                        }
                    }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        dragWindow = null
        return super.mouseReleased(mouseX, mouseY, button)
    }
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (dragWindow != null && button == 0) {
            dragWindow!!.x = mouseX.toInt() - dragOffsetX
            dragWindow!!.y = mouseY.toInt() - dragOffsetY
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!isClosing && animationProgress > 0f) {
                isClosing = true
                playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.8f)
                return true
            }
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    private fun playSound(soundEvent: net.minecraft.sound.SoundEvent, volume: Float, pitch: Float) {
        val client = MinecraftClient.getInstance()
        client.soundManager.play(PositionedSoundInstance.master(soundEvent, pitch, volume))
    }
    
    override fun shouldPause() = false
}