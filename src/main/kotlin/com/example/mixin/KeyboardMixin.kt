package com.example.mixin

import com.example.hud.HudManager
import net.minecraft.client.Keyboard
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Keyboard::class)
class KeyboardMixin {
    
    @Inject(method = ["onKey"], at = [At("HEAD")], cancellable = true)
    private fun onKey(window: Long, key: Int, scancode: Int, action: Int, mods: Int, ci: CallbackInfo) {
        // 如果HUD可见，拦截ESC键关闭HUD
        if (HudManager.isVisible()) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                HudManager.toggleHud()
                ci.cancel()
            }
        }
    }
}