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

import org.ladysnake.creeperspores.CreeperEntry;
import org.ladysnake.creeperspores.CreeperSpores;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static org.spongepowered.asm.mixin.injection.At.Shift.AFTER;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;

@Mixin(NaturalSpawner.class)
public abstract class SpawnHelperMixin {
    @ModifyVariable(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;moveTo(DDDFF)V", shift = AFTER))
    private static Mob substituteCreeper(Mob spawnedEntity) {
        if (spawnedEntity instanceof Creeper
                && spawnedEntity.level().getBrightness(LightLayer.SKY, spawnedEntity.blockPosition()) > 0
                && spawnedEntity.level().getGameRules().getRule(CreeperSpores.CREEPER_REPLACE_CHANCE).get() > spawnedEntity.getRandom().nextDouble()) {
            CreeperEntry creeperEntry = CreeperEntry.get(spawnedEntity.getType());
            if (creeperEntry != null) {
                return creeperEntry.createCreeperling(spawnedEntity);
            }
        }
        return spawnedEntity;
    }
}
