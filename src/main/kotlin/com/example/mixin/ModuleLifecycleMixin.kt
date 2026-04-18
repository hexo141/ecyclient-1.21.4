package com.example.mixin

import com.example.module.ModuleManager
import net.minecraft.client.MinecraftClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MinecraftClient::class)
abstract class ModuleLifecycleMixin {
    
    @Inject(method = ["joinWorld"], at = [At("RETURN")])
    private fun onJoinWorld(ci: CallbackInfo) {
        ModuleManager.onWorldLoad()
    }
    
    @Inject(method = ["stop"], at = [At("HEAD")])
    private fun onStop(ci: CallbackInfo) {
        ModuleManager.onClientShutdown()
    }
    
    @Inject(method = ["tick"], at = [At("HEAD")])
    private fun onTick(ci: CallbackInfo) {
        ModuleManager.tick()
    }
}
