package com.frnc.createvoid.wateringrecipes;

import com.frnc.createvoid.CreateVoid;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 浇水喷淋行为：当 Create Spout 向方块输出流体时触发。
 * <p>
 * 无状态枚举单例，通过两种方式接入 Create Spout：
 * </p>
 * <ol>
 *   <li><b>官方注册表</b>（首选）：{@link #registerBlockBehaviours()} 将配方输入方块
 *       注册进 {@link BlockSpoutingBehaviour#BY_BLOCK}，与 Create 内置行为（泥巴→泥土、
 *       耕地湿润等）走完全相同的代码路径，不依赖 Mixin。</li>
 *   <li><b>Mixin 兜底</b>：{@code BlockSpoutingBehaviourMixin} 在
 *       {@link BlockSpoutingBehaviour#get(Level, BlockPos)} 头部动态检查配方，用于
 *       覆盖注册表之外的、运行期才出现的配方输入方块。</li>
 * </ol>
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li>Spout 向方块位置（其下方第 2 格）输出流体</li>
 *   <li>Create 调用 {@code BlockSpoutingBehaviour.get()}，从 BY_BLOCK 注册表返回此实例</li>
 *   <li>Create 调用 {@link #fillBlock(Level, BlockPos, SpoutBlockEntity, FluidStack, boolean)}</li>
 *   <li>遍历所有 watering 配方，匹配方块 + 流体</li>
 *   <li>非模拟模式：替换方块并返回消耗的流体量</li>
 * </ol>
 */
public enum WateringBehaviour implements BlockSpoutingBehaviour {
    INSTANCE;

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 已注册到 BY_BLOCK 的方块集合，防止重复注册。
     * <p>
     * Create 的 {@code SimpleRegistry} 不允许重复注册同一个键（会抛
     * {@code IllegalArgumentException}）。而注册可能来自多条路径（静态注册、
     * 动态配方扫描、OnDatapackSyncEvent 多次触发），必须去重。
     * </p>
     */
    private static final Set<Block> REGISTERED_BLOCKS = ConcurrentHashMap.newKeySet();

    // ==================== 官方注册表注册 ====================

    /**
     * 将当前所有 watering 配方的输入方块注册进 Create 的 {@link BlockSpoutingBehaviour#BY_BLOCK}。
     * <p>
     * 这是 Create 官方支持的扩展方式（Create 自己的泥巴转换、耕地湿润、炼药锅加水
     * 都通过同一注册表实现）。Spout 的 {@code tick()} 会在其下方第 2 格查询该注册表，
     * 命中后直接调用 {@link #fillBlock}，完全不经过 Mixin。
     * </p>
     * <p>
     * <b>注意：</b>这里目前硬编码了当前 3 条配方的输入方块。新增浇水配方时，
     * 需要在配方 JSON 之外同时在此处注册对应输入方块（或者依赖 {@code BlockSpoutingBehaviourMixin}
     * 的动态兜底，但 Mixin 路径可靠性略低）。
     * </p>
     */
    public static void registerBlockBehaviours() {
        registerBehaviour(Blocks.COPPER_BLOCK);
        registerBehaviour(Blocks.EXPOSED_COPPER);
        registerBehaviour(Blocks.WEATHERED_COPPER);
        registerBehaviour(AllBlocks.COPPER_CASING.get());
        LOGGER.info("[CreateVoid] Registered watering block behaviours into BY_BLOCK registry");
    }

    private static void registerBehaviour(Block block) {
        // 去重：同一方块只注册一次，避免 SimpleRegistry 抛重复异常
        if (!REGISTERED_BLOCKS.add(block)) {
            return;
        }
        BlockSpoutingBehaviour.BY_BLOCK.register(block, INSTANCE);
        LOGGER.info("[CreateVoid]   - {} -> WateringBehaviour.INSTANCE", block.getDescriptionId());
    }

    /**
     * 动态注册：遍历当前所有 watering 配方，将每个配方的输入方块注册进
     * {@link BlockSpoutingBehaviour#BY_BLOCK}。
     * <p>
     * 在数据包同步/重载后调用（此时配方已加载）。这样新增浇水配方时
     * <b>无需修改任何代码</b>，只需添加配方 JSON 即可。
     * </p>
     * <p>
     * 配方 JSON 的 {@code ingredients[0]} 是输入方块（物品原料），
     * {@code ingredient.getItems()} 会解析出所有匹配的物品栈；
     * 对每个 BlockItem 注册其对应方块。支持 {@code {"item": ...}} 和
     * {@code {"tag": ...}} 两种写法（tag 会展开为标签内所有方块）。
     * </p>
     *
     * @param recipeManager 服务器的配方管理器（数据包同步时可用）
     */
    public static void registerBlockBehavioursFromRecipes(RecipeManager recipeManager) {
        for (Recipe<?> r : recipeManager.getAllRecipesFor(RecipeTypes.WATERING.getType())) {
            if (!(r instanceof WateringRecipe recipe)) continue;
            if (recipe.getIngredients().isEmpty()) continue;

            Ingredient ingredient = recipe.getIngredients().get(0);
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.getItem() instanceof BlockItem bi) {
                    registerBehaviour(bi.getBlock());
                }
            }
        }
        LOGGER.info("[CreateVoid] Dynamic watering behaviour registration done");
    }

    // ==================== 配方查找 ====================

    /**
     * 快速检查：仅匹配方块（不检查流体），供 Mixin 调用。
     *
     * @param level 当前世界
     * @param state 目标方块状态
     * @return true 如果有任何浇水配方匹配该方块
     */
    public static boolean getRecipe(Level level, BlockState state) {
        var recipes = level.getRecipeManager()
                .getAllRecipesFor(RecipeTypes.WATERING.getType());
        String blockName = state.getBlock().getDescriptionId();
        LOGGER.debug("[CreateVoid] WateringBehaviour.getRecipe: checking {} recipes for block {}",
                recipes.size(), blockName);

        for (Recipe<?> r : recipes) {
            WateringRecipe recipe = (WateringRecipe) r;
            if (recipe.testTargetBlock(state)) {
                LOGGER.debug("[CreateVoid] WateringBehaviour: block {} matched recipe {}",
                        blockName, recipe.getId());
                return true;
            }
        }
        LOGGER.debug("[CreateVoid] WateringBehaviour: no recipe matched block {}",
                blockName);
        return false;
    }

    /**
     * 精确匹配：方块 + 流体 都匹配才返回配方。
     *
     * @param level 当前世界
     * @param fluid Spout 输出的流体栈
     * @param state 目标方块状态
     * @return 匹配的配方，如果无匹配则返回 null
     */
    private static WateringRecipe getRecipe(Level level, FluidStack fluid, BlockState state) {
        for (Recipe<?> r : level.getRecipeManager()
                .getAllRecipesFor(RecipeTypes.WATERING.getType())) {
            WateringRecipe recipe = (WateringRecipe) r;
            if (!recipe.testTargetBlock(state)) continue;

            FluidIngredient required = recipe.getRequiredFluid();
            if (required.test(fluid)) {
                return recipe;
            }
        }
        return null;
    }

    // ==================== BlockSpoutingBehaviour 接口实现 ====================

    /**
     * 核心方法：由 Create Spout 调用，执行实际的方块填充/替换。
     *
     * @param level    当前世界
     * @param pos      目标方块位置
     * @param spout    Spout 方块实体
     * @param fluid    Spout 输出的流体
     * @param simulate true = 仅模拟（不实际修改世界）
     * @return 消耗的流体量（mB）；0 表示不匹配或无法执行
     */
    @Override
    public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout,
                         FluidStack fluid, boolean simulate) {

        // 1. 空流体检查
        if (fluid.getFluid() == null) return 0;

        // 1.5 已被转换过的方块：模拟阶段直接拒绝，防止 Spout 连续转换同一方块
        if (simulate && WateringBlockTracker.isConverted(level, pos)) {
            return 0;
        }

        // 2. 获取当前方块状态
        BlockState currentState = level.getBlockState(pos);

        // 3. 精确匹配配方（方块 + 流体）
        WateringRecipe recipe = getRecipe(level, fluid, currentState);
        if (recipe == null) return 0;

        // 4. 二次验证流体（防御性编程）
        FluidIngredient required = recipe.getRequiredFluid();
        if (!required.test(fluid)) return 0;

        // 5. 非模拟模式下执行方块替换
        if (!simulate) {
            List<ItemStack> results = recipe.rollResults();
            if (results.isEmpty()) {
                LOGGER.warn("[CreateVoid] Watering recipe {} returned empty results, aborting", recipe.getId());
                return 0;
            }

            ItemStack result = results.get(0);
            // 配方显式要求掉落物品，或输出本就不是方块：走"消耗 + 掉落"分支
            if (!recipe.isDropAsItem() && result.getItem() instanceof BlockItem blockItem) {
                // 方块 → 方块：原地替换为目标方块
                level.setBlockAndUpdate(pos, blockItem.getBlock().defaultBlockState());
                // 记录本次转换，同一位置的方块在破坏/重新放置前不再重复转换
                WateringBlockTracker.markConverted(level, pos, blockItem.getBlock());
                LOGGER.info("[CreateVoid] Watering recipe {}: replaced {} with {} (one step)",
                        recipe.getId(), currentState.getBlock().getDescriptionId(),
                        blockItem.getBlock().getDescriptionId());
            } else {
                // 方块 → 物品：消耗原方块，将结果以掉落物形式产出
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                Block.popResource(level, pos, result);
                // 记录本次转换，防止同一位置被重复浇灌（方块已消失，标记为空气）
                WateringBlockTracker.markConverted(level, pos, Blocks.AIR);
                LOGGER.info("[CreateVoid] Watering recipe {}: consumed {} and dropped {} x{}",
                        recipe.getId(), currentState.getBlock().getDescriptionId(),
                        result.getDisplayName().getString(), result.getCount());
            }
        }

        // 6. 返回消耗的流体量（mB）
        return required.getRequiredAmount();
    }
}
