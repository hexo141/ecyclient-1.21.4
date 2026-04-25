package com.example.module.combat

import com.example.module.GameModule
import com.example.module.ModuleCategory
import com.example.module.ModuleMetadata
import com.example.module.ModuleState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Box
import org.joml.Matrix4f

object ExecutionAura : GameModule {
    override val metadata = ModuleMetadata(
        id = "execution_aura",
        name = "斩杀光环",
        description = "在脚下生成光环,自动攻击指定范围内的实体",
        category = ModuleCategory.COMBAT
    )
    
    override var state: ModuleState = ModuleState.DISABLED
    
    var range: Double = 5.0
    var attackDelay: Int = 10
    var attackPlayers: Boolean = true
    var attackMobs: Boolean = true
    var attackAnimals: Boolean = false
    var circleColor: Int = 0x80FF0000.toInt()
    var circleSegments: Int = 64
    
    private var attackTimer = 0
    
    override fun onEnable() {
        attackTimer = 0
    }
    
    override fun onDisable() {
    }
    
    override fun onTick() {
        val client = MinecraftClient.getInstance()
        val player = client.player
        val world = client.world
        
        if (player == null || world == null) return
        
        if (attackTimer > 0) {
            attackTimer--
            return
        }
        
        val target = findTarget(player, world) ?: return
        
        attackEntity(client, target)
        attackTimer = attackDelay
    }
    
    override fun onRenderWorld(context: DrawContext, tickDelta: Float) {
    }
    
    private fun findTarget(player: PlayerEntity, world: net.minecraft.world.World): Entity? {
        var closestTarget: Entity? = null
        var closestDistance = Double.MAX_VALUE
        
        val searchBox = Box(
            player.x - range,
            player.y - range,
            player.z - range,
            player.x + range,
            player.y + range,
            player.z + range
        )
        
        world.getOtherEntities(player, searchBox) { entity ->
            entity is LivingEntity && !entity.isDead && isValidTarget(entity)
        }.forEach { entity ->
            val distance = player.squaredDistanceTo(entity)
            if (distance < closestDistance) {
                closestDistance = distance
                closestTarget = entity
            }
        }
        
        return closestTarget
    }
    
    private fun isValidTarget(entity: Entity): Boolean {
        return when (entity) {
            is PlayerEntity -> attackPlayers
            is HostileEntity -> attackMobs
            is PassiveEntity -> attackAnimals
            else -> false
        }
    }
    
    private fun attackEntity(client: MinecraftClient, target: Entity) {
        val player = client.player ?: return
        val interactionManager = client.interactionManager ?: return
        val networkHandler = player.networkHandler ?: return
        
        val originalYaw = player.yaw
        val originalPitch = player.pitch
        
        val diffX = target.x - player.x
        val diffZ = target.z - player.z
        val yaw = MathHelper.atan2(diffZ, diffX) * 180.0 / Math.PI - 90.0
        
        player.yaw = yaw.toFloat()
        player.pitch = 0f
        
        if (player.isOnGround) {
            player.jump()
        }
        
        interactionManager.attackEntity(player, target)
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND)
        
        player.yaw = originalYaw
        player.pitch = originalPitch
    }
    
    private fun renderCircle(matrices: MatrixStack, radius: Double, color: Int, segments: Int) {
        val matrix4f = matrices.peek().positionMatrix
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val alpha = (color shr 24) and 0xFF
        
        for (i in 0..segments) {
            val angle = i * 2.0 * Math.PI / segments
            val x = radius * Math.cos(angle)
            val z = radius * Math.sin(angle)
            
            buffer.vertex(matrix4f, x.toFloat(), 0.0f, z.toFloat()).color(red, green, blue, alpha)
        }
        
        val builtBuffer = buffer.end()
        BufferRenderer.drawWithGlobalProgram(builtBuffer)
    }
    
    override fun getConfig(): Map<String, Any> {
        return mapOf(
            "range" to range,
            "attackDelay" to attackDelay,
            "attackPlayers" to attackPlayers,
            "attackMobs" to attackMobs,
            "attackAnimals" to attackAnimals,
            "circleColor" to circleColor,
            "circleSegments" to circleSegments
        )
    }
    
    override fun applyConfig(config: Map<String, Any>) {
        (config["range"] as? Number)?.toDouble()?.let { range = it }
        (config["attackDelay"] as? Number)?.toInt()?.let { attackDelay = it }
        (config["attackPlayers"] as? Boolean)?.let { attackPlayers = it }
        (config["attackMobs"] as? Boolean)?.let { attackMobs = it }
        (config["attackAnimals"] as? Boolean)?.let { attackAnimals = it }
        (config["circleColor"] as? Number)?.toInt()?.let { circleColor = it }
        (config["circleSegments"] as? Number)?.toInt()?.let { circleSegments = it }
    }
}
