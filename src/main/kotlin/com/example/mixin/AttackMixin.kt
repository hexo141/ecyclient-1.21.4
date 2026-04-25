package com.example.mixin

import com.example.module.combat.CriticalHit
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(MinecraftClient::class)
abstract class AttackMixin {
    
    @Inject(method = ["doAttack"], at = [At("RETURN")])
    private fun onDoAttack(ci: CallbackInfoReturnable<Boolean>) {
        if (CriticalHit.state.toString() != "ENABLED") return
        if (ci.returnValue != true) return
        
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val networkHandler = player.networkHandler ?: return
        
        val originalY = player.pos.y
        
        networkHandler.sendPacket(
            PlayerMoveC2SPacket.PositionAndOnGround(
                player.pos.x,
                originalY + 0.0625,
                player.pos.z,
                false,
                player.horizontalCollision
            )
        )
        
        networkHandler.sendPacket(
            PlayerMoveC2SPacket.PositionAndOnGround(
                player.pos.x,
                originalY,
                player.pos.z,
                false,
                player.horizontalCollision
            )
        )
    }
}
