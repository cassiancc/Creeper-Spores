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

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.creeperspores.CreeperEntry;
import org.ladysnake.creeperspores.CreeperSpores;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    public abstract float getHealth();

    @Shadow
    @Nullable
    public abstract MobEffectInstance getEffect(Holder<MobEffect> holder);

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDeadOrDying()Z", ordinal = 1))
    private void spawnCreeperling(ServerLevel level, DamageSource cause, float damage, CallbackInfoReturnable<Boolean> cir) {
        for (CreeperEntry creeperEntry : CreeperEntry.all()) {
            var spores = this.getEffect(creeperEntry.sporeEffect());
            if (spores != null) {
                float chance = 0.2f * (spores.getAmplifier() + 1);
                if (this.getHealth() <= 0.0f) {
                    chance *= 4;
                }
                if (cause.is(CreeperSpores.SPAWNS_MORE_CREEPERLINGS)) {
                    chance *= 2;
                }
                if (random.nextFloat() < chance) {
                    creeperEntry.spawnCreeperling(this);
                }
            }
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float dealDoubleFireDamage(float damageAmount, ServerLevel serverLevel, DamageSource damage) {
        //noinspection ConstantConditions
        if ((Entity) this instanceof Creeper && damage.is(CreeperSpores.EXTRA_CREEPER_DAMAGE)) {
            return damageAmount * 2;
        }
        return damageAmount;
    }
}
