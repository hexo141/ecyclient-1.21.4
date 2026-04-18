package com.example.mixin

import com.example.hud.ModuleHudScreen
import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MinecraftClient::class)
abstract class ModuleHudKeyMixin {
    
    private var rightShiftWasPressed = false
    
    @Inject(method = ["handleInputEvents"], at = [At("HEAD")])
    private fun handleInputEvents(ci: CallbackInfo) {
        val client = MinecraftClient.getInstance()
        if (client.currentScreen != null) return
        
        val rightShiftPressed = GLFW.glfwGetKey(
            client.window.handle,
            GLFW.GLFW_KEY_RIGHT_SHIFT
        ) == GLFW.GLFW_PRESS
        
        if (rightShiftPressed && !rightShiftWasPressed) {
            client.setScreen(ModuleHudScreen())
        }
        
        rightShiftWasPressed = rightShiftPressed
    }
}
