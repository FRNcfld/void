package com.frnc.createvoid.client;

import com.frnc.createvoid.block.entity.EntityMyriadBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 实体万象里“漂浮物品”的渲染。
 * <p>
 * 方块本体（透明边框玻璃）使用本模组的 {@code entity_myriad} 模型 + CTModel 连接纹理，
 * 这个 BER 只负责把存进去的物品画成缓慢上下漂浮、绕 Y 轴旋转的掉落物样子。
 * </p>
 */
public class EntityMyriadRenderer implements BlockEntityRenderer<EntityMyriadBlockEntity> {

    private final ItemRenderer itemRenderer;

    public EntityMyriadRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(EntityMyriadBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = be.getDisplayStack();
        if (stack.isEmpty()) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = be.getBlockPos();
        float time = level.getGameTime() + partialTick;
        float bob = Mth.sin(time / 12.0F) * 0.06F;
        float spin = (time * 1.5F) % 360.0F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(0.6F, 0.6F, 0.6F);

        int light = LevelRenderer.getLightColor(level, pos);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, packedOverlay,
                poseStack, bufferSource, level, (int) pos.asLong());
        poseStack.popPose();
    }
}
