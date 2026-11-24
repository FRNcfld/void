package com.frnc.createvoid.jei;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.wateringrecipes.RecipeTypes;
import com.frnc.createvoid.wateringrecipes.WateringRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JEI 插件入口，注册两个分类：
 * <ul>
 *   <li>浇水 —— 本 mod 数据包里的 {@code create_void:watering} 配方；</li>
 *   <li>注液器隐藏配方 —— Create 内置、但没有配方 JSON 的注液行为
 *       （泥土→泥巴、耕地湿润、炼药锅注液）。</li>
 * </ul>
 */
@JeiPlugin
public class CreateVoidJEI implements IModPlugin {

    public static final RecipeType<WateringRecipe> WATERING_TYPE =
            RecipeType.create(CreateVoid.MOD_ID, "watering", WateringRecipe.class);

    /** Create 内置注液行为的 JEI 分类类型（复用 WateringRecipe 的渲染形状）。 */
    public static final RecipeType<WateringRecipe> SPOUT_HIDDEN_TYPE =
            RecipeType.create(CreateVoid.MOD_ID, "spout_hidden", WateringRecipe.class);

    /** 保存分类实例，供 registerRecipes/registerCatalysts 使用 */
    private WateringCategory wateringCategory;
    private WateringCategory spoutHiddenCategory;

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

        CreateRecipeCategory.Info<WateringRecipe> hiddenInfo = new CreateRecipeCategory.Info<>(
                SPOUT_HIDDEN_TYPE,
                Component.translatable("jei.create_void.spout_hidden"),
                registration.getJeiHelpers().getGuiHelper().createBlankDrawable(177, 70),
                registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(
                        new ItemStack(AllBlocks.SPOUT.get())),
                CreateVoidJEI::hiddenSpoutRecipes,
                List.of(() -> new ItemStack(AllBlocks.SPOUT.get()))
        );
        spoutHiddenCategory = new WateringCategory(hiddenInfo);
        registration.addRecipeCategories(spoutHiddenCategory);
    }

    /**
     * Create 内置注液行为对应的展示条目，数值与
     * {@code AllBlockSpoutingBehaviours} / {@code CauldronSpoutingBehavior} 保持一致。
     */
    private static List<WateringRecipe> hiddenSpoutRecipes() {
        List<WateringRecipe> recipes = new ArrayList<>();
        FluidIngredient water250 = FluidIngredient.fromFluid(Fluids.WATER, 250);

        recipes.add(hidden("dirt_to_mud", Items.DIRT, water250, Items.MUD, null));
        recipes.add(hidden("coarse_dirt_to_mud", Items.COARSE_DIRT, water250, Items.MUD, null));
        recipes.add(hidden("rooted_dirt_to_mud", Items.ROOTED_DIRT, water250, Items.MUD, null));

        recipes.add(hidden("farmland_moisture", Items.FARMLAND,
                FluidIngredient.fromFluid(Fluids.WATER, 100), Items.FARMLAND,
                Component.translatable("jei.create_void.spout_hidden.farmland")));

        // 水/熔岩炼药锅没有物品形态，JEI 物品槽只能显示炼药锅本体，用提示说明状态变化
        recipes.add(hidden("cauldron_water", Items.CAULDRON, water250, Items.CAULDRON,
                Component.translatable("jei.create_void.spout_hidden.cauldron_water")));
        recipes.add(hidden("cauldron_lava", Items.CAULDRON,
                FluidIngredient.fromFluid(Fluids.LAVA, 1000), Items.CAULDRON,
                Component.translatable("jei.create_void.spout_hidden.cauldron_lava")));
        recipes.add(hidden("water_cauldron_level", Items.CAULDRON, water250, Items.CAULDRON,
                Component.translatable("jei.create_void.spout_hidden.water_cauldron")));

        return recipes;
    }

    private static WateringRecipe hidden(String path, Item input, FluidIngredient fluid,
                                         Item output, Component note) {
        return HiddenSpoutRecipe.create(path, new ItemStack(input), fluid, new ItemStack(output), note);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 必须显式调用分类的 registerRecipes，否则配方不会注册到 JEI。
        // 本分类由本插件注册，Create 自己的 JEI 插件不会代为调用。
        if (wateringCategory != null) {
            wateringCategory.registerRecipes(registration);
        }
        if (spoutHiddenCategory != null) {
            spoutHiddenCategory.registerRecipes(registration);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 同样显式调用分类的 registerCatalysts（内部会添加 Spout 催化剂）
        if (wateringCategory != null) {
            wateringCategory.registerCatalysts(registration);
        }
        if (spoutHiddenCategory != null) {
            spoutHiddenCategory.registerCatalysts(registration);
        }
    }
}
