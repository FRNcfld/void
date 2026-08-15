package com.frnc.createvoid.wateringrecipes;

import com.google.gson.JsonObject;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 浇水配方序列化器：在 Create 的 {@link ProcessingRecipeSerializer} 基础上，
 * 支持可选字段 {@code "dropAsItem": true}（配方是否以"消耗方块 + 掉落物品"方式产出）。
 * <p>
 * Create 把 public 的 fromJson/fromNetwork/toNetwork 声明为 final，但预留了受保护的
 * 扩展钩子 readFromJson / readFromBuffer / writeToBuffer / writeToJson。
 * 这里覆写这四个钩子，把标志读入/写出 {@link WateringRecipe}。
 * </p>
 */
public class WateringRecipeSerializer extends ProcessingRecipeSerializer<WateringRecipe> {

    public WateringRecipeSerializer(ProcessingRecipeBuilder.ProcessingRecipeFactory<WateringRecipe> factory) {
        super(factory);
    }

    @Override
    protected WateringRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
        WateringRecipe recipe = super.readFromJson(recipeId, json);
        if (json.has("dropAsItem"))
            recipe.setDropAsItem(json.get("dropAsItem").getAsBoolean());
        return recipe;
    }

    @Override
    protected void writeToJson(JsonObject json, WateringRecipe recipe) {
        super.writeToJson(json, recipe);
        if (recipe.isDropAsItem())
            json.addProperty("dropAsItem", true);
    }

    @Override
    protected WateringRecipe readFromBuffer(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        boolean dropAsItem = buffer.readBoolean();
        WateringRecipe recipe = super.readFromBuffer(recipeId, buffer);
        recipe.setDropAsItem(dropAsItem);
        return recipe;
    }

    @Override
    protected void writeToBuffer(FriendlyByteBuf buffer, WateringRecipe recipe) {
        buffer.writeBoolean(recipe.isDropAsItem());
        super.writeToBuffer(buffer, recipe);
    }
}
