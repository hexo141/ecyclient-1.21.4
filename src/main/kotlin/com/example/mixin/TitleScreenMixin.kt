package com.example.mixin

import com.example.background.CustomBackgroundManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen
import net.minecraft.util.Util
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.min

@Mixin(TitleScreen::class)
abstract class TitleScreenMixin : Screen(Text.empty()) {
    
    private var animationTime = 0f
    
    private data class LinkInfo(
        val label: String,
        val url: String,
        val color: Int,
        var x: Int = 0,
        var y: Int = 0,
        var width: Int = 0,
        var height: Int = 0
    )
    
    private val links = mutableListOf<LinkInfo>()
    
    @Inject(method = ["renderBackground"], at = [At("HEAD")], cancellable = true)
    private fun onRenderBackground(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        cir: CallbackInfo
    ) {
        if (CustomBackgroundManager.isLoaded()) {
            CustomBackgroundManager.renderBackground(
                context,
                mouseX,
                mouseY,
                this.width,
                this.height,
                delta
            )
            cir.cancel()
        }
    }
    
    @Inject(method = ["init"], at = [At("RETURN")])
    private fun onInit(ci: CallbackInfo) {
        val client = MinecraftClient.getInstance()
        val buttonWidth = 200
        val buttonHeight = 20
        val verticalSpacing = 24
        val startX = this.width / 2 - buttonWidth / 2
        
        val buttons = mutableListOf<ButtonWidget>()
        
        val singleplayerButton = ButtonWidget.builder(Text.translatable("menu.singleplayer")) { button ->
            client.setScreen(SelectWorldScreen(this))
        }.dimensions(startX, 96, buttonWidth, buttonHeight).build()
        
        val multiplayerButton = ButtonWidget.builder(Text.translatable("menu.multiplayer")) { button ->
            client.setScreen(MultiplayerScreen(this))
        }.dimensions(startX, 96 + verticalSpacing, buttonWidth, buttonHeight).build()
        
        val languageButton = ButtonWidget.builder(Text.translatable("options.language")) { button ->
            client.setScreen(LanguageOptionsScreen(this, client.options, client.languageManager))
        }.dimensions(startX, 96 + verticalSpacing * 2, buttonWidth, buttonHeight).build()
        
        val optionsButton = ButtonWidget.builder(Text.translatable("menu.options")) { button ->
            client.setScreen(OptionsScreen(this, client.options))
        }.dimensions(startX, 96 + verticalSpacing * 3, buttonWidth, buttonHeight).build()
        
        val quitButton = ButtonWidget.builder(Text.translatable("menu.quit")) { button ->
            client.scheduleStop()
        }.dimensions(startX, 96 + verticalSpacing * 4, buttonWidth, buttonHeight).build()
        
        buttons.addAll(listOf(
            singleplayerButton,
            multiplayerButton,
            languageButton,
            optionsButton,
            quitButton
        ))
        
        val existingButtons = this.children().filterIsInstance<ButtonWidget>().toList()
        existingButtons.forEach { it -> this.remove(it) }
        
        buttons.forEach { button ->
            this.addDrawableChild(button)
        }
    }
    
    @Inject(method = ["render"], at = [At("HEAD")], cancellable = true)
    private fun onRender(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        ci: CallbackInfo
    ) {
        this.renderBackground(context, mouseX, mouseY, delta)
        
        this.children().filterIsInstance<ButtonWidget>().forEach { it.visible = false }
        super.render(context, mouseX, mouseY, delta)
        this.children().filterIsInstance<ButtonWidget>().forEach { it.visible = true }
        
        val hoveredButton = this.children().filterIsInstance<ButtonWidget>().find { button ->
            mouseX >= button.x && mouseX <= button.x + button.width && 
            mouseY >= button.y && mouseY <= button.y + button.height
        }
        
        this.children().forEach { child ->
            if (child is ButtonWidget) {
                renderCustomButtonStyle(context, child, mouseX, mouseY, child == hoveredButton)
            }
        }
        
        renderECYclientText(context, mouseX, mouseY, delta)
        
        renderInfoPanel(context, mouseX, mouseY)
        
        ci.cancel()
    }
    
    @Inject(method = ["mouseClicked"], at = [At("HEAD")], cancellable = true)
    private fun onMouseClicked(mouseX: Double, mouseY: Double, button: Int, cir: CallbackInfoReturnable<Boolean>) {
        for (link in links) {
            if (mouseX >= link.x && mouseX <= link.x + link.width &&
                mouseY >= link.y && mouseY <= link.y + link.height) {
                Util.getOperatingSystem().open(java.net.URI(link.url))
                cir.setReturnValue(true)
                return
            }
        }
    }
    
    private fun renderInfoPanel(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int
    ) {
        val client = MinecraftClient.getInstance()
        val guiScale = client.options.guiScale.value
        val scaleFactor = if (guiScale > 0) guiScale.toFloat() else 2.0f
        
        val basePadding = 8
        val baseLineSpacing = 2
        val baseMargin = 8
        val cornerRadius = 10
        
        val padding = (basePadding * scaleFactor / 2.0f).toInt().coerceAtLeast(6)
        val lineSpacing = (baseLineSpacing * scaleFactor / 2.0f).toInt().coerceAtLeast(1)
        val textHeight = this.textRenderer.fontHeight
        val margin = (baseMargin * scaleFactor / 2.0f).toInt().coerceAtLeast(6)
        
        links.clear()
        
        val thankYouText = "Thank you for using ECYclient!"
        val messageText = "This client is still improving. We kindly ask for your acceptance."
        
        links.add(LinkInfo("View bilibili", "https://space.bilibili.com/3494353628564285", 0xFF6190FF.toInt()))
        links.add(LinkInfo("View github", "https://github.com/hexo141", 0xFFFFFFFF.toInt()))
        
        var maxWidth = 0
        maxWidth = max(maxWidth, this.textRenderer.getWidth(thankYouText))
        maxWidth = max(maxWidth, this.textRenderer.getWidth(messageText))
        
        for (link in links) {
            val linkText = "${link.label}: ${link.url}"
            maxWidth = max(maxWidth, this.textRenderer.getWidth(linkText))
        }
        
        val panelWidth = maxWidth + padding * 2
        val panelHeight = (textHeight + lineSpacing) * 4 + padding * 2 - lineSpacing
        
        val panelX = margin
        val panelY = this.height - panelHeight - margin
        
        // 圆角背景
        drawRoundedRect(
            context, 
            panelX, 
            panelY, 
            panelWidth, 
            panelHeight, 
            0xDD1A1A1A.toInt(),
            cornerRadius
        )
        
        // 圆角边框
        drawRoundedRectOutline(
            context, 
            panelX, 
            panelY, 
            panelWidth, 
            panelHeight, 
            0x66FFFFFF.toInt(),
            cornerRadius
        )
        
        var currentY = panelY + padding
        
        context.drawText(this.textRenderer, thankYouText, panelX + padding, currentY, 0xFF4ADE80.toInt(), true)
        currentY += textHeight + lineSpacing
        
        context.drawText(this.textRenderer, messageText, panelX + padding, currentY, 0xFFFBBF24.toInt(), true)
        currentY += textHeight + lineSpacing + (lineSpacing / 2).coerceAtLeast(1)
        
        for (link in links) {
            val linkText = "${link.label}: ${link.url}"
            link.x = panelX + padding
            link.y = currentY
            link.width = this.textRenderer.getWidth(linkText)
            link.height = textHeight
            
            val isHovered = mouseX >= link.x && mouseX <= link.x + link.width &&
                           mouseY >= link.y && mouseY <= link.y + link.height
            
            val displayColor = if (isHovered) 0xFF60A5FA.toInt() else link.color
            context.drawText(this.textRenderer, linkText, link.x, link.y, displayColor, true)
            
            if (isHovered) {
                drawRoundedRect(
                    context,
                    link.x,
                    link.y + textHeight - 1,
                    link.width,
                    2,
                    displayColor,
                    1
                )
            }
            
            currentY += textHeight + lineSpacing
        }
    }
    
    private fun renderCustomButtonStyle(
        context: DrawContext,
        button: ButtonWidget,
        mouseX: Int,
        mouseY: Int,
        isHovered: Boolean
    ) {
        val x = button.x
        val y = button.y
        val width = button.width
        val height = button.height
        val cornerRadius = 8
        
        val bgColor = when {
            isHovered -> 0xCC2A2A2A.toInt()
            else -> 0xAA1A1A1A.toInt()
        }
        
        val strokeColor = when {
            isHovered -> 0xFFFFFFFF.toInt()
            else -> 0x66CCCCCC.toInt()
        }
        
        drawRoundedRect(context, x, y, width, height, bgColor, cornerRadius)
        drawRoundedRectOutline(context, x, y, width, height, strokeColor, cornerRadius)
        
        val textWidth = this.textRenderer.getWidth(button.message)
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - this.textRenderer.fontHeight) / 2 + 1
        
        val textColor = if (isHovered) 0xFFFFFFFF.toInt() else 0xFFDDDDDD.toInt()
        context.drawText(this.textRenderer, button.message, textX, textY, textColor, true)
    }
    
    private fun renderECYclientText(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        animationTime += delta * 20f
        
        val text = "ECYclient"
        val centerX = this.width / 2f
        val targetY = 50f
        val scale = 3.0f
        
        var totalWidth = 0f
        for (i in text.indices) {
            totalWidth += this.textRenderer.getWidth(text[i].toString()) * scale
        }
        
        var currentX = centerX - totalWidth / 2f
        
        for (i in text.indices) {
            val colorOffset = i * 40f
            val hue = (animationTime + colorOffset) % 360f
            val color = hsbToRgb(hue, 1.0f, 1.0f)
            
            val waveY = sin((animationTime / 30f) + i * 0.6f) * 5f
            
            val charWidth = this.textRenderer.getWidth(text[i].toString()) * scale
            
            context.matrices.push()
            context.matrices.translate(currentX, targetY + waveY, 0f)
            context.matrices.scale(scale, scale, 1f)
            context.drawText(this.textRenderer, text[i].toString(), 0, 0, color, true)
            context.matrices.pop()
            
            currentX += charWidth
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
        val p = b * (1 - s)
        val q = b * (1 - s * f)
        val t = b * (1 - s * (1 - f))
        
        when (i) {
            0 -> { r = b; g = t; blue = p }
            1 -> { r = q; g = b; blue = p }
            2 -> { r = p; g = b; blue = t }
            3 -> { r = p; g = q; blue = b }
            4 -> { r = t; g = p; blue = b }
            else -> { r = b; g = p; blue = q }
        }
        
        val red = (r * 255).toInt() and 0xFF
        val green = (g * 255).toInt() and 0xFF
        val blueInt = (blue * 255).toInt() and 0xFF
        
        return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blueInt
    }
    
    /**
     * 绘制圆角矩形（实心，无重叠）
     */
    private fun drawRoundedRect(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        color: Int,
        radius: Int
    ) {
        val r = radius.coerceAtMost(minOf(width / 2, height / 2))
        
        // 中央矩形（不覆盖四角区域）
        context.fill(x + r, y + r, x + width - r, y + height - r, color)
        
        // 上边条（左右各减去圆角半径）
        context.fill(x + r, y, x + width - r, y + r, color)
        
        // 下边条
        context.fill(x + r, y + height - r, x + width - r, y + height, color)
        
        // 左边条
        context.fill(x, y + r, x + r, y + height - r, color)
        
        // 右边条
        context.fill(x + width - r, y + r, x + width, y + height - r, color)
        
        // 四个圆角（绘制四分之一实心圆）
        drawQuarterCircle(context, x + r - 1, y + r - 1, r, color, 0)      // 左上
        drawQuarterCircle(context, x + width - r, y + r - 1, r, color, 1)  // 右上
        drawQuarterCircle(context, x + r - 1, y + height - r, r, color, 2) // 左下
        drawQuarterCircle(context, x + width - r, y + height - r, r, color, 3) // 右下
    }
    
    /**
     * 绘制圆角矩形边框（无重叠）
     */
    private fun drawRoundedRectOutline(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        color: Int,
        radius: Int
    ) {
        val r = radius.coerceAtMost(minOf(width / 2, height / 2))
        val thickness = 1
        
        // 上边（不包含圆角部分）
        context.fill(x + r, y, x + width - r, y + thickness, color)
        
        // 下边
        context.fill(x + r, y + height - thickness, x + width - r, y + height, color)
        
        // 左边
        context.fill(x, y + r, x + thickness, y + height - r, color)
        
        // 右边
        context.fill(x + width - thickness, y + r, x + width, y + height - r, color)
        
        // 四个圆角边框
        drawQuarterCircleOutline(context, x + r - 1, y + r - 1, r, color, thickness, 0)      // 左上
        drawQuarterCircleOutline(context, x + width - r, y + r - 1, r, color, thickness, 1)  // 右上
        drawQuarterCircleOutline(context, x + r - 1, y + height - r, r, color, thickness, 2) // 左下
        drawQuarterCircleOutline(context, x + width - r, y + height - r, r, color, thickness, 3) // 右下
    }
    
    /**
     * 绘制四分之一实心圆
     * corner: 0=左上, 1=右上, 2=左下, 3=右下
     */
    private fun drawQuarterCircle(context: DrawContext, cx: Int, cy: Int, r: Int, color: Int, corner: Int) {
        val rSq = r * r
        for (i in 0 until r) {
            for (j in 0 until r) {
                if (i * i + j * j <= rSq) {
                    val dx = when (corner) {
                        0 -> -i
                        1 -> i
                        2 -> -i
                        3 -> i
                        else -> 0
                    }
                    val dy = when (corner) {
                        0 -> -j
                        1 -> -j
                        2 -> j
                        3 -> j
                        else -> 0
                    }
                    context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color)
                }
            }
        }
    }
    
    /**
     * 绘制四分之一圆边框
     * thickness: 边框粗细（像素）
     */
    private fun drawQuarterCircleOutline(
        context: DrawContext,
        cx: Int,
        cy: Int,
        r: Int,
        color: Int,
        thickness: Int,
        corner: Int
    ) {
        val rSq = r * r
        val innerRSq = (r - thickness) * (r - thickness)
        for (i in 0 until r) {
            for (j in 0 until r) {
                val distSq = i * i + j * j
                if (distSq <= rSq && distSq >= innerRSq) {
                    val dx = when (corner) {
                        0 -> -i
                        1 -> i
                        2 -> -i
                        3 -> i
                        else -> 0
                    }
                    val dy = when (corner) {
                        0 -> -j
                        1 -> -j
                        2 -> j
                        3 -> j
                        else -> 0
                    }
                    context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color)
                }
            }
        }
    }
}