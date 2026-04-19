package com.example.module.impl

import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object EcyPlayerTag : GameModule {
    override val metadata = ModuleMetadata(
        id = "ecy_player_tag",
        name = "ECY Player Tag",
        version = "1.0.0",
        description = "Adds pink [ECY] tag before player names when they use the same client",
        enabled = true,
        category = ModuleCategory.DISPLAY
    )

    override var state: ModuleState = ModuleState.DISABLED

    private val client: MinecraftClient get() = MinecraftClient.getInstance()
    
    // 存储检测到的相同客户端玩家列表
    private val sameClientPlayers = mutableSetOf<String>()

    override fun onEnable() {
        // 初始化模块时扫描当前玩家列表
        scanForSameClientPlayers()
    }

    override fun onDisable() {
        sameClientPlayers.clear()
    }

    override fun onTick() {
        // 每10秒重新扫描玩家列表，检测新加入的玩家
        if (client.world?.time?.rem(200L) == 0L) {
            scanForSameClientPlayers()
        }
    }

    /**
     * 扫描当前玩家列表，检测是否使用相同客户端
     */
    private fun scanForSameClientPlayers() {
        val networkHandler = client.networkHandler ?: return
        sameClientPlayers.clear()
        
        // 获取所有玩家列表
        val playerList = networkHandler.playerList
        
        for (playerEntry in playerList) {
            val playerName = playerEntry.profile.name
            
            // 跳过本地玩家
            if (playerName == client.session.username) continue
            
            // 检测是否使用相同客户端
            if (detectSameClient(playerEntry)) {
                sameClientPlayers.add(playerName)
            }
        }
    }

    /**
     * 检测玩家是否使用相同客户端
     * 这里使用多种检测方法：
     * 1. 检查玩家名称是否包含特定标识
     * 2. 检查玩家皮肤或模型是否有特殊特征
     * 3. 检查玩家行为模式
     */
    private fun detectSameClient(playerEntry: PlayerListEntry): Boolean {
        val playerName = playerEntry.profile.name
        
        // 方法1: 检查玩家名称是否包含ECY相关标识
        if (playerName.contains("ECY", ignoreCase = true) ||
            playerName.contains("ecy", ignoreCase = true)) {
            return true
        }
        
        // 方法2: 检查玩家是否使用特殊皮肤（可以扩展）
        // 这里可以添加皮肤检测逻辑
        
        // 方法3: 检查玩家是否在特定服务器上（可以扩展）
        // 这里可以添加服务器检测逻辑
        
        // 方法4: 检查玩家是否有特定的行为模式（可以扩展）
        // 这里可以添加行为检测逻辑
        
        // 暂时返回false，需要后续实现更复杂的检测机制
        return false
    }

    /**
     * 修改玩家名称，添加ECY标签
     */
    fun modifyPlayerName(originalName: Text): Text {
        if (state != ModuleState.LOADED) return originalName
        
        val playerNameString = originalName.string
        
        // 如果是本地玩家，不添加标签
        if (playerNameString == client.session.username) {
            return originalName
        }
        
        // 检查是否在检测到的相同客户端玩家列表中
        if (sameClientPlayers.contains(playerNameString)) {
            // 使用粉红色标签 [ECY]
            return Text.literal("[").formatted(Formatting.WHITE)
                .append(Text.literal("ECY").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal("] ").formatted(Formatting.WHITE))
                .append(originalName)
        }
        
        return originalName
    }
}