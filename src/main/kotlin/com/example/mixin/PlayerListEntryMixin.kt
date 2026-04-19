package com.example.mixin

import com.example.module.impl.EcyPlayerTag
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PlayerListEntry::class)
class PlayerListEntryMixin {
    
    @Inject(method = ["getDisplayName"], at = [At("RETURN")], cancellable = true)
    private fun onGetDisplayName(cir: CallbackInfoReturnable<Text>) {
        val originalName = cir.returnValue
        if (originalName != null) {
            val modifiedName = EcyPlayerTag.modifyPlayerName(originalName)
            if (modifiedName != originalName) {
                cir.setReturnValue(modifiedName)
            }
        }
    }
}