/*
 * Creeper Spores
 * Copyright (C) 2019-2023 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package org.ladysnake.creeperspores.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.level.Level;
import org.ladysnake.creeperspores.common.CreeperlingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Ocelot.class, Cat.class})
public abstract class CatEntitiesMixin extends Animal {
    protected CatEntitiesMixin(EntityType<? extends Animal> type, Level world) {
        super(type, world);
    }

    @Inject(method = "registerGoals", at = @At("RETURN"))
    private void initGoals(CallbackInfo ci) {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, CreeperlingEntity.class, false));
    }
}
