package com.dooji.menko.client;

import com.dooji.menko.MenkoEntityTypes;
import com.dooji.menko.client.hud.MenkoHud;
import com.dooji.menko.client.render.MenkoCardRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class MenkoClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(MenkoEntityTypes.MENKO_CARD, MenkoCardRenderer::new);
		MenkoHud.init();
	}
}
