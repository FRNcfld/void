/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.network.filters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandler;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.gametest.framework.TestClassNameArgument;
import net.minecraft.gametest.framework.TestFunctionArgument;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;

/**
 * A filter for impl packets, used to filter/modify parts of vanilla impl messages that
 * will cause errors or warnings on vanilla clients, for example entity attributes that are added by Forge or mods.
 */
@ChannelHandler.Sharable
public class VanillaConnectionNetworkFilter extends VanillaPacketFilter
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public VanillaConnectionNetworkFilter()
    {
        super(
                ImmutableMap.<Class<? extends Packet<?>>, BiConsumer<Packet<?>, List<? super Packet<?>>>>builder()
                .put(handler(ClientboundUpdateAttributesPacket.class, VanillaConnectionNetworkFilter::filterEntityProperties))
                .put(handler(ClientboundCommandsPacket.class, VanillaConnectionNetworkFilter::filterCommandList))
                .put(handler(ClientboundUpdateTagsPacket.class, VanillaConnectionNetworkFilter::filterCustomTagTypes))
                .build()
        );
    }

    @Override
    protected boolean isNecessary(Connection manager)
    {
        return NetworkHooks.isVanillaConnection(manager);
    }

    /**
     * Filter for SEntityPropertiesPacket. Filters out any entity attributes that are not in the "minecraft" namespace.
     * A vanilla client would ignore these with an error log.
     */
    @NotNull
    private static ClientboundUpdateAttributesPacket filterEntityProperties(ClientboundUpdateAttributesPacket msg)
    {
        ClientboundUpdateAttributesPacket newPacket = new ClientboundUpdateAttributesPacket(msg.m_133588_(), Collections.emptyList());
        msg.m_133591_().stream()
                .filter(snapshot -> {
                    ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(snapshot.m_133601_());
                    return key != null && key.m_135827_().equals("minecraft");
                })
                .forEach(snapshot -> newPacket.m_133591_().add(snapshot));
        return newPacket;
    }

    /**
     * Filter for SCommandListPacket. Uses {@link CommandTreeCleaner} to filter out any ArgumentTypes that are not in the "minecraft" or "brigadier" namespace.
     * A vanilla client would fail to deserialize the packet and disconnect with an error message if these were sent.
     */
    @NotNull
    private static ClientboundCommandsPacket filterCommandList(ClientboundCommandsPacket packet)
    {
        CommandBuildContext commandBuildContext = Commands.m_255082_(VanillaRegistries.m_255371_());
        RootCommandNode<SharedSuggestionProvider> root = packet.m_237624_(commandBuildContext);
        RootCommandNode<SharedSuggestionProvider> newRoot = CommandTreeCleaner.cleanArgumentTypes(root, argType -> {
            if (argType instanceof TestFunctionArgument || argType instanceof TestClassNameArgument)
                return false; // Vanilla connections should not have gametest on, so we should filter these out always

            ArgumentTypeInfo<?, ?> info = ArgumentTypeInfos.m_235382_(argType);
            ResourceLocation id = BuiltInRegistries.f_256979_.m_7981_(info);
            return id != null && (id.m_135827_().equals("minecraft") || id.m_135827_().equals("brigadier"));
        });
        return new ClientboundCommandsPacket(newRoot);
    }

    /**
     * Filters out custom tag types that the vanilla client won't recognize.
     * It prevents a rare error from logging and reduces the packet size
     */
    private static ClientboundUpdateTagsPacket filterCustomTagTypes(ClientboundUpdateTagsPacket packet) {
        Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = packet.m_179482_()
                .entrySet().stream().filter(e -> isVanillaRegistry(e.getKey().m_135782_()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new ClientboundUpdateTagsPacket(tags);
    }

    private static boolean isVanillaRegistry(ResourceLocation location)
    {
        // Checks if the registry name is contained within the static view of both BuiltInRegistries and VanillaRegistries
        return RegistryManager.getVanillaRegistryKeys().contains(location)
                || VanillaRegistries.DATAPACK_REGISTRY_KEYS.stream().anyMatch(k -> k.m_135782_().equals(location));
    }
}
