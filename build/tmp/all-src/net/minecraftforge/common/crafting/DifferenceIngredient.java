/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** Ingredient that matches everything from the first ingredient that is not included in the second ingredient */
public class DifferenceIngredient extends AbstractIngredient
{
    private final Ingredient base;
    private final Ingredient subtracted;
    private ItemStack[] filteredMatchingStacks;
    private IntList packedMatchingStacks;

    protected DifferenceIngredient(Ingredient base, Ingredient subtracted)
    {
        this.base = base;
        this.subtracted = subtracted;
    }

    /**
     * Gets the difference from the two ingredients
     * @param base        Ingredient the item must match
     * @param subtracted  Ingredient the item must not match
     * @return  Ingredient that {@code base} anything in base that is not in {@code subtracted}
     */
    public static DifferenceIngredient of(Ingredient base, Ingredient subtracted)
    {
        return new DifferenceIngredient(base, subtracted);
    }

    @Override
    public boolean test(@Nullable ItemStack stack)
    {
        if (stack == null || stack.m_41619_())
            return false;
        return base.test(stack) && !subtracted.test(stack);
    }

    @Override
    public ItemStack[] m_43908_()
    {
        if (this.filteredMatchingStacks == null)
            this.filteredMatchingStacks = Arrays.stream(base.m_43908_())
                                                .filter(stack -> !subtracted.test(stack))
                                                .toArray(ItemStack[]::new);
        return filteredMatchingStacks;
    }

    @Override
    public boolean m_43947_()
    {
        return base.m_43947_();
    }

    @Override
    public boolean isSimple()
    {
        return base.isSimple() && subtracted.isSimple();
    }

    @Override
    protected void invalidate()
    {
        super.invalidate();
        this.filteredMatchingStacks = null;
        this.packedMatchingStacks = null;
    }

    @Override
    public IntList m_43931_()
    {
        if (this.packedMatchingStacks == null || checkInvalidation())
        {
            markValid();
            ItemStack[] matchingStacks = m_43908_();
            this.packedMatchingStacks = new IntArrayList(matchingStacks.length);
            for (ItemStack stack : matchingStacks)
                this.packedMatchingStacks.add(StackedContents.m_36496_(stack));

            this.packedMatchingStacks.sort(IntComparators.NATURAL_COMPARATOR);
        }
        return packedMatchingStacks;
    }

    @Override
    public JsonElement m_43942_()
    {
        JsonObject json = new JsonObject();
        json.addProperty("type", CraftingHelper.getID(Serializer.INSTANCE).toString());
        json.add("base", base.m_43942_());
        json.add("subtracted", subtracted.m_43942_());
        return json;
    }

    @Override
    public IIngredientSerializer<DifferenceIngredient> getSerializer()
    {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements IIngredientSerializer<DifferenceIngredient>
    {
        public static final IIngredientSerializer<DifferenceIngredient> INSTANCE = new Serializer();

        @Override
        public DifferenceIngredient parse(JsonObject json)
        {
            Ingredient base = Ingredient.m_288218_(json.get("base"), false);
            Ingredient without = Ingredient.m_288218_(json.get("subtracted"), false);
            return new DifferenceIngredient(base, without);
        }

        @Override
        public DifferenceIngredient parse(FriendlyByteBuf buffer)
        {
            Ingredient base = Ingredient.m_43940_(buffer);
            Ingredient without = Ingredient.m_43940_(buffer);
            return new DifferenceIngredient(base, without);
        }

        @Override
        public void write(FriendlyByteBuf buffer, DifferenceIngredient ingredient)
        {
            ingredient.base.m_43923_(buffer);
            ingredient.subtracted.m_43923_(buffer);
        }
    }
}
