package com.dooji.menko;

import com.dooji.menko.network.MenkoHudPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Menko implements ModInitializer {
	public static final String MOD_ID = "menko";

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(MenkoHudPayload.TYPE, MenkoHudPayload.CODEC);
		MenkoItems.init();
		MenkoEntityTypes.init();
		MenkoDispenserBehaviors.init();
		MenkoCreativeTabs.init();
		MenkoGameManager.init();
	}
}
