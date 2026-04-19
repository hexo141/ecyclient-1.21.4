package com.example.mixin

import com.example.module.ModuleState
import com.example.module.impl.ModuleListDisplay
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.InGameHud
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(InGameHud::class)
abstract class InGameHudMixin {
    
    @Inject(method = ["render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"], at = [At("TAIL")])
    private fun onRender(context: DrawContext, tickCounter: RenderTickCounter, ci: CallbackInfo) {
        val client = MinecraftClient.getInstance()
        
        if (client.currentScreen == null && ModuleListDisplay.state == ModuleState.LOADED) {
            ModuleListDisplay.render(context, tickCounter.getTickDelta(false))
        }
    }
}
