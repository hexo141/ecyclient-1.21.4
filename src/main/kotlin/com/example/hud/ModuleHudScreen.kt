package com.example.hud

import com.example.module.ModuleManager
import com.example.module.ModuleState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class ModuleHudScreen : Screen(Text.translatable("ecyclient.modules.hud")) {
    
    private val windowWidth = 120
    private val titleBarHeight = 16
    private val buttonHeight = 14
    private val backgroundColor = 0xBB000000.toInt()
    private val titleBarColor = 0xDD222222.toInt()
    private val enabledColor = 0xFF00AA00.toInt()
    private val disabledColor = 0xFF666666.toInt()
    private val textColor = 0xFFFFFFFF.toInt()
    private val titleTextColor = 0xFFAAAAAA.toInt()
    
    private data class ClickWindow(
        var x: Int,
        var y: Int,
        var minimized: Boolean = false,
        val buttons: MutableList<ModuleButton> = mutableListOf()
    )
    
    private data class ModuleButton(
        val name: String,
        val id: String,
        var enabled: Boolean = false,
        var x: Int = 0,
        var y: Int = 0,
        var width: Int = 0,
        var height: Int = 0
    )
    
    private val windows = mutableListOf<ClickWindow>()
    private var dragWindow: ClickWindow? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    
    override fun init() {
        super.init()
        windows.clear()
        
        if (windows.isEmpty()) {
            val metadataList = ModuleManager.getAllMetadata()
            if (metadataList.isEmpty()) {
                val window = ClickWindow(width / 2 - windowWidth / 2, height / 4)
                windows.add(window)
                return
            }
            
            val window = ClickWindow(width / 2 - windowWidth / 2, height / 4)
            
            for (metadata in metadataList) {
                val module = ModuleManager.getModule(metadata.id)
                val isEnabled = module?.state == ModuleState.LOADED
                
                window.buttons.add(ModuleButton(
                    name = metadata.name,
                    id = metadata.id,
                    enabled = isEnabled
                ))
            }
            
            windows.add(window)
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x80000000.toInt())
        
        for (window in windows) {
            renderWindow(context, window, mouseX, mouseY)
        }
    }
    
    private fun renderWindow(context: DrawContext, window: ClickWindow, mouseX: Int, mouseY: Int) {
        val windowHeight = if (window.minimized) {
            titleBarHeight
        } else {
            titleBarHeight + window.buttons.size * buttonHeight
        }
        
        context.fill(window.x - 1, window.y - 1, window.x + windowWidth + 1, window.y + windowHeight + 1, 0xFF000000.toInt())
        
        context.fill(window.x, window.y, window.x + windowWidth, window.y + titleBarHeight, titleBarColor)
        
        val title = if (window.minimized) "Modules +" else "Modules -"
        context.drawText(textRenderer, title, window.x + 4, window.y + (titleBarHeight - textRenderer.fontHeight) / 2, titleTextColor, true)
        
        if (!window.minimized) {
            for ((index, button) in window.buttons.withIndex()) {
                button.x = window.x
                button.y = window.y + titleBarHeight + index * buttonHeight
                button.width = windowWidth
                button.height = buttonHeight
                
                val bgColor = if (button.enabled) enabledColor else disabledColor
                context.fill(button.x, button.y, button.x + button.width, button.y + button.height, backgroundColor)
                
                val buttonColor = if (button.enabled) 0xFF00FF00.toInt() else 0xFF888888.toInt()
                context.fill(button.x, button.y, button.x + 2, button.y + button.height, buttonColor)
                
                context.drawText(
                    textRenderer,
                    button.name,
                    button.x + 4,
                    button.y + (buttonHeight - textRenderer.fontHeight) / 2,
                    textColor,
                    true
                )
            }
        }
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button)
        
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        
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
                        
                        if (newState) {
                            ModuleManager.enableModule(btn.id)
                        } else {
                            ModuleManager.disableModule(btn.id)
                        }
                        return true
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
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun shouldPause() = false
}
