package com.frnc.createvoid.jei;

import com.frnc.createvoid.wateringrecipes.WateringRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

/**
 * 浇水配方 JEI 分类。
 * 显示流体 + 方块 → 替换方块的配方界面，包含 3D Spout 动画。
 */
public class WateringCategory extends CreateRecipeCategory<WateringRecipe> {

    private final AnimatedSpoutCustomTarget spout;

    public WateringCategory(Info<WateringRecipe> info) {
        super(info);
        this.spout = new AnimatedSpoutCustomTarget();
    }

    /**
     * 设置配方界面的输入/输出槽位布局。
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WateringRecipe recipe, IFocusGroup focuses) {
        // 输入槽 — 目标方块物品（位置 27, 51）
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getIngredients().get(0));

        // 流体槽 — 所需流体（位置 27, 29）
        addFluidSlot(builder, 27, 29, recipe.getRequiredFluid());

        // 输出槽 — 配方结果（位置 132, 51）
        ProcessingOutput output = recipe.getRollableResults().get(0);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51)
                .setBackground(getRenderedSlot(output), -1, -1)
                .addItemStack(output.getStack())
                .addRichTooltipCallback(addStochasticTooltip(output));
    }

    /**
     * 绘制背景和 3D Spout 动画。
     */
    @Override
    public void draw(WateringRecipe recipe, IRecipeSlotsView slotsView,
                     GuiGraphics gfx, double mouseX, double mouseY) {

        // 1. 阴影（位置 62, 57）
        AllGuiTextures.JEI_SHADOW.render(gfx, 62, 57);

        // 2. 向下箭头（位置 126, 29）
        AllGuiTextures.JEI_DOWN_ARROW.render(gfx, 126, 29);

        // 3. 3D Spout 动画（居中偏右）
        List<BlockState> targets = Arrays.stream(recipe.getIngredients().get(0).getItems())
                .map(stack -> {
                    if (stack.getItem() instanceof BlockItem bi)
                        return bi.getBlock().defaultBlockState();
                    return Blocks.AIR.defaultBlockState();
                })
                .toList();

        // 传入所需流体，让 Spout 动画渲染水箱内流体和向下流动的流体柱
        spout.withTarget(targets)
                .withFluids(recipe.getRequiredFluid().getMatchingFluidStacks())
                .draw(gfx, getBackground().getWidth() / 2 - 13, 22);
    }
}
