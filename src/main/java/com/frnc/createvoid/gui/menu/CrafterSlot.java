package com.frnc.createvoid.gui.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 3×3 网格槽：被禁用时拒绝放入；内容变化时触发菜单重算结果预览。
 */
public class CrafterSlot extends Slot {

    private final CrafterMenu menu;

    public CrafterSlot(Container container, int index, int x, int y, CrafterMenu menu) {
        super(container, index, x, y);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Slot.mayPlace 默认恒为 true，不查容器 canPlaceItem；
        // 这里显式问容器（方块实体的 allowsItem：禁用 + 锁定过滤），否则玩家可手动绕过锁定。
        if (this.menu.isSlotDisabled(this.getSlotIndex())) {
            return false;
        }
        return this.container.canPlaceItem(this.getSlotIndex(), stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.menu.onGridChanged();
    }
}
