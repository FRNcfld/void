package com.frnc.createvoid.block.custom;

import com.frnc.createvoid.block.entity.EntityMyriadBlockEntity;
import com.frnc.createvoid.block.entity.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 实体万象方块：1×1×1 的透明玻璃样方块，没有实体碰撞箱，玩家和掉落物都能直接穿过。
 * <p>
 * 面相邻的同种方块视为一个整体（存储网络见 {@link EntityMyriadBlockEntity}）；
 * 方块逐 tick 检查与自身 1×1×1 包围盒相交的掉落物并主动吸收。
 * </p>
 * <p>
 * 方块本身不存取流体（不是 waterloggable），但会像普通方块一样把水/岩浆挡在外面；
 * 属性里的 {@code forceSolidOn} 让流体判定把它当作固体，不会流进方块空间并替换掉它。
 * </p>
 */
public class EntityMyriadBlock extends BaseEntityBlock implements IWrenchable {

    public EntityMyriadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 方块本体使用本模组自己的 entity_myriad 模型 + CTModel 连接纹理；
        // EntityMyriadRenderer 只负责里面悬浮的物品。
        return RenderShape.MODEL;
    }

    /**
     * 相邻的本方块之间跳过面渲染，避免连接处出现两层面片互相叠色 / 内部边框。
     * <p>这正是原版染色玻璃（{@code HalfTransparentBlock}）的处理方式。</p>
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, side);
    }

    // ==================== 扳手 ====================

    /** 方块没有朝向可旋转，扳手右键直接速拆（潜行与否都可以）。 */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return dismantle(state, context);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return dismantle(state, context);
    }

    /**
     * 扳手速拆：把方块本体直接放进玩家背包，再销毁方块。
     * <p>
     * 本体走 {@code Block.getDrops} 时会被战利品表的“精准采集”条件挡住，
     * 而扳手拆除不应该要求精准采集，所以这里单独补一份本体的掉落。
     * 内容物由 {@link #onRemove} 负责掉回世界。
     * </p>
     */
    private InteractionResult dismantle(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        if (player != null && !player.isCreative()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(this));
        }
        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    // ==================== 移除 ====================

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof EntityMyriadBlockEntity be) {
            be.dropContents(level);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntityMyriadBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        // 服务端做加工/吸取，客户端做风机加工粒子
        return createTickerHelper(type, ModBlockEntities.ENTITY_MYRIAD.get(),
                EntityMyriadBlockEntity::tickBlockEntity);
    }
}
