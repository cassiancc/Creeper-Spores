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

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.ladysnake.creeperspores.CreeperEntry;
import org.ladysnake.creeperspores.CreeperSpores;
import org.ladysnake.creeperspores.mixin.EntityAccessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class CreeperlingEntity extends PathfinderMob implements PowerableMob {
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(CreeperlingEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int MATURATION_TIME = 20 * 60 * 8;

    private final CreeperEntry kind;

    private int ticksInSunlight = 0;
    private boolean trusting;
    private AvoidEntityGoal<Player> fleeGoal;

    public CreeperlingEntity(CreeperEntry kind, Level world) {
        super(kind.creeperlingType(), world);
        this.kind = kind;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 0.3D, Ingredient.of(CreeperSpores.FERTILIZERS), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.setTrusting(false);
    }

    @Override
    public boolean isPowered() {
        return this.isCharged();
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType spawnType) {
        return super.checkSpawnRules(world, spawnType) && this.level().getBrightness(LightLayer.SKY, this.blockPosition()) > 0;
    }

    public boolean isTrusting() {
        return trusting;
    }

    public void setTrusting(boolean trusting) {
        this.trusting = trusting;
        if (this.fleeGoal == null) {
            this.fleeGoal = new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.0D, 1.2D);
        }
        if (trusting) {
            this.goalSelector.removeGoal(fleeGoal);
        } else {
            this.goalSelector.addGoal(4, fleeGoal);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(CreeperSpores.FERTILIZERS)) {
            if (!this.level().isClientSide) {
                this.applyFertilizer(held);
                this.setTrusting(true);
            }
            return InteractionResult.SUCCESS;
        } else if (held.is(ItemTags.CREEPER_IGNITERS)) {
            SoundEvent soundEvent = held.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
            this.level().playSound(player, this.getX(), this.getY(), this.getZ(), soundEvent, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            if (!this.level().isClientSide) {
                this.setSecondsOnFire(4);
                this.hurt(this.level().damageSources().inFire(), 5);
                if (!held.isDamageableItem()) {
                    held.shrink(1);
                } else {
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            if (interactSpawnEgg(player, this, held, this.kind)) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    public static boolean interactSpawnEgg(Player player, Entity interacted, ItemStack stack, CreeperEntry kind) {
        Item item = stack.getItem();
        if (item instanceof SpawnEggItem && ((SpawnEggItem)item).getType(stack.getTag()) == EntityType.CREEPER) {
            if (!interacted.level().isClientSide) {
                CreeperlingEntity creeperling = kind.spawnCreeperling(interacted);
                if (creeperling != null) {
                    if (stack.hasCustomHoverName()) {
                        creeperling.setCustomName(stack.getHoverName());
                    }

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
            }
            return true;
        }
        return false;
    }

    ArrayList<ServerPlayer> trackingPlayers =  new ArrayList<>();

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        trackingPlayers.add(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        trackingPlayers.remove(player);
    }

    public void applyFertilizer(ItemStack boneMeal) {
        if (!this.level().isClientSide() && this.ticksInSunlight < MATURATION_TIME) {
            if (boneMeal.is(CreeperSpores.SUPER_FERTILIZERS)) {
                this.ticksInSunlight = MATURATION_TIME;
            } else {
                this.ticksInSunlight += (20 * (60 + 120 * this.random.nextFloat()));
            }

            boneMeal.shrink(1);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(this.getId());
            ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(CreeperSpores.CREEPERLING_FERTILIZATION_PACKET, buf);

            for (ServerPlayer p : trackingPlayers) {
                p.connection.send(packet);
            }
        }
    }

    public static void createParticles(BlockableEventLoop<?> ctx, Player player, FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        ctx.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof CreeperlingEntity) {
                for(int i = 0; i < 15; ++i) {
                    RandomSource random = e.level().random;
                    double speedX = random.nextGaussian() * 0.02D;
                    double speedY = random.nextGaussian() * 0.02D;
                    double speedZ = random.nextGaussian() * 0.02D;
                    e.level().addParticle(ParticleTypes.HAPPY_VILLAGER, e.getX() - 0.5 + random.nextFloat(), e.getY() + random.nextFloat(), e.getZ() - 0.5 + random.nextFloat(), speedX, speedY, speedZ);
                }
            }
        });
    }

    @Override
    public boolean hurt(DamageSource cause, float amount) {
        if (super.hurt(cause, amount)) {
            if (!this.level().isClientSide) {
                Entity attacker = cause.getEntity();
                if (attacker instanceof Ocelot || attacker instanceof Cat) {
                    ((ServerLevel)this.level()).sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + attacker.getEyeHeight(), attacker.getZ(), 0, 0, 0.2f, 0, 0.1D);
                }
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData ret = super.finalizeSpawn(world, difficulty, spawnReason, data, tag);
        float localDifficulty = difficulty.getSpecialMultiplier();
        this.ticksInSunlight = (int) (MATURATION_TIME * this.random.nextFloat() * 0.9 * localDifficulty);
        return ret;
    }

    @Override
    public int getExperienceReward() {
        return 2 + this.level().random.nextInt(3);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader worldView) {
        // Creeperlings like sunlight
        int skyLightLevel = worldView.getBrightness(LightLayer.SKY, pos);
        // method_28516 == getBrightness
        float skyFavor = computeBrightnessByLightLevel(worldView.dimensionType().ambientLight())[skyLightLevel] * 3.0F;
        // But they can do with artificial light if there is not anything better
        float brightnessAtPos = worldView.getPathfindingCostFromLightLevels(pos);
        float favor = Math.max(brightnessAtPos, skyFavor);
        // They like good soils too
        if (worldView.getBlockState(pos.below(1)).is(BlockTags.BAMBOO_PLANTABLE_ON)) {
            favor += 3.0F;
        }
        // What they really want is camouflage
        if (worldView.getBlockState(pos).is(CreeperSpores.CREEPERLING_CAMOUFLAGE)) {
            favor += 4.0F;
        }
        return favor;
    }
    //this existed in DimensionType, apparently
    private static float[] computeBrightnessByLightLevel(float ambientLight) {
        float[] fs = new float[16];
        for (int i = 0; i <= 15; ++i) {
            float f = (float)i / 15.0f;
            float g = f / (4.0f - 3.0f * f);
            fs[i] = Mth.lerp(ambientLight, g, 1.0f);
        }
        return fs;
    }

    public boolean isCharged() {
        return this.entityData.get(CHARGED);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.isCharged()) {
            tag.putBoolean("powered", true);
        }
        tag.putBoolean("trusting", this.isTrusting());
        tag.putInt("ticksInSunlight", this.ticksInSunlight);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("powered")) {
            this.entityData.set(CHARGED, tag.getBoolean("powered"));
        }
        if (tag.contains("ticksInSunlight")) {
            this.ticksInSunlight = tag.getInt("ticksInSunlight");
        }
        if (tag.contains("trusting")) {
            this.setTrusting(tag.getBoolean("trusting"));
        }
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        super.thunderHit(world, lightning);
        this.entityData.set(CHARGED, true);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.level().getDifficulty() != Difficulty.PEACEFUL) {
            if (this.random.nextFloat() < this.getGrowthChance()) {
                ++this.ticksInSunlight;
            }
            if (this.ticksInSunlight >= MATURATION_TIME) {
                LivingEntity adult = kind.creeperType().create(this.level());
                if (adult == null) {    // fallback to vanilla creeper
                    adult = Objects.requireNonNull(CreeperEntry.getVanilla().creeperType().create(this.level()));
                }

                AttributeInstance adultMaxHealthAttr = adult.getAttribute(Attributes.MAX_HEALTH);
                AttributeInstance babyMaxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);

                assert adultMaxHealthAttr != null && babyMaxHealthAttr != null;

                UUID adultUuid = adult.getUUID();
                double defaultMaxHealth = adultMaxHealthAttr.getBaseValue();
                double healthMultiplier = adultMaxHealthAttr.getValue() / babyMaxHealthAttr.getValue();
                adult.load(this.saveWithoutId(new CompoundTag()));
                adult.setUUID(adultUuid);
                adultMaxHealthAttr.setBaseValue(defaultMaxHealth);
                adult.setHealth(adult.getHealth() * (float)healthMultiplier);

                this.level().addFreshEntity(adult);
                pushOutOfBlocks(adult);
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    private float getGrowthChance() {
        float skyExposition = this.level().getBrightness(LightLayer.SKY, this.blockPosition()) / 15f;
        return this.level().isDay() ? skyExposition : skyExposition * 0.5f * this.level().getMoonBrightness();
    }

    private static void pushOutOfBlocks(Entity self) {
        AABB bb = self.getBoundingBox();
        EntityAccessor access = ((EntityAccessor) self);
        access.invokeMoveTowardsClosestSpace(self.getX() - (double)self.getBbWidth() * 0.35D, bb.minY + 0.5D, self.getZ() + (double)self.getBbWidth() * 0.35D);
        access.invokeMoveTowardsClosestSpace(self.getX() - (double)self.getBbWidth() * 0.35D, bb.minY + 0.5D, self.getZ() - (double)self.getBbWidth() * 0.35D);
        access.invokeMoveTowardsClosestSpace(self.getX() + (double)self.getBbWidth() * 0.35D, bb.minY + 0.5D, self.getZ() - (double)self.getBbWidth() * 0.35D);
        access.invokeMoveTowardsClosestSpace(self.getX() + (double)self.getBbWidth() * 0.35D, bb.minY + 0.5D, self.getZ() + (double)self.getBbWidth() * 0.35D);
    }

    @Override
    public boolean removeWhenFarAway(double sqDistance) {
        return sqDistance > (128*128);
    }

    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource cause) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }
}
