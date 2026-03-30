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

import com.mojang.serialization.DynamicLike;
import org.ladysnake.creeperspores.CreeperSpores;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import net.minecraft.world.level.GameRules;

@Mixin(GameRules.class)
public abstract class GameRulesMixin {
    @Shadow @Final private Map<GameRules.Key<?>, GameRules.Value<?>> rules;

    @Inject(method = "loadFromTag", at = @At("RETURN"))
    private void loadOldGamerules(DynamicLike<?> dynamicLike, CallbackInfo ci) {
        dynamicLike.get("cspores_creeperGrief").asString().result().ifPresent(((GameRuleKeyAccessor) this.rules.get(CreeperSpores.CREEPER_GRIEF))::cspores$deserialize);
    }
}
