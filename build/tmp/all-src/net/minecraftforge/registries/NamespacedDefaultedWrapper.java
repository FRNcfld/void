/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import java.util.Optional;
import java.util.function.Function;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

class NamespacedDefaultedWrapper<T> extends NamespacedWrapper<T> implements DefaultedRegistry<T>
{
    private final ForgeRegistry<T> delegate;
    private final ResourceLocation defaultKey;
    private Holder.Reference<T> defaultHolder;

    NamespacedDefaultedWrapper(ForgeRegistry<T> fowner, Function<T, Holder.Reference<T>> intrusiveHolderCallback, RegistryManager stage) {
        super(fowner, intrusiveHolderCallback, stage);
        this.delegate = fowner;
        this.defaultKey = fowner.getDefaultKey();
    }

    // Reading Functions
    @Override
    public T m_7745_(@Nullable ResourceLocation name)
    {
        return this.delegate.getValue(name); //getOrDefault
    }

    @Override
    public Optional<Holder.Reference<T>> m_213642_(RandomSource rand)
    {
        if (this.defaultHolder != null)
            return super.m_213642_(rand).or(() -> Optional.of(this.defaultHolder));

        return super.m_213642_(rand);
    }

    @Override
    public ResourceLocation m_122315_()
    {
        return this.delegate.getDefaultKey();
    }

    @Nullable
    @Override
    Holder.Reference<T> onAdded(RegistryManager stage, int id, ResourceKey<T> key, T newValue, T oldValue)
    {
        Holder.Reference<T> newHolder = super.onAdded(stage, id, key, newValue, oldValue);

        if (newHolder != null && this.defaultKey != null && this.defaultKey.equals(key.m_135782_()))
            this.defaultHolder = newHolder;

        return newHolder;
    }

    public static class Factory<V> implements IForgeRegistry.CreateCallback<V>, IForgeRegistry.AddCallback<V>
    {
        public static final ResourceLocation ID = new ResourceLocation("forge", "registry_defaulted_wrapper");

        @Override
        public void onCreate(IForgeRegistryInternal<V> owner, RegistryManager stage)
        {
            ForgeRegistry<V> fowner = (ForgeRegistry<V>) owner;
            owner.setSlaveMap(ID, new NamespacedDefaultedWrapper<V>(fowner, fowner.getBuilder().getIntrusiveHolderCallback(), stage));
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onAdd(IForgeRegistryInternal<V> owner, RegistryManager stage, int id, ResourceKey<V> key, V value, V oldValue)
        {
            owner.getSlaveMap(ID, NamespacedDefaultedWrapper.class).onAdded(stage, id, key, value, oldValue);
        }
    }
}
