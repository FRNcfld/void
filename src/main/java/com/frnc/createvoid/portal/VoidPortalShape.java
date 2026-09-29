package com.frnc.createvoid.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 虚空传送门的形状检测。
 *
 * <h3>这是原版 {@code PortalShape} 的逐方法移植</h3>
 * <p>
 * 算法、常量、分支顺序都与原版一致，唯一的差异是把框架谓词
 * {@code FRAME} 从"写死黑曜石"改成构造参数（见 {@link VoidPortalFrame}）。
 * 因此本类可以直接对照原版 {@code net.minecraft.world.level.portal.PortalShape}
 * 阅读，不存在自创的启发式。
 * </p>
 *
 * <h3>算法</h3>
 * <ol>
 *   <li>{@link #calculateBottomLeft}：从候选点<b>一直向下</b>走到内部矩形底边，
 *       再沿 {@code rightDir} 的反方向退到左下角</li>
 *   <li>{@link #calculateWidth}：沿 {@code rightDir} 数到右边框。原版在此要求
 *       每个内部列的下方都是框架，因此不会把"门上方的露天空气"误算成内部</li>
 *   <li>{@link #calculateHeight}：数到顶边框，并要求顶边整条都是框架</li>
 * </ol>
 *
 * <h3>两个入口（与原版同构）</h3>
 * <ul>
 *   <li>{@link #findEmptyPortalShape}：内部还没有门方块——<b>点燃路径</b>用</li>
 *   <li>{@link #isComplete}：内部整片都是门方块——门方块的 {@code updateShape} 用</li>
 * </ul>
 * <p>
 * 这两个必须分开：若点燃路径也用"要求已点燃"的判定，空框架将永远点不着。
 * </p>
 *
 * <h3>双轴回退</h3>
 * <p>
 * {@link #findPortalShape} 会先按传入轴构造，失败后自动换另一个水平轴重试——
 * 这是原版 {@code findPortalShape} 的既有行为，不是本类新增的。
 * </p>
 */
public class VoidPortalShape {

    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;

    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    /** 框架谓词。原版是写死黑曜石的 private static 常量，这里是构造参数。 */
    private final Predicate<BlockState> isFrame;

    private int numPortalBlocks;
    @Nullable
    private BlockPos bottomLeft;
    private int height;
    private final int width;

    // ==================== 查找 ====================

    /**
     * 寻找一个框架合法、且内部尚未被点燃的传送门形状。
     *
     * @param level      世界
     * @param bottomLeft 候选的内部格（通常是点击面相邻的那一格）
     * @param axis       门的平面所展开的水平轴
     * @param isFrame    框架谓词
     */
    public static Optional<VoidPortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos bottomLeft,
                                                                 Direction.Axis axis,
                                                                 Predicate<BlockState> isFrame) {
        return findPortalShape(level, bottomLeft,
                shape -> shape.isValid() && shape.numPortalBlocks == 0, axis, isFrame);
    }

    /** 按 {@code axis} 构造并过滤；失败则换另一个水平轴重试（原版既有行为）。 */
    public static Optional<VoidPortalShape> findPortalShape(LevelAccessor level, BlockPos bottomLeft,
                                                            Predicate<VoidPortalShape> predicate,
                                                            Direction.Axis axis,
                                                            Predicate<BlockState> isFrame) {
        Optional<VoidPortalShape> optional =
                Optional.of(new VoidPortalShape(level, bottomLeft, axis, isFrame)).filter(predicate);
        if (optional.isPresent()) {
            return optional;
        }
        Direction.Axis otherAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Optional.of(new VoidPortalShape(level, bottomLeft, otherAxis, isFrame)).filter(predicate);
    }

    public VoidPortalShape(LevelAccessor level, BlockPos bottomLeft, Direction.Axis axis,
                           Predicate<BlockState> isFrame) {
        this.level = level;
        this.axis = axis;
        this.isFrame = isFrame;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        this.bottomLeft = this.calculateBottomLeft(bottomLeft);
        if (this.bottomLeft == null) {
            // 退化为 1×1 的无效形状，由 isValid() 拒掉（与原版一致）
            this.bottomLeft = bottomLeft;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = this.calculateWidth();
            if (this.width > 0) {
                this.height = this.calculateHeight();
            }
        }
    }

    // ==================== 尺寸测量 ====================

    @Nullable
    private BlockPos calculateBottomLeft(BlockPos pos) {
        for (int minY = Math.max(this.level.getMinBuildHeight(), pos.getY() - MAX_HEIGHT);
             pos.getY() > minY && isEmpty(this.level.getBlockState(pos.below()));
             pos = pos.below()) {
            // 一路向下走到内部底边
        }
        Direction direction = this.rightDir.getOpposite();
        int distance = this.getDistanceUntilEdgeAboveFrame(pos, direction) - 1;
        return distance < 0 ? null : pos.relative(direction, distance);
    }

    private int calculateWidth() {
        int width = this.getDistanceUntilEdgeAboveFrame(this.bottomLeft, this.rightDir);
        return width >= MIN_WIDTH && width <= MAX_WIDTH ? width : 0;
    }

    /**
     * 沿 {@code direction} 数到框架为止，用于确定宽度。
     * <p>每个空格都必须<b>下方也是框架</b>，否则中断——这一步保证不会把
     * "门上方的露天空气"当成内部空间。</p>
     */
    private int getDistanceUntilEdgeAboveFrame(BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i <= MAX_WIDTH; i++) {
            cursor.set(pos).move(direction, i);
            BlockState state = this.level.getBlockState(cursor);
            if (!isEmpty(state)) {
                if (this.isFrame.test(state)) {
                    return i;
                }
                break;
            }
            BlockState below = this.level.getBlockState(cursor.move(Direction.DOWN));
            if (!this.isFrame.test(below)) {
                break;
            }
        }
        return 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int height = this.getDistanceUntilTop(cursor);
        return height >= MIN_HEIGHT && height <= MAX_HEIGHT && this.hasTopFrame(cursor, height) ? height : 0;
    }

    /** 顶边整条必须都是框架。 */
    private boolean hasTopFrame(BlockPos.MutableBlockPos cursor, int distanceToTop) {
        for (int i = 0; i < this.width; i++) {
            cursor.set(this.bottomLeft).move(Direction.UP, distanceToTop).move(this.rightDir, i);
            if (!this.isFrame.test(this.level.getBlockState(cursor))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 向上数到顶部，同时统计内部已有的门方块数量（供 {@link #isComplete} 使用）。
     * <p>左右两条边在每一层都必须是框架，内部则必须为空。</p>
     */
    private int getDistanceUntilTop(BlockPos.MutableBlockPos cursor) {
        for (int i = 0; i < MAX_HEIGHT; i++) {
            cursor.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, -1);
            if (!this.isFrame.test(this.level.getBlockState(cursor))) {
                return i;
            }
            cursor.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, this.width);
            if (!this.isFrame.test(this.level.getBlockState(cursor))) {
                return i;
            }
            for (int j = 0; j < this.width; j++) {
                cursor.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, j);
                BlockState state = this.level.getBlockState(cursor);
                if (!isEmpty(state)) {
                    return i;
                }
                if (state.is(VoidPortalFrame.portalBlock())) {
                    this.numPortalBlocks++;
                }
            }
        }
        return MAX_HEIGHT;
    }

    /** 内部格：空气、火、或本 mod 的门方块（原版对应的是 {@code NETHER_PORTAL}）。 */
    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(VoidPortalFrame.portalBlock());
    }

    // ==================== 校验 ====================

    /** 框架合法（<b>不要求已点燃</b>）——点燃路径用。 */
    public boolean isValid() {
        return this.bottomLeft != null
                && this.width >= MIN_WIDTH && this.width <= MAX_WIDTH
                && this.height >= MIN_HEIGHT && this.height <= MAX_HEIGHT;
    }

    /** 内部整片都是门方块——门方块的 {@code updateShape} 用。 */
    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    // ==================== 放置 ====================

    /**
     * 把内部填成门方块。
     * <p>
     * 与原版一致：<b>整片无条件覆盖</b>，不逐格判空。{@link #isEmpty} 已经保证内部每
     * 一格都是空气/火/门方块，覆盖火正是原版行为。
     * </p>
     * <p>
     * 早先这里加了 {@code isAir()} 判断才填，会留下一个崩溃路径：内部若有火，那一格会
     * 被跳过，而 {@link VoidPortalTeleporter} 事后要靠"门内某一格"去反推整座门的矩形，
     * 恰好取到残留的火方块时 {@code getValue(AXIS)} 就会抛异常。
     * </p>
     *
     * @return 放置的格数（= 宽 × 高）。原版此方法返回 void，这里返回计数，
     *         让点燃路径能判断"是否真的点燃了"而不是只看 {@code isValid()}。
     */
    public int createPortalBlocks() {
        BlockState portal = VoidPortalFrame.portalBlock().defaultBlockState()
                .setValue(VoidPortal.AXIS, this.axis);
        // 与原版同样的 18 = UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        int placed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(this.bottomLeft,
                this.bottomLeft.relative(Direction.UP, this.height - 1)
                        .relative(this.rightDir, this.width - 1))) {
            this.level.setBlock(pos, portal, flags);
            placed++;
        }
        return placed;
    }

    // ==================== 访问器 ====================

    public Direction.Axis getAxis() {
        return this.axis;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @Nullable
    public BlockPos getBottomLeft() {
        return this.bottomLeft;
    }

    /** 内部中心（目标维度落点用）。仅在 {@link #isValid()} 为真时有意义。 */
    public BlockPos getCenter() {
        return this.bottomLeft.relative(Direction.UP, this.height / 2)
                .relative(this.rightDir, this.width / 2);
    }
}
