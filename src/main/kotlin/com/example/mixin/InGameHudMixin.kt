package com.example.mixin

import com.example.hud.HudManager
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.hud.InGameHud
import net.minecraft.client.render.RenderTickCounter
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(InGameHud::class)
class InGameHudMixin {
    
    @Inject(method = ["render"], at = [At("TAIL")])
    private fun onRender(context: DrawContext, tickCounter: RenderTickCounter, ci: CallbackInfo) {
        // HUD现在使用Screen类，不需要在这里渲染
    }
}