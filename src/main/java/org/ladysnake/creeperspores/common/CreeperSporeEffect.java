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
package org.ladysnake.creeperspores.common;

import com.google.common.base.Suppliers;
import org.ladysnake.creeperspores.CreeperEntry;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class CreeperSporeEffect extends MobEffect {
    private final EntityType<?> creeperType;
    private final Supplier<CreeperEntry> creeperEntry;

    public CreeperSporeEffect(MobEffectCategory type, int color, EntityType<?> creeperType) {
        super(type, color);
        this.creeperType = creeperType;
        this.creeperEntry = Suppliers.memoize(() -> Objects.requireNonNull(CreeperEntry.get(this.creeperType)));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1 && Math.random() < 0.6;
    }

    @Override
    public void applyEffectTick(LivingEntity affected, int amplifier) {
        this.creeperEntry.get().spawnCreeperling(affected);
    }

    @Override
    public String getOrCreateDescriptionId() {
        return "effect.creeperspores.creeper_spore";
    }

    public Component getLocalizedName() {
        return Component.translatable("effect.creeperspores.generic_spore", this.creeperType.getDescription());
    }

}
