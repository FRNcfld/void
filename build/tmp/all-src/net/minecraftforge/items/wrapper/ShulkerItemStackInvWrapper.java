/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.items.wrapper;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public class ShulkerItemStackInvWrapper implements IItemHandlerModifiable, ICapabilityProvider
{
    @ApiStatus.Internal
    @Nullable
    public static ICapabilityProvider createDefaultProvider(ItemStack itemStack)
    {
        var item = itemStack.m_41720_();
        if (item == Items.f_42265_ ||
            item == Items.f_42229_ ||
            item == Items.f_42225_ ||
            item == Items.f_42226_ ||
            item == Items.f_42275_ ||
            item == Items.f_42273_ ||
            item == Items.f_42227_ ||
            item == Items.f_42269_ ||
            item == Items.f_42274_ ||
            item == Items.f_42271_ ||
            item == Items.f_42268_ ||
            item == Items.f_42267_ ||
            item == Items.f_42272_ ||
            item == Items.f_42224_ ||
            item == Items.f_42228_ ||
            item == Items.f_42266_ ||
            item == Items.f_42270_
        )
            return new ShulkerItemStackInvWrapper(itemStack);
        return null;
    }

    private final ItemStack stack;
    private final LazyOptional<IItemHandler> holder = LazyOptional.of(() -> this);

    private CompoundTag cachedTag;
    private NonNullList<ItemStack> itemStacksCache;

    private ShulkerItemStackInvWrapper(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public int getSlots()
    {
        return 27;
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot)
    {
        validateSlotIndex(slot);
        return getItemList().get(slot);
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

        NonNullList<ItemStack> itemStacks = getItemList();

        ItemStack existing = itemStacks.get(slot);

        int limit = Math.min(getSlotLimit(slot), stack.m_41741_());

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
                itemStacks.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
            }
            else
            {
                existing.m_41769_(reachedLimit ? limit : stack.m_41613_());
            }
            setItemList(itemStacks);
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.m_41613_()- limit) : ItemStack.f_41583_;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        NonNullList<ItemStack> itemStacks = getItemList();
        if (amount == 0)
            return ItemStack.f_41583_;

        validateSlotIndex(slot);

        ItemStack existing = itemStacks.get(slot);

        if (existing.m_41619_())
            return ItemStack.f_41583_;

        int toExtract = Math.min(amount, existing.m_41741_());

        if (existing.m_41613_() <= toExtract)
        {
            if (!simulate)
            {
                itemStacks.set(slot, ItemStack.f_41583_);
                setItemList(itemStacks);
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
                itemStacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.m_41613_() - toExtract));
                setItemList(itemStacks);
            }

            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
        }
    }

    private void validateSlotIndex(int slot)
    {
        if (slot < 0 || slot >= getSlots())
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack)
    {
        return stack.m_41720_().m_142095_();
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack)
    {
        validateSlotIndex(slot);
        if (!isItemValid(slot, stack)) throw new RuntimeException("Invalid stack " + stack + " for slot " + slot + ")");
        NonNullList<ItemStack> itemStacks = getItemList();
        itemStacks.set(slot, stack);
        setItemList(itemStacks);
    }

    private NonNullList<ItemStack> getItemList()
    {
        CompoundTag rootTag = BlockItem.m_186336_(this.stack);
        if (cachedTag == null || !cachedTag.equals(rootTag))
            itemStacksCache = refreshItemList(rootTag);
        return itemStacksCache;
    }

    private NonNullList<ItemStack> refreshItemList(CompoundTag rootTag)
    {
        NonNullList<ItemStack> itemStacks = NonNullList.m_122780_(getSlots(), ItemStack.f_41583_);
        if (rootTag != null && rootTag.m_128425_("Items", CompoundTag.f_178202_))
        {
            ContainerHelper.m_18980_(rootTag, itemStacks);
        }
        cachedTag = rootTag;
        return itemStacks;
    }

    private void setItemList(NonNullList<ItemStack> itemStacks)
    {
        CompoundTag existing = BlockItem.m_186336_(this.stack);
        CompoundTag rootTag = ContainerHelper.m_18973_(existing == null ? new CompoundTag() : existing, itemStacks);
        BlockItem.m_186338_(this.stack, BlockEntityType.f_58939_, rootTag);
        cachedTag = rootTag;
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, this.holder);
    }
}
