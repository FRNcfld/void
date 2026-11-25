package com.frnc.createvoid.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 实体万象的方块实体。
 * <p>
 * 存储模型：<b>一个方块只容纳一种物品 ID</b>，但同一 ID 下不同 NBT（附魔、自定义名称等）
 * 会分成多个桶分别保存，不会互相覆盖。数量用 {@code long} 累加，因此单桶不限数量。
 * </p>
 * <p>
 * 连接模型：面相邻（6 向）的同种方块通过 BFS 视为一张网络。网络能容纳的“种类”上限等于
 * 网络中方块的数量——每个方块占一种 ID；网络里已有同 ID 时直接并入，否则找一个空方块落位。
 * 网络满了的新 ID 不会被吸收，掉落物保持原样。普通实体万象与分选方块是两种方块，
 * 即使相邻也不会连接成同一张网络。
 * </p>
 * <p>
 * 物品形态：吸取时会移除 {@link ItemEntity} 并把数据存进方块，随后由客户端渲染器以
 * “漂浮 + 旋转”的方式画出来，看起来仍像掉落物；但之后不再受掉落物清除（5 分钟消失）、
 * 漏斗吸取等规则影响。管道/漏斗可以通过 {@link ForgeCapabilities#ITEM_HANDLER} 主动抽取，
 * 这是数据层面的接口，与“掉落物实体不被吸走”并不冲突。
 * </p>
 * <p>
 * 加工：方块注册了 Create 的 {@link TransportedItemStackHandlerBehaviour}。每个 A 方块是一个
 * 并行加工槽位，会从整张网络里取一批（最多 {@value #MAX_BATCH_SIZE} 个）加工；所以 X 个相连的 A
 * 可以同时跑 X 批。风机只要扫到网络里任意一个 A，整张网络都算被这道气流覆盖；一台风机只计一次，
 * 多台风机才是多倍速度。物品仍留在拥有者的桶里，只做网络级占位，管道无法把它们抽走；
 * 拆方块/卸载时占位会自动释放，不会把物品锁死。
 * </p>
 * <p>
 * 产物：加工完成产出的新物品会被标成“已加工”后放回网络——优先并入同 ID 的桶，
 * 否则占用网络里的空 A 方块，所以只要还有空位，产物就会直接落进去而不是掉在地上。
 * 已加工的数量不会再被风机/压机选中；被管道、漏斗或玩家取出后标记自然消失。
 * </p>
 * <p>
 * 分选变体可以放入 Create 过滤器，过滤是<b>网络级</b>的：只要相连的分选网络里有任意一个
 * 方块放了过滤器，整张网络都按这些规则判定；多个过滤器取并集（命中任意一个即可）。
 * 过滤器只影响入库/管道插入/加工产物落位，不影响管道/漏斗抽取。
 * </p>
 */
public class EntityMyriadBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** BFS 网络上限，防止极端情况下（例如大面积连成一片）单次搜索过慢。 */
    private static final int MAX_NETWORK_BLOCKS = 512;

    /** 方块被破坏时，单次最多丢出的掉落物堆数；超出部分只记日志，避免瞬间刷爆实体。 */
    private static final int MAX_DROP_STACKS = 256;

    /** 护目镜提示里最多列出的物品行数，超出部分折叠成一行。 */
    private static final int MAX_TOOLTIP_LINES = 8;

    /** 内容物变化后最多每 4 tick 同步一次，避免管道每 tick 抽取都发包。 */
    private static final int SYNC_INTERVAL = 4;

    /** 风机/压机离开后，加工占位最多保留多少 tick，避免物品被无限期锁在“处理中”。 */
    private static final int PROCESSING_LEASE_TICKS = 10;

    /** 某物品被判定为当前机器加工不了后，多久内跳过它，避免每 tick 重复查配方。 */
    private static final int PROCESSING_SKIP_TICKS = 40;

    /** 单个 A 方块一次最多加工的批次大小；16 正好是 Create 时间修正的第一档。 */
    private static final int MAX_BATCH_SIZE = 16;

    /** 网络周围风机的重新扫描间隔；风机气流本身每 30 tick 重建一次。 */
    private static final int FAN_SCAN_INTERVAL = 10;

    private static final String TAG_STORED = "Stored";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_AMOUNT = "Amount";
    private static final String TAG_PROTECTED = "Protected";
    private static final String TAG_FAN_TYPE = "FanType";
    private static final String TAG_FILTER = "Filter";

    private final List<Bucket> buckets = new ArrayList<>();

    /** 内容物有变动、等待下一次节流同步（护目镜提示与方块内展示都依赖客户端数据）。 */
    private boolean syncPending;

    /** 护目镜提示的整张网络内容缓存：每 tick 最多重建一次，避免覆盖层按帧调用时反复 BFS。 */
    private final List<TooltipEntry> tooltipCache = new ArrayList<>();
    private long tooltipCacheTick = Long.MIN_VALUE;

    /** 护目镜提示里的网络过滤器缓存：每 tick 最多重建一次。 */
    private final List<ItemStack> tooltipFilters = new ArrayList<>();
    private long tooltipFilterCacheTick = Long.MIN_VALUE;

    /**
     * Create 的“可被风机/压机加工”的接口，由 {@link #addBehaviours} 注册。
     * <p>注意：SmartBlockEntity 的构造函数会调用 addBehaviours，所以这个字段不能带内联初始化，
     * 否则会在父类构造完成后又把它覆盖成 null。</p>
     */
    private TransportedItemStackHandlerBehaviour transportedHandler;

    /** 当前正在加工的一批物品；这些物品仍在拥有者的桶里，只在抽取时被扣掉。 */
    @Nullable
    private TransportedItemStack processingStack;
    /** 当前批次取自哪个 A 方块（矿车/压机等本方块没有物品时也能从网络里取）。 */
    @Nullable
    private BlockPos processingOwnerPos;
    /** 当前批次的桶模板，用于定位数量与槽位。 */
    @Nullable
    private ItemStack processingTemplate;
    /** 暂时加工不了的模板，跳过一段时间。 */
    @Nullable
    private ItemStack processingSkipTemplate;
    private long processingSkipUntil;
    /** 加工占位的租约；风机/压机持续工作时刷新，逾期释放。 */
    private long processingLeaseUntil;
    /** 正在同步调用压机回调，此时允许它修改锁定中的批次。 */
    private boolean pressingCallback;

    /** 当前正在加工 A 的风机类型；客户端用它生成对应粒子。 */
    @Nullable
    private FanProcessingType fanType;
    /** 风机类型的租约：风机持续调用时刷新，停止后清空。 */
    private long fanTypeLeaseUntil;

    /** 当前覆盖这张网络的每台风机各贡献一个类型；多台同类型就是多倍计时。 */
    private final List<FanProcessingType> affectingFanTypes = new ArrayList<>();
    private long nextFanScanTick;

    private LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();

    /** 分选变体的过滤器槽位；普通实体万象不使用，恒为空。 */
    private ItemStack filterStack = ItemStack.EMPTY;
    /** {@link #filterStack} 的判定缓存；过滤器变化时置空。 */
    @Nullable
    private FilterItemStack filterRule;

    public EntityMyriadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTITY_MYRIAD.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 风机通过这个 behaviour 找到本方块；压机则通过它把物品当作“仓库上的物品”来压。
        transportedHandler = new TransportedItemStackHandlerBehaviour(this, this::applyToAllItems);
        behaviours.add(transportedHandler);
    }

    // ==================== 查询 ====================

    public boolean isEmpty() {
        return buckets.isEmpty();
    }

    /** 客户端渲染用：取第一个桶的模板（数量恒为 1）。 */
    public ItemStack getDisplayStack() {
        return buckets.isEmpty() ? ItemStack.EMPTY : buckets.get(0).template;
    }

    private boolean hasItem(ItemStack stack) {
        return !buckets.isEmpty() && ItemStack.isSameItem(buckets.get(0).template, stack);
    }

    // ==================== 分选过滤器 ====================

    /** 分选方块当前的过滤器；空表示没有规则。返回副本，避免外部改动内部存档数据。 */
    public ItemStack getFilterStack() {
        return filterStack.copy();
    }

    /**
     * 存入一个过滤器（只保留 1 个），返回被替换下来的旧过滤器。
     * <p>过滤器的规则由 Create 的 {@link FilterItem} 提供，并立即参与入库/产物落位判定。</p>
     */
    public ItemStack insertFilter(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FilterItem)) {
            return ItemStack.EMPTY;
        }
        ItemStack previous = filterStack;
        filterStack = stack.copyWithCount(1);
        filterRule = null;
        markChanged();
        return previous;
    }

    /** 取回过滤器；没有时返回空。 */
    public ItemStack extractFilter() {
        ItemStack result = filterStack;
        if (!result.isEmpty()) {
            filterStack = ItemStack.EMPTY;
            filterRule = null;
            markChanged();
        }
        return result;
    }

    // ==================== 本地存取 ====================

    /**
     * 把整份物品并入本方块。
     * <p>本方块为空时可接受任意 ID；否则只接受同 ID（不同 NBT 会新开一个桶）。</p>
     *
     * @return 未存入的余量；成功吸入时恒为 {@link ItemStack#EMPTY}
     */
    private ItemStack insertLocal(ItemStack stack) {
        return insertLocal(stack, false);
    }

    /**
     * @param processed 是否为加工产物。产物数量会单独记入 {@link Bucket#protectedAmount}，
     *                  之后不会被风机/压机再次选中加工。
     */
    private ItemStack insertLocal(ItemStack stack, boolean processed) {
        if (stack.isEmpty() || stack.getCount() <= 0) {
            return ItemStack.EMPTY;
        }
        if (!buckets.isEmpty() && !hasItem(stack)) {
            return stack;
        }
        for (Bucket bucket : buckets) {
            if (ItemStack.isSameItemSameTags(bucket.template, stack)) {
                bucket.amount += stack.getCount();
                if (processed) {
                    bucket.protectedAmount += stack.getCount();
                }
                markChanged();
                return ItemStack.EMPTY;
            }
        }
        Bucket bucket = new Bucket(stack.copyWithCount(1), stack.getCount());
        if (processed) {
            bucket.protectedAmount = stack.getCount();
        }
        buckets.add(bucket);
        markChanged();
        return ItemStack.EMPTY;
    }

    // ==================== 网络 ====================

    /** 收集与本方块面相邻连通的整张网络（含自身），返回其中的方块实体。 */
    private List<EntityMyriadBlockEntity> collectNetwork() {
        List<EntityMyriadBlockEntity> result = new ArrayList<>();
        Level currentLevel = this.level;
        if (currentLevel == null) {
            return result;
        }
        for (BlockPos pos : collectNetworkPositions(currentLevel, worldPosition)) {
            if (currentLevel.getBlockEntity(pos) instanceof EntityMyriadBlockEntity be) {
                result.add(be);
            }
        }
        return result;
    }

    /**
     * 收集一张网络的方块坐标（含起点）。服务端吸取和客户端护目镜高亮共用。
     * <p>只走已加载区块，避免 BFS 顺带把远处区块加载/生成出来。</p>
     */
    public static List<BlockPos> collectNetworkPositions(Level level, BlockPos origin) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        Block originBlock = level.getBlockState(origin).getBlock();

        while (!queue.isEmpty() && result.size() < MAX_NETWORK_BLOCKS) {
            BlockPos current = queue.poll();
            result.add(current);
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.add(next)
                        && level.hasChunkAt(next)
                        && level.getBlockState(next).is(originBlock)) {
                    queue.add(next);
                }
            }
        }
        return result;
    }

    /**
     * 在整张网络里找落点：先做网络级过滤判定，再按同 ID、空方块的顺序选择。
     * 网络里没有任何过滤器时等同普通存储网络。
     */
    @Nullable
    private EntityMyriadBlockEntity findTarget(ItemStack stack) {
        List<EntityMyriadBlockEntity> network = collectNetwork();
        if (!networkAccepts(network, stack)) {
            return null;
        }
        for (EntityMyriadBlockEntity be : network) {
            if (be.hasItem(stack)) {
                return be;
            }
        }
        for (EntityMyriadBlockEntity be : network) {
            if (be.isEmpty()) {
                return be;
            }
        }
        return null;
    }

    /**
     * 网络级过滤：只要相连网络里任意一个方块带过滤器，整张网络都按这些过滤器判定。
     * 多个过滤器取并集——命中任意一个即可入库；没有任何过滤器时全部放行。
     */
    private boolean networkAccepts(List<EntityMyriadBlockEntity> network, ItemStack stack) {
        boolean hasFilter = false;
        for (EntityMyriadBlockEntity be : network) {
            FilterItemStack rule = be.getFilterRule();
            if (rule == null) {
                continue;
            }
            hasFilter = true;
            if (rule.test(level, stack)) {
                return true;
            }
        }
        return !hasFilter;
    }

    /** 本方块过滤器的判定缓存；没有过滤器时为 null。 */
    @Nullable
    private FilterItemStack getFilterRule() {
        if (filterStack.isEmpty()) {
            return null;
        }
        if (filterRule == null) {
            // FilterItemStack.of 会裁剪传入堆叠的 NBT，用副本避免改到存档里的过滤器
            filterRule = FilterItemStack.of(filterStack.copy());
        }
        return filterRule;
    }

    // ==================== 每 tick 逻辑 ====================

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            tickClientParticles();
            return;
        }
        flushSync();
        tickBeltProcessing();
        tickFanProcessing();
        if (processingStack != null && level.getGameTime() > processingLeaseUntil) {
            releaseProcessing();
        }
        // 拥有者方块被拆/卸载：输入已经不在网络里，批次作废
        if (processingStack != null && !isProcessingValid()) {
            clearProcessing();
        }
        if (fanType != null && level.getGameTime() > fanTypeLeaseUntil) {
            updateFanType(null);
        }
        tickAbsorption();
    }

    /**
     * 客户端：风机加工时在方块内部生成该类型的粒子。
     * <p>概率限流，避免一大片网络每格都狂刷粒子；类型自己的 spawn 方法还有更细的概率。</p>
     */
    private void tickClientParticles() {
        // 并行槽位可能从网络里取批，自己桶里反而是空的；只看是否有加工中的风机类型
        if (fanType == null) {
            return;
        }
        if (level.random.nextFloat() > 0.5F) {
            return;
        }
        fanType.spawnProcessingParticles(level, Vec3.atCenterOf(worldPosition));
    }

    /** 供方块注册 ticker 使用（服务端逻辑 + 客户端粒子）。 */
    public static void tickBlockEntity(Level level, BlockPos pos, BlockState state, EntityMyriadBlockEntity be) {
        be.tick();
    }

    private void tickAbsorption() {
        // 方块本身无碰撞箱，这里用它的 1×1×1 包围盒作为“碰撞箱”判断；
        // 掉落物的碰撞箱与之相交时才吸收，符合“通过碰撞箱判断”的要求。
        AABB blockBox = new AABB(worldPosition);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, blockBox);
        for (ItemEntity itemEntity : items) {
            if (itemEntity.isRemoved() || !itemEntity.isAlive()) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || !itemEntity.getBoundingBox().intersects(blockBox)) {
                continue;
            }
            if (absorb(stack)) {
                itemEntity.discard();
            }
        }
    }

    // ==================== 风机 / 压机加工 ====================

    /**
     * 压机通过 {@link TransportedItemStackHandlerBehaviour} 的回调驱动这里；
     * 风机回调不再直接加工（见 {@link #tickFanProcessing()}），否则一台风机扫到网络里多个 A 时会被重复计时。
     */
    private void applyToAllItems(float maxDistanceFromCenter,
                                 Function<TransportedItemStack, TransportedResult> processFunction) {
        // 风机可能持有旧的 behaviour 引用；方块已被移除时不要再加工，避免刷出产物
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return;
        }
        if (level.isClientSide) {
            // Create 的风机在客户端通过这个回调为“每个被加工的物品”生成该类型的粒子。
            // 本方块没有实体物品，只取一个非空桶作代表，避免大网络每格都狂刷粒子。
            for (Bucket bucket : buckets) {
                if (bucket.amount > 0) {
                    processFunction.apply(new TransportedItemStack(bucket.template.copyWithCount(1)));
                    break;
                }
            }
            return;
        }
        if (!pressingCallback) {
            return;
        }
        processBatch(processFunction, false);
    }

    /** 推进当前 A 的一个加工批次；风机和压机共用。 */
    private void processBatch(Function<TransportedItemStack, TransportedResult> processFunction,
                              boolean applyFanType) {
        if (level == null || level.isClientSide) {
            return;
        }
        processingLeaseUntil = level.getGameTime() + PROCESSING_LEASE_TICKS;

        // 拥有者方块被拆/卸载后，这一批的输入已经不在网络里了，不能继续产出。
        if (processingStack != null && !isProcessingValid()) {
            clearProcessing();
        }
        // 压机锁定的批次由压机回调处理，风机不要插手。
        if (processingStack != null && processingStack.locked && !pressingCallback) {
            return;
        }
        if (processingStack == null && !startProcessingBatch()) {
            return;
        }

        ItemStack before = processingStack.stack.copy();
        TransportedResult result = processFunction.apply(processingStack);
        if (applyFanType) {
            fanTypeLeaseUntil = level.getGameTime() + PROCESSING_LEASE_TICKS;
            updateFanType(processingStack != null ? processingStack.processedBy : null);
        }
        if (result == null || result.didntChangeFrom(before)) {
            // 风机判定该物品没有可用配方（processingTime == -1），暂时跳过它。
            if (processingStack != null && processingStack.processingTime == -1) {
                skipProcessingBatch();
            }
            return;
        }
        applyProcessingResult(result);
    }

    // ==================== 网络级风机驱动 ====================

    /**
     * 每台覆盖这张网络的鼓风机各推进一次所有并行槽位。
     * <p>规则：只要某道气流扫到网络里任意一个 A，整张网络都被这道气流视为扫到；
     * 一台风机只计一次，多台风机才是多倍速度。</p>
     */
    private void tickFanProcessing() {
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        if (now >= nextFanScanTick) {
            nextFanScanTick = now + FAN_SCAN_INTERVAL;
            affectingFanTypes.clear();
            scanAffectingFans(collectNetwork(), affectingFanTypes);
        }
        for (FanProcessingType type : affectingFanTypes) {
            advanceFanProcessing(type);
        }
    }

    private void advanceFanProcessing(FanProcessingType type) {
        if (type == null || level == null) {
            return;
        }
        processBatch(transported -> FanProcessing.applyProcessing(transported, level, type), true);
    }

    /** 扫描网络附近的鼓风机：任意一个 A 落在气流扫描范围内，这台风机就覆盖整张网络。 */
    private void scanAffectingFans(List<EntityMyriadBlockEntity> network, List<FanProcessingType> out) {
        if (network.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (EntityMyriadBlockEntity member : network) {
            BlockPos p = member.getBlockPos();
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        int radius = Math.max(AllConfigs.server().kinetics.fanPushDistance.get(),
                AllConfigs.server().kinetics.fanPullDistance.get()) + 8;
        int minChunkX = (minX - radius) >> 4;
        int maxChunkX = (maxX + radius) >> 4;
        int minChunkZ = (minZ - radius) >> 4;
        int maxChunkZ = (maxZ + radius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof EncasedFanBlockEntity fan)) {
                        continue;
                    }
                    FanProcessingType type = fanTypeForNetwork(fan, network);
                    if (type != null) {
                        out.add(type);
                    }
                }
            }
        }
    }

    /** 这台风机是否扫到网络里任意一个 A；返回它在扫描点的处理类型。 */
    @Nullable
    private FanProcessingType fanTypeForNetwork(EncasedFanBlockEntity fan,
                                                List<EntityMyriadBlockEntity> network) {
        AirCurrent current = fan.airCurrent;
        if (current == null || current.direction == null || current.maxDistance < 0.25F) {
            return null;
        }
        Direction dir = current.direction;
        BlockPos start = fan.getAirCurrentPos();
        float maxDistance = current.maxDistance;
        int limit = ((float) (int) maxDistance == maxDistance) ? (int) maxDistance : (int) maxDistance + 1;

        for (EntityMyriadBlockEntity member : network) {
            BlockPos a = member.getBlockPos();
            int dx = a.getX() - start.getX();
            int dy = a.getY() - start.getY();
            int dz = a.getZ() - start.getZ();
            int along = dx * dir.getStepX() + dy * dir.getStepY() + dz * dir.getStepZ();
            if (along < 1 || along > limit) {
                continue;
            }
            int px = dx - dir.getStepX() * along;
            int py = dy - dir.getStepY() * along;
            int pz = dz - dir.getStepZ() * along;
            if (dir.getAxis().isHorizontal()) {
                // Create 的水平气流会同时检查气流格和它下面一格
                if (py != 0 && py != -1) {
                    continue;
                }
                if (px != 0 || pz != 0) {
                    continue;
                }
            } else if (px != 0 || pz != 0) {
                continue;
            }
            FanProcessingType type = FanProcessingType.getAt(level, a);
            if (type == null) {
                type = current.getTypeAt(along - 1.0F);
            }
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    /**
     * 找到上方 1~2 格的压机并驱动它。
     * <p>支持两种摆法：</p>
     * <ul>
     *   <li>压机 → 空气 → 实体万象（标准“压机-仓库”间距，推荐，动画/粒子都对得上）；</li>
     *   <li>压机直接压在实体万象上（也能加工，只是压头会伸进方块里）。</li>
     * </ul>
     */
    private void tickBeltProcessing() {
        PressingBehaviour press = null;
        int distance = 0;
        for (int dy = 1; dy <= 2; dy++) {
            BlockEntityBehaviour behaviour = BlockEntityBehaviour.get(level, worldPosition.above(dy),
                    BeltProcessingBehaviour.TYPE);
            if (behaviour instanceof PressingBehaviour pressing) {
                press = pressing;
                distance = dy;
                break;
            }
        }
        if (press == null) {
            return;
        }

        processingLeaseUntil = level.getGameTime() + PROCESSING_LEASE_TICKS;

        if (processingStack != null && !isProcessingValid()) {
            clearProcessing();
        }
        if (press.specifics.getKineticSpeed() == 0) {
            releaseProcessing();
            return;
        }
        // 两格间距时，检查压头与仓库之间是否被挡住；直接压在上面时不检查（上方就是压机本身）。
        if (distance >= 2 && BeltProcessingBehaviour.isBlocked(level, worldPosition)
                && (processingStack == null || !processingStack.locked)) {
            return;
        }
        if (processingStack == null && !startProcessingBatch()) {
            return;
        }

        // 压机没在运行且配方不匹配：跳过这个模板，避免每 tick 重复查配方。
        if (!press.running && !press.specifics.tryProcessOnBelt(processingStack, null, true)) {
            skipProcessingBatch();
            return;
        }

        boolean wasLocked = processingStack.locked;
        ProcessingResult result;
        pressingCallback = true;
        try {
            result = wasLocked
                    ? press.handleHeldItem(processingStack, transportedHandler)
                    : press.handleReceivedItem(processingStack, transportedHandler);
        } finally {
            pressingCallback = false;
        }

        if (processingStack == null) {
            // 回调里已经把整批加工完并释放了。
            return;
        }
        if (result == ProcessingResult.HOLD) {
            processingStack.locked = true;
        } else if (result == ProcessingResult.REMOVE) {
            releaseProcessing();
        } else {
            // PASS：压机刚结束上一轮或暂时没接活，保留批次，下一 tick 再试。
            processingStack.locked = false;
        }
    }

    /**
     * 从整张网络里挑一个可处理的桶，占一批（最多 {@value #MAX_BATCH_SIZE} 个）。
     * <p>
     * 物品仍留在拥有者的桶里；{@link Bucket#reserved} 记录所有并行槽位占用的总量，
     * 这样 X 个 A 方块可以同时从同一个桶里各拿一批。
     * </p>
     */
    private boolean startProcessingBatch() {
        if (level == null) {
            return false;
        }
        long now = level.getGameTime();
        for (EntityMyriadBlockEntity owner : collectNetwork()) {
            for (Bucket bucket : owner.buckets) {
                if (bucket.amount <= 0) {
                    continue;
                }
                // 已加工产物不再送回机器；只剩这类数量时这个桶直接跳过
                long available = bucket.amount - bucket.reserved - bucket.protectedAmount;
                if (available <= 0) {
                    continue;
                }
                if (processingSkipTemplate != null && now < processingSkipUntil
                        && ItemStack.isSameItemSameTags(processingSkipTemplate, bucket.template)) {
                    continue;
                }
                int max = Math.max(1, bucket.template.getMaxStackSize());
                int cap = Math.min(MAX_BATCH_SIZE, max);
                int batch = (int) Math.min(available, cap);
                bucket.reserved += batch;
                processingTemplate = bucket.template.copy();
                processingOwnerPos = owner.getBlockPos();
                processingStack = new TransportedItemStack(bucket.template.copyWithCount(batch));
                return true;
            }
        }
        return false;
    }

    /** 把一批加工结果写回存储：从拥有者扣输入、把产出并回网络、保留压机剩余的输入。 */
    private void applyProcessingResult(TransportedResult result) {
        if (processingStack == null) {
            return;
        }
        if (!isProcessingValid()) {
            // 拥有者的桶已经不在了（被拆/被抽空），输入已不在网络里，不能再产出
            clearProcessing();
            return;
        }

        int input = processingStack.stack.getCount();
        TransportedItemStack held = result.hasHeldOutput() ? result.getHeldOutput() : null;
        int remaining = held == null ? 0 : held.stack.getCount();
        int consumed = Math.max(0, input - remaining);
        if (!consumeReserved(consumed)) {
            clearProcessing();
            return;
        }

        for (TransportedItemStack output : result.getOutputs()) {
            ItemStack stack = output.stack;
            if (stack.isEmpty()) {
                continue;
            }
            if (!absorbProcessed(stack)) {
                // 网络里没有空位：产出掉到方块旁边，之后空间腾出来还能再吸回去（按普通掉落物处理）
                Block.popResource(level, worldPosition, stack.copy());
            }
        }
        markChanged();

        if (held != null && remaining > 0) {
            processingStack = held;
            processingTemplate = held.stack.copyWithCount(1);
        } else {
            // 整批都已消耗，拥有者的占位也已经扣完；直接清状态，不能再 release 一次
            clearProcessing();
        }
    }

    /** 从拥有者的桶里扣除被加工掉的数量（同时释放对应的占位）。 */
    private boolean consumeReserved(int consumed) {
        if (processingOwnerPos == null || processingTemplate == null || consumed <= 0) {
            return true;
        }
        if (level == null
                || !(level.getBlockEntity(processingOwnerPos) instanceof EntityMyriadBlockEntity owner)) {
            return false;
        }
        for (int i = 0; i < owner.buckets.size(); i++) {
            Bucket bucket = owner.buckets.get(i);
            if (!ItemStack.isSameItemSameTags(bucket.template, processingTemplate)) {
                continue;
            }
            long take = Math.min(bucket.amount, consumed);
            bucket.amount -= take;
            bucket.reserved = Math.max(0L, bucket.reserved - take);
            if (bucket.amount <= 0 && bucket.reserved <= 0) {
                owner.buckets.remove(i);
            }
            owner.markChanged();
            return true;
        }
        return false;
    }

    private void skipProcessingBatch() {
        if (level != null && processingTemplate != null) {
            processingSkipTemplate = processingTemplate.copy();
            processingSkipUntil = level.getGameTime() + PROCESSING_SKIP_TICKS;
        }
        releaseProcessing();
    }

    /** 取消当前批次：把还没消耗的占位还给拥有者，物品仍留在桶里。 */
    private void releaseProcessing() {
        if (processingOwnerPos != null && processingTemplate != null && processingStack != null
                && level != null
                && level.getBlockEntity(processingOwnerPos) instanceof EntityMyriadBlockEntity owner) {
            for (int i = 0; i < owner.buckets.size(); i++) {
                Bucket bucket = owner.buckets.get(i);
                if (!ItemStack.isSameItemSameTags(bucket.template, processingTemplate)) {
                    continue;
                }
                bucket.reserved = Math.max(0L, bucket.reserved - processingStack.stack.getCount());
                if (bucket.amount <= 0 && bucket.reserved <= 0) {
                    owner.buckets.remove(i);
                }
                break;
            }
        }
        clearProcessing();
    }

    private void clearProcessing() {
        processingStack = null;
        processingTemplate = null;
        processingOwnerPos = null;
    }

    /** 拥有者方块/桶是否仍然存在；拆方块或抽空后批次必须作废，避免凭空产出。 */
    private boolean isProcessingValid() {
        if (processingOwnerPos == null || processingTemplate == null || level == null) {
            return false;
        }
        if (!(level.getBlockEntity(processingOwnerPos) instanceof EntityMyriadBlockEntity owner)) {
            return false;
        }
        for (Bucket bucket : owner.buckets) {
            if (ItemStack.isSameItemSameTags(bucket.template, processingTemplate)) {
                return true;
            }
        }
        return false;
    }

    /** 只在服务端调用；类型变化时同步给客户端，用于生成/停止加工粒子。 */
    private void updateFanType(@Nullable FanProcessingType type) {
        if (fanType == type) {
            return;
        }
        fanType = type;
        markChanged();
    }

    /** 桶里可被抽取/再取批的数量；被所有并行槽位占用的部分不可动。 */
    private long availableIn(Bucket bucket) {
        return Math.max(0L, bucket.amount - bucket.reserved);
    }

    /** 把一份掉落物并入网络；网络已满（无同 ID 且无空方块）时返回 false。 */
    private boolean absorb(ItemStack stack) {
        EntityMyriadBlockEntity target = findTarget(stack);
        if (target == null) {
            return false;
        }
        return target.insertLocal(stack).isEmpty();
    }

    /**
     * 把一份加工产物并入网络。落位规则与普通吸收相同：先找同 ID 的桶，其次找空 A 方块，
     * 因此网络里只要还有空位，新产物就会直接放进空位；网络满了才返回 false（由调用方掉落）。
     */
    private boolean absorbProcessed(ItemStack stack) {
        EntityMyriadBlockEntity target = findTarget(stack);
        if (target == null) {
            return false;
        }
        return target.insertLocal(stack, true).isEmpty();
    }

    // ==================== 数据同步 ====================

    private void markChanged() {
        setChanged();
        syncPending = true;
    }

    /**
     * 在 {@link #tick()} 里调用：内容物有变动时节流推送一次方块实体数据。
     * <p>护目镜提示需要客户端拿到完整的桶列表和数量，因此数量变化也要同步；
     * 节流 4 tick 是为了避免管道/漏斗高频抽取时每 tick 发包。</p>
     */
    private void flushSync() {
        if (!syncPending || level == null || level.isClientSide) {
            return;
        }
        if (level.getGameTime() % SYNC_INTERVAL != 0L) {
            return;
        }
        syncPending = false;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ==================== 破坏掉落 ====================

    /** 方块被移除时，把内容物按掉落物形态丢回世界。 */
    public void dropContents(Level level) {
        if (level.isClientSide) {
            return;
        }
        releaseProcessing();
        if (!filterStack.isEmpty()) {
            Block.popResource(level, worldPosition, filterStack.copy());
            filterStack = ItemStack.EMPTY;
            setChanged();
        }
        if (buckets.isEmpty()) {
            return;
        }
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;
        int dropped = 0;
        for (Bucket bucket : buckets) {
            long remaining = bucket.amount;
            int maxStack = Math.max(1, bucket.template.getMaxStackSize());
            while (remaining > 0 && dropped < MAX_DROP_STACKS) {
                int count = (int) Math.min(remaining, maxStack);
                level.addFreshEntity(new ItemEntity(level, x, y, z, bucket.template.copyWithCount(count)));
                remaining -= count;
                dropped++;
            }
            if (remaining > 0) {
                // 数量极端庞大时不再继续刷实体，宁可丢弃剩余部分也要保住服务器帧率
                LOGGER.warn("[CreateVoid] entity_myriad at {} dropped too many items; {} discarded",
                        worldPosition, remaining);
            }
        }
        buckets.clear();
        setChanged();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        releaseProcessing();
    }

    // ==================== 持久化 ====================

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        ListTag list = new ListTag();
        for (Bucket bucket : buckets) {
            CompoundTag entry = new CompoundTag();
            entry.put(TAG_ITEM, bucket.template.save(new CompoundTag()));
            entry.putLong(TAG_AMOUNT, bucket.amount);
            entry.putLong(TAG_PROTECTED, bucket.protectedAmount);
            list.add(entry);
        }
        tag.put(TAG_STORED, list);
        if (!filterStack.isEmpty()) {
            tag.put(TAG_FILTER, filterStack.save(new CompoundTag()));
        }
        if (fanType != null) {
            ResourceLocation key = CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(fanType);
            if (key != null) {
                tag.putString(TAG_FAN_TYPE, key.toString());
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        buckets.clear();
        ListTag list = tag.getList(TAG_STORED, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack stack = ItemStack.of(entry.getCompound(TAG_ITEM));
            long amount = entry.getLong(TAG_AMOUNT);
            if (!stack.isEmpty() && amount > 0) {
                Bucket bucket = new Bucket(stack.copyWithCount(1), amount);
                bucket.protectedAmount = Math.min(entry.getLong(TAG_PROTECTED), amount);
                buckets.add(bucket);
            }
        }
        filterStack = tag.contains(TAG_FILTER) ? ItemStack.of(tag.getCompound(TAG_FILTER)) : ItemStack.EMPTY;
        filterRule = null;
        String fanKey = tag.getString(TAG_FAN_TYPE);
        fanType = fanKey.isEmpty() ? null : FanProcessingType.parse(fanKey);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    // ==================== 护目镜提示 ====================

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 标题左缩进 4 个空格，让左上角的护目镜图标留在标题左侧（与 Create 元件一致）
        tooltip.add(Component.literal("    ")
                .append(Component.translatable("gui.create_void.entity_myriad.title")
                        .withStyle(ChatFormatting.WHITE)));
        addFilterTooltip(tooltip);

        List<TooltipEntry> contents = getNetworkContentsForTooltip();
        if (contents.isEmpty()) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("gui.create_void.entity_myriad.contents_label")
                            .withStyle(ChatFormatting.GRAY)));
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("gui.create_void.entity_myriad.empty")
                            .withStyle(ChatFormatting.GRAY)));
            return true;
        }

        tooltip.add(Component.literal("    ").append(
                Component.translatable("gui.create_void.entity_myriad.contents", contents.size())
                        .withStyle(ChatFormatting.GRAY)));

        int shown = 0;
        for (TooltipEntry entry : contents) {
            if (shown >= MAX_TOOLTIP_LINES) {
                tooltip.add(Component.literal("    ").append(
                        Component.translatable("gui.create_void.entity_myriad.more", contents.size() - shown)
                                .withStyle(ChatFormatting.DARK_GRAY)));
                break;
            }
            MutableComponent line = Component.literal("    ")
                    .append(entry.template.getHoverName().copy().withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" × " + formatAmount(entry.amount))
                            .withStyle(ChatFormatting.GOLD));
            tooltip.add(line);
            shown++;
        }
        return true;
    }

    /**
     * 过滤是网络级的，护目镜里列出整张网络中的全部过滤器。
     * 只有一个过滤器时附带它的规则摘要；多个过滤器只列名字，避免提示过长。
     */
    private void addFilterTooltip(List<Component> tooltip) {
        List<ItemStack> filters = getNetworkFiltersForTooltip();
        for (ItemStack filter : filters) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("gui.create_void.entity_myriad.filter", filter.getHoverName())
                            .withStyle(ChatFormatting.GRAY)));
            if (filters.size() == 1 && filter.getItem() instanceof FilterItem filterItem) {
                for (Component line : filterItem.makeSummary(filter.copy())) {
                    tooltip.add(Component.literal("      ").append(line));
                }
            }
        }
    }

    /** 护目镜用：整张网络里的过滤器（每 tick 最多重建一次）。 */
    private List<ItemStack> getNetworkFiltersForTooltip() {
        Level lvl = this.level;
        long now = lvl == null ? Long.MIN_VALUE : lvl.getGameTime();
        if (now != Long.MIN_VALUE && now == tooltipFilterCacheTick) {
            return tooltipFilters;
        }
        tooltipFilterCacheTick = now;
        tooltipFilters.clear();
        List<EntityMyriadBlockEntity> network = lvl != null && lvl.isClientSide
                ? collectNetwork()
                : List.of(this);
        for (EntityMyriadBlockEntity be : network) {
            if (!be.filterStack.isEmpty()) {
                tooltipFilters.add(be.filterStack.copy());
            }
        }
        return tooltipFilters;
    }

    /**
     * 护目镜看到的“A 的内容物”：相邻方块是一个整体，所以合并整张网络。
     * <p>
     * 按物品 ID 汇总（与存储规则“依据物品ID区分”一致），不同 NBT 在数据里仍各自保留，
     * 只是提示里合并成一行。覆盖层按帧调用本方法，用 gameTime 做缓存，每 tick 最多重建一次。
     * </p>
     */
    private List<TooltipEntry> getNetworkContentsForTooltip() {
        Level lvl = this.level;
        long now = lvl == null ? Long.MIN_VALUE : lvl.getGameTime();
        if (now != Long.MIN_VALUE && now == tooltipCacheTick) {
            return tooltipCache;
        }
        tooltipCacheTick = now;
        tooltipCache.clear();

        List<Bucket> source;
        if (lvl != null && lvl.isClientSide) {
            source = new ArrayList<>();
            for (EntityMyriadBlockEntity be : collectNetwork()) {
                source.addAll(be.buckets);
            }
        } else {
            source = buckets;
        }

        Map<Item, TooltipEntry> byItem = new LinkedHashMap<>();
        for (Bucket bucket : source) {
            byItem.computeIfAbsent(bucket.template.getItem(),
                    item -> new TooltipEntry(new ItemStack(item))).amount += bucket.amount;
        }
        tooltipCache.addAll(byItem.values());
        return tooltipCache;
    }

    private static String formatAmount(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    // ==================== IItemHandler（漏斗 / Create 管道） ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (!itemHandler.isPresent()) {
                itemHandler = LazyOptional.of(this::createItemHandler);
            }
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        itemHandler = LazyOptional.empty();
    }

    /**
     * 对外暴露整张相连网络：漏斗/管道贴在任意一个 A 上，都能看到并抽取所有相连 A 的内容物。
     * <p>
     * 槽位按“物品 + NBT”跨方块合并，加工占位（{@link Bucket#reserved}）不参与抽取；
     * 插入仍遵守“一个方块一种 ID”，自动寻找同 ID 方块或网络里的空方块。
     * </p>
     */
    private IItemHandler createItemHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return Math.max(1, buildNetworkSlots().size());
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                List<NetworkSlot> slots = buildNetworkSlots();
                if (slot < 0 || slot >= slots.size()) {
                    return ItemStack.EMPTY;
                }
                NetworkSlot networkSlot = slots.get(slot);
                return networkSlot.template.copyWithCount(
                        (int) Math.min(networkSlot.amount, Integer.MAX_VALUE));
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                EntityMyriadBlockEntity target = findTarget(stack);
                if (target == null) {
                    return stack;
                }
                return simulate ? ItemStack.EMPTY : target.insertLocal(stack);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                List<NetworkSlot> slots = buildNetworkSlots();
                if (slot < 0 || slot >= slots.size() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                return extractFromNetwork(slots.get(slot).template, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                // 单槽不限数量
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !stack.isEmpty() && findTarget(stack) != null;
            }
        };
    }

    /** 汇总整张网络里可抽取的桶（同一物品 + NBT 跨方块合并，排除加工占位）。 */
    private List<NetworkSlot> buildNetworkSlots() {
        List<NetworkSlot> slots = new ArrayList<>();
        for (EntityMyriadBlockEntity owner : collectNetwork()) {
            for (Bucket bucket : owner.buckets) {
                long available = availableIn(bucket);
                if (available <= 0) {
                    continue;
                }
                NetworkSlot merged = null;
                for (NetworkSlot slot : slots) {
                    if (ItemStack.isSameItemSameTags(slot.template, bucket.template)) {
                        merged = slot;
                        break;
                    }
                }
                if (merged == null) {
                    slots.add(new NetworkSlot(bucket.template.copy(), available));
                } else {
                    merged.amount += available;
                }
            }
        }
        return slots;
    }

    /** 从整张网络抽取指定物品；只动可抽取部分，加工占位不会被动到。 */
    private ItemStack extractFromNetwork(ItemStack template, int amount, boolean simulate) {
        if (template.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int remaining = amount;
        for (EntityMyriadBlockEntity owner : collectNetwork()) {
            for (int i = 0; i < owner.buckets.size() && remaining > 0; i++) {
                Bucket bucket = owner.buckets.get(i);
                if (!ItemStack.isSameItemSameTags(bucket.template, template)) {
                    continue;
                }
                long available = availableIn(bucket);
                if (available <= 0) {
                    continue;
                }
                int take = (int) Math.min(remaining, Math.min(available, Integer.MAX_VALUE));
                if (take <= 0) {
                    continue;
                }
                if (!simulate) {
                    bucket.amount -= take;
                    // 被取走的数量不再留在网络里，已加工标记随之收缩
                    bucket.protectedAmount = Math.min(bucket.protectedAmount, bucket.amount);
                    if (bucket.amount <= 0 && bucket.reserved <= 0) {
                        owner.buckets.remove(i);
                        i--;
                    }
                    owner.markChanged();
                }
                remaining -= take;
            }
            if (remaining <= 0) {
                break;
            }
        }
        int taken = amount - remaining;
        return taken <= 0 ? ItemStack.EMPTY : template.copyWithCount(taken);
    }

    // ==================== 内部结构 ====================

    /**
     * 一个 NBT 桶：同一种物品 ID、同一份 NBT。
     * {@link #template} 恒为数量 1，只用来保存物品与 NBT；真实数量在 {@link #amount}。
     */
    private static final class Bucket {
        private final ItemStack template;
        private long amount;
        /** 被所有并行加工槽位占用的数量；不持久化，重载后自动归零。 */
        private long reserved;
        /** 其中由机器加工产出的数量（属于 {@link #amount}），不会再被机器选中加工。 */
        private long protectedAmount;

        private Bucket(ItemStack template, long amount) {
            this.template = template;
            this.amount = amount;
        }
    }

    /** IItemHandler 对外暴露的合并槽位：同一物品 + NBT 跨方块累加。 */
    private static final class NetworkSlot {
        private final ItemStack template;
        private long amount;

        private NetworkSlot(ItemStack template, long amount) {
            this.template = template;
            this.amount = amount;
        }
    }

    /** 护目镜提示用的合并条目（同一物品 ID 跨方块累加，模板不带 NBT）。 */
    private static final class TooltipEntry {
        private final ItemStack template;
        private long amount;

        private TooltipEntry(ItemStack template) {
            this.template = template;
        }
    }
}
