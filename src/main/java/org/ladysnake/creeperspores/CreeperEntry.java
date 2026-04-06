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
package org.ladysnake.creeperspores;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntitySpawnReason;
import org.ladysnake.creeperspores.common.CreeperSporeEffect;
import org.ladysnake.creeperspores.common.CreeperlingEntity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record CreeperEntry(EntityType<? extends LivingEntity> creeperType,
                           EntityType<CreeperlingEntity> creeperlingType,
                           Holder<MobEffect> sporeEffect) {
    private static final Map<EntityType<?>, CreeperEntry> CREEPER_ENTRIES = new HashMap<>();

    static void register(EntityType<? extends LivingEntity> type, EntityType<CreeperlingEntity> creeperlingType, Holder<MobEffect> sporesEffect) {
        CREEPER_ENTRIES.put(type, new CreeperEntry(type, creeperlingType, sporesEffect));
    }

    public static Collection<CreeperEntry> all() {
        return CREEPER_ENTRIES.values();
    }

    @Nullable
    public static CreeperEntry get(EntityType<?> creeperType) {
        return CREEPER_ENTRIES.get(creeperType);
    }

    public static CreeperEntry getVanilla() {
        return Objects.requireNonNull(CREEPER_ENTRIES.get(EntityType.CREEPER));
    }

    /**
     * Spawns a creeperling at an affected entity
     */
    public CreeperlingEntity spawnCreeperling(Entity affected) {
        if (!affected.level().isClientSide()) {
            CreeperlingEntity spawn = Objects.requireNonNull(this.creeperlingType.create(affected.level(), EntitySpawnReason.EVENT));
            spawn.snapTo(affected.getX(), affected.getY(), affected.getZ(), 0, 0);
            affected.level().addFreshEntity(spawn);
            return spawn;
        }
        return null;
    }

    /**
     * Create a creeperling for an entity, without spawning it
     */
    public CreeperlingEntity createCreeperling(LivingEntity entity) {
        CreeperlingEntity creeperlingEntity = Objects.requireNonNull(creeperlingType.create(entity.level(), EntitySpawnReason.EVENT));
        creeperlingEntity.copyPosition(entity);
        return creeperlingEntity;
    }
}
