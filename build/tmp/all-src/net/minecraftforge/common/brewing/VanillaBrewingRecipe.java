/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.brewing;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;

/**
 * Used in BrewingRecipeRegistry to maintain the vanilla behaviour.
 *
 * Most of the code was simply adapted from net.minecraft.tileentity.TileEntityBrewingStand
 */
public class VanillaBrewingRecipe implements IBrewingRecipe {

    /**
     * Code adapted from TileEntityBrewingStand.isItemValidForSlot(int index, ItemStack stack)
     */
    @Override
    public boolean isInput(ItemStack stack)
    {
        Item item = stack.m_41720_();
        return item == Items.f_42589_ || item == Items.f_42736_ || item == Items.f_42739_ || item == Items.f_42590_;
    }

    /**
     * Code adapted from TileEntityBrewingStand.isItemValidForSlot(int index, ItemStack stack)
     */
    @Override
    public boolean isIngredient(ItemStack stack)
    {
        return PotionBrewing.m_43506_(stack);
    }

    /**
     * Code copied from TileEntityBrewingStand.brewPotions()
     * It brews the potion by doing the bit-shifting magic and then checking if the new PotionEffect list is different to the old one,
     * or if the new potion is a splash potion when the old one wasn't.
     */
    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient)
    {
        if (!input.m_41619_() && !ingredient.m_41619_() && isIngredient(ingredient))
        {
            ItemStack result = PotionBrewing.m_43529_(ingredient, input);
            if (result != input)
            {
                return result;
            }
            return ItemStack.f_41583_;
        }

        return ItemStack.f_41583_;
    }
}
