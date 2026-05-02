package com.dooji.menko.client.hud;

import com.dooji.menko.Menko;
import com.dooji.menko.item.MenkoCardItem;
import com.dooji.menko.network.MenkoHudPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.List;

public final class MenkoHud {
	private static Component message = Component.empty();
	private static boolean canCharge;
	private static boolean autoHide;
	private static long expiresAtMs;
	private static long peekUntilMs;
	private static float slideOffset = 116.0f;
	private static float slideTarget = 116.0f;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(MenkoHudPayload.TYPE, (payload, context) -> {
			boolean wasCharge = canCharge;
			message = payload.message();
			canCharge = payload.canCharge();
			autoHide = payload.durationTicks() > 0;
			expiresAtMs = autoHide ? Util.getMillis() + payload.durationTicks() * 50L : 0L;
			peekUntilMs = message.getString().isBlank() ? 0L : Util.getMillis() + 2000L;
			slideTarget = 0.0f;
			if (canCharge && !wasCharge) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.2f));
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(Menko.MOD_ID, "hud"), MenkoHud::render);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		long now = Util.getMillis();
		if (expiresAtMs > 0L && now >= expiresAtMs) {
			expiresAtMs = 0L;
			peekUntilMs = 0L;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (message == null || message.getString().isBlank()) {
			slideTarget = 116.0f;
		} else if (expiresAtMs > 0L) {
			slideTarget = 0.0f;
		} else if (autoHide) {
			slideTarget = 116.0f;
		} else if (canCharge && isChargingCard(minecraft)) {
			slideTarget = 0.0f;
		} else if (now < peekUntilMs) {
			slideTarget = 0.0f;
		} else {
			slideTarget = 108.0f;
		}

		float delta = (float) deltaTracker.getGameTimeDeltaTicks();
		slideOffset = Mth.approach(slideOffset, slideTarget, delta * 5.0f);

		if (slideTarget >= 115.5f && slideOffset >= 115.5f) {
			if (!message.getString().isBlank()) {
				clear();
			}

			return;
		}

		if (message == null || message.getString().isBlank()) {
			return;
		}

		Font font = minecraft.font;
		Identifier texture = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "textures/gui/menko_hud.png");
		Component title = Component.literal("めんこ");
		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();
		int x = (width - 256) / 2;
		int y = height - 116 + (int) slideOffset;
		int titleX = x + 123 - font.width(title) / 2;
		int messageX = x + 123;
		List<FormattedCharSequence> lines = font.split(message, 216);
		int lineCount = Math.min(lines.size(), 3);
		int messageY = y + 36 - (lineCount * font.lineHeight) / 2;

		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 256, 64, 256, 64);
		renderBoostBar(graphics, minecraft, x, y);
		graphics.text(font, title, titleX, y + 6, 0xFF2B1E13, false);
		for (int i = 0; i < lineCount; i++) {
			graphics.text(font, lines.get(i), messageX - font.width(lines.get(i)) / 2, messageY + i * font.lineHeight, 0xFF2B1E13, false);
		}
	}

	private static boolean isChargingCard(Minecraft minecraft) {
		return minecraft.player != null
			&& minecraft.player.isUsingItem()
			&& minecraft.player.getUseItem().getItem() instanceof MenkoCardItem;
	}

	private static void renderBoostBar(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y) {
		if (!canCharge || !isChargingCard(minecraft)) {
			return;
		}

		int usedTicks = 20 - minecraft.player.getUseItemRemainingTicks();
		float progress = Math.clamp(usedTicks / 20.0f, 0.0f, 1.0f);
		if (progress <= 0.0f) {
			return;
		}

		int barLeft = x + 244;
		int barRight = x + 248;
		int barTop = y + 8;
		int barBottom = y + 56;
		int fillHeight = Math.max(1, Math.round((barBottom - barTop) * progress));
		graphics.fillGradient(barLeft, barBottom - fillHeight, barRight, barBottom, 0xFF2A4B1F, 0xFFCAA48A);
	}

	private static void clear() {
		message = Component.empty();
		canCharge = false;
		autoHide = false;
		expiresAtMs = 0L;
		peekUntilMs = 0L;
		slideOffset = 116.0f;
		slideTarget = 116.0f;
	}
}
