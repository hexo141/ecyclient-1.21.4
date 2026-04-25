# Motion Blur Module 实现计划

## 项目背景
这是一个基于 Fabric + Kotlin 的 Minecraft 客户端模组 (1.21.4)，已有完整的模块系统，包括模块注册、状态管理、配置持久化等。

## 技术选型说明
在 Minecraft 1.21.4 Fabric 环境下实现运动模糊，需要：
1. 通过 Mixin 劫持游戏渲染管线
2. 使用帧缓冲 (Framebuffer) 捕获上一帧画面
3. 使用 OpenGL Shader 实现多帧混合
4. 在 `MinecraftClient.render()` 方法的适当时机注入后处理逻辑

## 文件结构

```
src/main/kotlin/com/example/
├── module/render/
│   └── MotionBlur.kt                    # 模块定义，处理配置参数
├── effect/
│   └── MotionBlurEffect.kt              # 运动模糊效果实现，管理Framebuffer和渲染
├── mixin/
│   ├── WorldRendererMixin.kt            # 劫持世界渲染，获取帧缓冲
│   └── GameRendererMixin.kt             # 劫持游戏渲染器，注入后处理
└── shader/
    └── MotionBlurShader.kt              # OpenGL着色器管理

src/main/resources/assets/ecyclient/shaders/
├── motionblur.frag                      # 片段着色器
└── motionblur.vert                      # 顶点着色器
```

## 实现步骤

### 1. 创建 OpenGL Shader 资源文件

**motionblur.vert** - 顶点着色器：简单的全屏四边形
```glsl
#version 150 core

in vec2 Position;
in vec2 UV;

out vec2 TexCoord;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
    TexCoord = UV;
}
```

**motionblur.frag** - 片段着色器：实现多帧混合的运动模糊
```glsl
#version 150 core

uniform sampler2D CurrentFrame;
uniform sampler2D PreviousFrame;
uniform float Strength;  // 模糊强度 0.0 - 1.0

in vec2 TexCoord;
out vec4 FragColor;

void main() {
    vec4 current = texture(CurrentFrame, TexCoord);
    vec4 previous = texture(PreviousFrame, TexCoord);
    
    // 线性插值混合当前帧和上一帧
    FragColor = mix(current, previous, Strength);
}
```

### 2. 创建 MotionBlurShader.kt

负责编译和管理 OpenGL 着色器程序。

核心功能：
- 加载 `.vert` 和 `.frag` 着色器文件
- 编译和链接着色器程序
- 提供 uniform 设置接口 (Strength)
- 使用 Minecraft 内置的 `ShaderProgram` 或原生 GL 调用

### 3. 创建 MotionBlurEffect.kt

负责管理运动模糊的渲染管线。

核心功能：
- 创建和管理 Framebuffer (当前帧 + 历史帧)
- 实现渲染到纹理 (Render To Texture)
- 调用 Shader 进行后处理混合
- 使用 `RenderPipeline` 执行全屏四边形绘制
- 正确处理帧缓冲的创建和销毁

### 4. 创建 MotionBlur.kt 模块

实现 `GameModule` 接口的运动模糊模块。

配置参数：
- `strength` (Float, 0.0-1.0): 模糊强度，默认 0.5
- `quality` (Int, 2-8): 采样质量，影响性能

生命周期：
- `onEnable()`: 初始化 Framebuffer 和 Shader
- `onDisable()`: 清理 Framebuffer 和 Shader 资源
- `onTick()`: 处理配置变更
- `getConfig()` / `applyConfig()`: 配置序列化

### 5. 创建 Mixin 劫持渲染管线

**WorldRendererMixin.kt**：
- 在 `render()` 方法结束后，将渲染结果复制到历史帧缓冲

**GameRendererMixin.kt**：
- 在 `renderWorld()` 方法后注入后处理逻辑
- 调用 `MotionBlurEffect.render()` 执行模糊混合

### 6. 注册模块

在 `EcyClient.kt` (模组入口) 中注册模块：
```kotlin
ModuleManager.register(MotionBlur())
```

### 7. 在 ModuleConfigScreen 中添加配置支持

确保配置屏幕能够显示和编辑 MotionBlur 的参数（已有通用配置系统支持）。

## 技术要点

1. **Framebuffer 管理**：
   - 使用 `net.minecraft.client.gl.Framebuffer` 创建帧缓冲
   - 需要处理窗口大小变化时的 Framebuffer 重建

2. **Shader 集成**：
   - Minecraft 1.21.4 使用 `ShaderProgram` 类管理着色器
   - 或者使用 `org.lwjgl.opengl.GL*` 直接调用原生 OpenGL

3. **渲染时机**：
   - 必须在 Minecraft 渲染管线的正确时机注入
   - 需要确保不干扰 Minecraft 的原生渲染

4. **性能考虑**：
   - 运动模糊会增加渲染开销
   - 需要正确释放 GPU 资源避免内存泄漏

5. **兼容性**：
   - 需要与模组已有的渲染系统兼容
   - 不干扰 HUD 和其他模块的渲染

## 风险点

1. Minecraft 1.21.4 的渲染 API 可能与旧版本不同，需要验证 Framebuffer 和 Shader API
2. Mixin 注入点需要精确选择，避免与其他模组冲突
3. 动态模糊可能在某些情况下导致画面异常（如 UI 渲染层）
