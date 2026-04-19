package com.example.module

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader

object ModuleManager {
    private val logger = LoggerFactory.getLogger("ModuleManager")
    private val modules = mutableMapOf<String, GameModule>()
    private val moduleMetadataMap = mutableMapOf<String, ModuleMetadata>()
    private var initialized = false
    private var worldLoaded = false

    private val gson = Gson()

    fun init() {
        if (initialized) {
            logger.warn("ModuleManager already initialized")
            return
        }
        initialized = true
        logger.info("Initializing ModuleManager...")
        
        registerBuiltInModules()
        scanModules()
    }
    
    fun registerModule(module: GameModule) {
        val metadata = module.metadata
        moduleMetadataMap[metadata.id] = metadata
        modules[metadata.id] = module
        logger.info("Registered built-in module: ${metadata.name} v${metadata.version}")
    }
    
    private fun registerBuiltInModules() {
        try {
            val autoWaterPlaceClass = Class.forName("com.example.module.impl.AutoWaterPlace")
            val instanceField = autoWaterPlaceClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            val module = instanceField.get(null) as GameModule
            registerModule(module)
            logger.info("Successfully registered AutoWaterPlace module")
        } catch (e: Exception) {
            logger.warn("Failed to register AutoWaterPlace module: ${e.message}")
            e.printStackTrace()
        }
        
        try {
            val autoSprintClass = Class.forName("com.example.module.impl.AutoSprint")
            val instanceField = autoSprintClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            val module = instanceField.get(null) as GameModule
            registerModule(module)
            logger.info("Successfully registered AutoSprint module")
        } catch (e: Exception) {
            logger.warn("Failed to register AutoSprint module: ${e.message}")
            e.printStackTrace()
        }
        
        try {
            val moduleListDisplayClass = Class.forName("com.example.module.impl.ModuleListDisplay")
            val instanceField = moduleListDisplayClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            val module = instanceField.get(null) as GameModule
            registerModule(module)
            logger.info("Successfully registered ModuleListDisplay module")
        } catch (e: Exception) {
            logger.warn("Failed to register ModuleListDisplay module: ${e.message}")
            e.printStackTrace()
        }
        
        try {
            val ecyPlayerTagClass = Class.forName("com.example.module.impl.EcyPlayerTag")
            val instanceField = ecyPlayerTagClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            val module = instanceField.get(null) as GameModule
            registerModule(module)
            logger.info("Successfully registered EcyPlayerTag module")
        } catch (e: Exception) {
            logger.warn("Failed to register EcyPlayerTag module: ${e.message}")
            e.printStackTrace()
        }
    }

    fun onWorldLoad() {
        if (worldLoaded) return
        worldLoaded = true
        logger.info("World loaded, loading enabled modules...")
        loadEnabledModules()
    }

    fun onClientShutdown() {
        logger.info("Client shutting down, unloading all modules...")
        unloadAllModules()
        worldLoaded = false
    }

    fun tick() {
        if (!worldLoaded) return
        modules.values.forEach { module ->
            if (module.state == ModuleState.LOADED) {
                try {
                    module.onTick()
                } catch (e: Exception) {
                    logger.error("Error ticking module ${module.metadata.id}: ${e.message}")
                }
            }
        }
    }

    fun getAllModules(): List<GameModule> = modules.values.toList()

    fun getModule(id: String): GameModule? = modules[id]

    fun getModuleMetadata(id: String): ModuleMetadata? = moduleMetadataMap[id]

    fun getAllMetadata(): List<ModuleMetadata> = moduleMetadataMap.values.toList()

    fun enableModule(id: String): Boolean {
        val module = modules[id] ?: return false
        if (module.state == ModuleState.LOADED) return true
        
        return loadModule(module)
    }

    fun disableModule(id: String): Boolean {
        val module = modules[id] ?: return false
        if (module.state == ModuleState.DISABLED) return true
        
        return unloadModule(module)
    }

    fun isModuleEnabled(id: String): Boolean {
        val metadata = moduleMetadataMap[id] ?: return false
        return metadata.enabled
    }

    private fun scanModules() {
        val modulesDir = FabricLoader.getInstance().gameDir.resolve("modules").toFile()
        
        if (!modulesDir.exists()) {
            modulesDir.mkdirs()
            logger.info("Created modules directory: ${modulesDir.absolutePath}")
            return
        }

        val moduleDirs = modulesDir.listFiles { file -> file.isDirectory } ?: emptyArray()
        logger.info("Found ${moduleDirs.size} module directories")

        for (moduleDir in moduleDirs) {
            try {
                val metadataFile = File(moduleDir, "module.json")
                if (!metadataFile.exists()) {
                    logger.warn("Skipping ${moduleDir.name}: no module.json found")
                    continue
                }

                val metadata = parseModuleMetadata(metadataFile)
                if (metadata == null) {
                    logger.warn("Skipping ${moduleDir.name}: invalid module.json")
                    continue
                }

                moduleMetadataMap[metadata.id] = metadata
                logger.info("Discovered module: ${metadata.name} v${metadata.version}")
            } catch (e: Exception) {
                logger.error("Failed to scan module ${moduleDir.name}: ${e.message}")
            }
        }
    }

    private fun parseModuleMetadata(file: File): ModuleMetadata? {
        return try {
            FileReader(file).use { reader ->
                val json = gson.fromJson(reader, JsonObject::class.java)
                ModuleMetadata(
                    id = json.get("id")?.asString ?: return null,
                    name = json.get("name")?.asString ?: return null,
                    version = json.get("version")?.asString ?: "1.0.0",
                    description = json.get("description")?.asString ?: "",
                    dependencies = json.getAsJsonArray("dependencies")?.let { array ->
                        array.map { it.asString }
                    } ?: emptyList(),
                    enabled = json.get("enabled")?.asBoolean ?: true,
                    category = json.get("category")?.asString?.let { 
                        try { ModuleCategory.valueOf(it) } catch (e: Exception) { ModuleCategory.OTHER }
                    } ?: ModuleCategory.OTHER
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to parse module.json: ${e.message}")
            null
        }
    }

    private fun loadEnabledModules() {
        val enabledModules = moduleMetadataMap.values.filter { it.enabled }
        logger.info("Loading ${enabledModules.size} enabled modules...")
        
        val sortedModules = sortByDependencies(enabledModules)
        
        for (metadata in sortedModules) {
            val existingModule = modules[metadata.id]
            if (existingModule != null && existingModule.state == ModuleState.DISABLED) {
                if (!loadModule(existingModule)) {
                    logger.error("Failed to load module: ${metadata.id}")
                }
            } else if (existingModule == null) {
                if (!loadModuleByMetadata(metadata)) {
                    logger.error("Failed to load module: ${metadata.id}")
                }
            }
        }
        
        // 确保所有已注册的内置模块都被启用（如果它们被标记为启用）
        modules.values.forEach { module ->
            if (module.metadata.enabled && module.state == ModuleState.DISABLED) {
                if (!loadModule(module)) {
                    logger.error("Failed to load built-in module: ${module.metadata.id}")
                }
            }
        }
    }

    private fun loadModuleByMetadata(metadata: ModuleMetadata): Boolean {
        for (depId in metadata.dependencies) {
            val depModule = modules[depId]
            if (depModule == null || depModule.state != ModuleState.LOADED) {
                logger.warn("Cannot load ${metadata.id}: dependency $depId not loaded")
                return false
            }
        }

        try {
            val moduleClass = Class.forName("${metadata.id}.ModuleImpl")
            val module = moduleClass.getDeclaredConstructor().newInstance() as GameModule
            return loadModule(module)
        } catch (e: Exception) {
            logger.error("Failed to instantiate module ${metadata.id}: ${e.message}")
            return false
        }
    }

    private fun loadModule(module: GameModule): Boolean {
        val id = module.metadata.id
        
        if (modules.containsKey(id) && module.state == ModuleState.LOADED) {
            logger.warn("Module $id already loaded")
            return true
        }

        if (modules.containsKey(id) && module.state != ModuleState.DISABLED) {
            logger.warn("Module $id in invalid state: ${module.state}")
            return false
        }

        module.state = ModuleState.LOADING

        for (depId in module.metadata.dependencies) {
            val depModule = modules[depId]
            if (depModule == null || depModule.state != ModuleState.LOADED) {
                logger.warn("Cannot load $id: dependency $depId not loaded")
                module.state = ModuleState.ERROR
                return false
            }
        }

        try {
            module.onEnable()
            module.state = ModuleState.LOADED
            if (!modules.containsKey(id)) {
                modules[id] = module
            }
            logger.info("Module loaded: ${module.metadata.name}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to enable module $id: ${e.message}")
            module.state = ModuleState.ERROR
            return false
        }
    }

    private fun unloadModule(module: GameModule): Boolean {
        val id = module.metadata.id
        
        val dependents = modules.values.filter { m ->
            m.metadata.dependencies.contains(id) && m.state == ModuleState.LOADED
        }
        
        for (dependent in dependents) {
            unloadModule(dependent)
        }

        try {
            module.onDisable()
            module.state = ModuleState.DISABLED
            logger.info("Module unloaded: ${module.metadata.name}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to disable module $id: ${e.message}")
            return false
        }
    }

    private fun unloadAllModules() {
        val sortedModules = sortByDependenciesReverse(modules.values.map { it.metadata })
        
        for (metadata in sortedModules) {
            modules[metadata.id]?.let { unloadModule(it) }
        }
        modules.clear()
    }

    private fun sortByDependencies(metadataList: List<ModuleMetadata>): List<ModuleMetadata> {
        val sorted = mutableListOf<ModuleMetadata>()
        val visited = mutableSetOf<String>()
        
        fun visit(metadata: ModuleMetadata) {
            if (visited.contains(metadata.id)) return
            visited.add(metadata.id)
            
            for (depId in metadata.dependencies) {
                val depMetadata = moduleMetadataMap[depId]
                if (depMetadata != null) {
                    visit(depMetadata)
                }
            }
            
            sorted.add(metadata)
        }
        
        for (metadata in metadataList) {
            visit(metadata)
        }
        
        return sorted
    }

    private fun sortByDependenciesReverse(metadataList: List<ModuleMetadata>): List<ModuleMetadata> {
        return sortByDependencies(metadataList).reversed()
    }
}
