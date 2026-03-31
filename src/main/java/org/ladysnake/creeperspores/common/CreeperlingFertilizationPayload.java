package org.ladysnake.creeperspores.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.ladysnake.creeperspores.CreeperSpores.id;

public record CreeperlingFertilizationPayload(int entityId) implements CustomPacketPayload {
	public static final ResourceLocation ID = id("creeperling-fertilization");
	public static final Type<CreeperlingFertilizationPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<FriendlyByteBuf, CreeperlingFertilizationPayload> STREAM_CODEC = ByteBufCodecs.INT.map(CreeperlingFertilizationPayload::new, CreeperlingFertilizationPayload::entity).cast();

	public int entity() {
		return entityId;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
