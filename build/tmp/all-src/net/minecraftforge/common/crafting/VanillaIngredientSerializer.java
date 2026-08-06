/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.crafting;

import java.util.stream.Stream;

import com.google.gson.JsonObject;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.FriendlyByteBuf;

public class VanillaIngredientSerializer implements IIngredientSerializer<Ingredient>
{
    public static final VanillaIngredientSerializer INSTANCE  = new VanillaIngredientSerializer();

    public Ingredient parse(FriendlyByteBuf buffer)
    {
        return Ingredient.m_43938_(Stream.generate(() -> new Ingredient.ItemValue(buffer.m_130267_())).limit(buffer.m_130242_()));
    }

    public Ingredient parse(JsonObject json)
    {
       return Ingredient.m_43938_(Stream.of(Ingredient.m_43919_(json)));
    }

    public void write(FriendlyByteBuf buffer, Ingredient ingredient)
    {
        ItemStack[] items = ingredient.m_43908_();
        buffer.m_130130_(items.length);

        for (ItemStack stack : items)
            buffer.m_130055_(stack);
    }
}
