package com.frnc.createvoid.Events;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.ModBlocks;
import com.frnc.createvoid.fluid.ModFluids;
import com.frnc.createvoid.gui.ModMenuTypes;
import com.frnc.createvoid.gui.screen.CrafterScreen;
import com.frnc.createvoid.particle.ModParticles;
import com.frnc.createvoid.particle.VoidPortalParticle;
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
            // 传送门用 translucent：纹理是半透明的（靠逐像素 alpha 抖动形成织理），
            // 与参考的天境/原版下界门一致，能透出背后的地形。
            // 模型几何直接继承原版 nether_portal_ns / _ew，所以不会出现薄片侧边。
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VOID_BLOCK.get(), RenderType.translucent());
            // 注册 Crafter 界面
            MenuScreens.register(ModMenuTypes.CRAFTER_3X3.get(), CrafterScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WHITE_SMOKE.get(), WhiteSmokeParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VOID_PORTAL.get(), VoidPortalParticle.Provider::new);
    }
}
