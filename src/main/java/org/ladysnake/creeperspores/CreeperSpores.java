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

import com.google.common.base.Suppliers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.ladysnake.creeperspores.common.CreeperSporeEffect;
import org.ladysnake.creeperspores.common.CreeperlingEntity;
import org.ladysnake.creeperspores.common.CreeperlingFertilizationPayload;
import org.ladysnake.creeperspores.mixin.EntityTypeAccessor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class CreeperSpores implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("creeper-spores");

    /** Identifiers corresponding to entity types that should be {@linkplain #registerCreeperLike(Identifier, EntityType)
        registered as creeper likes} if and when the entity type gets registered to {@link BuiltInRegistries#ENTITY_TYPE}.*/
    public static final Set<Identifier> CREEPER_LIKES = new HashSet<>(Arrays.asList(
            Identifier.fromNamespaceAndPath("minecraft", "creeper"),
            Identifier.fromNamespaceAndPath("mobz", "creep_entity"),
            Identifier.fromNamespaceAndPath("mobz", "crip_entity")
    ));

    public static final TagKey<Block> CREEPERLING_CAMOUFLAGE = TagKey.create(Registries.BLOCK, id("creeperling_camouflage"));
    public static final TagKey<Item> FERTILIZERS = TagKey.create(Registries.ITEM, id("fertilizers"));
    public static final TagKey<Item> SUPER_FERTILIZERS = TagKey.create(Registries.ITEM, id("super_fertilizers"));
    public static final TagKey<DamageType> SPAWNS_MORE_CREEPERLINGS = TagKey.create(Registries.DAMAGE_TYPE, id("spawns_more_creeperlings"));
    public static final TagKey<DamageType> EXTRA_CREEPER_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, id("extra_creeper_damage"));

    public static final Identifier CREEPERLING_FERTILIZATION_PACKET = id("creeperling-fertilization");
    public static final String GIVE_SPORES_TAG = "cspores:giveSpores";
    public static final int MAX_SPORE_TIME = 20 * 180;

    public static final GameRule<CreeperGrief> CREEPER_GRIEF =
            GameRuleBuilder.forEnum(CreeperGrief.CHARGED).buildAndRegister(id("creeper_grief"));
    public static final GameRule<Double> CREEPER_REPLACE_CHANCE =
            GameRuleBuilder.forDouble(0.2).buildAndRegister(id("creeper_replace_chance"));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("creeperspores", path);
    }

    public static <T> void visitRegistry(Registry<T> registry, BiConsumer<Identifier, T> visitor) {
        RegistryEntryAddedCallback.event(registry).register((index, identifier, entry) -> visitor.accept(identifier, entry));
        new HashSet<>(registry.keySet()).forEach(id -> visitor.accept(id, registry.getValue(id)));
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(CreeperlingFertilizationPayload.TYPE, CreeperlingFertilizationPayload.STREAM_CODEC);
        visitRegistry(BuiltInRegistries.ENTITY_TYPE, (id, type) -> {
            if (CREEPER_LIKES.contains(id)) {
                // can't actually check that the entity type is living, so just hope nothing goes wrong
                @SuppressWarnings("unchecked") EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
                registerCreeperLike(id, livingType);
            }
        });
    }

    @ApiStatus.Internal
    public static void registerCreeperLike(Identifier id) {
        // can't actually check that the entity type is living, so just hope nothing goes wrong
        // the cast to Optional<?> is not optional, according to javac
        @SuppressWarnings({"unchecked", "RedundantCast"}) Optional<EntityType<? extends LivingEntity>> creeperType = (Optional<EntityType<? extends LivingEntity>>) (Optional<?>) BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (creeperType.isPresent()) {
            registerCreeperLike(id, creeperType.get());
        } else {
            CREEPER_LIKES.add(id);
        }
    }

    @ApiStatus.Internal
    public static void registerCreeperLike(Identifier id, EntityType<? extends LivingEntity> type) {
        String prefix = id.getNamespace().equals("minecraft") ? "" : (id.toString().replace(':', '_') + "_");
        EntityType<CreeperlingEntity> creeperlingType = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                CreeperSpores.id(prefix + "creeperling"),
                createCreeperlingType(type, CreeperSpores.id(prefix + "creeperling"))
        );
		Holder<MobEffect> sporesEffect = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                CreeperSpores.id(prefix + "creeper_spore"),
                createCreeperSporesEffect(type)
        );
        CreeperEntry.register(type, creeperlingType, sporesEffect);
    }

    @Contract(pure = true)
    private static CreeperSporeEffect createCreeperSporesEffect(EntityType<?> creeperType) {
        return new CreeperSporeEffect(MobEffectCategory.NEUTRAL, 0x22AA00, creeperType);
    }

    @Contract(pure = true)
    private static EntityType<CreeperlingEntity> createCreeperlingType(EntityType<? extends LivingEntity> creeperType, Identifier id) {
        Supplier<CreeperEntry> kind = Suppliers.memoize(() -> CreeperEntry.get(creeperType));
        AttributeSupplier defaultAttributes = DefaultAttributes.getSupplier(creeperType);
        EntityType<CreeperlingEntity> creeperlingType = FabricEntityType.Builder.createMob((EntityType<CreeperlingEntity> type, Level world) -> new CreeperlingEntity(Objects.requireNonNull(kind.get()), world), creeperType.getCategory(), mob-> mob.defaultAttributes(()-> Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, defaultAttributes.getBaseValue(Attributes.MAX_HEALTH) * 0.5)
                .add(Attributes.MOVEMENT_SPEED, defaultAttributes.getBaseValue(Attributes.MOVEMENT_SPEED) * 0.8)))
                .sized(creeperType.getWidth() / 2f, creeperType.getHeight() / 2f)
                .clientTrackingRange(64)
                .updateInterval(1)
                .alwaysUpdateVelocity(true)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        ((EntityTypeAccessor) creeperlingType).setDescriptionId("entity.creeperspores.creeperling");
        return creeperlingType;
    }
}
