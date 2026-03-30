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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.ladysnake.creeperspores.common.CreeperSporeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin {
    @Unique
    private List<MobEffectInstance> renderedEffects;
    @Unique
    private int renderedEffectsIndex;

    @Inject(method = "renderLabels(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/Iterable;)V", at = @At("HEAD"))
    private void creeperspores$retrieveRenderedEffects(GuiGraphics graphics, int x, int height, Iterable<MobEffectInstance> effects, CallbackInfo ci) {
        renderedEffects = (List<MobEffectInstance>) effects;
        renderedEffectsIndex = 0;
    }

    @Inject(method = "renderLabels(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/Iterable;)V", at = @At("RETURN"))
    private void creeperspores$clearRenderedEffects(GuiGraphics graphics, int x, int height, Iterable<MobEffectInstance> effects, CallbackInfo ci) {
        renderedEffects = null;
    }

    @ModifyVariable(method = "getEffectName", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/network/chat/Component;copy()Lnet/minecraft/network/chat/MutableComponent;"), index = 2)
    private MutableComponent creeperspores$updateRenderedEffectName(MutableComponent drawnString) {
        if (renderedEffects != null) {
            MobEffect renderedEffect = renderedEffects.get(renderedEffectsIndex++).getEffect();
            if (renderedEffect instanceof CreeperSporeEffect sporeEffect) {
                return sporeEffect.getLocalizedName().plainCopy();
            }
        }
        return drawnString;
    }
}
