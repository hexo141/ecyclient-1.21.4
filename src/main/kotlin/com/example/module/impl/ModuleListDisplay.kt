package com.example.module.impl


import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import com.example.module.ModuleManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

object ModuleListDisplay : GameModule {
    override val metadata = ModuleMetadata(
        id = "module_list_display",
        name = "Module List Display",
        version = "1.0.0",
        description = "Displays active modules in the top-right corner",
        enabled = true,
        category = ModuleCategory.DISPLAY
    )

    override var state: ModuleState = ModuleState.DISABLED

    private val client: MinecraftClient get() = MinecraftClient.getInstance()

    override fun onEnable() {
    }

    override fun onDisable() {
    }

    override fun onTick() {
    }

    private var animationTime = 0f

    fun render(context: DrawContext, delta: Float) {
        if (state != ModuleState.LOADED) return
        
        animationTime += delta * 20f
        
        val client = MinecraftClient.getInstance()
        
        val enabledModules = ModuleManager.getAllModules()
            .filter { it.state == ModuleState.LOADED && it.metadata.id != metadata.id }
            .map { it.metadata.name }
        
        if (enabledModules.isEmpty()) return
        
        val textRenderer = client.textRenderer
        val padding = 4
        val lineHeight = textRenderer.fontHeight + 2
        val rightPadding = 4
        val blurAmount = 6
        
        val maxWidth = enabledModules.maxOfOrNull { textRenderer.getWidth(it) } ?: 80
        val panelWidth = maxWidth + padding * 2
        val panelHeight = enabledModules.size * lineHeight + padding * 2
        
        val panelX = client.window.scaledWidth - panelWidth - rightPadding
        val panelY = padding - padding / 2
        
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xAA000000.toInt())
        
        for (x in panelX until panelX + panelWidth step blurAmount) {
            for (y in panelY until panelY + panelHeight step blurAmount) {
                context.fill(x, y, x + blurAmount, y + blurAmount, 0x30000000.toInt())
            }
        }
        
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x20000000.toInt())
        
        var yPos = panelY + padding
        
        for (index in enabledModules.indices) {
            val moduleName = enabledModules[index]
            
            val colorOffset = index * 40f
            val hue = (animationTime + colorOffset) % 360f
            val color = hsbToRgb(hue, 1.0f, 1.0f)
            
            val moduleWidth = textRenderer.getWidth(moduleName)
            val xPos = client.window.scaledWidth - moduleWidth - rightPadding
            
            context.drawText(
                textRenderer,
                Text.literal(moduleName),
                xPos,
                yPos,
                color,
                true
            )
            
            yPos += lineHeight
        }
    }
    
    private fun hsbToRgb(h: Float, s: Float, b: Float): Int {
        val r: Float
        val g: Float
        val blue: Float
        
        if (s == 0f) {
            val v = (b * 255).toInt()
            return 0xFF000000.toInt() or (v shl 16) or (v shl 8) or v
        }
        
        val h1 = ((h % 360f) + 360f) % 360f / 60f
        val i = h1.toInt()
        val f = h1 - i
        val p = b * (1f - s)
        val q = b * (1f - s * f)
        val t = b * (1f - s * (1f - f))
        
        when (i % 6) {
            0 -> { r = b; g = t; blue = p }
            1 -> { r = q; g = b; blue = p }
            2 -> { r = p; g = b; blue = t }
            3 -> { r = p; g = q; blue = b }
            4 -> { r = t; g = p; blue = b }
            5 -> { r = b; g = p; blue = q }
            else -> { r = b; g = t; blue = p }
        }
        
        val red = (r * 255).toInt().coerceIn(0, 255)
        val green = (g * 255).toInt().coerceIn(0, 255)
        val blueInt = (blue * 255).toInt().coerceIn(0, 255)
        
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blueInt
    }
}
