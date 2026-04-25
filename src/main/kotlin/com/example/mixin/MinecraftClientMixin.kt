package com.example.mixin

import com.example.effect.MotionBlurEffect
import com.example.hud.HudManager
import com.example.module.ModuleManager
import com.example.module.render.MotionBlur
import com.example.util.VideoStopHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import org.slf4j.LoggerFactory
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MinecraftClient::class)
abstract class MinecraftClientMixin {
    private val logger = LoggerFactory.getLogger("ecyclient-mixin")
    
    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun onTick(ci: CallbackInfo) {
        ModuleManager.onTick()
    }
    
    @Inject(method = ["render"], at = [At("HEAD")])
    private fun onRenderStart(tick: Boolean, ci: CallbackInfo) {
        VideoStopHelper.onRenderStart()
        
        if (MotionBlur.state == com.example.module.ModuleState.ENABLED) {
            MotionBlurEffect.onBeforeRender()
        }
    }
    
    @Inject(method = ["render"], at = [At("TAIL")])
    private fun onRender(tick: Boolean, ci: CallbackInfo) {
        val client = MinecraftClient.getInstance()
        
        // 使用正确的渲染上下文
        val context = DrawContext(client, client.bufferBuilders.entityVertexConsumers)
        ModuleManager.onRender(context, 1.0f)
        
        // HUD现在使用Screen类，不需要在这里渲染
        
        if (MotionBlur.state == com.example.module.ModuleState.ENABLED) {
            MotionBlurEffect.onAfterRender()
        }
    }
    
    @Inject(method = ["handleInputEvents"], at = [At("HEAD")])
    private fun onHandleInputEvents(ci: CallbackInfo) {
        // 使用Minecraft的键绑定系统检测RSHIFT键
        val keyBinding = HudManager.getKeyBinding()
        if (keyBinding.wasPressed()) {
            logger.info("检测到RSHIFT键按下")
            HudManager.toggleHud()
        }
        
        // HUD现在使用Screen类，输入处理由Screen自身处理
    }
    
    @ModifyArg(
        method = ["updateWindowTitle"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"
        )
    )
    private fun modifyWindowTitle(title: String): String {
        return "[ECY] $title"
    }
}
