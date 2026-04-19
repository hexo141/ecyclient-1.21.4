package com.example.module.impl


import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import com.example.module.config.ConfigPersistence
import com.example.module.impl.config.autowaterplace.AutoWaterPlaceConfig
import com.google.gson.JsonObject
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

object AutoWaterPlace : GameModule {
    override val metadata = ModuleMetadata(
        id = "auto_water_place",
        name = "Auto Water Bucket MLG",
        version = "1.2.0",
        description = "Automatically places a water bucket to prevent fall damage",
        enabled = true,
        category = ModuleCategory.ASSISTANT
    )

    override var state: ModuleState = ModuleState.DISABLED

    private val client: MinecraftClient get() = MinecraftClient.getInstance()
    private var waterPlaced = false
    private var cooldownTicks = 0
    private var wasInAir = false
    private var airTimeTicks = 0

    override fun onEnable() {
        loadSavedConfig()
        resetState()
        wasInAir = false
        airTimeTicks = 0
    }

    override fun onDisable() {
        resetState()
    }
    
    private fun loadSavedConfig() {
        val savedConfig = ConfigPersistence.loadConfig("auto_water_place") ?: return
        
        val minFallDistance = savedConfig.get("minFallDistance")?.asFloat ?: AutoWaterPlaceConfig.minFallDistance
        val placeDistance = savedConfig.get("placeDistance")?.asDouble ?: AutoWaterPlaceConfig.placeDistance
        val maxGroundDistance = savedConfig.get("maxGroundDistance")?.asDouble ?: AutoWaterPlaceConfig.maxGroundDistance
        val cooldownTime = savedConfig.get("cooldownTime")?.asInt ?: AutoWaterPlaceConfig.cooldownTime
        val requireWaterBucket = savedConfig.get("requireWaterBucket")?.asBoolean ?: AutoWaterPlaceConfig.requireWaterBucket
        
        configure(
            minFallDistance = minFallDistance,
            placeDistance = placeDistance,
            maxGroundDistance = maxGroundDistance,
            cooldownTime = cooldownTime,
            requireWaterBucket = requireWaterBucket
        )
    }

    override fun onTick() {
        if (state != ModuleState.LOADED) return

        if (cooldownTicks > 0) {
            cooldownTicks--
            return
        }

        val player = client.player ?: return

        // 重置状态
        if (player.isOnGround || player.isTouchingWater || player.isInLava) {
            resetState()
            wasInAir = false
            airTimeTicks = 0
        }

        // 检测坠落并放水
        checkAndPlaceWater(player)
    }

    private fun checkAndPlaceWater(player: ClientPlayerEntity) {
        if (waterPlaced) return
        if (AutoWaterPlaceConfig.requireWaterBucket && !hasWaterBucket(player)) return
        
        // 在地面或水中不放水
        if (player.isOnGround || player.isTouchingWater || player.isInLava) return
        
        // 检测是否在坠落（向下运动）
        val isFalling = player.velocity.y < 0
        
        if (isFalling) {
            if (!wasInAir) {
                wasInAir = true
            }
            airTimeTicks++
        } else {
            // 向上运动时重置
            if (wasInAir && player.velocity.y > 0) {
                wasInAir = false
                airTimeTicks = 0
            }
            return
        }
        
        // 计算到地面的距离
        val groundDistance = getDistanceToGround(player)
        if (groundDistance.isInfinite() || groundDistance > AutoWaterPlaceConfig.maxGroundDistance) return
        
        val isCloseToGround = groundDistance <= AutoWaterPlaceConfig.placeDistance
        
        val hasFallenEnough = player.fallDistance >= AutoWaterPlaceConfig.minFallDistance || airTimeTicks > 10
        
        if (isCloseToGround && hasFallenEnough) {
            placeWater(player)
        }
    }

    /**
     * 计算玩家脚部到地面的距离
     * 只检测玩家碰撞箱正下方的方块，避免擦肩而过的方块误触发
     */
    private fun getDistanceToGround(player: ClientPlayerEntity): Double {
        val world = player.clientWorld
        val playerY = player.pos.y
        
        // 获取玩家碰撞箱的XZ范围（玩家碰撞箱宽度为0.6）
        val boundingBox = player.boundingBox
        val minX = kotlin.math.floor(boundingBox.minX).toInt()
        val maxX = kotlin.math.floor(boundingBox.maxX - 0.001).toInt()
        val minZ = kotlin.math.floor(boundingBox.minZ).toInt()
        val maxZ = kotlin.math.floor(boundingBox.maxZ - 0.001).toInt()
        
        for (offset in 0..AutoWaterPlaceConfig.maxGroundDistance.toInt() + 5) {
            val checkY = kotlin.math.floor(playerY - offset).toInt()
            if (checkY < 0) break
            
            // 只检查玩家碰撞箱正下方的方块
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val pos = BlockPos(x, checkY, z)
                    val blockState = world.getBlockState(pos)
                    if (!blockState.isAir) {
                        val blockTopY = pos.y + 1.0
                        val distance = playerY - blockTopY
                        if (distance >= 0) {
                            return distance
                        }
                    }
                }
            }
        }
        
        return Double.POSITIVE_INFINITY
    }

    private fun hasWaterBucket(player: ClientPlayerEntity): Boolean {
        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            if (stack.item == Items.WATER_BUCKET) {
                return true
            }
        }
        return false
    }

    private fun findWaterBucketSlot(player: ClientPlayerEntity): Int {
        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            if (stack.item == Items.WATER_BUCKET) {
                return i
            }
        }
        return -1
    }

    private fun findGroundBlockBelow(player: ClientPlayerEntity): BlockPos? {
        val world = player.clientWorld
        val startY = kotlin.math.floor(player.pos.y).toInt()
        val limit = startY - AutoWaterPlaceConfig.maxGroundDistance.toInt()
        
        // 获取玩家碰撞箱的XZ范围
        val boundingBox = player.boundingBox
        val minX = kotlin.math.floor(boundingBox.minX).toInt()
        val maxX = kotlin.math.floor(boundingBox.maxX - 0.001).toInt()
        val minZ = kotlin.math.floor(boundingBox.minZ).toInt()
        val maxZ = kotlin.math.floor(boundingBox.maxZ - 0.001).toInt()

        for (y in startY downTo limit.coerceAtLeast(0)) {
            // 只检查玩家碰撞箱正下方的方块
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val pos = BlockPos(x, y, z)
                    val blockState = world.getBlockState(pos)
                    if (!blockState.isAir) {
                        return pos
                    }
                }
            }
        }
        return null
    }

    private fun placeWater(player: ClientPlayerEntity) {
        val waterSlot = findWaterBucketSlot(player)
        if (waterSlot == -1) return

        val groundBlockPos = findGroundBlockBelow(player) ?: return
        
        val waterTargetPos = groundBlockPos.up()
        val world = player.clientWorld

        // 保存原槽位并切换到水桶
        val originalSlot = player.inventory.selectedSlot
        player.inventory.selectedSlot = waterSlot
        
        // 创建交互 - 点击地面方块的顶部（使用右击）
        val hitVec = Vec3d(groundBlockPos.x + 0.5, groundBlockPos.y + 1.0, groundBlockPos.z + 0.5)
        val hitResult = BlockHitResult(hitVec, Direction.UP, groundBlockPos, false)
        
        // 使用右击行为放置水桶
        client.interactionManager?.interactItem(player, Hand.MAIN_HAND)
        player.swingHand(Hand.MAIN_HAND)
        
        // 恢复原槽位
        player.inventory.selectedSlot = originalSlot
        
        waterPlaced = true
        cooldownTicks = AutoWaterPlaceConfig.cooldownTime
        
        // 输出成功消息
        println("Placed water at $groundBlockPos")
        val message = Text.literal("(AutoWaterPlace) ")
            .formatted(Formatting.GRAY)
            .append(Text.literal("Success!")
                .formatted(Formatting.GREEN, Formatting.BOLD))
        
        player.sendMessage(message, false)
    }

    private fun resetState() {
        waterPlaced = false
        wasInAir = false
        airTimeTicks = 0
    }

    fun configure(
        minFallDistance: Float = 3f,
        placeDistance: Double = 2.0,
        maxGroundDistance: Double = 10.0,
        cooldownTime: Int = 20,
        requireWaterBucket: Boolean = true
    ) {
        AutoWaterPlaceConfig.minFallDistance = minFallDistance
        AutoWaterPlaceConfig.placeDistance = placeDistance
        AutoWaterPlaceConfig.maxGroundDistance = maxGroundDistance
        AutoWaterPlaceConfig.cooldownTime = cooldownTime
        AutoWaterPlaceConfig.requireWaterBucket = requireWaterBucket
    }

    fun getConfig(): Map<String, Any> = AutoWaterPlaceConfig.toMap()
}