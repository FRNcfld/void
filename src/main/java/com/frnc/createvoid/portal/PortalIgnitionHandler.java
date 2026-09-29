package com.frnc.createvoid.portal;

import com.frnc.createvoid.CreateVoid;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 传送门激活：<b>右手手持精密构件右键框架方块</b>点燃，每次消耗 1 个。
 *
 * <h3>为什么用事件而不是物品类</h3>
 * <p>
 * 原版是 {@code FlintAndSteelItem.useOn} 点燃黑曜石门——那是原版自己的物品，
 * 我们可以给它覆写 {@code useOn}。但这里的激活物是 <b>Create 的精密构件</b>，
 * 是别的 mod 的物品，改不了它的类。所以只能走
 * {@link PlayerInteractEvent.RightClickBlock}：按"手持物是不是精密构件"判定，
 * 从而能支持任意 mod 的物品。
 * </p>
 *
 * <h3>为什么只处理主手</h3>
 * <p>
 * 该事件对主手与副手<b>各触发一次</b>。若两处都放行，第一次点燃成功后形状已被填满，
 * 第二次自然失败、事件不被取消——于是手持物的原版行为又会在框架上生效一遍。
 * 只放行主手即可避免这次重复。
 * </p>
 */
@Mod.EventBusSubscriber(modid = CreateVoid.MOD_ID)
public class PortalIgnitionHandler {

    /** 激活物：Create 的精密构件。 */
    public static final ResourceLocation PRECISION_MECHANISM_ID =
            new ResourceLocation("create", "precision_mechanism");

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        Direction face = event.getFace();
        if (face == null) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!isPrecisionMechanism(held)) {
            return;
        }

        Player player = event.getEntity();
        if (!VoidPortal.tryLight(level, event.getPos(), face, player)) {
            return;
        }

        // 阻止手持物的默认行为，并消耗一个（创造模式不消耗）
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!player.isCreative()) {
            held.shrink(1);
        }
    }

    /**
     * 手持物是否为精密构件。
     * <p>
     * 按 id 惰性查询而不是把 {@code Item} 缓存成静态字段：写死成静态字段会在
     * Create 缺失时直接抛空指针，按 id 查询天然安全，也符合可选依赖的语义。
     * </p>
     */
    private static boolean isPrecisionMechanism(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = ForgeRegistries.ITEMS.getValue(PRECISION_MECHANISM_ID);
        return item != null && item != Items.AIR && stack.is(item);
    }
}
