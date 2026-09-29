package com.frnc.createvoid.portal;

import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * 虚空传送门的跨维度传送器：负责在目标维度「找门 / 建门」，并把实体放进门里。
 *
 * <h3>为什么必须自己实现 {@link #getPortalInfo}</h3>
 * <p>
 * 这不是可选的。原版 {@code Entity.findDimensionEntryPoint} 把维度组合写死成
 * 主世界/地狱/末地三者：
 * </p>
 * <pre>
 * boolean flag2 = pDestination.dimension() == Level.NETHER;
 * if (this.level().dimension() != Level.NETHER &amp;&amp; !flag2) {
 *     return null;   // ← 自定义维度在这里返回 null
 * }
 * </pre>
 * <p>
 * {@code ITeleporter} 的默认 {@code getPortalInfo} 对本实现（{@code isVanilla()}
 * 为 false）只会返回 {@code entity.position()}——既不按 {@code coordinate_scale}
 * 换算，也不找门建门。所以两者都指望不上，只能自己给出 {@link PortalInfo}。
 * </p>
 * <p>
 * 反过来说，<b>不需要覆写 {@code placeEntity}</b>：它的默认实现就是
 * {@code repositionEntity.apply(true)}，而那段原版逻辑会拿这里返回的
 * {@link PortalInfo} 去 {@code moveTo} 并 {@code setDeltaMovement(speed)}。
 * 本类把 speed 设为 {@link Vec3#ZERO}，因此落点速度自然被清零。
 * </p>
 *
 * <h3>坐标换算</h3>
 * <p>
 * 用 {@code DimensionType.getTeleportationScale(src, dst)}，即原版同款
 * {@code v * dst / src}。主世界与虚空两边都是 1.0 → <b>1 : 1</b>
 * （与原版主世界↔地狱是 8 : 1 出自同一个公式）。
 * </p>
 */
public class VoidPortalTeleporter implements ITeleporter {

    /** 目标维度是虚空（"小"的一侧）时的搜索半径。 */
    private static final int SEARCH_RADIUS_SMALL = 16;

    /** 目标维度是主世界时的搜索半径。 */
    private static final int SEARCH_RADIUS_LARGE = 128;

    /** 自动生成的传送门内部尺寸（框架外框 4×5，与原版最小尺寸一致）。 */
    private static final int CREATE_WIDTH = 2;
    private static final int CREATE_HEIGHT = 3;

    private final ServerLevel targetLevel;
    private final double targetScale;

    public VoidPortalTeleporter(ServerLevel targetLevel) {
        this.targetLevel = targetLevel;
        this.targetScale = targetLevel.dimensionType().coordinateScale();
    }

    // ==================== ITeleporter ====================

    /**
     * 给出实体在目标维度的落点。
     * <p>
     * 返回 {@code null} 会中止本次传送（{@code Entity.changeDimension} 会直接
     * 返回 null），实体留在源维度——这正是目标维度地形不可用时的期望行为。
     * </p>
     * <p>
     * 源维度取自 {@code entity.level()}：调用点（{@code Entity.changeDimension}
     * 与 {@code ServerPlayer.changeDimension}）都在切换维度<b>之前</b>询问，
     * 此时实体的 level 仍是源维度。
     * </p>
     */
    @Override
    @Nullable
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel,
                                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        ServerLevel source = entity.level() instanceof ServerLevel serverLevel ? serverLevel : destLevel;

        // 定位实体所在的那一格门方块。不能用 entity.blockPosition()（它只是 floor(y)，
        // 实体贴地时会取到脚下方那格框架而不是门方块），详见 VoidPortal#findPortalBlockAt。
        //
        // 原版没这个问题，是因为它用的是 Entity.portalEntrancePos —— 那个值直接来自
        // BlockState#entityInside 传入的门方块坐标，从不由实体位置反推。（该字段是
        // protected，取不到，所以这里按包围盒自己找。）
        BlockPos entryPos = VoidPortal.findPortalBlockAt(source, entity);

        // 只有玩家会建门 —— 这是原版把"找"和"建"拆开的地方：
        //   Entity#getExitPortal        → 只 findPortalAround，从不新建
        //   ServerPlayer#getExitPortal  → 找不到才 createPortal
        // Create 的列车用的是探测实体（SuperGlueEntity），所以它走的是"只找不建"那条：
        // 半径内没有已存在的门就干脆不连通，绝不会凭空在对面建一座。
        boolean mayCreate = entity instanceof ServerPlayer;
        BlockUtil.FoundRectangle destination =
                resolveDestination(entryPos != null ? entryPos : entity.blockPosition(), source, mayCreate);
        if (destination == null) {
            return null;
        }

        // 原版做法（Entity#findDimensionEntryPoint）：量出源侧整座门的矩形，算出实体
        // 在其中的<b>相对</b>位置，再交给 PortalShape#createPortalInfo 映射到目标矩形上。
        //
        // 缺了这一步，落点就与"从门的哪个位置进的"完全无关——从 A 门顶部进去也会从
        // B 门底部冒出来。此前只返回单个方块坐标 + 0.1 的高度偏移，就是这个毛病。
        // 兜底值取自原版 findDimensionEntryPoint 的 else 分支，注意 z 分量是 0.0 而不是 0.5：
        // createPortalInfo 里厚度方向的坐标算的是 `0.5 + relative.z()`，
        // 填 0.5 会把它推成 1.0，落点整整偏出半格、可能卡进框架。
        Direction.Axis axis = Direction.Axis.X;
        Vec3 relative = new Vec3(0.5D, 0.0D, 0.0D);
        if (entryPos != null) {
            BlockState sourceState = source.getBlockState(entryPos);
            if (sourceState.hasProperty(VoidPortal.AXIS)) {
                axis = sourceState.getValue(VoidPortal.AXIS);
                BlockUtil.FoundRectangle sourceRect = BlockUtil.getLargestRectangleAround(
                        entryPos, axis, VoidPortalShape.MAX_WIDTH,
                        Direction.Axis.Y, VoidPortalShape.MAX_HEIGHT,
                        pos -> source.getBlockState(pos).getBlock() instanceof VoidPortal);
                relative = PortalShape.getRelativePosition(
                        sourceRect, axis, entity.position(), entity.getDimensions(entity.getPose()));
            }
        }

        // createPortalInfo 还会一并处理：门平面轴向不同时把朝向转 90°、把落点调整到不卡进方块的位置
        return PortalShape.createPortalInfo(destLevel, destination, axis, relative, entity,
                Vec3.ZERO, entity.getYRot(), entity.getXRot());
    }

    // ==================== 找门 / 建门 ====================

    /**
     * 按坐标比例换算到目标维度，先找已有门；找不到且 {@code mayCreate} 为真时才新建一座。
     *
     * @param pos         源维度中的位置
     * @param sourceLevel 源维度（用于取它的 {@code coordinate_scale}）
     * @param mayCreate   找不到门时是否允许新建。只有玩家为真，见 {@link #getPortalInfo}
     * @return 目标维度中<b>整座门</b>的矩形；没找到且不允许新建、或地形不可用时返回 {@code null}
     */
    @Nullable
    public BlockUtil.FoundRectangle resolveDestination(BlockPos pos, ServerLevel sourceLevel, boolean mayCreate) {
        double fromScale = sourceLevel.dimensionType().coordinateScale();
        BlockPos scaled = scalePos(pos, fromScale, this.targetScale);
        Optional<BlockUtil.FoundRectangle> found = findClosestPortal(scaled);
        if (found.isPresent()) {
            return found.get();
        }
        return mayCreate ? createPortal(scaled) : null;
    }

    /** 按坐标比例把位置换算到目标维度（原版对 x/z 取 floor，y 不缩放）。 */
    public static BlockPos scalePos(BlockPos pos, double fromScale, double toScale) {
        double factor = toScale / fromScale;
        return new BlockPos(
                (int) Math.floor(pos.getX() * factor),
                pos.getY(),
                (int) Math.floor(pos.getZ() * factor));
    }

    /**
     * 以 {@code center} 为中心在 POI 索引里找最近的一座传送门。
     *
     * <h4>只按 XZ 过滤，刻意不限制 Y</h4>
     * <p>
     * 这正是原版 {@code PortalForcer#findPortalAround} 的做法，也是<b>往返不再重复
     * 建门</b>的关键。坐标是 1:1，对面那座门就在相同的 XZ 上，但高度可能差几十格
     * （主世界的门在 y=64，虚空侧自动生成的门在地表 y≈1）。早先按
     * {@code center.getY() ± 16} 扫描，窗口根本够不到对面的门，于是每次往返都新建一座。
     * </p>
     * <p>
     * {@code ensureLoadedAndValid} 把范围内的区块加载到 {@code ChunkStatus.EMPTY}
     * 以便读 POI 数据——与原版一致，代价远低于加载完整方块数据。
     * </p>
     */
    /**
     * 找门时用的水平半径。
     * <p>
     * 照搬原版 {@code PortalForcer#findPortalAround} 的 {@code pIsNether ? 16 : 128}：
     * 去主世界放宽到 128，去"小"的一侧收窄到 16。原版这个不对称来自地狱坐标被压缩
     * 8 倍；本 mod 的虚空与主世界是 1:1，所以它更像"往主世界方向找得更宽"的语义——
     * 但这一点很重要：<b>只要在半径内找到已有的门，就不会再新建一座</b>。
     * </p>
     */
    private int searchRadius() {
        return this.targetLevel.dimension().equals(Level.OVERWORLD)
                ? SEARCH_RADIUS_LARGE
                : SEARCH_RADIUS_SMALL;
    }

    private Optional<BlockUtil.FoundRectangle> findClosestPortal(BlockPos center) {
        int radius = searchRadius();
        PoiManager poiManager = this.targetLevel.getPoiManager();
        poiManager.ensureLoadedAndValid(this.targetLevel, center, radius);

        return poiManager.getInSquare(
                        holder -> holder.is(VoidPortalPoi.VOID_PORTAL_KEY),
                        center, radius, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                // POI 记录只能说"这里曾经有过门"：方块变化是下一 tick 才清记录的，
                // 所以仍然核对一次实际方块，避免把实体送进已经拆掉的门
                .filter(pos -> this.targetLevel.getBlockState(pos).getBlock() instanceof VoidPortal)
                .min(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(center)))
                .map(this::rectangleOf);
    }

    /**
     * 由门内的任意一格推出<b>整座门</b>的矩形。
     * <p>
     * 原版 {@code Entity#findDimensionEntryPoint} 与 {@code PortalForcer#findPortalAround}
     * 都是这么做的。拿到矩形而不是单格坐标，是"按进门位置决定出门位置"的前提——
     * 单格坐标只能给出"落到那一格"，与实体的相对位置无关。
     * </p>
     */
    private BlockUtil.FoundRectangle rectangleOf(BlockPos portalBlockPos) {
        Direction.Axis axis = this.targetLevel.getBlockState(portalBlockPos).getValue(VoidPortal.AXIS);
        return BlockUtil.getLargestRectangleAround(
                portalBlockPos, axis, VoidPortalShape.MAX_WIDTH,
                Direction.Axis.Y, VoidPortalShape.MAX_HEIGHT,
                pos -> this.targetLevel.getBlockState(pos).getBlock() instanceof VoidPortal);
    }

    /**
     * 在 {@code pos} 附近新建一座传送门，返回门内中心位置。
     * <p>
     * 门沿 <b>X 轴</b>展开（门方块的 {@code AXIS} = X）。摆好框架后交给
     * {@link VoidPortalShape} 自行检测并填充内部，<b>不另写一套摆放逻辑</b>——
     * 形状与朝向由唯一一份检测代码决定，两者不可能不一致。
     * </p>
     */
    @Nullable
    private BlockUtil.FoundRectangle createPortal(BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();

        int surfaceY = this.targetLevel.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int y = findClearY(x, surfaceY, z);

        clearArea(x, y, z);

        // 外框比内部各大 1 格
        BlockState frame = VoidPortalFrame.railwayCasingState();
        if (frame == null) {
            return null;
        }
        for (int u = -1; u <= CREATE_WIDTH; u++) {
            for (int v = -1; v <= CREATE_HEIGHT; v++) {
                boolean border = u == -1 || u == CREATE_WIDTH || v == -1 || v == CREATE_HEIGHT;
                if (border) {
                    this.targetLevel.setBlock(new BlockPos(x + u, y + v, z), frame, Block.UPDATE_ALL);
                }
            }
        }
        // 门底垫一层，避免悬空
        for (int u = -1; u <= CREATE_WIDTH; u++) {
            BlockPos floor = new BlockPos(x + u, y - 1, z);
            if (this.targetLevel.getBlockState(floor).isAir()) {
                this.targetLevel.setBlock(floor, frame, Block.UPDATE_ALL);
            }
        }

        Optional<VoidPortalShape> shape = VoidPortalShape.findEmptyPortalShape(
                this.targetLevel, new BlockPos(x, y, z), Direction.Axis.X, VoidPortalFrame.predicate());
        if (shape.isEmpty() || shape.get().createPortalBlocks() == 0) {
            // 理论上不该发生；真发生了说明地形不可用，放弃这次建门
            return null;
        }
        return rectangleOf(shape.get().getCenter());
    }

    /** 从 {@code startY} 向上找连续两格空气的位置。 */
    private int findClearY(int x, int startY, int z) {
        int y = Math.max(this.targetLevel.getMinBuildHeight() + 1, startY);
        int maxY = this.targetLevel.getMaxBuildHeight() - CREATE_HEIGHT - 2;
        for (; y < maxY; y++) {
            if (this.targetLevel.getBlockState(new BlockPos(x, y, z)).isAir()
                    && this.targetLevel.getBlockState(new BlockPos(x, y + 1, z)).isAir()) {
                return y;
            }
        }
        return y;
    }

    /** 把框架与内部空间范围内的空气与残留门方块清掉。 */
    private void clearArea(int x, int y, int z) {
        for (int dx = -1; dx <= CREATE_WIDTH; dx++) {
            for (int dy = -1; dy <= CREATE_HEIGHT + 1; dy++) {
                BlockPos p = new BlockPos(x + dx, y + dy, z);
                BlockState state = this.targetLevel.getBlockState(p);
                if (state.isAir() || state.getBlock() instanceof VoidPortal) {
                    this.targetLevel.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
