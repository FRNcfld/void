package com.frnc.createvoid.portal;

import com.frnc.createvoid.particle.ModParticles;
import com.frnc.createvoid.world.dimension.ModDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 虚空传送门方块。
 *
 * <h3>这是原版 {@code NetherPortalBlock} 的移植</h3>
 * <p>
 * 状态、碰撞箱、{@link #updateShape} 的生命周期、{@link #animateTick} 的粒子分布、
 * {@link #rotate}、{@link #getCloneItemStack} 都与原版逐方法对应。差异只有两处，
 * 都是"虚空维度不在原版维度配对里"这个事实逼出来的：
 * </p>
 *
 * <h3>差异一：{@link #entityInside} 只在客户端调 {@code entity.handleInsidePortal}</h3>
 * <p>
 * 原版 {@code NetherPortalBlock.entityInside} 只有一行
 * {@code pEntity.handleInsidePortal(pPos)}，<b>两端都会执行</b>。它做的事是按
 * 客户端与服务端分开解读的：
 * </p>
 * <ul>
 *   <li><b>客户端</b>：只把实体标记为"在门里"，由
 *       {@code LocalPlayer.handleNetherPortalClient()} 消费——累计
 *       {@code spinningEffectIntensity} 产生屏幕扭曲，并播放一次
 *       {@code PORTAL_TRIGGER} 音效。所以这一份必须留着。</li>
 *   <li><b>服务端</b>：同一个标记会被下一 tick 的
 *       {@code Entity.handleNetherPortal()} 消费，而它把目标维度写死成：
 *       <pre>
 * ResourceKey&lt;Level&gt; resourcekey =
 *     this.level().dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
 *       </pre>
 *       虚空维度不在这条规则里 —— 服务端一设这个标记就会把玩家<b>送进地狱</b>。</li>
 * </ul>
 * <p>
 * 因此客户端照搬原版，服务端则<b>绝不能</b>设这个标记，改由本类自己完成
 * "计时 → 判定 → 传送"，计时的行为由 {@link VoidPortalTimers} 对齐原版。
 * 宿主侧也没有可插入的扩展点：1.20.1 的 Forge 还没有 1.20.2+ 的 {@code Portal}
 * 接口，那份 {@code portalTime} 字段也是私有的。
 * </p>
 *
 * <h3>差异二：目标维度是二元判定，不做来源记忆</h3>
 * <p>
 * 与原版同构：原版是「地狱 → 主世界，其余 → 地狱」，这里是
 * 「虚空 → 主世界，其余 → 虚空」。因此从地狱进虚空门会到虚空，
 * 从虚空回来则回主世界 —— 与原版把门建在末地时的行为一致。
 * </p>
 *
 * @see VoidPortalShape
 * @see VoidPortalTeleporter
 */
public class VoidPortal extends Block {

    /** 门的平面所展开的水平轴。 */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    /** 与原版地狱门相同的扁平碰撞箱（门是一层薄幕，不是整方块）。 */
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public VoidPortal(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    // ==================== 状态 / 形状 ====================

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_AABB : X_AXIS_AABB;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch (state.getValue(AXIS)) {
                    case X:
                        return state.setValue(AXIS, Direction.Axis.Z);
                    case Z:
                        return state.setValue(AXIS, Direction.Axis.X);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }

    /** 门方块不掉落，也没有物品形态。 */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    // ==================== 生命周期（照搬原版） ====================

    /**
     * 邻居变化后复查形状；框架不再完整则变回空气。
     * <p>
     * 与原版一致：只有「改动方向属于门的平面轴」时才跳过复查。
     * 判定用 {@link VoidPortalShape#isComplete()}——它从门方块自身重建整个形状，
     * 因此不依赖"相邻格里有框架"。
     * </p>
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = direction.getAxis();
        Direction.Axis doorAxis = state.getValue(AXIS);
        boolean crossAxis = axis != doorAxis && axis.isHorizontal();
        if (!crossAxis && !neighborState.is(this)
                && !new VoidPortalShape(level, pos, doorAxis, VoidPortalFrame.predicate()).isComplete()) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // ==================== 传送 ====================

    /**
     * 实体走进门内时推进驻留计时，达到 {@code getPortalWaitTime()} 才传送。
     * <p>详见类注释的「差异一」。</p>
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!entity.canChangeDimensions()) {
            return;
        }
        // 客户端：只标记"在门里"。原版 NetherPortalBlock 是两端都调 handleInsidePortal 的，
        // 客户端那份标记驱动屏幕扭曲（LocalPlayer.handleNetherPortalClient 累计的
        // spinningEffectIntensity）与 PORTAL_TRIGGER 音效。
        // 这里必须自己在客户端调用，否则进门只有音效缺失、连画面都不带扭的。
        //
        // 服务端则绝不能设这个标记：Entity.handleNetherPortal() 读的正是它，
        // 而那个方法把目标维度写死成地狱，一设就会把玩家送去地狱。
        if (level.isClientSide) {
            entity.handleInsidePortal(pos);
            return;
        }
        if (!(level instanceof ServerLevel serverLevel) || entity.isPassenger()) {
            return;
        }
        // 与原版 handleInsidePortal 一致：冷却期内持续刷新冷却，不累计驻留时间
        if (entity.isOnPortalCooldown()) {
            entity.setPortalCooldown();
            return;
        }

        // 与原版 handleNetherPortal 一致，比较的是"自增前"的驻留 tick 数
        if (VoidPortalTimers.advance(level, entity) < entity.getPortalWaitTime()) {
            return;
        }

        ServerLevel destination = resolveDestination(serverLevel);
        VoidPortalTimers.forget(entity);
        if (destination == null || destination == serverLevel) {
            return;
        }
        // 与原版一致：先上冷却再传送，新实体会继承它，避免刚落地就被送回来
        entity.setPortalCooldown();
        if (entity.changeDimension(destination, new VoidPortalTeleporter(destination)) == null) {
            // 传送失败（目标维度建不出门、维度被禁用等）。必须撤销冷却：
            // 否则下一 tick 会走进"在冷却中 → setPortalCooldown() 刷新冷却 → 仍在冷却中"
            // 的循环，实体被永久锁在门里出不来。
            // 原版没有这个分支，是因为它的原版维度配对下传送几乎不会失败。
            entity.setPortalCooldown(0);
        }
    }

    /**
     * 决定传送目标，与原版 {@code NetherPortalBlock} 的二元判定同构。
     *
     * @return 目标维度；维度不可用时返回 {@code null}
     */
    @Nullable
    private static ServerLevel resolveDestination(ServerLevel from) {
        MinecraftServer server = from.getServer();
        if (server == null) {
            return null;
        }
        return from.dimension().equals(ModDimension.VOID_LEVEL_KEY)
                ? server.getLevel(Level.OVERWORLD)
                : server.getLevel(ModDimension.VOID_LEVEL_KEY);
    }

    // ==================== 表现 ====================

    /**
     * 粒子分布与环境音照搬原版，但<b>粒子类型换成 {@link ModParticles#VOID_PORTAL}</b>。
     * <p>
     * 那是本 mod 自己的粒子：外形继承原版 {@code PortalParticle}，只把配色改成天境
     * 那种蓝（见 {@code VoidPortalParticle}）。位置抖动、沿门平面翻转的那套逻辑
     * 与原版 {@code NetherPortalBlock#animateTick} 逐行一致，也与天境一致。
     * </p>
     * <p>
     * 环境音仍是原版的 {@code PORTAL_AMBIENT}——天境用的是自定义音效加淡出，
     * 需要额外搬运音频资源，这里刻意不跟。
     * </p>
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double vx = (random.nextDouble() - 0.5D) * 0.5D;
            double vy = (random.nextDouble() - 0.5D) * 0.5D;
            double vz = (random.nextDouble() - 0.5D) * 0.5D;
            int flip = random.nextInt(2) * 2 - 1;
            // 沿门的平面轴抖动，使粒子贴着门幕而不是从整块方块的体积里冒出来
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = pos.getX() + 0.5D + 0.25D * flip;
                vx = random.nextFloat() * 2.0F * flip;
            } else {
                z = pos.getZ() + 0.5D + 0.25D * flip;
                vz = random.nextFloat() * 2.0F * flip;
            }
            level.addParticle(ModParticles.VOID_PORTAL.get(), x, y, z, vx, vy, vz);
        }
    }

    /**
     * 原版下界门会在自然维度里随机刷僵尸猪灵，这里照搬。
     * <p>
     * 注意虚空维度的 {@code dimension_type} 是 {@code "natural": true}，
     * 所以门在虚空一侧会真的刷出僵尸猪灵。若不需要，删掉本方法即可。
     * </p>
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.dimensionType().natural()
                && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                && random.nextInt(2000) < level.getDifficulty().getId()) {
            while (level.getBlockState(pos).is(this)) {
                pos = pos.below();
            }
            if (level.getBlockState(pos).isValidSpawn(level, pos, EntityType.ZOMBIFIED_PIGLIN)) {
                Entity piglin = EntityType.ZOMBIFIED_PIGLIN.spawn(level, pos.above(), MobSpawnType.STRUCTURE);
                if (piglin != null) {
                    piglin.setPortalCooldown();
                }
            }
        }
    }

    // ==================== 定位门方块 ====================

    /** 按包围盒取整找方块时的修正量，与 {@code Entity#checkInsideBlocks} 用的是同一个值。 */
    private static final double BOUNDS_EPSILON = 1.0E-7;

    /**
     * 找出实体包围盒所覆盖的虚空门方块，取最低的一格。
     *
     * <p>
     * 不能用 {@code entity.blockPosition()} 代替：它只是 {@code floor(position)}，而实体贴地时
     * {@code position.y} 常常落在方块边界<b>略下方</b>，floor 之后取到的是脚下方那格——门内的
     * 地面恰是框架（机壳），于是会误判成"不在门里"。
     * </p>
     * <p>
     * 取整方式与 {@code Entity#checkInsideBlocks} 逐字一致（最小值加 {@value #BOUNDS_EPSILON}、
     * 最大值减同样的量）。这不是巧合：{@code entityInside} 正是由那个方法按同样的范围遍历
     * 调用的，因此触发它的门方块必然落在本方法的扫描范围内——这是构造上保证的。
     * </p>
     *
     * @return 门方块坐标；实体没压在门上时返回 {@code null}
     */
    @Nullable
    public static BlockPos findPortalBlockAt(Level level, Entity entity) {
        AABB box = entity.getBoundingBox();
        BlockPos min = BlockPos.containing(box.minX + BOUNDS_EPSILON, box.minY + BOUNDS_EPSILON, box.minZ + BOUNDS_EPSILON);
        BlockPos max = BlockPos.containing(box.maxX - BOUNDS_EPSILON, box.maxY - BOUNDS_EPSILON, box.maxZ - BOUNDS_EPSILON);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos lowest = null;
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).getBlock() instanceof VoidPortal
                            && (lowest == null || y < lowest.getY())) {
                        lowest = cursor.immutable();
                    }
                }
            }
        }
        return lowest;
    }

    // ==================== 点燃 ====================

    /**
     * 尝试点燃传送门（由 {@link PortalIgnitionHandler} 调用）。
     *
     * @param level       世界（服务端）
     * @param clickedPos  被点击的框架方块位置
     * @param clickedFace 被点击的面
     * @param player      点燃的玩家（用于取朝向推出门平面轴）
     * @return 是否成功点燃
     */
    public static boolean tryLight(Level level, BlockPos clickedPos, Direction clickedFace,
                                   Player player) {
        if (level.isClientSide || clickedFace == null) {
            return false;
        }
        if (!VoidPortalFrame.isFrame(level.getBlockState(clickedPos))) {
            return false;
        }

        // 门平面轴：照抄原版 BaseFireBlock —— 取玩家水平朝向的逆时针轴，
        // 玩家正对框架时，该轴即为门的平面所展开的方向。
        Direction facing = player.getDirection();
        Direction.Axis axis = facing.getAxis().isHorizontal()
                ? facing.getCounterClockWise().getAxis()
                : Direction.Plane.HORIZONTAL.getRandomAxis(level.random);

        // 候选内部格：原版只试「点击位朝点击面偏移一格」；这里再补上点击块的
        // 其余邻面，因为右键框架时点在背面/侧面是常事，而形状检测本身不可能误判
        // （尺寸猜错只会验证失败），多试几个位置只增加成功率，不增加误判风险。
        Direction[] faces = Direction.values();
        BlockPos[] candidates = new BlockPos[faces.length];
        candidates[0] = clickedPos.relative(clickedFace);
        int count = 1;
        for (Direction face : faces) {
            if (face != clickedFace) {
                candidates[count++] = clickedPos.relative(face);
            }
        }

        for (int i = 0; i < count; i++) {
            Optional<VoidPortalShape> shape = VoidPortalShape.findEmptyPortalShape(
                    level, candidates[i], axis, VoidPortalFrame.predicate());
            if (shape.isPresent() && shape.get().createPortalBlocks() > 0) {
                level.playSound(null, clickedPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                        1.0F, level.random.nextFloat() * 0.4F + 0.8F);
                return true;
            }
        }
        return false;
    }
}
