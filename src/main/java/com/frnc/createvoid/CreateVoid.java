package com.frnc.createvoid;

import com.frnc.createvoid.block.ModBlocks;
import com.frnc.createvoid.block.entity.ModBlockEntities;
import com.frnc.createvoid.fluid.ModFluids;
import com.frnc.createvoid.gui.ModMenuTypes;
import com.frnc.createvoid.item.ModCreativeModeTabs;
import com.frnc.createvoid.item.ModItems;
import com.frnc.createvoid.network.ModNetwork;
import com.frnc.createvoid.particle.ModParticles;
import com.frnc.createvoid.portal.VoidPortalPoi;
import com.frnc.createvoid.portal.VoidPortalTrainTracks;
import com.frnc.createvoid.sound.ModSounds;
import com.frnc.createvoid.wateringrecipes.RecipeTypes;
import com.frnc.createvoid.wateringrecipes.WateringBehaviour;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * 虚空科技（CreateVoid）主入口。
 * <p>
 * 本 mod 是 Create（机械动力）的体验优化附属，主要提供：
 * </p>
 * <ul>
 *   <li><b>浇水系统</b>：让 Create 的 Spout 能把方块"浇"成另一种方块（铜氧化链、
 *       海带胶 → 铜机械、岩浆 → 下界残骸碎片）。</li>
 *   <li><b>自动合成器</b>：红石上升沿触发的 3×3 自动合成台，支持禁用槽 / 锁定槽。</li>
 *   <li><b>虚空维度与传送门</b>：与主世界 1:1 坐标的虚空维度；传送门按原版下界门实现，
 *       框架为 Create 的列车机壳，手持精密构件右键点燃，Create 的列车也能穿过往返。</li>
 *   <li><b>海带胶</b>：自定义流体，粘稠、不可被其他流体替换。</li>
 * </ul>
 */
@Mod(CreateVoid.MOD_ID)
public class CreateVoid {

    /** 本 mod 的命名空间，需与 META-INF/mods.toml 中的 modId 一致。 */
    public static final String MOD_ID = "create_void";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 快捷创建本 mod 的 ResourceLocation。 */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public CreateVoid(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModItems.ITEMS.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        // 传送门方块要登记为 POI，跨维度找门才能像原版那样无视 Y 高度（见 VoidPortalPoi）
        VoidPortalPoi.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModParticles.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        RecipeTypes.register(modEventBus);
        ModNetwork.register();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 静态白名单：把内置的浇水配方输入方块注册进 Create 的 BY_BLOCK 注册表。
        // 数据包同步后还会有一段动态注册（见 WateringBlockEvents），
        // 因此新增配方 JSON 时无需改动代码。
        WateringBehaviour.registerBlockBehaviours();

        // Create 联动：把门方块注册进 PortalTrackProvider，让 Create 的列车能穿过传送门
        VoidPortalTrainTracks.register(event);

        LOGGER.info("[CreateVoid] common setup done");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[CreateVoid] server starting");
    }
}
