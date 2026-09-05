package com.frnc.createvoid.network;

import com.frnc.createvoid.CreateVoid;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** 轻量 SimpleChannel：仅一个 C2S 报文（切换槽位禁用）。 */
public class ModNetwork {

    private static final String VERSION = "1";
    private static int nextId = 0;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CreateVoid.MOD_ID, "net"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(nextId++, CrafterToggleSlotMessage.class,
                CrafterToggleSlotMessage::encode,
                CrafterToggleSlotMessage::new,
                CrafterToggleSlotMessage::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), message);
    }
}
