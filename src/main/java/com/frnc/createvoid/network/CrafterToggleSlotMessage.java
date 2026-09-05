package com.frnc.createvoid.network;

import com.frnc.createvoid.gui.menu.CrafterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S：玩家在 Crafter GUI 里切换某个槽位的禁用状态。 */
public class CrafterToggleSlotMessage {

    private final int containerId;
    private final int slot;
    private final boolean disabled;

    public CrafterToggleSlotMessage(int containerId, int slot, boolean disabled) {
        this.containerId = containerId;
        this.slot = slot;
        this.disabled = disabled;
    }

    public CrafterToggleSlotMessage(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(slot);
        buf.writeBoolean(disabled);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || slot < 0 || slot >= 9) {
                return;
            }
            if (sender.containerMenu instanceof CrafterMenu menu && menu.containerId == containerId) {
                menu.setSlotState(slot, disabled);
            }
        });
        context.setPacketHandled(true);
    }
}
