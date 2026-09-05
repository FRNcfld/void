package com.frnc.createvoid.gui.menu;

import com.frnc.createvoid.block.entity.CrafterBlockEntity;
import com.frnc.createvoid.gui.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 自动合成器容器菜单：左边 3×3 网格 + 结果预览槽(134,35) + 玩家背包。
 * <p>
 * 布局对齐原版 crafter_port：网格 x=26,44,62 / y=17,35,53；玩家背包与热栏同原版。
 * 槽位禁用标记经 {@link ContainerData}(0..8 禁用位，9 红石触发)与客户端同步。
 * 禁用槽只允许为空槽时切换。
 * </p>
 */
public class CrafterMenu extends AbstractContainerMenu {

    /** 网格 0..8；玩家背包 9..44；结果预览槽 45。 */
    public static final int RESULT_SLOT = 45;
    public static final int GHOST_FIRST_SLOT = 46;
    public static final int PLAYER_INV_FIRST_SLOT = 9;
    public static final int PLAYER_INV_LAST_SLOT = RESULT_SLOT;

    private final Container grid;
    private final ContainerData data;
    private final ResultContainer resultContainer = new ResultContainer();
    /** 锁定格的“虚影槽”：仅用于把锁定物品同步给客户端显示，离屏且不可交互。 */
    private final SimpleContainer ghostContainer = new SimpleContainer(CrafterBlockEntity.CONTAINER_SIZE);

    /** 客户端（MenuType factory）使用：虚拟容器，物品由服务端推送进 Slot。 */
    public CrafterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CrafterBlockEntity.CONTAINER_SIZE),
                new SimpleContainerData(CrafterBlockEntity.DATA_COUNT));
    }

    /** 服务端使用：以方块实体的容器 + 容器数据构建。 */
    public CrafterMenu(int containerId, Inventory playerInventory, Container grid, ContainerData data) {
        super(ModMenuTypes.CRAFTER_3X3.get(), containerId);
        checkContainerSize(grid, CrafterBlockEntity.CONTAINER_SIZE);
        this.grid = grid;
        this.data = data;
        grid.startOpen(playerInventory.player);

        // 3×3 合成网格（行主序：index = row*3 + col）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new CrafterSlot(grid, row * 3 + col, 26 + col * 18, 17 + row * 18, this));
            }
        }
        // 玩家主背包（3 行）+ 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        // 只读结果预览槽（右侧产物格）
        this.addSlot(new NonInteractiveResultSlot(resultContainer, 0, 134, 35));
        // 锁定虚影槽（离屏，仅用于同步锁定物品到客户端）
        for (int i = 0; i < CrafterBlockEntity.CONTAINER_SIZE; i++) {
            this.addSlot(new Slot(ghostContainer, i, -10000, -10000));
        }

        this.addDataSlots(data);
        // 打开时就计算一次结果预览（服务端才有效）
        this.onGridChanged();
    }

    // ==================== 结果预览 ====================

    /** 网格槽内容变化时调用：服务端重算“当前布局能否合成”。 */
    public void onGridChanged() {
        if (grid instanceof CrafterBlockEntity crafter) {
            Level level = crafter.getLevel();
            if (level != null && !level.isClientSide) {
                resultContainer.setItem(0, crafter.getCraftingResult(level));
                refreshGhosts(crafter);
            }
        }
    }

    /** 把方块实体的锁定物品同步进虚影槽（服务端）。 */
    private void refreshGhosts(CrafterBlockEntity crafter) {
        for (int i = 0; i < CrafterBlockEntity.CONTAINER_SIZE; i++) {
            ghostContainer.setItem(i, crafter.getLockedItem(i));
        }
    }

    /** 客户端取锁定虚影物品（由服务端经虚影槽同步而来）。 */
    public ItemStack getGhostItem(int slotIndex) {
        return ghostContainer.getItem(slotIndex);
    }

    // ==================== 槽位禁用 ====================

    public boolean isSlotDisabled(int slot) {
        return slot >= 0 && slot < CrafterBlockEntity.CONTAINER_SIZE && data.get(slot) != 0;
    }

    public boolean isPowered() {
        return data.get(CrafterBlockEntity.DATA_TRIGGERED) != 0;
    }

    /** 客户端：该槽是否被锁定（由容器数据 10..18 同步而来）。 */
    public boolean isSlotLocked(int slot) {
        return slot >= 0 && slot < CrafterBlockEntity.CONTAINER_SIZE
                && data.get(CrafterBlockEntity.DATA_LOCK_START + slot) != 0;
    }

    /**
     * 切换禁用状态。服务端（grid 是方块实体）写回 BE；客户端直接改本地数据即时回显，
     * 真正的持久化通过 C2S 报文在服务端完成。
     */
    public void setSlotState(int slot, boolean disabled) {
        if (slot < 0 || slot >= CrafterBlockEntity.CONTAINER_SIZE) {
            return;
        }
        if (grid instanceof CrafterBlockEntity crafter) {
            crafter.setSlotState(slot, disabled);
        } else {
            data.set(slot, disabled ? 1 : 0);
        }
    }

    public Container getGrid() {
        return grid;
    }

    /** 服务端：shift 点击网格槽 → 切换物品类型锁定。 */
    public void toggleSlotLock(int slot) {
        if (grid instanceof CrafterBlockEntity crafter) {
            crafter.toggleSlotLock(slot);
            refreshGhosts(crafter);
        }
    }

    // ==================== 标准菜单逻辑 ====================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < PLAYER_INV_FIRST_SLOT) {
                // 网格 → 玩家背包
                if (!this.moveItemStackTo(stack, PLAYER_INV_FIRST_SLOT, PLAYER_INV_LAST_SLOT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < PLAYER_INV_LAST_SLOT) {
                // 玩家背包 → 网格
                if (!this.moveItemStackTo(stack, 0, PLAYER_INV_FIRST_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.grid.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.grid.stopOpen(player);
    }
}
