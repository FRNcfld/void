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
        return super.mayPlace(stack) && !this.menu.isSlotDisabled(this.getSlotIndex());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.menu.onGridChanged();
    }
}
