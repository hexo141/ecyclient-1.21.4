package com.example.module.example

import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object ExampleModule : GameModule {
    private val logger = LoggerFactory.getLogger("ecyclient-example")
    
    override val metadata = ModuleMetadata(
        id = "example_module",
        name = "示例模块",
        description = "这是一个用于测试模块系统的示例模块",
        category = ModuleCategory.MISC
    )
    
    override var state: ModuleState = ModuleState.DISABLED
    
    private var tickCount = 0
    
    override fun onEnable() {
        logger.info("示例模块已启用")
        tickCount = 0
    }
    
    override fun onDisable() {
        logger.info("示例模块已禁用")
        tickCount = 0
    }
    
    override fun onTick() {
        tickCount++
        
        // 每100tick输出一次调试信息
        if (tickCount % 100 == 0) {
            val client = MinecraftClient.getInstance()
            if (client.world != null && client.player != null) {
                client.player?.sendMessage(Text.literal("§a示例模块运行中 - tick: $tickCount"), false)
            }
        }
    }
    
    override fun onRenderWorld(context: DrawContext, tickDelta: Float) {
        val client = MinecraftClient.getInstance()
        val player = client.player
        
        if (player == null || client.world == null) return
        
        // 在玩家头顶显示模块状态信息
        val text = Text.literal("§6示例模块 §a运行中")
        val textRenderer = client.textRenderer
        val textWidth = textRenderer.getWidth(text)
        
        val playerPos = player.pos
        val camera = client.gameRenderer.camera
        
        // 计算屏幕位置
        val x = (client.window.scaledWidth - textWidth) / 2
        val y = client.window.scaledHeight / 4
        
        // 绘制文本背景
        context.fill(x - 2, y - 2, x + textWidth + 2, y + textRenderer.fontHeight + 2, 0x80000000.toInt())
        
        // 绘制文本
        context.drawText(textRenderer, text, x, y, 0xFFFFFF, true)
    }
}