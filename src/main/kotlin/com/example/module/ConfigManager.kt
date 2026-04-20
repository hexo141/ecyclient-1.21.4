package com.example.module

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import java.io.File

object ConfigManager {
    private val logger = LoggerFactory.getLogger("ecyclient-config")
    private val baseDir = File("ecyclient/config/modules")

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
            logger.info("配置目录已创建: ${baseDir.absolutePath}")
        }
    }

    fun save(module: GameModule) {
        try {
            val file = File(baseDir, "${module.metadata.id}.json")

            val json = JsonObject().apply {
                addProperty("enabled", module.state == ModuleState.ENABLED)
                addProperty("name", module.metadata.name)
                addProperty("description", module.metadata.description)
                addProperty("category", module.metadata.category.name)
            }

            file.writeText(json.toString())
            logger.debug("模块配置已保存: ${module.metadata.name}")
        } catch (e: Exception) {
            logger.error("保存模块配置失败: ${module.metadata.name}", e)
        }
    }

    fun load(module: GameModule) {
        try {
            val file = File(baseDir, "${module.metadata.id}.json")
            if (!file.exists()) {
                logger.debug("模块配置文件不存在: ${module.metadata.name}")
                return
            }

            val json = JsonParser.parseString(file.readText()).asJsonObject

            val enabled = json["enabled"]?.asBoolean ?: false
            if (enabled) {
                module.state = ModuleState.ENABLED
                logger.debug("模块配置已加载: ${module.metadata.name} (启用)")
            } else {
                module.state = ModuleState.DISABLED
                logger.debug("模块配置已加载: ${module.metadata.name} (禁用)")
            }
        } catch (e: Exception) {
            logger.error("加载模块配置失败: ${module.metadata.name}", e)
            module.state = ModuleState.DISABLED
        }
    }

    fun delete(module: GameModule) {
        try {
            val file = File(baseDir, "${module.metadata.id}.json")
            if (file.exists()) {
                file.delete()
                logger.info("模块配置已删除: ${module.metadata.name}")
            }
        } catch (e: Exception) {
            logger.error("删除模块配置失败: ${module.metadata.name}", e)
        }
    }

    fun getConfigDir(): File = baseDir
}