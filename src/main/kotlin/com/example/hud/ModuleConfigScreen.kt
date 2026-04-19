package com.example.hud

import com.example.module.ModuleManager
import com.example.module.config.ConfigPersistence
import com.example.module.impl.AutoWaterPlace
import com.google.gson.JsonObject
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class ModuleConfigScreen(
    private val moduleId: String,
    private val previousScreen: Screen?
) : Screen(Text.translatable("ecyclient.config")) {
    
    private val windowWidth = 300
    private val titleBarHeight = 16
    private val itemHeight = 20
    private val backgroundColor = 0xBB000000.toInt()
    private val titleBarColor = 0xDD222222.toInt()
    private val textColor = 0xFFFFFFFF.toInt()
    private val titleTextColor = 0xFFAAAAAA.toInt()
    private val buttonColor = 0xFF444444.toInt()
    private val buttonHoverColor = 0xFF666666.toInt()
    
    private data class ConfigItem(
        val name: String,
        val displayName: String,
        val type: String,
        var floatValue: Float = 0f,
        var doubleValue: Double = 0.0,
        var intValue: Int = 0,
        var booleanValue: Boolean = false,
        var stringValue: String = "",
        val minValue: Number? = null,
        val maxValue: Number? = null,
        val step: Number? = null
    )
    
    private data class EditButton(
        var x: Int = 0,
        var y: Int = 0,
        var width: Int = 0,
        var height: Int = 0,
        val label: String,
        val action: () -> Unit
    )
    
    private val configItems = mutableListOf<ConfigItem>()
    private val editButtons = mutableListOf<EditButton>()
    private var windowX = 0
    private var windowY = 0
    private var scrollOffset = 0
    private var isDragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    
    override fun init() {
        super.init()
        windowX = width / 2 - windowWidth / 2
        windowY = height / 4
        loadConfigItems()
    }
    
    private fun loadConfigItems() {
        configItems.clear()
        editButtons.clear()
        
        val savedConfig = ConfigPersistence.loadConfig(moduleId)
        
        when (moduleId) {
            "auto_water_place" -> {
                val config = AutoWaterPlace.getConfig()
                val minFallDistance = savedConfig?.get("minFallDistance")?.asFloat ?: (config["minFallDistance"] as Float)
                val placeDistance = savedConfig?.get("placeDistance")?.asDouble ?: (config["placeDistance"] as Double)
                val maxGroundDistance = savedConfig?.get("maxGroundDistance")?.asDouble ?: (config["maxGroundDistance"] as Double)
                val cooldownTime = savedConfig?.get("cooldownTime")?.asInt ?: (config["cooldownTime"] as Int)
                val requireWaterBucket = savedConfig?.get("requireWaterBucket")?.asBoolean ?: (config["requireWaterBucket"] as Boolean)
                
                configItems.add(ConfigItem(
                    name = "minFallDistance",
                    displayName = "Min Fall Distance",
                    type = "float",
                    floatValue = minFallDistance,
                    minValue = 1.0f,
                    maxValue = 100.0f,
                    step = 0.5f
                ))
                
                configItems.add(ConfigItem(
                    name = "placeDistance",
                    displayName = "Place Distance",
                    type = "double",
                    doubleValue = placeDistance,
                    minValue = 0.5,
                    maxValue = 20.0,
                    step = 0.5
                ))
                
                configItems.add(ConfigItem(
                    name = "maxGroundDistance",
                    displayName = "Max Ground Distance",
                    type = "double",
                    doubleValue = maxGroundDistance,
                    minValue = 1.0,
                    maxValue = 50.0,
                    step = 1.0
                ))
                
                configItems.add(ConfigItem(
                    name = "cooldownTime",
                    displayName = "Cooldown Time",
                    type = "int",
                    intValue = cooldownTime,
                    minValue = 0,
                    maxValue = 200,
                    step = 1
                ))
                
                configItems.add(ConfigItem(
                    name = "requireWaterBucket",
                    displayName = "Require Water Bucket",
                    type = "boolean",
                    booleanValue = requireWaterBucket
                ))
            }
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x80000000.toInt())
        renderWindow(context, mouseX, mouseY)
    }
    
    private fun renderWindow(context: DrawContext, mouseX: Int, mouseY: Int) {
        val windowHeight = titleBarHeight + configItems.size * itemHeight + 30
        val visibleHeight = Math.min(windowHeight, height - windowY - 10)
        
        context.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY + visibleHeight + 1, 0xFF000000.toInt())
        context.fill(windowX, windowY, windowX + windowWidth, windowY + titleBarHeight, titleBarColor)
        
        val moduleName = ModuleManager.getModuleMetadata(moduleId)?.name ?: "Unknown"
        context.drawText(textRenderer, "Config: $moduleName", windowX + 4, windowY + (titleBarHeight - textRenderer.fontHeight) / 2, titleTextColor, true)
        
        var y = windowY + titleBarHeight
        for ((index, item) in configItems.withIndex()) {
            if (y + itemHeight > windowY + visibleHeight) break
            
            val isHovered = mouseX >= windowX && mouseX <= windowX + windowWidth &&
                           mouseY >= y && mouseY <= y + itemHeight
            
            if (isHovered) {
                context.fill(windowX, y, windowX + windowWidth, y + itemHeight, 0x33FFFFFF.toInt())
            }
            
            val labelY = y + (itemHeight - textRenderer.fontHeight) / 2
            context.drawText(textRenderer, item.displayName, windowX + 4, labelY, textColor, true)
            
            when (item.type) {
                "float", "double", "int" -> {
                    val valueText = when (item.type) {
                        "float" -> String.format("%.1f", item.floatValue)
                        "double" -> String.format("%.1f", item.doubleValue)
                        "int" -> item.intValue.toString()
                        else -> ""
                    }
                    
                    val textWidth = textRenderer.getWidth(valueText)
                    context.drawText(textRenderer, valueText, windowX + windowWidth - textWidth - 60, labelY, 0xFF88FF88.toInt(), true)
                    
                    val btnMinus = EditButton(
                        x = windowX + windowWidth - 50,
                        y = y + 3,
                        width = 20,
                        height = itemHeight - 6,
                        label = "-",
                        action = {
                            when (item.type) {
                                "float" -> item.floatValue = (item.floatValue - (item.step?.toFloat() ?: 0.5f))
                                    .coerceIn((item.minValue?.toFloat() ?: 0f), (item.maxValue?.toFloat() ?: 100f))
                                "double" -> item.doubleValue = (item.doubleValue - (item.step?.toDouble() ?: 0.5))
                                    .coerceIn((item.minValue?.toDouble() ?: 0.0), (item.maxValue?.toDouble() ?: 100.0))
                                "int" -> item.intValue = (item.intValue - (item.step?.toInt() ?: 1))
                                    .coerceIn((item.minValue?.toInt() ?: 0), (item.maxValue?.toInt() ?: 100))
                            }
                            saveConfig()
                        }
                    )
                    
                    val btnPlus = EditButton(
                        x = windowX + windowWidth - 25,
                        y = y + 3,
                        width = 20,
                        height = itemHeight - 6,
                        label = "+",
                        action = {
                            when (item.type) {
                                "float" -> item.floatValue = (item.floatValue + (item.step?.toFloat() ?: 0.5f))
                                    .coerceIn((item.minValue?.toFloat() ?: 0f), (item.maxValue?.toFloat() ?: 100f))
                                "double" -> item.doubleValue = (item.doubleValue + (item.step?.toDouble() ?: 0.5))
                                    .coerceIn((item.minValue?.toDouble() ?: 0.0), (item.maxValue?.toDouble() ?: 100.0))
                                "int" -> item.intValue = (item.intValue + (item.step?.toInt() ?: 1))
                                    .coerceIn((item.minValue?.toInt() ?: 0), (item.maxValue?.toInt() ?: 100))
                            }
                            saveConfig()
                        }
                    )
                    
                    editButtons.add(btnMinus)
                    editButtons.add(btnPlus)
                    
                    val minusHovered = isButtonHovered(btnMinus, mouseX, mouseY)
                    val plusHovered = isButtonHovered(btnPlus, mouseX, mouseY)
                    
                    context.fill(btnMinus.x, btnMinus.y, btnMinus.x + btnMinus.width, btnMinus.y + btnMinus.height,
                        if (minusHovered) buttonHoverColor else buttonColor)
                    context.fill(btnPlus.x, btnPlus.y, btnPlus.x + btnPlus.width, btnPlus.y + btnPlus.height,
                        if (plusHovered) buttonHoverColor else buttonColor)
                    
                    context.drawText(textRenderer, "-", btnMinus.x + 7, btnMinus.y + 3, textColor, true)
                    context.drawText(textRenderer, "+", btnPlus.x + 6, btnPlus.y + 3, textColor, true)
                }
                "boolean" -> {
                    val valueText = if (item.booleanValue) "ON" else "OFF"
                    val valueColor = if (item.booleanValue) 0xFF00FF00.toInt() else 0xFFFF0000.toInt()
                    val textWidth = textRenderer.getWidth(valueText)
                    context.drawText(textRenderer, valueText, windowX + windowWidth - textWidth - 8, labelY, valueColor, true)
                }
            }
            
            y += itemHeight
        }
        
        val closeBtnY = y + 5
        val closeBtnHeight = 20
        val closeBtnHovered = mouseX >= windowX + 100 && mouseX <= windowX + 200 &&
                             mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnHeight
        
        context.fill(windowX + 100, closeBtnY, windowX + 200, closeBtnY + closeBtnHeight,
            if (closeBtnHovered) 0xFF555555.toInt() else 0xFF333333.toInt())
        val closeText = "Close"
        val closeTextWidth = textRenderer.getWidth(closeText)
        context.drawText(textRenderer, closeText, windowX + 100 + (100 - closeTextWidth) / 2, closeBtnY + 6, textColor, true)
        
        val resetBtnY = closeBtnY + 25
        val resetBtnHeight = 20
        val resetBtnHovered = mouseX >= windowX + 100 && mouseX <= windowX + 200 &&
                             mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnHeight
        
        context.fill(windowX + 100, resetBtnY, windowX + 200, resetBtnY + resetBtnHeight,
            if (resetBtnHovered) 0xFF775555.toInt() else 0xFF553333.toInt())
        val resetText = "Reset"
        val resetTextWidth = textRenderer.getWidth(resetText)
        context.drawText(textRenderer, resetText, windowX + 100 + (100 - resetTextWidth) / 2, resetBtnY + 6, textColor, true)
    }
    
    private fun isButtonHovered(button: EditButton, mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= button.x && mouseX <= button.x + button.width &&
               mouseY >= button.y && mouseY <= button.y + button.height
    }
    
    private fun saveConfig() {
        val configValues = configItems.map { item ->
            ConfigPersistence.ConfigValue(
                name = item.name,
                displayName = item.displayName,
                type = item.type,
                value = when (item.type) {
                    "float" -> item.floatValue
                    "double" -> item.doubleValue
                    "int" -> item.intValue
                    "boolean" -> item.booleanValue
                    "string" -> item.stringValue
                    else -> item.stringValue
                },
                minValue = item.minValue,
                maxValue = item.maxValue,
                step = item.step
            )
        }
        ConfigPersistence.saveConfig(moduleId, configValues)
        applyConfig()
    }
    
    private fun applyConfig() {
        when (moduleId) {
            "auto_water_place" -> {
                val minFallDistance = configItems.find { it.name == "minFallDistance" }?.floatValue ?: 3f
                val placeDistance = configItems.find { it.name == "placeDistance" }?.doubleValue ?: 2.0
                val maxGroundDistance = configItems.find { it.name == "maxGroundDistance" }?.doubleValue ?: 10.0
                val cooldownTime = configItems.find { it.name == "cooldownTime" }?.intValue ?: 20
                val requireWaterBucket = configItems.find { it.name == "requireWaterBucket" }?.booleanValue ?: true
                
                AutoWaterPlace.configure(
                    minFallDistance = minFallDistance,
                    placeDistance = placeDistance,
                    maxGroundDistance = maxGroundDistance,
                    cooldownTime = cooldownTime,
                    requireWaterBucket = requireWaterBucket
                )
            }
        }
    }
    
    private fun resetConfig() {
        when (moduleId) {
            "auto_water_place" -> {
                AutoWaterPlace.configure()
                loadConfigItems()
                ConfigPersistence.getConfigFile(moduleId).delete()
            }
        }
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val mx = mouseX.toInt()
            val my = mouseY.toInt()
            
            if (mx >= windowX && mx <= windowX + windowWidth &&
                my >= windowY && my <= windowY + titleBarHeight) {
                isDragging = true
                dragOffsetX = mx - windowX
                dragOffsetY = my - windowY
                return true
            }
            
            val windowHeight = titleBarHeight + configItems.size * itemHeight + 55
            val closeBtnY = windowY + titleBarHeight + configItems.size * itemHeight + 5
            val resetBtnY = closeBtnY + 25
            
            if (mx >= windowX + 100 && mx <= windowX + 200) {
                if (my >= closeBtnY && my <= closeBtnY + 20) {
                    close()
                    return true
                }
                if (my >= resetBtnY && my <= resetBtnY + 20) {
                    resetConfig()
                    return true
                }
            }
            
            for (btn in editButtons) {
                if (mx >= btn.x && mx <= btn.x + btn.width &&
                    my >= btn.y && my <= btn.y + btn.height) {
                    btn.action()
                    return true
                }
            }
            
            for ((index, item) in configItems.withIndex()) {
                val itemY = windowY + titleBarHeight + index * itemHeight
                if (mx >= windowX && mx <= windowX + windowWidth &&
                    my >= itemY && my <= itemY + itemHeight) {
                    if (item.type == "boolean") {
                        item.booleanValue = !item.booleanValue
                        saveConfig()
                        return true
                    }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = false
        return super.mouseReleased(mouseX, mouseY, button)
    }
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (isDragging && button == 0) {
            windowX = mouseX.toInt() - dragOffsetX
            windowY = mouseY.toInt() - dragOffsetY
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun shouldPause() = false
}
