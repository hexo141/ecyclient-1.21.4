package com.example

import com.example.util.VideoStopHelper
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.slf4j.LoggerFactory

object ECYclient : ModInitializer, PreLaunchEntrypoint {
    private val logger = LoggerFactory.getLogger("ecyclient")

	override fun onInitialize() {
		logger.info("Hello ECYclient!")
	}
	
	override fun onPreLaunch() {
		println("Playing startup video...")
		VideoStopHelper.startVideo()
		// 视频启动后再初始化日志拦截器
		Thread.sleep(500)
		com.example.util.GameLogInterceptor.init()
	}
}
