/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.items;

import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.NonNullList;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

public class ItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundTag>
{
    protected NonNullList<ItemStack> stacks;

    public ItemStackHandler()
    {
        this(1);
    }

    public ItemStackHandler(int size)
    {
        stacks = NonNullList.m_122780_(size, ItemStack.f_41583_);
    }

    public ItemStackHandler(NonNullList<ItemStack> stacks)
    {
        this.stacks = stacks;
    }

    public void setSize(int size)
    {
        stacks = NonNullList.m_122780_(size, ItemStack.f_41583_);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack)
    {
        validateSlotIndex(slot);
        this.stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots()
    {
        return stacks.size();
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot)
    {
        validateSlotIndex(slot);
        return this.stacks.get(slot);
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
    {
        if (stack.m_41619_())
            return ItemStack.f_41583_;

        if (!isItemValid(slot, stack))
            return stack;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        int limit = getStackLimit(slot, stack);

        if (!existing.m_41619_())
        {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.m_41613_();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.m_41613_() > limit;

        if (!simulate)
        {
            if (existing.m_41619_())
            {
                this.stacks.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
            }
            else
            {
                existing.m_41769_(reachedLimit ? limit : stack.m_41613_());
            }
            onContentsChanged(slot);
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.m_41613_()- limit) : ItemStack.f_41583_;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount == 0)
            return ItemStack.f_41583_;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        if (existing.m_41619_())
            return ItemStack.f_41583_;

        int toExtract = Math.min(amount, existing.m_41741_());

        if (existing.m_41613_() <= toExtract)
        {
            if (!simulate)
            {
                this.stacks.set(slot, ItemStack.f_41583_);
                onContentsChanged(slot);
                return existing;
            }
            else
            {
                return existing.m_41777_();
            }
        }
        else
        {
            if (!simulate)
            {
                this.stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.m_41613_() - toExtract));
                onContentsChanged(slot);
            }

            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
        }
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 64;
    }

    protected int getStackLimit(int slot, @NotNull ItemStack stack)
    {
        return Math.min(getSlotLimit(slot), stack.m_41741_());
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack)
    {
        return true;
    }

    @Override
    public CompoundTag serializeNBT()
    {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < stacks.size(); i++)
        {
            if (!stacks.get(i).m_41619_())
            {
                CompoundTag itemTag = new CompoundTag();
                itemTag.m_128405_("Slot", i);
                stacks.get(i).m_41739_(itemTag);
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.m_128365_("Items", nbtTagList);
        nbt.m_128405_("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt)
    {
        setSize(nbt.m_128425_("Size", Tag.f_178196_) ? nbt.m_128451_("Size") : stacks.size());
        ListTag tagList = nbt.m_128437_("Items", Tag.f_178203_);
        for (int i = 0; i < tagList.size(); i++)
        {
            CompoundTag itemTags = tagList.m_128728_(i);
            int slot = itemTags.m_128451_("Slot");

            if (slot >= 0 && slot < stacks.size())
            {
                stacks.set(slot, ItemStack.m_41712_(itemTags));
            }
        }
        onLoad();
    }

    protected void validateSlotIndex(int slot)
    {
        if (slot < 0 || slot >= stacks.size())
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
    }

    protected void onLoad()
    {

    }

    protected void onContentsChanged(int slot)
    {

    }
}
