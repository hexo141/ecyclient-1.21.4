package com.example.effect

import com.example.module.ModuleState
import com.example.module.render.MotionBlur
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.RenderPhase
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30

object MotionBlurEffect {
    private var previousFrame: SimpleFramebuffer? = null
    private var isInitialized = false

    fun init() {
        if (isInitialized) return
        val client = MinecraftClient.getInstance()
        val fbWidth = client.window.framebufferWidth
        val fbHeight = client.window.framebufferHeight
        previousFrame = SimpleFramebuffer(fbWidth, fbHeight, true)
        previousFrame!!.setClearColor(0f, 0f, 0f, 0f)
        previousFrame!!.clear()
        MinecraftClient.getInstance().framebuffer.beginWrite(false)
        isInitialized = true
    }

    fun resize() {
        val client = MinecraftClient.getInstance()
        val fbWidth = client.window.framebufferWidth
        val fbHeight = client.window.framebufferHeight
        if (previousFrame != null) {
            previousFrame!!.resize(fbWidth, fbHeight)
        }
    }

    fun cleanup() {
        previousFrame?.delete()
        previousFrame = null
        isInitialized = false
    }

    fun onBeforeRender() {
        if (MotionBlur.state != ModuleState.ENABLED) return
        if (!isInitialized) init()
        resize()
    }

    fun onAfterRender() {
        if (MotionBlur.state != ModuleState.ENABLED) return
        if (!isInitialized) return

        val client = MinecraftClient.getInstance()
        val mainFb = client.framebuffer
        val strength = MotionBlur.strength

        val fbWidth = mainFb.textureWidth
        val fbHeight = mainFb.textureHeight

        if (previousFrame == null) return

        val mainFboId = mainFb.fbo
        val prevFboId = previousFrame!!.fbo
        val prevTexture = previousFrame!!.colorAttachment

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFboId)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFboId)
        GL30.glBlitFramebuffer(0, 0, fbWidth, fbHeight, 0, 0, fbWidth, fbHeight, GL30.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST)

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainFboId)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val alpha = (255 * (1.0f - strength)).toInt().coerceIn(0, 255)

        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(0f, fbHeight.toFloat(), 0f).texture(0f, 1f).color(255, 255, 255, alpha)
        buffer.vertex(fbWidth.toFloat(), fbHeight.toFloat(), 0f).texture(1f, 1f).color(255, 255, 255, alpha)
        buffer.vertex(fbWidth.toFloat(), 0f, 0f).texture(1f, 0f).color(255, 255, 255, alpha)
        buffer.vertex(0f, 0f, 0f).texture(0f, 0f).color(255, 255, 255, alpha)

        val builtBuffer = buffer.end()

        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture)

        BufferRenderer.drawWithGlobalProgram(builtBuffer)

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        GL11.glDisable(GL11.GL_BLEND)
    }
}
