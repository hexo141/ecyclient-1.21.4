package com.example.hud

import com.example.module.ConfigManager
import com.example.module.GameModule
import com.example.module.ModuleManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class ModuleConfigScreen(private val module: GameModule) : Screen(Text.translatable("ecyclient.modules.config", module.metadata.name)) {
    
    private val config = module.getConfig()
    private val textFields = mutableMapOf<String, TextFieldWidget>()
    private val fieldLabels = mutableMapOf<String, String>()
    
    private val windowWidth = 300
    private val titleBarHeight = 20
    private val fieldHeight = 20
    private val labelWidth = 80
    private val buttonHeight = 20
    private val padding = 6
    private val backgroundColor = 0xBB000000.toInt()
    private val titleBarColor = 0xDD222222.toInt()
    private val fieldBgColor = 0xFF333333.toInt()
    private val textColor = 0xFFFFFFFF.toInt()
    private val titleTextColor = 0xFFAAAAAA.toInt()
    private val labelColor = 0xFF888888.toInt()
    
    private var animationProgress = 0f
    private var isClosing = false
    private val animationDuration = 0.25f
    private var lastRenderTime = 0L
    
    private var windowX = 0
    private var windowY = 0
    private var windowHeight = 0
    
    private var saveButton: ButtonWidget? = null
    private var cancelButton: ButtonWidget? = null
    
    override fun init() {
        super.init()
        animationProgress = 0f
        isClosing = false
        lastRenderTime = System.currentTimeMillis()
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.2f)
        
        textFields.clear()
        fieldLabels.clear()
        
        windowX = width / 2 - windowWidth / 2
        windowY = height / 4
        
        val fieldsCount = config.size
        val fieldsAreaHeight = fieldsCount * (fieldHeight + padding) + padding * 2
        val buttonsAreaHeight = buttonHeight + padding * 2
        val descriptionHeight = if (module.metadata.description.isNotEmpty()) 24 else 0
        windowHeight = titleBarHeight + descriptionHeight + fieldsAreaHeight + buttonsAreaHeight
        
        var currentY = windowY + titleBarHeight + descriptionHeight + padding
        
        val fieldStartX = windowX + padding
        val labelStartX = fieldStartX
        val inputStartX = labelStartX + labelWidth + padding
        val inputWidth = windowWidth - padding * 3 - labelWidth
        
        for ((key, value) in config) {
            fieldLabels[key] = "$key:"
            
            val textField = TextFieldWidget(
                textRenderer,
                inputStartX, currentY, inputWidth, fieldHeight,
                Text.literal(key)
            )
            textField.text = value.toString()
            textField.setEditableColor(textColor)
            textField.setUneditableColor(labelColor)
            textFields[key] = textField
            addSelectableChild(textField)
            
            currentY += fieldHeight + padding
        }
        
        val buttonWidth = 80
        val buttonY = windowY + windowHeight - buttonHeight - padding
        
        saveButton = ButtonWidget.builder(
            Text.translatable("保存"),
            { saveConfig() }
        ).dimensions(windowX + windowWidth / 2 - buttonWidth - padding / 2, buttonY, buttonWidth, buttonHeight).build()
        
        cancelButton = ButtonWidget.builder(
            Text.translatable("取消"),
            { startClosing() }
        ).dimensions(windowX + windowWidth / 2 + padding / 2, buttonY, buttonWidth, buttonHeight).build()
        
        addDrawableChild(saveButton)
        addDrawableChild(cancelButton)
        
        setInitialFocus(textFields.values.firstOrNull())
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
        renderFields(context, mouseX, mouseY, delta, easedProgress)
        
        saveButton?.render(context, mouseX, mouseY, delta)
        cancelButton?.render(context, mouseX, mouseY, delta)
    }
    
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    }
    
    private fun renderWindowFrame(context: DrawContext, alpha: Float) {
        val frameAlpha = (alpha * 255).toInt().coerceIn(0, 255)
        val frameColor = (0xFF000000.toInt() and 0x00FFFFFF) or (frameAlpha shl 24)
        context.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY + windowHeight + 1, frameColor)
        
        val bgAlpha = (alpha * 187).toInt().coerceIn(0, 187)
        val bgColor = (backgroundColor and 0x00FFFFFF) or (bgAlpha shl 24)
        context.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, bgColor)
        
        val titleAlpha = (alpha * 221).toInt().coerceIn(0, 221)
        val titleColor = (titleBarColor and 0x00FFFFFF) or (titleAlpha shl 24)
        context.fill(windowX, windowY, windowX + windowWidth, windowY + titleBarHeight, titleColor)
        
        val title = "${module.metadata.name} 配置"
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
        
        if (module.metadata.description.isNotEmpty()) {
            val descAlpha = (alpha * 170).toInt().coerceIn(0, 170)
            val descColor = (labelColor and 0x00FFFFFF) or (descAlpha shl 24)
            context.drawText(
                textRenderer,
                module.metadata.description,
                windowX + padding,
                windowY + titleBarHeight + 4,
                descColor,
                true
            )
        }
    }
    
    private fun renderFields(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float, alpha: Float) {
        var fieldIndex = 0
        val descriptionHeight = if (module.metadata.description.isNotEmpty()) 24 else 0
        var currentY = windowY + titleBarHeight + descriptionHeight + padding
        
        for ((key, textField) in textFields) {
            val fieldDelay = 0.05f + fieldIndex * 0.02f
            val fieldProgress = ((alpha - fieldDelay) / (1f - fieldDelay)).coerceIn(0f, 1f)
            val easedFieldProgress = easeOutCubic(fieldProgress)
            
            val labelAlpha = (easedFieldProgress * 255).toInt().coerceIn(0, 255)
            val labelColorFinal = (0xFFFFFF or (labelAlpha shl 24))
            
            var label = fieldLabels[key] ?: "$key:"
            
            // 截断过长的标签
            val maxLabelWidth = labelWidth - 4
            if (textRenderer.getWidth(label) > maxLabelWidth) {
                var trimmedLabel = label
                while (textRenderer.getWidth(trimmedLabel + "...") > maxLabelWidth && trimmedLabel.isNotEmpty()) {
                    trimmedLabel = trimmedLabel.dropLast(1)
                }
                label = trimmedLabel + "..."
            }
            
            context.drawText(
                textRenderer,
                label,
                windowX + padding,
                currentY + (fieldHeight - textRenderer.fontHeight) / 2,
                labelColorFinal,
                true
            )
            
            // 鼠标悬停时显示完整标签
            val mouseXPos = mouseX
            val mouseYPos = mouseY
            if (mouseXPos >= windowX + padding && 
                mouseXPos <= windowX + padding + labelWidth &&
                mouseYPos >= currentY && 
                mouseYPos <= currentY + fieldHeight) {
                val originalLabel = fieldLabels[key] ?: "$key:"
                if (originalLabel != label) {
                    // 绘制工具提示
                    renderTooltip(context, originalLabel, mouseXPos, mouseYPos)
                }
            }
            
            val bgAlpha = (easedFieldProgress * 80).toInt().coerceIn(0, 80)
            val bgColorFinal = (fieldBgColor and 0x00FFFFFF) or (bgAlpha shl 24)
            context.fill(
                textField.x, textField.y,
                textField.x + textField.width, textField.y + textField.height,
                bgColorFinal
            )
            
            textField.render(context, mouseX, mouseY, delta)
            
            currentY += fieldHeight + padding
            fieldIndex++
        }
    }

    // 添加工具提示方法
    private fun renderTooltip(context: DrawContext, text: String, mouseX: Int, mouseY: Int) {
        val padding = 4
        val textWidth = textRenderer.getWidth(text)
        val textHeight = textRenderer.fontHeight
        
        var tooltipX = mouseX + 12
        var tooltipY = mouseY - 12
        
        // 防止超出屏幕边界
        if (tooltipX + textWidth + padding * 2 > width) {
            tooltipX = mouseX - textWidth - padding * 2 - 12
        }
        if (tooltipY + textHeight + padding * 2 > height) {
            tooltipY = mouseY - textHeight - padding * 2 - 8
        }
        if (tooltipY < 0) {
            tooltipY = mouseY + 12
        }
        
        // 绘制背景
        context.fill(
            tooltipX, tooltipY,
            tooltipX + textWidth + padding * 2,
            tooltipY + textHeight + padding * 2,
            0xDD000000.toInt()
        )
        
        // 绘制边框
        context.fill(
            tooltipX - 1, tooltipY - 1,
            tooltipX + textWidth + padding * 2 + 1,
            tooltipY + textHeight + padding * 2 + 1,
            0xFF555555.toInt()
        )
        context.fill(
            tooltipX, tooltipY,
            tooltipX + textWidth + padding * 2,
            tooltipY + textHeight + padding * 2,
            0xDD000000.toInt()
        )
        
        // 绘制文本
        context.drawText(
            textRenderer,
            text,
            tooltipX + padding,
            tooltipY + padding,
            0xFFFFFF,
            true
        )
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
        for (textField in textFields.values) {
            if (textField.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            startClosing()
            return true
        }
        
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (hasShiftDown()) {
                saveConfig()
            }
            return true
        }
        
        for (textField in textFields.values) {
            if (textField.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        for (textField in textFields.values) {
            if (textField.charTyped(chr, modifiers)) {
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
            val newConfig = mutableMapOf<String, Any>()
            
            for ((key, textField) in textFields) {
                val text = textField.text
                val originalValue = config[key]
                
                when (originalValue) {
                    is Boolean -> newConfig[key] = text.toBoolean()
                    is Int -> newConfig[key] = text.toInt()
                    is Float -> newConfig[key] = text.toFloat()
                    is Double -> newConfig[key] = text.toDouble()
                    is String -> newConfig[key] = text
                    else -> newConfig[key] = text
                }
            }
            
            module.applyConfig(newConfig)
            ConfigManager.save(module)
            
            playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
            
            startClosing()
            
        } catch (e: Exception) {
            playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        }
    }
    
    override fun close() {
        client?.setScreen(null)
    }
    
    private fun playSound(soundEvent: net.minecraft.sound.SoundEvent, volume: Float, pitch: Float) {
        val client = MinecraftClient.getInstance()
        client.soundManager.play(PositionedSoundInstance.master(soundEvent, pitch, volume))
    }
    
    override fun shouldPause() = false
}