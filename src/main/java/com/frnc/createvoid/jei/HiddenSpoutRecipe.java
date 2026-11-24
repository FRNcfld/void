package com.frnc.createvoid.jei;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.wateringrecipes.WateringRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

/**
 * 仅用于 JEI 展示的「虚拟配方」。
 * <p>
 * Create 内置的注液行为（泥土→泥巴、耕地湿润、炼药锅注液）是硬编码在
 * {@code AllBlockSpoutingBehaviours} 里的，不存在配方 JSON，因此无法从
 * {@code RecipeManager} 查到。这里把它们包装成 {@link WateringRecipe} 的形状，
 * 只交给 JEI 渲染，不注册进配方管理器，也不参与游戏内匹配。
 * </p>
 */
public final class HiddenSpoutRecipe extends WateringRecipe {

    /** 输出槽的补充说明（如「湿润度 +1」），可为 null。 */
    @Nullable
    private final Component note;

    private HiddenSpoutRecipe(ProcessingRecipeParams params, @Nullable Component note) {
        super(params);
        this.note = note;
    }

    @Nullable
    public Component getNote() {
        return note;
    }

    public static HiddenSpoutRecipe create(String path, ItemStack input, FluidIngredient fluid,
                                           ItemStack output, @Nullable Component note) {
        Params params = new Params(new ResourceLocation(CreateVoid.MOD_ID, "jei_spout_hidden/" + path));
        params.addIngredient(Ingredient.of(input));
        params.addFluid(fluid);
        params.addResult(new ProcessingOutput(output, 1));
        return new HiddenSpoutRecipe(params, note);
    }

    /** {@link ProcessingRecipeParams} 的字段是 protected，用子类把构造过程收窄。 */
    private static final class Params extends ProcessingRecipeParams {

        private Params(ResourceLocation id) {
            super(id);
        }

        private void addIngredient(Ingredient ingredient) {
            ingredients.add(ingredient);
        }

        private void addFluid(FluidIngredient fluid) {
            fluidIngredients.add(fluid);
        }

        private void addResult(ProcessingOutput output) {
            results.add(output);
        }
    }
}
