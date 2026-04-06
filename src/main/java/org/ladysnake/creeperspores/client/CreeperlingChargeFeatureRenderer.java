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
package org.ladysnake.creeperspores.client;

import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;

public class CreeperlingChargeFeatureRenderer extends EnergySwirlLayer<CreeperRenderState, CreeperModel> {
    private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    private final CreeperModel creeperModel;

    public CreeperlingChargeFeatureRenderer(RenderLayerParent<CreeperRenderState, CreeperModel> ctx, EntityModelSet loader) {
        super(ctx);
        this.creeperModel = new CreeperModel(loader.bakeLayer(ModelLayers.CREEPER_ARMOR));
    }

    @Override
    protected boolean isPowered(CreeperRenderState state) {
        return state.isPowered;
    }

    @Override
    protected float xOffset(float v) {
        return v * 0.01F;
    }

    @Override
    protected Identifier getTextureLocation() {
        return SKIN;
    }

    @Override
    protected CreeperModel model() {
        return this.creeperModel;
    }

}
