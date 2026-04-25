package com.example.hud

import com.example.module.ConfigManager
import com.example.module.GameModule
import com.example.module.ModuleManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class ModuleConfigScreen(private val module: GameModule) : Screen(Text.translatable("ecyclient.modules.config", module.metadata.name)) {
    
    private val config = module.getConfig()
    private val configKeys = config.keys.toList()
    private val configValues = config.values.toMutableList()
    
    private val windowWidth = 260
    private val windowHeight = 280
    private val titleBarHeight = 20
    private val fieldHeight = 16
    private val padding = 6
    private val backgroundColor = 0xDD000000.toInt()
    private val titleBarColor = 0xFF000000.toInt()
    private val fieldBgColor = 0xBB000000.toInt()
    private val textColor = 0xFFFFFFFF.toInt()
    private val titleTextColor = 0xFFFFD700.toInt()
    private val labelColor = 0xFFCCCCCC.toInt()
    private val valueColor = 0xFFAAAAAA.toInt()
    private val borderColor = 0xFF333333.toInt()
    private val arrowColor = 0xFFFF0000.toInt()
    private val hoverColor = 0x22FFFFFF.toInt()
    private val editFieldBgColor = 0xFF222222.toInt()
    private val editFieldBorderColor = 0xFF555555.toInt()
    
    private var animationProgress = 0f
    private var isClosing = false
    private val animationDuration = 0.25f
    private var lastRenderTime = 0L
    
    private var windowX = 0
    private var windowY = 0
    
    private var scrollOffset = 0
    private val maxVisibleFields = 12
    
    private var originalConfig: Map<String, Any> = emptyMap()
    private var editingIndex: Int = -1
    private var editTextField: TextFieldWidget? = null
    
    override fun init() {
        super.init()
        animationProgress = 0f
        isClosing = false
        lastRenderTime = System.currentTimeMillis()
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.2f)
        
        originalConfig = config.toMap()
        
        windowX = width / 2 - windowWidth / 2
        windowY = height / 2 - windowHeight / 2
        
        editTextField = null
        editingIndex = -1
    }
    
    private fun scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f)
        }
    }
    
    private fun scrollDown() {
        val maxScroll = maxOf(0, configKeys.size - maxVisibleFields)
        if (scrollOffset < maxScroll) {
            scrollOffset++
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f)
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
        
        renderWindowFrame(context, easedProgress)
        renderConfigFields(context, mouseX, mouseY, delta, easedProgress)
        
        renderCloseButton(context, mouseX, mouseY, delta)
        
        editTextField?.render(context, mouseX, mouseY, delta)
    }
    
    private fun renderCloseButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val btnX = windowX + windowWidth - 16
        val btnY = windowY + 2
        val btnW = 14
        val btnH = 14
        
        val isHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH
        
        if (isHovered) {
            context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x44FF0000.toInt())
        }
        
        context.drawText(
            textRenderer,
            "X",
            btnX + (btnW - textRenderer.getWidth("X")) / 2,
            btnY + (btnH - textRenderer.fontHeight) / 2,
            if (isHovered) 0xFFFF4444.toInt() else 0xFFAAAAAA.toInt(),
            true
        )
    }
    
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    }
    
    private fun renderWindowFrame(context: DrawContext, alpha: Float) {
        val frameAlpha = (alpha * 255).toInt().coerceIn(0, 255)
        val frameColor = (0xFF000000.toInt() and 0x00FFFFFF) or (frameAlpha shl 24)
        context.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY + windowHeight + 1, frameColor)
        
        val bgAlpha = (alpha * 221).toInt().coerceIn(0, 221)
        val bgColor = (backgroundColor and 0x00FFFFFF) or (bgAlpha shl 24)
        context.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, bgColor)
        
        val titleAlpha = (alpha * 255).toInt().coerceIn(0, 255)
        val titleColor = (titleBarColor and 0x00FFFFFF) or (titleAlpha shl 24)
        context.fill(windowX, windowY, windowX + windowWidth, windowY + titleBarHeight, titleColor)
        
        val title = "Config"
        val titleTextAlpha = (alpha * 255).toInt().coerceIn(0, 255)
        val titleTextColorFinal = (titleTextColor and 0x00FFFFFF) or (titleTextAlpha shl 24)
        context.drawText(
            textRenderer,
            title,
            windowX + (windowWidth - textRenderer.getWidth(title)) / 2,
            windowY + (titleBarHeight - textRenderer.fontHeight) / 2,
            titleTextColorFinal,
            true
        )
        
        context.fill(
            windowX, windowY + titleBarHeight,
            windowX + windowWidth, windowY + titleBarHeight + 1,
            borderColor
        )
    }
    
    private fun renderConfigFields(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float, alpha: Float) {
        val contentStartY = windowY + titleBarHeight + 4
        val labelWidth = 110
        val valueStartX = windowX + windowWidth - padding - 4
        
        val maxScroll = maxOf(0, configKeys.size - maxVisibleFields)
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
        
        val visibleCount = minOf(maxVisibleFields, configKeys.size - scrollOffset)
        
        for (i in 0 until visibleCount) {
            val index = scrollOffset + i
            if (index >= configKeys.size) break
            
            val key = configKeys[index]
            val value = configValues[index]
            
            val fieldDelay = 0.05f + i * 0.02f
            val fieldProgress = ((alpha - fieldDelay) / (1f - fieldDelay)).coerceIn(0f, 1f)
            val easedFieldProgress = easeOutCubic(fieldProgress)
            
            val currentY = contentStartY + i * (fieldHeight + 2)
            
            val labelAlpha = (easedFieldProgress * 255).toInt().coerceIn(0, 255)
            val labelColorFinal = (labelColor and 0x00FFFFFF) or (labelAlpha shl 24)
            
            var displayLabel = key
            
            val maxLabelWidth = labelWidth - 4
            if (textRenderer.getWidth(displayLabel) > maxLabelWidth) {
                var trimmedLabel = displayLabel
                while (textRenderer.getWidth(trimmedLabel + "...") > maxLabelWidth && trimmedLabel.isNotEmpty()) {
                    trimmedLabel = trimmedLabel.dropLast(1)
                }
                displayLabel = trimmedLabel + "..."
            }
            
            context.drawText(
                textRenderer,
                displayLabel,
                windowX + padding,
                currentY + (fieldHeight - textRenderer.fontHeight) / 2,
                labelColorFinal,
                true
            )
            
            val isEditing = editingIndex == index
            
            if (isEditing) {
                val editFieldWidth = windowWidth - padding * 2 - labelWidth
                if (editTextField == null || editTextField!!.x != windowX + padding + labelWidth || editTextField!!.y != currentY) {
                    editTextField = TextFieldWidget(
                        textRenderer,
                        windowX + padding + labelWidth, currentY,
                        editFieldWidth, fieldHeight,
                        Text.literal(key)
                    )
                    editTextField!!.text = value.toString()
                    editTextField!!.setEditableColor(textColor)
                    editTextField!!.setUneditableColor(valueColor)
                    editTextField!!.setFocused(true)
                    setInitialFocus(editTextField)
                }
            } else {
                val valueText = formatValueForDisplay(key, value)
                val valueWidth = textRenderer.getWidth(valueText)
                val valueX = valueStartX - valueWidth
                
                val valueColorFinal = (valueColor and 0x00FFFFFF) or (labelAlpha shl 24)
                context.drawText(
                    textRenderer,
                    valueText,
                    valueX,
                    currentY + (fieldHeight - textRenderer.fontHeight) / 2,
                    valueColorFinal,
                    true
                )
            }
            
            if (!isEditing && mouseX >= windowX + padding && mouseX <= windowX + windowWidth - padding &&
                mouseY >= currentY && mouseY <= currentY + fieldHeight) {
                context.fill(
                    windowX + padding, currentY,
                    windowX + windowWidth - padding, currentY + fieldHeight,
                    hoverColor
                )
            }
        }
        
        if (scrollOffset > 0) {
            val upAlpha = (alpha * 255).toInt().coerceIn(0, 255)
            val upColor = (arrowColor and 0x00FFFFFF) or (upAlpha shl 24)
            context.drawText(
                textRenderer,
                "^",
                windowX + 2,
                windowY + titleBarHeight + 2,
                upColor,
                true
            )
        }
        
        if (scrollOffset < maxOf(0, configKeys.size - maxVisibleFields)) {
            val downAlpha = (alpha * 255).toInt().coerceIn(0, 255)
            val downColor = (arrowColor and 0x00FFFFFF) or (downAlpha shl 24)
            context.drawText(
                textRenderer,
                "v",
                windowX + 2,
                windowY + windowHeight - 20,
                downColor,
                true
            )
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
    
    private fun formatValueForDisplay(key: String, value: Any): String {
        return if (value is Int && key.lowercase().contains("color")) {
            String.format("#%08X", value.toLong() and 0xFFFFFFFFL)
        } else {
            value.toString()
        }
    }
    
    private fun parseConfigValue(key: String, originalValue: Any?, newText: String): Any {
        return when (originalValue) {
            is Boolean -> newText.toBoolean()
            is Int -> {
                if (key.lowercase().contains("color")) {
                    val hexStr = if (newText.startsWith("#")) newText.substring(1) else newText
                    java.lang.Long.parseLong(hexStr, 16).toInt()
                } else {
                    newText.toInt()
                }
            }
            is Float -> newText.toFloat()
            is Double -> newText.toDouble()
            is Long -> newText.toLong()
            is String -> newText
            else -> newText
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
        
        val btnX = windowX + windowWidth - 16
        val btnY = windowY + 2
        val btnW = 14
        val btnH = 14
        
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            startClosing()
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f)
            return true
        }
        
        val contentStartY = windowY + titleBarHeight + 4
        val labelWidth = 110
        val maxScroll = maxOf(0, configKeys.size - maxVisibleFields)
        val visibleCount = minOf(maxVisibleFields, configKeys.size - scrollOffset)
        
        for (i in 0 until visibleCount) {
            val index = scrollOffset + i
            if (index >= configKeys.size) break
            
            val currentY = contentStartY + i * (fieldHeight + 2)
            
            if (mx >= windowX + padding && mx <= windowX + windowWidth - padding &&
                my >= currentY && my <= currentY + fieldHeight) {
                
                if (editingIndex == index) {
                    finishEditing()
                } else {
                    editingIndex = index
                    editTextField = null
                }
                playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f)
                return true
            }
        }
        
        if (editingIndex >= 0) {
            finishEditing()
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    private fun finishEditing() {
        if (editingIndex >= 0 && editTextField != null) {
            val newText = editTextField!!.text
            val key = configKeys[editingIndex]
            val originalValue = originalConfig[key]
            
            try {
                val newValue = parseConfigValue(key, originalValue, newText)
                configValues[editingIndex] = newValue
                
                val newConfigMap = mutableMapOf<String, Any>()
                for (idx in configKeys.indices) {
                    newConfigMap[configKeys[idx]] = configValues[idx]
                }
                module.applyConfig(newConfigMap)
                ConfigManager.save(module)
                
                playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
            } catch (e: Exception) {
                playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
        }
        editingIndex = -1
        editTextField = null
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount < 0) {
            scrollDown()
        } else if (verticalAmount > 0) {
            scrollUp()
        }
        return true
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (editingIndex >= 0 && editTextField != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                finishEditing()
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingIndex = -1
                editTextField = null
                return true
            }
            if (editTextField!!.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            startClosing()
            return true
        }
        
        if (keyCode == GLFW.GLFW_KEY_UP) {
            scrollUp()
            return true
        }
        
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scrollDown()
            return true
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (editingIndex >= 0 && editTextField != null) {
            if (editTextField!!.charTyped(chr, modifiers)) {
                return true
            }
        }
        return super.charTyped(chr, modifiers)
    }
    
    private fun startClosing() {
        if (!isClosing && animationProgress > 0f) {
            isClosing = true
            playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.8f)
        } else {
            close()
        }
    }
    
    private fun saveConfig() {
        try {
            module.applyConfig(originalConfig)
            ConfigManager.save(module)
            
            playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
            
            startClosing()
            
        } catch (e: Exception) {
            playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        }
    }
    
    override fun close() {
        client?.setScreen(ModuleHudScreen())
    }
    
    private fun playSound(soundEvent: net.minecraft.sound.SoundEvent, volume: Float, pitch: Float) {
        val client = MinecraftClient.getInstance()
        client.soundManager.play(PositionedSoundInstance.master(soundEvent, pitch, volume))
    }
    
    override fun shouldPause() = false
}