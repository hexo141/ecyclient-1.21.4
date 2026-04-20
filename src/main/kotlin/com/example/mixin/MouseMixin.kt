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
        // 如果HUD可见，拦截鼠标点击事件
        if (HudManager.isVisible()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (action == GLFW.GLFW_PRESS) {
                    // 阻止鼠标点击事件传递到游戏
                    ci.cancel()
                }
            }
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