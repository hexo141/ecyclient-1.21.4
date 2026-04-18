package com.example.splash

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean

object SplashVideoPlayer {
    private val logger = LoggerFactory.getLogger("SplashVideoPlayer")
    private var mediaPlayer: MediaPlayer? = null
    private var stage: Stage? = null
    private var isPlaying = false
    private var logLabel: Label? = null
    private var statsLabel: Label? = null
    private var statsThread: Thread? = null
    private val recentLogs = mutableListOf<String>()

    @JvmStatic
    fun playVideo() {
        // 立即启动 JavaFX 平台并播放视频
        try {
            Platform.startup {
                initAndPlay()
            }
        } catch (e: IllegalStateException) {
            // 如果平台已经启动，直接播放
            Platform.runLater { initAndPlay() }
        }
    }

    @JvmStatic
    fun updateLog(message: String) {
        // 只有在 JavaFX 平台已初始化且标签存在时才更新
        try {
            if (logLabel != null) {
                Platform.runLater {
                    // 保存最近的日志，最多2条
                    recentLogs.add(message)
                    if (recentLogs.size > 2) {
                        recentLogs.removeAt(0)
                    }
                    logLabel?.text = recentLogs.joinToString("\n")
                }
            }
        } catch (e: IllegalStateException) {
            // 忽略 JavaFX 未初始化的异常
        }
    }

    private fun initAndPlay() {
        try {
            val videoPath = SplashVideoPlayer::class.java.classLoader
                .getResource("assets/ecyclient/SplashVideo.mp4")

            if (videoPath == null) {
                logger.warn("Splash video not found, skipping")
                return
            }

            stage = Stage().apply {
                initStyle(StageStyle.TRANSPARENT)
                // 窗口化设置
                width = 640.0
                height = 360.0
                isAlwaysOnTop = true
                
                // 将窗口放在屏幕正中央
                val screenWidth = javafx.stage.Screen.getPrimary().visualBounds.width
                val screenHeight = javafx.stage.Screen.getPrimary().visualBounds.height
                x = (screenWidth - width) / 2
                y = (screenHeight - height) / 2
            }

            val media = Media(videoPath.toExternalForm())
            mediaPlayer = MediaPlayer(media).apply {
                cycleCount = MediaPlayer.INDEFINITE
                play()
            }

            val mediaView = MediaView(mediaPlayer)
            statsLabel = Label("CPU: 0% MEM: 0%").apply {
                style = """
                    -fx-text-fill: white;
                    -fx-font-size: 12px;
                    -fx-background-color: rgba(0, 0, 0, 0.5);
                    -fx-padding: 5 10 5 10;
                    -fx-background-radius: 5;
                """.trimIndent()
            }
            
            logLabel = Label("Initializing game...").apply {
                style = """
                    -fx-text-fill: white;
                    -fx-font-size: 12px;
                    -fx-background-color: rgba(0, 0, 0, 0.5);
                    -fx-padding: 5 10 5 10;
                    -fx-background-radius: 5;
                """.trimIndent()
                isWrapText = true
                maxWidth = 620.0
            }
            
            val infoBox = VBox(5.0, statsLabel, logLabel)
            
            val root = StackPane().apply {
                children.add(mediaView)
                children.add(infoBox)
                // 设置圆角和透明背景
                setStyle("-fx-background-color: transparent;")
                // 将信息框放在左下角
                StackPane.setAlignment(infoBox, Pos.BOTTOM_LEFT)
                StackPane.setMargin(infoBox, javafx.geometry.Insets(10.0, 0.0, 10.0, 10.0))
            }

            mediaView.fitWidthProperty().bind(root.widthProperty())
            mediaView.fitHeightProperty().bind(root.heightProperty())

            stage!!.apply {
                // 创建带圆角的 Scene
                val scene = Scene(root, 640.0, 360.0)
                scene.fill = javafx.scene.paint.Color.TRANSPARENT
                // 通过 CSS 设置圆角
                scene.stylesheets.clear()
                root.style = """
                    -fx-background-color: transparent;
                    -fx-border-radius: 20;
                    -fx-background-radius: 20;
                """.trimIndent()
                // 使用 clip 来实现圆角裁剪
                val clip = javafx.scene.shape.Rectangle(640.0, 360.0).apply {
                    arcWidth = 40.0
                    arcHeight = 40.0
                }
                root.clip = clip
                this.scene = scene
                show()
            }

            isPlaying = true
            logger.info("Splash video started")
            
            // 启动系统资源监控线程
            startStatsMonitor()
        } catch (e: Exception) {
            logger.error("Failed to play splash video", e)
        }
    }

    @JvmStatic
    fun stopVideo() {
        if (isPlaying) {
            isPlaying = false
            statsThread?.interrupt()
            Platform.runLater {
                try {
                    mediaPlayer?.stop()
                    stage?.close()
                    logger.info("Splash video stopped")
                } catch (e: Exception) {
                    logger.error("Failed to stop splash video", e)
                }
            }
        }
    }
    
    private fun startStatsMonitor() {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        
        statsThread = Thread {
            try {
                while (isPlaying && !Thread.currentThread().isInterrupted) {
                    val cpuLoad = osBean.processCpuLoad
                    val cpuPercent = if (cpuLoad >= 0) String.format("%.0f", cpuLoad * 100) else "?"
                    
                    val totalMem = osBean.totalPhysicalMemorySize / 1024 / 1024
                    val freeMem = osBean.freePhysicalMemorySize / 1024 / 1024
                    val usedMem = totalMem - freeMem
                    val memPercent = if (totalMem > 0) String.format("%.0f", usedMem.toDouble() / totalMem * 100) else "?"
                    
                    Platform.runLater {
                        statsLabel?.text = "CPU: ${cpuPercent}%  MEM: ${memPercent}%"
                    }
                    
                    Thread.sleep(1000)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.apply {
            isDaemon = true
            start()
        }
    }
}
