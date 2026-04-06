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
package org.ladysnake.creeperspores.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.ladysnake.creeperspores.CreeperEntry;
import org.ladysnake.creeperspores.common.CreeperSporeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(EffectsInInventory.class)
public abstract class AbstractInventoryScreenMixin {

    @Unique
    private static final Holder<MobEffect> BASE_CREEPER_SPORES = CreeperEntry.getVanilla().sporeEffect();

    @ModifyVariable(method = "getEffectName", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/network/chat/Component;copy()Lnet/minecraft/network/chat/MutableComponent;"), name = "name")
    private MutableComponent creeperspores$updateRenderedEffectName(MutableComponent drawnString, @Local MobEffectInstance renderedEffect) {
        if (renderedEffect.getEffect() instanceof CreeperSporeEffect sporeEffect) {
            return sporeEffect.getLocalizedName().plainCopy();
        }
        return drawnString;
    }

    @WrapOperation(method = "extractEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getMobEffectSprite(Lnet/minecraft/core/Holder;)Lnet/minecraft/resources/Identifier;"))
    private Identifier creeperspores$updateRenderedEffectSprite(Holder<MobEffect> effect, Operation<Identifier> original) {
        if (effect.value() instanceof CreeperSporeEffect sporeEffect && sporeEffect != BASE_CREEPER_SPORES.value()) {
            return original.call(BASE_CREEPER_SPORES);
        }
        return original.call();
    }
}
