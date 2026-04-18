package com.example.background

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.util.Random
import kotlin.math.max
import kotlin.math.pow

object CustomBackgroundManager {
    private val logger = LoggerFactory.getLogger("ecyclient-background")
    private val random = Random()
    
    private const val RESOURCE_NAMESPACE = "ecyclient"
    private const val RESOURCE_PATH = "background/"
    private const val PARALLAX_STRENGTH = 0.03f
    private const val SWITCH_INTERVAL_MS = 60000L
    
    private var textureIdentifier: Identifier? = null
    private var backgroundImage: NativeImage? = null
    private var backgroundTexture: NativeImageBackedTexture? = null
    private var imageWidth = 0
    private var imageHeight = 0
    
    private var currentOffsetX = 0f
    private var currentOffsetY = 0f
    private var initialized = false
    private var selectedImageIdentifier: Identifier? = null
    private var lastMouseX = 0f
    private var lastMouseY = 0f
    private var availableImages: List<Identifier> = emptyList()
    private var currentImageIndex = 0
    private var lastSwitchTime = 0L
    
    private fun ensureInitialized() {
        if (!initialized) { 
            initialized = true
            initialize()
        }
    }
    
    private fun initialize() {
        try {
            availableImages = scanAvailableImages()
            
            if (availableImages.isEmpty()) {
                logger.warn("没有找到自定义背景图片")
                return
            }
            
            currentImageIndex = random.nextInt(availableImages.size)
            val selectedImage = availableImages[currentImageIndex]
            logger.info("已选择背景图片: {}", selectedImage)
            
            loadBackgroundImage(selectedImage)
            lastSwitchTime = System.currentTimeMillis()
        } catch (e: Exception) {
            logger.error("加载自定义背景失败", e)
        }
    }
    
    private fun scanAvailableImages(): List<Identifier> {
        val images = mutableListOf<Identifier>()
        
        try {
            val resourceManager = MinecraftClient.getInstance().resourceManager
            val searchPath = RESOURCE_PATH.dropLast(1)
            
            logger.info("正在扫描资源路径: namespace={}, path={}", RESOURCE_NAMESPACE, searchPath)
            
            val allResources = resourceManager.findResources(searchPath) { path ->
                path.path.endsWith(".png")
            }
            
            for ((identifier, _) in allResources) {
                if (identifier.namespace == RESOURCE_NAMESPACE) {
                    images.add(identifier)
                    logger.debug("找到背景图片: {}", identifier)
                }
            }
            
            logger.info("扫描到 {} 张背景图片", images.size)
        } catch (e: Exception) {
            logger.warn("扫描背景图片时出错", e)
        }
        
        return images
    }
    
    private fun loadBackgroundImage(imageIdentifier: Identifier) {
        try {
            selectedImageIdentifier = imageIdentifier
            val textureId = Identifier.of("ecyclient", "textures/background/${imageIdentifier.path.substringAfterLast('/').substringBefore(".png")}.png")
            
            val resourceManager = MinecraftClient.getInstance().resourceManager
            val resource = resourceManager.getResource(imageIdentifier).orElse(null)
            
            if (resource == null) {
                logger.error("无法找到资源文件: {}", imageIdentifier)
                return
            }
            
            val nativeImage = resource.inputStream.use { stream ->
                NativeImage.read(stream)
            }
            
            backgroundImage = nativeImage
            imageWidth = backgroundImage!!.width
            imageHeight = backgroundImage!!.height
            
            backgroundTexture = NativeImageBackedTexture(backgroundImage)
            textureIdentifier = textureId
            
            MinecraftClient.getInstance().textureManager.registerTexture(textureId, backgroundTexture!!)
            
            logger.info("背景图片加载成功: {}x{}, 纹理ID: {}", imageWidth, imageHeight, textureIdentifier)
        } catch (e: Exception) {
            logger.error("加载背景图片失败: {}", imageIdentifier, e)
            cleanup()
        }
    }
    
    fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, screenWidth: Int, screenHeight: Int, delta: Float) {
        ensureInitialized()
        
        if (textureIdentifier == null || backgroundImage == null) {
            return
        }
        
        checkAndSwitchBackground()
        
        val targetOffsetX = (mouseX - screenWidth / 2) * PARALLAX_STRENGTH
        val targetOffsetY = (mouseY - screenHeight / 2) * PARALLAX_STRENGTH
        
        val smoothFactor = 1.0f - 0.001f.pow(delta)
        currentOffsetX += (targetOffsetX - currentOffsetX) * smoothFactor
        currentOffsetY += (targetOffsetY - currentOffsetY) * smoothFactor
        
        val scaleX = screenWidth.toFloat() / imageWidth
        val scaleY = screenHeight.toFloat() / imageHeight
        val scale = max(scaleX, scaleY)
        
        val renderWidth = imageWidth * scale
        val renderHeight = imageHeight * scale
        
        val x = (screenWidth - renderWidth) / 2f + currentOffsetX
        val y = (screenHeight - renderHeight) / 2f + currentOffsetY
        
        context.drawTexture(
            RenderLayer::getGuiTextured,
            textureIdentifier!!,
            x.toInt(),
            y.toInt(),
            0.0f,
            0.0f,
            renderWidth.toInt(),
            renderHeight.toInt(),
            renderWidth.toInt(),
            renderHeight.toInt()
        )
    }
    
    private fun checkAndSwitchBackground() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSwitchTime >= SWITCH_INTERVAL_MS && availableImages.size > 1) {
            switchToNextImage()
            lastSwitchTime = currentTime
        }
    }
    
    private fun switchToNextImage() {
        currentImageIndex = (currentImageIndex + 1) % availableImages.size
        val nextImage = availableImages[currentImageIndex]
        logger.info("切换背景图片: {}", nextImage)
        
        cleanup()
        loadBackgroundImage(nextImage)
    }
    
    fun isLoaded(): Boolean {
        ensureInitialized()
        return textureIdentifier != null
    }
    
    fun cleanup() {
        try {
            backgroundTexture?.close()
        } catch (e: Exception) {
            logger.warn("清理背景纹理时出错", e)
        }
        backgroundTexture = null
        backgroundImage = null
        textureIdentifier = null
    }
}
