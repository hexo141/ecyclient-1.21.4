# ECYClient Module System v2 设计规范

## 🎯 目标

构建一个稳定、可扩展、生命周期安全的模块系统，支持：

* 清晰模块分类（5大类型）
* 明确运行状态（3大状态）
* 统一生命周期管理
* 可持久化配置系统
* 渲染 / Tick 解耦

---

# 🧩 一、模块分类（5 Types）

```kotlin
enum class ModuleCategory {
    COMBAT,      // 战斗类（KillAura, AimAssist）
    MOVEMENT,    // 移动类（Sprint, Fly）
    RENDER,      // 渲染类（ESP, HUD）
    PLAYER,      // 玩家行为（AutoEat, AutoTool）
    MISC         // 杂项（Chat, Fun）
}
```

---

# 🔄 二、模块状态（3 States）

```kotlin
enum class ModuleState {
    DISABLED,   // 未启用
    ENABLED,    // 运行中
    ERROR       // 崩溃/异常
}
```

---

# 🧠 三、生命周期设计

模块生命周期必须严格受 ModuleManager 控制：

```
注册 → ENABLE → Tick/Render → DISABLE → UNLOAD
```

禁止模块：

* 直接访问 Minecraft 生命周期（必须通过 Manager）
* 在 DISABLED 状态执行逻辑
* 在 world=null 时执行逻辑

---

# 🧱 四、模块基类（核心接口）

```kotlin
interface GameModule {

    val metadata: ModuleMetadata
    var state: ModuleState

    fun onEnable() {}
    fun onDisable() {}

    fun onTick() {}
    fun onRenderWorld(matrices: MatrixStack, tickDelta: Float) {}
}
```

---

# 🏷 五、模块元数据

```kotlin
data class ModuleMetadata(
    val id: String,
    val name: String,
    val description: String,
    val category: ModuleCategory
)
```

---

# 📂 六、配置系统（Config）

## 📍 路径规范

```
.minecraft/ecyclient/
    config/
        modules/
            player_esp.json
            auto_sprint.json
```

---

## 📄 配置结构

```json
{
  "enabled": true,
  "settings": {
    "color": "#FF0000",
    "lineWidth": 1.5
  }
}
```

---

## 🧾 Kotlin 实现

```kotlin
object ConfigManager {

    private val baseDir = File("ecyclient/config/modules")

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
    }

    fun save(module: GameModule) {
        val file = File(baseDir, "${module.metadata.id}.json")

        val json = JsonObject().apply {
            addProperty("enabled", module.state == ModuleState.ENABLED)
        }

        file.writeText(json.toString())
    }

    fun load(module: GameModule) {
        val file = File(baseDir, "${module.metadata.id}.json")
        if (!file.exists()) return

        val json = JsonParser.parseString(file.readText()).asJsonObject

        val enabled = json["enabled"]?.asBoolean ?: false
        if (enabled) ModuleManager.enable(module)
    }
}
```

---

# 🧭 七、ModuleManager（核心调度）

```kotlin
object ModuleManager {

    private val modules = mutableListOf<GameModule>()

    fun register(module: GameModule) {
        modules.add(module)
        ConfigManager.load(module)
    }

    fun enable(module: GameModule) {
        if (module.state == ModuleState.ENABLED) return

        try {
            module.onEnable()
            module.state = ModuleState.ENABLED
            ConfigManager.save(module)
        } catch (e: Exception) {
            module.state = ModuleState.ERROR
        }
    }

    fun disable(module: GameModule) {
        if (module.state == ModuleState.DISABLED) return

        try {
            module.onDisable()
            module.state = ModuleState.DISABLED
            ConfigManager.save(module)
        } catch (e: Exception) {
            module.state = ModuleState.ERROR
        }
    }

    fun toggle(module: GameModule) {
        if (module.state == ModuleState.ENABLED) disable(module)
        else enable(module)
    }

    fun onTick() {
        modules.forEach {
            if (it.state == ModuleState.ENABLED) {
                safeRun(it) { it.onTick() }
            }
        }
    }

    fun onRender(matrices: MatrixStack, tickDelta: Float) {
        modules.forEach {
            if (it.state == ModuleState.ENABLED) {
                safeRun(it) { it.onRenderWorld(matrices, tickDelta) }
            }
        }
    }

    private inline fun safeRun(module: GameModule, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            module.state = ModuleState.ERROR
        }
    }
}
```

---

# 🔐 八、安全策略（必须遵守）

### ✔ 生命周期保护

在任何模块逻辑前：

```kotlin
val client = MinecraftClient.getInstance()
if (client.world == null || client.player == null) return
```

---

### ✔ 渲染安全

* 不允许在 GUI / Menu 渲染
* 不允许访问 null camera

---

### ✔ 崩溃隔离

* 任意模块异常 → 状态变 ERROR
* 不影响其他模块

---

# 🚀 九、扩展方向

未来可以扩展：

* KeyBind 系统
* ClickGUI
* Setting DSL（类似 Meteor）
* EventBus 替代直接调用

---

# 🧾 结论

该架构具备：

* 清晰边界
* 生命周期安全
* 配置持久化
* 可扩展性强

可作为客户端核心模块系统长期使用。
