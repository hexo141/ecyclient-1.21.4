package com.example.module.impl


import com.example.module.GameModule
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
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
        enabled = true
    )

    override var state: ModuleState = ModuleState.DISABLED

    private val client: MinecraftClient get() = MinecraftClient.getInstance()
    private var waterPlaced = false
    private var cooldownTicks = 0
    private var wasInAir = false
    private var airTimeTicks = 0

    // Configuration options
    var minFallDistance = 3f
        private set
    var placeDistance = 2.0
        private set
    var maxGroundDistance = 10.0
        private set
    var cooldownTime = 20
        private set
    var requireWaterBucket = true
        private set

    override fun onEnable() {
        resetState()
        wasInAir = false
        airTimeTicks = 0
    }

    override fun onDisable() {
        resetState()
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
        if (requireWaterBucket && !hasWaterBucket(player)) return
        
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
        if (groundDistance.isInfinite() || groundDistance > maxGroundDistance) return
        
        // 触发条件：离地距离足够近
        val isCloseToGround = groundDistance <= placeDistance
        
        // 坠落距离足够 或者 在空中超过一定时间
        val hasFallenEnough = player.fallDistance >= minFallDistance || airTimeTicks > 10
        
        if (isCloseToGround && hasFallenEnough) {
            placeWater(player)
        }
    }

    /**
     * 计算玩家脚部到地面的距离
     */
    private fun getDistanceToGround(player: ClientPlayerEntity): Double {
        val world = player.clientWorld
        val playerX = player.pos.x
        val playerY = player.pos.y
        val playerZ = player.pos.z
        
        // 从玩家脚下开始向下扫描
        for (offset in 0..maxGroundDistance.toInt() + 5) {
            val checkY = (playerY - offset).toInt()
            if (checkY < 0) break
            
            // 检查多个水平位置，确保准确性
            val checkPositions = listOf(
                BlockPos(playerX.toInt(), checkY, playerZ.toInt()),
                BlockPos((playerX + 0.3).toInt(), checkY, playerZ.toInt()),
                BlockPos((playerX - 0.3).toInt(), checkY, playerZ.toInt()),
                BlockPos(playerX.toInt(), checkY, (playerZ + 0.3).toInt()),
                BlockPos(playerX.toInt(), checkY, (playerZ - 0.3).toInt())
            )
            
            for (pos in checkPositions) {
                val blockState = world.getBlockState(pos)
                if (!blockState.isAir) {
                    val blockTopY = pos.y + 1.0
                    val distance = playerY - blockTopY
                    if (distance >= 0 && distance < maxGroundDistance) {
                        return distance
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
        val startY = player.pos.y.toInt()
        val limit = startY - maxGroundDistance.toInt()

        for (y in startY downTo limit.coerceAtLeast(0)) {
            // 检查多个水平位置
            val positions = listOf(
                BlockPos(player.pos.x.toInt(), y, player.pos.z.toInt()),
                BlockPos((player.pos.x + 0.5).toInt(), y, player.pos.z.toInt()),
                BlockPos((player.pos.x - 0.5).toInt(), y, player.pos.z.toInt()),
                BlockPos(player.pos.x.toInt(), y, (player.pos.z + 0.5).toInt()),
                BlockPos(player.pos.x.toInt(), y, (player.pos.z - 0.5).toInt())
            )
            
            for (pos in positions) {
                val blockState = world.getBlockState(pos)
                if (!blockState.isAir) {
                    return pos
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
        cooldownTicks = cooldownTime
        
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
        this.minFallDistance = minFallDistance
        this.placeDistance = placeDistance
        this.maxGroundDistance = maxGroundDistance
        this.cooldownTime = cooldownTime
        this.requireWaterBucket = requireWaterBucket
    }

    fun getConfig(): Map<String, Any> = mapOf(
        "minFallDistance" to minFallDistance,
        "placeDistance" to placeDistance,
        "maxGroundDistance" to maxGroundDistance,
        "cooldownTime" to cooldownTime,
        "requireWaterBucket" to requireWaterBucket
    )
}