package com.frnc.createvoid.gui.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 离屏虚影槽：仅作为「把服务端数据同步给客户端」的载体，永远不可交互。
 * <p>
 * 用于把锁定槽的物品预览推送到客户端。槽位放在 (-10000, -10000)，本来就不在
 * 可视范围内，但仅仅"移出屏幕"是不够的：
 * </p>
 * <ul>
 *   <li>{@code Slot} 默认 {@code isActive() == true}，这意味着它仍是协议里可点击的槽位；</li>
 *   <li>空 {@link Container} 上的 {@code Slot.mayPlace} 默认为 true，
 *       因此一个构造出来的 {@code clicked} 报文就能往虚影槽里塞物品。</li>
 * </ul>
 * <p>
 * 这里显式关闭 {@code isActive} / {@code mayPlace} / {@code mayPickup}。
 * 注意服务端仍然可以写内容（{@code refreshGhosts} 使用容器自身的 setItem），
 * 因为同步走的是容器内容广播，与槽位是否"可交互"无关。
 * </p>
 */
public class GhostSlot extends Slot {

    public GhostSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    /** 不参与任何客户端交互（含拖拽、快速移动的落点判定）。 */
    @Override
    public boolean isActive() {
        return false;
    }
}
