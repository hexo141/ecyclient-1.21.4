package com.example.module

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory
import java.io.File

object ConfigManager {
    private val logger = LoggerFactory.getLogger("ecyclient-config")
    private val baseDir: File by lazy {
        val mcDir = MinecraftClient.getInstance().runDirectory
        File(mcDir, "ecyclient/config/modules")
    }

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
                
                // 获取模块配置并添加到JSON
                val moduleConfig = module.getConfig()
                for ((key, value) in moduleConfig) {
                    when (value) {
                        is Boolean -> addProperty(key, value)
                        is Int -> {
                            // 检测是否为颜色值（包含alpha通道的大整数或负数）
                            if (key.lowercase().contains("color") || value < 0 || value > 0xFFFFFF) {
                                // 保存为无符号十六进制字符串
                                val unsignedValue = value.toLong() and 0xFFFFFFFFL
                                addProperty(key, String.format("#%08X", unsignedValue))
                            } else {
                                addProperty(key, value)
                            }
                        }
                        is Number -> addProperty(key, value)
                        is String -> addProperty(key, value)
                        else -> addProperty(key, value.toString())
                    }
                }
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
            
            // 提取模块配置并应用
            val configMap = mutableMapOf<String, Any>()
            json.entrySet().forEach { entry ->
                val key = entry.key
                if (key !in listOf("enabled", "name", "description", "category")) {
                    val value = when {
                        entry.value.isJsonPrimitive -> {
                            val primitive = entry.value.asJsonPrimitive
                            when {
                                primitive.isBoolean -> primitive.asBoolean
                                primitive.isNumber -> primitive.asNumber
                                primitive.isString -> {
                                    val strValue = primitive.asString
                                    // 检测十六进制颜色代码
                                    if (strValue.startsWith("#") && (strValue.length == 7 || strValue.length == 9)) {
                                        try {
                                            val hexValue = strValue.substring(1)
                                            val longValue = java.lang.Long.parseLong(hexValue, 16)
                                            longValue.toInt()
                                        } catch (e: NumberFormatException) {
                                            strValue
                                        }
                                    } else {
                                        strValue
                                    }
                                }
                                else -> primitive.asString
                            }
                        }
                        else -> entry.value.toString()
                    }
                    configMap[key] = value
                }
            }
            
            // 应用模块配置
            module.applyConfig(configMap)
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