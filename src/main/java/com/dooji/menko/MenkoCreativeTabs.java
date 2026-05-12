package com.dooji.menko;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

class MenkoCreativeTabs {
	private static final ResourceKey<CreativeModeTab> MENKO = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko"));

	public static void init() {
		Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			MENKO,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.menko.menko"))
				.icon(() -> new ItemStack(MenkoItems.MENKO_NIGHTSKY))
				.displayItems((parameters, output) -> {
					output.accept(MenkoItems.MENKO_NIGHTSKY);
					output.accept(MenkoItems.MENKO_SAKURAFROG);
					output.accept(MenkoItems.MENKO_BEACH);
					output.accept(MenkoItems.MENKO_MARS);
					output.accept(MenkoItems.MENKO_SPACE);
					output.accept(MenkoItems.MENKO_NIGHTSKY_NONFUNC);
					output.accept(MenkoItems.MENKO_SAKURAFROG_NONFUNC);
					output.accept(MenkoItems.MENKO_BEACH_NONFUNC);
					output.accept(MenkoItems.MENKO_MARS_NONFUNC);
					output.accept(MenkoItems.MENKO_SPACE_NONFUNC);
					output.accept(MenkoItems.MENKO_NIGHTSKY_PERSISTENT);
					output.accept(MenkoItems.MENKO_SAKURAFROG_PERSISTENT);
					output.accept(MenkoItems.MENKO_BEACH_PERSISTENT);
					output.accept(MenkoItems.MENKO_MARS_PERSISTENT);
					output.accept(MenkoItems.MENKO_SPACE_PERSISTENT);
				})
				.build()
		);
	}
}
