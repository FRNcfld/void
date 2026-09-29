package com.frnc.createvoid.portal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 复刻原版下界传送门的"驻留计时"。
 *
 * <h3>为什么需要它</h3>
 * <p>
 * 原版把这份计时存在 {@code Entity.portalTime} 字段里，由
 * {@code Entity.handleNetherPortal()} 维护：站在门里每个 tick 自增，达到
 * {@code getPortalWaitTime()} 才传送；离开则每个 tick 衰减 4，所以中途走开不会传送。
 * </p>
 * <p>
 * 但 {@code handleNetherPortal()} 把目标维度写死为
 * {@code dimension() == NETHER ? OVERWORLD : NETHER}——虚空维度不在这个配对里，
 * 走那条路会把玩家送进地狱。该字段又是私有的，无法复用，因此只能自己维护一份。
 * </p>
 *
 * <h3>衰减是惰性的</h3>
 * <p>
 * {@code entityInside} 只在实体位于门内时被调用，实体离开后没人通知我们。
 * 所以这里不挂全局 tick 钩子，而是记录<b>最后一次</b>的 gameTime：下次被调用时，
 * 若中间隔了 {@code n} 个 tick，就按原版的速率（每 tick 4 点）补扣
 * {@code (n - 1) * 4}。行为与原版一致，且不需要遍历全世界的实体。
 * </p>
 *
 * <h3>按 tick 去重</h3>
 * <p>
 * 同一个 tick 内 {@code entityInside} 可能被调用多次（实体同时压着好几格门方块），
 * 详见 {@link #advance} 的说明——不去重会让传送比原版快好几倍。
 * </p>
 */
public final class VoidPortalTimers {

    /** 最多同时跟踪多少实体；超出后整体清空（与 WateringBlockTracker 同款有界策略）。 */
    private static final int MAX_TRACKED = 4096;

    /** 原版离开传送门后的衰减速率：每个 tick 扣 4。 */
    private static final int DECAY_PER_TICK = 4;

    private static final Map<UUID, Progress> PROGRESS = new ConcurrentHashMap<>();

    private VoidPortalTimers() {
    }

    private static final class Progress {
        int ticks;
        /** 上一次计入的 gameTime。初值 -1 使首次调用必然走"新条目"分支（gameTime 从 0 起）。 */
        long lastGameTime = -1L;
    }

    /**
     * 记一次"本 tick 实体在门内"，返回<b>自增前</b>的驻留 tick 数。
     *
     * <h4>为什么必须按 tick 去重</h4>
     * <p>
     * {@code BlockState.entityInside} 不是"实体在门里时每 tick 调一次"，而是
     * {@code Entity.checkInsideBlocks()} 对<b>实体包围盒覆盖的每一个方块</b>各调一次。
     * 玩家站在 2×3 的门里会同时压到好几格门方块，于是同一个 tick 里本方法会被调用
     * 多次。若不去重，计时会按倍数增长，传送比原版快好几倍。
     * </p>
     * <p>
     * 原版没有这个问题，是因为它只把 {@code isInsidePortal} 置为 true（重复写是幂等的），
     * 真正的自增发生在每 tick 一次的 {@code Entity.handleNetherPortal()} 里。
     * 这里用 {@code lastGameTime} 判重来达到同样效果。
     * </p>
     *
     * <h4>为什么返回自增前的值</h4>
     * <p>
     * 对齐原版 {@code this.portalTime++ >= i}：那个表达式用的也是自增前的值，
     * 因此生存玩家（wait = 80）在进门后的第 81 个 tick 传送，而不是第 80 个。
     * </p>
     *
     * @return 自增前的驻留 tick 数；调用方与 {@code entity.getPortalWaitTime()} 比较
     */
    public static int advance(Level level, Entity entity) {
        if (PROGRESS.size() >= MAX_TRACKED) {
            PROGRESS.clear();
        }
        long now = level.getGameTime();
        Progress progress = PROGRESS.computeIfAbsent(entity.getUUID(), k -> new Progress());

        if (now == progress.lastGameTime) {
            // 本 tick 已经计过数（实体同时压着多格门方块）→ 不重复累计，
            // 返回与首次调用相同的"自增前"值
            return progress.ticks - 1;
        }

        long elapsed = now - progress.lastGameTime;
        if (elapsed > 1) {
            // 中间有 tick 不在门内 → 按原版速率补扣（elapsed - 1 是真正"缺席"的 tick 数）
            progress.ticks = Math.max(0, progress.ticks - (int) Math.min(Integer.MAX_VALUE, (elapsed - 1) * DECAY_PER_TICK));
        }

        int current = progress.ticks;
        progress.ticks++;
        progress.lastGameTime = now;
        return current;
    }

    /** 传送完成（或目标维度不可达）时清掉计时，避免带着进度再次进门。 */
    public static void forget(Entity entity) {
        PROGRESS.remove(entity.getUUID());
    }
}
