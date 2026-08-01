package com.frnc.createvoid.wateringrecipes;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 浇水配方：流体 + 方块 → 替换方块。
 * <p>
 * 直接继承 Create 的 ProcessingRecipe，复用其 JSON 解析、结果滚动、流体匹配等逻辑。
 * 不需要自定义序列化器，全部由 {@link ProcessingRecipeBuilder.ProcessingRecipeFactory} 处理。
 * </p>
 *
 * <h3>JSON 格式示例</h3>
 * <pre>{@code
 * {
 *   "type": "create_void:watering",
 *   "ingredients": [
 *     { "item": "minecraft:copper_block" },
 *     { "fluid": "minecraft:water", "amount": 250 }
 *   ],
 *   "results": [
 *     { "item": "minecraft:exposed_copper" }
 *   ]
 * }
 * }</pre>
 * <p>
 * <b>注意：</b>Create 6.0.x 的 ProcessingRecipeSerializer 将物品和流体原料
 * <b>合并</b>放入 {@code "ingredients"} 数组中，通过 {@code FluidIngredient.isFluidIngredient()}
 * 自动区分。没有独立的 {@code "fluidIngredients"} 字段。
 * </p>
 */
public class WateringRecipe extends ProcessingRecipe<Container> {

    public WateringRecipe(ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(RecipeTypes.WATERING, params);
    }

    // ==================== 边界定义 ====================

    @Override
    protected int getMaxInputCount() {
        return 1;   // 最多 1 个物品输入（目标方块）
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;   // 最多 1 个物品输出（替换结果）
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;   // 最多 1 个流体输入
    }

    // ==================== Recipe 接口实现 ====================

    /**
     * 配方匹配始终返回 true。
     * <p>
     * 实际的方块+流体匹配在 {@link WateringBehaviour#fillBlock} 中完成，
     * 此处的 matches() 仅用于配方查看器（JEI/REI）的容器模式展示。
     * </p>
     */
    @Override
    public boolean matches(Container container, Level level) {
        return true;
    }

    /**
     * 检查目标方块是否匹配本配方。
     * 将 BlockState 转为对应物品的 ItemStack，然后交给 Ingredient 做匹配。
     *
     * @param state 目标位置的方块状态
     * @return true 如果该方块是此配方的合法输入
     */
    public boolean testTargetBlock(BlockState state) {
        if (getIngredients().isEmpty()) return false;
        Ingredient ingredient = getIngredients().get(0);
        return ingredient.test(state.getBlock().asItem().getDefaultInstance());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取配方所需的流体原料（第一个，也是唯一的一个）。
     *
     * @return 流体原料
     * @throws IllegalStateException 如果配方没有定义流体原料
     */
    public FluidIngredient getRequiredFluid() {
        if (getFluidIngredients().isEmpty())
            throw new IllegalStateException("Watering Recipe: has no fluid ingredient!");
        return getFluidIngredients().get(0);
    }

    // ==================== 序列化 ====================

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypes.WATERING.getSerializer();
    }
}
