package com.frnc.createvoid.portal;

import com.frnc.createvoid.block.custom.VoxelShapes;
import com.frnc.createvoid.world.dimension.ModDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VoidPortal extends Block {

    // 匹配模型：16x3x16像素的扁平薄板（定义于 block/custom/VoxelShapes）
    private static final VoxelShape SHAPE = VoxelShapes.VOID_BLOCK;

    // 冷却时间（单位：游戏刻，20刻 = 1秒）
    private static final int COOLDOWN_TICKS = 60;
    // 5x5搜索半径
    private static final int SEARCH_RADIUS = 2;

    public VoidPortal(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                  @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // 只在服务端执行，且必须空手
        if (level.isClientSide || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        // ---- 1. 冷却检查 ----
        CompoundTag persistentData = serverPlayer.getPersistentData();
        long lastTeleport = persistentData.getLong("lastVoidTeleport");
        long currentTime = level.getGameTime();
        if (currentTime - lastTeleport < COOLDOWN_TICKS) {
            long remainingTicks = COOLDOWN_TICKS - (currentTime - lastTeleport);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.create_void.portal_cooldown", remainingTicks / 20.0), true);
            return InteractionResult.FAIL;
        }

        ServerLevel currentLevel = (ServerLevel) level;

        // ---- 2. 决定目标维度 ----
        boolean toVoid = !currentLevel.dimension().equals(ModDimension.VOID_LEVEL_KEY);
        ServerLevel targetLevel = toVoid ?
                currentLevel.getServer().getLevel(ModDimension.VOID_LEVEL_KEY) :
                currentLevel.getServer().getLevel(Level.OVERWORLD);

        if (targetLevel == null) {
            return InteractionResult.FAIL; // 维度不存在
        }

        // ---- 3. 计算目标坐标（使用玩家位置，限制在世界边界内） ----
        WorldBorder border = targetLevel.getWorldBorder();
        double clampedX = Math.max(border.getMinX() + 1, Math.min(border.getMaxX() - 1, player.getX()));
        double clampedZ = Math.max(border.getMinZ() + 1, Math.min(border.getMaxZ() - 1, player.getZ()));
        int x = (int) clampedX;
        int z = (int) clampedZ;

        // ---- 4. 串门检测：在目标区块内查找已有传送门 ----
        BlockPos existingPortal = findNearbyPortal(targetLevel, x, z);
        BlockPos targetPos;

        if (existingPortal != null) {
            // 串门：传送到已有传送门
            targetPos = existingPortal;
        } else {
            // 没有已有传送门，智能搜索放置位置
            int surfaceY = targetLevel.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            targetPos = findOrCreatePortalPosition(targetLevel, x, surfaceY, z);
            // 放置新传送门
            if (!targetLevel.getBlockState(targetPos).is(this)) {
                targetLevel.setBlock(targetPos, defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // ---- 5. 传送玩家 ----
        serverPlayer.teleportTo(targetLevel,
                targetPos.getX() + 0.5,
                targetPos.getY() + 1,
                targetPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        // ---- 6. 播放末影珍珠音效和粒子效果 ----
        currentLevel.sendParticles(
                ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                30, 0.5, 0.5, 0.5, 0.1
        );
        currentLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);

        targetLevel.sendParticles(
                ParticleTypes.PORTAL,
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                30, 0.5, 0.5, 0.5, 0.1
        );
        targetLevel.playSound(null, targetPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);

        // ---- 7. 更新冷却时间 ----
        persistentData.putLong("lastVoidTeleport", currentTime);

        return InteractionResult.SUCCESS;
    }

    // ==================== 串门机制 ====================

    /**
     * 在目标坐标所在区块内查找已有传送门（串门）
     * 限制：仅在同一区块内搜索
     */
    private BlockPos findNearbyPortal(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int cx = minX; cx < minX + 16; cx++) {
            for (int cz = minZ; cz < minZ + 16; cz++) {
                int sy = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
                // 在地表附近上下几格搜索传送门
                for (int cy = sy; cy >= Math.max(level.getMinBuildHeight(), sy - 5); cy--) {
                    mutable.set(cx, cy, cz);
                    if (level.getBlockState(mutable).is(this)) {
                        return mutable.immutable();
                    }
                }
            }
        }
        return null; // 未找到已有传送门
    }

    // ==================== 智能放置逻辑 ====================

    /**
     * 在5x5范围内搜索适合放置传送门的位置。
     * 条件：上方有两格空气方块。
     * 若找不到合适位置，则在中心强行开辟空间。
     */
    private BlockPos findOrCreatePortalPosition(ServerLevel level, int centerX, int surfaceY, int centerZ) {
        List<BlockPos> candidates = new ArrayList<>();

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;

                if (!level.getWorldBorder().isWithinBounds(cx, cz)) {
                    continue;
                }

                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
                BlockPos candidate = new BlockPos(cx, y, cz);

                // 检查上方是否有两格空气方块
                if (level.getBlockState(candidate.above()).isAir()
                        && level.getBlockState(candidate.above(2)).isAir()) {
                    candidates.add(candidate);
                }
            }
        }

        // 有合适位置：优先选离中心最近的
        if (!candidates.isEmpty()) {
            candidates.sort((a, b) -> {
                int da = distSq(a, centerX, centerZ);
                int db = distSq(b, centerX, centerZ);
                return Integer.compare(da, db);
            });
            return candidates.get(0);
        }

        // 找不到合适位置：在中心强行开辟空间
        BlockPos forced = new BlockPos(centerX, surfaceY, centerZ);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos clearPos = forced.above(dy);
            BlockState bs = level.getBlockState(clearPos);
            if (!bs.isAir() && !bs.is(this)) {
                level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return forced;
    }

    private static int distSq(BlockPos pos, int cx, int cz) {
        int dx = pos.getX() - cx;
        int dz = pos.getZ() - cz;
        return dx * dx + dz * dz;
    }
}
