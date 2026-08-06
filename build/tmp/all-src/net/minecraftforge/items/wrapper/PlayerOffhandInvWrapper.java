/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.items.wrapper;

import net.minecraft.world.entity.player.Inventory;

public class PlayerOffhandInvWrapper extends RangedWrapper
{
    public PlayerOffhandInvWrapper(Inventory inv)
    {
        super(new InvWrapper(inv), inv.f_35974_.size() + inv.f_35975_.size(),
                inv.f_35974_.size() + inv.f_35975_.size() + inv.f_35976_.size());
    }
}
