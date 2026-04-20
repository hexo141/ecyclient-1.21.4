package com.example.mixin

import com.example.hud.HudManager
import net.minecraft.client.Mouse
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Mouse::class)
class MouseMixin {
    
    @Inject(method = ["onMouseButton"], at = [At("HEAD")], cancellable = true)
    private fun onMouseButton(window: Long, button: Int, action: Int, mods: Int, ci: CallbackInfo) {
        // 如果HUD可见，允许鼠标事件传递到HUD但不阻止游戏处理
        if (HudManager.isVisible()) {
            // 不取消事件，让HUD和游戏都能处理
            // HUD会优先处理，如果HUD处理了事件，游戏就不会收到
        }
    }
    
    @Inject(method = ["onCursorPos"], at = [At("HEAD")], cancellable = true)
    private fun onCursorPos(window: Long, x: Double, y: Double, ci: CallbackInfo) {
        // 如果HUD可见，允许鼠标移动但不影响游戏
        if (HudManager.isVisible()) {
            // 可以在这里添加HUD鼠标悬停检测
        }
    }
}