package com.dooji.menko;

import com.dooji.menko.entity.MenkoCardEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class MenkoEntityTypes {
	private static final Identifier MENKO_CARD_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_card");
	private static final ResourceKey<EntityType<?>> MENKO_CARD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MENKO_CARD_ID);

	public static final EntityType<MenkoCardEntity> MENKO_CARD = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MENKO_CARD_ID,
		EntityType.Builder.<MenkoCardEntity>of(MenkoCardEntity::new, MobCategory.MISC)
			.sized(Math.max(MenkoCardEntity.CARD_WIDTH, MenkoCardEntity.CARD_HEIGHT), MenkoCardEntity.CARD_THICKNESS)
			.clientTrackingRange(64)
			.updateInterval(1)
			.build(MENKO_CARD_KEY)
	);

	public static void init() {
	}
}
