package com.frnc.createvoid.block.custom;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 本 mod 各机械 / 传送方块的碰撞箱集合。
 * 每个形状都根据对应模型的元素包围盒推导而来：
 * 主体结构（外壳、底座、齿轮箱、屏幕等）计入碰撞，
 * 细小的装饰件（齿轮齿、天线、管道、窗口等）忽略，
 * 且所有坐标均被限制在方块单元 [0,16] 内，避免碰撞箱侵入相邻方块。
 */
public final class VoxelShapes {

    private VoxelShapes() {
    }

    // 传送方块：16x3x16 的扁平薄板，对应 void_block 模型唯一元素 [0,0,0]-[16,3,16]
    public static final VoxelShape VOID_BLOCK = box(0, 0, 0, 16, 3, 16);

    // 安山机器：主外壳 + 前方齿轮箱 + 顶部凸起
    // 模型主体元素：外壳 [0,0,4]-[16,16,16]，齿轮箱 [4,4,0]-[12,12,4]，顶部 [3,14,2]-[13,18,16]（y 截断到 16）
    public static final VoxelShape ANDESITE_MACHINE = or(
            box(0, 0, 4, 16, 16, 16),
            box(4, 4, 0, 12, 12, 4),
            box(3, 14, 2, 13, 16, 16));

    // 铜机器：底座 + 主体 + 流体罐 + 连接件
    // 模型主体元素：底座 [0,0,0]-[16,4,16]，主体 [6,4,6]-[16,20,16]（y 截断到 16），
    //              流体罐 [1,6,7]-[9,22,15]（y 截断到 16），连接件 [0,4,6]-[6,6,16]
    public static final VoxelShape COPPER_MACHINE = or(
            box(0, 0, 0, 16, 4, 16),
            box(6, 4, 6, 16, 16, 16),
            box(1, 6, 7, 9, 16, 15),
            box(0, 4, 6, 6, 6, 16));

    // 黄铜机器：底座 + 右侧主体 + 左侧机械臂 + 中央竖轴 + 左侧显像管柱
    // 模型主体元素：底座 [0,0,0]-[16,4,16]，右侧 [8,4,4]-[16,16,16]，左侧臂 [0,4,3]-[10,10,13]，
    //              竖轴 [10,2,2]-[14,18,6]（y 截断到 16），显像管柱 [1,12,5]-[7,21,11]（y 截断到 16，下接连接件 [1,10,5]-[7,12,11]）
    public static final VoxelShape BRASS_MACHINE = or(
            box(0, 0, 0, 16, 4, 16),
            box(8, 4, 4, 16, 16, 16),
            box(0, 4, 3, 10, 10, 13),
            box(10, 2, 2, 14, 16, 6),
            box(1, 10, 5, 7, 16, 11));

    // 红石机器：底座 + 显示屏
    // 模型主体元素：底座 [0,0,0]-[16,6,16]，屏幕 [2,6,3]-[14,15,6]（天线、按钮等细小件忽略）
    public static final VoxelShape REDSTONE_MACHINE = or(
            box(0, 0, 0, 16, 6, 16),
            box(2, 6, 3, 14, 15, 6));

    //自动合成器
    public static final VoxelShape CRAFTER = box(0,0,0,16,16,16);

    private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Shapes.box(x1 / 16.0, y1 / 16.0, z1 / 16.0, x2 / 16.0, y2 / 16.0, z2 / 16.0);
    }

    private static VoxelShape or(VoxelShape first, VoxelShape... rest) {
        return Shapes.or(first, rest);
    }
}
