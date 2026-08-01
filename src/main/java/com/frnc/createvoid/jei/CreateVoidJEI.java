package com.frnc.createvoid.jei;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.wateringrecipes.RecipeTypes;
import com.frnc.createvoid.wateringrecipes.WateringRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * JEI 插件入口，注册浇水配方分类（WateringCategory）。
 */
@JeiPlugin
public class CreateVoidJEI implements IModPlugin {

    public static final RecipeType<WateringRecipe> WATERING_TYPE =
            RecipeType.create(CreateVoid.MOD_ID, "watering", WateringRecipe.class);

    /** 保存分类实例，供 registerRecipes/registerCatalysts 使用 */
    private WateringCategory wateringCategory;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(CreateVoid.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        CreateRecipeCategory.Info<WateringRecipe> info = new CreateRecipeCategory.Info<>(
                WATERING_TYPE,
                Component.translatable("jei.create_void.watering"),
                registration.getJeiHelpers().getGuiHelper().createBlankDrawable(177, 70),
                // 分类图标：Spout 方块物品
                registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(
                        new ItemStack(AllBlocks.SPOUT.get())),
                () -> {
                    if (Minecraft.getInstance().level == null)
                        return Collections.emptyList();
                    return Minecraft.getInstance().level.getRecipeManager()
                            .getAllRecipesFor(RecipeTypes.WATERING.getType())
                            .stream()
                            .map(r -> (WateringRecipe) r)
                            .toList();
                },
                // 催化剂：Spout 方块
                List.of(() -> new ItemStack(AllBlocks.SPOUT.get()))
        );
        wateringCategory = new WateringCategory(info);
        registration.addRecipeCategories(wateringCategory);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 必须显式调用分类的 registerRecipes，否则配方不会注册到 JEI。
        // 本分类由本插件注册，Create 自己的 JEI 插件不会代为调用。
        if (wateringCategory != null) {
            wateringCategory.registerRecipes(registration);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 同样显式调用分类的 registerCatalysts（内部会添加 Spout 催化剂）
        if (wateringCategory != null) {
            wateringCategory.registerCatalysts(registration);
        }
    }
}
