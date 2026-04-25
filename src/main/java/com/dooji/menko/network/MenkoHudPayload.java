package com.dooji.menko.network;

import com.dooji.menko.Menko;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MenkoHudPayload(Component message, int durationTicks, boolean canCharge) implements CustomPacketPayload {
	public static final Identifier MENKO_HUD_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "hud");
	public static final CustomPacketPayload.Type<MenkoHudPayload> TYPE = new CustomPacketPayload.Type<>(MENKO_HUD_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, MenkoHudPayload> CODEC = StreamCodec.composite(
		ComponentSerialization.TRUSTED_STREAM_CODEC,
		MenkoHudPayload::message,
		ByteBufCodecs.INT,
		MenkoHudPayload::durationTicks,
		ByteBufCodecs.BOOL,
		MenkoHudPayload::canCharge,
		MenkoHudPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
