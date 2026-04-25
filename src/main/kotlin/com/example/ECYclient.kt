package com.example

import com.example.module.ModuleManager
import com.example.module.example.ExampleModule
import com.example.util.VideoStopHelper
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.slf4j.LoggerFactory

object ECYclient : ModInitializer, PreLaunchEntrypoint {
    private val logger = LoggerFactory.getLogger("ecyclient")

	override fun onInitialize() {
		logger.info("ECYclient 系统初始化中...")
		
		// 键绑定已经在HudManager中定义，不需要额外初始化
		
		// 注册模块
		ModuleManager.register(ExampleModule)
		ModuleManager.register(com.example.module.movement.AutoSprint)
		ModuleManager.register(com.example.module.player.AutoWaterPlace)
		ModuleManager.register(com.example.module.combat.CriticalHit)
		ModuleManager.register(com.example.module.combat.ExecutionAura)
		
		logger.info("ECYclient 系统初始化完成!")
		logger.info("按 RSHIFT 键打开HUD界面")
	}
	
	override fun onPreLaunch() {
		println("Playing startup video...")
		VideoStopHelper.startVideo()
		// 视频启动后再初始化日志拦截器
		Thread.sleep(500)
		com.example.util.GameLogInterceptor.init()
	}
}
