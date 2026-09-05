package com.frnc.createvoid.block.entity;

import com.frnc.createvoid.block.custom.CrafterBlock;
import com.frnc.createvoid.gui.menu.CrafterMenu;
import com.frnc.createvoid.particle.ModParticles;
import com.frnc.createvoid.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * 自动合成器的方块实体：容纳 3×3 网格、保存“禁用槽”标记、驱动合成消耗与产物输出。
 * <p>
 * 合成判定完全走原版 {@link RecipeType#CRAFTING} 的 Shaped/Shapeless 配方，
 * 匹配时把“被禁用的槽”当作空格处理（不参与配方形状）。
 * 产物先尝试送入正前方邻居的 IItemHandler，其次原版 {@link Container}，
 * 都没有则从正面抛成掉落物。
 * </p>
 */
public class CrafterBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int CONTAINER_SIZE = 9;
    public static final int GRID_WIDTH = 3;
    public static final int GRID_HEIGHT = 3;

    /** 容器数据同步：0..8 = 各槽禁用标记(0/1)，9 = 是否被红石触发。 */
    public static final int DATA_TRIGGERED = CONTAINER_SIZE;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final boolean[] disabledSlots = new boolean[CONTAINER_SIZE];
    private final SimpleContainerData containerData = new SimpleContainerData(CONTAINER_SIZE + 1);

    /** 合成动画倒计时（>0 时方块显示 crafting 纹理，期间忽略新触发）。 */
    private int craftingTicksRemaining;

    private LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();

    public CrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRAFTER.get(), pos, state);
    }

    // ==================== 菜单 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.create_void.crafter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // 把方块当前的红石触发状态同步进容器数据（9 号位），供 GUI 红石灯显示
        if (level != null && !level.isClientSide) {
            setTriggered(level.getBlockState(worldPosition).getValue(CrafterBlock.TRIGGERED));
        }
        return new CrafterMenu(containerId, inventory, this, containerData);
    }

    // ==================== 槽位禁用 ====================

    public boolean isSlotDisabled(int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE && disabledSlots[slot];
    }

    public void setSlotDisabled(int slot, boolean disabled) {
        setSlotState(slot, disabled);
    }

    /** 设置槽位禁用状态（服务端权威）。 */
    public void setSlotState(int slot, boolean disabled) {
        if (slot < 0 || slot >= CONTAINER_SIZE || disabledSlots[slot] == disabled) {
            return;
        }
        disabledSlots[slot] = disabled;
        containerData.set(slot, disabled ? 1 : 0);
        setChanged();
    }

    public boolean isCrafting() {
        return craftingTicksRemaining > 0;
    }

    public void setCraftingTicksRemaining(int ticks) {
        this.craftingTicksRemaining = ticks;
    }

    /** 比较器信号：被禁用槽的数量（0~9）。 */
    public int countDisabled() {
        int n = 0;
        for (boolean b : disabledSlots) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    /** 红石触发状态写入容器数据（9 号位），供已打开的 GUI 显示红石灯。 */
    public void setTriggered(boolean triggered) {
        containerData.set(DATA_TRIGGERED, triggered ? 1 : 0);
    }

    public SimpleContainerData getContainerData() {
        return containerData;
    }

    // ==================== 配方查找 ====================

    /**
     * 构造“启用槽视图”的 3×3 网格：禁用槽以 EMPTY 占位。
     * 仅用于只读匹配，menu 传 null。
     */
    // ==================== 服务端逐 tick 轮询红石（上升沿触发） ====================

    /**
     * 每个加载的服务端 tick 执行：
     * <ol>
     *   <li>若在合成动画中，递减倒计时，归零时收回 crafting 纹理；</li>
     *   <li>空闲时轮询红石输入，检测“上升沿”并调度方块 tick 合成一次
     *       （比仅依赖 neighborChanged 可靠，方块放在已供电电源旁也能触发）。</li>
     * </ol>
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CrafterBlockEntity be) {
        if (level.isClientSide || be == null) {
            return;
        }
        if (!(state.getBlock() instanceof CrafterBlock)) {
            return;
        }
        // 合成动画倒计时
        if (be.craftingTicksRemaining > 0) {
            be.craftingTicksRemaining--;
            if (be.craftingTicksRemaining == 0 && state.getValue(CrafterBlock.CRAFTING)) {
                level.setBlock(pos, state.setValue(CrafterBlock.CRAFTING, false), 3);
            }
            return;
        }
        // 空闲：轮询红石上升沿
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean triggered = state.getValue(CrafterBlock.TRIGGERED);
        if (powered && !triggered) {
            level.setBlock(pos, state.setValue(CrafterBlock.TRIGGERED, true), 3);
            be.setTriggered(true);
            level.scheduleTick(pos, state.getBlock(), 2);
        } else if (!powered && triggered) {
            level.setBlock(pos, state.setValue(CrafterBlock.TRIGGERED, false), 3);
            be.setTriggered(false);
        }
    }

    private String dumpGrid() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack s = items.get(i);
            sb.append(s.isEmpty() ? "_" : s.getHoverName().getString()).append('[').append(disabledSlots[i] ? "x" : " ").append("] ");
        }
        return sb.toString();
    }

    private TransientCraftingContainer buildGrid() {
        NonNullList<ItemStack> list = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (!disabledSlots[i]) {
                list.set(i, items.get(i));
            }
        }
        return new TransientCraftingContainer(null, GRID_WIDTH, GRID_HEIGHT, list);
    }

    public Optional<CraftingRecipe> findRecipe(Level level) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, buildGrid(), level);
    }

    /** GUI 结果预览：当前布局能否合成、合成出什么。 */
    public ItemStack getCraftingResult(Level level) {
        return findRecipe(level)
                .map(recipe -> recipe.assemble(buildGrid(), level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    // ==================== 合成一次 ====================

    /**
     * 由方块在红石上升沿的 tick 中调用，执行一次完整合成。
     *
     * @return true 表示成功合并且产物已处理
     */
    public boolean craftOnce(ServerLevel level, BlockState state) {
        LOGGER.info("[Crafter] craftOnce @{} grid={}", worldPosition, dumpGrid());
        Optional<CraftingRecipe> optional = findRecipe(level);
        if (optional.isEmpty()) {
            LOGGER.info("[Crafter] craftOnce: no recipe found @{}", worldPosition);
            return false;
        }
        CraftingRecipe recipe = optional.get();
        ItemStack result = recipe.assemble(buildGrid(), level.registryAccess());
        if (result.isEmpty()) {
            LOGGER.info("[Crafter] craftOnce: empty result for {} @{}", recipe.getId(), worldPosition);
            return false;
        }
        LOGGER.info("[Crafter] craftOnce: recipe={} result={}x{}", recipe.getId(), result.getDisplayName().getString(), result.getCount());

        consumeIngredients();
        dispatchResult(level, state, result);

        level.playSound(null, worldPosition, ModSounds.CRAFTER_CRAFT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        Direction front = state.getValue(CrafterBlock.ORIENTATION).front();
        double px = worldPosition.getX() + 0.5 + front.getStepX() * 0.5;
        double py = worldPosition.getY() + 0.5 + front.getStepY() * 0.5;
        double pz = worldPosition.getZ() + 0.5 + front.getStepZ() * 0.5;
        level.sendParticles(ModParticles.WHITE_SMOKE.get(), px, py, pz, 4, 0.1D, 0.1D, 0.1D, 0.01D);
        return true;
    }

    /**
     * 消耗原料。匹配时禁用槽按空格处理，启用且非空的格子都参与了配方，
     * 因此对每个启用非空格减 1；带“容器残留物”（桶等）的留下残留物。
     */
    private void consumeIngredients() {
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (disabledSlots[i]) {
                continue;
            }
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            boolean hasRemainder = stack.hasCraftingRemainingItem();
            stack.shrink(1);
            if (stack.isEmpty() && hasRemainder) {
                items.set(i, new ItemStack(stack.getItem().getCraftingRemainingItem()));
            }
        }
        setChanged();
    }

    private void dispatchResult(ServerLevel level, BlockState state, ItemStack result) {
        Direction front = state.getValue(CrafterBlock.ORIENTATION).front();
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(front));
        ItemStack remaining = result;

        if (neighbor != null) {
            // 1) 优先 IItemHandler（Create 管道 / 漏斗 / 容器）
            LazyOptional<IItemHandler> cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, front.getOpposite());
            if (cap.isPresent()) {
                IItemHandler handler = cap.resolve().orElse(null);
                if (handler != null) {
                    for (int s = 0; s < handler.getSlots() && !remaining.isEmpty(); s++) {
                        remaining = handler.insertItem(s, remaining, false);
                    }
                }
                cap.invalidate();
            }
            // 2) 其次原版 Container
            if (!remaining.isEmpty() && neighbor instanceof Container container) {
                remaining = insertIntoContainer(container, remaining);
            }
        }

        // 3) 无处安放就向正面抛出
        if (!remaining.isEmpty()) {
            spawnResult(level, remaining, front);
        }
    }

    private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack cur = container.getItem(i);
            if (cur.isEmpty()) {
                if (container.canPlaceItem(i, remaining)) {
                    int put = Math.min(remaining.getCount(), 64);
                    container.setItem(i, remaining.split(put));
                }
            } else if (ItemStack.isSameItemSameTags(cur, remaining)) {
                int space = Math.min(cur.getMaxStackSize(), 64) - cur.getCount();
                if (space > 0) {
                    int put = Math.min(space, remaining.getCount());
                    cur.grow(put);
                    remaining.shrink(put);
                    container.setChanged();
                }
            }
        }
        return remaining;
    }

    private void spawnResult(ServerLevel level, ItemStack stack, Direction front) {
        double x = worldPosition.getX() + 0.5 + front.getStepX() * 0.7;
        double y = worldPosition.getY() + 0.5 + front.getStepY() * 0.7;
        double z = worldPosition.getZ() + 0.5 + front.getStepZ() * 0.7;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        double d3 = level.random.nextDouble() * 0.1D + 0.2D;
        entity.setDeltaMovement(
                level.random.triangle(front.getStepX() * d3, 0.0172275D * 6),
                level.random.triangle(0.2D, 0.0172275D * 6),
                level.random.triangle(front.getStepZ() * d3, 0.0172275D * 6));
        level.addFreshEntity(entity);
    }

    // ==================== Container ====================

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return;
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return !isSlotDisabled(index);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0D;
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        byte mask = 0;
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (disabledSlots[i]) {
                mask |= (byte) (1 << i);
            }
        }
        tag.putByte("disabled_slots", mask);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        byte mask = tag.getByte("disabled_slots");
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            boolean disabled = (mask & (1 << i)) != 0;
            disabledSlots[i] = disabled;
            containerData.set(i, disabled ? 1 : 0);
        }
    }

    // ==================== IItemHandler capability（Create 等管道存取） ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (!itemHandler.isPresent()) {
                itemHandler = LazyOptional.of(CrafterBlockEntity.this::createItemHandler);
            }
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        itemHandler = LazyOptional.empty();
    }

    private IItemHandler createItemHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return CONTAINER_SIZE;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return getItem(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot < 0 || slot >= CONTAINER_SIZE || stack.isEmpty()) {
                    return stack;
                }
                // 禁用槽拒绝放入
                if (disabledSlots[slot]) {
                    return stack;
                }
                ItemStack current = items.get(slot);
                if (current.isEmpty()) {
                    int put = Math.min(stack.getCount(), getSlotLimit(slot));
                    if (!simulate) {
                        items.set(slot, stack.copy());
                        items.get(slot).setCount(put);
                        setChanged();
                    }
                    ItemStack rest = stack.copy();
                    rest.shrink(put);
                    return rest;
                }
                if (!ItemStack.isSameItemSameTags(current, stack)) {
                    return stack;
                }
                int space = Math.min(getSlotLimit(slot), current.getMaxStackSize()) - current.getCount();
                if (space <= 0) {
                    return stack;
                }
                int put = Math.min(space, stack.getCount());
                if (!simulate) {
                    current.grow(put);
                    setChanged();
                }
                ItemStack rest = stack.copy();
                rest.shrink(put);
                return rest;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < 0 || slot >= CONTAINER_SIZE || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                ItemStack current = items.get(slot);
                if (current.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                int take = Math.min(amount, current.getCount());
                ItemStack out = current.copy();
                out.setCount(take);
                if (!simulate) {
                    current.shrink(take);
                    setChanged();
                }
                return out;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !isSlotDisabled(slot);
            }
        };
    }
}
