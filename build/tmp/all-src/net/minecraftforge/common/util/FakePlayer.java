/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.util;

import com.mojang.authlib.GameProfile;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * A basic fake server player implementation that can be used to simulate player actions.
 */
public class FakePlayer extends ServerPlayer
{
    public FakePlayer(ServerLevel level, GameProfile name)
    {
        super(level.m_7654_(), level, name);
        this.f_8906_ = new FakePlayerNetHandler(level.m_7654_(), this);
    }

    @Override public void m_5661_(Component chatComponent, boolean actionBar) { }
    @Override public void m_6278_(Stat stat, int amount) { }
    @Override public boolean m_6673_(DamageSource source) { return true; }
    @Override public boolean m_7099_(Player player) { return false; }
    @Override public void m_6667_(DamageSource source) { }
    @Override public void m_8119_() { }
    @Override public void m_9156_(ServerboundClientInformationPacket packet) { }
    @Override @Nullable public MinecraftServer m_20194_() { return ServerLifecycleHooks.getCurrentServer(); }

    @ParametersAreNonnullByDefault
    private static class FakePlayerNetHandler extends ServerGamePacketListenerImpl {
        private static final Connection DUMMY_CONNECTION = new Connection(PacketFlow.CLIENTBOUND);

        public FakePlayerNetHandler(MinecraftServer server, ServerPlayer player) {
            super(server, DUMMY_CONNECTION, player);
        }

        @Override public void m_9933_() { }
        @Override public void m_9953_() { }
        @Override public void m_9942_(Component message) { }
        @Override public void m_5918_(ServerboundPlayerInputPacket packet) { }
        @Override public void m_5659_(ServerboundMoveVehiclePacket packet) { }
        @Override public void m_7376_(ServerboundAcceptTeleportationPacket packet) { }
        @Override public void m_7411_(ServerboundRecipeBookSeenRecipePacket packet) { }
        @Override public void m_7982_(ServerboundRecipeBookChangeSettingsPacket packet) { }
        @Override public void m_6947_(ServerboundSeenAdvancementsPacket packet) { }
        @Override public void m_7741_(ServerboundCommandSuggestionPacket packet) { }
        @Override public void m_7192_(ServerboundSetCommandBlockPacket packet) { }
        @Override public void m_6629_(ServerboundSetCommandMinecartPacket packet) { }
        @Override public void m_7965_(ServerboundPickItemPacket packet) { }
        @Override public void m_5591_(ServerboundRenameItemPacket packet) { }
        @Override public void m_5712_(ServerboundSetBeaconPacket packet) { }
        @Override public void m_7424_(ServerboundSetStructureBlockPacket packet) { }
        @Override public void m_8019_(ServerboundSetJigsawBlockPacket packet) { }
        @Override public void m_6449_(ServerboundJigsawGeneratePacket packet) { }
        @Override public void m_6321_(ServerboundSelectTradePacket packet) { }
        @Override public void m_6829_(ServerboundEditBookPacket packet) { }
        @Override public void m_7548_(ServerboundEntityTagQuery packet) { }
        @Override public void m_6780_(ServerboundBlockEntityTagQuery packet) { }
        @Override public void m_7185_(ServerboundMovePlayerPacket packet) { }
        @Override public void m_9774_(double x, double y, double z, float yaw, float pitch) { }
        @Override public void m_7502_(ServerboundPlayerActionPacket packet) { }
        @Override public void m_6371_(ServerboundUseItemOnPacket packet) { }
        @Override public void m_5760_(ServerboundUseItemPacket packet) { }
        @Override public void m_6936_(ServerboundTeleportToEntityPacket packet) { }
        @Override public void m_7529_(ServerboundResourcePackPacket packet) { }
        @Override public void m_5938_(ServerboundPaddleBoatPacket packet) { }
        @Override public void m_7026_(Component message) { }
        @Override public void m_9829_(Packet<?> packet) { }
        @Override public void m_243119_(Packet<?> packet, @Nullable PacketSendListener sendListener) { }
        @Override public void m_7798_(ServerboundSetCarriedItemPacket packet) { }
        @Override public void m_7388_(ServerboundChatPacket packet) { }
        @Override public void m_7953_(ServerboundSwingPacket packet) { }
        @Override public void m_5681_(ServerboundPlayerCommandPacket packet) { }
        @Override public void m_6946_(ServerboundInteractPacket packet) { }
        @Override public void m_6272_(ServerboundClientCommandPacket packet) { }
        @Override public void m_7951_(ServerboundContainerClosePacket packet) { }
        @Override public void m_5914_(ServerboundContainerClickPacket packet) { }
        @Override public void m_7191_(ServerboundPlaceRecipePacket packet) { }
        @Override public void m_6557_(ServerboundContainerButtonClickPacket packet) { }
        @Override public void m_5964_(ServerboundSetCreativeModeSlotPacket packet) { }
        @Override public void m_5527_(ServerboundSignUpdatePacket packet) { }
        @Override public void m_5683_(ServerboundKeepAlivePacket packet) { }
        @Override public void m_6828_(ServerboundPlayerAbilitiesPacket packet) { }
        @Override public void m_5617_(ServerboundClientInformationPacket packet) { }
        @Override public void m_7423_(ServerboundCustomPayloadPacket packet) { }
        @Override public void m_7477_(ServerboundChangeDifficultyPacket packet) { }
        @Override public void m_7728_(ServerboundLockDifficultyPacket packet) { }
        @Override public void m_9780_(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relativeSet) { }
        @Override public void m_215201_(int sequence) { }
        @Override public void m_214047_(ServerboundChatCommandPacket packet) { }
        @Override public void m_241885_(ServerboundChatAckPacket packet) { }
        @Override public void m_241992_(PlayerChatMessage message) { }
        @Override public void m_245431_(PlayerChatMessage message, ChatType.Bound boundChatType) { }
        @Override public void m_245903_(Component content, ChatType.Bound boundChatType) { }
        @Override public void m_252797_(ServerboundChatSessionUpdatePacket packet) { }
    }
}
