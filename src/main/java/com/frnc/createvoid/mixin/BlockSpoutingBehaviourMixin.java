package com.frnc.createvoid.mixin;

import com.frnc.createvoid.wateringrecipes.WateringBehaviour;
import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 兜底注入：在 Create 的 {@link BlockSpoutingBehaviour#get(Level, BlockPos)} 头部
 * 动态检查浇水配方。
 * <p>
 * 主要路径是 {@link WateringBehaviour#registerBlockBehavioursFromRecipes} 将配方输入
 * 方块注册进官方 {@code BY_BLOCK} 注册表；此 Mixin 仅作为额外兜底，覆盖注册表之外
 * 的、运行期才出现的配方输入方块。
 * </p>
 */
@Mixin(value = BlockSpoutingBehaviour.class, remap = false)
public interface BlockSpoutingBehaviourMixin {

    @Inject(
            method = "get(Lnet/minecraft/world/level/Level;" +
                     "Lnet/minecraft/core/BlockPos;)" +
                     "Lcom/simibubi/create/api/behaviour/spouting/BlockSpoutingBehaviour;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void createVoid$beforeGet(Level level, BlockPos pos,
                                              CallbackInfoReturnable<BlockSpoutingBehaviour> cir) {
        if (WateringBehaviour.getRecipe(level, level.getBlockState(pos))) {
            cir.setReturnValue(WateringBehaviour.INSTANCE);
        }
    }
}
