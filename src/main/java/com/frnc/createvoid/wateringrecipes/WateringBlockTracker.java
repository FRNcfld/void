package com.frnc.createvoid.wateringrecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跟踪已被 Spout 浇灌转换过的方块位置。
 * <p>
 * 目的：防止 Spout 对同一方块连续执行整条配方链
 * （铜块 → 斑驳 → 锈蚀 → 氧化）。一个方块位置只允许转换一次，
 * 玩家破坏并重新放置后才能再次浇灌。
 * </p>
 * <p>
 * 实现：服务器端内存 {@code Map<维度, Map<位置, 转换后的方块>>}。
 * 转换后记录目标方块；若当前位置仍是被转换后的方块则拒绝再次转换；
 * 若方块已变化（被破坏/替换）则自动清除标记。
 * </p>
 * <p>
 * <b>区块索引：</b>位置表同时按区块建立了索引（{@link #BY_CHUNK}），
 * 这样区块卸载时可以把该区块的标记一次性清掉。此前只在
 * {@code LevelEvent.Unload}（整个维度卸载）时清理，而那不是区块卸载事件，
 * 于是被探索过的区域里每个转换过的方块都会把 {@link BlockPos} 永久留在内存里。
 * </p>
 */
public final class WateringBlockTracker {

    /** 维度 → 位置 → 转换后的方块。 */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Block>> CONVERTED = new ConcurrentHashMap<>();

    /** 维度 → 区块(ChunkPos.toLong()) → 该区块内记录过的位置。 */
    private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> BY_CHUNK =
            new ConcurrentHashMap<>();

    private WateringBlockTracker() {}

    /**
     * 该位置是否已被浇灌转换（且方块未被替换）。
     */
    public static boolean isConverted(Level level, BlockPos pos) {
        Map<BlockPos, Block> levelMap = CONVERTED.get(level.dimension());
        if (levelMap == null) return false;

        Block convertedTo = levelMap.get(pos);
        if (convertedTo == null) return false;

        Block current = level.getBlockState(pos).getBlock();
        if (current == convertedTo) {
            return true;   // 仍是被转换后的方块 → 阻止再次转换
        }
        // 方块已变化（被破坏/替换/流体冲刷）→ 标记失效，允许转换
        remove(level.dimension(), pos);
        return false;
    }

    /**
     * 记录一次成功的浇灌转换。
     */
    public static void markConverted(Level level, BlockPos pos, Block block) {
        ResourceKey<Level> dimension = level.dimension();
        CONVERTED.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>()).put(pos, block);
        BY_CHUNK.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4),
                        k -> ConcurrentHashMap.newKeySet())
                .add(pos);
    }

    /**
     * 清除某位置的标记（方块被破坏/重新放置时调用）。
     */
    public static void clear(Level level, BlockPos pos) {
        remove(level.dimension(), pos);
    }

    private static void remove(ResourceKey<Level> dimension, BlockPos pos) {
        Map<BlockPos, Block> levelMap = CONVERTED.get(dimension);
        if (levelMap != null) {
            levelMap.remove(pos);
        }
        Map<Long, Set<BlockPos>> chunkMap = BY_CHUNK.get(dimension);
        if (chunkMap != null) {
            Set<BlockPos> bucket = chunkMap.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            if (bucket != null) {
                bucket.remove(pos);
            }
        }
    }

    /**
     * 清除某维度某个区块的全部标记（区块卸载时调用，防止内存泄漏）。
     *
     * @param dimension 维度
     * @param chunkX    区块 X
     * @param chunkZ    区块 Z
     */
    public static void clearChunk(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        Map<Long, Set<BlockPos>> chunkMap = BY_CHUNK.get(dimension);
        if (chunkMap == null) return;

        Set<BlockPos> bucket = chunkMap.remove(ChunkPos.asLong(chunkX, chunkZ));
        if (bucket == null || bucket.isEmpty()) return;

        Map<BlockPos, Block> levelMap = CONVERTED.get(dimension);
        if (levelMap != null) {
            for (BlockPos pos : bucket) {
                levelMap.remove(pos);
            }
        }
    }

    /**
     * 清除整个维度的标记（维度卸载时调用，防止内存泄漏）。
     */
    public static void clearLevel(ResourceKey<Level> dimension) {
        CONVERTED.remove(dimension);
        BY_CHUNK.remove(dimension);
    }
}
