package com.frnc.createvoid.block.custom;

import com.frnc.createvoid.block.entity.CrafterBlockEntity;
import com.frnc.createvoid.block.entity.ModBlockEntities;
import com.frnc.createvoid.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 自动合成器方块：一个由红石“上升沿”触发一次的原版式 3×3 自动合成器。
 * <p>
 * 状态：{@code orientation}(朝向)、{@code triggered}(被红石触发)、{@code crafting}(合成动画)。
 * 行为：放置时正面朝向玩家；邻居给出一次红石上升沿时调度自身 tick 执行一次合成；
 * 持续给信号不会重复合成，撤掉再给才会再次合成。合成细节见
 * {@link CrafterBlockEntity#craftOnce}。
 * </p>
 */
public class CrafterBlock extends BaseEntityBlock {

    public static final BooleanProperty CRAFTING = BooleanProperty.create("crafting");
    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

    /** 合成完成后，crafting 状态维持的时长（游戏刻），再调度 tick 收回动画。 */
    private static final int CRAFT_ANIMATION_TICKS = 6;

    private static final Logger LOGGER = LogUtils.getLogger();

    public CrafterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CRAFTING, false)
                .setValue(TRIGGERED, false)
                .setValue(ORIENTATION, FrontAndTop.NORTH_UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRAFTING, TRIGGERED, ORIENTATION);
    }

    // ==================== 朝向（正面朝玩家） ====================

    @Override
    public BlockState getStateForPlacement(@NotNull net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction front = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(front, Direction.UP));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ORIENTATION, rotation.rotation().rotate(state.getValue(ORIENTATION)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ORIENTATION, mirror.rotation().rotate(state.getValue(ORIENTATION)));
    }

    // ==================== 红石触发（上升沿，由方块实体逐 tick 轮询） ====================

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter)) {
            return;
        }
        // 合成动画期间忽略新触发，避免高频信号造成重复/错乱
        if (crafter.isCrafting()) {
            return;
        }
        // 注意：这里不再复查 TRIGGERED——本 tick 是由“上升沿”调度而来，触发已经捕获。
        // 若复查，2gt 时钟(1gt 通电)下等 craft 触发时信号已断，会永远不合成。
        if (crafter.craftOnce(level, state)) {
            // 合成已完成：进入 CRAFT_ANIMATION_TICKS 刻动画（由 BE serverTick 倒计时并收回 crafting 纹理）
            crafter.setCraftingTicksRemaining(CRAFT_ANIMATION_TICKS);
            if (!state.getValue(CRAFTING)) {
                level.setBlock(pos, state.setValue(CRAFTING, true), 3);
            }
        } else {
            // 失败：布局不可合成，播一次失败音
            level.playSound(null, pos, ModSounds.CRAFTER_FAIL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    // ==================== 交互 / 方块实体 ====================

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter) {
            player.openMenu(crafter);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter) {
                Containers.dropContents(level, pos, crafter);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrafterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CRAFTER.get(), CrafterBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ==================== 比较器 ====================

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter) {
            return crafter.countDisabled();
        }
        return 0;
    }

    // ==================== 形状 ====================

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull net.minecraft.world.phys.shapes.CollisionContext context) {
        return VoxelShapes.CRAFTER;
    }
}
