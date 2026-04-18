package com.example.mixin

import com.example.util.VideoStopHelper
import net.minecraft.client.MinecraftClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MinecraftClient::class)
abstract class MinecraftClientMixin {
    
    @Inject(method = ["render"], at = [At("HEAD")])
    private fun onRenderStart(tick: Boolean, ci: CallbackInfo) {
        VideoStopHelper.onRenderStart()
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
