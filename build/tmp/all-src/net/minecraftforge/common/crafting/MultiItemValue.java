/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.crafting;

import java.util.Collection;
import java.util.Collections;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient.Value;
import net.minecraftforge.registries.ForgeRegistries;

public class MultiItemValue implements Value
{
    private Collection<ItemStack> items;
    public MultiItemValue(Collection<ItemStack> items)
    {
        this.items = Collections.unmodifiableCollection(items);
    }

    @Override
    public Collection<ItemStack> m_6223_()
    {
        return items;
    }

    @Override
    public JsonObject m_6544_()
    {
        if (items.size() == 1)
            return toJson(items.iterator().next());

        JsonObject ret = new JsonObject();
        JsonArray array = new JsonArray();
        items.forEach(stack -> array.add(toJson(stack)));
        ret.add("items", array);
        return ret;
    }

    private JsonObject toJson(ItemStack stack)
    {
        JsonObject ret = new JsonObject();
        ret.addProperty("item", ForgeRegistries.ITEMS.getKey(stack.m_41720_()).toString());
        if (stack.m_41613_() != 1)
            ret.addProperty("count", stack.m_41613_());
        if (stack.m_41783_() != null)
            ret.addProperty("nbt", stack.m_41783_().toString()); //TODO: Better serialization?
        return ret;
    }

}
