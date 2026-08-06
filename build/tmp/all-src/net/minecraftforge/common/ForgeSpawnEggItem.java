/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.Item.Properties;

public class ForgeSpawnEggItem extends SpawnEggItem
{
    private static final List<ForgeSpawnEggItem> MOD_EGGS = new ArrayList<>();
    private static final Map<EntityType<? extends Mob>, ForgeSpawnEggItem> TYPE_MAP = new IdentityHashMap<>();
    private final Supplier<? extends EntityType<? extends Mob>> typeSupplier;

    public ForgeSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor, Properties props)
    {
        super((EntityType<? extends Mob>) null, backgroundColor, highlightColor, props);
        this.typeSupplier = type;

        MOD_EGGS.add(this);
    }

    @Override
    public EntityType<?> m_43228_(@Nullable CompoundTag tag)
    {
        EntityType<?> type = super.m_43228_(tag);
        return type != null ? type : typeSupplier.get();
    }

    @Nullable
    protected DispenseItemBehavior createDispenseBehavior()
    {
        return DEFAULT_DISPENSE_BEHAVIOR;
    }

    @Nullable
    public static SpawnEggItem fromEntityType(@Nullable EntityType<?> type)
    {
        SpawnEggItem ret = TYPE_MAP.get(type);
        return ret != null ? ret : SpawnEggItem.m_43213_(type);
    }

    @Override
    protected EntityType<?> getDefaultType() {
        return this.typeSupplier.get();
    }

    private static final DispenseItemBehavior DEFAULT_DISPENSE_BEHAVIOR = (source, stack) ->
    {
        Direction face = source.m_6414_().m_61143_(DispenserBlock.f_52659_);
        EntityType<?> type = ((SpawnEggItem)stack.m_41720_()).m_43228_(stack.m_41783_());

        try
        {
            type.m_20592_(source.m_7727_(), stack, null, source.m_7961_().m_121945_(face), MobSpawnType.DISPENSER, face != Direction.UP, false);
        }
        catch (Exception exception)
        {
            DispenseItemBehavior.f_181892_.error("Error while dispensing spawn egg from dispenser at {}", source.m_7961_(), exception);
            return ItemStack.f_41583_;
        }

        stack.m_41774_(1);
        source.m_7727_().m_220407_(GameEvent.f_157810_, source.m_7961_(), GameEvent.Context.m_223722_(source.m_6414_()));
        return stack;
    };

    @Mod.EventBusSubscriber(modid = "forge", bus = Mod.EventBusSubscriber.Bus.MOD)
    private static class CommonHandler
    {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event)
        {
            MOD_EGGS.forEach(egg ->
            {
                DispenseItemBehavior dispenseBehavior = egg.createDispenseBehavior();
                if (dispenseBehavior != null)
                {
                    DispenserBlock.m_52672_(egg, dispenseBehavior);
                }

                TYPE_MAP.put(egg.typeSupplier.get(), egg);
            });
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "forge", bus = Mod.EventBusSubscriber.Bus.MOD)
    private static class ColorRegisterHandler
    {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void registerSpawnEggColors(RegisterColorHandlersEvent.Item event)
        {
            MOD_EGGS.forEach(egg ->
                    event.getItemColors().m_92689_((stack, layer) -> egg.m_43211_(layer), egg)
            );
        }
    }
}