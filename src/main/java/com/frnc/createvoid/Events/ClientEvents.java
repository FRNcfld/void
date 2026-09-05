package com.frnc.createvoid.Events;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.ModBlocks;
import com.frnc.createvoid.fluid.ModFluids;
import com.frnc.createvoid.gui.ModMenuTypes;
import com.frnc.createvoid.gui.screen.CrafterScreen;
import com.frnc.createvoid.particle.ModParticles;
import com.frnc.createvoid.particle.WhiteSmokeParticle;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateVoid.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 设置流体渲染为不透明（粘稠质感）
            ItemBlockRenderTypes.setRenderLayer(ModFluids.KELP_GEL.get(), RenderType.solid());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_KELP_GEL.get(), RenderType.solid());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KELP_GEL_BLOCK.get(), RenderType.solid());
            // 注册 Crafter 界面
            MenuScreens.register(ModMenuTypes.CRAFTER_3X3.get(), CrafterScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WHITE_SMOKE.get(), WhiteSmokeParticle.Provider::new);
    }
}
