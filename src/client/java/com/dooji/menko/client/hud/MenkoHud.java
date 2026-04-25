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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.util.List;

public final class MenkoHud {
	private static Component message = Component.empty();
	private static boolean canCharge;
	private static long expiresAtMs;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(MenkoHudPayload.TYPE, (payload, context) -> {
			message = payload.message();
			canCharge = payload.canCharge();
			expiresAtMs = payload.durationTicks() > 0 ? Util.getMillis() + payload.durationTicks() * 50L : 0L;
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(Menko.MOD_ID, "hud"), MenkoHud::render);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (message == null || message.getString().isBlank()) {
			return;
		}

		if (expiresAtMs > 0L && Util.getMillis() >= expiresAtMs) {
			clear();
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Identifier texture = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "textures/gui/menko_hud.png");
		Component title = Component.literal("めんこ");
		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();
		int x = (width - 256) / 2;
		int y = height - 96;
		int titleX = x + 123 - font.width(title) / 2;
		int messageWidth = 216;
		int messageX = x + 123;
		List<FormattedCharSequence> lines = font.split(message, messageWidth);
		int lineCount = Math.min(lines.size(), 3);
		int messageY = y + 36 - (lineCount * font.lineHeight) / 2;

		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 256, 64, 256, 64);
		renderBoostBar(graphics, minecraft, x, y);
		graphics.text(font, title, titleX, y + 6, 0xFF2B1E13, false);
		for (int i = 0; i < lineCount; i++) {
			graphics.text(font, lines.get(i), messageX - font.width(lines.get(i)) / 2, messageY + i * font.lineHeight, 0xFF2B1E13, false);
		}
	}

	private static void renderBoostBar(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y) {
		if (!canCharge || minecraft.player == null || !minecraft.player.isUsingItem() || !(minecraft.player.getUseItem().getItem() instanceof MenkoCardItem)) {
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
		expiresAtMs = 0L;
	}
}
