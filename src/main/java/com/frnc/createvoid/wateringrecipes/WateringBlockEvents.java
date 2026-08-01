package com.frnc.createvoid.wateringrecipes;

import com.frnc.createvoid.CreateVoid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 事件处理器：
 * <ul>
 *   <li>数据包同步/重载时：从配方动态注册浇水行为（新增配方无需改代码）</li>
 *   <li>方块破坏/放置时：清除 {@link WateringBlockTracker} 转换标记，
 *       使方块被破坏并重新放置后可以再次被浇灌</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CreateVoid.MOD_ID)
public class WateringBlockEvents {

    /**
     * 数据包同步/重载后，配方已加载，动态注册所有浇水配方的输入方块。
     * 该事件在服务器启动（玩家加入）和 {@code /reload} 时都会触发。
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        MinecraftServer server = event.getPlayerList().getServer();
        WateringBehaviour.registerBlockBehavioursFromRecipes(server.getRecipeManager());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level) {
            WateringBlockTracker.clear(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level) {
            WateringBlockTracker.clear(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            WateringBlockTracker.clearLevel(level.dimension());
        }
    }
}
