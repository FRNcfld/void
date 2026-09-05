package com.frnc.createvoid.network;

import com.frnc.createvoid.gui.menu.CrafterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S：玩家在 Crafter GUI 里 shift+点击网格槽，切换该槽的“物品类型锁定”。 */
public class CrafterToggleSlotLockMessage {

    private final int containerId;
    private final int slot;

    public CrafterToggleSlotLockMessage(int containerId, int slot) {
        this.containerId = containerId;
        this.slot = slot;
    }

    public CrafterToggleSlotLockMessage(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(slot);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || slot < 0 || slot >= 9) {
                return;
            }
            if (sender.containerMenu instanceof CrafterMenu menu && menu.containerId == containerId) {
                menu.toggleSlotLock(slot);
            }
        });
        context.setPacketHandled(true);
    }
}
