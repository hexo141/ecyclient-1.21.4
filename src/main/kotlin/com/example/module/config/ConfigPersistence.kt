package com.example.module.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileReader
import org.slf4j.LoggerFactory

object ConfigPersistence {
    private val logger = LoggerFactory.getLogger("ConfigPersistence")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configDir: File by lazy {
        FabricLoader.getInstance().gameDir.resolve("ecyclient_config").toFile().also {
            if (!it.exists()) it.mkdirs()
        }
    }

    data class ConfigValue(
        val name: String,
        val displayName: String,
        val type: String,
        val value: Any,
        val minValue: Number? = null,
        val maxValue: Number? = null,
        val step: Number? = null
    )

    fun saveConfig(moduleId: String, configValues: List<ConfigValue>) {
        try {
            val configFile = File(configDir, "$moduleId.json")
            val json = JsonObject()
            
            for (configValue in configValues) {
                when (configValue.type) {
                    "float" -> json.addProperty(configValue.name, configValue.value as Float)
                    "double" -> json.addProperty(configValue.name, configValue.value as Double)
                    "int" -> json.addProperty(configValue.name, configValue.value as Int)
                    "boolean" -> json.addProperty(configValue.name, configValue.value as Boolean)
                    "string" -> json.addProperty(configValue.name, configValue.value as String)
                }
            }
            
            configFile.writeText(gson.toJson(json))
            logger.info("Saved config for module: $moduleId")
        } catch (e: Exception) {
            logger.error("Failed to save config for module $moduleId: ${e.message}")
        }
    }

    fun loadConfig(moduleId: String): JsonObject? {
        try {
            val configFile = File(configDir, "$moduleId.json")
            if (!configFile.exists()) return null
            
            FileReader(configFile).use { reader ->
                return gson.fromJson(reader, JsonObject::class.java)
            }
        } catch (e: Exception) {
            logger.error("Failed to load config for module $moduleId: ${e.message}")
            return null
        }
    }

    fun getConfigFile(moduleId: String): File {
        return File(configDir, "$moduleId.json")
    }

    fun hasConfig(moduleId: String): Boolean {
        return getConfigFile(moduleId).exists()
    }
}
