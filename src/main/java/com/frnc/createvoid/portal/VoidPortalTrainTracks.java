package com.frnc.createvoid.portal;

import com.frnc.createvoid.block.ModBlocks;
import com.frnc.createvoid.world.dimension.ModDimension;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Create 联动：让 Create 的<b>列车能穿过虚空传送门</b>往返主世界与虚空维度。
 *
 * <h3>为什么下界门能过车、虚空门原本不能</h3>
 * <p>
 * 这不是原版下界门自带的能力。Create 在自己的
 * {@code AllPortalTracks.registerDefaults()} 里显式做了：
 * </p>
 * <pre>
 * PortalTrackProvider.REGISTRY.register(Blocks.NETHER_PORTAL, AllPortalTracks::nether);
 * </pre>
 * <p>
 * 而 {@code nether()} 本体就一句
 * {@code PortalTrackProvider.fromTeleporter(level, face, NETHER, OVERWORLD, …)}。
 * 天境那条走的是同一个 {@code fromTeleporter}，只是传入天境自己的 {@code ITeleporter}。
 * 所以只要把 {@code create_void:void_block} 注册进去，虚空门就同样能过车。
 * </p>
 *
 * <h3>只注册门方块，不注册框架</h3>
 * <p>
 * {@code PortalTrackProvider.getOtherSide} 会取<b>轨道撞击面所连接的方块</b>去查注册表，
 * 命中才调 {@link #findExit}。把框架（列车机壳）也注册进来，看似能覆盖"车朝框架开过去"
 * 的入射方式，但 {@code fromProbe} 随后会做"同种方块"校验——<b>目标位置必须是同一种
 * 方块</b>。源方块是机壳、目标必然是门方块，这一校验永远不可能通过，注册了也连不上。
 * 因此只注册门方块；轨道需要真正撞进门幕才会触发。
 * </p>
 *
 * <h3>列车只连已有的门，不会凭空建门</h3>
 * <p>
 * 列车走的是探测实体（{@code SuperGlueEntity}）而不是玩家，所以
 * {@link VoidPortalTeleporter#getPortalInfo} 对它<b>只找门、不建门</b>——与原版一致：
 * {@code Entity#getExitPortal} 只调 {@code findPortalAround}，只有
 * {@code ServerPlayer#getExitPortal} 找不到时才 {@code createPortal}。
 * 半径内没有已存在的门，列车就是不连通，不会在对面凭空造一座。
 * </p>
 *
 * <h3>注册时机</h3>
 * <p>
 * 在 {@code FMLCommonSetupEvent} 注册。{@code REGISTRY} 是接口上的静态字段，
 * 类加载即可用，对注册顺序没有要求（键是我们的门方块，与 Create 自己的键不冲突）。
 * </p>
 */
public final class VoidPortalTrainTracks {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 与玩家传送一致的成对维度：主世界 ↔ 虚空。 */
    private static final ResourceKey<Level> FIRST_DIMENSION = Level.OVERWORLD;
    private static final ResourceKey<Level> SECOND_DIMENSION = ModDimension.VOID_LEVEL_KEY;

    private VoidPortalTrainTracks() {
    }

    /** 调用方见 {@code CreateVoid#commonSetup}。 */
    public static void register(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                PortalTrackProvider.REGISTRY.register(
                        ModBlocks.VOID_BLOCK.get(), VoidPortalTrainTracks::findExit);
                LOGGER.info("[CreateVoid] Registered void portal for Create train travel");
            } catch (Throwable t) {
                // 联动失败不应导致整个 mod 加载失败
                LOGGER.error("[CreateVoid] Failed to register Create train portal integration", t);
            }
        });
    }

    /**
     * 求"另一侧"的轨道出口。
     * <p>
     * 直接复用 {@link VoidPortalTeleporter}，因此坐标换算与落点逻辑和玩家传送完全一致
     * —— 不会出现"人过去了、车却落在别处"。
     * </p>
     *
     * @param level 入射轨道所在维度
     * @param face  入射轨道朝向传送门的面
     * @return 出口（目标维度 + 目标轨道面）；找不到返回 {@code null}
     */
    @Nullable
    private static PortalTrackProvider.Exit findExit(ServerLevel level, BlockFace face) {
        return PortalTrackProvider.fromTeleporter(
                level, face, FIRST_DIMENSION, SECOND_DIMENSION, VoidPortalTeleporter::new);
    }
}
