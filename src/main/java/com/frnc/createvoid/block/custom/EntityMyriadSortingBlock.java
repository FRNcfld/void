package com.frnc.createvoid.block.custom;

import com.frnc.createvoid.block.entity.EntityMyriadBlockEntity;
import com.simibubi.create.content.logistics.filter.FilterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 实体万象-分选：与实体万象共用存储网络和加工逻辑，额外提供一个过滤器槽位。
 * <p>
 * 手持 Create 的过滤器右键可放入过滤器（已有过滤器时交换并把旧的还给玩家）；
 * 空手右键取回。过滤是网络级的：整张相连的分选网络都按网络里存在的过滤器判定，
 * 多个过滤器取并集（命中任意一个即可）。
 * </p>
 */
public class EntityMyriadSortingBlock extends EntityMyriadBlock {

    public EntityMyriadSortingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EntityMyriadBlockEntity be)) {
            return super.use(state, level, pos, player, hand, hit);
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof FilterItem) {
            if (!level.isClientSide) {
                ItemStack previous = be.insertFilter(held);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                if (!previous.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(previous);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held.isEmpty() && !be.getFilterStack().isEmpty()) {
            if (!level.isClientSide) {
                ItemStack filter = be.extractFilter();
                if (!filter.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(filter);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }
}
