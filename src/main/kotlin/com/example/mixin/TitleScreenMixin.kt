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
import net.minecraft.client.option.SimpleOption
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen
import net.minecraft.util.Util
import kotlin.math.sin
import kotlin.math.max

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
        
        val basePadding = 5
        val baseLineSpacing = 2
        val baseCornerRadius = 4
        val baseMargin = 8
        
        val padding = (basePadding * scaleFactor / 2.0f).toInt().coerceAtLeast(3)
        val lineSpacing = (baseLineSpacing * scaleFactor / 2.0f).toInt().coerceAtLeast(1)
        val textHeight = this.textRenderer.fontHeight
        val cornerRadius = (baseCornerRadius * scaleFactor / 2.0f).toInt().coerceAtLeast(2)
        val margin = (baseMargin * scaleFactor / 2.0f).toInt().coerceAtLeast(4)
        
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
        
        context.fill(
            panelX,
            panelY,
            panelX + panelWidth,
            panelY + panelHeight,
            0xCC000000.toInt()
        )
        
        drawRoundedRectOutline(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, 0x66FFFFFF.toInt())
        
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
                context.fill(
                    link.x,
                    link.y + textHeight - 1,
                    link.x + link.width,
                    link.y + textHeight,
                    displayColor
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
        val cornerRadius = 3
        
        val bgColor = when {
            isHovered -> 0x99000000.toInt()
            else -> 0x80000000.toInt()
        }
        
        val strokeColor = when {
            isHovered -> 0xFFFFFFFF.toInt()
            else -> 0xFFCCCCCC.toInt()
        }
        
        context.fill(
            x + 1,
            y + 1,
            x + width - 1,
            y + height - 1,
            bgColor
        )
        
        drawRoundedRectOutline(context, x, y, width, height, cornerRadius, strokeColor)
        
        val textWidth = this.textRenderer.getWidth(button.message)
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - this.textRenderer.fontHeight) / 2 + 1
        context.drawText(this.textRenderer, button.message, textX, textY, -1, true)
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
    
    private fun drawRoundedRectOutline(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        color: Int
    ) {
        context.fill(x + radius, y, x + width - radius, y + 1, color)
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color)
        
        context.fill(x, y + radius, x + 1, y + height - radius, color)
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color)
        
        context.fill(x + 1, y, x + radius, y + 1, color)
        context.fill(x + width - radius, y, x + width - 1, y + 1, color)
        context.fill(x + 1, y + height - 1, x + radius, y + height, color)
        context.fill(x + width - radius, y + height - 1, x + width - 1, y + height, color)
    }
}
