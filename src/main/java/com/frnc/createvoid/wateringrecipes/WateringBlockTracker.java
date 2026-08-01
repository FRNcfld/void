package com.frnc.createvoid.wateringrecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跟踪已被 Spout 浇灌转换过的方块位置。
 * <p>
 * 目的：防止 Spout 对同一方块连续执行整条配方链
 * （铜块 → 斑驳 → 锈蚀 → 氧化）。一个方块位置只允许转换一次，
 * 玩家破坏并重新放置后才能再次浇灌。
 * </p>
 * <p>
 * 实现：服务器端内存 Map&lt;维度, Map&lt;位置, 转换后的方块&gt;&gt;。
 * 转换后记录目标方块；若当前位置仍是被转换后的方块则拒绝再次转换；
 * 若方块已变化（被破坏/替换）则自动清除标记。
 * </p>
 */
public final class WateringBlockTracker {

    private static final Map<ResourceKey<Level>, Map<BlockPos, Block>> CONVERTED = new ConcurrentHashMap<>();

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
        levelMap.remove(pos);
        return false;
    }

    /**
     * 记录一次成功的浇灌转换。
     */
    public static void markConverted(Level level, BlockPos pos, Block block) {
        CONVERTED.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(pos, block);
    }

    /**
     * 清除某位置的标记（方块被破坏/重新放置时调用）。
     */
    public static void clear(Level level, BlockPos pos) {
        Map<BlockPos, Block> levelMap = CONVERTED.get(level.dimension());
        if (levelMap != null) levelMap.remove(pos);
    }

    /**
     * 清除整个维度的标记（维度卸载时调用，防止内存泄漏）。
     */
    public static void clearLevel(ResourceKey<Level> dimension) {
        CONVERTED.remove(dimension);
    }
}
