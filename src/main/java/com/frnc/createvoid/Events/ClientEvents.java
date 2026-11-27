package com.frnc.createvoid.Events;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.ModBlocks;
import com.frnc.createvoid.block.entity.ModBlockEntities;
import com.frnc.createvoid.client.EntityMyriadRenderer;
import com.frnc.createvoid.fluid.ModFluids;
import com.frnc.createvoid.gui.ModMenuTypes;
import com.frnc.createvoid.gui.screen.CrafterScreen;
import com.frnc.createvoid.particle.ModParticles;
import com.frnc.createvoid.particle.VoidPortalParticle;
import com.frnc.createvoid.particle.WhiteSmokeParticle;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.model.ModelSwapper;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateVoid.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    /**
     * 实体万象自己的连接纹理：基础贴图与 8×8 连接图集都放在 {@code create_void} 命名空间下，
     * 替换这两个 PNG 即可换材质，不再依赖 Create 的 framed_glass 资源。
     */
    private static final CTSpriteShiftEntry ENTITY_MYRIAD_CT =
            CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
                    new ResourceLocation(CreateVoid.MOD_ID, "block/entity_myriad/entity_myriad"),
                    new ResourceLocation(CreateVoid.MOD_ID, "block/entity_myriad/entity_myriad_connected"));

    /** 分选变体使用独立的一套贴图与连接图集，后续调整外观不会影响普通实体万象。 */
    private static final CTSpriteShiftEntry ENTITY_MYRIAD_SORTING_CT =
            CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
                    new ResourceLocation(CreateVoid.MOD_ID, "block/entity_myriad_sorting/entity_myriad_sorting"),
                    new ResourceLocation(CreateVoid.MOD_ID, "block/entity_myriad_sorting/entity_myriad_sorting_connected"));

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
            // 实体万象（含分选变体）使用自己的 cutout 连接玻璃
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENTITY_MYRIAD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENTITY_MYRIAD_SORTING.get(), RenderType.cutout());
            // 注册 Crafter 界面
            MenuScreens.register(ModMenuTypes.CRAFTER_3X3.get(), CrafterScreen::new);

            // 实体万象与分选方块使用 Create 的“按住 Shift 查看说明”物品描述。
            // 具体文本在 lang 的 <block id>.tooltip.summary 中，格式与 Create 元件一致。
            registerDescription(ModBlocks.ENTITY_MYRIAD.get().asItem());
            registerDescription(ModBlocks.ENTITY_MYRIAD_SORTING.get().asItem());
        });
    }

    private static void registerDescription(Item item) {
        TooltipModifier.REGISTRY.register(item,
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WHITE_SMOKE.get(), WhiteSmokeParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VOID_PORTAL.get(), VoidPortalParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ENTITY_MYRIAD.get(), EntityMyriadRenderer::new);
    }

    /**
     * 让实体万象使用 Create 的连接纹理系统，但贴图取自本模组自己的资源：
     * 模型贴图是 entity_myriad，CTModel 按邻居自动切换到 entity_myriad_connected 图集。
     */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelSwapper.swapModels(event.getModels(),
                ModelSwapper.getAllBlockStateModelLocations(ModBlocks.ENTITY_MYRIAD.get()),
                model -> new CTModel(model, new SimpleCTBehaviour(ENTITY_MYRIAD_CT)));
        ModelSwapper.swapModels(event.getModels(),
                ModelSwapper.getAllBlockStateModelLocations(ModBlocks.ENTITY_MYRIAD_SORTING.get()),
                model -> new CTModel(model, new SimpleCTBehaviour(ENTITY_MYRIAD_SORTING_CT)));
    }
}
